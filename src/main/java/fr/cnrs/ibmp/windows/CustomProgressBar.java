// TODO Missing license header
package fr.cnrs.ibmp.windows;

import ij.IJ;

/**
 * TODO Documentation
 */
public class CustomProgressBar implements Runnable {

	@Override
	public void run() {
		try {
			String s = "                      ";
			int i = 0;
			while (true) {
				i = (i + 1) % 12;
				IJ.showStatus("Scaling image. " + s.substring(0, i) + "***"
						+ s.substring(0, 12 - i));
				Thread.sleep(100);
			}
		} catch (InterruptedException e) {
			// TODO Handle exception
		}
	}

}
