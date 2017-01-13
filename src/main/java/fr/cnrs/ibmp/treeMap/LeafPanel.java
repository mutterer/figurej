package fr.cnrs.ibmp.treeMap;

import java.awt.Color;
import java.awt.Font;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import fr.cnrs.ibmp.dataSets.DataSource;
import fr.cnrs.ibmp.labels.LabelPosition;
import fr.cnrs.ibmp.windows.MainController;
import fr.cnrs.ibmp.windows.MainWindow;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.gui.Toolbar;
import ij.plugin.Colors;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/**
 * Panel that shows an image and contains a link to a {@link DataSource}. It cannot have any children. 
 * <p>
 * (c) IBMP-CNRS 
 * </p>
 * @author Edda Zinck
 * @author Jerome Mutterer
 */
public class LeafPanel extends Panel {

	private static final long serialVersionUID = 1L;

	private DataSource imgData;
	private static int colorValue = 0x99aabb;
	private int maxW;
	private int maxH;

	// TODO We might be able to remove this state variable
	private boolean hasImg = false;

	private int[] myPanelPixels;

	// label stuff
	private transient TextRoi label;
	private String labelText = "";
	private int xLabelOffset = 10;
	private int yLabelOffset = 10;
	private Color labelColor = new Color(0, 0, 0);
	private Font labelFont = null;
	private LabelPosition labelPos = LabelPosition.TopLeft;
	private transient ImageProcessor ip = new ColorProcessor(10, 10);

	/**
	 * State variable that is used to denote a hidden label for which
	 * {@code hasLabel && !overlay.contains(label)} holds.
	 */
	private boolean hasLabel = false;

	// scale bar stuff
	private transient Roi scalebar;
	private transient Roi scalebarText;
	private int xScaleOffset = this.getW() - 20;
	private int yScaleOffset = this.getH() - 20;
	private int scaleBarHeight = separatorWidth;
	private double scaleBarWidth = 1;
	private Color scalebarColor;

	/**
	 * State variable that is used to denote a hidden scalebar for which
	 * {@code hasScalebar && !overlay.contains(scalebar)} holds.
	 */
	private boolean hasScalebar = false;

	// TODO What is this variable doing?
	private boolean scalebarVisible = false;
	private boolean scalebarTextVisible = false;
	private Font scaleBarTextFont = null;
	private int scalebarLabelJustification = -1;

	/**
	 * Creates a {@link LeafPanel} at the provided coordinates.
	 * 
	 * @param xPos
	 * @param yPos
	 * @param w
	 * @param h
	 */
	public LeafPanel(int xPos, int yPos, int w, int h) {
		super(xPos, yPos, w, h);
		maxW = w;
		maxH = h;
		updatePixelArray();

		if (scaleBarHeight == 0) {
			scaleBarHeight = 5;
		}
	}

	@Override
	public void draw(ImagePlus resImg) {
		if (hasLabel) {
			moveLabel(resImg.getOverlay());
		}

		if (hasScalebar) {
			moveScalebar(resImg);
		}

		ImageProcessor imp = resImg.getProcessor();
		int figHeight = resImg.getHeight();
		int figWidth = resImg.getWidth();
		int[] pixels = (int[]) imp.getPixels();
		for (int imgY = yPos; imgY < Math.min(figHeight, yPos + panelHeight); imgY++)
			for (int imgX = xPos; imgX < Math.min(figWidth, xPos + panelWidth); imgX++) {
				// if an image is scaled to a smaller size, its pixel array is
				// not updated (cache ;)),
				// only the sector drawn shrinks. pixels are only re-read if the
				// image becomes larger
				// (disabled atm because of heap space problem when dealing with
				// large images)
				pixels[imgY * figWidth + imgX] = myPanelPixels[(imgY - yPos)
						* maxW + imgX - xPos];
			}
		imp.setPixels(pixels);
		resImg.setProcessor(imp);
		updateMetadata();
	}

