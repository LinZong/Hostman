package moe.nemesiss.hostman.model

import com.alibaba.fastjson2.to
import moe.nemesiss.hostman.proguard.NoProguard

data class FileOperationResult(val success: Boolean = true,
                               val message: String = "",
                               val exceptionStack: String? = null) : NoProguard {
    companion object {
        fun deserialize(stringifyResult: String): FileOperationResult {
            return stringifyResult.to()
        }
    }
}
