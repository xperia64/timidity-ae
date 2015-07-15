#include <stdio.h>
#include <jni.h>
//#include <android/log.h>
#include "timidity.h"

extern int skfl_Decode(const char *InFileName, const char *ReqOutFileName);
extern void timidity_start_initialize(void);
extern int timidity_pre_load_configuration(void);
extern int timidity_post_load_configuration(void);
extern void timidity_init_player(void);
extern int timidity_play_main(int nfiles, char **files);
extern int play_list(int number_of_files, char *list_of_files[]);
extern int set_current_resampler(int type);
extern void midi_program_change(int ch, int prog); // HAX
extern void midi_volume_change(int ch, int prog); // MAOR HAX
extern int droid_rc;
extern int droid_arg;
extern int got_a_configuration;
extern FLOAT_T midi_time_ratio;

char* configFile;
char* configFile2;
int controlCode=0;
int controlArg=0;
int sixteen;
int mono;
int itIsDone=0;
//JNIEnv* envelope;
JavaVM  *jvm;
JNIEnv *theGoodEnv;
jclass pushClazz;
jmethodID pushBuffit;
jmethodID flushId;
jmethodID buffId;
jmethodID controlId;
jmethodID rateId;
jmethodID finishId;
jmethodID seekInitId;
jmethodID updateSeekId;
jmethodID pushLyricId;
jmethodID updateMaxChanId;
jmethodID updateProgId;
jmethodID updateVolId;
jmethodID updateDrumId;
jmethodID updateTempoId;
JNIEXPORT int JNICALL 
Java_com_xperia64_timidityae20_JNIHandler_prepareTimidity(JNIEnv * env, jobject  obj, jstring config, jstring config2, jint jmono, jint jcustResamp, jint jsixteen)
{
	mono = (int)jmono;
	sixteen = (int)jsixteen;
	//envelope = env;
	(*env)->GetJavaVM(env, &jvm);
		jboolean isCopy;
	configFile=(char*)(*env)->GetStringUTFChars(env, config, &isCopy); 
	configFile2=(char*)(*env)->GetStringUTFChars(env, config2, &isCopy); 
	int err;
	 //__android_log_print(ANDROID_LOG_DEBUG, "TIMIDITY", "Called RunIt!");
    timidity_start_initialize();

	//__android_log_print(ANDROID_LOG_DEBUG, "TIMIDITY", "Initing Timidity!");


    if ((err = timidity_pre_load_configuration()) != 0)
	return err;
	//__android_log_print(ANDROID_LOG_DEBUG, "TIMIDITY", "No preload err!");
    err += timidity_post_load_configuration();

    if (err) {
		//__android_log_print(ANDROID_LOG_DEBUG, "TIMIDITY", "Couldn't load config! (who cares)");
	//printf("couldn't load configuration file\n");
	 return -121;
    }
	//__android_log_print(ANDROID_LOG_ERROR, "TIMIDITY", "No postload err!");
    timidity_init_player();
	set_current_resampler(jcustResamp);
	//__android_log_print(ANDROID_LOG_DEBUG, "TIMIDITY", "Initing Player!");
	
	(*env)->ReleaseStringUTFChars(env, config, configFile);
	(*env)->ReleaseStringUTFChars(env, config2, configFile2);
	return 0;
}
void setMaxChannels(int ca)
{
	(*theGoodEnv)->CallStaticVoidMethod(theGoodEnv, pushClazz, updateMaxChanId, ca);
}
void finishHim()
{
	(*theGoodEnv)->CallStaticVoidMethod(theGoodEnv, pushClazz, finishId);
	//exit(0); //? do nothing
}
JNIEXPORT int JNICALL 
Java_com_xperia64_timidityae20_JNIHandler_loadSongTimidity(JNIEnv * env, jobject  obj, jstring song)
{
	// It would appear we have to do the following code every time a song is loaded
	// Don't you just love JNI?
	(*jvm)->AttachCurrentThread(jvm, &theGoodEnv, NULL);
	pushClazz=(*theGoodEnv)->FindClass(theGoodEnv, "com/xperia64/timidityae20/JNIHandler");
	pushBuffit=(*theGoodEnv)->GetStaticMethodID(theGoodEnv, pushClazz, "buffit", "([BI)V");
	flushId=(*theGoodEnv)->GetStaticMethodID(theGoodEnv, pushClazz, "flushIt", "()V");
	buffId=(*theGoodEnv)->GetStaticMethodID(theGoodEnv, pushClazz, "bufferSize", "()I");
	controlId=(*theGoodEnv)->GetStaticMethodID(theGoodEnv, pushClazz, "controlMe", "(I)V");
	buffId=(*theGoodEnv)->GetStaticMethodID(theGoodEnv, pushClazz, "bufferSize", "()I");
	rateId=(*theGoodEnv)->GetStaticMethodID(theGoodEnv, pushClazz, "getRate", "()I");
	finishId=(*theGoodEnv)->GetStaticMethodID(theGoodEnv, pushClazz, "finishIt", "()V");
	seekInitId=(*theGoodEnv)->GetStaticMethodID(theGoodEnv, pushClazz, "initSeeker", "(I)V");
	updateSeekId=(*theGoodEnv)->GetStaticMethodID(theGoodEnv, pushClazz, "updateSeeker", "(I)V");
	pushLyricId=(*theGoodEnv)->GetStaticMethodID(theGoodEnv, pushClazz, "updateLyrics", "([B)V");
	updateMaxChanId=(*theGoodEnv)->GetStaticMethodID(theGoodEnv, pushClazz, "updateMaxChannels", "(I)V");
	updateProgId=(*theGoodEnv)->GetStaticMethodID(theGoodEnv, pushClazz, "updateProgramInfo", "(II)V");
	updateVolId=(*theGoodEnv)->GetStaticMethodID(theGoodEnv, pushClazz, "updateVolInfo", "(II)V");
	updateDrumId=(*theGoodEnv)->GetStaticMethodID(theGoodEnv, pushClazz, "updateDrumInfo", "(II)V");
	updateTempoId=(*theGoodEnv)->GetStaticMethodID(theGoodEnv, pushClazz, "updateTempo", "(II)V");
	// Must be called once to open output. Thank you mac_main for the NULL file list thing	
	if(!itIsDone)
	{
		setMaxChannels((int)MAX_CHANNELS);
		timidity_play_main(0, NULL);
		itIsDone=1;
	}
	int main_ret;
	char *filez[1];
	jboolean isCopy;
	//filez = malloc(sizeof(char*) * 1);
	filez[0]=(char*)(*env)->GetStringUTFChars(env, song, &isCopy);
	//main_ret = timidity_play_main(1, filez);
	//__android_log_print(ANDROID_LOG_DEBUG, "TIMIDITY", "Don't froob it");
	play_list(1,filez);
	//__android_log_print(ANDROID_LOG_DEBUG, "TIMIDITY", "Didn't froob it");
	finishHim();
	(*env)->ReleaseStringUTFChars(env, song, filez[0]);
	(*theGoodEnv)->DeleteLocalRef(theGoodEnv, pushClazz);
    return 0;

}
JNIEXPORT int JNICALL 
Java_com_xperia64_timidityae20_JNIHandler_setResampleTimidity(JNIEnv * env, jobject  obj, jint jcustResamp)
{
	set_current_resampler(jcustResamp);
}

