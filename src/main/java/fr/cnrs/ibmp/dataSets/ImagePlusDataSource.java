
package fr.cnrs.ibmp.dataSets;

import java.io.Serializable;

/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich (University of Konstanz)
 */
public class ImagePlusDataSource extends ImageDataSource implements
	Serializable
{

	private static final long serialVersionUID = 1L;

	public ImagePlusDataSource(final ImagePlusDataSource dataSource) {
		super(dataSource);
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "-------------------------\n";

		return s;
	}

	@Override
	public String getStringRepresentation() {
		return "ImagePlus";
	}

}
