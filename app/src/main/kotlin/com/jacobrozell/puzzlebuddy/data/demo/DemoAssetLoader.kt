package com.jacobrozell.puzzlebuddy.data.demo

import android.content.Context
import com.jacobrozell.puzzlebuddy.data.repository.DemoPhotoSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoAssetLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) : DemoPhotoSource {
    override fun bytes(assetName: String): ByteArray? = runCatching {
        context.assets.open("demo/$assetName.jpg").use { it.readBytes() }
    }.getOrNull()
}
