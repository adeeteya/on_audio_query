package com.lucasjosino.on_audio_query.controllers

import android.Manifest
import android.content.pm.PackageManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionControllerTest {

    @Test
    fun `runtime permissions are empty before API 23`() {
        assertArrayEquals(
            emptyArray<String>(),
            PermissionController.requiredPermissionsForSdk(16)
        )
        assertArrayEquals(
            emptyArray<String>(),
            PermissionController.requiredPermissionsForSdk(22)
        )
    }

    @Test
    fun `runtime permission is read external storage from API 23 through 32`() {
        val expected = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

        assertArrayEquals(expected, PermissionController.requiredPermissionsForSdk(23))
        assertArrayEquals(expected, PermissionController.requiredPermissionsForSdk(32))
    }

    @Test
    fun `runtime permission is audio only from API 33`() {
        assertArrayEquals(
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
            PermissionController.requiredPermissionsForSdk(33)
        )
    }

    @Test
    fun `empty permission request is already granted`() {
        assertTrue(
            PermissionController.arePermissionResultsGranted(
                emptyArray(),
                intArrayOf()
            )
        )
    }

    @Test
    fun `all requested permission results must be granted`() {
        val requested = arrayOf("permission.one", "permission.two")

        assertTrue(
            PermissionController.arePermissionResultsGranted(
                requested,
                intArrayOf(
                    PackageManager.PERMISSION_GRANTED,
                    PackageManager.PERMISSION_GRANTED
                )
            )
        )
        assertFalse(
            PermissionController.arePermissionResultsGranted(
                requested,
                intArrayOf(
                    PackageManager.PERMISSION_GRANTED,
                    PackageManager.PERMISSION_DENIED
                )
            )
        )
        assertFalse(
            PermissionController.arePermissionResultsGranted(
                requested,
                intArrayOf(PackageManager.PERMISSION_GRANTED)
            )
        )
    }
}
