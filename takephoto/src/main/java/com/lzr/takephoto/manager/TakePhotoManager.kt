package com.lzr.takephoto.manager

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
import android.util.Log
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.lzr.takephoto.utils.PermissionUtils
import com.lzr.takephoto.utils.TUriParse
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
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
     * 拍照ResultCode
     */
    private val TAKE_PHOTO_RESULT = 1 shl 2

    /**
     * 选择相册ResultCode
     */
    private val DIRECTORY_PICTURES_RESULT = 1 shl 3

    /**
     * 裁剪ResultCode
     */
    private val PHOTO_WITCH_CROP_RESULT = 1 shl 5

    /**
     * 类型拍照
     */
    private val TYPE_TAKE_PHOTO = 1 shl 7

    /**
     * 类型从相册选择
     */
    private val TYPE_SELECT_IMAGE = 1 shl 8

    /**
     * 请求权限requestCode
     */
    private val PERMISSION_REQUEST_CODE = 1 shl 9

    /**
     * 类型
     */
    private var takeType = 0

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

    init {
        lifecycle.addListener(this)
    }

    /**
     * 打开相机
     */
    fun openCamera():TakePhotoManager {
        this.takeType = TYPE_TAKE_PHOTO
        return this
    }

    /**
     * 打开相册
     */
    fun openAlbum():TakePhotoManager{
        this.takeType = TYPE_SELECT_IMAGE
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
        val permissions = if (takeType == TYPE_TAKE_PHOTO) PERMISSION_CAMERAS else PERMISSION_STORAGE
        if (PermissionUtils.hasSelfPermissions(fragment.activity, *permissions)) {
            permissionGranted()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                fragment.requestPermissions(permissions, PERMISSION_REQUEST_CODE)
            } else {
                resultCallback?.takeFailure(Exception("权限是否在manifest中注册"))
            }
        }
    }

    private fun supportFragmentPermissionCheck(fragment: Fragment) {
        val permissions = if (takeType == TYPE_TAKE_PHOTO) PERMISSION_CAMERAS else PERMISSION_STORAGE
        if (PermissionUtils.hasSelfPermissions(fragment.activity, *permissions)) {
            permissionGranted()
        } else {
            fragment.requestPermissions(permissions, PERMISSION_REQUEST_CODE)
        }
    }


    private fun permissionGranted() {
        if (takeType == TYPE_TAKE_PHOTO) {
            goOpenCamera()
        } else if (takeType == TYPE_SELECT_IMAGE){
            goAlbum()
        }  else {
            resultCallback?.takeFailure(Exception("应该选择拍照或者图库"))
        }
    }

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
                    val photoURI: Uri = FileProvider.getUriForFile(
                        mContext,
                        "${mContext.packageName}.takephotoprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, TAKE_PHOTO_RESULT)
                }
            }
        }
    }


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

    @Throws(IOException::class)
    private fun copyImageFile(sourceFile:File) {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {

            var inPut:FileInputStream? = null
            var outPut:FileOutputStream? = null
            try {
                 inPut = FileInputStream(sourceFile)
                outPut = FileOutputStream(this)
                outPut?.channel.transferFrom(inPut?.channel, 0, inPut?.channel.size())
                currentPhotoPath = absolutePath
            } finally {
                inPut?.close()
                outPut?.close()
            }
        }
    }
    private fun goAlbum() {
        Intent(Intent.ACTION_PICK, EXTERNAL_CONTENT_URI).also { album ->
            album.setType("image/*")
            startActivityForResult(album, DIRECTORY_PICTURES_RESULT)
        }
    }


    private fun startActivityForResult(intent: Intent, requestCode: Int) {
        if (uTakePhoto.getSupportFragment() != null) {
            uTakePhoto.getSupportFragment()!!.startActivityForResult(intent, requestCode)
        } else if (uTakePhoto.getFragment() != null) {
            uTakePhoto.getFragment()!!.startActivityForResult(intent, requestCode)
        }
    }


    override fun onCreate() {
        isInit = true
        checkPermission()
    }

    override fun onDestroy() {
        TODO("Not yet implemented")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            TAKE_PHOTO_RESULT -> {
                if (resultCode == RESULT_OK) {
                    resultCallback?.takeSuccess(currentPhotoPath)
                } else {
                    resultCallback?.takeCancel()
                }
            }
            DIRECTORY_PICTURES_RESULT -> {
                if (resultCode == RESULT_OK) {
//                    data?.data?.let {
//                        mContext.contentResolver.query(it,null,null,null,null)?.let { cursor ->
//                            if (cursor.moveToFirst()) {
//                                currentPhotoPath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
//                            }
//                        }
//                    }
//                    try {
//                        copyImageFile(File(currentPhotoPath))
//                        resultCallback?.takeSuccess(currentPhotoPath)
//                    } catch (ex:Exception) {
//                        ex.printStackTrace()
//                    }
                    currentPhotoPath = TUriParse.getFilePathWithUri(data?.data, mContext)
                    resultCallback?.takeSuccess(currentPhotoPath)
                } else {
                    resultCallback?.takeCancel()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
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