/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.gui.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.xperia64.timidityae.R;
import com.xperia64.timidityae.TimidityActivity;
import com.xperia64.timidityae.util.Constants;
import com.xperia64.timidityae.util.FileComparator;
import com.xperia64.timidityae.util.Globals;
import com.xperia64.timidityae.util.SettingsStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class FileBrowserFragment extends ListFragment {
	public String currPath;

	private List<String> path;
	private boolean gotDir = false;
	private ActionFileBackListener mCallback;
	private Activity mActivity;

	private final OnItemLongClickListener longClick = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> l, View v, final int position, long id) {
			if (new File(path.get(position)).isFile() && Globals.isMidi(path.get(position))) {
				((TimidityActivity) mActivity).dynExport(path.get(position), false);
				return true;
			}
			return false;
		}

	};

	public interface ActionFileBackListener {
		void needFileBackCallback(boolean yes);
	}

	public static FileBrowserFragment create(String fold) {
		Bundle args = new Bundle();
		args.putString(Constants.currFoldKey, fold);
		FileBrowserFragment fragment = new FileBrowserFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null)
			currPath = getArguments().getString(Constants.currFoldKey);
		if (currPath == null)
			currPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		else if (!new File(currPath).exists())
			currPath = Environment.getExternalStorageDirectory().getAbsolutePath();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.list, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (!gotDir) {
			gotDir = true;
			getDir(currPath);
		}
	}

	@Override
	public void onAttach(Context activity) {
		super.onAttach(activity);
		if (activity instanceof Activity) {
			mActivity = (Activity) activity;
		}
		try {
			mCallback = (ActionFileBackListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement ActionFileBackListener");
		}
		if (Globals.shouldRestore) {
			Intent new_intent = new Intent();
			new_intent.setAction(Constants.msrv_rec);
			new_intent.putExtra(Constants.msrv_cmd, Constants.msrv_cmd_get_fold);
			mActivity.sendBroadcast(new_intent);
		}
	}

	@Override
	public void onDetach() {
		mActivity = null;
		mCallback = null;
		super.onDetach();
	}

	@Override
	public void onDestroy() {
		mActivity = null;
		mCallback = null;
		super.onDestroy();
	}

	public void refresh() {
		getDir(currPath);
	}

	public void getDir(String dirPath) {
		if (dirPath.matches(Globals.repeatedSeparatorString) && !new File(dirPath).canRead()) {
			return;
		}
		if (!new File(dirPath).canRead() && (dirPath.toLowerCase(Locale.US).equals("/storage/emulated") || dirPath.toLowerCase(Locale.US).equals("/storage/emulated/"))) {
			getDir(new File(dirPath).getParent());
			return;
		}
		currPath = dirPath;
		List<String> fname = new ArrayList<>();
		path = new ArrayList<>();
		if (currPath != null) {
			File f = new File(currPath);
			if (f.exists()) {
				File[] files = f.listFiles();
				if (files != null && files.length > 0) {
					Arrays.sort(files, new FileComparator());
					if (!currPath.matches(Globals.repeatedSeparatorString) && !((currPath.equals(File.separator + "storage" + File.separator) || currPath.equals(File.separator + "storage")) && !(new File(File.separator).canRead()))) {
						fname.add(Globals.parentString);
						// Thank you Marshmallow.
						// Disallowing access to /storage/emulated has now
						// prevent billions of hacking attempts daily.
						if (new File(f.getParent()).canRead()) {
							path.add(f.getParent() + File.separator);
						} else if (new File(File.separator).canRead()) {
							path.add(File.separator);
						} else {
							path.add(File.separator + "storage" + File.separator);
						}
						mCallback.needFileBackCallback(true);
					} else {
						mCallback.needFileBackCallback(false);
					}
					for (File file : files) {
						if ((!file.getName().startsWith(".") && !SettingsStorage.showHiddenFiles) || SettingsStorage.showHiddenFiles) {
							if (file.isFile()) {
								String extension = Globals.getFileExtension(file);
								if (extension != null) {

									if (Globals.getSupportedExtensions().contains("*" + extension + "*")) {

										path.add(file.getAbsolutePath());
										fname.add(file.getName());
									}
								} else if (file.getName().endsWith(File.separator)) {
									path.add(file.getAbsolutePath() + File.separator);
									fname.add(file.getName() + File.separator);
								}

							} else {
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
				ArrayAdapter<String> fileList = new ArrayAdapter<>(mActivity, R.layout.row, fname);
				getListView().setFastScrollEnabled(true);
				getListView().setOnItemLongClickListener(longClick);
				setListAdapter(fileList);
			}
		}
	}

	public void fixLongClick()
	{
		if(getListView()!=null)
		{
			getListView().setOnItemLongClickListener(longClick);
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		File file = new File(path.get(position));
		if (file.isDirectory()) {
			if (file.canRead()) {
				getDir(path.get(position));
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
				AlertDialog.Builder unreadableDialog = new AlertDialog.Builder(mActivity);
				unreadableDialog = unreadableDialog.setIcon(R.drawable.ic_launcher);
				unreadableDialog = unreadableDialog.setTitle(String.format("[%1$s] %2$s", file.getName(), mActivity.getResources().getString(R.string.fb_cread)));
				unreadableDialog = unreadableDialog.setPositiveButton(mActivity.getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});
				unreadableDialog.show();
			}
		} else {
			if (file.canRead()) {
				ArrayList<String> files = new ArrayList<>();
				int firstFile = -1;
				for (int i = 0; i < path.size(); i++) {
					if (!path.get(i).endsWith(File.separator)) {
						files.add(path.get(i));
						if (firstFile == -1) {
							firstFile = i;
						}
					}
				}
				((TimidityActivity) mActivity).selectedSong(files, position - firstFile, true, false, true);
			}
		}
		fixLongClick();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(Constants.currFoldKey, currPath);
	}
}
