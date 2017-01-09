package fr.cnrs.ibmp.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import ij.IJ;
import ij.plugin.MacroInstaller;

/**
 * Utility class for working with macros.
 * 
 * TODO Authorship
 */
public class Macros {

	public static void installMacroFromJar(final String filename) {
		String macro = null;
		try {
			ClassLoader pcl = IJ.getClassLoader();
			InputStream is = pcl.getResourceAsStream("macros/" + filename);
			if (is == null) {
				IJ.error("FigureJ installMacroFromJar", "Unable to load \""
						+ filename + "\" from jar file");
				return;
			}
			InputStreamReader isr = new InputStreamReader(is);
			StringBuffer sb = new StringBuffer();
			char[] b = new char[8192];
			int n;
			while ((n = isr.read(b)) > 0)
				sb.append(b, 0, n);
			macro = sb.toString();
			is.close();
		} catch (IOException e) {
			IJ.error("FigureJ installMacroFromJar", "" + e);
		}
		if (macro != null)
			(new MacroInstaller()).installSingleTool(macro);
	}

}
