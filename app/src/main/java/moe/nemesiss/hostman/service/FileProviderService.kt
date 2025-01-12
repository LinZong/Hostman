package moe.nemesiss.hostman.service

import android.content.Context
import android.os.Process
import android.util.Log
import androidx.annotation.Keep
import com.alibaba.fastjson2.toJSONString
import moe.nemesiss.hostman.IFileProvider
import moe.nemesiss.hostman.model.FileOperationResult
import java.io.File
import kotlin.system.exitProcess

class FileProviderService : IFileProvider.Stub {

    private val TAG = "FileProviderService"

    private var context: Context? = null

    @Keep
    constructor() {
        Log.w(TAG, "FileProvider service is creating...")
    }

    @Keep
    constructor(context: Context) {
        Log.w(TAG, "FileProvider service is creating...")
        this.context = context
    }

    override fun getFileTextContent(filePath: String?): String {
        if (filePath == null) {
            throw IllegalArgumentException("filePath should not be null!")
        }

        return File(filePath).readText(Charsets.UTF_8)
    }

    override fun getFileBytes(filePath: String?): ByteArray {
        if (filePath == null) {
            throw IllegalArgumentException("filePath should not be null!")
        }
        return File(filePath).readBytes()
    }

    override fun writeFileBytes(filePath: String?, fileContent: ByteArray?): String {
        if (filePath == null) {
            throw IllegalArgumentException("filePath should not be null!")
        }

        if (fileContent == null) {
            throw IllegalArgumentException("fileContent should not be null!")
        }

        try {
            File(filePath).writeBytes(fileContent)
            Log.w(TAG, "Write content to path: $filePath done!")
            return FileOperationResult(true).toJSONString()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to write content to $filePath", t)
            return FileOperationResult(false, message = t.message ?: "Unknown error", exceptionStack = t.stackTraceToString()).toJSONString()
        }
    }

    override fun getPid(): Int {
        return Process.myPid()
    }

    override fun getUid(): Int {
        return Process.myUid()
    }

    override fun destroy() {
        Log.w(TAG, "FileProvider service is destroying...")
        exitProcess(0)
    }

}