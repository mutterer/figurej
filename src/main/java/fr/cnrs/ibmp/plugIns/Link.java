
package fr.cnrs.ibmp.plugIns;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.measure.Calibration;
import ij.plugin.Macro_Runner;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Calendar;

import javax.swing.JFrame;

import fr.cnrs.ibmp.treeMap.LeafPanel;
import fr.cnrs.ibmp.treeMap.Panel;
import fr.cnrs.ibmp.windows.MainWindow;
import fr.cnrs.ibmp.dataSets.DataSource;
import fr.cnrs.ibmp.dataSets.ExternalDataSource;

/**
 * This interface is designed for classes that control external software
 * creating images to file to fill the active panel with.
 * <p>
 * File name organization and linking of path and name to the {@link DataSource}
 * is specified in this interface.
 * </p>
 * 
 * @author Jerome Mutterer
 * @author Edda Zinck
 */
public abstract class Link extends JFrame implements LinkInterface {

	private static final long serialVersionUID = 1L;
	private static final String s = "Linking to external software";

	protected final String extension = ".png";
	protected String fileName = "defName.png";
	protected String linkIdentifier = "defaultLink";
	protected static int counter = 0;

	private MainWindow mainW;
	protected LeafPanel leafPanel;
	private ExternalDataSource dataSource;

	public Link() {
		super(s);
	}

	public Link(MainWindow m) {
		super(s);
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		setBackground(new Color(0x99aabb));
		mainW = m;
		updateActivePanel();
	}

	/* *
	 * //** add info about external source file, such as .py chimera session,
	 * .svg inkscape, etc.
	 *//*
			* protected void setSource(String source) { if(leafPanel !=null) {
			* dataSource.setExternalSource(source); } }
			*/
	/**
	 * copy image to the result figure and set its path and name in the data
	 * source object so that it can be opened with the 'image open' button
	 */
	protected void store(String tempDir, String fileName) {
		// TODO replace either with java code or run macr file from jar.
		// IJ.runMacro("id=getImageID;setBatchMode(1);open('" + tempDir +
		// fileName +
		// "');w=call('ij.Prefs.get','selectedPanel.w',100);h=call('ij.Prefs.get','selectedPanel.h',100);run('Size...',
		// 'width=&w height=&h'); run('Copy');selectImage(id);run('Paste');");
		// runMyMacroFromJar("pasteImgFromFile.ijm", tempDir + fileName);
		ImagePlus imp = IJ.openImage(tempDir + fileName);
		if ((imp.getWidth() == leafPanel.getW()) && (imp.getHeight() == leafPanel
			.getH()))
		{
			Calibration cal = imp.getCalibration();
			imp.copy(false);
			IJ.run("Paste");
			if (leafPanel != null) {
				leafPanel.setPixels(mainW.getImagePlus());

				if (dataSource != null) {
					dataSource.setFileDirectory(tempDir);
					dataSource.setFileName(fileName);
					dataSource.setPixelCalibration(cal.pixelWidth, cal.getUnit());
				}
			}
		}
		else {
			IJ.error("Panel dimension do not match");
		}
	}

	/**
	 * If the currently active panel has not yet been filled with an image from a
	 * link software (R, Excel, PyMOL), a new name is created for the image
	 * produced by the software, else the old name is reused.
	 * 
	 * @param tempDir
	 */
	protected void checkFileName(String tempDir) {
		if (leafPanel != null) {
			if (!dataSource.getFileDirectory().equals(tempDir)) {
				updateFileName(linkIdentifier, extension);
				return;
			}

			String currName = dataSource.getFileName();
			if ((!currName.startsWith(linkIdentifier) || (!currName.endsWith(
				extension))))
			{
				updateFileName(linkIdentifier, extension);
				return;
			}
		}

		fileName = dataSource.getFileName();
	}

