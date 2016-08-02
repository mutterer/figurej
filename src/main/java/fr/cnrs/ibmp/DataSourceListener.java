// TODO Missing license headers
package fr.cnrs.ibmp;

import java.util.EventListener;

/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich
 */
public interface DataSourceListener extends EventListener {

	void dataSourceChanged(DataSourceEvent e);

}
