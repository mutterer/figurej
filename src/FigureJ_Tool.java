import ij.CommandListener;
import ij.Executer;
import ij.IJ;
import ij.IJEventListener;
import ij.ImageListener;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.gui.Toolbar;
import ij.io.OpenDialog;
import ij.plugin.MacroInstaller;
import ij.plugin.frame.Recorder;
import ij.plugin.tool.PlugInTool;
import ij.process.FloatPolygon;
import imagescience.transform.Affine;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import loci.formats.FormatException;
import loci.plugins.BF;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;
import loci.plugins.in.ImporterPrompter;
import plugIns.LabelDrawer;
import plugIns.Link;
import treeMap.LeafPanel;
import treeMap.Panel;
import treeMap.SeparatorPanel;
import utilities.LabelPosition;
import utilities.LabelType;
import utilities.MyImageMath;
import utilities.Serializer;
import windows.MainWindow;
import windows.NewFigureDialog;
import windows.PluginPanel;
import windows.ROIToolWindow;
import dataSets.DataSource;

/**
 * @author Edda Zinck
 * @author Jerome Mutterer (c) IBMP-CNRS
 * 
 *         main class handles switching and interaction between the result
 *         figure window and the window where image regions are selected.
 *         furthermore this class contains the window to create/open/safe
 *         figures and the panel control window (left one) in the listeners of
 *         this classes elements behavior like enabled-status of buttons or
 *         actions that take place on button clicks are defined.
 */

