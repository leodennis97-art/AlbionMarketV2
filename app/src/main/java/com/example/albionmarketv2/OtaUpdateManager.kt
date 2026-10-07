package com.example.albionmarketv2

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object OtaUpdateManager {

    fun getInstalledVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "0.0.0"
        } catch (_: Exception) {
            "0.0.0"
        }
    }

    fun getInstalledVersionCode(context: Context): Long {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (_: Exception) {
            0L
        }
    }

    fun getApkArchiveInfo(context: Context, apkFile: File): Pair<Long, String>? {
        return try {
            val pInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0) ?: return null
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
            val name = pInfo.versionName ?: "0.0.0"
            Pair(code, name)
        } catch (_: Exception) {
            null
        }
    }

    fun compareVersionStrings(v1: String, v2: String): Int {
        fun parseParts(v: String): List<Int> {
            val clean = v.trim().lowercase().removePrefix("v")
            return Regex("\\d+").findAll(clean).mapNotNull { it.value.toIntOrNull() }.toList()
        }
        val parts1 = parseParts(v1)
        val parts2 = parseParts(v2)
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }

    fun isNewerVersion(
        newCode: Long,
        newName: String,
        currentCode: Long,
        currentName: String
    ): Boolean {
        if (newCode > 0 && currentCode > 0) {
            if (newCode > currentCode) return true
            if (newCode < currentCode) return false
        }
        return compareVersionStrings(newName, currentName) > 0
    }

    private fun isZipApkHeader(file: File): Boolean {
        if (!file.exists() || file.length() < 500_000L) return false
        return try {
            file.inputStream().use { stream ->
                val header = ByteArray(4)
                val read = stream.read(header, 0, 4)
                read == 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() && header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun downloadAndInstallUpdate(
        context: Context,
        updateUrlInput: String? = null,
        force: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        CryptoSecurityUtils.setupPermissiveSSLAndHostnameVerifier()

        if (force) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "⏳ Wecke Render Cloud Server auf (kann bis zu 30 Sek. dauern)...", Toast.LENGTH_LONG).show()
            }
        }

        val baseUrls = ServerSyncManager.getServerBaseUrls(context)
        val mainBaseUrl = (baseUrls.firstOrNull() ?: "https://albionmarketv2-1.onrender.com").trimEnd('/')
        val timestamp = System.currentTimeMillis()

        val downloadUrls = if (!updateUrlInput.isNullOrBlank()) {
            val formattedInput = if (updateUrlInput.startsWith("/")) "$mainBaseUrl$updateUrlInput" else updateUrlInput
            listOf(
                if (formattedInput.contains("?")) "$formattedInput&t=$timestamp" else "$formattedInput?t=$timestamp",
                "https://albionmarketv2-1.onrender.com/download/AlbionDataPro.apk?v=$timestamp",
                "https://github.com/leodennis97-art/AlbionMarketV2/releases/latest/download/AlbionDataPro.apk?t=$timestamp"
            )
        } else {
            val list = mutableListOf<String>()
            for (base in baseUrls) {
                val cleanBase = base.trimEnd('/')
                list.add("$cleanBase/download/AlbionDataPro.apk?v=$timestamp")
                list.add("$cleanBase/dl?t=$timestamp")
                list.add("$cleanBase/apk?t=$timestamp")
            }
            list.add("https://github.com/leodennis97-art/AlbionMarketV2/releases/latest/download/AlbionDataPro.apk?t=$timestamp")
            list
        }

        val currentCode = getInstalledVersionCode(context)
        val currentName = getInstalledVersionName(context)

        // Robust warm-up polling loop to wait until sleeping Render cloud instance is fully awake
        var warmedUp = false
        var attempt = 0
        while (attempt < 10 && !warmedUp) {
            try {
                for (base in baseUrls) {
                    val cleanBase = base.trimEnd('/')
                    val warmUpUrl = URL("$cleanBase/api/health")
                    val conn = warmUpUrl.openConnection() as HttpURLConnection
                    conn.connectTimeout = 15000
                    conn.readTimeout = 15000
                    val code = conn.responseCode
                    conn.disconnect()
                    if (code == HttpURLConnection.HTTP_OK) {
                        warmedUp = true
                        break
                    }
                }
                if (warmedUp) {
                    if (force) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "✅ Server ist bereit. Lade Update herunter...", Toast.LENGTH_SHORT).show()
                        }
                    }
                    break
                }
            } catch (_: Exception) {}
            attempt++
            delay(3000L)
        }

        for (downloadUrl in downloadUrls) {
            try {
                var currentUrl = downloadUrl
                var connection = URL(currentUrl).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true

                var redirects = 0
                var responseCode = -1

                while (redirects < 5) {
                    connection.requestMethod = "GET"
                    connection.useCaches = false
                    connection.setRequestProperty("Cache-Control", "no-cache")
                    connection.setRequestProperty("Pragma", "no-cache")
                    connection.setRequestProperty("User-Agent", "AlbionDataPro/$currentName")
                    connection.setRequestProperty("Accept", "application/vnd.android.package-archive, */*")
                    connection.connectTimeout = 90000
                    connection.readTimeout = 90000
                    connection.connect()

                    responseCode = connection.responseCode
                    if (responseCode in 301..308) {
                        val redirectUrl = connection.getHeaderField("Location")
                        connection.disconnect()
                        if (!redirectUrl.isNullOrBlank()) {
                            currentUrl = redirectUrl
                            connection = URL(currentUrl).openConnection() as HttpURLConnection
                            connection.instanceFollowRedirects = true
                            redirects++
                            continue
                        }
                    }
                    break
                }

                println("OtaUpdateManager: Checking URL $currentUrl -> Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Direkt in den öffentlichen Android Download-Ordner herunterladen, um Paketfehler zu umgehen
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    val apkFile = File(downloadsDir, "AlbionDataPro.apk")
                    if (apkFile.exists()) apkFile.delete()

                    connection.inputStream.use { input ->
                        apkFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    if (apkFile.exists() && apkFile.length() > 500_000L) {
                        if (!isZipApkHeader(apkFile)) {
                            println("OtaUpdateManager: Heruntergeladene Datei hat keine gültige APK-Signatur (vermutlich HTML-Fehlerseite).")
                            apkFile.delete()
                            continue
                        }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "✅ Update erfolgreich in Downloads gespeichert. Starte Installation...", Toast.LENGTH_LONG).show()
                            installApk(context, apkFile)
                        }
                        return@withContext true
                    }
                }
            } catch (e: Exception) {
                println("OtaUpdateManager: Fehler beim Download von $downloadUrl: ${e.message}")
            }
        }

        // Direkter Link zum APK-Download auf der Render-Website, um "Paket ungültig" (HTML-Seiten-Download) zu verhindern
        try {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "🌐 Starte direkten APK-Download von Render...", Toast.LENGTH_LONG).show()
                val apkDirectUrl = "https://albionmarketv2-1.onrender.com/download/AlbionDataPro.apk"
                val intent = Intent(Intent.ACTION_VIEW, apkDirectUrl.toUri()).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
        }

        false
    }

    fun installApk(context: Context, @Suppress("UNUSED_PARAMETER") apkFile: File) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, "https://albionmarketv2-1.onrender.com/download/AlbionDataPro.apk".toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