	/**
	 * Writes information about {@link LeafPanel} into the free-text property tag
	 * of {@link MainWindow} (which is an {@link ImagePlus}.
	 */
	private void updateMetadata() {
		String cal = "1";
		String unit = "pixel";

		if (imgData != null) {
			cal = "" + imgData.getPixelWidth();
			unit = "" + imgData.getUnit();
		}

		String panelRoi = "" + xPos + "," + yPos + "," + panelWidth + ","
				+ panelHeight + "," + cal + "," + unit;

		if (imgData != null) {
			panelRoi += "," + imgData.getStringRepresentation();
		}

		ImagePlus imp = WindowManager.getImage((int) Prefs.get("figure.id", 0));
		if (imp != null) {
			String metadata = (String) imp.getProperty("Info");
			imp.setProperty("Info", metadata + "\n" + panelRoi);
		}
	}

	/**
	 * TODO Documentation
	 */
	protected void updatePixelArray() {
		if (hasImg) {
			expandPixelArray();
		} else {
			getNewArrayPixels();
		}
	}

	/**
	 * Defines the pixel values for each panel
	 */
	private void getNewArrayPixels() {
		myPanelPixels = new int[getW() * getH()];
		Arrays.fill(myPanelPixels, colorValue);
		maxW = getW();
		maxH = getH();
	}

	/**
	 * Re-fills the panel with its original color.
	 */
	public void eraseImage() {
		hasImg = false;
		Arrays.fill(myPanelPixels, colorValue);
	}

	/**
	 * creates a new pixel array, transfers the old data to the center (former:
	 * upper left - commented out) and fills the rest with default color
	 */
	private void expandPixelArray() {
		int[] myTempPixels = new int[panelWidth * panelHeight];
		Arrays.fill(myTempPixels, colorValue);
		int offSetY = panelHeight / 2 - maxH / 2;
		int offSetX = panelWidth / 2 - maxW / 2;
		for (int y = 0; y < panelHeight; y++) {
			for (int x = 0; x < panelWidth; x++) {
				if (x >= offSetX && x < offSetX + maxW && y >= offSetY
						&& y < offSetY + maxH) {
					myTempPixels[y * panelWidth + x] = myPanelPixels[(y - offSetY)
							* maxW + (x - offSetX)];
				}
				// if(maxW > x && maxH > y)
				// myTempPixels[y*panelWidth + x] = myPanelPixels[y*maxW + x];
				// else
				// myTempPixels[y*panelWidth + x] = colorValue;
			}
		}
		myPanelPixels = myTempPixels;
		maxW = getW();
		maxH = getH();
	}

	@Override
	public void setPixels(ImagePlus image) {
		hasImg = true;
		if (image.getProcessor() == null) {
			image.setProcessor(IJ.getImage().getProcessor());
		}
		// TODO is null after opening a new image
		System.out.println(image.getProcessor() != null);
		image.setRoi(new Roi(getX(), getY(), getW(), getH()));
		myPanelPixels = (int[]) image.getProcessor().crop().getPixels();
		maxW = getW();
		maxH = getH();
	}

	/**
	 * TODO Documentation
	 * 
	 * @param image
	 */
	public void setPanelPixels(ImagePlus image) {
		hasImg = true;
		if (image.getProcessor() == null) {
			image.setProcessor(IJ.getImage().getProcessor());
		}
		// TODO is null after opening a new image
		System.out.println(image.getProcessor() != null);
		// image.setRoi(new Roi(getX(), getY(), getW(), getH()));
		myPanelPixels = (int[]) image.getProcessor().getPixels();
		maxW = getW();
		maxH = getH();
	}

	/**
	 * TODO Documentation
	 * 
	 * @param pixels
	 */
	public void setPixels(int[] pixels) {
		if (pixels.length == panelHeight * panelWidth) {
			maxW = panelWidth;
			maxH = panelHeight;
			myPanelPixels = pixels;
			hasImg = true;
		} else {
			System.out.println("array length does not fit");
		}
	}

	@Override
	public void setW(int w) // throws SideLengthTooSmallException {
	{
		removeROICoords();
		if (w < minLeafSideLength) {
			System.out
					.println("STH WENT WRONG!!! (method: leaf setW)   tell it edda please!");
			// throw new SideLengthTooSmallException();
		}

		this.panelWidth = w;
		// if (w>maxW)
		{
			updatePixelArray();
			maxW = w;
		}
	}

