package moe.nemesiss.hostman.model

data class AppVersion(val version: String,
                      val release: GithubRelease? = null,
                      val downloadUrl: String? = null) :
        Comparable<AppVersion> {

    private val versionPart = version.split('.')

    private val major get() = versionPart[0].toInt()
    private val minor get() = versionPart[1].toInt()
    private val bugfix get() = versionPart[2].toInt()
    override fun compareTo(other: AppVersion): Int {
        return sequence {
            yield(major.compareTo(other.major))
            yield(minor.compareTo(other.minor))
            yield(bugfix.compareTo(other.bugfix))
        }.firstOrNull { it != 0 } ?: 0
    }
}
