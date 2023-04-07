package main.listeners;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class MainWindowListener implements WindowListener {

	@Override public void windowClosing(WindowEvent e) 
	{
		deinitJni();
		System.exit(0);
	}
	@Override public void windowOpened(WindowEvent e) {}
	@Override public void windowIconified(WindowEvent e) {}
	@Override public void windowDeiconified(WindowEvent e) {}
	@Override public void windowDeactivated(WindowEvent e) {}
	@Override public void windowClosed(WindowEvent e) {}
	@Override public void windowActivated(WindowEvent e) {}
	
	private native void deinitJni();
}
