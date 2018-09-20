/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.gui.fragments;

import android.content.Intent;
import android.content.UriPermission;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.xperia64.timidityae.R;
import com.xperia64.timidityae.SettingsActivity;
import com.xperia64.timidityae.gui.dialogs.FileBrowserDialog;
import com.xperia64.timidityae.util.ObjectSerializer;
import com.xperia64.timidityae.util.SettingsStorage;

import java.io.IOException;
import java.util.List;

public class DisplayPrefsFragment extends PreferenceFragmentCompat {
	SettingsActivity s;

	private ListPreference themePref; // Theme selection
	//private CheckBoxPreference hiddenFold; // Show hidden files or folders
	//private CheckBoxPreference showVids; // Show video files in the file browser
	private Preference defaultFoldPreference; // Browse for the default folder
	private EditTextPreference manHomeFolder; // Enter the default folder manually


	// API 14+
	//private CheckBoxPreference fplist; // Reorderable playlists

	// API 21+
	private Preference lolPref; // Select external storage to write to
	//private CheckBoxPreference lolNag; // Notify user about external storage

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		s = (SettingsActivity) getActivity();
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.settings_disp);

		themePref = (ListPreference) findPreference("fbTheme");
		//hiddenFold = (CheckBoxPreference) findPreference("hiddenSwitch");
		//showVids = (CheckBoxPreference) findPreference("videoSwitch");
		//fplist = (CheckBoxPreference) findPreference("fpSwitch");
		defaultFoldPreference = findPreference("defFold");
		manHomeFolder = (EditTextPreference) findPreference("defaultPath");
		lolPref = findPreference("lolWrite");
		//lolNag = (CheckBoxPreference) findPreference("shouldLolNag");

		themePref.setEnabled(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH);

		if (lolPref != null) {
			lolPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
				public boolean onPreferenceClick(Preference preference) {
					// dialog code here
					List<UriPermission> permissions = s.getContentResolver().getPersistedUriPermissions();
					if (!permissions.isEmpty()) {
						for (UriPermission p : permissions) {
							s.getContentResolver().releasePersistableUriPermission(p.getUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
						}
					}
					Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
					s.startActivityForResult(intent, 42);
					return true;
				}
			});
		}

		themePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				SettingsStorage.theme = Integer.parseInt((String) newValue);

				// Just to be safe
				try {
					s.prefs.edit().putString("tplusSoundfonts", ObjectSerializer.serialize(s.tmpSounds)).commit();
				} catch (IOException e) {
					e.printStackTrace();
				}

				Intent intent = s.getIntent();
				intent.putExtra("returnToDisp", true);
				s.finish();
				startActivity(intent);
				return true;
			}

		});

		defaultFoldPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				s.tmpItemEdit = manHomeFolder;
				s.tmpItemScreen = getPreferenceScreen();
				new FileBrowserDialog().create(3, null, s, s, s.getLayoutInflater(), true, s.prefs.getString("defaultPath", Environment.getExternalStorageDirectory().getAbsolutePath()), getResources().getString(R.string.fb_add));
				return true;
			}
		});
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

	}
}