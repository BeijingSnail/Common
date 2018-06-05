package com.example.commonlib.photo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;

import static android.app.Activity.RESULT_OK;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

/**
 * 调用系统相机 相册 剪裁
 */

public class PhotoUtils {
    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_GALLERY = 101;
    private static final int REQUEST_CROP = 102;

    private Activity mActivity;
    private final String applicationId;
    private File mCameraFile, mGalleryFile, mCropFile;
    private OnPhotoResultListener mOnPhotoResultListener;
    private boolean isCrop;

    public void setIsCrop(boolean flag) {
        this.isCrop = flag;
    }

    /**
     * @param activity
     * @param isCrop  是否进行剪裁
     * 构造方法
     */
    public PhotoUtils(Activity activity, boolean isCrop) {
        this.mActivity = activity;
        this.isCrop = isCrop;
        applicationId = mActivity.getApplicationInfo().packageName;
        String path = Environment.getExternalStorageDirectory() + "/Android/data/" + applicationId + "/files/";
        mCameraFile = new File(path, "IMAGE_FILE_NAME.jpg");//照相机的File对象
        //判断文件夹是否存在 不存在就创建一个 否则找不到该图片
        if (!mCameraFile.getParentFile().exists()) mCameraFile.getParentFile().mkdirs();
        mGalleryFile = new File(path, "IMAGE_GALLERY_NAME.jpg");//相册的File对象
        if (!mGalleryFile.getParentFile().exists()) mGalleryFile.getParentFile().mkdirs();
        mCropFile = new File(path, "PHOTO_FILE_NAME.jpg");//裁剪后的File对象
        if (!mCropFile.getParentFile().exists()) mCropFile.getParentFile().mkdirs();
    }

    /**
     * 拍照
     */
    public void takePicture() {
        Intent intentFromCapture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {//7.0及以上
            Uri uriForFile = FileProvider.getUriForFile(mActivity, applicationId + ".provider", mCameraFile);
            intentFromCapture.putExtra(MediaStore.EXTRA_OUTPUT, uriForFile);
            intentFromCapture.addFlags(FLAG_GRANT_READ_URI_PERMISSION);
            intentFromCapture.addFlags(FLAG_GRANT_WRITE_URI_PERMISSION);
//  mCameraFile -> /storage/emulated/0/Android/data/applicationId/files/IMAGE_FILE_NAME.jpg
        } else {
            intentFromCapture.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mCameraFile));
        }
        mActivity.startActivityForResult(intentFromCapture, REQUEST_CAMERA);
    }

    /**
     * 相册选择图片
     */
    public void selectPicture() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {//如果大于等于7.0使用FileProvider
            Uri uriForFile = FileProvider.getUriForFile
                    (mActivity, applicationId + ".provider", mGalleryFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uriForFile);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mGalleryFile));
        }
        mActivity.startActivityForResult(intent, REQUEST_GALLERY);
    }


    /**
     * 处理返回的结果
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CAMERA:
                    Uri inputUri;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        inputUri = FileProvider.getUriForFile(mActivity, applicationId + ".provider", mCameraFile);//通过FileProvider创建一个content类型的Uri
// inputUri -> content://applicationId.provider/sdcard_files/Android/data/applicationId /files/IMAGE_FILE_NAME.jpg
                    } else {
                        inputUri = Uri.fromFile(mCameraFile);
                    }
                    startPhotoZoom(inputUri);//设置输入类型
                    break;

                case REQUEST_CROP:
                    Uri cropFileUri;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        cropFileUri = FileProvider.getUriForFile(mActivity, applicationId + ".provider", mCropFile);//通过FileProvider创建一个content类型的Uri
                    } else {
                        cropFileUri = Uri.fromFile(mCropFile);
                    }

                    if (mOnPhotoResultListener != null) {
                        mOnPhotoResultListener.onPhotoResult(cropFileUri, mCropFile);
                    }
                    break;

                case REQUEST_GALLERY:
                    Uri dataUri = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        File imgUri = new File(GetImagePath.getPath(mActivity, data.getData()));
                        dataUri = FileProvider.getUriForFile(mActivity, applicationId + ".provider", imgUri);
                        // Uri dataUri = getImageContentUri(data.getData());
                    } else {
                        if (data != null) {
                            // 得到图片的全路径
                            dataUri = data.getData();
                        }
                    }
                    startPhotoZoom(dataUri);//设置输入类型
                    break;
            }
        }
    }

    public void startPhotoZoom(Uri inputUri) {
        if (inputUri == null) {
            Log.e("error", "The uri is not exist.");
            return;
        }
        Intent intent = new Intent("com.android.camera.action.CROP");
        Uri outPutUri = Uri.fromFile(mCropFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outPutUri);

        Uri data = inputUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
// outPutUri -> file:///storage/emulated/0/Android/data/applicationId/files/PHOTO_FILE_NAME.jpg
            intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(FLAG_GRANT_WRITE_URI_PERMISSION);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                String url = GetImagePath.getPath(mActivity, inputUri);//这个方法是处理4.4以上图片返回的Uri对象不同的处理方法
                Log.d("zzq", "4.4url: " + url);
                data = Uri.fromFile(new File(url));
            }
        }
        intent.setDataAndType(data, "image/*");

        // 设置裁剪
        intent.putExtra("crop", "true");
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 500);
        intent.putExtra("outputY", 500);
        intent.putExtra("return-data", false);
        intent.putExtra("noFaceDetection", false);//去除默认的人脸识别，否则和剪裁匡重叠
        intent.putExtra("outputFormat", "JPEG");

        if (isCrop) {
            mActivity.startActivityForResult(intent, REQUEST_CROP);//这里就将裁剪后的图片的Uri返回了
        } else {
            if (mOnPhotoResultListener != null) {
                mOnPhotoResultListener.onPhotoResult(data, new File(data.getPath()));
            }
        }
    }


    /**
     * 回掉监听
     */
    public interface OnPhotoResultListener {
        void onPhotoResult(Uri uri, File file);
    }

    public void setOnPhotoResultListener(OnPhotoResultListener listener) {
        this.mOnPhotoResultListener = listener;
    }

}