	@Override
	public void setH(int h) // throws SideLengthTooSmallException {
	{
		removeROICoords();
		if (h < minLeafSideLength) {
			System.out
					.println("STH WENT WRONG!!! (method: leaf setH)   tell it edda please!");
			// throw new SideLengthTooSmallException();
		}

		this.panelHeight = h;
		// if (h>maxH)
		{
			updatePixelArray();
			maxH = h;
		}
	}

	@Override
	protected void setX0PreservingX1(final int x0)
	{
		removeROICoords();
		if (xPos + panelWidth - x0 < minLeafSideLength) {
			System.out
					.println("STH WENT WRONG!!! (method: leaf setX0PreservingX1)   tell it edda please!");
			// throw new SideLengthTooSmallException();
		}
		panelWidth = xPos + panelWidth - x0;
		xPos = x0;

		// if (panelWidth>maxW)
		{
			updatePixelArray();
			maxW = panelWidth;
		}
	}

	@Override
	protected void setY0PreservingY1(final int y0)
	{
		removeROICoords();
		if (yPos + panelHeight - y0 < minLeafSideLength) {
			System.out
					.println("STH WENT WRONG!!! (method: leaf setY0PreservingY1)   tell it edda please!");
			// throw new SideLengthTooSmallException();
		}
		panelHeight = yPos + panelHeight - y0;
		yPos = y0;

		// if (panelHeight>maxH)
		{
			updatePixelArray();
			maxH = panelHeight;
		}
	}

	/**
	 * split a panel in the middle, either vertically or horizontally, depending
	 * on the coordinates. only done if panels don't fall below their minimal
	 * size.
	 */
	@Deprecated
	public void split(int x0, int y0, int x1, int y1) {
		parentPanel.split(x0, y0, x1, y1, this);
	}

	/**
	 * TODO Documentation
	 * 
	 * @param horizontally
	 */
	public void split(boolean horizontally) {
		parentPanel.split(horizontally, this);
	}

	/**
	 * Splits a panel into several panels of the same size. only done if panels
	 * don't fall below their minimal size
	 * 
	 * @param horizontally if true panel is split horizontally into two panels, so
	 *          that the panel width is preserved. else height is preserved.
	 * @param nr number of panels the panel is split into.
	 * @see LeafPanel
	 */
	public void split(int nr, boolean horizontally) {
		parentPanel.split(nr, horizontally, this);
	}

	@Override
	public void remove(Overlay o) {
		hideLabel(o);
		parentPanel.remove(this);
	}

	public boolean hasImg() {
		return hasImg;
	}

	public double getScaleBarWidth() {
		return scaleBarWidth;
	}

	public int getScalebarLabelJustification() {
		return scalebarLabelJustification;
	}

	@Override
	public Roi getHighlightROI() {
		Roi r = new Roi(this.getX(), this.getY(), this.getW(), this.getH());
		r.setFillColor(Colors.decode("#330000ff", null));
		return r;
	}

	public DataSource getImgData() {
		return imgData;
	}

	public void setImgData(final DataSource newData) {
		imgData = newData;
	}

	public static void setColorValue(int newColor) {
		colorValue = newColor;
	}

	public void setScalebarLabelJustification(int scalebarLabelJustification) {
		this.scalebarLabelJustification = scalebarLabelJustification;
	}

	public void setScaleBarWidth(double scaleBarWidth) {
		this.scaleBarWidth = scaleBarWidth;
	}

	public void setHasImg(boolean hasImg) {
		this.hasImg = hasImg;
	}

	@Override
	public void recover() {
		getNewArrayPixels();
	}

	@Override
	public List<DataSource> getDataSources(List<DataSource> list) {
		list.add(this.getImgData());
		return list;
	}

	@Override
	public String generateImageNotesString(String s) {
		// if(imgData.getNotes()!=imgData.defaultNote) {
		// s += imgData.getFileName()+":\n"+imgData.getNotes()+"\n\n";
		// }
		s += imgData;
		return s;
	}

	@Deprecated
	private void removeROICoords() {
		// imgData.setCoords(null, null);
	}

