# DISCONTINUED (8/2014)

Work on this project has been discontinued. It has been replaced with a new project named **CarBusInterface**: 

https://github.com/theksmith/CarBusInterface 

Everyone should move to the new project unless you require a USB based scantool (the new one uses Bluetooth scantools only thus far). The CarBusInterface project was created from scratch to be much more flexible and bug-free. It was also written observing more modern Android development best-practices.


# Steering Wheel Interface

Android application for interfacing with the steering wheel radio control buttons in certain Jeep/Chrysler/Dodge vehicles.

## Purpose

This project exists primarily as an example of how to interface an Android device with a vehicle bus using an inexpensive ELM327 based OBDII scantool.

This is NOT an OBDII or "code reader" type application! A scantool is used only to provide the interface to other vehicle buses that happen to also use OBDII standard protocols (SAE J1850 PWM, SAE J1850 VPW, ISO 9141-2, ISO 14230-4 KWP, ISO 15765-4 CAN, SAE J1939 CAN).

The application is not likely useful to many people exactly as-is, but is designed to be extended to work with your specific vehicle/scenario. Note that only USB scantools are currently supported, though adding BlueTooth should be relatively simple.

## Getting Started

DISCLAIMER: EVERYTHING INCLUDED IN THIS REPOSITORY IS FOR INFORMATION PURPOSES ONLY. I AM NOT RESPONSIBLE FOR ANY DAMAGE TO YOU, YOUR VEHICLE, AND/OR DEVICES. DO NOT ATTEMPT ANY OF THE FOLLOWING UNLESS YOU KNOW WHAT YOU ARE DOING.

### To test the application as-is:

Requirements:

+	An Android device with USB Host support running Android 4.1 or newer.
	
	NOTE: The majority of the functionality only requires Android 3.1. If you remove or refactor the "settings screen" functionality, you should be able to compile and deploy to Honeycomb devices.
	
	Root access is required for some functionality.

+	An ELM327 based USB scantool
	
	NOTE: Many of the cheap eBay clones do not implement every protocol correctly and therefore may not work. [This one](http://www.amazon.com/ScanTool-423001-ElmScan-Diagnostic-Software/dp/B002PYBZJO/) is known to work.

+	A USB OTG cable	(or an Android device with native USB Host port)

+	A Jeep/Chrysler/Dodge vehicle with a SAE J1850 VPW base PCI Bus (many late 90's through mid 2000 models)

Compile and deploy the application. Plug the scantool into the vehicle via the OBDII diagnostic port and into the Android device through the USB OTG cable. 

Respond to the Android prompt regarding a USB device being attached by selecting this app to launch. It is recommended you select the "always" option. NOTE: If you already have a default application for USB-Serial devices, you will need to clear that setting to ever see this prompt.

A notification should be seen saying the Steering Wheel Interface started. Click the notification for the settings screen where you can adjust the baud rate for your particular scantool. IMPORTANT: You will need to restart the app for the new baud rate (and some other settings) to take effect!

Press your steering wheel radio control buttons. By default, play/pause, next/prev track, volume up/down, and go-to-home-screen actions are supported. IMPORTANT: Only the volume actions do not require root, the other actions will fail silently on non-rooted devices!

Tested and confirmed working on a Motorola XOOM 4G running the CyanogenMod 10 ROM (Android 4.1) with a 2003 Jeep Grand Cherokee. UPDATE: Confirmed working in CM 10.1 (Android 4.2) as well.

### Using with other vehicle years/makes/models:

+	Step 1: Determine how to interface (via USB scantool) with the "comfort bus" or similar in your vehicle. This may involve modifications to the scantool and/or vehicle wiring.

	NOTE: Many older vehicles have their buses interconnected, thereby allowing you to simply connect to the OBDII port. Most newer vehicles isolate the Diagnostic Bus (on the OBDII port). For those vehicles you would have to splice into another bus directly. Extra pins on the OBDII port may expose additional buses. Also the radio harness typically exposes the comfort bus. Get a factory service manual with wiring diagrams or Google and hope someone else has hacked on your specific vehicle year/make/model.

	See [this article](http://theksmith.com/technology/hack-vehicle-bus-cheap-easy-part-1/) for more info.

+	Step 2: Determine what bus messages are sent when interacting with the factory device you wish to monitor. See [this article](http://theksmith.com/technology/hack-vehicle-bus-cheap-easy-part-2/) for an example how-to.

+	Step 3: Adjust the app as needed based on your findings. In particular, the BUTTON_* declarations in ButtonActions.java, the "Scantool Protocol" in settings, and the "Scantool Monitor Command".

	If all messages that you wish to monitor for are sent from or to a particular device id, use ATMT## or ATMR## for the monitor command. See the ELM327 datasheet for more info. You could use ATMA instead to monitor all bus messages, though this could affect performance.

## Copyright and License

Copyright 2013 Kristoffer Smith and other contributors.

Except for external references, this project is licensed under the MIT License.

[http://opensource.org/licenses/MIT](http://opensource.org/licenses/MIT)

## Contributing

Pull requests are welcome! Please contact me if you'd like to do a major re-work or extension of the project.

[https://github.com/theksmith/Steering-Wheel-Interface](https://github.com/theksmith/Steering-Wheel-Interface)

## Credits

### Authors

+	Kristoffer Smith ([http://theksmith.com/](http://theksmith.com/))

### Contributors

+	none yet

### References & Dependencies

+	This project uses the usb-serial-for-android library. Source code is included instead of the binary as some of the latest unreleased features were needed. The source has not been modified for this release. Note that this library is licensed under LGPL Version 2.1.

	[https://github.com/mik3y/usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android)
