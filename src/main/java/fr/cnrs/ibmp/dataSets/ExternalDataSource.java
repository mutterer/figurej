
package fr.cnrs.ibmp.dataSets;

import java.io.Serializable;

/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich (University of Konstanz)
 */
public class ExternalDataSource extends AbstractDataSource implements
	Serializable
{

	private static final long serialVersionUID = 1L;

	private String externalSource = "";

	public ExternalDataSource(final ExternalDataSource externalDataSource) {
		super(externalDataSource);

		this.externalSource = externalDataSource.getExternalSource();
	}

	public String getExternalSource() {
		return this.externalSource;
	}

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

}
