package moe.nemesiss.hostman.service

import com.alibaba.fastjson2.to
import com.alibaba.fastjson2.toJSONString
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.nemesiss.hostman.BuildConfig
import moe.nemesiss.hostman.boost.EasyDebug
import moe.nemesiss.hostman.model.*

object CheckUpdateService {

    private const val TAG = "CheckUpdateService"

    private const val DOWNLOAD_BROWSER_URL = "https://github.com/LinZong/Hostman/releases/latest"

    private const val LATEST_RELEASE_API_URL = "https://api.github.com/repos/LinZong/Hostman/releases/latest"

    suspend fun getLatestGithubRelease(): GithubRelease {
        val response = NetService.httpClient.get(LATEST_RELEASE_API_URL)
        val json = response.bodyAsText(Charsets.UTF_8)
        return json.to<GithubRelease>()
    }

    suspend fun getLatestAppVersion(): AppVersion? {
        try {
            val release = getLatestGithubRelease()
            EasyDebug.info(TAG) { "Latest GitHub release: ${release.toJSONString()}" }
            return AppVersion(release.tag_name, release, DOWNLOAD_BROWSER_URL)
        } catch (t: Throwable) {
            EasyDebug.warn(TAG, t) { "Failed to get latest version, exception occurred." }
            return null
        }
    }

    suspend fun checkNewVersionAvailable(): CheckVersionResult {

        try {
            val latestVersion = withContext(Dispatchers.IO) { getLatestAppVersion() } ?: return NoNewVersionResult
            val currentVersion = BuildConfig.VERSION_NAME.replace(".debug", "")
            if (latestVersion > AppVersion(currentVersion)) {
                EasyDebug.info(TAG) { "New version available: $latestVersion, current version: $currentVersion" }
                return NewVersionAvailableResult(latestVersion)
            }
            EasyDebug.info(TAG) { "No new version available, current version: $currentVersion" }
            return NoNewVersionResult
        } catch (t: Throwable) {
            EasyDebug.warn(TAG, t) { "Failed to check has latest version, exception occurred." }
            return NoNewVersionResult
        }
    }
}