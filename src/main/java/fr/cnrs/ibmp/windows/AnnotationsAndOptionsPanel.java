package fr.cnrs.ibmp.windows;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fr.cnrs.ibmp.LeafEvent;
import fr.cnrs.ibmp.LeafListener;
import fr.cnrs.ibmp.plugIns.LabelDrawer;
import fr.cnrs.ibmp.plugIns.Link;
import fr.cnrs.ibmp.treeMap.LeafPanel;
import fr.cnrs.ibmp.treeMap.SeparatorPanel;
import fr.cnrs.ibmp.utilities.Constants;
import fr.cnrs.ibmp.utilities.LabelPosition;
import fr.cnrs.ibmp.utilities.LabelType;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.gui.Toolbar;

/**
 * window opened by the options button of the panel window
 */
public class AnnotationsAndOptionsPanel extends JFrame implements LeafListener {

	private static final long serialVersionUID = 1L;

	//options
	private JButton adoptPixelsButton;
	private JButton openColorPickerButton;
	private JButton openFontsDialog;
	private JButton changeSeparatorColorButton;
	private JButton printFigure;
	private JButton drawLabelButton;
	private JButton removeLabelButton;

	private JButton newTextRoiButton;
	private JButton newArrowRoiButton;
	private JButton textOptionsButton;
	private JButton arrowOptionsButton;
	private JButton addItemToOverlayButton;
	private JButton duplicateItemButton;
	private JButton hideOverlayButton;
	private JButton showOverlayButton;

	/** Currently selected (active) LeafPanel */
	private LeafPanel activePanel;

	/** TODO Documentation */
	private MainWindow mainWindow;

	/** TODO Documentation */
	private LabelDrawer labelDraw = new LabelDrawer();

	private JPanel optionsPanel = new JPanel();
	// text labels
	private JTextField userDefinedLabelsInputField = new JTextField();
	private JLabel userDefinedLabelsMessage = new JLabel("  own labels:");
	// arrow 8599 plus superscript 8314
	// private JButton fontSpecButton = new JButton("fonts"+new
	// Character((char) 8230));
	private JTextField xLabelOffset = new JTextField("10");
	private JTextField yLabelOffset = new JTextField("10");
	private JComboBox labelPositionSelector = new JComboBox();
	private JComboBox labelTypeSelector = new JComboBox();
	// scale bars
	private JTextField xOffsScales = new JTextField("20");
	private JTextField yOffsScales = new JTextField("20");
	private JSlider scalebarSizeSlider = new JSlider();
	private JCheckBox scaleDisplayCheck = new JCheckBox();
	private JCheckBox scaleTextDisplayCheck = new JCheckBox("show value: ",
			false);
	private JLabel scaleTextValue = new JLabel("-");
	// suggested by Christian Blanck
	private JCheckBox lockScale = new JCheckBox("Lock pixel size");
	private JTextField scalebarHeight = new JTextField("10");
	private int[] a = { 1, 2, 5 };
	private double[] b = { 0.001, 0.01, 0.1, 1, 10, 100, 1000 };

