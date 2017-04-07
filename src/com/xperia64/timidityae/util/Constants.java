/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.util;

@SuppressWarnings({"WeakerAccess", "unused"})
public class Constants {

	public static final String pkg_name = "com.xperia64.timidityae";
	// MusicService Broadcast Arguments
	private static final String msrv_PRFX = ".MusicService";
	private static final String msrv = pkg_name + msrv_PRFX;
	public static final String msrv_rec = msrv + "_RECEIVER";
	public static final String msrv_cmd = msrv + "_CMD";
	public static final String msrv_songnum = msrv + "_SONGNUM";
	public static final String msrv_currfold = msrv + "_CURRFOLD";
	public static final String msrv_begin = msrv + "_Begin";
	public static final String msrv_loopmode = msrv + "_LoopMode";
	public static final String msrv_shufmode = msrv + "_ShuffleMode";
	public static final String msrv_seektime = msrv + "_SeekTime";
	public static final String msrv_infile = msrv + "_InputFile";
	public static final String msrv_outfile = msrv + "_OutputFile";
	public static final String msrv_reset = msrv + "_Reset";
	public static final String msrv_cpplist = msrv + "_CopyPlist";

	// MusicService Broadcast Commands
	public static final int msrv_cmd_error = -5;
	public static final int msrv_cmd_load_plist_play = 0;
	public static final int msrv_cmd_play = 1;
	public static final int msrv_cmd_pause = 2;
	public static final int msrv_cmd_next = 3;
	public static final int msrv_cmd_prev = 4;
	public static final int msrv_cmd_stop = 5;
	public static final int msrv_cmd_loop_mode = 6;
	public static final int msrv_cmd_shuf_mode = 7;
	// public static final int msrv_cmd_req_time = 8; // Unused
	public static final int msrv_cmd_seek = 9;
	public static final int msrv_cmd_get_fold = 10;
	public static final int msrv_cmd_get_info = 11;
	public static final int msrv_cmd_get_plist = 12;
	public static final int msrv_cmd_play_or_pause = 13; // TODO: Used with the Widget
	public static final int msrv_cmd_write_new = 14;
	public static final int msrv_cmd_write_curr = 15;
	public static final int msrv_cmd_save_cfg = 16;
	public static final int msrv_cmd_load_cfg = 17;
	public static final int msrv_cmd_reload_libs = 18;

	// TimidityActivity Broadcast Arguments
	private static final String ta_PRFX = ".TimidityActivity";
	private static final String ta = pkg_name + ta_PRFX;
	public static final String ta_rec = ta + "_RECEIVER";
	public static final String ta_cmd = ta + "_CMD";
	public static final String ta_filename = ta + "_FileName";
	public static final String ta_startt = ta + "_StartTime";
	public static final String ta_songttl = ta + "_SongTitle";
	public static final String ta_currpath = ta + "_CurrPath";
	public static final String ta_shufmode = ta + "_ShuffleMode";
	public static final String ta_loopmode = ta + "_LoopMode";
	public static final String ta_pause = ta + "_Pause";
	public static final String ta_pausea = ta + "_PauseArg";
	public static final String ta_highlight = ta + "_Highlight";
	public static final String ta_en_play = ta+"_EnablePlay";

	// TimidityActivity Broadcast Commands
	public static final int ta_cmd_error = -5;
	public static final int ta_cmd_gui_play = 0;
	public static final int ta_cmd_refresh_filebrowser = 1;
	public static final int ta_cmd_load_filebrowser = 2;
	public static final int ta_cmd_gui_play_full = 3;
	public static final int ta_cmd_copy_plist = 4;
	public static final int ta_cmd_pause_stop = 5;
	public static final int ta_cmd_update_art = 6;
	// public static final int ta_cmd_unused_7 = 7;
	public static final int ta_cmd_special_notification_finished = 8;
	public static final int ta_cmd_service_started = 9;
	public static final int ta_cmd_sox_dialog = 10;

	// Settings names
	public static final String sett_first_run = "tplusFirstRun";
	public static final String sett_theme = "fbTheme";
	public static final String sett_show_hidden_files = "hiddenSwitch";
	public static final String sett_home_folder = "defaultPath";
	public static final String sett_data_folder = "dataDir";
	public static final String sett_man_config = "manualConfig";
	public static final String sett_default_resamp = "tplusResamp";
	public static final String sett_channel_mode = "sdlChanValue";
	public static final String sett_audio_rate = "tplusRate";
	public static final String sett_vol = "tplusVol";
	public static final String sett_buffer_size = "tplusBuff";
	public static final String sett_show_videos = "videoSwitch";
	public static final String sett_should_ext_storage_nag = "shouldLolNag";
	public static final String sett_keep_partal_wave = "keepPartialWave";
	public static final String sett_default_back_btn = "useDefBack";
	public static final String sett_compress_midi_cfg = "compressCfg";
	public static final String sett_reshuffle_plist = "reShuffle";
	public static final String sett_free_insts = "tplusUnload";
	public static final String sett_preserve_silence = "tplusSilKey";
	public static final String sett_native_midi = "nativeMidiSwitch";
	public static final String sett_fancy_plist = "fpSwitch";
	public static final String sett_soundfonts = "tplusSoundfonts";

	public static final String sett_t_verbosity = "timidityVerbosity";

