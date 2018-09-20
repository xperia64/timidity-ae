/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.gui.fragments;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.xperia64.timidityae.R;
import com.xperia64.timidityae.SettingsActivity;
import com.xperia64.timidityae.gui.dialogs.SoxEffectsDialog;
import com.xperia64.timidityae.util.ObjectSerializer;

import java.io.IOException;
import java.util.ArrayList;

public class RootPrefsFragment extends PreferenceFragmentCompat {
	SettingsActivity s;

	private Preference disp;
	private Preference tplus;
	private Preference sox;

	//private CheckBoxPreference reShuffle; // Reshuffle playlist after stopping
	//private CheckBoxPreference nativeMidi; // Use MediaPlayer for MIDI playback
	//private CheckBoxPreference nativeMedia;
	//private CheckBoxPreference unsafeSox;
	//private CheckBoxPreference keepWav; // Keep broken wav files
	//private CheckBoxPreference useDefBack; // Use default back button behavior instead of swapping screens
	//private CheckBoxPreference compressCfg; // Compress config files

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		s = (SettingsActivity) getActivity();
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.settings_root);
		disp = findPreference("dsKey");
		tplus = findPreference("tplusKey");
		sox = findPreference("soxKey");

		//reShuffle = (CheckBoxPreference) findPreference("reShuffle");
		//nativeMidi = (CheckBoxPreference) findPreference("nativeMidiSwitch");
		//nativeMedia = (CheckBoxPreference) findPreference("nativeMediaSwitch");
		//unsafeSox = (CheckBoxPreference) findPreference("unsafeSoxSwitch");
		//keepWav = (CheckBoxPreference) findPreference("keepPartialWav");
		//useDefBack = (CheckBoxPreference) findPreference("useDefBack");
		//compressCfg = (CheckBoxPreference) findPreference("compressCfg");

		disp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {

				FragmentTransaction mFragmentTransaction = s.mFragmentManager.beginTransaction();
				mFragmentTransaction.replace(android.R.id.content, new DisplayPrefsFragment());
				mFragmentTransaction.addToBackStack(SettingsActivity.ROOT_PREFS).commit();
				return true;
			}
		});

		tplus.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {

				FragmentTransaction mFragmentTransaction = s.mFragmentManager.beginTransaction();
				mFragmentTransaction.replace(android.R.id.content, new TimidityPrefsFragment());
				mFragmentTransaction.addToBackStack(SettingsActivity.ROOT_PREFS).commit();
				return true;
			}
		});

		sox.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {

				/*FragmentTransaction mFragmentTransaction = s.mFragmentManager.beginTransaction();
				mFragmentTransaction.replace(android.R.id.content, new SoxPrefsFragment());
				mFragmentTransaction.addToBackStack(SettingsActivity.ROOT_PREFS).commit();*/
				new SoxEffectsDialog().create(getActivity(), getActivity().getLayoutInflater());
				return true;
			}
		});

		try {
			s.tmpSounds = (ArrayList<String>) ObjectSerializer.deserialize(s.prefs.getString("tplusSoundfonts", ObjectSerializer.serialize(new ArrayList<String>())));
			System.out.println("We have tmpSounds of size: " + s.tmpSounds.size());
			for (int i = 0; i < s.tmpSounds.size(); i++) {
				if (s.tmpSounds.get(i) == null)
					s.tmpSounds.remove(i);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (s.tmpSounds == null)
			s.tmpSounds = new ArrayList<>();

		if (s.loadDispSettings) {
			s.loadDispSettings = false;
			FragmentTransaction mFragmentTransaction = s.mFragmentManager.beginTransaction();
			mFragmentTransaction.replace(android.R.id.content, new DisplayPrefsFragment());
			mFragmentTransaction.addToBackStack(SettingsActivity.ROOT_PREFS);
			mFragmentTransaction.commit();
		}
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

	}
}