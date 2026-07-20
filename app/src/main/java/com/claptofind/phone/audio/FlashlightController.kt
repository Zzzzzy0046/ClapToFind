package com.claptofind.phone.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Controls the camera flashlight for alerts.
 * Requires CAMERA and FLASHLIGHT permissions.
 */
class FlashlightController(private val context: Context) {

    companion object {
        private const val TAG = "FlashlightController"
    }

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraId: String? = null
    @Volatile private var isOn = false
    @Volatile private var availableChecked = false
    @Volatile private var availableResult = false

    fun isAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false

        // Check CAMERA permission at runtime
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "CAMERA permission not granted, flashlight unavailable")
            return false
        }

        // Cache the result to avoid repeated camera enumeration
        if (availableChecked) return availableResult

        availableResult = try {
            cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            cameraId != null
        } catch (e: CameraAccessException) {
            Log.w(TAG, "Camera access error: ${e.message}")
            false
        }
        availableChecked = true
        return availableResult
    }

    fun turnOn() {
        if (isOn) return
        if (!isAvailable()) {
            Log.w(TAG, "Flashlight not available, cannot turn on")
            return
        }
        try {
            cameraId?.let { id ->
                cameraManager.setTorchMode(id, true)
                isOn = true
            }
        } catch (e: CameraAccessException) {
            Log.w(TAG, "Failed to turn on flashlight: ${e.message}")
        } catch (e: SecurityException) {
            Log.w(TAG, "Security exception — CAMERA permission denied")
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Invalid camera ID: ${e.message}")
        }
    }

    fun turnOff() {
        if (!isOn) return
        try {
            cameraId?.let { id ->
                cameraManager.setTorchMode(id, false)
                isOn = false
            }
        } catch (e: CameraAccessException) {
            Log.w(TAG, "Failed to turn off flashlight: ${e.message}")
        } catch (e: SecurityException) {
            Log.w(TAG, "Security exception turning off flashlight")
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Invalid camera ID turning off: ${e.message}")
        }
    }

    fun release() {
        turnOff()
    }
}
