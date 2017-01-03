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
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.xperia64.timidityae.R;
import com.xperia64.timidityae.util.Globals;

public class ArtFragment extends Fragment {
	private ImageView mImg;
	private boolean artOk = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.art_fragment, container, false);
		mImg = (ImageView) v.findViewById(R.id.albumArt);
		return v;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		// If the view is set up, we can set the album art
		artOk = true;
		setArt(Globals.currArt, getActivity());
	}

	public void setArt(final Bitmap art, Activity a) {
		if (artOk) {
			if (art != null) {
				a.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mImg.setImageBitmap(art);
						mImg.invalidate();
					}
				});
			} else {
				a.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mImg.setImageResource(R.drawable.timidity);
						mImg.invalidate();
					}
				});
			}
		}
	}
}
