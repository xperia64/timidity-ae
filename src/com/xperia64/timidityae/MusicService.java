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
import java.util.ArrayList;
import java.util.Random;

import com.xperia64.timidityae.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
//import android.util.Log;
import android.view.KeyEvent;
import android.widget.RemoteViews;
import android.widget.Toast;
public class MusicService extends Service{

	public ArrayList<String> playList;
	public int currSongNumber=-1;
	public boolean shouldStart;
	public boolean shouldAdvance=true;
	public String currFold;
	public int loopMode=1;
	public boolean shuffleMode=false;
	public boolean paused;
	public boolean fullStop=false;
	public boolean foreground=false;
	boolean death=false;
	boolean phonepause=false;
	String currTitle;
	Notification n;
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
	public int onStartCommand(Intent intent, int flags, int startId) {
	   handler = new Handler();
	   return super.onStartCommand(intent, flags, startId);
	}
	 PhoneStateListener phoneStateListener = new PhoneStateListener() {
         @Override
         public void onCallStateChanged(int state, String incomingNumber) {
             if (state == TelephonyManager.CALL_STATE_RINGING) {
                 //Incoming call: Pause music
            	 if(Globals.isPlaying==0&&!JNIHandler.paused&&!phonepause)
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
	
	 private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {

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
	            }else{
	        	int cmd = intent.getIntExtra(getResources().getString(R.string.msrv_cmd), -5); // V
	        	switch(cmd)
	        	{
	        	case 0: // We have a new playlist. Load it and the immediate song to load.
	        		death=true;
	        		ArrayList<String> tmpList = Globals.plist;//intent.getStringArrayListExtra("com.xperia64.timidityae.MusicService_PList");
	        		if(tmpList==null)
	        			break;
	        		int tmpNum = intent.getIntExtra(getResources().getString(R.string.msrv_songnum), -1);
	        		if(tmpNum<=-1)
	        			break;
	        		currSongNumber=tmpNum;
	        		playList=tmpList;
	        		Globals.plist=null;
	        		tmpList=null;
	        		currFold=intent.getStringExtra(getResources().getString(R.string.msrv_currfold));
	        		//System.out.println("saf: "+playList.get(currSongNumber));
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
	        		shuffleMode = intent.getBooleanExtra(getResources().getString(R.string.msrv_shufmode), false);
	        		break;
	        	case 8: // Request seekBar times
	        		break;
	        	case 9: // Actually seek
	        		JNIHandler.seekTo(intent.getIntExtra(getResources().getString(R.string.msrv_seektime), 0));
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
	                new_intent11.putExtra(getResources().getString(R.string.ta_filename),playList.get(currSongNumber));
	                sendBroadcast(new_intent11);
	        		break;
	        	case 12: // Request player info
	        		Intent new_intent12 = new Intent();
	                new_intent12.setAction(getResources().getString(R.string.ta_rec));
	                new_intent12.putExtra(getResources().getString(R.string.ta_cmd), 4);
	                Globals.plist=playList;
	                sendBroadcast(new_intent12);
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
            intentFilter.addAction(Intent.ACTION_MEDIA_BUTTON);
            registerReceiver(serviceReceiver, intentFilter);
        }
       
        TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if(mgr != null) {
            mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        foreground=false;
        stopForeground(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(serviceReceiver);
    }
	public void play()
	{
		if(playList!=null&&currSongNumber>=0)
		{
			shouldAdvance=false;
			death=true;
		stop();
		fullStop=false;
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
		try{
		mmr.setDataSource(playList.get(currSongNumber));
		tmpTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
		if(tmpTitle!=null)
		{
			if(TextUtils.isEmpty(tmpTitle))
				tmpTitle=playList.get(currSongNumber).substring(playList.get(currSongNumber).lastIndexOf('/')+1);
		}else{
			tmpTitle=playList.get(currSongNumber).substring(playList.get(currSongNumber).lastIndexOf('/')+1);
		}
		
		}catch (RuntimeException e)
		{
			tmpTitle=playList.get(currSongNumber).substring(playList.get(currSongNumber).lastIndexOf('/')+1);
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
			String goodPath=playList.get(currSongNumber).substring(0,playList.get(currSongNumber).lastIndexOf('/')+1)+"folder.jpg";
			if(new File(goodPath).exists())
			{
				try{
				Globals.currArt=BitmapFactory.decodeFile(goodPath);
				}catch (RuntimeException e){}
			}else{
				// Try albumart.jpg
				goodPath=playList.get(currSongNumber).substring(0,playList.get(currSongNumber).lastIndexOf('/')+1)+"AlbumArt.jpg";
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
			
		
		Intent new_intent = new Intent();
        new_intent.setAction(getResources().getString(R.string.ta_rec));
        new_intent.putExtra(getResources().getString(R.string.ta_cmd), 6);
        sendBroadcast(new_intent);
        
		currTitle=tmpTitle;
		shouldAdvance=true;
		paused=false;
      final int x=JNIHandler.play(playList.get(currSongNumber));
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
    	        new_intent.putExtra(getResources().getString(R.string.ta_filename),playList.get(currSongNumber));
    	        sendBroadcast(new_intent);
    		  }
    	      while(!death&&((Globals.isPlaying==0&&shouldAdvance))){
    	    	  try {Thread.sleep(25);} catch (InterruptedException e){}}
    	      if(shouldAdvance&&!death)
    	      {
    	    	  shouldAdvance=false;
    	    	  new Thread(new Runnable(){
    	        	  public void run()
    	        	  {
    	    	  if(playList.size()>1&&(((currSongNumber+1<playList.size()&&loopMode==0))||loopMode==1)){next();/*stop();*/}
        	      else if(loopMode==2||playList.size()==1){play();}else if(loopMode==0){Intent new_intent = new Intent();
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
			if(!shuffleMode)
			{
		if(++currSongNumber>=playList.size())
		{
			currSongNumber=0;
		}
		}else{
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
		}}
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
			stopForeground(true);
			foreground=false;
			fullStop=false;
			}
		}
	}
	public void updateNotification(String title, boolean paused)
	{
		 remoteViews = new RemoteViews(getPackageName(),  
	                R.layout.music_notification);  
	  	  remoteViews.setTextViewText(R.id.titley, currTitle);
	  	  remoteViews.setImageViewResource(R.id.notPause, (paused)?R.drawable.ic_media_play:R.drawable.ic_media_pause);
	  	  // Previous
	  	  Intent new_intent = new Intent();
	  	  new_intent.setAction(getResources().getString(R.string.msrv_rec));
	  	  new_intent.putExtra(getResources().getString(R.string.msrv_cmd), 4);
	  	  PendingIntent pendingNotificationIntent = PendingIntent.getBroadcast(this, 0, new_intent, 0);
	  	  remoteViews.setOnClickPendingIntent(R.id.notPrev, pendingNotificationIntent);
	  	  // Play/Pause
	  	  new_intent = new Intent();
	  	  new_intent.setAction(getResources().getString(R.string.msrv_rec));
	  	  new_intent.putExtra(getResources().getString(R.string.msrv_cmd), 2);
	  	  pendingNotificationIntent = PendingIntent.getBroadcast(this, 1, new_intent, 0);
	  	  remoteViews.setOnClickPendingIntent(R.id.notPause, pendingNotificationIntent);
	  	  // Next
	  	  new_intent = new Intent();
	  	  new_intent.setAction(getResources().getString(R.string.msrv_rec));
	  	  new_intent.putExtra(getResources().getString(R.string.msrv_cmd), 3);
	  	  pendingNotificationIntent = PendingIntent.getBroadcast(this, 2, new_intent, 0);
	  	  remoteViews.setOnClickPendingIntent(R.id.notNext, pendingNotificationIntent);
	  	  // Stop
	  	  new_intent = new Intent();
	  	  new_intent.setAction(getResources().getString(R.string.msrv_rec));
	  	  new_intent.putExtra(getResources().getString(R.string.msrv_cmd), 5);
	  	  pendingNotificationIntent = PendingIntent.getBroadcast(this, 3, new_intent, 0);
	  	  remoteViews.setOnClickPendingIntent(R.id.notStop, pendingNotificationIntent);
	  	 final Intent emptyIntent = new Intent(this, TimidityActivity.class);
		  PendingIntent pendingIntent = PendingIntent.getActivity(this, -1, emptyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		  NotificationCompat.Builder mBuilder =
				    new NotificationCompat.Builder(this)
				    .setSmallIcon(R.drawable.ic_launcher)
				    .setContentTitle(getResources().getString(R.string.app_name))
				    .setContentText(currTitle)
				    .setContentIntent(pendingIntent)
				    .setContent(remoteViews);
		  n=mBuilder.build();
		  n.flags|=Notification.FLAG_ONLY_ALERT_ONCE|Notification.FLAG_ONGOING_EVENT;
		  
		if(!foreground)
		  {
			  foreground=true;
		  startForeground(13901858, n);
		  TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
	        if(mgr != null) {
	            mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
	        }
		  }else{
			  NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
              mNotificationManager.notify(13901858, n);
		  }
			}
	
	}


