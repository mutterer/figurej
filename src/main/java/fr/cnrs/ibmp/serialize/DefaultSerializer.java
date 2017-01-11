
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
import fr.cnrs.ibmp.dataSets.ExternalDataSource;
import fr.cnrs.ibmp.dataSets.FileDataSource;

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
	private final static String roiFileName = "RoiSet.zip";

	@Override
	public MainWindow deserialize() {
		// TODO Move this to another class?
		OpenDialog od = new OpenDialog("Select FigureJ file to open", null);
		String directory = od.getDirectory();
		String name = od.getFileName();
		if (name == null) {
			return null;
		}

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
			// TODO Move to FileDataSource
			for (DataSource d : list) {
				if (d instanceof FileDataSource) {
					FileDataSource fileDataSource = (FileDataSource) d;
					if (!fileDataSource.getFileDirectory().equals("") && !fileDataSource
						.getFileDirectory().equals(directory))
					{
						fileDataSource.setFileDirectory(directory);
					}
					if ((fileDataSource.getFileName().equals("")) || fileDataSource
						.getFileName() == null)
					{
						fileDataSource.setFileDirectory("");
					}
					if ((fileDataSource.getFileDirectory().equals("")) || fileDataSource
						.getFileDirectory() == null)
					{
						fileDataSource.setFileName("");
					}
				}
			}

			mainWindow.recover();
			mainWindow.calibrateImage(mainWindow.getDPI(), "cm");

			// display arrows, scale bars and so on
			File roiFile = new File(directory + roiFileName);
			if (roiFile.exists()) {
				mainWindow.readInOldOveray(roiFile);
			}

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
	 * create the result image in that folder.
	 * <p>
	 * <em>NB: The file paths in the {@link DataSource}s are updated to point to
	 * copies in the output folder.</em>
	 * </p>
	 * 
	 * @param mainW result image to serialize
	 * @param outputDirectory directory result gets stored in
	 * @param pureFileName result image file name
	 */
	private void storeImageFiles(final MainWindow mainW, final String outputDirectory,
		final String pureFileName)
	{
		// Save image using the same name as for the serialization file
		mainW.saveImage(outputDirectory, pureFileName);

		// Remove the text labels and save the overlay to the output folder
		mainW.hideAllLabelsAndScalebars();
		saveOverlay(mainW.getImagePlus(), outputDirectory);

		List<DataSource> list = mainW.getDataSources();
		Set<String> filesFound = new HashSet<String>();

		// Save copies of all source images to the output folder
		for(DataSource dataSource : list) {
			if (dataSource instanceof FileDataSource) {
				FileDataSource fileDataSource = (FileDataSource) dataSource;
				String filePath = fileDataSource.getFileDirectory()+fileDataSource.getFileName();

				if (fileDataSource.isValid()) {
					File source = new File(filePath);

					/* Avoid name clashes from source files: sources can have the same file
					 * name in different directories. However, attempting to copy them to
					 * the output folder results in duplicate file names. To avoid this
					 * situation, duplicate file names are remedied by renaming. */
					// TODO Improve to use contains
					for (String s : filesFound) {
						if (s.endsWith(fileDataSource.getFileName()) && !s.equals(filePath)) {
							rename(fileDataSource);

							// Update filePath
							filePath = fileDataSource.getFileDirectory()+fileDataSource.getFileName();
						}
					}

					filesFound.add(filePath);
	
					File duplicate = new File(outputDirectory+fileDataSource.getFileName());
					try {
						copyFile(source, duplicate);
						fileDataSource.setFileDirectory(outputDirectory);
					}
					catch (IOException e) {
						IJ.error("couldn't store "+fileDataSource.getFileName());
					}
				}

				if (dataSource instanceof ExternalDataSource) {
					ExternalDataSource externalDataSource = (ExternalDataSource) dataSource;

					// TODO Avoid name clashes like before
					File source = new File(externalDataSource.getFileDirectory()+externalDataSource.getExternalSource());
					File duplicate = new File(outputDirectory+externalDataSource.getExternalSource());
					try {
						copyFile(source, duplicate);
						externalDataSource.setFileDirectory(outputDirectory);
					} catch (IOException e) {
						IJ.error("couldn't store "+externalDataSource.getExternalSource());
					}
				}
			}
		}

		// serialize the panel tree and the main window and store it in the output folder
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
			fos = new FileOutputStream(outputDirectory+pureFileName+serFileExtension);
			out = new ObjectOutputStream(fos);
			out.writeObject(mainW);
			out.flush();
			out.close();
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}

		// store a text file with the image notes in the output folder
		saveImageNotes(outputDirectory, mainW);
	}

	/**
	 * Adds an underscore and 2 random letters and a number to the file name of
	 * the data source
	 * 
	 * @param d data source which has an image that should be renamed
	 */
	private void rename(FileDataSource d) {
		Random r = new Random();
		String fileName = d.getFileName();
		String cleanedFileName = removeFileExtension(fileName);
		String fileExtension = getFileExtension(fileName);

		// add three random numbers as chars; 48 is the ASCII value of 0 
		d.setFileName(cleanedFileName + "_" + (char) (r.nextInt(25) + 97) +
			(char) (r.nextInt(10) + 48) + (char) (r.nextInt(25) + 97) +
			fileExtension);
	}

	/**
	 * Returns the file extension from a file name.
	 * 
	 * @param fileName {@link String} representation of a file name
	 * @return the file extension extracted from {@code fileName}.
	 */
	private String getFileExtension(String fileName) {
		return fileName.substring(fileName.lastIndexOf("."));
	}

	/**
	 * Removes the file extension from a file name.
	 * 
	 * @param fileName {@link String} representation of a file name
	 * @return a file name {@link String} with the file extension removed.
	 */
	private String removeFileExtension(final String fileName) {
		return fileName.substring(0, fileName.lastIndexOf("."));
	}

	/**
	 * Save {@link Overlay} of an image as ZIP file.
	 * 
	 * @param resultFigure image of which the overlay has to be stored
	 * @param dir directory the overlay is stored to
	 */
	private void saveOverlay(ImagePlus resultFigure, String dir) {
		Overlay overlay = resultFigure.getOverlay();
		if(overlay==null || overlay.size()==0) {
			return;
		}
	
		// FIXME Fails if there were Rois in the RoiManager before
		RoiManager roiManager = new RoiManager(false);
		for(int i=0; i<overlay.size(); i++) {
			roiManager.addRoi(overlay.get(i));
		}
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
				FileWriter fileWriter = new FileWriter(path + "imageNotes.txt");
				BufferedWriter out = new BufferedWriter(fileWriter);
				out.write(s);
				out.flush();
				out.close();
			} catch (IOException e) {
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
	public static void copyFile(File sourceFile, File destFile)
		throws IOException
	{
		if (!destFile.exists()) {
			destFile.createNewFile();
		}
		FileInputStream fIn = null;
		FileOutputStream fOut = null;
		FileChannel source = null;
		FileChannel destination = null;
		try {
			fIn = new FileInputStream(sourceFile);
			source = fIn.getChannel();
			fOut = new FileOutputStream(destFile);
			destination = fOut.getChannel();
			long transfered = 0;
			long bytes = source.size();
			while (transfered < bytes) {
				transfered += destination.transferFrom(source, 0, source.size());
				destination.position(transfered);
			}
		}
		finally {
			if (source != null) {
				source.close();
			}
			else if (fIn != null) {
				fIn.close();
			}
			if (destination != null) {
				destination.close();
			}
			else if (fOut != null) {
				fOut.close();
			}
		}
	}

}
