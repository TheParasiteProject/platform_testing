package android.platform.uiautomator_helpers

import android.os.Trace
import android.util.Log

/** Tracing utils until androidx tracing library is updated in the tree. */
internal object TracingUtils {

    // from frameworks/base/core/java/android/os/Trace.java MAX_SECTION_NAME_LEN.
    private const val MAX_TRACE_NAME_LEN = 127
    private const val TAG = "TracingUtils"

    inline fun <T> trace(sectionName: String, block: () -> T): T {
        Trace.beginSection(sectionName.shortenedIfNeeded())
        try {
            return block()
        } finally {
            Trace.endSection()
        }
    }

    private fun String.shortenedIfNeeded(): String =
        if (length > MAX_TRACE_NAME_LEN) {
            Log.w(TAG, "Section name too long: \"$this\" (len=$length, max=$MAX_TRACE_NAME_LEN)")
            substring(0, MAX_TRACE_NAME_LEN)
        } else this
}
