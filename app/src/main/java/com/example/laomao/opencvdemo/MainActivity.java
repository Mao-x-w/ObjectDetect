package com.example.laomao.opencvdemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.laomao.opencvdemo.image.ImagePicker;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
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
import org.opencv.video.BackgroundSubtractorKNN;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends BaseActivity {
    @BindView(R.id.page_width)
    EditText mPageWidth;
    @BindView(R.id.page_height)
    EditText mPageHeight;

    private String mPathname;
    private int mWidth;
    private int mHeight;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_openvc_demo);
        ButterKnife.bind(this);

        iniLoadOpenCV();
    }

    private void getData() {
        mWidth = Integer.valueOf(mPageWidth.getText().toString().trim());
        mHeight = Integer.valueOf(mPageHeight.getText().toString().trim());
    }

    private void iniLoadOpenCV() {
        boolean success = OpenCVLoader.initDebug();
        if (success) {
            Log.d("MainActivity","OpenCV Libraries loaded...");
        } else {
            Toast.makeText(this.getApplicationContext(), "WARNING: Could not load OpenCV Libraries!", Toast.LENGTH_LONG).show();
        }
    }

    @OnClick({R.id.pick_img})
    public void onClick(View view) {
        getData();
        switch (view.getId()) {
            case R.id.pick_img:
                //自定义方法的单选
                ImagePicker.from(getContext())
                        .single(true)
                        .cropWHScale((float) (mWidth*1.0/mHeight*1.0))
                        .listener(new ImagePicker.ImagePickCallback() {
                            @Override
                            public void onPicked(List<String> images) {
                                mPathname = images.get(0);
                                startActivity(new Intent(getContext(),GreyAndContoursActivity.class).putExtra("path",mPathname));
                            }

                            @Override
                            public void onCancel() {

                            }

                            @Override
                            public void onError() {

                            }
                        }).picker();
                break;
        }
    }


    static {
        System.loadLibrary("opencv_java3");
    }
}
