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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.xperia64.timidityae.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class FileBrowserFragment extends ListFragment {
	String currPath;
	List<String> path;
	List<String> fname;
	boolean gotDir = false;
	ActionFileBackListener mCallback;

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

				if (files.length > 0)
				{
					if (files != null)
					{
						Arrays.sort(files, new FileComparator());
					}
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
								int dotPosition = file.getName().lastIndexOf(
										".");
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
				setListAdapter(fileList);
			}
		}
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
