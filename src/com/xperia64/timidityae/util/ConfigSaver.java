/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import com.xperia64.timidityae.JNIHandler;
import com.xperia64.timidityae.TimidityActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

public class ConfigSaver implements TimidityActivity.SpecialAction {

	private Activity context;
	private String currSongName;
	private AlertDialog alerty;
	private boolean localfinished;

	public ConfigSaver(Activity context, String currSongName) {
		this.context = context;
		this.currSongName = currSongName;
	}

	public void promptSaveCfg() {
		localfinished = false;
		if (Globals.isMidi(currSongName) && JNIHandler.isActive()) {
			AlertDialog.Builder saveMidiConfigDialog = new AlertDialog.Builder(context);

			saveMidiConfigDialog.setTitle("Save Cfg");
			saveMidiConfigDialog.setMessage("Save a MIDI configuration file");

			// Set an EditText view to get user input
			final EditText input = new EditText(context);
			input.setInputType(InputType.TYPE_CLASS_TEXT);
			input.setFilters(new InputFilter[]{Globals.fileNameInputFilter});
			saveMidiConfigDialog.setView(input);

			saveMidiConfigDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					beginConfigFileSave(input.getText().toString());
				}
			});

			saveMidiConfigDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Canceled.
				}
			});

			alerty = saveMidiConfigDialog.show();
		}
	}

	private void beginConfigFileSave(String configFileName) {
		if (!configFileName.toLowerCase(Locale.US).endsWith(SettingsStorage.compressCfg ? ".tzf" : ".tcf")) {
			configFileName += (SettingsStorage.compressCfg ? ".tzf" : ".tcf");
		}
		String parent = currSongName.substring(0, currSongName.lastIndexOf('/') + 1);
		boolean canReallyWrite = true;
		boolean alreadyExists = new File(parent + configFileName).exists();
		String needRename = null;
		String probablyTheRoot = "";
		String probablyTheDirectory;

		// Safest way to check if we truly have write access to a file.
		// We will be touching this file anyway.
		// File.canWrite() lies with Lollipop's storage handling.
		try {
			new FileOutputStream(parent + configFileName, true).close();
		} catch (FileNotFoundException e) {
			canReallyWrite = false;
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Delete the test file if it was created and didn't actually exist.
		if (!alreadyExists && canReallyWrite) {
			new File(parent + configFileName).delete();
		}
		if (canReallyWrite && new File(parent).canWrite()) {
			configFileName = parent + configFileName;
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && DocumentFileUtils.docFileDevice != null) {
			// TODO
			// Write the file to getExternalFilesDir, then move it
			// with the Uri
			// We need to tell JNIHandler that movement is needed.

			String[] tmp = DocumentFileUtils.getExternalFilePaths(context, parent);
			probablyTheDirectory = tmp[0];
			probablyTheRoot = tmp[1];
			if (probablyTheDirectory.length() > 1) {
				needRename = parent.substring(parent.indexOf(probablyTheRoot) + probablyTheRoot.length()) + configFileName;
				configFileName = probablyTheDirectory + '/' + configFileName;
			} else {
				configFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + configFileName;
				return;
			}
		} else {
			configFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + configFileName;
		}
		final String finalval = configFileName;
		final boolean canWrite = canReallyWrite;
		final String needToRename = needRename;
		final String probRoot = probablyTheRoot;
		if (new File(finalval).exists() || (new File(probRoot + needRename).exists() && needToRename != null)) {
			AlertDialog deletionDialog = new AlertDialog.Builder(context).create();
			deletionDialog.setTitle("Warning");
			deletionDialog.setMessage("Overwrite config file?");
			deletionDialog.setCancelable(false);
			deletionDialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int buttonId) {
					if (!canWrite && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
						if (needToRename != null) {
							DocumentFileUtils.tryToDeleteFile(context, probRoot + needToRename);
							DocumentFileUtils.tryToDeleteFile(context, finalval);
						} else {
							DocumentFileUtils.tryToDeleteFile(context, finalval);
						}
					} else {
						new File(finalval).delete();
					}
					writeConfig(finalval, needToRename);
				}
			});
			deletionDialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getResources().getString(android.R.string.no), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int buttonId) {
				}
			});
			deletionDialog.show();
		} else {
			writeConfig(finalval, needToRename);
		}
	}

	public void writeConfig(final String finalval, final String needToRename) {
		Intent new_intent = new Intent();
		new_intent.setAction(Constants.msrv_rec);
		new_intent.putExtra(Constants.msrv_cmd, Constants.msrv_cmd_save_cfg);
		new_intent.putExtra(Constants.msrv_outfile, finalval);
		context.sendBroadcast(new_intent);
		final ProgressDialog prog;
		prog = new ProgressDialog(context);
		prog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		prog.setTitle("Saving CFG");
		prog.setMessage("Saving...");
		prog.setIndeterminate(true);
		prog.setCancelable(false);
		prog.show();
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (!localfinished && prog.isShowing()) {
					try {

						Thread.sleep(25);
					} catch (InterruptedException ignored) {}
				}

				context.runOnUiThread(new Runnable() {
					public void run() {
						String trueName = finalval;
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && DocumentFileUtils.docFileDevice != null && needToRename != null) {
							if (DocumentFileUtils.renameDocumentFile(context, finalval, needToRename)) {
								trueName = needToRename;
							} else {
								trueName = "Error";
							}
						}
						Toast.makeText(context, "Wrote " + trueName, Toast.LENGTH_SHORT).show();
						prog.dismiss();
						Intent outgoingIntent = new Intent();
						outgoingIntent.setAction(Constants.ta_rec);
						outgoingIntent.putExtra(Constants.ta_cmd, Constants.ta_cmd_refresh_filebrowser);
						context.sendBroadcast(outgoingIntent);
					}
				});

			}
		}).start();
	}

	@Override
	public AlertDialog getAlertDialog() {
		return alerty;
	}

	@Override
	public void setLocalFinished(boolean localfinished) {
		this.localfinished = localfinished;
	}
}
