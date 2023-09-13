import java.awt.Window;

import ij.*;
import ij.gui.*;
import ij.plugin.*;
import imagescience.ImageScience;

public class FigureJ2_ implements PlugIn {
	
	public static String version() {
		return "1.40";
	}

	private final static String IMAGESCIENCEMINISVERSION = "3.0.0";

	public void run(String arg) {
		String errorDetails = "";
		if (IJ.getVersion().compareTo("1.53c") < 0) {
			errorDetails += "* ImageJ 1.53c or later is required\n";
			errorDetails += "-> Use the Help>Update ImageJ... command.\n";
		}
		if (Menus.getCommands().get("Bio-Formats Importer") == null) {
			errorDetails += "* Bioformats is required\n";
		}
		// if (Menus.getCommands().get("Show LSMToolbox") == null) {
		// 	errorDetails += "* LSMToolbox is required\n";
		// }
		if ((Menus.getCommands().get("LSM Reader") == null)&& (Menus.getCommands().get("LSM...") == null)){
			errorDetails += "* LSM Reader is required\n";
		}
		// this was copied from NeuronJ source code by Erik Meijering
		try {
			if (ImageScience.version().compareTo(IMAGESCIENCEMINISVERSION) < 0)
				throw new IllegalStateException();
		} catch (Throwable e) {
			errorDetails += "* ImageScience version " + IMAGESCIENCEMINISVERSION + " or higher is required";
		}
		if (errorDetails != "") {
			IJ.error("FigureJ", "Some requirements are not met:\n \n"+errorDetails);
			return;
		} else {

			Window win = WindowManager.getWindow("FigureJ " + FigureJ2_.version());
			if (win==null) {
				Toolbar.removeMacroTools();
				Toolbar.addPlugInTool(new PanelSelection_Tool());
			} else {
				win.toFront();
			}
		}
	}
}
