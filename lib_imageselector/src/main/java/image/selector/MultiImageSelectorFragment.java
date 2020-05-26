package image.selector;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.RecyclerView;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.lib_imageselector.R;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import image.FileProviderUtils;
import io.reactivex.functions.Consumer;

public class MultiImageSelectorFragment extends Fragment implements OnClickListener {
	private static final String TAG = "MultiImageSelectorF";

	private static final int REQUEST_CAMERA = 1;
	/**
	 * 不同loader定义
	 */
	private static final int LOADER_IMAGE = 0;
	private static final int LOADER_CATEGORY = 1;
	private static final int LOADER_IMAGE_AND_VIDEO = 2;
	ImageSelectorCallBack callBack;
	TextView mTitle;
	ImageView mArrow;
	TextView mBtnDone;
	/**
	 * 列间距
	 */
	private int columnsWidth;
	private int imageSelectedMode;
	private String resType;
	private int imageSelectedCount;
	private String imageSelectedPath;
	private boolean isShowCamera;
	/**
	 * 是否为指定目录
	 */
	private boolean isAssignPath;
	private String cameraSavePathBase;
	private String cameraSavePath;
	private RelativeLayout mToolbar;
	private ImageView mClose;
	private RecyclerView mRecycler;
	private ImagesAdapter mAdapter;

	private LinkedList<Directory> mImageDirectories = new LinkedList<>();
	private List<Image> mSelectedImages = new ArrayList<>();
	private List<Image> mImages = new ArrayList<>();

	//  顶部目录选择项
	private View mDirectory;

	private int currentDirectory = 0;
	private boolean isSelectedVideo=false;

	private LoaderCallbacks<Cursor> mLoaderCallback = new LoaderCallbacks<Cursor>() {

		String[] columns = { MediaStore.Files.FileColumns._ID,
				MediaStore.Files.FileColumns.DATA,
				MediaStore.Files.FileColumns.DATE_ADDED,
				MediaStore.Files.FileColumns.MEDIA_TYPE,
				MediaStore.Files.FileColumns.MIME_TYPE,
				MediaStore.Files.FileColumns.DISPLAY_NAME,
				MediaStore.Files.FileColumns.WIDTH,
				MediaStore.Files.FileColumns.HEIGHT,
				MediaStore.Files.FileColumns.SIZE,
				"duration"
		};
		String imgAndVideoSelection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
				+ MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
				+ " OR "
				+ MediaStore.Files.FileColumns.MEDIA_TYPE + "="
				+ MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
		String imgSelection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
				+ MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
		final String orderBy = MediaStore.Files.FileColumns.DATE_ADDED;
		Uri queryUri = MediaStore.Files.getContentUri("external");

		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			switch (id) {
				case LOADER_IMAGE:
					return new CursorLoader(getActivity(), queryUri, columns, imgSelection, null,  orderBy+ " DESC");
				case LOADER_CATEGORY:
					return new CursorLoader(getActivity(), queryUri, columns, imgSelection, null,  orderBy+ " DESC");
				case LOADER_IMAGE_AND_VIDEO:
					return new CursorLoader(getActivity(), queryUri, columns, imgAndVideoSelection, null,  orderBy+ " DESC");
				default:
					return null;
			}
		}


		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			mImages.clear();
			mImageDirectories.clear();

			if (isShowCamera) {
				Image camera = new Image();
				camera.setIsCamera(true);
				mImages.add(camera);
			}

			onGetData(data, mImages);

			mAdapter.changeData(mImages);

			Directory directory = new Directory();
			directory.setName(getString(R.string.image_selector_all));
			directory.setPath("none");
			directory.setImages(mImages);
			mImageDirectories.addFirst(directory);
		}

		void onGetData(Cursor data, List<Image> imgSaver) {
			if (data == null || data.getCount() == 0)
				return;
			data.moveToFirst();
			do {
				//  获取图片的ID
				int imgId = data.getInt(data.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID));
				String path = data.getString(data.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA));
