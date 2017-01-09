// TODO Missing license header

package fr.cnrs.ibmp.dataSets;

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
public interface DataSourceInterface {

	public void clear();

	public boolean fromFile();

	@Override
	public String toString();
}
