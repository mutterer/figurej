package figure;
/*
 * @author Edda Zinck
 * @author Jerome Mutterer
 * (c) IBMP-CNRS   
 * 
 */
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.Prefs;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.RotatedRectRoi;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.plugin.frame.RoiManager;
import ij.process.ColorProcessor;
import loci.formats.FormatException;
import loci.plugins.BF;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;
import loci.plugins.in.ImporterPrompter;
import utils.DataSource;

public class FigureWindow extends ImagePlus implements Serializable , DropTargetListener {

	private static final long serialVersionUID = 1L;

	// defaults 
	private double dpi = 300;
	private int figureWidth = 512;
	private int figureHeight = 512;
	private  Panel rootPanel;
	private  Panel selectedPanel = null;
	private  Panel panelMouseWasReleasedOn = null;

	private Point mousePressedPoint = new Point();
	private Point mouseReleasedPoint = new Point();

	public  Panel getRootPanel() {
		return rootPanel;
	}

	private transient ImagePlus figure;
	
	public ImagePlus getResultFigure() {
		return figure;
	}

	public void setResultFigure(ImagePlus imp) {
		this.figure = imp;
	}


	public Point getMousePressedPoint() {
		return mousePressedPoint;
	}
	
	public Point getMouseReleasedPoint() {
		return mouseReleasedPoint;
	}

	// border width around separators wherein clicks are treated as clicks on
	// separators (important for tiny or invisible separators)
	private int clickTolerance = 10;
	private boolean quitWithoutSaving = false;