	public static final String sett_native_media = "nativeMediaSwitch";

	public static final String sett_sox_speed = "soxSpeed";
	public static final String sett_sox_speed_val = "soxSpeedVal";
	public static final String sett_sox_tempo = "soxTempo";
	public static final String sett_sox_tempo_val = "soxTempoVal";
	public static final String sett_sox_pitch = "soxPitch";
	public static final String sett_sox_pitch_val = "soxPitchVal";
	public static final String sett_sox_delay = "soxDelay";
	public static final String sett_sox_delay_valL = "soxDelayValL";
	public static final String sett_sox_delay_valR = "soxDelayValR";
	public static final String sett_sox_mancmd = "soxManCmd";
	public static final String sett_sox_fullcmd = "soxFullCmd";
	public static final String sett_sox_unsafe = "unsafeSoxSwitch";


	// Fragment Keys
	public static final String currFoldKey = "CURRENT_FOLDER";
	public static final String currPlistDirectory = "CURRENT_PLIST_DIR";
	public static final String currPlistSearch = "CURRENT_PLIST_SEARCH";

	// controlTimidity commands
	public static final int jni_tim_none = 0;
	public static final int jni_tim_quit = 1;
	public static final int jni_tim_next = 2;
	public static final int jni_tim_previous = 3;
	public static final int jni_tim_forward = 4;
	public static final int jni_tim_back = 5;
	public static final int jni_tim_jump = 6;
	public static final int jni_tim_toggle_pause = 7;
	public static final int jni_tim_restart = 8;
	public static final int jni_tim_pause = 9;
	public static final int jni_tim_continue = 10;
	public static final int jni_tim_really_previous = 11;
	public static final int jni_tim_change_volume = 12;
	public static final int jni_tim_load_file = 13;
	public static final int jni_tim_tune_end = 14;
	public static final int jni_tim_keyup = 15;
	public static final int jni_tim_keydown = 16;
	public static final int jni_tim_speedup = 17;
	public static final int jni_tim_speeddown = 18;
	public static final int jni_tim_voiceincr = 19;
	public static final int jni_tim_voicedecr = 20;
	public static final int jni_tim_toggle_drumchan = 21;
	public static final int jni_tim_reload = 22;
	public static final int jni_tim_toggle_sndspec = 23;
	public static final int jni_tim_change_rev_effb = 24;
	public static final int jni_tim_change_rev_time = 25;
	public static final int jni_tim_sync_restart = 26;
	public static final int jni_tim_toggle_ctl_speana = 27;
	public static final int jni_tim_change_rate = 28;
	public static final int jni_tim_output_changed = 29;
	public static final int jni_tim_stop = 30;
	public static final int jni_tim_toggle_mute = 31;
	public static final int jni_tim_solo_play = 32;
	public static final int jni_tim_mute_clear = 33;
	public static final int jni_tim_holdmask = 0x800;
	public static final int jni_tim_unholdmask = 0x8000;




	/*
	 * RESAMPLE_CSPLINE, 0
		RESAMPLE_LAGRANGE, 1
		RESAMPLE_GAUSS, 2
		RESAMPLE_NEWTON, 3
		RESAMPLE_LINEAR, 4
		RESAMPLE_NONE 5
	 
	/*
	 * #define RC_ERROR	-1
	#ifdef RC_NONE
	#undef RC_NONE
	#endif
	#define RC_NONE		0
	#define RC_QUIT		1
	#define RC_NEXT		2
	#define RC_PREVIOUS	3 // Restart this song at beginning, or the previous
				     song if we're less than a second into this one. 
	#define RC_FORWARD	4
	#define RC_BACK		5
	#define RC_JUMP		6
	#define RC_TOGGLE_PAUSE 7	Pause/continue 
	#define RC_RESTART	8	/* Restart song at beginning 
	#define RC_PAUSE	9	/* Really pause playing 
	#define RC_CONTINUE	10	/* Continue if paused 
	#define RC_REALLY_PREVIOUS 11	/* Really go to the previous song 
	#define RC_CHANGE_VOLUME 12
	#define RC_LOAD_FILE	13	/* Load a new midifile 
	#define RC_TUNE_END	14	/* The tune is over, play it again sam? 
	#define RC_KEYUP	15	/* Key up 
	#define RC_KEYDOWN	16	/* Key down 
	#define RC_SPEEDUP	17	/* Speed up 
	#define RC_SPEEDDOWN	18	/* Speed down 
	#define RC_VOICEINCR	19	/* Increase voices 
	#define RC_VOICEDECR	20	/* Decrease voices 
	#define RC_TOGGLE_DRUMCHAN 21	/* Toggle drum channel 
	#define RC_RELOAD	22	/* Reload & Play 
	#define RC_TOGGLE_SNDSPEC 23	/* Open/Close Sound Spectrogram Window 
	#define RC_CHANGE_REV_EFFB 24
	#define RC_CHANGE_REV_TIME 25
	#define RC_SYNC_RESTART 26
	#define RC_TOGGLE_CTL_SPEANA 27
	#define RC_CHANGE_RATE	28
	#define RC_OUTPUT_CHANGED      29
	#define RC_STOP		30	/* Stop to play 
	#define RC_TOGGLE_MUTE	31
	#define RC_SOLO_PLAY	32
	#define RC_MUTE_CLEAR	33*/
}
