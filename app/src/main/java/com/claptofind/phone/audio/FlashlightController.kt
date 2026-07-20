package com.claptofind.phone.audio

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build

/**
 * Controls the camera flashlight for alerts.
 * Requires CAMERA and FLASHLIGHT permissions.
 */
class FlashlightController(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraId: String? = null
    private var isOn = false

    fun isAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        return try {
            cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            cameraId != null
        } catch (e: CameraAccessException) {
            false
        }
    }

    fun turnOn() {
        if (isOn) return
        try {
            cameraId?.let { id ->
                cameraManager.setTorchMode(id, true)
                isOn = true
            }
        } catch (_: CameraAccessException) {
        } catch (_: IllegalArgumentException) {
        }
    }

    fun turnOff() {
        if (!isOn) return
        try {
            cameraId?.let { id ->
                cameraManager.setTorchMode(id, false)
                isOn = false
            }
        } catch (_: CameraAccessException) {
        } catch (_: IllegalArgumentException) {
        }
    }

    fun release() {
        turnOff()
    }
}