JNIEXPORT int JNICALL 
Java_com_xperia64_timidityae20_JNIHandler_decompressSFArk(JNIEnv * env, jobject  obj, jstring jfrom, jstring jto)
{
	jboolean isCopy;
	const char* from = (*env)->GetStringUTFChars(env, jfrom, &isCopy); 
	const char* to = (*env)->GetStringUTFChars(env, jto, &isCopy); 
	int x = sfkl_Decode(from, to);
	(*env)->ReleaseStringUTFChars(env, jfrom, from);
	(*env)->ReleaseStringUTFChars(env, jto, to);
	return x;
}

JNIEXPORT void JNICALL 
Java_com_xperia64_timidityae20_JNIHandler_setChannelTimidity(JNIEnv * env, jobject  obj, jint jchan, jint jprog)
{
	midi_program_change((int)jchan, (int)jprog);
}
JNIEXPORT void JNICALL 
Java_com_xperia64_timidityae20_JNIHandler_setChannelVolumeTimidity(JNIEnv * env, jobject  obj, jint jchan, jint jvol)
{
	midi_volume_change((int)jchan, (int)jvol);
}
char* getConfig()
{
	//__android_log_print(ANDROID_LOG_DEBUG, "TIMIDITY", "%s", configFile);
	return configFile;
}
char* getConfig2()
{
	//__android_log_print(ANDROID_LOG_DEBUG, "TIMIDITY", "%s", configFile2);
	return configFile2;
}

