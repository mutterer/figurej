
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
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

import externaltools.Link;
import externaltools.PluginPanel;
import figure.ContainerPanel;
import figure.FigureWindow;
import figure.LeafPanel;
import figure.Panel;
import figure.SeparatorPanel;
import ij.CommandListener;
import ij.CompositeImage;
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
import ij.plugin.frame.PlugInDialog;
import ij.plugin.frame.Recorder;
import ij.plugin.tool.PlugInTool;
import ij.process.FloatPolygon;
import imagescience.transform.Affine;
import loci.formats.FormatException;
import loci.plugins.BF;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;
import loci.plugins.in.ImporterPrompter;
import utils.DataSource;
import utils.FigureIO;
import utils.LabelDrawer;
import utils.LabelPosition;
import utils.LabelType;
import utils.MyImageMath;
import utils.NewFigureDialog;
import utils.ViewFinder;

/**
 * @author Edda Zinck
 * @author Jerome Mutterer (c) IBMP-CNRS
 * 
 *         main class handles switching and interaction between the figure
 *         window and the image source window where image regions are selected.
 *         this class also contains the main control window in the listeners of
 *         this classes elements behavior like enabled-status of buttons or
 *         actions that take place on button clicks are defined.
 */

public class PanelSelection_Tool extends PlugInTool implements ImageListener, IJEventListener, CommandListener {

	private String title = "FigureJ ";

	private ViewFinder viewfinder = new ViewFinder();
	private FigureWindow figure;
	private FigureControlPanel mainControlPanel;
	private AnnotationsPanel annotationsPanel;
	private ImagePlus openedImage;
	private LeafPanel activePanel;
	private boolean figureIsActive = true;
	private static DataSource copiedImageData = null;
	private FloatPolygon roiGeometryOnly;

	// GUI stuff
	private Dimension panelDimension = new Dimension(210, 600);
	private int guiBorder = 5;
	private int mainWindowXLocation = 0;
	private int mainWindowYLocation = 0;
	private static int backgroundColor = 0x99aabb;
	// main panel
	private JButton newFigureBtn;
	private JButton openFigureBtn;
	private JButton saveFigureBtn;
	private JButton hSplitBtn;
	private JButton vSplitBtn;
	private JButton removePanelBtn;
	private JButton debugBtn;
	private JButton copyPanelDataBtn;
	private JButton pastePanelDataBtn;
	private JButton clearPanelDataBtn;
	private JButton okImageBtn;
	public JButton cancelImageBtn;
	private JButton openImageBtn;
	private JButton openOptionsPanelBtn;
	private JComboBox<String> interpolationTypeComboBox;
	private JTextArea filePathLabel = new JTextArea("<path>");
	// annotation panel
	private JButton adoptPixelsButton;
	private JButton openColorPickerButton;
	private JButton changeSeparatorColorButton;
	private JButton printFigure;
	private JButton drawLabelButton;
	private JButton removeLabelButton;
	private JButton newTextRoiButton;
	private JButton newArrowRoiButton;
	private JButton addItemToOverlayButton;
	private JButton duplicateItemButton;
	private JButton hideOverlayButton;
	private JButton showOverlayButton;
	private JButton applyFontPtSize;
	private JTextField fontPtSize = new JTextField("12");


	// utils
	private LabelDrawer labelDraw = new LabelDrawer();
	private FigureIO serializer = new FigureIO();
	private Recorder recorder;

	/**
	 * Main Tool for interaction with either figure window or view finder window
	 */
	public PanelSelection_Tool() {

		// kill any existing instance in case there is one
		if (WindowManager.getFrame(title) != null)
			WindowManager.getFrame(title).dispose();
		IJ.addEventListener(this);
		initializeGUI();
		ImagePlus.addImageListener(this);
		Executer.addCommandListener(this);
		run("");
		// add some extra tools to the toolbar.
		installMacroFromJar("Overlay_Editing_Tools.ijm");
//		installMacroFromJar("panel_sticking_Tool.ijm");
//		installMacroFromJar("TextLabel_Tool.ijm");
		installMacroFromJar("Label_Tool.ijm");
		installMacroFromJar("FigureJ_useful_actions_Tool.ijm");
	}

	private void initializeGUI() {
		LeafPanel.setColorValue(backgroundColor);
		mainControlPanel = new FigureControlPanel();
		mainControlPanel.setVisible(true);
	}

	public void mousePressed(ImagePlus imp, MouseEvent e) {
		if (WindowManager.getCurrentImage() == null || WindowManager.getCurrentImage() != openedImage) {
			WindowManager.setCurrentWindow(figure.getWindow());
			figureIsActive = true;
			figure.mousePressed(imp, e);
			int count = e.getClickCount();
			Panel p = figure.getSelectedPanel();
			Prefs.set("selectedpanel.width", p.getW());
			Prefs.set("selectedpanel.height", p.getH());
			if (p instanceof LeafPanel) {
				activePanel = (LeafPanel) p;
				disableAllPanelWindowButtons(false);
				if (IJ.altKeyDown()) {
					IJ.log(activePanel.getImgData().createLog());
				}
				if (count == 2)
					mainControlPanel.openViewFinderTool(false);
			} else {
				activePanel = null;
				disableAllPanelWindowButtons(true);
			}
		} else {
			if (e.getClickCount() == 2 && !e.isConsumed()) {
				e.consume();
				cleanGUIandTransferROI();
			} else
				viewfinder.mousePressed(imp, e);
		}
	}

	public void mouseDragged(ImagePlus imp, MouseEvent e) {
		if (figureIsActive)
			figure.mouseDragged(imp, e);
		else
			viewfinder.mouseDragged(imp, e);
	}

	/**
	 * does either nothing, calls the ROI release method or updates the values
	 * of the active panel's window
	 */
	@Override
	public void mouseReleased(ImagePlus imp, MouseEvent e) {
		if (figureIsActive) {
			figure.mouseReleased(imp, e);

			if (activePanel != null) {
				Point a = new Point(figure.getMousePressedPoint());
				Point b = new Point(figure.getMouseReleasedPoint());
				Line l = new Line(a.x, a.y, b.x, b.y);
				boolean horizontalDrag = (b.x - a.x) * (b.x - a.x) > (b.y - a.y) * (b.y - a.y);
				if ((l.getRawLength() > 20) && (Prefs.get("figurej.mousePanelSlicing", 0) == 1))
					activePanel.split(2, horizontalDrag);
				figure.draw();
				mainControlPanel.updateValues();
			}
		} else {
			viewfinder.mouseReleased(imp, e);
		}

	}

	public void mouseMoved(ImagePlus imp, MouseEvent e) {
		if (figureIsActive) {
			figure.mouseMoved(imp, e);
		} else
			viewfinder.mouseMoved(imp, e);
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
			figureIsActive = false;
	}

	@Override
	public void imageUpdated(ImagePlus img) {
	}

