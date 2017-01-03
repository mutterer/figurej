package fr.cnrs.ibmp.windows;

import java.io.Serializable;

import fr.cnrs.ibmp.NewFigureEvent;
import fr.cnrs.ibmp.NewFigureListener;
import ij.IJ;

import ij.Prefs;
import ij.gui.GenericDialog;

/**
 * Dialog to request information from the user about the to be created figure,
 * e.g., width and height.
 * 
 * (c) IBMP-CNRS
 * 
 * @author Edda Zinck
 * @author Jerome Mutterer
 */
public class NewFigureDialog implements NewFigureListener, Serializable {

	private static final long serialVersionUID = 1L;

	private double printedWidth = 0;
	private double printedHeight = 0;
	private double printedSeparatorSize = 0;
	private String unit = "";

	private int width = 0;
	private int height = 0;
	private int separatorSize = 5;

	private int resolution = 0;

	private String[] units = { "cm", "mm", "inch" };

	private GenericDialog gd;

	private int fieldSize = 15;

	/**
	 * @return Unit of the provided dimensions, e.g., cm
	 */
	public String getUnit() {
		return unit;
	}

	/**
	 * @return Printed resolution in dots per inch.
	 */
	public double getResolution() {
		return resolution;
	}

	/**
	 * @return Height of resulting image in pixels.
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * @return Width of resulting image in pixels.
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @return width of the separator lines between leaf panels / different images
	 */
	public int getSeparatorSize() {
		return separatorSize;
	}

	/**
	 * @return true if a new image has to be created (dialog not cancelled, no new
	 *         template created)
	 */
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
		if (gd.wasCanceled()) {
			return false;
		}

		// store the values entered
		getValuesFromDialog();
		storeValues();
		
		double factor = 1;
		if (unit.equals("cm")) {
			factor = 1 / 2.54;
		} else if (unit.equals("mm")) {
			factor = 1 / 25.4;
		}

		// real world coordinates to pixel values
		width  = (int)((printedWidth  * resolution * factor) + 0.5); // for numbers >0 adding 0.5 before a cast gives the same result as rounding
		height = (int)((printedHeight * resolution * factor) + 0.5); // which costs more time
		separatorSize = (int)((printedSeparatorSize * resolution * factor) + 0.5);  

		if ((width > 0) && (height > 0)) {
			return true;
		}

		IJ.error("Image too small");
		return false; 
	}

	private void getValuesFromDialog() {
		printedWidth = gd.getNextNumber();
		printedHeight = gd.getNextNumber();
		printedSeparatorSize = gd.getNextNumber();
		unit = gd.getNextChoice();
		resolution = (int) gd.getNextNumber();
	}

	private void storeValues() {
		Prefs.set("figure.width", printedWidth);
		Prefs.set("figure.height", printedHeight);
		Prefs.set("figure.separator", printedSeparatorSize);
		Prefs.set("figure.unit", unit);
		Prefs.set("figure.resolution", resolution);
	}

	@Override
	public void newFigure(NewFigureEvent e) {
		openDialog();
	}

}

