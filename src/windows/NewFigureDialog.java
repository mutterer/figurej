package windows;

/*
 * @author Edda Zinck
 * @author Jerome Mutterer
 * (c) IBMP-CNRS
*/ 

import ij.IJ;

import ij.Prefs;
import ij.gui.GenericDialog;


public class NewFigureDialog  {

	private String  		unit = "";
	private int 			width  	= 0, height  = 0, resolution = 0,  separatorSize = 5;
	private double 			pWidth 	= 0, pHeight = 0, pseparatorSize=0;
	private String[] 		units 	= {"cm", "mm", "inch"};

	GenericDialog 			gd;
	private int 			fieldSize 	= 15;

	/** @return real world unit like cm */
	public String getUnit() {
		return unit;
	}

	public double getResolution() {
		return resolution;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	/** @return width of the separator lines between leaf panels / different images */
	public int getSeparatorSize() {
		return separatorSize;
	}

	/**@return true if a new image has to be created (dialog not cancelled, no new template created */
	public boolean openDialog() {

		gd = new GenericDialog("New Figure");

		gd.addMessage("Specify Printed Figure Dimensions");
		gd.addNumericField("Printed_Width",  Prefs.get("figure.width", 10.0), 2, fieldSize, "");
		gd.addNumericField("Printed_Height", Prefs.get("figure.height", 10.0), 2, fieldSize, "");
		gd.addNumericField("Separator_Size",  Prefs.get("figure.separator", 0.05),  2, fieldSize, "");
		gd.addChoice("Size_Unit", units, Prefs.get("figure.unit", "cm"));
		gd.addMessage("\n");
		gd.addNumericField("Resolution", Prefs.get("figure.resolution", 300), 0, 6, "d.p.i.");

		// show the dialog
		gd.showDialog();
		if (gd.wasCanceled()) 
			return false;

		// store the values entered
		pWidth = (double) gd.getNextNumber();
		pHeight 	= (double) gd.getNextNumber();
		pseparatorSize 	= (double) gd.getNextNumber();
		unit 		= gd.getNextChoice();
		resolution 	= (int) gd.getNextNumber();

		Prefs.set("figure.width", pWidth);
		Prefs.set("figure.height", pHeight);
		Prefs.set("figure.separator", pseparatorSize);
		Prefs.set("figure.unit", unit);
		Prefs.set("figure.resolution", resolution);
		
		double factor = 1;
		if ("cm" == unit) 
			factor = 1 / 2.54;
		else if ("mm" == unit) 
			factor = 1 / 25.4;

		// real world coordinates to pixel values
		width  = (int)((pWidth  * resolution * factor) +0.5);  // for numbers >0 adding 0.5 before a cast gives the same result as rounding
		height = (int)((pHeight * resolution * factor) +0.5);  // which costs more time
		separatorSize = (int)((pseparatorSize * resolution * factor) +0.5);  

		if ((width > 0) && (height > 0)) 
			return true;

		else {
			IJ.error("Image too small");
			return false; 
		}
	}

}
