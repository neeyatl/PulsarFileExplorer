package com.aurumtechie.pulsarfileexplorer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.ListFragment
import java.io.File
import java.util.*

/** ListFragment class to display all the files and folders present inside a folder
 * @author Neeyat Lotlikar */
class FilesListFragment : ListFragment() {

    private var path: String = ROOT_FLAG

    val currentPath
        get() = path

    companion object {
        private const val PATH_EXTRA = "path_extra"

        const val ROOT_FLAG = "root_path"

        fun getInstance(path: String): FilesListFragment =
            FilesListFragment().apply { this.path = path }

        interface DirectoryExplorer {
            fun onDirectoryClick(path: String)
        }
    }

    private lateinit var directoryExplorer: DirectoryExplorer

    private val values = mutableListOf<String>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        directoryExplorer = context as DirectoryExplorer
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If savedInstanceState is null, path will stay as default which will then used to show the root directories.
        if (savedInstanceState != null) path = savedInstanceState.getString(PATH_EXTRA, path)

        activity?.title = // Show the app name when the folder is at the root
            if (path == ROOT_FLAG) getString(R.string.app_name)
            // Display the folder name only
            else Helper.getFilenameForPath(path)

        // ROOT_FLAG isn't really a path and the file will be inaccessible as it doesn't exist
        if (path != ROOT_FLAG) {
            val dir = File(path)
            if (!dir.canRead())
                activity?.title = "${activity?.title} (inaccessible)"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        listAdapter = context?.let {
            ArrayAdapter(
                it,
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                values
            )
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        updateValues()
        (listAdapter as ArrayAdapter<*>).notifyDataSetChanged()
    }

    private fun updateValues() {
        if (values.isNotEmpty()) values.clear()

        if (path == ROOT_FLAG) {
            // Add root folders to values list
            val externalStorageFiles = ContextCompat.getExternalFilesDirs(context!!, null)
            externalStorageFiles.forEach {
                values.add(
                    it.path.toString()
                        .substringBefore("/Android/data/${context!!.packageName}/files")
                )
            }

        } else {
            val dir = File(path)
            val directories = mutableListOf<String>()
            val files = mutableListOf<String>()

            val list = dir.list()
            list?.let {
                for (file in it)
                    if (!file.startsWith("."))
                        if (!file.contains("."))
                            directories.add(file)
                        else files.add(file)
            }
            // Sorting directories and files separately so that they can be displayed separately
            directories.sortBy { it.toLowerCase(Locale.ROOT) }
            files.sortBy { it.toLowerCase(Locale.ROOT) }
            values.apply {
                addAll(directories)
                addAll(files)
                if (this.isEmpty()) add(getString(R.string.empty_folder_indicator_item_text))
            }
        }
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        super.onListItemClick(l, v, position, id)
        var filename = listAdapter?.getItem(position).toString()

        if (filename == getString(R.string.empty_folder_indicator_item_text)) {
            Toast.makeText(context, R.string.empty_folder_indicator_item_text, Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (path != ROOT_FLAG) // filename is already complete when ROOT_FLAG is the path
            filename = if (path.endsWith(File.separator)) path + filename
            else path + File.separator + filename

        if (File(filename).isDirectory)
            directoryExplorer.onDirectoryClick(filename)
        else startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.fromFile(File(filename)), getMimeType(filename))
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
                getString(R.string.open_using)
            )
        )
    }

    private fun getMimeType(url: String): String = when {
        url.endsWith(".doc") || url.endsWith(".docx") -> "application/msword"
        url.endsWith(".pdf") -> "application/pdf"
        url.endsWith(".ppt") || url.endsWith(".pptx") -> "application/vnd.ms-powerpoint"
        url.endsWith(".xls") || url.endsWith(".xlsx") -> "application/vnd.ms-excel"
        url.endsWith(".zip") || url.endsWith(".rar") -> "application/x-wav"
        url.endsWith(".rtf") -> "application/rtf"
        url.endsWith(".wav") || url.endsWith(".mp3")
                || url.endsWith(".m4a") || url.endsWith(".ogg") -> "audio/x-wav"
        url.endsWith(".gif") -> "image/gif"
        url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".png") -> "image/jpeg"
        url.endsWith(".txt") || url.endsWith(".mht")
                || url.endsWith(".mhtml") || url.endsWith(".html") -> "text/plain"
        url.endsWith(".3gp") || url.endsWith(".mpg")
                || url.endsWith(".mpeg") || url.endsWith(".mpe")
                || url.endsWith(".mp4") || url.endsWith(".avi") -> "video/*"
        else -> "*/*"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(PATH_EXTRA, path)
        super.onSaveInstanceState(outState)
    }

}
