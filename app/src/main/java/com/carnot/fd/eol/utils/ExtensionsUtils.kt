package com.carnot.fd.eol.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.annotation.AnyRes
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket


suspend fun apiCall(execute:suspend ()->Unit,onNoInternet:()->Unit,onException:(e:Exception)->Unit){
  /*  NetworkUtils.isInternetAvailable {
        if (it) {
            try {
                execute()
            } catch (e: Exception) {
                onException(e)
            }
        } else {
            onNoInternet()
        }
    }*/
    isInternetAvailable {
        if (it) {
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        execute()
                    }catch (e:Exception) {
                        onException(e)
                    }
                }
            } catch (e: Exception) {
                onException(e)
            }
        } else {
            onNoInternet()
        }
    }


}

suspend fun isInternetAvailable(listener: (connected: Boolean) -> Unit) {
     val HOST_NAME = "8.8.8.8"
     val PORT = 53
    val TIMEOUT = 1_500
    CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
        try {
            Socket().use { it.connect(InetSocketAddress(HOST_NAME, PORT), TIMEOUT) }
            withContext(Dispatchers.Main) {
                listener(true)
            }
        } catch (e: IOException) {
            withContext(Dispatchers.Main) {
                listener(false)
            }
        }catch(e: Exception) {
            withContext(Dispatchers.Main) {
                listener(false)
            }
        }
    }
}

fun Context.getAssetPdfUri(fileName: String): Uri? {
    return try {
        // Copy the file from assets to cache directory
        val file = File(cacheDir, fileName)
        assets.open(fileName).use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        // Return the URI for the copied file
        Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun Context.logMessage(value:String){
    LoggerHelper.saveLogToFile(this,value)
}
