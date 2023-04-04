package main;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import main.draw_panel.DrawPanel;

public class MainWindow extends JFrame 
{
	private static final long serialVersionUID = 1L;
	
	private Vector<BufferedImage> frames = null;
	private int frames_index = 0;
	private BufferedImage base_frame;
	private BufferedImage filtered_zones;
	private int width, height;
	private DrawPanel drawPanel;
	
	
	public MainWindow(String samples_dir_path, int width, int height) 
	{
		Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
		File sample_dir = new File(samples_dir_path);
//		setSize(screen_size.width, screen_size.height);
		setSize(800, 600);
		setTitle("frame: " + frames_index);
		add_key_listener();
		setLayout(new BorderLayout());
		frames = new Vector<BufferedImage>();
		for(File f:sample_dir.listFiles())
		{
			if(f.getName().equals("base_frame.bmp"))
				try {base_frame = ImageIO.read(f);} catch (IOException e) {e.printStackTrace();System.exit(2);}
			else
				try {frames.addElement(ImageIO.read(f));} catch (IOException e) {e.printStackTrace();System.exit(2);}
		}
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.width = width;
		this.height = height;
		filtered_zones = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		add_draw_panel();
		add_command_panel();
		add_menu_bar();
	}
	
	private void add_key_listener() 
	{
		KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		manager.addKeyEventDispatcher(new KeyEventDispatcher() 
		{
			@Override
			public boolean dispatchKeyEvent(KeyEvent e) 
			{
				if (e.getID() == KeyEvent.KEY_PRESSED) 
				{
//					System.out.println("KEY_PRESSED " + e.getKeyCode());
				} 
				else if (e.getID() == KeyEvent.KEY_RELEASED) 
				{
//					System.out.println("KEY_RELEASED " + e.getKeyCode());
					if(e.getKeyCode() == KeyEvent.VK_RIGHT)
					{
						frames_index = (frames_index + 1) % frames.size();
						setTitle("frame: " + frames_index);
						MainWindow.this.drawPanel.set_frame(frames.get(frames_index));
						MainWindow.this.drawPanel.repaint();
					}
				} 
				else if (e.getID() == KeyEvent.KEY_TYPED) 
				{
//					System.out.println("KEY_TYPED " + e.getKeyCode());
				}
				return false;
			}
		});
	}

	private void add_menu_bar() 
	{
		JMenuBar menubar = new JMenuBar();
		JMenu file_menu = new JMenu("file");
		JMenuItem exit_menu_item = new JMenuItem("exit");
		exit_menu_item.addActionListener(new ActionListener() 
		{
			@Override public void actionPerformed(ActionEvent e) {System.exit(0);}
		});
		file_menu.add(exit_menu_item);
		menubar.add(file_menu);
		setJMenuBar(menubar);
	}

	private void add_draw_panel() 
	{
		drawPanel = new DrawPanel(width, height, 8);
		drawPanel.set_base_frame(base_frame);
		drawPanel.set_frame(frames.elementAt(0));
		add(drawPanel, BorderLayout.CENTER);
	}

	private void add_command_panel() 
	{
		JPanel commands_panel = new JPanel();
		JComboBox treshholds_combo_box = new JComboBox<String>(create_tresholds_options(45));
		final JButton mode_button;
		JTextArea ignore_title;
		JComboBox ignore_radius_combo_box;
		
		treshholds_combo_box.addItemListener(new ItemListener() 
		{
			@Override
			public void itemStateChanged(ItemEvent e) 
			{
				MainWindow.this.drawPanel.update_treshold(Integer.parseInt((String)e.getItem()));
			}
		});
		treshholds_combo_box.setSelectedItem("" + drawPanel.get_treshold());
		
		ignore_title = new JTextArea("ignore radius");
		ignore_radius_combo_box = new JComboBox<String>(create_tresholds_options(100));
		ignore_radius_combo_box.addItemListener(new ItemListener() 
		{
			@Override
			public void itemStateChanged(ItemEvent e) 
			{
				MainWindow.this.drawPanel.update_ignore_radius(Integer.parseInt((String)e.getItem()));
			}
		});
		ignore_radius_combo_box.setSelectedItem("" + drawPanel.get_ignore_radius());
		
		mode_button = new JButton("Set ignore draw mode");
		mode_button.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				if(mode_button.getText().equals("Set ignore draw mode"))
				{
					mode_button.setText("Set normal view mode");
					MainWindow.this.drawPanel.set_mode(DrawPanel.MODE_SET_IGNORE_AREA);
				}
				else
				{
					mode_button.setText("Set ignore draw mode");
					MainWindow.this.drawPanel.set_mode(DrawPanel.MODE_NORMAL);
				}
			}
		});
		commands_panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		commands_panel.add(mode_button);
		commands_panel.add(treshholds_combo_box);
		commands_panel.add(ignore_title);
		commands_panel.add(ignore_radius_combo_box);
		add(commands_panel, BorderLayout.NORTH);
	}

	private String[] create_tresholds_options(int size) 
	{
		String ret[] = new String[size];
		int offset = 1;
		for(int i = offset; i < ret.length + offset; i++)
			ret[i-offset] = "" + i;
		return ret;
	}
	
	@Override
	public void paintComponents(Graphics g) 
	{
		super.paintComponents(g);
		g.drawImage(base_frame, 0, 0, null);
	}
	
	public static void main(String[] args) 
	{
		new MainWindow("/home/elazarkin/storage/datasets/diff_project/samples/12", 640, 480).setVisible(true);
	}

}
