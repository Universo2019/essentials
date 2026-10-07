package com.sameerasw.essentials.domain.controller

import android.content.Context
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.R
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy

/**
 * Starts/stops Wi-Fi tethering through the tethering service over a Shizuku binder.
 * `cmd wifi start-softap` only brings up a bare access point without internet sharing,
 * whereas this goes through the same path as the Settings hotspot toggle.
 */
internal object HotspotController {
    private const val TAG = "HotspotController"
    private const val CONNECTOR = "android.net.ITetheringConnector"
    private const val LISTENER = "android.net.IIntResultListener"
    private const val TETHERING_WIFI = 0

    // Shizuku runs as shell or root, both of which hold TETHER_PRIVILEGED. TetheringService
    // checks the caller package against the uid, and AppOps resolves "root" to uid 0.
    private fun callerPkg() = if (Shizuku.getUid() == 0) "root" else "com.android.shell"

    @RequiresApi(Build.VERSION_CODES.R)
    fun setEnabled(context: Context, enabled: Boolean) {
        val binder = requireNotNull(SystemServiceHelper.getSystemService("tethering")) { "tethering unavailable" }
        val connector =
            Class.forName("$CONNECTOR\$Stub")
                .getMethod("asInterface", IBinder::class.java)
                .invoke(null, ShizukuBinderWrapper(binder))!!
        val listenerClass = Class.forName(LISTENER)
        val listener = resultListener(context.applicationContext, enabled, listenerClass)
        val callerPkg = callerPkg()
        val string = String::class.java
        val int = Int::class.javaPrimitiveType!!

        // Android 12+ added a callingAttributionTag parameter; Android 11 lacks it.
        if (enabled) {
            val request =
                Class.forName("android.net.TetheringManager\$TetheringRequest\$Builder")
                    .getConstructor(int)
                    .newInstance(TETHERING_WIFI)
                    .let { it.javaClass.getMethod("build").invoke(it)!! }
            val parcel = request.javaClass.getMethod("getParcel").invoke(request)!!
            try {
                call(connector, "startTethering", arrayOf(parcel.javaClass, string, string, listenerClass), parcel, callerPkg, null, listener)
            } catch (_: NoSuchMethodException) {
                call(connector, "startTethering", arrayOf(parcel.javaClass, string, listenerClass), parcel, callerPkg, listener)
            }
        } else {
            try {
                call(connector, "stopTethering", arrayOf(int, string, string, listenerClass), TETHERING_WIFI, callerPkg, null, listener)
            } catch (_: NoSuchMethodException) {
                call(connector, "stopTethering", arrayOf(int, string, listenerClass), TETHERING_WIFI, callerPkg, listener)
            }
        }
    }

    private fun call(target: Any, name: String, types: Array<Class<*>>, vararg args: Any?) {
        val method = Class.forName(CONNECTOR).getMethod(name, *types)
        try {
            method.invoke(target, *args)
        } catch (e: InvocationTargetException) {
            throw (e.cause as? Exception ?: e)
        }
    }

    /** An IIntResultListener whose binder reports the tethering result code (0 = success). */
    private fun resultListener(context: Context, enabled: Boolean, listenerClass: Class<*>): Any {
        val verb = if (enabled) "start" else "stop"
        val binder =
            object : Binder() {
                override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
                    if (code != FIRST_CALL_TRANSACTION) return super.onTransact(code, data, reply, flags)
                    data.enforceInterface(LISTENER)
                    val result = data.readInt()
                    if (result == 0) {
                        Log.d(TAG, "Tethering $verb succeeded")
                    } else {
                        Log.w(TAG, "Tethering $verb failed with error $result")
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, context.getString(R.string.hotspot_tethering_failed, result), Toast.LENGTH_LONG).show()
                        }
                    }
                    return true
                }
            }.apply { attachInterface(null, LISTENER) }
        return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(listenerClass)) { proxy, method, args ->
            when (method.name) {
                "asBinder" -> binder
                "hashCode" -> binder.hashCode()
                "equals" -> proxy === args?.firstOrNull()
                "toString" -> LISTENER
                else -> null
            }
        }
    }
}
