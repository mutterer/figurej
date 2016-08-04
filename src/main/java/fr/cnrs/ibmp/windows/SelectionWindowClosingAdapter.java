// TODO Missing license header

package fr.cnrs.ibmp.windows;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import ij.ImagePlus;

/**
 * Specifies the closing behavior of the {@link ImagePlus} that is processed
 * with the {@link ROIToolWindow}.
 * <p>
 * (c) IBMP-CNRS
 * </p>
 * 
 * @author Edda Zinck
 * @author Jerome Mutterer
 */
public class SelectionWindowClosingAdapter extends WindowAdapter {

	@Override
	public void windowClosing(WindowEvent wEvent) {
		// TODO Hand-off to MainController
	}

	@Override
	public void windowClosed(WindowEvent e) { /* NB */ }

}
