// TODO Missing license header

package fr.cnrs.ibmp.windows;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JButton;

import fr.cnrs.ibmp.FigureJ_Tool;
import fr.cnrs.ibmp.ImageSelectionEvent;
import fr.cnrs.ibmp.ImageSelectionListener;
import fr.cnrs.ibmp.LeafEvent;
import fr.cnrs.ibmp.LeafListener;
import fr.cnrs.ibmp.NewFigureEvent;
import fr.cnrs.ibmp.NewFigureListener;
import fr.cnrs.ibmp.OpenFigureEvent;
import fr.cnrs.ibmp.SaveFigureEvent;
import fr.cnrs.ibmp.SaveFigureListener;
import fr.cnrs.ibmp.dataSets.DataSource;
import fr.cnrs.ibmp.labels.AbstractLabelDrawer;
import fr.cnrs.ibmp.windows.FigureControlPanel;
import ij.CommandListener;
import ij.Executer;
import ij.IJ;
import ij.IJEventListener;
import ij.ImageListener;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.PolygonRoi;
import ij.gui.Toolbar;
import ij.plugin.frame.Recorder;
import ij.process.FloatPolygon;
import fr.cnrs.ibmp.treeMap.LeafPanel;
import fr.cnrs.ibmp.treeMap.Panel;
import fr.cnrs.ibmp.utilities.Constants;
import fr.cnrs.ibmp.utilities.MyImageMath;
import fr.cnrs.ibmp.utilities.Serializer;

/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich
 */
