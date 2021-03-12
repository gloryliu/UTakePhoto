package com.lzr.takephoto.manager

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

/**
 * @author liuzhenrong
 * @date 3/9/21 2:11 PM
 * @desc
 */
object RequestManagerRetriever : Handler.Callback {

    private const val FRAGMENT_TAG = "com.huochat.utakephoto.manager.RequestManagerFragment"
    private const val ID_REMOVE_FRAGMENT_MANAGER = 1
    private const val ID_REMOVE_SUPPORT_FRAGMENT_MANAGER = 2
    private var mHandler: Handler

    private var pendingSupportFragments = HashMap<FragmentManager, SupportRequestManagerFragment>()
    private var pendingFragments = HashMap<android.app.FragmentManager, RequestManagerFragment>()

    init {
        mHandler = Handler(Looper.getMainLooper(), this)
    }

    override fun handleMessage(msg: Message): Boolean {
        var handled = true
        when(msg.what) {
            ID_REMOVE_FRAGMENT_MANAGER -> {
                (msg.obj as android.app.FragmentManager)?.let {
                    pendingFragments.remove(it)
                }
            }
            ID_REMOVE_SUPPORT_FRAGMENT_MANAGER -> {
                (msg.obj as FragmentManager)?.let {
                    pendingSupportFragments.remove(it)
                }
            }
            else -> {
                handled = false
            }
        }
        return handled
    }


    fun get(fragmentActivity: FragmentActivity):TakePhotoManager {
        return supportFragmentGet(fragmentActivity, fragmentActivity.supportFragmentManager)
    }

    fun get(fragment: Fragment):TakePhotoManager {
        return supportFragmentGet(fragment.activity!!, fragment.childFragmentManager)
    }

    fun get(activity: Activity):TakePhotoManager {
        return fragmentGet(activity, activity.fragmentManager)
    }

    fun get(fragment: android.app.Fragment):TakePhotoManager {
        return fragmentGet(fragment.activity,fragment.childFragmentManager)
    }

    private fun supportFragmentGet(context: Context, fm: FragmentManager):TakePhotoManager {
        var supportRequestManagerFragment = getSupportRequestManagerFragment(fm)
        var takePhotoManager = supportRequestManagerFragment.getTakePhotoManager()
        if (takePhotoManager == null) {
            takePhotoManager = DEFAULT_FACTORY.build(
                    supportRequestManagerFragment.getTakePhoto(),
                    supportRequestManagerFragment.getTakePhotoLifecycle(),
                    context)
            supportRequestManagerFragment.setTakePhotoManager(takePhotoManager)
        }
        return takePhotoManager
    }


    private fun getSupportRequestManagerFragment(fm: FragmentManager):SupportRequestManagerFragment {
        var current = fm.findFragmentByTag(FRAGMENT_TAG) as? SupportRequestManagerFragment
        if (current == null) {
            current = pendingSupportFragments.get(fm)
            if (current == null) {
                current = SupportRequestManagerFragment()
                pendingSupportFragments.put(fm, current)
                fm.beginTransaction().add(current, FRAGMENT_TAG).commitAllowingStateLoss()
                mHandler.obtainMessage(ID_REMOVE_SUPPORT_FRAGMENT_MANAGER, fm).sendToTarget()
            }
        }
        return current
    }

    private fun fragmentGet(context: Context, fm: android.app.FragmentManager):TakePhotoManager {
        var requestManagerFragment = getRequestManagerFragment(fm)
        var takePhotoManager = requestManagerFragment.getTakePhotoManager()
        if (takePhotoManager == null) {
            takePhotoManager = DEFAULT_FACTORY.build(
                    requestManagerFragment.getTakePhoto(),
                    requestManagerFragment.getTakePhotoLifecycle(),
                    context)
            requestManagerFragment.setTakePhotoManager(takePhotoManager)
        }
        return takePhotoManager
    }

    private fun getRequestManagerFragment(fm: android.app.FragmentManager):RequestManagerFragment {
        var current = fm.findFragmentByTag(FRAGMENT_TAG) as? RequestManagerFragment
        if (current == null) {
            current = pendingFragments.get(fm)
            if (current == null) {
                current = RequestManagerFragment()
                pendingFragments.put(fm, current)
                fm.beginTransaction().add(current, FRAGMENT_TAG).commitAllowingStateLoss()
                mHandler.obtainMessage(ID_REMOVE_FRAGMENT_MANAGER, fm).sendToTarget()
            }
        }
        return current
    }


    interface RequestManagerFactory{
        fun build(uTakePhoto: UTakePhoto,
                  lifecycle: Lifecycle,
                  mContext: Context): TakePhotoManager
    }

    private var DEFAULT_FACTORY = object : RequestManagerFactory{
        override fun build(uTakePhoto: UTakePhoto, lifecycle: Lifecycle, mContext: Context): TakePhotoManager {
            return TakePhotoManager(uTakePhoto, lifecycle, mContext)
        }

    }
}