	/** handle status of the image buttons */
	@Override
	public void imageClosed(ImagePlus img) {

		if (figure != null)
			if (img == openedImage) {
				setViewFinderToolOpenable(true);
				setControlFrameButtonStates(true);
				if (openedImage.isComposite() && WindowManager.getWindow("Channels") != null)
					((PlugInDialog) WindowManager.getWindow("Channels")).close();

			} else if (img == figure.getImagePlus()) {
				if (figure.getQuitWithoutSaving() == false)
					serializer.save(figure);
				disableAllPanelWindowButtons(true);
				setOpenNewButtonsStates(true);
			}
	}

	/**
	 * the pixels inside the ROI are rotated and scaled to the size of the panel
	 * the image belongs to. the pixel data is passed to the panel, the main
	 * window is set active again; the panels are repainted.
	 */
	private void transferROIDataToPanel(String macro) {
		Panel temp = figure.getSelectedPanel();

		if (temp != null)
			if (temp.getClass().getName().contains(LeafPanel.class.getName())) {
				// remember the size and position of the image fragment the user
				// selected
				double[] xVals = viewfinder.getXVals().clone();
				double[] yVals = viewfinder.getYVals().clone();

				// save coords of source roi for use outside figurej.
				String xRoi = "";
				String yRoi = "";
				for (int i = 0; i < xVals.length; i++) {
					xRoi = xRoi + IJ.d2s(xVals[i]) + ",";
					yRoi = yRoi + IJ.d2s(yVals[i]) + ",";
				}
				xRoi = xRoi.substring(0, xRoi.length() - 1);
				yRoi = yRoi.substring(0, yRoi.length() - 1);

				Prefs.set("figurej.xRoi", xRoi);
				Prefs.set("figurej.yRoi", yRoi);

				LeafPanel selectedPanel = (LeafPanel) temp;
				DataSource imageData = selectedPanel.getImgData();

				// store detailed information about the image the user chose for
				// a panel
				imageData.setCoords(xVals, yVals);
				imageData.setPixelCalibration(openedImage.getCalibration().pixelWidth,
						openedImage.getCalibration().getUnit());
				imageData.setMacro(macro);
				String fov = viewfinder.getFov();
				imageData.setFov(fov!=null?fov:"");
				imageData.setDisplayRange(openedImage.getDisplayRangeMin(), openedImage.getDisplayRangeMax());

				// position in stack like and composite images
				imageData.setSlice(openedImage.getSlice());
				imageData.setChannel(openedImage.getChannel());
				imageData.setFrame(openedImage.getFrame());
				imageData.setActChs(getActiveChannels(openedImage));

				filePathLabel.setText("path: " + imageData.getFileDirectory() + imageData.getFileName());

				float[] xRect = new float[xVals.length];
				float[] yRect = new float[xVals.length];

				for (int i = 0; i < xRect.length; i++) {
					xRect[i] = (float) xVals[i];
					yRect[i] = (float) yVals[i];
				}

				PolygonRoi boundingRect = new PolygonRoi(new FloatPolygon(xRect, yRect), PolygonRoi.POLYGON);

				double angle = Math.atan((yVals[3] - yVals[0]) / (xVals[3] - xVals[0])) * 180 / Math.PI
						+ ((xVals[3] < xVals[0]) ? 180 : 0);

				Line top = new Line(xVals[0], yVals[0], xVals[3], yVals[3]);
				double scaleFactor = selectedPanel.getW() / top.getRawLength();

				// FILL THE PANEL with the pixels selected from the image
				selectedPanel.setPixels(MyImageMath.getPixels(openedImage, boundingRect, angle, scaleFactor,
						selectedPanel.getW(), selectedPanel.getH(), getSelectedInterpolation()));
				// calculate the calibration
				imageData.setPixelCalibration((1 / scaleFactor) * openedImage.getCalibration().pixelWidth,
						openedImage.getCalibration().getUnit());
				imageData.setAngle(angle);
				imageData.setScaleFactor(scaleFactor);
				// IJ.log(""+Arrays.toString(xVals));
				figure.draw();
				figure.getImagePlus().killRoi();
			}

		setViewFinderToolOpenable(true);
		setControlFrameButtonStates(true);
		figureIsActive = true;
	}

	private String getActiveChannels(ImagePlus imp) {
		if (!imp.isComposite())
			return "";
		boolean[] active = ((CompositeImage) imp).getActiveChannels();
		int n = active.length;
		char[] chars = new char[n];
		int nChannels = imp.getNChannels();
		for (int i = 0; i < n; i++) {
			if (i < nChannels)
				chars[i] = active[i] ? '1' : '0';
			else
				chars[i] = '0';
		}
		return (new String(chars));
	}

