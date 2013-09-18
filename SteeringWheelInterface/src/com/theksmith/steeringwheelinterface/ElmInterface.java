package com.theksmith.steeringwheelinterface;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;

import com.hoho.android.usbserial.driver.FtdiSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;


/**
 * Wraps the serial device with methods to handle specific ELM based device communications.
 * 
 * @author Kristoffer Smith <kristoffer@theksmith.com>
 */
public class ElmInterface {
	protected static final String TAG = ElmInterface.class.getSimpleName();
	
	protected static final String ACTION_USB_PERMISSION = ElmInterface.class.getPackage().getName() + ".USB_PERMISSION";
	
	protected static final int MONITOR_START_WARM_ATTEMPTS = 3;
	protected static final int MONITOR_START_COLD_ATTEMPTS = 3;
	
	protected static final int DEFAULT_COMMAND_TOTAL_TIMEOUT = 2500;
	protected static final int DEFAULT_COMMAND_SEND_TIMEOUT = 250;
	protected static final int DEFAULT_COMMAND_DATA_TIMEOUT = 1000;
	protected static final int DEFAULT_COMMAND_RETRIES = 3;
	
	protected static final int DEFAULT_RESET_COMMAND_TOTAL_TIMEOUT = 5000;
	protected static final int DEFAULT_MONITOR_COMMAND_DATA_TIMEOUT = 5000;

	protected Context mAppContext;
	
    protected List<DeviceOpenEventListener> mDeviceOpenEventListeners = new ArrayList<DeviceOpenEventListener>();

	protected static enum PermissionStatus { NOT_RESPONDED, ERROR, DENIED, GRANTED };
	protected Boolean mUsbPermissionReceiverIsRegistered = false;
	
	protected UsbManager mUsbManager;
	protected UsbSerialDriver mSerialDevice;	
	protected SerialInputOutputManager mSerialIoManager;

	protected String mCommand = "";
	protected String mResponse = "";
	protected int mStartWarmAttempts = 0;
	protected int mStartColdAttempts = 0;
	protected int mCommandTimeoutTotal = 0;
	protected int mCommandTimeoutData = 0;
	protected int mCommandRetries = 0;
	protected int mCommandRetryCounter = 0;
	
	protected int mSettingDeviceNumber = 1;
	protected int mSettingBaud = 115200;
	protected String mSettingProtocolCommand = "ATSP2";	//setting for our original project use in a 2003 Jeep/Chrysler/Dodge
	protected String mSettingMonitorCommand = "ATMR11";	//setting for our original project use in a 2003 Jeep/Chrysler/Dodge
	protected int mStatus = 0;
	protected int mDeviceID = 0;

	protected static final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
	protected static final Handler mCommandTimeoutTotal_Timer = new Handler();	
	protected static final Handler mCommandTimeoutData_Timer = new Handler();
	protected ButtonActions mButtons;
	
	public static final int STATUS_CLOSED = 0;
	public static final int STATUS_CLOSED_FROMERROR = 1;
	public static final int STATUS_OPEN_STOPPED = 2;
	public static final int STATUS_OPEN_STOPPED_FROMERROR = 3;
	public static final int STATUS_OPEN_MONITORING = 4;
	
	
	private ElmInterface() { 
		//exists only to prevent creation of class without passing required param
	}

	
	/**
	 * Constructor.
	 * 
	 * @param appContext	the application context of the creator
	 */
	public ElmInterface(Context appContext) {
		mAppContext = appContext.getApplicationContext();
		mButtons = new ButtonActions(mAppContext);
	}
	
	
	public void setDeviceNumber(int number) {
		mSettingDeviceNumber = number;
	}
	

