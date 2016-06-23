package fr.cnrs.ibmp.utilities;

import ij.IJ;
import ij.plugin.PlugIn;

/**
 * Small helper plugin to open the FigureJ website.
 * 
 * @author Stefan Helfrich (University of Konstanz)
 */
public class WebsiteOpener implements PlugIn {

	@Override
	public void run(String arg) {
		IJ.run("URL...", "url=http://imagejdocu.tudor.lu/doku.php?id=plugin:utilities:figurej:start");
	}

}
