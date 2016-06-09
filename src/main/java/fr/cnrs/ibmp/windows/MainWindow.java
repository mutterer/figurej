package fr.cnrs.ibmp.windows;

/*
 * @author Edda Zinck
 * @author Jerome Mutterer
 * (c) IBMP-CNRS   
 * 
 */
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
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
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import fr.cnrs.ibmp.treeMap.ContainerPanel;
import fr.cnrs.ibmp.treeMap.LeafPanel;
import fr.cnrs.ibmp.treeMap.Panel;
import fr.cnrs.ibmp.treeMap.SeparatorPanel;
import fr.cnrs.ibmp.dataSets.DataSource;

public class MainWindow extends ImagePlus implements Serializable {

	private static final long serialVersionUID = 1L;

	// figure data
	private double dpi = 300;

	// result image size
	private int figureWidth = 512;
	private int figureHeight = 512;

	// root of the tree of panel objects storing images+data or separator color
	private Panel rootPanel;

	public Panel getRootPanel() {
		return rootPanel;
	}

	// result image and its pixels
	private transient ImagePlus resultFigure;

	public ImagePlus getResultFigure() {
		return resultFigure;
	}

	public void setResultFigure(ImagePlus resultFigure) {
		this.resultFigure = resultFigure;
	}

	// last clicked panel
	private Panel selectedPanel = null;

	private int[] mousePressedPoint = new int[2];

	public int[] getMousePressedPoint() {
		return mousePressedPoint;
	}

	private int[] mouseReleasedPoint = new int[2];

	public int[] getMouseReleasedPoint() {
		return mouseReleasedPoint;
	}

	// control snap behavior of dragged separator panels
	private int lastXSnap = -1;
	private int lastYSnap = -1;
	private int lastX = -1;
	private int lastY = -1;

