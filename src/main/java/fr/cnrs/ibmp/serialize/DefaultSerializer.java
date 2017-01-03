
package fr.cnrs.ibmp.serialize;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.io.OpenDialog;
import ij.plugin.frame.RoiManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import fr.cnrs.ibmp.windows.MainWindow;
import fr.cnrs.ibmp.dataSets.DataSource;

/**
 * The {@link DefaultSerializer} handles the process of storing and re-opening panel
 * trees and the figure that owns such a tree.
 * <p>
 * (c) IBMP-CNRS
 * </p>
 * 
 * @author Edda Zinck
 * @author Jerome Mutterer
 * @author Stefan Helfrich (University of Konstanz)
 */
public class DefaultSerializer implements Serializer, Serializable {

	private static final long serialVersionUID = 1L;

	private final static String serFileExtension = ".figurej";
	private static String roiFileName = "RoiSet.zip";

	@Override
	public MainWindow deserialize() {

		OpenDialog od = new OpenDialog("Select FigureJ file to open", null);
		String directory = od.getDirectory();
		String name = od.getFileName();
		if (name == null) return null;

		String fileName = directory + name;
		// de-serializing of the panel tree and main window
		FileInputStream fis = null;
		ObjectInputStream in = null;

		try {
			fis = new FileInputStream(fileName);
			in = new ObjectInputStream(fis);
			MainWindow mainWindow = (MainWindow) in.readObject();
			in.close();
			List<DataSource> list = mainWindow.getDataSources();

			// if folder containing the source images was moved, adapt the file path
			for (DataSource d : list) {

				if (!d.getFileDirectory().equals("") && !d.getFileDirectory().equals(
					directory)) d.setFileDirectory(directory);
				if ((d.getFileName() == "") || d.getFileName() == null) d
					.setFileDirectory("");
				if ((d.getFileDirectory() == "") || d.getFileDirectory() == null) d
					.setFileName("");
			}
			mainWindow.recover();
			mainWindow.calibrateImage(mainWindow.getDPI(), "cm");

			// display arrows, scale bars and so on
			if (new File(directory + roiFileName).exists()) mainWindow
				.readInOldOveray(directory + roiFileName);
			return mainWindow;
		}
		catch (IOException e) {
			// FIXME Handle exceptions
		}
		catch (ClassNotFoundException e) {
			// FIXME Handle exceptions
		}
		catch (Exception e) {
			// FIXME Handle exceptions
		}

		return null;
	}

	@Override
	public void serialize(MainWindow mainWindow) {
		String path = IJ.getDirectory("Select/Create Figure Output Folder");
		if (path == null) return;
		// IJ.log(path);
		String folderName = path.substring(0, path.length() - 1);
		folderName = folderName.substring(folderName.lastIndexOf(File.separator) +
			1);
		// IJ.log(folderName);
		storeImageFiles(mainWindow, path, folderName);
		mainWindow.draw();
	}

