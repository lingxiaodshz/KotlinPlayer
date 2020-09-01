package com.lingxiao.kotlin.player.media.library

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import com.lingxiao.kotlin.player.media.R
import com.lingxiao.kotlin.player.media.extensions.*

/**
 * @author luckw
 * @date   2020/8/31
 */

class BrowseTree(context: Context, musicSource: MusicSource){
    private val mediaIdToChildren
            = mutableMapOf<String, MutableList<MediaMetadataCompat>>()
    val searchableByUnknownCaller = true

    init {
        val rootList = mediaIdToChildren[UAMP_BROWSABLE_ROOT] ?: mutableListOf()

        val recommendedMetadata = MediaMetadataCompat.Builder()
            .apply {
                id = UAMP_RECOMMENDED_ROOT
                title = context.getString(R.string.recommended_title)
                albumArtUri = RESOURCE_ROOT_URI +
                        context.resources.getResourceEntryName(R.drawable.ic_recommended)
                flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            }.build()

        val albumsMetadata = MediaMetadataCompat.Builder().apply {
            id = UAMP_ALBUMS_ROOT
            title = context.getString(R.string.albums_title)
            albumArtUri = RESOURCE_ROOT_URI +
                    context.resources.getResourceEntryName(R.drawable.ic_album)
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        }.build()

        rootList += recommendedMetadata
        rootList += albumsMetadata
        mediaIdToChildren[UAMP_BROWSABLE_ROOT] = rootList

        musicSource.forEach { mediaItem ->
            val albumMediaId = mediaItem.album.urlEncoded
            val albumChildren = mediaIdToChildren[albumMediaId] ?: buildAlbumRoot(mediaItem)
            albumChildren += mediaItem

            // Add the first track of each album to the 'Recommended' category
            if (mediaItem.trackNumber == 1L){
                val recommendedChildren = mediaIdToChildren[UAMP_RECOMMENDED_ROOT]
                    ?: mutableListOf()
                recommendedChildren += mediaItem
                mediaIdToChildren[UAMP_RECOMMENDED_ROOT] = recommendedChildren
            }
        }
    }

    operator fun get(mediaId: String) = mediaIdToChildren[mediaId]

    private fun buildAlbumRoot(mediaItem: MediaMetadataCompat): MutableList<MediaMetadataCompat> {
        val albumMetadata = MediaMetadataCompat.Builder().apply {
            id = mediaItem.album.urlEncoded
            title = mediaItem.album
            artist = mediaItem.artist
            albumArt = mediaItem.albumArt
            albumArtUri = mediaItem.albumArtUri.toString()
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        }.build()

        // Adds this album to the 'Albums' category.
        val rootList = mediaIdToChildren[UAMP_ALBUMS_ROOT] ?: mutableListOf()
        rootList += albumMetadata
        mediaIdToChildren[UAMP_ALBUMS_ROOT] = rootList

        // Insert the album's root with an empty list for its children, and return the list.
        return mutableListOf<MediaMetadataCompat>().also {
            mediaIdToChildren[albumMetadata.id!!] = it
        }
    }
}

const val UAMP_BROWSABLE_ROOT = "/"
const val UAMP_EMPTY_ROOT = "@empty@"
const val UAMP_RECOMMENDED_ROOT = "__RECOMMENDED__"
const val UAMP_ALBUMS_ROOT = "__ALBUMS__"

const val MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"

const val RESOURCE_ROOT_URI = "android.resource://com.lingxiao.kotlin.player.next/drawable/"
