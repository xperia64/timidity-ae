/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.gui.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.xperia64.timidityae.R;
import com.xperia64.timidityae.util.FileComparator;
import com.xperia64.timidityae.util.Globals;
import com.xperia64.timidityae.util.SettingsStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class FileBrowserDialog implements OnItemClickListener {

	private ListView fbdList;
	private String currPath;
	private ArrayList<String> fname;
	private ArrayList<String> path;
	private String extensions;
	private Activity context;
	private int type;
	private String msg;
	private FileBrowserDialogListener onSelectedCallback;
	private AlertDialog ddd;

	public interface FileBrowserDialogListener {
		void setItem(String path, int type);

		void write();

		void ignore();
	}

	private boolean closeImmediately; // Should the dialog be closed immediately after selecting the file?

	@SuppressLint("InflateParams")
	public void create(int type, String extensions, FileBrowserDialogListener onSelectedCallback, Activity context, LayoutInflater layoutInflater, boolean closeImmediately, String path, String msg) {
		this.onSelectedCallback = onSelectedCallback;
		this.msg = msg;
		this.context = context;
		this.extensions = extensions;
		this.type = type; // A command for later reference. 0 is files, otherwise
		// folders
		this.closeImmediately = closeImmediately; // Close immediately after selecting a file/folder
		AlertDialog.Builder b = new AlertDialog.Builder(context);
		LinearLayout fbdLayout = (LinearLayout) layoutInflater.inflate(R.layout.list, null);
		fbdList = (ListView) fbdLayout.findViewById(android.R.id.list);
		fbdList.setOnItemClickListener(this);
		b.setView(fbdLayout);
		b.setCancelable(false);
		b.setTitle(this.context.getResources().getString((type == 0) ? R.string.fb_chfi : R.string.fb_chfo));
		if (!closeImmediately) {
			b.setPositiveButton(this.context.getResources().getString(R.string.done), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					FileBrowserDialog.this.onSelectedCallback.write();
				}
			});
		}
		b.setNegativeButton(context.getResources().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				FileBrowserDialog.this.onSelectedCallback.ignore();
			}
		});
		if (type != 0) {
			b.setNeutralButton(this.context.getResources().getString(R.string.fb_fold), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
		}
		if (path == null)
			path = Environment.getExternalStorageDirectory().getAbsolutePath();
		else if (!new File(path).exists())
			path = Environment.getExternalStorageDirectory().getAbsolutePath();
		getDir(path);
		ddd = b.create();
		ddd.show();
		Button theButton = ddd.getButton(DialogInterface.BUTTON_NEUTRAL);
		if (theButton != null)
			theButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					FileBrowserDialog.this.onSelectedCallback.setItem(currPath, FileBrowserDialog.this.type);
					if (FileBrowserDialog.this.closeImmediately) {
						ddd.dismiss();
					}
				}
			});
	}

	private void getDir(String dirPath) {
		currPath = dirPath;
		fname = new ArrayList<>();
		path = new ArrayList<>();
		if (currPath != null) {
			File f = new File(currPath);
			if (f.exists()) {
				File[] files = f.listFiles();
				if (files.length > 0) {
					Arrays.sort(files, new FileComparator());
					if (!currPath.matches(Globals.repeatedSeparatorString) && !(currPath.equals(File.separator + "storage" + File.separator) && !(new File(File.separator).canRead()))) {
						fname.add(Globals.parentString);
						// Thank you Marshmallow.
						// Disallowing access to /storage/emulated has now prevent billions of hacking attempts daily.
						if (new File(f.getParent()).canRead()) {
							path.add(f.getParent() + File.separator);
						} else if (new File(File.separator).canRead()) { // N seems to block reading /
							path.add(File.separator);
						} else {
							path.add(File.separator + "storage" + File.separator);
						}
					}
					for (File file : files) {
						if ((!file.getName().startsWith(".") && !SettingsStorage.showHiddenFiles) || SettingsStorage.showHiddenFiles) {
							if (file.isFile() && type == 0) {
								String extension = Globals.getFileExtension(file);
								if (extension != null) {
									if (extensions.contains("*" + extension + "*")) {
										path.add(file.getAbsolutePath());
										fname.add(file.getName());
									}
								} else if (file.getName().endsWith(File.separator)) {
									path.add(file.getAbsolutePath() + File.separator);
									fname.add(file.getName() + File.separator);
								}
							} else if (file.isDirectory()) {
								path.add(file.getAbsolutePath() + File.separator);
								fname.add(file.getName() + File.separator);
							}
						}
					}
				} else {
					if (!currPath.matches(Globals.repeatedSeparatorString) && !(currPath.equals(File.separator + "storage" + File.separator) && !(new File(File.separator).canRead()))) {
						fname.add(Globals.parentString);
						// Thank you Marshmallow.
						// Disallowing access to /storage/emulated has now prevent billions of hacking attempts daily.
						if (new File(f.getParent()).canRead()) {
							path.add(f.getParent() + File.separator);
						} else if (new File(File.separator).canRead()) { // N seems to block reading /
							path.add(File.separator);
						} else {
							path.add(File.separator + "storage" + File.separator);
						}

					}
				}

				ArrayAdapter<String> fileList = new ArrayAdapter<>(context, R.layout.row, fname);
				fbdList.setFastScrollEnabled(true);
				fbdList.setAdapter(fileList);
			}
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

		File file = new File(path.get(arg2));
		if (file.isDirectory()) {
			if (file.canRead()) {
				getDir(path.get(arg2));
			} else if (file.getAbsolutePath().equals("/storage/emulated") &&
					((new File("/storage/emulated/0").exists() && new File("/storage/emulated/0").canRead()) ||
							(new File("/storage/emulated/legacy").exists() && new File("/storage/emulated/legacy").canRead()) ||
							(new File("/storage/self/primary").exists() && new File("/storage/self/primary").canRead()))) {
				if (new File("/storage/emulated/0").exists() && new File("/storage/emulated/0").canRead()) {
					getDir("/storage/emulated/0");
				} else if ((new File("/storage/emulated/legacy").exists() && new File("/storage/emulated/legacy").canRead())) {
					getDir("/storage/emulated/legacy");
				} else {
					getDir("/storage/self/primary");
				}
			} else {
				AlertDialog.Builder unreadableDialog = new AlertDialog.Builder(context);
				unreadableDialog = unreadableDialog.setIcon(R.drawable.ic_launcher);
				unreadableDialog = unreadableDialog.setTitle(String.format("[%1$s] %2$s", file.getName(), context.getResources().getString(R.string.fb_cread)));
				unreadableDialog = unreadableDialog.setPositiveButton(context.getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});
				unreadableDialog.show();
			}
		} else {
			if (file.canRead()) {
				Toast.makeText(context, String.format("%1$s '%2$s'", msg, fname.get(arg2)), Toast.LENGTH_SHORT).show();
				onSelectedCallback.setItem(file.getAbsolutePath(), type);
				if (closeImmediately)
					ddd.dismiss();
			}
		}

	}

}
