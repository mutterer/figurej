package plugIns;

import ij.IJ;
import ij.gui.TextRoi;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.Font;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import treeMap.LeafPanel;
import utilities.LabelType;
import utilities.LabelPosition;

/**
 * @author Edda Zinck
 * this class draws labels on the active panel
 */
public class LabelDrawer {

	private int 	yOffset 	= 10;
	private int 	xOffset 	= 10;
	private LabelPosition 	pos 	= LabelPosition.TopLeft;
	private LabelType 	type 	= LabelType.ABC;
	private LeafPanel	activePanel;
	private int	counter			= -1;
	private String color		= "black";
	ImageProcessor ip 			= new ColorProcessor(10, 10);

	private int userMaxCount		= 0;
	private String[] userLabels = null;

	public void draw(MouseEvent e, LeafPanel activePanel) {
		this.activePanel = activePanel;
		int x;
		int y;
		if(type == LabelType.userDefined) {
			x = e.getX();		// TODO check
			y = e.getY();
		}

		String label = getLabel();

		x = getX(label);
		y = getY(label);

		String c = "FFFFFF";
		if(color.equals("black"))
			c= "000000";

		String macro = "setFont(\"user\"); makeText('"+label+"',"+x+","+y+");run(\"Add Selection...\", \"stroke=&"+c+"\"); run(\"Select None\");";
		IJ.runMacro(macro);
		//IJ.runMacro("setColor("+color+"); setFont(\"user\"); makeText("+getLabel()+","+x+","+y+");");
	}

	public void setValues(LeafPanel activePanel, String xOffset, String yOffset, String pos, String type)
	{
		this.activePanel = activePanel;
		this.xOffset = Integer.parseInt(xOffset);
		this.yOffset = Integer.parseInt(yOffset);
		this.pos = LabelPosition.valueOf(pos);
		this.type = LabelType.valueOf(type);		
	}

	public static List<String> getPositionTypes() {
		List<String> l = new ArrayList<String>();

		LabelPosition[] p = LabelPosition.values();
		for(LabelPosition pos:p)
			l.add(pos.toString());
		return l;
	}

	public static List<String> getLabelTypes() {
		List<String> l = new ArrayList<String>();

		LabelType[] t = LabelType.values();
		for(LabelType typ:t)
			l.add(typ.toString());
		return l;
	}


	/**
	 * @param label text to display on the labeled panel
	 * @return y distance between the left coordinate of the panel and the fist label letter
	 */
	public int getX(String label) {
		if(pos==LabelPosition.TopLeft ||pos==LabelPosition.BottomLeft )
			return activePanel.
					getX()+
					xOffset;
		else {
			ip.setFont(new Font(TextRoi.getFont(), TextRoi.getStyle(), TextRoi.getSize()));	
			return activePanel.getX()+activePanel.getW()-xOffset-ip.getStringWidth(label);
		}
	}

	/**
	 * @param label text to display on the labeled panel
	 * @return x distance between the left coordinate of the panel and the fist label letter
	 */
	public int getY(String label) {
		if(pos==LabelPosition.TopLeft ||pos==LabelPosition.TopRight )
			return activePanel.getY()+yOffset;
		else {
			ip.setFont(new Font(TextRoi.getFont(), TextRoi.getStyle(), TextRoi.getSize()));
			return activePanel.getY()+activePanel.getH()-yOffset-ip.getFontMetrics().getHeight();
		}
	}

	/**
	 * @return next label letter, depending on counter and type set
	 */
	public String getLabel() {
		counter ++;
		if(type == LabelType.ABC)
			return (char)(65+counter)+"";
		else if(type == LabelType.abc)
			return (char)(97+counter)+"";
		else if(type == LabelType._123)
			return (counter+1)+"";
		else if(type == LabelType.I_II_III)
			return getRomanNr();
		else return "-1";
	}


	/**
	 * @return next label letter, depending on counter and type set
	 */
	public String getLabel(String type, boolean reset) {
		// if reset flag set, reset the label counter
		if (reset) counter=-1;
		counter ++;
		if(type.equals(LabelType.ABC+""))
			return (char)(65+(counter%26))+"";
		else if(type.equals(LabelType.abc+""))
			return (char)(97+(counter%26))+"";
		else if(type.equals(LabelType._123+""))
			return ((counter)%26)+1+"";
		else if(type.equals(LabelType.I_II_III+""))
			return getRomanNr();
		else if(type.equals(LabelType.userDefined+""))
			return getUserLabel();
		return "-1";
	}
	private String getRomanNr() {
		// Int to Roman numeral method taken from:
		// http://www.roseindia.net/java/java-tips/45examples/misc/roman/roman.shtml
		String[] RCODE = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
		int[]    BVAL  = {1000, 900, 500, 400,  100,   90,  50, 40,   10,    9,   5,   4,    1};
		counter = (counter % 30);
		if(counter==0)
			counter++;
		int isLeft = counter;
	     String roman = "";         for (int i = 0; i < RCODE.length; i++) {
	            while (isLeft >= BVAL[i]) {
	            	isLeft -= BVAL[i];
	                roman  += RCODE[i];
	            }
	        }
		return roman;
	}

	private String getUserLabel() {
		if(userLabels != null)
			return userLabels[counter%userMaxCount];
		else return "-2";
	}

	public void setUserLabels(String labelString) {
		userLabels = labelString.split(";");
		userMaxCount = userLabels.length;
		for(String s: userLabels)
			if(s.startsWith(" "))
				s = s.substring(1);
	}

	/**
	 * @param c indicates which letter is drawn: a, A, I or 1 if -1; b, B .. if 0 and so on */
	public void setCount(int c) {
		counter = c;
	}
}
