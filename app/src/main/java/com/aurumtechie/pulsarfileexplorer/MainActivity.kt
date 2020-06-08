package com.aurumtechie.pulsarfileexplorer

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity(), FilesListFragment.Companion.DirectoryExplorer {

    companion object {
        private const val READ_REQUEST_CODE = 4579
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
            ) requestReadExternalStoragePermission()
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
        // pop the root directory fragment and then exit the app
        if (supportFragmentManager.backStackEntryCount == 1)
            super.onBackPressed()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    initializeFileExplorer()
                else if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                ) // If permission was denied once before but the user wasn't informed why the permission is necessary, do so.
                    AlertDialog.Builder(this)
                        .setMessage(R.string.read_external_storage_permission_rationale)
                        .setPositiveButton(android.R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                            requestReadExternalStoragePermission()
                        }.show()
                else /* If user has chosen to not be shown permission requests any longer,
                     inform the user about it's importance and redirect her/him to device settings
                     so that permissions can be given */
                    requestPermissionAndOpenSettings()
            }
        }
    }

    private fun requestReadExternalStoragePermission() = ActivityCompat.requestPermissions(
        this,
        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
        READ_REQUEST_CODE
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

}
