package moe.nemesiss.hostman.model

import android.os.IBinder

data class ServiceRef<T>(val binder: IBinder, val service: T) {

    fun pingBinder() = binder.pingBinder()
}
