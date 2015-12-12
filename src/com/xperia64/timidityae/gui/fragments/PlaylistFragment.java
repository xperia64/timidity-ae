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
package com.xperia64.timidityae.gui.fragments;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import com.xperia64.timidityae.gui.dialogs.FileBrowserDialog;
import com.xperia64.timidityae.gui.dialogs.FileBrowserDialog.FileBrowserDialogListener;
import com.xperia64.timidityae.util.FileComparator;
import com.xperia64.timidityae.util.Globals;
import com.xperia64.timidityae.R;
import com.xperia64.timidityae.TimidityActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
//import android.widget.TextView;
import android.widget.Toast;


public class PlaylistFragment extends ListFragment implements FileBrowserDialogListener {
	
	String playlistDir;
	boolean gotPlaylists=false;
	ActionPlaylistBackListener mCallback;
	ArrayList<String> path;
	ArrayList<String> fname;
	public ArrayList<String> currPlist;
	ArrayList<String> vola;
	public String plistName;
	String tmpName;
	int loki=-1;
	public int highlightMe = -1;
	public boolean mode=false;
	boolean dontReloadPlist = false;
	public interface ActionPlaylistBackListener{
		public void needPlaylistBackCallback(boolean yes, boolean current);
	}
	public static PlaylistFragment create(String fold) {
		Bundle args = new Bundle();
		args.putString(Globals.currPlistDirectory, fold);
		PlaylistFragment fragment = new PlaylistFragment();
		fragment.setArguments(args);
		return fragment;
		}
	@Override
	public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	if(getArguments()!=null)
		playlistDir = getArguments().getString(Globals.currPlistDirectory);
	if(playlistDir==null)
		playlistDir="/";
	}
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
        View v = inflater.inflate(R.layout.list, container, false);
        return v;    
    }
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if(!gotPlaylists)
		{
			gotPlaylists=true;
			getPlaylists(null);
		}
		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {

	        @Override
	        public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
	                final int pos, long id) {
	        	if(!mode)
	        	{
	        	AlertDialog.Builder builderSingle = new AlertDialog.Builder(
	                    getActivity());
	            builderSingle.setIcon(R.drawable.ic_launcher);
	            builderSingle.setTitle(getActivity().getResources().getString((pos==0)?R.string.plist_save2:R.string.plist_mod));
	            builderSingle.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						
					}
				});
	            if(pos!=0)
	            {
	            builderSingle.setNeutralButton(getResources().getString(R.string.plist_del), new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						AlertDialog.Builder builderDouble = new AlertDialog.Builder(
			                    getActivity()).setIcon(R.drawable.ic_launcher).setTitle(String.format(getActivity().getResources().getString(R.string.plist_del2), fname.get(pos)));
						builderDouble.setPositiveButton(android.R.string.yes,new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								File f = new File(path.get(pos));
								if(f.exists())
								{
									 String[] x = null;
									  try{
									        new FileOutputStream(path.get(pos),true).close();
									  }catch(FileNotFoundException e)
									  {
										x=Globals.getDocFilePaths(getActivity(), f.getAbsolutePath()); 
									  } catch (IOException e)
									{
										e.printStackTrace();
									}
									  
									  if(x!=null)
									  {
										  Globals.tryToDeleteFile(getActivity(), path.get(pos));
									  }else{
										  f.delete();
									  }
								}
								getPlaylists(null);
								dialog.dismiss();
								} 
							});
						builderDouble.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {dialog.dismiss();}});
						builderDouble.show();
					}
				});
	            builderSingle.setPositiveButton(getResources().getString(R.string.plist_ren), new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

						alert.setTitle(String.format(getResources().getString(R.string.plist_ren2), fname.get(pos)));

						final EditText input = new EditText(getActivity());
						alert.setView(input);

						alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
						  String value = input.getText().toString();
						  File f = new File(path.get(pos));
						  File f2 = new File(path.get(pos).substring(0,path.get(pos).lastIndexOf('/')+1)+value+".tpl");
						  if(f.exists())
						  {
							  if(f2.exists())
							  {
								  Toast.makeText(getActivity(), getResources().getString(R.string.plist_ex), Toast.LENGTH_SHORT).show();
							  }else{
								  String[] x = null;
								  try{
								        new FileOutputStream(f.getAbsolutePath(),true).close();
								  }catch(FileNotFoundException e)
								  {
									x=Globals.getDocFilePaths(getActivity(), f.getAbsolutePath()); 
								  } catch (IOException e)
								{
									e.printStackTrace();
								} 
								  
								  if(x!=null)
								  {
									  Globals.renameDocumentFile(getActivity(), f.getAbsolutePath(), f2.getAbsolutePath().substring(f2.getAbsolutePath().indexOf(x[1])+x[1].length()));
								  }else{
									  f.renameTo(f2);
								  }
								  
								  
							  }
						  }else
							  Toast.makeText(getActivity(), getResources().getString(R.string.plist_pnf), Toast.LENGTH_SHORT).show();
						  getPlaylists(null);
						  dialog.dismiss();
						  }
						
						});

						alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
						  public void onClick(DialogInterface dialog, int whichButton) {
							  dialog.dismiss();
						  }
						});

						alert.show();
					}
				});
	            }else{
	            	
	            	final EditText input2 = new EditText(getActivity());
					builderSingle.setView(input2);
	            	builderSingle.setPositiveButton(getResources().getString(R.string.plist_save), new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if(input2.getText()!=null)
							{
								if(!TextUtils.isEmpty(input2.getText().toString()))
								{
									tmpName=playlistDir+"/"+input2.getText().toString()+".tpl";
									vola=currPlist;
									write();
								}
							}
						}
	            	}
	            );
	            }
	            builderSingle.show();
	            return true;
	        }else if(!plistName.equals("CURRENT")){
	        	AlertDialog.Builder builderSingle = new AlertDialog.Builder(
	                    getActivity());
	        	
	            builderSingle.setIcon(R.drawable.ic_launcher);
	            builderSingle.setTitle(getResources().getString(R.string.plist_modit));
	            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
	                    getActivity(),
	                    android.R.layout.select_dialog_item);
	            arrayAdapter.add(getResources().getString(R.string.plist_ds));
	            arrayAdapter.add(getResources().getString(R.string.plist_addcsh));
	            arrayAdapter.add(getResources().getString(R.string.plist_addsh));
	            arrayAdapter.add(getResources().getString(R.string.plist_addfh));
	            arrayAdapter.add(getResources().getString(R.string.plist_addfth));
	            builderSingle.setNegativeButton(android.R.string.cancel,
	                    new DialogInterface.OnClickListener() {

	                        @Override
	                        public void onClick(DialogInterface dialog, int which) {
	                            dialog.dismiss();
	                        }
	                    });

	            builderSingle.setAdapter(arrayAdapter,
	                    new DialogInterface.OnClickListener() {

	                        @Override
	                        public void onClick(DialogInterface dialog, int which) {
	                        	vola=parsePlist(tmpName=plistName);
	                        	loki=pos;
	                            switch(which)
	                            {
	                            case 0:
	                            	setItem(null, -1);
	                            	write();
	                            	break;
	                            case 1:
	                            	if(((TimidityActivity)getActivity()).currSongName!=null)
	                            	{
	                            		setItem(((TimidityActivity)getActivity()).currSongName, 0);
	                            		write();
	                            	}
	                            	break;
	                            case 2:
	                            	new FileBrowserDialog().create( 0, (Globals.showVideos?Globals.musicVideoFiles:Globals.musicFiles), PlaylistFragment.this, getActivity(), getActivity().getLayoutInflater(), false, Globals.defaultFolder, getActivity().getResources().getString(R.string.fb_add));
	                            	break;
	                            case 3:
	                            	new FileBrowserDialog().create( 1, null, PlaylistFragment.this, getActivity(), getActivity().getLayoutInflater(), false, Globals.defaultFolder, getActivity().getResources().getString(R.string.fb_add));
	                            	break;
	                            case 4:
	                            	new FileBrowserDialog().create( 2, null, PlaylistFragment.this,getActivity(), getActivity().getLayoutInflater(), false, Globals.defaultFolder, getActivity().getResources().getString(R.string.fb_add));
	                            	break;
	                            }
	                        }
	                    });
	            builderSingle.show();
	        	return true;
	        }
				return true;
	        }
	    });
	}
	@Override
	public void onAttach(Context activity)
	{
		super.onAttach(activity);
		try {
            mCallback = (ActionPlaylistBackListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ActionPlaylistBackListener");
        }
		if(Globals.shouldRestore&&(Globals.plist==null||Globals.plist.size()==0))
    	{
    		Intent new_intent = new Intent();
    		new_intent.setAction(getActivity().getResources().getString(R.string.msrv_rec));
            new_intent.putExtra(getActivity().getResources().getString(R.string.msrv_cmd), 12);
            getActivity().sendBroadcast(new_intent);
    	}
	}
	public void getPlaylists(final String which)
	{
		if(which==null) // Root playlist dir.
		{
			mode=false;
			mCallback.needPlaylistBackCallback(false, false);
			fname = new ArrayList<String>();
			path = new ArrayList<String>();   
			File f = new File(playlistDir);
			fname.add("[ "+getResources().getString(R.string.plist_curr)+" ]");
			path.add("CURRENT");
			if(f.exists())
			{
			File[] files = f.listFiles(); 
			if (files!=null&&files.length>0){
				if(files != null)
				{
					Arrays.sort(files, new FileComparator());
				}
				 mCallback.needPlaylistBackCallback(false, false);
				for(int i=0; i < files.length; i++)

				{

					if (files[i].isFile()){
						int dotPosition = files[i].getName().lastIndexOf('.');
						String extension="";
						if (dotPosition != -1) {
							extension = (files[i].getName().substring(dotPosition)).toLowerCase(Locale.US);
								if(extension!=null){
									if(Globals.playlistFiles.contains("*"+extension+"*")){
										fname.add(files[i].getName().substring(0,files[i].getName().lastIndexOf('.')));
										path.add(files[i].getAbsolutePath());
									}
								}
						}
					}
				}
			}
									}
		}else{ // An actual playlist
			mCallback.needPlaylistBackCallback(true, which.equals("CURRENT"));
			mode=true;
			path = new ArrayList<String>();
			fname = new ArrayList<String>();
			if(which.equals("CURRENT")) // current playlist
			{
				if(currPlist!=null)
				{
					if(currPlist.size()>0)
					{
						dontReloadPlist = true;
						for(String xx:currPlist)
						{
							path.add(xx);
							fname.add(xx.substring(xx.lastIndexOf('/')+1, xx.length()));
						}
					}
				}
			}else{ // Another playlist
				dontReloadPlist = false;
				vola = path=parsePlist(which);
				for(String name : path)
				{
						fname.add(name.substring(name.lastIndexOf('/')+1));
				}
			}
		}
		ArrayAdapter<String> fileList; 
		if(which!=null&&which.equals("CURRENT"))
		{
			fileList = new ArrayAdapter<String>(getActivity(),R.layout.row,fname){
				
			   /* @Override
			    public View getView(int position, View convertView, ViewGroup parent)
			    {
			        final View renderer = super.getView(position, convertView, parent);
			    	if(Globals.defaultListColor==-1)
			    	{
			    		Globals.defaultListColor = Globals.getBackgroundColor(((TextView)renderer));
			    	}
			        if (position == highlightMe)
			        {
			        	//TODO Choose a nicer color in settings?
			            renderer.setBackgroundColor(0xFF00CC00);
			        }else{
			        	renderer.setBackgroundColor(Globals.defaultListColor);
			        }
			        //renderer.postInvalidate();
			        return renderer;
			    }*/
			};
		}else{
			fileList = new ArrayAdapter<String>(getActivity(),R.layout.row,fname);
		}
		
		
		
		//fileList.notifyDataSetChanged();
		//getListView().setFastScrollEnabled(true);
		//getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
	  	/* if(which!=null&&which.equals("CURRENT")&&highlightMe!=-1)
	   	 {
	  		 System.out.println("Selected: "+highlightMe);
	   		 fileList.setSelector(R.color.listSelection);
	   		 getListView().setItemChecked(highlightMe, true);
	   	 }*/
   	 setListAdapter(fileList);
	}
	public ArrayList<String> parsePlist(String path)
	{
		 ArrayList<String> plist = new ArrayList<String>();
		try{
		FileInputStream fstream = new FileInputStream(path);
  	  DataInputStream in = new DataInputStream(fstream);
  	  BufferedReader br = new BufferedReader(new InputStreamReader(in));
  	  String strLine;
  	 
  	  while ((strLine = br.readLine()) != null)   {
  		  if(!TextUtils.isEmpty(strLine))
  			  plist.add(strLine);
  			
  	  }
  	  in.close();
  	    }catch (Exception e){//Catch exception if any
  	  System.err.println("Error: " + e.getMessage());
  	  }
		return plist;
	}
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if(mode)
		{
			((TimidityActivity)getActivity()).selectedSong(path, position, true, true, dontReloadPlist);
		}else{
			getPlaylists(plistName=path.get(position));
		}
	}
	@Override
	public void setItem(String path, int type) {
		
		switch(type)
		{
		case -1:
			vola.remove(loki);
			break;
		case 0:
			vola.add(loki++, path);
			break;
		case 1:
			File f = new File(path);
			
			if(f.exists()&&f.isDirectory())
			{
				File[] files = f.listFiles();
				Arrays.sort(files, new FileComparator());
					for(File ff : files)
					{
						if(ff!=null)
						{
							if(ff.isFile())
							{
								int dotPosition = ff.getName().lastIndexOf('.');
								String extension="";
								if (dotPosition != -1) 
								{
									extension = (ff.getName().substring(dotPosition)).toLowerCase(Locale.US);
										if(extension!=null)
										{
											if((Globals.showVideos?Globals.musicVideoFiles:Globals.musicFiles).contains("*"+extension+"*"))
											{
												vola.add(loki++, ff.getAbsolutePath());
											}
										}
								}
							}
						}
					}
			}
			break;
		case 2:
			ArrayList<File> fff = walk(new File(path));
			for(File foo : fff)
			{
				if(foo!=null)
				{
					if(foo.isFile())
					{
						int dotPosition = foo.getName().lastIndexOf('.');
						String extension="";
						if (dotPosition != -1) {
							extension = (foo.getName().substring(dotPosition)).toLowerCase(Locale.US);
								if(extension!=null){
									if((Globals.showVideos?Globals.musicVideoFiles:Globals.musicFiles).contains("*"+extension+"*"))
									{
										vola.add(loki++, foo.getAbsolutePath());
									}
								}
								}
					}
				}
			}
			break;
		}
		
	}
	public void add()
	{
		if(mode)
		{
			vola=parsePlist(tmpName=plistName);
			AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                    getActivity());
        	loki=vola.size();
        	
            builderSingle.setIcon(R.drawable.ic_launcher);
            builderSingle.setTitle(getResources().getString(R.string.plist_addto));
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                    getActivity(),
                    android.R.layout.select_dialog_item);
            arrayAdapter.add(getResources().getString(R.string.plist_addcs));
            arrayAdapter.add(getResources().getString(R.string.plist_adds));
            arrayAdapter.add(getResources().getString(R.string.plist_addf));
            arrayAdapter.add(getResources().getString(R.string.plist_addft));
            builderSingle.setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

            builderSingle.setAdapter(arrayAdapter,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        	vola=parsePlist(plistName);
                            switch(which)
                            {
                            case 0:
                            	setItem(((TimidityActivity)getActivity()).currSongName, 0);
                            	write();
                            	break;
                            case 1:
                            	new FileBrowserDialog().create( 0, (((Globals.showVideos)?Globals.musicVideoFiles:Globals.musicFiles)), PlaylistFragment.this, getActivity(), getActivity().getLayoutInflater(), false, Globals.defaultFolder, getActivity().getResources().getString(R.string.fb_add));
                            	break;
                            case 2:
                            	new FileBrowserDialog().create( 1, null, PlaylistFragment.this, getActivity(), getActivity().getLayoutInflater(), false, Globals.defaultFolder, getActivity().getResources().getString(R.string.fb_add));
                            	break;
                            case 3:
                            	new FileBrowserDialog().create( 2, null, PlaylistFragment.this,getActivity(), getActivity().getLayoutInflater(), false, Globals.defaultFolder, getActivity().getResources().getString(R.string.fb_add));
                            	break;
                            }
                        }
                    });
            builderSingle.show();
		}else{
			AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

			alert.setTitle(getResources().getString(R.string.plist_crea));
 
			final EditText input = new EditText(getActivity());
			alert.setView(input);

			alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
				}
			});
			alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			  String value = input.getText().toString();
			  File f = new File(playlistDir+value+".tpl");
			 
			  if(!f.exists())
			  {
				  String[] needLol = null;
					try{
				        new FileOutputStream(playlistDir+value+".tpl").close();
				  }catch(FileNotFoundException e)
				  {
					needLol=Globals.getDocFilePaths(getActivity(), playlistDir); 
				  } catch (IOException e)
				{
					e.printStackTrace();
				} 
					if(needLol!=null)
					{
						Globals.tryToCreateFile(getActivity(), playlistDir+value+".tpl");
					}else{
						new File(playlistDir+value+".tpl").delete();
						try
						{
							f.createNewFile();
						} catch (IOException e)
						{
							e.printStackTrace();
						}
					}
			  }
			  
			  getPlaylists(null);
			  dialog.dismiss();
			  }
			
			});
			alert.show();
		}
	}
	 public ArrayList<File> walk( File fill ) {

	    	File[] files = fill.listFiles();
	    	Arrays.sort(files, new FileComparator());
	    	ArrayList<File> filez = new ArrayList<File>();
			for (File file : files) {
				if (file.isDirectory()) {
					filez.addAll(walk(file));
				} else {
					filez.add(file);
				}
			}
			
	        return filez;
	    }
	 public void ignore()
	 {
		 vola = parsePlist(plistName);
		 loki=vola.size();
	 }
	 public void write()
	 {
		 String[] needLol = null;
		 
			try{
		        new FileOutputStream(tmpName,true).close();
		  }catch(FileNotFoundException e)
		  {
			needLol=Globals.getDocFilePaths(getActivity(), tmpName); 
		  } catch (IOException e)
		{
			e.printStackTrace();
		} 

			if(needLol!=null)
			{
				
				FileWriter writer = null;
				 if(new File(tmpName).exists())
					 Globals.tryToDeleteFile(getActivity(), tmpName);
				 
				 String probablyTheDirectory = needLol[0];
				    String probablyTheRoot = needLol[1];
				    String needRename = null;
				    String value = null;
				    if(probablyTheDirectory.length()>1)
					{
						needRename = tmpName.substring(tmpName.indexOf(probablyTheRoot)+probablyTheRoot.length());
						value = probablyTheDirectory+tmpName.substring(tmpName.lastIndexOf('/'));
					}else{
						return;
					}
				try {
					writer = new FileWriter(value);
				} catch (IOException e1) {
					e1.printStackTrace();
				} 
				 for(String str: vola) {
				   try {
					writer.write(str+"\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
				 }
				 try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				 Globals.renameDocumentFile(getActivity(), value, needRename);
			}else{
				FileWriter writer = null;
				 if(new File(tmpName).exists())
					 Globals.tryToCreateFile(getActivity(), tmpName);
				try {
					writer = new FileWriter(tmpName);
				} catch (IOException e1) {
					e1.printStackTrace();
				} 
				 for(String str: vola) {
				   try {
					writer.write(str+"\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
				 }
				 try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		 
		 getPlaylists(plistName);
	}

}
