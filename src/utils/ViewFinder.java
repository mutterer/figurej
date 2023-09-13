package utils;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.RotatedRectRoi;
import ij.plugin.frame.RoiManager;
import ij.plugin.tool.PlugInTool;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.net.URL;

import javax.swing.ImageIcon;

import figure.LeafPanel;

/*
 * @author Edda Zinck
 * @author Jerome Mutterer
 * (c) IBMP-CNRS
 * date: April 2012 
 *
 * This class realizes a new ImageJ tool as plugin that allows to draw a rectangular region of interest on a overlay.
 * dragging, scaling and rotating the ROI does not change its sides aspect ratio.
 *
 */

public class ViewFinder extends PlugInTool {

	// the rectangle
	private Roi rectRoi;

	// the imageId it is supposed to work on
	private int workingImageID;

	public void setWorkingImageID(int id) {
		this.workingImageID = id;
	}

	// start size
	private double horizSize; // just used in initialization
	private double verticSize;

	// angle the ROI is rotated relative to its initial position
	private double angle = 0.;

	// grab distance for the rotation handles (small red lines)
	private int grabDistance = 20;
	private int x0, y0;
	private double cx, cy, xp, yp;

	// current ROI edge coordinates
	private static double[] xVals = new double[4];
	private static double[] yVals = new double[4];
	// 0-------3 edge indices
	// | |
	// 1-------2

	// ROI coordinates in the last position
	private double[] saveX = new double[4];
	private double[] saveY = new double[4];

	// click position indicators
	private boolean inside = false;
	private boolean cornerClick = false;
	private boolean handeClick = false;
	private String fov;

	public ViewFinder() {
		horizSize = 150.0;
		verticSize = 100.0;
	}

	/**
	 * @param prefW
	 *            preferred width of the ROI
	 * @param prefH
	 *            preferred height of the ROI if w or h are too large for the
	 *            opened image they get resized, but the aspect ratio is
	 *            preserved
	 */
	public ViewFinder(LeafPanel p) {
		horizSize = p.getW();
		verticSize = p.getH();
	}

	public void init(ImagePlus imp) {
		setWorkingImageID(imp.getID());
		// set size of the ROI
		double w;
		double h;
		w = imp.getWidth();
		h = imp.getHeight();
		fov ="";

		if ((w == horizSize) && (h == verticSize)) {

			xVals[0] = 0;
			xVals[1] = 0;
			xVals[2] = horizSize;
			xVals[3] = horizSize;

			yVals[0] = 0;
			yVals[1] = verticSize;
			yVals[2] = verticSize;
			yVals[3] = 0;

		} else {
			// as long as the size of the ROI is too large, make it smaller
			while (horizSize >= w || verticSize >= h) {
				horizSize /= 1.3;
				verticSize /= 1.3;
			}

			// calculate a centered position
			double xOffset = w / 2 - horizSize / 2;
			double yOffset = h / 2 - verticSize / 2;
			xVals[0] = xOffset;
			xVals[1] = xOffset;
			xVals[2] = xOffset + horizSize;
			xVals[3] = xOffset + horizSize;

			yVals[0] = yOffset;
			yVals[1] = yOffset + verticSize;
			yVals[2] = yOffset + verticSize;
			yVals[3] = yOffset;
		}
		cx = (xVals[0] + xVals[2]) / 2;
		cy = (yVals[0] + yVals[2]) / 2;

		// remember this position
		saveX = xVals.clone();
		saveY = yVals.clone();

	}

