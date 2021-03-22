package com.lzr.takephoto.utils;

import android.content.Context;

/**
 * @author liuzhenrong
 * @date 3/12/21 11:43 AM
 * @desc
 */
public class TConstant {

    /**
     * 类型拍照
     */
    public final static int TYPE_TAKE_PHOTO = 1000;

    /**
     * 类型从相册选择
     */
    public final static int TYPE_SELECT_IMAGE = 1001;

    /**
     * 请求权限requestCode
     */
    public final static int PERMISSION_REQUEST_CODE = 1002;

    public final static String getFileProviderName(Context context) {
        return context.getPackageName() + ".fileprovider";
    }
}
