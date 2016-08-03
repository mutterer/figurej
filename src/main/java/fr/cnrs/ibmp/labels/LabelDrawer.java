// TODO License header

package fr.cnrs.ibmp.labels;

/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich
 */
public interface LabelDrawer {

	/**
	 * Get the next unassigned label.
	 * 
	 * @return Unassigned label.
	 */
	public String next();

	/**
	 * Check if there are unassigned labels left.
	 * 
	 * @return Availability of unassigned labels.
	 */
	public boolean hasNext();

}
