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
import android.util.SparseIntArray;
//import android.util.Log;
import android.view.KeyEvent;
import android.widget.RemoteViews;
import android.widget.Toast;

public class MusicService extends Service{

	public ArrayList<String> playList;
	public SparseIntArray shuffledIndices;
	public SparseIntArray reverseShuffledIndices;
	public int currSongNumber=-1;
	public int realSongNumber=-1;
	public boolean shouldStart;
	public boolean shouldAdvance=true;
	public String currFold;
	public int loopMode=1;
	public int shuffleMode=0;
	public boolean paused;
	public boolean fullStop=false;
	public boolean foreground=false;
	public boolean fixedShuffle = true;
	boolean death=false;
	boolean phonepause=false;
	PowerManager.WakeLock wl;
	boolean shouldDoWidget=true;
	int[] ids;
	String currTitle;
	Notification n;
	Notification q;
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
	 
	@Override
	public void onTaskRemoved( Intent rootIntent ) {
	   Intent intent = new Intent( this, DummyActivity.class );
	   intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
	   startActivity( intent );
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
                 //Incoming call: Pause music
            	 if(Globals.isPlaying==0&&!JNIHandler.paused&&!phonepause&&!(JNIHandler.writeToFile&&!JNIHandler.finishedWriting))
            	 {
            		 phonepause=true;
            		 pause();
            	 }
             } else if(state == TelephonyManager.CALL_STATE_IDLE) {
                 //Not in call: Play music
            	 if(Globals.isPlaying==0&&JNIHandler.paused&&phonepause)
            	 {
            		 phonepause=false;
            		 pause();
            	 }
             } else if(state == TelephonyManager.CALL_STATE_OFFHOOK) {
                 //A call is dialing, active or on hold
             }
             super.onCallStateChanged(state, incomingNumber);
         }
     };
	 
	// onHandleIntent ...
	public void genShuffledPlist()
	{
		shuffledIndices = new SparseIntArray();
		reverseShuffledIndices = new SparseIntArray();
		ArrayList<Integer> tmp = new ArrayList<Integer>();
		for(int i = 0; i<playList.size(); i++)
		{
			tmp.add(i);
		}
		Collections.shuffle(tmp);
		
		for(int i = 0; i<playList.size(); i++)
		{
			shuffledIndices.put(i, tmp.get(i));
			reverseShuffledIndices.put(tmp.get(i), i);
		}
	}
	 private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {

			@SuppressWarnings("unchecked")
			@Override
	        public void onReceive(Context context, Intent intent) {
	        	if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
	                KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
	                if (KeyEvent.KEYCODE_MEDIA_PLAY == event.getKeyCode()||KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE == event.getKeyCode()) {
	                	if(Globals.isPlaying==0)
	    				{
	    					pause();
	    				}else{
	    					play();
	    				}
	                }else if(KeyEvent.KEYCODE_MEDIA_NEXT == event.getKeyCode())
	                {
	                	shouldAdvance=false;
		        		next();
	                }else if(KeyEvent.KEYCODE_MEDIA_PREVIOUS == event.getKeyCode())
	                {
	                	shouldAdvance=false;
		        		previous();
	                }else if(KeyEvent.KEYCODE_MEDIA_PAUSE == event.getKeyCode())
	                {
	                	if(Globals.isPlaying==0)
	    				{
	    					pause();
	    				}else{
	    					play();
	    				}
	                }else if(KeyEvent.KEYCODE_MEDIA_STOP == event.getKeyCode())
	                {
	                	fullStop=true;
		        		shouldAdvance=false;
		        		stop();
	                }
	            }else if(Intent.ACTION_HEADSET_PLUG.equals(intent.getAction()))
	            {
	            	if (intent.hasExtra("state")){
	                    if (intent.getIntExtra("state", 0) == 0){
	                        if (Globals.isPlaying==0){
	                        	if(!JNIHandler.paused)
	                        	{
	                        		pause();
	                        	}
	                        }
	                    }
	                }
	        	}else{
	        	int cmd = intent.getIntExtra(getResources().getString(R.string.msrv_cmd), -5); // V
        		//System.out.println("Cmd received: "+cmd);
	        	
	        	// Sigh. Here we go:
	        	switch(cmd)
	        	{
	        	case 0: // We have a new playlist. Load it and the immediate song to load.
	        		death=true;
	        		ArrayList<String> tmpList = Globals.plist;
	        		if(tmpList==null)
	        			break;
	        		int tmpNum = intent.getIntExtra(getResources().getString(R.string.msrv_songnum), -1);
	        		if(tmpNum<=-1)
	        			break;
	        		boolean shouldNotLoadPlist = intent.getBooleanExtra(getResources().getString(R.string.msrv_dlplist), false);
	        		
	        		if(!shouldNotLoadPlist) // mmm. Double negatives.
	        		{	
	        			currSongNumber=realSongNumber=tmpNum;
	        			playList=tmpList;
	        			genShuffledPlist();
	        			
	        		}else{
	        			currSongNumber = tmpNum;
	        			if(shuffleMode == 1)
	        				realSongNumber = shuffledIndices.get(tmpNum);
	        		}
	        		Globals.plist=null;
	        		tmpList=null;
	        		currFold=intent.getStringExtra(getResources().getString(R.string.msrv_currfold));
	        		
	        		if(shuffleMode == 1 && !shouldNotLoadPlist)
	        			currSongNumber = reverseShuffledIndices.get(realSongNumber);
	        		Intent new_intent20 = new Intent(); // I should name these properly at some point. I could use a more global intent variable I guess.
	                new_intent20.setAction(getResources().getString(R.string.ta_rec));
	                new_intent20.putExtra(getResources().getString(R.string.ta_cmd), 4);
	                if(shuffleMode == 1)
	                {
	                	Globals.tmpplist=new ArrayList<String>();
	                	for(int i = 0; i<playList.size(); i++)
	                	{
	                		Globals.tmpplist.add(playList.get(shuffledIndices.get(i)));
	                	}
	                }else{
	                	Globals.tmpplist=playList;
	                }
	                sendBroadcast(new_intent20);
	        		//if(shouldStart=intent.getBooleanExtra(getResources().getString(R.string.msrv_begin),false)||true)
	        	//	{
	        			play();
	        		//}
	        		break;
	        	case 1: // Initial play cmd
	        		play();
	        		break;
	        	case 2: // Play/pause
	        		pause();
	        		break;
	        	case 3: // Next
	        		shouldAdvance=false;
	        		next();
	        		break;
	        	case 4: // Previous
	        		shouldAdvance=false;
	        		previous();
	        		break;
	        	case 5: // Stop
	        		fullStop=true;
	        		Globals.hardStop=true;
	        		shouldAdvance=false;
	        		stop();
	        		break;
	        	case 6: // Loop mode
	        		int tmpMode = intent.getIntExtra(getResources().getString(R.string.msrv_loopmode), -1);
	        		if(tmpMode<0||tmpMode>2)
	        			break;
	        		loopMode=tmpMode;
	        		break;
	        	case 7: // Shuffle mode
	        		shuffleMode = intent.getIntExtra(getResources().getString(R.string.msrv_shufmode), 0);
	        		if(shuffleMode == 1)
	        		{
	        			fixedShuffle = false;
	        			if(Globals.reShuffle||reverseShuffledIndices==null||shuffledIndices==null)
	        			{	
	        				genShuffledPlist();
	        				
	        			}
	        			currSongNumber = reverseShuffledIndices.get(currSongNumber);
	        		}else{
	        			if(!fixedShuffle)
	        			{
	        				currSongNumber = realSongNumber;
	        				fixedShuffle = true;
	        			}
	        		}
	        		Intent new_intent19 = new Intent();
	                new_intent19.setAction(getResources().getString(R.string.ta_rec));
	                new_intent19.putExtra(getResources().getString(R.string.ta_cmd), 4);
	                if(shuffleMode == 1)
	                {
	                	Globals.tmpplist=new ArrayList<String>();
	                	for(int i = 0; i<playList.size(); i++)
	                	{
	                		Globals.tmpplist.add(playList.get(shuffledIndices.get(i)));
	                	}
	                }else{
	                	Globals.tmpplist=playList;
	                }
	                sendBroadcast(new_intent19);
	        		break;
	        	case 8: // Request seekBar times
	        		break;
	        	case 9: // Actually seek
	        		JNIHandler.seekTo(intent.getIntExtra(getResources().getString(R.string.msrv_seektime), 1));
	        		break;
	        	case 10: // Request current folder
	        		Intent new_intent10 = new Intent();
	                new_intent10.setAction(getResources().getString(R.string.ta_rec));
	                new_intent10.putExtra(getResources().getString(R.string.ta_cmd), 2);
	                
	                new_intent10.putExtra(getResources().getString(R.string.ta_currpath), currFold);
	                sendBroadcast(new_intent10);
	        		break;
	        	case 11: // Request player info
	        		Intent new_intent11 = new Intent();
	                new_intent11.setAction(getResources().getString(R.string.ta_rec));
	                new_intent11.putExtra(getResources().getString(R.string.ta_cmd), 3);
	        		new_intent11.putExtra(getResources().getString(R.string.ta_startt),JNIHandler.maxTime);
	                new_intent11.putExtra(getResources().getString(R.string.ta_shufmode), shuffleMode);
	                new_intent11.putExtra(getResources().getString(R.string.ta_loopmode), loopMode);
	                new_intent11.putExtra(getResources().getString(R.string.ta_songttl),currTitle);
	                if(shuffleMode == 1)
	                {
	                	new_intent11.putExtra(getResources().getString(R.string.ta_filename),playList.get(shuffledIndices.get(currSongNumber)));
	                }else{
	                	new_intent11.putExtra(getResources().getString(R.string.ta_filename),playList.get(currSongNumber));
	                }
	                
	                sendBroadcast(new_intent11);
	        		break;
	        	case 12: // Request player info
	        		Intent new_intent12 = new Intent();
	                new_intent12.setAction(getResources().getString(R.string.ta_rec));
	                new_intent12.putExtra(getResources().getString(R.string.ta_cmd), 4);
	                if(shuffleMode == 1)
	                {
	                	Globals.tmpplist=new ArrayList<String>();
	                	for(int i = 0; i<playList.size(); i++)
	                	{
	                		Globals.tmpplist.add(playList.get(shuffledIndices.get(i)));
	                	}
	                }else{
	                	Globals.tmpplist=playList;
	                }
	                sendBroadcast(new_intent12);
	        		break;
	        	case 13:
	        		if(Globals.isPlaying==0)
    				{
    					pause();
    				}else{
    					play();
    				}
	        		break;
	        	case 14: // We want to write an output file
	        		fullStop=true;
	        		shouldAdvance=false;
	        		Globals.hardStop=true;
	        		stop();
	        		while(((Globals.isPlaying==0||JNIHandler.alternativeCheck==333333)))
	        		{
	        			try {
	        				Thread.sleep(10);
	        			} catch (InterruptedException e) {
	        				e.printStackTrace();
	        			}
	        		}
	        		final String input = intent.getStringExtra(getResources().getString(R.string.msrv_infile));
	        			String output = intent.getStringExtra(getResources().getString(R.string.msrv_outfile));
	        			if(input!=null&&output!=null)
		        		{
		        			JNIHandler.setupOutputFile(output);
		        			JNIHandler.play(input);
		        		}
	        		
	        		
	        		new Thread(new Runnable(){

						@Override
						public void run()
						{
							while(!death&&((Globals.isPlaying==1))){
								if(JNIHandler.alternativeCheck==555555)
				    				  death=true;
				      			  try {Thread.sleep(10);} catch (InterruptedException e){}}
							 if(new File(input+".def.tcf").exists() || new File(input+".def.tzf").exists())
			           		  {
								 String suffix;
								 if(new File(input+".def.tcf").exists() && new File(input+".def.tzf").exists())
				           		  {
				           		  	suffix = (Globals.compressCfg?".def.tzf":".def.tcf");
				           		  }else if(new File(input+".def.tcf").exists()){
				           			  suffix = ".def.tcf";
				           		  }else{
				           			  suffix = ".def.tzf";
				           		  }
			           			  JNIHandler.shouldPlayNow=false;
			           			  JNIHandler.currTime=0;
			           			  while(Globals.isPlaying==0&&!death&&!JNIHandler.dataWritten)
			           			  {
			           				  try
			       					{
			       						Thread.sleep(25);
			       					} catch (InterruptedException e)
			       					{
			       						e.printStackTrace();
			       					}
			           			  }
			           		  Intent new_intent = new Intent(); // silly, but should be done async. I think.
			         	        new_intent.setAction(getResources().getString(R.string.msrv_rec));
			         	        new_intent.putExtra(getResources().getString(R.string.msrv_cmd), 17);
			         	        new_intent.putExtra(getResources().getString(R.string.msrv_infile),input+suffix);
			         	        new_intent.putExtra(getResources().getString(R.string.msrv_reset),true);
			         	        sendBroadcast(new_intent);
			           		  }
							
				        		 while(((Globals.isPlaying==0))){
				       	    	  try {Thread.sleep(25);} catch (InterruptedException e){}}
				        		 Intent new_intent14 = new Intent();
					                new_intent14.setAction(getResources().getString(R.string.ta_rec));
					                new_intent14.putExtra(getResources().getString(R.string.ta_cmd), 7);
					                sendBroadcast(new_intent14);
					                death=false;
						}
	        		}).start();
	        		//play();
	                break;
	        	case 15: // We want to write an output file while playing.
	        		shouldAdvance=false;
	        		if(JNIHandler.paused)
	        		{
	        			JNIHandler.pause();
	        			JNIHandler.waitUntilReady(50);
	        		}
	        		JNIHandler.seekTo(0); // Why is this async. Seriously.
	        		JNIHandler.waitUntilReady();
	        		JNIHandler.pause();
	        		JNIHandler.waitUntilReady();
	        		String output2 = intent.getStringExtra(getResources().getString(R.string.msrv_outfile));
	        		if(Globals.isPlaying==0&&output2!=null)
	        		{
	        			JNIHandler.setupOutputFile(output2);
	        			JNIHandler.pause();
	        		}
	        		new Thread(new Runnable(){

						@Override
						public void run()
						{
							while(((Globals.isPlaying==1))){
				      			  try {Thread.sleep(10);} catch (InterruptedException e){}}
				        		 while(((Globals.isPlaying==0))){
				       	    	  try {Thread.sleep(25);} catch (InterruptedException e){}}
				        		 Intent new_intent14 = new Intent();
					                new_intent14.setAction(getResources().getString(R.string.ta_rec));
					                new_intent14.putExtra(getResources().getString(R.string.ta_cmd), 8);
					                sendBroadcast(new_intent14);
					                new_intent14.setAction(getResources().getString(R.string.ta_rec));
					                new_intent14.putExtra(getResources().getString(R.string.ta_cmd), 5);
					                new_intent14.putExtra(getResources().getString(R.string.ta_pause), false);
					                sendBroadcast(new_intent14);
						}
	        		}).start();
	                break;
	        	case 16: // store midi settings
	        		boolean wasPaused=JNIHandler.paused;
	        		if(!JNIHandler.paused)
	        		{
	        			JNIHandler.pause();
	        			JNIHandler.waitUntilReady(50);
	        		}
	        		String output3 = intent.getStringExtra(getResources().getString(R.string.msrv_outfile));
	        		int[] numbers = new int[3];
	        		numbers[0] = JNIHandler.tb;
	        		numbers[1] = JNIHandler.koffset;
	        		numbers[2] = JNIHandler.maxvoice;
	        		String[] serializedSettings=new String[5]; // I'm sorry. I'm sorry.
	        		try
					{
						serializedSettings[0]=ObjectSerializer.serialize(JNIHandler.custInst);
						serializedSettings[1]=ObjectSerializer.serialize(JNIHandler.custVol);
		        		serializedSettings[2]=ObjectSerializer.serialize(JNIHandler.programs);
		        		serializedSettings[3]=ObjectSerializer.serialize(JNIHandler.volumes);
		        		serializedSettings[4]=ObjectSerializer.serialize(numbers);
					} catch (IOException e)
					{
						e.printStackTrace();
					}
	        		
	        		if(Globals.compressCfg)
	        		{
	        			   BufferedWriter writer = null;
	        			    try
	        			    {
	        			        GZIPOutputStream zip = new GZIPOutputStream(new FileOutputStream(new File(output3)));

	        			        writer = new BufferedWriter(new OutputStreamWriter(zip, "US-ASCII"));

	        			        for(String s : serializedSettings)
	    	        			{
	    	        				writer.append(s);
	    	        				writer.newLine();
	    	        			}
	        			    } catch (IOException e) {
								e.printStackTrace();
							}
	        			    finally
	        			    {           
	        			        if(writer != null)
									try {
										writer.close();
									} catch (IOException e) {
										e.printStackTrace();
									}
	        			    }
	        		}else{
	        		FileWriter fw = null;
	        		try {
	        			fw = new FileWriter(output3,false);
	        			for(String s : serializedSettings)
	        			{
	        				fw.write(s+"\n");
	        			}
	        			fw.close();
	        		} catch (IOException e) {
	        			e.printStackTrace();
	        		}}
	        		if(!wasPaused)
	        		{
	        			JNIHandler.pause();
	        			JNIHandler.waitUntilReady(50);
	        		}
	        		Intent new_intent15 = new Intent();
	        		new_intent15.setAction(getResources().getString(R.string.ta_rec));
	                new_intent15.putExtra(getResources().getString(R.string.ta_cmd), 8);
	                sendBroadcast(new_intent15);
	        		break;
	        	case 17: // load midi settings
	        		if(JNIHandler.paused)
	        		{
	        			JNIHandler.pause();
	        			JNIHandler.waitUntilReady(50);
	        		}
	        		String input2 = intent.getStringExtra(getResources().getString(R.string.msrv_infile));
	        		ArrayList<Integer> msprograms = new ArrayList<Integer>();
	        		ArrayList<Boolean> mscustInst = new ArrayList<Boolean>();
	        		ArrayList<Integer> msvolumes = new ArrayList<Integer>();
	        		ArrayList<Boolean> mscustVol = new ArrayList<Boolean>();
	        		int[] newnumbers = new int[3];
	        		FileInputStream fstream;
					try
					{
						BufferedReader br;
						if(input2.endsWith(".tzf"))
						{
							InputStream fileStream = new FileInputStream(input2);
							@SuppressWarnings("resource")
							InputStream gzipStream = new GZIPInputStream(fileStream);
							InputStreamReader decoder = new InputStreamReader(gzipStream, "US-ASCII");
							br = new BufferedReader(decoder);
						}else{
							
						fstream = new FileInputStream(input2);
						 DataInputStream in = new DataInputStream(fstream);
		        	  	  br = new BufferedReader(new InputStreamReader(in));
						}
		        	  	  mscustInst=(ArrayList<Boolean>) ObjectSerializer.deserialize(br.readLine());
		        	  	mscustVol=(ArrayList<Boolean>) ObjectSerializer.deserialize(br.readLine());
		        	  	msprograms=(ArrayList<Integer>) ObjectSerializer.deserialize(br.readLine());
		        	  	msvolumes=(ArrayList<Integer>) ObjectSerializer.deserialize(br.readLine());
		        	  	newnumbers = (int[]) ObjectSerializer.deserialize(br.readLine());
		        	  	br.close();
					} catch (IOException e)
					{
						e.printStackTrace();
					}
					if(mscustInst.size()!=mscustVol.size()||
	        				mscustVol.size()!=msprograms.size()||
	        				msprograms.size()!=msvolumes.size())
					{
	        			// wat
	        			break;	
					}
					for(int i = 0; i<mscustInst.size(); i++)
					{
						if(mscustInst.get(i))
						{
							JNIHandler.setChannelTimidity(i|0x800,msprograms.get(i));
							JNIHandler.programs.set(i,msprograms.get(i));
						}else{
							
							JNIHandler.setChannelTimidity(i|0x8000,msprograms.get(i));
						}
						JNIHandler.custInst.set(i,mscustInst.get(i));
						if(mscustVol.get(i))
						{
							JNIHandler.setChannelVolumeTimidity(i|0x800,msvolumes.get(i));
							JNIHandler.volumes.set(i,msvolumes.get(i));
						}else{
							JNIHandler.setChannelVolumeTimidity(i|0x8000, msvolumes.get(i));
						}
						JNIHandler.custVol.set(i,mscustVol.get(i));
					}
					int newtb = newnumbers[0]-JNIHandler.tb;
					if(newtb>0)
					{
						JNIHandler.controlTimidity(17, newtb);
						JNIHandler.waitUntilReady();
					}else if(newtb<0)
					{
						JNIHandler.controlTimidity(18, -1*newtb);
						JNIHandler.waitUntilReady();
					}
					JNIHandler.tb=newnumbers[0];
					
					int newko = newnumbers[1]-JNIHandler.koffset;
					if(newko>0)
					{
						JNIHandler.controlTimidity(15, newko);
						JNIHandler.waitUntilReady();
					}else if(newko<0)
					{
						JNIHandler.controlTimidity(16, newko);
						JNIHandler.waitUntilReady();
					}
					JNIHandler.koffset=newnumbers[1];
					int newvoice=newnumbers[2]-JNIHandler.maxvoice;
					if(newvoice!=0)
					{
						if(newvoice>0)
						{
							JNIHandler.controlTimidity(19,newvoice);
						}else{
							JNIHandler.controlTimidity(20,-1*newvoice);
						}
						JNIHandler.waitUntilReady();
					}
					if(intent.getBooleanExtra(getResources().getString(R.string.msrv_reset), false))
					{
						JNIHandler.seekTo(0);
						JNIHandler.shouldPlayNow=true;
						JNIHandler.waitUntilReady();
					}
	        		break;
	        	case 18:	// Reload native libs
	        		if(!JNIHandler.type)
	        		{
	        			fullStop=true;
		        		Globals.hardStop=true;
		        		shouldAdvance=false;
	        			stop();
	        			JNIHandler.waitForStop();
	        		}
	        		System.out.println("Unloading: "+JNIHandler.unloadLib());
	        		JNIHandler.prepared = false;
	        		JNIHandler.volumes = new ArrayList<Integer>();
	        		JNIHandler.programs = new ArrayList<Integer>();
	        		JNIHandler.drums = new ArrayList<Boolean>();
	        		JNIHandler.custInst = new ArrayList<Boolean>();
	        		JNIHandler.custVol = new ArrayList<Boolean>();
	        		System.out.println("Reloading: "+JNIHandler.loadLib(Globals.getLibDir(MusicService.this)+"libtimidityplusplus.so"));
	        		int x = JNIHandler.init(Globals.dataFolder+"timidity/","timidity.cfg", Globals.mono, Globals.defSamp, Globals.sixteen, Globals.buff, Globals.aRate, false, true);
	        		if(x!=0&&x!=-99)
	        		{
	        			Globals.nativeMidi=true;
	        			Toast.makeText(MusicService.this, String.format(getResources().getString(R.string.tcfg_error), x), Toast.LENGTH_LONG).show();
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
            intentFilter.addAction(getResources().getString(R.string.msrv_rec));
            intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
            intentFilter.addAction(Intent.ACTION_MEDIA_BUTTON);
            registerReceiver(serviceReceiver, intentFilter);
        }
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		  wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Timidity AE");
		  wl.setReferenceCounted(false);
		  if(shouldDoWidget)
			  ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), TimidityAEWidgetProvider.class));
       
        TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if(mgr != null) {
            mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        //foreground=false;
        if(wl.isHeld())
        	wl.release();
        
        stopForeground(true);
        
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(serviceReceiver);
    }
	@SuppressLint("NewApi")
	public void play()
	{
		if(playList!=null&&currSongNumber>=0)
		{
			shouldAdvance=false;
			death=true;
			fullStop=false;
		stop();
		death=false;
		Globals.shouldRestore=true;
		while(!death&&((Globals.isPlaying==0||JNIHandler.alternativeCheck==333333)))
		{
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		if(!death)
		{
		MediaMetadataRetriever mmr = new MediaMetadataRetriever();
		String tmpTitle;
		Globals.currArt=null;
		final int songIndex;
		if(shuffleMode == 1)
		{
			songIndex = realSongNumber = shuffledIndices.get(currSongNumber);
		}else{
			songIndex = realSongNumber = currSongNumber;
		}
		try{
		mmr.setDataSource(playList.get(songIndex));
		tmpTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
		if(tmpTitle!=null)
		{
			if(TextUtils.isEmpty(tmpTitle))
				tmpTitle=playList.get(songIndex).substring(playList.get(songIndex).lastIndexOf('/')+1);
		}else{
			tmpTitle=playList.get(songIndex).substring(playList.get(songIndex).lastIndexOf('/')+1);
		}
		
		}catch (RuntimeException e)
		{
			tmpTitle=playList.get(songIndex).substring(playList.get(songIndex).lastIndexOf('/')+1);
		}
		boolean goodart=false;
		if(Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) // Please work
		{
		try{
			
			byte[] art = mmr.getEmbeddedPicture();
			if(art!=null)
			{
				Globals.currArt=BitmapFactory.decodeByteArray(art, 0, art.length);
				goodart=Globals.currArt!=null;
			}
		}catch (Exception e){}
		}
		if(!goodart)
		{
			String goodPath=playList.get(songIndex).substring(0,playList.get(songIndex).lastIndexOf('/')+1)+"folder.jpg";
			if(new File(goodPath).exists())
			{
				try{
				Globals.currArt=BitmapFactory.decodeFile(goodPath);
				}catch (RuntimeException e){}
			}else{
				// Try albumart.jpg
				goodPath=playList.get(songIndex).substring(0,playList.get(songIndex).lastIndexOf('/')+1)+"AlbumArt.jpg";
				if(new File(goodPath).exists())
				{
					try{
						Globals.currArt=BitmapFactory.decodeFile(goodPath);
						}catch (RuntimeException e)
						{
							// 
						}
				}
			}
		}
		if(shouldDoWidget)
		{
			Intent intent = new Intent(this,TimidityAEWidgetProvider.class);
			intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			// Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
			// since it seems the onUpdate() is only fired on that:
			ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), TimidityAEWidgetProvider.class));
		
			//intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
			intent.putExtra("com.xperia64.timidityae.timidityaewidgetprovider.onlyart", true);
			sendBroadcast(intent);
		}
		Intent new_intent = new Intent();
        new_intent.setAction(getResources().getString(R.string.ta_rec));
        new_intent.putExtra(getResources().getString(R.string.ta_cmd), 6);
        sendBroadcast(new_intent);
        
		currTitle=tmpTitle;
		shouldAdvance=true;
		paused=false;
		
      final int x=JNIHandler.play(playList.get(songIndex));
      if(x!=0)
      {
    	  switch(x)
    	  {
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
    		  Toast.makeText(getApplicationContext(), String.format(getResources().getString(R.string.srv_unk),x), Toast.LENGTH_SHORT).show();
		   }
		});
    		  break;
    	  }
    	  
    	  Globals.isPlaying=1;
     	 JNIHandler.type=true;
     	 shouldAdvance=false;
     	 JNIHandler.paused=false;
     	stop();
      }else{
    	  updateNotification(currTitle, paused);    	  
      new Thread(new Runnable(){
    	  public void run()
    	  {
    		  while(!death&&((Globals.isPlaying==1&&shouldAdvance))){
    			  if(JNIHandler.alternativeCheck==555555)
    				  death=true;
    			  
    			  //System.out.println(String.format("alt check: %d death: %s isplaying: %d shouldAdvance: %s seekBarReady: %s",JNIHandler.alternativeCheck,death?"true":"false",Globals.isPlaying,shouldAdvance?"true":"false",JNIHandler.seekbarReady?"true":"false"));
    			  try {Thread.sleep(10);} catch (InterruptedException e){}}
    		  if(!death)
    		  {
    		  Intent new_intent = new Intent();
    	        new_intent.setAction(getResources().getString(R.string.ta_rec));
    	        new_intent.putExtra(getResources().getString(R.string.ta_cmd), 0);
    	        new_intent.putExtra(getResources().getString(R.string.ta_startt),JNIHandler.maxTime);
    	        new_intent.putExtra(getResources().getString(R.string.ta_songttl),currTitle);
    	        new_intent.putExtra(getResources().getString(R.string.ta_filename),playList.get(songIndex));
    	        sendBroadcast(new_intent);
    		  }
    		  if(new File(playList.get(songIndex)+".def.tcf").exists()||new File(playList.get(songIndex)+".def.tzf").exists())
    		  {
    			  String suffix;
    			  if(new File(playList.get(songIndex)+".def.tcf").exists()&&new File(playList.get(songIndex)+".def.tzf").exists())
    			  {
    				  suffix = (Globals.compressCfg?".def.tzf":".def.tcf");
    			  }else if(new File(playList.get(songIndex)+".def.tcf").exists())
    			  {
    				  suffix = ".def.tcf";
    			  }else{
    				  suffix = ".def.tzf";
    			  }
    			  JNIHandler.shouldPlayNow=false;
    			  JNIHandler.currTime=0;
    			  while(Globals.isPlaying==0&&!death&&shouldAdvance&&!JNIHandler.dataWritten)
    			  {
    				  try
					{
						Thread.sleep(25);
					} catch (InterruptedException e)
					{
						e.printStackTrace();
					}
    			  }
    		  Intent new_intent = new Intent(); // silly, but should be done async. I think.
  	        new_intent.setAction(getResources().getString(R.string.msrv_rec));
  	        new_intent.putExtra(getResources().getString(R.string.msrv_cmd), 17);
  	        new_intent.putExtra(getResources().getString(R.string.msrv_infile),playList.get(songIndex)+suffix);
  	        new_intent.putExtra(getResources().getString(R.string.msrv_reset),true);
  	        sendBroadcast(new_intent);
    		  }
    	      while(!death&&(((Globals.isPlaying==0||JNIHandler.alternativeCheck==333333)&&shouldAdvance))){
    	    	  try {Thread.sleep(25);} catch (InterruptedException e){}}
    	      if(shouldAdvance&&!death)
    	      {
    	    	  shouldAdvance=false;
    	    	  new Thread(new Runnable(){
    	        	  public void run()
    	        	  {
    	    	  if(playList.size()>1&&(((songIndex+1<playList.size()&&loopMode==0))||loopMode==1)){next();}
        	      else if(loopMode==2||playList.size()==1){play();}else if(loopMode==0){Globals.hardStop=true; Intent new_intent = new Intent();
                  new_intent.setAction(getResources().getString(R.string.ta_rec));
                  new_intent.putExtra(getResources().getString(R.string.ta_cmd), 5);
                  new_intent.putExtra(getResources().getString(R.string.ta_pause), false);
                  sendBroadcast(new_intent);}
    	        	  }
    	    	  }).start();
    	    	  
    	      }
    	      
    	  }
      }).start();
	}
		}
		}
	}
	public void pause()
	{
		if(playList!=null&&currSongNumber>=0)
		{
		if(Globals.isPlaying==0)
		{
		paused=!paused;
		JNIHandler.pause();
		Intent new_intent = new Intent();
        new_intent.setAction(getResources().getString(R.string.ta_rec));
        new_intent.putExtra(getResources().getString(R.string.ta_cmd), 5);
        new_intent.putExtra(getResources().getString(R.string.ta_pause), true);
        new_intent.putExtra(getResources().getString(R.string.ta_pausea), paused);
        sendBroadcast(new_intent);
        updateNotification(currTitle, paused);
        
        
		}
		}
	}
	public void next()
	{
		death=true;
		if(playList!=null&&currSongNumber>=0)
		{
			
			if(playList.size()>1)
			{
				if(shuffleMode==2){
					int tmpNum=currSongNumber;
					while(tmpNum==currSongNumber)
					{
						try {
							Thread.sleep(10); // Don't hog CPU. Please.
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						tmpNum=random.nextInt(playList.size());
					}
					currSongNumber=tmpNum;
				}else{
					if(++currSongNumber>=playList.size())
					{
						currSongNumber=0;
					}
				}
			}
		play();
		}
	}
	public void previous()
	{
		death=true;
		if(playList!=null&&currSongNumber>=0)
		{
		currSongNumber-=1;
		if(currSongNumber<0)
		{
			currSongNumber=playList.size()-1;
		}
		play();
		}
	}
	public void stop()
	{
		if(Globals.isPlaying==0)
		{

			death=true;
			Intent new_intent = new Intent();
            new_intent.setAction(getResources().getString(R.string.ta_rec));
            new_intent.putExtra(getResources().getString(R.string.ta_cmd), 5);
            new_intent.putExtra(getResources().getString(R.string.ta_pause), false);
            sendBroadcast(new_intent);
            
            
			Globals.shouldRestore=false;
			JNIHandler.stop();
			if(fullStop)
			{
				TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
				if(mgr != null) {
				    mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
				}
				Globals.shouldRestore=false;
				if(wl.isHeld())
					wl.release();
				foreground=false;
				fullStop=false;
				// Fix the widget
				if(shouldDoWidget)
				{
	            new_intent = new Intent(this, TimidityAEWidgetProvider.class);
	            new_intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
	            //new_intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
	            new_intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
	            new_intent.putExtra("com.xperia64.timidityae.timidityaewidgetprovider.paused", true);
	            new_intent.putExtra("com.xperia64.timidityae.timidityaewidgetprovider.title", "");
	            new_intent.putExtra("com.xperia64.timidityae.timidityaewidgetprovider.onlyart", false);
	            new_intent.putExtra("com.xperia64.timidityae.timidityaewidgetprovider.death", true);
	    		sendBroadcast(new_intent);
				}else{
					Globals.nukedWidgets=true;
				}
			stopForeground(true);
			//stopSelf();
			
			}
		}
	}
	public void updateNotification(String title, boolean paused)
	{
		//System.out.println("Updating notification");
		
		remoteViews = new RemoteViews(getPackageName(),
				R.layout.music_notification);
				remoteViews.setTextViewText(R.id.titley, currTitle);
				remoteViews.setImageViewResource(R.id.notPause, (paused)?R.drawable.ic_media_play:R.drawable.ic_media_pause);
				// Previous
				Intent new_intent = new Intent();
				//new_intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
				new_intent.setAction(getResources().getString(R.string.msrv_rec));
				new_intent.putExtra(getResources().getString(R.string.msrv_cmd), 4);
				PendingIntent pendingNotificationIntent = PendingIntent.getBroadcast(this, 1, new_intent, 0);
				remoteViews.setOnClickPendingIntent(R.id.notPrev, pendingNotificationIntent);
				// Play/Pause
				new_intent = new Intent();
				//new_intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
				new_intent.setAction(getResources().getString(R.string.msrv_rec));
				new_intent.putExtra(getResources().getString(R.string.msrv_cmd), 2);
				pendingNotificationIntent = PendingIntent.getBroadcast(this, 2, new_intent, 0);
				remoteViews.setOnClickPendingIntent(R.id.notPause, pendingNotificationIntent);
				// Next
				new_intent = new Intent();
				//new_intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
				new_intent.setAction(getResources().getString(R.string.msrv_rec));
				new_intent.putExtra(getResources().getString(R.string.msrv_cmd), 3);
				pendingNotificationIntent = PendingIntent.getBroadcast(this, 3, new_intent, 0);
				remoteViews.setOnClickPendingIntent(R.id.notNext, pendingNotificationIntent);
				// Stop
				new_intent = new Intent();
				//new_intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
				new_intent.setAction(getResources().getString(R.string.msrv_rec));
				new_intent.putExtra(getResources().getString(R.string.msrv_cmd), 5);
				pendingNotificationIntent = PendingIntent.getBroadcast(this, 4, new_intent, 0);
				remoteViews.setOnClickPendingIntent(R.id.notStop, pendingNotificationIntent);
				final Intent emptyIntent = new Intent(this, TimidityActivity.class);
				//emptyIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
				PendingIntent pendingIntent = PendingIntent.getActivity(this, 5, emptyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
				.setContentTitle(getResources().getString(R.string.app_name))
				.setContentText(currTitle)
				.setContentIntent(pendingIntent)
				.setContent(remoteViews);
				if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP)
					mBuilder.setSmallIcon(R.drawable.ic_lol);
				else
					mBuilder.setSmallIcon(R.drawable.ic_launcher);
				n=mBuilder.build();
				n.flags|=Notification.FLAG_ONLY_ALERT_ONCE|Notification.FLAG_ONGOING_EVENT;
				if(!foreground)
				{
				foreground=true;
				startForeground(13901858, n);
				if(!wl.isHeld())
					wl.acquire();
				 

				TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
				if(mgr != null) {
				mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
				}
				}else{
					if(!wl.isHeld())
						wl.acquire();
				NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				mNotificationManager.notify(13901858, n);
				}
				if(shouldDoWidget)
				{
				Intent intent = new Intent(this, TimidityAEWidgetProvider.class);
				//Intent intent = new Intent("Ilikepotatoes");
				intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			// Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
			// since it seems the onUpdate() is only fired on that:
				//intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
				intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
				intent.putExtra("com.xperia64.timidityae.timidityaewidgetprovider.paused", paused);
				intent.putExtra("com.xperia64.timidityae.timidityaewidgetprovider.title", currTitle);
				intent.putExtra("com.xperia64.timidityae.timidityaewidgetprovider.onlyart", true);
				sendBroadcast(intent);
				}
				}
	
	}


