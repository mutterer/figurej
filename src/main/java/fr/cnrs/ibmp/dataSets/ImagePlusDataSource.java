
package fr.cnrs.ibmp.dataSets;

import java.io.Serializable;

import ij.ImagePlus;

/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich (University of Konstanz)
 */
public class ImagePlusDataSource extends ImageDataSource implements
	Serializable
{

	private static final long serialVersionUID = 1L;

	/**
	 * Creates an {@link ImagePlusDataSource} from default values.
	 */
	public ImagePlusDataSource() {
		super();
	}

	/**
	 * 
	 * @param input
	 */
	public ImagePlusDataSource(final ImagePlus input) {
		super();

		// TODO Extract values
		this.imp = input;
	}
	
	/**
	 * Creates a new {@link ImagePlusDataSource} by making a deep copy of the
	 * input.
	 * 
	 * @param dataSource
	 */
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

	@Override
	public Object open() {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

}
