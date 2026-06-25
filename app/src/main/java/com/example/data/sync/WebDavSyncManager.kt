package com.example.data.sync

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.ByteString.Companion.encodeUtf8
import java.util.concurrent.TimeUnit

class WebDavSyncManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val backupDataAdapter = moshi.adapter(BackupData::class.java)

    private fun getAuthHeader(username: String, appPassword: String): String {
        val credentials = "$username:$appPassword"
        val base64 = credentials.encodeUtf8().base64()
        return "Basic $base64"
    }

    private fun sanitizeUrl(serverUrl: String): String {
        var url = serverUrl.trim()
        if (!url.endsWith("/")) {
            url += "/"
        }
        return url
    }

    suspend fun testConnection(username: String, appPassword: String, serverUrl: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val baseUrl = sanitizeUrl(serverUrl)
                val authHeader = getAuthHeader(username, appPassword)

                val request = Request.Builder()
                    .url(baseUrl)
                    .header("Authorization", authHeader)
                    .header("Depth", "0")
                    .method("PROPFIND", "".toRequestBody("text/xml".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful || response.code == 207 || response.code == 301 || response.code == 200) {
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("连接失败，状态码: ${response.code} (${response.message})"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun uploadBackup(
        username: String,
        appPassword: String,
        serverUrl: String,
        backupData: BackupData,
        remoteFileName: String = "loantracker_sync.json"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = sanitizeUrl(serverUrl)
            val fileUrl = "$baseUrl$remoteFileName"
            val authHeader = getAuthHeader(username, appPassword)

            // Ensure backup folder directory exists by executing MKCOL if needed
            // However, Jianguoyun root dav directory usually allows creating files directly at /dav/loantracker_sync.json
            val jsonString = backupDataAdapter.toJson(backupData)
            val requestBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(fileUrl)
                .header("Authorization", authHeader)
                .put(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 201 || response.code == 204 || response.code == 200) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("上传失败，状态码: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadBackup(
        username: String,
        appPassword: String,
        serverUrl: String,
        remoteFileName: String = "loantracker_sync.json"
    ): Result<BackupData> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = sanitizeUrl(serverUrl)
            val fileUrl = "$baseUrl$remoteFileName"
            val authHeader = getAuthHeader(username, appPassword)

            val request = Request.Builder()
                .url(fileUrl)
                .header("Authorization", authHeader)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    Result.failure(Exception("未在坚果云中找到备份文件 ($remoteFileName)"))
                } else if (!response.isSuccessful) {
                    Result.failure(Exception("下载失败，状态码: ${response.code}"))
                } else {
                    val bodyString = response.body?.string() ?: ""
                    val backupData = backupDataAdapter.fromJson(bodyString)
                    if (backupData != null) {
                        Result.success(backupData)
                    } else {
                        Result.failure(Exception("解析备份文件失败：内容格式不正确"))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
