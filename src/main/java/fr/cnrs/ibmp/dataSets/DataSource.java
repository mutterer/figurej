// TODO Missing license header

package fr.cnrs.ibmp.dataSets;

import fr.cnrs.ibmp.DataSourceEvent;
import fr.cnrs.ibmp.DataSourceListener;
import fr.cnrs.ibmp.treeMap.LeafPanel;

/**
 * Interface for data sources that can provide images to be shown in a figure.
 * <p>
 * Information storage for individual panels of an image. This includes in
 * particular the origin of the image (e.g. file) and the plane from which the
 * panel has been created. Additionally, this class saves operations that have
 * been applied to the source image after loading as an ImageJ1 macro.
 * </p>
 * <p>
 * (c) IBMP-CNRS
 * </p>
 * 
 * @author Edda Zinck
 * @author Jerome Mutterer
 * @author Stefan Helfrich (University of Konstanz)
 */
public interface DataSource {

	/**
	 * Clears the contents of a data source.
	 * 
	 * @deprecated Reusing {@link DataSource}s is discouraged.
	 */
	@Deprecated
	public void clear();

	/**
	 * Indicator if the {@link DataSource} links to a file.
	 * 
	 * @return {@code true} if panel content links to file, otherwise
	 *         {@code false}.
	 */
	public boolean fromFile();

	@Override
	public String toString();

	/**
	 * @return pixel width in calibrated units.
	 */
	public double getPixelWidth();

	/**
	 * @return {@code String} representation of calibration unit.
	 */
	public String getUnit();

	/**
	 * @return short {@code String} representation used for logging.
	 */
	public String getStringRepresentation();

	/**
	 * Sets the label of the {@link LeafPanel} associated with {@code this}.
	 * 
	 * @param label {@code String} representation of the label associated with
	 *          {@code this}.
	 */
	public void setLabel(String label);

	/**
	 * Sets the scalebar length in calibrated units. Call with {@code -1} to
	 * disable scalebar.
	 * 
	 * @param l scalebar length in calibrated units.
	 */
	public void setScalebarLength(double l);

	/**
	 * @return notes written to the panels's notes field on the GUI or empty
	 *         string
	 */
	public String getNotes();

	/**
	 * @param text notes on the image (e.g. from the GUI notes field)
	 */
	public void setNotes(String text);

	/**
	 * TODO Documentation
	 * @return
	 */
	public Object open();

	/**
	 * TODO Documentation
	 * @param listener
	 */
	public void removeListener(DataSourceListener listener);

	/**
	 * TODO Documentation
	 * @param listener
	 */
	public void addListener(DataSourceListener listener);

	/**
	 * TODO Documentation
	 */
	public void invalidateCoordinates();

}
