package fr.cnrs.ibmp.plugIns;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;

import fr.cnrs.ibmp.windows.MainWindow;
import ij.IJ;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GUI;

/**
 * This class prepares a clip board message that an excel macro can use to
 * output a selected diagram as image. the image can be linked and copied 
 * by this class to the active panel.
 * 
 * @author Jerome Mutterer
 * @author Edda Zinck
 */

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
public class Excel_Link extends Link {

	private static final long serialVersionUID = 1L;
	private static String tempDir = System.getProperty("user.home");
	private final String excelSplitIndicator = "||";
	private static Frame instance;

	public static final String LOC_KEY = "xlsclipboardlink.loc";		//TODO check if public necessary

	private JButton generateExcelCommandButton = new JButton("Generate Excel command");
	private JButton grabButton = new JButton("Grab view");
	private JButton helpButton = new JButton("Help");

	/**
	 * TODO Documentation
	 */
	public Excel_Link() {
		super();
		init();
	}

	/**
	 * TODO Documentation
	 * 
	 * @param main
	 */
	public Excel_Link(MainWindow main) {
		super(main);
		init();
	}

	private void init() {
		linkIdentifier = "FigureJ_ExcelChartIMG";

		if(! tempDir.endsWith(File.separator))
			tempDir += File.separator;

		fileName = linkIdentifier+counter+extension;
		if (instance != null) {
			WindowManager.toFront(instance);
			return;
		}
		WindowManager.addWindow(this);
		instance = this;
		setLayout(new FlowLayout());
		setTitle("Excel Link");

		addListeners();
		add(generateExcelCommandButton);
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

	/**
	 * TODO Documentation
	 */
	private void addListeners() {
		generateExcelCommandButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String script = "";
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
					IJ.showMessage("Excel_Link only supported on MacOSX and Windows");
					return;
				}

				IJ.runMacro(script);
				IJ.runMacro("getSelectionBounds(x,y,w,h);call('ij.Prefs.set','selectedPanel.w',w);call('ij.Prefs.set','selectedPanel.h',h);");
				IJ.showStatus("Excel message created");
			}
		});

		grabButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				store(tempDir,fileName);
			}
		});

		helpButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				IJ.showMessage("-> hit the 'generate Excel command' button. \n"+
						"-> switch to excel,  open your file and select your chart (click on its margin).\n" +
						"-> run the macro distributed with FigureJ: tools -> macro -> macros -> FigureJ macro -> run\n"+
						"-> go back to FigureJ and hit the 'Grab View' button. This should display your chart.");
			}
		});
	}

	/**
	 * TODO Documentation
	 */
	public void close() {
		//super.close();
		instance = null;
		Prefs.saveLocation(LOC_KEY, getLocation());
	}

}
