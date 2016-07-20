// TODO Missing license headers
package fr.cnrs.ibmp;

import java.util.EventListener;

/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich
 */
public interface LeafListener extends EventListener {

	void leafSelected(LeafEvent e);

	void leafDeselected(LeafEvent e);

	void leafResized(LeafEvent e);

	void leafCleared(LeafEvent e);
	
	void leafRemoved(LeafEvent e);
	
	void leafSplit(LeafEvent e);
}
