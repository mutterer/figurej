package fr.cnrs.ibmp.plugIns;

import ij.IJ;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GUI;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;

import fr.cnrs.ibmp.treeMap.LeafPanel;
import fr.cnrs.ibmp.windows.MainWindow;

/**
 * @author Jerome Mutterer
 * @author Edda Zinck
 * this class controls a Chimera instance and allows to create an image that is
 * used to fill the active panel with Chimera visualizations.
 */

public class Chimera_Link extends Link implements ActionListener {

	public static final String LOC_KEY 			= "chimeralink.loc";
	private static final long serialVersionUID 	= 1L;
	private static String tempDir 				=  IJ.getDirectory("startup")+"temp"+File.separator;
	private static Frame instance;
	private JButton viewPButton = new JButton("Set window size");
	private JButton grabButton 	= new JButton("Grab view");
	private JButton editButton	= new JButton("Edit");
	private JButton helpButton	= new JButton("Help");

	//TODO modifiers?!
	String[] portSettings 		= new String[3];



	public Chimera_Link() {
		super();
		init();
	}

	public Chimera_Link(MainWindow main) {
		super(main);
		init();
	}

	private void init() {
		linkIdentifyer = "Chimera_";
		// fileName = linkIdentifyer+counter+extension;
		if (instance != null) {
			WindowManager.toFront(instance);
			return;
		}
		WindowManager.addWindow(this);
		instance = this;
		setLayout(new FlowLayout());

		addListeners();
		setTitle("Chimera Link");
		add(viewPButton);
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
		viewPButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				updateActivePanel();
				int width = IJ.getImage().getRoi().getBounds().width;
				int height = IJ.getImage().getRoi().getBounds().height;
				if ((width>700)||(height>700)) {
					double aspectRatio = (double) width / (double) height;
					if (width>height) {
						width=700;height=(int) ((double) width/aspectRatio);
					} else {
						height=700;width= (int) ((double) height*aspectRatio);
					}					
				}
				String script = "";
				script = "windowsize " + IJ.d2s(width,0)+" "+IJ.d2s(height,0);
				chimeraNotify(tempDir+";"+script);
				IJ.runMacro("getSelectionBounds(x,y,w,h);call('ij.Prefs.set','selectedPanel.w',w);call('ij.Prefs.set','selectedPanel.h',h);");
			}
		});

		grabButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// chimera copy command description:
				// copy [ file filename ] [ format ] [ printer printername ] [ width x ] [ height y ] [ supersample n ] [ raytrace [ rtwait ] [ rtclean ]]
				updateActivePanel();
				int width = IJ.getImage().getRoi().getBounds().width;
				int height = IJ.getImage().getRoi().getBounds().height;
				((LeafPanel) getActivePanel()).getImgData().setFileDirectory("");
				fileName = linkIdentifyer+timeStamp();
				String script = "copy file "+tempDir+fileName+extension+ " png width "+ IJ.d2s(width,0)+" height "+ IJ.d2s(height,0)+" supersample 4";
				script +="\n"+"save "+tempDir+fileName+".py";
				chimeraNotify(tempDir+";"+script);
				store(tempDir,fileName+extension);
				((LeafPanel) getActivePanel()).getImgData().setExternalSource(fileName+".py");
				((LeafPanel) getActivePanel()).getImgData().setFileDirectory(tempDir);

			}
		});
		editButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				//updateActivePanel();
				fileName = ((LeafPanel) getActivePanel()).getImgData().getExternalSource();
				String dir = ((LeafPanel) getActivePanel()).getImgData().getFileDirectory();
				String script = "open "+dir+fileName;
				chimeraNotify(tempDir+";"+script);
			}
		});

		helpButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				IJ.showMessage("UCSF Chimera Link for FigureJ",
						"-> 'set window size' forces Chimera display to a suitable value for your panel.\n" +
						"-> 'grab view', well, grabs the Chimera view to the active panel.\n"+
						"-> 'edit', re-opens the Chimera session from the active panel.\n \n"+
						"Chimera should be running, accepting external data, and configured in FigureJ preferences."
						);
			}
		});
	}

	public void actionPerformed(ActionEvent evt) {
		// moved each case into buttons action listeners.
	}
	
	void chimeraNotify(String script) {
		runMyMacroFromJar("chimeraNotify.ijm", script);
	}

	public void close() {
	//	super.close();
		instance = null;
		Prefs.saveLocation(LOC_KEY, getLocation());
	}
}


