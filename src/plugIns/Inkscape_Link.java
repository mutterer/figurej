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

import treeMap.LeafPanel;
import windows.MainWindow;

/**
 * @author Jerome Mutterer
 * @author Edda Zinck this class controls communication with Inkscape
 */

public class Inkscape_Link extends Link implements ActionListener {

	public static final String LOC_KEY = "inkscape.loc";
	private static final long serialVersionUID = 1L;
	private static String tempDir = IJ.getDirectory("startup") + "temp"
			+ File.separator;
	private static Frame instance;
	private JButton createISPanelButton = new JButton("Create svg panel");
	private JButton grabButton = new JButton("Grab view");
	private JButton editButton = new JButton("Edit svg");
	private JButton helpButton = new JButton("Help");

	// TODO modifiers?!
	String[] portSettings = new String[3];

	public Inkscape_Link() {
		super();
		init();
	}

	public Inkscape_Link(MainWindow main) {
		super(main);
		init();
	}

	private void init() {
		linkIdentifyer = "Inkscape_";
		// fileName = linkIdentifyer+counter+extension;
		if (instance != null) {
			WindowManager.toFront(instance);
			return;
		}
		WindowManager.addWindow(this);
		instance = this;
		setLayout(new FlowLayout());

		addListeners();
		setTitle("Inkscape Link");
		add(createISPanelButton);
		add(grabButton);
		add(editButton);
		add(helpButton);

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
		createISPanelButton.addActionListener(new ActionListener() {
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
				String svg = getTextFromJar("imgs/empty.svg");
				svg = svg.replaceAll("##WIDTH##", IJ.d2s(width, 0));
				svg = svg.replaceAll("##HEIGHT##", IJ.d2s(height, 0));
				fileName = linkIdentifyer+timeStamp()+".svg";
				createFile (tempDir+fileName,svg);
				((LeafPanel) getActivePanel()).getImgData().setExternalSource(fileName);
				((LeafPanel) getActivePanel()).getImgData().setFileDirectory(tempDir);
				String script = "open;"+tempDir+fileName;
				inkscapeNotify(script);

			}
		});

		grabButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				updateActivePanel(); 
				fileName = ((LeafPanel) getActivePanel()).getImgData().getExternalSource(); 
				tempDir = ((LeafPanel) getActivePanel()).getImgData().getFileDirectory(); 
				String script = "grab;"+tempDir+fileName;
				inkscapeNotify(script);
				fileName = fileName.replaceAll(".svg", ".png");
				((LeafPanel) getActivePanel()).getImgData().setFileName(fileName); 
				store(tempDir,fileName);

			}
		});
		editButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				updateActivePanel(); 
				fileName = ((LeafPanel) getActivePanel()).getImgData().getExternalSource(); 
				tempDir = ((LeafPanel) getActivePanel()).getImgData().getFileDirectory(); 
				String script = "open;"+tempDir+fileName;
				inkscapeNotify(script);

			}
		});

		helpButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				IJ.showMessage(
						"Inkscape Link for FigureJ",
						"-> 'Create Inkscape Panel' creates and opens a new Inkscape document matching the panel size.\n"
								+ "-> 'grab view', well, grabs the Inkscape view (if saved) to the active panel.\n"
								+ "-> 'edit', opens an existing inkscape panel so that you can edit it.\n \n"
								+ "Inkscape should be installed and configured in FigureJ preferences.");
			}
		});
	}

	public void actionPerformed(ActionEvent evt) {
		// moved each case into buttons action listeners.
	}

	void inkscapeNotify(String script) {
		runMyMacroFromJar("inkscapeNotify.ijm", script);
	}

	public void close() {
		// super.close();
		instance = null;
		Prefs.saveLocation(LOC_KEY, getLocation());
	}
}
