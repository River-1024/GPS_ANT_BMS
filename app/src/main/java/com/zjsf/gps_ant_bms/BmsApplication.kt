package com.zjsf.gps_ant_bms

import android.app.Application

class BmsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            AppLogger.markCrashPending()
            AppLogger.e(
                "Crash",
                "uncaught exception on thread=${thread.name}",
                throwable
            )
            previousHandler?.uncaughtException(thread, throwable)
        }
    }
}
