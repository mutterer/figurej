package plugIns;
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

import windows.MainWindow;

/**
 * @author Jerome Mutterer
 * @author Edda Zinck
 * 
 * This class generates a command line command for the R statistical software.
 * The command has to be pasted to the R console and creates a png from the 
 * currently open R chart. FiugreJ can grab the png and paste it into the
 * active/selected panel. *
 */

public class R_Link extends Link implements ActionListener {

	private static final long serialVersionUID = 1L;
	private static final String LOC_KEY = "rclipboardlink.loc";
	private static Frame instance;
	private static String tempDir 	=  System.getProperty("java.io.tmpdir");
	public R_Link() {
		super();
		init();
	}

	public R_Link(MainWindow main) {

		super(main);
		init();
	}

	private void init() {
		linkIdentifyer = "RPanel_";
		if (instance != null) {
			WindowManager.toFront(instance);
			return;
		}
		WindowManager.addWindow(this);
		instance = this;
		setLayout(new FlowLayout());
		setTitle("R Link");

		addButton("Generate R command");
		addButton("Grab view");
		addButton("Help");
		pack();
		Point loc = Prefs.getLocation(LOC_KEY);
		if (loc != null) {
			setLocation(loc);
		} else {
			GUI.center(this);
		}
		setVisible(true);
	}

	public void actionPerformed(ActionEvent evt) {
		String script = "";
		Button b = (Button) evt.getSource();
		if (b.getName() == "Generate R command") {
			updateActivePanel();
			fileName = linkIdentifyer+timeStamp()+".png";

			script = "String.copy('dev.copy(png, \""+tempDir+fileName+"\","+IJ.runMacro("getSelectionBounds(x,y,w,h); return 'width='+w+', height='+h") +" , type = \"quartz\", antialias=\"default\"); dev.off();');";

			IJ.runMacro(script);
			IJ.runMacro("getSelectionBounds(x,y,w,h);call('ij.Prefs.set','selectedPanel.w',w);call('ij.Prefs.set','selectedPanel.h',h);");
			IJ.showMessage("Switch to R, dislap your chart and execute the FigureJ command by pressing 'ctrl' and 'v', then enter");

		} else if (b.getName() == "Grab view") {
			//checkFileName(tempDir);
			// jm removed this, b/c this inadequately incremented the file name, making it unavailable from fj.
			store(tempDir, fileName);
		}
		else if (b.getName() == "Help") {
			IJ.showMessage("->  start R and display your chart. \n\n"+
					"->  hit 'Generate R command'. This copies a message to your clip board. \n\n"+
					"->  paste by pressing 'ctrl' + 'v' or select Edit -> Paste from the R menu. Hit the enter key. \n\n"+
					"->  go back to FigureJ: hit the 'Grab view' button. This should dislay your chart.");
		}
	}

	void addButton(String label) {
		Button button = new Button(label);
		button.addActionListener(this);
		button.setName(label);
		add(button);
	}
	public void close() {
		//super.close();
		instance = null;
		Prefs.saveLocation(LOC_KEY, getLocation());
	}
}

