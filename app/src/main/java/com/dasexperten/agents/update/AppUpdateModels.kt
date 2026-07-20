package com.dasexperten.agents.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppUpdateManifest(
    @SerialName("versionCode") val versionCode: Int,
    @SerialName("versionName") val versionName: String = "",
    @SerialName("apkUrl") val apkUrl: String,
    @SerialName("notes") val notes: String = "",
    @SerialName("force") val force: Boolean = false,
)
