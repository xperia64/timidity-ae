/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.gui.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.xperia64.timidityae.JNIHandler;
import com.xperia64.timidityae.R;
import com.xperia64.timidityae.TimidityActivity;
import com.xperia64.timidityae.gui.dialogs.SoxEffectsDialog;
import com.xperia64.timidityae.util.Constants;
import com.xperia64.timidityae.util.DocumentFileUtils;
import com.xperia64.timidityae.util.Globals;
import com.xperia64.timidityae.util.SettingsStorage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@SuppressLint("Recycle")
public class PlayerFragment extends Fragment {

	//private boolean shouldAdvance = true;
	private int loopMode = 1;
	private int shuffleMode = 0;
	private boolean firstSelection;
	private boolean changingTime = false;
	//
	private TimidityActivity mActivity;
	//
	//private Random random = new Random(System.currentTimeMillis());
	// UI Elements
	private ImageButton previousButton; // |<
	private ImageButton rewindButton; // <<
	private ImageButton playButton; // >
	private ImageButton fastForwardButton; // >>
	private ImageButton nextButton; // >|
	private ImageButton shuffleButton;
	private ImageButton loopButton;
	private ImageButton stopButton;
	private SeekBar trackBar;
	private TextView songTitle;
	private TextView timeCounter;
	private ArtFragment artsy;
	private TrackFragment tracky;
	private LyricFragment lyrical;
	private int totalMinutes;
	private int totalSeconds;
	private int currMinutes;
	private int currSeconds;
	private int fragMode = 0; // 0 = AlbumArt, 1 = midi controls, 2 = Kareoke
	private boolean ffrw = false;
	private boolean enabledControls = false;
	public boolean canEnablePlay = true;
	private boolean shouldUpdateSeekBar = false;
	//
	public AlertDialog midiInfoDialog;
	private TextView tempo;
	private TextView pitch;
	private TextView voices;

	//
	public static PlayerFragment create() {
		return new PlayerFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mActivity = (TimidityActivity) getActivity();
	}

	Handler seekHandler = new Handler();
	Runnable seekbarUpdater = new Runnable() {

		@Override
		public void run() {
			seekUpdation();
		}
	};
	Runnable lyricUpdater = new Runnable() {
		@Override
		public void run() {
			lyricUpdation();
		}
	};

	public void lyricUpdation() {
		if (JNIHandler.isActive()) {
			if ((JNIHandler.mediaBackendFormat == JNIHandler.MediaFormat.FMT_TIMIDITY) && fragMode == 2) {
				lyrical.updateLyrics();
			}
			seekHandler.postDelayed(lyricUpdater, 10);
		}

	}

