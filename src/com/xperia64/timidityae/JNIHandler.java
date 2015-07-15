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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;


//import android.util.Log;

public class JNIHandler {
	


	// config folder, config file, mono, resampling algorithm, bits, preserve
	// silence
	private static native int prepareTimidity(String config, String config2,
			int jmono, int jcustResamp, int jsixteen, int jPresSil);

	private static native int loadSongTimidity(String filename);

	// See globals for commands
	public static native void controlTimidity(int jcmd, int jcmdArg);

	// -------------------------
	public static native void setChannelTimidity(int jchan, int jprog);

	public static native void setChannelVolumeTimidity(int jchan, int jvol);

	public static native boolean timidityReady();

	public static native int setResampleTimidity(int jcustResamp);

	public static native int decompressSFArk(String from, String to);
	public static DataOutputStream outFile;
	public static AudioTrack mAudioTrack;
	public static MediaPlayer mMediaPlayer;
	public static int maxTime = 0;
	public static int currTime = 0;
	public static boolean paused = false;
	public static boolean type = true; // true = mediaplayer, false = audiotrack
	public static boolean monoop;
	public static boolean bits;
	public static int rate;
	public static int buffer;
	public static int alternativeCheck = 555555; // VVVVVV=OK.
	// Timidity Stuff
	public static int MAX_CHANNELS = 32;
	public static int currsamp = 0;
	public static ArrayList<Integer> programs = new ArrayList<Integer>();
	public static ArrayList<Boolean> custInst = new ArrayList<Boolean>();
	public static ArrayList<Integer> volumes = new ArrayList<Integer>();
	public static ArrayList<Boolean> custVol = new ArrayList<Boolean>();
	public static ArrayList<Boolean> drums = new ArrayList<Boolean>();
	public static String currentLyric = "";
	public static int overwriteLyricAt = 0;
	public static int threadedError = 0;
	public static int exceptional = 0;
	public static int ttr;
	public static int tt;
	public static int tb=0;
	public static int voice;
	public static int maxvoice=256;
	public static int koffset=0;
	// file output things
	public static boolean writeToFile = false;
	public static long filesize=0;
	public static String fileToWrite="";
	public static boolean finishedWriting=false;
	public static boolean dataWritten=false;

	public static boolean shouldPlayNow = true;

	// public static ArrayList<String> lyricLines;
	// End Timidity Stuff

	static boolean prepared = false;

	public static void pause() // or unpause.
	{
		if (Globals.isPlaying == 0)
		{
			if (paused)
			{
				paused = false;
				if (type)
				{
					mMediaPlayer.start();
				} else
				{
					controlTimidity(7, 0);
					if(!(writeToFile&&!finishedWriting)&&mAudioTrack!=null)
					{
					try
					{
						mAudioTrack.play();
					} catch (Exception e)
					{
						e.printStackTrace();
					}
					}
				}
			} else
			{
				paused = true;
				if (type)
				{
					mMediaPlayer.pause();
				} else
				{
					controlTimidity(7, 0);
					if(!(writeToFile&&!finishedWriting)&&mAudioTrack!=null)
					{
					try
					{
						mAudioTrack.pause();
					} catch (Exception e)
					{
						e.printStackTrace();
					}
					}
				}
			}
		}
	}

	public static void stop()
	{
		if (type)
		{
			mMediaPlayer.setOnCompletionListener(null);
			try
			{
				mMediaPlayer.stop();
			} catch (IllegalStateException e)
			{
			}
			Globals.isPlaying = 1;
			alternativeCheck = 555555;
		} else
		{
			controlTimidity(30, 0);
		}
	}

	public static void seekTo(int time)
	{
		if (type)
		{
			mMediaPlayer.seekTo(time);
		} else
		{
			controlTimidity(6, time);
			waitUntilReady();
		}
	}
	private static byte[] intToByteArray(int i)
    {
        byte[] b = new byte[4];
        b[0] = (byte) (i & 0xFF);
        b[1] = (byte) ((i >> 8) & 0xFF);
        b[2] = (byte) ((i >> 16) & 0xFF);
        b[3] = (byte) ((i >> 24) & 0xFF);
        return b;
    }

