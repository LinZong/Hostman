package moe.nemesiss.hostman.service

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import java.io.Closeable

object NetService : Closeable {


    val httpClient = HttpClient(CIO)


    override fun close() {
        httpClient.close()
    }
}