int nativePush(char* buf, int nframes)
{
	
	//jclass clazz = (*theGoodEnv)->FindClass(theGoodEnv, "com/xperia64/timidityae20/JNIHandler");
	//jclass cls = (*envelope)->GetObjectClass(envelope, mine);

	//jmethodID buffit = (*theGoodEnv)->GetStaticMethodID(theGoodEnv, clazz, "buffit", "([BI)V");
	jbyteArray byteArr = (*theGoodEnv)->NewByteArray(theGoodEnv, nframes);
	(*theGoodEnv)->SetByteArrayRegion(theGoodEnv, byteArr , 0, nframes, (jbyte *)buf);
	(*theGoodEnv)->CallStaticVoidMethod(theGoodEnv, pushClazz, pushBuffit, byteArr, nframes);
	(*theGoodEnv)->DeleteLocalRef(theGoodEnv, byteArr);
	//(*theGoodEnv)->DeleteLocalRef(theGoodEnv, clazz);
	
	return 0;
}
JNIEXPORT void JNICALL 
Java_com_xperia64_timidityae20_JNIHandler_controlTimidity(JNIEnv*env, jobject obj, jint jcmd, jint jcmdArg)
{
	droid_rc=(int)jcmd;
	droid_arg=(int)jcmdArg;
	if(droid_rc==6) // When else are samples even used w/JNI?
	{
		droid_arg*=(midi_time_ratio)*getSampleRate();
	}
}
JNIEXPORT void JNICALL 
Java_com_xperia64_timidityae20_JNIHandler_mono(JNIEnv*env, jobject obj, jint jmono)
{
	mono=(int)jmono;
	//setMono(mono);
}
JNIEXPORT int JNICALL 
Java_com_xperia64_timidityae20_JNIHandler_timidityReady(JNIEnv*env, jobject obj)
{
	return controlCode;
}
void flushIt()
{
	//jclass clazz = (*theGoodEnv)->FindClass(theGoodEnv, "com/xperia64/timidityae20/JNIHandler");
	//jmethodID buffit = (*theGoodEnv)->GetStaticMethodID(theGoodEnv, pushClazz, "flushIt", "()V");
	(*theGoodEnv)->CallStaticIntMethod(theGoodEnv, pushClazz, flushId);
	//(*theGoodEnv)->DeleteLocalRef(theGoodEnv, clazz);
}
int getBuffer()
{
	//jclass clazz = (*theGoodEnv)->FindClass(theGoodEnv, "com/xperia64/timidityae20/JNIHandler");
	//jmethodID buffit = (*theGoodEnv)->GetStaticMethodID(theGoodEnv, clazz, "bufferSize", "()I");
	int r = (int)(*theGoodEnv)->CallStaticIntMethod(theGoodEnv, pushClazz, buffId);
	//(*theGoodEnv)->DeleteLocalRef(theGoodEnv, clazz);
	return r;
}
int getMono()
{
	return mono;
}
int getSixteen()
{
	return sixteen;
}
/*int pollForControl()
{
	int tmp = controlCode;
	controlCode=0;
	return tmp;
}
void setControl(int x)
{
	controlCode = x;
}
int getControlArg()
{
	int tmp = controlArg;
	controlArg=0;
	return tmp;
}*/
void setMaxTime(int time)
{
	(*theGoodEnv)->CallStaticVoidMethod(theGoodEnv, pushClazz, seekInitId, time);
}

void setCurrTime(int time)
{
	(*theGoodEnv)->CallStaticVoidMethod(theGoodEnv, pushClazz, updateSeekId, time);
}
void controller(int aa)
{
	//jclass clazz = (*theGoodEnv)->FindClass(theGoodEnv, "com/xperia64/timidityae20/JNIHandler");
	//jclass cls = (*envelope)->GetObjectClass(envelope, mine);
	//jmethodID buffit = (*theGoodEnv)->GetStaticMethodID(theGoodEnv, clazz, "controlMe", "(I)V");
	(*theGoodEnv)->CallStaticVoidMethod(theGoodEnv, pushClazz, controlId, aa);
	//(*theGoodEnv)->DeleteLocalRef(theGoodEnv, clazz);
}
int getSampleRate()
{
	return (*theGoodEnv)->CallStaticIntMethod(theGoodEnv, pushClazz, rateId);
}

void setCurrLyric(char* lyric)
{
		jbyteArray byteArr = (*theGoodEnv)->NewByteArray(theGoodEnv, 300);
	(*theGoodEnv)->SetByteArrayRegion(theGoodEnv, byteArr , 0, 300, (jbyte *)lyric);
	(*theGoodEnv)->CallStaticVoidMethod(theGoodEnv, pushClazz, pushLyricId, byteArr, 300);
	(*theGoodEnv)->DeleteLocalRef(theGoodEnv, byteArr);
}

void setProgram(int ch, int prog)
{
	(*theGoodEnv)->CallStaticVoidMethod(theGoodEnv, pushClazz, updateProgId, ch, prog);
}
void setVol(int ch, int vol)
{
	(*theGoodEnv)->CallStaticVoidMethod(theGoodEnv, pushClazz, updateVolId, ch, vol);
}
void setDrum(int ch, int isDrum)
{
	(*theGoodEnv)->CallStaticVoidMethod(theGoodEnv, pushClazz, updateDrumId, ch, isDrum);
}
void sendTempo(int t, int tr)
{
	(*theGoodEnv)->CallStaticVoidMethod(theGoodEnv, pushClazz, updateTempoId, t, tr);
}