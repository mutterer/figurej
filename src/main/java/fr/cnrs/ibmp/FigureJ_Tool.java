package fr.cnrs.ibmp;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.Line;
import ij.plugin.tool.PlugInTool;
import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.event.EventListenerList;

import fr.cnrs.ibmp.treeMap.LeafPanel;
import fr.cnrs.ibmp.treeMap.Panel;
import fr.cnrs.ibmp.utilities.Constants;
import fr.cnrs.ibmp.windows.MainController;
import fr.cnrs.ibmp.windows.MainWindow;

/**
 * main class handles switching and interaction between the result figure window
 * and the window where image regions are selected. furthermore this class
 * contains the window to create/open/safe figures and the panel control window
 * (left one) in the listeners of this classes elements behavior like
 * enabled-status of buttons or actions that take place on button clicks are
 * defined.
 * 
 * (c) IBMP-CNRS
 * 
 * @author Edda Zinck
 * @author Jerome Mutterer
 * @author Stefan Helfrich
 */
public class FigureJ_Tool extends PlugInTool {

	private String title = "FigureJ ";
	
//	private ImagePlus openedImage; // image the ROI tool works on
	private MainWindow mainWindow;
	private boolean mainWindowActive = true;

	/** TODO Documentation */
	private EventListenerList listeners = new EventListenerList();

	// click behavior
	private boolean reactOnRelease = false;

	/** Currently active LeafPanel */
	private LeafPanel activePanel;

	private MainController mainController = MainController.getInstance();

	public FigureJ_Tool() {
//		removeExistingInstance();
		this.mainWindow = mainController.getMainWindow();
	}

	private void removeExistingInstance() {
		if (WindowManager.getFrame(title) != null)
			WindowManager.getFrame(title).dispose();
	}

	@Override
	public String getToolName() {
		return "FigureJ Tool";
	}

	@Override
	public String getToolIcon() {
		return "CfffF00ff Cf00F50a9 C00fF0a56 Cff0Fbd33 C000 L404f L0444 L09f9 La9af Lacfc Lf0ff";
	}

	/**
	 * handles the switch between the two different image windows (main window
	 * with the panel where the result image is composed and the window of the
	 * ROI tool where you can crop the region of a image you want to display).
	 * depending on if the ROI tool is open or not either methods of the one or
	 * of the other class are called.
	 */
	@Override
	public void mousePressed(ImagePlus imp, MouseEvent e) {
		if (mainWindow == null) {
			mainWindow = mainController.getMainWindow();
		}

		if (imp.getTitle().equals(Constants.title)) {
			mainWindowActive = true;
			mainWindow.mousePressed(imp, e);
//			int count = e.getClickCount();
			
			Panel p = mainWindow.getSelectedPanel();
			/* Check whether the user has selected a LeafPanel or a SeparatorPanel.
			 * Enable (leaf) or disable (separator) the respective buttons in the
			 * FigureControlPanel. */
			if (p instanceof LeafPanel) {
				activePanel = (LeafPanel) p;
				
				notifyLeafSelected(new LeafEvent(p));
				
				// from version1b2, double click a panel to open an image.
				// on the mac only, could find a windows fix so far.
				// TODO Fix behavior
//				if ((count == 2) && IJ.isMacintosh())
//					panelWindow.openTiltedROITool(false);
			} else {
				// Separator selected
				activePanel = null;
				
				notifySeparatorSelected(new SeparatorEvent(p));
			}
		}
	}

	/** calls either the dragging method of the main window or the ROI tool */
	@Override
	public void mouseDragged(ImagePlus imp, MouseEvent e) {
		if (mainWindow == null) {
			mainWindow = mainController.getMainWindow();
		}

		if (imp.getTitle().equals(Constants.title))
			mainWindow.mouseDragged(imp, e);
	}

	/**
	 * does either nothing, calls the ROI release method or updates the values
	 * of the active panel's window
	 */
	@Override
	public void mouseReleased(ImagePlus imp, MouseEvent e) {
		if (mainWindow == null) {
			mainWindow = mainController.getMainWindow();
		}

		if (reactOnRelease && mainWindowActive) {
			mainWindow.mouseReleased(imp, e);

			if (activePanel != null) {
				Point a = mainWindow.getMousePressedPoint();
				Point b = mainWindow.getMouseReleasedPoint();
				Line l = new Line(a.x, a.y, b.x, b.y);
				boolean horizontalDrag = (b.x - a.x) * (b.x - a.x) > (b.y - a.y) *
					(b.y - a.y);
				if ((l.getRawLength() > 20) && (Prefs.get("figurej.mousePanelSlicing",
					0) == 1)) activePanel.split(2, horizontalDrag);
				mainWindow.draw();

				notifyLeafResized(new LeafEvent(activePanel));

				IJ.showStatus(activePanel.getScaleBarText());
			}
		}
	}

	@Override
	public void mouseMoved(ImagePlus imp, MouseEvent e) {
		if (mainWindow == null) {
			mainWindow = mainController.getMainWindow();
		}

		if (mainWindow != null) {
			mainWindow.mouseMoved(imp, e);
		}
	}

	public void addLeafListener(LeafListener listener) {
		listeners.add(LeafListener.class, listener);
	}

	public void removeLeafListener(LeafListener listener) {
		listeners.remove(LeafListener.class, listener);
	}

	protected synchronized void notifyLeafSelected(LeafEvent event) {
		for (LeafListener l : listeners.getListeners(LeafListener.class))
			l.leafSelected(event);
	}
	
	protected synchronized void notifyLeafResized(LeafEvent event) {
		for (LeafListener l : listeners.getListeners(LeafListener.class))
			l.leafResized(event);
	}
	
	public void addSeparatorListener(SeparatorListener listener) {
		listeners.add(SeparatorListener.class, listener);
	}

	public void removeSeparatorListener(SeparatorListener listener) {
		listeners.remove(SeparatorListener.class, listener);
	}

	protected synchronized void notifySeparatorSelected(SeparatorEvent event) {
		for (SeparatorListener l : listeners.getListeners(SeparatorListener.class))
			l.separatorSelected(event);
	}

}
