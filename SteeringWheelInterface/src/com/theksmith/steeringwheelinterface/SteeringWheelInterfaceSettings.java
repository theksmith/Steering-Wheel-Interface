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
 * a simple settings screen (the app's only UI)
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

		Preference exit = findPreference("action_exit");
		exit.setOnPreferenceClickListener(mExitOnClickListener);			
		
		bindPreferenceSummaryToValue(findPreference("scantool_baud"));
		bindPreferenceSummaryToValue(findPreference("scantool_device_number"));
	}
	
		
	protected void bindPreferenceSummaryToValue(Preference preference) {
		preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		sBindPreferenceSummaryToValueListener.onPreferenceChange(
				preference,
				PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
	}

	
	protected OnPreferenceClickListener mExitOnClickListener = new OnPreferenceClickListener() {		
		@Override
		public boolean onPreferenceClick(Preference preference) {
			mParentActivity.finish();
			return false;
		}
	};
	

	protected OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();
			preference.setSummary(stringValue);
			return true;
		}
	};
}