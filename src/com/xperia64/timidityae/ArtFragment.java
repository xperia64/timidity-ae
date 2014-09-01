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

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockFragment;
import com.xperia64.timidityae.R;

public class ArtFragment extends SherlockFragment {
	private ImageView mImg;
	private boolean artOk = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.art_fragment, container, false);
		mImg = (ImageView) v.findViewById(R.id.albumArt);
		return v;

	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		artOk = true;
		setArt(Globals.currArt, getActivity());
	}

	public void setArt(final Bitmap art, Activity a)
	{
		if (artOk)
		{
			if (art != null)
			{
				a.runOnUiThread(new Runnable() {
					@Override
					public void run()
					{
						mImg.setImageBitmap(art);
						mImg.invalidate();
					}
				});
			} else
			{
				a.runOnUiThread(new Runnable() {
					@Override
					public void run()
					{
						mImg.setImageResource(R.drawable.timidity);
						mImg.invalidate();
					}
				});
			}
		}
	}
}
