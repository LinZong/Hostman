package moe.nemesiss.hostman.model

import moe.nemesiss.hostman.proguard.NoProguard


data class GithubRelease(
    val url: String,
    val assets_url: String,
    val upload_url: String,
    val html_url: String,
    val id: Long,
    val node_id: String,
    val tag_name: String,
    val target_commitish: String,
    val name: String,
    val draft: Boolean,
    val prerelease: Boolean,
    val created_at: String,
    val published_at: String,
    val assets: List<Asset>,
    val tarball_url: String,
    val zipball_url: String,
    val body: String
) : NoProguard


data class Asset(
    val url: String,
    val id: Long,
    val node_id: String,
    val name: String,
    val label: String?,
    val content_type: String,
    val state: String,
    val size: Long,
    val download_count: Int,
    val created_at: String,
    val updated_at: String,
    val browser_download_url: String
) : NoProguard
