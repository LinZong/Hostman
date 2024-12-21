package moe.nemesiss.hostman.service

import java.io.Closeable

object ModuleService : Closeable {

    val netService = NetService

    val checkUpdateService = CheckUpdateService
    override fun close() {
        netService.close()
    }
}