	public void setBaudRate(int rate) {
		mSettingBaud = rate;
	}
	
	
	public void setProtocolCommand(String command) {
		mSettingProtocolCommand = command;
	}
	
	
	public void setMonitorCommand(String command) {
		mSettingMonitorCommand = command;
	}

	
	public int getsStatus() {
		return mStatus;
	}
    
	
	/**
	 * Called by deviceOpen() to obtain user permission to access the device.
	 */
	protected BroadcastReceiver mUsbPermissionReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						if (device != null) {							
							Log.d(TAG, "PERMISSION FOR SERIAL DEVICE GRANTED");							
							openDeviceFinish(device);
						} else {
							Log.w(TAG, "ERROR OBTAINING SERIAL DEVICE PERMISION - NO DEVICE");
						}
					} else {
						Log.w(TAG, "PERMISSION FOR SERIAL DEVICE DENIED");
					}
				}
			}
		}
	};
		
	
	/**
	 * Finds and opens the serial device.
	 * Optionally call setDeviceNumber() and setBaudRate() prior.
	 * 
	 * @param timeout		Milliseconds to wait for user response to Android permission dialog.
	 */
	public void deviceOpen() {
		mSerialDevice = null;
		
		if (mUsbManager == null) {			
			mUsbManager = (UsbManager)mAppContext.getSystemService(Context.USB_SERVICE);
		}

		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		mAppContext.registerReceiver(mUsbPermissionReceiver, filter);
		mUsbPermissionReceiverIsRegistered = true;
				
		/* connect to nth device per settings
		 * this allows leaving the first device alone for some other apps which
		 * are not multi-device aware and will grab the first one even if it's open 
		 */
		int deviceCounter = 1;
		UsbDevice device = null;
		
		HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();				
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

		List<UsbSerialDriver> deviceDrivers;
		Iterator<UsbSerialDriver> deviceDriverIterator;
		UsbSerialDriver deviceDriver;
		
		//iterate all usb devices
		while(deviceIterator.hasNext() && deviceCounter <= mSettingDeviceNumber) {
			device = deviceIterator.next();
			
			if (device != null) {				
				//probe the device to determine if it has an usb-serial-for-android supported drivers/interfaces
				deviceDrivers = UsbSerialProber.probeSingleDevice(mUsbManager, device);
				deviceDriverIterator = deviceDrivers.iterator();
				
				while(deviceDriverIterator.hasNext()){					
					deviceDriver = deviceDriverIterator.next();
					
					//see if this driver/interface is an FTDI serial interface
					if (deviceDriver instanceof FtdiSerialDriver) {
						if (deviceCounter == mSettingDeviceNumber) {					
							break;
						} else {
							deviceCounter++;
						}
					}
				}
				
				if (deviceCounter == mSettingDeviceNumber) {
					break;
				}
			}
			
			deviceDriver = null;
			device = null;
		}
		
		if (device == null) {
			Log.w(TAG, "COULD NOT FIND SERIAL DEVICE NUMBER: " + mSettingDeviceNumber);	        
		} else {
			if (mUsbManager.hasPermission(device)) {
				openDeviceFinish(device);
			} else {
				//get explicit permissions for device for when app is not launched from a usb connection intent
				PendingIntent devicePermissionIntent = PendingIntent.getBroadcast(mAppContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
				mUsbManager.requestPermission(device, devicePermissionIntent);
			}
        }
	}
	
	
	public void openDeviceFinish(UsbDevice device) {
		try {
			//TODO: update code flow to remove need for use of the deprecated acquire() method
    		mSerialDevice = UsbSerialProber.acquire(mUsbManager, device);
    		
    		if (mSerialDevice != null) {
	        	Log.i(TAG, "SERIAL DEVICE FOUND: " + mSerialDevice);
	        	
	        	mSerialDevice.open();
	        	mSerialDevice.setParameters(mSettingBaud, UsbSerialDriver.DATABITS_8, UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
	        		        	
	        	ioManagerReset();

	        	mStatus = STATUS_OPEN_STOPPED;
	        	
	        	deviceOpenEvent_Fire();
	        	
	        	return;	//this is the only successful exit path for this method
    		} else {
    			Log.w(TAG, "COULD NOT ACQUIRE SERIAL DEVICE NUMBER: " + mSettingDeviceNumber);
    		}
        } catch (Exception ex) {
            Log.e(TAG, "ERROR OPENING DEVICE", ex);
        }
		
		deviceClose();	        
        mStatus = STATUS_CLOSED_FROMERROR;
	}
	
	
	/**
	 * Gracefully closes the current open serial device.
	 * Calls monitorStop(), no need to call prior.
	 */
	public void deviceClose() {
		deviceClose(false);
	}
	
	
	protected void deviceClose(Boolean fromError) {
		commandTimeout_TimersStop();
		
		if (mUsbPermissionReceiverIsRegistered) {
			mAppContext.unregisterReceiver(mUsbPermissionReceiver);
			mUsbPermissionReceiverIsRegistered = false;
		}
		
		try {
    		if (mSerialDevice != null) {
    			monitorStop();    			
    			mSerialDevice.close();
    		}    		
    	} catch (Exception ex) {
    		Log.e(TAG, "ERROR CLOSING SERIAL DEVICE", ex);
    	}
    	 
    	mSerialDevice = null;
    	         
		if (fromError) {
			mStatus = STATUS_CLOSED_FROMERROR;
		} else {
			mStatus = STATUS_CLOSED;
		}
	}
	
	
	private final SerialInputOutputManager.Listener mIoListener = new SerialInputOutputManager.Listener() {
        @Override
        public void onRunError(Exception ex) {
        	//do we care? i.e. probably already seen a related exception elsewhere...
        	//Log.w(TAG, "ERROR WITH SerialInputOutputManager.Listener", ex);            
        }

        @Override
        public void onNewData(final byte[] data) {
        	ElmInterface.this.ioManagerOnReceivedData(data);
        }
    };
    
    
    protected void ioManagerReset() {
    	ioManagerStop();
    	ioManagerStart();
    }
    
    
    protected void ioManagerStop() {
        if (mSerialIoManager != null) {
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }
    

    protected void ioManagerStart() {
        if (mSerialDevice != null) {
            mSerialIoManager = new SerialInputOutputManager(mSerialDevice, mIoListener);
            mExecutor.submit(mSerialIoManager);
        }
    }
	

    public void ioManagerOnReceivedData(byte[] data) {
    	if (mCommandTimeoutData > 0) {
    		commandTimeoutData_TimerReStart(mCommandTimeoutData);
    	}
    	
    	String strdata = new String(data);
    	Log.d(TAG, "DATA RECEIVED: " + strdata);
    	
    	//mResponse will continue to append data till a condition below sees it as a complete response and handles it  
    	mResponse += strdata;

		//this is a catch for when the device resets due to cranking or a hardware error
    	if (mResponse.contains("ELM327") || mResponse.contains("LV RESET")) {
    		//just pretend the command was an intentional reset to re-start the entire command sequence 
    		mCommand = "ATZ";
    	}
    	
    	//for each command, allow response to append data until we see the expected full response 
    	if (mCommand == "ATZ" || mCommand == "ATI") {
    		if (!mResponse.contains(">") || !mResponse.contains("ELM327")) return;    		
    		Log.d(TAG, "ELM DEVICE FOUND");
    		sendCommand("ATE1");
    	} else if (mCommand == "ATE1") {
    		if (!mResponse.contains(">") || !mResponse.contains("OK")) return;
    		Log.d(TAG, "ECHO ON");
    		sendCommand("ATL1");    		
    	} else if (mCommand == "ATL1") {
    		if (!mResponse.contains(mCommand) || !mResponse.contains(">") || !mResponse.contains("OK")) return;
    		Log.d(TAG, "LINE BREAKS ON");
    		sendCommand("ATS1");
    	} else if (mCommand == "ATS1") {
    		if (!mResponse.contains(mCommand) || !mResponse.contains(">") || !mResponse.contains("OK")) return;
    		Log.d(TAG, "SPACES ON");
    		sendCommand("ATH1");
    	} else if (mCommand == "ATH1") {
    		if (!mResponse.contains(mCommand) || !mResponse.contains(">") || !mResponse.contains("OK")) return;    		
    		Log.d(TAG, "HEADERS ON");
    		sendCommand(mSettingProtocolCommand);
    	} else if (mCommand == mSettingProtocolCommand) {
    		if (!mResponse.contains(mCommand) || !mResponse.contains(">") || !mResponse.contains("OK")) return;    		
    		Log.d(TAG, "PROTOCOL SET");
    		sendCommand(mSettingMonitorCommand, 0, DEFAULT_MONITOR_COMMAND_DATA_TIMEOUT, DEFAULT_COMMAND_RETRIES);    	
    	} else if (mCommand == mSettingMonitorCommand) {
    		if (!mResponse.contains("\r") && !mResponse.contains("\n")) return;
    		mButtons.performAction(mResponse.trim());
    		mResponse = "";
    	} else {
    		Log.w(TAG, "UNEXPECTED DATA RECEIVED (WHILE NO COMMAND PENDING): " + mResponse);
    	}
    }
	
	
    /**
     * Begins monitoring the serial device.
     * Must call deviceOpen() prior.
     * Optionally call setProtocolCommand() prior.
     * 
     * @throws Exception
     */
	public void monitorStart() throws Exception {
		if (mStatus != STATUS_CLOSED && mStatus != STATUS_CLOSED_FROMERROR) {
			ioManagerReset();
			
			mStartWarmAttempts = 0;
			mStartColdAttempts = 0;
			monitorStartWarm();			
			
			mStatus = STATUS_OPEN_MONITORING;
		} else {
			Log.w(TAG, "MONITOR START ATTEMPT WHILE DEVICE CLOSED");
			throw new Exception("Cannot start monitoring, device not open.");
		}
	}
	

	/**
	 * Stops monitoring the serial device.
	 * Must have called deviceOpen() and monitorStart() prior.
	 *  
	 * @throws Exception
	 */
	public void monitorStop() throws Exception {
		monitorStop(false);
	}
	
	
	protected void monitorStop(Boolean fromError) throws Exception {
		if (mStatus != STATUS_CLOSED && mStatus != STATUS_CLOSED_FROMERROR) {
			//attempt to send a simple command to make sure any current long running commands are stopped, ignore results
			sendCommandBlind("ATI");
		
			//attempt to send the Low Power Mode command and ignore results (only newer ELM devices support this)
			sendCommandBlind("LP");
		} else {
			Log.w(TAG, "MONITOR STOP ATTEMPT WHILE DEVICE CLOSED");
			throw new Exception("Cannot stop monitoring, device not open.");
		}
		
		ioManagerStop();
		
		if (fromError) {
			mStatus = STATUS_OPEN_STOPPED_FROMERROR;
		} else {
			mStatus = STATUS_OPEN_STOPPED;
		}
	}

	
	protected void monitorStartWarm() {
		if (mStartWarmAttempts < MONITOR_START_WARM_ATTEMPTS) {
			Log.d(TAG, "MONITORING WARM START ATTEMPT: " + mStartWarmAttempts);
	        sendCommand("ATI", DEFAULT_RESET_COMMAND_TOTAL_TIMEOUT, 0, 1);
		} else {
			Log.d(TAG, "MONITORING WARM START - TOO MANY ATTEMPTS");
			monitorStartCold();
		}
	
		mStartWarmAttempts++;
	}
	
	
	protected void monitorStartCold() {
		if (mStartColdAttempts < MONITOR_START_COLD_ATTEMPTS) {
			Log.d(TAG, "MONITORING COLD START ATTEMPT: " + mStartColdAttempts);
			sendCommand("ATZ", DEFAULT_RESET_COMMAND_TOTAL_TIMEOUT, 0, 1);
		} else {
			Log.d(TAG, "MONITORING COLD START - TOO MANY ATTEMPTS");
			try {
				monitorStop(true);
			} catch (Exception ex) { }
		}
		
		mStartColdAttempts++;
	}
	
	
	protected Runnable commandTimeout_TimersRun = new Runnable() {
		public void run() {
			if (ElmInterface.this.mCommandRetryCounter < ElmInterface.this.mCommandRetries) {
				Log.d(ElmInterface.TAG, "COMMAND OR DATA TIMEOUT - RETRYING COMMAND ATTEMPT: " + ElmInterface.this.mCommandRetryCounter);
				ElmInterface.this.sendCommandRetry();			
			} else {
				ElmInterface.this.monitorStartWarm();
			}
		}
	};
	
	
	protected void commandTimeout_TimersStop() {
		commandTimeoutTotal_TimerStop();
		commandTimeoutData_TimerStop();
	}
	
	
	protected void commandTimeoutTotal_TimerStop() {
		mCommandTimeoutTotal_Timer.removeCallbacks(commandTimeout_TimersRun);
	}

	
	protected void commandTimeoutTotal_TimerReStart(int timeout) {
		commandTimeoutTotal_TimerStop();
		mCommandTimeoutTotal_Timer.postDelayed(commandTimeout_TimersRun, timeout);
	}
	
	
	protected void commandTimeoutData_TimerStop() {
		mCommandTimeoutData_Timer.removeCallbacks(commandTimeout_TimersRun);
	}

	
	protected void commandTimeoutData_TimerReStart(int timeout) {
		commandTimeoutData_TimerStop();
		mCommandTimeoutData_Timer.postDelayed(commandTimeout_TimersRun, timeout);
	}
	
	
	/**
	 * Send a command to the serial device with no retries and do not expect response.
	 * 
	 * @param command			A valid ELM AT Command.
	 * @return					Returns false if the command was not written correctly to device.
	 */
	public Boolean sendCommandBlind(String command) {
		return sendCommand(command, 0, 0, 0, false, true);
	}
	
	
	/**
	 * Retry last command with same params.
	 * Must call sendCommand() prior.
	 * 
	 * @return					Returns false if the command was not written correctly to device.
	 */
	public Boolean sendCommandRetry() {
		return sendCommand(mCommand, mCommandTimeoutTotal, mCommandTimeoutData, mCommandRetries, true, false);
	}
	
	
	/**
	 * Send a command to the serial device using the default params.
	 * 
	 * @param command			A valid ELM AT Command.
	 * @return					Returns false if the command was not written correctly to device.
	 */
	public Boolean sendCommand(String command) {
		return sendCommand(command, DEFAULT_COMMAND_TOTAL_TIMEOUT, DEFAULT_COMMAND_DATA_TIMEOUT, DEFAULT_COMMAND_RETRIES, false, false); 
	}
	
	
	/**
	 * Send a command to the serial device.
	 * 
	 * @param command			A valid ELM AT Command.
	 * @param timeoutTotal		Total milliseconds to wait for a complete response before retrying.
	 * @param timeoutData		Milliseconds to wait between partial response fragments before retrying. 
	 * @param retries			Number of times to retry command if a complete response is not received (or if timeouts occur)
	 * @return					Returns false if the command was not written correctly to device.
	 */
	public Boolean sendCommand(String command, int timeoutTotal, int timeoutData, int retries) {
		return sendCommand(command, timeoutTotal, timeoutData, retries, false, false); 
	}

	
	protected Boolean sendCommand(String command, int timeoutTotal, int timeoutData, int retries, Boolean isRetry, Boolean isBlind) {
		commandTimeout_TimersStop();

		mResponse = "";
		mCommand = "";

		command = command.trim();
		Log.d(TAG, "SENDING COMMAND: " + command);
		
		if (isBlind) {
			mCommandTimeoutTotal = 0;
			mCommandTimeoutData = 0;
			
			mCommandRetries = 0;
			mCommandRetryCounter = 0;
		} else {
			mCommandTimeoutTotal = timeoutTotal;
			mCommandTimeoutData = timeoutData;
			
			mCommandRetries = retries;			
			if (isRetry) {
				mCommandRetryCounter++;
			} else {
				mCommandRetryCounter = 0;
			}

			mCommand = command;
			
			if (timeoutTotal > 0) {
				commandTimeoutTotal_TimerReStart(timeoutTotal);
			}			
			if (timeoutData > 0) {
				commandTimeoutData_TimerReStart(timeoutData);
			}
		}
				
		command += "\r";		
		byte[] bytes = command.getBytes();
		int written = 0;

		if (mSerialDevice != null) {
			try {
				written = mSerialDevice.write(bytes, DEFAULT_COMMAND_SEND_TIMEOUT);
			} catch (Exception ex) {
				Log.e(TAG, "ERROR WRITING COMMAND TO DEVICE", ex);
			}
		}
		
		if (written == bytes.length) {
			return true;
		}
		return false;
	}

	
	private synchronized void deviceOpenEvent_Fire() {
		DeviceOpenEvent event = new DeviceOpenEvent(this);
		
		Iterator<DeviceOpenEventListener> listeners = mDeviceOpenEventListeners.iterator();
		while (listeners.hasNext()) {
			listeners.next().onDeviceOpenEvent(event);			
		}
	}
	
	
	public synchronized void deviceOpenEvent_AddListener(DeviceOpenEventListener listener) {
		mDeviceOpenEventListeners.add(listener);
	}
	

	public synchronized void deviceOpenEvent_RemoveListener(DeviceOpenEventListener listener) {
		mDeviceOpenEventListeners.remove(listener);
	}


	public class DeviceOpenEvent extends EventObject {
		private static final long serialVersionUID = 1L;

		public DeviceOpenEvent(Object source) {
	        super(source);
	    }
	}
	

	public interface DeviceOpenEventListener
	{
	    public void onDeviceOpenEvent(DeviceOpenEvent event);
	}
}
