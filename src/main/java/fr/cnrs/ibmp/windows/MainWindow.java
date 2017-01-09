// TODO Missing license header
package fr.cnrs.ibmp.windows;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.plugin.frame.RoiManager;
import ij.process.ColorProcessor;

import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import fr.cnrs.ibmp.treeMap.ContainerPanel;
import fr.cnrs.ibmp.treeMap.LeafPanel;
import fr.cnrs.ibmp.treeMap.Panel;
import fr.cnrs.ibmp.treeMap.SeparatorPanel;
import fr.cnrs.ibmp.LeafEvent;
import fr.cnrs.ibmp.LeafListener;
import fr.cnrs.ibmp.dataSets.DataSource;
import fr.cnrs.ibmp.dataSets.FileDataSource;

/**
 * Implements the interactive visualization of the current state of a figure.
 * <p>
 * (c) IBMP-CNRS
 * </p>
 * 
 * @author Edda Zinck
 * @author Jerome Mutterer
 * @author Stefan Helfrich (University of Konstanz)
 */
public class MainWindow extends ImagePlus implements Serializable, LeafListener {

	private static final long serialVersionUID = 1L;

	/* --- Figure data --- */
	/** TODO Documentation */
	private double dpi = 300;

	/** Result image size */
	private int figureWidth = 512;
	private int figureHeight = 512;

	/** Root of the tree of panel objects storing images+data or separator color. */
	private Panel rootPanel;

	/** Result image and its pixels. */
	private transient ImagePlus resultFigure;

	/** {@link Panel} that was last clicked. */
	private Panel selectedPanel;

	// TODO Use Point instead?
	/** TODO Documentation */
	private int[] mousePressedPoint = new int[2];

	// TODO Use Point instead?
	/** TODO Documentation */
	private int[] mouseReleasedPoint = new int[2];

	/* --- control snap behavior of dragged separator panels --- */
	private int lastXSnap = -1;
	private int lastYSnap = -1;
	private int lastX = -1;
	private int lastY = -1;

	/**
	 * Border width (in pixels) around separators wherein clicks are treated as
	 * clicks on separators (important for tiny or invisible separators).
	 */
	private int clickTolerance = 10;

	/**
	 * Setting for not saving the MainWindow when it's closed.
	 * 
	 * @deprecated Currently not used, might be removed in future versions
	 */
	@Deprecated
	private boolean quitWithoutSaving = false;

	/**
	 * @param w
	 *            width of the result image
	 * @param h
	 *            height of the result image
	 * @param frameX
	 *            x position of the frame containing the result image
	 * @param frameY
	 *            y position of the frame containing the result image
	 * @param resolution
	 *            resolution of the result image in DPI
	 * @param separatorSize
	 *            distance or border width between different small images/panels
	 *            on the result image
	 */
	public MainWindow(int w, int h, int frameX, int frameY, double resolution,
			int separatorSize) {
		figureWidth = w;
		figureHeight = h;
		dpi = resolution;
		init(frameX, frameY, separatorSize);
	}

