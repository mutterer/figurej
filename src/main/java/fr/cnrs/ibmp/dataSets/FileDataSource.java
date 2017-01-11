
package fr.cnrs.ibmp.dataSets;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import ij.IJ;
import ij.ImagePlus;
import loci.formats.FormatException;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;

/**
 * File-based {@link DataSource}.
 * 
 * @author Stefan Helfrich
 */
public class FileDataSource extends ImageDataSource implements Serializable {

	private static final long serialVersionUID = 1L;

	// TODO Use path instead?s
	// image file location
	private String fileDirectory = "";
	private String fileName = "";

	/** selected series from multi-series file formats (e.g. *.lif files). */
	private int selectedSeries = -1;

	/**
	 * Creates a {@link FileDataSource} from default values.
	 */
	public FileDataSource() {
		super();
	}

	public FileDataSource(final FileDataSource dataSource) {
		super(dataSource);

		this.fileDirectory = dataSource.fileDirectory;
		this.fileName = dataSource.fileName;
	}

	/**
	 * @return the {@link #selectedSeries}.
	 */
	public int getSelectedSeries() {
		return selectedSeries;
	}

	/**
	 * @return directory the image serving as source for the panel this dataSource
	 *         belongs to is saved in directory changes to the one the serialized
	 *         file is stored in upon first hit of the "save" button
	 */
	public String getFileDirectory() {
		return fileDirectory;
	}

	/**
	 * @return name of the image serving as source for the panel this dataSource
	 *         belongs to name eventually changes upon first hit of the "save"
	 *         button, if an image with same name and different directory is used
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * @param filePath path of the image chosen to fill the panel this data source
	 *          belongs to with
	 */
	public void setFileDirectory(String filePath) {
		if (!filePath.isEmpty() && !filePath.endsWith(File.separator)) filePath +=
			File.separator;
		this.fileDirectory = filePath;

		notifyListeners();
	}

	/**
	 * @param fileName of the image chosen to fill the panel this data source
	 *          belongs to with (without path!)
	 */
	public void setFileName(final String fileName) {
		this.fileName = fileName;

		notifyListeners();
	}

	/**
	 * TODO Documentation
	 * 
	 * @param selectedSeries
	 */
	public void setSelectedSeries(final int selectedSeries) {
		this.selectedSeries = selectedSeries;
	}

	/**
	 * TODO Documentation
	 */
	@Override
	public void clear() {
		super.clear();

		setFileDirectory("");
		setFileName("");
	}

	@Override
	public boolean fromFile() {
		return true;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "Image Datasource: " + fileName + "\n";
		s += "Original folder: " + fileDirectory + "\n";
		s += "-------------------------\n";

		return s;
	}

	@Override
	public String getStringRepresentation() {
		return getFileDirectory() + "," + getFileName();
	}

	@Override
	public Object open() {
		ImagePlus openedImage;
		
		String path = getFileDirectory() + getFileName();

		// TODO Don't use Bio-Formats per default (due to issues with standard TIFF files)
		// TODO Split up into several try/catch blocks
		try {
			// Set some sensible default options
			ImporterOptions options = getDefaultImporterOptions();
			options.setId(path);

			// Configure/prepare the import process
			ImportProcess process = new ImportProcess(options);
			process.execute();

			// Get the options that have been used in the end
			options = process.getOptions();

			if (getSelectedSeries() == -1) {
				// Get selected series with the highest index
				int highestSelectedSeriesIndex = getHighestSelectedSeriesIndex(process);

				// Store information in DataSource
				setSelectedSeries(highestSelectedSeriesIndex);

				if (IJ.debugMode) {
					IJ.log("" + highestSelectedSeriesIndex);
				}
			} else {
				// Try to load the series with the highest previously selected index
				options.clearSeries();
				options.setSeriesOn(getSelectedSeries(), true);
			}

			// Open the selected series with the highest index
			ImagePlusReader reader = new ImagePlusReader(process);
			ImagePlus[] imps = reader.openImagePlus();
			openedImage = imps[imps.length - 1];
		} catch (FormatException e) {
			// TODO Make a multi-catch with switch to Java 7/8
			e.printStackTrace();
			IJ.log("Bioformats had problems reading this file.");
//			handleImageOpenFailure();
			return null;
		} catch (IOException e) {
		// TODO Make a multi-catch with switch to Java 7/8
			e.printStackTrace();
			IJ.log("Bioformats had problems reading this file.");
//			handleImageOpenFailure();
			return null;
		}

		openedImage.setPosition(getChannel(), getSlice(), getFrame());

		// Activate the channels that were open when storing an image
		if (openedImage.isComposite()) {
			openedImage.setDisplayMode(IJ.COMPOSITE); // TODO Required?
			if (getActChs() == "") {
				openedImage.setActiveChannels("11111111");
			} else {
				openedImage.setActiveChannels(getActChs());
			}
		}

		return openedImage;
	}

	/**
	 * Composes an {@link ImporterOptions} instance that contains sensible default
	 * options for opening images with Bio-Formats.
	 * <p>
	 * Images are set to be opened as hyperstack. Also, the Bio-Formats upgrade
	 * check is disabled.
	 * </p>
	 * 
	 * @return An {@link ImporterOptions} instance with default settings
	 * @throws IOException If there is an issue with opening the default options
	 *           from file
	 */
	private ImporterOptions getDefaultImporterOptions() throws IOException {
		ImporterOptions options = new ImporterOptions();
		options.setFirstTime(false);
		options.setUpgradeCheck(false);
		options.setStackFormat(ImporterOptions.VIEW_HYPERSTACK);
		return options;
	}

	/**
	 * TODO Documentation
	 * 
	 * @param process
	 * @return
	 */
	private int getHighestSelectedSeriesIndex(final ImportProcess process) {
		int seriesCount = process.getSeriesCount();
		int highestSelectedSeriesIndex = 0;
		for (int i = 0; i < seriesCount; i++) {
			if (process.getOptions().isSeriesOn(i)) {
				highestSelectedSeriesIndex = i;
			}
		}
		return highestSelectedSeriesIndex;
	}

	public boolean isValid() {
		return isFilenameValid() && isFolderValid();
	}
	
	public boolean isFilenameValid() {
		return !getFileName().equals("") && !getFileName().isEmpty();
	}
	
	public boolean isFolderValid() {
		return !getFileDirectory().equals("") && !getFileDirectory().isEmpty();
	}
}
