package com.xperia64.timidityae.gui.fragments;

import com.xperia64.timidityae.R;
import com.xperia64.timidityae.util.Globals;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class MeterFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.art_fragment, container, false);
		
		return v;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		// If the view is set up, we can set the album art
		
	}
	
}
