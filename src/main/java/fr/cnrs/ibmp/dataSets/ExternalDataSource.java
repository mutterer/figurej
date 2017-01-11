
package fr.cnrs.ibmp.dataSets;

import fr.cnrs.ibmp.plugIns.Link;

/**
 * An {@link ExternalDataSource} keeps, in addition to a rendered pixel-based
 * representation of the link's content, a link to the source file such that it
 * can be re-opened using the known {@link Link}.
 * 
 * @author Stefan Helfrich (University of Konstanz)
 */
public class ExternalDataSource extends FileDataSource
{

	private static final long serialVersionUID = 1L;

	/** File name of the original input file (e.g. SVG). */
	private String externalSource = "";

	/**
	 * Creates an {@link ExternalDataSource} with default settings.
	 */
	public ExternalDataSource() {
		super();
	}

	/**
	 * TODO Documentation
	 * 
	 * @param externalDataSource
	 */
	public ExternalDataSource(final ExternalDataSource externalDataSource) {
		super(externalDataSource);

		this.externalSource = externalDataSource.getExternalSource();
	}

	/**
	 * TODO Documentation
	 *
	 * @return
	 */
	public String getExternalSource() {
		return this.externalSource;
	}

	/**
	 * TODO Documentation
	 * 
	 * @param source
	 */
	public void setExternalSource(String source) {
		this.externalSource = source;

		notifyListeners();
	}

	@Override
	public void clear() {
		super.clear();

		setExternalSource("");
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "External Datasource: " + (externalSource.equals("") ? "none"
			: externalSource) + "\n";
		s += "-------------------------\n";

		return s;
	}

	@Override
	public String getStringRepresentation() {
		return "External";
	}

	@Override
	public Object open() {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

}