	private static String droppedFilePath ="";

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
	public FigureWindow(int w, int h, int frameX, int frameY, double resolution, int separatorSize) {
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


		ColorProcessor cp = new ColorProcessor(figureWidth, figureHeight);
		figure = new ImagePlus("FigureJ", cp);

		// start to build the panel tree structure
		rootPanel = new ContainerPanel(0, 0, figureWidth, figureHeight);
		Panel p = new LeafPanel(0, 0, figureWidth, figureHeight);
		selectedPanel = p;
		rootPanel.addChild(p);
		rootPanel.setSeparatorWidth(sepSize);
		rootPanel.draw(figure);
		figure.setProp("xmargin", "10");  // I can do better.
		figure.setProp("ymargin", "10");

		ImageWindow.centerNextImage(); // hides the image info bar
		figure.show();

		ImageWindow window = figure.getWindow();
		window.setLocation(frameX, frameY);
		window.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				figure.changes = false;
			}
		});

		Prefs.set("figure.id", figure.getID());

		highlightSelectedPanel();
		figure.deleteRoi();
		new DropTarget(window.getCanvas(), this);

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
		ImageWindow.centerNextImage(); 
		IJ.newImage("FigureJ", "RGB", figureWidth, figureHeight, 1);
		figure = IJ.getImage();
		figure.setProcessor(new ColorProcessor(figureWidth, figureHeight));
		Prefs.set("figure.id", figure.getID());
		if (figure.getOverlay() == null)
			figure.setOverlay(new Overlay());
		rootPanel.draw(figure);
		highlightSelectedPanel();
	}

	/** @return result figure */
	public ImagePlus getImagePlus() {
		return figure;
	}

	/**
	 * @param fileName
	 *            saves the file as png
	 */
	public void saveImage(String path, String fileName) {
		System.out.println("saving " + fileName);
		hideROI();
		new FileSaver(figure.flatten()).saveAsTiff(path + "FullResolution_" + fileName + ".tif");
		new FileSaver(figure.flatten()).saveAsJpeg(path + "JpegCompressed_" + fileName + ".jpg");
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
		highlightSelectedPanel();

	}

	/**
	 * @param imp
	 * @param e
	 *            check which panel was clicked
	 */
	public void mousePressed(ImagePlus imp, MouseEvent e) {
		ImageCanvas canv = imp.getCanvas();
		Panel clickedPanel = rootPanel.getClicked(canv.offScreenX(e.getX()), canv.offScreenY(e.getY()),
				getSeparatorPanelClickTolerance());

		if (clickedPanel != null) {
			selectedPanel = clickedPanel;
			highlightSelectedPanel();
		}
		mousePressedPoint.x = canv.offScreenX(e.getX());
		mousePressedPoint.y = canv.offScreenY(e.getY());

	}

	public void mouseMoved(ImagePlus imp, MouseEvent e) {
		ImageCanvas canv = imp.getCanvas();
		Panel hoveredPanel = rootPanel.getClicked(canv.offScreenX(e.getX()), canv.offScreenY(e.getY()),
				getSeparatorPanelClickTolerance());
		if (hoveredPanel != null) {
			if (hoveredPanel.getClass().getName().contains(LeafPanel.class.getName())) {
				switchCursor("reset");
				if (IJ.isMacintosh())
					IJ.showStatus("Double click to associate or open image");
			} else if (hoveredPanel.getClass().getName().contains(SeparatorPanel.class.getName())) {
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
	 * set that panel as active that fits to the coordinates of the last click
	 * 
	 * @param ignoreTolerance
	 *            if true the coordinates are used unchanged - nullPointer save.
	 *            else a tolerance value is subtracted from leaves and added to
	 *            separators so that tiny separators can get clicked.
	 */
	public void updateSelectedPanel(boolean ignoreTolerance) {
		if (ignoreTolerance)
			selectedPanel = rootPanel.getClicked(mousePressedPoint.x, mousePressedPoint.y, 0);
		else
			selectedPanel = rootPanel.getClicked(mousePressedPoint.x, mousePressedPoint.y,
					getSeparatorPanelClickTolerance());
	}

	public void mouseReleased(ImagePlus imp, MouseEvent e) {
		ImageCanvas canv = imp.getCanvas();
		panelMouseWasReleasedOn = rootPanel.getClicked(canv.offScreenX(e.getX()), canv.offScreenY(e.getY()),
				getSeparatorPanelClickTolerance());

		// this happens when user swaps two panels.
		if ((panelMouseWasReleasedOn != null) && (panelMouseWasReleasedOn != selectedPanel)&&!(panelMouseWasReleasedOn instanceof SeparatorPanel)) {

			figure.setRoi(selectedPanel.getRectangle());
			ImagePlus source1 = figure.crop();
			
			figure.setRoi(panelMouseWasReleasedOn.getRectangle());
			ImagePlus source2 = figure.crop();

			DataSource temp = ((LeafPanel) panelMouseWasReleasedOn).getImgData();
			
			((LeafPanel) panelMouseWasReleasedOn).setImgData(((LeafPanel) selectedPanel).getImgData());
			((LeafPanel) selectedPanel).setImgData(temp);

			((LeafPanel) selectedPanel).getImgData().setCoords(null, null);
			((LeafPanel) panelMouseWasReleasedOn).getImgData().setCoords(null, null);
			////
			int w = source1.getWidth();
			int h = source1.getHeight();
			double roiWidth = panelMouseWasReleasedOn.getW();
			double roiHeight = panelMouseWasReleasedOn.getH();
			while ((roiWidth >= w) || (roiHeight >= h)) {
				roiWidth /= 1.3;
				roiHeight /= 1.3;
			}
			source1.setRoi(new RotatedRectRoi((w - roiWidth) / 2, h / 2, (w + roiWidth) / 2, h / 2, roiHeight));
			ImagePlus imp2 = source1.crop();
			imp2 = imp2.resize(panelMouseWasReleasedOn.getW(), panelMouseWasReleasedOn.getH(), "bilinear");
			imp2.copy(false);
			figure.setRoi(panelMouseWasReleasedOn.getRectangle());
			IJ.run(figure, "Paste", "");
			((LeafPanel) panelMouseWasReleasedOn).setPixels(figure);
			
			////
			w = source2.getWidth();
			h = source2.getHeight();
			roiWidth = selectedPanel.getW();
			roiHeight = selectedPanel.getH();
			while ((roiWidth >= w) || (roiHeight >= h)) {
				roiWidth /= 1.01;
				roiHeight /= 1.01;
			}
			source2.setRoi(new RotatedRectRoi((w - roiWidth) / 2, h / 2, (w + roiWidth) / 2, h / 2, roiHeight));
			imp2 = source2.crop();
			imp2 = imp2.resize(selectedPanel.getW(), selectedPanel.getH(), "bilinear");
			imp2.copy(false);
			figure.setRoi(selectedPanel.getRectangle());
			IJ.run(figure, "Paste", "");
			((LeafPanel) selectedPanel).setPixels(figure);
			///
			highlightSelectedPanel();
		}
		mouseReleasedPoint.x = canv.offScreenX(e.getX());
		mouseReleasedPoint.y = canv.offScreenY(e.getY());
		highlightSelectedPanel();
	}

	/**
	 * @return distance which is added to separators so that they can get
	 *         clicked also if they are very thin depends on zoom factor and
	 *         result image size
	 */
	private int getSeparatorPanelClickTolerance() {
		return (int) (clickTolerance / figure.getCanvas().getMagnification());
	}

	/** highlight the selected panel */
	public void highlightSelectedPanel() {
		figure.setRoi(selectedPanel.getHighlightROI());
		figure.updateImage();
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
		ImageCanvas canv = imp.getCanvas();
		int x1 = canv.offScreenX(e.getX());
		int y1 = canv.offScreenY(e.getY());
		figure.killRoi();
		if (selectedPanel.getClass().getName().contains(SeparatorPanel.class.getName())) {

			int closestX = rootPanel.getClosestX(selectedPanel, Integer.MAX_VALUE);
			int closestY = rootPanel.getClosestY(selectedPanel, Integer.MAX_VALUE);

			if (Math.abs(closestX - x1) < Panel.snapDist)
				x1 = closestX;
			if (Math.abs(closestY - y1) < Panel.snapDist)
				y1 = closestY;

			selectedPanel.getParent().reShape(x1, y1, (SeparatorPanel) selectedPanel);

			// log the separator's parents panels dimensions to the status bar.
			String dimensions = "";
			for (Panel p : selectedPanel.getParent().getChildren()) {
				if (!p.getClass().getName().contains("Sep"))
					dimensions += IJ.d2s(p.getW() * imp.getCalibration().pixelWidth, 2) + "x"
							+ IJ.d2s(p.getH() * imp.getCalibration().pixelHeight, 2) + " ";
			}
			IJ.showStatus(dimensions + imp.getCalibration().getUnit());

		} else {

			int x0 = selectedPanel.getX()+selectedPanel.getW()/2;
			int y0 = selectedPanel.getY()+selectedPanel.getH()/2;

			 double d = Math.sqrt((y1-y0)*(y1-y0)+(x1-x0)*(x1-x0));
			  double step=3;
			    double r=15;
			    float[] xa = new float[(int)Math.floor(d/step)];
			    float[] ya = new float[xa.length];
			    for (int i=0;i<xa.length;i++) {
			        double j=i*step;
			        xa[i]=(float) (x0+j*(x1-x0)/d+Math.sin(j/7)*r);
			        ya[i]=(float) (y0+j*(y1-y0)/d+Math.cos(j/7)*r);
			    }
			    if (xa.length>1) {
			        xa[0]=x0;
			        ya[0]=y0;
			        xa[xa.length-1]=x1;
			        ya[ya.length-1]=y1;
			    }
			
			
			PolygonRoi spring = new PolygonRoi(xa, ya, Roi.FREELINE);
			spring.setUnscalableStrokeWidth(3);
			spring.setStrokeColor(Color.getHSBColor(((float)(System.currentTimeMillis()%1000))/1000, 0.8f, 1));
			figure.setRoi(spring);
		}

		figure.setProperty("Info", "");
		figure.setProp("panels", "\n");

		rootPanel.draw(figure);
	}

	/**
	 * load the pixels of every panel to the result image; pass these changed
	 * pixels to the processor and redraw the result image
	 */
	@Override
	public void draw() {
		figure.changes = true;
		figure.setProperty("Info", "");
		figure.setProp("panels", "\n");

		rootPanel.draw(figure);
		highlightSelectedPanel();
	}

	// unused atm
	/**
	 * store the pixels of the result image in the pixel arrays of the fitting
	 * leaf panels makes sense in cases where the result image is changed by
	 * native IJ methods or other plugins
	 */
	public void adoptExternalChanges() {
		rootPanel.setPixels(figure);
	}

	/**
	 * @param resolution
	 * @param unit
	 */
	public void calibrateImage(double resolution, String unit) {
		Calibration c = figure.getCalibration();
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
		figure.setCalibration(c);
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
		figure.deleteRoi();
	}

	public void hideAllLabelsAndScalebars() {
		rootPanel.hideLabel(figure.getOverlay());
		rootPanel.hideScalebar(figure.getOverlay());
	}

	public boolean getQuitWithoutSaving() {
		return quitWithoutSaving;
	}

	public void setQuitWithoutSaving(boolean quitWithoutSaving) {
		this.quitWithoutSaving = quitWithoutSaving;
	}

	public void setCal(Calibration c) {
		figure.setCalibration(c);
	}

	private  ImagePlus openFirstTime(String path) {
		ImagePlus openedImage = new ImagePlus();
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

				((LeafPanel) selectedPanel).getImgData().setSelectedSerie(selectedSerie);

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

		return openedImage;
	}

	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
		// TODO Auto-generated method stub
	}

	@Override
	public void dragOver(DropTargetDragEvent dtde) {
		// TODO Auto-generated method stub
		int dx = dtde.getLocation().x;
		int dy = dtde.getLocation().y;
		double mag = figure.getCanvas().getMagnification();
		Panel hoveredPanel = rootPanel.getClicked((int) Math.floor(dx / mag), (int) Math.floor(dy / mag),
				getSeparatorPanelClickTolerance());
		if ((hoveredPanel != null) && !(hoveredPanel instanceof SeparatorPanel)) {
			selectedPanel = hoveredPanel;
			highlightSelectedPanel();
		}
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde) {
	}

	@Override
	public void dragExit(DropTargetEvent dte) {
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void drop(DropTargetDropEvent dtde) {
		dtde.acceptDrop(DnDConstants.ACTION_COPY);
		DataFlavor[] flavors = null;
		try {
			Transferable t = dtde.getTransferable();
			droppedFilePath = null;
			flavors = t.getTransferDataFlavors();
			for (int i = 0; i < flavors.length; i++) {
				if (flavors[i].isFlavorJavaFileListType()) {
					Object data = t.getTransferData(DataFlavor.javaFileListFlavor);
					List l = ((java.util.List) data);
					droppedFilePath = l.get(0).toString();
					break;
				}
			}
			if (droppedFilePath != null) {
//				Thread thread = new Thread(this, "FigureJDND_");
//				thread.setPriority(Math.max(thread.getPriority() - 1, Thread.MIN_PRIORITY));
//				thread.start();
				String path = this.droppedFilePath;
				try {
					File f = new File(path);
					((LeafPanel) selectedPanel).getImgData().setFileDirectory(f.getParent());
					((LeafPanel) selectedPanel).getImgData().setFileName(f.getName());
					((LeafPanel) selectedPanel).getImgData().setExternalSource("");

					ImagePlus imp = openFirstTime(f.getCanonicalPath());

					Calibration cal = imp.getCalibration();
					
					int w = imp.getWidth();
					int h = imp.getHeight();
					double roiWidth = selectedPanel.getW();
					double roiHeight = selectedPanel.getH();

					while ((roiWidth >= w) || (roiHeight >= h)) {
						roiWidth /= 1.3;
						roiHeight /= 1.3;
					}

					imp.setRoi(new RotatedRectRoi((w - roiWidth) / 2, imp.getHeight() / 2, (w + roiWidth) / 2,
							imp.getHeight() / 2, roiHeight));
					ImagePlus imp2 = imp.crop();
					imp2 = imp2.resize(selectedPanel.getW(), selectedPanel.getH(), "bilinear");
					imp2.copy(false);
					IJ.run(figure, "Paste", "");
					((LeafPanel) selectedPanel).setPixels(figure);
					((LeafPanel) selectedPanel).getImgData().setPixelCalibration(cal.pixelWidth, cal.getUnit());
				} catch (Throwable e) {
					if (!Macro.MACRO_CANCELED.equals(e.getMessage()))
						IJ.handleException(e);
				}
			}
		} catch (Exception e) {
			dtde.dropComplete(false);
			return;
		}
		dtde.dropComplete(true);
		if (flavors == null || flavors.length == 0)
			IJ.error("First drag and drop ignored\nPlease try again.");
	}


	public static String getDroppedFilePath() {
		return droppedFilePath;
	}

	public static void setDroppedFilePath(String droppedFilePath) {
		FigureWindow.droppedFilePath = droppedFilePath;
	}
}
