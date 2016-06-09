package fr.cnrs.ibmp.treeMap;

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

import java.awt.Color;
import java.awt.Font;
import java.text.DecimalFormat;
import java.util.List;

import fr.cnrs.ibmp.utilities.LabelPosition;
import fr.cnrs.ibmp.dataSets.DataSource;

/*
 * @author Edda Zinck
 * @author Jerome Mutterer
 * (c) IBMP-CNRS 
 *
 * panel containing a link to a data source (image)
 * can not have any children. 
 */
public class LeafPanel extends Panel {

	private static final long serialVersionUID = 1L;
	private DataSource imgData;
	private static int colorValue = 0x99aabb;
	private int maxW;
	private int maxH;

	private boolean hasImg = false;

	public boolean isHasImg() {
		return hasImg;
	}

	public void setHasImg(boolean hasImg) {
		this.hasImg = hasImg;
	}

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
	private boolean hasLabel = false;
	// scale bar stuff
	private transient Roi scalebar;
	private transient Roi scalebarText;
	private int xScaleOffset = this.getW() - 20;
	private int yScaleOffset = this.getH() - 20;
	private int scaleBarHeight = separatorWidth;
	private double scaleBarWidth = 1;

	public double getScaleBarWidth() {
		return scaleBarWidth;
	}

	public void setScaleBarWidth(double scaleBarWidth) {
		this.scaleBarWidth = scaleBarWidth;
	}

	private Color scalebarColor;
	private boolean hasScalebar = false;
	private boolean scalebarVisible = false;
	private boolean scalebarTextVisible = false;
	private Font scaleBarTextFont = null;
	private int scalebarLabelJustification = -1;

	public int getScalebarLabelJustification() {
		return scalebarLabelJustification;
	}

	public void setScalebarLabelJustification(int scalebarLabelJustification) {
		this.scalebarLabelJustification = scalebarLabelJustification;
	}

	public LeafPanel(int xPos, int yPos, int w, int h) {
		super(xPos, yPos, w, h);
		imgData = new DataSource();
		maxW = w;
		maxH = h;
		updatePixelArray();

		if (scaleBarHeight == 0)
			scaleBarHeight = 5;
	}

