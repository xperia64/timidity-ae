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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.xperia64.timidityae.R;

public class LyricFragment extends Fragment {
	TextView lyrics;
	ScrollView scrollContainer;
	boolean ready;

	// TODO Make lyrics shiny-er
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{

		ready = false;
		View root = inflater.inflate(R.layout.lyrical_fragment, container,
				false);
		lyrics = (TextView) root.findViewById(R.id.lyrics);
		scrollContainer = (ScrollView) root.findViewById(R.id.lyric_holder);

		return root;

	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		ready = true;
	}

	public void updateLyrics()
	{
		if (ready && getActivity() != null)
		{
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run()
				{
					lyrics.setText(TextUtils.isEmpty(JNIHandler.currentLyric) ? "(No Lyrics)"
							: JNIHandler.currentLyric);
					lyrics.invalidate();
					if (scrollContainer != null)
						scrollContainer.fullScroll(ScrollView.FOCUS_DOWN);
				}
			});
		}

	}
}
