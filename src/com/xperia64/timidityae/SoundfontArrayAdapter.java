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

import java.io.File;
import java.util.List;

import com.xperia64.timidityae.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ToggleButton;

public class SoundfontArrayAdapter extends ArrayAdapter<String> {

	public interface SoundfontArrayAdapterListener {
		public void setSFEnabled(int position, boolean yes);
	}

	SoundfontArrayAdapterListener mc;
	private LayoutInflater inflater;

	public SoundfontArrayAdapter(SoundfontArrayAdapterListener c, Context context, List<String> sfList) {
		super(context, R.layout.row_check, R.id.checkText, sfList);
		mc = c;
		inflater = LayoutInflater.from(context);
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final String sfName = this.getItem(position);

		ToggleButton switchy;
		TextView textView;

		convertView = inflater.inflate(R.layout.row_check, null);

		textView = (TextView) convertView.findViewById(R.id.checkText);
		switchy = (ToggleButton) convertView.findViewById(R.id.sfSwitch);

		switchy.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ToggleButton cb = (ToggleButton) v;
				mc.setSFEnabled(position, cb.isChecked());
			}
		});

		switchy.setChecked(!sfName.startsWith("#"));
		textView.setText(sfName.substring(sfName.lastIndexOf(File.separator) + 1));

		return convertView;
	}

}
