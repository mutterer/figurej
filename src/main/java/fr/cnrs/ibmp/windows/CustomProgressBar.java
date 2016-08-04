
package fr.cnrs.ibmp.windows;

import fr.cnrs.ibmp.treeMap.LeafPanel;
import ij.IJ;

/**
 * Customized progress bar to show the status of the transfer of information
 * from an image to a {@link LeafPanel}. It uses the ImageJ status bar to show
 * the progress.
 * <p>
 * (c) IBMP-CNRS
 * </p>
 * 
 * @author Edda Zinck
 * @author Jerome Mutterer
 */
public class CustomProgressBar implements Runnable {

	@Override
	public void run() {
		try {
			String s = "                      ";
			int i = 0;
			while (true) {
				i = (i + 1) % 12;
				IJ.showStatus("Scaling image. " + s.substring(0, i) + "***" + s
					.substring(0, 12 - i));
				Thread.sleep(100);
			}
		}
		catch (InterruptedException e) {
			// TODO Handle exception
		}
	}

}