	public static void waitUntilReady()
	{
		waitUntilReady(10);
	}
	public static void waitUntilReady(int interval)
	{
		while(!JNIHandler.timidityReady())
		{
			  try {Thread.sleep(interval);} catch (InterruptedException e){}
		}
	}
    public static byte[] shortToByteArray(short data)
    {
        return new byte[]{(byte)(data & 0xff),(byte)((data >>> 8) & 0xff)};
    }
	public static void setupOutputFile(String filename)
	{
		if(Globals.isPlaying==0)
		{
			mAudioTrack.stop();
			mAudioTrack.release();
		}
		writeToFile=true;
		finishedWriting=false;
			fileToWrite=filename;
			filesize=0;
			 try {
			        long mySubChunk1Size = 16;
			        int myBitsPerSample= (bits?16:8);
			        int myFormat = 1;
			        long myChannels = (monoop?1:2);
			        long mySampleRate = rate;
			        long myByteRate = mySampleRate * myChannels * myBitsPerSample/8;
			        int myBlockAlign = (int) (myChannels * myBitsPerSample/8);

			        //byte[] clipData = getBytesFromFile(fileToConvert);

			        //long myDataSize = clipData.length; // this changes
			        //long myChunk2Size =  myDataSize * myChannels * myBitsPerSample/8;
			        //long myChunkSize = 36 + myChunk2Size;

			        OutputStream os;        
			        os = new FileOutputStream(new File(fileToWrite));
			        BufferedOutputStream bos = new BufferedOutputStream(os);
			         outFile = new DataOutputStream(bos);

			        outFile.writeBytes("RIFF");                                 // 00 - RIFF
			        outFile.write(intToByteArray(0/*(int)myChunkSize*/), 0, 4);      // 04 - how big is the rest of this file?
			        outFile.writeBytes("WAVE");                                 // 08 - WAVE
			        outFile.writeBytes("fmt ");                                 // 12 - fmt 
			        outFile.write(intToByteArray((int)mySubChunk1Size), 0, 4);  // 16 - size of this chunk
			        outFile.write(shortToByteArray((short)myFormat), 0, 2);     // 20 - what is the audio format? 1 for PCM = Pulse Code Modulation
			        outFile.write(shortToByteArray((short)myChannels), 0, 2);   // 22 - mono or stereo? 1 or 2?  (or 5 or ???)
			        outFile.write(intToByteArray((int)mySampleRate), 0, 4);     // 24 - samples per second (numbers per second)
			        outFile.write(intToByteArray((int)myByteRate), 0, 4);       // 28 - bytes per second
			        outFile.write(shortToByteArray((short)myBlockAlign), 0, 2); // 32 - # of bytes in one sample, for all channels
			        outFile.write(shortToByteArray((short)myBitsPerSample), 0, 2);  // 34 - how many bits in a sample(number)?  usually 16 or 24
			        outFile.writeBytes("data");                                 // 36 - data
			        outFile.write(intToByteArray(0/*(int)myChunkSize*/), 0, 4);       // 40 - how big is this data chunk
			        //outFile.write(clipData);                                    // 44 - the actual data itself - just a long string of numbers

			        //outFile.flush();
			        //outFile.close();

			    } catch (IOException e) {
			        e.printStackTrace();
			    }
	}
	
