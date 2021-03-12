package com.lzr.takephoto.manager

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

/**
 * @author liuzhenrong
 * @date 3/9/21 10:10 AM
 * @desc
 */
class UTakePhoto {

    private var fragemt:android.app.Fragment? = null
    private var suppertFragment:Fragment? = null

    constructor(fragemt:android.app.Fragment) {
        this.fragemt = fragemt
    }

    constructor(suppertFragment:Fragment) {
        this.suppertFragment = suppertFragment
    }

    companion object {
        fun with(activity: FragmentActivity):TakePhotoManager {
            return RequestManagerRetriever.get(activity)
        }

        fun with(activity: Activity):TakePhotoManager {
            return RequestManagerRetriever.get(activity)
        }

        fun with(fragment: Fragment):TakePhotoManager {
            return RequestManagerRetriever.get(fragment)
        }

        fun with(fragemt: android.app.Fragment):TakePhotoManager {
            return RequestManagerRetriever.get(fragemt)
        }
    }

    fun getFragment():android.app.Fragment? {
        return fragemt
    }

    fun getSupportFragment():Fragment? {
        return suppertFragment
    }
}