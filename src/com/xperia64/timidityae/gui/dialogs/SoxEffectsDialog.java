/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.gui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.xperia64.timidityae.R;
import com.xperia64.timidityae.SettingsActivity;
import com.xperia64.timidityae.util.Constants;
import com.xperia64.timidityae.util.SettingsStorage;

import java.util.Locale;
import java.util.Set;

public class SoxEffectsDialog {

	private Activity context;

	private CheckBox speedChk;
	private final double[] speedVals = {0.05,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20};
	private boolean speedFromSetter = false;
	private SeekBar speedSeek;
	private EditText speedVal;

	private CheckBox tempoChk;
	private final double[] tempoVals = {0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1,2,3,4,5,6,7,8,9,10};
	private boolean tempoFromSetter = false;
	private SeekBar tempoSeek;
	private EditText tempoVal;

	private CheckBox pitchChk;
	private final int[] pitchVals = {-500,-400,-300,-200,-100,0,100,200,300,400,500};
	private boolean pitchFromSetter = false;
	private SeekBar pitchSeek;
	private EditText pitchVal;

	private CheckBox delayChk;
	private EditText delayValL;
	private EditText delayValR;

	private TextView equivCmd;

	private EditText manCmd;

	public void create(final Activity c, final LayoutInflater f) {

		context = c;
		AlertDialog.Builder b = new AlertDialog.Builder(context);
		final ScrollView mLayout = (ScrollView) f.inflate(R.layout.sox_options, null);

		double d;
		int i, offset;

		speedChk = (CheckBox) mLayout.findViewById(R.id.soxSpeedChk);
		speedSeek = (SeekBar) mLayout.findViewById(R.id.speedSeek);
		speedVal = (EditText) mLayout.findViewById(R.id.speedValue);

		tempoChk = (CheckBox) mLayout.findViewById(R.id.soxTempoChk);
		tempoSeek = (SeekBar) mLayout.findViewById(R.id.tempoSeek);
		tempoVal = (EditText) mLayout.findViewById(R.id.tempoValue);

		pitchChk = (CheckBox) mLayout.findViewById(R.id.soxPitchChk);
		pitchSeek = (SeekBar) mLayout.findViewById(R.id.pitchSeek);
		pitchVal = (EditText) mLayout.findViewById(R.id.pitchValue);

		delayChk = (CheckBox) mLayout.findViewById(R.id.soxDelayChk);
		delayValL = (EditText) mLayout.findViewById(R.id.delayValueL);
		delayValR = (EditText) mLayout.findViewById(R.id.delayValueR);

		equivCmd = (TextView) mLayout.findViewById(R.id.equivSoxCmd);
		manCmd = (EditText) mLayout.findViewById(R.id.custSoxCmd);

		TextView.OnEditorActionListener focusClearer = new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					mLayout.requestFocus();
					InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(mLayout.getWindowToken(), 0);
				}
				return false;
			}
		};

		speedChk.setChecked(SettingsStorage.soxEnableSpeed);

		speedChk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				SettingsStorage.soxEnableSpeed = isChecked;
				speedSeek.setEnabled(isChecked);
				speedVal.setEnabled(isChecked);
				refreshCommandTxt();
			}
		});

		speedSeek.setEnabled(SettingsStorage.soxEnableSpeed);

		d = SettingsStorage.soxSpeedVal;
		if (d > 0)
		{
			if (d > speedVals[speedVals.length - 1]) {
				offset = speedVals.length - 1;
			} else if (d < speedVals[0]) {
				offset = 0;
			} else if (d < 1) {
				offset = ((int) (d * 10));
			} else {
				offset = ((int) d) + 9;
			}
			speedSeek.setProgress(offset);
			speedVal.setText(String.format(Locale.US, "%.4f",d));
		}

		speedSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if(fromUser && !speedFromSetter)
				{
					speedFromSetter = true;
					speedVal.setText(String.format(Locale.US, "%.4f",speedVals[progress]));
					refreshCommandTxt();
				}else{
					speedFromSetter = false;
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
		});

		speedVal.setEnabled(SettingsStorage.soxEnableSpeed);

		speedVal.setOnEditorActionListener(focusClearer);

		speedVal.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void afterTextChanged(Editable s) {
				if (!speedFromSetter) {
					if (s.length() > 0) {
						double d;
						try {
							d = Double.parseDouble(s.toString());
						} catch (NumberFormatException ignored) {
							return;
						}
						speedFromSetter = true;
						int offset;
						if (d > speedVals[speedVals.length - 1]) {
							offset = speedVals.length - 1;
						} else if (d < speedVals[0]) {
							offset = 0;
						} else if (d < 1) {
							offset = ((int) (d * 10));
						} else {
							offset = ((int) d) + 9;
						}
						speedSeek.setProgress(offset);
						speedFromSetter = false;
					}
					refreshCommandTxt();
				} else {
					speedFromSetter= false;
				}
			}
		});

		tempoChk.setChecked(SettingsStorage.soxEnableTempo);

		tempoChk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				SettingsStorage.soxEnableTempo = isChecked;
				tempoSeek.setEnabled(isChecked);
				tempoVal.setEnabled(isChecked);
				refreshCommandTxt();
			}
		});

		tempoSeek.setEnabled(SettingsStorage.soxEnableTempo);

		d = SettingsStorage.soxTempoVal;
		if (d > 0) {
			if (d > tempoVals[tempoVals.length - 1]) {
				offset = tempoVals.length - 1;
			} else if (d < tempoVals[0]) {
				offset = 0;
			} else if (d < 1) {
				offset = (int) (d * 10) - 1;
			} else {
				offset = ((int) d) + 10;
			}
			tempoSeek.setProgress(offset);
			tempoVal.setText(String.format(Locale.US, "%.4f",d));
		}

		tempoSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if(fromUser && !tempoFromSetter)
				{
					tempoFromSetter = true;
					tempoVal.setText(String.format(Locale.US, "%.4f",tempoVals[progress]));
					refreshCommandTxt();
				}else{
					tempoFromSetter = false;
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
		});

		tempoVal.setEnabled(SettingsStorage.soxEnableTempo);

		tempoVal.setOnEditorActionListener(focusClearer);

		tempoVal.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void afterTextChanged(Editable s) {
				if (!tempoFromSetter) {
					if (s.length() > 0) {
						double d;
						try{
							d = Double.parseDouble(s.toString());
						}catch(NumberFormatException ignored)
						{
							return;
						}
						tempoFromSetter = true;
						int offset;
						if(d>tempoVals[tempoVals.length-1])
						{
							offset = tempoVals.length-1;
						}else if(d<tempoVals[0])
						{
							offset = 0;
						}else if(d<1){
							offset = (int)(d*10)-1;
						}else{
							offset = ((int)d)+10;
						}
						tempoSeek.setProgress(offset);
						tempoFromSetter = false;
					}
					refreshCommandTxt();
				} else {
					tempoFromSetter= false;
				}
			}
		});

		pitchChk.setChecked(SettingsStorage.soxEnablePitch);

		pitchChk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				SettingsStorage.soxEnablePitch = isChecked;
				pitchSeek.setEnabled(isChecked);
				pitchVal.setEnabled(isChecked);
				refreshCommandTxt();
			}
		});

		pitchSeek.setEnabled(SettingsStorage.soxEnablePitch);

		i = SettingsStorage.soxPitchVal;
		if(i>pitchVals[pitchVals.length-1]){
			offset = pitchVals.length-1;
		}else if(i<pitchVals[0]){
			offset = 0;
		}else if(i<0){
			offset = (i+500)/100;
		}else{
			offset = (i/100)+5;
		}
		pitchSeek.setProgress(offset);

		pitchSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if(fromUser && !pitchFromSetter)
				{
					pitchFromSetter = true;
					pitchVal.setText(Integer.toString(pitchVals[progress]));
					refreshCommandTxt();
				}else{
					pitchFromSetter = false;
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
		});

		pitchVal.setEnabled(SettingsStorage.soxEnablePitch);

		pitchVal.setOnEditorActionListener(focusClearer);

		pitchVal.setText(Integer.toString(SettingsStorage.soxPitchVal));

		pitchVal.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void afterTextChanged(Editable s) {
				if (!pitchFromSetter) {
					if (s.length() > 0) {
						int i;
						try{
							i = Integer.parseInt(s.toString());
						}catch(NumberFormatException ignored)
						{
							return;
						}
						pitchFromSetter = true;
						int offset;
						if(i>pitchVals[pitchVals.length-1])
						{
							offset = pitchVals.length-1;
						}else if(i<pitchVals[0])
						{
							offset = 0;
						}else if(i<0){
							offset = (i+500)/100;
						}else{
							offset = (i/100)+5;
						}
						pitchSeek.setProgress(offset);
						pitchFromSetter = false;
					}
					refreshCommandTxt();
				} else {
					pitchFromSetter= false;
				}
			}
		});

		delayChk.setChecked(SettingsStorage.soxEnableDelay);

		delayChk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				SettingsStorage.soxEnableDelay = isChecked;
				delayValL.setEnabled(isChecked);
				delayValR.setEnabled(isChecked);
				refreshCommandTxt();
			}
		});

		TextWatcher delayWatcher = new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void afterTextChanged(Editable s) {
				refreshCommandTxt();
			}
		};

		delayValL.setEnabled(SettingsStorage.soxEnableDelay);

		delayValL.setOnEditorActionListener(focusClearer);

		delayValL.setText(String.format(Locale.US, "%.4f",SettingsStorage.soxDelayL));

		delayValL.addTextChangedListener(delayWatcher);

		delayValR.setEnabled(SettingsStorage.soxEnableDelay);

		delayValR.setOnEditorActionListener(focusClearer);

		delayValR.setText(String.format(Locale.US, "%.4f",SettingsStorage.soxDelayR));

		delayValR.addTextChangedListener(delayWatcher);

		manCmd.setOnEditorActionListener(focusClearer);

		if(SettingsStorage.soxManCmd!=null)
		{
			manCmd.setText(SettingsStorage.soxManCmd);
		}

		manCmd.addTextChangedListener(delayWatcher);

		b.setPositiveButton(c.getResources().getString(R.string.done), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {

			}
		});

		b.setNeutralButton("Save CFG", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
				SharedPreferences.Editor e = prefs.edit();
				e.putBoolean(Constants.sett_sox_speed, SettingsStorage.soxEnableSpeed);
				e.putFloat(Constants.sett_sox_speed_val, (float) SettingsStorage.soxSpeedVal);
				e.putBoolean(Constants.sett_sox_tempo, SettingsStorage.soxEnableTempo);
				e.putFloat(Constants.sett_sox_tempo_val, (float) SettingsStorage.soxTempoVal);
				e.putBoolean(Constants.sett_sox_pitch, SettingsStorage.soxEnablePitch);
				e.putInt(Constants.sett_sox_pitch_val, SettingsStorage.soxPitchVal);
				e.putBoolean(Constants.sett_sox_delay, SettingsStorage.soxEnableDelay);
				e.putFloat(Constants.sett_sox_delay_valL, (float) SettingsStorage.soxDelayL);
				e.putFloat(Constants.sett_sox_delay_valR, (float) SettingsStorage.soxDelayR);
				e.putString(Constants.sett_sox_mancmd, SettingsStorage.soxManCmd);

				e.putString(Constants.sett_sox_fullcmd, SettingsStorage.soxEffStr);
				e.commit();
			}
		});

		refreshCommandTxt();
		b.setTitle("SoX Effects");
		b.setView(mLayout);
		AlertDialog ddd = b.create();
		ddd.show();
	}

	private void refreshCommandTxt() {
		StringBuilder cmd = new StringBuilder();
		if(speedChk.isChecked())
		{
			try{
				double d = Double.parseDouble(speedVal.getText().toString());
				SettingsStorage.soxSpeedVal = d;
				if(d<0.9999 || d>1.0001) // Floats are cool
				{
					cmd.append("speed ");
					cmd.append(speedVal.getText().toString());
					cmd.append(';');
				}
			}catch(NumberFormatException ignored){}
		}
		if(tempoChk.isChecked())
		{
			try{
				double d = Double.parseDouble(tempoVal.getText().toString());
				SettingsStorage.soxTempoVal = d;
				if(d<0.9999 || d>1.0001) // Floats are cool
				{
					cmd.append("tempo ");
					cmd.append(tempoVal.getText().toString());
					cmd.append(';');
				}
			}catch(NumberFormatException ignored){}
		}
		if(pitchChk.isChecked())
		{
			try{
				int i = Integer.parseInt(pitchVal.getText().toString());
				SettingsStorage.soxPitchVal = i;
				if(i != 0)
				{
					cmd.append("pitch ");
					cmd.append(pitchVal.getText().toString());
					cmd.append(';');
				}
			}catch(NumberFormatException ignored){}
		}
		if(delayChk.isChecked())
		{
			double delayL = 0;
			try{
				delayL = Double.parseDouble(delayValL.getText().toString());
			}catch(NumberFormatException ignored){}
			double delayR = 0;
			try{
				delayR = Double.parseDouble(delayValR.getText().toString());
			}catch(NumberFormatException ignored){}

			SettingsStorage.soxDelayL  = delayL;
			SettingsStorage.soxDelayR = delayR;

			if(delayL>0.0001)
			{
				cmd.append("delay ");
				cmd.append(delayL);
				if(delayR>0.0001)
				{
					cmd.append(' ');
					cmd.append(delayR);
				}
				cmd.append(';');
			}else{
				if(delayR>0.0001)
				{
					cmd.append("delay 0 ");
					cmd.append(delayR);
					cmd.append(';');
				}
			}
		}
		if(cmd.length() == 0)
		{
			cmd.append("(empty)");
		}
		equivCmd.setText(cmd.toString());
		equivCmd.postInvalidate();

		SettingsStorage.soxManCmd = manCmd.getText().toString();
		String processed = equivCmd.getText().toString();
		if(processed.equals("(empty)"))
		{
			SettingsStorage.soxEffStr =  manCmd.getText().toString();
		}else{
			SettingsStorage.soxEffStr =  processed+manCmd.getText().toString();
		}
	}
}