/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Build;

import com.xperia64.timidityae.util.Constants;
import com.xperia64.timidityae.util.Globals;
import com.xperia64.timidityae.util.SettingsStorage;
import com.xperia64.timidityae.util.WavWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import static com.xperia64.timidityae.JNIHandler.PlaybackState.*;

// The native methods exist. Really.
@SuppressWarnings("JniMissingFunction")
public class JNIHandler {



	// config folder, config file, mono, resampling algorithm, preserve
	// silence
	public static native int loadLib(String libPath);
	public static native int unloadLib();
	private static native int prepareTimidity(String config, String config2, int jmono, int jcustResamp, int jPresSil, int jreloading, int jfreeInsts, int jverbosity, int jvolume);
	private static native int loadSongTimidity(String filename);

	// See globals for commands
	public static native void controlTimidity(int jcmd, int jcmdArg);

	// -------------------------
	public static native void setChannelTimidity(int jchan, int jprog);

	public static native void setChannelVolumeTimidity(int jchan, int jvol);

	private static native boolean timidityReady();

	public static native int setResampleTimidity(int jcustResamp);

	public static native int decompressSFArk(String from, String to);

	public static native int soxInit(int jreloading, int jrate);

	public static native int soxPlay(String jfileName, String[][] jeffects, int jignoreSafety);

	public static native void soxSeek(int jtime);

	public static native void soxStop();

	// public static DataOutputStream outFile;
	public static AudioTrack mAudioTrack;
	public static MediaPlayer mMediaPlayer;
	public static int maxTime = 0;
	public static int currTime = 0;

	public enum MediaFormat {
		FMT_MEDIAPLAYER, FMT_TIMIDITY, FMT_SOX
	};

	public static MediaFormat mediaBackendFormat = MediaFormat.FMT_MEDIAPLAYER;

	private static int channelMode; // 0 = mono (downmixed), 1 = mono (synthesized), 2 = stereo
	public static int rate;
	public static int buffer;

	// Timidity Stuff
	public static int MAX_CHANNELS = 32;
	public static int currsamp = 0;
	public static ArrayList<Integer> programs = new ArrayList<>();
	public static ArrayList<Boolean> custInst = new ArrayList<>();
	public static ArrayList<Integer> volumes = new ArrayList<>();
	public static ArrayList<Boolean> custVol = new ArrayList<>();
	public static ArrayList<Boolean> drums = new ArrayList<>();
	public static String currentLyric = "";
	private static int overwriteLyricAt = 0;
	public static int exceptional = 0;
	public static int playbackPercentage;
	public static int playbackTempo; // This number is not the tempo in BPM, but some number that can be used to calculate the real tempo
	public static int tempoCount = 0; // How many times the tempo up/down buttons have been pressed
	public static int voice;
	public static int maxvoice = 256;
	public static int keyOffset = 0;

	static boolean dataWritten = false;

	static boolean shouldPlayNow = true;

	public static String errorReason;

	public enum PlaybackState {
		STATE_UNINIT, STATE_IDLE, STATE_LOADING, STATE_PLAYING, STATE_PAUSING,
		STATE_PAUSED, STATE_RESUMING, STATE_SEEKING, STATE_REQSTOP, STATE_STOPPING,
		STATE_ERROR
	};

	public static PlaybackState state = STATE_UNINIT;

	public static boolean isActive()
	{
		// Playing, Pausing, Paused, Resuming, or Seeking.
		return state == STATE_PLAYING || state == STATE_PAUSING || state == STATE_PAUSED || state == STATE_RESUMING || state == STATE_SEEKING;
	}


	static WavWriter currentWavWriter = null;

