package com.aurumtechie.pulsarfileexplorer

import java.io.File

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

}