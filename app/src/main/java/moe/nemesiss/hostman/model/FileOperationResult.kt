package moe.nemesiss.hostman.model

import com.alibaba.fastjson2.JSON
import moe.nemesiss.hostman.proguard.NoProguard

data class FileOperationResult(val success: Boolean = true,
                               val message: String = "",
                               val exceptionStack: String? = null) : NoProguard {
    companion object {
        fun deserialize(stringifyResult: String): FileOperationResult {
            return JSON.parseObject(stringifyResult, FileOperationResult::class.java)
        }
    }
}
