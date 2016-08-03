
package fr.cnrs.ibmp.labels;

/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich
 */
public class UppercaseLabelDrawer extends AbstractLabelDrawer {

	@Override
	public String next() {
		while (contains(counter++)) {
			// Do nothing
		}

		counter += reversedOrder ? -1 : 1;

		if (reversedOrder) {
			return (char) (65 + (counter % 26)) + "";
		}

		return (char) (65 + counter) + "";
	}

}
