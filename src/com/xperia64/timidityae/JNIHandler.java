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

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;

import com.xperia64.timidityae.util.Constants;
import com.xperia64.timidityae.util.Globals;
import com.xperia64.timidityae.util.WavWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

//import android.util.Log;


// The native methods exist. Really.
@SuppressWarnings("JniMissingFunction")
public class JNIHandler {

	// config folder, config file, mono, resampling algorithm, bits, preserve
	// silence
	public static native int loadLib(String libPath);

	public static native int unloadLib();

	private static native int prepareTimidity(String config, String config2, int jmono, int jcustResamp, int jsixteen, int jPresSil, int jreloading, int jfreeInsts);

	private static native int loadSongTimidity(String filename);

	// See globals for commands
	public static native void controlTimidity(int jcmd, int jcmdArg);

	// -------------------------
	public static native void setChannelTimidity(int jchan, int jprog);

	public static native void setChannelVolumeTimidity(int jchan, int jvol);

	private static native boolean timidityReady();

	public static native int setResampleTimidity(int jcustResamp);

	public static native int decompressSFArk(String from, String to);

	// public static DataOutputStream outFile;
	public static AudioTrack mAudioTrack;
	public static MediaPlayer mMediaPlayer;
	public static int maxTime = 0;
	public static int currTime = 0;
	public static boolean paused = false;
	public static boolean isMediaPlayerFormat = true; // true = mediaplayer, false = audiotrack
	private static int channelMode; // 0 = mono (downmixed), 1 = mono (synthesized), 2 = stereo
	private static boolean sixteenBit;
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
	public static int tb = 0;
	public static int voice;
	public static int maxvoice = 256;
	public static int keyOffset = 0;
	// public static boolean breakLoops = false;
	static boolean dataWritten = false;

	static boolean shouldPlayNow = true;


	private static boolean finishedCallbackCheck = true; // Is set
	public static boolean isPlaying = false;
	static boolean isBlocking = false; // false = not currently blocking, true = blocking. Makes sure timidity actually returned.

	// public static ArrayList<String> lyricLines;
	// End Timidity Stuff

	static boolean prepared = false;

	static WavWriter currentWavWriter = null;

