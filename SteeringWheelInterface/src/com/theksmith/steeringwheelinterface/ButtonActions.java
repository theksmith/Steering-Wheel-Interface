/*
 * TODO: Re-do this class to allow end users to define the button's bus messages and map each to a predefined action
 * 
 * Currently the project is assumed to be only a template. A developer would need to override
 * this class with their own specific implementation. To make this a "real" app, we will
 * need to make the buttons and their associated actions definable via the settings screen.
 */

package com.theksmith.steeringwheelinterface;

import java.util.Date;
import java.util.HashMap;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;
import android.view.KeyEvent;


/**
 * Provides a central place to define which bus messages correspond to which buttons, and which buttons do what actions.
 * This class is a basic template that would most likely be overridden.
 * 
 * @author Kristoffer Smith <stuff@theksmith.com>
 */
public class ButtonActions {
	protected static final String TAG = ButtonActions.class.getSimpleName();
	
	protected static final int HARDWARE_BOUNCE_THRESHOLD = 50;	//milliseconds
	
	protected Context mAppContext;
	protected HashMap<String, Long> mBusMessageDebounceTimes = new HashMap<String, Long>();
	
    //performAction() return status
	public static final int STATUS_ERROR_UNKNOWN = 0;
	public static final int STATUS_ERROR_HARDWAREBOUNCE = 1;
	public static final int STATUS_ERROR_UNKNOWNBUTTON = 2;
	public static final int STATUS_ERROR_ACTIONERROR = 3;
	public static final int STATUS_SUCCESS = 4;	

	//the known buttons and the beginning of their corresponding bus messages
	//TODO: allow wildcard or regex definitions
	public static final String BUTTON_LEFT_CENTER = "3D 11 00 80";
	public static final String BUTTON_LEFT_DOWN = "3D 11 10 00";
	public static final String BUTTON_LEFT_UP = "3D 11 20 00";
	public static final String BUTTON_RIGHT_CENTER = "3D 11 00 02";
	public static final String BUTTON_RIGHT_DOWN = "3D 11 02 00";
	public static final String BUTTON_RIGHT_UP = "3D 11 04 00";
	
	
	private ButtonActions() { 
		//exists only to prevent creation of class without passing required param
	}

	
	/**
	 * Constructor.
	 * 
	 * @param appContext		The application context of the container app. 
	 */
	public ButtonActions(Context appContext) {
		mAppContext = appContext.getApplicationContext();
	}
	

	/**
	 * Executes a particular button's assigned action.
	 * 
	 * @param forBusMessage		A bus message, expected to correspond to a button (one of the ButtonActions.BUTTON_XYZ definitions).
	 * @return 					Returns one of the ButtonActions.STATUS_XYZ definitions.
	 */
	public int performAction(String forBusMessage) {
		try {
			if (isHardwareBounce(forBusMessage)) {
				Log.i(TAG, "Hardware bounce: " + forBusMessage);
				return STATUS_ERROR_HARDWAREBOUNCE;
			}
			
			if (forBusMessage.startsWith(BUTTON_LEFT_CENTER)) {
				btnMediaPause();
			} else if (forBusMessage.startsWith(BUTTON_LEFT_DOWN)) {
				btnMediaTrackPrevious();
			} else if (forBusMessage.startsWith(BUTTON_LEFT_UP)) {
				btnMediaTrackNext();
			} else if (forBusMessage.startsWith(BUTTON_RIGHT_CENTER)) {
				btnHomeScreen();
			} else if (forBusMessage.startsWith(BUTTON_RIGHT_DOWN)) {
				btnVolumeDown();
			} else if (forBusMessage.startsWith(BUTTON_RIGHT_UP)) {
				btnVolumeUp();
			} else {
				Log.i(TAG, "Unknown button: " + forBusMessage);
				return STATUS_ERROR_UNKNOWNBUTTON;
			}
		} catch (Exception ex) {
			Log.e(TAG, "Error performing action for button: " + forBusMessage, ex);
			return STATUS_ERROR_ACTIONERROR;
		}
		
		return STATUS_SUCCESS;
	}
	

	protected boolean isHardwareBounce(String busMessage) {
		Boolean isBounce = true;		
		
		long now = (new Date()).getTime();		
		Long last = mBusMessageDebounceTimes.get(busMessage);
		
		if (last != null) {
			if (now - last > HARDWARE_BOUNCE_THRESHOLD) {
				isBounce = false;
			}
		}
		
		mBusMessageDebounceTimes.put(busMessage, now);
		
		return isBounce;
	}
		
	
	protected void btnMediaPause() throws Exception {
		//TODO: this requires root, is there a better way? how does Tasker do it?
		Runtime.getRuntime().exec("su -c input keyevent " + KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
    }
    

	protected void btnMediaTrackNext() throws Exception {
   		Runtime.getRuntime().exec("su -c input keyevent " + KeyEvent.KEYCODE_MEDIA_NEXT);
    }
    
    
	protected void btnMediaTrackPrevious() throws Exception {
   		Runtime.getRuntime().exec("su -c input keyevent " +  + KeyEvent.KEYCODE_MEDIA_PREVIOUS);
    }

	
	protected void btnHomeScreen() throws Exception {
		Runtime.getRuntime().exec("su -c input keyevent " + KeyEvent.KEYCODE_HOME);
    }

    
	protected void btnVolumeUp() throws Exception {
   		AudioManager mAudioManager = (AudioManager)mAppContext.getSystemService(Context.AUDIO_SERVICE);    		
   		mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
    }
    
    
	protected void btnVolumeDown() throws Exception {
		AudioManager mAudioManager = (AudioManager)mAppContext.getSystemService(Context.AUDIO_SERVICE);    		
		mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
    }    
}