public class FigureJ_Tool extends PlugInTool implements ImageListener,
		IJEventListener, CommandListener {

	private String title = "FigureJ ";
	private String version = "1.10b";

	// GUI parts and windows
	private ROIToolWindow selectionWindow = new ROIToolWindow(); // image region
	// selection
	// tool /
	// ROI tool
	private MainWindow mainWindow; // result image / panels
	private FigureControlPanel panelWindow; // control window for the active
	// panel
	private AnnotationsAndOptionsPanel optionsWindow; // window displaying
	// optional settings

	private ImagePlus openedImage; // image the ROI tool works on
	private LeafPanel activePanel; // should be a leaf. separators are handled
	// by the main window itself

	// click behavior
	private boolean reactOnRelease = true;
	private boolean mainWindowActive = true;

	// controlling the result image window
	private Frame appNewOpenSaveWindow = new Frame();
	private Button buttonNew;
	private Button buttonOpen;
	private Button buttonSave;

	// split leaf panels
	private JButton splitHButton;
	private JButton splitVButton;
	private JButton removePanelButton;
	private JButton debugButton;

	// copy data source properties of a panel
	private JButton copyDataButton;
	private JButton pasteDataButton;
	private JButton clearPixelsButton;
	private JButton optionsButton;
	private JComboBox interpolationType;
	private JTextField filePathLabel = new JTextField("<path>");

	private DataSource copiedImageData = null;
	private FloatPolygon roiGeometryOnly;

	// controls for the image region selection / ROI tool
	private JButton imageOKButton;
	public JButton imageCancelButton;
	private JButton openImageButton;
	private JButton logoButton;

	// layout stuff
	private Dimension panelDimension = new Dimension(200, 600);
	private int guiBorder = 5;
	private int mainWindowXLocation = 0;
	private int mainWindowYLocation = 0;
	private static int backgroundColor = 0x99aabb;

	// options
	private JButton adoptPixelsButton;
	private JButton openColorPickerButton;
	private JButton openFontsDialog;
	private JButton changeSeparatorColorButton;
	private JButton printFigure;
	private JButton drawLabelButton;
	private JButton removeLabelButton;

	private JButton newTextRoiButton;
	private JButton newArrowRoiButton;
	private JButton textOptionsButton;
	private JButton arrowOptionsButton;
	private JButton addItemToOverlayButton;
	private JButton duplicateItemButton;
	private JButton hideOverlayButton;
	private JButton showOverlayButton;

	// private JButton removeScalebarButton;
	private LabelDrawer labelDraw = new LabelDrawer();

	// object that controls storing and reopening result images
	private Serializer serializer = new Serializer();

	// creates macros if opened images are pre-processed by the user
	private Recorder recorder;

	public FigureJ_Tool() {

		// kill any existing instance in case there is one
		if (WindowManager.getFrame(title) != null)
			WindowManager.getFrame(title).dispose();
		IJ.addEventListener(this);
		initializeFigureJGUI();
		ImagePlus.addImageListener(this);
		Executer.addCommandListener(this);
		run("");
		// add some extra tools to the toolbar.
		installMacroFromJar("panel_sticking_Tool.ijm");
		installMacroFromJar("insets_Tool.ijm");
	}

	/**
	 * upper left window. create and show its three buttons to open, save or
	 * generate a new image.
	 */
	private void initializeFigureJGUI() {
		LeafPanel.setColorValue(backgroundColor);

		appNewOpenSaveWindow.setTitle(title);
		appNewOpenSaveWindow.setLayout(new GridLayout(2, 3));
		appNewOpenSaveWindow.addWindowListener(new FigureJClosingAdaptor()); // specify
		// closing
		// behavior
		appNewOpenSaveWindow.setLocation(0, 0);

		// handle the 3 buttons of this window
		buttonNew = new Button("New");
		buttonOpen = new Button("Open");
		buttonSave = new Button("Save");

		setupButtonsActions();

		buttonNew.setPreferredSize(new Dimension(50, 22));
		buttonOpen.setPreferredSize(new Dimension(50, 22));
		buttonSave.setPreferredSize(new Dimension(50, 22));

		appNewOpenSaveWindow.add(buttonNew);
		appNewOpenSaveWindow.add(buttonOpen);
		appNewOpenSaveWindow.add(buttonSave);
		Label l = new Label(version);
		l.setFont(new Font("sanserif", Font.PLAIN, 9));
		l.setAlignment(Label.RIGHT);
		appNewOpenSaveWindow.add(new Label(""));
		appNewOpenSaveWindow.add(new Label(""));
		appNewOpenSaveWindow.add(l);
		appNewOpenSaveWindow.setBackground(new Color(backgroundColor));

		appNewOpenSaveWindow.pack();

		WindowManager.addWindow(appNewOpenSaveWindow);

		appNewOpenSaveWindow.setVisible(true);
	}

	/**
	 * handles the switch between the two different image windows (main window
	 * with the panel where the result image is composed and the window of the
	 * ROI tool where you can crop the region of a image you want to display).
	 * depending on if the ROI tool is open or not either methods of the one or
	 * of the other class are called.
	 */
	public void mousePressed(ImagePlus imp, MouseEvent e) {
		// switch to main window if there is no image opened with the ROI tool
		// (that would be the openedImage)
		if (WindowManager.getCurrentImage() == null
				|| WindowManager.getCurrentImage() != openedImage) {
			mainWindowActive = true;
			mainWindow.mousePressed(imp, e);
			int count = e.getClickCount();
			Panel p = mainWindow.getSelectedPanel();
			// check whether the user selected a leaf or a separator and make
			// the fitting buttons active (leaf) or not
			if (p.getClass().getName().contains(LeafPanel.class.getName())) {
				activePanel = (LeafPanel) p;
				disableAllPanelWindowButtons(false);
				if (IJ.altKeyDown()) {
					IJ.log(activePanel.getImgData().createLog());
				}
				// from version1b2, double click a panel to open an image.
				// on the mac only, could find a windows fix so far.
				if ((count == 2)&&IJ.isMacintosh())
					panelWindow.openTiltedROITool(false);
			} else {
				activePanel = null;
				disableAllPanelWindowButtons(true);
			}
		} else {
			int count = e.getClickCount();
			if (count == 2) {
				cleanGUIandTransferROI();
			} else
				selectionWindow.mousePressed(imp, e);
		}
	}

	/** calls either the dragging method of the main window or the ROI tool */
	public void mouseDragged(ImagePlus imp, MouseEvent e) {
		if (mainWindowActive)
			mainWindow.mouseDragged(imp, e);
		else
			selectionWindow.mouseDragged(imp, e);
	}

	/**
	 * does either nothing, calls the ROI release method or updates the values
	 * of the active panel's window
	 */
	@Override
	public void mouseReleased(ImagePlus imp, MouseEvent e) {
		if (reactOnRelease) {
			if (mainWindowActive) {
				mainWindow.mouseReleased(imp, e);

				if (activePanel != null) {
					Point a = new Point(mainWindow.getMousePressedPoint()[0],
							mainWindow.getMousePressedPoint()[1]);
					Point b = new Point(mainWindow.getMouseReleasedPoint()[0],
							mainWindow.getMouseReleasedPoint()[1]);
					Line l = new Line(a.x, a.y, b.x, b.y);
					boolean horizontalDrag = (b.x - a.x) * (b.x - a.x) > (b.y - a.y)
							* (b.y - a.y);
					if ((l.getRawLength() > 20)&&(Prefs.get("figurej.mousePanelSlicing", 0) == 1))
						activePanel.split(2, horizontalDrag);
					mainWindow.draw();
					panelWindow.updateValues();
					IJ.showStatus(activePanel.getScaleBarText());
				}
			} else {
				selectionWindow.mouseReleased(imp, e);
			}
		}
	}

	public void mouseMoved(ImagePlus imp, MouseEvent e) {
		if (mainWindowActive) {
			mainWindow.mouseMoved(imp, e);
		} else
			selectionWindow.mouseMoved(imp, e);
	}

	public String getToolIcon() {
		return "CfffF00ff Cf00F50a9 C00fF0a56 Cff0Fbd33 C000 L404f L0444 L09f9 La9af Lacfc Lf0ff";
	}

	public String getToolName() {
		return "FigureJ";
	}

	/** if the tilted rect tool is opened, deactivates the main frame */
	@Override
	public void imageOpened(ImagePlus img) {
		if (img == openedImage)
			mainWindowActive = false;
	}

	@Override
	public void imageUpdated(ImagePlus img) {
	}

	/** handle status of the image buttons */
	@Override
	public void imageClosed(ImagePlus img) {

		if (mainWindow != null)
			// if the ROI tool image is closed activate the main frame; close
			// the channel selection tool if necessary
			if (img == openedImage) {
				setROIToolOpenable(true);
				setControlFrameButtonStates(true);
				if (openedImage.isComposite()
						&& WindowManager.getFrame("Channels") != null)
					( WindowManager.getFrame("Channels")).dispose();
			} else
			// if the result figure itself is closed ask if the user
			// wants to store the result
			if (img == mainWindow.getImagePlus()) {
				if (mainWindow.getQuitWithoutSaving() == false)
					serializer.serialize(mainWindow);
				disableAllPanelWindowButtons(true);
				// allow to open a figure or
				// create a new one
				setOpenNewButtonsStates(true);
			}
		if (optionsWindow != null)
			optionsWindow.dispose();
	}

	/**
	 * the pixels inside the ROI are rotated and scaled to the size of the panel
	 * the image belongs to. the pixel data is passed to the panel, the main
	 * window is set active again; the panels are repainted.
	 */
	private void transferROIDataToPanel(String macro) {
		Panel temp = mainWindow.getSelectedPanel();

		if (temp != null)
			if (temp.getClass().getName().contains(LeafPanel.class.getName())) {
				// remember the size and position of the image fragment the user
				// selected
				double[] xVals = selectionWindow.getXVals().clone();
				double[] yVals = selectionWindow.getYVals().clone();
				
				
				// save coords of source roi for use outside figurej.
				String xRoi = "";
				String yRoi = "";
				for (int i = 0; i < xVals.length; i++) {
					xRoi = xRoi+ IJ.d2s(xVals[i])+",";
					yRoi = yRoi+ IJ.d2s(yVals[i])+",";
				}
				xRoi = xRoi.substring(0, xRoi.length()-1);
				yRoi = yRoi.substring(0, yRoi.length()-1);

				Prefs.set("figurej.xRoi", xRoi);
				Prefs.set("figurej.yRoi", yRoi);


				LeafPanel selectedPanel = (LeafPanel) temp;
				DataSource imageData = selectedPanel.getImgData();

				// store detailed information about the image the user chose for
				// a panel
				imageData.setCoords(xVals, yVals);
				imageData.setPixelCalibration(
						openedImage.getCalibration().pixelWidth, openedImage
								.getCalibration().getUnit());
				imageData.setMacro(macro);
				imageData.setDisplayRange(openedImage.getDisplayRangeMin(),
						openedImage.getDisplayRangeMax());

				// position in stack like and composite images
				imageData.setSlice(openedImage.getSlice());
				imageData.setChannel(openedImage.getChannel());
				imageData.setFrame(openedImage.getFrame());
				WindowManager.setTempCurrentImage(openedImage);
				imageData
						.setActChs(IJ
								.runMacro("ch='';if (is('composite')) Stack.getActiveChannels(ch);return ch;"));

				filePathLabel.setText(imageData.getFileDirectory()
						+ imageData.getFileName());
				// / TODO: DEBUG FROM HERE
				// / IJ.log(imageData.getFileDirectory() +
				// imageData.getFileName());

				float[] xRect = new float[xVals.length];
				float[] yRect = new float[xVals.length];

				for (int i = 0; i < xRect.length; i++) {
					xRect[i] = (float) xVals[i];
					yRect[i] = (float) yVals[i];
				}

				PolygonRoi boundingRect = new PolygonRoi(new FloatPolygon(
						xRect, yRect), PolygonRoi.POLYGON);

				double angle = Math.atan((yVals[3] - yVals[0])
						/ (xVals[3] - xVals[0]))
						* 180 / Math.PI + ((xVals[3] < xVals[0]) ? 180 : 0);

				Line top = new Line(xVals[0], yVals[0], xVals[3], yVals[3]);
				double scaleFactor = selectedPanel.getW() / top.getRawLength();

				// FILL THE PANEL with the pixels selected from the image
				selectedPanel.setPixels(MyImageMath.getPixels(openedImage,
						boundingRect, angle, scaleFactor, selectedPanel.getW(),
						selectedPanel.getH(), getSelectedInterpolation()));
				// calculate the calibration
				imageData.setPixelCalibration(
						(1 / scaleFactor)
								* openedImage.getCalibration().pixelWidth,
						openedImage.getCalibration().getUnit());
				imageData.setAngle(angle);
				imageData.setScaleFactor(scaleFactor);
				// IJ.log(""+Arrays.toString(xVals));
				mainWindow.draw();
				mainWindow.getImagePlus().killRoi();
			}

		setROIToolOpenable(true);
		setControlFrameButtonStates(true);
		mainWindowActive = true;
	}

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

	private void setupButtonsActions() {
		buttonNew.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				NewFigureDialog dialog = new NewFigureDialog();
				// open the dialog and check if the user closed/cancelled it or
				// hit "ok" to create a new figure
				if (dialog.openDialog()) {
					setOpenNewButtonsStates(false);

					// create main- and panel windows and arrange them
					mainWindowXLocation = panelDimension.width + guiBorder + 5;
					mainWindowYLocation = appNewOpenSaveWindow.getHeight()
							+ appNewOpenSaveWindow.getLocation().y + guiBorder;

					mainWindow = new MainWindow(dialog.getWidth(), dialog
							.getHeight(), mainWindowXLocation,
							mainWindowYLocation, dialog.getResolution(), dialog
									.getSeparatorSize());
					mainWindow.calibrateImage(dialog.getResolution(),
							dialog.getUnit());
					Panel p = mainWindow.getSelectedPanel();
					if (p.getClass().getName()
							.contains(LeafPanel.class.getName()))
						activePanel = (LeafPanel) p;
					else
						activePanel = null;

					if (panelWindow != null)
						panelWindow.updateValues();

					else
						panelWindow = new FigureControlPanel();
					panelWindow.setVisible(true);
					labelDraw.setCount(-1);
				}
			}
		});

		buttonSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				IJ.run("Select None"); // assuming the figure is the active
				// window !! remove the ROI showing
				// which panel is selected
				serializer.serialize(mainWindow);
				mainWindow.draw(); // show scale bars and labels again
			}
		});
		buttonOpen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setOpenNewButtonsStates(false);

				mainWindow = null;
				mainWindow = serializer.deserialize();
				// if opened successfully
				if (mainWindow != null) {
					/*
					 * Panel p = mainWindow.getSelectedPanel(); if
					 * (p.getClass().getName()
					 * .contains(LeafPanel.class.getName())) activePanel =
					 * (LeafPanel) p; else activePanel = null;
					 */
					if (panelWindow == null)
						panelWindow = new FigureControlPanel();
					else
						panelWindow.updateValues();
				} else {
					// failed to open
					setOpenNewButtonsStates(true);
					return;
				}
				mainWindow.draw();
			}
		});

		buttonNew.setPreferredSize(new Dimension(90, 30));
		buttonOpen.setPreferredSize(new Dimension(90, 30));
		buttonSave.setPreferredSize(new Dimension(90, 30));
		buttonSave.setEnabled(false);
	}

	/**
	 * @param isTrue
	 *            if true enable buttons to open an existing or create a new
	 *            figure and disables the save button; it is assumed that no
	 *            figure is open currently; else it is assumed that an image is
	 *            already open and therefore only the safe button is enabled
	 */
	private void setOpenNewButtonsStates(boolean isTrue) {
		if (isTrue) {
			buttonNew.setEnabled(true);
			buttonOpen.setEnabled(true);
			buttonSave.setEnabled(false);
		} else {
			buttonNew.setEnabled(false);
			buttonOpen.setEnabled(false);
			buttonSave.setEnabled(true);
		}
	}

	/**
	 * @param isTrue
	 *            : if true disable all buttons of the panel window else enables
	 *            all helpful e.g. if separators are dragged
	 */
	private void disableAllPanelWindowButtons(boolean isTrue) {
		if (isTrue) {
			openImageButton.setEnabled(false);
			imageOKButton.setEnabled(false);
			imageCancelButton.setEnabled(false);

			splitHButton.setEnabled(false);
			splitVButton.setEnabled(false);
			removePanelButton.setEnabled(false);

			copyDataButton.setEnabled(false);
			pasteDataButton.setEnabled(false);
			clearPixelsButton.setEnabled(false);
		} else {
			openImageButton.setEnabled(true);
			imageOKButton.setEnabled(false);
			imageCancelButton.setEnabled(false);

			splitHButton.setEnabled(true);
			splitVButton.setEnabled(true);
			removePanelButton.setEnabled(true);

			copyDataButton.setEnabled(true);
			pasteDataButton.setEnabled(true);
			clearPixelsButton.setEnabled(true);
		}
	}

	/**
	 * @param isTrue
	 *            if true enables the button that allows to open the image
	 *            attached to a panel with the ROI tool so that an image region
	 *            can be selected to fill the panel with. "OK" and "cancel" are
	 *            disabled. if false it is assumed that the image with the ROI
	 *            tool is already open: the image open button is disabled to
	 *            avoid two open ROI tools; "OK" and "cancel" to either take
	 *            over or ignore the changes made on the ROI are enabled.
	 */
	private void setROIToolOpenable(boolean isTrue) {
		if (isTrue) {
			openImageButton.setEnabled(true);
			imageOKButton.setEnabled(false);
			imageCancelButton.setEnabled(false);
		} else {
			openImageButton.setEnabled(false);
			imageOKButton.setEnabled(true);
			imageCancelButton.setEnabled(true);
		}
	}

	/**
	 * @param isTrue
	 *            if true activates all the buttons that can be clicked, if a
	 *            panel is selected. else disables the buttons that do not work
	 *            when an image (ROI tool) is open.
	 */
	private void setControlFrameButtonStates(boolean isTrue) {
		if (!isTrue) {
			splitHButton.setEnabled(false);
			splitVButton.setEnabled(false);
			removePanelButton.setEnabled(false);

			copyDataButton.setEnabled(false);
			pasteDataButton.setEnabled(false);
			clearPixelsButton.setEnabled(false);
		} else {
			splitHButton.setEnabled(true);
			splitVButton.setEnabled(true);
			removePanelButton.setEnabled(true);

			copyDataButton.setEnabled(true);
			pasteDataButton.setEnabled(true);
			clearPixelsButton.setEnabled(true);
		}
	}

	@Override
	/** remove the highlighting of the clicked panel because this could be dragged around e.g. with the arrow tool*/
	public void eventOccurred(int eventID) {
		if (eventID == IJEventListener.TOOL_CHANGED) {
			if (mainWindow != null && mainWindow.getImagePlus() != null)
				mainWindow.getImagePlus().changes = false;
		}
	}

	public static int getBGColor() {
		return backgroundColor;
	}

	/**
	 * specify the closing behavior of the new-open-save window: ask the user if
	 * he wants to save changes, close all tool windows and switch to the imageJ
	 * rectangle tool
	 */
	public class FigureJClosingAdaptor extends WindowAdapter {
		public void windowClosing(WindowEvent wEvent) {
			String info = "";
			if (mainWindow != null) {
				if (mainWindow.getImagePlus().changes)
					info = "\nCurrent figure will be lost.";
				GenericDialog gd = new GenericDialog("FigureJ");
				gd.addMessage("Are you sure you want to quit FigureJ?" + info);
				gd.showDialog();
				if (gd.wasCanceled())
					return;
			}
			if (mainWindow != null)
				mainWindow.setQuitWithoutSaving(true);
			wEvent.getWindow().dispose();
			appNewOpenSaveWindow.dispose();

			IJ.setTool("rectangle");

			if (panelWindow != null)
				panelWindow.dispose();

			// application closed just after opening
			if (mainWindow != null && mainWindow.getImagePlus() != null)
				mainWindow.getImagePlus().close();

			if (optionsWindow != null)
				optionsWindow.dispose();
		}

		public void windowClosed(WindowEvent e) {
			WindowManager.removeWindow((Frame) appNewOpenSaveWindow);
			Toolbar.restoreTools();

		}
	}

	/**
	 * specify the closing behavior of the selecion window
	 */
	public class SelectionWindowClosingAdaptor extends WindowAdapter {
		public void windowClosing(WindowEvent wEvent) {
			// cleanGUIandTransferROI();
			imageCancelButton.doClick();
		}

		public void windowClosed(WindowEvent e) {

		}
	}

	/**
	 * control window for panels; allows splitting of panels as well as
	 * assigning of an image to a panel, open / change this image and select a
	 * ROI. contains the method to transfer the ROI pixels to the panel.
	 */
	private class FigureControlPanel extends JFrame {

		private static final long serialVersionUID = 1L;

		private DataSource data; // data source of the current active panel
		private JComboBox splitNr = new JComboBox();
		private JPanel cellPanel = new JPanel(new GridLayout(3, 1, 0, 0));
		private JTextArea notes = new JTextArea(4, 1);

		public FigureControlPanel() {

			// build GUI
			this.setTitle("Figure Control");
			this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			this.setLocation(0, appNewOpenSaveWindow.getHeight()
					+ appNewOpenSaveWindow.getLocation().y + guiBorder);
			if (activePanel != null)
				data = activePanel.getImgData();

			setButtons();
			showPanelCoordinates();
			fillComboBoxes();
			addPanelWindowToolTips();

			setGUIImageFrameValues();
			setNotesListener();

			this.add(cellInit(), BorderLayout.NORTH);
			this.add(imageInit(), BorderLayout.CENTER);

			if (Prefs.get("figurej.externalTools", 0) == 1)
				this.add(new PluginPanel(mainWindow), BorderLayout.SOUTH);

			this.setBackground(new Color(backgroundColor));
			this.pack();
			this.setVisible(true);
		}

		/** assign the window to a different panel and update the values */
		private void updateValues() {
			data = activePanel.getImgData();
			showPanelCoordinates();
			setGUIImageFrameValues();
			if (optionsWindow != null) {
				optionsWindow.scaleDisplayCheck.setSelected(activePanel
						.isScalebarVisible());
				optionsWindow.scaleTextDisplayCheck.setSelected(activePanel
						.isScalebarTextVisible());
				optionsWindow.scaleTextValue.setText(activePanel
						.getShortScaleBarText());
			}
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
					"imgs/logo.png")));
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

			splitNr = new JComboBox();
			for (Integer i = 0; i < 10; i++)
				splitNr.addItem(i);
			splitNr.setSelectedItem(2);

			interpolationType = new JComboBox();
			ArrayList<String> l = getInterpolationTypes();
			Iterator<String> itr = l.iterator();
			while (itr.hasNext())
				interpolationType.addItem(itr.next());
			interpolationType.setSelectedItem("quintic B-spline");
		}

		private ArrayList<String> getInterpolationTypes() {
			ArrayList<String> l = new ArrayList<String>();
			l.add("nearest neighbor");
			l.add("linear");
			l.add("cubic convolution");
			l.add("cubic B-spline");
			l.add("cubic O-MOMS");
			l.add("quintic B-spline");
			return l;
		}

		/** show image path and image notes on the GUI */
		private void setGUIImageFrameValues() {
			if (data != null) {
				if (data.getFileDirectory() != "")
					filePathLabel.setText(data.getFileDirectory()
							+ data.getFileName());
				notes.setText(data.getNotes());
			}
		}

		/**
		 * add a listener to the notes GUI field that stores the changes in the
		 * right dataSource object
		 */
		private void setNotesListener() {

			notes.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void removeUpdate(DocumentEvent arg0) {
					data.setNotes(notes.getText());
				}

				@Override
				public void insertUpdate(DocumentEvent arg0) {
					data.setNotes(notes.getText());
				}

				@Override
				public void changedUpdate(DocumentEvent arg0) {
					data.setNotes(notes.getText());
				}
			});

		}

		/** initialize the buttons, set names and listeners */
		private void setButtons() {
			logoButton = new JButton();
			logoButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Link.runMyMacroFromJar("figurej_help.ijm", "");
				}
			});

			optionsButton = new JButton("more" + new Character((char) 8230));
			optionsButton.setPreferredSize(getMinimumSize());
			optionsButton.addActionListener(new ActionListener() {
				// display options window
				public void actionPerformed(ActionEvent e) {
					if (optionsWindow == null)
						optionsWindow = new AnnotationsAndOptionsPanel(
								optionsButton.getLocation().x + 50,
								getLocation().y + optionsButton.getLocation().y
										+ 30);
					optionsWindow.setVisible(true);
				}
			});
			splitHButton = new JButton("split -");
			splitHButton.addActionListener(new ActionListener() {
				// split active panel horizontally in as many children as chosen
				// in the splitNr combo box
				public void actionPerformed(ActionEvent e) {
					activePanel.split((Integer) splitNr.getSelectedItem(), true);
					mainWindow.draw();
				}
			});
			splitVButton = new JButton("split |");
			splitVButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// split active panel vertically in as many children as
					// chosen in the splitNr combo box
					activePanel.split((Integer) splitNr.getSelectedItem(),
							false);
					mainWindow.draw();
				}
			});
			// moved to options dialog
			adoptPixelsButton = new JButton("adopt current panel pixels");
			adoptPixelsButton.addActionListener(new ActionListener() {
				// let the active panel grab the pixels drawn on his area and
				// store them
				// is useful if somebody draws with non figureJ tools on the
				// result window and wants to store these changes
				public void actionPerformed(ActionEvent e) {
					if (activePanel != null)
						activePanel.setPixels(mainWindow.getImagePlus());
				}
			});

			changeSeparatorColorButton = new JButton("update separator color");
			changeSeparatorColorButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int newColor = Toolbar.getForegroundColor().getRGB();
					SeparatorPanel.setColor(newColor);
					if (mainWindow != null)
						mainWindow.draw();
				}
			});
			openColorPickerButton = new JButton("open color picker");
			openColorPickerButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					IJ.runPlugIn("ij.plugin.frame.ColorPicker", "");
				}
			});
			
			openFontsDialog = new JButton("open fonts dialog");
			openFontsDialog.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					IJ.runPlugIn("ij.plugin.SimpleCommands", "fonts");
				}
			});

			newTextRoiButton = new JButton("" + new Character((char) 8314)
					+ " T  text");
			newTextRoiButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					boolean fontsWindowsOpen = WindowManager.getFrame("Fonts") != null;
					if (!fontsWindowsOpen) {
						IJ.run("Fonts...");
					}
					IJ.selectWindow("FigureJ");
					IJ.run("Select None");
					IJ.setTool("text");

