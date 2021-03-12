package com.lzr.takephoto.manager

import android.net.Uri

/**
 * @author liuzhenrong
 * @date 3/9/21 9:58 AM
 * @desc 图片选择结果回调接口
 */
interface TakePhotoResult {

    fun takeSuccess(filePath: String)

    fun takeFailure(ex: Exception)

    fun takeCancel()

}