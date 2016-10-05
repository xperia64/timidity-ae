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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.xperia64.timidityae.R;
import com.xperia64.timidityae.gui.TimidityAEWidgetProvider;
import com.xperia64.timidityae.util.Globals;
import com.xperia64.timidityae.util.ObjectSerializer;
import com.xperia64.timidityae.util.CommandStrings;
import com.xperia64.timidityae.util.SettingsStorage;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
//import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
//import android.content.ComponentName;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;
//import android.util.Log;
import android.view.KeyEvent;
import android.widget.RemoteViews;
import android.widget.Toast;

public class MusicService extends Service {

	public ArrayList<String> playList;
	public SparseIntArray shuffledIndices;
	public SparseIntArray reverseShuffledIndices; // HashMap<Integer, Integer>
	public int currSongNumber = -1;
	public int realSongNumber = -1;
	public boolean shouldStart;
	public boolean shouldAdvance = true;
	public String currFold;
	public int loopMode = 1;
	public int shuffleMode = 0;
	public boolean paused;
	public boolean fullStop = false;
	public boolean foreground = false;
	public boolean fixedShuffle = true;
	boolean death = false;
	boolean phonepause = false;
	PowerManager.WakeLock wl;
	boolean shouldDoWidget = true;
	int[] widgetIds;
	String currTitle;
	Notification mainNotification;
	PowerManager pm;
	RemoteViews remoteViews;
	Random random = new Random(System.currentTimeMillis());
	private final IBinder musicBind = new MusicBinder();

	@Override
	public IBinder onBind(Intent arg0) {
		return musicBind;
	}

	public class MusicBinder extends Binder {
		MusicService getService() {
			return MusicService.this;
		}
	}

	private Handler handler;

