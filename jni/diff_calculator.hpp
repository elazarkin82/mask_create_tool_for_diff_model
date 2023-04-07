/*
 * diff_calculator.hpp
 *
 *  Created on: 7 Apr 2023
 *      Author: elazarkin
 */

#ifndef JNI_DIFF_CALCULATOR_HPP_
#define JNI_DIFF_CALCULATOR_HPP_

#include <stdio.h>
#include <string.h>
#include <string>
#include <vector>

#define MIN(A, B) A > B ? B : A
#define MAX(A, B) A < B ? B : A

typedef unsigned char uchar;

typedef struct
{
	int x, y;
}Pixel;


void create_work_frames(std::vector<uchar*>&all_frames, int frame_index, int diff_frames_range, std::vector<uchar*>&work_frames)
{
	int start_index = MAX(0, frame_index - diff_frames_range);
	int end_index = start_index  + diff_frames_range;
	work_frames.empty();
	for(int i = start_index; i < end_index; i++)
		work_frames.push_back(all_frames[i]);
	fprintf(stderr, "work_frames size: %zu\n", work_frames.size());
}

void create_base_frame(uchar *base_frame, int w, int h, std::vector<uchar*>&work_frames)
{
	int look_range = 3;
	int look_range_best_index = look_range/2;
	int histogram[256];
	for(int i = 0; i < w*h; i++)
	{
		int min_value = 255, max_value = 0;
		memset(histogram, 0, sizeof(histogram));
		for(int frame_index = 0; frame_index < work_frames.size(); frame_index++)
		{
			min_value = MIN(min_value, work_frames[frame_index][i]);
			max_value = MAX(max_value, work_frames[frame_index][i]);
			histogram[work_frames[frame_index][i]]++;
		}

		if(max_value - min_value < look_range)
		{
			int best_value = histogram[min_value];
			int best_index = min_value;
			for(int j = min_value; j <= max_value; j++)
				if(histogram[i] > best_value)
				{
					best_value = histogram[j];
					best_index = j;
				}
			base_frame[i] = best_index;
		}
		else
		{
			int best_sum = 0;
			int sum = 0;
			int best_index;
			for(int j = min_value; j < min_value + look_range; j++)
				sum += histogram[j];

			best_sum = sum;
			best_index = min_value + look_range_best_index;
			for(int j = min_value + 1; j <= max_value - look_range; j++)
			{
				sum -= histogram[j-1];
				sum += histogram[j+look_range];
				if(sum > best_sum)
				{
					best_sum = sum;
					best_index = j + look_range_best_index;
				}
			}
			base_frame[i] = best_index;
		}
	}
}

#endif /* JNI_DIFF_CALCULATOR_HPP_ */
