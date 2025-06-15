package com.nof1.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for handling image operations
 */
object ImageUtils {
    
    private const val IMAGE_DIRECTORY = "note_images"
    
    /**
     * Creates a temporary file for capturing an image from camera
     */
    fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "NOTE_$timeStamp"
        val storageDir = File(context.getExternalFilesDir(null), IMAGE_DIRECTORY)
        
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        
        return File(storageDir, "$imageFileName.jpg")
    }
    
    /**
     * Gets a FileProvider URI for the given file
     */
    fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
    
    /**
     * Copies an image from a URI to the app's internal storage
     */
    fun copyImageToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val inputStream: InputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val destinationFile = createImageFile(context)
            
            val outputStream = FileOutputStream(destinationFile)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Deletes an image file from storage
     */
    fun deleteImage(imagePath: String): Boolean {
        return try {
            val file = File(imagePath)
            if (file.exists()) {
                file.delete()
            } else {
                true // File doesn't exist, consider it deleted
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Checks if an image file exists
     */
    fun imageExists(imagePath: String?): Boolean {
        return if (imagePath != null) {
            File(imagePath).exists()
        } else {
            false
        }
    }
} 