	public AnnotationsAndOptionsPanel(int xLocation, int yLocation) {

		this.setLocation(xLocation, yLocation);
		this.setTitle("Options");

		initButtons();
		initLabelGUI();
		initScalebarGUI();
		addOptionsWindowToolTips();

		// labels layout
		JPanel labelsPanel = new JPanel(new GridLayout(3, 2));
		JPanel labelsOffsets = new JPanel();

		labelsPanel.setPreferredSize(new Dimension(253, 110));
		labelsPanel.setBorder(BorderFactory
				.createTitledBorder("Panel Labels"));

		labelsOffsets.add(new JLabel(new ImageIcon(getClass().getResource(
				"/imgs/ALeft1.png"))));
		labelsOffsets.add(xLabelOffset);
		labelsOffsets.add(new JLabel(new ImageIcon(getClass().getResource(
				"/imgs/ATop1.png"))));
		labelsOffsets.add(yLabelOffset);

		labelsPanel.add(drawLabelButton);
		labelsPanel.add(removeLabelButton);
		labelsPanel.add(labelTypeSelector);
		labelsPanel.add(userDefinedLabelsInputField);
		labelsPanel.add(labelPositionSelector);
		labelsPanel.add(labelsOffsets);

		JPanel scalebarsPanel = new JPanel(new GridLayout(3, 1));
		JPanel scalebarsVisibilityAndSizePanel = new JPanel(new FlowLayout(
				FlowLayout.LEFT));
		JPanel scalebarsOffsetsPanel = new JPanel(new FlowLayout(
				FlowLayout.LEFT));
		JPanel scalebarsTextVisibility = new JPanel(new FlowLayout(
				FlowLayout.LEFT));

		scalebarsPanel.setBorder(BorderFactory
				.createTitledBorder("Panel Scale bars"));

		scalebarsVisibilityAndSizePanel.add(scaleDisplayCheck);
		scalebarsVisibilityAndSizePanel.add(scalebarSizeSlider);

		scalebarsTextVisibility.add(scaleTextDisplayCheck);
		scalebarsTextVisibility.add(scaleTextValue);
		scalebarsOffsetsPanel.add(new JLabel("height:"));
		scalebarsOffsetsPanel.add(scalebarHeight);

		scalebarsOffsetsPanel.add(new JLabel("   "));
		scalebarsOffsetsPanel.add(new JLabel(new ImageIcon(getClass()
				.getResource("/imgs/iconBarRight.png"))));
		scalebarsOffsetsPanel.add(xOffsScales);
		scalebarsOffsetsPanel.add(new JLabel(new ImageIcon(getClass()
				.getResource("/imgs/iconBarLow.png"))));
		scalebarsOffsetsPanel.add(yOffsScales);

		scalebarsPanel.add(scalebarsVisibilityAndSizePanel);
		scalebarsPanel.add(scalebarsTextVisibility);
		scalebarsPanel.add(scalebarsOffsetsPanel);

		// Overlay Items

		JPanel overlayItemsPanel = new JPanel(new GridLayout(3, 2));
		overlayItemsPanel.setPreferredSize(new Dimension(253, 110));
		overlayItemsPanel.setBorder(BorderFactory
				.createTitledBorder("Overlay Items"));
		overlayItemsPanel.add(newTextRoiButton);
		overlayItemsPanel.add(newArrowRoiButton);
		overlayItemsPanel.add(addItemToOverlayButton);
		overlayItemsPanel.add(duplicateItemButton);
		overlayItemsPanel.add(hideOverlayButton);
		overlayItemsPanel.add(showOverlayButton);

		// Miscellaneous Items and temporary test/debug items

		// JPanel miscItemsPanel = new JPanel(new
		// GridLayout(2+(int)Prefs.get("figurej.debug", 0), 1));
		JPanel miscItemsPanel = new JPanel(new GridLayout(4, 1));
		miscItemsPanel.setBorder(BorderFactory.createTitledBorder("Misc."));

		// if (Prefs.get("figurej.debug", 0)==1)
		// miscItemsPanel.add(debugButton);
		Prefs.set("figurej.lockPixelSize", false);

		// TODO: enable this when workflow is made clear.
		// miscItemsPanel.add(lockScale);
		miscItemsPanel.add(openColorPickerButton);
		//miscItemsPanel.add(openFontsDialog);
		miscItemsPanel.add(adoptPixelsButton);
		miscItemsPanel.add(changeSeparatorColorButton);
		miscItemsPanel.add(printFigure);

		// Fill the options panel
		optionsPanel.setLayout(new BoxLayout(optionsPanel,
				BoxLayout.PAGE_AXIS));
		optionsPanel.add(labelsPanel);
		optionsPanel.add(scalebarsPanel);
		optionsPanel.add(overlayItemsPanel);
		optionsPanel.add(miscItemsPanel);

		this.add(optionsPanel);
		this.setBackground(Constants.backgroundColor);
		pack();
	}

