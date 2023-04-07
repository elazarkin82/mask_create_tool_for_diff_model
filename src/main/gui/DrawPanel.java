package main.gui;

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
import java.awt.image.DataBufferByte;


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
	private int width, height, padding;
	private BufferedImage screen = null;
	private Graphics2D gscreen;
	private Area frame_area = new Area();
	private Area diff_area = new Area();
	private int MODE = MODE_NORMAL;
	
	public DrawPanel(int w, int h, int padding)
	{
		m_base_frame = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
		m_frame = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
		m_mask = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
		m_ignore_areas = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
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
					addIgnoreAreaJni(x, y);
					DrawPanel.this.repaint();
				}
			}
			@Override public void mousePressed(MouseEvent e) {}
			@Override public void mouseExited(MouseEvent e) {}
			@Override public void mouseEntered(MouseEvent e) {}
			@Override public void mouseClicked(MouseEvent e) {}
		});
	}
	
	private void update_gray_image(BufferedImage image, byte gray_bytes[])
	{
		image.getRaster().setDataElements(0, 0, width, height, gray_bytes);
	}
	
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
		{
			byte gray_bytes[] = ((DataBufferByte)m_base_frame.getRaster().getDataBuffer()).getData();
			readBaseFrameBytesJni(gray_bytes);
			gscreen.drawImage(m_base_frame, x0, y0, image_w, image_h, null);
		}
		if(m_frame != null)
		{
			byte gray_bytes[] = ((DataBufferByte)m_frame.getRaster().getDataBuffer()).getData();
			readFrameBytesJni(gray_bytes);
			gscreen.drawImage(m_frame, x1, y0, image_w, image_h, null);
		}
		g.drawImage(screen, 0, 0, null);
	}

	public void set_mode(int mode) {MODE = mode;}
	private native void getBaseFrame(byte [] pixels);
	private native void readBaseFrameBytesJni(byte[] image_bytes);
	private native void readFrameBytesJni(byte[] image_bytes);
	private native void addIgnoreAreaJni(int x, int y);
}