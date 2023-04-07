#include <jni.h>
#include <stdio.h>
#include <vector>

int s_frame_index = 10;
std::vector<char *> all_frames;


extern "C" JNIEXPORT void JNICALL Java_main_gui_DrawPanel_testHello(JNIEnv *env, jobject thisObj)
{
   fprintf(stderr, "Hello World!\n");
}

extern "C" JNIEXPORT void JNICALL Java_main_MainWindow_initJni(
	JNIEnv *env, jobject thisObj,jobjectArray all_frame_paths
)
{

}

extern "C" JNIEXPORT void JNICALL Java_main_MainWindow_deinitJni()
{
//	TODO
}

extern "C" JNIEXPORT jint JNICALL Java_main_MainWindow_getFrameIndexJni()
{
	return s_frame_index;
}

extern "C" JNIEXPORT void JNICALL Java_main_MainWindow_moveFramesIndexJni(jint offset)
{
	int size = all_frames.size();
	if(size != 0)
	{
		s_frame_index = (s_frame_index + size + offset)%size;
	}
}