	public static void pause() // or unpause.
	{
		if (state == STATE_PLAYING || state == STATE_PAUSED) {
			if (state == PlaybackState.STATE_PAUSED) {
				switch(mediaBackendFormat)
				{
					case FMT_MEDIAPLAYER:
						mMediaPlayer.start();
						state = STATE_PLAYING;
						break;
					case FMT_SOX:
						try {
							mAudioTrack.play();
						} catch (Exception e) {
							e.printStackTrace();
						}
						state = STATE_PLAYING;
						break;
					case FMT_TIMIDITY:
						controlTimidity(Constants.jni_tim_toggle_pause, 0);
						if (!(currentWavWriter != null && !currentWavWriter.finishedWriting) && mAudioTrack != null) {
							try {
								mAudioTrack.play();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						state = STATE_PLAYING; // FIXME: Should be STATE_RESUMINGMIDITY:
						break;
				}


			} else {
				state = STATE_PAUSED; // FIXME: Should be STATE_PAUSING
				switch(mediaBackendFormat) {
					case FMT_MEDIAPLAYER:
						mMediaPlayer.pause();
						break;
					case FMT_SOX:
						try {
							mAudioTrack.pause();
						} catch (Exception e) {
							e.printStackTrace();
						}
						break;
					case FMT_TIMIDITY:
						controlTimidity(Constants.jni_tim_toggle_pause, 0);
						if (!(currentWavWriter != null && !currentWavWriter.finishedWriting) && mAudioTrack != null) {
							try {
								mAudioTrack.pause();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						break;
				}
			}
		}
	}

	/*
		Template:
		switch(mediaBackendFormat)
		{
			case FMT_MEDIAPLAYER:

				break;
			case FMT_SOX:

				break;
			case FMT_TIMIDITY:

				break;
		}
	 */

	public static void stop() {
		state = STATE_REQSTOP;
		switch(mediaBackendFormat)
		{
			case FMT_MEDIAPLAYER:
				mMediaPlayer.setOnCompletionListener(null);
				try {
					mMediaPlayer.stop();
				} catch (IllegalStateException ignored) {}
				state = STATE_IDLE;
				break;
			case FMT_SOX:
				soxStop();
				break;
			case FMT_TIMIDITY:
				controlTimidity(Constants.jni_tim_stop, 0);
				break;
		}
	}

	public static void seekTo(int time) {
		PlaybackState oldstate = state;
		switch(mediaBackendFormat)
		{
			case FMT_MEDIAPLAYER:
				mMediaPlayer.seekTo(time);
				break;
			case FMT_SOX:
				state = STATE_SEEKING;
				soxSeek(time);
				state = oldstate;
				break;
			case FMT_TIMIDITY:
				state = STATE_SEEKING;
				controlTimidity(Constants.jni_tim_jump, time);
				waitUntilReady();
				state = oldstate;
				break;
		}
	}

	public static void waitUntilReady() {
		waitUntilReady(10);
	}

	public static void waitUntilReady(int interval) {
		if(mediaBackendFormat != MediaFormat.FMT_TIMIDITY)
		{
			return;
		}
		while (!timidityReady()) {
			try {
				Thread.sleep(interval);
			} catch (InterruptedException ignored) {}
		}
	}

	public static void waitForStop() {
		waitForStop(10);
	}

	public static void waitForStop(int interval) {
		if (mediaBackendFormat == MediaFormat.FMT_MEDIAPLAYER) {
			while (state != STATE_IDLE) {
				try {
					Thread.sleep(interval);
				} catch (InterruptedException ignored) {}
			}
		} else {
			try {
				playThread.join();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	public static void waitForStable() { waitForStable(10); }

	private static void waitForStable(int interval) {
		while(state != STATE_IDLE && state != STATE_PAUSED && state != STATE_PLAYING)
		{
			try {
				Thread.sleep(interval);
			} catch (InterruptedException ignored) {}
		}
	}

	public static void setupOutputFile(String filename) {
		currentWavWriter = new WavWriter();
		currentWavWriter.setupOutputFile(filename, (channelMode < 2), rate);
	}

	// Used by native (TiMidity++)
	@SuppressWarnings("unused")
	public static void buffit(byte[] data, int length) {
		dataWritten = true;
		if (shouldPlayNow) {

			if (channelMode == 0) {
				byte[] mono = new byte[length / 2];
				for (int i = 0; i < mono.length / 2; ++i) {
					int HI = 1;
					int LO = 0;
					int left = (data[i * 4 + HI] << 8) | (data[i * 4 + LO] & 0xff);
					int right = (data[i * 4 + 2 + HI] << 8) | (data[i * 4 + 2 + LO] & 0xff);
					int avg = (left + right) / 2;
					mono[i * 2 + HI] = (byte) ((avg >> 8) & 0xff);
					mono[i * 2 + LO] = (byte) (avg & 0xff);
				}
				if (currentWavWriter != null && !currentWavWriter.finishedWriting) {
					try {
						currentWavWriter.write(data, 0, length);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					try {
						mAudioTrack.write(mono, 0, mono.length);
					} catch (IllegalStateException ignored) {}
				}
			} else {
				if (currentWavWriter != null && !currentWavWriter.finishedWriting) {
					try {
						currentWavWriter.write(data, 0, length);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					try {
						mAudioTrack.write(data, 0, length);
					} catch (IllegalStateException ignored) {}
				}
			}
		}

	}

	// Used by native (SoX)
	@SuppressWarnings("unused")
	private static void buffsox(short[] data, int length)
	{
		try {
			mAudioTrack.write(data, 0, length);
		} catch (IllegalStateException ignored) {}

		// Hack for pauses
		while(state == STATE_PAUSED) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private static AudioAttributes mAudioAttributes;
	public static int init(String path, String file, int mono, int resamp, int b, int r, boolean preserveSilence, boolean reloading, boolean freeInsts, int verbosity, int v) {
		if (state == STATE_UNINIT) {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				mAudioAttributes = new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build();
			}
			System.out.println(String.format(Locale.US, "Opening Timidity: Path: %s cfgFile: %s resample: %s mono: %s buffer: %d rate: %d", path, file, Globals.sampls[resamp], ((mono == 1) ? "true" : "false"), b, r));
			System.out.println("Max channels: " + MAX_CHANNELS);
			for (int i = 0; i < MAX_CHANNELS; i++) {
				volumes.add(75); // Assuming not XG
				programs.add(0);
				drums.add(i == 9);
				custInst.add(false);
				custVol.add(false);
			}
			channelMode = mono;
			rate = r;
			buffer = b;
			if (mMediaPlayer == null)
				mMediaPlayer = new MediaPlayer();

			int code = prepareTimidity(path, path + file, (channelMode == 1) ? 1 : 0, resamp, preserveSilence ? 1 : 0, reloading ? 1 : 0, freeInsts ? 1 : 0, verbosity, v)
					+ soxInit(reloading ? 1 : 0, rate);
			state = STATE_IDLE; // TODO: Maybe keep as UNINIT if code != 0?
			return code;
		} else {
			// Log.w("Warning", "Attempt to prepare again cancelled.");
			return -99;
		}
	}

	private static void resetVars()
	{
		errorReason = "";

		if(mediaBackendFormat == MediaFormat.FMT_TIMIDITY) {
			keyOffset = 0;
			tempoCount = 0;
			currentLyric = "";
			overwriteLyricAt = 0;
			dataWritten = false;
			shouldPlayNow = true;
			for (int i = 0; i < MAX_CHANNELS; i++) {
				volumes.set(i, 75); // Assuming not XG
				programs.set(i, 0);
				drums.set(i, i == 9);
				custInst.set(i, false);
				custVol.set(i, false);
			}
		}
	}

	private static Thread playThread;

	public static int play(final String songTitle) {
		if (new File(songTitle).exists()) {
			if (state == STATE_IDLE) {

				state = STATE_LOADING;
				mediaBackendFormat = Globals.determineFormat(songTitle);

				resetVars();

				switch(mediaBackendFormat)
				{
					case FMT_MEDIAPLAYER:
						try {
							mMediaPlayer.setOnCompletionListener(null);
							mMediaPlayer.reset();
							if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
								mMediaPlayer.setAudioAttributes(mAudioAttributes);
							} else {
								mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
							}
							mMediaPlayer.setVolume(100, 100);
							mMediaPlayer.setDataSource(songTitle);
							mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
								public void onPrepared(MediaPlayer arg0) {
									initSeekBar(arg0.getDuration());
								}
							});
							mMediaPlayer.prepare();
							mMediaPlayer.start();

							state = STATE_PLAYING;

							mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
								@Override
								public void onCompletion(MediaPlayer arg0) {
									arg0.setOnCompletionListener(null);
									state = STATE_IDLE;
								}
							});

						} catch (Exception ignored) {} // TODO: Don't really care. Should I?
						break;
					case FMT_SOX:
						playThread = new Thread(new Runnable() {
							public void run() {
								if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
									mAudioTrack = new AudioTrack(mAudioAttributes, new AudioFormat.Builder().setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).build(),
											buffer, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
								} else {
									mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, rate,
											AudioFormat.CHANNEL_OUT_STEREO,
											AudioFormat.ENCODING_PCM_16BIT,
											buffer, AudioTrack.MODE_STREAM);
								}

								mAudioTrack.play();
								state = STATE_PLAYING;
								String[][] soxEffects = {};
								if(!SettingsStorage.soxEffStr.isEmpty())
								{
									String[] firstLayer = SettingsStorage.soxEffStr.trim().replaceAll("[;]+",";").replaceAll("^;+","").replaceAll(";+$","").split(";");
									soxEffects = new String[firstLayer.length][];
									for(int i = 0; i<firstLayer.length; i++)
									{
										soxEffects[i] = firstLayer[i].trim().split(" ");
									}
								}
								int soxRes = soxPlay(songTitle, soxEffects, SettingsStorage.unsafeSoxSwitch?1:0);
								if(soxRes<1)
								{
									// We have an error.
									errorReason = String.format(Locale.US, "Bad sox effect %1$s (%2$d)", soxEffects[-soxRes][0], -soxRes);
									state = STATE_ERROR;
									mAudioTrack.release();
								}
							}
						});
						playThread.setPriority(Thread.MAX_PRIORITY);
						playThread.start();
						break;
					case FMT_TIMIDITY:
						// Reset the audio track every time.
						// The audiotrack should be in the same thread as the
						// timidity stuff for black midi.
						playThread = new Thread(new Runnable() {
							public void run() {
								if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
									mAudioTrack = new AudioTrack(mAudioAttributes, new AudioFormat.Builder().setChannelMask((channelMode == 2)?AudioFormat.CHANNEL_OUT_STEREO:AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).build(),
											buffer, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
								} else {
									mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, rate,
											(channelMode == 2) ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO,
											AudioFormat.ENCODING_PCM_16BIT,
											buffer, AudioTrack.MODE_STREAM);
								}

								if (!(currentWavWriter != null && !currentWavWriter.finishedWriting)) {
									try {
										mAudioTrack.play();
									} catch (Exception e) {
										exceptional |= 1;
									}
								}
								state = STATE_PLAYING; // FIXME: should use the Timidity callback; Or maybe see if data is written
								loadSongTimidity(songTitle);
								if(state != STATE_IDLE)
									state = STATE_STOPPING;

								if (currentWavWriter != null && !currentWavWriter.finishedWriting)
									currentWavWriter.finishOutput();
							}
						});
						playThread.setPriority(Thread.MAX_PRIORITY);
						playThread.start();
						break;
				}
				return 0;
			} else {
				return -9;
			}
		}
		return -1;
	}

	// Used by native (TiMidity++)
	@SuppressWarnings("unused")
	public static void controlCallback(int y) {
		final String[] control = { "PM_REQ_MIDI", //0

				"PM_REQ_INST_NAME", //1

				"PM_REQ_DISCARD", //2

				"PM_REQ_FLUSH", //3

				"PM_REQ_GETQSIZ", //4

				"PM_REQ_SETQSIZ", //5

				"PM_REQ_GETFRAGSIZ", //6
				"PM_REQ_RATE", //7

				"PM_REQ_GETSAMPLES", //8

				"PM_REQ_PLAY_START", //9

				"PM_REQ_PLAY_END", //10

				"PM_REQ_GETFILLABLE", //11

				"PM_REQ_GETFILLED", //12

				"PM_REQ_OUTPUT_FINISH", //13

				"PM_REQ_DIVISIONS" /*14*/ };

		System.out.println("TiMidity++ Command: "+y+ " "+ control[y]);

		if (y == 10) {
			if (mAudioTrack != null) {
				try {
					mAudioTrack.stop();
				} catch (IllegalStateException ignored) {}
				mAudioTrack.release();
			}
			state = STATE_IDLE;
		}
	}

	// Used by native (TiMidity++)
	@SuppressWarnings("unused")
	public static int bufferSize() {
		// If a wav file is currently being written,
		// tell timidity it is doing a great job at playing in realtime
		// to avoid cutting off notes and such.
		if (currentWavWriter != null && !currentWavWriter.finishedWriting) {
			return 0;
		}
		try {
			// Samples * Number of Channels * sample size
			return (mAudioTrack.getPlaybackHeadPosition() *
					mAudioTrack.getChannelCount() * 2); // 16 bit is 2
		} catch (IllegalStateException e) {
			return 0;
		}
	}

	// Used by native (TiMidity++)
	@SuppressWarnings("unused")
	public static void finishCallback() {
		// TODO: Which is more correct: The finish callback or TiMidity++ code 10?
		state = STATE_IDLE;
	}

	// Used by native (TiMidity++)
	@SuppressWarnings("unused")
	public static void flushTrack() {
		mAudioTrack.flush();
	}

	// Used by native (TiMidity++)
	@SuppressWarnings({"unused", "WeakerAccess"})
	public static void initSeekBar(int seeker) {
		maxTime = seeker;
	}

	// Used by native (TiMidity++)
	@SuppressWarnings("unused")
	public static void updateSeekBar(int seekIt, int voices) {
		currTime = seekIt;
		voice = voices;
	}

	public static int getRate() {
		return mAudioTrack.getSampleRate();
	}

	// Used by native (TiMidity++)
	@SuppressWarnings("unused")
	public static void updateLyrics(byte[] b) {
		if(b.length<3)
		{
			return;
		}
		final StringBuilder stb = new StringBuilder(currentLyric);
		final StringBuilder tmpBuild = new StringBuilder();
		boolean isNormalLyric = (b[0] == 'L');
		boolean isNewline = (b[0] == 'N');
		boolean isComment = (b[0] == 'Q');

		for (int i = 2; i < b.length; i++) {
			if (b[i] == 0)
				break;
			tmpBuild.append((char) b[i]);
		}
		if (isComment) // comments always get newlines
		{
			stb.append(tmpBuild);
			stb.append('\n');
			overwriteLyricAt = stb.length();
		} else if (isNewline || isNormalLyric) {
			if (isNewline) {
				stb.append('\n');
				overwriteLyricAt = stb.length();
			}
			stb.replace(overwriteLyricAt, stb.length(), tmpBuild.toString());
		} else { // A marker or something
			stb.append(tmpBuild);
			stb.append("\n");
			overwriteLyricAt = stb.length();
		}
		currentLyric = stb.toString();
	}

	public static void updateCmsg(byte[] b)
	{
		final StringBuilder stb = new StringBuilder(currentLyric);
		for(byte bb : b)
		{
			stb.append((char)bb);
		}
		stb.append("\n");
		currentLyric = stb.toString();
	}

	// Used by native (TiMidity++)
	@SuppressWarnings("unused")
	public static void updateMaxChannels(int val) {
		MAX_CHANNELS = val;
	}

	// Used by native (TiMidity++)
	@SuppressWarnings("unused")
	public static void updateProgramInfo(int ch, int prog) {
		if (ch < MAX_CHANNELS)
			programs.set(ch, prog);
	}

	// Used by native (TiMidity++)
	@SuppressWarnings("unused")
	public static void updateVolInfo(int ch, int vol) {
		if (ch < MAX_CHANNELS)
			volumes.set(ch, vol);
	}

	// Used by native (TiMidity++)
	@SuppressWarnings("unused")
	public static void updateDrumInfo(int ch, int isDrum) {
		if (ch < MAX_CHANNELS)
			drums.set(ch, (isDrum != 0));
	}

	// Used by native (TiMidity++)
	@SuppressWarnings("unused")
	public static void updateTempo(int t, int tr) {
		playbackTempo = t;
		playbackPercentage = tr;
		//System.out.println("T: "+t+" tr "+tr);
		// TODO something
		// int x = (int) (500000 / (double) t * 120 * (double) tr / 100 + 0.5);
		// System.out.println("T: "+t+ " TR: "+tr+" X: "+x);
	}

	// Used by native (TiMidity++)
	@SuppressWarnings("unused")
	public static void updateMaxVoice(int vvv) {
		maxvoice = vvv;
	}

	// Used by native (TiMidity++)
	@SuppressWarnings("unused")
	public static void updateKey(int k) {
		keyOffset = k;
	}

	// Used by native (SoX)
	@SuppressWarnings("unused")
	public static void soxOverDone()
	{
		if (mAudioTrack != null) {
			try {
				// Write an extra two seconds of silence to ensure we've flushed fully
				if(state != STATE_REQSTOP) {
					state = STATE_STOPPING;
					buffsox(new short[(rate) * 2], ((rate) * 2));
					mAudioTrack.flush();
				}
				mAudioTrack.stop();
			} catch (IllegalStateException ignored) {}
			mAudioTrack.release();
		}
		state = STATE_IDLE;
	}
}
