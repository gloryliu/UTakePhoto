package com.lzr.takephoto.manager

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
import android.util.Log
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.lzr.takephoto.crop.Crop
import com.lzr.takephoto.utils.PermissionUtils
import com.lzr.takephoto.utils.TConstant
import com.lzr.takephoto.utils.TUriParse
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


/**
 * @author liuzhenrong
 * @date 3/9/21 10:21 AM
 * @desc 选择图片管理对象
 */
class TakePhotoManager(
        private var uTakePhoto: UTakePhoto,
        private var lifecycle: Lifecycle,
        private var mContext: Context
):LifecycleListener{

    /**
     * 权限
     */
    private val PERMISSION_CAMERAS = arrayOf(
            Manifest.permission.CAMERA,
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    )
    private val PERMISSION_STORAGE = arrayOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    )


    /**
     * 类型
     */
    private var takeType = 0

    /**
     * 是否裁剪
     */
    private var isCrop = false

    /**
     * 是否初始化
     */
    private var isInit = false

    /**
     * 结果回调接口
     */
    private var resultCallback:TakePhotoResult? = null

    /**
     * 图片的绝对路径
     */
    private lateinit var currentPhotoPath: String
    private lateinit var currentCropPhotoPath: String

    init {
        lifecycle.addListener(this)
    }

    enum class Mode{
        CAMERA,
        ALBUM
    }

    /**
     * 获取图片
     */
    fun photo(mode:Mode):TakePhotoManager {
        when(mode) {
            Mode.CAMERA -> {
                this.takeType = TConstant.TYPE_TAKE_PHOTO
            }
            Mode.ALBUM -> {
                this.takeType = TConstant.TYPE_SELECT_IMAGE
            }
        }
        return this
    }

    /**
     * 打开相机
     */
    private fun openCamera():TakePhotoManager {
        this.takeType = TConstant.TYPE_TAKE_PHOTO
        return this
    }

    /**
     * 打开相册
     */
    private fun openAlbum():TakePhotoManager {
        this.takeType = TConstant.TYPE_SELECT_IMAGE
        return this
    }

    /**
     * 是否裁剪图片
     */
    fun crop():TakePhotoManager {
        this.isCrop = true
        return this
    }

    /**
     * 请求图片
     */
    fun request(resultCallback: TakePhotoResult) {
        this.resultCallback = resultCallback
        if (!isInit) return
        checkPermission()
    }

    /**
     * 检查权限
     */
    private fun checkPermission() {
        if (resultCallback==null) return
        if (takeType == 0) {
            resultCallback?.takeFailure(Exception("应该先调用openCamera或者openAlbum"))
            return
        }
        if (uTakePhoto.getFragment() != null) {
            fragmentPermissionCheck(uTakePhoto.getFragment()!!)
        } else if (uTakePhoto.getSupportFragment() != null) {
            supportFragmentPermissionCheck(uTakePhoto.getSupportFragment()!!)
        }
    }

    private fun fragmentPermissionCheck(fragment: android.app.Fragment) {
        val permissions = if (takeType == TConstant.TYPE_TAKE_PHOTO) PERMISSION_CAMERAS else PERMISSION_STORAGE
        if (PermissionUtils.hasSelfPermissions(fragment.activity, *permissions)) {
            permissionGranted()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                fragment.requestPermissions(permissions, TConstant.PERMISSION_REQUEST_CODE)
            } else {
                resultCallback?.takeFailure(Exception("权限是否在manifest中注册"))
            }
        }
    }

    private fun supportFragmentPermissionCheck(fragment: Fragment) {
        val permissions = if (takeType == TConstant.TYPE_TAKE_PHOTO) PERMISSION_CAMERAS else PERMISSION_STORAGE
        if (PermissionUtils.hasSelfPermissions(fragment.activity, *permissions)) {
            permissionGranted()
        } else {
            fragment.requestPermissions(permissions, TConstant.PERMISSION_REQUEST_CODE)
        }
    }


    private fun permissionGranted() {
        if (takeType == TConstant.TYPE_TAKE_PHOTO) {
            goOpenCamera()
        } else if (takeType == TConstant.TYPE_SELECT_IMAGE){
            goAlbum()
        }  else {
            resultCallback?.takeFailure(Exception("应该选择拍照或者图库"))
        }
    }


    /**
     * 打开相机
     */
    private lateinit var cameraOutPhotoUri: Uri
    private fun goOpenCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(mContext.packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    Log.d("LIU", "创建文件出错了")
                    resultCallback?.takeFailure(Exception("创建文件出错了"))
                    ex.printStackTrace()
                    null
                }
                photoFile?.also {
                    cameraOutPhotoUri = FileProvider.getUriForFile(
                            mContext,
                            "${mContext.packageName}.takephotoprovider",
                            it
                    )
                    takePictureIntent.putExtra(MediaStore.Images.Media.ORIENTATION, 0)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraOutPhotoUri)
                    startActivityForResult(takePictureIntent, TConstant.TYPE_TAKE_PHOTO)
                }
            }
        }
    }


    /**
     * 创建相机输出文件
     */
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    /**
     * 创建裁剪输出文件
     */
    @Throws(IOException::class)
    private fun createCropImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
                "JPEG_crop_${timeStamp}_",
                ".jpg",
                storageDir
        ).apply {
            currentCropPhotoPath = absolutePath
        }
    }


    /**
     * 打开相册
     */
    private fun goAlbum() {
        Intent(Intent.ACTION_PICK, EXTERNAL_CONTENT_URI).also { album ->
            album.setType("image/*")
            startActivityForResult(album, TConstant.TYPE_SELECT_IMAGE)
        }
    }


    /**
     * 打开相机或者相册
     */
    private fun startActivityForResult(intent: Intent, requestCode: Int) {
        if (uTakePhoto.getSupportFragment() != null) {
            uTakePhoto.getSupportFragment()!!.startActivityForResult(intent, requestCode)
        } else if (uTakePhoto.getFragment() != null) {
            uTakePhoto.getFragment()!!.startActivityForResult(intent, requestCode)
        }
    }

    /**
     * 裁剪图片
     */
    private lateinit var cropOutPhotoUri: Uri
    private fun beginCrop(source: Uri?) {
        val photoFile: File? = try {
            createCropImageFile()
        } catch (ex: IOException) {
            Log.d("LIU", "创建文件出错了")
            resultCallback?.takeFailure(Exception("创建文件出错了"))
            ex.printStackTrace()
            null
        }
        photoFile?.also {
            cropOutPhotoUri = FileProvider.getUriForFile(
                    mContext,
                    "${mContext.packageName}.takephotoprovider",
                    it
            )
            if (uTakePhoto.getFragment() != null) {
                Crop.of(source, cropOutPhotoUri).asSquare().start(uTakePhoto.getFragment()?.activity, uTakePhoto.getFragment())
            } else if (uTakePhoto.getSupportFragment() != null) {
                Crop.of(source, cropOutPhotoUri).asSquare().start(uTakePhoto.getSupportFragment()?.context, uTakePhoto.getSupportFragment())
            }
        }
    }

    /**
     * 处理裁剪结果
     */
    private fun handleCrop(resultCode: Int, result: Intent?) {
        if (resultCode == RESULT_OK) {
            var uri = Crop.getOutput(result)
            ///currentPhotoPath = TUriParse.getFilePathWithUri(Crop.getOutput(result), mContext)
            Log.e("LIU handleCrop=", currentCropPhotoPath)
            resultCallback?.takeSuccess(currentCropPhotoPath)
        } else if (resultCode == Crop.RESULT_ERROR) {
            Log.e("LIU handleCrop=", "失败了")
            resultCallback?.takeFailure(Exception("裁剪图片失败"))
        }
    }


    override fun onCreate() {
        isInit = true
        checkPermission()
    }

    override fun onDestroy() {
        isCrop = false
        Log.e("LIU", "onDestroy")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            TConstant.TYPE_TAKE_PHOTO -> {
                if (resultCode == RESULT_OK) {
                    if (isCrop) {
                        beginCrop(cameraOutPhotoUri)
                    } else {
                        resultCallback?.takeSuccess(currentPhotoPath)
                    }
                } else {
                    resultCallback?.takeCancel()
                }
            }
            TConstant.TYPE_SELECT_IMAGE -> {
                if (resultCode == RESULT_OK) {
                    if (isCrop) {
                        beginCrop(data?.data)
                    } else {
                        currentPhotoPath = TUriParse.getFilePathWithUri(data?.data, mContext)
                        resultCallback?.takeSuccess(currentPhotoPath)
                    }
                } else {
                    resultCallback?.takeCancel()
                }
            }

            Crop.REQUEST_CROP -> {
                handleCrop(resultCode, data)
            }
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        if (requestCode == TConstant.PERMISSION_REQUEST_CODE) {
            var deniedList = ArrayList<String>()
            var neverAskAgainList = ArrayList<String>()
            permissions.forEachIndexed { index, s ->
                if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                    if (uTakePhoto.getFragment() != null) {
                        if (!PermissionUtils.shouldShowRequestPermissionRationale(
                                        uTakePhoto.getSupportFragment(),
                                        permissions[index]
                                )
                        ) {
                            neverAskAgainList.add(permissions[index])
                        } else {
                            deniedList.add(permissions[index])
                        }
                    } else if (uTakePhoto.getSupportFragment() != null) {
                        if (!PermissionUtils.shouldShowRequestPermissionRationale(
                                        uTakePhoto.getSupportFragment(),
                                        permissions[index]
                                )
                        ) {
                            neverAskAgainList.add(permissions[index])
                        } else {
                            deniedList.add(permissions[index])
                        }
                    }
                }
            }
            if (deniedList.isEmpty() && neverAskAgainList.isEmpty()) {
                permissionGranted()
            } else {
                if (!deniedList.isEmpty()) {
                    resultCallback?.takeFailure(Exception("拒绝了权限"))
                }
                if (!neverAskAgainList.isEmpty()) {
                    resultCallback?.takeFailure(Exception("权限没有打开"))
                }
            }
        }
    }
}