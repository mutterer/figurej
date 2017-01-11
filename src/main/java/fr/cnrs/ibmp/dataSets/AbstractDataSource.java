
package fr.cnrs.ibmp.dataSets;

import javax.swing.event.EventListenerList;

import fr.cnrs.ibmp.DataSourceEvent;
import fr.cnrs.ibmp.DataSourceListener;

/**
 * Abstract super class for {@link DataSource} implementations.
 * 
 * @author Stefan Helfrich (University of Konstanz)
 */
public abstract class AbstractDataSource implements DataSource {

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
	 * Creates an {@link AbstractDataSource} from default values.
	 */
	public AbstractDataSource() {}

	@Override
	public String getNotes() {
		return notes;
	}

	@Override
	public double getPixelWidth() {
		return pixelWidth;
	}

	@Override
	public String getUnit() {
		return calibrationUnit;
	}
	
	@Override
	public void setNotes(String text) {
		notes = text;
	}

	/**calibration information
	 * @param pixelSize
	 * @param unit */
	public void setPixelCalibration(double pixelSize, String unit) {
		calibrationUnit = unit;
		pixelWidth = pixelSize;
	}

	@Override
	public void setLabel(String label) {
		this.label = label;
	}

	@Override
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

	@Override
	public void removeListener(DataSourceListener listener) {
		listeners.remove(DataSourceListener.class, listener);
	}

	@Override
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

	@Override
	public void invalidateCoordinates() {
		// NB: Do nothing
		// FIXME Can be moved to DataSource when switching to Java8
	}

}
