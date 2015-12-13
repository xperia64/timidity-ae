package com.xperia64.timidityae.util;

public class CommandStrings {
	// MusicService Broadcast Arguments
	public static final String msrv_rec = "com.xperia64.timidityae.MusicService_RECEIVER";
	public static final String msrv_cmd = "com.xperia64.timidityae.MusicService_CMD";
	public static final String msrv_songnum = "com.xperia64.timidityae.MusicService_SONGNUM";
	public static final String msrv_currfold = "com.xperia64.timidityae.MusicService_CURRFOLD";
	public static final String msrv_begin = "com.xperia64.timidityae.MusicService_Begin";
	public static final String msrv_loopmode = "com.xperia64.timidityae.MusicService_LoopMode";
	public static final String msrv_shufmode = "com.xperia64.timidityae.MusicService_ShuffleMode";
	public static final String msrv_seektime = "com.xperia64.timidityae.MusicService_SeekTime";
	public static final String msrv_infile = "com.xperia64.timidityae.MusicService_InputFile";
	public static final String msrv_outfile = "com.xperia64.timidityae.MusicService_OutputFile";
	public static final String msrv_reset = "com.xperia64.timidityae.MusicService_Reset";
	public static final String msrv_dlplist = "com.xperia64.timidityae.MusicService_DontLoadPlist";

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
	public static final String ta_rec = "com.xperia64.timidityae.TimidityActivity_RECEIVER";
	public static final String ta_cmd = "com.xperia64.timidityae.TimidityActivity_CMD";
	public static final String ta_filename = "com.xperia64.timidityae.TimidityActivity_FileName";
	public static final String ta_startt = "com.xperia64.timidityae.TimidityActivity_StartTime";
	public static final String ta_songttl = "com.xperia64.timidityae.TimidityActivity_SongTitle";
	public static final String ta_currpath = "com.xperia64.timidityae.TimidityActivity_CurrPath";
	public static final String ta_shufmode = "com.xperia64.timidityae.TimidityActivity_ShuffleMode";
	public static final String ta_loopmode = "com.xperia64.timidityae.TimidityActivity_LoopMode";
	public static final String ta_pause = "com.xperia64.timidityae.TimidityActivity_Pause";
	public static final String ta_pausea = "com.xperia64.timidityae.TimidityActivity_PauseArg";

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

	// Fragment Keys
	public static final String currFoldKey = "CURRENT_FOLDER";
	public static final String currPlistDirectory = "CURRENT_PLIST_DIR";

}
