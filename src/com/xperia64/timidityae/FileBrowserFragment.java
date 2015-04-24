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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.xperia64.timidityae.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ListFragment;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.text.Spanned;

public class FileBrowserFragment extends ListFragment {
	String currPath;
	List<String> path;
	List<String> fname;
	boolean gotDir = false;
	ActionFileBackListener mCallback;
	public boolean localfinished=false;
	public interface ActionFileBackListener {
		public void needFileBackCallback(boolean yes);
	}

	public static FileBrowserFragment create(String fold)
	{
		Bundle args = new Bundle();
		args.putString(Globals.currFoldKey, fold);
		FileBrowserFragment fragment = new FileBrowserFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (getArguments() != null)
			currPath = getArguments().getString(Globals.currFoldKey);
		if (currPath == null)
			currPath = "/";
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.list, container, false);
		return v;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		if (!gotDir)
		{
			gotDir = true;
			getDir(currPath);
		}
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		try
		{
			mCallback = (ActionFileBackListener) activity;
		} catch (ClassCastException e)
		{
			throw new ClassCastException(activity.toString()
					+ " must implement ActionFileBackListener");
		}
		if (Globals.shouldRestore)
		{
			Intent new_intent = new Intent();
			new_intent.setAction(getActivity().getResources().getString(
					R.string.msrv_rec));
			new_intent.putExtra(
					getActivity().getResources().getString(R.string.msrv_cmd),
					10);
			getActivity().sendBroadcast(new_intent);
		}
	}

	public void getDir(String dirPath)
	{
		currPath = dirPath;
		fname = new ArrayList<String>();
		path = new ArrayList<String>();
		if (currPath != null)
		{
			File f = new File(currPath);
			if (f.exists())
			{
				File[] files = f.listFiles();
				if (files != null&&files.length > 0)
				{
					
						Arrays.sort(files, new FileComparator());
					
					// System.out.println(currPath);
					if (!currPath.matches("[/]+"))
					{
						fname.add("../");
						path.add(f.getParent() + "/");
						mCallback.needFileBackCallback(true);
					} else
					{
						mCallback.needFileBackCallback(false);
					}
					for (int i = 0; i < files.length; i++)
					{
						File file = files[i];
						if ((!file.getName().startsWith(".") && !Globals.showHiddenFiles)
								|| Globals.showHiddenFiles)
						{
							if (file.isFile())
							{
								int dotPosition = file.getName().lastIndexOf(".");
								String extension = "";
								if (dotPosition != -1)
								{
									extension = (file.getName()
											.substring(dotPosition))
											.toLowerCase(Locale.US);
									if (extension != null)
									{

										if ((Globals.showVideos ? Globals.musicVideoFiles
												: Globals.musicFiles)
												.contains("*" + extension + "*"))
										{

											path.add(file.getAbsolutePath());
											fname.add(file.getName());
										}
									} else if (file.getName().endsWith("/"))
									{
										path.add(file.getAbsolutePath() + "/");
										fname.add(file.getName() + "/");
									}
								}
							} else
							{
								path.add(file.getAbsolutePath() + "/");
								fname.add(file.getName() + "/");
							}
						}
					}
				} else
				{
					if (!currPath.matches("[/]+"))
					{
						fname.add("../");
						path.add(f.getParent() + "/");

					}
				}
				ArrayAdapter<String> fileList = new ArrayAdapter<String>(
						getActivity(), R.layout.row, fname);
				getListView().setFastScrollEnabled(true);
				getListView().setOnItemLongClickListener(new OnItemLongClickListener()
				{

					@Override
					public boolean onItemLongClick(AdapterView<?> l, View v, final int position, long id)
					{
					    localfinished=false;
						if(new File(path.get(position)).isFile()&& Globals.isMidi(path.get(position)))
						{
							
						
						AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

						alert.setTitle("Convert to WAV File");
						alert.setMessage("Exports the MIDI/MOD file to WAV.\nNative Midi must be disabled in settings.\nWarning: WAV files are large.");
						InputFilter filter = new InputFilter() { 
					        public CharSequence filter(CharSequence source, int start, int end, 
					Spanned dest, int dstart, int dend) { 
					                for (int i = start; i < end; i++) { 
					                	String IC = "*/*\n*\r*\t*\0*\f*`*?***\\*<*>*|*\"*:*";
					                        if (IC.contains("*"+source.charAt(i)+"*")) { 
					                                return ""; 
					                        } 
					                } 
					                return null; 
					        } 
						};
						// Set an EditText view to get user input 
						final EditText input = new EditText(getActivity());
						input.setFilters(new InputFilter[]{filter});
						alert.setView(input);

						alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
						  String value = input.getText().toString();
						  if(!value.toLowerCase(Locale.US).endsWith(".wav"))
							  value+=".wav";
						  String parent=path.get(position).substring(0,path.get(position).lastIndexOf('/')+1);
						  boolean canWrite=true;
						  boolean alreadyExists = new File(parent+value).exists();
						  String needRename = null;
						  String probablyTheRoot = "";
						  String probablyTheDirectory = "";
						  try{
						        new FileOutputStream(parent+value,true).close();
						  }catch(FileNotFoundException e)
						  {
							canWrite=false;  
						  } catch (IOException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						  if(!alreadyExists&&canWrite)
							  new File(parent+value).delete();
						  if(canWrite&&new File(parent).canWrite())
						  {
							  value=parent+value;
						  }else if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP&& Globals.theFold!=null)
						  {
							  //TODO
							  // Write the file to getExternalFilesDir, then move it with the Uri
							  // We need to tell JNIHandler that movement is needed.
							
							  String[] tmp = Globals.getDocFilePaths(getActivity(),parent);
							  probablyTheDirectory = tmp[0];
							  probablyTheRoot = tmp[1];
							if(probablyTheDirectory.length()>1)
							{
								needRename = parent.substring(parent.indexOf(probablyTheRoot)+probablyTheRoot.length())+value;
								value = probablyTheDirectory+'/'+value;
							}else{
								value=Environment.getExternalStorageDirectory().getAbsolutePath()+'/'+value;
							}
						  }else{
							  value=Environment.getExternalStorageDirectory().getAbsolutePath()+'/'+value;
						  }
						  final String finalval = value;
						  final String needToRename = needRename;
						  final String probRoot = probablyTheRoot;
						  if(new File(finalval).exists()||(new File(probRoot+needRename).exists()&&needToRename!=null))
						  {
							  AlertDialog dialog2 = new AlertDialog.Builder(getActivity()).create();
							    dialog2.setTitle("Warning");
							    dialog2.setMessage("Overwrite WAV file?");
							    dialog2.setCancelable(false);
							    dialog2.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {
							        public void onClick(DialogInterface dialog, int buttonId) {
							        	if(needToRename!=null)
							        	{
							        		Globals.tryToDeleteFile(getActivity(), probRoot+needToRename);
							        		Globals.tryToDeleteFile(getActivity(), finalval);
							        	}else{
							        		Globals.tryToDeleteFile(getActivity(), finalval);
							        	}
							        		
								        	saveWavPart2(position, finalval, needToRename);
							        	
							        }

									
							    });
							    dialog2.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(android.R.string.no), new DialogInterface.OnClickListener() {
							        public void onClick(DialogInterface dialog, int buttonId) {
							          
							        }
							    });
							    dialog2.show();
						  }else{
						     saveWavPart2(position, finalval, needToRename);
						  }
						  
						}
						});

						alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						  public void onClick(DialogInterface dialog, int whichButton) {
						    // Canceled.
						  }
						});

						alert.show();
						
						return true;
					}else{
						
					}
						return false;
					}
					
				});
				setListAdapter(fileList);
			}
		}
	}

	
	public void saveWavPart2(final int position, final String finalval, final String needToRename)
	{
		((TimidityActivity)getActivity()).writeFile(path.get(position), finalval);
		//JNIHandler.setupOutputFile("/sdcard/whya.wav");
		//System.out.println(path.get(position));
		//System.out.println(JNIHandler.play(path.get(position)));
	  final ProgressDialog prog;
	  prog = new ProgressDialog(getActivity());
	  prog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        dialog.dismiss();
		    }
		});
	  prog.setTitle("Converting to WAV");
	  prog.setMessage("Converting...");       
	  prog.setIndeterminate(false);
	  prog.setCancelable(false);
	  prog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	  prog.show();
        new Thread(new Runnable() {                 
			@Override
			public void run() {
			    while(!localfinished&&prog.isShowing()){
			    	
			    	prog.setMax(JNIHandler.maxTime);
			    	prog.setProgress(JNIHandler.currTime);
			    	try {Thread.sleep(25);} catch (InterruptedException e){}}
			    if(!localfinished)
			    {
			    	JNIHandler.stop();
			    	 getActivity().runOnUiThread(new Runnable() {
					        public void run() {
			    	Toast.makeText( getActivity(), "Conversion canceled", Toast.LENGTH_SHORT).show();
			    	if(!Globals.keepWav)
			    	{
			    		if(new File(finalval).exists())
			    			new File(finalval).delete();
			    	}else{
			    		getDir(currPath);
			    	}
					        }
			    	 });
			    	
		    			
			    }else{
			    	getActivity().runOnUiThread(new Runnable() {
			        public void run() {
			        	String trueName = finalval;
			        	if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP&& Globals.theFold!=null&&needToRename!=null)
			        	{
			        		if(Globals.renameDocumentFile(getActivity(), finalval, needToRename))
			        		{
			        			trueName=needToRename;
			        		}else{
			        			trueName="Error";
			        		}
			        	}
			        	Toast.makeText( getActivity(), "Wrote "+trueName, Toast.LENGTH_SHORT).show();
			        	prog.dismiss();
			        	getDir(currPath);
			        }
			    });
			    }
			}
        }).start();
	}
	
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		File file = new File(path.get(position));
		if (file.isDirectory())
		{
			if (file.canRead())
			{
				getDir(path.get(position));
			} else
			{
				new AlertDialog.Builder(getActivity())
						.setIcon(R.drawable.ic_launcher)
						.setTitle(
								"["
										+ file.getName()
										+ "] "
										+ (getActivity().getResources()
												.getString(R.string.fb_cread)))
						.setPositiveButton(
								getActivity().getResources().getString(
										android.R.string.ok),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which)
									{
									}
								}).show();
			}
		} else
		{
			if (file.canRead())
			{
				ArrayList<String> files = new ArrayList<String>();
				int firstFile = -1;
				for (int i = 0; i < path.size(); i++)
				{
					if (!path.get(i).endsWith("/"))
					{
						files.add(path.get(i));
						if (firstFile == -1)
						{
							firstFile = i;
						}
					}
				}
				((TimidityActivity) getActivity()).selectedSong(files, position
						- firstFile, true, false);
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putString(Globals.currFoldKey, currPath);
	}
}
