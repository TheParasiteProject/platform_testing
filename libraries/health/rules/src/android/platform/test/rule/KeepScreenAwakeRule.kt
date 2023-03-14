package android.platform.test.rule

import org.junit.runner.Description

/**
 * Making sure "Stay awake" setting from Developer settings is set so the screen doesn't turn off
 * while tests are running
 *
 * Setting value is bit-based with 4 bits responsible for different types of charging. So the value
 * is device-dependent but non-zero value means the settings is on.
 * See [Settings.STAY_ON_WHILE_PLUGGED_IN] for more information.
 */
class KeepScreenAwakeRule : TestWatcher() {

    override fun starting(description: Description?) {
        val result = executeShellCommand("settings get global stay_on_while_plugged_in").trim()
        if (result == "0") {
            throw AssertionError("'Stay awake' option in developer settings should be enabled")
        }
    }
}
