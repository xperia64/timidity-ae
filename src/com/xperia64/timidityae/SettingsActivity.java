/*******************************************************************************
 * Copyright (C) 2014 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * Copyright (C) 1999-2008 Masanao Izumo <iz@onicos.co.jp>
 * <p>
 * Copyright (C) 1995 Tuukka Toivonen <tt@cgs.fi>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.github.machinarius.preferencefragment.PreferenceFragment;
import com.xperia64.timidityae.gui.dialogs.FileBrowserDialog;
import com.xperia64.timidityae.gui.dialogs.FileBrowserDialog.FileBrowserDialogListener;
import com.xperia64.timidityae.gui.dialogs.SoundfontDialog;
import com.xperia64.timidityae.gui.dialogs.SoundfontDialog.SoundfontDialogListener;
import com.xperia64.timidityae.util.Constants;
import com.xperia64.timidityae.util.DocumentFileUtils;
import com.xperia64.timidityae.util.Globals;
import com.xperia64.timidityae.util.ObjectSerializer;
import com.xperia64.timidityae.util.SettingsStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("CommitPrefEdits")
public class SettingsActivity extends AppCompatActivity implements FileBrowserDialogListener, SoundfontDialogListener {

	public static SettingsActivity mInstance = null;
	private ArrayList<String> tmpSounds;
	// private int buffSize;
	private boolean needRestart = false;
	private boolean needUpdateSf = false;

	private ListPreference themePref;
	private CheckBoxPreference hiddenFold;
	private CheckBoxPreference showVids;
	private CheckBoxPreference fplist;
	private Preference defaultFoldPreference;
	private Preference reinstallSoundfont;
	private Preference lolPref;
	private EditTextPreference manHomeFolder;
	// -- needs restart below --
	private CheckBoxPreference manTcfg;
	private Preference sfPref;
	private CheckBoxPreference psilence;
	private CheckBoxPreference unload;
	private ListPreference resampMode;
	private ListPreference stereoMode;
	// private ListPreference bitMode;
	private ListPreference rates;
	private EditTextPreference bufferSize;
	private Preference dataFoldPreference;
	private EditTextPreference manDataFolder;
	// private PreferenceScreen ds;
	private PreferenceScreen tplus;
	// -- needs restart above --
	private CheckBoxPreference nativeMidi;
	private CheckBoxPreference keepWav;
	private SharedPreferences prefs;
	private TimidityPrefsFragment pf;
	private float abElevation;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		mInstance = this;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR && Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			this.setTheme(android.support.v7.appcompat.R.style.Theme_AppCompat);
		} else {
			this.setTheme((SettingsStorage.theme == 1) ? android.support.v7.appcompat.R.style.Theme_AppCompat_Light_DarkActionBar : android.support.v7.appcompat.R.style.Theme_AppCompat);
		}
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Display the fragment as the main content.
		FragmentManager mFragmentManager = getSupportFragmentManager();
		FragmentTransaction mFragmentTransaction = mFragmentManager.beginTransaction();
		pf = new TimidityPrefsFragment();
		mFragmentTransaction.replace(android.R.id.content, pf);
		mFragmentTransaction.commit();
		abElevation = getSupportActionBar().getElevation();
	}

	public void setUpNestedScreen(PreferenceScreen preferenceScreen) {
		final Dialog dialog = preferenceScreen.getDialog();

		Toolbar bar;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			try {
				LinearLayout root = (LinearLayout) dialog.findViewById(android.R.id.list).getParent();
				bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
				bar.setElevation(abElevation);
				root.addView(bar, 0);
			} catch (ClassCastException e) {
				FrameLayout root = (FrameLayout) dialog.findViewById(android.R.id.list).getParent();
				bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
				bar.setElevation(abElevation);
				root.addView(bar, 0);
			}
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			LinearLayout root = (LinearLayout) dialog.findViewById(android.R.id.list).getParent();
			bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
			root.addView(bar, 0); // insert at top
		} else {
			ViewGroup root = (ViewGroup) dialog.findViewById(android.R.id.content);
			ListView content = (ListView) root.getChildAt(0);

			root.removeAllViews();

			bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);

			int height;
			TypedValue tv = new TypedValue();
			if (getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
				height = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
			} else {
				height = bar.getHeight();
			}

			content.setPadding(0, height, 0, 0);

			root.addView(content);
			root.addView(bar);
		}

		bar.setTitle(preferenceScreen.getTitle());

		bar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return false;
	}

	@Override
	public void onBackPressed() {
		// Store the soundfonts
		try {
			prefs.edit().putString("tplusSoundfonts", ObjectSerializer.serialize(tmpSounds)).commit();
		} catch (IOException e) {
			e.printStackTrace();
		}
		SettingsStorage.reloadSettings(this, this.getAssets());
		if (needUpdateSf) {
			SettingsStorage.writeCfg(SettingsActivity.this, SettingsStorage.dataFolder + "/timidity/timidity.cfg", tmpSounds); // TODO																						// ??
		}

		if (needRestart) {
			Intent new_intent = new Intent();
			new_intent.setAction(Constants.msrv_rec);
			new_intent.putExtra(Constants.msrv_cmd, Constants.msrv_cmd_reload_libs);
			sendBroadcast(new_intent);
		}
		Intent returnIntent = new Intent();
		setResult(3, returnIntent);
		this.finish();

	}

	@Override
	public void setItem(String path, int type) {
		if (path != null) {
			if (!TextUtils.isEmpty(path)) {
				switch (type) {
					case 3:
						prefs.edit().putString("defaultPath", path).commit();
						manHomeFolder.setText(path);
						SettingsStorage.homeFolder = path;
						((BaseAdapter) pf.getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
						break;
					case 4:
						prefs.edit().putString("dataDir", path).commit();
						manDataFolder.setText(path);
						((BaseAdapter) pf.getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
						break;
					case 5:
						// soundfont fun
						break;
				}
				return;
			}
		}
		Toast.makeText(this, getResources().getString(R.string.invalidfold), Toast.LENGTH_SHORT).show();
	}

	@RequiresApi(api = Build.VERSION_CODES.KITKAT)
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Check which request we're responding to
		if (requestCode == 42) {
			if (resultCode == RESULT_OK) {

				Uri treeUri = data.getData();
				getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
				DocumentFileUtils.docFileDevice = treeUri;
			} else {
				DocumentFileUtils.docFileDevice = null;
			}

		}
	}

	@Override
	public void write() {
	}

	@Override
	public void ignore() {
	}

	@Override
	public void writeSoundfonts(ArrayList<String> l) {
		if (l.size() == tmpSounds.size()) {
			for (int i = 0; i < l.size(); i++) {
				if (!l.get(i).equals(tmpSounds.get(i))) {
					needRestart = true;
					needUpdateSf = true;
					break;
				}
			}
		} else {
			needRestart = true;
			needUpdateSf = true;
		}
		if (needUpdateSf) {
			tmpSounds.clear();
			for (String foo : l) {
				tmpSounds.add(foo);
			}
		}
	}

	public static class TimidityPrefsFragment extends PreferenceFragment {

		SettingsActivity s;
		@Override
		public void onCreate(Bundle savedInstanceState) {

			super.onCreate(savedInstanceState);
			s = (SettingsActivity) getActivity();
			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.settings);
			s.prefs = PreferenceManager.getDefaultSharedPreferences(s.getBaseContext());
			s.themePref = (ListPreference) findPreference("fbTheme");
			s.hiddenFold = (CheckBoxPreference) findPreference("hiddenSwitch");
			s.showVids = (CheckBoxPreference) findPreference("videoSwitch");
			s.fplist = (CheckBoxPreference) findPreference("fpSwitch");
			s.defaultFoldPreference = findPreference("defFold");
			s.reinstallSoundfont = findPreference("reSF");
			s.manHomeFolder = (EditTextPreference) findPreference("defaultPath");
			s.dataFoldPreference = findPreference("defData");
			s.manDataFolder = (EditTextPreference) findPreference("dataDir");
			s.manTcfg = (CheckBoxPreference) findPreference("manualConfig");
			s.sfPref = findPreference("sfConfig");
			s.resampMode = (ListPreference) findPreference("tplusResamp");
			s.stereoMode = (ListPreference) findPreference("sdlChanValue");
			// s.bitMode = (ListPreference) findPreference("tplusBits");
			s.rates = (ListPreference) findPreference("tplusRate");
			s.bufferSize = (EditTextPreference) findPreference("tplusBuff");
			// nativeMidi = (CheckBoxPreference)
			// findPreference("nativeMidiSwitch");
			// ds = (PreferenceScreen) findPreference("dsKey");
			s.tplus = (PreferenceScreen) findPreference("tplusKey");
			s.nativeMidi = (CheckBoxPreference) findPreference("nativeMidiSwitch");
			s.keepWav = (CheckBoxPreference) findPreference("keepPartialWav");
			s.sfPref.setEnabled(!s.manTcfg.isChecked());
			s.lolPref = findPreference("lolWrite");
			s.psilence = (CheckBoxPreference) findPreference("tplusSilKey");
			s.unload = (CheckBoxPreference) findPreference("tplusUnload");

			s.hiddenFold.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference arg0, Object arg1) {
					SettingsStorage.showHiddenFiles = (Boolean) arg1;
					return true;
				}

			});
			s.showVids.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference arg0, Object arg1) {
					SettingsStorage.showVideos = (Boolean) arg1;
					return true;
				}

			});
			s.nativeMidi.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference arg0, Object arg1) {
					if (!SettingsStorage.onlyNative)
						SettingsStorage.nativeMidi = (Boolean) arg1;
					else
						SettingsStorage.nativeMidi = true;
					return true;
				}

			});
			if (s.fplist != null) {
				s.fplist.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference arg0, Object arg1) {
						SettingsStorage.enableDragNDrop = (Boolean) arg1;
						return true;
					}

				});
			}
			s.keepWav.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference arg0, Object arg1) {
					SettingsStorage.keepPartialWav = (Boolean) arg1;
					return true;
				}

			});
			s.defaultFoldPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					// dialog code here

					new FileBrowserDialog().create(3, null, s, s, s.getLayoutInflater(), true, s.prefs.getString("defaultPath", Environment.getExternalStorageDirectory().getAbsolutePath()), getResources().getString(R.string.fb_add));
					return true;
				}
			});
			if (s.lolPref != null)
				s.lolPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
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
			s.reinstallSoundfont.setOnPreferenceClickListener(new OnPreferenceClickListener() {

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
						public void onClick(DialogInterface dialog, int buttonId) {

						}
					});
					dialog.show();
					return true;

				}

			});
			s.dataFoldPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					// dialog code here
					s.needRestart = true;
					new FileBrowserDialog().create(4, null, s, s, s.getLayoutInflater(), true, s.prefs.getString("dataDir", Environment.getExternalStorageDirectory().getAbsolutePath()), getResources().getString(R.string.fb_add));
					return true;
				}
			});

			try {
				s.tmpSounds = (ArrayList<String>) ObjectSerializer.deserialize(s.prefs.getString("tplusSoundfonts", ObjectSerializer.serialize(new ArrayList<String>())));
				for (int i = 0; i < s.tmpSounds.size(); i++) {
					if (s.tmpSounds.get(i) == null)
						s.tmpSounds.remove(i);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			s.rates.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {

					if (!(s.rates.getValue()).equals(newValue)) {
						s.needRestart = true;
						String stereo = s.stereoMode.getValue();
						String sixteen = "16";// s.bitMode.getValue();
						boolean sb = stereo == null || stereo.equals("2");
						boolean sxb = sixteen.equals("16");
						SparseIntArray mmm = SettingsStorage.validBuffers(SettingsStorage.validRates(sb, sxb), sb, sxb);
						if (mmm != null) {
							int minBuff = mmm.get(Integer.parseInt((String) newValue));

							int buff = Integer.parseInt(s.bufferSize.getText());
							if (buff < minBuff) {
								s.prefs.edit().putString("tplusBuff", Integer.toString(minBuff)).commit();
								s.bufferSize.setText(Integer.toString(minBuff));
								Toast.makeText(s, getResources().getString(R.string.invalidbuff), Toast.LENGTH_SHORT).show();
								((BaseAdapter) s.tplus.getRootAdapter()).notifyDataSetChanged();
								((BaseAdapter) s.tplus.getRootAdapter()).notifyDataSetInvalidated();
							}
						}
					}
					return true;
				}
			});
			if (s.tmpSounds == null)
				s.tmpSounds = new ArrayList<>();
			s.manTcfg.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference arg0, Object arg1) {
					boolean manual = (Boolean) arg1;
					s.sfPref.setEnabled(!manual);
					s.needRestart = true;
					s.needUpdateSf = !manual;
					if (!manual) {
						s.needUpdateSf = true;
					}
					return true;
				}

			});

			s.psilence.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference arg0, Object arg1) {
					s.needRestart = true;
					return true;
				}

			});
			s.unload.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference arg0, Object arg1) {
					s.needRestart = true;
					return true;
				}

			});
			s.sfPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					new SoundfontDialog().create(s.tmpSounds, s, s, s.getLayoutInflater(), s.prefs.getString("defaultPath", Environment.getExternalStorageDirectory().getPath()));
					return true;
				}

			});
			s.resampMode.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					if (!(s.resampMode.getValue()).equals(newValue)) {
						s.needRestart = true;
					}
					return true;
				}

			});
			s.manDataFolder.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					if (!(s.manDataFolder.getText()).equals(newValue)) {
						s.needRestart = true;
					}
					return true;
				}
			});
			// buffSize = Integer.parseInt(prefs.getString("tplusBuff",
			// "192000"));
			// System.out.println("Buffsize is: "+buffSize);
			SettingsStorage.updateBuffers(SettingsStorage.updateRates());
			int[] values = SettingsStorage.updateRates();
			if (values != null) {
				CharSequence[] hz = new CharSequence[values.length];
				CharSequence[] hzItems = new CharSequence[values.length];
				for (int i = 0; i < values.length; i++) {
					hz[i] = Integer.toString(values[i]) + "Hz";
					hzItems[i] = Integer.toString(values[i]);
				}
				s.rates.setEntries(hz);
				s.rates.setEntryValues(hzItems);
				s.rates.setDefaultValue(Integer.toString(AudioTrack.getNativeOutputSampleRate(AudioTrack.MODE_STREAM)));
				s.rates.setValue(s.prefs.getString("tplusRate", Integer.toString(AudioTrack.getNativeOutputSampleRate(AudioTrack.MODE_STREAM))));
			}
			s.bufferSize.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, final Object newValue) {
					if (!s.bufferSize.getText().equals(newValue)) {
						s.needRestart = true;
						String txt = (String) newValue;
						if (txt != null) {
							if (!TextUtils.isEmpty(txt)) {

								String stereo = s.stereoMode.getValue();
								String sixteen = "16"; // s.bitMode.getValue();
								boolean sb = stereo == null || stereo.equals("2");
								boolean sxb = sixteen.equals("16");
								SparseIntArray mmm = SettingsStorage.validBuffers(SettingsStorage.validRates(sb, sxb), sb, sxb);
								if (mmm != null) {

									int minBuff = mmm.get(Integer.parseInt(s.rates.getValue()));

									int buff = Integer.parseInt(txt);
									if (buff < minBuff) {
										s.prefs.edit().putString("tplusBuff", Integer.toString(minBuff)).commit();
										((EditTextPreference) preference).setText(Integer.toString(minBuff));
										Toast.makeText(s, getResources().getString(R.string.invalidbuff), Toast.LENGTH_SHORT).show();
										((BaseAdapter) s.tplus.getRootAdapter()).notifyDataSetChanged();
										((BaseAdapter) s.tplus.getRootAdapter()).notifyDataSetInvalidated();
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
			s.stereoMode.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					if (!s.stereoMode.getValue().equals(newValue)) {
						s.needRestart = true;
						String stereo = (String) newValue;
						String sixteen = "16"; // s.bitMode.getValue();
						boolean sb = stereo == null || stereo.equals("2");
						boolean sxb = sixteen.equals("16");
						SparseIntArray mmm = SettingsStorage.validBuffers(SettingsStorage.validRates(sb, sxb), sb, sxb);
						if (mmm != null) {

							int minBuff = mmm.get(Integer.parseInt(s.rates.getValue()));

							int buff = Integer.parseInt(s.bufferSize.getText());
							if (buff < minBuff) {
								s.prefs.edit().putString("tplusBuff", Integer.toString(minBuff)).commit();
								s.bufferSize.setText(Integer.toString(minBuff));
								Toast.makeText(s, getResources().getString(R.string.invalidbuff), Toast.LENGTH_SHORT).show();
								((BaseAdapter) s.tplus.getRootAdapter()).notifyDataSetChanged();
								((BaseAdapter) s.tplus.getRootAdapter()).notifyDataSetInvalidated();
							}
						}

					}
					return true;
				}
			});
			s.themePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					SettingsStorage.theme = Integer.parseInt((String) newValue);
					Intent intent = s.getIntent();
					s.finish();
					startActivity(intent);
					return true;
				}

			});
		}

		public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
			super.onPreferenceTreeClick(preferenceScreen, preference);

			if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {

				if (preference instanceof PreferenceScreen) {
					s.setTheme((SettingsStorage.theme == 1) ? android.support.v7.appcompat.R.style.Theme_AppCompat_Light_DarkActionBar : android.support.v7.appcompat.R.style.Theme_AppCompat);
					s.setUpNestedScreen((PreferenceScreen) preference);
				}
			}
			return false;
		}
	}

}
