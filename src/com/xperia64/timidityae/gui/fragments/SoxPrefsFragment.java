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

import com.github.machinarius.preferencefragment.PreferenceFragment;
import com.xperia64.timidityae.SettingsActivity;

public class SoxPrefsFragment extends PreferenceFragment {
	SettingsActivity s;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		s = (SettingsActivity) getActivity();
		// Load the preferences from an XML resource
		// TODO addPreferencesFromResource(R.xml.settings_sox);

	}

}