public class MainController implements NewFigureListener, SaveFigureListener,
	WindowListener, CommandListener, IJEventListener, ImageListener, LeafListener, ActionListener, ImageSelectionListener
{

	/** TODO Documentation */
	private MainWindow mainWindow;

	/** TODO Documentation */
	private FigureControlPanel panelWindow;
	
	/** TODO Documentation */
	private NewOpenSaveFrame newOpenSaveFrame;
	
	/** TODO Documentation */
	private NewFigureDialog newFigureDialog;
	
	//GUI parts and windows
	/** TODO Documentation */
	private ROIToolWindow selectionWindow; // image region selection tool / ROI tool
	
	/** TODO Documentation */
	private AnnotationsAndOptionsPanel optionsWindow; // window displaying optional settings
	
	/** TODO Documentation */
	private int mainWindowXLocation = 0;
	
	/** TODO Documentation */
	private int mainWindowYLocation = 0;

	/** TODO Documentation */
	// private JButton removeScalebarButton;

	/** TODO Documentation */
	// object that controls storing and reopening result images
	private Serializer serializer = new Serializer();

	/** TODO Documentation */
	// creates macros if opened images are pre-processed by the user
	private Recorder recorder;

	/** TODO Documentation */
	private Dimension panelDimension = new Dimension(200, 600);

	/** Currently active LeafPanel */
	private LeafPanel activePanel;

	private FigureJ_Tool figureJTool;

	/** TODO Remove this! */
	private ImagePlus openedImage;

	private static MainController instance;

	/** TODO Documentation */
	private MainController() {
		IJ.addEventListener(this);
		ImagePlus.addImageListener(this);
		Executer.addCommandListener(this);
		
		this.newOpenSaveFrame = new NewOpenSaveFrame();
		this.newOpenSaveFrame.addNewFigureListener(this);
		
		this.newFigureDialog = new NewFigureDialog();
		this.newOpenSaveFrame.addNewFigureListener(this.newFigureDialog);
	}

	public void createMainWindow() {
	// Compute location of new MainWindow
		mainWindowXLocation = panelDimension.width + Constants.guiBorder + 5;
		mainWindowYLocation = newOpenSaveFrame.getHeight() +
				newOpenSaveFrame.getLocation().y + Constants.guiBorder;

		// Create new MainWindow
		mainWindow = new MainWindow(newFigureDialog.getWidth(), newFigureDialog.getHeight(),
			mainWindowXLocation, mainWindowYLocation, newFigureDialog.getResolution(),
			newFigureDialog.getSeparatorSize());
		mainWindow.calibrateImage(newFigureDialog.getResolution(), newFigureDialog.getUnit());
		
		// Initialize activePanel
		Panel p = mainWindow.getSelectedPanel();
		if (p instanceof LeafPanel) {
			activePanel = (LeafPanel) p;
		} else {
			activePanel = null;
		}
	}

	@Override
	public void newFigure(NewFigureEvent e) {
		// Initialize main window
		createMainWindow();
		
		// Initialize FigureControlPanel
		if (panelWindow == null) {
			panelWindow = new FigureControlPanel(this, new Point(0, newOpenSaveFrame.getHeight()
				+ newOpenSaveFrame.getLocation().y + Constants.guiBorder));
			panelWindow.addLeafListener(mainWindow);
			panelWindow.addLeafListener(this); // NB FILO list for listeners
			panelWindow.addLeafListener(selectionWindow);

			figureJTool.addLeafListener(panelWindow);

			panelWindow.setVisible(true);
		}
	}
	
	@Override
	public void saveFigure(SaveFigureEvent e) {
		// assuming the figure is the active window !! remove the ROI showing which
		// panel is selected
		IJ.run("Select None");
		
		serializer.serialize(mainWindow);
		mainWindow.draw(); // show scale bars and labels again
	}

	public void openFigure(OpenFigureEvent e) {
		mainWindow = null;
		mainWindow = serializer.deserialize();

		// if opened successfully
		if (mainWindow != null) {
			/*
			 * Panel p = mainWindow.getSelectedPanel(); if
			 * (p.getClass().getName()
			 * .contains(LeafPanel.class.getName())) activePanel =
			 * (LeafPanel) p; else activePanel = null;
			 */
			if (panelWindow == null) {
				panelWindow = new FigureControlPanel(this, new Point(0, newOpenSaveFrame.getHeight()
					+ newOpenSaveFrame.getLocation().y + Constants.guiBorder));
				panelWindow.setVisible(true);
			}
		} else {
			getNewOpenSaveFrame().setOpenNewButtonsStates(true);
			return;
		}
			
		mainWindow.draw();
	}

	/**
	 * TODO Documentation
	 * 
	 * @return
	 */
	public static synchronized MainController getInstance() {
		if (MainController.instance == null) {
			MainController.instance = new MainController();
		}

		return MainController.instance;
	}

	@Override
	public void windowClosing(WindowEvent wEvent) {
		String info = "";
		if (mainWindow != null) {
			if (mainWindow.getImagePlus().changes)
				info = "\nCurrent figure will be lost.";
			GenericDialog gd = new GenericDialog("FigureJ");
			gd.addMessage("Are you sure you want to quit FigureJ?" + info);
			gd.showDialog();
			if (gd.wasCanceled())
				return;
		}
		if (mainWindow != null)
			mainWindow.setQuitWithoutSaving(true);
		wEvent.getWindow().dispose();
		newOpenSaveFrame.dispose();

		IJ.setTool("rect");

		if (panelWindow != null)
			panelWindow.dispose();

		// application closed just after opening
		if (mainWindow != null && mainWindow.getImagePlus() != null)
			mainWindow.getImagePlus().close();

		if (optionsWindow != null)
			optionsWindow.dispose();
	}

	@Override
	public void windowClosed(WindowEvent e) {
		WindowManager.removeWindow((Frame) newOpenSaveFrame);
		Toolbar.restoreTools();

	}

	@Override
	public void windowOpened(WindowEvent e) { /* NB */ }

	@Override
	public void windowIconified(WindowEvent e) { /* NB */ }

	@Override
	public void windowDeiconified(WindowEvent e) { /* NB */ }

	@Override
	public void windowActivated(WindowEvent e) { /* NB */ }

	@Override
	public void windowDeactivated(WindowEvent e) { /* NB */ }

	@Override
	public String commandExecuting(String command) {
		if ((WindowManager.getImageCount()>0)&&(IJ.getImage().getTitle()=="FigureJ") && command.equals("Make Composite"))
			return null; // do not run this command
		else 
			return command;
	}
	
	/** remove the highlighting of the clicked panel because this could be dragged around e.g. with the arrow tool*/
	@Override
	public void eventOccurred(int eventID) {
		if (eventID == IJEventListener.TOOL_CHANGED) {
			if (mainWindow != null && mainWindow.getImagePlus() != null)
				mainWindow.getImagePlus().changes = false;
		}
	}
	
	/** if the tilted rect tool is opened, deactivates the main frame */
	@Override
	public void imageOpened(ImagePlus img) { /* NB */ }

	@Override
	public void imageUpdated(ImagePlus img) { /* NB */ }

	/** handle status of the image buttons */
	@Override
	public void imageClosed(ImagePlus img) {

		if (mainWindow != null)
			// if the ROI tool image is closed activate the main frame; close
			// the channel selection tool if necessary
			if (img == openedImage) {
				panelWindow.setROIToolOpenable(true);
				panelWindow.setControlFrameButtonStates(true);
				if (openedImage.isComposite()
						&& WindowManager.getFrame("Channels") != null)
					( WindowManager.getFrame("Channels")).dispose();
			} else
			// if the result figure itself is closed ask if the user
			// wants to store the result
			if (img == mainWindow.getImagePlus()) {
				if (mainWindow.getQuitWithoutSaving() == false)
					serializer.serialize(mainWindow);
				panelWindow.disableAllPanelWindowButtons(true);
				// allow to open a figure or
				// create a new one
				// TODO Setup events
				newOpenSaveFrame.setOpenNewButtonsStates(true);
			}
		if (optionsWindow != null)
			optionsWindow.dispose();
	}
	
	/**
	 * the pixels inside the ROI are rotated and scaled to the size of the panel
	 * the image belongs to. the pixel data is passed to the panel, the main
	 * window is set active again; the panels are repainted.
	 * 
	 * @param macro TODO Documentation
	 */
	private void transferROIDataToPanel(String macro) {
		Panel temp = mainWindow.getSelectedPanel();

		if (temp != null)
			if (temp.getClass().getName().contains(LeafPanel.class.getName())) {
				// Remember the size and position of the image fragment the user selected
				double[] xVals = selectionWindow.getXVals().clone();
				double[] yVals = selectionWindow.getYVals().clone();

				// Save coordinates of source ROI for use with external tools
				saveRoiCoordinates(xVals, yVals);

				LeafPanel selectedPanel = (LeafPanel) temp;
	
				// store detailed information about the image the user chose for a panel
				DataSource imageData = selectedPanel.getImgData();
				imageData.setCoords(xVals, yVals);
				imageData.setPixelCalibration(
						openedImage.getCalibration().pixelWidth, openedImage
								.getCalibration().getUnit());
				imageData.setMacro(macro);
				imageData.setDisplayRange(openedImage.getDisplayRangeMin(),
						openedImage.getDisplayRangeMax());

				// position in stack like and composite images
				imageData.setSlice(openedImage.getSlice());
				imageData.setChannel(openedImage.getChannel());
				imageData.setFrame(openedImage.getFrame());
				WindowManager.setTempCurrentImage(openedImage);
				imageData
						.setActChs(IJ
								.runMacro("ch='';if (is('composite')) Stack.getActiveChannels(ch);return ch;"));

//				filePathLabel.setText(imageData.getFileDirectory()
//						+ imageData.getFileName());
				// / TODO: DEBUG FROM HERE
				// / IJ.log(imageData.getFileDirectory() +
				// imageData.getFileName());

				float[] xRect = new float[xVals.length];
				float[] yRect = new float[xVals.length];

				for (int i = 0; i < xRect.length; i++) {
					xRect[i] = (float) xVals[i];
					yRect[i] = (float) yVals[i];
				}

				PolygonRoi boundingRect = new PolygonRoi(new FloatPolygon(
						xRect, yRect), PolygonRoi.POLYGON);

				double angle = Math.atan((yVals[3] - yVals[0])
						/ (xVals[3] - xVals[0]))
						* 180 / Math.PI + ((xVals[3] < xVals[0]) ? 180 : 0);

				Line top = new Line(xVals[0], yVals[0], xVals[3], yVals[3]);
				double scaleFactor = selectedPanel.getW() / top.getRawLength();

				// FILL THE PANEL with the pixels selected from the image
//				selectedPanel.setPixels(MyImageMath.getPixels(openedImage,
//						boundingRect, angle, scaleFactor, selectedPanel.getW(),
//						selectedPanel.getH(), getSelectedInterpolation()));
				// calculate the calibration
				imageData.setPixelCalibration(
						(1 / scaleFactor)
								* openedImage.getCalibration().pixelWidth,
						openedImage.getCalibration().getUnit());
				imageData.setAngle(angle);
				imageData.setScaleFactor(scaleFactor);
				// IJ.log(""+Arrays.toString(xVals));
				mainWindow.draw();
				mainWindow.getImagePlus().killRoi();
			}

		panelWindow.setROIToolOpenable(true);
		panelWindow.setControlFrameButtonStates(true);
	}

	/**
	 * TODO Documentation
	 * 
	 * @param selectedImage
	 * @param xVals
	 * @param yVals
	 * @param macro
	 */
	private void transferROIDataToPanel(final ImagePlus selectedImage,
		final double[] xVals, final double[] yVals, final String macro)
	{
		if ((mainWindow.getSelectedPanel() != null) &&
			(mainWindow.getSelectedPanel() instanceof LeafPanel))
		{
			// Remember the size and position of the image fragment the user selected
			double[] xValsCopy = xVals.clone();
			double[] yValsCopy = yVals.clone();

			// Save coordinates of source ROI for use with external tools
			saveRoiCoordinates(xValsCopy, yValsCopy);

			// store detailed information about the image the user chose for a panel
			LeafPanel selectedPanel = (LeafPanel) mainWindow.getSelectedPanel();
			DataSource imageData = selectedPanel.getImgData();
			imageData.setCoords(xValsCopy, yValsCopy);
			imageData.setPixelCalibration(selectedImage.getCalibration().pixelWidth,
				selectedImage.getCalibration().getUnit());
			imageData.setMacro(macro);
			imageData.setDisplayRange(selectedImage.getDisplayRangeMin(),
				selectedImage.getDisplayRangeMax());

			// position in stack like and composite images
			imageData.setSlice(selectedImage.getSlice());
			imageData.setChannel(selectedImage.getChannel());
			imageData.setFrame(selectedImage.getFrame());
			WindowManager.setTempCurrentImage(selectedImage);
			imageData.setActChs(IJ.runMacro(
				"ch='';if (is('composite')) Stack.getActiveChannels(ch);return ch;"));

			float[] xRect = new float[xValsCopy.length];
			float[] yRect = new float[xValsCopy.length];

			for (int i = 0; i < xRect.length; i++) {
				xRect[i] = (float) xValsCopy[i];
				yRect[i] = (float) yValsCopy[i];
			}

			double angle = Math.atan((yValsCopy[3] - yValsCopy[0]) / (xValsCopy[3] -
				xValsCopy[0])) * 180 / Math.PI + ((xValsCopy[3] < xValsCopy[0]) ? 180
					: 0);

			Line top = new Line(xValsCopy[0], yValsCopy[0], xValsCopy[3],
				yValsCopy[3]);
			double scaleFactor = selectedPanel.getW() / top.getRawLength();

			// Fill the panel with the pixels selected from the image
			selectedPanel.setPixels((int[]) selectedImage.getProcessor().getPixels());
			
			// calculate the calibration
			imageData.setPixelCalibration(selectedImage.getCalibration().pixelWidth,
				selectedImage.getCalibration().getUnit());
			imageData.setAngle(angle);
			imageData.setScaleFactor(scaleFactor);

			mainWindow.draw();
			mainWindow.getImagePlus().killRoi();
		}

		panelWindow.setROIToolOpenable(true);
		panelWindow.setControlFrameButtonStates(true);
	}

	/**
	 * @param xVals
	 * @param yVals
	 */
	private void saveRoiCoordinates(double[] xVals, double[] yVals) {
		String xRoi = "";
		String yRoi = "";
		for (int i = 0; i < xVals.length; i++) {
			xRoi = xRoi+ IJ.d2s(xVals[i])+",";
			yRoi = yRoi+ IJ.d2s(yVals[i])+",";
		}
		xRoi = xRoi.substring(0, xRoi.length()-1);
		yRoi = yRoi.substring(0, yRoi.length()-1);

		Prefs.set("figurej.xRoi", xRoi);
		Prefs.set("figurej.yRoi", yRoi);
	}

	public void cleanGUIandTransferROI() {

		String recordedMacro = "";

		if (recorder != null) {
			recordedMacro = recorder.getText();
			try {
				recorder.close();
			} catch (Exception e2) {
				System.err.println("recorder nullpointer");
			}
		}
		// fire custom progress bar b/c some transforms are slow
		Thread t = new Thread(new CustomProgressBar());
		t.start();
		try {
			transferROIDataToPanel(recordedMacro);
		} catch (Exception e1) {
			IJ.error("Could not transform image.\n" + e1.getMessage());
		}
		openedImage.close();
		IJ.run("Select None");
		t.interrupt();

		IJ.showStatus("done.");
	}

	@Override
	public void leafSelected(LeafEvent e) {
		if (IJ.altKeyDown()) {
			LeafPanel leafPanel = (LeafPanel) e.getSource();
			IJ.log(leafPanel.getImgData().createLog());
		}
	}

	@Override
	public void leafDeselected(LeafEvent e) { /* NB */ }

	@Override
	public void leafResized(LeafEvent e) { /* NB */ }

	public void setFigureJTool(FigureJ_Tool figureJTool) {
		this.figureJTool = figureJTool;
		this.figureJTool.addLeafListener(this);
		this.figureJTool.addLeafListener(panelWindow);
		this.figureJTool.addLeafListener(optionsWindow);
	}

	@Override
	public void leafCleared(LeafEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void leafRemoved(LeafEvent e) {
		LeafPanel leaf = (LeafPanel) e.getSource();
		
		// TODO Might need to be changed to activePanel
		leaf.hideLabel(mainWindow.getImagePlus()
			.getOverlay());
		leaf.hideScalebar(mainWindow.getImagePlus()
				.getOverlay());
		leaf.remove(mainWindow.getImagePlus().getOverlay());
	}

	public Panel getSelectedPanel() {
		return mainWindow.getSelectedPanel();
	}

	/**
	 * TODO Documentation
	 * @return
	 */
	public MainWindow getMainWindow() {
		return mainWindow;
	}

	/**
	 * TODO Documentation
	 * 
	 * @return
	 */
	public Dimension getPanelDimension() {
		return panelDimension;
	}

	/**
	 * TODO Documentation
	 * 
	 * @return
	 */
	public NewOpenSaveFrame getNewOpenSaveFrame() {
		// TODO Auto-generated method stub
		return newOpenSaveFrame;
	}

	@Override
	public void leafSplit(LeafEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JButton optionsButton = (JButton) e.getSource();

		if (optionsWindow == null) optionsWindow = new AnnotationsAndOptionsPanel(
			optionsButton.getLocation().x + 50, optionsButton.getParent()
				.getLocation().y + optionsButton.getLocation().y + 30);

		// HACK the panel shouldn't have a reference to the mainWindow
		optionsWindow.setMainWindow(mainWindow);
		optionsWindow.setActivePanel(activePanel);
		
		figureJTool.addLeafListener(optionsWindow);

		optionsWindow.setVisible(true);
	}

	public LeafPanel getActivePanel() {
		return activePanel;
	}

	@Override
	public void imageSelected(ImageSelectionEvent e) {
		// fire custom progress bar b/c some transforms are slow
		Thread t = new Thread(new CustomProgressBar());
		t.start();
		try {
			transferROIDataToPanel((ImagePlus) e.getSource(), e.getxVals(), e
				.getyVals(), e.getMacro());
		}
		catch (Exception e1) {
			IJ.error("Could not transform image.\n" + e1.getMessage());
		}
		finally {
			t.interrupt();
		}
		
		// Focus main window
		WindowManager.setCurrentWindow(mainWindow.getWindow());
		
		// Select the FigureJ Tool
		Toolbar.getInstance().setTool(figureJTool.getToolName());
	}

	public void setRoiTool(ROIToolWindow roiTool) {
		this.selectionWindow = roiTool;
	}

}
