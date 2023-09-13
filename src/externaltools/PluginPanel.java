package externaltools;

/*
 * @author Edda Zinck
 * @author Jerome Mutterer
 * (c) IBMP-CNRS
 * 
 */
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import figure.ContainerPanel;
import figure.FigureWindow;
import figure.Panel;
import ij.IJ;

public class PluginPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private JButton genericButton = new JButton("Generic");
	private JButton chimeraButton = new JButton("Chimera");
	private JButton inkscapeButton = new JButton("Inkscape");
	private JButton scriptButton = new JButton("Script");
	private JButton exportSVGBtn = new JButton("to *.svg");

	private Link generic, chimera, inkscape, script;

	public PluginPanel() {
		this.setBorder(BorderFactory.createTitledBorder("External Tools"));
		this.setLayout(new GridLayout(2, 2));
		this.add(genericButton);
		this.add(scriptButton);
		this.add(chimeraButton);
		this.add(inkscapeButton);
		// this.add(exportSVGBtn);

	}

	public void setFigureWindow(FigureWindow w) {
		addListeners(w);
		genericButton.setEnabled(true);
		scriptButton.setEnabled(true);
		chimeraButton.setEnabled(true);
		inkscapeButton.setEnabled(true);
		exportSVGBtn.setEnabled(true);
	}

	public void resetFigureWindow() {

		if (genericButton.getActionListeners().length > 0)
			genericButton.removeActionListener(genericButton.getActionListeners()[0]);
		if (scriptButton.getActionListeners().length > 0)
			scriptButton.removeActionListener(scriptButton.getActionListeners()[0]);
		if (chimeraButton.getActionListeners().length > 0)
			chimeraButton.removeActionListener(chimeraButton.getActionListeners()[0]);
		if (inkscapeButton.getActionListeners().length > 0)
			inkscapeButton.removeActionListener(inkscapeButton.getActionListeners()[0]);

		genericButton.setEnabled(false);
		scriptButton.setEnabled(false);
		chimeraButton.setEnabled(false);
		inkscapeButton.setEnabled(false);

	}

	private void addListeners(final FigureWindow w) {

		chimeraButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (chimera == null)
					chimera = new Chimera_Link(w);
				chimera.setVisible(true);
			}
		});
		genericButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (generic == null)
					generic = new Generic_Link(w);
				generic.setVisible(true);
			}
		});

		inkscapeButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (inkscape == null)
					inkscape = new Inkscape_Link(w);
				inkscape.setVisible(true);
			}
		});

		scriptButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (script == null)
					script = new ScriptPanel_Link(w);
				script.setVisible(true);
			}

		});

		exportSVGBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// export to svg moved to figureIO
			}

		});

	}
}
