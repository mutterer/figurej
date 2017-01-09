
package fr.cnrs.ibmp.dataSets;

import javax.swing.event.EventListenerList;

import fr.cnrs.ibmp.DataSourceEvent;
import fr.cnrs.ibmp.DataSourceListener;

/**
 * Abstract super class for {@link DataSourceInterface} implementations.
 * 
 * @author Stefan Helfrich (University of Konstanz)
 */
public abstract class AbstractDataSource implements DataSourceInterface {

	// notes stored in a text file when the figure is saved
	private String notes = "";
	public final String defaultNote = "<my image notes>";

	//calibration info
	private double pixelWidth = 1.0;
	private String calibrationUnit = "pixel";

	// label info
	private String label = "";
	private String scalebarInfo = "";
	
	private EventListenerList listeners = new EventListenerList();

	public AbstractDataSource(final AbstractDataSource dataSource) {
		this.notes = dataSource.getNotes();
//		this.listeners = dataSource.getListeners();
		
		this.pixelWidth = dataSource.pixelWidth;
		this.calibrationUnit = dataSource.calibrationUnit;
	}

	/**
	 * @return notes written to the panels's notes field on the GUI or empty
	 *         string
	 */
	public String getNotes() {
		return notes;
	}

	/**@return pixel width in @see getUnit() units */
	public double getPixelWidth() {
		return pixelWidth;
	}
	/**@return measure of the pixel width */
	public String getUnit() {
		return calibrationUnit;
	}
	
	/** @param text notes on the image (e.g. from the GUI notes field) */
	public void setNotes(String text) {
		notes = text;
	}

	/**calibration information
	 * @param pixelSize
	 * @param unit */
	public void setPixelCalibration(double pixelSize, String unit) {
		this.calibrationUnit = unit;
		pixelWidth = pixelSize;
	}

	/**@param label String label of the panel or "" */
	public void setLabel(String label) {
		this.label = label;
	}

	/**@param sInfo length of the scale bar, -1 if no scale bar */
	public void setScalebarLength(double l) {
		if(l <= 0)
			scalebarInfo = "";
		else scalebarInfo = (l)+" "+calibrationUnit;
	}
	
	@Override
	public void clear() {
		setNotes("");
	}

	@Override
	public boolean fromFile() {
		return false;
	}

	/**
	 * TODO Documentation
	 */
	protected void notifyListeners() {
		for (DataSourceListener listener : listeners.getListeners(
			DataSourceListener.class))
			listener.dataSourceChanged(new DataSourceEvent(this));
	}

	public void removeListener(DataSourceListener listener) {
		listeners.remove(DataSourceListener.class, listener);
	}

	public void addListener(DataSourceListener listener) {
		listeners.add(DataSourceListener.class, listener);
	}

	@Override
	public String toString() {
		String s = "";
		s += "Panel: " + (label == "" ? "Untitled" : label) + "\n";
		s += "User notes:\n";
		s += (notes != "" ? (notes + "\n") : "");
		s += "Scale bar: " + (scalebarInfo == null ? "Undefined" : scalebarInfo) +
			"\n";

		return s;
	}

}
