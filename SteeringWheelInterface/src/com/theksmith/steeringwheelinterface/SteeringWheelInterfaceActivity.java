package com.theksmith.steeringwheelinterface;

import android.R.style;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceFragment;


/**
 * Main entry point activity, has no UI.
 * Starts the background service by default.
 * Opens the settings screen if called with Intent.ACTION_EDIT. 
 * Kills the background service and exits the app if called with Intent.ACTION_DELETE.
 * 
 * @author Kristoffer Smith <kristoffer@theksmith.com>
 */
public class SteeringWheelInterfaceActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		try {
			PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
			setTitle(getTitle() + " " + info.versionName);
		} catch (NameNotFoundException e) { }
		
		String action = getIntent().getAction(); 
		
		if (action == Intent.ACTION_EDIT) {
	    	//replace the blank UI with the settings screen	    	
			this.setTheme(style.Theme_DeviceDefault);	

			PreferenceFragment mSettingsFragment = new SteeringWheelInterfaceSettings();			
	        getFragmentManager()
	        		.beginTransaction()
	                .replace(android.R.id.content, mSettingsFragment)
	                .commit();
		} else if (action == Intent.ACTION_DELETE) {
			//TODO: if settings screen is open when this is called, it stays - how to end it?
			
			//kill car interface service and exit the app
			Intent interfaceService = new Intent(getBaseContext(), SteeringWheelInterfaceService.class);
			stopService(interfaceService);
			
			finish();
		} else {	    	
			//start the main car interface service and then exit the app
			Intent interfaceService = new Intent(getBaseContext(), SteeringWheelInterfaceService.class);
			startService(interfaceService);
			
			finish();
	    }
	}
}
