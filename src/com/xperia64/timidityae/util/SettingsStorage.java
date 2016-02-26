package com.xperia64.timidityae.util;

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

import com.xperia64.timidityae.JNIHandler;
import com.xperia64.timidityae.R;
import com.xperia64.timidityae.TimidityActivity;

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
	public static boolean sixteenBit;
	public static int audioRate;
	public static boolean nativeMidi;
	public static int bufferSize;
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
	
	@SuppressLint("NewApi")
	public static void reloadSettings(Activity c, AssetManager assets) {

		prefs = PreferenceManager.getDefaultSharedPreferences(c);
		firstRun = prefs.getBoolean(CommandStrings.sett_first_run, true);
		theme = Integer.parseInt(prefs.getString(CommandStrings.sett_theme, "1"));
		showHiddenFiles = prefs.getBoolean(CommandStrings.sett_show_hidden_files, false);
		homeFolder = prefs.getString(CommandStrings.sett_home_folder, Environment.getExternalStorageDirectory().getAbsolutePath());
		dataFolder = prefs.getString(CommandStrings.sett_data_folder, Environment.getExternalStorageDirectory() + "/TimidityAE/");
		manualConfig = prefs.getBoolean(CommandStrings.sett_man_config, false);
		JNIHandler.currsamp = defaultResamp = Integer.parseInt(prefs.getString(CommandStrings.sett_default_resamp, "0"));
		channelMode = Integer.parseInt(prefs.getString(CommandStrings.sett_channel_mode, "2"));
		sixteenBit = true;// prefs.getString("tplusBits", "16").equals("16");
		audioRate = Integer.parseInt(prefs.getString(CommandStrings.sett_audio_rate, Integer.toString(AudioTrack.getNativeOutputSampleRate(AudioTrack.MODE_STREAM))));
		bufferSize = Integer.parseInt(prefs.getString(CommandStrings.sett_buffer_size, "192000"));
		showVideos = prefs.getBoolean(CommandStrings.sett_show_videos, true);
		shouldExtStorageNag = prefs.getBoolean(CommandStrings.sett_should_ext_storage_nag, true);
		keepPartialWav = prefs.getBoolean(CommandStrings.sett_keep_partal_wave, false);
		useDefaultBack = prefs.getBoolean(CommandStrings.sett_default_back_btn, false);
		compressCfg = prefs.getBoolean(CommandStrings.sett_compress_midi_cfg, true);
		reShuffle = prefs.getBoolean(CommandStrings.sett_reshuffle_plist, false);
		freeInsts = prefs.getBoolean(CommandStrings.sett_free_insts, true);
		preserveSilence = prefs.getBoolean(CommandStrings.sett_preserve_silence, true);
		enableDragNDrop = prefs.getBoolean(CommandStrings.sett_fancy_plist, true);
		if (!onlyNative)
			nativeMidi = prefs.getBoolean(CommandStrings.sett_native_midi, false);
		else
			nativeMidi = true;
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			UiModeManager uiModeManager = (UiModeManager) c.getSystemService(Context.UI_MODE_SERVICE);
			isTV = (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION);
		}

	}
	// -----------------------------------

	public static int[] updateRates() {
		if (prefs != null) {
			int[] values = validRates(prefs.getString(CommandStrings.sett_channel_mode, "2").equals("2"), true);
			CharSequence[] hz = new CharSequence[values.length];
			CharSequence[] hzItems = new CharSequence[values.length];
			boolean validRate = false;
			for (int i = 0; i < values.length; i++) {
				hz[i] = Integer.toString(values[i]) + "Hz";
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
			SparseIntArray buffMap = validBuffers(rata, prefs.getString(CommandStrings.sett_channel_mode, "2").equals("2"), true);
			int realMin = buffMap.get(Integer.parseInt(prefs.getString(CommandStrings.sett_audio_rate, Integer.toString(AudioTrack.getNativeOutputSampleRate(AudioTrack.MODE_STREAM)))));
			if (bufferSize < realMin) {
				prefs.edit().putString(CommandStrings.sett_buffer_size, Integer.toString(bufferSize = realMin)).commit();
				return false;
			}
		}
		return true;
	}

	public static void disableLollipopStorageNag() {
		prefs.edit().putBoolean(CommandStrings.sett_should_ext_storage_nag, shouldExtStorageNag = false).commit();
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
							if (line.indexOf("soundfont \"") >= 0 && line.lastIndexOf('"') >= 0) {
								try {
									String st = line.substring(line.indexOf("soundfont \"") + 11, line.lastIndexOf('"'));
									soundfonts.add(st);
								} catch (ArrayIndexOutOfBoundsException e1) {
									e1.printStackTrace();
								}

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
						eee.putString("tplusSoundfonts", ObjectSerializer.serialize(soundfonts));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				eee.commit();
				return true;
			} else {
				// Should probably check if 8rock11e exists no matter what
				eee.putBoolean("manConfig", false);

				AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

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
					protected Void doInBackground(Void... arg0) {

						if (Globals.extract8Rock(a) != 777) {
							Toast.makeText(a, "Could not extrct default soundfont", Toast.LENGTH_SHORT).show();
						}
						return null;
					}

					@Override
					protected void onPostExecute(Void result) {
						if (pd != null)
							pd.dismiss();
						ArrayList<String> tmpConfig = new ArrayList<String>();
						tmpConfig.add(rootStorage.getAbsolutePath() + "/soundfonts/8Rock11e.sf2");
						try {
							eee.putString("tplusSoundfonts", ObjectSerializer.serialize(tmpConfig));
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
	public static void migrateFrom1X(File newData) {
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
		path = path.replaceAll("[/]+", "/");
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
					String needRename = null;
					String value = null;
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
				}
				FileWriter fw = null;
				try {
					fw = new FileWriter(path, false);
				} catch (IOException e) {

					e.printStackTrace();
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

	public static boolean cfgIsAuto(String path) {
		String firstLine = "";
		try {
			FileInputStream fstream = new FileInputStream(path);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			firstLine = br.readLine();

			in.close();
		} catch (Exception e) {
		}
		if (firstLine != null)
			return firstLine.contains(Globals.autoSoundfontHeader);
		return false;
	}

	public static int[] validRates(boolean stereo, boolean sixteen) {
		ArrayList<Integer> valid = new ArrayList<Integer>();
		for (int rate : new int[] { 8000, 11025, 16000, 22050, 44100, 48000, 88200, 96000 }) {

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
