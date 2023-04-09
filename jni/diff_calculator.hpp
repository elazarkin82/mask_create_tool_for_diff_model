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

#include <stdint.h>
#include <sys/time.h>

#define MIN(A, B) A > B ? B : A
#define MAX(A, B) A < B ? B : A
#define POW2(A) (A)*(A)

typedef unsigned char uchar;

struct Pixel
{
	Pixel(int _x, int _y): x(_x), y(_y){}
	int x, y;
};

inline uint64_t getUseconds()
{
	struct timeval tp;
	gettimeofday(&tp, NULL);
	return tp.tv_sec * 1000000 + tp.tv_usec;
}

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
	UcharMemoryGuard(uchar *data, size_t size)
	{
		m_data = (uchar *) malloc(size);
		memcpy(m_data, data, size);
		m_size = size;
	}
	virtual ~UcharMemoryGuard()
	{
		free(m_data);
	}
	uchar *data() {return m_data;};
	uchar & operator [](int index) {return m_data[index];}
};

static const int X_OFFSETS[] = {-1, 0, 1,-1, 1,-1, 0, 1};
static const int Y_OFFSETS[] = {-1,-1,-1, 0, 0, 1, 1, 1};
static const int OFFSETS_SIZE = sizeof(X_OFFSETS)/sizeof(int);


void create_work_frames(std::vector<uchar*>&all_frames, int frame_index, int diff_frames_range, std::vector<uchar*>&work_frames)
{
	int start_index = MAX(0, frame_index - diff_frames_range);
	int end_index = start_index  + diff_frames_range;
	work_frames.clear();
	for(int i = start_index; i < end_index; i++)
		work_frames.push_back(all_frames[i]);
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
				if(histogram[j] > best_value)
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

void remove_bulge_pixels_by_neighbord_size(uchar *diff_frame, int w, int h, int min_neigbords)
{
	UcharMemoryGuard helpful_mask(diff_frame, w*h);
	for(int y = 1; y < h -1; y++)
		for(int x = 1; x < w - 1; x++)
		{
			int neigbords_size = 0;
			for(int i = 0; i < OFFSETS_SIZE; i++)
			{
				int curr_x = x + X_OFFSETS[i];
				int curr_y = y + Y_OFFSETS[i];
				if(diff_frame[curr_y*w + curr_x] == 255)
					neigbords_size++;
			}
			if(neigbords_size < min_neigbords)
			{
				helpful_mask[y*w + x] = 0;
			}
		}
	memcpy(diff_frame, helpful_mask.data(), w*h);
}

void inline _find_blob(uchar *img, int w, int h, int x, int y, std::vector<Pixel> &pixels)
{
	pixels.push_back(Pixel(x, y));
	img[pixels[0].y*w + pixels[0].x] = 0;
	for(int i = 0; i < pixels.size(); i++)
	{
		Pixel &curr_pixel = pixels[i];
		for(int i = 0; i < OFFSETS_SIZE; i++)
		{
			Pixel next_pixel(curr_pixel.x + X_OFFSETS[i], curr_pixel.y + Y_OFFSETS[i]);
			// we dosn't fear about crossing of the border cause the img have black rectangle on the border edge ??
			if(next_pixel.x >= 0 && next_pixel.x < w && next_pixel.y >= 0  && next_pixel.y < h)
			{
				if(img[next_pixel.y*w + next_pixel.x] == 255)
				{
					pixels.push_back(next_pixel);
					img[next_pixel.y*w + next_pixel.x] = 0;
				}
			}
		}
	}
}

void remove_small_parts(uchar *diff_frame, int w, int h, int min_part_size)
{
	std::vector<Pixel> pixel_container;
	UcharMemoryGuard helpful_mask(diff_frame, w*h);

	for(int y = 0; y < h; y++)
		for(int x = 0; x < w; x++)
		{
			if(x == 0 || x == w-1 || y == 0 || y == h-1)
				helpful_mask[y*w+x] = 0;
			else if(helpful_mask[y*w + x] == 255)
			{
				pixel_container.clear();
				_find_blob(helpful_mask.data(), w, h, x, y, pixel_container);
				if(pixel_container.size() < min_part_size)
					for(Pixel p: pixel_container)
						diff_frame[p.y*w + p.x] = 0;
				else fprintf(stderr, "remove_small_parts not clean %zu size part!\n", pixel_container.size());
			}
		}
}

#endif /* JNI_DIFF_CALCULATOR_HPP_ */
