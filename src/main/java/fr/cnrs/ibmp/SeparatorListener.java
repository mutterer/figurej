// TODO Missing license headers
package fr.cnrs.ibmp;

import java.util.EventListener;

/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich
 */
public interface SeparatorListener extends EventListener {

	void separatorSelected(SeparatorEvent e);

	void separatorDeselected(SeparatorEvent e);

	void separatorResized(SeparatorEvent e);

}
