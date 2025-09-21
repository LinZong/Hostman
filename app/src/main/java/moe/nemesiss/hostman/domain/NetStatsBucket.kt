package moe.nemesiss.hostman.domain


class NetStatsBucket {
    var uid: Int = 0
    var tag: Int = 0
    var state: Int = 0
    var defaultNetworkStatus: Int = 0
    var metered: Int = 0
    var roaming: Int = 0
    var beginTimeStamp: Long = 0
    var endTimeStamp: Long = 0
    var rxBytes: Long = 0
    var rxPackets: Long = 0
    var txBytes: Long = 0
    var txPackets: Long = 0
}
