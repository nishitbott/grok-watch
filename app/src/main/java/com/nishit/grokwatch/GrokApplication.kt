package com.nishit.grokwatch

import android.app.Application
import android.os.Process
import kotlin.system.exitProcess

class GrokApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            try {
                CrashLog.save(this, throwable)
            } catch (_: Throwable) {
                // Prefs can fail; the process still has to die.
            }
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }
}