	/**
	 * check whether the user clicked near an edge or separator or outside of
	 * the ROI
	 */
	public void mousePressed(ImagePlus imp, MouseEvent e) {

		e.consume();
		ImageCanvas ic = imp.getCanvas();
		int x = ic.offScreenX(e.getX());
		int y = ic.offScreenY(e.getY());
		x0 = x;
		y0 = y;

		// assert rect already exists
		if (rectRoi == null) {
			init(imp);
			drawRect(imp);
		}

		// check if clicked inside rectangle
		inside = rectRoi.contains(x, y);
		// check if clicked within grab distance of a corner
		cornerClick = false;
		for (int i = 0; i < xVals.length; i++) {
			double d = Math.sqrt((x - xVals[i]) * (x - xVals[i]) + (y - yVals[i]) * (y - yVals[i]));
			if (d < grabDistance) {
				cornerClick = true;
			}
		}
		// check if clicked within grab distance of median line and outside
		handeClick = false;
		for (int i = 0; i < 4; i++) {
			double d0 = Math.sqrt((x - xVals[i]) * (x - xVals[i]) + (y - yVals[i]) * (y - yVals[i]));
			double d1 = Math.sqrt((x - xVals[(i + 1) % 4]) * (x - xVals[(i + 1) % 4])
					+ (y - yVals[(i + 1) % 4]) * (y - yVals[(i + 1) % 4]));
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
	}

	public void mouseMoved(ImagePlus imp, MouseEvent e) {
		ImageCanvas ic = imp.getCanvas();
		int x = ic.offScreenX(e.getX());
		int y = ic.offScreenY(e.getY());
		e.consume();
		x0 = x;
		y0 = y;
		// assert rect already exists
		if (rectRoi != null) {

			// check if clicked inside rectangle
			boolean inside = rectRoi.contains(x, y);
			// check if clicked within grab distance of a corner
			boolean cornerClick = false;
			for (int i = 0; i < xVals.length; i++) {
				double d = Math.sqrt((x - xVals[i]) * (x - xVals[i]) + (y - yVals[i]) * (y - yVals[i]));
				if (d < grabDistance) {
					cornerClick = true;
				}
			}
			// check if clicked within grab distance of median line and outside
			boolean handeClick = false;
			for (int i = 0; i < 4; i++) {
				double d0 = Math.sqrt((x - xVals[i]) * (x - xVals[i]) + (y - yVals[i]) * (y - yVals[i]));
				double d1 = Math.sqrt((x - xVals[(i + 1) % 4]) * (x - xVals[(i + 1) % 4])
						+ (y - yVals[(i + 1) % 4]) * (y - yVals[(i + 1) % 4]));
				if (Math.abs(d1 - d0) < grabDistance) {
					handeClick = !inside;
				}
			}

			saveX = xVals.clone();
			saveY = yVals.clone();

			if (cornerClick) {
				switchCursor("Expand.png");
			} else if (handeClick) {
				switchCursor("Refresh.png");
			} else if (inside) {
				switchCursor("Move.png");
			} else
				switchCursor("reset");

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
				// IJ.log("img null");
				return;
			} else {
				int width = icon.getIconWidth();
				int height = icon.getIconHeight();
				Point hotSpot = new Point(width / 2, height / 2);
				Cursor crosshairCursor = toolkit.createCustomCursor(image, hotSpot, name);
				ImageCanvas.setCursor(crosshairCursor, 0);
			}
		}
	}

	/**
	 * depending on where the user clicked the ROI is scaled, rotated or moved
	 */
	public void mouseDragged(ImagePlus imp, MouseEvent e) {
		if (cornerClick) {
			scaleRect(imp, e);
		} else if (handeClick) {
			rotateRect(imp, e);
		} else if (inside) {
			moveRect(imp, e);
		}
		e.consume();
		drawRect(imp);
	}

	/** remember the coordinates of the current position */
	public void mouseReleased(ImagePlus imp, MouseEvent e) {
		ImageCanvas ic = imp.getCanvas();
		int x = ic.offScreenX(e.getX());
		int y = ic.offScreenY(e.getY());
		x0 = x;
		y0 = y;
		saveX = xVals.clone();
		saveY = yVals.clone();
		e.consume();
	}

	/** define the message shown when the tool symbol is double clicked */
	public void showOptionsDialog() {
		// not used
	}

	/** define the icon the IJ tool gets in the tool bar */
	public String getToolIcon() {
		return "C000"; // not used
	}

