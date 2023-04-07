package main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import main.gui.DrawPanel;

public class MainWindow extends JFrame 
{
	private static final long serialVersionUID = 1L;
	
	static {System.loadLibrary("diff_calculator_jni");}
	
	private int width, height;
	private BufferedImage filtered_zones;
	private DrawPanel drawPanel;
	private File ignore_areas_file_path;
	
	
	public MainWindow(String samples_dir_path, int width, int height) 
	{
		Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
		File sample_dir = new File(samples_dir_path);
//		setSize(screen_size.width, screen_size.height);
		setSize(screen_size.width, screen_size.height/2);
		ignore_areas_file_path = new File(sample_dir, "ignored_area.bin");
		setTitle("frame: " + getFrameIndexJni());
		add_key_listener();
		setLayout(new BorderLayout());
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.width = width;
		this.height = height;
		add_draw_panel();
		add_command_panel();
		add_menu_bar();
		addWindowListener(new WindowListener() 
		{
			@Override public void windowOpened(WindowEvent e) {}
			@Override public void windowIconified(WindowEvent e) {}
			@Override public void windowDeiconified(WindowEvent e) {}
			@Override public void windowDeactivated(WindowEvent e) {}
			@Override public void windowClosing(WindowEvent e) {deinitJni();}
			@Override public void windowClosed(WindowEvent e) {}
			@Override public void windowActivated(WindowEvent e) {}
		});
	}
	
	private void add_key_listener() 
	{
		KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		manager.addKeyEventDispatcher(new KeyEventDispatcher() 
		{
			@Override
			public boolean dispatchKeyEvent(KeyEvent e) 
			{
				if (e.getID() == KeyEvent.KEY_RELEASED) 
				{
					if(e.getKeyCode() == KeyEvent.VK_RIGHT)
					{
						moveFramesIndexJni(1);
						setTitle("frame: " + getFrameIndexJni());
						MainWindow.this.drawPanel.repaint();
					}
					else if(e.getKeyCode() == KeyEvent.VK_LEFT)
					{
						moveFramesIndexJni(-1);
						setTitle("frame: " + getFrameIndexJni());
						MainWindow.this.drawPanel.repaint();
					}
				}
				else if (e.getID() == KeyEvent.KEY_PRESSED) {}
				else if (e.getID() == KeyEvent.KEY_TYPED) {}
				return false;
			}
		});
	}

	private void add_menu_bar() 
	{
		JMenuBar menubar = new JMenuBar();
		JMenu file_menu = new JMenu("file");
		JMenuItem save_ignore_area_item = new JMenuItem("save ignore area");
		JMenuItem exit_menu_item = new JMenuItem("exit");
		save_ignore_area_item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				try 
				{
					ImageIO.write(drawPanel.get_ignore_areas_img(), "png", ignore_areas_file_path);
				} 
				catch (IOException e1) {e1.printStackTrace();}
			}
		});
		exit_menu_item.addActionListener(new ActionListener() 
		{
			@Override public void actionPerformed(ActionEvent e) {System.exit(0);}
		});
		file_menu.add(save_ignore_area_item);
		file_menu.add(exit_menu_item);
		menubar.add(file_menu);
		setJMenuBar(menubar);
	}

	private void add_draw_panel() 
	{
		drawPanel = new DrawPanel(width, height, 8);
		if(ignore_areas_file_path.exists())
		{
			try 
			{
				BufferedImage ignore_area = ImageIO.read(ignore_areas_file_path);
				drawPanel.set_ignore_areas(ignore_area);
			} 
			catch (IOException e) {e.printStackTrace();}
		}
		// set base frame after ignore cause this function create
		//  void ignore areas if it dosn't exist!
		add(drawPanel, BorderLayout.CENTER);
	}

	private void add_command_panel() 
	{
		JPanel commands_panel = new JPanel();
		JComboBox treshholds_combo_box = new JComboBox<String>(create_combo_box_int_options(1, 45, 1));
		JLabel mode_title = new JLabel("mode"), ignore_title = new JLabel("ignore radius"), tresh_title = new JLabel("thresh");
		final JButton mode_button;
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
		
		ignore_radius_combo_box = new JComboBox<String>(create_combo_box_int_options(2, 100, 8));
		ignore_radius_combo_box.addItemListener(new ItemListener() 
		{
			@Override
			public void itemStateChanged(ItemEvent e) 
			{
				MainWindow.this.drawPanel.update_ignore_radius(Integer.parseInt((String)e.getItem()));
			}
		});
		ignore_radius_combo_box.setSelectedItem("" + drawPanel.get_ignore_radius());
		
		mode_button = new JButton("set ignore draw");
		mode_button.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				if(mode_button.getText().equals("set ignore draw"))
				{
					mode_button.setText("normal");
					MainWindow.this.drawPanel.set_mode(DrawPanel.MODE_SET_IGNORE_AREA);
				}
				else
				{
					mode_button.setText("set ignore draw");
					MainWindow.this.drawPanel.set_mode(DrawPanel.MODE_NORMAL);
				}
			}
		});
		commands_panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		commands_panel.add(tresh_title);
		commands_panel.add(treshholds_combo_box);
		commands_panel.add(ignore_title);
		commands_panel.add(ignore_radius_combo_box);
		commands_panel.add(mode_title);
		commands_panel.add(mode_button);
		add(commands_panel, BorderLayout.NORTH);
	}

	private String[] create_combo_box_int_options(int start, int end, int step) 
	{
		String ret[] = new String[(end - start)/step + 1];
		for(int i = start; i <= end; i += step)
		{
			ret[(i-start)/step] = "" + i;
		}
		return ret;
	}
	
	private native void initJni(String[] all_frames_pathes, int w, int h);
	private native void loadIgnoreArea(String file_path);
	private native void deinitJni();
	private native int getFrameIndexJni();
	private native void moveFramesIndexJni(int i);
	private native void getBaseFrame(byte [] pixels);
	private native void getIgnoreFrame(byte [] pixels);
	
	public static void main(String[] args) 
	{
		new MainWindow("/home/elazarkin/storage/datasets/diff_project/samples/12", 640, 480).setVisible(true);
	}

}
