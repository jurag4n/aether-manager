package dev.aether.manager.util

data class DeviceInfo(
    val model     : String  = "Unknown Device",
    val android   : String  = "?",
    val kernel    : String  = "?",
    val selinux   : String  = "?",
    val rootType  : String  = "Unknown",
    val soc       : SocType = SocType.OTHER,
    val socRaw    : String  = "",
    val pid       : String  = "",
    val profile   : String  = "balance",
    val safeMode  : Boolean = false,
    val bootCount : Int     = 0,
)

enum class SocType(val label: String) {
    SNAPDRAGON("Snapdragon"),
    MEDIATEK  ("MediaTek"),
    EXYNOS    ("Exynos"),
    KIRIN     ("Kirin"),
    OTHER     ("Universal")
}
