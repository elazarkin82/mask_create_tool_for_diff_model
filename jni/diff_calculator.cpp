#include <jni.h>

#include "diff_calculator.hpp"

int s_width = 0, s_height = 0;
int s_frame_index = 0;
std::vector<std::string> all_frames_names;
std::vector<uchar *> all_frames;
std::vector<uchar *> work_frames;
int s_tresh = 0;
int s_ignore_radius;
int s_diff_frames_range;

uchar *base_frame;
std::vector<Pixel> ignore_pixels;

extern "C" JNIEXPORT void JNICALL Java_main_MainWindow_initJni(
	JNIEnv *env, jobject thisObj,jobjectArray all_frame_paths, int width, int height
)
{
	int stringCount = env->GetArrayLength(all_frame_paths);
	s_width = width;
	s_height = height;
	for (int i = 0; i < stringCount; i++)
	{
		FILE *file;
		jstring jfile_path = (jstring) (env->GetObjectArrayElement(all_frame_paths, i));
		const char *file_path = env->GetStringUTFChars(jfile_path, 0);
		all_frames_names.push_back(std::string(file_path));
		if((file=fopen(file_path, "rb")) != NULL)
		{
			uchar *bytes = (uchar *) malloc(width*height);
			fread(bytes, width*height, 1, file);
			fclose(file);
			all_frames.push_back(bytes);
		}
		env->ReleaseStringUTFChars(jfile_path, file_path);
	}

	base_frame = (uchar *) malloc(width*height);
	create_work_frames(all_frames, s_frame_index, s_diff_frames_range, work_frames);
	create_base_frame(base_frame, width, height, work_frames);
}

extern "C" JNIEXPORT void JNICALL Java_main_listeners_MainWindowListener_deinitJni(JNIEnv *env, jobject thisObj)
{
	for(int i = 0; i < all_frames.size(); i++)
		free(all_frames[i]);

	free(base_frame);
}

extern "C" JNIEXPORT jint JNICALL Java_main_MainWindow_getFrameIndexJni(JNIEnv *env, jobject thisObj)
{
	return s_frame_index;
}

extern "C" JNIEXPORT void JNICALL Java_main_MainWindow_moveFramesIndexJni(JNIEnv *env, jobject thisObj, jint offset)
{
	int size = all_frames.size();
	if(size != 0)
	{
		s_frame_index = (s_frame_index + size + offset)%size;
	}
}

extern "C" JNIEXPORT void JNICALL Java_main_MainWindow_updateTreshJni(JNIEnv *env, jobject thisObj, jint thresh)
{
	s_tresh = thresh;
}

extern "C" JNIEXPORT jint JNICALL Java_main_MainWindow_getTreshJni(JNIEnv *env, jobject thisObj)
{
	return s_tresh;
}

extern "C" JNIEXPORT void JNICALL Java_main_MainWindow_updateIgnoreRadiusJni(JNIEnv *env, jobject thisObj, jint ignore_radius)
{
	s_ignore_radius = ignore_radius;
}

extern "C" JNIEXPORT jint JNICALL Java_main_MainWindow_getIgnoreRadiusJni(JNIEnv *env, jobject thisObj)
{
	return s_ignore_radius;
}

extern "C" JNIEXPORT void JNICALL Java_main_MainWindow_setDiffFramesRangeJni(JNIEnv *env, jobject thisObj, jint diff_frames_range)
{
	fprintf(stderr, "set DiffFramesRange: %d\n", diff_frames_range);
	s_diff_frames_range = diff_frames_range;
}

extern "C" JNIEXPORT void JNICALL Java_main_gui_DrawPanel_readBaseFrameBytesJni(JNIEnv *env, jobject thisObj, jbyteArray jframe_bytes)
{
	uchar *ptr = (uchar *)env->GetByteArrayElements(jframe_bytes, 0);
	memcpy(ptr, base_frame, s_width*s_height);
	env->ReleaseByteArrayElements(jframe_bytes, (jbyte*)ptr, 0);
}

extern "C" JNIEXPORT void JNICALL Java_main_gui_DrawPanel_readFrameBytesJni(JNIEnv *env, jobject thisObj, jbyteArray jframe_bytes)
{
	uchar *ptr = (uchar *)env->GetByteArrayElements(jframe_bytes, 0);
	memcpy(ptr, all_frames[s_frame_index], s_width*s_height);
	env->ReleaseByteArrayElements(jframe_bytes, (jbyte*)ptr, 0);
}
