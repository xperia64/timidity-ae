/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.gui.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.SparseIntArray;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.xperia64.timidityae.R;
import com.xperia64.timidityae.SettingsActivity;
import com.xperia64.timidityae.gui.dialogs.FileBrowserDialog;
import com.xperia64.timidityae.gui.dialogs.SoundfontDialog;
import com.xperia64.timidityae.util.Globals;
import com.xperia64.timidityae.util.SettingsStorage;

/**
 * Created by xperia64 on 1/2/17.
 */

public class TimidityPrefsFragment extends PreferenceFragmentCompat {
	SettingsActivity s;

	// TiMidity++ Settings
	private CheckBoxPreference manTcfg; // Use manual timidity.cfg?
	private Preference sfPref; // Open soundfont manager
	private CheckBoxPreference psilence; // Preserve silence and beginning of midi
	private CheckBoxPreference unload; // Unload instruments
	private ListPreference resampMode; // Default resampling algorithm
	private ListPreference stereoMode; // Synth Mono, Downmixed Mono, or Stereo
	private ListPreference rates; // Audio rates
	private EditTextPreference volume; // Amplification. Default 70, max 800
	private EditTextPreference bufferSize; // Buffer size, in something. I use 192000 by default.
	private ListPreference verbosity;

	// Timidity AE Data Settings
	private Preference reinstallSoundfont;
	private Preference dataFoldPreference;
	private EditTextPreference manDataFolder;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		s = (SettingsActivity) getActivity();
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.settings_tim);

		manTcfg = (CheckBoxPreference) findPreference("manualConfig");
		sfPref = findPreference("sfConfig");
		psilence = (CheckBoxPreference) findPreference("tplusSilKey");
		unload = (CheckBoxPreference) findPreference("tplusUnload");
		resampMode = (ListPreference) findPreference("tplusResamp");
		stereoMode = (ListPreference) findPreference("sdlChanValue");
		rates = (ListPreference) findPreference("tplusRate");
		volume = (EditTextPreference) findPreference("tplusVol");
		bufferSize = (EditTextPreference) findPreference("tplusBuff");
		verbosity = (ListPreference) findPreference("timidityVerbosity");

		reinstallSoundfont = findPreference("reSF");
		dataFoldPreference = findPreference("defData");
		manDataFolder = (EditTextPreference) findPreference("dataDir");

		SettingsStorage.updateBuffers(SettingsStorage.updateRates());
		int[] values = SettingsStorage.updateRates();
		if (values != null) {
			CharSequence[] hz = new CharSequence[values.length];
			CharSequence[] hzItems = new CharSequence[values.length];
			for (int i = 0; i < values.length; i++) {
				hz[i] = Integer.toString(values[i]) + "Hz";
				hzItems[i] = Integer.toString(values[i]);
			}
			rates.setEntries(hz);
			rates.setEntryValues(hzItems);
			rates.setDefaultValue(Integer.toString(AudioTrack.getNativeOutputSampleRate(AudioTrack.MODE_STREAM)));
			rates.setValue(s.prefs.getString("tplusRate", Integer.toString(AudioTrack.getNativeOutputSampleRate(AudioTrack.MODE_STREAM))));
		}

		manTcfg.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1) {
				boolean manual = (Boolean) arg1;
				sfPref.setEnabled(!manual);
				s.needRestart = true;
				s.needUpdateSf = !manual;
				if (!manual) {
					s.needUpdateSf = true;
				}
				return true;
			}
		});

		sfPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				new SoundfontDialog().create(s.tmpSounds, s, s, s.getLayoutInflater(), s.prefs.getString("defaultPath", Environment.getExternalStorageDirectory().getPath()));
				return true;
			}

		});

		psilence.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1) {
				s.needRestart = true;
				return true;
			}

		});

		unload.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1) {
				s.needRestart = true;
				return true;
			}

		});

		resampMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (!(resampMode.getValue()).equals(newValue)) {
					s.needRestart = true;
				}
				return true;
			}

		});

		stereoMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (!stereoMode.getValue().equals(newValue)) {
					s.needRestart = true;
					String stereo = (String) newValue;
					String sixteen = "16"; // s.bitMode.getValue();
					boolean sb = stereo == null || stereo.equals("2");
					boolean sxb = sixteen.equals("16");
					SparseIntArray mmm = SettingsStorage.validBuffers(SettingsStorage.validRates(sb, sxb), sb, sxb);
					if (mmm != null) {
						int minBuff = mmm.get(Integer.parseInt(rates.getValue()));

						int buff = Integer.parseInt(bufferSize.getText());
						if (buff < minBuff) {
							s.prefs.edit().putString("tplusBuff", Integer.toString(minBuff)).commit();
							bufferSize.setText(Integer.toString(minBuff));
							Toast.makeText(s, getResources().getString(R.string.invalidbuff), Toast.LENGTH_SHORT).show();
							//((BaseAdapter) TimidityPrefsFragment.this.getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
							//((BaseAdapter) TimidityPrefsFragment.this.getPreferenceScreen().getRootAdapter()).notifyDataSetInvalidated();
						}
					}
				}
				return true;
			}
		});

		rates.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (!(rates.getValue()).equals(newValue)) {
					s.needRestart = true;
					String stereo = stereoMode.getValue();
					String sixteen = "16";// s.bitMode.getValue();
					boolean sb = stereo == null || stereo.equals("2");
					boolean sxb = sixteen.equals("16");
					SparseIntArray mmm = SettingsStorage.validBuffers(SettingsStorage.validRates(sb, sxb), sb, sxb);
					if (mmm != null) {
						int minBuff = mmm.get(Integer.parseInt((String) newValue));

						int buff = Integer.parseInt(bufferSize.getText());
						if (buff < minBuff) {
							s.prefs.edit().putString("tplusBuff", Integer.toString(minBuff)).commit();
							bufferSize.setText(Integer.toString(minBuff));
							Toast.makeText(s, getResources().getString(R.string.invalidbuff), Toast.LENGTH_SHORT).show();
							//((BaseAdapter) TimidityPrefsFragment.this.getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
							//((BaseAdapter) TimidityPrefsFragment.this.getPreferenceScreen().getRootAdapter()).notifyDataSetInvalidated();
						}
					}
				}
				return true;
			}
		});

		volume.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (!volume.getText().equals(newValue)) {
					s.needRestart = true;
					String txt = (String) newValue;
					if (txt != null) {
						if (!txt.isEmpty()) {
							int volume = Integer.parseInt(txt);
							if(volume < 0 || volume > 800)
							{
								Toast.makeText(s, "Invalid volume. Must be between 0 and 800", Toast.LENGTH_SHORT).show();
								return false;
							}
							return true;
						}
					}
					return false;
				}
				return true;
			}
		});

		bufferSize.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, final Object newValue) {
				if (!bufferSize.getText().equals(newValue)) {
					s.needRestart = true;
					String txt = (String) newValue;
					if (txt != null) {
						if (!txt.isEmpty()) {
							String stereo = stereoMode.getValue();
							String sixteen = "16"; // s.bitMode.getValue();
							boolean sb = stereo == null || stereo.equals("2");
							boolean sxb = sixteen.equals("16");
							SparseIntArray mmm = SettingsStorage.validBuffers(SettingsStorage.validRates(sb, sxb), sb, sxb);
							if (mmm != null) {
								int minBuff = mmm.get(Integer.parseInt(rates.getValue()));

								int buff = Integer.parseInt(txt);
								if (buff < minBuff) {
									s.prefs.edit().putString("tplusBuff", Integer.toString(minBuff)).commit();
									((EditTextPreference) preference).setText(Integer.toString(minBuff));
									Toast.makeText(s, getResources().getString(R.string.invalidbuff), Toast.LENGTH_SHORT).show();
									//((BaseAdapter) TimidityPrefsFragment.this.getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
									//((BaseAdapter) TimidityPrefsFragment.this.getPreferenceScreen().getRootAdapter()).notifyDataSetInvalidated();
									return false;
								}
							}
							return true;
						}
					}
					return false;
				}
				return true;
			}

		});

		verbosity.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1) {
				s.needRestart = true;
				return true;
			}

		});

		reinstallSoundfont.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				AlertDialog dialog = new AlertDialog.Builder(getActivity()).create();
				dialog.setTitle(getResources().getString(R.string.sett_resf_q));
				dialog.setMessage(getResources().getString(R.string.sett_resf_q_sum));
				dialog.setCancelable(true);
				dialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int buttonId) {
						AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {
							ProgressDialog pd;
							@Override
							protected void onPreExecute() {
								pd = new ProgressDialog(s);
								pd.setTitle(getResources().getString(R.string.extract));
								pd.setMessage(getResources().getString(R.string.extract_sum));
								pd.setCancelable(false);
								pd.setIndeterminate(true);
								pd.show();
							}
							@Override
							protected Integer doInBackground(Void... arg0) {
								return Globals.extract8Rock(s);
							}
							@Override
							protected void onPostExecute(Integer result) {
								if (pd != null) {
									pd.dismiss();
									if (result != 777) {
										Toast.makeText(s, getResources().getString(R.string.sett_resf_err), Toast.LENGTH_SHORT).show();
									} else {
										Toast.makeText(s, getResources().getString(R.string.extract_def), Toast.LENGTH_LONG).show();
									}
								}
							}
						};
						task.execute((Void[]) null);
					}
				});
				dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int buttonId) {}
				});
				dialog.show();
				return true;
			}
		});

		dataFoldPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				s.needRestart = true;
				s.tmpItemEdit = manDataFolder;
				s.tmpItemScreen = getPreferenceScreen();
				new FileBrowserDialog().create(4, null, s, s, s.getLayoutInflater(), true, s.prefs.getString("dataDir", Environment.getExternalStorageDirectory().getAbsolutePath()), getResources().getString(R.string.fb_add));
				return true;
			}
		});

		manDataFolder.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (!(manDataFolder.getText()).equals(newValue)) {
					s.needRestart = true;
				}
				return true;
			}
		});
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

	}
}