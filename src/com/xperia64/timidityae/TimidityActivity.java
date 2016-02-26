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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
//import android.content.SharedPreferences;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
//import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
//import com.actionbarsherlock.app.SherlockFragment;

import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.xperia64.timidityae.R;
import com.xperia64.timidityae.gui.dialogs.FileBrowserDialog;
import com.xperia64.timidityae.gui.fragments.FileBrowserFragment;
import com.xperia64.timidityae.gui.fragments.PlayerFragment;
import com.xperia64.timidityae.gui.fragments.PlaylistFragment;
import com.xperia64.timidityae.util.ConfigSaver;
import com.xperia64.timidityae.util.DocumentFileUtils;
import com.xperia64.timidityae.util.DownloadTask;
import com.xperia64.timidityae.util.Globals;
import com.xperia64.timidityae.util.CommandStrings;
import com.xperia64.timidityae.util.SettingsStorage;
import com.xperia64.timidityae.util.WavSaver;

// Eclipse Stahp
@SuppressLint("NewApi")
public class TimidityActivity extends AppCompatActivity implements FileBrowserFragment.ActionFileBackListener, PlaylistFragment.ActionPlaylistBackListener, FileBrowserDialog.FileBrowserDialogListener {
	// public static TimidityActivity staticthis;
	private MenuItem menuButton;
	private MenuItem menuButton2;
	//private int viewPagerLocation = 0;
	private FileBrowserFragment fileFrag;
	private PlayerFragment playFrag;
	private PlaylistFragment plistFrag;
	ViewPager viewPager;
	boolean needFileBack = false;
	boolean needPlaylistBack = false;
	boolean fromPlaylist = false;
	boolean needService = true;
	public String currSongName;
	boolean needInit = false;
	boolean fromIntent = false;
	boolean deadlyDeath = false;
	// public boolean localfinished;
	int oldTheme;
	boolean oldPlist;
	// AlertDialog alerty;

	public interface SpecialAction {
		public AlertDialog getAlertDialog();

		public void setLocalFinished(boolean finished);
	}

