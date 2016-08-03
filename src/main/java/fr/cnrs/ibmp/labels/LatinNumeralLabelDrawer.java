
package fr.cnrs.ibmp.labels;

/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich
 */
public class LatinNumeralLabelDrawer extends AbstractLabelDrawer {

	@Override
	public String next() {
		while (contains(counter++)) {
			// Do nothing
		}

		counter += reversedOrder ? -1 : 1;

		if (reversedOrder) {
			return ((counter) % 26) + 1 + "";
		}

		return (counter + 1) + "";
	}

}
