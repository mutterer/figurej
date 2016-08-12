package fr.cnrs.ibmp.windows;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.event.EventListenerList;

import fr.cnrs.ibmp.ImageSelectionEvent;
import fr.cnrs.ibmp.ImageSelectionListener;
import fr.cnrs.ibmp.LeafEvent;
import fr.cnrs.ibmp.LeafListener;
import fr.cnrs.ibmp.treeMap.LeafPanel;
import fr.cnrs.ibmp.utilities.MyImageMath;
import ij.IJ;
import ij.IJEventListener;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.plugin.frame.Recorder;
import ij.plugin.tool.PlugInTool;
import ij.process.ColorProcessor;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

/**
 * This class realizes a new ImageJ tool as plugin that allows so draw a
 * rectangular region of interest on a overlay. dragging, scaling and rotating
 * the ROI does not change its sides aspect ratio.
 * 
 * FIXME: bad angle calculation leads to mirror condition at some angle
 * boundaries (blue line flips during rotation)
 * 
 * (c) IBMP-CNRS
 * 
 * @author Edda Zinck
 * @author Jerome Mutterer
 * @author Stefan Helfrich
 */
public class ROIToolWindow extends PlugInTool implements KeyListener, LeafListener, IJEventListener {

	/** TODO Documentation */
	private EventListenerList listeners = new EventListenerList();

	/**
	 * The {@link Roi} that is drawn on the active image to indicate the cropping
	 * region. It is {@code null} if the tool was not initialized yet.
	 */
	private Roi rectRoi;

	public static String toolName = "FigureJ ROITool";

	/** {@link Recorder} instance used for saving changes to images. */
	private Recorder recorder;

	/**
	 * @return the {@link Recorder} instance
	 */
	public Recorder getRecorder() {
		return recorder;
	}

	/**
	 * @return the recorded changes from {@link #recorder}.
	 */
	public String getRecordedChanges() {
		return recorder.getText();
	}
	
	/**
	 * Horizontal size (width) of the selection ROI in the active image. Computed
	 * from the panel width via down-scaling (keeping the aspect ratio).
	 */
	private double horizontalSize = 150.0;
	
	/**
	 * Vertical size (height) of the selection ROI in the active image. Computed
	 * from the panel height via down-scaling (keeping the aspect ratio).
	 */
	private double verticalSize = 100.0;

	// angle the ROI is rotated relative to its initial position
	private double angle = 0d;

	// grab distance for the rotation handles (small red lines)
	private static int grabDistance = 20;
	private int x0, y0;
	private double cx, cy, xp, yp;

	// current ROI edge coordinates
	private double[] xVals = new double[4]; // 0-------3 edge indices
	private double[] yVals = new double[4]; // | |
	// // 1-------2
	// ROI coordinates in the last position
	private double[] saveX = new double[4];
	private double[] saveY = new double[4];

	// click position indicators
	private boolean inside = false;
	private boolean cornerClick = false;
	private boolean handeClick = false;

	private ImagePlus imagePlus;

	private int interpolationMethod;

	private int panelWidth;

	private int panelHeight;

