package com.aurumtechie.pulsarfileexplorer

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.File


class MainActivity : AppCompatActivity(), FilesListFragment.Companion.DirectoryExplorer {

    companion object {
        private const val REQUEST_CODE = 4579
        private const val CURRENT_FRAGMENT_KEY = "current_fragment_restore"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) { // If permissions are given, update the UI.
            if (savedInstanceState == null) initializeFileExplorer()
            /* else SavedInstanceState will be used by the fragment manager to restore the state*/
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) requestExternalStoragePermission()
            else requestPermissionAndOpenSettings()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the state of the fragments so that it can be retrieved on activity recreation
        supportFragmentManager.apply {
            putFragment(outState, CURRENT_FRAGMENT_KEY, findFragmentById(R.id.directoryContainer)!!)
        }
    }

    // Add the root directory fragment which is the first screen the user sees.
    private fun initializeFileExplorer() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.directoryContainer, FilesListFragment())
            .addToBackStack(null).commit()
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
            startActivity(Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }).also { finish() }
        else {
            super.onBackPressed()
            // Update the title as the path of the current fragment after popping the previous one
            if (supportFragmentManager.backStackEntryCount > 0)
                supportFragmentManager.findFragmentById(R.id.directoryContainer).let {
                    if (it is FilesListFragment)
                        title = // Show the app name when the folder is at the root
                            if (it.currentPath == FilesListFragment.ROOT_FLAG) getString(R.string.app_name)
                            // Display the folder name only
                            else Helper.getFilenameForPath(it.currentPath)
                }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    initializeFileExplorer()
                else if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                ) // If permission was denied once before but the user wasn't informed why the permission is necessary, do so.
                    MaterialAlertDialogBuilder(this)
                        .setMessage(R.string.external_storage_permission_rationale)
                        .setPositiveButton(android.R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                            requestExternalStoragePermission()
                        }.show()
                else /* If user has chosen to not be shown permission requests any longer,
                     inform the user about it's importance and redirect her/him to device settings
                     so that permissions can be given */
                    requestPermissionAndOpenSettings()
            }
        }
    }


    private fun requestExternalStoragePermission() = ActivityCompat.requestPermissions(
        this,
        arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ),
        REQUEST_CODE
    )

    private fun requestPermissionAndOpenSettings() = AlertDialog.Builder(this)
        .setMessage(R.string.permission_request)
        .setPositiveButton(R.string.show_settings) { dialog, _ ->
            dialog.dismiss()
            // Open application settings to enable the user to toggle the permission settings
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            })
        }.show()


    /** Creates a new file in the current folder (function is an onClick function defined in activity layout)
     * @author Neeyat Lotlikar
     * @param view FloatingActionButton View object used to show a message to the user
     * @see com.google.android.material.floatingactionbutton.FloatingActionButton
     * @see R.layout.activity_main
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

        val fileName = "New File ${System.currentTimeMillis()}" // Default file name

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
    }

}