	/**
	 * Stores the result as TIFF file in the indicated directory. Creates a folder
	 * in this directory that has the same name as the TIFF and generates the
	 * serialize file as well as the overlay and copies of every image used to
	 * create the result image in that folder. The file path in the dataSources is
	 * updated.
	 * 
	 * @param mainW result image to serialize
	 * @param dir directory result gets stored in
	 * @param pureFileName result image file name
	 */
	private void storeImageFiles(final MainWindow mainW, final String dir,
		final String pureFileName)
	{
		// TODO Use ImageJ's built-in functionality for saving images
		// save image using the same name as for the serialization file
		// had to move this a little below, as everything is inside the folder now.
		String nameOfNewDir=dir;

		mainW.saveImage(dir,pureFileName);

		// remove the text labels and save the overlay to this folder
		mainW.hideAllLabelsAndScalebars();
		saveOverlay(mainW.getImagePlus(), nameOfNewDir);

		List<DataSource> list = mainW.getDataSources();
		Set<String>	filesFound = new HashSet<String>();

		// save copies of all source images to this folder
		for(DataSource dataSource:list)
		{
			String beforeSavePath = dataSource.getFileDirectory();
			if(dataSource.getFileName() != "" && !dataSource.getFileName().isEmpty())
			{
				File source = new File(dataSource.getFileDirectory()+dataSource.getFileName());
				//if there are files with the same name but stored in different directories, one is renamed 
				//so that both can be stored in the same directory
				for(String s: filesFound)
					if(s.endsWith(dataSource.getFileName()) && !s.equals(dataSource.getFileDirectory()+dataSource.getFileName())) {
						{
							rename(dataSource);
						}
					}

				filesFound.add(dataSource.getFileDirectory()+dataSource.getFileName());

				File duplicate = new File(nameOfNewDir+dataSource.getFileName());
				try {
					copyFile(source, duplicate);
					dataSource.setFileDirectory(nameOfNewDir);
				}
				catch (IOException e) {
					IJ.error("couldn't store "+dataSource.getFileName());
				}
			}
			if(dataSource.getExternalSource() != ""){
				//String tempDir = System.getProperty("java.io.tmpdir");
				File source = new File(beforeSavePath+dataSource.getExternalSource());
				File duplicate = new File(nameOfNewDir+dataSource.getExternalSource());
				try {
					// IJ.log(source.toString());
					// IJ.log(duplicate.toString());
					copyFile(source, duplicate);
					dataSource.setFileDirectory(nameOfNewDir);
				}
				catch (IOException e) {
					IJ.error("couldn't store "+dataSource.getExternalSource());
				}
			}

		}

		// serialize the panel tree and the main window and store it in the new folder
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
			fos = new FileOutputStream(nameOfNewDir+pureFileName+serFileExtension);   
			out = new ObjectOutputStream(fos);
			out.writeObject(mainW);
			out.close();
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}

		// store a text file with the image notes in the new folder
		saveImageNotes(nameOfNewDir, mainW);
	}

	/**
	 * Adds an underscore and 2 random letters and a number to the file name of
	 * the data source
	 * 
	 * @param d data source which has an image that should be renamed
	 */
	private void rename(DataSource d) {
		// TODO What is this doing and why?
		Random r = new Random();
		String name = d.getFileName();
		String ending = name.substring(name.lastIndexOf("."), name.length());
		name = name.replace(ending, "");
		//add three random numbers as chars; 48 is the ASCII value of 0 
		d.setFileName(name+"_"+(char)(r.nextInt(25)+97)+(char)(r.nextInt(10)+48)+(char)(r.nextInt(25)+97)+ending);
	}

	/**
	 * Save {@link Overlay} of an image as ZIP file.
	 * 
	 * @param resultFigure image of which the overlay has to be stored
	 * @param dir directory the overlay is stored to
	 */
	private void saveOverlay(ImagePlus resultFigure, String dir) {
		Overlay overlay = resultFigure.getOverlay();
		if(overlay==null || overlay.size()==0)
			return;
	
		// FIXME Fails if there were Rois in the RoiManager before
		RoiManager roiManager = new RoiManager(false);
		for(int i=0; i<overlay.size(); i++)
			roiManager.addRoi(overlay.get(i));
		roiManager.runCommand("Save", dir+roiFileName );
	}

	/**
	 * Write all image notes/details to a text file.
	 * 
	 * @param path directory the file is stored in
	 * @param mainW result window containing the images
	 */
	private void saveImageNotes(String path, MainWindow mainW) {
		String s = mainW.getAllImageNotes();
		if (s != null) {
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(path +
					"imageNotes.txt"));
				out.write(s);
				out.close();
			}
			catch (IOException e) {
				IJ.error("could not store the image notes");
			}
		}
	}

	/**
	 * If a file with the same name as destFile exists in the target directory,
	 * the method does nothing.
	 * <p>
	 * <a href="http://stackoverflow.com/questions/106770/standard-concise-way-to-copy-a-
	 * file-in-java">http://stackoverflow.com/questions/106770/standard-concise-way-to-copy-a-
	 * file-in-java</a>
	 * </p>
	 * 
	 * @param sourceFile file which's content is going to be copied
	 * @param destFile file the content is copied to
	 * @throws IOException
	 */
	private void copyFile(File sourceFile, File destFile) throws IOException {
		if(!destFile.exists()) {
			destFile.createNewFile();
		}
		else 
			return;

		FileChannel source = null;
		FileChannel destination = null;

		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		}
		finally {
			if(source != null) {
				source.close();
			}
			if(destination != null) {
				destination.close();
			}
		}
	}
}
