package com.nishit.grokwatch

import android.content.Context
import android.util.Log

internal object CrashLog {
    private const val PREFS = "grok_crash"
    private const val KEY_TRACE = "trace"

    fun save(context: Context, throwable: Throwable) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TRACE, Log.getStackTraceString(throwable))
            .commit()
    }

    fun read(context: Context): String? {
        return context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TRACE, null)
            ?.takeIf { it.isNotBlank() }
    }

    fun clear(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_TRACE)
            .commit()
    }
}
