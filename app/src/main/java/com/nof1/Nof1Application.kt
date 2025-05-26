package com.nof1

import android.app.Application
import com.nof1.data.local.Nof1Database

/**
 * Application class for the Nof1 app.
 */
class Nof1Application : Application() {
    val database by lazy { Nof1Database.getDatabase(this) }
} 