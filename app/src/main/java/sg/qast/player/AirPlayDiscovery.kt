package sg.qast.player

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo

/**
 * Read-only AirPlay service discovery via Android NSD/mDNS.
 * This module does NOT implement an AirPlay sender, pairing, or playback.
 */
class AirPlayDiscovery(context: Context) {
    private val manager = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val found = linkedMapOf<String, String>()
    var onDevices: (List<String>) -> Unit = {}
    var onError: (String) -> Unit = {}
    private var active = false

    private val listener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String) = Unit
        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            if (serviceInfo.serviceType.contains("_airplay._tcp")) {
                synchronized(found) {
                    found[serviceInfo.serviceName] = serviceInfo.serviceName
                    onDevices(found.values.sorted())
                }
            }
        }
        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            synchronized(found) {
                found.remove(serviceInfo.serviceName)
                onDevices(found.values.sorted())
            }
        }
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            active = false
            onError("AirPlay discovery could not start ($errorCode)")
            try { manager.stopServiceDiscovery(this) } catch (_: Exception) {}
        }
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            onError("AirPlay discovery stop failed ($errorCode)")
        }
        override fun onDiscoveryStopped(serviceType: String) = Unit
    }

    fun start() {
        if (active) return
        active = true
        try {
            manager.discoverServices("_airplay._tcp.", NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (error: Exception) {
            active = false
            onError("AirPlay discovery unavailable: ${error.message}")
        }
    }

    fun stop() {
        if (!active) return
        active = false
        try { manager.stopServiceDiscovery(listener) } catch (_: Exception) {}
        synchronized(found) {
            found.clear()
            onDevices(emptyList())
        }
    }
}
