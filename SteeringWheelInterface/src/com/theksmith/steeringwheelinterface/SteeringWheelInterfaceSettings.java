package com.theksmith.steeringwheelinterface;

import com.theksmith.steeringwheelinterface.R;

import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;


/**
 * A simple settings screen (the app's only UI).
 * 
 * @author Kristoffer Smith <stuff@theksmith.com>
 */
public class SteeringWheelInterfaceSettings extends PreferenceFragment {
	protected Activity mParentActivity;
	
	
	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		mParentActivity = activity;		
	}

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.pref_general);

		//actions (prefs acting only as buttons)
		Preference exit = findPreference("action_exit");
		exit.setOnPreferenceClickListener(mExitOnClickListener);			
		
		//for textbox type prefs
		bindTxtPrefSummaryToValue(findPreference("scantool_baud"));
		bindTxtPrefSummaryToValue(findPreference("scantool_device_number"));
	}
	
	
	protected OnPreferenceClickListener mExitOnClickListener = new OnPreferenceClickListener() {		
		@Override
		public boolean onPreferenceClick(Preference preference) {
			mParentActivity.finish();
			return false;
		}
	};
	
	
	protected void bindTxtPrefSummaryToValue(Preference preference) {
		preference.setOnPreferenceChangeListener(mTxtPrefChangeListener);

		mTxtPrefChangeListener.onPreferenceChange(
				preference,
				PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
	}

	
	protected OnPreferenceChangeListener mTxtPrefChangeListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();
			preference.setSummary(stringValue);
			return true;
		}
	};
}