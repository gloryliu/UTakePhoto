package com.lzr.takephoto.manager

import android.app.Fragment
import android.content.Intent
import android.os.Bundle

/**
 * @author liuzhenrong
 * @date 3/9/21 11:35 AM
 * @desc
 */
class RequestManagerFragment : Fragment() {

    private var activityFragmentLifecycle:ActivityFragmentLifecycle = ActivityFragmentLifecycle()
    private var takePhotoManager:TakePhotoManager? = null
    private var uTakePhoto:UTakePhoto = UTakePhoto(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityFragmentLifecycle.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        activityFragmentLifecycle.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        activityFragmentLifecycle.onActivityResult(requestCode,resultCode,data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        activityFragmentLifecycle.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun getTakePhoto():UTakePhoto {
        return uTakePhoto
    }

    fun getTakePhotoLifecycle():ActivityFragmentLifecycle {
        return activityFragmentLifecycle
    }

    fun getTakePhotoManager():TakePhotoManager? {
        return takePhotoManager
    }

    fun setTakePhotoManager(takePhotoManager:TakePhotoManager) {
        this.takePhotoManager = takePhotoManager
    }
}