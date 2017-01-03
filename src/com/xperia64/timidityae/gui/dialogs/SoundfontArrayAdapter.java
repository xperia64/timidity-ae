/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.gui.dialogs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.xperia64.timidityae.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class SoundfontArrayAdapter extends ArrayAdapter<String> {

	interface SoundfontArrayAdapterListener {
		void setSFEnabled(int position, boolean yes);
	}

	private SoundfontArrayAdapterListener mc;
	private LayoutInflater inflater;
	private ArrayList<Boolean> itemChecked = new ArrayList<>();

	SoundfontArrayAdapter(SoundfontArrayAdapterListener c, Context context, List<String> sfList) {
		super(context, R.layout.row_check, R.id.checkText, sfList);
		mc = c;
		inflater = LayoutInflater.from(context);
		for (int i = 0; i < sfList.size(); i++) {
			itemChecked.add(i, !sfList.get(i).startsWith("#"));
		}
	}

	@NonNull
	@Override
	public View getView(final int position, View convertView, @NonNull final ViewGroup parent) {
		View view = convertView;
		final SoundfontHolder viewHolder;
		if (view == null) {
			view = inflater.inflate(R.layout.row_check, null);
			viewHolder = new SoundfontHolder();
			viewHolder.name = (TextView) view.findViewById(R.id.checkText);
			viewHolder.b = (ToggleButton) view.findViewById(R.id.sfSwitch);

			view.setTag(viewHolder);
		} else {
			viewHolder = (SoundfontHolder) view.getTag();
		}
		String tmp = getItem(position);
		viewHolder.name.setText(tmp.substring(tmp.lastIndexOf(File.separator) + 1));
		viewHolder.b.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				mc.setSFEnabled(position, viewHolder.b.isChecked());
			}
		});
		viewHolder.b.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				itemChecked.set(position, isChecked);
			}
		});
		viewHolder.b.setChecked(itemChecked.get(position));
		return view;
	}

	private static class SoundfontHolder {

		TextView name;
		ToggleButton b;
	}
}
