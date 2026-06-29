package com.jacobrozell.puzzlebuddy

import android.app.Application
import com.jacobrozell.puzzlebuddy.support.FirebaseBootstrap
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PuzzleBuddyApp : Application() {
    @Inject lateinit var firebaseBootstrap: FirebaseBootstrap

    override fun onCreate() {
        super.onCreate()
        firebaseBootstrap.initialize(this)
    }
}
