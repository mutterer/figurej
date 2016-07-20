// TODO Missing license header

package fr.cnrs.ibmp.utilities;

/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich
 */
public enum Interpolation {
	NEAREST_NEIGHBOR("nearest neighbor"),
	LINEAR("linear"),
	CUBIC_CONVOLUTION("cubic convolution"),
	CUBIC_B_SPLINE("cubic B-spline"),
	CUBIC_O_MOMS("cubic O-MOMS"),
	QUINTIC_B_SPLINE("quintic B-spline");

	public String name;

	private Interpolation(String name) {
		this.name = name;
	}

}