//				if (!path.toLowerCase().endsWith(".jpeg") && !path.toLowerCase().endsWith(".jpg") && !path.toLowerCase().endsWith(".png")) {
//					continue;
//				}
				//  如果是指定目录，但是当前image的目录不在指定目录下，则不做任何操作
				if (isAssignPath && !path.startsWith(imageSelectedPath)) {
					continue;
				}
				Image image = new Image();
				image.setPath(path);
				image.setName(data.getString(data.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)));
				image.setUri(Uri.parse(queryUri.toString() + "/" + imgId));
				image.setId(imgId);
				image.setDate(data.getLong(data.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)));
//				image.setMediaType(data.getInt(data.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)));
//				image.setWidth(data.getInt(data.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)));
//				image.setHeight(data.getInt(data.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)));
//				image.setSize(data.getLong(data.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)));
//				image.setDuration(data.getLong(data.getColumnIndexOrThrow("duration")));
				boolean exist = false;
				File f = new File(image.getPath());
				if (f.exists()) {

					File parent = f.getParentFile();
					for (Directory directory : mImageDirectories) {
						if (directory.getPath().equals(parent.getAbsolutePath())) {
							directory.getImages().add(image);
							exist = true;
						}
					}

					if (!exist) {
						Directory directory = new Directory();
						directory.setName(parent.getName());
						directory.setPath(parent.getAbsolutePath());
						directory.setImages(new ArrayList<Image>());
						directory.getImages().add(image);
						mImageDirectories.add(directory);
					}
					imgSaver.add(image);
				}
			} while (data.moveToNext());
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {

		}
	};

	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof ImageSelectorCallBack) {
			this.callBack = (ImageSelectorCallBack) activity;
		}
	}

	@Nullable
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.image_selector_fragment, container, false);
	}

	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		this.imageSelectedMode = this.getArguments().getInt(ImageSelectorConstants.EXTRA_IMAGE_SELECT_MODE, ImageSelectorConstants.IMAGE_SELECT_MODE_MULTI);
