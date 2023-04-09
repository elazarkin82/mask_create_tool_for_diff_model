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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import main.gui.DrawPanel;
import main.listeners.MainWindowListener;

public class MainWindow extends JFrame 
{
	private static final long serialVersionUID = 1L;
	
	static {System.loadLibrary("diff_calculator_jni");}
	
	private int samples_size;
	private int width, height;
	private DrawPanel draw_panel;
	private File ignore_areas_file_path;
	private JDialog progress_dialog;
	
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
		init_progress_block_pane();
	}
	
	private void init_progress_block_pane() 
	{
		progress_dialog = new JDialog(this, "please wait!", true);
		progress_dialog.add(BorderLayout.CENTER, new JLabel("  In progress..."));
		progress_dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		progress_dialog.setSize(300, 75);
		progress_dialog.setLocationRelativeTo(this);
	}

	private void init_variables(String samples_dir_path) 
	{
		File sample_dir = new File(samples_dir_path);
		Vector<String> all_samples_files = new Vector<String>();
		
		for(File f:sample_dir.listFiles())
			if(
				!f.getName().startsWith("ignored_area.") 
				&& 
				!f.getName().startsWith("base_frame.") 
				&&
				!f.getName().endsWith("_mask.bmp")
				&&
				!f.getName().endsWith("_mask.bin")
				&&
				f.getName().endsWith(".bin")
			)
				all_samples_files.add(f.getAbsolutePath());
		
		samples_size = all_samples_files.size();
		
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
			loadIgnoreAreaMask(ignore_areas_file_path.getAbsolutePath());
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
						int step = e.isControlDown() ? 10: 1;
						moveFramesIndexJni(step);
						setTitle("frame: " + getFrameIndexJni());
						draw_panel.repaint();
					}
					else if(e.getKeyCode() == KeyEvent.VK_LEFT)
					{
						int step = e.isControlDown() ? 10: 1;
						moveFramesIndexJni(-step);
						setTitle("frame: " + getFrameIndexJni());
						draw_panel.repaint();
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
		JMenuItem save_items_mask = new JMenuItem("save items mask");
		JMenuItem exit_menu_item = new JMenuItem("exit");
		save_ignore_area_item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				saveIgnoreAreasMask(ignore_areas_file_path.getAbsolutePath());
			}
		});
		save_items_mask.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				Thread t = new Thread(new Runnable(){public void run() {progress_dialog.setVisible(true);}});
				t.start();
				saveAllItemMasksJni();
				progress_dialog.setVisible(false);
				MainWindow.this.repaint();
			}
		});
		exit_menu_item.addActionListener(new ActionListener() 
		{
			@Override public void actionPerformed(ActionEvent e) {System.exit(0);}
		});
		file_menu.add(save_ignore_area_item);
		file_menu.add(save_items_mask);
		file_menu.add(exit_menu_item);
		menubar.add(file_menu);
		setJMenuBar(menubar);
	}

	private void add_command_panel() 
	{
		JPanel commands_panel = new JPanel();
		JLabel thresh_title = new JLabel("thesh");
		JComboBox<String> treshholds_cb = new JComboBox<String>(create_combo_box_int_options(1, 45, 1));
		JLabel ignore_title = new JLabel("ignore radius");
		JComboBox<String> ignore_radius_cb = new JComboBox<String>(create_combo_box_int_options(2, 100, 8));
		JLabel min_blob_size_title = new JLabel("blob size");
		JComboBox<String> min_blobs_size_cb = new JComboBox<String>(create_combo_box_int_options(50, 4000, 50));
		JLabel mode_title = new JLabel("mode");
		JLabel tresh_title = new JLabel("thresh");
		
		final JButton play_button;
		final JButton mode_button;
		
		treshholds_cb.addItemListener(new ItemListener() 
		{
			@Override
			public void itemStateChanged(ItemEvent e) 
			{
				updateThreshJni(Integer.parseInt((String)e.getItem()));
				draw_panel.repaint();
			}
		});
		treshholds_cb.setSelectedItem("" + getThreshJni());
		
		min_blobs_size_cb.addItemListener(new ItemListener() 
		{
			@Override
			public void itemStateChanged(ItemEvent e) 
			{
				setMinBlobSizeJni(Integer.parseInt((String)e.getItem()));
				draw_panel.repaint();
			}
		});
		setMinBlobSizeJni(Integer.parseInt((String)min_blobs_size_cb.getSelectedItem()));
		
		ignore_radius_cb.addItemListener(new ItemListener() 
		{
			@Override
			public void itemStateChanged(ItemEvent e) 
			{
				updateIgnoreRadiusJni(Integer.parseInt((String)e.getItem()));
			}
		});
		ignore_radius_cb.setSelectedItem("" + getIgnoreRadiusJni());
		
		play_button = new JButton("play");
		play_button.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				if(play_button.getText().equals("play"))
				{
					play_button.setText("stop");
					// TODO move this thread outside and care that in closing all mission complited cause 
					//  of jni crashing!
					new Thread(new Runnable() 
					{
						@Override
						public void run() 
						{
							while(play_button.getText().equals("stop"))
							{
								moveFramesIndexJni(1);
								MainWindow.this.setTitle("" + getFrameIndexJni());
								draw_panel.repaint();
							}
						}
					}).start();
				}
				else play_button.setText("play");
			}
		});
		
		mode_button = new JButton(DrawPanel.MODE_TITLE[draw_panel.get_current_mode()]);
		mode_button.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				int next_mode = (draw_panel.get_current_mode() + 1) % DrawPanel.MODE_TITLE.length;
				mode_button.setText(DrawPanel.MODE_TITLE[next_mode]);
				MainWindow.this.draw_panel.set_mode(next_mode);
			}
		});
		commands_panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		commands_panel.add(thresh_title);
		commands_panel.add(tresh_title);
		commands_panel.add(treshholds_cb);
		commands_panel.add(ignore_title);
		commands_panel.add(ignore_radius_cb);
		commands_panel.add(min_blob_size_title);
		commands_panel.add(min_blobs_size_cb);
		commands_panel.add(mode_title);
		commands_panel.add(mode_button);
		commands_panel.add(play_button);
		add(commands_panel, BorderLayout.NORTH);
	}

	private String[] create_combo_box_int_options(int start, int end, int step) 
	{
		String ret[] = new String[(end - start)/step + 1];
		for(int i = start; i <= end; i += step)
			ret[(i-start)/step] = "" + i;
		return ret;
	}
	
	private native void initJni(String[] all_frames_pathes, int w, int h);
	private native int getFrameIndexJni();
	private native void moveFramesIndexJni(int i);
	private native void updateThreshJni(int thresh);
	private native int getThreshJni();
	private native void updateIgnoreRadiusJni(int ignore_radius);
	private native void setMinBlobSizeJni(int min_size);
	private native int getIgnoreRadiusJni();
	private native void getIgnoreFrame(byte [] pixels);
	private native void setDiffFramesRangeJni(int frames_size);
	private native void loadIgnoreAreaMask(String file_path);
	private native void saveIgnoreAreasMask(String out_path);
	private native void saveAllItemMasksJni();
	
	public static void main(String[] args) 
	{
		new MainWindow("/home/elazarkin/storage/datasets/diff_project/samples/12", 640, 480).setVisible(true);
	}

}
