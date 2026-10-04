package com.example.albionmarketv2

import android.content.Context
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ExternalStorageBackupManager {

    private fun getBackupFolder(context: Context): File {
        try {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val albionFolder = File(docsDir, "AlbionDataPro")
            if (!albionFolder.exists()) {
                albionFolder.mkdirs()
            }
            if (albionFolder.exists() && albionFolder.canWrite()) {
                return albionFolder
            }
        } catch (_: Throwable) {}

        val altFolder = File(context.getExternalFilesDir(null), "AlbionDataPro")
        try {
            if (!altFolder.exists()) {
                altFolder.mkdirs()
            }
        } catch (_: Throwable) {}
        return altFolder
    }

    fun backupSnapshots(context: Context, snapshots: List<PriceSnapshot>) {
        try {
            val folder = getBackupFolder(context)
            val file = File(folder, "price_snapshots_backup.json")
            val arr = JSONArray()
            val trimmed = snapshots.takeLast(2000)
            for (s in trimmed) {
                val obj = JSONObject().apply {
                    put("itemId", s.itemId)
                    put("city", s.city)
                    put("sellPriceMin", s.sellPriceMin)
                    put("buyPriceMax", s.buyPriceMax)
                    put("timestampMs", s.timestampMs)
                    put("sellPriceMinAmount", s.sellPriceMinAmount)
                }
                arr.put(obj)
            }
            file.writeText(arr.toString(), Charsets.UTF_8)
            println("[ExternalStorageBackupManager] Backed up ${trimmed.size} price snapshots to ${file.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadBackupSnapshots(context: Context): List<PriceSnapshot> {
        val list = mutableListOf<PriceSnapshot>()
        try {
            val folder = getBackupFolder(context)
            val file = File(folder, "price_snapshots_backup.json")
            if (file.exists() && file.length() > 0) {
                val jsonStr = file.readText(Charsets.UTF_8)
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val itemId = obj.optString("itemId", "")
                    val city = obj.optString("city", "")
                    if (itemId.isBlank() || city.isBlank()) continue
                    list.add(
                        PriceSnapshot(
                            itemId = itemId,
                            city = city,
                            sellPriceMin = obj.optInt("sellPriceMin", 0),
                            buyPriceMax = obj.optInt("buyPriceMax", 0),
                            timestampMs = obj.optLong("timestampMs", System.currentTimeMillis()),
                            sellPriceMinAmount = obj.optInt("sellPriceMinAmount", 0)
                        )
                    )
                }
                println("[ExternalStorageBackupManager] Loaded ${list.size} price snapshots from external backup: ${file.absolutePath}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun backupTradeOrders(context: Context, jsonStr: String) {
        try {
            val folder = getBackupFolder(context)
            val file = File(folder, "trade_orders_backup.json")
            file.writeText(jsonStr, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadBackupTradeOrders(context: Context): String {
        try {
            val folder = getBackupFolder(context)
            val file = File(folder, "trade_orders_backup.json")
            if (file.exists() && file.length() > 0) {
                return file.readText(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "[]"
    }

    fun backupGoldPurchases(context: Context, jsonStr: String) {
        try {
            val folder = getBackupFolder(context)
            val file = File(folder, "gold_purchases_backup.json")
            file.writeText(jsonStr, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadBackupGoldPurchases(context: Context): String {
        try {
            val folder = getBackupFolder(context)
            val file = File(folder, "gold_purchases_backup.json")
            if (file.exists() && file.length() > 0) {
                return file.readText(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "[]"
    }

    fun backupGoldSales(context: Context, jsonStr: String) {
        try {
            val folder = getBackupFolder(context)
            val file = File(folder, "gold_sales_backup.json")
            file.writeText(jsonStr, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadBackupGoldSales(context: Context): String {
        try {
            val folder = getBackupFolder(context)
            val file = File(folder, "gold_sales_backup.json")
            if (file.exists() && file.length() > 0) {
                return file.readText(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "[]"
    }
}