	@Override
	public void draw(ImagePlus resImg) {
		if (hasLabel == true)
			moveLabel(resImg.getOverlay());
		moveScalebar(resImg);

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

	private void updateMetadata() {
		String cal = "1";
		String unit = "pixel";
		try {
			cal = "" + imgData.getPixelWidth();
			unit = "" + imgData.getUnit();
		} catch (Exception e) {
		}
		String panelRoi = "" + xPos + "," + yPos + "," + panelWidth + ","
				+ panelHeight + "," + cal + "," + unit
				// +","+imgData.getType() //TODO remove safely
				+ "," + imgData.getFileDirectory() + ","
				+ imgData.getFileName();

		ImagePlus imp = WindowManager.getImage((int) Prefs.get("figure.id", 0));
		if (imp != null) {
			String metadata = (String) imp.getProperty("Info");
			imp.setProperty("Info", metadata + "\n" + panelRoi);
		}
	}

	protected void updatePixelArray() {
		if (hasImg)
			expandPixelArray();

		else
			getNewArrayPixels();
	}

	/** define the pixel values for each panel */
	private void getNewArrayPixels() {

		myPanelPixels = new int[panelWidth * panelHeight];
		for (int y = 0; y < panelHeight; y++)
			for (int x = 0; x < panelWidth; x++)
				myPanelPixels[y * panelWidth + x] = colorValue;
		maxW = getW();
		maxH = getH();
	}

	/** re-fill the panel with its original color */
	public void eraseImage() {
		hasImg = false;
		for (int y = 0; y < panelHeight; y++)
			for (int x = 0; x < panelWidth; x++)
				myPanelPixels[y * panelWidth + x] = colorValue;
	}

	/**
	 * creates a new pixel array, transfers the old data to the center (former:
	 * upper left - commented out) and fills the rest with default color
	 */
	private void expandPixelArray() {
		int[] myTempPixels = new int[panelWidth * panelHeight];
		int offSetY = panelHeight / 2 - maxH / 2;
		int offSetX = panelWidth / 2 - maxW / 2;
		for (int y = 0; y < panelHeight; y++)
			for (int x = 0; x < panelWidth; x++) {
				if (x >= offSetX && x < offSetX + maxW && y >= offSetY
						&& y < offSetY + maxH)
					myTempPixels[y * panelWidth + x] = myPanelPixels[(y - offSetY)
							* maxW + (x - offSetX)];
				else
					myTempPixels[y * panelWidth + x] = colorValue;

				// if(maxW > x && maxH > y)
				// myTempPixels[y*panelWidth + x] = myPanelPixels[y*maxW + x];
				// else
				// myTempPixels[y*panelWidth + x] = colorValue;
			}
		myPanelPixels = myTempPixels;
		maxW = getW();
		maxH = getH();
	}

	@Override
	public void setPixels(ImagePlus image) {
		hasImg = true;
		if (image.getProcessor() == null)
			image.setProcessor(IJ.getImage().getProcessor());
		System.out.println(image.getProcessor() != null); // TODO is null after
															// opening a new
															// image
		image.setRoi(new Roi(getX(), getY(), getW(), getH()));
		myPanelPixels = (int[]) image.getProcessor().crop().getPixels();
		maxW = getW();
		maxH = getH();

	}

	public void setPanelPixels(ImagePlus image) {
		hasImg = true;
		if (image.getProcessor() == null)
			image.setProcessor(IJ.getImage().getProcessor());
		System.out.println(image.getProcessor() != null); // TODO is null after
															// opening a new
															// image
		// image.setRoi(new Roi(getX(), getY(), getW(), getH()));
		myPanelPixels = (int[]) image.getProcessor().getPixels();
		maxW = getW();
		maxH = getH();

	}

	public void setPixels(int[] pixels) {
		if (pixels.length == panelHeight * panelWidth) {
			maxW = panelWidth;
			maxH = panelHeight;
			myPanelPixels = pixels;
			hasImg = true;
		} else
			System.out.println("array length does not fit");

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
	protected void setX0PreservingX1(int x0) // throws
												// SideLengthTooSmallException {
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
	protected void setY0PreservingY1(int y0) // throws
												// SideLengthTooSmallException {
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

	@Override
	public void split(int x0, int y0, int x1, int y1) {
		parentPanel.split(x0, y0, x1, y1, this);
	}

	@Override
	public void split(boolean horizontally) {
		parentPanel.split(horizontally, this);
	}

	@Override
	public void split(int nr, boolean horizontally) {
		parentPanel.split(nr, horizontally, this);
	}

	@Override
	public void remove(Overlay o) {
		hideLabel(o);
		parentPanel.remove(this);
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

	public void setImgData(DataSource newData) {
		imgData = newData;
	}

	public static void setColorValue(int newColor) {
		colorValue = newColor;
	}

	@Override
	public void recover() {
		getNewArrayPixels();
	}

	/**
	 * returns true if x and y are inside the panel from which a certain
	 * tolerance border got subtracted
	 */
	@Override
	public boolean isClicked(int x, int y, int tol) {
		if (x >= xPos + tol && x <= xPos + panelWidth - tol && y >= yPos + tol
				&& y <= yPos + panelHeight - tol)
			return true;
		return false;
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
		s += imgData.createLog();
		return s;
	}

	private void removeROICoords() {
		imgData.setCoords(null, null);
	}

	/**
	 * @param label
	 *            text to display on the labeled panel
	 * @return y distance between the left coordinate of the panel and the fist
	 *         label letter
	 */
	private int getXLabelOffset() {
		if (labelPos == LabelPosition.TopLeft
				|| labelPos == LabelPosition.BottomLeft)
			return this.getX() + xLabelOffset;
		else {
			if (ip == null)
				ip = new ColorProcessor(10, 10);
			ip.setFont(this.labelFont);
			// ip.setFont(new Font(TextRoi.getFont(), TextRoi.getStyle(),
			// TextRoi.getSize()));
			return this.getX() + this.getW() - xLabelOffset
					- ip.getStringWidth(labelText);
		}
	}

	/**
	 * @param label
	 *            text to display on the labeled panel
	 * @return x distance between the left coordinate of the panel and the fist
	 *         label letter
	 */
	private int getYYLabelOffset() {
		if (labelPos == LabelPosition.TopLeft
				|| labelPos == LabelPosition.TopRight)
			return this.getY() + yLabelOffset;
		else {
			if (ip == null)
				ip = new ColorProcessor(10, 10);
			ip.setFont(this.labelFont);
			// ip.setFont(new Font(TextRoi.getFont(), TextRoi.getStyle(),
			// TextRoi.getSize()));
			return this.getY() + this.getH() - yLabelOffset
					- ip.getFontMetrics().getHeight();
		}
	}

	@Override
	public void setLabel(ImagePlus resultFigure, String text, int xOffset,
			int yOffset, LabelPosition pos) {

		hasLabel = true;
		labelPos = pos;
		xLabelOffset = xOffset;
		yLabelOffset = yOffset;

		labelText = text;

		labelColor = Toolbar.getForegroundColor();
		labelFont = new Font(TextRoi.getFont(), TextRoi.getStyle(),
				TextRoi.getSize());
		Overlay o = resultFigure.getOverlay();
		if (o == null)
			resultFigure.setOverlay(new Overlay());

		o = resultFigure.getOverlay();
		moveLabel(o);
	}

	@Override
	public void moveLabel(Overlay o) {
		if (o == null || !hasLabel) {
			return;
		}
		if (label != null)
			o.remove(label);
		imgData.setLabel(labelText);

		label = new TextRoi(getXLabelOffset(), getYYLabelOffset(), labelText,
				labelFont);
		label.setStrokeColor(labelColor);
		o.add(label);
	}

	@Override
	public void removeLabel(Overlay o) {
		hasLabel = false;
		if (label != null)
			o.remove(label);
		imgData.setLabel("");
		label = null;
	}

	@Override
	public void hideLabel(Overlay o) {
		if (label != null)
			o.remove(label);
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

	public boolean isScalebarVisible() {
		// TODO Auto-generated method stub
		return scalebarVisible;
	}

	public String getScaleBarText() {
		// TODO Auto-generated method stub
		if (!scalebarVisible)
			return "";
		else
			return "scalebar:"
					+ IJ.d2s(scaleBarWidth * imgData.getPixelWidth(), 2)
					+ imgData.getUnit();
	}

	public String getShortScaleBarText() {
		// TODO Auto-generated method stub
		if (!scalebarVisible)
			return "";

		else {
			DecimalFormat df = new DecimalFormat("########.#######");
			return df.format(scaleBarWidth * imgData.getPixelWidth())
					+ " " + imgData.getUnit();
		}
	}

	public void setScalebarColor(Color c) {
		// TODO Auto-generated method stub
		this.scalebarColor = c;
	}

	public void setScaleBarTextVisible(boolean b) {
		// TODO Auto-generated method stub
		scalebarTextVisible = b;
	}
	public boolean isScalebarTextVisible() {
		// TODO Auto-generated method stub
		return scalebarTextVisible;
	}

	public Font getScaleBarTextFont() {
		return scaleBarTextFont;
	}

	public void setScaleBarTextFont(Font scaleBarTextFont) {
		this.scaleBarTextFont = scaleBarTextFont;
	}

}