	/**
	 * Computes the x coordinate of the label.
	 * 
	 * @return x coordinate of the first label letter
	 */
	private int getXLabelPosition() {
		if (labelPos == LabelPosition.TopLeft ||
			labelPos == LabelPosition.BottomLeft)
		{
			return getX() + xLabelOffset;
		}

		// TODO Obsolete?
		if (ip == null) {
			ip = new ColorProcessor(10, 10);
		}
		ip.setFont(labelFont);
		return getX() + getW() - xLabelOffset - ip.getStringWidth(labelText);
	}

	/**
	 * Computes the y coordinate of the label.
	 * 
	 * @return y coordinate of the first label letter
	 */
	private int getYLabelPosition() {
		if (labelPos == LabelPosition.TopLeft ||
			labelPos == LabelPosition.TopRight)
		{
			return getY() + yLabelOffset;
		}

		// TODO Obsolete?
		if (ip == null) {
			ip = new ColorProcessor(10, 10);
		}
		ip.setFont(labelFont);
		return getY() + getH() - yLabelOffset - ip.getFontMetrics().getHeight();
	}

	@Override
	public void setLabel(ImagePlus resultFigure, String text, int xOffset,
		int yOffset, LabelPosition pos)
	{
		hasLabel = true;
		labelPos = pos;
		xLabelOffset = xOffset;
		yLabelOffset = yOffset;

		labelText = text;

		labelColor = Toolbar.getForegroundColor();
		labelFont = new Font(TextRoi.getFont(), TextRoi.getStyle(), TextRoi
			.getSize());

		Overlay overlay = resultFigure.getOverlay();
		if (overlay == null) {
			overlay = new Overlay();
			resultFigure.setOverlay(overlay);
		}

		// TODO What if moveLabel fails?
		moveLabel(overlay);
	}

	@Override
	public void moveLabel(Overlay o) {
		if (o == null || !hasLabel) {
			return;
		}

		if (label != null) {
			o.remove(label);
		}

		// TODO Check if imgData == null
		imgData.setLabel(labelText);

		label = new TextRoi(getXLabelPosition(), getYLabelPosition(), labelText,
				labelFont);
		label.setStrokeColor(labelColor);
		o.add(label);
	}

	@Override
	public void removeLabel(Overlay o) {
		hasLabel = false;
		if (label != null) {
			o.remove(label);
		}

		imgData.setLabel("");
		label = null;
	}

	@Override
	public void hideLabel(Overlay o) {
		if (label != null) {
			o.remove(label);
		}
	}

	@Override
	public void setScalebar(ImagePlus resultFigure, int xOffset, int yOffset,
			double width, int height) {

		hasScalebar = true;
		scaleBarHeight = height;
		xScaleOffset = xOffset;
		yScaleOffset = yOffset;
		scaleBarWidth = width;
		if (scalebarColor == null)
			scalebarColor = Toolbar.getForegroundColor();

		Overlay o = resultFigure.getOverlay();
		if (o == null)
			resultFigure.setOverlay(new Overlay());

		o = resultFigure.getOverlay();

		moveScalebar(resultFigure);
	}