	private static void finishOutput()
	{
		if(writeToFile&&!finishedWriting)
		{
			try
			{
				outFile.flush();
				outFile.close();
			} catch (IOException e1)
			{
				e1.printStackTrace();
			}
			finishedWriting=true;
			long myDataSize = filesize; // this changes
	        int myBitsPerSample= (bits?16:8);
	        long myChannels = (monoop?1:2);
			long myChunk2Size =  myDataSize * myChannels * myBitsPerSample/8;
	        long myChunkSize = 36 + myChunk2Size;
	     
	        RandomAccessFile raf = null;
			try
			{
				 raf = new RandomAccessFile(fileToWrite, "rw");
			} catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			
			try
			{
				raf.seek(04);
				raf.write(intToByteArray((int)myChunkSize));
				raf.seek(40);
				raf.write(intToByteArray((int)myDataSize));
				raf.close();
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	public static void buffit(byte[] data, int length)
	{
		dataWritten=true;
		if(shouldPlayNow)
		{
			if(writeToFile&&!finishedWriting)
			{
				try
				{
					filesize+=length;
					outFile.write(data, 0, length);
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}else{
				try
				{
					mAudioTrack.write(data, 0, length);
				} catch (IllegalStateException e)
				{}
			}
		}
			

	}

	public static int init(String path, String file, boolean mono, int resamp, boolean sixteen, int b, int r, boolean preserveSilence)
	{
		if (!prepared)
		{
			 System.out.println(String.format("Opening Timidity: Path: %s cfgFile: %s resample: %s mono: %s sixteenBit: %s buffer: %d rate: %d",
			 path, file,
			 Globals.sampls[resamp],(mono?"true":"false"),(sixteen?"true":"false"),b,
			 r));
			for (int i = 0; i < MAX_CHANNELS; i++)
			{
				volumes.add(75); // Assuming not XG
				programs.add(0);
				drums.add(i == 9);
				custInst.add(false);
				custVol.add(false);
			}
			monoop = mono;
			bits = sixteen;
			rate = r;
			buffer = b;
			mMediaPlayer = new MediaPlayer();

			prepared = true;
			return prepareTimidity(path, path + file, monoop ? 1 : 0, resamp,
					bits ? 1 : 0, preserveSilence ? 1 : 0);
		} else
		{
			// Log.w("Warning", "Attempt to prepare again cancelled.");
			return -99;
		}
	}

	
	static Thread t;
	public static int play(final String songTitle)
	{
		if (new File(songTitle).exists())
		{

			if (alternativeCheck == 555555)
			{
				koffset=0;
				tb=0;
				currentLyric = "";
				overwriteLyricAt = 0;
				Globals.isPlaying = 0;
				type = false;
				paused = false;
				dataWritten=false;
				shouldPlayNow=true;
				for (int i = 0; i < MAX_CHANNELS; i++)
				{
					custInst.set(i,false);
					custVol.set(i,false);
				}
				if (!Globals.isMidi(songTitle))
				{
					type = true;
					try
					{
						mMediaPlayer.setOnCompletionListener(null);
						mMediaPlayer.reset();
						mMediaPlayer
								.setAudioStreamType(AudioManager.STREAM_MUSIC);
						mMediaPlayer.setVolume(100, 100);
						mMediaPlayer.setDataSource(songTitle);
						mMediaPlayer
								.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
									public void onPrepared(MediaPlayer arg0)
									{
										initSeeker(arg0.getDuration());
									}
								});
						mMediaPlayer.prepare();
						mMediaPlayer.start();
						alternativeCheck = 333333;

						mMediaPlayer
								.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
									@Override
									public void onCompletion(MediaPlayer arg0)
									{
										arg0.setOnCompletionListener(null);

										Globals.isPlaying = 1;
										alternativeCheck = 555555;
									}
								});

					} catch (Exception e)
					{
						e.printStackTrace();

					}


				} else
				{
					// Reset the audio track every time.
					// The audiotrack should be in the same thread as the
					// timidity stuff for black midi.
					t = new Thread(new Runnable() {
						public void run()
						{
							alternativeCheck = 333333;

							mAudioTrack = new AudioTrack(
									AudioManager.STREAM_MUSIC, rate,
									(!monoop) ? AudioFormat.CHANNEL_OUT_STEREO
											: AudioFormat.CHANNEL_OUT_MONO,
									(bits) ? AudioFormat.ENCODING_PCM_16BIT
											: AudioFormat.ENCODING_PCM_8BIT,
									buffer, AudioTrack.MODE_STREAM);

							if(!(writeToFile&&!finishedWriting))
								try
								{
									mAudioTrack.play();
								} catch (Exception e)
								{
									exceptional |= 1;
								}
							
							alternativeCheck=loadSongTimidity(songTitle);
							alternativeCheck = 555555;
							Globals.isPlaying = 1;
							if(writeToFile&&!finishedWriting)
								finishOutput();
						}
					});
					t.setPriority(Thread.MAX_PRIORITY);
					t.start();

				}
				// }
				return 0;
			} else
			{

				return -9;
			}
		}
		return -1;
	}

