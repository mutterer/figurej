package fr.cnrs.ibmp;

import fr.cnrs.ibmp.utilities.Macros;
import fr.cnrs.ibmp.windows.MainController;
import fr.cnrs.ibmp.windows.ROIToolWindow;
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

	/** Error string that communicates missing libraries */
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
		
		MainController mainController = MainController.getInstance();
		
		FigureJ_Tool figureJTool = new FigureJ_Tool();
		Toolbar.addPlugInTool(figureJTool);
		mainController.setFigureJTool(figureJTool);
		
		ROIToolWindow roiTool = new ROIToolWindow();
		Toolbar.addPlugInTool(roiTool);
		IJ.addEventListener(roiTool);
		figureJTool.addLeafListener(roiTool);
		mainController.setRoiTool(roiTool);
		roiTool.addImageSelectionListener(mainController);
		
		// Add some extra tools to the toolbar.
		Macros.installMacroFromJar("panel_sticking_Tool.ijm");
		Macros.installMacroFromJar("insets_Tool.ijm");
		
		// Select the FigureJ Tool
		Toolbar toolbar = Toolbar.getInstance();
		toolbar.setTool(figureJTool.getToolName());
	}

	/**
	 * Code adapted from NeuronJ source code by Erik Meijering.
	 */
	private void checkImageScience() {
		try {
			if (ImageScience.version().compareTo(MINISVERSION) < 0) {
				errorDetails += "* ImageScience version " + MINISVERSION + " or higher is required";
			}
		} catch (NoClassDefFoundError e) {
			errorDetails += "* ImageScience is required";
		}
	}

	private void checkLSMReader() {
		if ((Menus.getCommands().get("LSM Reader") == null)
				&& (Menus.getCommands().get("LSMReader...") == null)) {
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
