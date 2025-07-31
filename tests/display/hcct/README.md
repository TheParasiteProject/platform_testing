# Hardware Composer Compliance Tester (HCCT)

HCCT is a test suite created by the display team for focused testing of the
Android Hardware Composer (HWC) stack.

This suite goes beyond the coverage provided by existing tests like the Vendor
Test Suite (VTS). It enables more targeted validation of specific HWC
functionalities.

One key capability of HCCT is its use of VKMS (Virtual Kernel Mode Setting),
such as to simulate and listen to hotplug events. This allows for robust testing
of how the HWC stack handles dynamic display connections and disconnections on
multiple custom displays, which can be difficult to test reliably on physical
hardware.
