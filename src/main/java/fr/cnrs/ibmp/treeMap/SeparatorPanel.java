package fr.cnrs.ibmp.treeMap;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.Colors;
import ij.process.ImageProcessor;

/**
 * TODO Documentation
 * <p>
 * (c) IBMP-CNRS
 * </p>
 * @author Edda Zinck
 * @author Jerome Mutterer
 */
public class SeparatorPanel extends AbstractPanel{

	private static final long serialVersionUID = 1L;
	private static int rgbDefColor = 0xff000000 | (255<<16) | (255<<8) | 255;
	// private static int rgbDefColor = 0x99aabb;
	// 0x99aabb;

	public final static int snapDist = 15;

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
	public int getClosestY(AbstractPanel p, int currentClosest) {
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
	public int getClosestX(AbstractPanel p, int currentClosest) {
		if( (this != p)&&(this.getW()<this.getH())) {
			if(this.getX()>p.getX()-snapDist && this.getX()<p.getX()+snapDist)
				currentClosest = this.getX();
		}
		else if( (this != p)&&(this.getW()>this.getH()))
			if(this.getX()+this.getW()/2-separatorWidth > p.getX()-snapDist && this.getX()+this.getW()/2-separatorWidth < p.getX()+snapDist)
				currentClosest = this.getX();
		
		return currentClosest;
	}

	@Override
	public Roi getHighlightROI() {
		Roi r = new Roi(this.getX(), this.getY(), this.getW()+1, this.getH()+1);
		r.setFillColor(Colors.decode("#66ff0000", null));
		return r;
	}

	/**
	 * TODO Documentation
	 * 
	 * @param newColor
	 */
	public static void setColor(int newColor) {
		rgbDefColor = newColor;
	}
}
