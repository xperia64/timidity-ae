/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.UiModeManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.SparseIntArray;
import android.widget.Toast;

import com.xperia64.timidityae.JNIHandler;
import com.xperia64.timidityae.R;
import com.xperia64.timidityae.TimidityActivity;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

@SuppressLint("CommitPrefEdits")
public class SettingsStorage {
	// ---------SETTINGS STORAGE----------
	private static SharedPreferences prefs; // The preferences used by this class
	public static boolean firstRun;
	public static int theme; // 1 = Light, 2 = Dark
	public static boolean showHiddenFiles;
	public static String homeFolder;
	public static String dataFolder;
	public static boolean manualConfig;
	public static int defaultResamp;
	public static boolean shouldExtStorageNag;
	public static int channelMode; // 0 = stereo downsampled to mono, 1 = timidity-synthesized mono, 2 = stereo
	public static int audioRate;
	public static boolean nativeMidi;
	public static int volume;
	public static int bufferSize;
	public static int verbosity;
	public static boolean keepPartialWav;
	public static boolean onlyNative;
	public static boolean showVideos;
	public static boolean useDefaultBack;
	public static boolean compressCfg = true;
	public static boolean nukedWidgets;
	public static boolean reShuffle;
	public static boolean preserveSilence = true;
	public static boolean freeInsts = true;
	public static boolean isTV;
	public static boolean enableDragNDrop = true;

	public static boolean soxEnableSpeed = false;
	public static double soxSpeedVal = -1;

	public static boolean soxEnableTempo = false;
	public static double soxTempoVal = -1;

	public static boolean soxEnablePitch = false;
	public static int soxPitchVal = 0;

	public static boolean soxEnableDelay = false;
	public static double soxDelayL = 0;
	public static double soxDelayR = 0;

	public static String soxManCmd;

	public static String soxEffStr = "";

	public static boolean unsafeSoxSwitch = false;

	// Delay: d = disabled, l = Left, r = Right, b = Both 
	public static int delay = 0;
	public static int delayLevel = -1;

	// Chorus: d = disabled, n = normal, s = surround, level = [0,127]
	public static int chorus = 0;
	public static int chorusLevel = -1;

	// Reverb: d = disabled, n = normal, g = global, f = freeverb, G = global freeverb
	public static int reverb = 0;
	public static int reverbLevel = -1;

	public static boolean nativeMedia = true;

