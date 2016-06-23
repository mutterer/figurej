package fr.cnrs.ibmp;

import ij.IJ;
import ij.Menus;
import ij.gui.Toolbar;
import ij.plugin.PlugIn;
import imagescience.ImageScience;

/**
 * Main {@link PlugIn} of FigureJ that checks for satisfied dependencies.
 */
public class FigureJ implements PlugIn {

	/** Minimum required ImageScience version */
	private final static String MINISVERSION = "3.0.0";

	String errorDetails = "";

	@Override
	public void run(String arg) {
		// Check if requirements are fulfilled
		checkImageJ();
		checkBioformats();
		checkLSMToolbox();
		checkLSMReader();
		checkImageScience();

		if (!errorDetails.isEmpty()) {
			IJ.error("FigureJ", "Some libraries are missing:\n \n" + errorDetails);
			return;
		}
		
		Toolbar.addPlugInTool(new FigureJ_Tool());
	}

	/**
	 * Code adapted from NeuronJ source code by Erik Meijering.
	 */
	private void checkImageScience() {
		if (ImageScience.version().compareTo(MINISVERSION) < 0) {
			errorDetails += "* ImageScience version " + MINISVERSION +
				" or higher is required";
		}
	}

	private void checkLSMReader() {
		if (Menus.getCommands().get("LSMReader...") == null) {
			errorDetails += "* LSM Reader is required\n";
		}
	}

	private void checkLSMToolbox() {
		if (Menus.getCommands().get("Show LSMToolbox") == null) {
			errorDetails += "* LSMToolbox is required\n";
		}
	}

	private void checkBioformats() {
		if (Menus.getCommands().get("Bio-Formats Importer") == null) {
			errorDetails += "* Bioformats is required\n";
		}
	}

	private void checkImageJ() {
		if (IJ.getVersion().compareTo("1.48") < 0) {
			errorDetails += "* ImageJ 1.48 or later is required\n";
		}
	}

}
