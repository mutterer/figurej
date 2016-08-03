
package fr.cnrs.ibmp.labels;

/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich
 */
public class UserDefinedLabelDrawer extends AbstractLabelDrawer {

	private String[] userLabels = null;

	@Override
	public String next() {
		counter += reversedOrder ? -1 : 1;

		if (reversedOrder) {
			// TODO
		}
		
		if (userLabels != null) {
			return userLabels[counter % maxCounter];
		}

		return "-2";
	}

	public void setUserLabels(String labelString) {
		userLabels = labelString.split(";");
		maxCounter = userLabels.length;
		for (String s : userLabels)
			if (s.startsWith(" ")) s = s.substring(1);
	}

}
