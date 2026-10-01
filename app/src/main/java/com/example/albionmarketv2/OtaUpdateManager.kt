package com.example.albionmarketv2

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object OtaUpdateManager {

    suspend fun downloadAndInstallUpdate(context: Context, updateUrlInput: String? = null): Boolean = withContext(Dispatchers.IO) {
        val downloadUrls = if (!updateUrlInput.isNullOrBlank()) {
            listOf(updateUrlInput)
        } else {
            ServerSyncManager.getServerBaseUrls(context).map { "$it/download/AlbionDataPro.apk" }
        }

        for (downloadUrl in downloadUrls) {
            try {
                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 10000
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val apkFile = File(context.getExternalFilesDir(null), "albion_update.apk")
                    if (apkFile.exists()) apkFile.delete()

                    connection.inputStream.use { input ->
                        apkFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    if (apkFile.exists() && (apkFile.length() > 0)) {
                        withContext(Dispatchers.Main) {
                            installApk(context, apkFile)
                        }
                        return@withContext true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        false
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = "package:${context.packageName}".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile,
                )
                setDataAndType(uri, "application/vnd.android.package-archive")
            }
            context.startActivity(intent)
            try {
                apkFile.deleteOnExit()
            } catch (_: Exception) {}
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