	/**
	 * Initializes the main ImagePlus that represents the figure. TODO Remove
	 * sepSize parameter
	 * 
	 * @param frameX x position of the frame containing the result image
	 * @param frameY y position of the frame containing the result image create
	 *          main window and register its pixels, add first leaf to the panel
	 *          tree
	 */
	public void init(int frameX, int frameY, int sepSize) {
		ColorProcessor cp = new ColorProcessor(figureWidth, figureHeight);
		resultFigure = new ImagePlus("FigureJ", cp);

		// start to build the panel tree structure
		rootPanel = new ContainerPanel(0, 0, figureWidth, figureHeight);
		Panel p = new LeafPanel(0, 0, figureWidth, figureHeight);
		selectedPanel = p;
		rootPanel.addChild(p);
		rootPanel.setSeparatorWidth(sepSize);
		rootPanel.draw(resultFigure);

		// Get rid of the image info banner on top of the image
		ImageWindow.centerNextImage(); 
		resultFigure.show();

		ImageWindow window = resultFigure.getWindow();
		window.setLocation(frameX, frameY);
		
		// Avoid ImageJ asking to save the image
		window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				resultFigure.changes = false;
			}
		});

		Prefs.set("figure.id", resultFigure.getID());

		// TODO Why?
		showROI();
		resultFigure.deleteRoi();
	}

	/**
	 * Creates a new {@link #resultFigure} and uses the (deserialized)
	 * {@link #rootPanel} to recreate a figure.
	 * 
	 * @param xPos x coordinate of the main window
	 * @param yPos y coordinate of the main window to be called after
	 *          deserialization; rebuilds the main window (not serialized) and
	 *          draws the panel tree that was recovered
	 */
	public void recover() {
		ColorProcessor cp = new ColorProcessor(figureWidth, figureHeight);
		resultFigure = new ImagePlus("FigureJ", cp);

		// TODO Why is this stored via the Prefs API?
		Prefs.set("figure.id", resultFigure.getID());

		// TODO This should be obsolete, because the ImagePlus was just created
		if (resultFigure.getOverlay() == null)
			resultFigure.setOverlay(new Overlay());

		// Draw the recovered state from rootPanel onto the new image
		rootPanel.draw(resultFigure);
		showROI();

		// Get rid of the image info banner on top of the image
		ImageWindow.centerNextImage();
		resultFigure.show();
		
		// TODO Set location?
		// TODO Disable the "Do you want to save?" message?
	}

	/**
	 * @param fileName
	 *            saves the file as png
	 */
	public void saveImage(String path, String fileName) {
		hideROI();

		IJ.log("saving " + fileName);
		FileSaver fileSaver = new FileSaver(resultFigure.flatten());
		fileSaver.saveAsTiff(path + "FullResolution_" + fileName + ".tif");
		fileSaver.saveAsJpeg(path + "JpegCompressed_" + fileName + ".jpg");
	}

	/**
	 * TODO Documentation
	 * 
	 * @param imp
	 * @param e
	 */
	public void mouseDragged(ImagePlus imp, MouseEvent e) {
		// move separators
		// snapping of separators to other separator start, end and middle
		// points
		if (selectedPanel instanceof SeparatorPanel) {
			ImageCanvas canv = imp.getCanvas();
			int x1 = canv.offScreenX(e.getX());
			int y1 = canv.offScreenY(e.getY());

			int closestX = rootPanel.getClosestX(selectedPanel, Integer.MAX_VALUE);
			int closestY = rootPanel.getClosestY(selectedPanel, Integer.MAX_VALUE);

			if (Math.abs(closestX - x1) < SeparatorPanel.snapDist) x1 = closestX;
			if (Math.abs(closestY - y1) < SeparatorPanel.snapDist) y1 = closestY;

			selectedPanel.getParent().reShape(x1, y1, (SeparatorPanel) selectedPanel);

			// log the separator's parents panels dimensions to the status bar.
			String dimensions = "";
			for (Panel p : selectedPanel.getParent().getChildren()) {
				if (!(p instanceof SeparatorPanel))
					dimensions += String.format("%d.2x%d.2 %s", p.getW() * imp
						.getCalibration().pixelWidth, p.getH() * imp
							.getCalibration().pixelHeight);
			}
			IJ.showStatus(dimensions + imp.getCalibration().getUnit());
		} else {
			IJ.showStatus("dragging");
		}

		// Clear image properties
		resultFigure.setProperty("Info", "");

		rootPanel.draw(resultFigure);
		showROI();
	}

	/**
	 * TODO Documentation
	 * 
	 * @param imp
	 * @param e
	 */
	public void mouseMoved(ImagePlus imp, MouseEvent e) {
		ImageCanvas canv = imp.getCanvas();
		Panel hoveredPanel = rootPanel.getClicked(canv.offScreenX(e.getX()), canv
			.offScreenY(e.getY()), getTol());
		if (hoveredPanel != null) {
			if (hoveredPanel instanceof LeafPanel) {
				switchCursor("reset");
				if (IJ.isMacintosh()) IJ.showStatus(
					"Double click to associate or open image");
			}
			else if (hoveredPanel instanceof SeparatorPanel) {
				if (hoveredPanel.getW() > hoveredPanel.getH()) {
					// TODO Replace with built-in cursor
					switchCursor("Up-down.png");
				}
				else {
					// TODO Replace with built-in cursor
					switchCursor("Left-right.png");
				}
				IJ.showStatus("Drag to adjust");
			}
			else {
				IJ.showStatus("");
			}
		}
	}

	/**
	 * TODO Documentation
	 * 
	 * @param imp
	 * @param e check which panel was clicked
	 */
	public void mousePressed(ImagePlus imp, MouseEvent e) {
		ImageCanvas canv = imp.getCanvas();
		Panel clickedPanel = rootPanel.getClicked(canv.offScreenX(e.getX()),
				canv.offScreenY(e.getY()), getTol());

		if (clickedPanel != null) {
			selectedPanel = clickedPanel;
			showROI();
		}
		mousePressedPoint[0] = canv.offScreenX(e.getX());
		mousePressedPoint[1] = canv.offScreenY(e.getY());

	}

	public void mouseReleased(ImagePlus imp, MouseEvent e) {
		// IJ.log("release");
		ImageCanvas canv = imp.getCanvas();
		Panel releasedPanel = rootPanel.getClicked(canv.offScreenX(e.getX()),
				canv.offScreenY(e.getY()), getTol());
		if ((releasedPanel != null) && (releasedPanel != selectedPanel)) {
			IJ.showStatus("What did you expect?");
		}
		mouseReleasedPoint[0] = canv.offScreenX(e.getX());
		mouseReleasedPoint[1] = canv.offScreenY(e.getY());
		showROI();
	}

	protected static Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);

	/**
	 * Separators drag icons credits : http://www.oxygen-icons.org/
	 * 
	 * @param name
	 */
	private void switchCursor(String name) {
		if (name.equals("reset")) {
			ImageCanvas.setCursor(defaultCursor, 0);
		}
		else {
			URL imageUrl = getClass().getResource("/imgs/" + name);
			ImageIcon icon = new ImageIcon(imageUrl);
			Image image = icon.getImage();
			if (image != null) {
				int iconWidth = icon.getIconWidth();
				int iconHeight = icon.getIconHeight();
				Point hotSpot = new Point(iconWidth / 2, iconHeight / 2);

				Toolkit toolkit = Toolkit.getDefaultToolkit();
				Cursor crosshairCursor = toolkit.createCustomCursor(image, hotSpot,
					name);
				ImageCanvas.setCursor(crosshairCursor, 0);

			}
			else {
				IJ.log("Mouse cursor icon could not be found.");
			}
		}
	}

	/**
	 * set that panel as active that fits to the coordinates of the last click
	 * 
	 * @param ignoreTolerance
	 *            if true the coordinates are used unchanged - nullPointer save.
	 *            else a tolerance value is subtracted from leaves and added to
	 *            separators so that tiny separators can get clicked.
	 */
	public void updateSelectedPanel(boolean ignoreTolerance) {
		selectedPanel = rootPanel.getClicked(mousePressedPoint[0],
			mousePressedPoint[1], ignoreTolerance ? 0 : getTol());
	}

	/**
	 * @return distance which is added to separators so that they can get clicked
	 *         also if they are very thin depends on zoom factor and result image
	 *         size
	 */
	private int getTol() {
		return (int) (clickTolerance / resultFigure.getCanvas().getMagnification());
	}

	/** highlight the selected panel */
	private void showROI() {
		resultFigure.setRoi(selectedPanel.getHighlightROI());
		resultFigure.updateImage();
	}

	/**
	 * @param path
	 *            open a zip file in the given directory, read in the ROIs it
	 *            contains and display them
	 * @deprecated Use {@link #readInOldOveray(File)} instead.
	 */
	@Deprecated
	public void readInOldOveray(String path) {
		RoiManager roiManager = new RoiManager(false);
		roiManager.runCommand("Open", path);
		roiManager.runCommand("Show All");
	}

	/**
	 * TODO Documentation
	 * 
	 * @param path
	 *            open a zip file in the given directory, read in the ROIs it
	 *            contains and display them
	 */
	public void readInOldOveray(final File path) {
		RoiManager roiManager = new RoiManager(false);
		roiManager.runCommand("Open", path.getAbsolutePath());
		roiManager.runCommand("Show All");
	}

	/**
	 * load the pixels of every panel to the result image; pass these changed
	 * pixels to the processor and redraw the result image
	 */
	@Override
	public void draw() {
		resultFigure.changes = true;
		resultFigure.setProperty("Info", "");
		rootPanel.draw(resultFigure);
		showROI();
	}

	// unused atm
	/**
	 * store the pixels of the result image in the pixel arrays of the fitting
	 * leaf panels makes sense in cases where the result image is changed by
	 * native IJ methods or other plugins
	 */
	public void adoptExternalChanges() {
		rootPanel.setPixels(resultFigure);
	}

	/**
	 * TODO Documentation
	 * 
	 * @param resolution
	 * @param unit
	 */
	public void calibrateImage(double resolution, String unit) {
		Calibration c = resultFigure.getCalibration();
		c.setXUnit(unit);
		c.setYUnit(unit);
		c.setUnit(unit);
		c.setValueUnit(unit);
		double factor;
		if (unit == "cm")
			factor = 2.54 / resolution;
		else if (unit == "mm")
			factor = 25.4 / resolution;
		else
			factor = 1 / resolution;
		c.pixelWidth = factor;
		c.pixelHeight = factor;
		resultFigure.setCalibration(c);
	}

	/** @return dpi of the result image */
	public double getDPI() {
		return this.dpi;
	}

	/**
	 * @return string containing all notes and data source infos of all leaf
	 *         panels
	 */
	public String getAllImageNotes() {
		return rootPanel.generateImageNotesString("");
	}

	/**
	 * get a list containing every dataSource used in the panel/tree structure
	 * (one for each leaf)
	 */
	public List<FileDataSource> getDataSources() {
		ArrayList<FileDataSource> list = new ArrayList<FileDataSource>();
		rootPanel.getDataSources(list);
		return list;
	}

	/**
	 * Deletes the {@link Roi} that denotes the currently selected panel in the
	 * figure.
	 */
	public void hideROI() {
		resultFigure.deleteRoi();
	}

	/**
	 * Hides labels and scalebars on the figure.
	 * <p>
	 * Delegates this tasks to the {@link #rootPanel}.
	 * </p>
	 * TODO Should the rootPanel be responsible for this?
	 */
	public void hideAllLabelsAndScalebars() {
		rootPanel.hideLabel(resultFigure.getOverlay());
		rootPanel.hideScalebar(resultFigure.getOverlay());
	}

	/**
	 * @return the last panel the user clicked on
	 */
	public Panel getSelectedPanel() {
		return selectedPanel;
	}

	/**
	 * Sets {@link #selectedPanel} and makes it the active selection in the UI.
	 */
	public void setSelectedPanel(final Panel p) {
		selectedPanel = p;
		showROI();
	}

	public boolean getQuitWithoutSaving() {
		return quitWithoutSaving;
	}

	public void setQuitWithoutSaving(boolean quitWithoutSaving) {
		this.quitWithoutSaving = quitWithoutSaving;
	}

	public void setCal(Calibration c) {
		this.resultFigure.setCalibration(c);
	}

	public ImagePlus getResultFigure() {
		return resultFigure;
	}

	public void setResultFigure(ImagePlus resultFigure) {
		this.resultFigure = resultFigure;
	}

	public Point getMousePressedPoint() {
		return new Point(mousePressedPoint[0], mousePressedPoint[1]);
	}

	public Point getMouseReleasedPoint() {
		return new Point(mouseReleasedPoint[0], mouseReleasedPoint[1]);
	}

	public Panel getRootPanel() {
		return rootPanel;
	}

	/** @return result figure */
	public ImagePlus getImagePlus() {
		return resultFigure;
	}
	
	@Override
	public void leafSelected(LeafEvent e) { /* NB */ }

	@Override
	public void leafDeselected(LeafEvent e) { /* NB */ }

	@Override
	public void leafResized(LeafEvent e) { /* NB */ }

	@Override
	public void leafCleared(LeafEvent e) {
		draw();
	}

	@Override
	public void leafRemoved(LeafEvent e) {
		updateSelectedPanel(true);
		draw();
	}

	@Override
	public void leafSplit(LeafEvent e) {
		updateSelectedPanel(true);
		draw();
	}

}
