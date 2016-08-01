// TODO License header

package fr.cnrs.ibmp;

import java.util.EventObject;

import ij.ImagePlus;

/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich
 */
public class ImageSelectionEvent extends EventObject {

	private double[] xVals;
	private double[] yVals;
	private String macro;

	public ImageSelectionEvent(final ImagePlus selectedImage,
		final double[] xVals, final double[] yVals, final String macro)
	{
		super(selectedImage);
		this.setxVals(xVals);
		this.setyVals(yVals);
		this.setMacro(macro);
	}

	public String getMacro() {
		return macro;
	}

	public void setMacro(String macro) {
		this.macro = macro;
	}

	public double[] getxVals() {
		return xVals;
	}

	public void setxVals(double[] xVals) {
		this.xVals = xVals;
	}

	public double[] getyVals() {
		return yVals;
	}

	public void setyVals(double[] yVals) {
		this.yVals = yVals;
	}

}
