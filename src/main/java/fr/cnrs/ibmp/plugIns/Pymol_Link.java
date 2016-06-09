package fr.cnrs.ibmp.plugIns;

import ij.IJ;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GUI;

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.JButton;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import fr.cnrs.ibmp.windows.MainWindow;

/**
 * @author Jerome Mutterer
 * @author Edda Zinck
 * this class controls a PyMOL server instance and allows to create an image that is
 * used to fill the active panel with PyMOL visualizations.
 */

public class Pymol_Link extends Link implements ActionListener {

	public static final String LOC_KEY 			= "pymollink.loc";
	private static final long serialVersionUID 	= 1L;
	private static String tempDir 				=  System.getProperty("java.io.tmpdir");
	private static Frame instance;
	private JButton viewPButton = new JButton("Format viewport");
	private JButton rayTButton 	= new JButton("Raytrace view");
	private JButton grabButton 	= new JButton("Grab view");
	private JButton helpButton	= new JButton("Help");

	//TODO modifiers?!
	String jeditUserPrefs; 
	String[] portSettings 		= new String[3];



	public Pymol_Link() {
		super();
		init();
	}

	public Pymol_Link(MainWindow main) {
		super(main);
		init();
	}

	private void init() {
		linkIdentifyer = "figureJ_PymolIMG";
		fileName = linkIdentifyer+counter+extension;
		if (instance != null) {
			WindowManager.toFront(instance);
			return;
		}
		WindowManager.addWindow(this);
		instance = this;
		setLayout(new FlowLayout());

		addListeners();
		setTitle("Pymol Link");

		add(viewPButton);
		add(rayTButton);
		add(grabButton);
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
				String script = "";
				script = "viewport " + IJ.runMacro("getSelectionBounds(x,y,w,h); return ''+w+','+h");
				pymolNotify(script);
				IJ.runMacro("getSelectionBounds(x,y,w,h);call('ij.Prefs.set','selectedPanel.w',w);call('ij.Prefs.set','selectedPanel.h',h);");
			}
		});

		rayTButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				String script = "";
				script = "ray " + IJ.runMacro("getSelectionBounds(x,y,w,h); return ''+w+','+h");
				pymolNotify(script);
				IJ.runMacro("getSelectionBounds(x,y,w,h);call('ij.Prefs.set','selectedPanel.w',w);call('ij.Prefs.set','selectedPanel.h',h);");
			}
		});

		grabButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				checkFileName(tempDir);
				String script = "png " + tempDir + fileName;
				IJ.showStatus(script);

				pymolNotify(script);
				IJ.wait(1000); 

				store(tempDir, fileName);
			}
		});

		helpButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				IJ.showMessage("-> start PyMOL in server mode and display your content.\n"+
						"-> go back to FigureJ and hit 'Format viewport' to adapt the size.\n" +
						"-> in case you need a high quality image, click 'raytrace fiew'.\n" +
						"-> hit 'grab view' to display your content in FigureJ.\n" +
						" \n"+" \n"+
						"-> server mode:\n"+
						"MAC: open your terminal window and type: \n cd /Applications/MacPyMOL.app/Contents/MacOS/; ./MacPyMOL -R;\n \n"+
						" hit enter. PyMOL should start.\n If PyMOL is not installed in Applications, adapt the path.\n \n"+
						"Windows: open your command line tool and type:\n cd <insert path to your PyMOL>; pymol -r;\n hit enter."
						);
			}
		});
	}

	public void actionPerformed(ActionEvent evt) {
		String script = "";
		Button b = (Button) evt.getSource();
		if (b.getName() == "Format viewport") {
			updateActivePanel();
			script = "viewport " + IJ.runMacro("getSelectionBounds(x,y,w,h); return ''+w+','+h");
			pymolNotify(script);
		} else if (b.getName() == "Raytrace view") {
			script = "ray " + IJ.runMacro("getSelectionBounds(x,y,w,h); return ''+w+','+h");
			pymolNotify(script);
		} else if (b.getName() == "Grab view") {
			script = "png " + tempDir + fileName;
			IJ.showStatus(script);

			pymolNotify(script);
			IJ.wait(1000); 
			IJ.runMacro("id=getImageID;setBatchMode(1);open('" + tempDir + fileName + "');run('Copy');selectImage(id);run('Paste');");
			store(tempDir,fileName);

		}
	}

	void pymolNotify(String script) {
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		try {
			config.setServerURL(new URL("http://localhost:9123/RPC2"));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);
			Object[] params = new Object[] {script};
			client.execute("do", params);
		} catch (Exception e) {
			IJ.log("PyMol link error. Restart PyMol in server mode"); }
	}

	public void close() {
	//	super.close();
		instance = null;
		Prefs.saveLocation(LOC_KEY, getLocation());
	}
}


