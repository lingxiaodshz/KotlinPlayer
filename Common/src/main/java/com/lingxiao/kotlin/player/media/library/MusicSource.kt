package com.lingxiao.kotlin.player.media.library

import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import androidx.annotation.IntDef
import com.lingxiao.kotlin.player.media.extensions.*

/**
 * @author luckw
 * @date   2020/9/1
 */

interface MusicSource : Iterable<MediaMetadataCompat> {
    suspend fun load()

    fun whenReady(performAction: (Boolean) -> Unit): Boolean

    fun search(query: String, extras: Bundle): List<MediaMetadataCompat>

}

@IntDef(
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
)
@Retention(AnnotationRetention.SOURCE)
annotation class State

/**
 * State indicating the source was created, but no initialization has performed.
 */
const val STATE_CREATED = 1

/**
 * State indicating initialization of the source is in progress.
 */
const val STATE_INITIALIZING = 2

/**
 * State indicating the source has been initialized and is ready to be used.
 */
const val STATE_INITIALIZED = 3

/**
 * State indicating an error has occurred.
 */
const val STATE_ERROR = 4

abstract class AbstractMusicSource : MusicSource {
    @State
    var state: Int = STATE_CREATED
        set(value) {
            if (value == STATE_INITIALIZED || value == STATE_ERROR) {
                synchronized(onReadyListeners) {
                    field = value
                    onReadyListeners.forEach { listener ->
                        listener(state == STATE_INITIALIZED)
                    }
                }
            } else {
                field = value
            }
        }

    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    override fun whenReady(performAction: (Boolean) -> Unit): Boolean =
        when (state) {
            STATE_CREATED, STATE_INITIALIZING -> {
                onReadyListeners += performAction
                false
            }
            else -> {
                performAction(state != STATE_ERROR)
                true
            }
        }

    override fun search(query: String, extras: Bundle): List<MediaMetadataCompat> {
        val focusSearchResult = when (extras[MediaStore.EXTRA_MEDIA_FOCUS]) {
            MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE -> {
                val genre = extras[EXTRA_MEDIA_GENRE]
                filter { song -> song.genre == genre }
            }
            MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE -> {
                val artist = extras[MediaStore.EXTRA_MEDIA_ARTIST]
                filter { song ->
                    song.artist == artist || song.albumArtist == artist
                }
            }

            MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE -> {
                // For an Album focused search, album and artist are set.
                val artist = extras[MediaStore.EXTRA_MEDIA_ARTIST]
                val album = extras[MediaStore.EXTRA_MEDIA_ALBUM]
                Log.d(TAG, "Focused album search: album='$album' artist='$artist")
                filter { song ->
                    (song.artist == artist || song.albumArtist == artist) && song.album == album
                }
            }
            MediaStore.Audio.Media.ENTRY_CONTENT_TYPE -> {
                // For a Song (aka Media) focused search, title, album, and artist are set.
                val title = extras[MediaStore.EXTRA_MEDIA_TITLE]
                val album = extras[MediaStore.EXTRA_MEDIA_ALBUM]
                val artist = extras[MediaStore.EXTRA_MEDIA_ARTIST]
                Log.d(TAG, "Focused media search: title='$title' album='$album' artist='$artist")
                filter { song ->
                    (song.artist == artist || song.albumArtist == artist) && song.album == album
                            && song.title == title
                }
            }
            else -> {
                emptyList()
            }
        }

        if (focusSearchResult.isEmpty()) {
            return if (query.isNotBlank()) {
                filter { song ->
                    song.title.containsCaseInsensitive(query) || song.genre.containsCaseInsensitive(
                        query
                    )
                }
            } else {
                return shuffled()
            }
        } else {
            return focusSearchResult
        }
    }

    private val EXTRA_MEDIA_GENRE
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaStore.EXTRA_MEDIA_GENRE
        } else {
            "android.intent.extra.genre"
        }
}

private const val TAG = "MusicSource"