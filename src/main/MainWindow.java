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
import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import main.gui.DrawPanel;
import main.listeners.MainWindowListener;

public class MainWindow extends JFrame 
{
	private static final long serialVersionUID = 1L;
	
	static {System.loadLibrary("diff_calculator_jni");}
	
	private int width, height;
	private DrawPanel draw_panel;
	
	
	public MainWindow(String samples_dir_path, int width, int height) 
	{
		Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
		setLayout(new BorderLayout());
		
		this.width = width;
		this.height = height;
		// setSize(screen_size.width, screen_size.height);
		setSize(screen_size.width, screen_size.height/2);
		setTitle("frame: " + getFrameIndexJni());
		add_key_listener();
		add((draw_panel = new DrawPanel(width, height, 8)), BorderLayout.CENTER);
		add_command_panel();
		add_menu_bar();
		addWindowListener(new MainWindowListener());
		init_variables(samples_dir_path);
	}
	
	private void init_variables(String samples_dir_path) 
	{
		File sample_dir = new File(samples_dir_path);
		File ignore_areas_file_path;
		Vector<String> all_samples_files = new Vector<String>();
		
		for(File f:sample_dir.listFiles())
			if(f.getName() != "ignored_area.bin" && f.getName() != "base_frame.bin" && f.getName().endsWith(".bin"))
				all_samples_files.add(f.getAbsolutePath());
		
		Collections.sort(all_samples_files, new Comparator<String>() 
		{
			@Override
			public int compare(String a, String b) 
			{
				String a_splited[] = a.split("/");
				String a_filename = a_splited[a_splited.length - 1];
				String b_splited[] = b.split("/");
				String b_filename = b_splited[b_splited.length - 1];
				int a_value = Integer.parseInt(a_filename.split("\\.")[0]);
				int b_value = Integer.parseInt(b_filename.split("\\.")[0]);
				return a_value - b_value;
			}
		});
		// 300 frame for 30fps ~= 10 secs
		setDiffFramesRangeJni(Math.min(300, all_samples_files.size()));
		initJni(all_samples_files.toArray(new String[all_samples_files.size()]), width, height);
		
		ignore_areas_file_path = new File(sample_dir, "ignored_area.bin");
		if(ignore_areas_file_path.exists())
			loadIgnoreArea(ignore_areas_file_path.getAbsolutePath());
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
						MainWindow.this.repaint();
					}
					else if(e.getKeyCode() == KeyEvent.VK_LEFT)
					{
						moveFramesIndexJni(-1);
						setTitle("frame: " + getFrameIndexJni());
						MainWindow.this.repaint();
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
				// TODO
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

	private void add_command_panel() 
	{
		JPanel commands_panel = new JPanel();
		JComboBox<String> treshholds_combo_box = new JComboBox<String>(create_combo_box_int_options(1, 45, 1));
		JLabel mode_title = new JLabel("mode");
		JLabel tresh_title = new JLabel("thresh");
		JLabel ignore_title = new JLabel("ignore radius");
		final JButton mode_button;
		JComboBox<String> ignore_radius_combo_box = new JComboBox<String>(create_combo_box_int_options(2, 100, 8));;
		
		treshholds_combo_box.addItemListener(new ItemListener() 
		{
			@Override
			public void itemStateChanged(ItemEvent e) 
			{
				updateTreshJni(Integer.parseInt((String)e.getItem()));
				MainWindow.this.repaint();
			}
		});
		treshholds_combo_box.setSelectedItem("" + getTreshJni());
		
		ignore_radius_combo_box.addItemListener(new ItemListener() 
		{
			@Override
			public void itemStateChanged(ItemEvent e) 
			{
				updateIgnoreRadiusJni(Integer.parseInt((String)e.getItem()));
			}
		});
		ignore_radius_combo_box.setSelectedItem("" + getIgnoreRadiusJni());
		
		mode_button = new JButton("set ignore draw");
		mode_button.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				if(mode_button.getText().equals("set ignore draw"))
				{
					mode_button.setText("normal");
					MainWindow.this.draw_panel.set_mode(DrawPanel.MODE_SET_IGNORE_AREA);
				}
				else
				{
					mode_button.setText("set ignore draw");
					MainWindow.this.draw_panel.set_mode(DrawPanel.MODE_NORMAL);
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
	private native int getFrameIndexJni();
	private native void updateTreshJni(int parseInt);
	private native int getTreshJni();
	private native void updateIgnoreRadiusJni(int parseInt);
	private native int getIgnoreRadiusJni();
	private native void moveFramesIndexJni(int i);
	private native void getIgnoreFrame(byte [] pixels);
	private native void setDiffFramesRangeJni(int frames_size);
	
	public static void main(String[] args) 
	{
		new MainWindow("/home/elazarkin/storage/datasets/diff_project/samples/12", 640, 480).setVisible(true);
	}

}
