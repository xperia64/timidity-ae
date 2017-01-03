package com.xperia64.timidityae.gui;

import android.widget.Filter;

/**
 * Created by xperia64 on 1/3/17.
 */

public interface SearchableAdapter {
	Filter getFilter();

	void notifyDataSetChanged();

	int currentToReal(int position);
}
