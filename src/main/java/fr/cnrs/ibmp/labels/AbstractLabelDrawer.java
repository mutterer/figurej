package fr.cnrs.ibmp.labels;

import java.awt.Font;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import fr.cnrs.ibmp.treeMap.LeafPanel;
import ij.IJ;
import ij.gui.TextRoi;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/**
 * Draws labels on the active panel.
 * 
 * TODO Is this ever used?
 * 
 * @author Edda Zinck
 * @author Stefan Helfrich
 */
public abstract class AbstractLabelDrawer extends HashSet<Integer> implements
	LabelDrawer
{

	private int yOffset = 10;
	private int xOffset = 10;
	private LabelPosition pos = LabelPosition.TopLeft;
	private LeafPanel activePanel;

	protected int counter = -1;
	protected int maxCounter = 26;

	protected boolean reversedOrder = false;
	
	private String color = "black";
	private ImageProcessor ip = new ColorProcessor(10, 10);

//	public void draw(final MouseEvent e, final LeafPanel activePanel) {
//		this.activePanel = activePanel;
//		int x;
//		int y;
//		if(type == LabelType.userDefined) {
//			x = e.getX();		// TODO check
//			y = e.getY();
//		}
//
//		String label = next();
//
//		x = getX(label);
//		y = getY(label);
//
//		String c = "FFFFFF";
//		if(color.equals("black"))
//			c= "000000";
//
//		String macro = "setFont(\"user\"); makeText('"+label+"',"+x+","+y+");run(\"Add Selection...\", \"stroke=&"+c+"\"); run(\"Select None\");";
//		IJ.runMacro(macro);
//		//IJ.runMacro("setColor("+color+"); setFont(\"user\"); makeText("+getLabel()+","+x+","+y+");");
//	}

	/**
	 * TODO Documentation
	 * 
	 * @param activePanel
	 * @param xOffset
	 * @param yOffset
	 * @param pos
	 * @param type
	 */
	@Deprecated
	public void setValues(LeafPanel activePanel, String xOffset, String yOffset, String pos, String type)
	{
		setActivePanel(activePanel);
		setXOffset(Integer.parseInt(xOffset));
		setYOffset(Integer.parseInt(yOffset));
		setPos(LabelPosition.valueOf(pos));
	}

	/**
	 * TODO Documentation
	 * 
	 * @return
	 */
	public static List<String> getPositionTypes() {
		List<String> l = new ArrayList<String>();

		LabelPosition[] p = LabelPosition.values();
		for(LabelPosition pos:p)
			l.add(pos.toString());
		return l;
	}

	/**
	 * TODO Documentation
	 * 
	 * @return
	 */
	public static List<String> getLabelTypes() {
		List<String> l = new ArrayList<String>();

		LabelType[] t = LabelType.values();
		for(LabelType typ:t)
			l.add(typ.toString());
		return l;
	}


	/**
	 * TODO Documentation
	 * 
	 * @param label text to display on the labeled panel
	 * @return y distance between the left coordinate of the panel and the fist
	 *         label letter
	 */
	public int getX(String label) {
		if (pos == LabelPosition.TopLeft || pos == LabelPosition.BottomLeft)
			return activePanel.getX() + xOffset;

		ip.setFont(new Font(TextRoi.getFont(), TextRoi.getStyle(), TextRoi
			.getSize()));
		return activePanel.getX() + activePanel.getW() - xOffset - ip
			.getStringWidth(label);
	}

	/**
	 * TODO Documentation
	 * 
	 * @param label text to display on the labeled panel
	 * @return x distance between the left coordinate of the panel and the fist
	 *         label letter
	 */
	public int getY(String label) {
		if (pos == LabelPosition.TopLeft || pos == LabelPosition.TopRight)
			return activePanel.getY() + yOffset;

		ip.setFont(new Font(TextRoi.getFont(), TextRoi.getStyle(), TextRoi
			.getSize()));
		return activePanel.getY() + activePanel.getH() - yOffset - ip
			.getFontMetrics().getHeight();
	}

	/**
	 * TODO Documentation
	 */
	public void decreaseLabelCounter() {
		decreaseLabelCounter(1);
	}

	/**
	 * TODO Documentation
	 * 
	 * @param byValue
	 */
	public void decreaseLabelCounter(final int byValue) {
		this.counter -= byValue;
	}

	/**
	 * @param c indicates which letter is drawn: a, A, I or 1 if -1; b, B .. if 0 and so on */
	public void setCount(int c) {
		counter = c;
	}

	
	/**
	 * @return the yOffset
	 */
	public int getYOffset() {
		return yOffset;
	}

	
	/**
	 * @param yOffset the yOffset to set
	 */
	public void setYOffset(int yOffset) {
		this.yOffset = yOffset;
	}

	
	/**
	 * @return the xOffset
	 */
	public int getXOffset() {
		return xOffset;
	}

	
	/**
	 * @param xOffset the xOffset to set
	 */
	public void setXOffset(int xOffset) {
		this.xOffset = xOffset;
	}

	
	/**
	 * @return the pos
	 */
	public LabelPosition getPos() {
		return pos;
	}

	
	/**
	 * @param pos the pos to set
	 */
	public void setPos(LabelPosition pos) {
		this.pos = pos;
	}

	/**
	 * @return the activePanel
	 */
	public LeafPanel getActivePanel() {
		return activePanel;
	}

	/**
	 * @param activePanel the activePanel to set
	 */
	public void setActivePanel(LeafPanel activePanel) {
		this.activePanel = activePanel;
	}

	@Override
	public Iterator<Integer> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String next() {
		return "-1";
	}

	@Override
	public boolean hasNext() {
		return counter < maxCounter;
	}

}
