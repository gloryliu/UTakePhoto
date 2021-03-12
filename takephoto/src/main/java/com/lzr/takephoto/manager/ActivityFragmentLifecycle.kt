package com.lzr.takephoto.manager

import android.content.Intent
import java.util.*

/**
 * @author liuzhenrong
 * @date 3/9/21 11:29 AM
 * @desc
 */
class ActivityFragmentLifecycle : Lifecycle {

    private var lifecycleListeners = Collections.newSetFromMap(WeakHashMap<LifecycleListener, Boolean>())
    private var isCreated = false

    override fun addListener(listener: LifecycleListener) {
        lifecycleListeners.add(listener)
        if (isCreated) {
            listener.onCreate()
        }
    }

    override fun removeListener(listener: LifecycleListener) {
        lifecycleListeners.remove(listener)
    }

    fun onCreate() {
        isCreated = true
        getSnapshot(lifecycleListeners).forEach {
            it.onCreate()
            isCreated = false
        }
    }

    fun onDestroy() {
        getSnapshot(lifecycleListeners).forEach {
            it.onDestroy()
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        getSnapshot(lifecycleListeners).forEach {
            it.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        getSnapshot(lifecycleListeners).forEach {
            it.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun <T> getSnapshot(other: Collection<T>): List<T> {
        val result: MutableList<T> = ArrayList(other.size)
        for (item in other) {
            if (item != null) {
                result.add(item)
            }
        }
        return result
    }
}