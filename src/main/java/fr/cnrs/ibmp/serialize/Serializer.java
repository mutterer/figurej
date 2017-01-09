
package fr.cnrs.ibmp.serialize;

import fr.cnrs.ibmp.windows.MainWindow;

/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich (University of Konstanz)
 */
public interface Serializer {

	/**
	 * Opens a file selection dialog. tries to open a serialization file and
	 * rebuilds the panel structure as well as the overlay
	 * 
	 * @return the window the figure is drawn on with images, separators, arrows
	 *         and so on
	 */
	public MainWindow deserialize();

	/**
	 * Opens a dialog to select a file name and directory; calls methods to store
	 * the result image as TIFF and to serialize the other information.
	 * 
	 * @param mainWindow
	 */
	public void serialize(MainWindow mainWindow);

}
