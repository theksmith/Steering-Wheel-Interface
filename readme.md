# Steering Wheel Interface

An Android application for interfacing with the steering wheel radio
control buttons in certain Jeep/Chrysler/Dodge vehicles.

## Purpose

This project exists primarily as an example of how to interface an
Android device with a vehicle bus using inexpensive ELM327/ELM329 based
OBDII scan tools.

*This is NOT an OBDII or "code reader" type application!* A scan tool is
used only to provide the interface to other vehicle buses which use
OBDII compatible protocols.

The project was set up to be very extensible. Adding support for
BlueTooth based scan tools and other bus protocols is the logical next
step.

## Getting Started

### To test the application as-is you need:

+	And Android device with USB Host support running Android 4.1 (Jelly
	Bean) or newer

	Root access is required for some functionality.
	
	Note: The majority of the functionality only requires
	Android 3.1 (Honeycomb). If you remove or refactor the "settings screen"
	functionality, you should be able to compile and deploy to Honeycomb
	devices.

+	An ELM 327 based USB scan tool
	
	Note: Many of the cheap eBay clones do not implement every protocol 
	correctly and may not work.
	
	This one is known to work:
	[http://www.amazon.com/ScanTool-423001-ElmScan-Diagnostic-Software/dp/
	B002PYBZJO/](http://www.amazon.com/ScanTool-423001-ElmScan-Diagnostic-
	Software/dp/B002PYBZJO/)

+	A USB OTG cable	(unless your device has a native USB Host port)

+	A Jeep/Chrysler/Dodge vehicle with a SAE J1850 VPW base PCI Bus
	(many late 90's through mid 2000's models)

Compile and deploy the application. Plug the scan tool into the OBDII 
diagnostic port and into the Android device through the USB OTG cable. 

A notification should popup saying the Steering Wheel Interface started.
Click the notification for the settings screen where you can adjust the 
baud rate for your particular scan tool.

Press your steering wheel radio control buttons. By default, play/pause, 
next/prev track, volume up/down, and go-to-home-screen actions are
supported. Only the volume actions do not require root, the other actions 
will fail silently on non-rooted devices.

Tested and confirmed working on a Motorola XOOM 4G running the 
CyanogenMod 10 ROM.

### Using with other vehicles:

(documentation pending)

## Contributors

GitHub pull requests are welcome! Please contact me if you'd like to do
a major re-work or extension of the project.

[https://github.com/theksmith/Steering-Wheel-Interface](https://github.com/theksmith/Steering-Wheel-Interface)

## Authors

Kristoffer Smith [http://theksmith.com/](http://theksmith.com/)

## Copyright and License

Copyright 2013 Kristoffer Smith and other contributors

Except for external libraries having with their own license, this
project is licensed under the MIT License.

[http://opensource.org/licenses/MIT](http://opensource.org/licenses/MIT)
