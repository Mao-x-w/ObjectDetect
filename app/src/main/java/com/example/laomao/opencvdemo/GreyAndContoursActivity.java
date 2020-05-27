package com.example.laomao.opencvdemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class GreyAndContoursActivity extends BaseActivity {

    @BindView(R.id.grey_img)
    ImageView mGreyImg;
    @BindView(R.id.contours_img)
    ImageView mContoursImg;
    private String mPathname;

    private double maxArea = 0;
    private List<Double> mList=new ArrayList<>();

    private int whiteNum = 0;
    private int allNum =0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grey_and_contours);
        ButterKnife.bind(this);

        mPathname = getIntent().getStringExtra("path");

        convert2Gray();
//        contours();

        Bitmap bitmap = BitmapFactory.decodeFile(mPathname);
        Bitmap binaryzationBitmap = getBinaryzationBitmap(bitmap);
        mContoursImg.setImageBitmap(binaryzationBitmap);

        //先经过一步灰度化
//        Mat src = Imgcodecs.imread(mPathname);
//        Mat gray = new Mat();
//        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
//        src = gray;
//        //二值化
//        binaryzation(src);
//
//        Bitmap bitmap = grayMat2Bitmap(src);
//        mContoursImg.setImageBitmap(bitmap);

    }

    @OnClick({R.id.grey_process, R.id.contours_detect, R.id.detect_size})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.grey_process:
                convert2Gray();
                break;
            case R.id.contours_detect:
                contours();
                break;
            case R.id.detect_size:
                maxArea=297*210*(whiteNum*1.0/ allNum *1.0);
                startActivity(new Intent(getContext(),CalculateSizeActivity.class).putExtra("area",maxArea));
                break;
        }
    }

    private void contours() {
        // read image
        Mat src = Imgcodecs.imread(mPathname);


        if (src.empty()) {
            return;
        }
        Mat dst = new Mat();

        measureContours(src, dst);

        // 转换为Bitmap，显示
        Bitmap bm = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);
        Mat result = new Mat();
        Imgproc.cvtColor(dst, result, Imgproc.COLOR_BGR2RGBA);
        Utils.matToBitmap(result, bm);

        // show
        mContoursImg.setImageBitmap(bm);

        // release memory
        src.release();
        dst.release();
        result.release();
    }

    private void convert2Gray() {
        Mat src = Imgcodecs.imread(mPathname);
        if (src.empty()) {
            return;
        }
        Mat dst = new Mat();

        Imgproc.cvtColor(src, dst, Imgproc.COLOR_BGR2GRAY);
        Bitmap bitmap = grayMat2Bitmap(dst);
        mGreyImg.setImageBitmap(bitmap);
        src.release();
        dst.release();
    }

    private Bitmap grayMat2Bitmap(Mat result) {
        Mat image = null;
        if (result.cols() > 1000 || result.rows() > 1000) {
            image = new Mat();
            Imgproc.resize(result, image, new Size(result.cols() / 4, result.rows() / 4));
        } else {
            image = result;
        }
        Bitmap bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
        Imgproc.cvtColor(image, image, Imgproc.COLOR_GRAY2RGBA);
        Utils.matToBitmap(image, bitmap);
        image.release();
        return bitmap;
    }

    private void measureContours(Mat src, Mat dst) {
        Mat gray = new Mat();
        Mat binary = new Mat();

//        binaryzation(src);
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

        // 二值
//        BackgroundSubtractorMOG2 backgroundSubtractorMOG2 = BackgroundSubtractorMOG2.__fromPtr__(0);
//        backgroundSubtractorMOG2.setDetectShadows(false);
//        backgroundSubtractorMOG2.apply(gray,binary);

        Imgproc.threshold(gray, binary, 0, 255, Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU);

        // 轮廓发现
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        // 测量轮廓
        dst.create(src.size(), src.type());
        for (int i = 0; i < contours.size(); i++) {
            Rect rect = Imgproc.boundingRect(contours.get(i));
            double w = rect.width;
            double h = rect.height;
            double rate = Math.min(w, h) / Math.max(w, h);
            Log.i("Bound Rect", "rate : " + rate);
            RotatedRect minRect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(i).toArray()));
            w = minRect.size.width;
            h = minRect.size.height;
            rate = Math.min(w, h) / Math.max(w, h);
            Log.i("Min Bound Rect", "rate : " + rate);

            double area = Imgproc.contourArea(contours.get(i), false);
            double arclen = Imgproc.arcLength(new MatOfPoint2f(contours.get(i).toArray()), true);
            Log.i("contourArea", "area : " + area);
            Log.i("arcLength", "arcLength : " + arclen);
            Imgproc.drawContours(dst, contours, i, new Scalar(0, 0, 255), 1);

            if (area>0){
                mList.add(area);
            }

            if (area > maxArea) {
                maxArea = area;
            }
        }

        // 释放内存
        gray.release();
        binary.release();
    }

    // threshold根据亮度去除背景
    private Mat MyThresholdHsv(Mat frame) {
        Mat hsvImg = new Mat();
        List<Mat> hsvPlanes = new ArrayList<>();
        Mat thresholdImg = new Mat();

        // threshold the image with the average hue value
        hsvImg.create(frame.size(), CvType.CV_8U);
        Imgproc.cvtColor(frame, hsvImg, Imgproc.COLOR_BGR2HSV);
        Core.split(hsvImg, hsvPlanes);
        // get the average hue value of the image
        Scalar average = Core.mean(hsvPlanes.get(0));
        double threshValue = average.val[0];
        Imgproc.threshold(hsvPlanes.get(0), thresholdImg, threshValue, 179.0, Imgproc.THRESH_BINARY_INV);

        Imgproc.blur(thresholdImg, thresholdImg, new Size(15, 15));

        // dilate to fill gaps, erode to smooth edges
        Imgproc.dilate(thresholdImg, thresholdImg, new Mat(), new Point(-1, -1), 1);
        Imgproc.erode(thresholdImg, thresholdImg, new Mat(), new Point(-1, -1), 3);

        Imgproc.threshold(thresholdImg, thresholdImg, threshValue, 179.0, Imgproc.THRESH_BINARY);

        // create the new image
        Mat foreground = new Mat(frame.size(), CvType.CV_8UC3, new Scalar(0, 0, 0));
        thresholdImg.convertTo(thresholdImg, CvType.CV_8U);
        frame.copyTo(foreground, thresholdImg);
        return foreground;
    }

    //grabCut分割技术
    public static Mat myGrabCut(Mat in, Point tl, Point br) {
        Mat mask = new Mat();
        Mat image = in;
        mask.create(image.size(), CvType.CV_8UC1);
        mask.setTo(new Scalar(0));

        Mat bgdModel = new Mat();// Mat.eye(1, 13 * 5, CvType.CV_64FC1);
        Mat fgdModel = new Mat();// Mat.eye(1, 13 * 5, CvType.CV_64FC1);

        Mat source = new Mat(1, 1, CvType.CV_8U, new Scalar(3));
        Rect rectangle = new Rect(tl, br);
        Imgproc.grabCut(image, mask, rectangle, bgdModel, fgdModel, 3, Imgproc.GC_INIT_WITH_RECT);
        Core.compare(mask, source, mask, Core.CMP_EQ);
        Mat foreground = new Mat(image.size(), CvType.CV_8UC1, new Scalar(0, 0, 0));
        image.copyTo(foreground, mask);

        return foreground;

    }

    //findContours分割技术
    private Mat MyFindLargestRectangle(Mat original_image) {
        Mat imgSource = original_image;
        Imgproc.cvtColor(imgSource, imgSource, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Canny(imgSource, imgSource, 50, 50);
        Imgproc.GaussianBlur(imgSource, imgSource, new Size(5, 5), 5);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(imgSource, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        double maxArea = 0;
        int maxAreaIdx = -1;
        MatOfPoint largest_contour = contours.get(0);
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        for (int idx = 0; idx < contours.size(); idx++) {
            MatOfPoint temp_contour = contours.get(idx);
            double contourarea = Imgproc.contourArea(temp_contour);
            if (contourarea - maxArea > 1) {
                maxArea = contourarea;
                largest_contour = temp_contour;
                maxAreaIdx = idx;
                MatOfPoint2f new_mat = new MatOfPoint2f(temp_contour.toArray());
                int contourSize = (int) temp_contour.total();
                Imgproc.approxPolyDP(new_mat, approxCurve, contourSize * 0.05, true);
            }
        }


        Imgproc.drawContours(imgSource, contours, -1, new Scalar(255, 0, 0), 1);
        Imgproc.fillConvexPoly(imgSource, largest_contour, new Scalar(255, 255, 255));
        Imgproc.drawContours(imgSource, contours, maxAreaIdx, new Scalar(0, 0, 255), 3);

        return imgSource;
    }

    public static Mat removeBackground(Mat nat) {
        Mat m = new Mat();

        Imgproc.cvtColor(nat, m, Imgproc.COLOR_BGR2GRAY);
        double threshold = Imgproc.threshold(m, m, 0, 255, Imgproc.THRESH_OTSU);
        Mat pre = new Mat(nat.size(), CvType.CV_8UC3, new Scalar(0, 0, 0));
        Mat fin = new Mat(nat.size(), CvType.CV_8UC3, new Scalar(0, 0, 0));
        for (int i = 0; i < m.rows(); i++) {
            for (int j = 0; j < m.cols(); j++) {
                double[] ds = m.get(i, j);
                double[] data = { ds[0] / 255, ds[0] / 255, ds[0] / 255 };
                pre.put(i, j, data);
            }
        }
        for (int i = 0; i < pre.rows(); i++) {
            for (int j = 0; j < pre.cols(); j++) {
                double[] pre_ds = pre.get(i, j);
                double[] nat_ds = nat.get(i, j);
                double[] data = { pre_ds[0] * nat_ds[0], pre_ds[1] * nat_ds[1], pre_ds[2] * nat_ds[2] };
                fin.put(i, j, data);
            }
        }

        return fin;
    }

    public void binaryzation(Mat mat) {
        int BLACK = 0;
        int WHITE = 255;
        int ucThre = 0, ucThre_new = 127;
        int nBack_count, nData_count;
        int nBack_sum, nData_sum;
        int nValue;
        int i, j;

        int width = mat.width(), height = mat.height();
        //寻找最佳的阙值
        while (ucThre != ucThre_new) {
            nBack_sum = nData_sum = 0;
            nBack_count = nData_count = 0;

            for (j = 0; j < height; ++j) {
                for (i = 0; i < width; i++) {
                    nValue = (int) mat.get(j, i)[0];

                    if (nValue > ucThre_new) {
                        nBack_sum += nValue;
                        nBack_count++;
                    } else {
                        nData_sum += nValue;
                        nData_count++;
                    }
                }
            }

            nBack_sum = nBack_sum / nBack_count;
            nData_sum = nData_sum / nData_count;
            ucThre = ucThre_new;
            ucThre_new = (nBack_sum + nData_sum) / 2;
        }

        //二值化处理
        int nBlack = 0;
        int nWhite = 0;
        for (j = 0; j < height; ++j) {
            for (i = 0; i < width; ++i) {
                nValue = (int) mat.get(j, i)[0];
                if (nValue > ucThre_new) {
                    mat.put(j, i, WHITE);
                    nWhite++;
                } else {
                    mat.put(j, i, BLACK);
                    nBlack++;
                }
            }
        }

        // 确保白底黑字
        if (nBlack > nWhite) {
            for (j = 0; j < height; ++j) {
                for (i = 0; i < width; ++i) {
                    nValue = (int) (mat.get(j, i)[0]);
                    if (nValue == 0) {
                        mat.put(j, i, WHITE);
                    } else {
                        mat.put(j, i, BLACK);
                    }
                }
            }
        }
    }


    /**
     * 对图片进行二值化处理
     *
     * @param bm
     *            原始图片
     * @return 二值化处理后的图片
     */
    public Bitmap getBinaryzationBitmap(Bitmap bm) {
        Bitmap bitmap = null;
        // 获取图片的宽和高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 创建二值化图像
        bitmap = bm.copy(Bitmap.Config.ARGB_8888, true);
        // 遍历原始图像像素,并进行二值化处理
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // 得到当前的像素值
                int pixel = bitmap.getPixel(i, j);
                // 得到Alpha通道的值
                int alpha = pixel & 0xFF000000;
                // 得到Red的值
                int red = (pixel & 0x00FF0000) >> 16;
                // 得到Green的值
                int green = (pixel & 0x0000FF00) >> 8;
                // 得到Blue的值
                int blue = pixel & 0x000000FF;
                // 通过加权平均算法,计算出最佳像素值
                int gray = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
                // 对图像设置黑白图
                if (gray <= 95) {
                    gray = 255;
                    whiteNum++;
                } else {
                    gray = 0;
                }
                allNum++;
                // 得到新的像素值
                int newPiexl = alpha | (gray << 16) | (gray << 8) | gray;
                // 赋予新图像的像素
                bitmap.setPixel(i, j, newPiexl);
            }
        }
        return bitmap;
    }

}
