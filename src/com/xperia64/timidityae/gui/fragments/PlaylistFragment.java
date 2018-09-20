/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.gui.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ListFragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.xperia64.timidityae.R;
import com.xperia64.timidityae.TimidityActivity;
import com.xperia64.timidityae.gui.DynamicListView;
import com.xperia64.timidityae.gui.DynamicListView.DraggerCallback;
import com.xperia64.timidityae.gui.SearchableAdapter;
import com.xperia64.timidityae.gui.SearchableArrayAdapter;
import com.xperia64.timidityae.gui.StableArrayAdapter;
import com.xperia64.timidityae.gui.StableArrayAdapter.PlistMenuCallback;
import com.xperia64.timidityae.gui.dialogs.FileBrowserDialog;
import com.xperia64.timidityae.gui.dialogs.FileBrowserDialog.FileBrowserDialogListener;
import com.xperia64.timidityae.util.Constants;
import com.xperia64.timidityae.util.DocumentFileUtils;
import com.xperia64.timidityae.util.FileComparator;
import com.xperia64.timidityae.util.Globals;
import com.xperia64.timidityae.util.SettingsStorage;

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

public class PlaylistFragment extends ListFragment implements FileBrowserDialogListener, DraggerCallback, PlistMenuCallback {

	private String playlistDir;
	private boolean gotPlaylists = false;
	private ActionPlaylistBackListener mCallback;
	private ArrayList<String> path;
	private ArrayList<String> fname;
	public ArrayList<String> currPlist;
	private ArrayList<String> vola;
	public String plistName;
	private String tmpName;
	private int loki = -1;
	public boolean isPlaylist = false;
	private boolean copyPlist = true;

	private boolean refreshAfterWrite = true;

	private int scrollPosition = -1;

	private EditText searchTxt;
	private String oldSearchTxt;
	private SearchableAdapter ada;

