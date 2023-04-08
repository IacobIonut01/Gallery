/**
 * Source
 * https://github.com/saparkhid/AndroidFileNamePicker/blob/main/javautil/FileUtils.java
 */
package com.dot.gallery.feature_node.presentation.util

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.TextUtils
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.min

class FileUtils(var context: Context) {
    @SuppressLint("NewApi")
    fun getPath(uri: Uri): String? {
        // check here to KITKAT or new version
        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
        val selection: String?
        val selectionArgs: Array<String>?
        // DocumentProvider
        if (isKitKat) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                var fullPath = getPathFromExtSD(split)
                if (fullPath == null || !fileExists(fullPath)) {
                    fullPath = copyFileToInternalStorage(uri, FALLBACK_COPY_FOLDER)
                }
                return if (fullPath != "") {
                    fullPath
                } else {
                    null
                }
            }


            // DownloadsProvider
            if (isDownloadsDocument(uri)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.contentResolver.query(
                        uri, arrayOf(
                            MediaStore.MediaColumns.DISPLAY_NAME
                        ), null, null, null
                    ).use { cursor ->
                        if (cursor != null && cursor.moveToFirst()) {
                            val fileName = cursor.getString(0)
                            val path = Environment.getExternalStorageDirectory()
                                .toString() + "/Download/" + fileName
                            if (!TextUtils.isEmpty(path)) {
                                return path
                            }
                        }
                    }
                    val id: String = DocumentsContract.getDocumentId(uri)
                    if (!TextUtils.isEmpty(id)) {
                        if (id.startsWith("raw:")) {
                            return id.replaceFirst("raw:".toRegex(), "")
                        }
                        val contentUriPrefixesToTry = arrayOf(
                            "content://downloads/public_downloads",
                            "content://downloads/my_downloads"
                        )
                        for (contentUriPrefix in contentUriPrefixesToTry) {
                            return try {
                                val contentUri = ContentUris.withAppendedId(
                                    Uri.parse(contentUriPrefix),
                                    id.toLong()
                                )
                                getDataColumn(context, contentUri, null, null)
                            } catch (e: NumberFormatException) {
                                //In Android 8 and Android P the id is not a number
                                uri.path!!.replaceFirst("^/document/raw:".toRegex(), "")
                                    .replaceFirst("^raw:".toRegex(), "")
                            }
                        }
                    }
                } else {
                    val id = DocumentsContract.getDocumentId(uri)
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:".toRegex(), "")
                    }
                    try {
                        contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), id.toLong()
                        )
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                    }
                    if (contentUri != null) return getDataColumn(
                        context, contentUri, null, null
                    )
                }
            }


            // MediaProvider
            if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                when (type) {
                    "image" -> {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    "video" -> {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    }
                    "audio" -> {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                    "document" -> {
                        contentUri = MediaStore.Files.getContentUri(MediaStore.getVolumeName(uri))
                    }
                }
                selection = "_id=?"
                selectionArgs = arrayOf(
                    split[1]
                )
                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
            if (isGoogleDriveUri(uri)) {
                return getDriveFilePath(uri)
            }
            if (isWhatsAppFile(uri)) {
                return getFilePathForWhatsApp(uri)
            }
            if ("content".equals(uri.scheme, ignoreCase = true)) {
                if (isGooglePhotosUri(uri)) {
                    return uri.lastPathSegment
                }
                if (isGoogleDriveUri(uri)) {
                    return getDriveFilePath(uri)
                }
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    copyFileToInternalStorage(uri, FALLBACK_COPY_FOLDER)
                } else {
                    getDataColumn(context, uri, null, null)
                }
            }
            if ("file".equals(uri.scheme, ignoreCase = true)) {
                return uri.path
            }
        } else {
            if (isWhatsAppFile(uri)) {
                return getFilePathForWhatsApp(uri)
            }
            if ("content".equals(uri.scheme, ignoreCase = true)) {
                val projection = arrayOf(
                    MediaStore.Images.Media.DATA
                )
                var cursor: Cursor? = null
                try {
                    cursor = context.contentResolver
                        .query(uri, projection, null, null, null)
                    if (cursor != null && cursor.moveToFirst()) {
                        return cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                cursor?.close()
            }
        }
        return copyFileToInternalStorage(uri, FALLBACK_COPY_FOLDER)
    }

    private fun getDriveFilePath(uri: Uri): String {
        val returnCursor = context.contentResolver.query(uri, null, null, null, null)
        /*
         * Get the column indexes of the data in the Cursor,
         *     * move to the first row in the Cursor, get the data,
         *     * and display it.
         * */
        val nameIndex = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        returnCursor.close()
        val file = File(context.cacheDir, name)
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            var read: Int
            val maxBufferSize = 1 * 1024 * 1024
            val bytesAvailable = inputStream!!.available()
            val bufferSize = min(bytesAvailable, maxBufferSize)
            val buffers = ByteArray(bufferSize)
            while (inputStream.read(buffers).also { read = it } != -1) {
                outputStream.write(buffers, 0, read)
            }
            inputStream.close()
            outputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, e.message!!)
        }
        return file.path
    }

    /***
     * Used for Android Q+
     * @param uri
     * @param newDirName if you want to create a directory, you can set this variable
     * @return
     */
    private fun copyFileToInternalStorage(uri: Uri, newDirName: String): String {
        val returnCursor = context.contentResolver.query(
            uri, arrayOf(
                OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE
            ), null, null, null
        )


        /*
         * Get the column indexes of the data in the Cursor,
         *     * move to the first row in the Cursor, get the data,
         *     * and display it.
         * */
        val nameIndex = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        returnCursor.close()
        val output: File = if (newDirName != "") {
            val randomCollisionAvoidance = UUID.randomUUID().toString()
            val dir =
                File(context.filesDir.toString() + File.separator + newDirName + File.separator + randomCollisionAvoidance)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            File(context.filesDir.toString() + File.separator + newDirName + File.separator + randomCollisionAvoidance + File.separator + name)
        } else {
            File(context.filesDir.toString() + File.separator + name)
        }
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(output)
            var read: Int
            val bufferSize = 1024
            val buffers = ByteArray(bufferSize)
            while (inputStream!!.read(buffers).also { read = it } != -1) {
                outputStream.write(buffers, 0, read)
            }
            inputStream.close()
            outputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, e.message!!)
        }
        return output.path
    }

    private fun getFilePathForWhatsApp(uri: Uri): String {
        return copyFileToInternalStorage(uri, "whatsapp")
    }

    private fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(
            column
        )
        try {
            cursor = context.contentResolver.query(
                uri!!, projection,
                selection, selectionArgs, null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    private fun isWhatsAppFile(uri: Uri): Boolean {
        return "com.whatsapp.provider.media" == uri.authority
    }

    private fun isGoogleDriveUri(uri: Uri): Boolean {
        return "com.google.android.apps.docs.storage" == uri.authority || "com.google.android.apps.docs.storage.legacy" == uri.authority
    }

    companion object {
        var FALLBACK_COPY_FOLDER = "upload_part"
        private const val TAG = "FileUtils"
        private var contentUri: Uri? = null
        private fun fileExists(filePath: String): Boolean {
            val file = File(filePath)
            return file.exists()
        }

        private fun getPathFromExtSD(pathData: Array<String>): String? {
            val type = pathData[0]
            val relativePath = File.separator + pathData[1]
            var fullPath: String
            // on my Sony devices (4.4.4 & 5.1.1), `type` is a dynamic string
            // something like "71F8-2C0A", some kind of unique id per storage
            // don't know any API that can get the root path of that storage based on its id.
            //
            // so no "primary" type, but let the check here for other devices
            if ("primary".equals(type, ignoreCase = true)) {
                fullPath = Environment.getExternalStorageDirectory().toString() + relativePath
                if (fileExists(fullPath)) {
                    return fullPath
                }
            }
            if ("home".equals(type, ignoreCase = true)) {
                fullPath = "/storage/emulated/0/Documents$relativePath"
                if (fileExists(fullPath)) {
                    return fullPath
                }
            }

            // Environment.isExternalStorageRemovable() is `true` for external and internal storage
            // so we cannot relay on it.
            //
            // instead, for each possible path, check if file exists
            // we'll start with secondary storage as this could be our (physically) removable sd card
            fullPath = System.getenv("SECONDARY_STORAGE")!! + relativePath
            if (fileExists(fullPath)) {
                return fullPath
            }
            fullPath = System.getenv("EXTERNAL_STORAGE")!! + relativePath
            return if (fileExists(fullPath)) {
                fullPath
            } else null
        }

        private fun isExternalStorageDocument(uri: Uri): Boolean {
            return "com.android.externalstorage.documents" == uri.authority
        }

        private fun isDownloadsDocument(uri: Uri): Boolean {
            return "com.android.providers.downloads.documents" == uri.authority
        }
    }
}