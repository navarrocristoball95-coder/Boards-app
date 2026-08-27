package com.quickreply.boards.data.sync

import android.content.Context
import com.quickreply.boards.data.local.dao.FolderDao
import com.quickreply.boards.data.local.dao.QuickReplyDao
import com.quickreply.boards.data.local.entity.FolderEntity
import com.quickreply.boards.data.local.entity.QuickReplyEntity
import com.quickreply.boards.data.model.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object SupabaseSyncConfig {
    const val SUPABASE_URL = "https://nbtzhmsyvjjgtkfupsby.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5idHpobXN5dmpqZ3RrZnVwc2J5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY3MDU0NzAsImV4cCI6MjEwMjI4MTQ3MH0.yD3i_dAmNDINxpJzu22AvY_S7WqWT7VGD3m8KEGAhOk"
}

data class UserSession(
    val accessToken: String,
    val userId: String,
    val email: String,
    val refreshToken: String = ""
)

class SupabaseSyncManager(
    private val context: Context,
    private val folderDao: FolderDao,
    private val quickReplyDao: QuickReplyDao
) {
    private val prefs = context.getSharedPreferences("supabase_auth_prefs", Context.MODE_PRIVATE)

    fun getSession(): UserSession? {
        val token = prefs.getString("access_token", null) ?: return null
        val userId = prefs.getString("user_id", "") ?: ""
        val email = prefs.getString("user_email", "") ?: ""
        val refreshToken = prefs.getString("refresh_token", "") ?: ""
        return UserSession(token, userId, email, refreshToken)
    }

    fun saveSession(session: UserSession) {
        prefs.edit()
            .putString("access_token", session.accessToken)
            .putString("user_id", session.userId)
            .putString("user_email", session.email)
            .putString("refresh_token", session.refreshToken)
            .apply()
    }

    fun signOut() {
        prefs.edit().clear().apply()
    }

    /**
     * Renueva automáticamente el token de acceso usando el refresh_token
     */
    suspend fun refreshSession(): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val current = getSession()
            val refToken = current?.refreshToken
            if (current == null || refToken.isNullOrBlank()) {
                return@withContext Result.failure(Exception("No hay refresh_token disponible"))
            }

            val url = URL("${SupabaseSyncConfig.SUPABASE_URL}/auth/v1/token?grant_type=refresh_token")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", SupabaseSyncConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }

            val body = JSONObject().apply {
                put("refresh_token", refToken.trim())
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(resp)
                val newAccessToken = json.getString("access_token")
                val newRefreshToken = json.optString("refresh_token", refToken)
                val userObj = json.optJSONObject("user")
                val userId = userObj?.optString("id", current.userId) ?: current.userId
                val userEmail = userObj?.optString("email", current.email) ?: current.email

                val updatedSession = UserSession(newAccessToken, userId, userEmail, newRefreshToken)
                saveSession(updatedSession)
                Result.success(updatedSession)
            } else {
                val errResp = conn.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: "Error al renovar sesión"
                Result.failure(Exception(errResp))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Iniciar sesión en Supabase con Email y Contraseña
     */
    suspend fun signIn(email: String, password: String): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${SupabaseSyncConfig.SUPABASE_URL}/auth/v1/token?grant_type=password")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", SupabaseSyncConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }

            val body = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(resp)
                val token = json.getString("access_token")
                val refreshToken = json.optString("refresh_token", "")
                val userObj = json.getJSONObject("user")
                val userId = userObj.getString("id")
                val userEmail = userObj.optString("email", email.trim())

                val session = UserSession(token, userId, userEmail, refreshToken)
                saveSession(session)
                Result.success(session)
            } else {
                val errResp = conn.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: "Error al autenticar"
                val errJson = try { JSONObject(errResp) } catch (_: Exception) { null }
                val errorMsg = errJson?.optString("error_description") ?: errJson?.optString("msg") ?: "Credenciales incorrectas"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Solicitar recuperación de contraseña por correo electrónico
     */
    suspend fun resetPassword(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${SupabaseSyncConfig.SUPABASE_URL}/auth/v1/recover")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", SupabaseSyncConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }

            val body = JSONObject().apply {
                put("email", email.trim())
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            if (conn.responseCode in 200..299) {
                Result.success(Unit)
            } else {
                val errResp = conn.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: "Error al solicitar recuperación"
                val errJson = try { JSONObject(errResp) } catch (_: Exception) { null }
                val errorMsg = errJson?.optString("error_description") ?: errJson?.optString("msg") ?: "No se pudo enviar el correo de recuperación"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crear cuenta nueva en Supabase
     */
    suspend fun signUp(email: String, password: String): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${SupabaseSyncConfig.SUPABASE_URL}/auth/v1/signup")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", SupabaseSyncConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }

            val body = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(resp)
                val token = json.optString("access_token", "")
                val refreshToken = json.optString("refresh_token", "")
                val userObj = json.optJSONObject("user")
                val userId = userObj?.optString("id", "") ?: ""
                val userEmail = userObj?.optString("email", email) ?: email

                if (token.isNotBlank()) {
                    val session = UserSession(token, userId, userEmail, refreshToken)
                    saveSession(session)
                    Result.success(session)
                } else {
                    Result.success(UserSession("", userId, userEmail, ""))
                }
            } else {
                val errResp = conn.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: "Error al registrarse"
                val errJson = try { JSONObject(errResp) } catch (_: Exception) { null }
                val errorMsg = errJson?.optString("error_description") ?: errJson?.optString("msg") ?: "No se pudo registrar"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun performFullSync(): Result<SyncSummary> = withContext(Dispatchers.IO) {
        try {
            var session = getSession()
            var bearerToken = if (session != null && session.accessToken.isNotBlank()) session.accessToken else SupabaseSyncConfig.SUPABASE_ANON_KEY

            // Auto-renovar token en segundo plano para evitar expiración 401
            if (session != null && session.refreshToken.isNotBlank()) {
                val refreshResult = refreshSession()
                if (refreshResult.isSuccess) {
                    session = refreshResult.getOrNull()
                    bearerToken = session?.accessToken ?: bearerToken
                }
            }

            var uploadedCount = 0
            val effectiveUserId = session?.userId?.takeIf { it.isNotBlank() } ?: "00000000-0000-0000-0000-000000000000"

            // 1. SUBIDA DE TABLEROS LOCALES PENDIENTES A SUPABASE (Primero padres, luego hijos)
            val localFolders = folderDao.getAllFoldersList()
            val (rootFolders, childFolders) = localFolders.partition { it.parentId == null }

            // Subir tableros raíz
            rootFolders.forEach { folder ->
                if (folder.remoteId == null || !folder.isSynced) {
                    val createdRemoteId = uploadFolderToRemote(folder, effectiveUserId, bearerToken, null)
                    if (createdRemoteId != null) {
                        folderDao.updateFolder(folder.copy(remoteId = createdRemoteId, isSynced = true))
                        uploadedCount++
                    }
                }
            }

            // Subir subcarpetas con el parent_id del padre
            val updatedLocalFolders = folderDao.getAllFoldersList().associateBy { it.id }
            childFolders.forEach { subfolder ->
                if (subfolder.remoteId == null || !subfolder.isSynced) {
                    val parentFolder = updatedLocalFolders[subfolder.parentId]
                    val parentRemoteId = parentFolder?.remoteId
                    val createdRemoteId = uploadFolderToRemote(subfolder, effectiveUserId, bearerToken, parentRemoteId)
                    if (createdRemoteId != null) {
                        folderDao.updateFolder(subfolder.copy(remoteId = createdRemoteId, isSynced = true))
                        uploadedCount++
                    }
                }
            }

            // 2. SUBIDA DE RESPUESTAS LOCALES PENDIENTES A SUPABASE
            val unsyncedReplies = quickReplyDao.getUnsyncedReplies()
            val currentFolders = folderDao.getAllFoldersList().associateBy { it.id }

            unsyncedReplies.forEach { reply ->
                val parentFolder = currentFolders[reply.folderId]
                if (parentFolder?.remoteId != null) {
                    if (reply.remoteId == null) {
                        if (!reply.isDeleted) {
                            val createdRemoteId = uploadReplyToRemote(reply, parentFolder.remoteId, effectiveUserId, bearerToken)
                            if (createdRemoteId != null) {
                                quickReplyDao.updateReply(reply.copy(remoteId = createdRemoteId, isSynced = true))
                                uploadedCount++
                            }
                        } else {
                            quickReplyDao.updateReply(reply.copy(isSynced = true))
                        }
                    } else {
                        val ok = updateRemoteReply(reply, parentFolder.remoteId, bearerToken)
                        if (ok) {
                            quickReplyDao.updateReply(reply.copy(isSynced = true))
                            uploadedCount++
                        }
                    }
                }
            }

            // 3. DESCARGA Y SINCRONIZACIÓN DE TABLEROS Y SUBCARPETAS DESDE SUPABASE
            val remoteFolders = fetchRemoteFolders(bearerToken)
            var foldersUpdated = 0
            val folderRemoteToLocalMap = mutableMapOf<String, Long>()
            val allLocalFolders = folderDao.getAllFoldersList().toMutableList()

            fun sanitizeName(n: String): String {
                return n.replace(Regex("^[\\p{So}\\p{Sk}\\p{Sm}\\p{Sc}\\p{P}\\s]+"), "").trim()
            }

            // Pase 1: Crear o vincular carpetas locales
            remoteFolders.forEach { remoteFolder ->
                val remoteId = remoteFolder.optString("id", "")
                if (remoteId.isBlank()) return@forEach
                val name = remoteFolder.optString("name", "Board").takeIf { it.isNotBlank() && it != "null" } ?: "Board"
                val color = remoteFolder.optString("color", "#4361EE").takeIf { it.isNotBlank() && it != "null" } ?: "#4361EE"
                val isDeleted = remoteFolder.optBoolean("is_deleted", false)

                val localExisting = allLocalFolders.find { it.remoteId == remoteId }
                    ?: allLocalFolders.find { sanitizeName(it.name).equals(sanitizeName(name), ignoreCase = true) }

                if (localExisting != null) {
                    folderRemoteToLocalMap[remoteId] = localExisting.id
                    if (localExisting.remoteId != remoteId) {
                        val updated = localExisting.copy(remoteId = remoteId, isSynced = true)
                        folderDao.updateFolder(updated)
                    }
                } else if (!isDeleted) {
                    val newLocalId = folderDao.insertFolder(
                        FolderEntity(
                            name = name,
                            colorHex = color,
                            remoteId = remoteId,
                            isSynced = true
                        )
                    )
                    folderRemoteToLocalMap[remoteId] = newLocalId
                    foldersUpdated++
                }
            }

            // Pase 2: Vincular parentId en subcarpetas
            remoteFolders.forEach { remoteFolder ->
                val remoteId = remoteFolder.optString("id", "")
                val parentRemoteId = remoteFolder.optString("parent_id", "").takeIf { it.isNotBlank() && it != "null" }
                val localFolderId = folderRemoteToLocalMap[remoteId]

                if (localFolderId != null) {
                    val targetParentId = if (parentRemoteId != null) folderRemoteToLocalMap[parentRemoteId] else null
                    val existing = folderDao.getFolderById(localFolderId)
                    if (existing != null && existing.parentId != targetParentId) {
                        folderDao.updateFolder(existing.copy(parentId = targetParentId))
                    }
                }
            }

            // 4. DESCARGA DE RESPUESTAS RÁPIDAS
            val remoteReplies = fetchRemoteReplies(bearerToken)
            var repliesUpdated = 0
            val allLocalReplies = quickReplyDao.getAllRepliesIncludingDeletedList()
            val localRepliesByRemoteId = allLocalReplies.filter { it.remoteId != null }.associateBy { it.remoteId }

            remoteReplies.forEach { remoteReply ->
                val remoteId = remoteReply.optString("id", "")
                if (remoteId.isBlank()) return@forEach
                val folderRemoteId = remoteReply.optString("folder_id", "")
                val rawTitle = remoteReply.optString("title", "Sin título")
                val title = if (rawTitle.isBlank() || rawTitle == "null") "Sin título" else rawTitle
                val rawContent = remoteReply.optString("content", "")
                val content = if (rawContent == "null") "" else rawContent
                val typeStr = remoteReply.optString("content_type", "TEXT")
                val isFavorite = remoteReply.optBoolean("is_favorite", false)
                val isDeleted = remoteReply.optBoolean("is_deleted", false)
                val mediaUrl = remoteReply.optString("media_url", "").takeIf { it.isNotBlank() && it != "null" }

                // Eliminar cualquier mensaje de prueba residual que venga de la nube
                val isDummyPrueba = title.trim().equals("prueba", ignoreCase = true) || 
                                    title.trim().equals("mensaje de prueba", ignoreCase = true)
                if (isDummyPrueba) {
                    permanentDeleteReplyRemote(remoteId)
                    return@forEach
                }

                val localFolderId = folderRemoteToLocalMap[folderRemoteId] 
                    ?: folderDao.getAllFoldersList().firstOrNull()?.id 
                    ?: 1L

                val contentType = try {
                    ContentType.valueOf(typeStr)
                } catch (_: Exception) {
                    ContentType.TEXT
                }

                val resolvedMediaUri = if (contentType == ContentType.LINK) {
                    mediaUrl ?: (if (content.startsWith("http://") || content.startsWith("https://")) content else null)
                } else {
                    mediaUrl
                }

                val resolvedContent = if (contentType == ContentType.LINK) {
                    val clean = content.replace(Regex("""\[?([^\]\n]+)\]?\s*\((https?://[^\)]+)\)"""), "$1")
                    if (resolvedMediaUri != null) clean.replace(resolvedMediaUri, "").trim() else clean.trim()
                } else {
                    content
                }

                fun normalizeTitle(t: String): String {
                    return t.trim().lowercase().replace(" de facebook", "").replace(" facebook", "")
                }

                val existingLocal = localRepliesByRemoteId[remoteId]
                    ?: allLocalReplies.firstOrNull { 
                        it.folderId == localFolderId && (
                            it.title.trim().equals(title.trim(), ignoreCase = true) ||
                            normalizeTitle(it.title) == normalizeTitle(title)
                        )
                    }

                if (existingLocal != null) {
                    quickReplyDao.updateReply(
                        existingLocal.copy(
                            title = title,
                            content = resolvedContent,
                            contentType = contentType,
                            mediaUri = resolvedMediaUri ?: existingLocal.mediaUri,
                            isFavorite = isFavorite,
                            isDeleted = isDeleted,
                            remoteId = remoteId,
                            isSynced = true
                        )
                    )
                    repliesUpdated++
                } else if (!isDeleted) {
                    quickReplyDao.insertReply(
                        QuickReplyEntity(
                            folderId = localFolderId,
                            title = title,
                            content = resolvedContent,
                            contentType = contentType,
                            mediaUri = resolvedMediaUri,
                            isFavorite = isFavorite,
                            isDeleted = false,
                            remoteId = remoteId,
                            isSynced = true
                        )
                    )
                    repliesUpdated++
                }
            }

            Result.success(SyncSummary(foldersUpdated + uploadedCount, repliesUpdated))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun uploadFolderToRemote(folder: FolderEntity, userId: String, token: String, parentRemoteId: String? = null): String? {
        try {
            val url = URL("${SupabaseSyncConfig.SUPABASE_URL}/rest/v1/folders")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", SupabaseSyncConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=representation")
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
            }

            val body = JSONObject().apply {
                put("user_id", if (userId.isNotBlank() && userId != "00000000-0000-0000-0000-000000000000") userId else JSONObject.NULL)
                put("name", folder.name)
                put("color", folder.colorHex)
                put("order_index", folder.sortOrder)
                if (parentRemoteId != null) {
                    put("parent_id", parentRemoteId)
                }
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val jsonArr = JSONArray(resp)
                if (jsonArr.length() > 0) {
                    return jsonArr.getJSONObject(0).getString("id")
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun uploadReplyToRemote(reply: QuickReplyEntity, folderRemoteId: String, userId: String, token: String): String? {
        try {
            val url = URL("${SupabaseSyncConfig.SUPABASE_URL}/rest/v1/quick_replies")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", SupabaseSyncConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=representation")
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
            }

            val uploadMediaUrl = if (reply.contentType == ContentType.LINK) {
                reply.mediaUri ?: if (reply.content.startsWith("http://") || reply.content.startsWith("https://")) reply.content else null
            } else {
                reply.mediaUri
            }

            val body = JSONObject().apply {
                put("user_id", if (userId.isNotBlank() && userId != "00000000-0000-0000-0000-000000000000") userId else JSONObject.NULL)
                put("folder_id", folderRemoteId)
                put("title", reply.title)
                put("content", reply.content)
                put("content_type", reply.contentType.name)
                put("media_url", uploadMediaUrl ?: JSONObject.NULL)
                put("is_favorite", reply.isFavorite)
                put("is_deleted", reply.isDeleted)
                put("order_index", reply.sortOrder)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val jsonArr = JSONArray(resp)
                if (jsonArr.length() > 0) {
                    return jsonArr.getJSONObject(0).getString("id")
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun updateRemoteReply(reply: QuickReplyEntity, folderRemoteId: String, token: String): Boolean {
        try {
            val url = URL("${SupabaseSyncConfig.SUPABASE_URL}/rest/v1/quick_replies?id=eq.${reply.remoteId}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                setRequestProperty("apikey", SupabaseSyncConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
            }

            val uploadMediaUrl = if (reply.contentType == ContentType.LINK) {
                reply.mediaUri ?: if (reply.content.startsWith("http://") || reply.content.startsWith("https://")) reply.content else null
            } else {
                reply.mediaUri
            }

            val body = JSONObject().apply {
                put("folder_id", folderRemoteId)
                put("title", reply.title)
                put("content", reply.content)
                put("content_type", reply.contentType.name)
                put("media_url", uploadMediaUrl ?: JSONObject.NULL)
                put("is_favorite", reply.isFavorite)
                put("is_deleted", reply.isDeleted)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
            return conn.responseCode in 200..299
        } catch (_: Exception) {}
        return false
    }

    suspend fun deleteReplyRemote(reply: QuickReplyEntity): Boolean {
        val session = getSession() ?: return false
        val bearerToken = session.accessToken
        val remoteId = reply.remoteId ?: return false

        return withContext(Dispatchers.IO) {
            try {
                val url = URL("${SupabaseSyncConfig.SUPABASE_URL}/rest/v1/quick_replies?id=eq.$remoteId")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "PATCH"
                    setRequestProperty("apikey", SupabaseSyncConfig.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $bearerToken")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Prefer", "return=minimal")
                    doOutput = true
                    connectTimeout = 6000
                    readTimeout = 6000
                }

                val body = JSONObject().apply {
                    put("is_deleted", true)
                }

                OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
                conn.responseCode in 200..299
            } catch (_: Exception) {
                false
            }
        }
    }

    suspend fun permanentDeleteReplyRemote(remoteId: String): Boolean {
        val session = getSession() ?: return false
        val bearerToken = session.accessToken

        return withContext(Dispatchers.IO) {
            try {
                val url = URL("${SupabaseSyncConfig.SUPABASE_URL}/rest/v1/quick_replies?id=eq.$remoteId")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                    setRequestProperty("apikey", SupabaseSyncConfig.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $bearerToken")
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = 6000
                    readTimeout = 6000
                }
                conn.responseCode in 200..299
            } catch (_: Exception) {
                false
            }
        }
    }

    suspend fun deleteFolderRemote(folder: FolderEntity): Boolean {
        val session = getSession() ?: return false
        val bearerToken = session.accessToken
        val remoteId = folder.remoteId ?: return false

        return withContext(Dispatchers.IO) {
            try {
                deleteRemoteFolder(remoteId, bearerToken)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun deleteRemoteFolder(remoteId: String, token: String) {
        try {
            val url = URL("${SupabaseSyncConfig.SUPABASE_URL}/rest/v1/folders?id=eq.$remoteId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("apikey", SupabaseSyncConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 6000
                readTimeout = 6000
            }
            conn.responseCode
        } catch (_: Exception) {}
    }

    private fun fetchRemoteFolders(token: String): List<JSONObject> {
        val url = URL("${SupabaseSyncConfig.SUPABASE_URL}/rest/v1/folders?is_deleted=eq.false&select=*")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("apikey", SupabaseSyncConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 8000
            readTimeout = 8000
        }

        return if (conn.responseCode in 200..299) {
            val response = conn.inputStream.bufferedReader().use(BufferedReader::readText)
            val jsonArray = JSONArray(response)
            (0 until jsonArray.length()).map { jsonArray.getJSONObject(it) }
        } else {
            emptyList()
        }
    }

    private fun fetchRemoteReplies(token: String): List<JSONObject> {
        val url = URL("${SupabaseSyncConfig.SUPABASE_URL}/rest/v1/quick_replies?select=*")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("apikey", SupabaseSyncConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 8000
            readTimeout = 8000
        }

        return if (conn.responseCode in 200..299) {
            val response = conn.inputStream.bufferedReader().use(BufferedReader::readText)
            val jsonArray = JSONArray(response)
            (0 until jsonArray.length()).map { jsonArray.getJSONObject(it) }
        } else {
            emptyList()
        }
    }
}

data class SyncSummary(
    val foldersSynced: Int,
    val repliesSynced: Int
)