	private int getSelectedInterpolation() {
		String interpolation = (String) interpolationTypeComboBox.getSelectedItem();
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

	/**
	 * @param isTrue
	 *            if true enable buttons to open an existing or create a new
	 *            figure and disables the save button; it is assumed that no
	 *            figure is open currently; else it is assumed that an image is
	 *            already open and therefore only the safe button is enabled
	 */
	private void setOpenNewButtonsStates(boolean isTrue) {
		if (isTrue) {
			newFigureBtn.setEnabled(true);
			openFigureBtn.setEnabled(true);
			saveFigureBtn.setEnabled(false);
		} else {
			newFigureBtn.setEnabled(false);
			openFigureBtn.setEnabled(false);
			saveFigureBtn.setEnabled(true);
		}
	}

	/**
	 * @param isTrue
	 *            : if true disable all buttons of the panel window else enables
	 *            all helpful e.g. if separators are dragged
	 */
	private void disableAllPanelWindowButtons(boolean isTrue) {
		if (isTrue) {
			openImageBtn.setEnabled(false);
			okImageBtn.setEnabled(false);
			cancelImageBtn.setEnabled(false);

			hSplitBtn.setEnabled(false);
			vSplitBtn.setEnabled(false);
			removePanelBtn.setEnabled(false);

			copyPanelDataBtn.setEnabled(false);
			pastePanelDataBtn.setEnabled(false);
			clearPanelDataBtn.setEnabled(false);
		} else {
			openImageBtn.setEnabled(true);
			okImageBtn.setEnabled(false);
			cancelImageBtn.setEnabled(false);

			hSplitBtn.setEnabled(true);
			vSplitBtn.setEnabled(true);
			removePanelBtn.setEnabled(true);

			copyPanelDataBtn.setEnabled(true);
			pastePanelDataBtn.setEnabled(true);
			clearPanelDataBtn.setEnabled(true);
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
	private void setViewFinderToolOpenable(boolean isTrue) {
		if (isTrue) {
			openImageBtn.setEnabled(true);
			okImageBtn.setEnabled(false);
			cancelImageBtn.setEnabled(false);
		} else {
			openImageBtn.setEnabled(false);
			okImageBtn.setEnabled(true);
			cancelImageBtn.setEnabled(true);
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
			hSplitBtn.setEnabled(false);
			vSplitBtn.setEnabled(false);
			removePanelBtn.setEnabled(false);

			copyPanelDataBtn.setEnabled(false);
			pastePanelDataBtn.setEnabled(false);
			clearPanelDataBtn.setEnabled(false);
		} else {
			hSplitBtn.setEnabled(true);
			vSplitBtn.setEnabled(true);
			removePanelBtn.setEnabled(true);

			copyPanelDataBtn.setEnabled(true);
			pastePanelDataBtn.setEnabled(true);
			clearPanelDataBtn.setEnabled(true);
		}
	}

	@Override
	/**
	 * remove the highlighting of the clicked panel because this could be
	 * dragged around e.g. with the arrow tool
	 */
	public void eventOccurred(int eventID) {
		if (eventID == IJEventListener.TOOL_CHANGED) {
			if (figure != null && figure.getImagePlus() != null)
				figure.getImagePlus().changes = false;
		}
	}

	public static int getBGColor() {
		return backgroundColor;
	}

	/**
	 * specify the closing behavior of the new-open-save window: ask the user if
	 * he wants to save changes, close all tool figure and switch to the imageJ
	 * rectangle tool
	 */
	public class FigureJClosingAdaptor extends WindowAdapter {
		public void windowClosing(WindowEvent wEvent) {
			String message = "";
			if (figure != null) {
				if (figure.getImagePlus().changes)
					message = "\nCurrent figure will be lost.";
				GenericDialog gd = new GenericDialog("FigureJ");
				gd.addMessage("Are you sure you want to quit FigureJ?" + message);
				gd.showDialog();
				if (gd.wasCanceled())
					return;
			}
			if (figure != null)
				figure.setQuitWithoutSaving(true);
			wEvent.getWindow().dispose();

			IJ.setTool("rectangle");

			if (mainControlPanel != null)
				mainControlPanel.dispose();

			// application closed just after opening
			if (figure != null && figure.getImagePlus() != null)
				figure.getImagePlus().close();

			if (annotationsPanel != null)
				annotationsPanel.dispose();

			if (mainControlPanel.getPluginPanel()!=null)
				mainControlPanel.getPluginPanel().resetFigureWindow();
		}

		public void windowClosed(WindowEvent e) {
			WindowManager.removeWindow((Frame) mainControlPanel);
			Toolbar.restoreTools();

		}
	}

	/**
	 * specify the closing behavior of the selecion window
	 */
	public class SelectionWindowClosingAdaptor extends WindowAdapter {
		public void windowClosing(WindowEvent wEvent) {
			// cleanGUIandTransferROI();
			cancelImageBtn.doClick();
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
		private JComboBox<Integer> splitNr = new JComboBox<Integer>();
		private JPanel ioBtnsPanel = new JPanel(new GridLayout(3, 1, 0, 0));
		private JPanel layoutBtnsPanel = new JPanel(new GridLayout(3, 1, 0, 0));
		private JTextArea notesTextArea = new JTextArea("notes:", 4, 1);
		private PluginPanel pluginPanel = new PluginPanel();


		public FigureControlPanel() {

			initializeButtons();
			fillComboBoxes();
			addPanelWindowToolTips();
			setGUIImageFrameValues();
			setNotesListener();

			setTitle("FigureJ " + FigureJ2_.version());
			setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
			add(ioPanelInit());
			add(splitPanelInit());
			add(imagePanelInit());
			if (Prefs.get("figurej.externalTools", 0) == 1)
				add(pluginPanel);

			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			addWindowListener(new FigureJClosingAdaptor());
			pack();
			setVisible(true);
			setSize(panelDimension.width, this.getHeight());
			setMaximumSize(new Dimension(panelDimension.width, 2 * panelDimension.height));
			Rectangle ijBounds = IJ.getInstance().getBounds();
			setLocation(0, ijBounds.y + ijBounds.height + 5);
			WindowManager.addWindow((Frame) this);
		}

		public PluginPanel getPluginPanel() {
			return pluginPanel;
		}

		/** assign the window to a different panel and update the values */
		private void updateValues() {
			if (activePanel!=null) {
			data = activePanel.getImgData();

			setGUIImageFrameValues();
			if (annotationsPanel != null) {
				annotationsPanel.scaleDisplayCheck.setSelected(activePanel.isScalebarVisible());
				annotationsPanel.scaleBarTextDisplayCheck.setSelected(activePanel.isScalebarTextVisible());
				annotationsPanel.scaleTextValue.setText(activePanel.getShortScaleBarText());
				annotationsPanel.updateLabelField(activePanel.getLabel());
			}
			}
		}

		/** new / open / save panel */
		private JPanel ioPanelInit() {
			ioBtnsPanel.setBorder(BorderFactory.createTitledBorder("Figure"));
			ioBtnsPanel.setLayout(new GridLayout(0, 3));
			ioBtnsPanel.add(newFigureBtn);
			ioBtnsPanel.add(openFigureBtn);
			ioBtnsPanel.add(saveFigureBtn);
			return ioBtnsPanel;
		}

		/** split and clear buttons */
		private JPanel splitPanelInit() {

			layoutBtnsPanel.setBorder(BorderFactory.createTitledBorder("Layout"));
			layoutBtnsPanel.setLayout(new GridLayout(2 + ((IJ.debugMode == true) ? 1 : 0), 2));
			layoutBtnsPanel.add(hSplitBtn);
			layoutBtnsPanel.add(removePanelBtn);
			layoutBtnsPanel.add(vSplitBtn);
			layoutBtnsPanel.add(splitNr);

			if (IJ.debugMode)
				layoutBtnsPanel.add(debugBtn);

			return layoutBtnsPanel;
		}

		/** arrange the lower buttons that handle image and image info data */
		private JPanel imagePanelInit() {
			JPanel imagePanel = new JPanel(new BorderLayout());

			imagePanel.setBorder(BorderFactory.createTitledBorder("Image"));

			JPanel p1 = new JPanel(new GridLayout(1, 1));
			JPanel p3 = new JPanel(new GridLayout(1, 1));
			JPanel p4 = new JPanel(new GridLayout(1, 2));
			JPanel p5 = new JPanel(new GridLayout(1, 2));

			JPanel upper = new JPanel(new GridLayout(1, 1));
			JPanel middle = new JPanel(new GridLayout(2, 2));

			filePathLabel.setMaximumSize(panelDimension);
			filePathLabel.setEditable(false);
			filePathLabel.setLineWrap(true);
			filePathLabel.setWrapStyleWord(true);
			filePathLabel.setBackground(upper.getBackground());
			filePathLabel.setFont(new Font("sansserif", Font.PLAIN, 10));
			p1.add(filePathLabel);

			upper.add(p1);
			upper.setMaximumSize(new Dimension(panelDimension.width, 2 * panelDimension.height));

			middle.add(copyPanelDataBtn);
			middle.add(clearPanelDataBtn);
			middle.add(pastePanelDataBtn);
			middle.add(openOptionsPanelBtn);

			p3.add(openImageBtn);
			p4.add(interpolationTypeComboBox);
			p5.add(okImageBtn);
			p5.add(cancelImageBtn);

			JPanel openImageButtons = new JPanel(new GridLayout(3, 1));
			openImageButtons.add(p3);
			openImageButtons.add(p4);
			openImageButtons.add(p5);

			JPanel lower = new JPanel(new BorderLayout());
			lower.add(middle, BorderLayout.CENTER);
			lower.add(openImageButtons, BorderLayout.SOUTH);

			imagePanel.add(upper, BorderLayout.NORTH);
			notesTextArea.setFont(new Font("sansserif", Font.PLAIN, 10));
			notesTextArea.setLineWrap(true);
			notesTextArea.setMaximumSize(notesTextArea.getSize());
			notesTextArea.setMinimumSize(notesTextArea.getSize());

			JScrollPane areaScrollPane = new JScrollPane(notesTextArea);
			areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
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

			for (Integer i = 2; i < 10; i++)
				splitNr.addItem(i);
			splitNr.setSelectedItem(2);

			interpolationTypeComboBox = new JComboBox<String>();
			ArrayList<String> l = getInterpolationTypes();
			Iterator<String> itr = l.iterator();
			while (itr.hasNext())
				interpolationTypeComboBox.addItem(itr.next());
			interpolationTypeComboBox.setSelectedItem("nearest neighbor");
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

		/** show image path and image notesTextArea on the GUI */
		private void setGUIImageFrameValues() {
			if (data != null) {
				if (data.getFileDirectory() != "")
					filePathLabel.setText("path: " + data.getFileDirectory() + data.getFileName());
				else
					filePathLabel.setText("path: ");
				notesTextArea.setText(data.getNotes() == "" ? "notes:" : data.getNotes());
			} else {
				filePathLabel.setText("path: ");
				notesTextArea.setText(
						"Welcome to FigureJ!\nEasy article figures with original data\nand processing history.");
			}
		}

		/**
		 * add a listener to the notesTextArea GUI field that stores the changes
		 * in the right dataSource object
		 */
		private void setNotesListener() {

			notesTextArea.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void removeUpdate(DocumentEvent arg0) {
					if (data != null)
						data.setNotes(notesTextArea.getText());
				}

				@Override
				public void insertUpdate(DocumentEvent arg0) {
					if (data != null)
						data.setNotes(notesTextArea.getText());
				}

				@Override
				public void changedUpdate(DocumentEvent arg0) {
					if (data != null)
						data.setNotes(notesTextArea.getText());
				}
			});

		}

		/** initialize the buttons, set names and listeners */
		private void initializeButtons() {

			newFigureBtn = new JButton("new");
			newFigureBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {

					NewFigureDialog dialog = new NewFigureDialog();

					if (dialog.openDialog()) {
						setOpenNewButtonsStates(false);

						Rectangle ijBounds = IJ.getInstance().getBounds();
						mainWindowXLocation = panelDimension.width + guiBorder;
						mainWindowYLocation = ijBounds.y + ijBounds.height + guiBorder;

						figure = new FigureWindow(dialog.getWidth(), dialog.getHeight(), mainWindowXLocation,
								mainWindowYLocation, dialog.getResolution(), dialog.getSeparatorSize());
						
						figure.calibrateImage(dialog.getResolution(), dialog.getUnit());
						
						Panel p = figure.getSelectedPanel();
						
						if (p.getClass().getName().contains(LeafPanel.class.getName()))
							activePanel = (LeafPanel) p;
						else
							activePanel = null;

						mainControlPanel.updateValues();
						labelDraw.setCount(-1);
						if (Prefs.get("figurej.externalTools", 0) == 1)
							pluginPanel.setFigureWindow(figure);
					}
				}
			});

			openFigureBtn = new JButton("open");
			openFigureBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setOpenNewButtonsStates(false);

					figure = null;
					figure = serializer.open();
					// if opened successfully
					if (figure != null) {
						mainControlPanel.updateValues();
					} else {
						// failed to open
						setOpenNewButtonsStates(true);
						return;
					}
					figure.draw();
					if (Prefs.get("figurej.externalTools", 0) == 1)
						pluginPanel.setFigureWindow(figure);

				}
			});

