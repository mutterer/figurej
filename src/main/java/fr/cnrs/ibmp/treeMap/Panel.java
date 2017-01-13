package fr.cnrs.ibmp.treeMap;

import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import fr.cnrs.ibmp.dataSets.DataSource;
import fr.cnrs.ibmp.labels.LabelPosition;

/**
 * Abstract superclass for panels that make up a figure.
 * <p>
 * (c) IBMP-CNRS
 * </p>
 * @author Edda Zinck
 * @author Jerome Mutterer
 */
public abstract class Panel implements Serializable, PanelInterface {

	private static final long serialVersionUID = 1L;
	protected int xPos;
	protected int yPos;

	protected int panelWidth;
	protected int panelHeight;

	protected ContainerPanel parentPanel;
	protected List<Panel> children = new LinkedList<Panel>(); 	// children  successively arranged! topmost / most left child first

	protected static int separatorWidth = 7;
	protected static int minLeafSideLength = 20;

	/**
	 * TODO Documentation
	 * 
	 * @param xPos
	 * @param yPos
	 * @param w
	 * @param h
	 */
	public Panel(int xPos, int yPos, int w, int h) {
		this.parentPanel = null;
		this.xPos = xPos;
		this.yPos = yPos;

		this.panelWidth = w;
		this.panelHeight = h;
	}

	/**
	 * Checks if a click was inside a {@link Panel}.
	 * 
	 * @param x x coordinate of a click (in pixels)
	 * @param y y coordinate of a click (in pixels)
	 * @param tol tolerance added to the panel's borders (in pixels)
	 * @return true if {@code x} and {@code y} are inside the panel with an
	 *         additional tolerance added to the panel's borders.
	 */
	public boolean isClicked(int x, int y, int tol) {
		if (x >= xPos + tol && x <= xPos + panelWidth - tol && y >= yPos + tol &&
			y <= yPos + panelHeight - tol)
		{
			return true;
		}
		return false;
	}

	/**
	 * TODO Documentation
	 * 
	 * @return the panel the deepest down in the tree hierarchy that was clicked
	 *         (should return a leaf or separator). used to avoid overlays by
	 *         containers.
	 */
	public Panel getClicked(int x, int y, int tol) {
		Panel clicked = null;
		if(this.isClicked(x, y, tol))
			clicked = this;
		for (Panel child : children) {
			Panel temp = child.getClicked(x, y, tol);
			if(temp != null)
				clicked = temp;
				// TODO break; ?
		}

		return clicked;
	}

	/**
	 * TODO Documentation
	 * 
	 * @param p panel the closest important y coordinate is searched for
	 * @param currentClosest some value e.g. max value
	 * @return if no important coordinate (separator or middle point of a
	 *         separator) is in the snap distance region: currentClosest else the
	 *         nearest of the y coordinates in the region
	 */
	public int getClosestY(Panel p, int currentClosest) {
		for(Panel q: children) {
			int cur = q.getClosestY(p, currentClosest);
			if(cur<currentClosest)
				currentClosest = cur;
		}
		return currentClosest;
	}

	/**
	 * TODO Documentation
	 * 
	 * @param s
	 * @return
	 */
	public String generateImageNotesString(String s) {
		for(Panel child: children) {
			String t = child.generateImageNotesString(s);
			if(t != null && !t.equals("")) {
				s = t;
			}
		}
		return s;
	}

	/**
	 * TODO Documentation
	 * 
	 * @param p panel the closest important x coordinate is searched for
	 * @param currentClosest some large value, e.g. max value
	 * @return if no important coordinate (separator or middle point of a
	 *         separator) is in the snap distance region: initial currentClosest
	 *         value, else the nearest of the y coordinates in the region
	 * @see SeparatorPanel
	 */
	public int getClosestX(Panel p, int currentClosest) {
		for(Panel q: children) {
			int cur = q.getClosestX(p, currentClosest);
			if(cur<currentClosest) {
				currentClosest = cur;
			}
		}
		return currentClosest;
	}

	/**
	 * TODO Documentation
	 *
	 * @param p
	 */
	protected void remove(LeafPanel p) {
		// NB
	}

	/**
	 * If a tree contains only a single leaf, this one can't get removed.
	 * 
	 * @param removeLeaf the leaf (image) that gets removed from the tree (result
	 *          image)
	 */
	public void remove(Overlay o) {
		// NB
	}

