package plugIns;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GUI;
import ij.io.FileInfo;
import ij.macro.Interpreter;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;

import treeMap.LeafPanel;
import windows.MainWindow;

/**
 * @author Jerome Mutterer
 * @author Edda Zinck
 * this class controls a Chimera instance and allows to create an image that is
 * used to fill the active panel with Chimera visualizations.
 */

public class ScriptPanel_Link extends Link implements ActionListener {

	public static final String LOC_KEY 			= "scriptpanel.loc";
	private static final long serialVersionUID 	= 1L;
	private static String tempDir 				=  IJ.getDirectory("startup")+"temp"+File.separator;
	private static Frame instance;
	private JButton newScriptButton = new JButton("New Panel Script");
	private JButton grabButton 	= new JButton("Apply");
	private JButton editButton	= new JButton("Edit");
	private JButton helpButton	= new JButton("Help");

	//TODO modifiers?!
	String[] portSettings 		= new String[3];



	public ScriptPanel_Link() {
		super();
		init();
	}

	public ScriptPanel_Link(MainWindow main) {
		super(main);
		init();
	}

	private void init() {
		linkIdentifyer = "ScriptPanel_";
		if (instance != null) {
			WindowManager.toFront(instance);
			return;
		}
		WindowManager.addWindow(this);
		instance = this;
		setLayout(new FlowLayout());

		addListeners();
		setTitle("Script Panel Link");
		add(newScriptButton);
		add(grabButton);
		add(editButton);
		add(helpButton);
		pack();
		
		Point loc = Prefs.getLocation(LOC_KEY);
		if (loc != null) {
			setLocation(loc);
		} else {
			GUI.center(this);
		}
		setVisible(true);
	}
	private void addListeners(){
		newScriptButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				updateActivePanel();
				int width = IJ.getImage().getRoi().getBounds().width;
				int height = IJ.getImage().getRoi().getBounds().height;
				fileName = linkIdentifyer+timeStamp()+".ijm";
				String script = tempDir+fileName+";"+IJ.d2s(width,0)+";"+IJ.d2s(height,0);
				runMyMacroFromJar("createPanelScript.ijm", script);
				((LeafPanel) getActivePanel()).getImgData().setExternalSource(fileName);
				((LeafPanel) getActivePanel()).getImgData().setFileDirectory(tempDir);
				IJ.run("Edit...", "open=["+tempDir+fileName+"]");
			}
		});

		grabButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String name = ((LeafPanel) getActivePanel()).getImgData().getExternalSource();
				String dir = ((LeafPanel) getActivePanel()).getImgData().getFileDirectory();
				// run batch macro and save output as png
				String macro = IJ.openAsString(dir+name);
				String result = runPanelScript(macro, null);
				store(tempDir,result);


			}
		});
		editButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				//updateActivePanel();
				fileName = ((LeafPanel) getActivePanel()).getImgData().getExternalSource();
				if (!fileName.endsWith(".ijm")) return;
				String dir = ((LeafPanel) getActivePanel()).getImgData().getFileDirectory();
				IJ.run("Edit...", "open=["+dir+fileName+"]");
			}
		});

		helpButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				IJ.showMessage("Script (macro) Link for FigureJ Panels",
						"-> 'new panel script' forces Chimera display to a suitable value for your panel.\n" +
						"-> 'apply', runs the panel generating macro and places it inside the figure.\n"+
						"-> 'edit', re-opens the script for the active panel."
						);
			}
		});
	}

	public void actionPerformed(ActionEvent evt) {
		// moved each case into buttons action listeners.
	}
	
	private String runPanelScript(String macro, ImagePlus imp) {
		// stolen from batchprocesser class 
		String result="";
		WindowManager.setTempCurrentImage(imp);
		Interpreter interp = new Interpreter();
		try {
			ImagePlus outputImage = interp.runBatchMacro(macro, imp);
			IJ.run(outputImage,"RGB Color",null);
			IJ.saveAs(outputImage,"png", tempDir+fileName);
			((LeafPanel) getActivePanel()).setPanelPixels(outputImage);
			FileInfo fi = outputImage.getOriginalFileInfo();
            if (fi!=null && fi.fileName!=null) result= fi.fileName;
		} catch(Throwable e) {
			interp.abortMacro();
			String msg = e.getMessage();
			if (!(e instanceof RuntimeException && msg!=null && e.getMessage().equals(Macro.MACRO_CANCELED)))
				IJ.handleException(e);
			return "";
		}
		return result;
	}


	public void close() {
	//	super.close();
		instance = null;
		Prefs.saveLocation(LOC_KEY, getLocation());
	}
}


