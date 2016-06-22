package fr.cnrs.ibmp;

import ij.Prefs;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich (University of Konstanz)
 */
public class PreferencesDialog implements PlugIn {

	private GenericDialog gd = new GenericDialog("FigureJ Preferences");
	
	@Override
	public void run(String arg) {
		gd.addCheckbox("Enable External tools", Prefs.get("figurej.externalTools", false));
		gd.addStringField("Path to USCF Chimera", Prefs.get("figurej.chimeraPath", "not set"), 60);
		gd.addStringField("Path to Inkscape", Prefs.get("figurej.inkscapePath", "not set"), 60);
		gd.addCheckbox("Allow Slicing Panels by Dragging the Mouse", Prefs.get("figurej.mousePanelSlicing",false));
		gd.showDialog();
		
		if (!gd.wasCanceled()) {
			Prefs.set("figurej.externalTools", gd.getNextBoolean());
			Prefs.set("figurej.chimeraPath", gd.getNextString());
			Prefs.set("figurej.inkscapePath", gd.getNextString());
			Prefs.set("figurej.mousePanelSlicing", gd.getNextBoolean());
		}
	}

}