	/**
	 * Recover from serialization process.
	 */
	@Deprecated
	public void recover() {
	}

	/**
	 * Draws pixels in the right position onto the input {@link ImagePlus}.
	 * 
	 * @param resIMG the {@link ImagePlus} to draw on.
	 */
	public void draw(ImagePlus resIMG) {
		for (Panel child : children) {
			child.draw(resIMG);
		}
	}

	/**
	 * @return ROI of panel size and position
	 */
	public Roi getHighlightROI() {
		return null;
	}

	/**
	 * Prints the panel with its dimensions and type as well as its children.
	 * 
	 * @deprecated Use {@link #toString()} instead.
	 */
	@Deprecated
	public void print() {
		System.out.println(xPos+" "+yPos+"\t "+panelWidth+" "+panelHeight+"\t"+this.getClass());
		for (Panel child : children) {
			child.print();
		}
		System.out.println();
	}

	@Override
	public String toString() {
		String s = String.format("%d %d\t%d %d\t%s%n", xPos, yPos, panelWidth, panelHeight, this.getClass());
		for (Panel child : children) {
			s += child.toString();
		}

		return s;
	}

	/**
	 * @return subtree of the current panel
	 */
	public Collection<Panel> getChildren() {
		return children;
	}

	/**
	 * @return width of the panel
	 */
	public int getW() {
		return panelWidth;
	}

	/**
	 * @return height of the panel
	 */
	public int getH() {
		return panelHeight;
	}

	/**
	 * @return parent panel (panel of the next higher level in the tree)
	 */
	public ContainerPanel getParent() {
		return parentPanel;
	}

	/**
	 * @return x position of the panel. root panel should have a xPos of 0
	 */
	public int getX() {
		return xPos;
	}

	/**
	 * @return y position of the panel. root panel should have a yPos of 0
	 */
	public int getY() {
		return yPos;
	}

	/**
	 * Changes the x1 coordinate of the panel.
	 * <p>
	 * please call this method only if checking the ability of the panel to change
	 * its size with the canSetW(..) method if you want to have your panels to
	 * have a minimum side length
	 * </p>
	 * 
	 * @param w new width of the panel
	 */
	public void setW(int w) {
		this.panelWidth = w;
	}

	/**changes the y1 coordinate of the panel
	 * @param h new height of the panel 
	 * please call this method only if checking the ability of the panel to change its size 
	 * with the canSetH(..) method if you want to have your panels to have a minimum side length*/
	public void setH(int h) //throws SideLengthTooSmallException {
	{
		this.panelHeight = h;
	}
	/**changes the width of the panel as well as its left x value (x0).
	 * doesn't change the x1 coordinate.
	 * @param x0 new left x value of the panel 
	 * please call this method only if checking the ability of the panel to change its size 
	 * with the canSetX0PreservingX1(..) method if you want to have your panels to have a minimum side length*/
	protected void setX0PreservingX1(int x0)  //throws SideLengthTooSmallException {
	{
		if(x0>xPos+panelWidth)
		{			
			System.out.println("EXCEPTION in class panel setY0PreservingY1: x0 > x1!!! please report that to edda!");
			//throw new SideLengthTooSmallException();
		}
		panelWidth = xPos+panelWidth - x0;
		xPos = x0;		
	}	
	/**changes the height of the panel as well as its upper y value (y0).
	 * doesn't change the x1 coordinate.
	 * @param x0 new smaller x value of the panel 
	 * please call this method only if checking the ability of the panel to change its size 
	 * with the canSetY0PreservingY1(..) method if you want to have your panels to have a minimum side length*/
	protected void setY0PreservingY1(int y0)  //throws SideLengthTooSmallException {
	{
		if(y0>yPos+panelHeight)
		{			
			System.out.println("EXCEPTOPN in class panel setY0PreservingY1: y0 > y1!!  please report that to edda!");
			//throw new SideLengthTooSmallException();
		}
		panelHeight = yPos+panelHeight - y0;
		yPos = y0;
	}
	/**
	 * @param new width for the current panel
	 * @return true if the width is larger than the minimal size
	 * container overrides this method so that it only returns true if none of his children would get too small 
	 * by setting the containers size to the parameter value
	 */
	public boolean canSetW(int w) {
		if(w < minLeafSideLength)
			return false;
		return true;
	}

