package com.xperia64.timidityae.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import com.xperia64.timidityae.R;
import com.xperia64.timidityae.TimidityActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.EditText;
import android.widget.Toast;

public class ConfigSaver implements TimidityActivity.SpecialAction {

	Activity context;
	String currSongName;
	AlertDialog alerty;

	public ConfigSaver(Activity context, String currSongName) {
		this.context = context;
		this.currSongName = currSongName;
	}

	boolean localfinished;

	public void saveCfg() {
		localfinished = false;
		if (Globals.isMidi(currSongName) && Globals.isPlaying == 0) {
			AlertDialog.Builder alert = new AlertDialog.Builder(context);

			alert.setTitle("Save Cfg");
			alert.setMessage("Save a MIDI configuration file");
			InputFilter filter = new InputFilter() {
				public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
					for (int i = start; i < end; i++) {
						String IC = "*/*\n*\r*\t*\0*\f*`*?***\\*<*>*|*\"*:*";
						if (IC.contains("*" + source.charAt(i) + "*")) {
							return "";
						}
					}
					return null;
				}
			};
			// Set an EditText view to get user input
			final EditText input = new EditText(context);
			input.setFilters(new InputFilter[] { filter });
			alert.setView(input);

			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String value = input.getText().toString();
					if (!value.toLowerCase(Locale.US).endsWith(Globals.compressCfg ? ".tzf" : ".tcf")) {
						value += (Globals.compressCfg ? ".tzf" : ".tcf");
					}
					String parent = currSongName.substring(0, currSongName.lastIndexOf('/') + 1);
					boolean aWrite = true;
					boolean alreadyExists = new File(parent + value).exists();
					String needRename = null;
					String probablyTheRoot = "";
					String probablyTheDirectory = "";
					try {
						new FileOutputStream(parent + value, true).close();
					} catch (FileNotFoundException e) {
						aWrite = false;
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (!alreadyExists && aWrite) {
						new File(parent + value).delete();
					}
					if (aWrite && new File(parent).canWrite()) {
						value = parent + value;
					} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Globals.theFold != null) {
						// TODO
						// Write the file to getExternalFilesDir, then move it
						// with the Uri
						// We need to tell JNIHandler that movement is needed.

						String[] tmp = Globals.getDocFilePaths(context, parent);
						probablyTheDirectory = tmp[0];
						probablyTheRoot = tmp[1];
						if (probablyTheDirectory.length() > 1) {
							needRename = parent.substring(parent.indexOf(probablyTheRoot) + probablyTheRoot.length()) + value;
							value = probablyTheDirectory + '/' + value;
						} else {
							value = Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + value;
							return;
						}
					} else {
						value = Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + value;
					}
					final String finalval = value;
					final boolean canWrite = aWrite;
					final String needToRename = needRename;
					final String probRoot = probablyTheRoot;
					if (new File(finalval).exists() || (new File(probRoot + needRename).exists() && needToRename != null)) {
						AlertDialog dialog2 = new AlertDialog.Builder(context).create();
						dialog2.setTitle("Warning");
						dialog2.setMessage("Overwrite config file?");
						dialog2.setCancelable(false);
						dialog2.setButton(DialogInterface.BUTTON_POSITIVE, context.getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int buttonId) {
								if (!canWrite && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
									if (needToRename != null) {
										Globals.tryToDeleteFile(context, probRoot + needToRename);
										Globals.tryToDeleteFile(context, finalval);
									} else {
										Globals.tryToDeleteFile(context, finalval);
									}
								} else {
									new File(finalval).delete();
								}
								saveCfgPart2(finalval, needToRename);
							}
						});
						dialog2.setButton(DialogInterface.BUTTON_NEGATIVE, context.getResources().getString(android.R.string.no), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int buttonId) {
							}
						});
						dialog2.show();
					} else {
						saveCfgPart2(finalval, needToRename);
					}
				}
			});

			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Canceled.
				}
			});

			alerty = alert.show();
		}
	}

	public void saveCfgPart2(final String finalval, final String needToRename) {
		Intent new_intent = new Intent();
		new_intent.setAction(context.getResources().getString(R.string.msrv_rec));
		new_intent.putExtra(context.getResources().getString(R.string.msrv_cmd), 16);
		new_intent.putExtra(context.getResources().getString(R.string.msrv_outfile), finalval);
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
					} catch (InterruptedException e) {
					}
				}

				context.runOnUiThread(new Runnable() {
					public void run() {
						String trueName = finalval;
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Globals.theFold != null && needToRename != null) {
							if (Globals.renameDocumentFile(context, finalval, needToRename)) {
								trueName = needToRename;
							} else {
								trueName = "Error";
							}
						}
						Toast.makeText(context, "Wrote " + trueName, Toast.LENGTH_SHORT).show();
						prog.dismiss();
						Intent outgoingIntent = new Intent();
						outgoingIntent.setAction(context.getResources().getString(R.string.ta_rec));
						outgoingIntent.putExtra(context.getResources().getString(R.string.ta_cmd), 1);
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
		// TODO Auto-generated method stub
		this.localfinished = localfinished;
	}
}
