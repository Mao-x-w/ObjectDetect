package com.example.laomao.opencvdemo.image;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;


import com.example.laomao.opencvdemo.BaseActivity;
import com.example.laomao.opencvdemo.Configuration;
import com.hwangjr.rxbus.RxBus;

import java.io.File;

import image.selector.ImageSelectorActivity;
import image.selector.crop.ImageCropperActivity;


/**
 * User: Joy
 * Date: 2016/11/8
 * Time: 19:04
 */

public class ImagePickerActivity extends BaseActivity {

	public static final String EXTRA_IMAGE_PICKER_MODE = "_mode";

	public static final int IMAGE_PICKER_MODE_CAMERA = -1;
	public static final int IMAGE_PICKER_MODE_CAMERA_CROP = -2;
	public static final int IMAGE_PICKER_MODE_GALLERY_SINGLE = ImageSelectorActivity.IMAGE_SELECTOR_MODE_SINGLE;
	public static final int IMAGE_PICKER_MODE_GALLERY_SINGLE_CROP = ImageSelectorActivity.IMAGE_SELECTOR_MODE_SINGLE_CROP;
	public static final int IMAGE_PICKER_MODE_GALLERY_MULTI = ImageSelectorActivity.IMAGE_SELECTOR_MODE_MULTI;

	public static final String EXTRA_IMAGE_CROP_SHAPE = "_shape";
	public static final String EXTRA_IMAGE_CROP_WIDTH_HEIGHT_SCALE="_scale";
	public static final int IMAGE_CROP_SHAPE_SQUARE = 0x1;
	public static final int IMAGE_CROP_SHAPE_CIRCLE = 0x2;

	public static final String EXTRA_IMAGE_COUNT = "_count";

	public static final String RX_EVENT_IMAGE = "_image";

	private static final int REQUEST_CAMERA = 1001;
	private static final int REQUEST_GALLERY = 1002;
	private static final int REQUEST_CROPPER_IMAGE = 1003;

	private int mode = 0;
	private int shape = 1;
	private float cropWHScale=1.0f;

	private String mImageSavePath;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mode = getIntent().getIntExtra(EXTRA_IMAGE_PICKER_MODE, IMAGE_PICKER_MODE_CAMERA);
		shape = getIntent().getIntExtra(EXTRA_IMAGE_CROP_SHAPE, IMAGE_CROP_SHAPE_SQUARE);
		cropWHScale = getIntent().getFloatExtra(EXTRA_IMAGE_CROP_WIDTH_HEIGHT_SCALE,1);
		mImageSavePath = String.format("%s/%s.jpg", Configuration.getInstance().getTempPath(), System.currentTimeMillis());
		if (mode < IMAGE_PICKER_MODE_GALLERY_SINGLE) {

			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(mImageSavePath)));
			intent.putExtra("return-data", true);
			startActivityForResult(intent, REQUEST_CAMERA);
		} else {
			Intent intent = new Intent(this, ImageSelectorActivity.class);
			intent.putExtra(ImageSelectorActivity.EXTRA_IMAGE_SELECTOR_COUNT, getIntent().getIntExtra(EXTRA_IMAGE_COUNT, 9));
			intent.putExtra(ImageSelectorActivity.EXTRA_IMAGE_SELECTOR_MODE, mode);
			intent.putExtra(ImageSelectorActivity.EXTRA_IMAGE_SELECTOR_SHOW_CAMERA, true);
			intent.putExtra(ImageSelectorActivity.EXTRA_IMAGE_SELECTOR_CROP_SHAPE, shape == IMAGE_CROP_SHAPE_SQUARE ? ImageSelectorActivity.IMAGE_SELECTOR_CROP_SHAPE_SQUARE : ImageSelectorActivity.IMAGE_SELECTOR_CROP_SHAPE_CIRCLE);
			intent.putExtra(ImageSelectorActivity.EXTRA_IMAGE_SELECTOR_CROP_WIDTH_HEIGHT_SCALE,cropWHScale);
			startActivityForResult(intent, REQUEST_GALLERY);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != RESULT_OK) {
			RxBus.get().post(RX_EVENT_IMAGE,resultCode == RESULT_CANCELED ? new String[]{} : null);
			finish();
			return;
		}
		switch (requestCode) {
			case REQUEST_CAMERA:
				if (mode == IMAGE_PICKER_MODE_CAMERA_CROP) {
					Intent intent = new Intent(getContext(), ImageCropperActivity.class);
					intent.putExtra(ImageCropperActivity.EXTRA_IMAGE_PATH, mImageSavePath);
					intent.putExtra(ImageCropperActivity.EXTRA_IMAGE_CROP_SHAPE, shape == IMAGE_CROP_SHAPE_SQUARE ? ImageSelectorActivity.IMAGE_SELECTOR_CROP_SHAPE_SQUARE : ImageSelectorActivity.IMAGE_SELECTOR_CROP_SHAPE_CIRCLE);
					intent.putExtra(ImageCropperActivity.EXTRA_IMAGE_CROP_WIDTH_HEIGHT_SCALE,cropWHScale);
					startActivityForResult(intent, REQUEST_CROPPER_IMAGE);
				} else {
					RxBus.get().post(RX_EVENT_IMAGE,new String[]{mImageSavePath});
					finish();
				}
				break;
			case REQUEST_GALLERY:
				if (mode == IMAGE_PICKER_MODE_GALLERY_MULTI) {
					RxBus.get().post(RX_EVENT_IMAGE,data.getStringArrayListExtra(ImageSelectorActivity.EXTRA_RESULT_IMAGE_SELECTED_PATH).toArray(new String[]{}));
				} else {
					RxBus.get().post(RX_EVENT_IMAGE,new String[]{data.getStringExtra(ImageSelectorActivity.EXTRA_RESULT_IMAGE_SELECTED_PATH)});
				}
				finish();
				break;
			case REQUEST_CROPPER_IMAGE:
				String path = data.getStringExtra(ImageCropperActivity.EXTRA_IMAGE_PATH);
				RxBus.get().post(RX_EVENT_IMAGE,new String[]{path});
				finish();
				break;
		}
	}
}
