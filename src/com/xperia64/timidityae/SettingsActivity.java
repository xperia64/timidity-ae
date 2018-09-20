/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.view.MenuItem;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.xperia64.timidityae.gui.dialogs.FileBrowserDialog.FileBrowserDialogListener;
import com.xperia64.timidityae.gui.dialogs.SoundfontDialog.SoundfontDialogListener;
import com.xperia64.timidityae.gui.fragments.RootPrefsFragment;
import com.xperia64.timidityae.util.Constants;
import com.xperia64.timidityae.util.DocumentFileUtils;
import com.xperia64.timidityae.util.ObjectSerializer;
import com.xperia64.timidityae.util.SettingsStorage;

import java.io.IOException;
import java.util.ArrayList;

@SuppressLint("CommitPrefEdits")
public class SettingsActivity extends AppCompatActivity implements FileBrowserDialogListener, SoundfontDialogListener {

	public static SettingsActivity mInstance = null;
	public ArrayList<String> tmpSounds;

	public boolean needRestart = false;
	public boolean needUpdateSf = false;

	public SharedPreferences prefs;
	public FragmentManager mFragmentManager;

	public EditTextPreference tmpItemEdit;
	public PreferenceScreen tmpItemScreen;

	public static String ROOT_PREFS = "RootPreferences";
	public static String DISP_PREFS = "DispPreferences";
	public static String TIM_PREFS = "TimPreferences";
	public static String SOX_PREFS = "SoXPreferences";

	public boolean loadDispSettings = false;

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
		prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		mFragmentManager = getSupportFragmentManager();
		FragmentTransaction mFragmentTransaction = mFragmentManager.beginTransaction();
		mFragmentTransaction.replace(android.R.id.content, new RootPrefsFragment());
		mFragmentTransaction.commit();
		loadDispSettings = getIntent().getBooleanExtra("returnToDisp", false);
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
		if(mFragmentManager.getBackStackEntryCount() > 0)
		{
			mFragmentManager.popBackStack();
			return;
		}
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
			if (!path.isEmpty()) {
				switch (type) {
					case 3:
						prefs.edit().putString("defaultPath", path).commit();
						tmpItemEdit.setText(path);
						SettingsStorage.homeFolder = path;
						//((BaseAdapter) tmpItemScreen.getRootAdapter()).notifyDataSetChanged();
						break;
					case 4:
						prefs.edit().putString("dataDir", path).commit();
						tmpItemEdit.setText(path);
						//((BaseAdapter) tmpItemScreen.notifyChanged().getRootAdapter()).notifyDataSetChanged();
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
}
