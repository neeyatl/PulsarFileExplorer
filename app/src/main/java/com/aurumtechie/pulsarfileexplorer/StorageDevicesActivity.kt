package com.aurumtechie.pulsarfileexplorer

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_storage_devices.*

class StorageDevicesActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE = 4579
    }

    private val values = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_storage_devices)

        // REQUEST permissions

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) { // If permissions are given, update the UI.
            if (savedInstanceState == null) initializeStorageDevicesList()
            // else SavedInstanceState will be used by the os to restore the state
        } else requestExternalStoragePermission()

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
                    initializeStorageDevicesList()
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

    private fun requestPermissionAndOpenSettings() = MaterialAlertDialogBuilder(this)
        .setMessage(R.string.permission_request)
        .setPositiveButton(R.string.show_settings) { dialog, _ ->
            dialog.dismiss()
            // Open application settings to enable the user to toggle the permission settings
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            })
        }.show()

    /** Updates the values list, initializes an adapter to display data in the ListView, and initializes a click listener to respond to user clicks
     * @author Neeyat Lotlikar
     * @see storageDevicesList*/
    private fun initializeStorageDevicesList() {
        // READ the storage data and populate the values list
        readExternalFileDirs()

        // initialize the adapter to display the views
        this@StorageDevicesActivity.storageDevicesList.adapter =
            ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                values
            )

        this.storageDevicesList.setOnItemClickListener { parent, _, position, _ ->
            startActivity(Intent(this, FilesListActivity::class.java).apply {
                putExtra(
                    FilesListActivity.STORAGE_DEVICE_ROOT_PATH_EXTRA,
                    parent.getItemAtPosition(position).toString()
                )
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.refresh_button, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = if (item.itemId == R.id.refresh) {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) updateStorageDevicesList()
        else requestExternalStoragePermission()
        true
    } else super.onOptionsItemSelected(item)

    /** Function updates the List View with the latest values and shows a message to the user
     * @author Neeyat Lotlikar
     * @see storageDevicesList*/
    private fun updateStorageDevicesList() {
        readExternalFileDirs()
        (this.storageDevicesList.adapter as ArrayAdapter<*>).notifyDataSetChanged()
        Snackbar.make(storageDevicesList, R.string.refreshed_successfully, Snackbar.LENGTH_LONG)
            .show()
    }

    /** Function clears the values list and updates it with the latest values
     * @author Neeyat Lotlikar
     * @see values*/
    private fun readExternalFileDirs() {
        if (values.isNotEmpty()) values.clear()
        val externalStorageFiles = ContextCompat.getExternalFilesDirs(this, null)
        externalStorageFiles.forEach {
            values.add(
                it.path.toString()
                    .substringBefore("/Android/data/${this.packageName}/files")
            )
        }
    }

}