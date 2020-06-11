package com.aurumtechie.pulsarfileexplorer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.fragment.app.ListFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.util.*

/** ListFragment class to display all the files and folders present inside a folder
 * @author Neeyat Lotlikar */
class FilesListFragment(private var path: String = ROOT_FLAG) : ListFragment(),
    AdapterView.OnItemLongClickListener {
    val currentPath
        get() = path

    companion object {
        private const val PATH_EXTRA = "path_extra"

        const val ROOT_FLAG = "root_path"

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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        listView.onItemLongClickListener = this
    }

    override fun onStart() {
        super.onStart()
        updateListViewItems()
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

    /** Updates values and then notifies the ArrayAdapter to update the UI
     * @author Neeyat Lotlikar */
    fun updateListViewItems() {
        updateValues()
        (listAdapter as ArrayAdapter<*>).notifyDataSetChanged()
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        super.onListItemClick(l, v, position, id)
        var filename = listAdapter?.getItem(position).toString()

        if (filename == getString(R.string.empty_folder_indicator_item_text)) {
            Snackbar.make(v, R.string.empty_folder_indicator_item_text, Snackbar.LENGTH_SHORT)
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

    /** Function to get the MimeType from a filename by comparing it's file extension
     * @author Neeyat Lotlikar
     * @param filename String name of the file. Can also be a path.
     * @return String MimeType */
    private fun getMimeType(filename: String): String = when (filename.subSequence(
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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(PATH_EXTRA, path)
        super.onSaveInstanceState(outState)
    }


    override fun onItemLongClick(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
    ): Boolean {
        requestFileDeletion(
            File(
                currentPath + File.separator + listAdapter?.getItem(position).toString()
            )
        )
        return true
    }

    /** Takes user input for file deletion and deletes the file if it can be deleted. Prompts the user with the result.
     * @author Neeyat Lotlikar
     * @param file File object of the file to be deleted */
    private fun requestFileDeletion(file: File) {
        if (!file.canWrite()) {
            Snackbar.make(
                listView,
                R.string.file_cannot_be_deleted,
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.delete_file)
            .setMessage(if (file.isDirectory) R.string.directory_delete_warning else R.string.are_you_sure)
            .setPositiveButton(android.R.string.yes) { dialog, _ ->
                dialog.dismiss()

                if (file.delete())
                    Snackbar.make(
                        listView,
                        R.string.file_deleted_successfully,
                        Snackbar.LENGTH_SHORT
                    ).show()
                else Snackbar.make(
                    listView,
                    R.string.file_cannot_be_deleted,
                    Snackbar.LENGTH_SHORT
                ).show()

                updateListViewItems()
            }.setNegativeButton(android.R.string.no) { dialog, _ ->
                dialog.dismiss()
            }.show()
    }


    /** Takes user input for the file name. Renames the file if it can be renamed. Prompts the user with the result.
     * @author Neeyat Lotlikar
     * @param file File object of the file to be renamed*/
    fun renameFile(file: File) {
        val fileNameEditText = EditText(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(10)
            hint = getString(R.string.enter_file_name)
            inputType = InputType.TYPE_CLASS_TEXT
        }

        val dialog = MaterialAlertDialogBuilder(context).setCustomTitle(fileNameEditText)
            .setPositiveButton(R.string.save) { dialog, _ ->
                dialog.dismiss()

                val inputFileName = fileNameEditText.text.toString().trim()
                if (inputFileName.isEmpty() || inputFileName.isBlank()) {
                    Snackbar.make(
                        listView,
                        R.string.filename_cannot_be_empty,
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                if (inputFileName.startsWith('.')) {
                    Snackbar.make(
                        listView,
                        R.string.filename_cannot_start_with_a_dot,
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val finalFileName = "$currentPath${File.separator}$inputFileName.txt"

                File(currentPath).listFiles()?.forEach {
                    if (it.absolutePath == finalFileName) {
                        Snackbar.make(listView, R.string.file_already_exists, Snackbar.LENGTH_SHORT)
                            .show()
                        return@setPositiveButton
                    }
                }

                if (file.renameTo(File(finalFileName)))
                    Snackbar.make(listView, R.string.file_saved_successfully, Snackbar.LENGTH_SHORT)
                        .show()
                else Snackbar.make(listView, R.string.file_cannot_be_renamed, Snackbar.LENGTH_SHORT)
                    .show()

                updateListViewItems()
            }.create()
        dialog.show()
        dialog.window?.apply { // After the window is created, get the SoftInputMode
            clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
    }

}