/*					ImagePlus imp = IJ.getImage();
					imp.createNewRoi((int)(imp.getWidth()/2), (int)(imp.getHeight()/2));
					TextRoi r = (TextRoi) imp.getRoi();					
					String str = "Text..."; 
					char[] charArray = str.toCharArray();
					for (char c : charArray) r.addChar(c);
					r.setLocation((int)(imp.getWidth()/2), (int)(imp.getHeight()/2));
					imp.setRoi(r);
*/
					IJ.showStatus("Text tool selected");

					
				}
			});

			textOptionsButton = new JButton("" + new Character((char) 8230));
			textOptionsButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					IJ.run("Fonts...");
				}
			});

			newArrowRoiButton = new JButton("" + new Character((char) 8314)
					+ new Character((char) 8599) + " arrow");
			newArrowRoiButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
								
					IJ.selectWindow("FigureJ");
					IJ.run("Select None");

					Window fr[] = Window.getWindows();
					boolean arrowsOptionsOpen = false;
					for (int i = 0; i < fr.length; i++) {
						if (fr[i].toString().indexOf("Arrow Tool") > 0
								&& fr[i].isVisible())
							arrowsOptionsOpen = true;
					}
					if (!arrowsOptionsOpen)
						IJ.doCommand("Arrow Tool...");
					IJ.showStatus("Arrow tool selected");

				}

			});

			arrowOptionsButton = new JButton("" + new Character((char) 8230));
			arrowOptionsButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					IJ.doCommand("Arrow Tool...");
				}
			});

			addItemToOverlayButton = new JButton("add");
			addItemToOverlayButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ImagePlus imp = IJ.getImage();
					IJ.run(imp, "Add Selection...", "");
					IJ.run(imp, "Select None", "");
					// String macro = "fc= toHex(getValue('rgb.foreground')); while (lengthOf(fc)<6) {fc='0'+fc;} run('Add Selection...', 'stroke=#'+fc+' fill=none');run('Select None');";
					// IJ.runMacro(macro);
				}
			});

			duplicateItemButton = new JButton("duplicate");
			duplicateItemButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ImagePlus imp = IJ.getImage();
					Roi roi = imp.getRoi();
					if (roi != null) {
						imp.saveRoi();
						// IJ.run(imp, "Add Selection...", "");
//						String macro = "shift=30;getSelectionBounds(x, y, width, height);"
//								+ "fc= toHex(getValue('rgb.foreground')); while (lengthOf(fc)<6) {fc='0'+fc;} run('Add Selection...', 'stroke=#'+fc+' fill=none');run('Select None');"
//								+ "run('Select None');run('Restore Selection', '');setSelectionLocation(x+ shift, y+ shift/1.5);";
						IJ.run(imp, "Add Selection...", "");
						String macro = "run('Restore Selection', '');shift=30;getSelectionBounds(x, y, width, height);setSelectionLocation(x+ shift, y+ shift/1.5);";
						IJ.runMacro(macro);
					}
				}
			});

			hideOverlayButton = new JButton("hide");
			hideOverlayButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					IJ.run("Hide Overlay");
				}
			});

			showOverlayButton = new JButton("show");
			showOverlayButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					IJ.run("Show Overlay");
				}
			});

			printFigure = new JButton("print at actual size");
			printFigure.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					double res = mainWindow.getDPI();
					IJ.run("Select None");
					Link.runMyMacroFromJar("print_to_scale.ijm", IJ.d2s(res));
				}
			});

			drawLabelButton = new JButton("draw");
			drawLabelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// reset flag if alt is pressed, to reset the label counter
					boolean reset = (e.getModifiers() & ActionEvent.ALT_MASK) != 0;
					boolean backwards = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;

					optionsWindow.addPanelLabel(reset, backwards);
					boolean fontsWindowsOpen = WindowManager.getFrame("Fonts") != null;
					if (!fontsWindowsOpen) {
						IJ.run("Fonts...");
						IJ.selectWindow("FigureJ");
					}
					mainWindow.draw();

				}
			});

			removeLabelButton = new JButton("remove");
			removeLabelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					activePanel.removeLabel(mainWindow.getImagePlus()
							.getOverlay());
					mainWindow.draw();
				}
			});

			openImageButton = new JButton("open image");
			openImageButton.addActionListener(new ActionListener() {
				// open the image and selector to specify the pixels the active
				// panel is filled with
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
				public void actionPerformed(ActionEvent e) {
					if (openedImage.isComposite()
							&& WindowManager.getFrame("Channels") != null)
						( WindowManager.getFrame("Channels"))
								.dispose();
					cleanGUIandTransferROI();
				}
			});

			imageCancelButton = new JButton("cancel");
			imageCancelButton.setEnabled(false);
			imageCancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// do not transfer the selected pixels, clear/close the
					// recorder and close the channel selection tool if
					// necessary
					setROIToolOpenable(true);
					setControlFrameButtonStates(true);
					if (openedImage.isComposite()
							&& WindowManager.getFrame("Channels") != null)
						( WindowManager.getFrame("Channels"))
								.dispose();
					if (recorder != null) {
						try {
							recorder.close();
						} catch (Exception e2) {
							System.err.println("recorder nullpointer");
						}
					}
					openedImage.close();
					mainWindowActive = true;
				}
			});

			removePanelButton = new JButton("remove");
			removePanelButton.addActionListener(new ActionListener() {
				// remove the active panel and get a new one
				public void actionPerformed(ActionEvent e) {
					activePanel.hideLabel(mainWindow.getImagePlus()
							.getOverlay());
					activePanel.hideScalebar(mainWindow.getImagePlus()
							.getOverlay());
					activePanel.remove(mainWindow.getImagePlus().getOverlay());
					mainWindow.updateSelectedPanel(true);
					activePanel = (LeafPanel) mainWindow.getSelectedPanel();
					mainWindow.draw();
				}
			});

			copyDataButton = new JButton("copy");
			copyDataButton.addActionListener(new ActionListener() {
				// copy image properties of a panel to another

				public void actionPerformed(ActionEvent e) {
					copiedImageData = activePanel.getImgData();

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

						float[] x = new float[copiedImageData.getSourceX().length];
						float[] y = new float[x.length];
						for (int i = 0; i < x.length; i++) {
							x[i] = (float) copiedImageData.getSourceX()[i];
							y[i] = (float) copiedImageData.getSourceY()[i];
						}
						setRoiGeometryOnly(new FloatPolygon(x, y, 4));
					}

				}
			});

			pasteDataButton = new JButton("paste");
			pasteDataButton.addActionListener(new ActionListener() {
				// copy image properties of one panel to another
				public void actionPerformed(ActionEvent e) {
					if ((e.getModifiers() & ActionEvent.ALT_MASK) != 0) {
						openTiltedROITool(true);
					} else if (copiedImageData != null) {
						activePanel.setImgData(copiedImageData.clone());
						panelWindow.updateValues();
						openTiltedROITool(false);
					}
				}
			});

			clearPixelsButton = new JButton("clear");
			clearPixelsButton.addActionListener(new ActionListener() {
				// remove the assigned image from a panel and clear the
				// dataSource
				public void actionPerformed(ActionEvent e) {
					data.setCoords(null, null);
					data.setExternalSource("");
					data.setFileDirectory("");
					data.setFileName("");
					data.setMacro("");
					data.setSlice(1);
					data.setChannel(1);
					data.setFrame(1);
					data.setDisplayRange(-1., -1.);
					filePathLabel.setText("<path>");
					activePanel.eraseImage();
					activePanel.hideScalebar(mainWindow.getImagePlus()
							.getOverlay());
					mainWindow.draw();
				}
			});

			debugButton = new JButton("xtra");
			debugButton.addActionListener(new ActionListener() {
				// TEST BUTTON ONLY VISIBLE WHEN FIGUREJ IN DEBUG MODE.
				public void actionPerformed(ActionEvent e) {
					mainWindow.getRootPanel().addChild(
							new LeafPanel(100, 100, 100, 100));
					mainWindow.draw();
				}
			});

		}

		private void addPanelWindowToolTips() {
			notes.setToolTipText("enter your own notes here");
			interpolationType.setToolTipText("select interpolation type");
			splitNr.setToolTipText("n of panels appearing when splitting");
			splitHButton.setToolTipText("divide panel horizontally");
			splitVButton.setToolTipText("divide panel vertically");
			newTextRoiButton.setToolTipText("add a new text");
			newArrowRoiButton.setToolTipText("add a new arrow");
			arrowOptionsButton.setToolTipText("open arrows options");
			textOptionsButton.setToolTipText("open text options");
			hideOverlayButton.setToolTipText("hide all overlay items");
			showOverlayButton.setToolTipText("show all overlay items");
			addItemToOverlayButton
					.setToolTipText("add current item to overlay");
			duplicateItemButton
					.setToolTipText("add current item to overlay and duplicates it");
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
		 * opens the image that belongs to the selected panel or a file chooser
		 * dialog if non assigned yet; starts the tool which is used to select
		 * the region of the image that shall be visible on the panel
		 */
		private void openTiltedROITool(boolean reuseGeometry) {
			setROIToolOpenable(false); // handle buttons
			setControlFrameButtonStates(false); // again buttons
			selectionWindow = new ROIToolWindow(activePanel.getW(),
					activePanel.getH()); // new tilted rectangle ROI tool

			int nrOfOpenImgs = WindowManager.getImageCount();
			// IJ.log(activePanel.getImgData().getFileDirectory()+","+activePanel.getImgData().getFileName());
			// IJ.log(""+(activePanel.getImgData().getFileName().isEmpty()));
			// IJ.log(""+(activePanel.getImgData().getFileDirectory().isEmpty()));
			if ((activePanel.getImgData().getFileDirectory() == "")
					|| (activePanel.getImgData().getFileName() == "")
					|| (activePanel.getImgData().getFileDirectory().isEmpty())
					|| (activePanel.getImgData().getFileName().isEmpty())

			) {
				OpenDialog opener = new OpenDialog(
						"Choose an image to fill your panel with!", "");
				if (opener.getFileName() != "") {
					// IJ.log(opener.getFileName());
					data.setExternalSource(""); // b/c an new image datasource
					// was just selected.
					data.setFileDirectory(opener.getDirectory());
					data.setFileName(opener.getFileName());
				} else {
					data.setFileDirectory("");
					data.setFileName("");
					data.setCoords(new double[4], new double[4]);
					mainWindowActive = true;
					setROIToolOpenable(true);
					return;
				}

				String path = data.getFileDirectory() + data.getFileName();

				// TODO improve robustness for complex types, eg. lif datasets
				// as handled by bioformats!!

				if (path.toLowerCase().endsWith(".czi")
						|| path.toLowerCase().endsWith(".zvi")) {
					try {
						ImagePlus[] bfi = BF.openImagePlus(path);
						openedImage = bfi[0];
					} catch (FormatException e) {
						e.printStackTrace();
						IJ.log("Bioformats had problems reading this file.");
					} catch (IOException e) {
						e.printStackTrace();
						IJ.log("Bioformats had problems reading this file.");
					}
				} else if (path.toLowerCase().endsWith(".lif")) {

					try {
						ImporterOptions options = new ImporterOptions();
						options.setId(path);
						options.setFirstTime(false);
						options.setUpgradeCheck(false);
						options.setStackFormat(ImporterOptions.VIEW_HYPERSTACK);
						ImportProcess process = new ImportProcess(options);
						new ImporterPrompter(process);
						process.execute();
						options = process.getOptions();
						options.setStackFormat(ImporterOptions.VIEW_HYPERSTACK);

						int count = process.getSeriesCount();
						int selectedSerie = 0;
						for (int i = 0; i < count; i++) {
							if (options.isSeriesOn(i))
								selectedSerie = i;
						}

						data.setSelectedSerie(selectedSerie);
						// IJ.log("" + selectedSerie);
						ImagePlus[] imps = BF.openImagePlus(options);
						openedImage = imps[imps.length - 1];
					} catch (FormatException e) {
						e.printStackTrace();
						IJ.log("Bioformats had problems reading this file.");
					} catch (IOException e) {
						e.printStackTrace();
						IJ.log("Bioformats had problems reading this file.");
					}

				} else {
					openedImage = IJ.openImage(path);
				}
				if (openedImage == null) {
					tryToCatchImageOpenFailure(nrOfOpenImgs); // grab files that
																// are
					// opened, not returned
					// by opening software
					// (e.g. old lsm reader)
				}
				if (openedImage == null) { // if strange file was selected, go
					// back to the main window
					handleImageOpenFailure();
					return;
				}

			} else {
				String path = activePanel.getImgData().getFileDirectory()
						+ data.getFileName();
				if (path.toLowerCase().endsWith(".czi")
						|| path.toLowerCase().endsWith(".zvi")) {
					try {
						ImagePlus[] bfi = BF.openImagePlus(path);
						openedImage = bfi[0];
					} catch (FormatException e) {
						e.printStackTrace();
						IJ.log("Bioformats has issues reading this file.");
					} catch (IOException e) {
						e.printStackTrace();
						IJ.log("Bioformats has issues reading this file.");
					}
				} else if (path.toLowerCase().endsWith(".lif")) {

					try {
						ImporterOptions options = new ImporterOptions();
						options.setId(path);
						options.setFirstTime(false);
						options.setUpgradeCheck(false);
						options.setStackFormat(ImporterOptions.VIEW_HYPERSTACK);
						options.clearSeries();
						options.setSeriesOn(activePanel.getImgData()
								.getSelectedSerie(), true);
						ImagePlus[] imps = BF.openImagePlus(options);
						openedImage = imps[imps.length - 1];
					} catch (FormatException e) {
						e.printStackTrace();
						IJ.log("Bioformats had problems reading this file.");
					} catch (IOException e) {
						e.printStackTrace();
						IJ.log("Bioformats had problems reading this file.");
					}

				} else {
					openedImage = IJ.openImage(path);
				}
				// else open the assigned image and rotate the ROI to its
				// last angle.
				if (openedImage == null) {
					tryToCatchImageOpenFailure(nrOfOpenImgs);
				}
				if (openedImage == null) { // if strange file was selected, go
					// back to the main window
					handleImageOpenFailure();
					return;
				}

				openedImage.setPosition(data.getChannel(), data.getSlice(),
						data.getFrame());
			}
			openedImage.show();

			selectionWindow.init(openedImage);

			// use old ROI coordinates if panel size did not change
			if (data.getSourceX() != null && data.getSourceY() != null)
				selectionWindow.setCoords(data.getSourceX(), data.getSourceY());

			// try to use the same roi on different image
			if (reuseGeometry) {
				FloatPolygon r = getRoiGeometryOnly();
				double[] x = new double[4];
				double[] y = new double[4];
				for (int i = 0; i < x.length; i++) {
					x[i] = (double) r.xpoints[i];
					y[i] = (double) r.ypoints[i];
				}

				selectionWindow.setCoords(x, y);

			}

			// IJ 1.47o4 introduced a new Recorder constructor: new
			// Recorder(boolean visible)
			recorder = new Recorder(false);

			if (openedImage.getCanvas() == null) {
				tryToCatchImageOpenFailure(nrOfOpenImgs);
			}

			if (openedImage.getCanvas() == null) { // if trying to open strange
				// files, e.g. java class
				// files (the imagePlus
				// itself != null: do nothing and return to the main window
				handleImageOpenFailure();
				return;
			}
			openedImage.getWindow().setLocation(
					panelDimension.width + guiBorder + 30,
					appNewOpenSaveWindow.getHeight()
							+ appNewOpenSaveWindow.getLocation().y + guiBorder
							+ 20);

			// removed, we do not want to reapply preprocessing automatically
			// IJ.runMacro(activePanel.getImgData().getMacro());

			if (openedImage.isComposite()) {
				IJ.doCommand("Channels Tool...");
				if (data.getActChs() == "")
					IJ.runMacro("Stack.setDisplayMode('composite');Stack.setActiveChannels('11111111');");
				else
					IJ.runMacro("Stack.setDisplayMode('composite');Stack.setActiveChannels('"
							+ data.getActChs() + "');");
			}

			selectionWindow.drawRect(openedImage); // show ROI
			activePanel.getImgData().setFileDirectory(data.getFileDirectory());
			activePanel.getImgData().setFileName(data.getFileName());
			openedImage.getWindow().addWindowListener(
					new SelectionWindowClosingAdaptor());
		}

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
			mainWindowActive = true;
			setROIToolOpenable(true);
			data.setFileDirectory("");
			data.setFileName("");
			IJ.error("failed to open the image.");
		}

	}

	/**
	 * window opened by the options button of the panel window
	 */
	private class AnnotationsAndOptionsPanel extends JFrame {

		private static final long serialVersionUID = 1L;

		private JPanel optionsPanel = new JPanel();
		// text labels
		private JTextField userDefinedLabelsInputField = new JTextField();
		private JLabel userDefinedLabelsMessage = new JLabel("  own labels:");
		// arrow 8599 plus superscript 8314
		// private JButton fontSpecButton = new JButton("fonts"+new
		// Character((char) 8230));
		private JTextField xLabelOffset = new JTextField("10");
		private JTextField yLabelOffset = new JTextField("10");
		private JComboBox labelPositionSelector = new JComboBox();
		private JComboBox labelTypeSelector = new JComboBox();
		// scale bars
		private JTextField xOffsScales = new JTextField("20");
		private JTextField yOffsScales = new JTextField("20");
		private JSlider scalebarSizeSlider = new JSlider();
		private JCheckBox scaleDisplayCheck = new JCheckBox();
		private JCheckBox scaleTextDisplayCheck = new JCheckBox("show value: ",
				false);
		private JLabel scaleTextValue = new JLabel("-");
		// suggested by Christian Blanck
		private JCheckBox lockScale = new JCheckBox("Lock pixel size");
		private JTextField scalebarHeight = new JTextField("10");
		private int[] a = { 1, 2, 5 };
		private double[] b = { 0.001, 0.01, 0.1, 1, 10, 100, 1000 };

		private AnnotationsAndOptionsPanel(int xLocation, int yLocation) {

			this.setLocation(xLocation, yLocation);
			this.setTitle("Options");

			initLabelGUI();
			initScalebarGUI();
			addOptionsWindowToolTips();

			// labels layout
			JPanel labelsPanel = new JPanel(new GridLayout(3, 2));
			JPanel labelsOffsets = new JPanel();

			labelsPanel.setPreferredSize(new Dimension(253, 110));
			labelsPanel.setBorder(BorderFactory
					.createTitledBorder("Panel Labels"));

			labelsOffsets.add(new JLabel(new ImageIcon(getClass().getResource(
					"/imgs/ALeft1.png"))));
			labelsOffsets.add(xLabelOffset);
			labelsOffsets.add(new JLabel(new ImageIcon(getClass().getResource(
					"/imgs/ATop1.png"))));
			labelsOffsets.add(yLabelOffset);

			labelsPanel.add(drawLabelButton);
			labelsPanel.add(removeLabelButton);
			labelsPanel.add(labelTypeSelector);
			labelsPanel.add(userDefinedLabelsInputField);
			labelsPanel.add(labelPositionSelector);
			labelsPanel.add(labelsOffsets);

			JPanel scalebarsPanel = new JPanel(new GridLayout(3, 1));
			JPanel scalebarsVisibilityAndSizePanel = new JPanel(new FlowLayout(
					FlowLayout.LEFT));
			JPanel scalebarsOffsetsPanel = new JPanel(new FlowLayout(
					FlowLayout.LEFT));
			JPanel scalebarsTextVisibility = new JPanel(new FlowLayout(
					FlowLayout.LEFT));

			scalebarsPanel.setBorder(BorderFactory
					.createTitledBorder("Panel Scale bars"));

			scalebarsVisibilityAndSizePanel.add(scaleDisplayCheck);
			scalebarsVisibilityAndSizePanel.add(scalebarSizeSlider);

			scalebarsTextVisibility.add(scaleTextDisplayCheck);
			scalebarsTextVisibility.add(scaleTextValue);
			scalebarsOffsetsPanel.add(new JLabel("height:"));
			scalebarsOffsetsPanel.add(scalebarHeight);

			scalebarsOffsetsPanel.add(new JLabel("   "));
			scalebarsOffsetsPanel.add(new JLabel(new ImageIcon(getClass()
					.getResource("/imgs/iconBarRight.png"))));
			scalebarsOffsetsPanel.add(xOffsScales);
			scalebarsOffsetsPanel.add(new JLabel(new ImageIcon(getClass()
					.getResource("/imgs/iconBarLow.png"))));
			scalebarsOffsetsPanel.add(yOffsScales);

			scalebarsPanel.add(scalebarsVisibilityAndSizePanel);
			scalebarsPanel.add(scalebarsTextVisibility);
			scalebarsPanel.add(scalebarsOffsetsPanel);

			// Overlay Items

			JPanel overlayItemsPanel = new JPanel(new GridLayout(3, 2));
			overlayItemsPanel.setPreferredSize(new Dimension(253, 110));
			overlayItemsPanel.setBorder(BorderFactory
					.createTitledBorder("Overlay Items"));
			overlayItemsPanel.add(newTextRoiButton);
			overlayItemsPanel.add(newArrowRoiButton);
			overlayItemsPanel.add(addItemToOverlayButton);
			overlayItemsPanel.add(duplicateItemButton);
			overlayItemsPanel.add(hideOverlayButton);
			overlayItemsPanel.add(showOverlayButton);

			// Miscellaneous Items and temporary test/debug items

			// JPanel miscItemsPanel = new JPanel(new
			// GridLayout(2+(int)Prefs.get("figurej.debug", 0), 1));
			JPanel miscItemsPanel = new JPanel(new GridLayout(4, 1));
			miscItemsPanel.setBorder(BorderFactory.createTitledBorder("Misc."));

			// if (Prefs.get("figurej.debug", 0)==1)
			// miscItemsPanel.add(debugButton);
			Prefs.set("figurej.lockPixelSize", false);

			// TODO: enable this when workflow is made clear.
			// miscItemsPanel.add(lockScale);
			miscItemsPanel.add(openColorPickerButton);
			//miscItemsPanel.add(openFontsDialog);
			miscItemsPanel.add(adoptPixelsButton);
			miscItemsPanel.add(changeSeparatorColorButton);
			miscItemsPanel.add(printFigure);

			// Fill the options panel
			optionsPanel.setLayout(new BoxLayout(optionsPanel,
					BoxLayout.PAGE_AXIS));
			optionsPanel.add(labelsPanel);
			optionsPanel.add(scalebarsPanel);
			optionsPanel.add(overlayItemsPanel);
			optionsPanel.add(miscItemsPanel);

			this.add(optionsPanel);
			this.setBackground(new Color(FigureJ_Tool.getBGColor()));
			pack();
		}

		/**
		 * settings of the buttons, combo boxes labels .. handling text label
		 * design
		 */
		private void initLabelGUI() {
			disableUserDefinedLabelsInputField(true);

			Iterator<String> itr = LabelDrawer.getPositionTypes().iterator();
			while (itr.hasNext())
				labelPositionSelector.addItem(itr.next());
			labelPositionSelector.setSelectedItem("TopLeft");

			itr = LabelDrawer.getLabelTypes().iterator();
			while (itr.hasNext())
				labelTypeSelector.addItem(itr.next());
			labelTypeSelector.setSelectedItem("ABC");

			labelTypeSelector.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if ((labelTypeSelector.getSelectedItem() + "")
							.equals(LabelType.userDefined.toString()))
						disableUserDefinedLabelsInputField(false);
					else
						disableUserDefinedLabelsInputField(true);
				}
			});
		}

		private void initScalebarGUI() {
			scalebarSizeSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent arg0) {
					scaleDisplayCheck.setSelected(true);
					int xOff = stringToNr(xOffsScales, activePanel.getW() / 3);
					scalebarSizeSlider.setMaximum(activePanel.getW() - xOff);
					scalebarSizeSlider.setToolTipText("scale bar length");
					double d = getClosestScaleBar();
					activePanel.setScalebar(mainWindow.getImagePlus(), xOff,
							stringToNr(yOffsScales, activePanel.getH() / 2), d,
							stringToNr(scalebarHeight, 30));
					mainWindow.draw();
					IJ.showStatus(IJ.d2s(d
							* activePanel.getImgData().getPixelWidth(), 2)
							+ " " + activePanel.getImgData().getUnit());
					// TODO:
					scaleTextValue.setText(activePanel.getShortScaleBarText());
				}
			});
			scaleDisplayCheck.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (!scaleDisplayCheck.isSelected()) {
						activePanel.removeScalebar(mainWindow.getImagePlus()
								.getOverlay());
						mainWindow.draw();
					} else {
						int xOff = stringToNr(xOffsScales,
								activePanel.getW() / 2);
						scalebarSizeSlider.setMaximum(activePanel.getW() - xOff);
						scalebarSizeSlider.setToolTipText("scale bar length");
						double d = getClosestScaleBar();
						activePanel.setScalebarColor(Toolbar
								.getForegroundColor());
						activePanel.setScalebar(
								mainWindow.getImagePlus(),
								xOff,
								stringToNr(yOffsScales, activePanel.getH() / 2),
								d, stringToNr(scalebarHeight, 30));
						mainWindow.draw();
						IJ.showStatus(IJ.d2s(d
								* activePanel.getImgData().getPixelWidth(), 2)
								+ " " + activePanel.getImgData().getUnit());
					}

				}
			});
			scaleTextDisplayCheck.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (!scaleTextDisplayCheck.isSelected()) {
						activePanel.setScaleBarTextVisible(false);
						activePanel.setScalebarLabelJustification(-1);
						mainWindow.draw();
					} else {
						activePanel.setScaleBarTextVisible(true);
						activePanel.setScaleBarTextFont(new Font(TextRoi
								.getFont(), TextRoi.getStyle(), TextRoi
								.getSize()));
						mainWindow.draw();
					}

				}
			});
			lockScale.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (lockScale.isSelected()) {
						Prefs.set("figurej.lockPixelSize", true);
						double pixelWidth = activePanel.getImgData()
								.getPixelWidth();
						Prefs.set("figurej.lockedPixelSize", pixelWidth);
						lockScale
								.setText("<html>Lock Pixel size: <font color=red>LOCKED</font></html>");

					} else {
						Prefs.set("figurej.lockPixelSize", false);
						lockScale.setText("<html>Lock Pixel size</html>");

					}

				}
			});
		}

		/** display a new text label on the active panel */
		public void addPanelLabel(boolean reset, boolean backwards) {
			if (labelTypeSelector.getSelectedItem().toString()
					.equals(LabelType.userDefined.toString()))
				labelDraw.setUserLabels(userDefinedLabelsInputField.getText());

			String label = labelDraw.getLabel(
					labelTypeSelector.getSelectedItem() + "", reset, backwards);
			activePanel.setLabel(
					mainWindow.getImagePlus(),
					label,
					stringToNr(xLabelOffset, activePanel.getW() / 2),
					stringToNr(yLabelOffset, activePanel.getH() / 2),
					LabelPosition.valueOf(labelPositionSelector
							.getSelectedItem() + ""));
		}

		/** allow the user to type own label strings in a text field or not */
		private void disableUserDefinedLabelsInputField(boolean disable) {
			if (disable) {
				userDefinedLabelsMessage.setEnabled(false);
				userDefinedLabelsInputField.setVisible(false);
				userDefinedLabelsInputField.setToolTipText(null);
			} else {
				userDefinedLabelsMessage.setEnabled(true);
				userDefinedLabelsInputField.setVisible(true);
				userDefinedLabelsInputField
						.setToolTipText("insert your own labels, separate by semicolon");
			}
		}

		/** find good value depending on slider position */
		private double getClosestScaleBar() {
			double dist = 1000000;
			double[] c = new double[2];
			double mousedist = (scalebarSizeSlider.getMaximum() - scalebarSizeSlider
					.getValue()) * activePanel.getImgData().getPixelWidth();
			for (int i = 0; i < b.length; i++)
				for (int j = 0; j < a.length; j++) {
					double currentdist = Math.abs(mousedist - a[j] * b[i]);
					if (currentdist < dist) {
						c[0] = a[j];
						c[1] = b[i];
						dist = currentdist;
					}
				}
			return (double) ((c[0] * c[1]) / activePanel.getImgData()
					.getPixelWidth());
		}

		/** set info messages that pop up if mouse stays longer over a component */
		private void addOptionsWindowToolTips() {
			drawLabelButton.setToolTipText("add text label to panel");
			removeLabelButton.setToolTipText("delete text label from panel");
			labelTypeSelector.setToolTipText("choose label type");
			labelPositionSelector.setToolTipText("choose label position");
			xLabelOffset.setToolTipText("vertical distance to panel border");
			yLabelOffset.setToolTipText("horizontal distance to panel border");
			xOffsScales.setToolTipText("distance to right panel border");
			yOffsScales.setToolTipText("distance to lower panel border");
			scalebarHeight.setToolTipText("scalebar height");
			changeSeparatorColorButton
					.setToolTipText("Update separators color to current foreground color");
		}

		/**
		 * @param textField
		 *            textfield to read out
		 * @param max
		 *            maximum value; larger values are replaced by 10
		 * @return string input of the text field converted to number; if input
		 *         > max or not a number: 10 per default
		 */
		private int stringToNr(JTextField textField, int max) {
			int i = 10;
			try {
				i = Integer.parseInt(textField.getText());
			} catch (NumberFormatException f) {
				i = 10;
			} finally {
				if (i < 0 || i > max)
					i = 10;
			}
			;
			return i;
		}

	}

	private class CustomProgressBar implements Runnable {
		public void run() {
			try {
				String s = "                      ";
				int i = 0;
				while (true) {
					i = (i + 1) % 12;
					IJ.showStatus("Scaling image. " + s.substring(0, i) + "***"
							+ s.substring(0, 12 - i));
					Thread.sleep(100);
				}
			} catch (InterruptedException e) {
			}
		}
	}

	public static void installMacroFromJar(String name) {
		String macro = null;
		try {
			ClassLoader pcl = IJ.getClassLoader();
			InputStream is = pcl.getResourceAsStream("macros/" + name);
			if (is == null) {
				IJ.error("FigureJ installMacroFromJar", "Unable to load \""
						+ name + "\" from jar file");
				return;
			}
			InputStreamReader isr = new InputStreamReader(is);
			StringBuffer sb = new StringBuffer();
			char[] b = new char[8192];
			int n;
			while ((n = isr.read(b)) > 0)
				sb.append(b, 0, n);
			macro = sb.toString();
			is.close();
		} catch (IOException e) {
			IJ.error("FigureJ installMacroFromJar", "" + e);
		}
		if (macro != null)
			(new MacroInstaller()).installSingleTool(macro);
	}

	public void cleanGUIandTransferROI() {

		String recordedMacro = "";

		if (recorder != null) {
			recordedMacro = recorder.getText();
			try {
				recorder.close();
			} catch (Exception e2) {
				System.err.println("recorder nullpointer");
			}
		}
		// fire custom progress bar b/c some transforms are slow
		Thread t = new Thread(new CustomProgressBar());
		t.start();
		try {
			transferROIDataToPanel(recordedMacro);
		} catch (Exception e1) {
			IJ.error("Could not transform image.\n" + e1.getMessage());
		}
		openedImage.close();
		IJ.run("Select None");
		t.interrupt();

		IJ.showStatus("done.");
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

	@Override
	public String commandExecuting(String command) {
		if ((WindowManager.getImageCount()>0)&&(IJ.getImage().getTitle()=="FigureJ") && command.equals("Make Composite"))
			return null; // do not run this command
		else 
			return command;
	}

}
