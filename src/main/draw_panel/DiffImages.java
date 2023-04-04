package main.draw_panel;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class DiffImages 
{
	public static BufferedImage calc_diff_mask(
		BufferedImage base, BufferedImage frame, BufferedImage ignore_areas, int treshold
	)
	{
		long t0 = System.currentTimeMillis();
		BufferedImage diff = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
		
		for(int y = 1; y < base.getHeight()-1; y++)
			for(int x = 1; x < base.getWidth()-1; x++)
			{
				int gray_base = base.getRGB(x, y) & 0xff;
				int gray_frame = frame.getRGB(x, y) & 0xff;
				if((ignore_areas.getRGB(x, y) & 0xff) == 0 && Math.abs(gray_frame - gray_base) >= treshold)
					diff.setRGB(x, y, Color.white.getRGB());
				else
					diff.setRGB(x, y, Color.black.getRGB());
			}
		
		diff = clean_stand_alone_pixels(diff);
		System.out.printf("time to calculate diff: %3.2f secs\n", (System.currentTimeMillis() - t0)/1000.0);
		return diff;
	}

	private static int[] get_histogram(BufferedImage image) 
	{
		int ret[] = new int[256];
		for(int y = 1; y < image.getHeight()-1; y++)
			for(int x = 1; x < image.getWidth()-1; x++)
				ret[(image.getRGB(x, y) & 0xff)]++;
		return ret;
	}

	private static BufferedImage clean_stand_alone_pixels(BufferedImage image) 
	{
		BufferedImage ret = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		final int x_offsets[] = {-1, 0, 1, -1, 1, -1, 0, 1};
		final int y_offsets[] = {-1,-1,-1,  0, 0,  1, 1, 1};
		final int min_neigbors = 4;
		int cleaned_counter = 0;
		for(int y = 1; y < image.getHeight()-1; y++)
			for(int x = 1; x < image.getWidth()-1; x++)
			{
				if(image.getRGB(x, y) ==  Color.white.getRGB())
				{
					int white_counter = 0;
					for(int i = 0; i < x_offsets.length; i++)
						if(image.getRGB(x + x_offsets[i], y + y_offsets[i]) == Color.white.getRGB())
							white_counter++;
					if(white_counter < min_neigbors)
					{
						ret.setRGB(x, y, Color.black.getRGB());
						cleaned_counter++;
					}
					else
					{
						ret.setRGB(x, y, Color.white.getRGB());
					}
				}
				else ret.setRGB(x, y, Color.black.getRGB());
			}
		System.out.printf("cleaned neigbors: %d\n", cleaned_counter);
		return ret;
	}
}
