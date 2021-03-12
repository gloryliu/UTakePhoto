package com.lzr.takephoto.manager

import android.content.Intent

/**
 * @author liuzhenrong
 * @date 3/9/21 9:51 AM
 * @desc 感知Activity和Fragment的生命周期变化
 */
interface LifecycleListener {

    fun onCreate()

    fun onDestroy()

    fun onActivityResult(requestCode:Int, resultCode:Int, data:Intent?)

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)

}