//		this.resType = this.getArguments().getString(ImageSelectorConstants.EXTRA_IMAGE_SELECTOR_RES_TYPE);
		this.imageSelectedCount = this.getArguments()
				.getInt(ImageSelectorConstants.EXTRA_IMAGE_SELECT_COUNT, ImageSelectorConstants.IMAGE_SELECT_COUNT_DEFAULT);
		this.imageSelectedPath = this.getArguments().getString(ImageSelectorConstants.EXTRA_IMAGE_SELECT_PATH);
		this.isAssignPath = !TextUtils.isEmpty(imageSelectedPath);
		this.isShowCamera = this.getArguments().getBoolean(ImageSelectorConstants.EXTRA_IMAGE_SELECT_SHOW_CAMERA, true);
		this.cameraSavePathBase = this.getArguments().getString(ImageSelectorConstants.EXTRA_IMAGE_SELECT_CAMERA_SAVE_PATH);
		if (TextUtils.isEmpty(this.cameraSavePathBase)) {
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				this.cameraSavePathBase = Environment.getExternalStorageDirectory().getAbsolutePath();
			} else {
				this.cameraSavePathBase = Environment.getDataDirectory().getAbsolutePath();
			}
		}

		this.mToolbar = (RelativeLayout) view.findViewById(R.id.title);
		this.mClose = (ImageView) view.findViewById(R.id.album_select_close);
		mClose.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (callBack == null)
					return;
				callBack.onCancel();
			}
		});
		this.mBtnDone = (TextView) view.findViewById(R.id.image_selector_btn_ok);
		this.mTitle = (TextView) view.findViewById(R.id.image_selector_title);
		this.mArrow = (ImageView) view.findViewById(R.id.image_selector_arrow);
		this.mDirectory = view.findViewById(R.id.image_selector_directory);
		this.mRecycler = (RecyclerView) view.findViewById(R.id.image_selector_images);
		this.mRecycler.setLayoutManager(new GridLayoutManager(this.getActivity(), 3));
		this.mRecycler.setHasFixedSize(true);
		this.mRecycler.setAdapter(this.mAdapter = new ImagesAdapter(this.getActivity(), new ArrayList<Image>()));
		this.mDirectory.setOnClickListener(this);
		this.mBtnDone.setOnClickListener(this);
		this.mDirectory.setVisibility(this.isAssignPath ? View.GONE : View.VISIBLE);
	}

	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mRecycler.post(new Runnable() {
			@Override
			public void run() {
				columnsWidth = getActivity().getResources().getDimensionPixelOffset(R.dimen.size_5);
				mAdapter.setItemSide((int) (((float) mRecycler.getWidth() - (float) columnsWidth * 2) / 3));
				getActivity().getSupportLoaderManager().initLoader(LOADER_IMAGE, null, mLoaderCallback);
			}
		});
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == REQUEST_CAMERA && this.callBack != null) {
				//				/** 如果当前图片的度数不为0,则纠正图片的度数 */
				//				if (ImageTools.getExifOrientation(cameraSavePath) != 0) {
				//					ImageTools.rotateImage(cameraSavePath);
				//				}
				this.callBack.onImagesSelected(ImageSelectSource.Camera, new ArrayList<>(Arrays.asList(new String[]{this.cameraSavePath})));
			}
		} else if (this.callBack != null) {
			this.callBack.onCancel();
		}
	}

	/**
	 * 显示目录
	 */
	void showDirectories() {
		if (mImageDirectories.size() <= 1) {
			return;
		}
		final ListPopupWindow popupWindow = new ListPopupWindow(this.getActivity());
		ViewCompat.animate(mArrow).rotation(180).setDuration(0).start();
		popupWindow.setBackgroundDrawable(new ColorDrawable());
		popupWindow.setAdapter(new DirectoryAdapter(this.getActivity(), this.mImageDirectories));
		popupWindow.setWidth(ListPopupWindow.MATCH_PARENT);
		int height=getScreenHeight(getContext())-getResources().getDimensionPixelOffset(R.dimen.size_85);
		popupWindow.setHeight(height);
		popupWindow.setAnchorView(mToolbar);
		popupWindow.setModal(true);
		popupWindow.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				mTitle.setText(mImageDirectories.get(position).getName());
				mAdapter.changeData(mImageDirectories.get(position).getImages());
				currentDirectory = position;
				popupWindow.dismiss();
			}
		});
		popupWindow.show();

		popupWindow.getListView().setDividerHeight(getActivity().getResources().getDimensionPixelOffset(R.dimen.size_10));
		popupWindow.getListView().setDivider(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
		popupWindow.getListView().setBackgroundColor(Color.WHITE);
		popupWindow.setSelection(this.currentDirectory);
		popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
			@Override
			public void onDismiss() {
				ViewCompat.animate(mArrow).rotation(0).setDuration(0).start();
			}
		});

		mToolbar.setBackgroundColor(Color.WHITE);
	}

	public static int getScreenHeight(Context context) {
		DisplayMetrics dm = new DisplayMetrics();
		((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(dm);
		return dm.heightPixels;
	}

	boolean isSingleMode() {
		return this.imageSelectedMode == 1;
	}

	public void onClick(View v) {
		if (v.getId() == R.id.image_selector_directory) {
			this.showDirectories();
		} else if (v.getId() == R.id.image_selector_btn_ok) {
			if (this.callBack == null)
				return;

			ArrayList<String> paths = new ArrayList<>();
			for (Image image : mSelectedImages) {
				paths.add(image.getPath());
			}
			this.callBack.onImagesSelected(ImageSelectSource.Album, paths);
		}
	}


	public enum ImageSelectSource {
		Album(0x1),
		Camera(0x2);
		int type;

		ImageSelectSource(int type) {
			this.type = type;
		}

		public int getTypeId() {
			return type;
		}
	}

	public interface ImageSelectorCallBack {
		void onImagesSelected(ImageSelectSource selectSource, ArrayList<String> list);

		void onCancel();

		void onMediaSelected(int mediaType,ArrayList<String> pathList);
	}

	class ImagesAdapter extends RecyclerView.Adapter<ImagesAdapter.ImageHolder> {
		static final int TYPE_IMAGE = 1;
		static final int TYPE_CAMERA = 2;
		private Context context;
		private List<Image> images;

		private int itemSide = 0;

		ImagesAdapter(Context context, List<Image> images) {
			this.context = context;
			this.images = images;
		}

		public ImageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			switch (viewType) {
				case TYPE_IMAGE:
					return new ImageHolder(LayoutInflater.from(context).inflate(R.layout.image_selector_item_image, parent, false));
				case TYPE_CAMERA:
					return new CameraHolder(LayoutInflater.from(context).inflate(R.layout.image_selector_item_camera, parent, false));
			}
			return null;
		}

		public int getItemCount() {
			return images.size();
		}

		public void onBindViewHolder(final ImageHolder holder, final int position) {

			if (itemSide != 0) {
				if (holder.itemView.getLayoutParams() == null) {
					GridLayoutManager.LayoutParams params = new GridLayoutManager.LayoutParams(itemSide, itemSide);
					holder.itemView.setLayoutParams(params);
				} else {
					holder.itemView.getLayoutParams().width = itemSide;
					holder.itemView.getLayoutParams().height = itemSide;
				}
			}
			// 调用系统相机
			if (holder.isCamera) {
				holder.itemView.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {

//						if (mSelectedImages.size() >= imageSelectedCount) {
//							OnceToast.showToast(context,context.getString(R.string.image_selector_msg_amount_limit, imageSelectedCount));
//							return;
//						}

						RxPermissions rxPermissions = new RxPermissions(MultiImageSelectorFragment.this);
						rxPermissions.request(Manifest.permission.CAMERA)
								.subscribe(new Consumer<Boolean>() {
									@Override
									public void accept(Boolean aBoolean) throws Exception {
										if (aBoolean){
											Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
											cameraSavePath = cameraSavePathBase + "/" + System.currentTimeMillis() + ".jpg";

											Uri uri=null;
											if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
												uri = FileProvider.getUriForFile(getContext(), FileProviderUtils.getFileProviderName(), new File(cameraSavePath));
											} else {
												uri = Uri.fromFile(new File(cameraSavePath));
											}

											intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
											intent.putExtra("return-data", true);
											startActivityForResult(intent, REQUEST_CAMERA);
										}
									}
								});

					}
				});
			} else {
				final Image image = images.get(position);
				Glide.with(context)
						.asBitmap()
						.load(image.getUri())
						.placeholder(new ColorDrawable(Color.parseColor("#555555")))
						.error(R.drawable.image_selector_load_error)
						.centerCrop()
						.into(holder.img);

				TextPaint tp = holder.checkText.getPaint();
				tp.setFakeBoldText(true);
				holder.checkText.setText("");
				for (int i = 0; i < mSelectedImages.size(); i++) {
					if (mSelectedImages.get(i).equals(image)){
						image.setSelected(mSelectedImages.get(i).isSelected());
						holder.checkText.setText(i+1+"");
					}
				}
				final boolean isSelected = image.isSelected();

				holder.checkText.setVisibility(isSingleMode()?View.GONE:View.VISIBLE);
				holder.checkText.setSelected(isSelected);
				holder.videoDisplayIcon.setVisibility(View.INVISIBLE);
				holder.videoCover.setVisibility(View.GONE);

				holder.img.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {

						if (isSingleMode() && callBack != null) {
							callBack.onImagesSelected(ImageSelectSource.Album, new ArrayList<>(Arrays.asList(new String[]{image.getPath()})));
							return;
						}

						// 如果只有一张图片，那么点击直接返回图片
						if (imageSelectedCount==1&&callBack!=null){
							callBack.onImagesSelected(ImageSelectSource.Album, new ArrayList<>(Arrays.asList(new String[]{image.getPath()})));
							return;
						}

						if (!image.isSelected() && mSelectedImages.size() >= imageSelectedCount) {
							Toast.makeText(context, context.getString(R.string.image_selector_msg_amount_limit, imageSelectedCount), Toast.LENGTH_SHORT).show();
							return;
						}

						image.setSelected(!isSelected);
						mAdapter.notifyDataSetChanged();
						if (image.isSelected()) {
							mSelectedImages.add(image);
						} else {
							mSelectedImages.remove(image);
						}

						if (mSelectedImages.size() > 0) {
							mBtnDone.setEnabled(true);
							mBtnDone.setTextColor(Color.parseColor("#333333"));
						} else {
							mBtnDone.setEnabled(false);
							mBtnDone.setTextColor(Color.parseColor("#999999"));
						}
					}
				});
			}
		}

		public int getItemViewType(int position) {
			return (images.get(position)).isCamera() ? TYPE_CAMERA : TYPE_IMAGE;
		}

		public void changeData(List<Image> images) {
			clear();
			this.images.addAll(images);
			notifyDataSetChanged();
//			notifyItemRangeChanged(0, images.size());
		}

		public void insert(Image image) {
			this.images.add(image);
			this.notifyItemInserted(this.images.size() - 1);
		}

		public void clear() {
			this.images.clear();
		}

		public void setItemSide(int itemSide) {
			this.itemSide = itemSide;
		}

		class CameraHolder extends ImageHolder {
			public CameraHolder(View itemView) {
				super(itemView);
				this.isCamera = true;
			}
		}

		class ImageHolder extends android.support.v7.widget.RecyclerView.ViewHolder {
			boolean isCamera = false;
			ImageView img;
			ImageView videoDisplayIcon;
			TextView checkText;
			ImageView videoCover;

			public ImageHolder(View itemView) {
				super(itemView);
				this.img = (ImageView) itemView.findViewById(R.id.image_selector_image);
				this.checkText = (TextView) itemView.findViewById(R.id.image_selector_text);
				this.videoDisplayIcon = (ImageView)itemView.findViewById(R.id.video_display_icon);
				this.videoCover = (ImageView)itemView.findViewById(R.id.video_cover);
			}
		}
	}

	class DirectoryAdapter extends BaseAdapter {
		private List<Directory> directories;
		private Context context;

		DirectoryAdapter(Context context, List<Directory> directories) {
			this.context = context;
			this.directories = directories;
		}

		public int getCount() {
			return this.directories.size();
		}

		public Object getItem(int position) {
			return this.directories.get(position);
		}

		public long getItemId(int position) {
			return (long) position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder mHolder;
			if (convertView == null) {
				convertView = LayoutInflater.from(this.getContext()).inflate(R.layout.image_selector_directory_item, parent, false);
				mHolder = new ViewHolder(convertView);
				convertView.setTag(mHolder);
			} else {
				mHolder = (ViewHolder) convertView.getTag();
			}

			Directory directory = (Directory) getItem(position);
			mHolder.txtDirect.setText(directory.getName());
			mHolder.txtDirectNum.setText(String.format("%d张图片", directory.getImages().size()));
			if (directory.getImages().size() > 1 && isShowCamera) {
				Glide.with(context)
						.asBitmap()
						.load(directory.getImages().get(1).getUri())
						.error(R.drawable.image_selector_load_error)
						.into(mHolder.imgThumb);
			} else if (directory.getImages().size() > 0 && !isShowCamera) {
				Glide.with(context)
						.asBitmap()
						.load(directory.getImages().get(0).getUri())
						.error(R.drawable.image_selector_load_error)
						.into(mHolder.imgThumb);
			}else {
				Glide.with(context)
						.asBitmap()
						.load(directory.getImages().get(0).getUri())
						.error(R.drawable.image_selector_load_error)
						.into(mHolder.imgThumb);
			}
			return convertView;
		}

		public Context getContext() {
			return this.context;
		}

		class ViewHolder {
			ImageView imgThumb;
			TextView txtDirect;
			TextView txtDirectNum;

			ViewHolder(View itemView) {
				this.imgThumb = (ImageView) itemView.findViewById(R.id.image_selector_dir_item_thumb);
				this.txtDirect = (TextView) itemView.findViewById(R.id.image_selector_dir_item_dir);
				this.txtDirectNum = (TextView) itemView.findViewById(R.id.image_selector_dir_item_dir_num);
			}
		}
	}
}

