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
#include <math.h>
#include <string>
#include <vector>

#define MIN(A, B) A > B ? B : A
#define MAX(A, B) A < B ? B : A
#define POW2(A) (A)*(A)

typedef unsigned char uchar;

typedef struct
{
	int x, y;
}Pixel;

class UcharMemoryGuard
{
private:
	size_t m_size;
	uchar *m_data;
public:
	UcharMemoryGuard(size_t size)
	{
		m_data = (uchar *) malloc(size);
		m_size = size;
	}
	virtual ~UcharMemoryGuard()
	{
		free(m_data);
	}
	uchar *data() {return m_data;};
	uchar & operator [](int index) {return m_data[index];}
};


void create_work_frames(std::vector<uchar*>&all_frames, int frame_index, int diff_frames_range, std::vector<uchar*>&work_frames)
{
	int start_index = MAX(0, frame_index - diff_frames_range);
	int end_index = start_index  + diff_frames_range;
	work_frames.empty();
	for(int i = start_index; i < end_index; i++)
		work_frames.push_back(all_frames[i]);
	fprintf(stderr, "work_frames size: %zu\n", work_frames.size());
}

inline int _calc_best_index(int min_index, int range, int *histograms)
{
	float sum = 0.0;
	float total_weight = 0.0;
	for(int i = min_index; i < min_index + range; i++)
	{
		sum += i*histograms[i];
		total_weight += histograms[i];
	}
	return roundf(sum/total_weight);
}

void create_base_frame(uchar *base_frame, int w, int h, std::vector<uchar*>&work_frames)
{
	int look_range = 3;
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
			best_index = _calc_best_index(min_value, look_range, histogram);
			for(int j = min_value + 1; j <= max_value - look_range; j++)
			{
				sum -= histogram[j-1];
				sum += histogram[j+look_range];
				if(sum > best_sum)
				{
					best_sum = sum;
					best_index = _calc_best_index(j, look_range, histogram);
				}
			}
			base_frame[i] = best_index;
		}
	}
}

void calculate_simple_diff(uchar *frame, uchar *base_frame, int w, int h, int tresh, uchar *out)
{
	for(int i = 0; i < w*h; i++)
		out[i] = abs((int)base_frame[i] - (int)frame[i]) > tresh ? 255 : 0;
}

void set_ignore_area_frame_values_in_radius(uchar *ignore_areas_frame, int jx, int jy, int w, int h, int ignore_radius, uchar value)
{
	int min_x = MAX(0, jx - ignore_radius);
	int max_x = MIN(w, jx + ignore_radius);
	int min_y = MAX(0, jy - ignore_radius);
	int max_y = MIN(h, jy + ignore_radius);

	for(int y = min_y; y < max_y; y++)
		for(int x = min_x; x< max_x; x++)
		{
			float radius = fsqrt(POW2(jx - x) + POW2(jy - y));
			if(radius < ignore_radius)
				ignore_areas_frame[y*w + x] = value;
		}
}

void remove_ingore_areas(uchar *ignore_areas_frame, uchar *diff_frame, int w, int h)
{
	for(int i = 0; i < w*h; i++)
		if(ignore_areas_frame[i] == 255)
			diff_frame[i] = 0;
}

#endif /* JNI_DIFF_CALCULATOR_HPP_ */
