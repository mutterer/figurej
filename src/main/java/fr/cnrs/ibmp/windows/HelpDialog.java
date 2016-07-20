package fr.cnrs.ibmp.windows;

import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich (University of Konstanz)
 */
public class HelpDialog implements PlugIn {

	private GenericDialog gd;
	
	@Override
	public void run(String arg) {
		// imagej macro that displays a credits message and takes you to fj website.
		String url = "http://imagejdocu.tudor.lu/doku.php?id=plugin:utilities:figurej:start";
		
		gd = new GenericDialog("About FigureJ");
		gd.addMessage("Easy article figures with FigureJ\n \nJerome Mutterer & Edda Zinck\nCNRS, 2016.");
		gd.addMessage("\nClick 'Help' to proceed to FigureJ homepage with tutorial.");
		gd.addHelp(url);
		
		gd.showDialog();
	}

}
