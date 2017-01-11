// TODO Missing license header

package fr.cnrs.ibmp.windows;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;

import fr.cnrs.ibmp.DataSourceEvent;
import fr.cnrs.ibmp.DataSourceListener;
import fr.cnrs.ibmp.ImageSelectionEvent;
import fr.cnrs.ibmp.ImageSelectionListener;
import fr.cnrs.ibmp.LeafEvent;
import fr.cnrs.ibmp.LeafListener;
import fr.cnrs.ibmp.SeparatorEvent;
import fr.cnrs.ibmp.SeparatorListener;
import fr.cnrs.ibmp.dataSets.DataSource;
import fr.cnrs.ibmp.dataSets.DataSources;
import fr.cnrs.ibmp.dataSets.FileDataSource;
import fr.cnrs.ibmp.dataSets.ImageDataSource;
import fr.cnrs.ibmp.treeMap.LeafPanel;
import fr.cnrs.ibmp.treeMap.Panel;
import fr.cnrs.ibmp.utilities.Constants;
import fr.cnrs.ibmp.utilities.Interpolation;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.Toolbar;
import ij.io.OpenDialog;
import ij.process.FloatPolygon;
import imagescience.transform.Affine;

/**
 * Control window for panels; allows splitting of panels as well as
 * assigning of an image to a panel, open / change this image and select a
 * ROI. contains the method to transfer the ROI pixels to the panel.
 * <p>
 * (c) IBMP-CNRS
 * </p>
 * @author Edda Zinck
 * @author Jerome Mutterer
 * @author Stefan Helfrich (University of Konstanz)
 */