			saveFigureBtn = new JButton("save");
			saveFigureBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				// debug-style dump of figure structure for now
					if (((e.getModifiers() & ActionEvent.ALT_MASK)
							* ((e.getModifiers() & ActionEvent.CTRL_MASK))) != 0) {
						level = 0;
						dumpPanel(figure.getRootPanel()); // for the root panel
						listPanels(figure.getRootPanel()); // for root panel children
					} 
					// save by serializing 
					else {
						IJ.runPlugIn(figure, "Selection", "none");
						figure.killRoi();
						serializer.save(figure);
						figure.draw();
					}
				}
				private int level = 0;

				private void listPanels(Panel rootPanel) {
					level++;
					ArrayList<Panel> pa = rootPanel.getChildren();
					for (Panel p : pa) {
						dumpPanel(p);
						if (p instanceof ContainerPanel)
							listPanels(p);
					}
					level--;
				}

				private void dumpPanel(Panel p) {
					Rectangle r = p.getRectangle();
					String shape = r.toString();
					shape = shape.substring(shape.indexOf("["));
					String pType = p.toString();
					pType = pType.substring(0, pType.indexOf("@"));
					System.out.println(levelString() + pType + " " + shape);
				}

				private String levelString() {
					StringBuffer outputBuffer = new StringBuffer();
					for (int i = 0; i < level; i++) {
						outputBuffer.append("-");
					}
					return outputBuffer.toString();
				}
			});
			saveFigureBtn.setEnabled(false);

			openOptionsPanelBtn = new JButton("decorate" + new Character((char) 8230));
			openOptionsPanelBtn.addActionListener(new ActionListener() {
				// display options window
				public void actionPerformed(ActionEvent e) {
					if (annotationsPanel == null)
						annotationsPanel = new AnnotationsPanel(openOptionsPanelBtn.getLocation().x + 50,
								getLocation().y + openOptionsPanelBtn.getLocation().y + 30);
					annotationsPanel.setVisible(true);
				}
			});

			hSplitBtn = new JButton("split " + new Character((char) 8212));
			hSplitBtn.addActionListener(new ActionListener() {
				// split active panel horizontally in as many children as chosen
				// in the splitNr combo box
				public void actionPerformed(ActionEvent e) {
					activePanel.split((Integer) splitNr.getSelectedItem(), true);
					figure.draw();
				}
			});

			vSplitBtn = new JButton("split |");
			vSplitBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// split active panel vertically in as many children as
					// chosen in the splitNr combo box
					activePanel.split((Integer) splitNr.getSelectedItem(), false);
					figure.draw();
				}
			});

			adoptPixelsButton = new JButton("adopt current panel pixels");
			adoptPixelsButton.addActionListener(new ActionListener() {
				// let the active panel grab the pixels drawn on his area and
				// store them
				// is useful if somebody draws with non figureJ tools on the
				// result window and wants to store these changes
				public void actionPerformed(ActionEvent e) {
					if (activePanel != null)
						activePanel.setPixels(figure.getImagePlus());
				}
			});

			changeSeparatorColorButton = new JButton("update separator color");
			changeSeparatorColorButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int newColor = Toolbar.getForegroundColor().getRGB();
					SeparatorPanel.setColor(newColor);
					if (figure != null)
						figure.draw();
				}
			});

			openColorPickerButton = new JButton("open color picker");
			openColorPickerButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					IJ.runPlugIn("ij.plugin.frame.ColorPicker", "");
				}
			});

			newTextRoiButton = new JButton("new Text");
			newTextRoiButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String fontName = TextRoi.getDefaultFontName();
					int fontSize = TextRoi.getDefaultFontSize();
					int fontStyle = TextRoi.getDefaultFontStyle();
					boolean anti = TextRoi.isAntialiased();
					int x = figure.getSelectedPanel().getX();
					int y = figure.getSelectedPanel().getY();
					
					IJ.selectWindow("FigureJ");
					IJ.run("Select None");
					IJ.setTool("text");
					IJ.runPlugIn("ij.plugin.SimpleCommands","fonts");
					// String fStyle = ""+((fontStyle&1)>0?"bold ":"")+((fontStyle&2)>0?"italic ":"")+(anti?"anti ":"");
					// IJ.runMacro("setFont('"+fontName+"',"+fontSize*figure.getDPI()/72+",'"+fStyle+"');s='type...';makeText(s,"+x+","+y+");");
				}
			});

			newArrowRoiButton = new JButton("new Arrow");
			newArrowRoiButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {

					IJ.selectWindow("FigureJ");
					IJ.run("Select None");

					Window fr[] = Window.getWindows();
					boolean arrowsOptionsOpen = false;
					for (int i = 0; i < fr.length; i++) {
						if (fr[i].toString().indexOf("Arrow Tool") > 0 && fr[i].isVisible())
							arrowsOptionsOpen = true;
					}
					if (!arrowsOptionsOpen)
						IJ.doCommand("Arrow Tool...");
					IJ.showStatus("Arrow tool selected");

				}

			});

			addItemToOverlayButton = new JButton("add ROI");
			addItemToOverlayButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ImagePlus imp = IJ.getImage();
					IJ.run(imp, "Add Selection...", "");
					IJ.run(imp, "Select None", "");
				}
			});

			duplicateItemButton = new JButton("duplicate ROI");
			duplicateItemButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ImagePlus imp = IJ.getImage();
					Roi roi = imp.getRoi();
					if (roi != null) {
						imp.saveRoi();
						IJ.run(imp, "Add Selection...", "");
						String macro = "run('Restore Selection', '');shift=30;getSelectionBounds(x, y, width, height);setSelectionLocation(x+ shift, y+ shift/1.5);";
						IJ.runMacro(macro);
					}
				}
			});

			hideOverlayButton = new JButton("hide all");
			hideOverlayButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					IJ.run("Hide Overlay");
				}
			});

			showOverlayButton = new JButton("show all");
			showOverlayButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					IJ.run("Show Overlay");
				}
			});

			applyFontPtSize = new JButton("font size:");
			applyFontPtSize.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					IJ.runPlugIn("ij.plugin.SimpleCommands","fonts");
					Window win = WindowManager.getWindow("Fonts");
					Component p = win.getComponents()[11];
					TextField tf = (TextField) ((Container)p).getComponents()[1];
					int docptsize = (int) (Integer.parseInt(fontPtSize.getText())*figure.getDPI()/72);
					tf.setText(""+docptsize);
					}
			});

			printFigure = new JButton("print at actual size");
			printFigure.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					double res = figure.getDPI();
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
					boolean doAllPanels = ((e.getModifiers() & ActionEvent.ALT_MASK)
							* ((e.getModifiers() & ActionEvent.CTRL_MASK))) != 0;

					annotationsPanel.addPanelLabel(reset, backwards, doAllPanels);
					figure.draw();

				}
			});

			removeLabelButton = new JButton("remove");
			removeLabelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					activePanel.removeLabel(figure.getImagePlus().getOverlay());
					figure.draw();
				}
			});

			openImageBtn = new JButton("assign / re-open image");
			openImageBtn.addActionListener(new ActionListener() {
				// open the image and selector to specify the pixels the active
				// panel is filled with
				public void actionPerformed(ActionEvent e) {
					openViewFinderTool(false);
				}
			});

			okImageBtn = new JButton("ok");
			okImageBtn.setEnabled(false);
			okImageBtn.addActionListener(new ActionListener() {
				// fill a panel with pixels
				// store possibly run image processing steps; close channel
				// selection tool if necessary
				public void actionPerformed(ActionEvent e) {
					if (openedImage.isComposite() && WindowManager.getWindow("Channels") != null)
						((PlugInDialog) WindowManager.getWindow("Channels")).close();

					cleanGUIandTransferROI();
				}
			});

			cancelImageBtn = new JButton("cancel");
			cancelImageBtn.setEnabled(false);
			cancelImageBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// do not transfer the selected pixels, clear/close the
					// recorder and close the channel selection tool if
					// necessary
					setViewFinderToolOpenable(true);
					setControlFrameButtonStates(true);
					if (openedImage.isComposite() && WindowManager.getWindow("Channels") != null)
						((PlugInDialog) WindowManager.getWindow("Channels")).close();

					if (recorder != null) {
						try {
							recorder.close();
						} catch (Exception e2) {
							System.err.println("recorder nullpointer");
						}
					}
					openedImage.close();
					figureIsActive = true;
				}
			});

			removePanelBtn = new JButton("remove");
			removePanelBtn.addActionListener(new ActionListener() {
				// remove the active panel and get a new one
				public void actionPerformed(ActionEvent e) {
					activePanel.hideLabel(figure.getImagePlus().getOverlay());
					activePanel.hideScalebar(figure.getImagePlus().getOverlay());
					activePanel.remove(figure.getImagePlus().getOverlay());
					figure.updateSelectedPanel(true);
					activePanel = (LeafPanel) figure.getSelectedPanel();
					figure.draw();
				}
			});

			copyPanelDataBtn = new JButton("copy");
			copyPanelDataBtn.addActionListener(new ActionListener() {
				// copy image properties of a panel to another
				public void actionPerformed(ActionEvent e) {
					copiedImageData = activePanel.getImgData();

					if ((e.getModifiers() & ActionEvent.ALT_MASK) != 0) {
						// copies geometry only
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

			pastePanelDataBtn = new JButton("paste");
			pastePanelDataBtn.addActionListener(new ActionListener() {
				// copy image properties of one panel to another
				public void actionPerformed(ActionEvent e) {
					if ((e.getModifiers() & ActionEvent.ALT_MASK) != 0) {
						openViewFinderTool(true);
					} else if (copiedImageData != null) {
						activePanel.setImgData(copiedImageData.clone());
						mainControlPanel.updateValues();
						openViewFinderTool(false);
					}
				}
			});

			clearPanelDataBtn = new JButton("clear");
			clearPanelDataBtn.addActionListener(new ActionListener() {
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
					data.setFov("");
					filePathLabel.setText("<path>");
					activePanel.eraseImage();
					activePanel.hideScalebar(figure.getImagePlus().getOverlay());
					figure.draw();
				}
			});

			debugBtn = new JButton("debug");
			debugBtn.addActionListener(new ActionListener() {
				// TEST BUTTON ONLY VISIBLE WHEN IMAGEJ IN DEBUG MODE.
				public void actionPerformed(ActionEvent e) {

				}
			});

		}

		private void addPanelWindowToolTips() {
			notesTextArea.setToolTipText("enter your own notes here");
			interpolationTypeComboBox.setToolTipText("select interpolation type");
			splitNr.setToolTipText("n of panels appearing when splitting");
			hSplitBtn.setToolTipText("divide panel horizontally");
			vSplitBtn.setToolTipText("divide panel vertically");
			newTextRoiButton.setToolTipText("add a new text");
			newArrowRoiButton.setToolTipText("add a new arrow");
			hideOverlayButton.setToolTipText("hide all overlay items");
			showOverlayButton.setToolTipText("show all overlay items");
			addItemToOverlayButton.setToolTipText("add current item to overlay");
			duplicateItemButton.setToolTipText("add current item to overlay and duplicates it");
			openImageBtn.setToolTipText("select an image to fill the panel with");
			okImageBtn.setToolTipText("fill panel with selected image region");
			cancelImageBtn.setToolTipText("cancel panel filling");
			removePanelBtn.setToolTipText("delete selected panel");
			copyPanelDataBtn.setToolTipText("copy panel content");
			pastePanelDataBtn.setToolTipText("paste panel content");
			clearPanelDataBtn.setToolTipText("remove image from panel");
			openOptionsPanelBtn.setToolTipText("scale bars, panel labels and more...");
		}

		/**
		 * opens the image that belongs to the selected panel or a file chooser
		 * dialog if non assigned yet; starts the tool which is used to select
		 * the region of the image that shall be visible on the panel
		 */
		public void openViewFinderTool(boolean reuseGeometry) {
			setViewFinderToolOpenable(false); // handle buttons
			setControlFrameButtonStates(false); // again buttons

			viewfinder = new ViewFinder(activePanel);

			int nrOfOpenImgs = WindowManager.getImageCount();

			if ((activePanel.getImgData().getFileDirectory() == "") || (activePanel.getImgData().getFileName() == "")
					|| (activePanel.getImgData().getFileDirectory().isEmpty())
					|| (activePanel.getImgData().getFileName().isEmpty())) {
				OpenDialog opener = new OpenDialog("Choose image to fill panel", "");
				if ((opener.getFileName() !=null)&&(opener.getFileName() != "") ){
					data.setExternalSource(""); // b/c an new image datasource
					// was just selected.
					data.setFileDirectory(opener.getDirectory());
					data.setFileName(opener.getFileName());
				} else {
					data.setFileDirectory("");
					data.setFileName("");
					data.setCoords(new double[4], new double[4]);
					figureIsActive = true;
					setViewFinderToolOpenable(true);
					return;
				}

				String path = data.getFileDirectory() + data.getFileName();

				if (path.toLowerCase().endsWith(".czi") || path.toLowerCase().endsWith(".zvi")) {
					try {
						ImagePlus[] bfi = BF.openImagePlus(path);
						openedImage = bfi[0];
					} catch (FormatException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
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

						ImagePlus[] imps = BF.openImagePlus(options);
						openedImage = imps[imps.length - 1];
					} catch (FormatException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
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
				String path = activePanel.getImgData().getFileDirectory() + data.getFileName();
				if (path.toLowerCase().endsWith(".czi") || path.toLowerCase().endsWith(".zvi")) {
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
						options.setSeriesOn(activePanel.getImgData().getSelectedSerie(), true);
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

				openedImage.setPosition(data.getChannel(), data.getSlice(), data.getFrame());
			}
			openedImage.show();

			viewfinder.init(openedImage);

			// use old ROI coordinates if panel size did not change
			if (data.getSourceX() != null && data.getSourceY() != null)
				viewfinder.setCoords(data.getSourceX(), data.getSourceY());

			// try to use the same roi on different image
			if (reuseGeometry) {
				FloatPolygon r = getRoiGeometryOnly();
				double[] x = new double[4];
				double[] y = new double[4];
				for (int i = 0; i < x.length; i++) {
					x[i] = (double) r.xpoints[i];
					y[i] = (double) r.ypoints[i];
				}
				viewfinder.setCoords(x, y);
			}

			// stealth recording
			Prefs.set("recorder.mode", "Macro");
			recorder = new Recorder(false);

			if (openedImage.getCanvas() == null) {
				tryToCatchImageOpenFailure(nrOfOpenImgs);
			}

			if (openedImage.getCanvas() == null) {
				handleImageOpenFailure();
				return;
			}
			Rectangle ijBonds = IJ.getInstance().getBounds();
			openedImage.getWindow().setLocation(panelDimension.width + guiBorder + 30, ijBonds.y + ijBonds.height + 20);

			// removed, we do not want to reapply preprocessing automatically
			// IJ.runMacro(activePanel.getImgData().getMacro());

			if (openedImage.isComposite()) {
				IJ.doCommand("Channels Tool...");
				if (data.getActChs().indexOf("1") < 0) {
					openedImage.setDisplayMode(IJ.COMPOSITE);
					openedImage.setActiveChannels("11111111");
				} else {
					openedImage.setDisplayMode(IJ.COMPOSITE);
					openedImage.setActiveChannels(data.getActChs());
				}
			}

			viewfinder.drawRect(openedImage); // show ROI
			activePanel.getImgData().setFileDirectory(data.getFileDirectory());
			activePanel.getImgData().setFileName(data.getFileName());
			openedImage.getWindow().addWindowListener(new SelectionWindowClosingAdaptor());

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
			figureIsActive = true;
			setViewFinderToolOpenable(true);
			data.setFileDirectory("");
			data.setFileName("");
			IJ.error("failed to open the image.");
		}

	}

	/**
	 * window opened by the options button of the panel window
	 */
	private class AnnotationsPanel extends JFrame {

		private static final long serialVersionUID = 1L;

		private JPanel optionsPanel = new JPanel();
		// text labels
		private JTextField panelCaptionTextField = new JTextField();
		// private JLabel userDefinedLabelsMessage = new JLabel(" own labels:");

		private JTextField xLabelOffset = new JTextField("10");
		private JTextField yLabelOffset = new JTextField("10");
		private JComboBox<String> labelPositionComboBox = new JComboBox<String>();
		private JComboBox<String> labelTypeConboBox = new JComboBox<String>();
		// scale bars
		private JTextField xOffsScales = new JTextField("20");
		private JTextField yOffsScales = new JTextField("20");
		private JSlider scaleBarSizeSlider = new JSlider();
		private JCheckBox scaleDisplayCheck = new JCheckBox();
		
		// private JCheckBox scaleFontsToDocDPI = new JCheckBox("Fonts use doc DPI");
		
		private JCheckBox scaleBarTextDisplayCheck = new JCheckBox("show value: ", false);
		private JLabel scaleTextValue = new JLabel("-");
		// suggested by Christian Blanck
		private JCheckBox lockScale = new JCheckBox("Lock pixel size");
		private JTextField scaleBarHeight = new JTextField("10");
		private int[] a = { 1, 2, 5 };
		private double[] b = { 0.001, 0.01, 0.1, 1, 10, 100, 1000 };

		private AnnotationsPanel(int xLocation, int yLocation) {

			this.setLocation(xLocation, yLocation);
			this.setTitle("Options");

			initLabelGUI();
			initScalebarGUI();
			addOptionsWindowToolTips();

			// labels layout
			JPanel labelsPanel = new JPanel(new GridLayout(2, 2));
			JPanel labelsOffsets = new JPanel();

			labelsPanel.setPreferredSize(new Dimension(253, 110));
			labelsPanel.setBorder(BorderFactory.createTitledBorder("Panel Labels"));

			labelsOffsets.add(new JLabel(new ImageIcon(getClass().getResource("/imgs/ALeft1.png"))));
			labelsOffsets.add(xLabelOffset);
			labelsOffsets.add(new JLabel(new ImageIcon(getClass().getResource("/imgs/ATop1.png"))));
			labelsOffsets.add(yLabelOffset);

			//labelsPanel.add(drawLabelButton);
			//labelsPanel.add(removeLabelButton);
			labelsPanel.add(labelTypeConboBox);
			panelCaptionTextField.addActionListener(drawLabelButton.getActionListeners()[0]);

			labelsPanel.add(panelCaptionTextField);
			labelsPanel.add(labelPositionComboBox);
			labelsPanel.add(labelsOffsets);

			JPanel scalebarsPanel = new JPanel(new GridLayout(3, 1));
			JPanel scalebarsVisibilityAndSizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			JPanel scalebarsOffsetsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			JPanel scalebarsTextVisibility = new JPanel(new FlowLayout(FlowLayout.LEFT));

			scalebarsPanel.setBorder(BorderFactory.createTitledBorder("Panel Scale bars"));

			scalebarsVisibilityAndSizePanel.add(scaleDisplayCheck);
			scalebarsVisibilityAndSizePanel.add(scaleBarSizeSlider);

			scalebarsTextVisibility.add(scaleBarTextDisplayCheck);
			scalebarsTextVisibility.add(scaleTextValue);
			scalebarsOffsetsPanel.add(new JLabel("height:"));
			scalebarsOffsetsPanel.add(scaleBarHeight);

			scalebarsOffsetsPanel.add(new JLabel("   "));
			scalebarsOffsetsPanel.add(new JLabel(new ImageIcon(getClass().getResource("/imgs/iconBarRight.png"))));
			scalebarsOffsetsPanel.add(xOffsScales);
			scalebarsOffsetsPanel.add(new JLabel(new ImageIcon(getClass().getResource("/imgs/iconBarLow.png"))));
			scalebarsOffsetsPanel.add(yOffsScales);

			scalebarsPanel.add(scalebarsVisibilityAndSizePanel);
			scalebarsPanel.add(scalebarsTextVisibility);
			scalebarsPanel.add(scalebarsOffsetsPanel);

			// Overlay Items

			JPanel overlayItemsPanel = new JPanel(new GridLayout(4, 2));
			overlayItemsPanel.setPreferredSize(new Dimension(253, 140));
			overlayItemsPanel.setBorder(BorderFactory.createTitledBorder("Overlay Items"));
			overlayItemsPanel.add(newTextRoiButton);
			overlayItemsPanel.add(newArrowRoiButton);
			overlayItemsPanel.add(applyFontPtSize);
			overlayItemsPanel.add(fontPtSize);
			overlayItemsPanel.add(addItemToOverlayButton);
			overlayItemsPanel.add(duplicateItemButton);
			overlayItemsPanel.add(hideOverlayButton);
			overlayItemsPanel.add(showOverlayButton);

			// Miscellaneous Items
			JPanel miscItemsPanel = new JPanel(new GridLayout(5, 1));
			miscItemsPanel.setBorder(BorderFactory.createTitledBorder("Misc."));

			// TODO: enable this when workflow is made clear.
			// Prefs.set("figurej.lockPixelSize", false);

			//scaleFontsToDocDPI.setSelected(Prefs.get("figurej.scalefigurefonts", false));
			// miscItemsPanel.add(scaleFontsToDocDPI);

			miscItemsPanel.add(openColorPickerButton);
			miscItemsPanel.add(adoptPixelsButton);
			miscItemsPanel.add(changeSeparatorColorButton);
			miscItemsPanel.add(printFigure);

			// Fill the options panel
			optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
			optionsPanel.add(labelsPanel);
			optionsPanel.add(scalebarsPanel);
			optionsPanel.add(overlayItemsPanel);
			optionsPanel.add(miscItemsPanel);

			this.add(optionsPanel);
			this.setBackground(new Color(PanelSelection_Tool.getBGColor()));
			pack();
		}

		public void updateLabelField(String label) {
			panelCaptionTextField.setText(label);
			
		}

		/**
		 * settings of the buttons, combo boxes labels .. handling text label
		 * design
		 */
		private void initLabelGUI() {

			Iterator<String> itr = LabelDrawer.getPositionTypes().iterator();
			while (itr.hasNext())
				labelPositionComboBox.addItem(itr.next());
			labelPositionComboBox.setSelectedItem("TopLeft");

			itr = LabelDrawer.getLabelTypes().iterator();
			while (itr.hasNext())
				labelTypeConboBox.addItem(itr.next());

			labelTypeConboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if ((labelTypeConboBox.getSelectedItem() + "").equals(LabelType.userDefined.toString()))
						disablePanelCaptionTextField(false);
					else {
						disablePanelCaptionTextField(true);
							labelDraw.setCount(-1);
							boolean clear = (labelTypeConboBox.getSelectedItem() + "").equals(LabelType.none.toString());
							listAndLabelPanels(figure.getRootPanel(),clear); // for root panel children
							figure.highlightSelectedPanel();
					}
				}
			});
			labelTypeConboBox.setSelectedItem("userDefined");
			
			
			labelPositionComboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {					
					activePanel.setLabel(figure.getImagePlus(), activePanel.getLabel(), stringToNr(xLabelOffset, activePanel.getW() / 2),
							stringToNr(yLabelOffset, activePanel.getH() / 2),
							LabelPosition.valueOf(labelPositionComboBox.getSelectedItem() + ""));
							figure.highlightSelectedPanel();
				}
			});
			
			xLabelOffset.getDocument().addDocumentListener(new DocumentListener() {
				  public void changedUpdate(DocumentEvent e) {
					    updateValues();
					  }
					  public void removeUpdate(DocumentEvent e) {
						  updateValues();
					  }
					  public void insertUpdate(DocumentEvent e) {
						  updateValues();
					  }

					  public void updateValues() {
							figure.getImagePlus().setProp("xmargin", xLabelOffset.getText());

					  }
					});
			
			yLabelOffset.getDocument().addDocumentListener(new DocumentListener() {
				  public void changedUpdate(DocumentEvent e) {
					    updateValues();
					  }
					  public void removeUpdate(DocumentEvent e) {
						  updateValues();
					  }
					  public void insertUpdate(DocumentEvent e) {
						  updateValues();
					  }

					  public void updateValues() {
							figure.getImagePlus().setProp("ymargin", yLabelOffset.getText());
					  }
					});
			
			
		}

		private void initScalebarGUI() {
			scaleBarSizeSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent arg0) {
					scaleDisplayCheck.setSelected(true);
					int xOff = stringToNr(xOffsScales, activePanel.getW() / 3);
					scaleBarSizeSlider.setMaximum(activePanel.getW() - xOff);
					scaleBarSizeSlider.setToolTipText("scale bar length");
					double d = getClosestScaleBar();
					activePanel.setScalebar(figure.getImagePlus(), xOff,
							stringToNr(yOffsScales, activePanel.getH() / 2), d, stringToNr(scaleBarHeight, 30));
					figure.draw();
					IJ.showStatus(IJ.d2s(d * activePanel.getImgData().getPixelWidth(), 2) + " "
							+ activePanel.getImgData().getUnit());
					scaleTextValue.setText(activePanel.getShortScaleBarText());
				}
			});
			scaleDisplayCheck.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (!scaleDisplayCheck.isSelected()) {
						activePanel.removeScalebar(figure.getImagePlus().getOverlay());
						figure.draw();
					} else {
						int xOff = stringToNr(xOffsScales, activePanel.getW() / 2);
						scaleBarSizeSlider.setMaximum(activePanel.getW() - xOff);
						scaleBarSizeSlider.setToolTipText("scale bar length");
						double d = getClosestScaleBar();
						activePanel.setScalebarColor(Toolbar.getForegroundColor());
						activePanel.setScalebar(figure.getImagePlus(), xOff,
								stringToNr(yOffsScales, activePanel.getH() / 2), d, stringToNr(scaleBarHeight, 300));
						figure.draw();
						IJ.showStatus(IJ.d2s(d * activePanel.getImgData().getPixelWidth(), 2) + " "
								+ activePanel.getImgData().getUnit());
					}

				}
			});
			