	/**
	 * TODO Documentation
	 * 
	 * @param fileNameIdentifier
	 * @param extension
	 */
	private void updateFileName(String fileNameIdentifier, String extension) {
		counter++;
		fileName = fileNameIdentifier + counter + extension;
	}

	/**
	 * TODO Documentation
	 */
	protected void updateActivePanel() {
		Panel p = mainW.getSelectedPanel();
		if (p instanceof LeafPanel) {
			leafPanel = (LeafPanel) p;
			dataSource = (ExternalDataSource) leafPanel.getImgData();
		}

		double boundsWidth = IJ.getImage().getRoi().getBounds().getWidth();
		double boundsHeight = IJ.getImage().getRoi().getBounds().getHeight();
		Prefs.set("selectedPanel.w", boundsWidth);
		Prefs.set("selectedPanel.h", boundsHeight);
	}

	/**
	 * TODO Documentation
	 * 
	 * @return
	 */
	protected Panel getActivePanel() {
		Panel p = mainW.getSelectedPanel();
		return p;
	}

	/**
	 * TODO Documentation
	 * 
	 * @return
	 */
	public String timeStamp() {
		Calendar date = Calendar.getInstance();
		String stamp = Integer.toString(date.get(Calendar.YEAR)) + IJ.pad(date.get(
			Calendar.MONTH), 2) + IJ.pad(date.get(Calendar.DAY_OF_MONTH), 2) + IJ.pad(
				date.get(Calendar.HOUR_OF_DAY), 2) + IJ.pad(date.get(Calendar.MINUTE),
					2) + IJ.pad(date.get(Calendar.SECOND), 2);
		return stamp;
	}

	/**
	 * TODO Documentation
	 * 
	 * @param path
	 * @return
	 */
	public String getTextFromJar(String path) {
		String text = "";
		try {
			// get the text resource as a stream
			ClassLoader pcl = getClass().getClassLoader();
			InputStream is = pcl.getResourceAsStream(path);
			if (is == null) {
				IJ.showMessage("FigureJ Link error", "File not found in JAR at " +
					path);
				return "";
			}
			InputStreamReader isr = new InputStreamReader(is);
			StringBuffer sb = new StringBuffer();
			char[] b = new char[8192];
			int n;
			// read a block and append any characters
			while ((n = isr.read(b)) > 0)
				sb.append(b, 0, n);
			// display the text in a TextWindow
			text = sb.toString();
		}
		catch (IOException e) {
			String msg = e.getMessage();
			if (msg == null || msg.equals("")) msg = "" + e;
			IJ.showMessage("FigureJ Link error", msg);
		}
		return text;
	}

	/**
	 * TODO Documentation
	 * 
	 * @param name
	 * @param arg
	 */
	public static void runMyMacroFromJar(String name, String arg) {
		String macro = null;
		try {
			ClassLoader pcl = IJ.getClassLoader();
			InputStream is = pcl.getResourceAsStream("macros/" + name);
			if (is == null) {
				IJ.error("FigureJ installMacroFromJar", "Unable to load \"" + name +
					"\" from jar file");
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
		}
		catch (IOException e) {
			IJ.error("FigureJ runMacroFromJar", "" + e);
		}
		if (macro != null) {
			Macro_Runner mr = new Macro_Runner();
			mr.runMacro(macro, arg);
		}
	}

	/**
	 * Writes text to a file located at the specified absolute path.
	 * 
	 * @param filePath Absolute path to the file to which the content is written.
	 * @param text {@link String} representation of the text to be written to the
	 *          file.
	 */
	public static void writeToFile(String filePath, String text) {
		File file = new File(filePath);
		if (file.exists()) {
			IJ.error("FigureJ Link: File exists error");
		}

		PrintWriter writer;
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(filePath);
			writer = new PrintWriter(new BufferedOutputStream(fos));
			writer.print(text);
			writer.flush();
			writer.close();
			fos.close();
		}
		catch (IOException e) {
			IJ.error("FigureJ file creation error \n\"" + e.getMessage() + "\"\n");
			return;
		}
	}

}
