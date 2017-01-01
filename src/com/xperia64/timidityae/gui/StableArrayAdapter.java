/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xperia64.timidityae.gui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.xperia64.timidityae.R;
import com.xperia64.timidityae.util.Globals;

import java.util.HashMap;
import java.util.List;

public class StableArrayAdapter extends ArrayAdapter<String> {

	final int INVALID_ID = -1;

	HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

	Context context;

	PlistMenuCallback ayylmao;
	List<String> list;
	final boolean shouldHighlight;

	public interface PlistMenuCallback {
		public void openMenu(int which);
	}

	public StableArrayAdapter(Context context, int textViewResourceId, List<String> objects, PlistMenuCallback aaaa, boolean shouldHighlight) {
		super(context, textViewResourceId, objects);
		list = objects;
		ayylmao = aaaa;
		this.shouldHighlight = shouldHighlight;
		this.context = context;
		for (int i = 0; i < objects.size(); ++i) {
			mIdMap.put(objects.get(i), i);
		}
	}

	@Override
	public long getItemId(int position) {
		if (position < 0 || position >= mIdMap.size()) {
			return INVALID_ID;
		}
		String item = getItem(position);
		return mIdMap.get(item);
	}

	@Override
	public boolean hasStableIds() {
		return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.row_menu, null);
		}

		//Handle TextView and display string from your list
		TextView listItemText = (TextView) view.findViewById(R.id.menuRowText);
		String xx = list.get(position);
		if (xx.contains("*")) {
			listItemText.setText(xx.substring(xx.lastIndexOf('/') + 1, xx.indexOf("*")));
		} else {
			listItemText.setText(xx.substring(xx.lastIndexOf('/') + 1));
		}

		//Handle buttons and add onClickListeners
		ImageView deleteBtn = (ImageView) view.findViewById(R.id.menuRowButton);
		if (shouldHighlight) {
			deleteBtn.setVisibility(View.GONE);
		} else {
			deleteBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					//do something
					ayylmao.openMenu(position);
					notifyDataSetChanged();
				}
			});
		}

		if (shouldHighlight) {
			if (Globals.defaultListColor == -1) {
				Globals.defaultListColor = Globals.getBackgroundColor(listItemText);
			}
			if (position == Globals.highlightMe) {
				// TODO Choose a nicer color in settings?
				view.setBackgroundColor(0xFF00CC00);
			} else {
				view.setBackgroundColor(Globals.defaultListColor);
			}
			view.postInvalidate();
		}
		return view;
	}

}
