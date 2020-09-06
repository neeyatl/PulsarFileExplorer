package com.aurumtechie.pulsarfileexplorer

import java.io.File
import java.util.*

object Helper {

    /** Function to get the character sequence from after the last instance of File.separatorChar in a path
     * @author Neeyat Lotlikar
     * @param path String path representing the file
     * @return String filename which is the character sequence from after the last instance of File.separatorChar in a path
     * if the path contains the File.separatorChar. Else, the same path.*/
    fun getFilenameForPath(path: String): String =
        if (!path.contains(File.separatorChar)) path
        else path.subSequence(
            path.lastIndexOf(File.separatorChar) + 1, // Discard the File.separatorChar
            path.length // parameter is used exclusively. Substring produced till n - 1 characters are reached.
        ).toString()

    /** Function to get the MimeType from a filename by comparing it's file extension
     * @author Neeyat Lotlikar
     * @param filename String name of the file. Can also be a path.
     * @return String MimeType */
    fun getMimeType(filename: String): String = if (filename.lastIndexOf('.') == -1)
        "resource/folder"
    else
        when (filename.subSequence(
            filename.lastIndexOf('.'),
            filename.length
        ).toString().toLowerCase(Locale.ROOT)) {
            ".doc", ".docx" -> "application/msword"
            ".pdf" -> "application/pdf"
            ".ppt", ".pptx" -> "application/vnd.ms-powerpoint"
            ".xls", ".xlsx" -> "application/vnd.ms-excel"
            ".zip", ".rar" -> "application/x-wav"
            ".7z" -> "application/x-7z-compressed"
            ".rtf" -> "application/rtf"
            ".wav", ".mp3", ".m4a", ".ogg", ".oga", ".weba" -> "audio/*"
            ".ogx" -> "application/ogg"
            ".gif" -> "image/gif"
            ".jpg", ".jpeg", ".png", ".bmp" -> "image/*"
            ".csv" -> "text/csv"
            ".m3u8" -> "application/vnd.apple.mpegurl"
            ".txt", ".mht", ".mhtml", ".html" -> "text/plain"
            ".3gp", ".mpg", ".mpeg", ".mpe", ".mp4", ".avi", ".ogv", ".webm" -> "video/*"
            else -> "*/*"
        }

}