	// border width around separators wherein clicks are treated as clicks on
	// separators (important for tiny or invisible separators)
	private int clickTolerance = 10;
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
		init(frameX, frameY, separatorSize);
		dpi = resolution;

	}

	/**
	 * @param frameX
	 *            x position of the frame containing the result image
	 * @param frameY
	 *            y position of the frame containing the result image create
	 *            main window and register its pixels, add first leaf to the
	 *            panel tree
	 */
	public void init(int frameX, int frameY, int sepSize) {
		// ImageWindow.centerNextImage(); // gets rid of the image info banner
		// on
		// // top of the image.
		ColorProcessor cp = new ColorProcessor(figureWidth, figureHeight);
		resultFigure = new ImagePlus("FigureJ", cp);

		// start to build the panel tree structure
		rootPanel = new ContainerPanel(0, 0, figureWidth, figureHeight);
		Panel p = new LeafPanel(0, 0, figureWidth, figureHeight);
		selectedPanel = p;
		rootPanel.addChild(p);
		rootPanel.setSeparatorWidth(sepSize);
		rootPanel.draw(resultFigure);

		ImageWindow.centerNextImage(); // gets rid of the image info banner on
										// top of the image.
		resultFigure.show();

		ImageWindow window = resultFigure.getWindow();
		window.setLocation(frameX, frameY);
		window.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				resultFigure.changes = false;
			}
		});

		Prefs.set("figure.id", resultFigure.getID());

		showROI();
		resultFigure.deleteRoi();
	}

	/**
	 * @param xPos
	 *            x coordinate of the main window
	 * @param yPos
	 *            y coordinate of the main window to be called after
	 *            deserialization; rebuilds the main window (not serialized) and
	 *            draws the panel tree that was recovered
	 */
	public void recover() {
		ImageWindow.centerNextImage(); // gets rid of the image info banner on
										// top of the image.
		IJ.newImage("FigureJ", "RGB", figureWidth, figureHeight, 1);
		resultFigure = IJ.getImage();
		resultFigure
				.setProcessor(new ColorProcessor(figureWidth, figureHeight));
		Prefs.set("figure.id", resultFigure.getID());
		if (resultFigure.getOverlay() == null)
			resultFigure.setOverlay(new Overlay());
		rootPanel.draw(resultFigure);
		showROI();
	}

	/** @return result figure */
	public ImagePlus getImagePlus() {
		return resultFigure;
	}

	/**
	 * @param fileName
	 *            saves the file as png
	 */
	public void saveImage(String path, String fileName) {
		System.out.println("saving " + fileName);
		hideROI();
		new FileSaver(resultFigure.flatten()).saveAsTiff(path
				+ "FullResolution_" + fileName + ".tif");
		// // new
		// FileSaver(resultFigure.flatten()).saveAsZip(path+"ZIPPED_HIRES_"+fileName+".zip");
		new FileSaver(resultFigure.flatten()).saveAsJpeg(path
				+ "JpegCompressed_" + fileName + ".jpg");
	}

	/** @return the last panel the user clicked on */
	public Panel getSelectedPanel() {
		return selectedPanel;
	}

	/**
	 * @param p
	 *            define which panel behaves as if the user had clicked it
	 */
	public void setSelectedPanel(Panel p) {
		selectedPanel = p;
		showROI();

	}

	/**
	 * @param imp
	 * @param e
	 *            check which panel was clicked
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

	public void mouseMoved(ImagePlus imp, MouseEvent e) {
		ImageCanvas canv = imp.getCanvas();
		Panel hoveredPanel = rootPanel.getClicked(canv.offScreenX(e.getX()),
				canv.offScreenY(e.getY()), getTol());
		if (hoveredPanel != null) {
			if (hoveredPanel.getClass().getName()
					.contains(LeafPanel.class.getName())) {
				switchCursor("reset");
				if (IJ.isMacintosh()) IJ.showStatus("Double click to associate or open image");
			} else if (hoveredPanel.getClass().getName()
					.contains(SeparatorPanel.class.getName())) {
				if (hoveredPanel.getW() > hoveredPanel.getH()) {
					switchCursor("Up-down.png");
				} else {
					switchCursor("Left-right.png");
				}

				IJ.showStatus("Drag to adjust");

			} else {
				IJ.showStatus("");
			}
		}

	}

	protected static Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);

	private void switchCursor(String name) {
		// separators drag icons credits : http://www.oxygen-icons.org/

		if (name == "reset") {
			ImageCanvas.setCursor(defaultCursor, 0);

		} else {
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			String path = "/imgs/" + name;
			URL imageUrl = getClass().getResource(path);

			ImageIcon icon = new ImageIcon(imageUrl);
			Image image = icon.getImage();
			if (image == null) {
				IJ.log("img null");
				return;
			} else {
				int width = icon.getIconWidth();
				int height = icon.getIconHeight();
				Point hotSpot = new Point(width / 2, height / 2);
				Cursor crosshairCursor = toolkit.createCustomCursor(image,
						hotSpot, name);
				ImageCanvas.setCursor(crosshairCursor, 0);
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
		if (ignoreTolerance)
			selectedPanel = rootPanel.getClicked(mousePressedPoint[0],
					mousePressedPoint[1], 0);
		else
			selectedPanel = rootPanel.getClicked(mousePressedPoint[0],
					mousePressedPoint[1], getTol());
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

	/**
	 * @return distance which is added to separators so that they can get
	 *         clicked also if they are very thin depends on zoom factor and
	 *         result image size
	 */
	private int getTol() {
		return (int) (clickTolerance / resultFigure.getCanvas()
				.getMagnification());
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
	 */
	public void readInOldOveray(String path) {
		RoiManager roiManager = new RoiManager(false);
		roiManager.runCommand("Open", path);
		roiManager.runCommand("Show All");
	}

	public void mouseDragged(ImagePlus imp, MouseEvent e) {
		// move separators
		// snapping of separators to other separator start, end and middle
		// points
		if (selectedPanel.getClass().getName()
				.contains(SeparatorPanel.class.getName())) {
			ImageCanvas canv = imp.getCanvas();
			int x1 = canv.offScreenX(e.getX());
			int y1 = canv.offScreenY(e.getY());

			int closestX = rootPanel.getClosestX(selectedPanel, Integer.MAX_VALUE);
			int closestY = rootPanel.getClosestY(selectedPanel, Integer.MAX_VALUE);
			
			if (Math.abs(closestX - x1) < SeparatorPanel.snapDist) x1 = closestX;
			if (Math.abs(closestY - y1) < SeparatorPanel.snapDist) y1 = closestY;
				
			
			selectedPanel.getParent().reShape(x1, y1,
					(SeparatorPanel) selectedPanel);

			// log the separator's parents panels dimensions to the status bar.
			String dimensions = "";
			for (Panel p : selectedPanel.getParent().getChildren()) {
				if (!p.getClass().getName().contains("Sep"))
					dimensions += IJ.d2s(p.getW()
							* imp.getCalibration().pixelWidth, 2)
							+ "x"
							+ IJ.d2s(p.getH()
									* imp.getCalibration().pixelHeight, 2)
							+ " ";
			}
			IJ.showStatus(dimensions + imp.getCalibration().getUnit());

		} else {
			IJ.showStatus("dragging");
		}

		resultFigure.setProperty("Info", "");
		rootPanel.draw(resultFigure);
		showROI();
	}

	/**
	 * load the pixels of every panel to the result image; pass these changed
	 * pixels to the processor and redraw the result image
	 */
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
	public List<DataSource> getDataSources() {
		ArrayList<DataSource> list = new ArrayList<DataSource>();
		rootPanel.getDataSources(list);
		return list;
	}

	public void hideROI() {
		resultFigure.deleteRoi();
	}

	public void hideAllLabelsAndScalebars() {
		rootPanel.hideLabel(resultFigure.getOverlay());
		rootPanel.hideScalebar(resultFigure.getOverlay());
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

}
