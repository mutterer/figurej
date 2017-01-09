
package fr.cnrs.ibmp.dataSets;

import java.io.File;
import java.io.Serializable;

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
	private int selectedSeries;

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

}
