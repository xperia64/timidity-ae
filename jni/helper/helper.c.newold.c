#include <stdio.h>
#include <jni.h>
#include <android/log.h>
extern void timidity_start_initialize(void);
extern int timidity_pre_load_configuration(void);
extern int timidity_post_load_configuration(void);
extern void timidity_init_player(void);
extern int timidity_play_main(int nfiles, char **files);
extern int got_a_configuration;
//extern int set_tim_opt_long(int c, char *optarg, int index);
char* configFile;
char* configFile2;
JavaVM  *jvm;
JNIEnv *theGoodEnv;
int controlCode=0;
int controlArg=-1;
JNIEXPORT int JNICALL 
Java_com_xperia64_timidityae20_MainActivity_prepare(JNIEnv * env, jobject  obj, jstring config, jstring config2)
{
	(*env)->GetJavaVM(env, &jvm);
		jboolean isCopy;
	configFile=(*env)->GetStringUTFChars(env, config, &isCopy); 
	configFile2=(*env)->GetStringUTFChars(env, config2, &isCopy); 
	int err;
	 __android_log_print(ANDROID_LOG_DEBUG, "TIMIDITY", "Called RunIt!");
    timidity_start_initialize();

	__android_log_print(ANDROID_LOG_DEBUG, "TIMIDITY", "Initing Timidity!");


    if ((err = timidity_pre_load_configuration()) != 0)
	return err;
	__android_log_print(ANDROID_LOG_DEBUG, "TIMIDITY", "No preload err!");
    err += timidity_post_load_configuration();

    if (err) {
		__android_log_print(ANDROID_LOG_DEBUG, "TIMIDITY", "Couldn't load config! (who cares)");
	printf("couldn't load configuration file\n");
	// return 1;
    }
	__android_log_print(ANDROID_LOG_ERROR, "TIMIDITY", "No postload err!");
    timidity_init_player();
	__android_log_print(ANDROID_LOG_DEBUG, "TIMIDITY", "Initing Player!");
	(*env)->ReleaseStringUTFChars(env, config, configFile);
	(*env)->ReleaseStringUTFChars(env, config2, configFile2);
	return 0;
}
JNIEXPORT int JNICALL 
Java_com_xperia64_timidityae20_MainActivity_mainzes(JNIEnv * env, jobject  obj, jstring song)
{
	
(*jvm)->AttachCurrentThread(jvm, &theGoodEnv,NULL);
	int main_ret;
    		jboolean isCopy;
	char** filez = NULL;
	filez = malloc(sizeof(char*) * 1);

	filez[0]=(*env)->GetStringUTFChars(env, song, &isCopy); ;

	main_ret = timidity_play_main(1, filez);
	__android_log_print(ANDROID_LOG_DEBUG, "TIMIDITY", "Playing!");
	(*env)->ReleaseStringUTFChars(env, song, filez[0]);
    return main_ret;

}

char* getConfig()
{
	return configFile;
}
char* getConfig2()
{
	return configFile2;
}
int nativePush(char* buf, int nframes)
{
	
	jclass clazz = (*theGoodEnv)->FindClass(theGoodEnv, "com/xperia64/timidityae20/MainActivity");
	jmethodID buffit = (*theGoodEnv)->GetStaticMethodID(theGoodEnv, clazz, "buffit", "([BI)V");
	jbyteArray byteArr = (*theGoodEnv)->NewByteArray(theGoodEnv, nframes+1);
	(*theGoodEnv)->SetByteArrayRegion(theGoodEnv, byteArr , 0, nframes, (jbyte *)buf);
	(*theGoodEnv)->CallStaticVoidMethod(theGoodEnv, clazz, buffit, byteArr, nframes);
	(*theGoodEnv)->DeleteLocalRef(theGoodEnv, byteArr);
	(*theGoodEnv)->DeleteLocalRef(theGoodEnv, clazz);
	
	return 0;
}
JNIEXPORT void JNICALL 
Java_com_xperia64_timidityae20_MainActivity_control(JNIEnv*env, jobject obj, jint jcmd, jint jcmdArg)
{
	controlCode = (int)jcmd;
	controlArg=(int)jcmdArg;
}
int pollForControl()
{
	int tmp = controlCode;
	controlCode=0;
	return tmp;
}
int getControlArg()
{
	int tmp = controlArg;
	controlArg=-1;
	return tmp;
}
void finishHim()
{
	
	//exit(0); //? do nothing
}
