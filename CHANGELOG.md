# Version 1.2 (9/18/2013)

+	Minor bug fixes and general code cleanup
+	Improved documentation, including new basic info for how to use with other vehicles.
+	Change: Upgraded the usb-serial-for-android library to latest source (from 2013-03-13). May improve compatibility with some devices. Also allows the library to be used as-is (no longer requires source code modifications). Source is still included only because no updated binary has been released yet.
+	Change: No longer displaying toast popup when app exits due to disconnect
+	Change: Updated default baud rate to most commonly used
+	Bug Fix: No longer needlessly restarts every 30 seconds when status is "monitoring" (this had been backported to master branch already)
+	Bug Fix: Fixed logic errors preventing the exit on disconnect feature from always working
+	Bug Fix: Added additional scantool initialization step to ensure buttons are recognized regardless of previous device config (turn on spaces in bus messages)

# Version 1.1 (3/17/2013)

+	Miscellaneous minor improvements, bug fixes, and project cleanup
+	New Feature: Allows launching app before connecting scantool (will request USB device permissions correctly)
+	New Feature: Ability to specify a device number to connect to in settings (allows leaving first scantool free for other apps when multiple devices are connected)
+	New Feature: Ability to specify a protocol for the scantool in settings
+	New Feature: Ability to specify the monitor command for the scantool in settings

# Version 1.0 (3/16/2013)

+	Initial public release
