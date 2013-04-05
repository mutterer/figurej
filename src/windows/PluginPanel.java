package windows;

/*
 * @author Edda Zinck
 * @author Jerome Mutterer
 * (c) IBMP-CNRS
 * 
 */
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import plugIns.Chimera_Link;
import plugIns.Generic_Link;
import plugIns.Inkscape_Link;
import plugIns.Link;
// import plugIns.Pymol_Link;
import plugIns.R_Link;
import plugIns.ScriptPanel_Link;

public class PluginPanel extends JPanel{
	private static final long serialVersionUID = 1L;
	// private JButton pyMolButton = new JButton("PyMOL");
	// private JButton excelButton = new JButton("Excel");
	private JButton genericButton = new JButton("Generic");
	private JButton rButton		= new JButton("R");
	private JButton chimeraButton = new JButton("Chimera");
	private JButton inkscapeButton = new JButton("Inkscape");
	private JButton scriptButton = new JButton("Script");

	private Link generic, r, chimera, inkscape, script;

	public PluginPanel(final MainWindow w) {
		this.setLayout(new GridLayout(3	, 2));
		// this.add(pyMolButton);
		this.add(genericButton);
		this.add(scriptButton);
		this.add(chimeraButton);
		this.add(inkscapeButton);
		this.add(rButton);
		//TODO: re-enable when export from excel works.
		// this.add(excelButton);
		this.setBorder(BorderFactory.createTitledBorder("External Tools"));

		addListeners(w);
	}

	private void addListeners(final MainWindow w) {
//		pyMolButton.addActionListener(new ActionListener() {
//
//			@Override
//			public void actionPerformed(ActionEvent arg0) {
//				if(py==null)
//					py = new Pymol_Link(w);
//				py.setVisible(true);				
//			}
//		});
		
		chimeraButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(chimera==null)
					chimera = new Chimera_Link(w);
				chimera.setVisible(true);				
			}
		});
		genericButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(generic==null)
					generic = new Generic_Link(w);
				generic.setVisible(true);				
			}
		});
		
		inkscapeButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(inkscape==null)
					inkscape = new Inkscape_Link(w);
				inkscape.setVisible(true);				
			}
		});

//		excelButton.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent arg0) {
//				if(e==null)
//					e = new Excel_Link(w);
//				e.setVisible(true);	
//			}
//		});

		rButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(r==null)
					r = new R_Link(w);
				r.setVisible(true);				
			}

		});
		scriptButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(script==null)
					script = new ScriptPanel_Link(w);
				script.setVisible(true);				
			}

		});
	}
}
