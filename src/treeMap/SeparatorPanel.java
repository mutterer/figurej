package treeMap;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.Colors;
import ij.process.ImageProcessor;

/*
 * @author Edda Zinck
 * @author Jerome Mutterer
 * (c) IBMP-CNRS   
 * 
*/
public class SeparatorPanel extends Panel{

	private static final long serialVersionUID = 1L;
	private static int rgbDefColor = 0xff000000 | (255<<16) | (255<<8) | 255;
	// private static int rgbDefColor = 0x99aabb;
	// 0x99aabb;

	public SeparatorPanel(int xPos, int yPos, int width, int height){	
		super(xPos, yPos, width, height);
	}

	/**
	 * @param xPos
	 * @param yPos
	 * @param length either width or height of the separator, depending on boolean parameter
	 * @param setHeighToDefault if true, length is interpreted as height, width gets default value, else the other way round
	 */
	public SeparatorPanel(int xPos, int yPos, int length, boolean setHeighToDefault){	
		super(xPos, yPos, separatorWidth, length);
		if(setHeighToDefault) {
			xPos = length;
			yPos = separatorWidth;
		}
	}

	
	@Override
	public void draw(ImagePlus resImg){
		ImageProcessor imp = resImg.getProcessor();
		int figHeight = resImg.getHeight();
		int figWidth  = resImg.getWidth();
		int[] pixels  = (int[]) imp.getPixels();
		for(int imgY=yPos; 		imgY < Math.min(figHeight, yPos+panelHeight); imgY++)
			for(int imgX=xPos; 	imgX < Math.min(figWidth,  xPos+panelWidth ); imgX++) {
				pixels[imgY*figWidth + imgX] = rgbDefColor;
			}
		imp.setPixels(pixels);
		resImg.setProcessor(imp);
	}

	@Override
	public int getClosestY(Panel p, int currentClosest) {
		if ((this != p)&&(this.getW()>this.getH())) {
			if(this.getY()>p.getY()-snapDist && this.getY()<p.getY()+snapDist)
				currentClosest = this.getY();
		}
		else if ((this != p)&&(this.getW()>this.getH())) {
			if(this.getY()+this.getH()/2 -separatorWidth> p.getY()-snapDist && this.getY()+this.getH()/2-separatorWidth < p.getY()+snapDist)
				currentClosest = this.getY();
		}
		return currentClosest;
	}

	@Override
	public int getClosestX(Panel p, int currentClosest) {
		if( (this != p)&&(this.getW()<this.getH())) {
			if(this.getX()>p.getX()-snapDist && this.getX()<p.getX()+snapDist)
				currentClosest = this.getX();
		}
		else if( (this != p)&&(this.getW()>this.getH()))
			if(this.getX()+this.getW()/2-separatorWidth > p.getX()-snapDist && this.getX()+this.getW()/2-separatorWidth < p.getX()+snapDist)
				currentClosest = this.getX();
		
		return currentClosest;
	}

	/** returns true if x and y are close the panel */
	@Override
	public boolean isClicked(int x, int y, int tol)
	{
		if(x >= xPos-tol && x <= xPos+panelWidth+tol && y >= yPos-tol && y <= yPos+panelHeight+tol)
			return true;
		return false;

	}
	@Override
	public Roi getHighlightROI() {
		Roi r = new Roi(this.getX(), this.getY(), this.getW()+1, this.getH()+1);
		r.setFillColor(Colors.decode("#66ff0000", null));
		return r;
	}
	
	public static void setColor(int newColor) {
		rgbDefColor = newColor;
	}
}