	protected static Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);

	/**
	 * @param prefW
	 *            preferred width of the ROI
	 * @param prefH
	 *            preferred height of the ROI if w or h are too large for the
	 *            opened image they get resized, but the aspect ratio is
	 *            preserved
	 */
	public ROIToolWindow(double prefW, double prefH) {
		horizontalSize = prefW;
		verticalSize = prefH;
	}

	public ROIToolWindow() {
		run("");
	}

	public void init(ImagePlus imp) {
		this.imagePlus = imp;

		// set size of the ROI
		double w = imp.getWidth();
		double h = imp.getHeight();

		// as long as the size of the ROI is too large, make it smaller
		while (horizontalSize >= w || verticalSize >= h) {
			horizontalSize /= 1.5;
			verticalSize /= 1.5;
		}

		// TODO Keep externally set coordinates
		// calculate a centered position
		double xOffset = w / 2 - horizontalSize / 2;
		double yOffset = h / 2 - verticalSize / 2;
		xVals[0] = xOffset;
		xVals[1] = xOffset;
		xVals[2] = xOffset + horizontalSize;
		xVals[3] = xOffset + horizontalSize;

		yVals[0] = yOffset;
		yVals[1] = yOffset + verticalSize;
		yVals[2] = yOffset + verticalSize;
		yVals[3] = yOffset;
		cx = (xVals[0] + xVals[2]) / 2;
		cy = (yVals[0] + yVals[2]) / 2;

		// remember this position
		saveX = xVals.clone();
		saveY = yVals.clone();
	}

	/**
	 * Use a default size ROI and rotates it about the given angle
	 * 
	 * @param angle rotation angle in degree
	 */
	public void init(ImagePlus imp, double angle) {
		init(imp);
		this.angle = angle * Math.PI / 180;
		transformRect(imp, 1, angle);
	}

	/**
	 * Resets the state of {@code this}.
	 */
	private void reset() {
		// Reset imagePlus
		imagePlus = null;

		// Reset rectRoi to disable further cursor changes
		rectRoi = null;

		// Reset cursor
		switchCursor("reset");
	}

	/**
	 * check whether the user clicked near an edge or separator or outside of the
	 * ROI
	 */
	@Override
	public void mousePressed(ImagePlus imp, MouseEvent e) {

		ImageCanvas ic = imp.getCanvas();
		int x = ic.offScreenX(e.getX());
		int y = ic.offScreenY(e.getY());
		x0 = x;
		y0 = y;

		// assert rectRoi already exists
		if (rectRoi == null) {
			init(imp);
			drawRect(imp);
		}

		// Create cropped image
		int count = e.getClickCount();
		if (count == 2) {
			ImagePlus generatedCroppedImagePlus = generateCroppedImagePlus();
			notifyImageSelected(new ImageSelectionEvent(generatedCroppedImagePlus,
				xVals, yVals, getRecordedChanges()));

			reset();

			return;
		}

		// check if clicked inside rectangle
		inside = rectRoi.contains(x, y);
		// check if clicked within grab distance of a corner
		cornerClick = false;
		for (int i = 0; i < xVals.length; i++) {
			double d = Math.sqrt((x - xVals[i]) * (x - xVals[i]) + (y - yVals[i]) *
				(y - yVals[i]));
			if (d < grabDistance) {
				cornerClick = true;
			}
		}
		// check if clicked within grab distance of median line and outside
		handeClick = false;
		for (int i = 0; i < 4; i++) {
			double d0 = Math.sqrt((x - xVals[i]) * (x - xVals[i]) + (y - yVals[i]) *
				(y - yVals[i]));
			double d1 = Math.sqrt((x - xVals[(i + 1) % 4]) * (x - xVals[(i + 1) %
				4]) + (y - yVals[(i + 1) % 4]) * (y - yVals[(i + 1) % 4]));
			if (Math.abs(d1 - d0) < grabDistance) {
				handeClick = !inside;
			}
		}

		if ((!inside && !cornerClick && !handeClick) || IJ.altKeyDown()) {
			init(imp);
			drawRect(imp);
		}

		saveX = xVals.clone();
		saveY = yVals.clone();
		// IJ.showStatus((medianClick ? "median " : " ") + (cornerClick ?
		// "corner " : " ") + (inside ? "inside" : "outside"));
	}

	@Override
	public void mouseMoved(ImagePlus imp, MouseEvent e) {
		ImageCanvas ic = imp.getCanvas();
		int x = ic.offScreenX(e.getX());
		int y = ic.offScreenY(e.getY());
		x0 = x;
		y0 = y;

		// assert rect already exists
		if (rectRoi != null) {
			// check if clicked inside rectangle
			boolean inside = rectRoi.contains(x, y);
			// check if clicked within grab distance of a corner
			boolean cornerClick = false;
			for (int i = 0; i < xVals.length; i++) {
				double d = Math.sqrt((x - xVals[i]) * (x - xVals[i]) + (y - yVals[i]) *
					(y - yVals[i]));
				if (d < grabDistance) {
					cornerClick = true;
				}
			}

			// check if clicked within grab distance of median line and outside
			boolean handeClick = false;
			for (int i = 0; i < 4; i++) {
				double d0 = Math.sqrt((x - xVals[i]) * (x - xVals[i]) + (y - yVals[i]) *
					(y - yVals[i]));
				double d1 = Math.sqrt((x - xVals[(i + 1) % 4]) * (x - xVals[(i + 1) %
					4]) + (y - yVals[(i + 1) % 4]) * (y - yVals[(i + 1) % 4]));
				if (Math.abs(d1 - d0) < grabDistance) {
					handeClick = !inside;
				}
			}

			saveX = xVals.clone();
			saveY = yVals.clone();
			// IJ.showStatus((medianClick ? "median " : " ") + (cornerClick ?
			// "corner " : " ") + (inside ? "inside" : "outside"));
			if (cornerClick) {
				switchCursor("Expand.png");
			}
			else if (handeClick) {
				switchCursor("Refresh.png");
			}
			else if (inside) {
				switchCursor("Move.png");
			}
			else {
				switchCursor("reset");
			}
		}
	}

	/**
	 * Changes the cursor to the icon from the provided filename, e.g.
	 * "Expand.png". Icons are read from {@code src/main/resources/imgs}.
	 * 
	 * @param name Filename of the cursor icon to set
	 */
	private void switchCursor(String name) {
		// separators drag icons credits : http://www.oxygen-icons.org/
		if (name == "reset") {
			ImageCanvas.setCursor(defaultCursor, 0);
		}
		else {
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			String path = "/imgs/" + name;
			URL imageUrl = getClass().getResource(path);

			ImageIcon icon = new ImageIcon(imageUrl);
			Image image = icon.getImage();
			if (image == null) {
				IJ.log("img null");
				return;
			}
			int width = icon.getIconWidth();
			int height = icon.getIconHeight();
			Point hotSpot = new Point(width / 2, height / 2);
			Cursor crosshairCursor = toolkit.createCustomCursor(image, hotSpot, name);
			ImageCanvas.setCursor(crosshairCursor, 0);
		}
	}

	/** depending on where the user clicked the ROI is scaled, rotated or moved */
	@Override
	public void mouseDragged(ImagePlus imp, MouseEvent e) {
		if (cornerClick) {
			scaleRect(imp, e);
		} else if (handeClick) {
			rotateRect(imp, e);
		} else if (inside) {
			moveRect(imp, e);
		}
		drawRect(imp);
	}

	/** remember the coordinates of the current position */
	@Override
	public void mouseReleased(ImagePlus imp, MouseEvent e) {
		ImageCanvas ic = imp.getCanvas();
		int x = ic.offScreenX(e.getX());
		int y = ic.offScreenY(e.getY());
		x0 = x;
		y0 = y;
		saveX = xVals.clone();
		saveY = yVals.clone();
	}

	@Override
	public void showOptionsDialog() { /* NB */ }

	@Override
	public String getToolName() {
		return ROIToolWindow.toolName;
	}
	
	@Override
	public String getToolIcon() {
		// TODO Define unique icon
		return "C037D06D15D16D24D25D26D27D28D29D2aD33D34D35D36D37D3bD3cD42D43D44D45D46D47D48D4cD4dDb1Db2Db6Db7Db8Db9DbaDbbDbcDc2Dc3Dc7Dc8Dc9DcaDcbDd4Dd5Dd6Dd7Dd8Dd9DdaDe8De9Df8CabcD05D14D17D18D19D1aD23D2bD2cD32D3dD41D51D52D53D54D55D56D57D58Da6Da7Da8Da9DaaDabDacDadDbdDc1DccDd2Dd3DdbDe4De5De6De7DeaDf9";
	}

	/**
	 * draw a region of interest (ROI) on an overlay using the rectRoi coordinates
	 */
	public void drawRect(ImagePlus imp) {
		if (imagePlus == imp) {
			Overlay overlay = new Overlay();

			// setup floats arrays from rect to add as polygon ROI
			float[] px = new float[xVals.length];
			float[] py = new float[xVals.length];
			for (int i = 0; i < 4; i++) {
				px[i] = (float) xVals[i];
				py[i] = (float) yVals[i];
			}

			// create ROI and its layout
			rectRoi = new PolygonRoi(px, py, 4, Roi.POLYGON);
			rectRoi.setStrokeWidth(1);
			rectRoi.setStrokeColor(new Color(255, 0, 0));
			overlay.add(rectRoi);

			drawHandle(overlay, xVals[1], yVals[1], Color.white);
			drawHandle(overlay, xVals[2], yVals[2], Color.white);

			// add handles
			for (int i = 0; i < 4; i++) {
				xp = (xVals[i] + xVals[(i + 1) % 4]) / 2;
				yp = (yVals[i] + yVals[(i + 1) % 4]) / 2;
				angle = Math.atan((xVals[(i + 1) % 4] - xVals[i]) / (yVals[(i + 1) %
					4] - yVals[i]));
				double sign = (rectRoi.contains((int) (xp + 2 * Math.cos(angle)),
					(int) (yp - 2 * Math.sin(angle)))) ? -1 : 1;
				Line axis = new Line(xp, yp, xp + 20 * Math.cos(angle) * sign, yp - 20 *
					Math.sin(angle) * sign);
				axis.setStrokeColor(new Color(255, 0, 0));
				axis.setStrokeWidth(1);
				overlay.add(axis);
				drawHandle(overlay, axis.x2d, axis.y2d, Color.gray);
			}

			// add top line in a different color to visualize top
			Line top = new Line(xVals[0], yVals[0], xVals[3], yVals[3]);
			top.setStrokeColor(new Color(0, 0, 255));
			top.setStrokeWidth(1);
			overlay.add(top);
			drawHandle(overlay, top.x1d, top.y1d, Color.white);
			drawHandle(overlay, top.x2d, top.y2d, Color.white);

			imp.setOverlay(overlay);

			double w = imp.getCalibration().pixelWidth * Math.sqrt((xVals[0] -
				xVals[3]) * (xVals[0] - xVals[3]) + (yVals[0] - yVals[3]) * (yVals[0] -
					yVals[3]));
			double h = imp.getCalibration().pixelWidth * Math.sqrt((xVals[0] -
				xVals[1]) * (xVals[0] - xVals[1]) + (yVals[0] - yVals[1]) * (yVals[0] -
					yVals[1]));

			IJ.showStatus("(" + IJ.d2s(w, 3) + " , " + IJ.d2s(h, 3) + ")");
		}
	}

	/**
	 * TODO Documentation
	 * 
	 * @param overlay
	 * @param x2d
	 * @param y2d
	 * @param color
	 */
	private void drawHandle(Overlay overlay, double x2d, double y2d, Color color) {
		double zoomFactor = IJ.getImage().getCanvas().getMagnification();
		double size = 6.0d/zoomFactor;
		Roi r = new Roi(x2d-size/2, y2d-size/2,size,size);
		r.setFillColor(Color.black);
		overlay.add(r);
		size = 4.0d/zoomFactor;
		Roi r2 = new Roi(x2d-size/2, y2d-size/2,size,size);
		r2.setFillColor(color);
		overlay.add(r2);
	}

	/** move the ROI preserving size and rotation angle */
	private void moveRect(ImagePlus imp, MouseEvent e) {
		ImageCanvas ic = imp.getCanvas();
		int x = ic.offScreenX(e.getX());
		int y = ic.offScreenY(e.getY());
		int dx = x - x0;
		int dy = y - y0;

		int imgW = imp.getWidth();
		int imgH = imp.getHeight();

		// avoid that the rectangle is dragged outside of the canvas
		for (int i = 0; i < 4; i++) {
			if (saveX[i] + dx >= imgW)
				dx = (int) ((saveX[i] - imgW) * -1);
			if (saveX[i] + dx < 0)
				dx = (int) (saveX[i] * -1);
			if (saveY[i] + dy >= imgH)
				dy = (int) ((saveY[i] - imgH) * -1);
			if (saveY[i] + dy < 0)
				dy = (int) (saveY[i] * -1);
		}

		xVals[0] = saveX[0] + dx;
		xVals[1] = saveX[1] + dx;
		xVals[2] = saveX[2] + dx;
		xVals[3] = saveX[3] + dx;
		yVals[0] = saveY[0] + dy;
		yVals[1] = saveY[1] + dy;
		yVals[2] = saveY[2] + dy;
		yVals[3] = saveY[3] + dy;
		cx = (xVals[0] + xVals[2]) / 2;
		cy = (yVals[0] + yVals[2]) / 2;
	}

	/** 
	 * Scales the rectangle preserving its aspect ratio.
	 * 
	 * @param imp TODO
	 * @param e TODO
	 */
	private void scaleRect(ImagePlus imp, MouseEvent e) {
		ImageCanvas ic = imp.getCanvas();
		int x = ic.offScreenX(e.getX());
		int y = ic.offScreenY(e.getY());
		double growthFactor = Math.sqrt((x - cx) * (x - cx) + (y - cy)
				* (y - cy))
				/ Math.sqrt((x0 - cx) * (x0 - cx) + (y0 - cy) * (y0 - cy));
		transformRect(imp, growthFactor, 0); // just scale
	}

	/**
	 * rotate the rectangle; the angle is calculated depending on the mouse
	 * position
	 */
	private void rotateRect(ImagePlus imp, MouseEvent e) {
		ImageCanvas ic = imp.getCanvas();
		int x = ic.offScreenX(e.getX());
		int y = ic.offScreenY(e.getY());
		double initialAngle = Math.atan((x0 - cx) / (y0 - cy));
		double currentAngle = Math.atan((x - cx) / (y - cy));
		// calculate delta angle
		double dAlpha = currentAngle - initialAngle;
		if ((y >= cy) && (y0 < cy)) {
			dAlpha += Math.PI;
		}
		transformRect(imp, 1, dAlpha); // just rotate
	}

	/**
	 * @param imp
	 * @param scale
	 *            scaling factor
	 * @param r
	 *            rotation factor
	 */
	private void transformRect(ImagePlus imp, double scale, double r) {
		// scaling factor pre transformation
		double c1 = scale * Math.cos(r);
		double s1 = scale * Math.sin(r);
		double c2 = scale * Math.cos(r + Math.PI);
		double s2 = scale * Math.sin(r + Math.PI);

		double[] tempX = new double[4];
		double[] tempY = new double[4];

		// transformation matrix
		tempX[0] = cx + (saveX[0] - cx) * c1 + (saveY[0] - cy) * s1;
		tempY[0] = cy + (saveX[0] - cx) * s2 - (saveY[0] - cy) * c2;
		tempX[1] = cx + (saveX[1] - cx) * c1 + (saveY[1] - cy) * s1;
		tempY[1] = cy + (saveX[1] - cx) * s2 - (saveY[1] - cy) * c2;
		tempX[2] = cx + (saveX[2] - cx) * c1 + (saveY[2] - cy) * s1;
		tempY[2] = cy + (saveX[2] - cx) * s2 - (saveY[2] - cy) * c2;
		tempX[3] = cx + (saveX[3] - cx) * c1 + (saveY[3] - cy) * s1;
		tempY[3] = cy + (saveX[3] - cx) * s2 - (saveY[3] - cy) * c2;

		// avoid that the rectangle can leave the canvas
		int imgW = imp.getWidth();
		int imgH = imp.getHeight();
		for (int i = 0; i < 4; i++) {
			if (tempX[i] >= imgW || tempX[i] < 0 || tempY[i] >= imgH
					|| tempY[i] < 0)
				return;
		}
		xVals = tempX;
		yVals = tempY;
	}

	/** @return angle the ROI is rotated */
	public double getAngle() {
		return angle;
	}

	/** @return the 4 ROI x coordinates */
	public double[] getXVals() {
		return xVals;
	}

	/** @return the 4 ROI y coordinates */
	public double[] getYVals() {
		return yVals;
	}

	/**
	 * @param ang
	 *            rotation factor
	 */
	public void setAngle(double ang) {
		angle = ang;
	}

	/**
	 * @param xCoords
	 *            x coordinates of the ROI
	 * @param yCoords
	 *            y coordinates of the ROI
	 */
	public void setCoords(double[] xCoords, double[] yCoords) {
		xVals = xCoords;
		yVals = yCoords;
	}

	private ImagePlus generateCroppedImagePlus() {
		float[] xRect = new float[xVals.length];
		float[] yRect = new float[xVals.length];

		for (int i = 0; i < xRect.length; i++) {
			xRect[i] = (float) xVals[i];
			yRect[i] = (float) yVals[i];
		}
		
		PolygonRoi boundingRect = new PolygonRoi(new FloatPolygon(xRect, yRect),
			Roi.POLYGON);

		double angle = Math.atan((yVals[3] - yVals[0]) / (xVals[3] - xVals[0])) *
			180 / Math.PI + ((xVals[3] < xVals[0]) ? 180 : 0);

		Line top = new Line(xVals[0], yVals[0], xVals[3], yVals[3]);
		double scaleFactor = panelWidth / top.getRawLength();

		// Create new image
		// TODO Refactor to new method
		ImageProcessor croppedImageProcessor = MyImageMath.getImageProcessor(
			imagePlus, boundingRect, angle, scaleFactor, panelWidth,
			panelHeight, interpolationMethod);
		ImageProcessor ip = new ColorProcessor(croppedImageProcessor.getWidth(),
			croppedImageProcessor.getHeight(), (int[]) croppedImageProcessor
				.getPixels());
		ImagePlus croppedImagePlus = new ImagePlus("", ip);

		Calibration cal = imagePlus.getCalibration();
		cal.pixelWidth = (1 / scaleFactor) * imagePlus.getCalibration().pixelWidth;
		cal.pixelHeight = (1 / scaleFactor) * imagePlus.getCalibration().pixelHeight;
		croppedImagePlus.setCalibration(cal);
		
		return croppedImagePlus;
	}

	@Override
	public void eventOccurred(int eventID) {
		if (eventID == IJEventListener.TOOL_CHANGED) {
				String name = IJ.getToolName();
				
				if (name.equals(this.getToolName())) {
					toolToggled(IJ.getImage());
				}
		}
	}

	@Override
	public void keyTyped(KeyEvent e) { /* NB */ }

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			ImagePlus generatedCroppedImagePlus = generateCroppedImagePlus();
			notifyImageSelected(new ImageSelectionEvent(generatedCroppedImagePlus,
				xVals, yVals, getRecordedChanges()));
		}
	}

	@Override
	public void keyReleased(KeyEvent e) { /* NB */ }

	@Override
	public void leafSelected(LeafEvent e) {
		LeafPanel panel = (LeafPanel) e.getSource();
		updateDimensions(panel.getW(), panel.getH());
		updateCoordinates(panel.getImgData().getSourceX(), panel.getImgData().getSourceY());
	}

	/**
	 * Updates the coordinates of the corners of the rectangular ROI.
	 * 
	 * @param sourceX x coordinates of ROI
	 * @param sourceY y coordinates of ROI
	 */
	private void updateCoordinates(double[] sourceX, double[] sourceY) {
		setCoords(sourceX, sourceY);
	}

	/**
	 * TODO Documention
	 * 
	 * @param width
	 * @param height
	 */
	private void updateDimensions(final int width, final int height) {
		horizontalSize = width;
		verticalSize = height;

		panelWidth = width;
		panelHeight = height;
	}

	@Override
	public void leafDeselected(LeafEvent e) { /* NB */ }

	@Override
	public void leafResized(LeafEvent e) { /* NB */ }

	@Override
	public void leafCleared(LeafEvent e) { /* NB */ }

	@Override
	public void leafRemoved(LeafEvent e) { /* NB */ }

	@Override
	public void leafSplit(LeafEvent e) { 
		LeafPanel panel = (LeafPanel) e.getSource();
		updateDimensions(panel.getW(), panel.getH());
	}

	/**
	 * Executed when {@code this} has been activated / toggled from the ImageJ1
	 * toolbar.
	 * 
	 * @param activeImagePlus the active {@link ImagePlus} at the time of toggling
	 */
	private void toolToggled(ImagePlus activeImagePlus) {
		// TODO Check if there is a history for that image

		init(activeImagePlus);

		// Save filename from which the file has been loaded
		FileInfo fileInfo = activeImagePlus.getOriginalFileInfo();

		// If no directory is available, we don't really have to record the history
		if (fileInfo.directory != null) {
			// Start recording changes
			recordChanges();
		}

		drawRect(activeImagePlus);
	}

	private void recordChanges() {
		recorder = new Recorder(false);
	}

	protected synchronized void notifyImageSelected(ImageSelectionEvent event) {
		for (ImageSelectionListener l : listeners.getListeners(ImageSelectionListener.class))
			l.imageSelected(event);
	}
	
	public void addImageSelectionListener(ImageSelectionListener listener) {
		listeners.add(ImageSelectionListener.class, listener);
	}

	public void removeImageSelectionListener(ImageSelectionListener listener) {
		listeners.remove(ImageSelectionListener.class, listener);
	}
}