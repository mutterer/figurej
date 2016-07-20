package fr.cnrs.ibmp;

import java.util.EventListener;

/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich
 */
public interface SaveFigureListener extends EventListener {

	void saveFigure(SaveFigureEvent e);

}
