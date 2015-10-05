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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.xperia64.timidityae.R;

@SuppressLint("Recycle")
public class PlayerFragment extends Fragment {
	
	public boolean shouldAdvance=true;
	public int loopMode=1;
	public int shuffleMode=0;
	boolean firstSelection;
	boolean sliding=false;
	//
	public TimidityActivity mActivity;
	//
	Random random = new Random(System.currentTimeMillis());
	// UI Elements
	ImageButton previousButton; // |<
	ImageButton rewindButton; // <<
	ImageButton playButton; // >
	ImageButton fastForwardButton; // >>
	ImageButton nextButton; // >|
	ImageButton shuffleButton;
	ImageButton loopButton;
	ImageButton stopButton;
	SeekBar trackBar;
	TextView songTitle;
	TextView timeCounter;
	FrameLayout subContainer;
	ArtFragment artsy;
	TrackFragment tracky;
	LyricFragment lyrical;
	int totalMinutes;
	int totalSeconds;
	int currMinutes;
	int currSeconds;
	int fragMode=0; // 0 = AlbumArt, 1 = midi controls, 2 = Kareoke
	boolean ffrw=false;
	boolean enabledControls=false;
	boolean canEnablePlay=true;
	boolean updaterPlacid = false;
	//
	AlertDialog ddd;
	TextView tempo;
	TextView pitch;
	TextView voices;
	//
	public static PlayerFragment create() {
		PlayerFragment fragment = new PlayerFragment();
		return fragment;
		}
	@Override
	public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	mActivity=(TimidityActivity) getActivity();
	}
	
	Handler seekHandler = new Handler();
	Runnable seekbarUpdater = new Runnable() {

		@Override
		public void run() {
			seekUpdation();
		}
	};
	Runnable lyricUpdater = new Runnable()
	{
		@Override
		public void run()
		{
			lyricUpdation();
		}
	};
	public void lyricUpdation()
	{
		if(Globals.isPlaying==0)
		{
			if(!JNIHandler.type&&fragMode==2)
			{
				lyrical.updateLyrics();
			}
			seekHandler.postDelayed(lyricUpdater,10);
		}
		
	}
	public void seekUpdation() {
		if(Globals.isPlaying==0&&isAdded())
		{
			if(!JNIHandler.type)
			{
				if(JNIHandler.mAudioTrack!=null)
				{
					if((JNIHandler.exceptional&1)!=0)
					{
						Toast.makeText(getActivity(), "Error initializing AudioTrack. Try decreasing the buffer size.", Toast.LENGTH_LONG).show();
						shouldAdvance=false;
						canEnablePlay=true;
						mActivity.stop();
					}
					try{
					if(!enabledControls&&(JNIHandler.mAudioTrack.getPlaybackHeadPosition()>=500))
					{
						enabledControls=true;
						canEnablePlay=true;
						fastForwardButton.setEnabled(true);
						rewindButton.setEnabled(true);
						trackBar.setEnabled(true);
						playButton.setEnabled(true);
					}
					}catch(Exception e)
					{
						
					}
				}
			}else{
				enabledControls=true;
    			canEnablePlay=true;
				fastForwardButton.setEnabled(true);
				rewindButton.setEnabled(true);
				trackBar.setEnabled(true);
				playButton.setEnabled(true);
			}
			if(fragMode==1)
			{
				tracky.updateList();
			}
			if(ddd!=null&&ddd.isShowing())
			{
				if(tempo!=null)
					tempo.setText(String.format(getResources().getString(R.string.mop_tempo),JNIHandler.ttr, (int) (500000 / (double) JNIHandler.tt * 120 * (double) JNIHandler.ttr / 100 + 0.5)));
				if(pitch!=null)
					pitch.setText(String.format(getResources().getString(R.string.mop_pitch),((JNIHandler.koffset>0)?"+":"")+Integer.toString(JNIHandler.koffset)));
				if(voices!=null)
					voices.setText(String.format(getResources().getString(R.string.mop_voice), JNIHandler.voice, JNIHandler.maxvoice));
			}
			try { //NOPE
    			if(JNIHandler.mMediaPlayer!=null&&JNIHandler.type&&JNIHandler.mMediaPlayer.isPlaying()) // Are these evaluated in order? I hope so
    			{
    				JNIHandler.currTime=JNIHandler.mMediaPlayer.getCurrentPosition();
    			}}catch (Exception e) {}
			if(getActivity()!=null&&!sliding)
			{
				updaterPlacid=false;
				totalMinutes = 0;
    			totalSeconds = JNIHandler.maxTime;
				currMinutes = 0;
				currSeconds = JNIHandler.currTime;
    			if(JNIHandler.type)
    			{
    				totalSeconds /= 1000; // ms to s
    				currSeconds /= 1000;
    			}
    			totalMinutes=totalSeconds/60;
				totalSeconds%=60;
    			currMinutes=currSeconds/60;
				currSeconds%=60;
			getActivity().runOnUiThread(new Runnable(){
	    		@Override
	    		public void run()
	    		{
	    			trackBar.setMax(JNIHandler.maxTime);
	    			trackBar.setProgress(JNIHandler.currTime);
	    			trackBar.invalidate();
	    			
	    			timeCounter.setText(String.format("%d:%02d/%d:%02d",currMinutes,currSeconds,totalMinutes,totalSeconds));
	    			timeCounter.invalidate();
	    		}
	    	});
			seekHandler.postDelayed(seekbarUpdater, 500);
			}else{
				updaterPlacid=true;
			}
		}
	}
	
	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		if(Globals.shouldRestore)
    	{
    		Intent new_intent = new Intent();
    		new_intent.setAction(getActivity().getResources().getString(R.string.msrv_rec));
            new_intent.putExtra(getActivity().getResources().getString(R.string.msrv_cmd), 11);
            getActivity().sendBroadcast(new_intent);
    	}
	}
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
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
        subContainer = (FrameLayout) v.findViewById(R.id.midiContainer);
        return v;    
    }
	public void pauseStop(boolean cmd, final boolean arg)
	{
		if(cmd) // pause
		{
				getActivity().runOnUiThread(new Runnable(){
		    		@Override
		    		public void run()
		    		{
		    			playButton.setImageResource((arg)?R.drawable.ic_media_play:R.drawable.ic_media_pause);
		    		}
		    	});
			
		}else{ // stop
			getActivity().runOnUiThread(new Runnable(){
	    		@Override
	    		public void run()
	    		{
	    			trackBar.setProgress(0);
	    			trackBar.setMax(0);
	    			trackBar.setEnabled(false);
	    			rewindButton.setEnabled(false);
	    			fastForwardButton.setEnabled(false);
	    			if(canEnablePlay)
	    			{
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

        fm.beginTransaction();
		artsy = new ArtFragment();
		ft.replace(R.id.midiContainer, artsy);
        ft.commit();
		previousButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				shouldAdvance=false;
				canEnablePlay=false;
				playButton.setEnabled(false);
				mActivity.prev();
			}
		});
		rewindButton.setEnabled(false);
		rewindButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				ffrw=true;
				int to = trackBar.getProgress()-(JNIHandler.type?3000:3);
				to=(to>trackBar.getMax()?trackBar.getMax():to<0?0:to);
				trackBar.setProgress(to);
			}

		});
		rewindButton.setOnLongClickListener(new OnLongClickListener(){

			private Handler mHandler = new Handler();
			int count;
			int mult;
			@Override
			public boolean onLongClick(View arg0) {
				count=0;
				mult=1;
				ffrw=true;
				mHandler.post(mAction);
				return true;
			}
			Runnable mAction = new Runnable() {
		        @Override public void run() {
		        	sliding=true;
		        	int to = trackBar.getProgress()-(3*mult*(JNIHandler.type?1000:1));
					to=(to>trackBar.getMax()?trackBar.getMax():to<0?0:to);
					trackBar.setProgress(to);
					if(rewindButton.isPressed())
					{	
						if(count++>5)
						{
							count=0;
							mult++;
						}
						mHandler.postDelayed(this, 500);
					}
					else
					{
						sliding=false; 
						if(!JNIHandler.type) 
							mActivity.seek(trackBar.getProgress()); 
						seekUpdation();
					}
		        }
		    };
			
		});
		playButton.setEnabled(false); // >
		playButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if(Globals.isPlaying==0)
				{
					mActivity.pause();
				}else{
					arg0.setEnabled(false);
					mActivity.play();
				}
			}
		});
		fastForwardButton.setEnabled(false);
		fastForwardButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				ffrw=true;
				int to = trackBar.getProgress()+(JNIHandler.type?3000:3);
				to=(to>trackBar.getMax()?trackBar.getMax():to<0?0:to);
				trackBar.setProgress(to);
			}

		});
		fastForwardButton.setOnLongClickListener(new OnLongClickListener(){

			private Handler mHandler = new Handler();
			int count;
			int mult;
			@Override
			public boolean onLongClick(View arg0) {
				count = 0;
				mult = 1;
				ffrw=true;
				mHandler.post(mAction);
				return true;
			}
			Runnable mAction = new Runnable() {
		        @Override public void run() {
		        	sliding=true;
		        	int to = trackBar.getProgress()+(3*mult*(JNIHandler.type?1000:1));
					to=(to>trackBar.getMax()?trackBar.getMax():to<0?0:to);
					trackBar.setProgress(to);
					if(fastForwardButton.isPressed())
					{	
						if(count++>5)
						{
							count=0;
							mult++;
						}
						mHandler.postDelayed(this, 500);
					}
					else
					{
						sliding=false; 
						if(!JNIHandler.type) 
							mActivity.seek(trackBar.getProgress()); 
						seekUpdation();
					}
		        }
		    };
			
		});
		nextButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				shouldAdvance=false;
				canEnablePlay=false;
				playButton.setEnabled(false);
				mActivity.next();
			}
		});
		shuffleButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if(++shuffleMode>2)
					shuffleMode=0;
				int shufRes = R.drawable.ic_menu_forward;
				switch(shuffleMode)
				{
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
		loopButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(final View arg0)
			{
				if(++loopMode>2){loopMode=0;}
				getActivity().runOnUiThread(new Runnable(){
		    		@Override
		    		public void run()
		    		{
		    			switch(loopMode)
		    			{
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
		stopButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				shouldAdvance=false;
				canEnablePlay=true;
				mActivity.stop();
			}
		});
		trackBar.setEnabled(false);
		trackBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
		{

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				if(arg2||ffrw)
				{
					if(arg0.isEnabled()&&(JNIHandler.type||ffrw))
					{
						mActivity.seek(arg1);
					}
					if(!fastForwardButton.isPressed()&&!rewindButton.isPressed())
						ffrw=false;
					totalMinutes = 0;
	    			totalSeconds = arg0.getMax();
					currMinutes = 0;
					currSeconds = arg1;
	    			if(JNIHandler.type)
	    			{
	    				totalSeconds /= 1000; // ms to s
	    				currSeconds /= 1000;
	    			}
	    			totalMinutes=totalSeconds/60;
					totalSeconds%=60;
	    			currMinutes=currSeconds/60;
					currSeconds%=60;
					getActivity().runOnUiThread(new Runnable(){
			    		@Override
			    		public void run()
			    		{
			    			
			    			timeCounter.setText(String.format("%d:%02d/%d:%02d",currMinutes,currSeconds,totalMinutes,totalSeconds));
			    			timeCounter.invalidate();
			    		}
			    	});
				}
			}
			@Override public void onStartTrackingTouch(SeekBar arg0) {sliding=true; }
			@Override public void onStopTrackingTouch(SeekBar arg0) {
				
				sliding=false; 
			if(!JNIHandler.type) 
				mActivity.seek(arg0.getProgress()); 
			if(updaterPlacid) seekUpdation();}
			
		});
		
		trackBar.setIndeterminate(false);
		((TimidityActivity)getActivity()).readyForInit();
	}
	
	public void play(final int seekBarTime, final String title, final int shuffleMode, final int loopMode)
	{
		enabledControls=false;
		canEnablePlay=false;
		playButton.setEnabled(false);
		if(tracky!=null)
			tracky.reset();
		
		if(ddd!=null)
		{
			if(ddd.isShowing())
			{	
				ddd.dismiss();
				ddd=null;
			}
		}
		this.shuffleMode=shuffleMode;
		this.loopMode=loopMode;
		if(getActivity()!=null)
		{
		getActivity().runOnUiThread(new Runnable(){
    		@Override
    		public void run()
    		{
    			playButton.setImageResource(R.drawable.ic_media_pause);
    			trackBar.setMax(seekBarTime);
      			trackBar.setProgress(0);
      			songTitle.setText(title);
	    	      seekUpdation();
	    	      if(!JNIHandler.type)lyricUpdation();
	    	      switch(loopMode)
	    			{
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
					switch(shuffleMode)
					{
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
		if(JNIHandler.type&&fragMode!=0)
		{
			FragmentManager fm = getFragmentManager();
	        FragmentTransaction ft = fm.beginTransaction();
	        fragMode=0;
	        fm.beginTransaction();
			artsy = new ArtFragment();
			ft.replace(R.id.midiContainer, artsy);
            ft.commitAllowingStateLoss();
		}
		}
	}
	public void play(final int seekBarTime, final String title)
	{
		enabledControls=false;
		canEnablePlay=false;
		playButton.setEnabled(false);
		if(tracky!=null)
			tracky.reset();
		
		if(ddd!=null)
		{
			if(ddd.isShowing())
			{
				ddd.dismiss();
				ddd=null;
			}
		}
		if(getActivity()!=null)
		{
		getActivity().runOnUiThread(new Runnable(){
    		@Override
    		public void run()
    		{
    			playButton.setImageResource(R.drawable.ic_media_pause);
    			trackBar.setProgress(0);
    			trackBar.setMax(seekBarTime);
      			songTitle.setText(title);
	    	    seekUpdation();
	    	    if(!JNIHandler.type)lyricUpdation();
    		}
    	});
		}
		if(JNIHandler.type&&fragMode!=0)
		{
			FragmentManager fm = getFragmentManager();
	        FragmentTransaction ft = fm.beginTransaction();
	        fragMode=0;
	        fm.beginTransaction();
			artsy = new ArtFragment();
			ft.replace(R.id.midiContainer, artsy);
            ft.commitAllowingStateLoss();
		}
	}
	public void incrementInterface() {
		FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        fm.beginTransaction();
		if((!JNIHandler.type)&&Globals.isPlaying==0)
		{
			if(++fragMode>2)
				fragMode=0;
		}else{
			fragMode=0;
		}
		switch(fragMode)
		{
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
        fragMode=which;
        fm.beginTransaction();
		if((!JNIHandler.type)&&Globals.isPlaying==0)
		{
			if(fragMode>2)
				fragMode=0;
		}else{
			fragMode=0;
		}
		switch(fragMode)
		{
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
	/*public void updateMidiDialog(boolean which[], int t, int tr, int voices)
	{
		
	}*/
	@SuppressLint("InflateParams")
	public void showMidiDialog() {
		AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
		View v = getActivity().getLayoutInflater().inflate(R.layout.midi_options, null);
		Button speedUp = (Button) v.findViewById(R.id.speedUp);
		Button slowDown = (Button) v.findViewById(R.id.slowDown);
		Button keyUp = (Button) v.findViewById(R.id.keyUp);
		Button keyDown = (Button) v.findViewById(R.id.keyDown);
		Button vplus = (Button) v.findViewById(R.id.vplus);
		Button vminus = (Button) v.findViewById(R.id.vminus);
		Button export = (Button) v.findViewById(R.id.exportButton);
		Button saveCfg = (Button) v.findViewById(R.id.saveCfg);
		Button loadCfg = (Button) v.findViewById(R.id.loadCfg);
		Button savedefCfg = (Button) v.findViewById(R.id.savedefCfg);
		final Button deldefCfg = (Button) v.findViewById(R.id.deldefCfg);
		deldefCfg.setEnabled(new File( mActivity.currSongName+".def.tcf").exists()||new File( mActivity.currSongName+".def.tzf").exists());
		tempo = (TextView) v.findViewById(R.id.tempoText);
		pitch = (TextView) v.findViewById(R.id.pitchText);
		voices = (TextView) v.findViewById(R.id.voiceText);
		
		tempo.setText(String.format(getResources().getString(R.string.mop_tempo),JNIHandler.ttr, (int) (500000 / (double) JNIHandler.tt * 120 * (double) JNIHandler.ttr / 100 + 0.5)));
		pitch.setText(String.format(getResources().getString(R.string.mop_pitch),((JNIHandler.koffset>0)?"+":"")+Integer.toString(JNIHandler.koffset)));
		voices.setText(String.format(getResources().getString(R.string.mop_voice), JNIHandler.voice, JNIHandler.maxvoice));
		speedUp.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				JNIHandler.controlTimidity(17,1);
				JNIHandler.waitUntilReady();
				JNIHandler.tb++;
			}
			
		});
		slowDown.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				JNIHandler.controlTimidity(18,1);
				JNIHandler.waitUntilReady();
				JNIHandler.tb--;
			}
			
		});
		keyUp.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				JNIHandler.controlTimidity(15,1);
				JNIHandler.waitUntilReady();
			}
			
		});
		keyDown.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				JNIHandler.controlTimidity(16,-1);
				JNIHandler.waitUntilReady();
			}
			
		});
		vplus.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				JNIHandler.controlTimidity(19,5);
				JNIHandler.waitUntilReady();
			}
			
		});
		vminus.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				JNIHandler.controlTimidity(20,5);
				JNIHandler.waitUntilReady();
			}
			
		});
		export.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				mActivity.dynExport();
			}
			
		});
		saveCfg.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				mActivity.saveCfg();
			}
			
		});
		loadCfg.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				mActivity.loadCfg();
			}
			
		});
		savedefCfg.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				
				String value1;
				String value2;
				boolean alreadyExists = (new File(mActivity.currSongName+".def.tcf").exists()||new File(mActivity.currSongName+".def.tzf").exists());
				boolean aWrite=true;
				  String needRename1 = null;
				  String needRename2 = null;
				  String probablyTheRoot = "";
				  String probablyTheDirectory = "";
				  try{
				        if(Globals.compressCfg)
				        	new FileOutputStream(mActivity.currSongName+".def.tzf",true).close();
				        else
				        	new FileOutputStream(mActivity.currSongName+".def.tcf",true).close();
				  }catch(FileNotFoundException e)
				  {
					aWrite=false;  
				  } catch (IOException e)
				{
					e.printStackTrace();
				}
				  final boolean canWrite = aWrite;
				  if(!alreadyExists&&canWrite)
				  {
					  new File(mActivity.currSongName+".def.tcf").delete();
					  new File(mActivity.currSongName+".def.tzf").delete();
				  }
				  
				  if(canWrite&&new File(mActivity.currSongName).canWrite())
				  {
					  value1=mActivity.currSongName+(Globals.compressCfg?".def.tzf":".def.tcf");
					  value2=mActivity.currSongName+(Globals.compressCfg?".def.tcf":".def.tzf");
				  }else if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP&& Globals.theFold!=null)
				  {
					  //TODO
					  // Write the file to getExternalFilesDir, then move it with the Uri
					  // We need to tell JNIHandler that movement is needed.
					  // This is pretty much done now?
					  String[] tmp = Globals.getDocFilePaths(getActivity(),mActivity.currSongName);
					  probablyTheDirectory = tmp[0];
					  probablyTheRoot = tmp[1];
					if(probablyTheDirectory.length()>1)
					{
						needRename1 = (mActivity.currSongName).substring(mActivity.currSongName.indexOf(probablyTheRoot)+probablyTheRoot.length())+(Globals.compressCfg?".def.tzf":".def.tcf");
						needRename2 = (mActivity.currSongName).substring(mActivity.currSongName.indexOf(probablyTheRoot)+probablyTheRoot.length())+(Globals.compressCfg?".def.tcf":".def.tzf");
						value1 = probablyTheDirectory+mActivity.currSongName.substring(mActivity.currSongName.lastIndexOf('/'))+(Globals.compressCfg?".def.tzf":".def.tcf");
						value2 = probablyTheDirectory+mActivity.currSongName.substring(mActivity.currSongName.lastIndexOf('/'))+(Globals.compressCfg?".def.tcf":".def.tzf");
					}else{
						Toast.makeText(getActivity(), "Could not write config file. Did you give Timidity write access to the root of your external sd card?" , Toast.LENGTH_SHORT).show();
						return;
					}
				  }else{
					  if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP)
					  {
						  Toast.makeText(getActivity(), "Could not write config file. Did you give Timidity write access to the root of your external sd card?" , Toast.LENGTH_SHORT).show();
					  }else{
						  Toast.makeText(getActivity(), "Could not write config file. Permission denied." , Toast.LENGTH_SHORT).show();
					  }
					  return;
				  }
				  final String finalval1 = value1;
				  final String finalval2 = value2;
				  final String needToRename1 = needRename1;
				  final String needToRename2 = needRename2;
				  final String probRoot = probablyTheRoot;
				if(alreadyExists)
				{
					AlertDialog dialog = new AlertDialog.Builder(mActivity).create();
				    dialog.setTitle("Warning");
				    dialog.setMessage("Overwrite default config file?");
				    dialog.setCancelable(false);
				    dialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int buttonId) {
				        	if(!canWrite&&Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP)
				        	{
				        		if(needToRename1!=null)
				        		{
				        			Globals.tryToDeleteFile(getActivity(), probRoot+needToRename1);
				        			Globals.tryToDeleteFile(getActivity(), finalval1);
				        			Globals.tryToDeleteFile(getActivity(), probRoot+needToRename2);
				        			Globals.tryToDeleteFile(getActivity(), finalval2);
				        		}else{
				        			Globals.tryToDeleteFile(getActivity(), finalval1);
				        			Globals.tryToDeleteFile(getActivity(), finalval2);
				        		}
				        	}else{
				        		new File(mActivity.currSongName+".def.tcf").delete();
								  new File(mActivity.currSongName+".def.tzf").delete();
				        	}
				        	mActivity.localfinished=false;
				        	mActivity.saveCfgPart2(finalval1, needToRename1);
				        	deldefCfg.setEnabled(true);
				        	/*Intent new_intent = new Intent();
					    	new_intent.setAction(getResources().getString(R.string.msrv_rec));
					    	new_intent.putExtra(getResources().getString(R.string.msrv_cmd), 16);
					    	new_intent.putExtra(getResources().getString(R.string.msrv_outfile), mActivity.currSongName+".def.tcf");
					    	getActivity().sendBroadcast(new_intent);
					    	deldefCfg.setEnabled(true);*/
				        }
				    });
				    dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(android.R.string.no), new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int buttonId) {
				          
				          
				        }
				    });
				    dialog.show();
					
				}else{
					/*Intent new_intent = new Intent();
			    	new_intent.setAction(getResources().getString(R.string.msrv_rec));
			    	new_intent.putExtra(getResources().getString(R.string.msrv_cmd), 16);
			    	new_intent.putExtra(getResources().getString(R.string.msrv_outfile), mActivity.currSongName+".def.tcf");
			    	getActivity().sendBroadcast(new_intent);
			    	deldefCfg.setEnabled(true);*/
					mActivity.localfinished=false;
					mActivity.saveCfgPart2(finalval1, needToRename1);
					deldefCfg.setEnabled(true);
				}
			}
			
			
		});
		
		deldefCfg.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				
				if(new File( mActivity.currSongName+".def.tcf").exists() || new File(mActivity.currSongName+".def.tzf").exists())
				{
					boolean aWrite=true;
					  try{
					        if(Globals.compressCfg)
					        	new FileOutputStream(mActivity.currSongName+".def.tzf",true).close();
					        else
					        	new FileOutputStream(mActivity.currSongName+".def.tcf",true).close();
					  }catch(FileNotFoundException e)
					  {
						aWrite=false;  
					  } catch (IOException e)
					{
						e.printStackTrace();
					}
					  final boolean canWrite = aWrite;
					AlertDialog dialog = new AlertDialog.Builder(mActivity).create();
				    dialog.setTitle("Warning");
				    dialog.setMessage("Really delete default config file?");
				    dialog.setCancelable(false);
				    dialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int buttonId) {
				        	
				        	if(!canWrite&&Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP)
				        	{
				        		Globals.tryToDeleteFile(getActivity(), mActivity.currSongName+".def.tzf");
				        		Globals.tryToDeleteFile(getActivity(), mActivity.currSongName+".def.tcf");
				        	}else{
				        		new File(mActivity.currSongName+".def.tcf").delete();
								new File(mActivity.currSongName+".def.tzf").delete();
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
		
		final Spinner x = (Spinner) v.findViewById(R.id.resampSpinner);
		List<String> arrayAdapter = new ArrayList<String>();
		for(String yyy : Globals.sampls)
			arrayAdapter.add(yyy);
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_spinner_item, arrayAdapter);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		x.setAdapter(dataAdapter);
		firstSelection=true;
		x.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int pos, long id) {
				if(firstSelection)
					firstSelection=false;
				else{
				JNIHandler.setResampleTimidity(JNIHandler.currsamp=pos);
				JNIHandler.seekTo(JNIHandler.currTime);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}
			
		});
		x.setSelection(JNIHandler.currsamp);
		if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			v.setBackgroundColor(Globals.theme==1?Color.WHITE:Color.BLACK);
		
		b.setView(v);
		b.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{

			@Override
			public void onClick(DialogInterface dialog, int which) {
				
			}
		});
		b.setTitle(getActivity().getResources().getString(R.string.mop));
		ddd=b.create();
		ddd.show();
	
	}
	public void setArt()
	{
		if(fragMode==0&&artsy!=null&&getActivity()!=null)
		{
			artsy.setArt(Globals.currArt, getActivity());
		}
	}
	 

}
