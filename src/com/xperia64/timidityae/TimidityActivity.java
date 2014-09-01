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
import java.util.Locale;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
//import com.actionbarsherlock.app.SherlockFragment;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.xperia64.timidityae.R;

public class TimidityActivity extends SherlockFragmentActivity implements FileBrowserFragment.ActionFileBackListener, PlaylistFragment.ActionPlaylistBackListener {
	//public static TimidityActivity staticthis;
	private MenuItem menuButton;
	private MenuItem menuButton2;
	private int mode=0;
	private FileBrowserFragment fileFrag;
	private PlayerFragment playFrag;
	private PlaylistFragment plistFrag;
	ViewPager viewPager;
	boolean needFileBack=false;
	boolean needPlaylistBack=false;
	boolean fromPlaylist=false;
	boolean needService=true;
	String currSongName;
	boolean needInit=false;
	int oldTheme;
	private BroadcastReceiver activityReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
        	int cmd = intent.getIntExtra(getResources().getString(R.string.ta_cmd), -5); // -V
        	switch(cmd)
        	{
        	case -5:
        		break;
        	case 0:
        		currSongName=intent.getStringExtra(getResources().getString(R.string.ta_filename));
        		if(viewPager.getCurrentItem()==1)
        		{
        			menuButton.setVisible(!JNIHandler.type);
        			menuButton.setEnabled(!JNIHandler.type);
        			menuButton2.setVisible(!JNIHandler.type);
        			menuButton2.setEnabled(!JNIHandler.type);
        		}
        		playFrag.play(intent.getIntExtra(getResources().getString(R.string.ta_startt),0), intent.getStringExtra(getResources().getString(R.string.ta_songttl)));
        		break;
        	case 1:
        		break;
        	case 2: 
        		fileFrag.getDir(intent.getStringExtra(getResources().getString(R.string.ta_currpath)));
        		//System.out.println(integExtrnt.getStrina(getResources().getString(R.string.ta_currpath)));
        		break;
        	case 3:
        		//System.out.println("case 3");
        		intent.getStringExtra(getResources().getString(R.string.ta_filename));
        		if(viewPager.getCurrentItem()==1)
        		{
        			menuButton.setVisible(!JNIHandler.type);
        			menuButton.setEnabled(!JNIHandler.type);
        			menuButton2.setVisible(!JNIHandler.type);
        			menuButton2.setEnabled(!JNIHandler.type);
        		}
        		playFrag.play(intent.getIntExtra(getResources().getString(R.string.ta_startt),0), 
        				currSongName=intent.getStringExtra(getResources().getString(R.string.ta_songttl)), 
        				intent.getBooleanExtra(getResources().getString(R.string.ta_shufmode), false), 
        				intent.getIntExtra(getResources().getString(R.string.ta_loopmode),1));
        		break;
        	case 4:
        		plistFrag.currPlist=Globals.plist;
        		Globals.plist=null;
        		break;
        	case 5: // Notifiy pause/stop
        		playFrag.pauseStop(intent.getBooleanExtra(getResources().getString(R.string.ta_pause), false), 
        				intent.getBooleanExtra(getResources().getString(R.string.ta_pausea),false));
        		break;
        	case 6: // notify art
        		//currSongName = intent.getStringExtra(getResources().getString(R.string.ta_filename));
        		playFrag.setArt();
        		break;

        		
        	}
        }
    };
    
   /* @Override
    protected void onPause()
    {
    	
    }
    
    @Override
    protected void onResume()
    {
    	
    }*/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if(savedInstanceState==null)
		{
			Globals.reloadSettings(this, getAssets());
		}else{
			if(!savedInstanceState.getBoolean("justtheme", false))
			{
				Globals.reloadSettings(this, getAssets());
			}
		}
		try {
    		System.loadLibrary("timidityhelper");   
    	}
        catch( UnsatisfiedLinkError e) {
        	Log.e("Bad:","Cannot grab timidity");
        	Globals.nativeMidi = Globals.onlyNative=true;
        }
		oldTheme = Globals.theme;
		if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH){
	        this.setTheme((Globals.theme==1)?com.actionbarsherlock.R.style.Theme_Sherlock_Light_DarkActionBar:com.actionbarsherlock.R.style.Theme_Sherlock);
	   }else{
		   this.setTheme((Globals.theme==1)?android.R.style.Theme_Holo_Light_DarkActionBar:android.R.style.Theme_Holo);
	   }
	super.onCreate(savedInstanceState);
	if(savedInstanceState==null)
	{
		needInit=Globals.initialize(getAssets(), this);
	}else{
		if(!savedInstanceState.getBoolean("justtheme", false))
		{
			Globals.reloadSettings(this, getAssets());
		}
		needService=savedInstanceState.getBoolean("needservice", true);
	}
	/*IntentFilter filter = new IntentFilter();
	filter.addAction("com.xperia64.timidityae20.ACTION_STOP");
	filter.addAction("com.xperia64.timidityae20.ACTION_PAUSE");
	filter.addAction("com.xperia64.timidityae20.ACTION_NEXT");
	filter.addAction("com.xperia64.timidityae20.ACTION_PREV");*/
	//registerReceiver(receiver, filter);

	setContentView(R.layout.main);
	
	
	if (activityReceiver != null) {
		//Create an intent filter to listen to the broadcast sent with the action "ACTION_STRING_ACTIVITY"
		            IntentFilter intentFilter = new IntentFilter(getResources().getString(R.string.ta_rec));
		//Map the intent filter to the receiver
		            registerReceiver(activityReceiver, intentFilter);
		        }

		//Start the service on launching the application
		        if(needService)
		        {
		        	needService=false;
		        	startService(new Intent(this, MusicService.class));
		        }
		        
	viewPager = (ViewPager) findViewById(R.id.vp_main);
	viewPager.setAdapter(new TimidityFragmentPagerAdapter());
	viewPager.setOnPageChangeListener(new OnPageChangeListener() {

        @Override
        public void onPageSelected(int index) {
        	mode=index;
        	switch(index)
        	{
        	case 0:
        		fromPlaylist=false;
        		if(getSupportActionBar()!=null)
				{
        			if(menuButton!=null)
        			{
        				menuButton.setIcon(R.drawable.ic_menu_refresh);
        				menuButton.setVisible(true);
        				menuButton.setEnabled(true);
        				menuButton.setTitle(getResources().getString(R.string.refreshfld));
        				menuButton.setTitleCondensed(getResources().getString(R.string.refreshcon));
        			}
        			if(menuButton2!=null)
        			{
        				menuButton2.setIcon(R.drawable.ic_menu_home);
        				menuButton2.setTitle(getResources().getString(R.string.homefld));
        				menuButton2.setTitleCondensed(getResources().getString(R.string.homecon));
        				menuButton2.setVisible(true);
        				menuButton2.setEnabled(true);
        			}
					getSupportActionBar().setDisplayHomeAsUpEnabled(needFileBack);
				}else{
					getSupportActionBar().setDisplayHomeAsUpEnabled(false);
					getSupportActionBar().setHomeButtonEnabled(false);
				}
        		if(fileFrag!=null)
        			if(fileFrag.getListView()!=null)
        				fileFrag.getListView().setFastScrollEnabled(true);
        		break;
        	case 1:
        		if(getSupportActionBar()!=null)
				{
        			if(menuButton!=null)
        			{
        				menuButton.setIcon(R.drawable.ic_menu_agenda);
        				menuButton.setTitle(getResources().getString(R.string.view));
        				menuButton.setTitleCondensed(getResources().getString(R.string.viewcon));
        				menuButton.setVisible(!JNIHandler.type);
            			menuButton.setEnabled(!JNIHandler.type);
        			}
        			if(menuButton2!=null)
        			{
        				menuButton2.setIcon(R.drawable.ic_menu_info_details);
        				menuButton2.setTitle(getResources().getString(R.string.playback));
        				menuButton2.setTitleCondensed(getResources().getString(R.string.playbackcon));
        				menuButton2.setVisible(!JNIHandler.type);
            			menuButton2.setEnabled(!JNIHandler.type);
        			}
        			getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        			getSupportActionBar().setHomeButtonEnabled(false);
				}
        		break;
        	case 2:
        		fromPlaylist=true;
        		if(getSupportActionBar()!=null)
				{
        			if(menuButton!=null)
        			{
        			menuButton.setIcon(R.drawable.ic_menu_refresh);
        			menuButton.setTitle(getResources().getString(R.string.refreshpls));
        			menuButton.setTitleCondensed(getResources().getString(R.string.refreshcon));
        			menuButton.setVisible(true);
            		menuButton.setEnabled(true);
        			}
        			if(menuButton2!=null)
        			{
        				menuButton2.setIcon(R.drawable.ic_menu_add);
        				menuButton2.setTitle(getResources().getString(R.string.add));
        				menuButton2.setTitleCondensed(getResources().getString(R.string.addcon));
        				if(plistFrag!=null)
        				{
        					menuButton2.setVisible((plistFrag.plistName!=null&&plistFrag.mode)?!plistFrag.plistName.equals("CURRENT"):true);
        					menuButton2.setEnabled((plistFrag.plistName!=null&&plistFrag.mode)?!plistFrag.plistName.equals("CURRENT"):true);
        				}
        			}
            		if(plistFrag!=null)
            			if(plistFrag.getListView()!=null)
            				plistFrag.getListView().setFastScrollEnabled(true);
				if(needPlaylistBack)
				{
					getSupportActionBar().setDisplayHomeAsUpEnabled(true);
				}
				}else{
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
	}
	public void initCallback()
	{
		int x = JNIHandler.init(Globals.dataFolder+"/timidity/","timidity.cfg", Globals.mono, Globals.defSamp, getApplicationContext(), Globals.sixteen, Globals.buff, Globals.aRate, false);
		if(x!=0&&x!=-99)
		{
			Globals.nativeMidi=true;
			Toast.makeText(this, String.format(getResources().getString(R.string.tcfg_error), x), Toast.LENGTH_LONG).show();
		}
		if(getIntent().getData()!=null)
		{
			String data;
			if((data=getIntent().getData().getPath())!=null&&getIntent().getData().getScheme()!=null)
			{
				//System.out.println("Data is: "+data+" "+getIntent().getData().getScheme());
				if(getIntent().getData().getScheme().equals("file"))
				{
					if(new File(data).exists())
					{
						File f = new File(data.substring(0,data.lastIndexOf('/')+1));
						if(f.exists())
						{
							if(f.isDirectory())
							{
								ArrayList<String> files = new ArrayList<String>();
								int position=-1;
								int goodCounter=0;
								for(File ff: f.listFiles())
								{
									if(ff!=null)
									{
										if(ff.isFile())
										{
											int dotPosition = ff.getName().lastIndexOf('.');
											String extension="";
											if (dotPosition != -1) {
												extension = (ff.getName().substring(dotPosition)).toLowerCase(Locale.US);
													if(extension!=null){
														if((Globals.showVideos?Globals.musicVideoFiles:Globals.musicFiles).contains("*"+extension+"*"))
														{
															
															files.add(ff.getPath());
															if(ff.getPath().equals(data))
																position=goodCounter;
															goodCounter++;
														}
													}
											}
										}
									}
								}
								if(position==-1)
									Toast.makeText(this, getResources().getString(R.string.intErr1), Toast.LENGTH_SHORT).show();
								else{
								selectedSong(files,position,true,false);
								fileFrag.getDir(data.substring(0,data.lastIndexOf('/')+1));
								}
							}
						}
					}else{
						Toast.makeText(this, getResources().getString(R.string.srv_fnf),Toast.LENGTH_SHORT).show();
					}
				}else{
					Toast.makeText(this, getResources().getString(R.string.intErr2),Toast.LENGTH_SHORT).show();
				}
			}
		}
	}
	@Override
	public void onDestroy()
	{
		unregisterReceiver(activityReceiver);
		super.onDestroy();
	}
	@Override
	protected void onSaveInstanceState(Bundle icicle) {
		  super.onSaveInstanceState(icicle);
		  icicle.putBoolean("justtheme", true);
		  icicle.putBoolean("needservice", false);
		}
	@Override
	  public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getSupportMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
	    menuButton=menu.findItem(R.id.add);
	    menuButton2=menu.findItem(R.id.subtract);
	    return true;
	  } 
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item)
	{
		if(item.getItemId()==R.id.add)
		{
			//System.out.println("add");
			switch(mode)
			{
			case 0:
				//System.out.println("Hello90145");
				fileFrag.getDir(fileFrag.currPath);
				break;
			case 1:
				if(playFrag!=null&&!JNIHandler.type)
				{
					playFrag.incrementInterface();
				}/*else{
					Toast.makeText(this, "Non midi file", Toast.LENGTH_SHORT).show();
				}*/
				break;
			case 2:
				plistFrag.getPlaylists(plistFrag.mode?plistFrag.plistName:null);
				break;
			}
		}else if(item.getItemId()==R.id.subtract){
			//System.out.println("subtract");
			switch(mode)
			{
			case 0:
				//System.out.println("Hello90145");
				fileFrag.getDir(Globals.defaultFolder);
				break;
			case 1:
				if(playFrag!=null)
				{
					if(!JNIHandler.type)
					{
					playFrag.showMidiDialog();
					}/*else{
						Toast.makeText(this, "Non midi file", Toast.LENGTH_SHORT).show();
					}*/
				}
				break;
			case 2:
				plistFrag.add();
				break;
				
			}
		}else if(item.getItemId()==android.R.id.home)
		{
			//System.out.println("home");
			 onBackPressed();
		}else if(item.getItemId()==R.id.quit)
		{
			//System.out.println("quit");
			stopService(new Intent(this, MusicService.class));
			unregisterReceiver(activityReceiver);
			this.finish();
			System.exit(0);
		}else if(item.getItemId()==R.id.asettings)
		{
			Intent mainact = new Intent(this, SettingsActivity.class);
            mainact.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivityForResult( mainact, 1 );
		}else if(item.getItemId()==R.id.ahelp)
		{
			new AlertDialog.Builder(this).setTitle(getResources().getString(R.string.helpt))
			.setMessage(getResources().getString(R.string.helper)).setNegativeButton("OK", new OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					
				}
				
			}).show();
		}
		return true;
	}
	@Override
	public void onBackPressed()
	{
		switch(mode)
		{
		case 0:
			if(fileFrag!=null)
				if(fileFrag.currPath!=null)
					if(!fileFrag.currPath.matches("[/]+"))
					{
						fileFrag.getDir(new File(fileFrag.currPath).getParent().toString());
					}else
						viewPager.setCurrentItem(1);
			break;
		case 1:
			viewPager.setCurrentItem((fromPlaylist)?2:0);
			break;
		case 2:
			if(plistFrag.mode)
				plistFrag.getPlaylists(null);
			else
				viewPager.setCurrentItem(1);
			break;
		}
	}
	public void selectedSong(ArrayList<String> files, int songNumber, boolean begin, boolean loc)
	{
		fromPlaylist=loc;
		if(viewPager!=null)
			viewPager.setCurrentItem(1);
		Globals.plist=files;
		if(plistFrag!=null)
			plistFrag.currPlist=files;
		Intent new_intent = new Intent();
	    new_intent.setAction(getResources().getString(R.string.msrv_rec));
	    new_intent.putExtra(getResources().getString(R.string.msrv_cmd), 5);
	    sendBroadcast(new_intent);
		new_intent = new Intent();
	    new_intent.setAction(getResources().getString(R.string.msrv_rec));
	    new_intent.putExtra(getResources().getString(R.string.msrv_cmd), 0);
	   
	    if(fileFrag!=null)
	    	new_intent.putExtra(getResources().getString(R.string.msrv_currfold),fileFrag.currPath);
	    new_intent.putExtra(getResources().getString(R.string.msrv_songnum), songNumber);
	    new_intent.putExtra(getResources().getString(R.string.msrv_begin), begin);
	    sendBroadcast(new_intent);
	    //System.out.println("sent bradcast");
	}
	public class TimidityFragmentPagerAdapter extends FragmentPagerAdapter {
	final String[] pages = {"Files", "Player", "Playlists"};
	public TimidityFragmentPagerAdapter() {
	super(getSupportFragmentManager());
	}
	@Override
	public int getCount() {
	return pages.length;
	}
	@Override
	public Fragment getItem(int position) {
		switch(position)
		{
		case 0:
			fileFrag = FileBrowserFragment.create(Globals.defaultFolder);
			return fileFrag;
		case 1:
			playFrag = PlayerFragment.create();
			return playFrag;
		case 2:
			//System.out.println("creationist");
			plistFrag = PlaylistFragment.create(Globals.dataFolder+"/playlists/");
			return plistFrag;
		default:
			return null;//PageFragment.create(position + 1);
		}
	
	}
	@Override
	public CharSequence getPageTitle(int position) {
		return pages[position];
		}
	}
	/*public static class PageFragment extends SherlockFragment {
	public static final String ARG_PAGE = "ARG_PAGE";
	//private int mPage;
	public static PageFragment create(int page) {
	Bundle args = new Bundle();
	args.putInt(ARG_PAGE, page);
	PageFragment fragment = new PageFragment();
	fragment.setArguments(args);
	return fragment;
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	//mPage = getArguments().getInt(ARG_PAGE);
	}
	/*@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	Bundle savedInstanceState) {
		View view = null;
		switch(mPage)
		{
		case 1: // Files
			//view = inflater.inflate(R.layout.filebrowser, container, false);
			break;
		case 2: //
			break;
		case 3: // Playlists
			break;
		default:
			view = inflater.inflate(R.layout.fragment_page, container, false);
			TextView textView = (TextView) view;
			textView.setText("Fragment #" + mPage);
			break;
		}
	
	return view;
	}*//*
	
	}*/
	@Override
	public void needFileBackCallback(boolean yes) {
		needFileBack=yes;
		if(getSupportActionBar()!=null)
		{
			if(viewPager.getCurrentItem()==0)
			{
				if(needFileBack)
				{
					getSupportActionBar().setDisplayHomeAsUpEnabled(true);
				}else{
					getSupportActionBar().setDisplayHomeAsUpEnabled(false);
					getSupportActionBar().setHomeButtonEnabled(false);
				}
			}
		}
	}
	@Override
	public void needPlaylistBackCallback(boolean yes, boolean current) {
		needPlaylistBack=yes;
		if(getSupportActionBar()!=null)
		{
			if(viewPager.getCurrentItem()==2)
			{
				if(needPlaylistBack)
				{
					getSupportActionBar().setDisplayHomeAsUpEnabled(true);
					menuButton2.setVisible(!current);
					menuButton2.setEnabled(!current);
				}else{
					menuButton2.setVisible(true);
					menuButton2.setEnabled(true);
					getSupportActionBar().setDisplayHomeAsUpEnabled(false);
					getSupportActionBar().setHomeButtonEnabled(false);
				}
			}
		}
	}
	/*@Override
	public ArrayList<String> getCurrentPlaylist() {
		return Globals.plist;
	}*/
	// Broadcast actions
	// This is painful.
	public void play()
	{
		Intent new_intent = new Intent();
	    new_intent.setAction(getResources().getString(R.string.msrv_rec));
	    new_intent.putExtra(getResources().getString(R.string.msrv_cmd), 1);
	    sendBroadcast(new_intent);
	}
	public void pause()
	{
		Intent new_intent = new Intent();
	    new_intent.setAction(getResources().getString(R.string.msrv_rec));
	    new_intent.putExtra(getResources().getString(R.string.msrv_cmd), 2);
	    sendBroadcast(new_intent);
	}
	public void next()
	{
		Intent new_intent = new Intent();
	    new_intent.setAction(getResources().getString(R.string.msrv_rec));
	    new_intent.putExtra(getResources().getString(R.string.msrv_cmd), 3);
	    sendBroadcast(new_intent);
	}
	public void prev()
	{
		Intent new_intent = new Intent();
	    new_intent.setAction(getResources().getString(R.string.msrv_rec));
	    new_intent.putExtra(getResources().getString(R.string.msrv_cmd), 4);
	    sendBroadcast(new_intent);
	}
	public void stop()
	{
		Intent new_intent = new Intent();
	    new_intent.setAction(getResources().getString(R.string.msrv_rec));
	    new_intent.putExtra(getResources().getString(R.string.msrv_cmd), 5);
	    sendBroadcast(new_intent);
	}
	public void loop(int mode)
	{
		Intent new_intent = new Intent();
	    new_intent.setAction(getResources().getString(R.string.msrv_rec));
	    new_intent.putExtra(getResources().getString(R.string.msrv_cmd), 6);
	    new_intent.putExtra(getResources().getString(R.string.msrv_loopmode), mode);
	    sendBroadcast(new_intent);
	}
	public void shuffle(boolean mode)
	{
		Intent new_intent = new Intent();
	    new_intent.setAction(getResources().getString(R.string.msrv_rec));
	    new_intent.putExtra(getResources().getString(R.string.msrv_cmd), 7);
	    new_intent.putExtra(getResources().getString(R.string.msrv_shufmode), mode);
	    sendBroadcast(new_intent);
	}
	public void seek(int time)
	{
		Intent new_intent = new Intent();
	    new_intent.setAction(getResources().getString(R.string.msrv_rec));
	    new_intent.putExtra(getResources().getString(R.string.msrv_cmd), 9);
	    new_intent.putExtra(getResources().getString(R.string.msrv_seektime), time);
	    sendBroadcast(new_intent);
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    // Check which request we're responding to
	    if (requestCode == 1) {
	        if(oldTheme!=Globals.theme)
	        {
	        	Intent intent = getIntent();
	        	intent.putExtra("justtheme", true);
	        	intent.putExtra("needservice", false);
	            finish();
	            startActivity(intent);	
	        }
	       
	    }
	}
	public void readyForInit() {
		if(needInit)
			initCallback();
	}
}
