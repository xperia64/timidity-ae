/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

public class DocumentFileUtils {

	/**
	 * The DocumentFileUtils class is used to simplify some operations related to API21+'s external storage access.
	 */

	public static Uri docFileDevice = null;


	private static String fixRepeatedSeparator(String filename) {
		return filename.replaceAll(Globals.repeatedSeparatorString, File.separator);
	}

	public static boolean tryToDeleteFile(Context c, String filename) {
		filename = fixRepeatedSeparator(filename);
		if (new File(filename).exists()) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && docFileDevice != null) {
				DocumentFile df = DocumentFile.fromTreeUri(c, docFileDevice);
				String split[] = filename.split("/");
				int i;
				for (i = 0; i < split.length; i++) {
					// Did we find the document file?
					if (split[i].equals(df.getName())) {
						i++;
						break;
					}
				}
				while (i < split.length) {
					df = df.findFile(split[i++]);
					// upper.append("../");
					if (df == null) {
						Log.e("TimidityAE Globals", "Delete file error (file not found)");
						return false;
					}
				}
				// Why on earth is DocumentFile's delete method recursive by default?
				// Seriously. I wiped my sd card twice because of this.
				if (df != null && df.isFile() && !df.isDirectory()) {
					df.delete();
				} else {
					Log.e("TimidityAE Globals", "Delete file error (file not found)");
					return false;
				}
			} else {
				new File(filename).delete();
			}
			return true;
		}
		return false;
	}

	public static boolean tryToCreateFile(Context context, String filename, String mimetype) {
		filename = fixRepeatedSeparator(filename);
		if (!(new File(filename).exists())) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && docFileDevice != null) {
				DocumentFile df = DocumentFile.fromTreeUri(context, docFileDevice);
				String split[] = filename.split("/");
				int i;
				for (i = 0; i < split.length; i++) {
					if (split[i].equals(df.getName())) {
						i++;
						break;
					}
				}
				while (i < split.length - 1) {
					df = df.findFile(split[i++]);
					if (df == null) {
						Log.e("TimidityAE Globals", "Create file error.");
						return false;
					}
				}
				if (df != null) {
					df.createFile(mimetype, split[split.length - 1]);
				} else {
					return false;
				}
			} else {
				try {
					new File(filename).createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
			}
		}
		return true; // I guess if it exists already it's technically created
	}


	/**
	 * Renames/moves a file using the DocumentFile class.
	 * This is intended for use with API21+ external storage access.
	 * The existing file must be on the same filesystem as the existing file.
	 *
	 * @param context the android context
	 * @param from    the full path to the original file
	 * @param subTo   the path to the new file without the mount point prefix
	 * @return whether the operation was successful
	 */
	private static boolean renameDocumentFile21(Context context, String from, String subTo) {
		// From is the full path
		// subTo is the path without the device prefix.
		// So /storage/sdcard1/folder/file.mid should be folder/file.mid
		if (docFileDevice == null)
			return false;
		from = fixRepeatedSeparator(from);
		subTo = fixRepeatedSeparator(subTo);
		DocumentFile df = DocumentFile.fromTreeUri(context, docFileDevice);
		String split[] = from.split(File.separator);
		int i;

		// Locate the filesystem-relative path by comparing it to the
		// DocumentFile root directory.
		for (i = 0; i < split.length; i++) {
			if (split[i].equals(df.getName())) {
				i++;
				break;
			}
		}
		StringBuilder upper = new StringBuilder();
		while (i < split.length) {
			df = df.findFile(split[i++]);
			upper.append(Globals.parentString); // Usually "../"
			if (df == null) {
				Log.e("TimidityAE Globals", "Rename file error.");
				return false;
			}
		}
		// DocumentFile's rename renames in the context of the current working directory.
		// The relative root directory must be specified in terms of many "../"
		return df != null && upper.length() > 3 && df.renameTo(upper.substring(0, upper.length() - 3) + subTo);
	}

	private static boolean renameDocumentFile23(Context context, String from, String subTo) {
		if (docFileDevice == null)
			return false;
		from = fixRepeatedSeparator(from);
		subTo = fixRepeatedSeparator(subTo);
		DocumentFile in = DocumentFile.fromTreeUri(context, docFileDevice);
		String insplit[] = from.split(File.separator);
		int i;
		for (i = 0; i < insplit.length; i++) {
			if (insplit[i].equals(in.getName())) {
				i++;
				break;
			}
		}
		while (i < insplit.length) {
			in = in.findFile(insplit[i++]);

			if (in == null) {
				Log.e("TimidityAE Globals", "Rename file error.");
				return false;
			}
		}
		String outsplit[] = subTo.split(File.separator);
		DocumentFile out = DocumentFile.fromTreeUri(context, docFileDevice);
		for (i = 0; i < outsplit.length - 1; i++) {
			if (out.findFile(outsplit[i]) != null) {
				out = out.findFile(outsplit[i]);
			} else {
				out = out.createDirectory(outsplit[i]);
			}
		}
		out = out.createFile("application/octet-stream", outsplit[outsplit.length - 1]);

		try {
			DataInputStream is = new DataInputStream(new BufferedInputStream(context.getContentResolver().openInputStream(in.getUri())));
			DataOutputStream os = new DataOutputStream(new BufferedOutputStream(context.getContentResolver().openOutputStream(out.getUri())));
			while ((i = is.read()) > -1) {
				os.write(i);
			}
			is.close();
			os.close();
			in.delete();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public static boolean renameDocumentFile(final Context context, final String from, final String subTo) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return renameDocumentFile23(context, from, subTo);
		} else {
			return renameDocumentFile21(context, from, subTo);
		}
	}

	/**
	 * This method attempts to find the temporary folder on the filesystem containing parent.
	 *
	 * @param context the android context
	 * @param parent  the file/folder we want to find the storage device of
	 * @return String array in the form {Package-writable 'files' folder, Best guess at the path to the root of the storage device}
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static String[] getExternalFilePaths(Context context, String parent) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
			return null; // Error.

		parent = fixRepeatedSeparator(parent);
		String probablyTheDirectory = "";
		String probablyTheRoot = "";
		File par = new File(parent);
		String absoluteParent = par.getAbsolutePath();
		// Ensure that this ends with a separator
		// This helps edge cases such as files on root directories of filesystems.
		// Where we have folders like /storage/sdcard/ and /storage/sdcard1/
		if (!absoluteParent.endsWith(File.separator)) {
			absoluteParent = absoluteParent + File.separator;
		}
		File[] exf = context.getExternalFilesDirs(null);
		for (File f : exf) {
			if (f != null) {
				String ex = f.getAbsolutePath();
				if (!ex.contains("Android")) {
					break;
				}
				ex = ex.substring(0, ex.indexOf("/Android/") + 1);
				String ss1;
				String ss2;
				int lastmatch = 1;
				while (lastmatch < absoluteParent.length() && lastmatch < ex.length()) {
					ss1 = ex.substring(0, lastmatch + 1);
					ss2 = absoluteParent.substring(0, lastmatch + 1);
					if (ss1.equals(ss2)) {
						lastmatch++;
					} else {
						break;
					}
				}
				if (lastmatch < ex.length()) {
					continue;
				}
				String theRoot = absoluteParent.substring(0, lastmatch);
				File testFile = new File(theRoot);
				// The root must have the path of a folder, exist, and actually be a folder
				if (theRoot.endsWith(File.separator) && testFile.exists() && testFile.isDirectory()) {
					probablyTheDirectory = f.getAbsolutePath();
					probablyTheRoot = theRoot;
					break;
				}
			}
		}
		System.out.println(String.format("A %s B %s C %s", parent, probablyTheDirectory, probablyTheRoot));
		String[] rets = new String[2];
		rets[0] = probablyTheDirectory;
		rets[1] = probablyTheRoot;
		return rets;
	}

}
