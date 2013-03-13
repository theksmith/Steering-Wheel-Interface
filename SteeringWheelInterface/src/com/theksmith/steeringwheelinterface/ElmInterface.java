package com.theksmith.steeringwheelinterface;

import com.theksmith.steeringwheelinterface.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;


/**
 * interface to the vehicle
 * wraps the serial device with methods to handle our ELM specific interface requirements
 * 
 * @author Kristoffer Smith <stuff@theksmith.com>
 */

public class ElmInterface {
	protected static final String TAG = ElmInterface.class.getSimpleName();
	
	protected static final int MONITOR_START_WARM_ATTEMPTS = 3;
	protected static final int MONITOR_START_COLD_ATTEMPTS = 3;
	
	protected static final int DEFAULT_COMMAND_TOTAL_TIMEOUT = 2500;
	protected static final int DEFAULT_COMMAND_SEND_TIMEOUT = 250;
	protected static final int DEFAULT_COMMAND_DATA_TIMEOUT = 1000;
	protected static final int DEFAULT_COMMAND_RETRIES = 3;

	protected Context mAppContext;

	protected UsbSerialDriver mSerialDevice;
	protected UsbManager mUsbManager;
	protected SerialInputOutputManager mSerialIoManager;

	protected int mStartWarmAttempts = 0;
	protected int mStartColdAttempts = 0;
	
	protected int mStatus = 0;
	protected String mCommand = "";
	protected String mResponse = "";
	
	protected int mCommandTimeoutTotal = 0;
	protected int mCommandTimeoutData = 0;
	protected int mCommandRetries = 0;
	protected int mCommandRetryCounter = 0;

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
	 * constructor
	 * @param appContext	the application context of the creator
	 */
	public ElmInterface(Context appContext) {
		mAppContext = appContext.getApplicationContext();
		mButtons = new ButtonActions(mAppContext);
	}
	
	
	public int getsStatus() {
		return mStatus;
	}
	
	
	public int deviceOpen() {
		if (mUsbManager == null) {			
			mUsbManager = (UsbManager)mAppContext.getSystemService(Context.USB_SERVICE);
		}
		
        mSerialDevice = UsbSerialProber.acquire(mUsbManager);
        
        if (mSerialDevice != null) {
        	//todo: get explicit permissions for device for when app is not launched from a usb connection intent
        	
            try {
            	Log.i(TAG, "SERIAL DEVICE FOUND: " + mSerialDevice);            	
            	
            	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mAppContext);
            	String baudDefault = mAppContext.getString(R.string.scantool_baud);
				int baudValue = Integer.parseInt(settings.getString("scantool_baud", baudDefault));
				
            	mSerialDevice.setParameters(baudValue, UsbSerialDriver.DATABITS_8, UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
            	mSerialDevice.open();
            	
            	ioManagerReset();
            	
            	mStatus = STATUS_OPEN_STOPPED;            	
            	return mSerialDevice.getDevice().getDeviceId();
            } catch (Exception ex) {
                Log.e(TAG, "ERROR OPENING DEVICE", ex);
            }
        } else {
        	Log.w(TAG, "COULD NOT FIND SERIAL DEVICE");
        }

        deviceClose();
        
        mStatus = STATUS_CLOSED_FROMERROR;        
        return 0;
	}
    
	
	public void deviceClose() {
		deviceClose(false);
	}
	
	
	protected void deviceClose(Boolean fromError) {
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
    		sendCommand("ATH1");   
    	} else if (mCommand == "ATH1") {
    		if (!mResponse.contains(mCommand) || !mResponse.contains(">") || !mResponse.contains("OK")) return;    		
    		Log.d(TAG, "HEADERS ON");
    		sendCommand("ATSP2");
    	} else if (mCommand == "ATSP2") {
    		if (!mResponse.contains(mCommand) || !mResponse.contains(">") || !mResponse.contains("OK")) return;    		
    		Log.d(TAG, "PROTOCOL SET");
    		sendCommand("ATMR11", 0, 5000, 3);    	
    	} else if (mCommand == "ATMR11") {
    		if (!mResponse.contains("\r") && !mResponse.contains("\n")) return;
    		mButtons.performAction(mResponse.trim());
    		mResponse = "";
    	} else {
    		Log.w(TAG, "UNEXPECTED DATA RECEIVED (WHILE NO COMMAND PENDING): " + mResponse);
    	}
    }
	
	
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
	        sendCommand("ATI", 2500, 0, 1);
		} else {
			Log.d(TAG, "MONITORING WARM START - TOO MANY ATTEMPTS");
			monitorStartCold();
		}
	
		mStartWarmAttempts++;
	}
	
	
	protected void monitorStartCold() {
		if (mStartColdAttempts < MONITOR_START_COLD_ATTEMPTS) {
			Log.d(TAG, "MONITORING COLD START ATTEMPT: " + mStartColdAttempts);
			sendCommand("ATZ", 5000, 0, 1);
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
	
	
	public Boolean sendCommandRetry() {
		return sendCommand(mCommand, mCommandTimeoutTotal, mCommandTimeoutData, mCommandRetries, true, false);
	}
	
	
	public Boolean sendCommand(String command) {
		return sendCommand(command, DEFAULT_COMMAND_TOTAL_TIMEOUT, DEFAULT_COMMAND_DATA_TIMEOUT, DEFAULT_COMMAND_RETRIES, false, false); 
	}
	
	
	public Boolean sendCommand(String command, int timeoutTotal, int timeoutData, int retries) {
		return sendCommand(command, timeoutTotal, timeoutData, retries, false, false); 
	}
	
	
	protected Boolean sendCommandBlind(String command) {
		return sendCommand(command, 0, 0, 0, false, true);
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
}
