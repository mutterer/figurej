import ij.*;
import ij.gui.*;
import ij.plugin.*;

public class figure_j implements PlugIn {

	public void run(String arg) {
		if (IJ.getVersion().compareTo("1.47o")<0) {
			IJ.error("ImageJ 1.47o or later is required to run FigureJ");
			return;
		}
		Toolbar.removeMacroTools();
		Toolbar.addPlugInTool(new FigureJ_Tool());
	}
}
