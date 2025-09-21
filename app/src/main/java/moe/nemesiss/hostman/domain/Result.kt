package moe.nemesiss.hostman.domain

data class Result<T>(val success: Boolean = true,
                     val data: T? = null,
                     val errorCode: String? = null,
                     val errorMsg: String? = null,
                     val stacktrace: String? = null)
