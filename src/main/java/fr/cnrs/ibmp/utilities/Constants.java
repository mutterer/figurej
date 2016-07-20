// TODO Missing license header

package fr.cnrs.ibmp.utilities;

import java.awt.Color;

/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich
 */
public abstract class Constants {

	public static final int guiBorder = 5;
	public static final Color backgroundColor = new Color(0x99aabb);

	public static final String title = "FigureJ";
	
	public static String version() {
		String version = null;
		final Package versionPackage = Constants.class.getPackage();
		if (versionPackage != null) {
			version = versionPackage.getImplementationVersion();
		}
		return version == null ? "DEV" : version;
	}

}