	boolean shouldUseDragNDrop() {
		return (SettingsStorage.enableDragNDrop && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH));
	}

	public interface ActionPlaylistBackListener {
		void needPlaylistBackCallback(boolean yes, boolean current);
	}


	public static PlaylistFragment create(String fold) {
		Bundle args = new Bundle();
		args.putString(Constants.currPlistDirectory, fold);
		PlaylistFragment fragment = new PlaylistFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			playlistDir = getArguments().getString(Constants.currPlistDirectory);
			if (playlistDir == null)
				playlistDir = File.separator; // TODO this should not be root
		}
	}

	public synchronized void updateFilter(CharSequence cs) {
		ada.getFilter().filter(cs);
		oldSearchTxt = cs.toString();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v;
		if (shouldUseDragNDrop()) {
			v = inflater.inflate(R.layout.plist_reorder, container, false);
		} else {
			v = inflater.inflate(R.layout.plist, container, false);
		}
		searchTxt = (EditText) v.findViewById(R.id.searchText);
		if(!isPlaylist) {
			searchTxt.setVisibility(View.GONE);
		}
		searchTxt.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
				// When user changed the Text
				if(ada!=null) {
					if(oldSearchTxt.equals(cs.toString()))
					{
						// Sometimes this will fire the previous filter and the new filter at the same time.
						// This branch is to prevent that.
						//System.out.println("Warning: Not filtering the same string");
					}else {
						updateFilter(cs);
					}
					if(getListView() instanceof DynamicListView && plistName != null) {
						if (cs.toString().isEmpty()) {
							// If the ListView is a DynamicListView, we are already on high enough of an API
							//noinspection NewApi
							((DynamicListView)getListView()).setDragState(plistName.equals("CURRENT")? DynamicListView.DragState.DRAG_DISABLED: DynamicListView.DragState.DRAG_ENABLED);
						} else {
							//noinspection NewApi
							((DynamicListView)getListView()).setDragState(plistName.equals("CURRENT")? DynamicListView.DragState.DRAG_DISABLED: DynamicListView.DragState.DRAG_WARNING);
						}
					}
				}
			}
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}

			@Override
			public void afterTextChanged(Editable arg0) {}
		});

		return v;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (shouldUseDragNDrop()) {
			// We already check for this in the above statement
			//noinspection NewApi
			((DynamicListView) getListView()).setDraggerCallback(this);
		}
		if (!gotPlaylists) {
			gotPlaylists = true;
			getPlaylists(isPlaylist?plistName:null);
		}
		if (!shouldUseDragNDrop()) {
			getListView().setOnItemLongClickListener(new OnItemLongClickListener() {

				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int pos, long id) {
					openMenu(pos);
					return true;
				}
			});
		}
	}

	@Override
	public void onAttach(Context activity) {
		super.onAttach(activity);
		try {
			mCallback = (ActionPlaylistBackListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement ActionPlaylistBackListener");
		}
		if (Globals.shouldRestore && (Globals.plist == null || Globals.plist.size() == 0)) {
			Intent new_intent = new Intent();
			new_intent.setAction(Constants.msrv_rec);
			new_intent.putExtra(Constants.msrv_cmd, Constants.msrv_cmd_get_plist);
			getActivity().sendBroadcast(new_intent);
		}
	}

	boolean getplaylisthreadrunning = false;
	public void getPlaylists(final String which) {

		if(getplaylisthreadrunning)
			return;

		if(which == null || which.equals("CURRENT"))
		{
			searchTxt.setVisibility(View.GONE);
			searchTxt.getText().clear();
		}else{
			searchTxt.setVisibility(View.VISIBLE);
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				getplaylisthreadrunning = true;
				ListAdapter l;
				if (shouldUseDragNDrop()) {
					// We alread check for this in the above statement
					//noinspection NewApi
					l = getPlaylists14(which);
				} else {
					l = getPlaylists13(which);
				}
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						setListAdapter(l);
						if(scrollPosition>=0)
						{
							if(scrollPosition>=getListView().getCount())
							{
								scrollPosition = getListView().getCount()-1;
							}
							getListView().setSelection(scrollPosition);
							scrollPosition = -1;
						}
					}
				});
				getplaylisthreadrunning = false;
			}
		}).run();
	}

	@RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public ListAdapter getPlaylists14(final String which) {
		StableArrayAdapter fileList = null;
		if (which == null) // Root playlist dir.
		{
			isPlaylist = false;

			mCallback.needPlaylistBackCallback(false, false);
			fname = new ArrayList<>();
			path = new ArrayList<>();
			File f = new File(playlistDir);
			fname.add("[ " + getResources().getString(R.string.plist_curr) + " ]");
			path.add("CURRENT");
			if (f.exists()) {
				File[] files = f.listFiles();
				if (files != null && files.length > 0) {
					Arrays.sort(files, new FileComparator());
					mCallback.needPlaylistBackCallback(false, false);
					for (File file : files) {
						if (file.isFile()) {
							String extension = Globals.getFileExtension(file);
							if (extension != null) {
								if (Globals.playlistFiles.contains("*" + extension + "*")) {
									fname.add(file.getName().substring(0, file.getName().lastIndexOf('.')));
									path.add(file.getAbsolutePath());
								}
							}
						}
					}
				}
			}
			ada = fileList = new StableArrayAdapter(getActivity(), R.layout.row_menu, fname, this, false);
			((DynamicListView) getListView()).setDragState(DynamicListView.DragState.DRAG_DISABLED);
		} else { // An actual playlist
			mCallback.needPlaylistBackCallback(true, which.equals("CURRENT"));
			isPlaylist = true;
			path = new ArrayList<>();
			fname = new ArrayList<>();
			if (which.equals("CURRENT")) // current playlist
			{
				if (currPlist != null) {
					if (currPlist.size() > 0) {
						copyPlist = false;
						for (String xx : currPlist) {
							path.add(xx);
							fname.add(xx.substring(xx.lastIndexOf('/') + 1, xx.length()));
						}
					}
				}
			} else { // Another playlist
				copyPlist = true;
				vola = path = parsePlist(which);
				for (String name : path) {
					fname.add(name.substring(name.lastIndexOf('/') + 1));
				}
			}
			path = Globals.normalToUuid(path);
			((DynamicListView) getListView()).setDragState(which.equals("CURRENT")? DynamicListView.DragState.DRAG_DISABLED:(searchTxt.getText().toString().isEmpty()?DynamicListView.DragState.DRAG_ENABLED:DynamicListView.DragState.DRAG_WARNING));
			ada = fileList = new StableArrayAdapter(getActivity(), R.layout.row_menu, path, this, which.equals("CURRENT"));
		}
		if(ada != null)
			ada.getFilter().filter(searchTxt.getText());
		oldSearchTxt = searchTxt.getText().toString();
		return fileList;
	}

	ListAdapter getPlaylists13(final String which) {
		if (which == null) // Root playlist dir.
		{
			isPlaylist = false;
			mCallback.needPlaylistBackCallback(false, false);
			fname = new ArrayList<>();
			path = new ArrayList<>();
			File f = new File(playlistDir);
			fname.add("[ " + getResources().getString(R.string.plist_curr) + " ]");
			path.add("CURRENT");
			if (f.exists()) {
				File[] files = f.listFiles();
				if (files != null && files.length > 0) {
					Arrays.sort(files, new FileComparator());
					mCallback.needPlaylistBackCallback(false, false);
					for (File file : files) {
						if (file.isFile()) {
							String extension = Globals.getFileExtension(file);
							if (extension != null) {
								if (Globals.playlistFiles.contains("*" + extension + "*")) {
									fname.add(file.getName().substring(0, file.getName().lastIndexOf('.')));
									path.add(file.getAbsolutePath());
								}
							}
						}
					}
				}
			}
		} else { // An actual playlist
			mCallback.needPlaylistBackCallback(true, which.equals("CURRENT"));
			isPlaylist = true;
			path = new ArrayList<>();
			fname = new ArrayList<>();
			if (which.equals("CURRENT")) // current playlist
			{
				if (currPlist != null) {
					if (currPlist.size() > 0) {
						copyPlist = false;
						for (String xx : currPlist) {
							path.add(xx);
							fname.add(xx.substring(xx.lastIndexOf('/') + 1, xx.length()));
						}
					}
				}
			} else { // Another playlist
				copyPlist = true;
				vola = path = parsePlist(which);
				for (String name : path) {
					fname.add(name.substring(name.lastIndexOf('/') + 1));
				}
			}
		}

		SearchableArrayAdapter fileList = new SearchableArrayAdapter(getActivity(), R.layout.row, fname, which!=null && which.equals("CURRENT"));
		ada = fileList;

		ada.getFilter().filter(searchTxt.getText());
		oldSearchTxt = searchTxt.getText().toString();
		return fileList;
	}

	public ArrayList<String> parsePlist(String path) {
		ArrayList<String> plist = new ArrayList<>();
		try {
			FileInputStream fstream = new FileInputStream(path);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;

			while ((strLine = br.readLine()) != null) {
				if (!strLine.isEmpty())
					plist.add(strLine);

			}
			in.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
		return plist;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if (isPlaylist) {
			if(!searchTxt.getText().toString().isEmpty() && ada != null)
			{
				position = ada.currentToReal(position);
			}
			ArrayList<String> paths = (shouldUseDragNDrop() ? Globals.uuidToNormal(path) : path);
			((TimidityActivity) getActivity()).selectedSong(paths, position, true, true, copyPlist);
		} else {
			getPlaylists(plistName = path.get(position));
		}
	}

	@Override
	public void setItem(String path, int type) {

		switch (type) {
			case -1:
				vola.remove(loki);
				break;
			case 0:
				vola.add(loki++, path);
				break;
			case 1:
				File f = new File(path);
				if (f.exists() && f.isDirectory()) {
					File[] files = f.listFiles();
					Arrays.sort(files, new FileComparator());
					for (File ff : files) {
						if (ff != null) {
							if (ff.isFile()) {
								if (Globals.hasSupportedExtension(ff)) {
									vola.add(loki++, ff.getAbsolutePath());
								}
							}
						}
					}
				}
				break;
			case 2:
				ArrayList<File> fff = recurseFolder(new File(path));
				for (File foo : fff) {
					if (foo != null) {
						if (foo.isFile()) {
							if (Globals.hasSupportedExtension(foo)) {
								vola.add(loki++, foo.getAbsolutePath());
							}
						}
					}
				}
				break;
		}

	}

	public void add() {
		if (isPlaylist) {
			scrollPosition = getListView().getCount();
			vola = parsePlist(tmpName = plistName);
			AlertDialog.Builder builderSingle = new AlertDialog.Builder(getActivity());
			loki = vola.size();

			builderSingle.setIcon(R.drawable.ic_launcher);
			builderSingle.setTitle(getResources().getString(R.string.plist_addto));
			final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.select_dialog_item);
			arrayAdapter.add(getResources().getString(R.string.plist_addcs));
			arrayAdapter.add(getResources().getString(R.string.plist_adds));
			arrayAdapter.add(getResources().getString(R.string.plist_addf));
			arrayAdapter.add(getResources().getString(R.string.plist_addft));
			builderSingle.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});

			builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					vola = parsePlist(plistName);
					switch (which) {
						case 0:
							if (((TimidityActivity) getActivity()).currSongName != null) {
								setItem(((TimidityActivity) getActivity()).currSongName, 0);
								write();
							}
							break;
						case 1:
							new FileBrowserDialog().create(0, Globals.getSupportedExtensions(), PlaylistFragment.this, getActivity(), getActivity().getLayoutInflater(), false, SettingsStorage.homeFolder, getActivity().getResources().getString(R.string.fb_add));
							break;
						case 2:
							new FileBrowserDialog().create(1, null, PlaylistFragment.this, getActivity(), getActivity().getLayoutInflater(), false, SettingsStorage.homeFolder, getActivity().getResources().getString(R.string.fb_add));
							break;
						case 3:
							new FileBrowserDialog().create(2, null, PlaylistFragment.this, getActivity(), getActivity().getLayoutInflater(), false, SettingsStorage.homeFolder, getActivity().getResources().getString(R.string.fb_add));
							break;
					}
				}
			});
			builderSingle.show();
		} else {
			AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

			alert.setTitle(getResources().getString(R.string.plist_crea));

			final EditText input = new EditText(getActivity());
			input.setInputType(InputType.TYPE_CLASS_TEXT);
			alert.setView(input);

			alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {

				}
			});
			alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String value = input.getText().toString();
					File f = new File(playlistDir + value + ".tpl");

					if (!f.exists()) {
						String[] needLol = null;
						try {
							new FileOutputStream(playlistDir + value + ".tpl").close();
						} catch (FileNotFoundException e) {
							needLol = DocumentFileUtils.getExternalFilePaths(getActivity(), playlistDir);
						} catch (IOException e) {
							e.printStackTrace();
						}
						if (needLol != null) {
							DocumentFileUtils.tryToCreateFile(getActivity(), playlistDir + value + ".tpl", "timidityae/tpl");
						} else {
							new File(playlistDir + value + ".tpl").delete();
							try {
								f.createNewFile();
							} catch (IOException e) {
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

	public ArrayList<File> recurseFolder(File fill) {

		File[] files = fill.listFiles();
		if (fill.canRead() && files != null) {
			Arrays.sort(files, new FileComparator());
			ArrayList<File> filez = new ArrayList<>();
			for (File file : files) {
				if (file.isDirectory()) {
					ArrayList<File> test = recurseFolder(file);
					if (test != null)
						filez.addAll(test);
				} else {
					filez.add(file);
				}
			}

			return filez;
		}
		return null;
	}

	public void ignore() {
		vola = parsePlist(plistName);
		loki = vola.size();
	}

	public void write() {
		String[] needLol = null;

		try {
			new FileOutputStream(tmpName, true).close();
		} catch (FileNotFoundException e) {
			needLol = DocumentFileUtils.getExternalFilePaths(getActivity(), tmpName);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (needLol != null) {

			FileWriter writer = null;
			if (new File(tmpName).exists())
				DocumentFileUtils.tryToDeleteFile(getActivity(), tmpName);

			String probablyTheDirectory = needLol[0];
			String probablyTheRoot = needLol[1];
			String needRename;
			String fileToWrite;
			if (probablyTheDirectory.length() > 1) {
				needRename = tmpName.substring(tmpName.indexOf(probablyTheRoot) + probablyTheRoot.length());
				fileToWrite = probablyTheDirectory + tmpName.substring(tmpName.lastIndexOf('/'));
			} else {
				return;
			}
			try {
				writer = new FileWriter(fileToWrite);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			if(writer == null)
			{
				return;
			}
			for (String str : vola) {
				try {
					writer.write(str + "\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			DocumentFileUtils.renameDocumentFile(getActivity(), fileToWrite, needRename);
		} else {
			FileWriter writer = null;
			if (new File(tmpName).exists())
				DocumentFileUtils.tryToCreateFile(getActivity(), tmpName, "timidityae/tpl");
			try {
				writer = new FileWriter(tmpName);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			if(writer == null)
			{
				return;
			}
			for (String str : vola) {
				try {
					writer.write(str + "\n");
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

		if (refreshAfterWrite) {
			getPlaylists(plistName);
		} else {
			refreshAfterWrite = true;
		}
	}

	// DynamicListCallback
	@Override
	public ArrayList<String> getArrayList() {
		return path;
	}

	@Override
	public void saveReordering() {
		vola = Globals.uuidToNormal(path);
		tmpName = plistName;
		refreshAfterWrite = false;
		write();
	}

	// PlistMenuCallback
	@Override
	public void openMenu(final int pos) {
		if (!isPlaylist) {
			AlertDialog.Builder builderSingle = new AlertDialog.Builder(getActivity());
			builderSingle.setIcon(R.drawable.ic_launcher);
			builderSingle.setTitle(getActivity().getResources().getString((pos == 0) ? R.string.plist_save2 : R.string.plist_mod));
			builderSingle.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();

				}
			});
			if (pos != 0) {
				builderSingle.setNeutralButton(getResources().getString(R.string.plist_del), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						AlertDialog.Builder builderDouble = new AlertDialog.Builder(getActivity()).setIcon(R.drawable.ic_launcher).setTitle(String.format(getActivity().getResources().getString(R.string.plist_del2), fname.get(pos)));
						builderDouble.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								File f = new File(path.get(pos));
								if (f.exists()) {
									String[] x = null;
									try {
										new FileOutputStream(path.get(pos), true).close();
									} catch (FileNotFoundException e) {
										x = DocumentFileUtils.getExternalFilePaths(getActivity(), f.getAbsolutePath());
									} catch (IOException e) {
										e.printStackTrace();
									}

									if (x != null) {
										DocumentFileUtils.tryToDeleteFile(getActivity(), path.get(pos));
									} else {
										f.delete();
									}
								}
								getPlaylists(null);
								dialog.dismiss();
							}
						});
						builderDouble.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						});
						builderDouble.show();
					}
				});
				builderSingle.setPositiveButton(getResources().getString(R.string.plist_ren), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

						alert.setTitle(String.format(getResources().getString(R.string.plist_ren2), fname.get(pos)));

						final EditText input = new EditText(getActivity());
						input.setInputType(InputType.TYPE_CLASS_TEXT);
						alert.setView(input);

						alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								String value = input.getText().toString();
								File f = new File(path.get(pos));
								File f2 = new File(path.get(pos).substring(0, path.get(pos).lastIndexOf(File.separator) + 1) + value + ".tpl");
								if (f.exists()) {
									if (f2.exists()) {
										Toast.makeText(getActivity(), getResources().getString(R.string.plist_ex), Toast.LENGTH_SHORT).show();
									} else {
										String[] x = null;
										try {
											new FileOutputStream(f.getAbsolutePath(), true).close();
										} catch (FileNotFoundException e) {
											x = DocumentFileUtils.getExternalFilePaths(getActivity(), f.getAbsolutePath());
										} catch (IOException e) {
											e.printStackTrace();
										}

										if (x != null) {
											DocumentFileUtils.renameDocumentFile(getActivity(), f.getAbsolutePath(), f2.getAbsolutePath().substring(f2.getAbsolutePath().indexOf(x[1]) + x[1].length()));
										} else {
											f.renameTo(f2);
										}

									}
								} else
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
			} else {

				final EditText input2 = new EditText(getActivity());
				input2.setInputType(InputType.TYPE_CLASS_TEXT);
				builderSingle.setView(input2);
				builderSingle.setPositiveButton(getResources().getString(R.string.plist_save), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (input2.getText() != null) {
							if (!input2.getText().toString().isEmpty()) {
								tmpName = playlistDir + File.separator + input2.getText().toString() + ".tpl";
								vola = currPlist;
								write();
							}
						}
					}
				});
			}
			builderSingle.show();
		} else if (!plistName.equals("CURRENT")) {
			scrollPosition = pos;
			AlertDialog.Builder builderSingle = new AlertDialog.Builder(getActivity());

			builderSingle.setIcon(R.drawable.ic_launcher);
			builderSingle.setTitle(getResources().getString(R.string.plist_modit));
			final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.select_dialog_item);
			arrayAdapter.add(getResources().getString(R.string.plist_ds));
			arrayAdapter.add(getResources().getString(R.string.plist_addcsh));
			arrayAdapter.add(getResources().getString(R.string.plist_addsh));
			arrayAdapter.add(getResources().getString(R.string.plist_addfh));
			arrayAdapter.add(getResources().getString(R.string.plist_addfth));
			builderSingle.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});

			builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					vola = parsePlist(tmpName = plistName);
					loki = pos;
					if(!searchTxt.getText().toString().isEmpty() && ada != null)
					{
						loki = ada.currentToReal(loki);
					}
					switch (which) {
						case 0:
							setItem(null, -1);
							write();
							break;
						case 1:
							if (((TimidityActivity) getActivity()).currSongName != null) {
								setItem(((TimidityActivity) getActivity()).currSongName, 0);
								write();
							}
							break;
						case 2:
							new FileBrowserDialog().create(0, Globals.getSupportedExtensions(), PlaylistFragment.this, getActivity(), getActivity().getLayoutInflater(), false, SettingsStorage.homeFolder, getActivity().getResources().getString(R.string.fb_add));
							break;
						case 3:
							new FileBrowserDialog().create(1, null, PlaylistFragment.this, getActivity(), getActivity().getLayoutInflater(), false, SettingsStorage.homeFolder, getActivity().getResources().getString(R.string.fb_add));
							break;
						case 4:
							new FileBrowserDialog().create(2, null, PlaylistFragment.this, getActivity(), getActivity().getLayoutInflater(), false, SettingsStorage.homeFolder, getActivity().getResources().getString(R.string.fb_add));
							break;
					}
				}
			});
			builderSingle.show();
		}
	}
}