	/**
	 * draw a region of interest (ROI) on an overlay using the rect coordinates
	 */
	public void drawRect(ImagePlus imp) {

		if (workingImageID == imp.getID()) {
						
			Overlay overLay = new Overlay();
			// setup floats arrays from rect to add as polygon ROI
			float[] px = new float[xVals.length];
			float[] py = new float[xVals.length];

			for (int i = 0; i < 4; i++) {
				px[i] = (float) xVals[i];
				py[i] = (float) yVals[i];
			}

			// create ROI and its layout
			rectRoi = new PolygonRoi(px, py, 4, Roi.POLYLINE);
			rectRoi.setStrokeWidth(2 / IJ.getImage().getCanvas().getMagnification());
			rectRoi.setStrokeColor(new Color(255, 0, 0));
			overLay.add(rectRoi);
			drawHandle(overLay, xVals[1], yVals[1], Color.white);
			drawHandle(overLay, xVals[2], yVals[2], Color.white);

			// add handles
			for (int i = 0; i < 4; i++) {
				xp = (xVals[i] + xVals[(i + 1) % 4]) / 2;
				yp = (yVals[i] + yVals[(i + 1) % 4]) / 2;
				angle = Math.atan((xVals[(i + 1) % 4] - xVals[i]) / (yVals[(i + 1) % 4] - yVals[i]));
				double sign = (rectRoi.contains((int) (xp + 2 * Math.cos(angle)), (int) (yp - 2 * Math.sin(angle))))
						? -1 : 1;
				Line axis = new Line(xp, yp, xp + 15 * Math.cos(angle) * sign, yp - 15 * Math.sin(angle) * sign);
				axis.setStrokeColor(new Color(255, 0, 0));
				axis.setStrokeWidth(1 / IJ.getImage().getCanvas().getMagnification());
				overLay.add(axis);
				drawHandle(overLay, axis.x2d, axis.y2d, Color.green);
			}
			// add top line in a different color to visualize top
			Line top = new Line(xVals[0], yVals[0], xVals[3], yVals[3]);
			top.setStrokeColor(new Color(0, 0, 255));
			top.setStrokeWidth(2 / IJ.getImage().getCanvas().getMagnification());
			overLay.add(top);
			drawHandle(overLay, top.x1d, top.y1d, Color.white);
			drawHandle(overLay, top.x2d, top.y2d, Color.white);
			overLay.selectable(false);
			imp.setOverlay(overLay);
			double w = imp.getCalibration().pixelWidth * Math.sqrt(
					(xVals[0] - xVals[3]) * (xVals[0] - xVals[3]) + (yVals[0] - yVals[3]) * (yVals[0] - yVals[3]));
			double h = imp.getCalibration().pixelWidth * Math.sqrt(
					(xVals[0] - xVals[1]) * (xVals[0] - xVals[1]) + (yVals[0] - yVals[1]) * (yVals[0] - yVals[1]));
			IJ.showStatus("(" + IJ.d2s(w, 3) + " , " + IJ.d2s(h, 3) + ")");
			fov = "(width:" + IJ.d2s(w, 2) + " , height:" + IJ.d2s(h, 2) + ")";

		}
	}
	public static void toRoiManager() {
		
		// create ROI and its layout
		Roi r = new RotatedRectRoi( (xVals[0]+xVals[1])/2,(yVals[0]+yVals[1])/2, 
				(xVals[2]+xVals[3])/2,(yVals[3]+yVals[2])/2, 
				Math.sqrt((xVals[2] - xVals[3]) * (xVals[2] - xVals[3]) + (yVals[2] - yVals[3]) * (yVals[2] - yVals[3]))
				); 
		r.setStrokeWidth(2 / IJ.getImage().getCanvas().getMagnification());
		r.setStrokeColor(new Color(255, 255, 255));
		r.setHandleSize(0);
		Frame frame = WindowManager.getFrame("ROI Manager");
		if (frame==null)
			IJ.run("ROI Manager...");
		frame = WindowManager.getFrame("ROI Manager");
		if (frame==null || !(frame instanceof RoiManager))
			IJ.error("ROI Manager not found");
		RoiManager rm = (RoiManager)frame;
		rm.addRoi(r);

	}
	private void drawHandle(Overlay overLay, double x2d, double y2d, Color color) {
		// TODO Auto-generated method stub
		double zoomFactor = IJ.getImage().getCanvas().getMagnification();
		double size = 8.0d / zoomFactor;
		Roi r = new OvalRoi(x2d - size / 2, y2d - size / 2, size, size);
		r.setFillColor(Color.black);
		overLay.add(r);
		size = 6.0d / zoomFactor;
		Roi r2 = new OvalRoi(x2d - size / 2, y2d - size / 2, size, size);
		r2.setFillColor(color);
		overLay.add(r2);
	}

	/**
	 * use a default size ROI and rotate it about the given angle
	 * 
	 * @param angle
	 *            rotation angle in degree
	 */
	public void init(ImagePlus imp, double angle) {
		init(imp);
		angle = angle * Math.PI / 180;
		transformRect(imp, 1, angle);
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

	/** scale the rectangle preserving the aspect ratio */
	private void scaleRect(ImagePlus imp, MouseEvent e) {
		ImageCanvas ic = imp.getCanvas();
		int x = ic.offScreenX(e.getX());
		int y = ic.offScreenY(e.getY());
		double growthFactor = Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy))
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
			if (tempX[i] >= imgW || tempX[i] < 0 || tempY[i] >= imgH || tempY[i] < 0)
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

	public String getFov() {
		return fov;
	}
}