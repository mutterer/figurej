
package fr.cnrs.ibmp.windows;

import ij.Prefs;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

/**
 * A dialog that allows the user to set preferences, e.g. paths to external
 * tools. The preferences are stored persistently, i.e. they are available after
 * closing and opening ImageJ.
 * 
 * @author Stefan Helfrich (University of Konstanz)
 */
public class PreferencesDialog implements PlugIn {

	private GenericDialog gd;

	/**
	 * Constructs a {@link PreferencesDialog}.
	 */
	protected PreferencesDialog() {
		gd = new GenericDialog("FigureJ Preferences");
		gd.addCheckbox("Enable External tools", Prefs.get("figurej.externalTools",
			false));
		gd.addStringField("Path to USCF Chimera", Prefs.get("figurej.chimeraPath",
			"not set"), 60);
		gd.addStringField("Path to Inkscape", Prefs.get("figurej.inkscapePath",
			"not set"), 60);
		gd.addCheckbox("Allow Slicing Panels by Dragging the Mouse", Prefs.get(
			"figurej.mousePanelSlicing", false));
	}

	@Override
	public void run(String arg) {
		gd.showDialog();

		if (!gd.wasCanceled()) {
			Prefs.set("figurej.externalTools", gd.getNextBoolean());
			Prefs.set("figurej.chimeraPath", gd.getNextString());
			Prefs.set("figurej.inkscapePath", gd.getNextString());
			Prefs.set("figurej.mousePanelSlicing", gd.getNextBoolean());
		}
	}

}
