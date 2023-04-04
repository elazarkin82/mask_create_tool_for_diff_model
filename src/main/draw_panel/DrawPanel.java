package main.draw_panel;

import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;


public class DrawPanel extends Canvas
{
	private static final long serialVersionUID = 1L;
	
	public static final int MODE_NORMAL = 0;
	public static final int MODE_SET_IGNORE_AREA = 1;

	private class Area
	{
		int x0 = -1, y0 = -1, x1 = -1, y1 = -1;
		
		public Area() {}
		public void update(int x0, int y0, int x1, int y1)
		{
			this.x0 = x0;this.x1 = x1; this.y0 = y0;this.y1 = y1;
		}
		
		public boolean is_inside(int x, int y)
		{
			return x >= x0 && x < x1 && y >= y0 && y < y1;
		}
	}
	
	private BufferedImage m_base_frame = null;
	private BufferedImage m_frame = null;
	private BufferedImage m_mask = null;
	private BufferedImage m_ignore_areas = null;
	private Graphics2D m_ignore_areas_g = null;
	private int width, height, padding;
	private BufferedImage screen = null;
	private Graphics2D gscreen;
	private Area frame_area = new Area();
	private Area diff_area = new Area();
	private int treshold = 5, ignore_radius=50; 
	private int MODE = MODE_NORMAL;
	
	public DrawPanel(int w, int h, int padding)
	{
		width = w; height = h;
		this.padding = padding;
		add_mouse_listeners();
	}
	
	private void add_mouse_listeners() 
	{
		addMouseMotionListener(new MouseMotionListener() 
		{
			@Override
			public void mouseMoved(MouseEvent e) 
			{
				Area ignore_area_setter = null;
				if(diff_area.is_inside(e.getX(), e.getY()))
					ignore_area_setter = diff_area;
				else if(frame_area.is_inside(e.getX(), e.getY()))
					ignore_area_setter = frame_area;
				
				if(MODE == MODE_SET_IGNORE_AREA && ignore_area_setter != null)
				{
					int x = e.getX() - ignore_area_setter.x0;
					int y = e.getY() - ignore_area_setter.y0;
					DrawPanel.this.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
				}
				else
				{
					DrawPanel.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			}
			
			@Override public void mouseDragged(MouseEvent e) {}
		});
		
		addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) 
			{
				Area ignore_area_setter = null;
				if(diff_area.is_inside(e.getX(), e.getY()))
					ignore_area_setter = diff_area;
				else if(frame_area.is_inside(e.getX(), e.getY()))
					ignore_area_setter = frame_area;
				
				if(MODE == MODE_SET_IGNORE_AREA && ignore_area_setter != null)
				{
					int x = (int)((e.getX() - ignore_area_setter.x0)*m_frame.getWidth()/(ignore_area_setter.x1 - ignore_area_setter.x0));
					int y = (int)((e.getY() - ignore_area_setter.y0)*m_frame.getHeight()/(ignore_area_setter.y1 - ignore_area_setter.y0));
					m_ignore_areas_g.setColor(Color.white);
					m_ignore_areas_g.fillOval(x - ignore_radius/2, y - ignore_radius/2, ignore_radius, ignore_radius);
					for(int yy = 0; yy < m_ignore_areas.getHeight(); yy++)
						for(int xx = 0; xx < m_ignore_areas.getWidth(); xx++)
						{
							if((m_ignore_areas.getRGB(xx, yy) & 0xff) != 0)
								m_ignore_areas.setRGB(xx, yy, Color.white.getRGB());
						}
					DrawPanel.this.repaint();
				}
			}
			
			@Override public void mousePressed(MouseEvent e) {}
			@Override public void mouseExited(MouseEvent e) {}
			@Override public void mouseEntered(MouseEvent e) {}
			@Override public void mouseClicked(MouseEvent e) {}
		});
	}

	public void set_base_frame(BufferedImage image) 
	{
		m_base_frame = image;
		if(
			m_ignore_areas == null 
			|| 
			m_ignore_areas.getWidth() != m_base_frame.getWidth()
			||
			m_ignore_areas.getHeight() != m_base_frame.getHeight()
		)
		{
			m_ignore_areas = new BufferedImage(m_base_frame.getWidth(), m_base_frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
			m_ignore_areas_g = m_ignore_areas.createGraphics();
			m_ignore_areas_g.setColor(Color.black);
			m_ignore_areas_g.fillRect(0, 0, m_ignore_areas.getWidth(), m_ignore_areas.getHeight());
		}
	}
	public void set_frame(BufferedImage image) {m_frame = image;}
	
	@Override
	public void paint(Graphics g) 
	{
		Dimension max_size = getSize();
		int image_w, image_h, x0, y0, x1, y1;
		if(screen == null || screen.getWidth() != max_size.width || screen.getHeight() != max_size.height)
		{
			screen = new BufferedImage(max_size.width, max_size.height, BufferedImage.TYPE_INT_ARGB);
			gscreen = screen.createGraphics();
		}
		x0 = 0; y0 = 0;
		x1 = screen.getWidth()/2 + padding/2; 
		y1 = screen.getHeight()/2 + padding/2;
		image_w = screen.getWidth()*height/width/2 - padding/2;
		image_h = screen.getHeight()/2 - padding/2;
		if(m_base_frame != null)
			gscreen.drawImage(m_base_frame, x0, y0, image_w, image_h, null);
		if(m_frame != null)
		{
			BufferedImage frame_with_ignore = new BufferedImage(
				m_frame.getWidth(), m_frame.getHeight(), BufferedImage.TYPE_INT_ARGB
			);
			Graphics2D frame_with_ignore_g = frame_with_ignore.createGraphics();
			frame_with_ignore_g.drawImage(m_frame, 0, 0, null);
			frame_with_ignore_g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.5f));
			frame_with_ignore_g.drawImage(m_ignore_areas, 0, 0, null);
			gscreen.drawImage(frame_with_ignore, x1, y0, image_w, image_h, null);
			frame_area.update(x1, y0, x1 + image_w, y0 + image_h);
		}
		
		if(m_base_frame != null && m_frame != null)
		{
			BufferedImage diff = DiffImages.calc_diff_mask(m_base_frame, m_frame, m_ignore_areas, treshold);
			gscreen.drawImage(diff, x0, y1, image_w, image_h, null);
			diff_area.update(x0, y1, x0 + image_w, y1 + image_h);
		}		
		g.drawImage(screen, 0, 0, null);
	}


	public void set_mode(int mode) 
	{
		MODE = mode;
	}
	
	public String get_treshold() {return "" + treshold;}
	public void update_treshold(int value) 
	{
		treshold = value;
		this.repaint();
	}
	public String get_ignore_radius() {return "" + ignore_radius;}

	public void update_ignore_radius(int value) {ignore_radius = value;}
}