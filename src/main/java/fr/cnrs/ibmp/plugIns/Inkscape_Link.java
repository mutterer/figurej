package fr.cnrs.ibmp.plugIns;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;

import fr.cnrs.ibmp.dataSets.ExternalDataSource;
import fr.cnrs.ibmp.treeMap.LeafPanel;
import fr.cnrs.ibmp.windows.MainWindow;
import ij.IJ;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GUI;
import ij.gui.Roi;

/**
 * This class controls communication with Inkscape.
 * 
 * @author Jerome Mutterer
 * @author Edda Zinck 
 */
public class Inkscape_Link extends Link {

	public static final String LOC_KEY = "inkscape.loc";
	private static final long serialVersionUID = 1L;
	private static String tempDir = IJ.getDirectory("startup") + "temp"
			+ File.separator;
	private static Frame instance;
	private JButton createISPanelButton = new JButton("Create SVG panel");
	private JButton grabButton = new JButton("Grab view");
	private JButton editButton = new JButton("Edit svg");
	private JButton helpButton = new JButton("Help");

	String[] portSettings = new String[3];

	/**
	 * Creates an {@link Inkscape_Link} with default settings.
	 */
	public Inkscape_Link() {
		super();
		init();
	}

	public Inkscape_Link(MainWindow main) {
		super(main);
		init();
	}

	private void init() {
		linkIdentifier = "Inkscape_";
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
				// TODO This should be unnecessary if Link listens to LeafSelected events
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
				fileName = linkIdentifier+timeStamp()+".svg";
				writeToFile(tempDir+fileName,svg);
				if (getActivePanel() instanceof LeafPanel) {
					ExternalDataSource externalDataSource = new ExternalDataSource();
					externalDataSource.setExternalSource(fileName);
					externalDataSource.setFileDirectory(tempDir);

					LeafPanel activePanel = (LeafPanel) getActivePanel();
					activePanel.setImgData(externalDataSource);
				}

				String script = "open;"+tempDir+fileName;
				inkscapeNotify(script);
			}
		});

		grabButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				updateActivePanel(); 
				
				if (getActivePanel() instanceof LeafPanel) {
					LeafPanel activePanel = (LeafPanel) getActivePanel();

					if (activePanel.getImgData() instanceof ExternalDataSource) {
						ExternalDataSource externalDataSource = (ExternalDataSource) activePanel.getImgData();

						fileName = externalDataSource.getExternalSource();
						tempDir = externalDataSource.getFileDirectory();

						// Execute script to generate a PNG version of the source image
						String script = "grab;"+tempDir+fileName;
						inkscapeNotify(script);

						String svgFileName = fileName.replaceAll(".svg", ".png");
						externalDataSource.setFileName(svgFileName); 
						store(tempDir, svgFileName);
					} else {
						// TODO Ask user if (s)he really wants to replace the DataSource
					}
				}
			}
		});

		editButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				updateActivePanel(); 
				
				if (getActivePanel() instanceof LeafPanel) {
					LeafPanel activePanel = (LeafPanel) getActivePanel();

					if (activePanel.getImgData() instanceof ExternalDataSource) {
						ExternalDataSource externalDataSource = (ExternalDataSource) activePanel.getImgData();

						fileName = externalDataSource.getExternalSource(); 
						tempDir = externalDataSource.getFileDirectory(); 
						String script = "open;"+tempDir+fileName;
						inkscapeNotify(script);
					}
				}
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

	void inkscapeNotify(String script) {
		runMyMacroFromJar("inkscapeNotify.ijm", script);
	}

	public void close() {
		// super.close();
		instance = null;
		Prefs.saveLocation(LOC_KEY, getLocation());
	}
}
