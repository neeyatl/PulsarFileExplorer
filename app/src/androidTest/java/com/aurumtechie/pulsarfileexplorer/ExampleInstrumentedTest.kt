package com.aurumtechie.pulsarfileexplorer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.test.espresso.core.internal.deps.guava.collect.Iterables
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class OpenFolderTest {
    @Test
    fun openFolderUsingAnIntent() {
        val appContext = getInstrumentation().targetContext
        appContext.startActivity(Intent(appContext, FilesListActivity::class.java).apply {
            setDataAndType(Uri.fromFile(File("/storage/emulated/0/Notes")), "resource/folder")
        })
        val currentActivity = getCurrentActivity()
        assertTrue(currentActivity is FilesListActivity)
        val activeFragment =
            (currentActivity as FilesListActivity).supportFragmentManager.findFragmentById(R.id.directoryContainer)
        assertTrue(activeFragment is FilesListFragment)
        assertEquals(
            "/storage/emulated/0/Notes",
            (activeFragment as FilesListFragment).currentPath
        )
    }

    @Throws(Throwable::class)
    fun getCurrentActivity(): Activity? {
        getInstrumentation().waitForIdleSync()
        val activity = arrayOfNulls<Activity>(1)
        val activities =
            ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
        activity[0] = Iterables.getOnlyElement(activities)
        return activity[0]
    }
}
