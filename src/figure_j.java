import ij.*;
import ij.gui.*;
import ij.plugin.*;
import imagescience.ImageScience;

public class figure_j implements PlugIn {
	private final static String MINISVERSION = "3.0.0";

	public void run(String arg) {
		String errorDetails = "";
		if (IJ.getVersion().compareTo("1.48") < 0) {
			errorDetails += "* ImageJ 1.48 or later is required\n";
		}
		if (Menus.getCommands().get("Bio-Formats Importer") == null) {
			errorDetails += "* Bioformats is required\n";
		}
		if (Menus.getCommands().get("Show LSMToolbox") == null) {
			errorDetails += "* LSMToolbox is required\n";
		}
		if (Menus.getCommands().get("LSM Reader") == null) {
			errorDetails += "* LSM Reader is required\n";
		}
		// this was taken from NeuronJ source code by Erik Meijering
		try {
			if (ImageScience.version().compareTo(MINISVERSION) < 0)
				throw new IllegalStateException();
		} catch (Throwable e) {
			errorDetails += "* ImageScience version " + MINISVERSION + " or higher is required";
		}
		if (errorDetails != "") {
			IJ.error("FigureJ", "Some libraries are missing:\n \n"+errorDetails);
			return;
		} else {
			Toolbar.addPlugInTool(new FigureJ_Tool());
		}
	}
}
