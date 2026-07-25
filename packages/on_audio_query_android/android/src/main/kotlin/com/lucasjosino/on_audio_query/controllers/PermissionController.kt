package com.lucasjosino.on_audio_query.controllers

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lucasjosino.on_audio_query.PluginProvider
import com.lucasjosino.on_audio_query.interfaces.PermissionManagerInterface
import io.flutter.Log
import io.flutter.plugin.common.PluginRegistry

class PermissionController : PermissionManagerInterface,
    PluginRegistry.RequestPermissionsResultListener {

    companion object {
        private const val TAG: String = "PermissionController"

        private const val REQUEST_CODE: Int = 88560

        internal fun requiredPermissionsForSdk(sdkInt: Int): Array<String> {
            return when {
                sdkInt >= Build.VERSION_CODES.TIRAMISU -> {
                    arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
                }
                sdkInt >= Build.VERSION_CODES.M -> {
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                else -> emptyArray()
            }
        }

        internal fun arePermissionResultsGranted(
            requestedPermissions: Array<String>,
            grantResults: IntArray
        ): Boolean {
            if (requestedPermissions.isEmpty()) return true
            return grantResults.size == requestedPermissions.size &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        }
    }

    var retryRequest: Boolean = false

    private val permissions: Array<String> = requiredPermissionsForSdk(Build.VERSION.SDK_INT)

    override fun permissionStatus(): Boolean = permissions.all {
        // After "leaving" this class, context will be null so, we need this context argument to
        // call the [checkSelfPermission].
        return ContextCompat.checkSelfPermission(
            PluginProvider.context(),
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun requestPermission() {
        Log.d(TAG, "Requesting permissions.")
        Log.d(TAG, "SDK: ${Build.VERSION.SDK_INT}, Should retry request: $retryRequest")
        if (permissions.isEmpty()) {
            PluginProvider.result().success(true)
            return
        }

        val activity = PluginProvider.activity()
        ActivityCompat.requestPermissions(activity, permissions, REQUEST_CODE)
    }

    // Second requestPermission, this one with the option "Never Ask Again".
    override fun retryRequestPermission(): Boolean {
        val activity = PluginProvider.activity()
        val shouldRetry = permissions.any {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
        if (!shouldRetry) return false

        Log.d(TAG, "Retrying permission request")
        retryRequest = false
        requestPermission()
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        // When the incoming request code doesn't match the request codes defined by the on_audio_query
        // plugin return [false] to indicate the [on_audio_query] plugin is not handling the request
        // result and Android should continue executing other registered handlers.
        if (REQUEST_CODE != requestCode) return false

        // Check permission
        val isPermissionGranted = arePermissionResultsGranted(
            this.permissions,
            grantResults
        )

        Log.d(TAG, "Permission accepted: $isPermissionGranted")

        // After all checks, we can handle the permission request.
        val result = PluginProvider.result()
        when {
            isPermissionGranted -> result.success(true)
            retryRequest && retryRequestPermission() -> Unit
            else -> result.success(false)
        }

        // Return [true] here to indicate that the [on_audio_query] plugin handled the permission request
        // result and Android should not continue executing other registered handlers.
        return true
    }
}
