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
import java.io.File;

import fr.cnrs.ibmp.windows.MainWindow;

/**
 * @author Jerome Mutterer
 * @author Edda Zinck
 * 
 * this class prepares a clip board message that an excel macro can use to
 * output a selected diagram as image. the image can be linked and copied 
 * by this class to the active panel */

/* keep in mind that you need FULL PATHS in VBA (excel macro language)
for Mac this means: paths have to start with 'MacHD'. 
Furthermore VBA uses colons as path separators instead of '/' or '/'
example path: MacHD:Users:aSpecialUser:FigureJ_ExcelChartIMG1.png*/


/*
 Function GetClipboardText(nChars As Integer) As String 'read text from clip board
    Dim BufObj As MSForms.DataObject
    Set BufObj = New MSForms.DataObject
    BufObj.GetFromClipboard
    GetClipboardText = Left(BufObj.GetText, nChars) ' Get only first nChars
End Function

Sub FigureJMacro2()
    Dim monTab() As String
    monTab = Split(GetClipboardText(250), "||")    'text an den doppelpunkten in substrings zerschneiden, monTab ist jetzt  ein string-array
    If Not monTab(0) = "figureJ" Then                        'figureJ ist das keyword
      MsgBox ("no figureJ message in the clip board!")
    Else
      i = ActiveChart.Parent.Index
      ActiveSheet.Shapes(i).Line.Visible = msoFalse     'linien verbergen
      ActiveSheet.ChartObjects(i).Width = monTab(1)    'werte f�r breite + h�he aus clipboard message lesen und diagram anpassen
      ActiveSheet.ChartObjects(i).Height = monTab(2)
      If InStr(Application.OperatingSystem, "Mac") Then
        Dim myPath As String
        myPath = MacScript("return (path to home folder) as String")
        ActiveChart.Export myPath + monTab(3)                             'diagram exportieren
      Else 'the windows part is not tested!!!
        'ActiveChart.Export monTab(3)
        Set oShell = CreateObject("WScript.Shell")
        Dim myWindowsPath As String
        myWindowsPath = oShell.SpecialFolders("MyDocuments")
        Dim lastSeparator As Integer
        lastSeparator = InStrRev(myWindowsPath, ":")
        myWindowsPath = Mid(myWindowsPath, 1, lastSeparator)
        ActiveChart.Export myWindowsPath + monTab(3)
      End If
End If
End Sub
 */
public class Excel_Link extends Link implements ActionListener {

	private static final long 	serialVersionUID 	= 1L;
	private static String 		tempDir 			= System.getProperty("user.home");
	private final String 		excelSplitIndicator = "||";
	private static Frame 		instance;

	public static final String LOC_KEY = "xlsclipboardlink.loc";		//TODO check if public necessary

	public Excel_Link() {
		super();
		init();
	}

	public Excel_Link(MainWindow main) {
		super(main);
		init();
	}

	private void init() {
		linkIdentifyer = "FigureJ_ExcelChartIMG";

		if(! tempDir.endsWith(File.separator))
			tempDir += File.separator;

		fileName = linkIdentifyer+counter+extension;
		if (instance != null) {
			WindowManager.toFront(instance);
			return;
		}
		WindowManager.addWindow(this);
		instance = this;
		setLayout(new FlowLayout());
		setTitle("Excel Link");

		addButton("Generate Excel command");
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
		if (b.getName() == "Generate Excel command") {
			updateActivePanel();
			checkFileName(tempDir);
			script = "String.copy('figureJ"+IJ.runMacro("getSelectionBounds(x,y,w,h); return '"+excelSplitIndicator+"'+w+'"+excelSplitIndicator+"'+h") 
					+excelSplitIndicator;
			if (IJ.isMacOSX()) {
				script += fileName+excelSplitIndicator+"');"; 
			} else if (IJ.isWindows()) {
				script += fileName+excelSplitIndicator+"');"; 
				// script += tempDir.replace(File.separatorChar, ':')+fileName+excelSplitIndicator+"');";
				// TODO find out if copying to the temp folder would work at least here
			}
			else {
				IJ.showMessage("the excel link only works under MacOSX and XWindows");
				return;
			}

			IJ.runMacro(script);
			IJ.runMacro("getSelectionBounds(x,y,w,h);call('ij.Prefs.set','selectedPanel.w',w);call('ij.Prefs.set','selectedPanel.h',h);");
			IJ.showStatus("Excel message created");

		} else if (b.getName() == "Grab view") {
			store(tempDir,fileName);
		}
		else if (b.getName().equals("Help"))
		{
			IJ.showMessage("-> hit the 'generate Excel command' button. \n"+
					"-> switch to excel,  open your file and select your chart (click on its margin).\n" +
					"-> run the macro distributed with FigureJ: tools -> macro -> macros -> FigureJ macro -> run\n"+
					"-> go back to FigureJ and hit the 'Grab View' button. This should display your chart.");
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

