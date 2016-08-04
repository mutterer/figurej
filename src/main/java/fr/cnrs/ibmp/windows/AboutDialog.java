
package fr.cnrs.ibmp.windows;

import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

/**
 * Dialog that displays credits and a copyright message. Additionally, it
 * contains a Help button that takes the user to the FigureJ website.
 * 
 * @author Stefan Helfrich (University of Konstanz)
 */
public class AboutDialog implements PlugIn {

	private GenericDialog gd;

	/**
	 * Constructs an {@link AboutDialog}.
	 */
	protected AboutDialog() {
		gd = new GenericDialog("About FigureJ");
		gd.addMessage(
			"Easy article figures with FigureJ\n \nJerome Mutterer & Edda Zinck\nCNRS, 2012 - 2016.");
		gd.addMessage("Stefan Helfrich\nUniversity of Konstanz, 2016.");
		gd.addMessage(
			"\nClick 'Help' to proceed to FigureJ homepage with tutorial.");
		String url =
			"http://imagejdocu.tudor.lu/doku.php?id=plugin:utilities:figurej:start";
		gd.addHelp(url);
	}

	@Override
	public void run(String arg) {
		gd.showDialog();
	}

}
