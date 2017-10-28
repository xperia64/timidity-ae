package com.xperia64.timidityae.gui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import com.xperia64.timidityae.R;
import com.xperia64.timidityae.util.Globals;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xperia64 on 1/3/17.
 */

public class SearchableArrayAdapter extends ArrayAdapter<String> implements SearchableAdapter{

	private List<String> list;
	private List<String> displayedList; // Values to be displayed
	private final List<Integer> realPositions;
	private final boolean shouldHighlight;

	private Context context;

	public SearchableArrayAdapter(Context context, int resource, List<String> objects, boolean shouldHighlight) {
		super(context, resource, objects);
		this.context = context;
		this.shouldHighlight = shouldHighlight;
		list = objects;
		displayedList = objects;
		realPositions = new ArrayList<>();
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
		return position;
	}

	@NonNull
	@Override
	public View getView(int position, View convertView, @NonNull ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.row, null);
		}

		TextView listItemText = (TextView) view.findViewById(R.id.rowtext);
		String xx = displayedList.get(position);
		listItemText.setText(xx.substring(xx.lastIndexOf('/') + 1));

		if (shouldHighlight) {
			if (Globals.defaultListColor == -1) {
				Globals.defaultListColor = Globals.getBackgroundColor(((TextView) view));
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
			protected synchronized FilterResults performFiltering(CharSequence constraint) {
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
