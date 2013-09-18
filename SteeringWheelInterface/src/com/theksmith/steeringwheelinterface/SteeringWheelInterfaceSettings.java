package com.theksmith.steeringwheelinterface;

import com.theksmith.steeringwheelinterface.R;

import android.app.Activity;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;


/**
 * A simple settings screen (the app's only UI).
 * 
 * @author Kristoffer Smith <kristoffer@theksmith.com>
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
		
		//for preference types with string values
		bindStringPreferenceSummaryToValue(findPreference("scantool_baud"));
		bindStringPreferenceSummaryToValue(findPreference("scantool_device_number"));
		bindStringPreferenceSummaryToValue(findPreference("scantool_monitor_command"));
		bindStringPreferenceSummaryToValue(findPreference("scantool_protocol"));
	}
	
	
	protected OnPreferenceClickListener mExitOnClickListener = new OnPreferenceClickListener() {		
		@Override
		public boolean onPreferenceClick(Preference preference) {
			mParentActivity.finish();
			return false;
		}
	};
	
	
	protected void bindStringPreferenceSummaryToValue(Preference preference) {
		preference.setOnPreferenceChangeListener(mStringPreferenceChangeListener);

		mStringPreferenceChangeListener.onPreferenceChange(
				preference,
				PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
	}

	
	protected OnPreferenceChangeListener mStringPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();
			
			if (preference instanceof ListPreference) {
				ListPreference list = (ListPreference)preference;
				stringValue = (String)list.getEntries()[list.findIndexOfValue(stringValue)];
			}
			
			preference.setSummary(stringValue);
			
			return true;
		}
	};
}