//			scaleFontsToDocDPI.addActionListener(new ActionListener() {
//				@Override
//				public void actionPerformed(ActionEvent arg0) {
//					Prefs.set("figurej.scalefigurefonts",scaleFontsToDocDPI.isSelected());
//				}
//			});

			scaleBarTextDisplayCheck.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (!scaleBarTextDisplayCheck.isSelected()) {
						activePanel.setScaleBarTextVisible(false);
						activePanel.setScalebarLabelJustification(-1);
						figure.draw();
					} else {
						activePanel.setScaleBarTextVisible(true);
						activePanel.setScaleBarTextFont(new Font(TextRoi.getDefaultFontName(),
								TextRoi.getDefaultFontStyle(), TextRoi.getDefaultFontSize()));
						figure.draw();
					}

				}
			});
			lockScale.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (lockScale.isSelected()) {
						Prefs.set("figurej.lockPixelSize", true);
						double pixelWidth = activePanel.getImgData().getPixelWidth();
						Prefs.set("figurej.lockedPixelSize", pixelWidth);
						lockScale.setText("<html>Lock Pixel size: <font color=red>LOCKED</font></html>");

					} else {
						Prefs.set("figurej.lockPixelSize", false);
						lockScale.setText("<html>Lock Pixel size</html>");

					}

				}
			});
		}

		/** display a new text label on the active panel */
		public void addPanelLabel(boolean reset, boolean backwards, boolean doAllPanels) {

			if (doAllPanels&!(labelTypeConboBox.getSelectedItem().toString().equals(LabelType.userDefined.toString()))) {
				labelDraw.setCount(-1);
				listAndLabelPanels(figure.getRootPanel(), false); // for root panel children

				return;
			}			
			
			if (labelTypeConboBox.getSelectedItem().toString().equals(LabelType.userDefined.toString()))
				labelDraw.setUserLabels(panelCaptionTextField.getText());
			
			String label = labelDraw.getLabel(labelTypeConboBox.getSelectedItem() + "", reset, backwards);
			
			activePanel.setLabel(figure.getImagePlus(), label, stringToNr(xLabelOffset, activePanel.getW() / 2),
					stringToNr(yLabelOffset, activePanel.getH() / 2),
					LabelPosition.valueOf(labelPositionComboBox.getSelectedItem() + ""));
		}

		private void listAndLabelPanels(Panel rootPanel, boolean clear) {
			ArrayList<Panel> pa = rootPanel.getChildren();
			for (Panel p : pa) {
				if (p instanceof LeafPanel) {
					String label = labelDraw.getLabel(labelTypeConboBox.getSelectedItem() + "", false, false);
					p.setLabel(figure.getImagePlus(), clear?"":label, stringToNr(xLabelOffset, p.getW() / 2),
							stringToNr(yLabelOffset, p.getH() / 2),
							LabelPosition.valueOf(labelPositionComboBox.getSelectedItem() + ""));

				} else if (p instanceof ContainerPanel)
					listAndLabelPanels(p, clear);
			}
		}


		/** allow the user to type own label strings in a text field or not */
		private void disablePanelCaptionTextField(boolean disable) {
			if (disable) {
				panelCaptionTextField.setEnabled(false);
				panelCaptionTextField.setToolTipText("switch to 'user defined' to edit");
			} else {
				panelCaptionTextField.setEnabled(true);
				panelCaptionTextField.setToolTipText("insert your own labels, separate by semicolon");
			}
		}

		/** find good value depending on slider position */
		private double getClosestScaleBar() {
			double dist = 1000000;
			double[] c = new double[2];
			double mousedist = (scaleBarSizeSlider.getMaximum() - scaleBarSizeSlider.getValue())
					* activePanel.getImgData().getPixelWidth();
			for (int i = 0; i < b.length; i++)
				for (int j = 0; j < a.length; j++) {
					double currentdist = Math.abs(mousedist - a[j] * b[i]);
					if (currentdist < dist) {
						c[0] = a[j];
						c[1] = b[i];
						dist = currentdist;
					}
				}
			return (double) ((c[0] * c[1]) / activePanel.getImgData().getPixelWidth());
		}

		/**
		 * set info messages that pop up if mouse stays longer over a component
		 */
		private void addOptionsWindowToolTips() {
			drawLabelButton.setToolTipText("<html>Add text label to panel<br><br> +SHIFT: backwards<br> +ALT: reset<br> +CTRL-ALT:do all!</html>");
			removeLabelButton.setToolTipText("Delete text label from panel");
			labelTypeConboBox.setToolTipText("Choose label type");
			labelPositionComboBox.setToolTipText("Choose label position");
			xLabelOffset.setToolTipText("Vertical distance to panel border");
			yLabelOffset.setToolTipText("Horizontal distance to panel border");
			xOffsScales.setToolTipText("Distance to right panel border");
			yOffsScales.setToolTipText("Distance to lower panel border");
			scaleBarHeight.setToolTipText("Scalebar height");
///			scaleFontsToDocDPI.setToolTipText("Fonts pt sizes are corrected to document resolution");
			changeSeparatorColorButton.setToolTipText("Update separators color to current foreground color");
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
					IJ.showStatus("Scaling image. " + s.substring(0, i) + "***" + s.substring(0, 12 - i));
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
				IJ.error("FigureJ installMacroFromJar", "Unable to load \"" + name + "\" from jar file");
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

	// Some commands are not welcome on Figures, let's ignore them.
	@Override
	public String commandExecuting(String command) {
		if ((WindowManager.getImageCount() > 0) && (IJ.getImage().getTitle() == "FigureJ")
				&& command.equals("Make Composite"))
			return null; // do not run this command
		else
			return command;
	}

}