	public static void pause() // or unpause.
	{
		if (isPlaying) {
			if (paused) {
				paused = false;
				if (isMediaPlayerFormat) {
					mMediaPlayer.start();
				} else {
					controlTimidity(Constants.jni_tim_toggle_pause, 0);
					if (!(currentWavWriter != null && !currentWavWriter.finishedWriting) && mAudioTrack != null) {
						try {
							mAudioTrack.play();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			} else {
				paused = true;
				if (isMediaPlayerFormat) {
					mMediaPlayer.pause();
				} else {
					controlTimidity(Constants.jni_tim_toggle_pause, 0);
					if (!(currentWavWriter != null && !currentWavWriter.finishedWriting) && mAudioTrack != null) {
						try {
							mAudioTrack.pause();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	public static void stop() {
		if (isMediaPlayerFormat) {
			mMediaPlayer.setOnCompletionListener(null);
			try {
				mMediaPlayer.stop();
			} catch (IllegalStateException ignored) {}
			isPlaying = false;
			finishedCallbackCheck = true;
			isBlocking = false;
		} else {
			controlTimidity(Constants.jni_tim_stop, 0);
		}
	}

	public static void seekTo(int time) {
		if (isMediaPlayerFormat) {
			mMediaPlayer.seekTo(time);
		} else {
			controlTimidity(Constants.jni_tim_jump, time);
			waitUntilReady();
		}
	}

	public static void waitUntilReady() {
		waitUntilReady(10);
	}

	public static void waitUntilReady(int interval) {
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
		if (isMediaPlayerFormat) {
			while (isPlaying || isBlocking || !finishedCallbackCheck) {
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
		/**/
	}

	public static void setupOutputFile(String filename) {
		currentWavWriter = new WavWriter();
		currentWavWriter.setupOutputFile(filename, sixteenBit, (channelMode < 2), rate);
	}

	// Used by native (TiMidity++)
	@SuppressWarnings("unused")
	public static void buffit(byte[] data, int length) {
		dataWritten = true;
		if (shouldPlayNow) {

			if (channelMode == 0) {
				byte[] mono = new byte[length / 2];
				if (sixteenBit) {
					for (int i = 0; i < mono.length / 2; ++i) {
						int HI = 1;
						int LO = 0;
						int left = (data[i * 4 + HI] << 8) | (data[i * 4 + LO] & 0xff);
						int right = (data[i * 4 + 2 + HI] << 8) | (data[i * 4 + 2 + LO] & 0xff);
						int avg = (left + right) / 2;
						mono[i * 2 + HI] = (byte) ((avg >> 8) & 0xff);
						mono[i * 2 + LO] = (byte) (avg & 0xff);
					}
				} else {
					for (int i = 0; i < mono.length; ++i) {
						int left = (data[i * 2]) & 0xff;
						int right = (data[i * 2 + 1]) & 0xff;
						int avg = (left + right) / 2;
						mono[i] = (byte) (avg);
					}
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

	public static int init(String path, String file, int mono, int resamp, boolean sixteen, int b, int r, boolean preserveSilence, boolean reloading, boolean freeInsts) {
		if (!prepared) {

			System.out.println(String.format(Locale.US, "Opening Timidity: Path: %s cfgFile: %s resample: %s mono: %s sixteenBit: %s buffer: %d rate: %d", path, file, Globals.sampls[resamp], ((mono == 1) ? "true" : "false"), (sixteen ? "true" : "false"), b, r));
			System.out.println("Max channels: " + MAX_CHANNELS);
			for (int i = 0; i < MAX_CHANNELS; i++) {
				volumes.add(75); // Assuming not XG
				programs.add(0);
				drums.add(i == 9);
				custInst.add(false);
				custVol.add(false);
			}
			channelMode = mono;
			sixteenBit = sixteen;
			rate = r;
			buffer = b;
			if (mMediaPlayer == null)
				mMediaPlayer = new MediaPlayer();

			prepared = true;
			return prepareTimidity(path, path + file, (channelMode == 1) ? 1 : 0, resamp, sixteenBit ? 1 : 0, preserveSilence ? 1 : 0, reloading ? 1 : 0, freeInsts ? 1 : 0);
		} else {
			// Log.w("Warning", "Attempt to prepare again cancelled.");
			return -99;
		}
	}

	private static Thread playThread;

	public static int play(final String songTitle) {

		if (new File(songTitle).exists()) {
			if (!isBlocking) {
				keyOffset = 0;
				tb = 0;
				currentLyric = "";
				overwriteLyricAt = 0;
				isPlaying = true;
				finishedCallbackCheck = false;
				isMediaPlayerFormat = false;
				paused = false;
				dataWritten = false;
				shouldPlayNow = true;
				for (int i = 0; i < MAX_CHANNELS; i++) {
					custInst.set(i, false);
					custVol.set(i, false);
				}
				if (!Globals.isMidi(songTitle)) {
					isMediaPlayerFormat = true;
					try {
						mMediaPlayer.setOnCompletionListener(null);
						mMediaPlayer.reset();
						mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
						mMediaPlayer.setVolume(100, 100);
						mMediaPlayer.setDataSource(songTitle);
						mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
							public void onPrepared(MediaPlayer arg0) {
								initSeekBar(arg0.getDuration());
							}
						});
						mMediaPlayer.prepare();
						mMediaPlayer.start();
						isBlocking = true;

						mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
							@Override
							public void onCompletion(MediaPlayer arg0) {
								arg0.setOnCompletionListener(null);

								isPlaying = false;
								finishedCallbackCheck = true;
								isBlocking = false;
							}
						});

					} catch (Exception e) {
						e.printStackTrace();

					}

				} else {
					// Reset the audio track every time.
					// The audiotrack should be in the same thread as the
					// timidity stuff for black midi.
					playThread = new Thread(new Runnable() {
						public void run() {
							isBlocking = true;
							mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, rate,
									(channelMode == 2) ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO,
									(sixteenBit) ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT,
									buffer, AudioTrack.MODE_STREAM);

							if (!(currentWavWriter != null && !currentWavWriter.finishedWriting)) {
								try {
									mAudioTrack.play();
								} catch (Exception e) {
									exceptional |= 1;
								}
							}
							loadSongTimidity(songTitle);
							isBlocking = false;

							if (currentWavWriter != null && !currentWavWriter.finishedWriting)
								currentWavWriter.finishOutput();
						}
					});
					playThread.setPriority(Thread.MAX_PRIORITY);
					playThread.start();

				}
				// }
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
		/*
		 * String[] control = { "PM_REQ_MIDI",
		 * 
		 * "PM_REQ_INST_NAME",
		 * 
		 * "PM_REQ_DISCARD",
		 * 
		 * "PM_REQ_FLUSH",
		 * 
		 * "PM_REQ_GETQSIZ",
		 * 
		 * "PM_REQ_SETQSIZ",
		 * 
		 * "PM_REQ_GETFRAGSIZ",
		 * 
		 * "PM_REQ_RATE",
		 * 
		 * "PM_REQ_GETSAMPLES",
		 * 
		 * "PM_REQ_PLAY_START",
		 * 
		 * "PM_REQ_PLAY_END",
		 * 
		 * "PM_REQ_GETFILLABLE",
		 * 
		 * "PM_REQ_GETFILLED",
		 * 
		 * "PM_REQ_OUTPUT_FINISH",
		 * 
		 * "PM_REQ_DIVISIONS" };
		 */
		if (y == 10) {

			// Globals.isPlaying = 1; // Wait until all is unloaded.
			if (mAudioTrack != null) {
				try {
					mAudioTrack.stop();
				} catch (IllegalStateException ignored) {}
				mAudioTrack.release();
			}
		} /*
			 * else if (y == 10) { // TODO something here to tell that timidity
			 * is ready }
			 */
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
			return (((mAudioTrack.getPlaybackHeadPosition() *
					mAudioTrack.getChannelCount() *
					(2 - (mAudioTrack.getAudioFormat() & 1))))); // 16 bit is 2, 8 bit is 3. We should never have to worry about 4, which is floating.
		} catch (IllegalStateException e) {
			return 0;
		}
	}

	// Used by native (TiMidity++)
	@SuppressWarnings("unused")
	public static void finishCallback() {
		finishedCallbackCheck = true;
		isPlaying = false;
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
		if (isComment) // commentsAlways get newlines
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


}
