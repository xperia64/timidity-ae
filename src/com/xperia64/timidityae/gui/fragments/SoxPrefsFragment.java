package com.xperia64.timidityae.gui.fragments;

import android.os.Bundle;

import com.github.machinarius.preferencefragment.PreferenceFragment;
import com.xperia64.timidityae.SettingsActivity;

/**
 * Created by xperia64 on 1/2/17.
 */

public class SoxPrefsFragment extends PreferenceFragment {
	SettingsActivity s;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		s = (SettingsActivity) getActivity();
		// Load the preferences from an XML resource
		// TODO addPreferencesFromResource(R.xml.settings_sox);

	}

}