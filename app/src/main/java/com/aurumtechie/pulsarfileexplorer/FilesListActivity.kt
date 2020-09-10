package com.aurumtechie.pulsarfileexplorer

import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import java.io.File

class FilesListActivity : AppCompatActivity(), FilesListFragment.Companion.DirectoryExplorer {

    companion object {
        private const val CURRENT_FRAGMENT_KEY = "current_fragment_restore"
        const val STORAGE_DEVICE_ROOT_PATH_EXTRA = "storage_device_root_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_files_list)

        // Initialize: Shows fragment with files and folders from the drive's root
        val path = intent?.getStringExtra(STORAGE_DEVICE_ROOT_PATH_EXTRA)
        path?.let {
            supportFragmentManager.beginTransaction()
                .replace(R.id.directoryContainer, FilesListFragment(it))
                .addToBackStack(null).commit()
        } ?: Toast.makeText(this, "An error: path is null", Toast.LENGTH_LONG).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the state of the fragments so that it can be retrieved on activity recreation
        supportFragmentManager.apply {
            putFragment(outState, CURRENT_FRAGMENT_KEY, findFragmentById(R.id.directoryContainer)!!)
        }
    }

    // Invoked from the fragment. Adds another fragment to the back stack representing the new directory the user clicked on.
    override fun onDirectoryClick(path: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.directoryContainer, FilesListFragment(path))
            .addToBackStack(null).commit()
    }

    override fun onBackPressed() {
        // Exit app onBackPressed when only the root fragment is present in the backStack
        if (supportFragmentManager.backStackEntryCount == 1)
            finish() // Exit the activity
        else {
            super.onBackPressed()
            // Update the title as the path of the current fragment after popping the previous one
            if (supportFragmentManager.backStackEntryCount > 0)
                supportFragmentManager.findFragmentById(R.id.directoryContainer).let {
                    if (it is FilesListFragment)
                        title = Helper.getFilenameForPath(it.currentPath)
                }
        }
    }

    /** Creates a new file in the current folder (function is an onClick function defined in activity layout)
     * @author Neeyat Lotlikar
     * @param view FloatingActionButton View object used to show a message to the user
     * @see com.google.android.material.floatingactionbutton.FloatingActionButton
     * @see R.layout.activity_files_list
     */
    fun addNewFile(view: View) {

        val activeFragment = // Get the fragment which is currently active
            (supportFragmentManager.findFragmentById(R.id.directoryContainer) as FilesListFragment)

        val dir = File(activeFragment.currentPath)
        if (!dir.canWrite()) {
            Snackbar.make(
                view,
                R.string.cannot_write_to_folder,
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        val fileName = "new_${System.currentTimeMillis()}" // Default file name

        PopupMenu(this, view).apply {
            menuInflater.inflate(R.menu.fab_popup_menu, menu)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.add_file -> {
                        val file = File("${dir.absolutePath}${File.separatorChar}$fileName.txt")
                        if (file.createNewFile()) {

                            // Update activeFragmentUI
                            activeFragment.updateListViewItems()

                            Snackbar.make(
                                view,
                                R.string.file_created_successfully,
                                Snackbar.LENGTH_LONG
                            ).setAction(R.string.rename) {
                                activeFragment.renameFile(file)
                            }.show()
                        } else
                            Snackbar.make(
                                view,
                                R.string.file_cannot_be_created,
                                Snackbar.LENGTH_LONG
                            ).show()
                        true
                    }
                    R.id.add_folder -> {
                        val file = File("${dir.absolutePath}${File.separatorChar}$fileName")
                        if (file.mkdir()) {

                            // Update activeFragmentUI
                            activeFragment.updateListViewItems()

                            Snackbar.make(
                                view,
                                R.string.folder_created_successfully,
                                Snackbar.LENGTH_LONG
                            ).setAction(R.string.rename) {
                                activeFragment.renameFile(file)
                            }.show()
                        } else
                            Snackbar.make(
                                view,
                                R.string.folder_cannot_be_created,
                                Snackbar.LENGTH_LONG
                            ).show()
                        true
                    }
                    else -> false
                }
            }
        }.show()
    }

}
