package moe.nemesiss.hostman.model

sealed class CheckVersionResult(val newVersionAvailable: Boolean)

data class NewVersionAvailableResult(val latestVersion: AppVersion) : CheckVersionResult(true)

data object NoNewVersionResult : CheckVersionResult(false)


