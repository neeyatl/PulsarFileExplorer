package com.aurumtechie.pulsarfileexplorer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.fragment.app.ListFragment
import com.aurumtechie.pulsarfileexplorer.Helper.getMimeType
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.util.*

/** ListFragment class to display all the files and folders present inside a folder
 * @author Neeyat Lotlikar */
class FilesListFragment(private var path: String) : ListFragment(),
    AdapterView.OnItemLongClickListener {
    val currentPath
        get() = path

    companion object {
        private const val PATH_EXTRA = "path_extra"

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

        activity?.title = Helper.getFilenameForPath(path)

        val dir = File(path)
        if (!dir.canRead()) activity?.title = "${activity?.title} (inaccessible)"
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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(PATH_EXTRA, path)
        super.onSaveInstanceState(outState)
    }

    @SuppressLint("InflateParams")
    override fun onItemLongClick(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
    ): Boolean {

        val clickedFile = File(
            currentPath + File.separator + listAdapter?.getItem(position).toString()
        )

        context?.let { context ->
            val bottomSheetDialog = BottomSheetDialog(context)
            val sheetView = activity?.layoutInflater
                ?.inflate(R.layout.bottom_sheet_dialog_options, null)
            if (sheetView != null)
                bottomSheetDialog.setContentView(sheetView)
            bottomSheetDialog.show()

            sheetView?.findViewById<ImageView>(R.id.rename_text_view)
                ?.setOnClickListener {
                    bottomSheetDialog.dismiss()
                    renameFile(clickedFile)
                }

            sheetView?.findViewById<ImageView>(R.id.delete_text_view)
                ?.setOnClickListener {
                    bottomSheetDialog.dismiss()
                    requestFileDeletion(clickedFile)
                }

            sheetView?.findViewById<ImageView>(R.id.share_text_view)
                ?.setOnClickListener {
                    bottomSheetDialog.dismiss()
                    startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                setDataAndType(
                                    Uri.fromFile(clickedFile),
                                    getMimeType(clickedFile.name)
                                )
                                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(clickedFile))
                            },
                            getString(R.string.share_using)
                        )
                    )
                }
        }
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
            .setTitle(
                "${if (file.isDirectory) getString(R.string.delete_folder) else getString(R.string.delete_file)} " +
                        "\"${file.name}\""
            )
            .setMessage(
                if (file.isDirectory)
                    file.list()
                        ?.fold(0) { acc, path -> acc + if (File(file.absolutePath + File.separator + path).isDirectory) 1 else 0 }
                        .toString() + " folders and "
                            + file.list()
                        ?.fold(0) { acc, path -> acc + if (File(file.absolutePath + File.separator + path).isFile) 1 else 0 }
                        .toString() + " files are present. " + getString(R.string.directory_delete_warning)
                else getString(R.string.are_you_sure)
            )
            .setPositiveButton(android.R.string.yes) { dialog, _ ->
                dialog.dismiss()

                if (file.isFile)
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
                else if (file.isDirectory)
                    if (file.deleteRecursively())
                        Snackbar.make(
                            listView,
                            R.string.folder_deleted_successfully,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    else Snackbar.make(
                        listView,
                        R.string.recursive_folder_deletion_error,
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

        if (!file.canWrite()) {
            Snackbar.make(
                listView,
                R.string.file_cannot_be_renamed,
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        val fileNameEditText =
            (layoutInflater.inflate(R.layout.rename_edittext, null, false) as EditText).apply {
                setText(file.name)
            }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.rename)
            .setView(fileNameEditText)
            .setPositiveButton(R.string.save) { dialog, _ ->
                dialog.dismiss()

                val inputFileName = fileNameEditText.text.toString().trim()
                if (inputFileName.isEmpty() || inputFileName.isBlank()) {
                    Snackbar.make(
                        listView,
                        R.string.name_cannot_be_empty,
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                if (inputFileName.startsWith('.')) {
                    Snackbar.make(
                        listView,
                        R.string.name_cannot_start_with_a_dot,
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val finalFileName =
                    StringBuilder(currentPath + File.separator + inputFileName).apply {
                        // Make it a txt file by default if it doesn't contain a file extension defined by the user
                        if (file.isFile && !inputFileName.contains('.')) append(".txt")
                    }.toString()

                File(currentPath).listFiles()?.forEach {
                    if (it.absolutePath == finalFileName) {
                        Snackbar.make(listView, R.string.file_already_exists, Snackbar.LENGTH_SHORT)
                            .show()
                        return@setPositiveButton
                    }
                }

                if (file.renameTo(File(finalFileName)))
                    Snackbar.make(
                        listView,
                        if (file.isFile) R.string.file_saved_successfully
                        else R.string.folder_saved_successfully,
                        Snackbar.LENGTH_SHORT
                    ).show()
                else Snackbar.make(
                    listView,
                    if (file.isFile) R.string.file_cannot_be_renamed
                    else R.string.folder_cannot_be_renamed,
                    Snackbar.LENGTH_SHORT
                ).show()

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
