/**
 * 
 */
package fr.cnrs.ibmp.windows;

import java.awt.Button;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.event.EventListenerList;

import fr.cnrs.ibmp.NewFigureEvent;
import fr.cnrs.ibmp.NewFigureListener;
import fr.cnrs.ibmp.OpenFigureEvent;
import fr.cnrs.ibmp.OpenFigureListener;
import fr.cnrs.ibmp.SaveFigureEvent;
import fr.cnrs.ibmp.SaveFigureListener;
import fr.cnrs.ibmp.utilities.Constants;
import ij.WindowManager;


/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich
 */
public class NewOpenSaveFrame extends JFrame {

	private EventListenerList listeners = new EventListenerList();

	private Button buttonNew;
	private Button buttonOpen;
	private Button buttonSave;

	/**
	 * @throws HeadlessException
	 */
	public NewOpenSaveFrame() {
		super();
		
		initialize();
	}

	private void initialize() {
		setTitle(Constants.title);
		setLayout(new GridLayout(2, 3));
		setLocation(0, 0);

		// handle the 3 buttons of this window
		buttonNew = new Button("New");
		buttonOpen = new Button("Open");
		buttonSave = new Button("Save");

		setupButtonsActions();

		buttonNew.setPreferredSize(new Dimension(50, 22));
		buttonOpen.setPreferredSize(new Dimension(50, 22));
		buttonSave.setPreferredSize(new Dimension(50, 22));

		add(buttonNew);
		add(buttonOpen);
		add(buttonSave);
		
		Label l = new Label(Constants.version());
		l.setFont(new Font("sanserif", Font.PLAIN, 9));
		l.setAlignment(Label.RIGHT);
		
		// TODO Define a proper layout
		add(new Label(""));
		add(new Label(""));
		add(l);
		setBackground(Constants.backgroundColor);

		pack();

		WindowManager.addWindow(this);

		setVisible(true);
	}
	
	private void setupButtonsActions() {
		buttonNew.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setOpenNewButtonsStates(false);
				notifyNewFigure(new NewFigureEvent(this));
			}

		});

		buttonOpen.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setOpenNewButtonsStates(false);
				notifyOpenFigure(new OpenFigureEvent(this));
			}

		});

		buttonSave.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				notifySaveFigure(new SaveFigureEvent(this));
			}

		});

		// Set sizes for buttons in NewOpenSave window
		buttonNew.setPreferredSize(new Dimension(90, 30));
		buttonOpen.setPreferredSize(new Dimension(90, 30));
		buttonSave.setPreferredSize(new Dimension(90, 30));
		
		// Disable Save button
		buttonSave.setEnabled(false);
	}

	/**
	 * Controls buttons in the NewOpenSave window.
	 * 
	 * if true enable buttons to open an existing or create a new
	 *            figure and disables the save button; it is assumed that no
	 *            figure is open currently; else it is assumed that an image is
	 *            already open and therefore only the safe button is enabled
	 * 
	 * @param isTrue
	 */
	protected void setOpenNewButtonsStates(boolean isTrue) {
		buttonNew.setEnabled(isTrue);
		buttonOpen.setEnabled(isTrue);
		buttonSave.setEnabled(!isTrue);
	}
	
	public void addNewFigureListener(NewFigureListener listener) {
		listeners.add(NewFigureListener.class, listener);
	}

	public void removeNewFigureListener(NewFigureListener listener) {
		listeners.remove(NewFigureListener.class, listener);
	}

	protected synchronized void notifyNewFigure(NewFigureEvent event) {
		for (NewFigureListener l : listeners.getListeners(NewFigureListener.class))
			l.newFigure(event);
	}

	public void addSaveFigureListener(SaveFigureListener listener) {
		listeners.add(SaveFigureListener.class, listener);
	}

	public void removeSaveFigureListener(SaveFigureListener listener) {
		listeners.remove(SaveFigureListener.class, listener);
	}

	protected synchronized void notifySaveFigure(SaveFigureEvent event) {
		for (SaveFigureListener l : listeners.getListeners(SaveFigureListener.class))
			l.saveFigure(event);
	}

	public void addOpenFigureListener(OpenFigureListener listener) {
		listeners.add(OpenFigureListener.class, listener);
	}

	public void removeOpenFigureListener(OpenFigureListener listener) {
		listeners.remove(OpenFigureListener.class, listener);
	}

	protected synchronized void notifyOpenFigure(OpenFigureEvent event) {
		for (OpenFigureListener l : listeners.getListeners(OpenFigureListener.class))
			l.openFigure(event);
	}
	
}
