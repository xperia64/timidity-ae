/*******************************************************************************
 * Copyright (C) 2014 xperia64 <xperiancedapps@gmail.com>
 * 
 * Copyright (C) 1999-2008 Masanao Izumo <iz@onicos.co.jp>
 *     
 * Copyright (C) 1995 Tuukka Toivonen <tt@cgs.fi>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.xperia64.timidityae.FileBrowserDialog.FileBrowserDialogListener;
import com.xperia64.timidityae.SoundfontDialog.SoundfontDialogListener;
import com.xperia64.timidityae.R;

import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.widget.BaseAdapter;
import android.widget.Toast;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
public class SettingsActivity extends PreferenceActivity implements FileBrowserDialogListener, SoundfontDialogListener {
	
	public static SettingsActivity mInstance = null;
	private ArrayList<String> tmpSounds;
	//private int buffSize;
	private boolean needRestart=false;
	private boolean needUpdateSf=false;
	
	private ListPreference themePref;
	private CheckBoxPreference hiddenFold;
	private CheckBoxPreference showVids;
	private Preference defaultFoldPreference;
	private Preference reinstallSoundfont;
	private EditTextPreference manHomeFolder;
	// -- needs restart below -- 
	private CheckBoxPreference manTcfg;
	private Preference sfPref;
	private ListPreference resampMode;
	private ListPreference stereoMode;
	private ListPreference bitMode;
	private ListPreference rates;
	private EditTextPreference bufferSize;
	private Preference dataFoldPreference;
	private EditTextPreference manDataFolder;
	
	//private PreferenceScreen ds;
	private PreferenceScreen tplus;
	// -- needs restart above -- 
	private CheckBoxPreference nativeMidi;
	private SharedPreferences prefs;
	        @SuppressWarnings({ "deprecation", "unchecked" })
			@Override
	        protected void onCreate(Bundle savedInstanceState) {
	        	mInstance = this;
	        	// Themes are borked.
	        	if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH){
	    		   this.setTheme((Globals.theme==1)?android.R.style.Theme_Holo_Light_DarkActionBar:android.R.style.Theme_Holo);
	    	   }
	        	super.onCreate(savedInstanceState);
	                addPreferencesFromResource(R.layout.settings);
	                prefs = PreferenceManager
	    	                .getDefaultSharedPreferences(getBaseContext());
	                themePref = (ListPreference) findPreference("fbTheme");
	                hiddenFold = (CheckBoxPreference) findPreference("hiddenSwitch");
	                showVids = (CheckBoxPreference) findPreference("videoSwitch");
	                defaultFoldPreference = findPreference("defFold");
	                reinstallSoundfont = findPreference("reSF");
	                manHomeFolder = (EditTextPreference) findPreference("defaultPath");
	                dataFoldPreference = findPreference("defData");
	                manDataFolder = (EditTextPreference) findPreference("dataDir");
	                manTcfg = (CheckBoxPreference) findPreference("manualConfig");
	                sfPref = findPreference("sfConfig");
	                resampMode = (ListPreference) findPreference("tplusResamp");
	                stereoMode = (ListPreference) findPreference("sdlChanValue");
	                bitMode = (ListPreference) findPreference("tplusBits");
	                rates = (ListPreference) findPreference("tplusRate");
	                bufferSize = (EditTextPreference) findPreference("tplusBuff");
	                //nativeMidi = (CheckBoxPreference) findPreference("nativeMidiSwitch");
	                //ds = (PreferenceScreen) findPreference("dsKey");
	                tplus = (PreferenceScreen) findPreference("tplusKey");
	                nativeMidi = (CheckBoxPreference) findPreference("nativeMidiSwitch");
	                sfPref.setEnabled(!manTcfg.isChecked());
	                
	                
	                hiddenFold.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){

						@Override
						public boolean onPreferenceChange(Preference arg0,
								Object arg1) {
								Globals.showHiddenFiles=(Boolean)arg1;
							return true;
						}
	                	
	                });
	                showVids.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){

						@Override
						public boolean onPreferenceChange(Preference arg0,
								Object arg1) {
								Globals.showVideos=(Boolean)arg1;
							return true;
						}
	                	
	                });
	                nativeMidi.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){

						@Override
						public boolean onPreferenceChange(Preference arg0,
								Object arg1) {
							if(!Globals.onlyNative)
								Globals.nativeMidi=(Boolean)arg1;
							else
								Globals.nativeMidi=true;
							return true;
						}
	                	
	                });
	                defaultFoldPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
	                        public boolean onPreferenceClick(Preference preference) {
	                            // dialog code here
	                        	
	                        	new FileBrowserDialog().create(3, null, SettingsActivity.this, SettingsActivity.this, SettingsActivity.this.getLayoutInflater(), true, prefs.getString("defaultPath", Environment.getExternalStorageDirectory().getAbsolutePath()));
	                            return true;
	                        }
	                    });
	                reinstallSoundfont.setOnPreferenceClickListener(new OnPreferenceClickListener(){

						@Override
						public boolean onPreferenceClick(Preference arg0) {
							AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
								
								ProgressDialog pd;
								@Override
								protected void onPreExecute() {
									 pd = new ProgressDialog(SettingsActivity.this);
									pd.setTitle(getResources().getString(R.string.extract));
									pd.setMessage(getResources().getString(R.string.extract_sum));
									pd.setCancelable(false);
									pd.setIndeterminate(true);
									pd.show();
								}
									
								@Override
								protected Void doInBackground(Void... arg0) {
									Globals.extract8Rock(getAssets());
									return null;
								}
								
								@Override
								protected void onPostExecute(Void result) {
									if (pd!=null) {
										pd.dismiss();
										Toast.makeText(SettingsActivity.this,getResources().getString(R.string.extract_def),Toast.LENGTH_LONG).show();
										//b.setEnabled(true);
									}
								}
									
							};
							task.execute((Void[])null);
							return true;
						}
	                	
	                });
	                dataFoldPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
	                        public boolean onPreferenceClick(Preference preference) {
	                            // dialog code here
	                        	needRestart=true;
	                        	new FileBrowserDialog().create(4, null, SettingsActivity.this, SettingsActivity.this, SettingsActivity.this.getLayoutInflater(), true, prefs.getString("defaultPath", Environment.getExternalStorageDirectory().getAbsolutePath()));
	                            return true;
	                        }
	                    });
	                
	                try {
						tmpSounds = (ArrayList<String>) ObjectSerializer.deserialize(prefs.getString("tplusSoundfonts", ObjectSerializer.serialize(new ArrayList<String>())));
					} catch (IOException e) {
						e.printStackTrace();
					}
	               
	                rates.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){

						@Override
						public boolean onPreferenceChange(
								Preference preference, Object newValue) {
							needRestart=true;
							String stereo = stereoMode.getValue();
							String sixteen = bitMode.getValue();
							boolean sb=(stereo!=null)?stereo.equals("2"):true;
							boolean sxb=(sixteen!=null)?sixteen.equals("16"):true;
							HashMap<Integer, Integer> mmm = Globals.validBuffers(Globals.validRates(sb,sxb),sb,sxb);
							if(mmm!=null)
							{
								
							int minBuff = mmm.get(Integer.parseInt((String) newValue));
							
							int buff = Integer.parseInt(bufferSize.getText());
							if(buff<minBuff)
							{
								prefs.edit().putString("tplusBuff",Integer.toString(minBuff)).commit();
								bufferSize.setText(Integer.toString(minBuff));
								Toast.makeText(SettingsActivity.this, getResources().getString(R.string.invalidbuff), Toast.LENGTH_SHORT).show();
								((BaseAdapter)tplus.getRootAdapter()).notifyDataSetChanged();
								((BaseAdapter)tplus.getRootAdapter()).notifyDataSetInvalidated();
							}
							}
							return true;
						}
	                });
	                if(tmpSounds == null)
	                	tmpSounds = new ArrayList<String>();
	                manTcfg.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){

						@Override
						public boolean onPreferenceChange(Preference arg0,
								Object arg1) {
							sfPref.setEnabled(!(Boolean)arg1);
							return true;
						}
	                	
	                });
	                sfPref.setOnPreferenceClickListener(new OnPreferenceClickListener(){

						@Override
						public boolean onPreferenceClick(Preference preference) {
							new SoundfontDialog().create(tmpSounds, SettingsActivity.this, SettingsActivity.this, getLayoutInflater(), prefs.getString("defaultPath", "/sdcard/"));
							return true;
						}
	                	
	                });
	                resampMode.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){

						@Override
						public boolean onPreferenceChange(
								Preference preference, Object newValue) {
							needRestart=true;
							return true;
						}
	                	
	                });
	                manDataFolder.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
	                	@Override
						public boolean onPreferenceChange(Preference preference, Object newValue) {
	                		needRestart=true;
							return true;
						}
	                });
	               // buffSize = Integer.parseInt(prefs.getString("tplusBuff", "192000"));
	                //System.out.println("Buffsize is: "+buffSize);
	                Globals.updateBuffers(Globals.updateRates());
	                int[] values = Globals.updateRates();
	                CharSequence[] hz = new CharSequence[values.length];
	                CharSequence[] hzItems = new CharSequence[values.length];
	                for(int i = 0; i<values.length; i++)
	                {
	                	hz[i]=Integer.toString(values[i])+"Hz";
	                	hzItems[i]=Integer.toString(values[i]);
	                }
	                rates.setEntries(hz);
	                rates.setEntryValues(hzItems);
	                rates.setDefaultValue(Integer.toString(AudioTrack.getNativeOutputSampleRate(AudioTrack.MODE_STREAM)));
	                rates.setValue(prefs.getString("tplusRate",Integer.toString(AudioTrack.getNativeOutputSampleRate(AudioTrack.MODE_STREAM))));
	                bufferSize.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){

						@Override
						public boolean onPreferenceChange(
								Preference preference, final Object newValue) {
							needRestart=true;
							String txt = (String)newValue;
							if(txt!=null)
							{
								if(!TextUtils.isEmpty(txt))
								{
									
									String stereo = stereoMode.getValue();
									String sixteen = bitMode.getValue();
									boolean sb=(stereo!=null)?stereo.equals("2"):true;
									boolean sxb=(sixteen!=null)?sixteen.equals("16"):true;
									HashMap<Integer, Integer> mmm = Globals.validBuffers(Globals.validRates(sb,sxb),sb,sxb);
									if(mmm!=null)
									{
										
									int minBuff = mmm.get(Integer.parseInt(rates.getValue()));
									
									int buff = Integer.parseInt(txt);
									if(buff<minBuff)
									{
										prefs.edit().putString("tplusBuff",Integer.toString(minBuff)).commit();
										((EditTextPreference)preference).setText(Integer.toString(minBuff));
										Toast.makeText(SettingsActivity.this, getResources().getString(R.string.invalidbuff), Toast.LENGTH_SHORT).show();
										((BaseAdapter)tplus.getRootAdapter()).notifyDataSetChanged();
										((BaseAdapter)tplus.getRootAdapter()).notifyDataSetInvalidated();
										return false;
									}
									}
									return true;
									//System.out.println("Text is change");
									//return Globals.updateBuffers(Globals.updateRates());
								}
							}
							return false;
						}
						
	                	
	                });
	                stereoMode.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){

						@Override
						public boolean onPreferenceChange(
								Preference preference, Object newValue) {
							needRestart=true;
							String stereo = (String) newValue;
							String sixteen = bitMode.getValue();
							boolean sb=(stereo!=null)?stereo.equals("2"):true;
							boolean sxb=(sixteen!=null)?sixteen.equals("16"):true;
							HashMap<Integer, Integer> mmm = Globals.validBuffers(Globals.validRates(sb,sxb),sb,sxb);
							if(mmm!=null)
							{
								
							int minBuff = mmm.get(Integer.parseInt(rates.getValue()));
							
							int buff = Integer.parseInt(bufferSize.getText());
							if(buff<minBuff)
							{
								prefs.edit().putString("tplusBuff",Integer.toString(minBuff)).commit();
								bufferSize.setText(Integer.toString(minBuff));
								Toast.makeText(SettingsActivity.this, getResources().getString(R.string.invalidbuff), Toast.LENGTH_SHORT).show();
								((BaseAdapter)tplus.getRootAdapter()).notifyDataSetChanged();
								((BaseAdapter)tplus.getRootAdapter()).notifyDataSetInvalidated();
							}
							}
							return true;
						}
	                });
	                bitMode.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){

						@Override
						public boolean onPreferenceChange(
								Preference preference, Object newValue) {
							needRestart=true;
							String stereo = stereoMode.getValue();
							String sixteen = (String) newValue;
							boolean sb=(stereo!=null)?stereo.equals("2"):true;
							boolean sxb=(sixteen!=null)?sixteen.equals("16"):true;
							HashMap<Integer, Integer> mmm = Globals.validBuffers(Globals.validRates(sb,sxb),sb,sxb);
							if(mmm!=null)
							{
								
							int minBuff = mmm.get(Integer.parseInt(rates.getValue()));
							
							int buff = Integer.parseInt(bufferSize.getText());
							if(buff<minBuff)
							{
								prefs.edit().putString("tplusBuff",Integer.toString(minBuff)).commit();
								bufferSize.setText(Integer.toString(minBuff));
								Toast.makeText(SettingsActivity.this, getResources().getString(R.string.invalidbuff), Toast.LENGTH_SHORT).show();
								((BaseAdapter)tplus.getRootAdapter()).notifyDataSetChanged();
								((BaseAdapter)tplus.getRootAdapter()).notifyDataSetInvalidated();
							}
							}
							return true;
						}
	                });
	                themePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){

						@Override
						public boolean onPreferenceChange(
								Preference preference, Object newValue) {
							Globals.theme=Integer.parseInt((String) newValue);
							Intent intent = getIntent();
				            finish();
				            startActivity(intent);							
				            return true;
						}
	                	
	                });
	                
	        }
	        
			@Override
	    	public boolean onKeyDown(int KeyCode, KeyEvent event) {
	    	    if(KeyCode == KeyEvent.KEYCODE_BACK){
	    	    	// Store the soundfonts
	    	    	try {
						prefs.edit().putString("tplusSoundfonts", ObjectSerializer.serialize(tmpSounds)).commit();
					} catch (IOException e) {
						e.printStackTrace();
					}
	    	    	if(needRestart)
	    	    		Toast.makeText(this, getResources().getString(R.string.restart), Toast.LENGTH_SHORT).show();
	    	    	
	    	    	if(needUpdateSf)
	    	    		Globals.writeCfg(Globals.dataFolder+"/timidity/timidity.cfg", tmpSounds);
					//Globals.reloadSettings(getBaseContext());
					Intent returnIntent = new Intent();
					setResult(3, returnIntent);
    	    		this.finish();
	    	    	return true;
	    	    }
	    	    	
	    	    return super.onKeyDown(KeyCode, event);
	    	    }

			
			@SuppressWarnings( "deprecation" )
			@Override
			public void setItem(String path, int type) {
				if(path!=null)
				{
					if(!TextUtils.isEmpty(path))
					{
						switch(type)
						{
							case 3:
								prefs.edit().putString("defaultPath", path).commit();
								manHomeFolder.setText(path);
								Globals.defaultFolder=path;
								((BaseAdapter)getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
							break;
							case 4:
								prefs.edit().putString("dataDir", path).commit();
								manDataFolder.setText(path);
								((BaseAdapter)getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
							break;
							case 5:
								//soundfont fun
							break;
						}
						return;
					}
				}	
					Toast.makeText(this, getResources().getString(R.string.invalidfold), Toast.LENGTH_SHORT).show();
			}

			@Override
			public void write() {}

			@Override
			public void ignore() {}

			@Override
			public void writeSoundfonts(ArrayList<String> l) {
				needRestart=true;
				needUpdateSf=true;
				tmpSounds = new ArrayList<String>(l.size());

				for (String foo: l) {
				  tmpSounds.add(foo);
				}
			}
	       
}