public class FigureControlPanel extends JFrame implements LeafListener,
	SeparatorListener, DataSourceListener, ImageSelectionListener
{

	//split leaf panels
	private JButton splitHButton;
	private JButton splitVButton;
	private JButton removePanelButton;
	private JButton debugButton;

	// copy data source properties of a panel
	private JButton copyDataButton;
	private JButton pasteDataButton;
	private JButton clearPixelsButton;
	private JButton optionsButton;
	@SuppressWarnings("rawtypes")
	private JComboBox interpolationType;
	private JTextField filePathLabel = new JTextField("<path>");

	private DataSource copiedImageData = null;
	private FloatPolygon roiGeometryOnly;
	
	//controls for the image region selection / ROI tool
	private JButton imageOKButton;
	public JButton imageCancelButton;
	private JButton openImageButton;
	private JButton logoButton;
	
	private static final long serialVersionUID = 1L;
	
	@SuppressWarnings("rawtypes")
	private JComboBox splitNr = new JComboBox();
	private JPanel cellPanel = new JPanel(new GridLayout(3, 1, 0, 0));
	private JTextArea notes = new JTextArea(4, 1);

	private LeafPanel activePanel;
	private ImagePlus openedImage;

	private MainController mainController;

	private EventListenerList listeners = new EventListenerList();

	/**
	 * TODO Documentation
	 * 
	 * @param initialPosition
	 */
	public FigureControlPanel(MainController mainController, Point initialPosition) {

		this.mainController = mainController;
		this.activePanel = mainController.getActivePanel();
		
		// build GUI
		this.setTitle("Figure Control");
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.setLocation(initialPosition);

		setButtons();
		showPanelCoordinates();
		fillComboBoxes();
		addPanelWindowToolTips();

		ROIToolWindow roiTool = mainController.getRoiTool();
		roiTool.addImageSelectionListener(this);
		
		setGUIImageFrameValues(null);
		setNotesListener();

		this.add(cellInit(), BorderLayout.NORTH);
		this.add(imageInit(), BorderLayout.CENTER);

		if (Prefs.get("figurej.externalTools", 0) == 1)
			this.add(new PluginPanel(mainController), BorderLayout.SOUTH);

		this.setBackground(Constants.backgroundColor);
		this.pack();
		this.setVisible(true);
	}

	/**
	 * displays the coordinates of the selected panel in the lower line of
	 * the imageJ main window
	 */
	private void showPanelCoordinates() {
		if (activePanel != null)
			IJ.showStatus("x=" + activePanel.getX() + ", y="
					+ activePanel.getY() + ", w=" + activePanel.getW()
					+ ", h=" + activePanel.getH());
	}

	/** arrange the upper buttons which control a panel (split e.g.) */
	private JPanel cellInit() {
		cellPanel.setBorder(BorderFactory.createTitledBorder("Layout"));
		cellPanel.setLayout(new GridLayout(3, 2));

		cellPanel.add(splitHButton);
		cellPanel.add(removePanelButton);

		cellPanel.add(splitVButton);
		cellPanel.add(optionsButton);

		cellPanel.add(splitNr);
		// if (Prefs.get("figurej.debug", 0)==1) cellPanel.add(debugButton);

		return cellPanel;
	}

	/** arrange the lower buttons that handle image and image info data */
	private JPanel imageInit() {
		JPanel imagePanel = new JPanel(new BorderLayout());

		imagePanel.setBorder(BorderFactory.createTitledBorder("Image"));

		JPanel p0 = new JPanel(new GridLayout(1, 1));
		JPanel p1 = new JPanel(new GridLayout(1, 1));
		JPanel p2 = new JPanel(new GridLayout(1, 1));
		JPanel p3 = new JPanel(new GridLayout(1, 1));
		JPanel p4 = new JPanel(new GridLayout(1, 2));
		JPanel p5 = new JPanel(new GridLayout(1, 1));

		JPanel upper = new JPanel(new GridLayout(3, 1));
		JPanel middle = new JPanel(new GridLayout(3, 1));

		p0.add(new JLabel("path:"));
		p1.add(filePathLabel);
		p2.add(new JLabel("notes:"));
		filePathLabel.setEditable(false);

		filePathLabel.setColumns(15);

		upper.add(p0);
		upper.add(p1);
		upper.add(p2);

		middle.add(copyDataButton);
		middle.add(pasteDataButton);
		middle.add(clearPixelsButton);

		JPanel copyPasteIconButtons = new JPanel(new GridLayout(1, 2));
		copyPasteIconButtons.add(middle);

		logoButton.setIcon(new ImageIcon(getClass().getResource(
				"/imgs/logo.png")));
		copyPasteIconButtons.add(logoButton);

		p4.add(interpolationType);
		p3.add(openImageButton);
		p5.add(imageOKButton);
		p5.add(imageCancelButton);

		JPanel openImageButtons = new JPanel(new GridLayout(3, 1));
		openImageButtons.add(p3);
		openImageButtons.add(p4);
		openImageButtons.add(p5);

		JPanel lower = new JPanel(new BorderLayout());
		lower.add(copyPasteIconButtons, BorderLayout.CENTER);
		lower.add(openImageButtons, BorderLayout.SOUTH);

		imagePanel.add(upper, BorderLayout.NORTH);
		notes.setLineWrap(true);
		notes.setMaximumSize(notes.getSize());

		JScrollPane areaScrollPane = new JScrollPane(notes);
		areaScrollPane
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		imagePanel.add(areaScrollPane, BorderLayout.CENTER);
		imagePanel.add(lower, BorderLayout.SOUTH);

		return imagePanel;
	}

	/**
	 * fill the drop-down lists indicating into how many child panels a
	 * panel is split and which interpolation types can be chosen to scale
	 * images upon filling a panel with an image
	 */
	private void fillComboBoxes() {
		initSplitNr();
		initInterpolationType();
	}

	/**
	 * TODO Documentation
	 */
	private void initSplitNr() {
		// TODO Implement custom ComboBoxModel?
		splitNr = new JComboBox();
		
		// TODO Replace 10 with descriptive constant
		for (Integer i = 0; i < 10; i++)
			splitNr.addItem(i);
		
		// Set default
		splitNr.setSelectedItem(2);
	}

	/**
	 * TODO Documentation
	 */
	private void initInterpolationType() {
		interpolationType = new JComboBox();
		
		for (String interpolationName : getInterpolationTypes())
			interpolationType.addItem(interpolationName);
		
		// Set default
		interpolationType.setSelectedItem(Interpolation.QUINTIC_B_SPLINE.name);
	}

	/**
	 * TODO Documentation
	 * 
	 * @return
	 */
	private List<String> getInterpolationTypes() {
		List<String> l = new LinkedList<String>();

		for (Interpolation interpolation : Interpolation.values()) {
			l.add(interpolation.name);
		}

		return l;
	}

	/**
	 * Set image path and image notes.
	 */
	private void setGUIImageFrameValues(final DataSource dataSource) {
		if (dataSource == null) {
			filePathLabel.setText("<path>");
			notes.setText("");
			return;
		}

		filePathLabel.setText(dataSource.getStringRepresentation());
		notes.setText(dataSource.getNotes());
	}

	/**
	 * add a listener to the notes GUI field that stores the changes in the
	 * right dataSource object
	 */
	private void setNotesListener() {

		notes.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent arg0) {
				DataSource imgData = activePanel.getImgData();
				if (imgData != null) {
					imgData.setNotes(notes.getText());
				}
			}

			@Override
			public void insertUpdate(DocumentEvent arg0) {
				DataSource imgData = activePanel.getImgData();
				if (imgData != null) {
					imgData.setNotes(notes.getText());
				}
			}

			@Override
			public void changedUpdate(DocumentEvent arg0) {
				DataSource imgData = activePanel.getImgData();
				if (imgData != null) {
					imgData.setNotes(notes.getText());
				}
			}
		});

	}

	/** initialize the buttons, set names and listeners */
	private void setButtons() {
		logoButton = new JButton();
		logoButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new AboutDialog().run("");
			}
		});

		optionsButton = new JButton("more" + new Character((char) 8230));
		optionsButton.setPreferredSize(getMinimumSize());
		optionsButton.addActionListener(mainController);
		
		splitHButton = new JButton("split -");
		splitHButton.addActionListener(new ActionListener() {
			// split active panel horizontally in as many children as chosen
			// in the splitNr combo box
			@Override
			public void actionPerformed(ActionEvent e) {
				activePanel.split((Integer) splitNr.getSelectedItem(), true);

				notifyLeafSplit(new LeafEvent(activePanel));
			}
		});
		
		splitVButton = new JButton("split |");
		splitVButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// split active panel vertically in as many children as
				// chosen in the splitNr combo box				
				activePanel.split((Integer) splitNr.getSelectedItem(), false);

				notifyLeafSplit(new LeafEvent(activePanel));
			}
		});

		openImageButton = new JButton("open image");
		openImageButton.addActionListener(new ActionListener() {
			// open the image and selector to specify the pixels the active
			// panel is filled with
			@Override
			public void actionPerformed(ActionEvent e) {
				openTiltedROITool(false);
			}
		});
		imageOKButton = new JButton("ok");
		imageOKButton.setEnabled(false);
		imageOKButton.addActionListener(new ActionListener() {
			// fill a panel with pixels
			// store possibly run image processing steps; close channel
			// selection tool if necessary
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Decrease code duplication
				setROIToolOpenable(false);
				setControlFrameButtonStates(false);

				if (openedImage.isComposite() && WindowManager.getFrame("Channels") != null)
					(WindowManager.getFrame("Channels")).dispose();

				ROIToolWindow roiTool = (ROIToolWindow) Toolbar.getPlugInTool();
				roiTool.extractRegion();

				// Close openedImage
				openedImage.close();

				// Switch to FigureJ Tool
				mainController.activateFigureJTool();
			}
		});

		imageCancelButton = new JButton("cancel");
		imageCancelButton.setEnabled(false);
		imageCancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Decrease code duplication
				// do not transfer the selected pixels, clear/close the
				// recorder and close the channel selection tool if
				// necessary
				setROIToolOpenable(true);
				setControlFrameButtonStates(true);
				
				// TODO Clear macro recorder
				
				// TODO Close Channels Tool
				if (openedImage.isComposite() && WindowManager.getFrame(
					"Channels") != null) (WindowManager.getFrame("Channels")).dispose();

				// Close openedImage
				openedImage.close();
				
				// Switch to FigureJ Tool
				mainController.activateFigureJTool();
			}
		});

		removePanelButton = new JButton("remove");
		removePanelButton.addActionListener(new ActionListener() {
			// remove the active panel and get a new one
			@Override
			public void actionPerformed(ActionEvent e) {
				notifyLeafRemoved(new LeafEvent(activePanel));
				
				Panel p = mainController.getSelectedPanel();
				if (p instanceof LeafPanel)
					activePanel = (LeafPanel) p;
			}
		});

		copyDataButton = new JButton("copy");
		copyDataButton.addActionListener(new ActionListener() {
			// copy image properties of a panel to another

			@Override
			public void actionPerformed(ActionEvent e) {
				copiedImageData = activePanel.getImgData();

				if (copiedImageData instanceof ImageDataSource) {
					if ((e.getModifiers() & ActionEvent.ALT_MASK) != 0) {
						// TODO dump the roitool coordinates to the roi manager.
						// TODO later use this in the roi tool....
						// was suggested by ton timmers.
						// * suggested workflow:
						// * on figure panel, alt-copy copies source region roi
						// only
						// * then upon alt open or (alt paste), the new
						// selection tool targets as-similar-as-possible region
						// * that could fill the target panel...
						
						ImageDataSource copiedImageDataSource = (ImageDataSource) copiedImageData;
						float[] x = new float[copiedImageDataSource.getSourceX().length];
						float[] y = new float[x.length];
						for (int i = 0; i < x.length; i++) {
							x[i] = (float) copiedImageDataSource.getSourceX()[i];
							y[i] = (float) copiedImageDataSource.getSourceY()[i];
						}
						setRoiGeometryOnly(new FloatPolygon(x, y, 4));
					}
				}
			}
		});

		pasteDataButton = new JButton("paste");
		pasteDataButton.addActionListener(new ActionListener() {
			// copy image properties of one panel to another
			@Override
			public void actionPerformed(ActionEvent e) {
				if ((e.getModifiers() & ActionEvent.ALT_MASK) != 0) {
					openTiltedROITool(true);
				} else if (copiedImageData != null) {
					activePanel.setImgData(DataSources.newDataSource(copiedImageData));
					
					notifyLeafSelected(new LeafEvent(activePanel));
					openTiltedROITool(false);
				}
			}
		});

		clearPixelsButton = new JButton("clear");
		clearPixelsButton.addActionListener(new ActionListener() {
			// remove the assigned image from a panel and clear the
			// dataSource
			@Override
			public void actionPerformed(ActionEvent e) {
				activePanel.clear();

				notifyLeafCleared(new LeafEvent(activePanel));
			}
		});

		debugButton = new JButton("xtra");
		debugButton.addActionListener(new ActionListener() {
			// TEST BUTTON ONLY VISIBLE WHEN FIGUREJ IN DEBUG MODE.
			@Override
			public void actionPerformed(ActionEvent e) {
				mainController.getMainWindow().getRootPanel().addChild(
						new LeafPanel(100, 100, 100, 100));
				mainController.getMainWindow().draw();
			}
		});

	}

	private void addPanelWindowToolTips() {
		notes.setToolTipText("enter your own notes here");
		interpolationType.setToolTipText("select interpolation type");
		splitNr.setToolTipText("n of panels appearing when splitting");
		splitHButton.setToolTipText("divide panel horizontally");
		splitVButton.setToolTipText("divide panel vertically");
		openImageButton
		.setToolTipText("select an image to fill the panel with");
		imageOKButton
				.setToolTipText("fill panel with selected image region");
		imageCancelButton.setToolTipText("cancel panel filling");
		removePanelButton.setToolTipText("delete selected panel");
		copyDataButton.setToolTipText("copy panel content");
		pasteDataButton.setToolTipText("paste panel content");
		clearPixelsButton.setToolTipText("remove image from panel");
	}

	/**
	 * Opens the image that belongs to the selected panel or a file chooser dialog
	 * if none assigned yet. Toggles the tool which is used to select the region
	 * of the image that shall be visible on the panel.
	 * 
	 * @param reuseGeometry TODO Documentation
	 */
	private void openTiltedROITool(boolean reuseGeometry) {
		// TODO Rename this method and refactor
		setROIToolOpenable(false); // handle buttons
		setControlFrameButtonStates(false); // again buttons

		DataSource imgData = activePanel.getImgData();

		int nrOfOpenImgs = WindowManager.getImageCount();
		if (imgData == null) {
			// Active panel is empty. Show file chooser.
			OpenDialog opener = new OpenDialog("Choose an image to fill your panel with!", "");
			if (opener.getFileName() != null && !opener.getFileName().equals("")) {
				FileDataSource fileDataSource = new FileDataSource();
				fileDataSource.setFileDirectory(opener.getDirectory());
				fileDataSource.setFileName(opener.getFileName());
				imgData = fileDataSource;
			} else {
				setROIToolOpenable(true);
				return;
			}
		}

		// Open ImagePlus from DataSource
		openedImage = (ImagePlus) imgData.open();
		openedImage.show();

		// Activate ROIToolWindow on active image
		Toolbar toolbar = Toolbar.getInstance();
		toolbar.setTool(ROIToolWindow.toolName);
		int toolId = toolbar.getToolId(ROIToolWindow.toolName);
		toolbar.setTool(toolId);

		// TODO What does this do?
		// try to use the same roi on different image
		if (imgData instanceof ImageDataSource && reuseGeometry) {
				FloatPolygon r = getRoiGeometryOnly();
				double[] x = new double[4];
				double[] y = new double[4];
				for (int i = 0; i < x.length; i++) {
					x[i] = r.xpoints[i];
					y[i] = r.ypoints[i];
				}
		
				((ImageDataSource) imgData).setCoords(x, y);
		}

		if (openedImage.getCanvas() == null) {
			tryToCatchImageOpenFailure(nrOfOpenImgs);
		}

		if (openedImage.getCanvas() == null) {
			// if trying to open strange files, e.g. java class files (the imagePlus
			// itself != null: do nothing and return to the main window
			handleImageOpenFailure();
			return;
		}

		// Place the new image sensibly on screen
		openedImage.getWindow().setLocation(computeOptimalImageLocation());

		// TODO Do we need this?
		// TODO Does this trigger an event?
//		imgData.setFileDirectory(imgData.getFileDirectory());
//		imgData.setFileName(imgData.getFileName());

		// Add closing adapter
		openedImage.getWindow().addWindowListener(new SelectionWindowClosingAdapter());

		// Update DataSource of LeafPanel
		activePanel.setImgData(imgData);
	}

	/**
	 * Computes the optimal location for a new image that is opened with respect
	 * to the main panel.
	 * 
	 * @return Optimal location for a new image
	 */
	private Point computeOptimalImageLocation() {
		return new Point(mainController.getPanelDimension().width + Constants.guiBorder + 30,
			mainController.getNewOpenSaveFrame().getHeight()
					+ mainController.getNewOpenSaveFrame().getLocation().y + Constants.guiBorder
					+ 20);
	}

	/**
	 * TODO Documentation
	 * 
	 * @param nrOfOpenImgs
	 */
	private void tryToCatchImageOpenFailure(int nrOfOpenImgs) {
		if (nrOfOpenImgs < 1)
			return;
		if (nrOfOpenImgs < WindowManager.getImageCount())
			openedImage = WindowManager.getCurrentImage();
	}

	/**
	 * show an error message and activate the main frame, reset the paths
	 * and enable the image open button
	 */
	private void handleImageOpenFailure() {
		setROIToolOpenable(true);
		activePanel.setImgData(null);
		IJ.error("failed to open the image.");
	}
	
	/**
	 * @return the roiGeometryOnly
	 */
	public FloatPolygon getRoiGeometryOnly() {
		return roiGeometryOnly;
	}

	/**
	 * @param roiGeometryOnly
	 *            the roiGeometryOnly to set
	 */
	public void setRoiGeometryOnly(FloatPolygon roiGeometryOnly) {
		this.roiGeometryOnly = roiGeometryOnly;
	}

	/**
	 * TODO Documentation
	 * 
	 * @param isTrue
	 *            : if true disable all buttons of the panel window else enables
	 *            all helpful e.g. if separators are dragged
	 */
	void disableAllPanelWindowButtons(boolean isTrue) {
		openImageButton.setEnabled(!isTrue);
		imageOKButton.setEnabled(false);
		imageCancelButton.setEnabled(false);

		splitHButton.setEnabled(!isTrue);
		splitVButton.setEnabled(!isTrue);
		removePanelButton.setEnabled(!isTrue);

		copyDataButton.setEnabled(!isTrue);
		pasteDataButton.setEnabled(!isTrue);
		clearPixelsButton.setEnabled(!isTrue);
	}

	/**
	 * TODO Documentation
	 * 
	 * @param isTrue
	 *            if true enables the button that allows to open the image
	 *            attached to a panel with the ROI tool so that an image region
	 *            can be selected to fill the panel with. "OK" and "cancel" are
	 *            disabled. if false it is assumed that the image with the ROI
	 *            tool is already open: the image open button is disabled to
	 *            avoid two open ROI tools; "OK" and "cancel" to either take
	 *            over or ignore the changes made on the ROI are enabled.
	 */
	void setROIToolOpenable(boolean isTrue) {
		openImageButton.setEnabled(isTrue);
		imageOKButton.setEnabled(!isTrue);
		imageCancelButton.setEnabled(!isTrue);
	}

	/**
	 * TODO Documentation
	 * 
	 * @param isTrue
	 *            if true activates all the buttons that can be clicked, if a
	 *            panel is selected. else disables the buttons that do not work
	 *            when an image (ROI tool) is open.
	 */
	void setControlFrameButtonStates(boolean isTrue) {
		splitHButton.setEnabled(isTrue);
		splitVButton.setEnabled(isTrue);
		removePanelButton.setEnabled(isTrue);

		copyDataButton.setEnabled(isTrue);
		pasteDataButton.setEnabled(isTrue);
		clearPixelsButton.setEnabled(isTrue);
	}

	/**
	 * TODO Documentation
	 * 
	 * @return
	 */
	private int getSelectedInterpolation() {
		String interpolation = (String) interpolationType.getSelectedItem();
		int interpolationScheme = Affine.BSPLINE5;
		if (interpolation.equals("nearest neighbor"))
			interpolationScheme = Affine.NEAREST;
		else if (interpolation.equals("linear"))
			interpolationScheme = Affine.LINEAR;
		else if (interpolation.equals("cubic convolution"))
			interpolationScheme = Affine.CUBIC;
		else if (interpolation.equals("cubic B-spline"))
			interpolationScheme = Affine.BSPLINE3;
		else if (interpolation.equals("cubic O-MOMS"))
			interpolationScheme = Affine.OMOMS3;
		else if (interpolation.equals("quintic B-spline"))
			interpolationScheme = Affine.BSPLINE5;
		return interpolationScheme;
	}

	@Override
	public void leafSelected(LeafEvent e) {
		DataSource imgData = activePanel.getImgData();
		if (imgData != null) {
			activePanel.getImgData().removeListener(this);
		}

		LeafPanel leafPanel = (LeafPanel) e.getSource();
		activePanel = leafPanel;

		imgData = activePanel.getImgData();
		if (imgData != null) {
			activePanel.getImgData().addListener(this);
		}

		showPanelCoordinates();

		// Enable the image opening-related buttons
		disableAllPanelWindowButtons(false);

		// Update the UI
		setGUIImageFrameValues(imgData);
	}

	@Override
	public void leafDeselected(LeafEvent e) { /* NB */ }

	@Override
	public void leafResized(LeafEvent e) { /* NB */ }
	
	@Override
	public void separatorSelected(SeparatorEvent e) {
		disableAllPanelWindowButtons(true);
	}

	@Override
	public void separatorDeselected(SeparatorEvent e) { /* NB */ }

	@Override
	public void separatorResized(SeparatorEvent e) { /* NB */ }

	@Override
	public void leafCleared(LeafEvent e) {
		filePathLabel.setText("<path>");
	}

	public void addLeafListener(LeafListener listener) {
		listeners.add(LeafListener.class, listener);
	}

	public void removeLeafListener(LeafListener listener) {
		listeners.remove(LeafListener.class, listener);
	}

	protected synchronized void notifyLeafCleared(LeafEvent event) {
		for (LeafListener l : listeners.getListeners(LeafListener.class))
			l.leafCleared(event);
	}
	
	protected synchronized void notifyLeafRemoved(LeafEvent event) {
		for (LeafListener l : listeners.getListeners(LeafListener.class))
			l.leafRemoved(event);
	}

	protected synchronized void notifyLeafSplit(LeafEvent event) {
		for (LeafListener l : listeners.getListeners(LeafListener.class))
			l.leafSplit(event);
	}

	protected synchronized void notifyLeafSelected(LeafEvent event) {
		for (LeafListener l : listeners.getListeners(LeafListener.class))
			l.leafSelected(event);
	}

	@Override
	public void leafRemoved(LeafEvent e) { /* NB */ }

	@Override
	public void leafSplit(LeafEvent e) {
		activePanel = (LeafPanel) mainController.getSelectedPanel();
	}

	@Override
	public void dataSourceChanged(DataSourceEvent e) {
		setGUIImageFrameValues((DataSource) e.getSource());
	}

	/**
	 * Specifies the closing behavior of the {@link ImagePlus} that is processed
	 * with the {@link ROIToolWindow}.
	 */
	public class SelectionWindowClosingAdapter extends WindowAdapter {

		@Override
		public void windowClosing(WindowEvent wEvent) {
			// Restore state of UI
			setROIToolOpenable(true);
			setControlFrameButtonStates(true);

			// Activate the FigureJTool
			mainController.activateFigureJTool();
		}

	}

	@Override
	public void imageSelected(ImageSelectionEvent e) {
		// Restore state of UI
		setROIToolOpenable(true);
		setControlFrameButtonStates(true);
	}

}

