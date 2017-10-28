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
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.xperia64.timidityae.R;
import com.xperia64.timidityae.util.Globals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class StableArrayAdapter extends ArrayAdapter<String> implements SearchableAdapter {

	private final int INVALID_ID = -1;

	private HashMap<String, Integer> mIdMap = new HashMap<>();

	private Context context;

	private PlistMenuCallback ayylmao;
	private List<String> list;
	private List<String> displayedList; // Values to be displayed
	private final List<Integer> realPositions;
	private final boolean shouldHighlight;

	public interface PlistMenuCallback {
		void openMenu(int which);
	}

	public StableArrayAdapter(Context context, int textViewResourceId, List<String> objects, PlistMenuCallback aaaa, boolean shouldHighlight) {
		super(context, textViewResourceId, objects);
		list = objects;
		displayedList = objects;
		ayylmao = aaaa;
		realPositions = new ArrayList<>();
		this.shouldHighlight = shouldHighlight;
		this.context = context;
		for (int i = 0; i < objects.size(); ++i) {
			mIdMap.put(objects.get(i), i);
		}
	}

	@Override
	public int getCount() {
		return displayedList.size();
	}

	@Override
	public String getItem(int position) {
		return displayedList.get(position);
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

	@NonNull
	@Override
	public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.row_menu, null);
		}

		//Handle TextView and display string from your list
		TextView listItemText = (TextView) view.findViewById(R.id.menuRowText);
		String xx = displayedList.get(position);
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


	@Override
	public Filter getFilter() {
		Filter filter = new Filter() {

			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {

				displayedList = (List<String>) results.values; // has the filtered values
				notifyDataSetChanged();  // notifies the data with new filtered values
			}

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults results = new FilterResults();        // Holds the results of a filtering operation in values
				List<String> FilteredArrList = new ArrayList<>();
				synchronized (realPositions) {

					if (list == null) {
						list = new ArrayList<>(displayedList); // saves the original data in mOriginalValues
					}

					/********
					 *
					 *  If constraint(CharSequence that is received) is null returns the mOriginalValues(Original) values
					 *  else does the Filtering and returns FilteredArrList(Filtered)
					 *
					 ********/
					if (constraint == null || constraint.length() == 0) {

						// set the Original result to return
						results.count = list.size();
						results.values = list;
					} else {
						constraint = constraint.toString().toLowerCase();
						realPositions.clear();
						for (int i = 0; i < list.size(); i++) {
							String data = list.get(i);
							if (data.toLowerCase().contains(constraint.toString())) {
								FilteredArrList.add(data);
								realPositions.add(i);
							}

						}
						// set the Filtered result to return
						results.count = FilteredArrList.size();
						results.values = FilteredArrList;
					}
				}
				return results;
			}
		};
		return filter;
	}

	@Override
	public int currentToReal(int position) {
		return realPositions.get(position);
	}
}