	/**
	 * settings of the buttons, combo boxes labels .. handling text label
	 * design
	 */
	private void initLabelGUI() {
		disableUserDefinedLabelsInputField(true);

		Iterator<String> itr = LabelDrawer.getPositionTypes().iterator();
		while (itr.hasNext())
			labelPositionSelector.addItem(itr.next());
		labelPositionSelector.setSelectedItem("TopLeft");

		itr = LabelDrawer.getLabelTypes().iterator();
		while (itr.hasNext())
			labelTypeSelector.addItem(itr.next());
		labelTypeSelector.setSelectedItem("ABC");

		labelTypeSelector.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if ((labelTypeSelector.getSelectedItem() + "")
						.equals(LabelType.userDefined.toString()))
					disableUserDefinedLabelsInputField(false);
				else
					disableUserDefinedLabelsInputField(true);
			}
		});
	}

	private void initScalebarGUI() {
		scalebarSizeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				scaleDisplayCheck.setSelected(true);
				int xOff = stringToNr(xOffsScales, activePanel.getW() / 3);
				scalebarSizeSlider.setMaximum(activePanel.getW() - xOff);
				scalebarSizeSlider.setToolTipText("scale bar length");
				double d = getClosestScaleBar();
				activePanel.setScalebar(mainWindow.getImagePlus(), xOff,
						stringToNr(yOffsScales, activePanel.getH() / 2), d,
						stringToNr(scalebarHeight, 30));
				mainWindow.draw();
				IJ.showStatus(IJ.d2s(d
						* activePanel.getImgData().getPixelWidth(), 2)
						+ " " + activePanel.getImgData().getUnit());
				// TODO:
				scaleTextValue.setText(activePanel.getShortScaleBarText());
			}
		});
		scaleDisplayCheck.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (!scaleDisplayCheck.isSelected()) {
					activePanel.removeScalebar(mainWindow.getImagePlus()
							.getOverlay());
					mainWindow.draw();
				} else {
					int xOff = stringToNr(xOffsScales,
							activePanel.getW() / 2);
					scalebarSizeSlider.setMaximum(activePanel.getW() - xOff);
					scalebarSizeSlider.setToolTipText("scale bar length");
					double d = getClosestScaleBar();
					activePanel.setScalebarColor(Toolbar
							.getForegroundColor());
					activePanel.setScalebar(
							mainWindow.getImagePlus(),
							xOff,
							stringToNr(yOffsScales, activePanel.getH() / 2),
							d, stringToNr(scalebarHeight, 30));
					mainWindow.draw();
					IJ.showStatus(IJ.d2s(d
							* activePanel.getImgData().getPixelWidth(), 2)
							+ " " + activePanel.getImgData().getUnit());
				}

			}
		});
		scaleTextDisplayCheck.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (!scaleTextDisplayCheck.isSelected()) {
					activePanel.setScaleBarTextVisible(false);
					activePanel.setScalebarLabelJustification(-1);
					mainWindow.draw();
				} else {
					activePanel.setScaleBarTextVisible(true);
					activePanel.setScaleBarTextFont(new Font(TextRoi
							.getFont(), TextRoi.getStyle(), TextRoi
							.getSize()));
					mainWindow.draw();
				}

			}
		});
		lockScale.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (lockScale.isSelected()) {
					Prefs.set("figurej.lockPixelSize", true);
					double pixelWidth = activePanel.getImgData()
							.getPixelWidth();
					Prefs.set("figurej.lockedPixelSize", pixelWidth);
					lockScale
							.setText("<html>Lock Pixel size: <font color=red>LOCKED</font></html>");

				} else {
					Prefs.set("figurej.lockPixelSize", false);
					lockScale.setText("<html>Lock Pixel size</html>");

				}

			}
		});
	}

	/** display a new text label on the active panel */
	public void addPanelLabel(boolean reset, boolean backwards) {
		if (labelTypeSelector.getSelectedItem().toString()
				.equals(LabelType.userDefined.toString()))
			labelDraw.setUserLabels(userDefinedLabelsInputField.getText());

		String label = labelDraw.getLabel(
				labelTypeSelector.getSelectedItem() + "", reset, backwards);
		activePanel.setLabel(
				mainWindow.getImagePlus(),
				label,
				stringToNr(xLabelOffset, activePanel.getW() / 2),
				stringToNr(yLabelOffset, activePanel.getH() / 2),
				LabelPosition.valueOf(labelPositionSelector
						.getSelectedItem() + ""));
	}

	/** allow the user to type own label strings in a text field or not */
	private void disableUserDefinedLabelsInputField(boolean disable) {
		if (disable) {
			userDefinedLabelsMessage.setEnabled(false);
			userDefinedLabelsInputField.setVisible(false);
			userDefinedLabelsInputField.setToolTipText(null);
		} else {
			userDefinedLabelsMessage.setEnabled(true);
			userDefinedLabelsInputField.setVisible(true);
			userDefinedLabelsInputField
					.setToolTipText("insert your own labels, separate by semicolon");
		}
	}

	/** find good value depending on slider position */
	private double getClosestScaleBar() {
		double dist = 1000000;
		double[] c = new double[2];
		double mousedist = (scalebarSizeSlider.getMaximum() - scalebarSizeSlider
				.getValue()) * activePanel.getImgData().getPixelWidth();
		for (int i = 0; i < b.length; i++)
			for (int j = 0; j < a.length; j++) {
				double currentdist = Math.abs(mousedist - a[j] * b[i]);
				if (currentdist < dist) {
					c[0] = a[j];
					c[1] = b[i];
					dist = currentdist;
				}
			}
		return (double) ((c[0] * c[1]) / activePanel.getImgData()
				.getPixelWidth());
	}

	/** set info messages that pop up if mouse stays longer over a component */
	private void addOptionsWindowToolTips() {
		drawLabelButton.setToolTipText("add text label to panel");
		removeLabelButton.setToolTipText("delete text label from panel");
		labelTypeSelector.setToolTipText("choose label type");
		labelPositionSelector.setToolTipText("choose label position");
		xLabelOffset.setToolTipText("vertical distance to panel border");
		yLabelOffset.setToolTipText("horizontal distance to panel border");
		xOffsScales.setToolTipText("distance to right panel border");
		yOffsScales.setToolTipText("distance to lower panel border");
		scalebarHeight.setToolTipText("scalebar height");
		changeSeparatorColorButton
				.setToolTipText("Update separators color to current foreground color");
	}

	/**
	 * @param textField
	 *            textfield to read out
	 * @param max
	 *            maximum value; larger values are replaced by 10
	 * @return string input of the text field converted to number; if input
	 *         > max or not a number: 10 per default
	 */
	private int stringToNr(JTextField textField, int max) {
		int i = 10;
		try {
			i = Integer.parseInt(textField.getText());
		} catch (NumberFormatException f) {
			i = 10;
		} finally {
			if (i < 0 || i > max)
				i = 10;
		}
		;
		return i;
	}

	private void initButtons() {
	// moved to options dialog
			adoptPixelsButton = new JButton("adopt current panel pixels");
			adoptPixelsButton.addActionListener(new ActionListener() {
				// let the active panel grab the pixels drawn on his area and
				// store them
				// is useful if somebody draws with non figureJ tools on the
				// result window and wants to store these changes
				public void actionPerformed(ActionEvent e) {
					if (activePanel != null)
						activePanel.setPixels(mainWindow.getImagePlus());
				}
			});

			changeSeparatorColorButton = new JButton("update separator color");
			changeSeparatorColorButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int newColor = Toolbar.getForegroundColor().getRGB();
					SeparatorPanel.setColor(newColor);
					if (mainWindow != null)
						mainWindow.draw();
				}
			});
			openColorPickerButton = new JButton("open color picker");
			openColorPickerButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					IJ.runPlugIn("ij.plugin.frame.ColorPicker", "");
				}
			});
			
			openFontsDialog = new JButton("open fonts dialog");
			openFontsDialog.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					IJ.runPlugIn("ij.plugin.SimpleCommands", "fonts");
				}
			});

			newTextRoiButton = new JButton("" + new Character((char) 8314)
					+ " T  text");
			newTextRoiButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					boolean fontsWindowsOpen = WindowManager.getFrame("Fonts") != null;
					if (!fontsWindowsOpen) {
						IJ.run("Fonts...");
					}
					IJ.selectWindow("FigureJ");
					IJ.run("Select None");
					IJ.setTool("text");

	/*					ImagePlus imp = IJ.getImage();
					imp.createNewRoi((int)(imp.getWidth()/2), (int)(imp.getHeight()/2));
					TextRoi r = (TextRoi) imp.getRoi();					
					String str = "Text..."; 
					char[] charArray = str.toCharArray();
					for (char c : charArray) r.addChar(c);
					r.setLocation((int)(imp.getWidth()/2), (int)(imp.getHeight()/2));
					imp.setRoi(r);
	*/
					IJ.showStatus("Text tool selected");

					
				}
			});

			textOptionsButton = new JButton("" + new Character((char) 8230));
			textOptionsButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					IJ.run("Fonts...");
				}
			});

			newArrowRoiButton = new JButton("" + new Character((char) 8314)
					+ new Character((char) 8599) + " arrow");
			newArrowRoiButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
								
					IJ.selectWindow("FigureJ");
					IJ.run("Select None");

					Window fr[] = Window.getWindows();
					boolean arrowsOptionsOpen = false;
					for (int i = 0; i < fr.length; i++) {
						if (fr[i].toString().indexOf("Arrow Tool") > 0
								&& fr[i].isVisible())
							arrowsOptionsOpen = true;
					}
					if (!arrowsOptionsOpen)
						IJ.doCommand("Arrow Tool...");
					IJ.showStatus("Arrow tool selected");

				}

			});

			arrowOptionsButton = new JButton("" + new Character((char) 8230));
			arrowOptionsButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					IJ.doCommand("Arrow Tool...");
				}
			});

			addItemToOverlayButton = new JButton("add");
			addItemToOverlayButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ImagePlus imp = IJ.getImage();
					IJ.run(imp, "Add Selection...", "");
					IJ.run(imp, "Select None", "");
					// String macro = "fc= toHex(getValue('rgb.foreground')); while (lengthOf(fc)<6) {fc='0'+fc;} run('Add Selection...', 'stroke=#'+fc+' fill=none');run('Select None');";
					// IJ.runMacro(macro);
				}
			});

			duplicateItemButton = new JButton("duplicate");
			duplicateItemButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ImagePlus imp = IJ.getImage();
					Roi roi = imp.getRoi();
					if (roi != null) {
						imp.saveRoi();
						// IJ.run(imp, "Add Selection...", "");
//						String macro = "shift=30;getSelectionBounds(x, y, width, height);"
//								+ "fc= toHex(getValue('rgb.foreground')); while (lengthOf(fc)<6) {fc='0'+fc;} run('Add Selection...', 'stroke=#'+fc+' fill=none');run('Select None');"
//								+ "run('Select None');run('Restore Selection', '');setSelectionLocation(x+ shift, y+ shift/1.5);";
						IJ.run(imp, "Add Selection...", "");
						String macro = "run('Restore Selection', '');shift=30;getSelectionBounds(x, y, width, height);setSelectionLocation(x+ shift, y+ shift/1.5);";
						IJ.runMacro(macro);
					}
				}
			});

			hideOverlayButton = new JButton("hide");
			hideOverlayButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					IJ.run("Hide Overlay");
				}
			});

			showOverlayButton = new JButton("show");
			showOverlayButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					IJ.run("Show Overlay");
				}
			});

			printFigure = new JButton("print at actual size");
			printFigure.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					double res = mainWindow.getDPI();
					IJ.run("Select None");
					Link.runMyMacroFromJar("print_to_scale.ijm", IJ.d2s(res));
				}
			});

			drawLabelButton = new JButton("draw");
			drawLabelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// reset flag if alt is pressed, to reset the label counter
					boolean reset = (e.getModifiers() & ActionEvent.ALT_MASK) != 0;
					boolean backwards = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;

					addPanelLabel(reset, backwards);
					boolean fontsWindowsOpen = WindowManager.getFrame("Fonts") != null;
					if (!fontsWindowsOpen) {
						IJ.run("Fonts...");
						IJ.selectWindow("FigureJ");
					}
					mainWindow.draw();

				}
			});

			removeLabelButton = new JButton("remove");
			removeLabelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					activePanel.removeLabel(mainWindow.getImagePlus()
							.getOverlay());
					mainWindow.draw();
				}
			});
	}
	
	private void initTooltips() {
		newTextRoiButton.setToolTipText("add a new text");
		newArrowRoiButton.setToolTipText("add a new arrow");
		arrowOptionsButton.setToolTipText("open arrows options");
		textOptionsButton.setToolTipText("open text options");
		hideOverlayButton.setToolTipText("hide all overlay items");
		showOverlayButton.setToolTipText("show all overlay items");
		addItemToOverlayButton
				.setToolTipText("add current item to overlay");
		duplicateItemButton
				.setToolTipText("add current item to overlay and duplicates it");
	}

	@Override
	public void leafSelected(LeafEvent e) {
		activePanel = (LeafPanel) e.getSource();

		scaleDisplayCheck.setSelected(activePanel.isScalebarVisible());
		scaleTextDisplayCheck.setSelected(activePanel.isScalebarTextVisible());
		scaleTextValue.setText(activePanel.getShortScaleBarText());
	}

	@Override
	public void leafDeselected(LeafEvent e) { /* NB */ }

	@Override
	public void leafResized(LeafEvent e) { /* NB */ }

	@Override
	public void leafCleared(LeafEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void leafRemoved(LeafEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void leafSplit(LeafEvent e) {
		// TODO Auto-generated method stub
		
	}

}