	SpecialAction special;
	private BroadcastReceiver activityReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			int cmd = intent.getIntExtra(CommandStrings.ta_cmd, CommandStrings.ta_cmd_error); // -V
			switch (cmd) {
			case CommandStrings.ta_cmd_gui_play:
				currSongName = intent.getStringExtra(CommandStrings.ta_filename);
				if (viewPager.getCurrentItem() == 1) {
					menuButton.setIcon(R.drawable.ic_menu_agenda);
					menuButton.setTitle(getResources().getString(R.string.view));
					menuButton.setTitleCondensed(getResources().getString(R.string.viewcon));
					menuButton.setVisible((!JNIHandler.isMediaPlayerFormat) && JNIHandler.isPlaying);
					menuButton.setEnabled((!JNIHandler.isMediaPlayerFormat) && JNIHandler.isPlaying);
					menuButton2.setIcon(R.drawable.ic_menu_info_details);
					menuButton2.setTitle(getResources().getString(R.string.playback));
					menuButton2.setTitleCondensed(getResources().getString(R.string.playbackcon));
					menuButton2.setVisible((!JNIHandler.isMediaPlayerFormat) && JNIHandler.isPlaying);
					menuButton2.setEnabled((!JNIHandler.isMediaPlayerFormat) && JNIHandler.isPlaying);
				}
				playFrag.play(intent.getIntExtra(CommandStrings.ta_startt, 0), intent.getStringExtra(CommandStrings.ta_songttl));
				if (plistFrag != null) {
					Globals.highlightMe = intent.getIntExtra("stupidNumber", -1);
					try {

						int x = plistFrag.getListView().getFirstVisiblePosition();
						plistFrag.getPlaylists(plistFrag.isPlaylist ? plistFrag.plistName : null);
						plistFrag.getListView().setSelection(x);
					} catch (Exception e) {

					}

				}

				break;
			case CommandStrings.ta_cmd_refresh_filebrowser:
				if(fileFrag!=null)
				{
					fileFrag.refresh();
				}
				break;
			case CommandStrings.ta_cmd_load_filebrowser:
				try {
					if(fileFrag!=null)
					{
						fileFrag.getDir(intent.getStringExtra(CommandStrings.ta_currpath));
					}
				} catch (IllegalStateException e) {

				}
				break;
			case CommandStrings.ta_cmd_gui_play_full:
				currSongName = intent.getStringExtra(CommandStrings.ta_filename);
				if (viewPager.getCurrentItem() == 1) {
					menuButton.setIcon(R.drawable.ic_menu_agenda);
					menuButton.setTitle(getResources().getString(R.string.view));
					menuButton.setTitleCondensed(getResources().getString(R.string.viewcon));
					menuButton.setVisible((!JNIHandler.isMediaPlayerFormat) && JNIHandler.isPlaying);
					menuButton.setEnabled((!JNIHandler.isMediaPlayerFormat) && JNIHandler.isPlaying);
					menuButton2.setIcon(R.drawable.ic_menu_info_details);
					menuButton2.setTitle(getResources().getString(R.string.playback));
					menuButton2.setTitleCondensed(getResources().getString(R.string.playbackcon));
					menuButton2.setVisible((!JNIHandler.isMediaPlayerFormat) && JNIHandler.isPlaying);
					menuButton2.setEnabled((!JNIHandler.isMediaPlayerFormat) && JNIHandler.isPlaying);
				}
				playFrag.play(intent.getIntExtra(CommandStrings.ta_startt, 0), intent.getStringExtra(CommandStrings.ta_songttl), intent.getIntExtra(CommandStrings.ta_shufmode, 0), intent.getIntExtra(CommandStrings.ta_loopmode, 1));
				break;
			case CommandStrings.ta_cmd_copy_plist:
				plistFrag.currPlist = Globals.tmpplist;
				Globals.tmpplist = null;
				break;
			case CommandStrings.ta_cmd_pause_stop: // Notifiy pause/stop
				if (!intent.getBooleanExtra(CommandStrings.ta_pause, false) && Globals.hardStop) {
					Globals.hardStop = false;
					if (viewPager.getCurrentItem() == 1) {
						menuButton.setIcon(R.drawable.ic_menu_agenda);
						menuButton.setTitle(getResources().getString(R.string.view));
						menuButton.setTitleCondensed(getResources().getString(R.string.viewcon));
						menuButton.setVisible(false);
						menuButton.setEnabled(false);
						menuButton2.setIcon(R.drawable.ic_menu_info_details);
						menuButton2.setTitle(getResources().getString(R.string.playback));
						menuButton2.setTitleCondensed(getResources().getString(R.string.playbackcon));
						menuButton2.setVisible(false);
						menuButton2.setEnabled(false);
					}
					playFrag.setInterface(0);
					TimidityActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							if (playFrag.midiInfoDialog != null) {
								if (playFrag.midiInfoDialog.isShowing()) {
									playFrag.midiInfoDialog.dismiss();
									playFrag.midiInfoDialog = null;
								}
							}
							if (special != null && special.getAlertDialog() != null) {
								AlertDialog alerty = special.getAlertDialog();
								if (alerty.isShowing()) {
									alerty.dismiss();
									alerty = null;
								}
							}
						}
					});
				}
				playFrag.pauseStop(intent.getBooleanExtra(CommandStrings.ta_pause, false), intent.getBooleanExtra(CommandStrings.ta_pausea, false));
				break;
			case CommandStrings.ta_cmd_update_art: // notify art
				// currSongName =
				// intent.getStringExtra(ServiceStrings.ta_filename));
				if (playFrag != null)
					playFrag.setArt();
				break;
			// case ServiceStrings.ta_cmd_unused_7:
			// fileFrag.localfinished=true;
			// break;
			case CommandStrings.ta_cmd_special_notification_finished:
				if (special != null) {
					special.setLocalFinished(true);
				}
				break;

			}
		}
	};

	/*
	 * @Override protected void onPause() {
	 * 
	 * }
	 */
	@Override
	protected void onResume() {
		deadlyDeath = false;
		super.onResume();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		handleIntentData(intent);
	}

	final int PERMISSION_REQUEST = 177;
	final int NUM_PERMISSIONS = 3;

	public void requestPermissions() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
		/*
		 * ||ContextCompat.checkSelfPermission(this,
		 * Manifest.permission.READ_PHONE_STATE) !=
		 * PackageManager.PERMISSION_GRANTED
		 */) {

			// Should we show an explanation?
			if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {

				new AlertDialog.Builder(this).setTitle("Permissions").setMessage("Timidity AE needs to be able to:\n" + "Read your storage to play music files\n\n" + "Write to your storage to save configuration files\n\n" + "Read phone state to auto-pause music during a phone call\n" + "Timidity will not make phone calls or do anything besides checking if your device is receiving a call")

				.setPositiveButton("OK", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						actuallyRequestPermissions();

					}

				}).setNegativeButton("Cancel", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						new AlertDialog.Builder(TimidityActivity.this).setTitle("Error").setMessage("Timidity AE cannot proceed without these permissions").setPositiveButton("OK", new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								TimidityActivity.this.finish();
							}

						}).setCancelable(false).show();

					}

				}).setCancelable(false).show();

			} else {

				// No explanation needed, we can request the permission.
				actuallyRequestPermissions();
			}
		} else {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
				Globals.phoneState = false;
			}
			yetAnotherInit();
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void actuallyRequestPermissions() {
		ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE }, PERMISSION_REQUEST);
	}

	public void yetAnotherInit() {
		needInit = SettingsStorage.initialize(TimidityActivity.this);
		readyForInit();
		if(fileFrag!=null)
		{
			fileFrag.refresh();
		}
	}

	@TargetApi(23)
	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
		case PERMISSION_REQUEST: {
			// If request is cancelled, the result arrays are empty.
			boolean good = true;
			if (permissions.length != NUM_PERMISSIONS || grantResults.length != NUM_PERMISSIONS) {
				good = false;
			}

			for (int i = 0; i < grantResults.length && good; i++) {
				if (permissions[i].equals(Manifest.permission.READ_PHONE_STATE)) {
					if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
						Globals.phoneState = false;
					}
					continue;
				}
				if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
					good = false;
				}

			}
			if (!good) {

				// permission denied, boo! Disable the app.
				new AlertDialog.Builder(TimidityActivity.this).setTitle("Error").setMessage("Timidity AE cannot proceed without these permissions.").setPositiveButton("OK", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						TimidityActivity.this.finish();
					}

				}).setCancelable(false).show();
			} else {
				if (!Environment.getExternalStorageDirectory().canRead()) {
					// Buggy emulator? Try restarting the app
					AlarmManager alm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
					alm.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, PendingIntent.getActivity(this, 237462, new Intent(this, TimidityActivity.class), Intent.FLAG_ACTIVITY_NEW_TASK));
					System.exit(0);
				}
				yetAnotherInit();
			}
			return;
		}

			// other 'case' lines to check for other
			// permissions this app might request
		}
	}

	@SuppressLint("InlinedApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		deadlyDeath = false;
		if (savedInstanceState == null) {
			SettingsStorage.reloadSettings(this, getAssets());
		} else {
			// For some reason when I kill the activity and restart it,
			// justtheme is true, but Globals.theme = 0
			if (!savedInstanceState.getBoolean("justtheme", false) || SettingsStorage.theme == 0) {
				SettingsStorage.reloadSettings(this, getAssets());
			}
		}
		Bundle extras = getIntent().getExtras();
		if(extras!=null)
		{
			if (!extras.getBoolean("justtheme", false) || SettingsStorage.theme == 0) {
				SettingsStorage.reloadSettings(this, getAssets());
			}
		}
		try {
			System.loadLibrary("timidityhelper");
		} catch (UnsatisfiedLinkError e) {
			Log.e("Bad:", "Cannot load timidityhelper");
			SettingsStorage.nativeMidi = SettingsStorage.onlyNative = true;
		}
		if (JNIHandler.loadLib(Globals.getLibDir(this) + "libtimidityplusplus.so") < 0) {
			Log.e("Bad:", "Cannot load timidityplusplus");
			SettingsStorage.nativeMidi = SettingsStorage.onlyNative = true;
		} else {
			Globals.libLoaded = true;
		}
		oldTheme = SettingsStorage.theme;
		oldPlist = SettingsStorage.enableDragNDrop;
		this.setTheme((SettingsStorage.theme == 1) ? android.support.v7.appcompat.R.style.Theme_AppCompat_Light_DarkActionBar : android.support.v7.appcompat.R.style.Theme_AppCompat);
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			Log.i("Timidity", "Initializing");
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				// Uggh.
				requestPermissions();
			} else {
				yetAnotherInit();
			}

		} else {
			Log.i("Timidity", "Resuming...");
			needService = !isMyServiceRunning(MusicService.class);
			Fragment tmp = getSupportFragmentManager().getFragment(savedInstanceState, "playfrag");
			if (tmp != null)
				playFrag = (PlayerFragment) tmp;

			tmp = getSupportFragmentManager().getFragment(savedInstanceState, "plfrag");
			if (tmp != null)
				plistFrag = (PlaylistFragment) tmp;
			tmp = getSupportFragmentManager().getFragment(savedInstanceState, "fffrag");
			if (tmp != null)
				fileFrag = (FileBrowserFragment) tmp;
			if (!isMyServiceRunning(MusicService.class)) {
				SettingsStorage.reloadSettings(this, getAssets());
				initCallback2();
				if (viewPager != null) {
					if (viewPager.getCurrentItem() == 1) {
						viewPager.setCurrentItem(0);
					}
				}
			}
			/*
			 * if(!savedInstanceState.getBoolean("justtheme", false)) {
			 * Globals.reloadSettings(this, getAssets()); }
			 */

		}
		/*
		 * IntentFilter filter = new IntentFilter();
		 * filter.addAction("com.xperia64.timidityae20.ACTION_STOP");
		 * filter.addAction("com.xperia64.timidityae20.ACTION_PAUSE");
		 * filter.addAction("com.xperia64.timidityae20.ACTION_NEXT");
		 * filter.addAction("com.xperia64.timidityae20.ACTION_PREV");
		 */
		// registerReceiver(receiver, filter);

		setContentView(R.layout.main);

		if (activityReceiver != null) {
			// Create an intent filter to listen to the broadcast sent with the
			// action "ACTION_STRING_ACTIVITY"
			IntentFilter intentFilter = new IntentFilter(CommandStrings.ta_rec);
			// Map the intent filter to the receiver
			registerReceiver(activityReceiver, intentFilter);
		}

		// Start the service on launching the application
		if (needService) {
			needService = false;
			Globals.probablyFresh = 0;
			// System.out.println("Starting service");
			startService(new Intent(this, MusicService.class));
		}

		viewPager = (ViewPager) findViewById(R.id.vp_main);
		viewPager.setAdapter(new TimidityFragmentPagerAdapter());
		viewPager.addOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int index) {
				switch (index) {
				case 0:
					fromPlaylist = false;
					if (getSupportActionBar() != null) {
						if (menuButton != null) {
							menuButton.setIcon(R.drawable.ic_menu_refresh);
							menuButton.setVisible(true);
							menuButton.setEnabled(true);
							menuButton.setTitle(getResources().getString(R.string.refreshfld));
							menuButton.setTitleCondensed(getResources().getString(R.string.refreshcon));
						}
						if (menuButton2 != null) {
							menuButton2.setIcon(R.drawable.ic_menu_home);
							menuButton2.setTitle(getResources().getString(R.string.homefld));
							menuButton2.setTitleCondensed(getResources().getString(R.string.homecon));
							menuButton2.setVisible(true);
							menuButton2.setEnabled(true);
						}
						getSupportActionBar().setDisplayHomeAsUpEnabled(needFileBack);
					} else {
						getSupportActionBar().setDisplayHomeAsUpEnabled(false);
						getSupportActionBar().setHomeButtonEnabled(false);
					}
					if (fileFrag != null)
						if (fileFrag.getListView() != null)
							fileFrag.getListView().setFastScrollEnabled(true);
					break;
				case 1:
					if (getSupportActionBar() != null) {
						if (menuButton != null) {
							menuButton.setIcon(R.drawable.ic_menu_agenda);
							menuButton.setTitle(getResources().getString(R.string.view));
							menuButton.setTitleCondensed(getResources().getString(R.string.viewcon));
							menuButton.setVisible((!JNIHandler.isMediaPlayerFormat) && JNIHandler.isPlaying);
							menuButton.setEnabled((!JNIHandler.isMediaPlayerFormat) && JNIHandler.isPlaying);
						}
						if (menuButton2 != null) {
							menuButton2.setIcon(R.drawable.ic_menu_info_details);
							menuButton2.setTitle(getResources().getString(R.string.playback));
							menuButton2.setTitleCondensed(getResources().getString(R.string.playbackcon));
							menuButton2.setVisible((!JNIHandler.isMediaPlayerFormat) && JNIHandler.isPlaying);
							menuButton2.setEnabled((!JNIHandler.isMediaPlayerFormat) && JNIHandler.isPlaying);
						}
						getSupportActionBar().setDisplayHomeAsUpEnabled(false);
						getSupportActionBar().setHomeButtonEnabled(false);
					}
					break;
				case 2:
					fromPlaylist = true;
					if (getSupportActionBar() != null) {
						if (menuButton != null) {
							menuButton.setIcon(R.drawable.ic_menu_refresh);
							menuButton.setTitle(getResources().getString(R.string.refreshpls));
							menuButton.setTitleCondensed(getResources().getString(R.string.refreshcon));
							menuButton.setVisible(true);
							menuButton.setEnabled(true);
						}
						if (menuButton2 != null) {
							menuButton2.setIcon(R.drawable.ic_menu_add);
							menuButton2.setTitle(getResources().getString(R.string.add));
							menuButton2.setTitleCondensed(getResources().getString(R.string.addcon));
							if (plistFrag != null) {
								menuButton2.setVisible((plistFrag.plistName != null && plistFrag.isPlaylist) ? !plistFrag.plistName.equals("CURRENT") : true);
								menuButton2.setEnabled((plistFrag.plistName != null && plistFrag.isPlaylist) ? !plistFrag.plistName.equals("CURRENT") : true);
							}
						}
						if (plistFrag != null)
							if (plistFrag.getListView() != null)
								plistFrag.getListView().setFastScrollEnabled(true);
						if (needPlaylistBack) {
							getSupportActionBar().setDisplayHomeAsUpEnabled(true);
						}
					} else {
						getSupportActionBar().setDisplayHomeAsUpEnabled(false);
						getSupportActionBar().setHomeButtonEnabled(false);
					}
					break;
				}

			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {

			}

			@Override
			public void onPageScrollStateChanged(int arg0) {

			}
		});
		if(extras!=null && extras.getInt("fragmentpage",-1)>=0)
		{
			viewPager.setCurrentItem(extras.getInt("fragmentpage",-1));
		}
	}

	@SuppressLint("NewApi")
	public void initCallback() {
		if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
			List<UriPermission> permissions = getContentResolver().getPersistedUriPermissions();
			int trueExt = 0;
			for (File f : getExternalFilesDirs(null)) {
				if (f != null)
					trueExt++;
			}
			if ((permissions == null || permissions.isEmpty()) && SettingsStorage.shouldExtStorageNag && trueExt > 1) {
				new AlertDialog.Builder(this).setTitle("SD Card Access").setCancelable(false).setMessage("Would you like to give Timidity AE write access to your external sd card? This is recommended if you're converting files or would like to place Timidity AE's data directory there. Problems may occur if a directory other than the root of your SD card is selected.").setPositiveButton("Yes", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
						startActivityForResult(intent, 42);
					}

				}).setNegativeButton("No, do not ask again", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						SettingsStorage.disableLollipopStorageNag();
						initCallback2();
					}

				}).setNeutralButton("No", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						initCallback2();
					}
				}).show();
			} else {
				for (UriPermission permission : permissions) {
					if (permission.isReadPermission() && permission.isWritePermission()) {
						DocumentFileUtils.docFileDevice = permission.getUri();
					}
				}
				initCallback2();
			}
		} else {
			initCallback2();
		}
	}

	public void initCallback2() {
		int x = JNIHandler.init(SettingsStorage.dataFolder + "timidity/", "timidity.cfg", SettingsStorage.channelMode, SettingsStorage.defaultResamp, SettingsStorage.sixteenBit, SettingsStorage.bufferSize, SettingsStorage.audioRate, SettingsStorage.preserveSilence, false, SettingsStorage.freeInsts);
		if (x != 0 && x != -99) {
			SettingsStorage.nativeMidi = true;
			Toast.makeText(this, String.format(getResources().getString(R.string.tcfg_error), x), Toast.LENGTH_LONG).show();
			if(fileFrag!=null)
			{
				fileFrag.refresh();
			}
		}
		handleIntentData(getIntent());
	}

	public void handleIntentData(Intent in) {
		if (in.getData() != null) {
			String data;
			if ((data = in.getData().getPath()) != null && in.getData().getScheme() != null) {
				if (in.getData().getScheme().equals("file")) {
					if (new File(data).exists()) {
						File f = new File(data.substring(0, data.lastIndexOf('/') + 1));
						if (f!=null && f.exists() && f.isDirectory() && f.listFiles()!=null ) {
								ArrayList<String> files = new ArrayList<String>();
								int position = -1;
								int goodCounter = 0;
								for (File ff : f.listFiles()) {
									if (ff != null && ff.isFile()) {
										if(Globals.hasSupportedExtension(ff))
										{
												files.add(ff.getPath());
												if (ff.getPath().equals(data))
													position = goodCounter;
												goodCounter++;
										}
									}
								}
								if (position == -1)
									Toast.makeText(this, getResources().getString(R.string.intErr1), Toast.LENGTH_SHORT).show();
								else {
									stop();
									selectedSong(files, position, true, false, true);
									fileFrag.getDir(data.substring(0, data.lastIndexOf('/') + 1));
								}
						}
					} else {
						Toast.makeText(this, getResources().getString(R.string.srv_fnf), Toast.LENGTH_SHORT).show();
					}
				} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO && in.getData().getScheme().equals("http") || in.getData().getScheme().equals("https")) {
					if (!data.endsWith("/")) {
						if (!Globals.getExternalCacheDir(this).exists()) {
							Globals.getExternalCacheDir(this).mkdirs();
						}
						final DownloadTask downloadTask = new DownloadTask(this);
						downloadTask.execute(in.getData().toString(), in.getData().getLastPathSegment());
						in.setData(null);
					} else {
						Toast.makeText(this, "This is a directory, not a file", Toast.LENGTH_SHORT).show();
					}
				} else if (in.getData().getScheme().equals("content") && (data.contains("downloads"))) {
					String filename = null;
					Cursor cursor = null;
					try {
						cursor = this.getContentResolver().query(in.getData(), new String[] { OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE }, null, null, null);
						if (cursor != null && cursor.moveToFirst()) {
							filename = cursor.getString(0);
						}
					} finally {
						if (cursor != null)
							cursor.close();
					}
					try {
						InputStream input = getContentResolver().openInputStream(in.getData());
						if (new File(Globals.getExternalCacheDir(this).getAbsolutePath() + '/' + filename).exists()) {
							new File(Globals.getExternalCacheDir(this).getAbsolutePath() + '/' + filename).delete();
						}
						OutputStream output = new FileOutputStream(Globals.getExternalCacheDir(this).getAbsolutePath() + '/' + filename);

						byte[] buffer = new byte[4096];
						int count;
						while ((count = input.read(buffer)) != -1) {
							output.write(buffer, 0, count);
						}
						output.close();
					} catch (IOException e) {
						e.printStackTrace();
						return;
					}

					File f = new File(Globals.getExternalCacheDir(this).getAbsolutePath() + '/');
					if (f.exists()) {
						if (f.isDirectory()) {
							ArrayList<String> files = new ArrayList<String>();
							int position = -1;
							int goodCounter = 0;
							for (File ff : f.listFiles()) {
								if (ff != null && ff.isFile()) {
									if (Globals.hasSupportedExtension(ff)) {
										files.add(ff.getPath());
										if (ff.getPath().equals(Globals.getExternalCacheDir(this).getAbsolutePath() + '/' + filename))
											position = goodCounter;
										goodCounter++;
									}
								}
							}
							if (position == -1)
								Toast.makeText(this, getResources().getString(R.string.intErr1), Toast.LENGTH_SHORT).show();
							else {
								stop();
								selectedSong(files, position, true, false, true);
								fileFrag.getDir(Globals.getExternalCacheDir(this).getAbsolutePath());
							}
						}
					}

				} else {
					Toast.makeText(this, getResources().getString(R.string.intErr2) + " (" + in.getData().getScheme() + ")", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	public void downloadFinished(String data, String theFilename) {
		ArrayList<String> files = new ArrayList<String>();
		String name = Globals.getExternalCacheDir(this).getAbsolutePath() + '/' + theFilename;
		if (Globals.hasSupportedExtension(name)) {

			files.add(name);
			stop();
			selectedSong(files, 0, true, false, true);
			fileFrag.getDir(name.substring(0, name.lastIndexOf('/') + 1));
		}
	}

	@Override
	public void onDestroy() {
		try {
			unregisterReceiver(activityReceiver);
		} catch (IllegalArgumentException e) {

		}
		super.onDestroy();
		// if(deadlyDeath)
		// System.exit(0);
	}

	private boolean isMyServiceRunning(Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void onSaveInstanceState(Bundle icicle) {
		super.onSaveInstanceState(icicle);
		icicle.putBoolean("justtheme", true);
		if (playFrag != null)
			getSupportFragmentManager().putFragment(icicle, "playfrag", playFrag);
		if (plistFrag != null)
			getSupportFragmentManager().putFragment(icicle, "plfrag", plistFrag);
		if (fileFrag != null)
			getSupportFragmentManager().putFragment(icicle, "fffrag", fileFrag);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		menuButton = menu.findItem(R.id.menuBtn1);
		menuButton2 = menu.findItem(R.id.menuBtn2);
		switch (viewPager.getCurrentItem()) {
		case 0:
			fromPlaylist = false;
			if (getSupportActionBar() != null) {
				if (menuButton != null) {
					menuButton.setIcon(R.drawable.ic_menu_refresh);
					menuButton.setVisible(true);
					menuButton.setEnabled(true);
					menuButton.setTitle(getResources().getString(R.string.refreshfld));
					menuButton.setTitleCondensed(getResources().getString(R.string.refreshcon));
				}
				if (menuButton2 != null) {
					menuButton2.setIcon(R.drawable.ic_menu_home);
					menuButton2.setTitle(getResources().getString(R.string.homefld));
					menuButton2.setTitleCondensed(getResources().getString(R.string.homecon));
					menuButton2.setVisible(true);
					menuButton2.setEnabled(true);
				}
				getSupportActionBar().setDisplayHomeAsUpEnabled(needFileBack);
			} else {
				getSupportActionBar().setDisplayHomeAsUpEnabled(false);
				getSupportActionBar().setHomeButtonEnabled(false);
			}
			if (fileFrag != null)
				if (fileFrag.getListView() != null)
					fileFrag.getListView().setFastScrollEnabled(true);
			break;
		case 1:
			if (getSupportActionBar() != null) {
				if (menuButton != null) {
					menuButton.setIcon(R.drawable.ic_menu_agenda);
					menuButton.setTitle(getResources().getString(R.string.view));
					menuButton.setTitleCondensed(getResources().getString(R.string.viewcon));
					menuButton.setVisible((!JNIHandler.isMediaPlayerFormat) && JNIHandler.isPlaying);
					menuButton.setEnabled((!JNIHandler.isMediaPlayerFormat) && JNIHandler.isPlaying);
				}
				if (menuButton2 != null) {
					menuButton2.setIcon(R.drawable.ic_menu_info_details);
					menuButton2.setTitle(getResources().getString(R.string.playback));
					menuButton2.setTitleCondensed(getResources().getString(R.string.playbackcon));
					menuButton2.setVisible((!JNIHandler.isMediaPlayerFormat) && JNIHandler.isPlaying);
					menuButton2.setEnabled((!JNIHandler.isMediaPlayerFormat) && JNIHandler.isPlaying);
				}
				getSupportActionBar().setDisplayHomeAsUpEnabled(false);
				getSupportActionBar().setHomeButtonEnabled(false);
			}
			break;
		case 2:
			fromPlaylist = true;
			if (getSupportActionBar() != null) {
				if (menuButton != null) {
					menuButton.setIcon(R.drawable.ic_menu_refresh);
					menuButton.setTitle(getResources().getString(R.string.refreshpls));
					menuButton.setTitleCondensed(getResources().getString(R.string.refreshcon));
					menuButton.setVisible(true);
					menuButton.setEnabled(true);
				}
				if (menuButton2 != null) {
					menuButton2.setIcon(R.drawable.ic_menu_add);
					menuButton2.setTitle(getResources().getString(R.string.add));
					menuButton2.setTitleCondensed(getResources().getString(R.string.addcon));
					if (plistFrag != null) {
						menuButton2.setVisible((plistFrag.plistName != null && plistFrag.isPlaylist) ? !plistFrag.plistName.equals("CURRENT") : true);
						menuButton2.setEnabled((plistFrag.plistName != null && plistFrag.isPlaylist) ? !plistFrag.plistName.equals("CURRENT") : true);
					}
				}
				if (plistFrag != null)
					if (plistFrag.getListView() != null)
						plistFrag.getListView().setFastScrollEnabled(true);
				if (needPlaylistBack) {
					getSupportActionBar().setDisplayHomeAsUpEnabled(true);
				}
			} else {
				getSupportActionBar().setDisplayHomeAsUpEnabled(false);
				getSupportActionBar().setHomeButtonEnabled(false);
			}
			break;
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menuBtn1) {
			switch (viewPager.getCurrentItem()) {
			case 0:
				if(fileFrag!=null)
				{
					int x = fileFrag.getListView().getFirstVisiblePosition();
					fileFrag.refresh();
					fileFrag.setSelection(x);
				}
				break;
			case 1:
				if (playFrag != null && !JNIHandler.isMediaPlayerFormat) {
					playFrag.incrementInterface();
				}
				break;
			case 2:
				if(plistFrag != null)
				{
					int position = plistFrag.getListView().getFirstVisiblePosition();
					plistFrag.getPlaylists(plistFrag.isPlaylist ? plistFrag.plistName : null);
					plistFrag.getListView().setSelection(position);
				}
				break;
			}
		} else if (item.getItemId() == R.id.menuBtn2) {
			switch (viewPager.getCurrentItem()) {
			case 0:
				if (fileFrag != null)
					fileFrag.getDir(SettingsStorage.homeFolder);
				break;
			case 1:
				if (playFrag != null) {
					if ((!JNIHandler.isMediaPlayerFormat) && JNIHandler.isPlaying) {
						playFrag.showMidiDialog();
					}
				}
				break;
			case 2:
				if(plistFrag!=null)
				{
					plistFrag.add();
				}
				break;
			}
		} else if (item.getItemId() == android.R.id.home) {
			onBackPressed();
		} else if (item.getItemId() == R.id.quit) {
			deadlyDeath = true;
			Intent stopServiceIntent = new Intent();
			stopServiceIntent.setAction(CommandStrings.msrv_rec);
			stopServiceIntent.putExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_stop);
			sendBroadcast(stopServiceIntent);
			stopService(new Intent(this, MusicService.class));
			unregisterReceiver(activityReceiver);
			android.os.Process.killProcess(android.os.Process.myPid()); // Probably
																		// the
																		// same
			// System.exit(0);
		} else if (item.getItemId() == R.id.asettings) {
			Intent mainact = new Intent(this, SettingsActivity.class);
			mainact.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivityForResult(mainact, 1);
		} else if (item.getItemId() == R.id.ahelp) {
			new AlertDialog.Builder(this).setTitle(getResources().getString(R.string.helpt)).setMessage(getResources().getString(R.string.helper)).setNegativeButton("OK", new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {

				}

			}).show();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {

		switch (viewPager.getCurrentItem()) {
		case 0:
			if (fileFrag != null)
				if (fileFrag.currPath != null)
					if (!fileFrag.currPath.matches("[/]+")) {
						fileFrag.getDir(new File(fileFrag.currPath).getParent().toString());
					} else {
						if (SettingsStorage.useDefaultBack) {
							super.onBackPressed();
							return;
						}
						viewPager.setCurrentItem(1);
					}
			break;
		case 1:
			if (SettingsStorage.useDefaultBack) {
				super.onBackPressed();
				return;
			}
			viewPager.setCurrentItem((fromPlaylist) ? 2 : 0);
			break;
		case 2:
			if (plistFrag.isPlaylist)
				plistFrag.getPlaylists(null);
			else {
				if (SettingsStorage.useDefaultBack) {
					super.onBackPressed();
					return;
				}
				viewPager.setCurrentItem(1);
			}
			break;
		}
	}

	public void selectedSong(ArrayList<String> files, int songNumber, boolean begin, boolean fromPlaylist, boolean copyPlist) {
		if(!Globals.hasSupportedExtension(files.get(songNumber)))
		{
			Toast.makeText(this, "Error: Timidity is not loaded. Please make sure the config is valid.", Toast.LENGTH_LONG).show();;
			return;
		}
		this.fromPlaylist = fromPlaylist;
		if (viewPager != null) {
			viewPager.setCurrentItem(1);
		}
		Globals.plist = files;
		if (plistFrag != null && copyPlist) {
			plistFrag.currPlist = files;
		}

		// plistFrag.getListView().setItemChecked(songNumber, true);
		Intent new_intent = new Intent();
		new_intent.setAction(CommandStrings.msrv_rec);
		new_intent.putExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_stop);
		sendBroadcast(new_intent);
		new_intent = new Intent();
		new_intent.setAction(CommandStrings.msrv_rec);
		new_intent.putExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_load_plist_play);

		if (fileFrag != null) {
			new_intent.putExtra(CommandStrings.msrv_currfold, fileFrag.currPath);
		}
		new_intent.putExtra(CommandStrings.msrv_songnum, songNumber);
		new_intent.putExtra(CommandStrings.msrv_begin, begin);
		new_intent.putExtra(CommandStrings.msrv_cpplist, copyPlist);
		sendBroadcast(new_intent);
	}

	

	@Override
	public void needFileBackCallback(boolean yes) {
		needFileBack = yes;
		if (getSupportActionBar() != null) {
			if(viewPager!=null)
			{
				if (viewPager.getCurrentItem() == 0) {
					if (needFileBack) {
						getSupportActionBar().setDisplayHomeAsUpEnabled(true);
					} else {
						getSupportActionBar().setDisplayHomeAsUpEnabled(false);
						getSupportActionBar().setHomeButtonEnabled(false);
					}
				}
			}
		}
	}

	@Override
	public void needPlaylistBackCallback(boolean yes, boolean current) {
		if(menuButton2==null)
			return;
		needPlaylistBack = yes;
		if (getSupportActionBar() != null) {
			if (viewPager.getCurrentItem() == 2) {
				if (needPlaylistBack) {
					getSupportActionBar().setDisplayHomeAsUpEnabled(true);
					menuButton2.setVisible(!current);
					menuButton2.setEnabled(!current);
				} else {
					menuButton2.setVisible(true);
					menuButton2.setEnabled(true);
					getSupportActionBar().setDisplayHomeAsUpEnabled(false);
					getSupportActionBar().setHomeButtonEnabled(false);
				}
			}
		}
	}

	// Broadcast actions
	// This is painful.
	public void play() {
		Intent playIntent = new Intent();
		playIntent.setAction(CommandStrings.msrv_rec);
		playIntent.putExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_play);
		sendBroadcast(playIntent);
	}

	public void pause() {
		Intent pauseIntent = new Intent();
		pauseIntent.setAction(CommandStrings.msrv_rec);
		pauseIntent.putExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_pause);
		sendBroadcast(pauseIntent);
	}

	public void next() {
		Intent nextIntent = new Intent();
		nextIntent.setAction(CommandStrings.msrv_rec);
		nextIntent.putExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_next);
		sendBroadcast(nextIntent);
	}

	public void prev() {
		Intent new_intent = new Intent();
		new_intent.setAction(CommandStrings.msrv_rec);
		new_intent.putExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_prev);
		sendBroadcast(new_intent);
	}

	public void stop() {
		Intent stopIntent = new Intent();
		stopIntent.setAction(CommandStrings.msrv_rec);
		stopIntent.putExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_stop);
		sendBroadcast(stopIntent);
	}

	public void loop(int mode) {
		Intent loopIntent = new Intent();
		loopIntent.setAction(CommandStrings.msrv_rec);
		loopIntent.putExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_loop_mode);
		loopIntent.putExtra(CommandStrings.msrv_loopmode, mode);
		sendBroadcast(loopIntent);
	}

	public void shuffle(int mode) {
		Intent shuffleIntent = new Intent();
		shuffleIntent.setAction(CommandStrings.msrv_rec);
		shuffleIntent.putExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_shuf_mode);
		shuffleIntent.putExtra(CommandStrings.msrv_shufmode, mode);
		sendBroadcast(shuffleIntent);
	}

	public void seek(int time) {
		Intent seekIntent = new Intent();
		seekIntent.setAction(CommandStrings.msrv_rec);
		seekIntent.putExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_seek);
		seekIntent.putExtra(CommandStrings.msrv_seektime, time);
		sendBroadcast(seekIntent);
	}

	public void writeFile(String input, String output) {
		Intent writeWavIntent = new Intent();
		writeWavIntent.setAction(CommandStrings.msrv_rec);
		writeWavIntent.putExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_write_new);
		writeWavIntent.putExtra(CommandStrings.msrv_infile, input);
		writeWavIntent.putExtra(CommandStrings.msrv_outfile, output);
		sendBroadcast(writeWavIntent);
	}

	@SuppressLint("NewApi")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Check which request we're responding to
		if (requestCode == 1) {
			if (oldTheme != SettingsStorage.theme || oldPlist != SettingsStorage.enableDragNDrop) {
				Intent intent = getIntent();
				intent.putExtra("justtheme", true);
				intent.putExtra("needservice", false);
				intent.putExtra("fragmentpage", viewPager.getCurrentItem());
				finish();
				startActivity(intent);
			}

		} else if (requestCode == 42) {
			if (resultCode == RESULT_OK) {
				Uri treeUri = data.getData();
				getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
				DocumentFileUtils.docFileDevice = treeUri;
			}
			initCallback2();
		}
	}

	public void readyForInit() {
		if (needInit)
			initCallback();
	}

	public void loadCfg() {
		new FileBrowserDialog().create(0, Globals.configFiles, this, this, getLayoutInflater(), true, currSongName.substring(0, currSongName.lastIndexOf('/')), "Loaded");
	}

	public void loadCfg(String path) {
		Intent new_intent = new Intent();
		new_intent.setAction(CommandStrings.msrv_rec);
		new_intent.putExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_load_cfg);
		new_intent.putExtra(CommandStrings.msrv_infile, path);
		sendBroadcast(new_intent);
	}

	@Override
	public void setItem(String path, int type) {
		loadCfg(path);
	}

	@Override
	public void write() {

	}

	@Override
	public void ignore() {

	}

	public void setLocalFinished(boolean yes) {
		if (special != null) {
			special.setLocalFinished(yes);
		}
	}

	public void dynExport(boolean whilePlaying) {
		WavSaver ws = new WavSaver(this, currSongName, whilePlaying);
		special = ws;
		ws.dynExport();
	}

	public void dynExport(String filename, boolean whilePlaying) {
		WavSaver ws = new WavSaver(this, filename, whilePlaying);
		special = ws;
		ws.dynExport();
	}

	public void saveCfg() {
		ConfigSaver cs = new ConfigSaver(this, currSongName);
		special = cs;
		cs.promptSaveCfg();
	}

	public void saveCfgPart2(String s1, String s2) {
		ConfigSaver cs = new ConfigSaver(this, currSongName);
		special = cs;
		cs.writeConfig(s1, s2);
	}
	
	public class TimidityFragmentPagerAdapter extends FragmentPagerAdapter {
		final String[] pages = { "Files", "Player", "Playlists" };

		public TimidityFragmentPagerAdapter() {
			super(getSupportFragmentManager());
		}

		@Override
		public int getCount() {
			return pages.length;
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case 0:
				fileFrag = FileBrowserFragment.create(SettingsStorage.homeFolder);
				return fileFrag;
			case 1:
				playFrag = PlayerFragment.create();
				return playFrag;
			case 2:
				plistFrag = PlaylistFragment.create(SettingsStorage.dataFolder + "playlists/");
				return plistFrag;
			default:
				return null;
			}
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return pages[position];
		}
	}
}
