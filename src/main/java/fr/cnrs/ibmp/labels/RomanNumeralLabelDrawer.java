
package fr.cnrs.ibmp.labels;

/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich
 */
public class RomanNumeralLabelDrawer extends AbstractLabelDrawer {

	/**
	 * Computes roman numeral from latin numeral. Code adapted from <a href=
	 * "http://www.roseindia.net/java/java-tips/45examples/misc/roman/roman.shtml">
	 * http://www.roseindia.net/java/java-tips/45examples/misc/roman/roman.shtml
	 * </a>
	 * 
	 * @return
	 */
	@Override
	public String next() {
		counter += reversedOrder ? -1 : 1;

		if (reversedOrder) {
			throw new UnsupportedOperationException("Not yet implemented");
		}
		
		String[] RCODE = { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX",
			"V", "IV", "I" };
		int[] BVAL = { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
		counter = (counter % 30);
		if (counter == 0) counter++;
		int isLeft = counter;
		String roman = "";
		for (int i = 0; i < RCODE.length; i++) {
			while (isLeft >= BVAL[i]) {
				isLeft -= BVAL[i];
				roman += RCODE[i];
			}
		}
		return roman;
	}

}
