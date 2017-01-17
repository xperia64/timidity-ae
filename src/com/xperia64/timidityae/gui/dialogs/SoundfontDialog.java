/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.gui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.xperia64.timidityae.JNIHandler;
import com.xperia64.timidityae.R;
import com.xperia64.timidityae.gui.dialogs.FileBrowserDialog.FileBrowserDialogListener;
import com.xperia64.timidityae.util.Globals;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class SoundfontDialog implements OnItemLongClickListener, FileBrowserDialogListener, SoundfontArrayAdapter.SoundfontArrayAdapterListener {

	private ArrayList<String> sfList;
	private ArrayList<String> tmpList;
	private Context context;
	private ListView mList;
	private SoundfontDialogListener mCallback;

	public interface SoundfontDialogListener {
		void writeSoundfonts(ArrayList<String> l);
	}

	public void create(ArrayList<String> currList, SoundfontDialogListener sl, final Activity c, final LayoutInflater f, final String path) {
		sfList = new ArrayList<>(currList.size());
		for (String foo : currList) {
			sfList.add(foo);
		}
		context = c;
		mCallback = sl;
		AlertDialog.Builder b = new AlertDialog.Builder(context);
		LinearLayout mLayout = (LinearLayout) f.inflate(R.layout.list, null);
		mList = (ListView) mLayout.findViewById(android.R.id.list);

		SoundfontArrayAdapter fileList = new SoundfontArrayAdapter(this, context, sfList);
		mList.setAdapter(fileList);
		mList.setOnItemLongClickListener(this);
		b.setView(mLayout);
		b.setCancelable(false);
		b.setTitle(c.getResources().getString(R.string.sf_man));
		b.setPositiveButton(c.getResources().getString(R.string.done), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mCallback.writeSoundfonts(sfList);
			}
		});
		b.setNeutralButton(c.getResources().getString(R.string.addcon), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		b.setNegativeButton(c.getResources().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		AlertDialog ddd = b.create();
		ddd.show();
		Button theButton = ddd.getButton(DialogInterface.BUTTON_NEUTRAL);
		theButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				tmpList = new ArrayList<>();
				new FileBrowserDialog().create(0, Globals.fontFiles, SoundfontDialog.this, c, f, false, path, c.getResources().getString(R.string.fb_add));
			}
		});

	}

	@Override
	public void setItem(final String path, int type) {
		if (path.toLowerCase(Locale.US).endsWith(".sfark") || path.toLowerCase(Locale.US).endsWith(".sfark.exe")) {
			AlertDialog.Builder be = new AlertDialog.Builder(context);
			be.setCancelable(false);
			be.setTitle("Extract sfArk?");
			be.setMessage(String.format("%s must be extracted. Extract to %s?", path.substring(path.lastIndexOf('/') + 1), path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.')) + ".sf2"));
			be.setNegativeButton(context.getResources().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {

				}
			});
			be.setPositiveButton(context.getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

						ProgressDialog pd;

						@Override
						protected void onPreExecute() {
							pd = new ProgressDialog(context);
							pd.setTitle(context.getResources().getString(R.string.extract));
							pd.setMessage("Extracting");
							pd.setCancelable(false);
							pd.setIndeterminate(true);
							pd.show();
						}

						@Override
						protected Void doInBackground(Void... arg0) {
							JNIHandler.decompressSFArk(path, path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.')) + ".sf2");
							return null;
						}

						@Override
						protected void onPostExecute(Void result) {
							if (pd != null)
								pd.dismiss();
							if (new File(path.substring(0, path.lastIndexOf('.')) + ".sf2").exists()) {
								tmpList.add(path.substring(0, path.lastIndexOf('.')) + ".sf2");
								AlertDialog.Builder bee = new AlertDialog.Builder(context);
								bee.setCancelable(false);
								bee.setTitle("Delete sfArk?");
								bee.setMessage(String.format("Delete %s?", path.substring(path.lastIndexOf('/') + 1)));
								bee.setNegativeButton(context.getResources().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog, int which) {

									}
								});
								bee.setPositiveButton(context.getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog, int which) {
										new File(path).delete();
									}
								});
							} else
								Toast.makeText(context, "Error extracting sfArk", Toast.LENGTH_SHORT).show();
							// b.setEnabled(true);
						}

					};
					task.execute((Void[]) null);

				}
			});
			be.show();
		} else {
			tmpList.add(path);
		}

	}

	@Override
	public void write() {

		sfList.addAll(tmpList);

		SoundfontArrayAdapter fileList = new SoundfontArrayAdapter(this, context, sfList);
		mList.setAdapter(fileList);
		mList.setOnItemLongClickListener(this);
	}

	@Override
	public void ignore() {
		tmpList = null;
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int arg2, long arg3) {
		AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);
		builderSingle.setIcon(R.drawable.ic_launcher);
		builderSingle.setTitle(context.getResources().getString(R.string.sf_rem));
		builderSingle.setCancelable(false);
		builderSingle.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();

			}
		});
		builderSingle.setPositiveButton(context.getResources().getString(R.string.sf_rem2), new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				AlertDialog.Builder builderDouble = new AlertDialog.Builder(context).setIcon(R.drawable.ic_launcher).setTitle(String.format(context.getResources().getString(R.string.sf_com), sfList.get(arg2).substring(sfList.get(arg2).lastIndexOf('/') + 1))).setCancelable(false);
				builderDouble.setPositiveButton(context.getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						sfList.remove(arg2);

						SoundfontArrayAdapter fileList = new SoundfontArrayAdapter(SoundfontDialog.this, context, sfList);
						mList.setAdapter(fileList);
						mList.setOnItemLongClickListener(SoundfontDialog.this);
					}
				});
				builderDouble.setNegativeButton(context.getResources().getString(R.string.no), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				builderDouble.show();
				dialog.dismiss();

			}
		});
		builderSingle.show();
		return true;
	}

	@Override
	public void setSFEnabled(int position, boolean enable) {
		String pos = sfList.get(position);
		if (pos.startsWith("#") && enable) {
			pos = pos.substring(1);
			sfList.set(position, pos);
		} else if (!pos.startsWith("#") && !enable) {
			sfList.set(position, "#" + pos);
		}
	}

}
