package top.wkbin.taixu.ui.manju

/**
 * 漫剧产物的路径契约。
 *
 * 这里必须区分两套路径，混用会导致真机必然失败：
 *  - [hostWorkDir]：宿主（Android 应用）侧路径，出图/配音写文件走它；
 *  - [guestWorkDir]：PRoot 沙盒内路径，ffmpeg 在沙盒里执行，只认它。
 *
 * 二者指向同一份数据（`/workspace` 是 PRoot bind mount，宿主与 guest 共享），
 * 但字符串不同：宿主是 `<filesDir>/workspace/...`，guest 是 `/workspace/...`。
 */
data class ManjuPaths(
    val hostWorkDir: String,
    val guestWorkDir: String,
) {
    /** 宿主路径 → guest 路径；不在工作目录内则原样返回。 */
    fun toGuest(hostPath: String): String =
        if (hostPath.startsWith(hostWorkDir)) {
            guestWorkDir + hostPath.removePrefix(hostWorkDir)
        } else {
            hostPath
        }

    fun hostFile(name: String): String = "$hostWorkDir/$name"

    companion object {
        /** guest 侧漫剧产物目录（位于沙盒 /workspace 下）。 */
        const val GUEST_MANJU_DIR = "/workspace/manju"

        /** 宿主侧相对 workspace 的子目录名。 */
        const val RELATIVE_DIR = "manju"
    }
}
