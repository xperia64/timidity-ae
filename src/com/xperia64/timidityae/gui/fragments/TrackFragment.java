/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.gui.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;

import com.xperia64.timidityae.JNIHandler;
import com.xperia64.timidityae.R;
import com.xperia64.timidityae.util.Constants;
import com.xperia64.timidityae.util.SettingsStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrackFragment extends Fragment {

	private ArrayList<Integer> localInst = new ArrayList<>();
	private ArrayList<Integer> localVol = new ArrayList<>();

	private ArrayAdapter<String> trackListAdapter;
	private boolean fromUser;
	private ListView trackList;
	// int bigCounter=6;
	private AlertDialog ddd;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.track_fragment, container, false);
		trackList = (ListView) v.findViewById(R.id.trackList);
		return v;

	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		reset();

		trackListAdapter = new ArrayAdapter<>(getActivity(), R.layout.row);
		for (int i = 0; i < JNIHandler.MAX_CHANNELS; i++) {
			trackListAdapter.add(String.format(getActivity().getResources().getString(R.string.trk_form), (getActivity().getResources().getString(JNIHandler.drums.get(i) ? R.string.trk_drum : R.string.trk_inst2)), (i + 1), JNIHandler.drums.get(i) ? 0 : localInst.get(i) + 1, localVol.get(i)));
		}
		trackList.setAdapter(trackListAdapter);
		trackList.setOnItemClickListener(new OnItemClickListener() {

			@SuppressLint({"InflateParams", "SetTextI18n"})
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, final int arg2, long arg3) {
				AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
				View v = getActivity().getLayoutInflater().inflate(R.layout.track_dialog, null);
				final Spinner instSpin = (Spinner) v.findViewById(R.id.instSpin);
				instSpin.setClickable(JNIHandler.custInst.get(arg2) && !JNIHandler.drums.get(arg2));
				instSpin.setOnLongClickListener(null);
				instSpin.setEnabled(JNIHandler.custInst.get(arg2) && !JNIHandler.drums.get(arg2));
				List<String> arrayAdapter = new ArrayList<>();
				final int offset = (!JNIHandler.drums.get(arg2)) ? 0 : 34;
				if (!JNIHandler.drums.get(arg2)) {
					Collections.addAll(arrayAdapter, getActivity().getResources().getStringArray(R.array.midi_instruments));
				} else {
					Collections.addAll(arrayAdapter, getActivity().getResources().getStringArray(R.array.midi_drums));
				}
				ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, arrayAdapter);
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
					dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
				} else {
					dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				}

				dataAdapter.setNotifyOnChange(false);
				if (!JNIHandler.drums.get(arg2)) {
					instSpin.setAdapter(dataAdapter);
					instSpin.setSelection(localInst.get(arg2) - offset);
				}
				final EditText txtVol = (EditText) v.findViewById(R.id.txtVol);
				txtVol.setText(Integer.toString(localVol.get(arg2)));
				txtVol.setClickable(JNIHandler.custVol.get(arg2));
				txtVol.setEnabled(JNIHandler.custVol.get(arg2));

				final SeekBar volSeek = (SeekBar) v.findViewById(R.id.volSeek);
				volSeek.setClickable(JNIHandler.custVol.get(arg2));
				volSeek.setEnabled(JNIHandler.custVol.get(arg2));
				volSeek.setMax(127);
				volSeek.setProgress(localVol.get(arg2));
				volSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

					@Override
					public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
						if (arg2 && !fromUser) {
							fromUser = true;
							txtVol.setText(Integer.toString(arg0.getProgress()));
						} else {
							fromUser = false;
						}
					}

					@Override
					public void onStartTrackingTouch(SeekBar arg0) {
					}

					@Override
					public void onStopTrackingTouch(SeekBar arg0) {
					}

				});

				txtVol.addTextChangedListener(new TextWatcher() {
					public void afterTextChanged(Editable s) {
						if (!fromUser) {
							if (s.length() > 0) {
								int numm = Integer.parseInt(s.toString());
								if (numm > 127) {
									fromUser = true;
									numm = 127;
								}
								if (numm < 0) {
									fromUser = true;
									numm = 0;
								}
								if (fromUser) {
									txtVol.setText(Integer.toString(numm));
								}
								fromUser = true;
								volSeek.setProgress(numm);
								fromUser = false;
							}
						} else {
							fromUser = false;
						}
					}

					public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					}

					public void onTextChanged(CharSequence s, int start, int before, int count) {
					}
				});
				final CheckBox inst = (CheckBox) v.findViewById(R.id.defInstr);
				inst.setEnabled(!JNIHandler.drums.get(arg2));
				inst.setChecked(!JNIHandler.custInst.get(arg2));
				inst.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton arg0, boolean arg1) {

						instSpin.setClickable(!arg1);
						instSpin.setEnabled(!arg1);
					}

				});
				final CheckBox vol = (CheckBox) v.findViewById(R.id.defVol);
				vol.setChecked(!JNIHandler.custVol.get(arg2));
				// System.out.println("Def inst:
				// "+(!JNIHandler.custInst.get(arg2)?"true":"false")+" def vol:
				// "+(!JNIHandler.custVol.get(arg2)?"true":"false"));
				vol.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton arg0, boolean arg1) {

						volSeek.setClickable(!arg1);
						volSeek.setEnabled(!arg1);
						txtVol.setClickable(!arg1);
						txtVol.setEnabled(!arg1);
					}

				});
				if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
					v.setBackgroundColor(SettingsStorage.theme == 1 ? Color.WHITE : Color.BLACK);
				b.setView(v);
				b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						JNIHandler.custInst.set(arg2, !inst.isChecked());
						JNIHandler.custVol.set(arg2, !vol.isChecked());
						JNIHandler.setChannelVolumeTimidity(arg2 | (JNIHandler.custVol.get(arg2) ? Constants.jni_tim_holdmask : Constants.jni_tim_unholdmask), volSeek.getProgress());
						JNIHandler.setChannelTimidity(arg2 | (JNIHandler.custInst.get(arg2) ? Constants.jni_tim_holdmask : Constants.jni_tim_unholdmask), instSpin.getSelectedItemPosition());
						if (JNIHandler.state == JNIHandler.PlaybackState.STATE_PLAYING)
							JNIHandler.seekTo(JNIHandler.currTime);
						// bigCounter=12;
						updateList();
					}
				});

				b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});
				b.setTitle(String.format((getActivity().getResources().getString(R.string.trk_form2)), (arg2 + 1)));
				ddd = b.create();
				ddd.show();
			}

		});
	}

	public void reset() {
		localInst = new ArrayList<>();
		localVol = new ArrayList<>();
		if (ddd != null) {
			if (ddd.isShowing()) {
				ddd.dismiss();
				ddd = null;
			}
		}
		for (Integer x : JNIHandler.programs) {
			localInst.add(x);
		}
		for (Integer x : JNIHandler.volumes) {
			localVol.add(x);
		}
	}

	public void updateList() {
		if (JNIHandler.mediaBackendFormat == JNIHandler.MediaFormat.FMT_TIMIDITY) {
			// if(++bigCounter>4)
			// {
			// bigCounter=0;
			boolean needUpdate = false;

			for (int i = 0; i < JNIHandler.MAX_CHANNELS; i++) {
				if (i < localInst.size()) {
					if (!localInst.get(i).equals(JNIHandler.programs.get(i))) {
						localInst.set(i, JNIHandler.programs.get(i));
						needUpdate = true;
					}
				}
				if (i < localVol.size()) {
					if (!localVol.get(i).equals(JNIHandler.volumes.get(i))) {
						localVol.set(i, JNIHandler.volumes.get(i));
						needUpdate = true;
					}
				}
			}
			if (needUpdate) {
				// System.out.println("Need an update");
				// Prevents 'clear()' from clearing/resetting the listview
				trackListAdapter.setNotifyOnChange(false);
				trackListAdapter.clear();
				for (int i = 0; i < JNIHandler.MAX_CHANNELS; i++) {
					trackListAdapter.add(String.format(getActivity().getResources().getString(R.string.trk_form),
							(getActivity().getResources().getString(JNIHandler.drums.get(i) ? R.string.trk_drum : R.string.trk_inst2)),
							(i + 1), JNIHandler.drums.get(i) ? 0 : localInst.get(i) + 1,
							localVol.get(i)));
				}
				trackListAdapter.notifyDataSetChanged();
			}
			// }
		}
	}
}
