/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.gui.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.xperia64.timidityae.JNIHandler;
import com.xperia64.timidityae.R;

public class LyricFragment extends Fragment {
	private TextView lyrics;
	private ScrollView scrollContainer;
	private boolean ready;

	private String oldLyrics;

	// TODO Make lyrics shiny-er
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ready = false;
		View root = inflater.inflate(R.layout.lyrical_fragment, container, false);
		lyrics = (TextView) root.findViewById(R.id.lyrics);
		scrollContainer = (ScrollView) root.findViewById(R.id.lyric_holder);
		return root;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ready = true;
	}

	public void updateLyrics() {
		if (ready && getActivity() != null) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					oldLyrics = lyrics.getText().toString();
					lyrics.setText(JNIHandler.currentLyric.isEmpty() ? "(No Lyrics)" : JNIHandler.currentLyric);
					lyrics.invalidate();
					if (scrollContainer != null && !oldLyrics.equals(lyrics.getText().toString()))
						scrollContainer.fullScroll(ScrollView.FOCUS_DOWN);
				}
			});
		}

	}
}