	@SuppressLint("NewApi")
	@Override
	public void onTaskRemoved(Intent rootIntent) {
		
		// Android versions less than M kill the Service if the app is removed for some reason.
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
		{
			Intent intent = new Intent(this, DummyActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			super.onTaskRemoved(rootIntent);
		}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handler = new Handler();
		return START_NOT_STICKY;
		// return super.onStartCommand(intent, flags, startId);
	}

	PhoneStateListener phoneStateListener = new PhoneStateListener() {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			if (state == TelephonyManager.CALL_STATE_RINGING) {
				// Incoming call: Pause music
				if (JNIHandler.isPlaying && !JNIHandler.paused && !phonepause && !(JNIHandler.currentWavWriter != null && !JNIHandler.currentWavWriter.finishedWriting)) {
					phonepause = true;
					pause();
				}
			} else if (state == TelephonyManager.CALL_STATE_IDLE) {
				// Not in call: Play music
				if (JNIHandler.isPlaying && JNIHandler.paused && phonepause) {
					phonepause = false;
					pause();
				}
			} else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
				// A call is dialing, active or on hold
			}
			super.onCallStateChanged(state, incomingNumber);
		}
	};

	public void genShuffledPlist() {
		if (playList != null) {
			shuffledIndices = new SparseIntArray();
			reverseShuffledIndices = new SparseIntArray();
			ArrayList<Integer> tmp = new ArrayList<Integer>();
			for (int i = 0; i < playList.size(); i++) {
				tmp.add(i);
			}
			Collections.shuffle(tmp);

			for (int i = 0; i < playList.size(); i++) {
				shuffledIndices.put(i, tmp.get(i));
				reverseShuffledIndices.put(tmp.get(i), i);
			}
		}

	}

	private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {

		@SuppressWarnings("unchecked")
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
				KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					switch (event.getKeyCode()) {
					case KeyEvent.KEYCODE_MEDIA_PLAY:
					case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
					case KeyEvent.KEYCODE_MEDIA_PAUSE:
						if (JNIHandler.isPlaying) {
							pause();
						} else {
							play();
						}
						break;
					case KeyEvent.KEYCODE_MEDIA_NEXT:
						shouldAdvance = false;
						next();
						break;
					case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
						shouldAdvance = false;
						previous();
						break;
					case KeyEvent.KEYCODE_MEDIA_STOP:
						fullStop = true;
						shouldAdvance = false;
						stop();
						break;
					}
				}
			} else if (Intent.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
				if (JNIHandler.isPlaying && !JNIHandler.paused && intent.getIntExtra("state", -1) == 0) {
					pause();
				}
			} else {
				int cmd = intent.getIntExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_error); // V
				// System.out.println("Cmd received: "+cmd);

				// Sigh. Here we go:
				Intent outgoingIntent = new Intent();
				switch (cmd) {
				case CommandStrings.msrv_cmd_load_plist_play: // We have a new playlist. Load it and the immediate song to load.
					death = true;
					ArrayList<String> tmpList = Globals.plist;
					if (tmpList == null) {
						break;
					}
					int tmpNum = intent.getIntExtra(CommandStrings.msrv_songnum, -1);
					if (tmpNum <= -1) {
						break;
					}
					boolean shouldCopyPlist = intent.getBooleanExtra(CommandStrings.msrv_cpplist, true);
					if (shouldCopyPlist) {
						currSongNumber = realSongNumber = tmpNum;
						playList = tmpList;
						genShuffledPlist();
						
					} else {
						currSongNumber = tmpNum;
						if (shuffleMode == 1) {
							realSongNumber = shuffledIndices.get(tmpNum);
						}
					}
					Globals.plist = null;
					tmpList = null;
					currFold = intent.getStringExtra(CommandStrings.msrv_currfold);

					if (shuffleMode == 1 && shouldCopyPlist) {
						currSongNumber = reverseShuffledIndices.get(realSongNumber);
					}
					outgoingIntent.setAction(CommandStrings.ta_rec);
					outgoingIntent.putExtra(CommandStrings.ta_cmd, CommandStrings.ta_cmd_copy_plist);
					if (shuffleMode == 1) {
						Globals.tmpplist = new ArrayList<String>();
						for (int i = 0; i < playList.size(); i++) {
							Globals.tmpplist.add(playList.get(shuffledIndices.get(i)));
						}
					} else {
						Globals.tmpplist = playList;
					}
					sendBroadcast(outgoingIntent);
					// if(shouldStart=intent.getBooleanExtra(ServiceStrings.msrv_begin),false)||true)
					// {
					play();
					// }
					break;
				case CommandStrings.msrv_cmd_play: // Initial play cmd
					play();
					break;
				case CommandStrings.msrv_cmd_pause: // Play/pause
					pause();
					break;
				case CommandStrings.msrv_cmd_next: // Next
					shouldAdvance = false;
					next();
					break;
				case CommandStrings.msrv_cmd_prev: // Previous
					shouldAdvance = false;
					previous();
					break;
				case CommandStrings.msrv_cmd_stop: // Stop
					fullStop = true;
					Globals.hardStop = true;
					shouldAdvance = false;
					stop();
					break;
				case CommandStrings.msrv_cmd_loop_mode: // Loop mode
					int tmpMode = intent.getIntExtra(CommandStrings.msrv_loopmode, -1);
					if (tmpMode < 0 || tmpMode > 2)
						break;
					loopMode = tmpMode;
					break;
				case CommandStrings.msrv_cmd_shuf_mode: // Shuffle mode
					if (playList == null) {
						break;
					}
					shuffleMode = intent.getIntExtra(CommandStrings.msrv_shufmode, 0);
					if (shuffleMode == 1) {
						fixedShuffle = false;
						if (SettingsStorage.reShuffle || reverseShuffledIndices == null || shuffledIndices == null) {
							genShuffledPlist();
						}
						if (reverseShuffledIndices == null) {
							break;
						}
						currSongNumber = reverseShuffledIndices.get(currSongNumber);
					} else {
						if (!fixedShuffle) {
							currSongNumber = realSongNumber;
							fixedShuffle = true;
						}
					}
					outgoingIntent.setAction(CommandStrings.ta_rec);
					outgoingIntent.putExtra(CommandStrings.ta_cmd, CommandStrings.ta_cmd_copy_plist);
					outgoingIntent.putExtra(CommandStrings.ta_highlight, currSongNumber);
					if (shuffleMode == 1) {
						Globals.tmpplist = new ArrayList<String>();
						for (int i = 0; i < playList.size(); i++) {
							Globals.tmpplist.add(playList.get(shuffledIndices.get(i)));
						}
					} else {
						Globals.tmpplist = playList;
					}
					sendBroadcast(outgoingIntent);
					break;
				/*
				 * case ServiceStrings.msrv_cmd_req_time: // Request seekBar times break;
				 */
				case CommandStrings.msrv_cmd_seek: // Actually seek
					JNIHandler.seekTo(intent.getIntExtra(CommandStrings.msrv_seektime, 1));
					break;
				case CommandStrings.msrv_cmd_get_fold: // Request current folder
					outgoingIntent.setAction(CommandStrings.ta_rec);
					outgoingIntent.putExtra(CommandStrings.ta_cmd, CommandStrings.ta_cmd_load_filebrowser);
					outgoingIntent.putExtra(CommandStrings.ta_currpath, currFold);
					sendBroadcast(outgoingIntent);
					break;
				case CommandStrings.msrv_cmd_get_info: // Request player info
					outgoingIntent.setAction(CommandStrings.ta_rec);
					outgoingIntent.putExtra(CommandStrings.ta_cmd, CommandStrings.ta_cmd_gui_play_full);
					outgoingIntent.putExtra(CommandStrings.ta_startt, JNIHandler.maxTime);
					outgoingIntent.putExtra(CommandStrings.ta_shufmode, shuffleMode);
					outgoingIntent.putExtra(CommandStrings.ta_loopmode, loopMode);
					outgoingIntent.putExtra(CommandStrings.ta_songttl, currTitle);
					if (shuffleMode == 1) {
						outgoingIntent.putExtra(CommandStrings.ta_filename, playList.get(shuffledIndices.get(currSongNumber)));
					} else {
						outgoingIntent.putExtra(CommandStrings.ta_filename, playList.get(currSongNumber));
					}

					sendBroadcast(outgoingIntent);
					break;
				case CommandStrings.msrv_cmd_get_plist: // Request playlist
					outgoingIntent.setAction(CommandStrings.ta_rec);
					outgoingIntent.putExtra(CommandStrings.ta_cmd, CommandStrings.ta_cmd_copy_plist);
					if (shuffleMode == 1) {
						Globals.tmpplist = new ArrayList<String>();
						for (int i = 0; i < playList.size(); i++) {
							Globals.tmpplist.add(playList.get(shuffledIndices.get(i)));
						}
					} else {
						Globals.tmpplist = playList;
					}
					sendBroadcast(outgoingIntent);
					break;
				case CommandStrings.msrv_cmd_play_or_pause:
					if (JNIHandler.isPlaying) {
						pause();
					} else {
						play();
					}
					break;
				case CommandStrings.msrv_cmd_write_new: // We want to write an output file
					fullStop = true;
					shouldAdvance = false;
					Globals.hardStop = true;
					stop();
					while (((JNIHandler.isPlaying || JNIHandler.isBlocking))) {
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					final String input = intent.getStringExtra(CommandStrings.msrv_infile);
					String output = intent.getStringExtra(CommandStrings.msrv_outfile);
					if (input != null && output != null) {
						JNIHandler.setupOutputFile(output);
						JNIHandler.play(input);
					}
					new Thread(new Runnable() {

						@Override
						public void run() {
							while (!death && ((!JNIHandler.isPlaying))) {
								if (!JNIHandler.isBlocking) {
									death = true;
								}
								try {
									Thread.sleep(10);
								} catch (InterruptedException e) {
								}
							}
							if (new File(input + ".def.tcf").exists() || new File(input + ".def.tzf").exists()) {
								String suffix;
								if (new File(input + ".def.tcf").exists() && new File(input + ".def.tzf").exists()) {
									suffix = (SettingsStorage.compressCfg ? ".def.tzf" : ".def.tcf");
								} else if (new File(input + ".def.tcf").exists()) {
									suffix = ".def.tcf";
								} else {
									suffix = ".def.tzf";
								}
								JNIHandler.shouldPlayNow = false;
								JNIHandler.currTime = 0;
								while (JNIHandler.isPlaying && !death && !JNIHandler.dataWritten) {
									try {
										Thread.sleep(25);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								}
								Intent loadCfgIntent = new Intent(); // silly, but should be done async. I think.
								loadCfgIntent.setAction(CommandStrings.msrv_rec);
								loadCfgIntent.putExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_load_cfg);
								loadCfgIntent.putExtra(CommandStrings.msrv_infile, input + suffix);
								loadCfgIntent.putExtra(CommandStrings.msrv_reset, true);
								sendBroadcast(loadCfgIntent);
							}
							while (((JNIHandler.isPlaying))) {
								try {
									Thread.sleep(25);
								} catch (InterruptedException e) {
								}
							}

							Intent outgoingIntent = new Intent();
							outgoingIntent.setAction(CommandStrings.ta_rec);
							outgoingIntent.putExtra(CommandStrings.ta_cmd, CommandStrings.ta_cmd_special_notification_finished);
							sendBroadcast(outgoingIntent);
							death = false;
						}
					}).start();
					// play();
					break;
				case CommandStrings.msrv_cmd_write_curr: // We want to write an output file while playing.
					shouldAdvance = false;
					if (JNIHandler.paused) {
						JNIHandler.pause();
						JNIHandler.waitUntilReady(50);
					}
					JNIHandler.seekTo(0); // Why is this async. Seriously.
					JNIHandler.waitUntilReady();
					JNIHandler.pause();
					JNIHandler.waitUntilReady();
					String output2 = intent.getStringExtra(CommandStrings.msrv_outfile);
					if (JNIHandler.isPlaying && output2 != null) {
						JNIHandler.setupOutputFile(output2);
						JNIHandler.pause();
					}
					new Thread(new Runnable() {
						@Override
						public void run() {
							while (((!JNIHandler.isPlaying))) {
								try {
									Thread.sleep(10);
								} catch (InterruptedException e) {
								}
							}
							while (((JNIHandler.isPlaying))) {
								try {
									Thread.sleep(25);
								} catch (InterruptedException e) {
								}
							}
							Intent outgoingIntent = new Intent();
							outgoingIntent.setAction(CommandStrings.ta_rec);
							outgoingIntent.putExtra(CommandStrings.ta_cmd, CommandStrings.ta_cmd_special_notification_finished);
							sendBroadcast(outgoingIntent);
							outgoingIntent.setAction(CommandStrings.ta_rec);
							outgoingIntent.putExtra(CommandStrings.ta_cmd, CommandStrings.ta_cmd_pause_stop);
							outgoingIntent.putExtra(CommandStrings.ta_pause, false);
							sendBroadcast(outgoingIntent);
						}
					}).start();
					break;
				case CommandStrings.msrv_cmd_save_cfg: // store midi settings
					boolean wasPaused = JNIHandler.paused;
					if (!JNIHandler.paused) {
						JNIHandler.pause();
						JNIHandler.waitUntilReady(50);
					}
					String output3 = intent.getStringExtra(CommandStrings.msrv_outfile);
					int[] numbers = new int[3];
					numbers[0] = JNIHandler.tb;
					numbers[1] = JNIHandler.keyOffset;
					numbers[2] = JNIHandler.maxvoice;
					String[] serializedSettings = new String[5]; // I'm sorry. I'm sorry.
					try {
						serializedSettings[0] = ObjectSerializer.serialize(JNIHandler.custInst);
						serializedSettings[1] = ObjectSerializer.serialize(JNIHandler.custVol);
						serializedSettings[2] = ObjectSerializer.serialize(JNIHandler.programs);
						serializedSettings[3] = ObjectSerializer.serialize(JNIHandler.volumes);
						serializedSettings[4] = ObjectSerializer.serialize(numbers);
					} catch (IOException e) {
						e.printStackTrace();
					}

					if (SettingsStorage.compressCfg) {
						BufferedWriter writer = null;
						try {
							GZIPOutputStream zip = new GZIPOutputStream(new FileOutputStream(new File(output3)));

							writer = new BufferedWriter(new OutputStreamWriter(zip, "US-ASCII"));

							for (String s : serializedSettings) {
								writer.append(s);
								writer.newLine();
							}
						} catch (IOException e) {
							e.printStackTrace();
						} finally {
							if (writer != null)
								try {
									writer.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
						}
					} else {
						FileWriter fw = null;
						try {
							fw = new FileWriter(output3, false);
							for (String s : serializedSettings) {
								fw.write(s + "\n");
							}
							fw.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (!wasPaused) {
						JNIHandler.pause();
						JNIHandler.waitUntilReady(50);
					}
					Intent outgoingIntent15 = new Intent();
					outgoingIntent15.setAction(CommandStrings.ta_rec);
					outgoingIntent15.putExtra(CommandStrings.ta_cmd, CommandStrings.ta_cmd_special_notification_finished);
					sendBroadcast(outgoingIntent15);
					break;
				case CommandStrings.msrv_cmd_load_cfg: // load midi settings
					if (JNIHandler.paused) {
						JNIHandler.pause();
						JNIHandler.waitUntilReady(50);
					}
					String input2 = intent.getStringExtra(CommandStrings.msrv_infile);
					ArrayList<Integer> msprograms = new ArrayList<Integer>();
					ArrayList<Boolean> mscustInst = new ArrayList<Boolean>();
					ArrayList<Integer> msvolumes = new ArrayList<Integer>();
					ArrayList<Boolean> mscustVol = new ArrayList<Boolean>();
					int[] newnumbers = new int[3];
					FileInputStream fstream;
					try {
						BufferedReader br;
						if (input2.endsWith(".tzf")) {
							InputStream fileStream = new FileInputStream(input2);
							InputStream gzipStream = new GZIPInputStream(fileStream);
							InputStreamReader decoder = new InputStreamReader(gzipStream, "US-ASCII");
							br = new BufferedReader(decoder);
						} else {

							fstream = new FileInputStream(input2);
							DataInputStream in = new DataInputStream(fstream);
							br = new BufferedReader(new InputStreamReader(in));
						}
						// I could check if all of these are actually ArrayLists, but eclipse still won't be happy
						mscustInst = (ArrayList<Boolean>) ObjectSerializer.deserialize(br.readLine());
						mscustVol = (ArrayList<Boolean>) ObjectSerializer.deserialize(br.readLine());
						msprograms = (ArrayList<Integer>) ObjectSerializer.deserialize(br.readLine());
						msvolumes = (ArrayList<Integer>) ObjectSerializer.deserialize(br.readLine());
						newnumbers = (int[]) ObjectSerializer.deserialize(br.readLine());
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (mscustInst.size() != mscustVol.size() || mscustVol.size() != msprograms.size() || msprograms.size() != msvolumes.size()) {
						// wat
						break;
					}
					for (int i = 0; i < mscustInst.size(); i++) {
						if (mscustInst.get(i)) {
							JNIHandler.setChannelTimidity(i | 0x800, msprograms.get(i));
							JNIHandler.programs.set(i, msprograms.get(i));
						} else {

							JNIHandler.setChannelTimidity(i | 0x8000, msprograms.get(i));
						}
						JNIHandler.custInst.set(i, mscustInst.get(i));
						if (mscustVol.get(i)) {
							JNIHandler.setChannelVolumeTimidity(i | 0x800, msvolumes.get(i));
							JNIHandler.volumes.set(i, msvolumes.get(i));
						} else {
							JNIHandler.setChannelVolumeTimidity(i | 0x8000, msvolumes.get(i));
						}
						JNIHandler.custVol.set(i, mscustVol.get(i));
					}
					int newtb = newnumbers[0] - JNIHandler.tb;
					if (newtb > 0) {
						JNIHandler.controlTimidity(CommandStrings.jni_speedup, newtb);
						JNIHandler.waitUntilReady();
					} else if (newtb < 0) {
						JNIHandler.controlTimidity(CommandStrings.jni_speeddown, -1 * newtb);
						JNIHandler.waitUntilReady();
					}
					JNIHandler.tb = newnumbers[0];

					int newko = newnumbers[1] - JNIHandler.keyOffset;
					if (newko > 0) {
						JNIHandler.controlTimidity(CommandStrings.jni_keyup, newko);
						JNIHandler.waitUntilReady();
					} else if (newko < 0) {
						JNIHandler.controlTimidity(CommandStrings.jni_keydown, newko);
						JNIHandler.waitUntilReady();
					}
					JNIHandler.keyOffset = newnumbers[1];
					int newvoice = newnumbers[2] - JNIHandler.maxvoice;
					if (newvoice != 0) {
						if (newvoice > 0) {
							JNIHandler.controlTimidity(CommandStrings.jni_voiceincr, newvoice);
						} else {
							JNIHandler.controlTimidity(CommandStrings.jni_voicedecr, -1 * newvoice);
						}
						JNIHandler.waitUntilReady();
					}
					if (intent.getBooleanExtra(CommandStrings.msrv_reset, false)) {
						JNIHandler.seekTo(0);
						JNIHandler.shouldPlayNow = true;
						JNIHandler.waitUntilReady();
					}
					break;
				case CommandStrings.msrv_cmd_reload_libs: // Reload native libs
					if (!JNIHandler.isMediaPlayerFormat) {
						fullStop = true;
						Globals.hardStop = true;
						shouldAdvance = false;
						stop();
						JNIHandler.waitForStop();
					}
					int logRet = JNIHandler.unloadLib();
					Log.d("TIMIDITY", "Unloading: " + logRet);
					JNIHandler.prepared = false;
					JNIHandler.volumes = new ArrayList<Integer>();
					JNIHandler.programs = new ArrayList<Integer>();
					JNIHandler.drums = new ArrayList<Boolean>();
					JNIHandler.custInst = new ArrayList<Boolean>();
					JNIHandler.custVol = new ArrayList<Boolean>();
					logRet = JNIHandler.loadLib(Globals.getLibDir(MusicService.this) + "libtimidityplusplus.so");
					Log.d("TIMIDITY", "Reloading: " + logRet);
					int x = JNIHandler.init(SettingsStorage.dataFolder + "timidity/", "timidity.cfg", SettingsStorage.channelMode, SettingsStorage.defaultResamp, SettingsStorage.sixteenBit, SettingsStorage.bufferSize, SettingsStorage.audioRate, SettingsStorage.preserveSilence, true, SettingsStorage.freeInsts);
					if (x != 0 && x != -99) {
						SettingsStorage.onlyNative = SettingsStorage.nativeMidi = true;
						Toast.makeText(MusicService.this, String.format(getResources().getString(R.string.tcfg_error), x), Toast.LENGTH_LONG).show();
						Intent outgoingIntent16 = new Intent();
						outgoingIntent16.setAction(CommandStrings.ta_rec);
						outgoingIntent16.putExtra(CommandStrings.ta_cmd, CommandStrings.ta_cmd_refresh_filebrowser);
						sendBroadcast(outgoingIntent16);
					}
					break;
				}
			}
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		if (serviceReceiver != null) {
			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(CommandStrings.msrv_rec);
			intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
			intentFilter.addAction(Intent.ACTION_MEDIA_BUTTON);
			registerReceiver(serviceReceiver, intentFilter);
		}
		Intent outgoingIntent = new Intent();
		outgoingIntent.setAction(CommandStrings.ta_rec);
		outgoingIntent.putExtra(CommandStrings.ta_cmd, CommandStrings.ta_cmd_service_started);
		sendBroadcast(outgoingIntent);
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Timidity AE");
		wl.setReferenceCounted(false);
		if (shouldDoWidget)
			widgetIds = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), TimidityAEWidgetProvider.class));

		TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		if (mgr != null && Globals.phoneState) {
			mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		}
		// foreground=false;
		if (wl.isHeld())
			wl.release();

		stopForeground(true);

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(serviceReceiver);
	}

	public void toastErrorCode(final int code)
	{
		switch (code) {
		case -1:
			handler.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.srv_fnf), Toast.LENGTH_SHORT).show();
				}
			});

			break;
		case -3:
			handler.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getApplicationContext(), "Error initializing AudioTrack. Try decreasing the buffer size.", Toast.LENGTH_LONG).show();
				}
			});

			break;
		case -9:
			handler.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.srv_loading), Toast.LENGTH_SHORT).show();
				}
			});
			break;
		default:
			handler.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getApplicationContext(), String.format(getResources().getString(R.string.srv_unk), code), Toast.LENGTH_SHORT).show();
				}
			});
			break;
		}
	}
	
	@SuppressLint("NewApi")
	public String handleMetadata(String fileName) {
		MediaMetadataRetriever mmr = new MediaMetadataRetriever();

		String tmpTitle;
		try {
			mmr.setDataSource(fileName);
			tmpTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
		} catch (RuntimeException e) {
			tmpTitle = fileName.substring(fileName.lastIndexOf('/') + 1);
		}
		if (tmpTitle != null) {
			if (TextUtils.isEmpty(tmpTitle))
				tmpTitle = fileName.substring(fileName.lastIndexOf('/') + 1);
		} else {
			tmpTitle = fileName.substring(fileName.lastIndexOf('/') + 1);
		}
		setupMediaArtAndWidget(fileName, mmr);
		return tmpTitle;
	}

	public void play() {
		if (playList != null && currSongNumber >= 0) {
			shouldAdvance = false;
			death = true;
			fullStop = false;
			stop();
			death = false;
			Globals.shouldRestore = true;
			JNIHandler.waitForStop();
			/*
			 * while (!death && ((JNIHandler.isPlaying || JNIHandler.isBlocking == true))) { try { Thread.sleep(10); } catch (InterruptedException e) { e.printStackTrace(); } }
			 */

			if (!death) {
				final int songIndex;
				if (shuffleMode == 1) {
					songIndex = realSongNumber = shuffledIndices.get(currSongNumber);
				} else {
					songIndex = realSongNumber = currSongNumber;
				}

				Intent outgoingIntent = new Intent();
				outgoingIntent.setAction(CommandStrings.ta_rec);
				outgoingIntent.putExtra(CommandStrings.ta_cmd, CommandStrings.ta_cmd_update_art);
				sendBroadcast(outgoingIntent);

				final String fileName = playList.get(songIndex);
				currTitle = handleMetadata(fileName);
				shouldAdvance = true;
				paused = false;

				final int x = JNIHandler.play(fileName);
				if (x != 0) {
					toastErrorCode(x);

					JNIHandler.isPlaying = false;
					JNIHandler.isMediaPlayerFormat = true;
					shouldAdvance = false;
					JNIHandler.paused = false;
					stop();
				} else {
					updateNotification(currTitle, paused);
					new Thread(new Runnable() {
						public void run() {
							// Wait for timidity to actually start playing
							while (!death && ((!JNIHandler.isPlaying && shouldAdvance))) {
								if (!JNIHandler.isBlocking)
									death = true;

								// System.out.println(String.format("alt check: %d death: %s isplaying: %d shouldAdvance: %s seekBarReady: %s",JNIHandler.alternativeCheck,death?"true":"false",Globals.isPlaying,shouldAdvance?"true":"false",JNIHandler.seekbarReady?"true":"false"));
								try {
									Thread.sleep(10);
								} catch (InterruptedException e) {
								}
							}
							if (!death) {
								final Intent guiIntent = new Intent();
								guiIntent.setAction(CommandStrings.ta_rec);
								guiIntent.putExtra(CommandStrings.ta_cmd, CommandStrings.ta_cmd_gui_play);
								guiIntent.putExtra(CommandStrings.ta_startt, JNIHandler.maxTime);
								guiIntent.putExtra(CommandStrings.ta_songttl, currTitle);
								guiIntent.putExtra(CommandStrings.ta_filename, fileName);
								guiIntent.putExtra(CommandStrings.ta_highlight, currSongNumber);
								sendBroadcast(guiIntent);
							}
							if (new File(fileName + ".def.tcf").exists() || new File(fileName + ".def.tzf").exists()) {
								String suffix;
								if (new File(fileName + ".def.tcf").exists() && new File(fileName + ".def.tzf").exists()) {
									suffix = (SettingsStorage.compressCfg ? ".def.tzf" : ".def.tcf");
								} else if (new File(fileName + ".def.tcf").exists()) {
									suffix = ".def.tcf";
								} else {
									suffix = ".def.tzf";
								}
								JNIHandler.shouldPlayNow = false;
								JNIHandler.currTime = 0;
								while (JNIHandler.isPlaying && !death && shouldAdvance && !JNIHandler.dataWritten) {
									try {
										Thread.sleep(25);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								}
								final Intent cfgLoadIntent = new Intent(); // silly, but should be done async. I think.
								cfgLoadIntent.setAction(CommandStrings.msrv_rec);
								cfgLoadIntent.putExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_load_cfg);
								cfgLoadIntent.putExtra(CommandStrings.msrv_infile, fileName + suffix);
								cfgLoadIntent.putExtra(CommandStrings.msrv_reset, true);
								sendBroadcast(cfgLoadIntent);
							}
							JNIHandler.waitForStop();
							/*while (!death && (((JNIHandler.isPlaying || JNIHandler.isBlocking) && shouldAdvance))) {
								try {
									Thread.sleep(25);
								} catch (InterruptedException e) {
								}
							}*/
							if (shouldAdvance && !death) {
								shouldAdvance = false;
								if (playList.size() > 1 && (((songIndex + 1 < playList.size() && loopMode == 0)) || loopMode == 1)) {
									final Intent nextIntent = new Intent();
									nextIntent.setAction(CommandStrings.msrv_rec);
									nextIntent.putExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_next);
									sendBroadcast(nextIntent);
								} else if (loopMode == 2 || playList.size() == 1) {
									final Intent playIntent = new Intent();
									playIntent.setAction(CommandStrings.msrv_rec);
									playIntent.putExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_play);
									sendBroadcast(playIntent);
								} else if (loopMode == 0) {
									Globals.hardStop = true;
									final Intent stopIntent = new Intent();
									stopIntent.setAction(CommandStrings.ta_rec);
									stopIntent.putExtra(CommandStrings.ta_cmd, CommandStrings.ta_cmd_pause_stop);
									stopIntent.putExtra(CommandStrings.ta_pause, false);
									sendBroadcast(stopIntent);
								}
							}
						}
					}).start();
				}
			}
		}
	}

	public void pause() {
		if (playList != null && currSongNumber >= 0) {
			if (JNIHandler.isPlaying) {
				paused = !paused;
				JNIHandler.pause();
				Intent newIntent = new Intent();
				newIntent.setAction(CommandStrings.ta_rec);
				newIntent.putExtra(CommandStrings.ta_cmd, CommandStrings.ta_cmd_pause_stop);
				newIntent.putExtra(CommandStrings.ta_pause, true);
				newIntent.putExtra(CommandStrings.ta_pausea, paused);
				sendBroadcast(newIntent);
				updateNotification(currTitle, paused);

			}
		}
	}

	public void next() {
		death = true;
		if (playList != null && currSongNumber >= 0) {

			if (playList.size() > 1) {
				if (shuffleMode == 2) {
					int tmpNum = currSongNumber;
					while (tmpNum == currSongNumber) {
						tmpNum = random.nextInt(playList.size());
						try {
							Thread.sleep(10); // Don't hog CPU. Please.
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					currSongNumber = tmpNum;
				} else {
					if (++currSongNumber >= playList.size()) {
						currSongNumber = 0;
					}
				}
			}
			play();
		}
	}

	public void previous() {
		death = true;
		if (playList != null && currSongNumber >= 0) {
			currSongNumber -= 1;
			if (currSongNumber < 0) {
				currSongNumber = playList.size() - 1;
			}
			play();
		}
	}

	public void stop() {
		if (JNIHandler.isPlaying) {

			death = true;
			Intent stopIntent = new Intent();
			stopIntent.setAction(CommandStrings.ta_rec);
			stopIntent.putExtra(CommandStrings.ta_cmd, CommandStrings.ta_cmd_pause_stop);
			stopIntent.putExtra(CommandStrings.ta_pause, false);
			sendBroadcast(stopIntent);

			Globals.shouldRestore = false;
			JNIHandler.stop();
			if (fullStop) {
				TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
				if (mgr != null && Globals.phoneState) {
					mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
				}
				Globals.shouldRestore = false;
				if (wl.isHeld())
					wl.release();
				foreground = false;
				fullStop = false;
				// Fix the widget
				if (shouldDoWidget) {
					stopIntent = new Intent(this, TimidityAEWidgetProvider.class);
					stopIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
					// stopIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
					stopIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
					stopIntent.putExtra("com.xperia64.timidityae.timidityaewidgetprovider.paused", true);
					stopIntent.putExtra("com.xperia64.timidityae.timidityaewidgetprovider.title", "");
					stopIntent.putExtra("com.xperia64.timidityae.timidityaewidgetprovider.onlyart", false);
					stopIntent.putExtra("com.xperia64.timidityae.timidityaewidgetprovider.death", true);
					sendBroadcast(stopIntent);
				} else {
					SettingsStorage.nukedWidgets = true;
				}
				stopForeground(true);
				// stopSelf();

			}
		}
	}

	public void updateNotification(String title, boolean paused) {
		// System.out.println("Updating notification");

		remoteViews = new RemoteViews(getPackageName(), R.layout.music_notification);
		remoteViews.setTextViewText(R.id.titley, currTitle);
		remoteViews.setImageViewResource(R.id.notPause, (paused) ? R.drawable.ic_media_play : R.drawable.ic_media_pause);
		// Previous
		final Intent prevIntent = new Intent();
		// newIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		prevIntent.setAction(CommandStrings.msrv_rec);
		prevIntent.putExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_prev);
		PendingIntent pendingNotificationIntent = PendingIntent.getBroadcast(this, 1, prevIntent, 0);
		remoteViews.setOnClickPendingIntent(R.id.notPrev, pendingNotificationIntent);
		// Play/Pause
		final Intent playPauseIntent = new Intent();
		// newIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		playPauseIntent.setAction(CommandStrings.msrv_rec);
		playPauseIntent.putExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_pause);
		pendingNotificationIntent = PendingIntent.getBroadcast(this, 2, playPauseIntent, 0);
		remoteViews.setOnClickPendingIntent(R.id.notPause, pendingNotificationIntent);
		// Next
		final Intent nextIntent = new Intent();
		// newIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		nextIntent.setAction(CommandStrings.msrv_rec);
		nextIntent.putExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_next);
		pendingNotificationIntent = PendingIntent.getBroadcast(this, 3, nextIntent, 0);
		remoteViews.setOnClickPendingIntent(R.id.notNext, pendingNotificationIntent);
		// Stop
		final Intent stopIntent = new Intent();
		// newIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		stopIntent.setAction(CommandStrings.msrv_rec);
		stopIntent.putExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_stop);
		pendingNotificationIntent = PendingIntent.getBroadcast(this, 4, stopIntent, 0);
		remoteViews.setOnClickPendingIntent(R.id.notStop, pendingNotificationIntent);
		final Intent emptyIntent = new Intent(this, TimidityActivity.class);
		// emptyIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 5, emptyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this).setContentTitle(getResources().getString(R.string.app_name)).setContentText(currTitle).setContentIntent(pendingIntent).setContent(remoteViews);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			mBuilder.setSmallIcon(R.drawable.ic_lol);
		} else {
			mBuilder.setSmallIcon(R.drawable.ic_launcher);
		}
		mainNotification = mBuilder.build();
		mainNotification.flags |= Notification.FLAG_ONLY_ALERT_ONCE | Notification.FLAG_ONGOING_EVENT;
		if (!foreground) {
			foreground = true;
			// Dear Google,
			// Your terrible Marshmallow power management had better not break this.
			// If it does, I am using REQUEST_IGNORE_BATTERY_OPTIMIZATIONS.
			// If you ban me for using it, there will be consequences.
			startForeground(Globals.NOTIFICATION_ID, mainNotification);
			if (!wl.isHeld()) {
				wl.acquire();
			}

			TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
			if (mgr != null && Globals.phoneState) {
				mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
			}
		} else {
			if (!wl.isHeld()) {
				wl.acquire();
			}
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.notify(Globals.NOTIFICATION_ID, mainNotification);
		}
		if (shouldDoWidget) {
			Intent intent = new Intent(this, TimidityAEWidgetProvider.class);
			intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			// Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
			// since it seems the onUpdate() is only fired on that:
			// intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
			intent.putExtra("com.xperia64.timidityae.timidityaewidgetprovider.paused", paused);
			intent.putExtra("com.xperia64.timidityae.timidityaewidgetprovider.title", currTitle);
			intent.putExtra("com.xperia64.timidityae.timidityaewidgetprovider.onlyart", true);
			sendBroadcast(intent);
		}
	}

	@SuppressLint("NewApi")
	public void setupMediaArtAndWidget(String fileName, MediaMetadataRetriever mmr) {
		Globals.currArt = null;
		boolean goodart = false;
		if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) // Please work
		{
			try {

				byte[] art = mmr.getEmbeddedPicture();
				if (art != null) {
					Globals.currArt = BitmapFactory.decodeByteArray(art, 0, art.length);
					goodart = Globals.currArt != null;
				}
			} catch (Exception e) {
			}
		}
		if (!goodart) {
			String goodPath = fileName.substring(0, fileName.lastIndexOf('/') + 1) + "folder.jpg";
			if (new File(goodPath).exists()) {
				try {
					Globals.currArt = BitmapFactory.decodeFile(goodPath);
				} catch (RuntimeException e) {
				}
			} else {
				// Try albumart.jpg
				goodPath = fileName.substring(0, fileName.lastIndexOf('/') + 1) + "AlbumArt.jpg";
				if (new File(goodPath).exists()) {
					try {
						Globals.currArt = BitmapFactory.decodeFile(goodPath);
					} catch (RuntimeException e) {
						//
					}
				}
			}
		}
		if (shouldDoWidget) {
			Intent intent = new Intent(this, TimidityAEWidgetProvider.class);
			intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			// Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
			// since it seems the onUpdate() is only fired on that:
			widgetIds = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), TimidityAEWidgetProvider.class));

			// intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
			intent.putExtra("com.xperia64.timidityae.timidityaewidgetprovider.onlyart", true);
			sendBroadcast(intent);
		}
	}
}
