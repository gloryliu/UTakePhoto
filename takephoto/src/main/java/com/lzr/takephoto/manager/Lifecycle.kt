package com.lzr.takephoto.manager

import androidx.annotation.NonNull

/**
 * @author liuzhenrong
 * @date 3/9/21 9:57 AM
 * @desc
 */
interface Lifecycle {

    fun addListener(@NonNull listener:LifecycleListener)

    fun removeListener(@NonNull listener:LifecycleListener)

}