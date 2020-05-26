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
import androidx.fragment.app.ListFragment
import java.io.File
import java.util.*

class FilesListFragment : ListFragment() {

    private var path: String = "/storage/emulated/0"

    val currentPath
        get() = path

    companion object {
        private const val PATH_EXTRA = "path_extra"

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

        if (savedInstanceState != null) path = savedInstanceState.getString(PATH_EXTRA, path)

        activity?.title = path

        val dir = File(path)
        if (!dir.canRead())
            activity?.title = "${activity?.title} (inaccessible)"

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

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        super.onListItemClick(l, v, position, id)
        var filename = listAdapter?.getItem(position)

        if (filename == getString(R.string.empty_folder_indicator_item_text)) {
            Toast.makeText(context, R.string.empty_folder_indicator_item_text, Toast.LENGTH_SHORT)
                .show()
            return
        }

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
                || url.endsWith(".m4a") -> "audio/x-wav"
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
