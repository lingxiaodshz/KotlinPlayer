package com.lingxiao.kotlin.player.media.extensions

import android.content.ContentResolver
import android.net.Uri
import java.io.File

/**
 * @author luckw
 * @date   2020/8/31
 */

fun File.asAlbumArtContentUri():Uri{
    return Uri.Builder()
        .scheme(ContentResolver.SCHEME_CONTENT)
        .authority(AUTHORITY)
        .build()
}

private const val AUTHORITY = "com.lingxiao.kotlin.player.media.library.provider"