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
import java.util.Locale;

import com.xperia64.timidityae.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public class FileBrowserDialog implements OnItemClickListener {

	ListView fbdList;
	LinearLayout fbdLayout;
	String currPath;
	ArrayList<String> fname;
	ArrayList<String> path;
	String extensions;
	Activity context;
	int type;
	String msg;
	FileBrowserDialogListener mCallback;
	AlertDialog ddd;
	public interface FileBrowserDialogListener {
		public void setItem(String path, int type);

		public void write();

		public void ignore();
	}

	boolean closeImmediate;

	public void create(int t, String fileFilter, FileBrowserDialogListener pf,
			Activity c, LayoutInflater f, boolean ci, String path, String ms) // This is disgusting. Sorry.
	{
		mCallback = pf;
		msg=ms;
		context = c;
		extensions = fileFilter;
		type = t; // A command for later reference. 0 is files, otherwise
					// folders
		closeImmediate = ci; // Close immediately after selecting a file/folder
		AlertDialog.Builder b = new AlertDialog.Builder(context);
		fbdLayout = (LinearLayout) f.inflate(R.layout.list, null);
		fbdList = (ListView) fbdLayout.findViewById(android.R.id.list);
		fbdList.setOnItemClickListener(this);
		b.setView(fbdLayout);
		b.setCancelable(false);
		b.setTitle(c.getResources().getString(
				(type == 0) ? R.string.fb_chfi : R.string.fb_chfo));
		if (!closeImmediate)
		{
			b.setPositiveButton(c.getResources().getString(R.string.done),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							mCallback.write();
						}
					});
		}
		b.setNegativeButton(
				c.getResources().getString(android.R.string.cancel),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						mCallback.ignore();
					}
				});
		if (type != 0)
		{
			b.setNeutralButton(c.getResources().getString(R.string.fb_fold),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
						}
					});
		}
		getDir(path);
		ddd = b.create();
		ddd.show();
		Button theButton = ddd.getButton(DialogInterface.BUTTON_NEUTRAL);
		if (theButton != null)
			theButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v)
				{
					mCallback.setItem(currPath, type);
					if (closeImmediate)
					{
						ddd.dismiss();
					}
				}
			});
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
					}
					for (int i = 0; i < files.length; i++)

					{
						File file = files[i];

						if ((!file.getName().startsWith(".") && !Globals.showHiddenFiles)
								|| Globals.showHiddenFiles)
						{
							if (file.isFile() && type == 0)
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

										if (extensions.contains("*" + extension
												+ "*"))
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
							} else if (file.isDirectory())
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
						context, R.layout.row, fname);
				fbdList.setFastScrollEnabled(true);
				fbdList.setAdapter(fileList);
			}
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
	{

		File file = new File(path.get(arg2));
		if (file.isDirectory())
		{
			if (file.canRead())
			{
				getDir(path.get(arg2));
			} else
			{
				new AlertDialog.Builder(context)
						.setIcon(R.drawable.ic_launcher)
						.setTitle(
								"["
										+ file.getName()
										+ "] "
										+ (context.getResources()
												.getString(R.string.fb_cread)))
						.setPositiveButton(
								context.getResources().getString(
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
				Toast.makeText(
						context,
						msg+ " '" + fname.get(arg2) + '\'',
						Toast.LENGTH_SHORT).show();
				mCallback.setItem(file.getAbsolutePath(), type);
				if(closeImmediate)
					ddd.dismiss();
			}
		}

	}

}