	public static void reloadSettings(Activity c, AssetManager assets) {

		prefs = PreferenceManager.getDefaultSharedPreferences(c);
		firstRun = prefs.getBoolean(Constants.sett_first_run, true);
		// The light theme is broken below ICS it seems.
		theme = (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)?0:Integer.parseInt(prefs.getString(Constants.sett_theme, "1"));
		showHiddenFiles = prefs.getBoolean(Constants.sett_show_hidden_files, false);
		homeFolder = prefs.getString(Constants.sett_home_folder, Environment.getExternalStorageDirectory().getAbsolutePath());
		dataFolder = prefs.getString(Constants.sett_data_folder, Environment.getExternalStorageDirectory() + "/TimidityAE/");
		manualConfig = prefs.getBoolean(Constants.sett_man_config, false);
		JNIHandler.currsamp = defaultResamp = Integer.parseInt(prefs.getString(Constants.sett_default_resamp, "0"));
		channelMode = Integer.parseInt(prefs.getString(Constants.sett_channel_mode, "2"));
		audioRate = Integer.parseInt(prefs.getString(Constants.sett_audio_rate, Integer.toString(AudioTrack.getNativeOutputSampleRate(AudioTrack.MODE_STREAM))));
		volume = Integer.parseInt(prefs.getString(Constants.sett_vol, "70"));
		bufferSize = Integer.parseInt(prefs.getString(Constants.sett_buffer_size, "192000"));
		showVideos = prefs.getBoolean(Constants.sett_show_videos, true);
		shouldExtStorageNag = prefs.getBoolean(Constants.sett_should_ext_storage_nag, true);
		verbosity = Integer.parseInt(prefs.getString(Constants.sett_t_verbosity, "-1"));
		keepPartialWav = prefs.getBoolean(Constants.sett_keep_partal_wave, false);
		useDefaultBack = prefs.getBoolean(Constants.sett_default_back_btn, false);
		compressCfg = prefs.getBoolean(Constants.sett_compress_midi_cfg, true);
		reShuffle = prefs.getBoolean(Constants.sett_reshuffle_plist, false);
		freeInsts = prefs.getBoolean(Constants.sett_free_insts, true);
		preserveSilence = prefs.getBoolean(Constants.sett_preserve_silence, true);
		enableDragNDrop = prefs.getBoolean(Constants.sett_fancy_plist, true);
		nativeMidi = onlyNative || prefs.getBoolean(Constants.sett_native_midi, false);

		nativeMedia = prefs.getBoolean(Constants.sett_native_media, true);

		soxEnableSpeed = prefs.getBoolean(Constants.sett_sox_speed, false);
		soxSpeedVal = prefs.getFloat(Constants.sett_sox_speed_val, -1);
		soxEnableTempo = prefs.getBoolean(Constants.sett_sox_tempo, false);
		soxTempoVal = prefs.getFloat(Constants.sett_sox_tempo_val, -1);
		soxEnablePitch = prefs.getBoolean(Constants.sett_sox_pitch, false);
		soxPitchVal = prefs.getInt(Constants.sett_sox_pitch_val, 0);
		soxEnableDelay = prefs.getBoolean(Constants.sett_sox_delay, false);
		soxDelayL = prefs.getFloat(Constants.sett_sox_delay_valL, 0);
		soxDelayR = prefs.getFloat(Constants.sett_sox_delay_valR, 0);
		soxManCmd = prefs.getString(Constants.sett_sox_mancmd, "");

		soxEffStr = prefs.getString(Constants.sett_sox_fullcmd, "");

		unsafeSoxSwitch = prefs.getBoolean(Constants.sett_sox_unsafe, false);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			UiModeManager uiModeManager = (UiModeManager) c.getSystemService(Context.UI_MODE_SERVICE);
			isTV = (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION);
		}

	}
	// -----------------------------------

	public static int[] updateRates() {
		if (prefs != null) {
			int[] values = validRates(prefs.getString(Constants.sett_channel_mode, "2").equals("2"), true);
			//CharSequence[] hz = new CharSequence[values.length];
			CharSequence[] hzItems = new CharSequence[values.length];
			boolean validRate = false;
			for (int i = 0; i < values.length; i++) {
				//hz[i] = Integer.toString(values[i]) + "Hz";
				hzItems[i] = Integer.toString(values[i]);
				if (prefs.getString("tplusRate", Integer.toString(AudioTrack.getNativeOutputSampleRate(AudioTrack.MODE_STREAM))).equals(hzItems[i])) {
					validRate = true;
					break;
				}
			}

			if (!validRate)
				prefs.edit().putString("tplusRate", Integer.toString(AudioTrack.getNativeOutputSampleRate(AudioTrack.MODE_STREAM))).commit();

			return values;
		}
		return null;
	}

	public static boolean updateBuffers(int[] rata) {
		if (rata != null) {
			SparseIntArray buffMap = validBuffers(rata, prefs.getString(Constants.sett_channel_mode, "2").equals("2"), true);
			int realMin = buffMap.get(Integer.parseInt(prefs.getString(Constants.sett_audio_rate, Integer.toString(AudioTrack.getNativeOutputSampleRate(AudioTrack.MODE_STREAM)))));
			if (bufferSize < realMin) {
				prefs.edit().putString(Constants.sett_buffer_size, Integer.toString(bufferSize = realMin)).commit();
				return false;
			}
		}
		return true;
	}

	public static void disableLollipopStorageNag() {
		prefs.edit().putBoolean(Constants.sett_should_ext_storage_nag, shouldExtStorageNag = false).commit();
	}

	public static boolean initialize(final Activity a) {
		if (firstRun) {
			final File rootStorage = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/TimidityAE/");

			// Create TimidityAE's default data folder
			if (!rootStorage.exists()) {
				rootStorage.mkdir();
			}
			// Create the playlist folder
			File playlistDir = new File(rootStorage.getAbsolutePath() + "/playlists/");
			if (!playlistDir.exists()) {
				playlistDir.mkdir();
			}
			File tcfgDir = new File(rootStorage.getAbsolutePath() + "/timidity/");
			if (!tcfgDir.exists()) {
				tcfgDir.mkdir();
			}
			File sfDir = new File(rootStorage.getAbsolutePath() + "/soundfonts/");
			if (!sfDir.exists()) {
				sfDir.mkdir();
			}
			updateBuffers(updateRates());
			audioRate = Integer.parseInt(prefs.getString("tplusRate", Integer.toString(AudioTrack.getNativeOutputSampleRate(AudioTrack.MODE_STREAM))));
			// This is usually a safe number, but should probably do a test or something
			bufferSize = Integer.parseInt(prefs.getString("tplusBuff", "192000"));
			migrateFrom1X(rootStorage);
			final Editor eee = prefs.edit();
			firstRun = false;
			eee.putBoolean("tplusFirstRun", false);
			eee.putString("dataDir", Environment.getExternalStorageDirectory().getAbsolutePath() + "/TimidityAE/");
			if (new File(dataFolder + "/timidity/timidity.cfg").exists()) {
				if (manualConfig = !cfgIsAuto(dataFolder + "/timidity/timidity.cfg")) {
					eee.putBoolean("manConfig", true);
				} else {
					eee.putBoolean("manConfig", false);
					ArrayList<String> soundfonts = new ArrayList<String>();
					FileInputStream fstream = null;
					try {
						fstream = new FileInputStream(dataFolder + "/timidity/timidity.cfg");
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
					// Get the object of DataInputStream
					DataInputStream in = new DataInputStream(fstream);
					BufferedReader br = new BufferedReader(new InputStreamReader(in));
					// Read File Line By Line
					try {
						br.readLine(); // skip first line
					} catch (IOException e) {
						e.printStackTrace();
					}
					String line;
					try {
						while ((line = br.readLine()) != null) {
							if (line.contains("soundfont \"") && line.lastIndexOf('"') >= 0) {
								try {
									String st = line.substring(line.indexOf("soundfont \"") + 11, line.lastIndexOf('"'));
									soundfonts.add(st);
								} catch (ArrayIndexOutOfBoundsException e1) {
									e1.printStackTrace();
								}
							} else if (line.indexOf("#extension opt ") == 0) {
								if (line.indexOf("--no-unload-instruments") > 0) {
									SettingsStorage.freeInsts = false;
								}/*else if(line.indexOf("")>0)
								{
									
								}else if(line.indexOf("")>0)
								{
									
								}*/
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						eee.putString(Constants.sett_soundfonts, ObjectSerializer.serialize(soundfonts));
						eee.putBoolean(Constants.sett_free_insts, SettingsStorage.freeInsts);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				eee.commit();
				return true;
			} else {
				// Should probably check if 8rock11e exists no matter what
				eee.putBoolean("manConfig", false);

				AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {

					ProgressDialog pd;

					@Override
					protected void onPreExecute() {
						pd = new ProgressDialog(a);
						pd.setTitle(a.getResources().getString(R.string.extract));
						pd.setMessage(a.getResources().getString(R.string.extract_sum));
						pd.setCancelable(false);
						pd.setIndeterminate(true);
						pd.show();
					}

					@Override
					protected Integer doInBackground(Void... arg0) {
						return Globals.extract8Rock(a);
					}

					@Override
					protected void onPostExecute(Integer result) {
						if (pd != null) {
							pd.dismiss();
							if (result != 777) {
								Toast.makeText(a, a.getResources().getString(R.string.sett_resf_err), Toast.LENGTH_SHORT).show();
								return;
							}
						}
						ArrayList<String> tmpConfig = new ArrayList<String>();
						tmpConfig.add(rootStorage.getAbsolutePath() + "/soundfonts/8Rock11e.sf2");
						try {
							eee.putString(Constants.sett_soundfonts, ObjectSerializer.serialize(tmpConfig));
						} catch (IOException e) {
							e.printStackTrace();
						}
						eee.commit();
						writeCfg(a, rootStorage.getAbsolutePath() + "/timidity/timidity.cfg", tmpConfig);
						((TimidityActivity) a).initCallback();
					}

				};
				task.execute((Void[]) null);
				return false;
			}

		} else {
			return true;
		}
	}

	// This can probably be removed soon.
	private static void migrateFrom1X(File newData) {
		File oldPlists = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/com.xperia64.timidityae/playlists/");
		if (oldPlists.exists()) {
			if (oldPlists.isDirectory()) {
				for (File f : oldPlists.listFiles()) {
					if (f.getName().toLowerCase(Locale.US).endsWith(".tpl")) {
						f.renameTo(new File(newData.getAbsolutePath() + "/playlists/" + f.getName()));
					}
				}
			}
		}
		File oldSoundfonts = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/com.xperia64.timidityae/soundfonts/");
		if (oldSoundfonts.exists()) {
			if (oldSoundfonts.isDirectory()) {
				for (File f : oldSoundfonts.listFiles()) {
					if (f.getName().toLowerCase(Locale.US).endsWith(".sf2") || f.getName().toLowerCase(Locale.US).endsWith(".sfark")) {
						f.renameTo(new File(newData.getAbsolutePath() + "/soundfonts/" + f.getName()));
					}
				}
			}
		}
	}

	public static void writeCfg(Context c, String path, ArrayList<String> soundfonts) {
		if (path == null) {
			Toast.makeText(c, "Configuration path null (3)", Toast.LENGTH_LONG).show();
			return;
		}
		if (soundfonts == null) {
			Toast.makeText(c, "Soundfonts null (4)", Toast.LENGTH_LONG).show();
			return;
		}
		path = path.replaceAll(Globals.repeatedSeparatorString, "/");
		if (!manualConfig) {
			String[] needLol = null;
			try {
				new FileOutputStream(path, true).close();
			} catch (FileNotFoundException e) {
				needLol = DocumentFileUtils.getExternalFilePaths(c, path);
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && needLol != null) {
				if (DocumentFileUtils.docFileDevice != null) {
					String probablyTheDirectory = needLol[0];
					String probablyTheRoot = needLol[1];
					String needRename;
					String value;
					if (probablyTheDirectory.length() > 1) {
						needRename = path.substring(path.indexOf(probablyTheRoot) + probablyTheRoot.length());
						value = probablyTheDirectory + path.substring(path.lastIndexOf('/'));
					} else {
						return;
					}
					if (new File(path).exists()) {
						if (cfgIsAuto(path) || new File(path).length() <= 0) {
							DocumentFileUtils.tryToDeleteFile(c, path);
						} else {
							Toast.makeText(c, "Renaming manually edited cfg... (7)", Toast.LENGTH_LONG).show();
							DocumentFileUtils.renameDocumentFile(c, path, needRename + ".manualTimidityCfg." + Long.toString(System.currentTimeMillis()));
						}
					}

					FileWriter fw = null;
					try {
						fw = new FileWriter(value, false);
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						fw.write(Globals.autoSoundfontHeader + "\n");
					} catch (IOException e) {
						e.printStackTrace();
					}

					for (String s : soundfonts) {
						try {
							fw.write((s.startsWith("#") ? "#" : "") + "soundfont \"" + s + "\"\n");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					try {
						fw.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					DocumentFileUtils.renameDocumentFile(c, value, needRename);
				} else {
					Toast.makeText(c, "Could not write configuration file. Does Timidity AE have write access to the data folder? (1)", Toast.LENGTH_LONG).show();
				}

			} else {

				File theConfig = new File(path);
				if (theConfig.exists()) // It should exist if we got here.
				{
					if (!theConfig.canWrite()) {
						Toast.makeText(c, "Could not write configuration file. Does Timidity AE have write access to the data folder? (2)", Toast.LENGTH_LONG).show();
						return;
					}
					if (cfgIsAuto(path) || theConfig.length() <= 0) // Negative file length? Who knows.
					{
						theConfig.delete(); // Auto config, safe to delete
					} else {
						Toast.makeText(c, "Renaming manually edited cfg... (6)", Toast.LENGTH_LONG).show();
						theConfig.renameTo(new File(path + ".manualTimidityCfg." + Long.toString(System.currentTimeMillis()))); // manual config, rename for later
					}
				} else {
					File parent = new File(path.substring(0, path.lastIndexOf(File.separator)));
					if (!parent.mkdirs() && !parent.isDirectory()) {
						Toast.makeText(c, "Error writing config. Make sure data directory is writable (7)", Toast.LENGTH_LONG).show();
						return;
					}
				}
				FileWriter fw = null;
				try {
					fw = new FileWriter(path, false);
				} catch (IOException e) {

					e.printStackTrace();
				}
				if (fw == null) {
					Toast.makeText(c, "Error writing config. Make sure data directory is writable (8)", Toast.LENGTH_LONG).show();
					return;
				}
				try {
					fw.write(Globals.autoSoundfontHeader + "\n");
				} catch (IOException e) {
					e.printStackTrace();
				}

				for (String s : soundfonts) {
					if (s == null)
						continue;
					try {
						fw.write((s.startsWith("#") ? "#" : "") + "soundfont \"" + s + "\"\n");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				try {
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static boolean cfgIsAuto(String path) {
		String firstLine = "";
		try {
			FileInputStream fstream = new FileInputStream(path);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			firstLine = br.readLine();

			in.close();
		} catch (Exception ignored) {}
		return firstLine != null && firstLine.contains(Globals.autoSoundfontHeader);
	}

	public static int[] validRates(boolean stereo, boolean sixteen) {
		ArrayList<Integer> valid = new ArrayList<>();
		for (int rate : new int[]{8000, 11025, 16000, 22050, 44100, 48000, 88200, 96000}) {

			int bufferSize = AudioTrack.getMinBufferSize(rate, (stereo) ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO, (sixteen) ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT);
			if (bufferSize > 0) {
				// buffer size is valid, Sample rate supported
				valid.add(rate);
			}
		}
		int[] rates = new int[valid.size()];
		for (int i = 0; i < rates.length; i++)
			rates[i] = valid.get(i);
		return rates;
	}

	public static SparseIntArray validBuffers(int[] rates, boolean stereo, boolean sixteen) {
		SparseIntArray buffers = new SparseIntArray();
		for (int rate : rates) {
			buffers.put(rate, AudioTrack.getMinBufferSize(rate, (stereo) ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO, (sixteen) ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT));
		}
		return buffers;
	}
	/*
	 * public static boolean canWrite(String path) { if(!path.endsWith("/")) { return false; } if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) { Random r = new Random(); // Generate a random unique temporary file. File f = new File(path+r.nextInt(1000000)); while(f.exists()) { f = new File(path+r.nextInt(1000000)); try { Thread.sleep(10); } catch (InterruptedException e) { e.printStackTrace(); } } try { f.createNewFile(); } catch (IOException e) { return false; } if(f.exists())
	 * f.delete(); return true; }else{ return new File(path).canWrite(); } }
	 */
	/*
	 * public static boolean canWrite(DocumentFile path) { return false; }
	 */

}