	/**
	 * @param new height for the current panel
	 * @return true if the height is larger than the minimal size
	 * container overrides this method so that it only returns true if none of his children would get too small 
	 * by setting the containers size to the parameter value
	 */
	public boolean canSetH(int h) {
		if(h < minLeafSideLength)
			return false;
		return true;
	}

	/**
	 * @param new left x coordinate for the current panel
	 * @return true if the width resulting from moving the left x coordinate to its new location without changing
	 * the right coordinate is larger than the minimal size
	 * container overrides this method so that it only returns true if none of his children would get too small 
	 * by changing the containers size as described
	 */
	protected boolean canSetX0PreservingX1(int x0) {
		if(xPos+panelWidth - x0 < minLeafSideLength)
			return false;
		return true;
	}

	/**
	 * @param new upper y coordinate for the current panel
	 * @return true if the height resulting from moving the upper y coordinate to its new location without changing
	 * the lower coordinate is larger than the minimal size
	 * container overrides this method so that it only returns true if none of his children would get too small 
	 * by changing the containers size as described
	 */
	protected boolean canSetY0PreservingY1(int y0) {
		if(yPos+panelHeight-y0<minLeafSideLength) 
			return false;
		return true;
	}

	/**changes both x values, preserves the width of the panel.
	 * @param newX new value for the left x coordinate  */
	protected void setX(int newX) {
		if(newX<0) xPos = 0;
		else
			xPos = newX;
	}

	/**changes both y values, preserves the height of the panel.
	 * @param newY new value for the left y coordinate  */
	protected void setY(int newY) {
		if(newY<0) yPos = 0;
		else
			yPos = newY;
	}


	/** sets the parent panel. does not set the current panel as the parent's child!! */
	protected void setParent(ContainerPanel newParent) {
		this.parentPanel = newParent;	
	}
	/** @param newW new width of the lines between the images */
	public void setSeparatorWidth(int newW)
	{
		if(separatorWidth >= 0)
			separatorWidth = newW;
		else { 
			System.out.println("negative separator width not accepted (container panel)");
		}
	}
	/**@return width of the lines between the images */
	public int getSeparatorWidth() {
		return separatorWidth;
	}

	/** container panels contain leafs and separators which are called children*/
	public void addChild(Panel child)
	{
		// careful!  don`t adds the parent (but correctly overridden by container)
		children.add(child);	
	}
	
	/** @return number of leafs + separators the container stores*/
	public int getNrOfChildren() {
		return children.size();
	}
	
	/**
	 * @param image result image that got changed by non-celly functions (macros; plug ins.. ) 
	 * grab the changed pixels and store them  */
	public void setPixels(ImagePlus image) {
		for(Panel child:children)
			child.setPixels(image);
	}

	/**@param list empty list
	 * @return the list filled with all objects containing information (path, comment calibration..) about respectively one panel */
	public List<DataSource> getDataSources(List<DataSource> list) {
		for(Panel p:children) {
			p.getDataSources(list);
		}
		return list;
	}
	
	/**
	 * @param resultFigure image to draw on
	 * @param text label text
	 * @param xOffset distance to the left margin of the panel
	 * @param yOffset distance to the top margin of the panel
	 * @param pos position of the label
	 * 
	 * draws a text ROI
	 */
	public void setLabel(ImagePlus resultFigure, String text, int xOffset, int yOffset, LabelPosition pos) {
	}
	
	/**
	 * @param o Overlay of the image the label is drawn on (==overlay of the result image)
	 * moves a label so that it keeps its position relative to the panel corner when the panel changes size	 */
	public void moveLabel(Overlay o) {
	}
	
	/**
	 * @param o Overlay of the image the label is drawn on (==overlay of the result image) 
	 * containers do not remove the overlays of their children
	 * removes the label of the panel and destroys the object */
	public void removeLabel(Overlay o) {
		
	}
	
	/**
	 * @param o Overlay of the image the label is drawn on (==overlay of the result image) 
	 * removes the label of the panel but does not destroy the object
	 * containers hide the labels of their children
	 * (called before serialization to avoid getting labels unlinked to panels after re opening)*/
	public void hideLabel(Overlay o) {
	}
	
	public void setScalebar(ImagePlus resultFigure, int xOffset, int yOffset, double width, int height) {
		
	}
	
	public void moveScalebar(ImagePlus resultFigure) {
		
	}
	
	public void removeScalebar(Overlay o) {
		
	}
	
	public void hideScalebar(Overlay o) {
		
	}
}