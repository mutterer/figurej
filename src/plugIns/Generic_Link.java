package plugIns;

import ij.IJ;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GUI;
import ij.gui.Roi;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;

import windows.MainWindow;

/**
 * Generic FigureJ link mechanism
 * designed for ICY, but will work with any software. 
 * @author Jerome Mutterer
 * @author Edda Zinck this class controls communication with Inkscape
 */

public class Generic_Link extends Link implements ActionListener {

	public static final String LOC_KEY = "genericLink.loc";
	private static final long serialVersionUID = 1L;
	private static String tempDir = IJ.getDirectory("startup") + "temp"
			+ File.separator;
	private static Frame instance;
	private JButton exposePanelButton = new JButton("Expose Panel");
	private JButton grabButton = new JButton("Grab view");
	// private JButton editButton = new JButton("Edit svg");
	// private JButton helpButton = new JButton("Help");

	// TODO modifiers?!
	String[] portSettings = new String[3];

	public Generic_Link() {
		super();
		init();
	}

	public Generic_Link(MainWindow main) {
		super(main);
		init();
	}

	private void init() {
		linkIdentifyer = "Generic_";
		// fileName = linkIdentifyer+counter+extension;
		if (instance != null) {
			WindowManager.toFront(instance);
			return;
		}
		WindowManager.addWindow(this);
		instance = this;
		setLayout(new FlowLayout());

		addListeners();
		setTitle("Generic Link");
		add(exposePanelButton);
		add(grabButton);

		pack();
		Point loc = Prefs.getLocation(LOC_KEY);
		if (loc != null) {
			setLocation(loc);
		} else {
			GUI.center(this);
		}
		setVisible(true);
	}

	private void addListeners() {
		exposePanelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				updateActivePanel();
				int width = 100;
				int height = 100;
				Roi r = IJ.getImage().getRoi();
				if (r != null) {
					width = IJ.getImage().getRoi().getBounds().width;
					height = IJ.getImage().getRoi().getBounds().height;
				}
				fileName = linkIdentifyer+timeStamp()+".tif";
				// Write in preferences here, and save prefs.
				Prefs.set("figurej.panelWidth", width);
				Prefs.set("figurej.panelHeight", height);
				Prefs.set("figurej.tempDir", tempDir);
				Prefs.set("figurej.panelFilename", fileName);
				Prefs.savePreferences();
//				((LeafPanel) getActivePanel()).getImgData().setExternalSource(fileName);
//				((LeafPanel) getActivePanel()).getImgData().setFileDirectory(tempDir);

			}
		});

		grabButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				updateActivePanel();
				// Read from preferences here.
				tempDir = Prefs.get("figurej.tempDir", "");
				fileName= Prefs.get("figurej.panelFilename", "");
				// IJ.log(tempDir+fileName);
				store(tempDir,fileName);
			}
		});

	}

	public void actionPerformed(ActionEvent evt) {
		// moved each case into buttons action listeners.
	}

	public void close() {
		// super.close();
		instance = null;
		Prefs.saveLocation(LOC_KEY, getLocation());
	}
}
