package com.example.service

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

enum class OutputKind { PHONE_SPEAKER, WIRED, BLUETOOTH, USB, OTHER }

data class OutputDevice(
    val kind: OutputKind = OutputKind.PHONE_SPEAKER,
    val label: String = "Phone speaker"
)

/**
 * Works out where audio is currently being sent (phone speaker, wired / bluetooth
 * headphones...) and tells us when that changes (plug in / connect / disconnect).
 */
object AudioOutputMonitor {

    private var callback: AudioDeviceCallback? = null

    fun current(context: Context): OutputDevice {
        val am = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return OutputDevice()
        val outputs = try {
            am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        } catch (e: Exception) {
            emptyList()
        }

        // Priority: Bluetooth > wired > USB > phone speaker
        outputs.firstOrNull { isBluetooth(it.type) }?.let {
            val name = it.productName?.toString()?.trim().orEmpty()
            return OutputDevice(OutputKind.BLUETOOTH, if (name.isNotEmpty()) name else "Bluetooth audio")
        }
        outputs.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_LINE_ANALOG
        }?.let {
            return OutputDevice(OutputKind.WIRED, "Wired headphones")
        }
        outputs.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                it.type == AudioDeviceInfo.TYPE_USB_ACCESSORY
        }?.let {
            return OutputDevice(OutputKind.USB, "USB audio")
        }
        return OutputDevice(OutputKind.PHONE_SPEAKER, "Phone speaker")
    }

    private fun isBluetooth(type: Int): Boolean =
        type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            type == AudioDeviceInfo.TYPE_HEARING_AID ||
            type == 26 || // TYPE_BLE_HEADSET (API 31)
            type == 27 || // TYPE_BLE_SPEAKER (API 31)
            type == 30    // TYPE_BLE_BROADCAST (API 33)

    /** Start watching. Safe to call more than once. */
    fun start(context: Context, onChanged: (OutputDevice) -> Unit) {
        if (callback != null) {
            onChanged(current(context))
            return
        }
        val appContext = context.applicationContext
        val am = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val cb = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                onChanged(current(appContext))
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                onChanged(current(appContext))
            }
        }
        callback = cb
        am.registerAudioDeviceCallback(cb, Handler(Looper.getMainLooper()))
        onChanged(current(appContext))
    }
}
