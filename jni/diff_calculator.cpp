#include <jni.h>

#include "diff_calculator.hpp"
#include "bmp_saver.hpp"

#include <regex>

extern "C" JNIEXPORT void JNICALL Java_main_MainWindow_initJni(
	JNIEnv *env, jobject thisObj,jobjectArray all_frame_paths, int width, int height
)
{
	int stringCount = env->GetArrayLength(all_frame_paths);
	fprintf(stderr, "start initJni!\n");
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
			int total_size = width*height;
			int readed_size = 0;
			while(readed_size < total_size)
				readed_size += fread(&bytes[readed_size], 1, total_size, file);
			fclose(file);
			all_frames.push_back(bytes);
		}
		env->ReleaseStringUTFChars(jfile_path, file_path);
	}

	base_frame = (uchar *) malloc(width*height);
	create_work_frames(all_frames, s_frame_index, s_diff_frames_range, work_frames);
	create_base_frame(base_frame, width, height, work_frames);

	ignore_areas_frame = (uchar *) malloc(width*height);
	memset(ignore_areas_frame, 0, width*height);
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
		s_frame_index = (s_frame_index + size + offset)%size;
	create_work_frames(all_frames, s_frame_index, s_diff_frames_range, work_frames);
	create_base_frame(base_frame, s_width, s_height, work_frames);
}

extern "C" JNIEXPORT void JNICALL Java_main_MainWindow_updateThreshJni(JNIEnv *env, jobject thisObj, jint thresh)
{
	s_thresh = thresh;
}

extern "C" JNIEXPORT jint JNICALL Java_main_MainWindow_getThreshJni(JNIEnv *env, jobject thisObj)
{
	return s_thresh;
}

extern "C" JNIEXPORT void JNICALL Java_main_MainWindow_updateIgnoreRadiusJni(JNIEnv *env, jobject thisObj, jint ignore_radius)
{
	s_ignore_radius = ignore_radius;
}

extern "C" JNIEXPORT void JNICALL Java_main_MainWindow_setMinBlobSizeJni(JNIEnv *env, jobject thisObj, jint min_blob_size)
{
	s_min_blob_size = min_blob_size;
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

extern "C" JNIEXPORT void JNICALL Java_main_MainWindow_loadIgnoreAreaMask(JNIEnv *env, jobject thisObj, jstring jout_path)
{
	const char *out_path = env->GetStringUTFChars(jout_path, 0);
	FILE *file;
	if((file=fopen(out_path, "rb")) != NULL)
	{
		int total_size = s_width*s_height;
		int readed_size = 0;
		while(readed_size < total_size)
			readed_size += fread(ignore_areas_frame, 1, total_size, file);
		fclose(file);
	}
	env->ReleaseStringUTFChars(jout_path, out_path);
}

extern "C" JNIEXPORT void JNICALL Java_main_MainWindow_saveIgnoreAreasMask(JNIEnv *env, jobject thisObj, jstring jout_path)
{
	const char *out_path = env->GetStringUTFChars(jout_path, 0);
	FILE *file;
	if((file=fopen(out_path, "wb")) != NULL)
	{
		fwrite(ignore_areas_frame, s_width*s_height, 1, file);
		fclose(file);
	}
	env->ReleaseStringUTFChars(jout_path, out_path);
}

extern "C" JNIEXPORT void JNICALL Java_main_MainWindow_saveAllItemMasksJni(JNIEnv *env, jobject thisObj)
{
	UcharMemoryGuard out_diff_mask(s_width*s_height);
	UcharMemoryGuard bmp_diff_mask(s_width*s_height*3);

	for(int i = 0; i < all_frames.size(); i++)
	{
		std::string bmp_out_path = std::regex_replace(all_frames_names[i], std::regex(".bin"), "_mask.bmp");
		create_work_frames(all_frames, i, s_diff_frames_range, work_frames);
		create_base_frame(base_frame, s_width, s_height, work_frames);
		create_diff_mask(all_frames[i], base_frame, out_diff_mask.data());

		for(int y = 0; y < s_height; y++)
			for(int x = 0; x < s_width; x++)
			{
				int j = y*s_width + x;
				int j2 = (s_height - 1 - y)*s_width + x;
				bmp_diff_mask[j2*3] = out_diff_mask[j];
				bmp_diff_mask[j2*3+1] = out_diff_mask[j];
				bmp_diff_mask[j2*3+2] = out_diff_mask[j];
			}

		bmp::saveBitmapImage(bmp_diff_mask.data(), s_width, s_height, 3, bmp_out_path.c_str());
	}
}

extern "C" JNIEXPORT void JNICALL Java_main_gui_DrawPanel_readBaseFrameBytesJni(JNIEnv *env, jobject thisObj, jbyteArray jframe_bytes)
{
	uchar *ptr = (uchar *)env->GetByteArrayElements(jframe_bytes, 0);
	memcpy(ptr, base_frame, s_width*s_height);
	env->ReleaseByteArrayElements(jframe_bytes, (jbyte*)ptr, 0);
}

extern "C" JNIEXPORT void JNICALL Java_main_gui_DrawPanel_readFrameBytesJni(JNIEnv *env, jobject thisObj, jbyteArray jframe_bytes)
{
	static const int IGNORE_AREA_OFFSET = -50;
	uchar *ptr = (uchar *)env->GetByteArrayElements(jframe_bytes, 0);
	memcpy(ptr, all_frames[s_frame_index], s_width*s_height);
	for(int i = 0; i < s_width*s_height; i++)
		if(ignore_areas_frame[i] == 255)
			ptr[i] += IGNORE_AREA_OFFSET;
//			ptr[i] = MAX(0, MIN(255, ptr[i] + IGNORE_AREA_OFFSET));
	env->ReleaseByteArrayElements(jframe_bytes, (jbyte*)ptr, 0);
}

extern "C" JNIEXPORT void JNICALL Java_main_gui_DrawPanel_readDiffFrameBytesJni(JNIEnv *env, jobject thisObj, jbyteArray jframe_bytes)
{
	uint64_t t0 = getUseconds();
	uchar *ptr = (uchar *)env->GetByteArrayElements(jframe_bytes, 0);
	create_diff_mask(all_frames[s_frame_index], base_frame, ptr);
	env->ReleaseByteArrayElements(jframe_bytes, (jbyte*)ptr, 0);
	fprintf(stderr, "time calc diff take %3.4f secs\n", (getUseconds() - t0)/1000000.0f);
}

extern "C" JNIEXPORT void JNICALL Java_main_gui_DrawPanel_addIgnoreAreaJni(JNIEnv *env, jobject thisObj, jint jx, jint jy)
{
	set_ignore_area_frame_values_in_radius(ignore_areas_frame, jx, jy, s_width, s_height, s_ignore_radius, 255);
}

extern "C" JNIEXPORT void JNICALL Java_main_gui_DrawPanel_removeIgnoreAreaJni(JNIEnv *env, jobject thisObj, int jx, int jy)
{
	set_ignore_area_frame_values_in_radius(ignore_areas_frame, jx, jy, s_width, s_height, s_ignore_radius, 0);
}