	public static void controlMe(int y)
	{
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
		if (y == 10)
		{
			Globals.isPlaying = 1; // Wait until all is unloaded.
			if (mAudioTrack != null)
			{
				try
				{
					mAudioTrack.stop();
				} catch (IllegalStateException e)
				{
				}
			}
			mAudioTrack.release();

		} /*else if (y == 10)
		{
			// TODO something here to tell that timidity is ready
		}*/
	}

	public static int bufferSize()
	{
		if(writeToFile&&!finishedWriting)
		{
			return 0;
		}
		try
		{

			return (((int) (mAudioTrack.getPlaybackHeadPosition()
					* mAudioTrack.getChannelCount() * (2 - (mAudioTrack
					.getAudioFormat() & 1))))); // # of frames *4 = comparison
												// factor
		} catch (IllegalStateException e)
		{
			return 0;
		}
	}

	public static void finishIt()
	{
	}

	public static void flushIt()
	{
		mAudioTrack.flush();
	}

	public static void initSeeker(int seeker)
	{
		maxTime = seeker;
	}

	public static void updateSeeker(int seekIt, int voices)
	{
		currTime = seekIt;
		voice=voices;
	}

	public static int getRate()
	{
		return mAudioTrack.getSampleRate();
	}

	public static void updateLyrics(byte[] b)
	{
		final StringBuilder stb = new StringBuilder(currentLyric);
		final StringBuilder tmpBuild = new StringBuilder();
		boolean isNormalLyric = b[0] == 'L';
		boolean isNewline = b[0] == 'N';
		boolean isComment = b[0] == 'Q';

		for (int i = 2; i < b.length; i++)
		{
			if (b[i] == 0)
				break;
			tmpBuild.append((char) b[i]);
		}
		if (isComment) // commentsAlways get newlines
		{
			stb.append(tmpBuild);
			stb.append('\n');
			overwriteLyricAt = stb.length();
		} else if (isNewline || isNormalLyric)
		{
			if (isNewline)
			{
				stb.append('\n');
				overwriteLyricAt = stb.length();
			}
			stb.replace(overwriteLyricAt, stb.length(), tmpBuild.toString());
		} else
		{ // A marker or something
			stb.append(tmpBuild);
			stb.append("\n");
			overwriteLyricAt = stb.length();
		}
		currentLyric = stb.toString();
	}

	public static void updateMaxChannels(int val)
	{
		MAX_CHANNELS = val;
	}

	public static void updateProgramInfo(int ch, int prog)
	{
		if (ch < MAX_CHANNELS)
			programs.set(ch, prog);
	}

	public static void updateVolInfo(int ch, int vol)
	{
		if (ch < MAX_CHANNELS)
			volumes.set(ch, vol);
	}

	public static void updateDrumInfo(int ch, int isDrum)
	{
		if (ch < MAX_CHANNELS)
			drums.set(ch, (isDrum != 0));
	}

	public static void updateTempo(int t, int tr)
	{
		tt=t;
		ttr=tr;
		// TODO something
		// int x = (int) (500000 / (double) t * 120 * (double) tr / 100 + 0.5);
		// System.out.println("T: "+t+ " TR: "+tr+" X: "+x);
	}
	public static void updateMaxVoice(int vvv)
	{
		maxvoice=vvv;
	}
	public static void updateKey(int k)
	{
		koffset=k;
	}
}