	@Override
	public void moveScalebar(ImagePlus imp) {
		Overlay o = imp.getOverlay();
		if (o == null || !hasScalebar) {
			return;
		}
		if (scalebar != null) {
			o.remove(scalebar);
		}
		if (scalebarText != null) {
			o.remove(scalebarText);
		}

		double x1 = this.getX() + this.getW() - xScaleOffset;
		double y1 = this.getY() + this.getH() - yScaleOffset;
		double x2 = this.getX() + this.getW() - xScaleOffset - scaleBarWidth
				+ scaleBarHeight;
		double y2 = this.getY() + this.getH() - yScaleOffset;
		Line l = new Line(x1, y1, x2, y2);
		String value = this.getShortScaleBarText();
		ImageProcessor ip = imp.getProcessor();
		ip.setFont(this.getScaleBarTextFont()!=null?this.getScaleBarTextFont():new Font(TextRoi.getFont(), TextRoi.getStyle(),
				TextRoi.getSize()));
		
		if (scalebarLabelJustification==-1) scalebarLabelJustification = TextRoi.getGlobalJustification();
		
		double xLabelOffset = x1 - ip.getStringWidth(value);
		if (scalebarLabelJustification == TextRoi.CENTER) xLabelOffset = x1 - scaleBarWidth/2 - ip.getStringWidth(value)/2;
		else if (scalebarLabelJustification == TextRoi.LEFT) xLabelOffset = x1 - scaleBarWidth;
		TextRoi text = new TextRoi(xLabelOffset, y1
				- ip.getFontMetrics().getHeight() - scaleBarHeight, value,this.getScaleBarTextFont());
		
		l.setImage(imp); // for calibration
		text.setImage(imp);
		scalebarVisible = true;

		scalebar = l;
		scalebarText = text;
		scalebar.setStrokeWidth(scaleBarHeight);
		scalebar.setStrokeColor(scalebarColor);
		if (scaleBarWidth > 9) {
			o.add(scalebar);
			// IJ.showStatus
			// (IJ.d2s(scaleBarWidth*this.getImgData().getPixelWidth(),2)+" "+this.getImgData().getUnit());
			imgData.setScalebarLength(scaleBarWidth
					* this.getImgData().getPixelWidth());

			if (this.isScalebarTextVisible()){
				scalebarText.setStrokeColor(scalebarColor);
				o.add(scalebarText);
			}
		} else {
			// IJ.showStatus ("Scale bar too small");
			imgData.setScalebarLength(-1.0);
		}

		if (o == null || !hasLabel) {
			return;
		}

	}

	@Override
	public void removeScalebar(Overlay o) {
		hasScalebar = false;
		if (scalebar != null) {
			o.remove(scalebar);
		}
		if (scalebarText != null) {
			o.remove(scalebarText);
		}
		imgData.setScalebarLength(-1);
		scalebar = null;
		scalebarVisible = false;
		scalebarTextVisible = false;

	}

	@Override
	public void hideScalebar(Overlay o) {
		if (scalebar != null)
			o.remove(scalebar);
		if (scalebarText != null) {
			o.remove(scalebarText);
		}
		scalebarVisible = false;
		//scalebarTextVisible = false;
	}

	/**
	 * TODO Documentation
	 * 
	 * @return
	 */
	public boolean isScalebarVisible() {
		return scalebarVisible;
	}

	/**
	 * TODO Documentation
	 * @return
	 */
	public String getScaleBarText() {
		if (!scalebarVisible) {
			return "";
		}

		return "scalebar:"
				+ IJ.d2s(scaleBarWidth * imgData.getPixelWidth(), 2)
				+ imgData.getUnit();
	}

	/**
	 * TODO Documentation
	 * 
	 * @return
	 */
	public String getShortScaleBarText() {
		if (!scalebarVisible) {
			return "";
		}

		DecimalFormat df = new DecimalFormat("########.#######");
		return df.format(scaleBarWidth * imgData.getPixelWidth())
				+ " " + imgData.getUnit();

	}

	/**
	 * TODO Documentation
	 * @param c
	 */
	public void setScalebarColor(final Color c) {
		scalebarColor = c;
	}

	/**
	 * TODO Documentation
	 * 
	 * @param b
	 */
	public void setScaleBarTextVisible(final boolean b) {
		scalebarTextVisible = b;
	}

	/**
	 * TODO Documentation
	 * 
	 * @return
	 */
	public boolean isScalebarTextVisible() {
		return scalebarTextVisible;
	}

	/**
	 * TODO Documentation
	 *
	 * @return
	 */
	public Font getScaleBarTextFont() {
		return scaleBarTextFont;
	}

	/**
	 * TODO Documentation
	 * 
	 * @param scaleBarTextFont
	 */
	public void setScaleBarTextFont(final Font scaleBarTextFont) {
		this.scaleBarTextFont = scaleBarTextFont;
	}

	/**
	 * Checks if {@code this} contains a {@link DataSource}.
	 * 
	 * @return true if {@link #imgData} does not contain a {@link DataSource}.
	 */
	public boolean isEmpty() {
		return getImgData() == null;
	}

	public void clear() {
		imgData.clear();

		eraseImage();
		// HACK Ugly ugly code
		hideScalebar(MainController.getInstance().getMainWindow().getImagePlus().getOverlay());
	}
	
}
