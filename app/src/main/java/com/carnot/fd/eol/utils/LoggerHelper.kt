package com.carnot.fd.eol.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LoggerHelper {
    fun saveLogToFile(context: Context, log: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get the app-specific directory for storing files
                val file = File(context.getExternalFilesDir(null), "logs.txt")

                // Ensure the file exists
                if (!file.exists()) {
                    file.createNewFile()
                }

                // Get the current timestamp
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                    Date()
                )

                // Create a log entry with the timestamp
                val logEntry = "$timestamp: $log"

                // Append the log entry to the file
                file.appendText("$logEntry\n")

                Log.d("Log", "Log saved successfully to file: ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e("Log", "Error saving log to file: ${e.message}")
//                logCrashError(
//                    apiName = "saveLogToFile",
//                    error = e,
//                    message = "Error saving log to file"
//                )
            }
        }
    }
    private fun Context.getScopedStorageFile(fileName: String): File {
        val directory = getExternalMediaDirs().firstOrNull()
        val file = File(directory, fileName)
        return file
    }

}