	public void seekUpdation() {
		if (JNIHandler.isActive() && isAdded()) {
			if ((JNIHandler.mediaBackendFormat != JNIHandler.MediaFormat.FMT_MEDIAPLAYER)) {
				if (JNIHandler.mAudioTrack != null) {
					if ((JNIHandler.exceptional & 1) != 0) {
						Toast.makeText(getActivity(), "Error initializing AudioTrack. Try decreasing the buffer size.", Toast.LENGTH_LONG).show();
						canEnablePlay = true;
						mActivity.stop();
					}
					try {
						if (!enabledControls && (JNIHandler.mAudioTrack.getPlaybackHeadPosition() >= 500)) {
							enabledControls = true;
							canEnablePlay = true;
							fastForwardButton.setEnabled(true);
							rewindButton.setEnabled(true);
							trackBar.setEnabled(true);
							playButton.setEnabled(true);
						}
					} catch (Exception ignored) {}
				}
			} else {
				enabledControls = true;
				canEnablePlay = true;
				fastForwardButton.setEnabled(true);
				rewindButton.setEnabled(true);
				trackBar.setEnabled(true);
				playButton.setEnabled(true);
			}
			if (fragMode == 1) {
				tracky.updateList();
			}
			if (midiInfoDialog != null && midiInfoDialog.isShowing()) {
				if (tempo != null)
					tempo.setText(String.format(getResources().getString(R.string.mop_tempo), JNIHandler.playbackPercentage, (int) (500000 / (double) JNIHandler.playbackTempo * 120 * (double) JNIHandler.playbackPercentage / 100 + 0.5)));
				if (pitch != null)
					pitch.setText(String.format(getResources().getString(R.string.mop_pitch), ((JNIHandler.keyOffset > 0) ? "+" : "") + Integer.toString(JNIHandler.keyOffset)));
				if (voices != null)
					voices.setText(String.format(getResources().getString(R.string.mop_voice), JNIHandler.voice, JNIHandler.maxvoice));
			}
			try { // NOPE
				if (JNIHandler.mMediaPlayer != null && (JNIHandler.mediaBackendFormat == JNIHandler.MediaFormat.FMT_MEDIAPLAYER) && JNIHandler.mMediaPlayer.isPlaying()) // Are these evaluated in order? I hope so
				{
					JNIHandler.currTime = JNIHandler.mMediaPlayer.getCurrentPosition();
				}
			} catch (Exception ignored) {}
			if (getActivity() != null && !changingTime) {
				shouldUpdateSeekBar = false;
				totalMinutes = 0;
				totalSeconds = JNIHandler.maxTime;
				currMinutes = 0;
				currSeconds = JNIHandler.currTime;
				if ((JNIHandler.mediaBackendFormat == JNIHandler.MediaFormat.FMT_MEDIAPLAYER)) {
					totalSeconds /= 1000; // ms to s
					currSeconds /= 1000;
				}
				totalMinutes = totalSeconds / 60;
				totalSeconds %= 60;
				currMinutes = currSeconds / 60;
				currSeconds %= 60;
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						trackBar.setMax(JNIHandler.maxTime);
						trackBar.setProgress(JNIHandler.currTime);
						trackBar.invalidate();

						timeCounter.setText(String.format(Locale.US, "%1$d:%2$02d/%3$d:%4$02d", currMinutes, currSeconds, totalMinutes, totalSeconds));
						timeCounter.invalidate();
					}
				});
				seekHandler.postDelayed(seekbarUpdater, 500);
			} else {
				shouldUpdateSeekBar = true;
			}
		}
	}

	@Override
	public void onAttach(Context activity) {
		super.onAttach(activity);
		if (Globals.shouldRestore) {
			Intent new_intent = new Intent();
			new_intent.setAction(Constants.msrv_rec);
			new_intent.putExtra(Constants.msrv_cmd, Constants.msrv_cmd_get_info);
			getActivity().sendBroadcast(new_intent);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.player, container, false);
		previousButton = (ImageButton) v.findViewById(R.id.previousButton);
		rewindButton = (ImageButton) v.findViewById(R.id.rewindButton);
		playButton = (ImageButton) v.findViewById(R.id.playButton);
		fastForwardButton = (ImageButton) v.findViewById(R.id.fastForwardButton);
		nextButton = (ImageButton) v.findViewById(R.id.nextButton);
		shuffleButton = (ImageButton) v.findViewById(R.id.shuffleButton);
		loopButton = (ImageButton) v.findViewById(R.id.loopButton);
		stopButton = (ImageButton) v.findViewById(R.id.stopButton);
		trackBar = (SeekBar) v.findViewById(R.id.seekBar);
		songTitle = (TextView) v.findViewById(R.id.songTitle);
		timeCounter = (TextView) v.findViewById(R.id.timeCount);
		return v;
	}

	public void pauseStop(boolean cmd, final boolean arg) {
		if (cmd) // pause
		{
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					playButton.setImageResource((arg) ? R.drawable.ic_media_play : R.drawable.ic_media_pause);
				}
			});

		} else { // stop
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					trackBar.setProgress(0);
					trackBar.setMax(0);
					trackBar.setEnabled(false);
					rewindButton.setEnabled(false);
					fastForwardButton.setEnabled(false);
					if (canEnablePlay) {
						playButton.setEnabled(true);
					}
					playButton.setImageResource(R.drawable.ic_media_play);
				}
			});
		}
	}

	@SuppressLint("Recycle")
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();

		artsy = new ArtFragment();
		ft.replace(R.id.midiContainer, artsy);
		ft.commit();
		previousButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				canEnablePlay = false;
				playButton.setEnabled(false);
				mActivity.prev();
			}
		});
		rewindButton.setEnabled(false);
		rewindButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				ffrw = true;
				int to = trackBar.getProgress() - ((JNIHandler.mediaBackendFormat == JNIHandler.MediaFormat.FMT_MEDIAPLAYER) ? 3000 : 3);
				to = (to > trackBar.getMax() ? trackBar.getMax() : to < 0 ? 0 : to);
				trackBar.setProgress(to);
			}

		});
		rewindButton.setOnLongClickListener(new OnLongClickListener() {

			private Handler mHandler = new Handler();
			int count;
			int mult;

			@Override
			public boolean onLongClick(View arg0) {
				count = 0;
				mult = 1;
				ffrw = true;
				mHandler.post(mAction);
				return true;
			}

			Runnable mAction = new Runnable() {
				@Override
				public void run() {
					changingTime = true;
					int to = trackBar.getProgress() - (3 * mult * ((JNIHandler.mediaBackendFormat == JNIHandler.MediaFormat.FMT_MEDIAPLAYER) ? 1000 : 1));
					to = (to > trackBar.getMax() ? trackBar.getMax() : to < 0 ? 0 : to);
					trackBar.setProgress(to);
					if (rewindButton.isPressed()) {
						if (count++ > 5) {
							count = 0;
							mult++;
						}
						mHandler.postDelayed(this, 500);
					} else {
						changingTime = false;
						if (JNIHandler.mediaBackendFormat != JNIHandler.MediaFormat.FMT_MEDIAPLAYER)
							mActivity.seek(trackBar.getProgress());
						seekUpdation();
					}
				}
			};

		});
		playButton.setEnabled(false); // >
		playButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (JNIHandler.isActive()) {
					mActivity.pause();
				} else {
					arg0.setEnabled(false);
					mActivity.play();
				}
			}
		});
		fastForwardButton.setEnabled(false);
		fastForwardButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				ffrw = true;
				int to = trackBar.getProgress() + ((JNIHandler.mediaBackendFormat == JNIHandler.MediaFormat.FMT_MEDIAPLAYER) ? 3000 : 3);
				to = (to > trackBar.getMax() ? trackBar.getMax() : to < 0 ? 0 : to);
				trackBar.setProgress(to);
			}

		});
		fastForwardButton.setOnLongClickListener(new OnLongClickListener() {

			private Handler mHandler = new Handler();
			int count;
			int mult;

			@Override
			public boolean onLongClick(View arg0) {
				count = 0;
				mult = 1;
				ffrw = true;
				mHandler.post(mAction);
				return true;
			}

			Runnable mAction = new Runnable() {
				@Override
				public void run() {
					changingTime = true;
					int to = trackBar.getProgress() + (3 * mult * ((JNIHandler.mediaBackendFormat == JNIHandler.MediaFormat.FMT_MEDIAPLAYER) ? 1000 : 1));
					to = (to > trackBar.getMax() ? trackBar.getMax() : to < 0 ? 0 : to);
					trackBar.setProgress(to);
					if (fastForwardButton.isPressed()) {
						if (count++ > 5) {
							count = 0;
							mult++;
						}
						mHandler.postDelayed(this, 500);
					} else {
						changingTime = false;
						if (JNIHandler.mediaBackendFormat != JNIHandler.MediaFormat.FMT_MEDIAPLAYER)
							mActivity.seek(trackBar.getProgress());
						seekUpdation();
					}
				}
			};

		});
		nextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				canEnablePlay = false;
				playButton.setEnabled(false);
				mActivity.next();
			}
		});
		shuffleButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (++shuffleMode > 2)
					shuffleMode = 0;
				int shufRes = R.drawable.ic_menu_forward;
				switch (shuffleMode) {
					case 0:
						shufRes = R.drawable.ic_menu_forward;
						break;
					case 1:
						shufRes = R.drawable.ic_menu_shuffle;
						break;
					case 2:
						shufRes = R.drawable.ic_menu_revert;
						break;
				}
				((ImageButton) arg0).setImageResource(shufRes);
				mActivity.shuffle(shuffleMode);
			}
		});
		loopButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View arg0) {
				if (++loopMode > 2) {
					loopMode = 0;
				}
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						switch (loopMode) {
							case 0:
								((ImageButton) arg0).setImageResource(R.drawable.ic_menu_forward);
								break;
							case 1:
								((ImageButton) arg0).setImageResource(R.drawable.ic_menu_refresh);
								break;
							case 2:
								((ImageButton) arg0).setImageResource(R.drawable.ic_menu_rotate);
								break;
						}
					}
				});
				mActivity.loop(loopMode);
			}
		});
		stopButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				canEnablePlay = true;
				mActivity.stop();
			}
		});
		trackBar.setEnabled(false);
		trackBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				if (arg2 || ffrw) {
					if (arg0.isEnabled() && ((JNIHandler.mediaBackendFormat == JNIHandler.MediaFormat.FMT_MEDIAPLAYER) || ffrw)) {
						mActivity.seek(arg1);
					}
					if (!fastForwardButton.isPressed() && !rewindButton.isPressed())
						ffrw = false;
					totalMinutes = 0;
					totalSeconds = arg0.getMax();
					currMinutes = 0;
					currSeconds = arg1;
					if (JNIHandler.mediaBackendFormat == JNIHandler.MediaFormat.FMT_MEDIAPLAYER) {
						totalSeconds /= 1000; // ms to s
						currSeconds /= 1000;
					}
					totalMinutes = totalSeconds / 60;
					totalSeconds %= 60;
					currMinutes = currSeconds / 60;
					currSeconds %= 60;
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {

							timeCounter.setText(String.format(Locale.US, "%1$d:%2$02d/%3$d:%4$02d", currMinutes, currSeconds, totalMinutes, totalSeconds));
							timeCounter.invalidate();
						}
					});
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				changingTime = true;
			}

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {

				changingTime = false;
				if (JNIHandler.mediaBackendFormat != JNIHandler.MediaFormat.FMT_MEDIAPLAYER)
					mActivity.seek(arg0.getProgress());
				if (shouldUpdateSeekBar)
					seekUpdation();
			}

		});

		trackBar.setIndeterminate(false);
	}

	public void play(final int seekBarTime, final String title, final int shuffleMode, final int loopMode) {
		enabledControls = false;
		canEnablePlay = false;
		playButton.setEnabled(false);
		if (tracky != null)
			tracky.reset();

		if (midiInfoDialog != null) {
			if (midiInfoDialog.isShowing()) {
				midiInfoDialog.dismiss();
				midiInfoDialog = null;
			}
		}
		this.shuffleMode = shuffleMode;
		this.loopMode = loopMode;
		if (getActivity() != null) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					playButton.setImageResource(R.drawable.ic_media_pause);
					trackBar.setMax(seekBarTime);
					trackBar.setProgress(0);
					songTitle.setText(title);
					seekUpdation();
					if ((JNIHandler.mediaBackendFormat == JNIHandler.MediaFormat.FMT_TIMIDITY))
						lyricUpdation();
					switch (loopMode) {
						case 0:
							loopButton.setImageResource(R.drawable.ic_menu_forward);
							break;
						case 1:
							loopButton.setImageResource(R.drawable.ic_menu_refresh);
							break;
						case 2:
							loopButton.setImageResource(R.drawable.ic_menu_rotate);
							break;
					}
					int shufRes = R.drawable.ic_menu_forward;
					switch (shuffleMode) {
						case 0:
							shufRes = R.drawable.ic_menu_forward;
							break;
						case 1:
							shufRes = R.drawable.ic_menu_shuffle;
							break;
						case 2:
							shufRes = R.drawable.ic_menu_revert;
							break;
					}
					shuffleButton.setImageResource(shufRes);
				}
			});
			if ((JNIHandler.mediaBackendFormat != JNIHandler.MediaFormat.FMT_TIMIDITY) && fragMode != 0) {
				FragmentManager fm = getFragmentManager();
				FragmentTransaction ft = fm.beginTransaction();
				fragMode = 0;
				artsy = new ArtFragment();
				ft.replace(R.id.midiContainer, artsy);
				ft.commitAllowingStateLoss();
			}
		}
	}

	public void play(final int seekBarTime, final String title) {
		enabledControls = false;
		canEnablePlay = false;
		playButton.setEnabled(false);
		if (tracky != null)
			tracky.reset();

		if (midiInfoDialog != null) {
			if (midiInfoDialog.isShowing()) {
				midiInfoDialog.dismiss();
				midiInfoDialog = null;
			}
		}
		if (getActivity() != null) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					playButton.setImageResource(R.drawable.ic_media_pause);
					trackBar.setProgress(0);
					trackBar.setMax(seekBarTime);
					songTitle.setText(title);
					seekUpdation();
					if (JNIHandler.mediaBackendFormat == JNIHandler.MediaFormat.FMT_TIMIDITY)
						lyricUpdation();
				}
			});
		}
		if ((JNIHandler.mediaBackendFormat != JNIHandler.MediaFormat.FMT_TIMIDITY) && fragMode != 0) {
			FragmentManager fm = getFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			fragMode = 0;
			artsy = new ArtFragment();
			ft.replace(R.id.midiContainer, artsy);
			ft.commitAllowingStateLoss();
		}
	}

	public void incrementInterface() {
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();

		if ((JNIHandler.mediaBackendFormat == JNIHandler.MediaFormat.FMT_TIMIDITY) && JNIHandler.isActive()) {
			if (++fragMode > 2)
				fragMode = 0;
		} else {
			fragMode = 0;
		}
		switch (fragMode) {
			case 0:
				artsy = new ArtFragment();
				ft.replace(R.id.midiContainer, artsy);
				ft.commitAllowingStateLoss();
				break;
			case 1:
				tracky = new TrackFragment();
				ft.replace(R.id.midiContainer, tracky);
				ft.commitAllowingStateLoss();
				break;
			case 2:
				lyrical = new LyricFragment();
				ft.replace(R.id.midiContainer, lyrical);
				ft.commitAllowingStateLoss();
				break;
		}
	}

	public void setInterface(int which) {
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		fragMode = which;
		if ((JNIHandler.mediaBackendFormat == JNIHandler.MediaFormat.FMT_TIMIDITY) && JNIHandler.isActive()) {
			if (fragMode > 2)
				fragMode = 0;
		} else {
			fragMode = 0;
		}
		switch (fragMode) {
			case 0:
				artsy = new ArtFragment();
				ft.replace(R.id.midiContainer, artsy);
				ft.commitAllowingStateLoss();
				break;
			case 1:
				tracky = new TrackFragment();
				ft.replace(R.id.midiContainer, tracky);
				ft.commitAllowingStateLoss();
				break;
			case 2:
				lyrical = new LyricFragment();
				ft.replace(R.id.midiContainer, lyrical);
				ft.commitAllowingStateLoss();
				break;
		}
	}

	/*
	 * public void updateMidiDialog(boolean which[], int t, int tr, int voices) {
	 * 
	 * }
	 */
	@SuppressLint("InflateParams")
	public void showMidiDialog() {
		AlertDialog.Builder midiInfoDialogBuilder = new AlertDialog.Builder(getActivity());
		View midiDialogView = getActivity().getLayoutInflater().inflate(R.layout.midi_options, null);
		Button speedUp = (Button) midiDialogView.findViewById(R.id.speedUp);
		Button slowDown = (Button) midiDialogView.findViewById(R.id.slowDown);
		Button keyUp = (Button) midiDialogView.findViewById(R.id.keyUp);
		Button keyDown = (Button) midiDialogView.findViewById(R.id.keyDown);
		Button vplus = (Button) midiDialogView.findViewById(R.id.vplus);
		Button vminus = (Button) midiDialogView.findViewById(R.id.vminus);
		Button export = (Button) midiDialogView.findViewById(R.id.exportButton);
		Button saveCfg = (Button) midiDialogView.findViewById(R.id.saveCfg);
		Button loadCfg = (Button) midiDialogView.findViewById(R.id.loadCfg);
		Button savedefCfg = (Button) midiDialogView.findViewById(R.id.savedefCfg);
		final Button deldefCfg = (Button) midiDialogView.findViewById(R.id.deldefCfg);
		deldefCfg.setEnabled(new File(mActivity.currSongName + ".def.tcf").exists() || new File(mActivity.currSongName + ".def.tzf").exists());
		tempo = (TextView) midiDialogView.findViewById(R.id.tempoText);
		pitch = (TextView) midiDialogView.findViewById(R.id.pitchText);
		voices = (TextView) midiDialogView.findViewById(R.id.voiceText);

		tempo.setText(String.format(getResources().getString(R.string.mop_tempo), JNIHandler.playbackPercentage, (int) (500000 / (double) JNIHandler.playbackTempo * 120 * (double) JNIHandler.playbackPercentage / 100 + 0.5)));
		pitch.setText(String.format(getResources().getString(R.string.mop_pitch), ((JNIHandler.keyOffset > 0) ? "+" : "") + Integer.toString(JNIHandler.keyOffset)));
		voices.setText(String.format(getResources().getString(R.string.mop_voice), JNIHandler.voice, JNIHandler.maxvoice));
		speedUp.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				JNIHandler.controlTimidity(Constants.jni_tim_speedup, 1);
				JNIHandler.waitUntilReady();
				JNIHandler.tempoCount++;
			}

		});
		slowDown.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				JNIHandler.controlTimidity(Constants.jni_tim_speeddown, 1);
				JNIHandler.waitUntilReady();
				JNIHandler.tempoCount--;
			}

		});
		keyUp.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				JNIHandler.controlTimidity(Constants.jni_tim_keyup, 1);
				JNIHandler.waitUntilReady();
			}

		});
		keyDown.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				JNIHandler.controlTimidity(Constants.jni_tim_keydown, -1);
				JNIHandler.waitUntilReady();
			}

		});
		vplus.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				JNIHandler.controlTimidity(Constants.jni_tim_voiceincr, 5);
				JNIHandler.waitUntilReady();
			}

		});
		vminus.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				JNIHandler.controlTimidity(Constants.jni_tim_voicedecr, 5);
				JNIHandler.waitUntilReady();
			}

		});
		export.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				mActivity.dynExport(true);
			}

		});
		saveCfg.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				mActivity.saveCfg();
			}

		});
		loadCfg.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				mActivity.loadCfg();
			}

		});
		savedefCfg.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				String value1;
				String value2;
				boolean alreadyExists = (new File(mActivity.currSongName + ".def.tcf").exists() || new File(mActivity.currSongName + ".def.tzf").exists());
				boolean aWrite = true;
				String needRename1 = null;
				String needRename2 = null;
				String probablyTheRoot = "";
				String probablyTheDirectory;
				try {
					if (SettingsStorage.compressCfg)
						new FileOutputStream(mActivity.currSongName + ".def.tzf", true).close();
					else
						new FileOutputStream(mActivity.currSongName + ".def.tcf", true).close();
				} catch (FileNotFoundException e) {
					aWrite = false;
				} catch (IOException e) {
					e.printStackTrace();
				}
				final boolean canWrite = aWrite;
				if (!alreadyExists && canWrite) {
					new File(mActivity.currSongName + ".def.tcf").delete();
					new File(mActivity.currSongName + ".def.tzf").delete();
				}

				if (canWrite && new File(mActivity.currSongName).canWrite()) {
					value1 = mActivity.currSongName + (SettingsStorage.compressCfg ? ".def.tzf" : ".def.tcf");
					value2 = mActivity.currSongName + (SettingsStorage.compressCfg ? ".def.tcf" : ".def.tzf");
				} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && DocumentFileUtils.docFileDevice != null) {
					// TODO
					// Write the file to getExternalFilesDir, then move it with the Uri
					// We need to tell JNIHandler that movement is needed.
					// This is pretty much done now?
					String[] tmp = DocumentFileUtils.getExternalFilePaths(getActivity(), mActivity.currSongName);
					probablyTheDirectory = tmp[0];
					probablyTheRoot = tmp[1];
					if (probablyTheDirectory.length() > 1) {
						needRename1 = (mActivity.currSongName).substring(mActivity.currSongName.indexOf(probablyTheRoot) + probablyTheRoot.length()) + (SettingsStorage.compressCfg ? ".def.tzf" : ".def.tcf");
						needRename2 = (mActivity.currSongName).substring(mActivity.currSongName.indexOf(probablyTheRoot) + probablyTheRoot.length()) + (SettingsStorage.compressCfg ? ".def.tcf" : ".def.tzf");
						value1 = probablyTheDirectory + mActivity.currSongName.substring(mActivity.currSongName.lastIndexOf('/')) + (SettingsStorage.compressCfg ? ".def.tzf" : ".def.tcf");
						value2 = probablyTheDirectory + mActivity.currSongName.substring(mActivity.currSongName.lastIndexOf('/')) + (SettingsStorage.compressCfg ? ".def.tcf" : ".def.tzf");
					} else {
						Toast.makeText(getActivity(), "Could not write config file. Did you give Timidity write access to the root of your external sd card?", Toast.LENGTH_SHORT).show();
						return;
					}
				} else {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
						Toast.makeText(getActivity(), "Could not write config file. Did you give Timidity write access to the root of your external sd card?", Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(getActivity(), "Could not write config file. Permission denied.", Toast.LENGTH_SHORT).show();
					}
					return;
				}
				final String finalval1 = value1;
				final String finalval2 = value2;
				final String needToRename1 = needRename1;
				final String needToRename2 = needRename2;
				final String probRoot = probablyTheRoot;
				if (alreadyExists) {
					AlertDialog dialog = new AlertDialog.Builder(mActivity).create();
					dialog.setTitle("Warning");
					dialog.setMessage("Overwrite default config file?");
					dialog.setCancelable(false);
					dialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int buttonId) {
							if (!canWrite && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
								DocumentFileUtils.tryToDeleteFile(getActivity(), probRoot + needToRename1);
								DocumentFileUtils.tryToDeleteFile(getActivity(), finalval1);
								DocumentFileUtils.tryToDeleteFile(getActivity(), probRoot + needToRename2);
								DocumentFileUtils.tryToDeleteFile(getActivity(), finalval2);
							} else {
								new File(mActivity.currSongName + ".def.tcf").delete();
								new File(mActivity.currSongName + ".def.tzf").delete();
							}
							mActivity.setLocalFinished(false);
							mActivity.saveCfgPart2(finalval1, needToRename1);
							deldefCfg.setEnabled(true);
						}
					});
					dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(android.R.string.no), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int buttonId) {

						}
					});
					dialog.show();

				} else {
					mActivity.setLocalFinished(false);
					mActivity.saveCfgPart2(finalval1, needToRename1);
					deldefCfg.setEnabled(true);
				}
			}

		});

		deldefCfg.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				if (new File(mActivity.currSongName + ".def.tcf").exists() || new File(mActivity.currSongName + ".def.tzf").exists()) {
					boolean aWrite = true;
					try {
						if (SettingsStorage.compressCfg)
							new FileOutputStream(mActivity.currSongName + ".def.tzf", true).close();
						else
							new FileOutputStream(mActivity.currSongName + ".def.tcf", true).close();
					} catch (FileNotFoundException e) {
						aWrite = false;
					} catch (IOException e) {
						e.printStackTrace();
					}
					final boolean canWrite = aWrite;
					AlertDialog dialog = new AlertDialog.Builder(mActivity).create();
					dialog.setTitle("Warning");
					dialog.setMessage("Really delete default config file?");
					dialog.setCancelable(false);
					dialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int buttonId) {

							if (!canWrite && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
								DocumentFileUtils.tryToDeleteFile(getActivity(), mActivity.currSongName + ".def.tzf");
								DocumentFileUtils.tryToDeleteFile(getActivity(), mActivity.currSongName + ".def.tcf");
							} else {
								new File(mActivity.currSongName + ".def.tcf").delete();
								new File(mActivity.currSongName + ".def.tzf").delete();
							}
							deldefCfg.setEnabled(false);
						}
					});
					dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(android.R.string.no), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int buttonId) {

						}
					});
					dialog.show();

				}
			}

		});

		final Spinner x = (Spinner) midiDialogView.findViewById(R.id.resampSpinner);
		List<String> arrayAdapter = new ArrayList<>();
		Collections.addAll(arrayAdapter, Globals.sampls);
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, arrayAdapter);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		x.setAdapter(dataAdapter);
		firstSelection = true;
		x.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long id) {
				if (firstSelection)
					firstSelection = false;
				else {
					JNIHandler.setResampleTimidity(JNIHandler.currsamp = pos);
					JNIHandler.seekTo(JNIHandler.currTime);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}

		});
		x.setSelection(JNIHandler.currsamp);
		if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			midiDialogView.setBackgroundColor(SettingsStorage.theme == 1 ? Color.WHITE : Color.BLACK);

		midiInfoDialogBuilder.setView(midiDialogView);
		midiInfoDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

			}
		});
		midiInfoDialogBuilder.setTitle(getActivity().getResources().getString(R.string.mop));
		midiInfoDialog = midiInfoDialogBuilder.create();
		midiInfoDialog.show();

	}

	public void setArt() {
		if (fragMode == 0 && artsy != null && getActivity() != null) {
			artsy.setArt(Globals.currArt, getActivity());
		}
	}

	public void showSoxDialog() {
		/*AlertDialog.Builder soxInfoDialogBuilder = new AlertDialog.Builder(getActivity());
		View soxDialogView = getActivity().getLayoutInflater().inflate(R.layout.sox_options_basic, null);
		final EditText soxEff = (EditText) soxDialogView.findViewById(R.id.soxDlgText);
		soxEff.setText(SettingsStorage.soxEffStr);
		soxInfoDialogBuilder.setView(soxDialogView);
		soxInfoDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				SettingsStorage.soxEffStr = soxEff.getText().toString();
			}
		});
		soxInfoDialogBuilder.setTitle("SoX Effects");
		midiInfoDialog = soxInfoDialogBuilder.create();
		midiInfoDialog.show();*/
		new SoxEffectsDialog().create(getActivity(), getActivity().getLayoutInflater());
	}
}
