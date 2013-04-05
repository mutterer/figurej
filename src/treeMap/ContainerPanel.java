package treeMap;

import ij.IJ;
import ij.gui.Overlay;

import java.util.ArrayList;


/**
 * @author Edda Zinck
 * @author Jerome Mutterer
 * (c) IBMP-CNRS 
 * 
 * invisible panel
 * 
 * can contain leaf panels (= panel that can display an image) or further containers.
 * all the elements in a container have either the same height - vertically splitable container -
 * or the same width.
 */

public class ContainerPanel extends Panel {
	private static final long serialVersionUID = 1L;
	private boolean horizontallySplitable;
	private static final String smallSidelengthWarning = "";//>>   side length fell below minimum! no splitting done.";
	private final int minLeafSideLength = LeafPanel.minLeafSideLength;

	public ContainerPanel(int xPos, int yPos, int w, int h){
		super(xPos, yPos, w, h);
	}

	/**
	 * @return always false because containers are invisible and therefore only their visible content 
	 * can be clicked */
	@Override
	public boolean isClicked(int x, int y, int tol) {
		return false;
	}

	/**
	 * @param horizontalSplit if true the panel is split horizontally; else vertically
	 * @param child0 leaf panel (=image) to split
	 * 
	 * depending on the on the parameter the panel is either split horizontally or vertically
	 * images currently are split by shrinking the panel passed as parameter: it either loses height or width. the space becoming free
	 * is filled with a new panel.
	 * if containing several images, the panel has a split preference:
	 * either all his images have to have the same width (horizontallySplittable=true) or all of them have the same height.
	 * if child images of a container are split in the direction that fits to the preference, the container shrinks 
	 * the split image and adds a new one + a separator in the free space.
	 * else the shrunken image, the new one and the separator are added to a new container, which's split preference is set polar to the 
	 * original container's preference; this new container then is added as child to the first container, while the shrunken image
	 * is removed from the original container's children list.
	 */
	@Override
	protected void split(boolean horizontalSplit, LeafPanel child0){
		LeafPanel child1;
		SeparatorPanel separator;
		ContainerPanel container = new ContainerPanel(child0.getX(), child0.getY(), child0.getW(), child0.getH());

		if(children.size()<=1)
			horizontallySplitable = horizontalSplit;			// set split preference on first split
		int x1 = child0.getX()+child0.getW()/2 - separatorWidth;
		int y1 = child0.getY()+child0.getH()/2 - separatorWidth;

		if(horizontalSplit)
		{
			int child0Y1 = y1;									// concerning the child y0 is the y position of the top of the panel, y1 is the lower one
			int child1Y0 = child0Y1 + separatorWidth;

			int child0Height = child0Y1 - child0.yPos;
			int child1Height = child0.yPos+child0.panelHeight - child1Y0;

			if(child0Height<minLeafSideLength || child1Height<minLeafSideLength)
			{
				System.out.println(smallSidelengthWarning);
				return;
			}

			child0.panelHeight=(child0Height);					// shrink
			separator 	= new SeparatorPanel(child0.xPos, child0Y1, child0.panelWidth, separatorWidth);
			child1 		= new LeafPanel(	 child0.xPos, child1Y0, child0.panelWidth, child1Height);  // the new image is set below the "splitted" one

		}
		else
		{
			int child0X1 = x1;			
			int child1X0 = child0X1 + separatorWidth;

			int child0Width = child0X1 - child0.xPos;
			int child1Width = child0.xPos+child0.panelWidth - child1X0;

			if(child0Width<minLeafSideLength || child1Width<minLeafSideLength)
			{
				System.out.println(smallSidelengthWarning);
				return;
			}

			child0.panelWidth = child0Width;
			separator 	= new SeparatorPanel(child0X1, child0.yPos, separatorWidth, child0.panelHeight);
			child1 		= new LeafPanel(	 child1X0, child0.yPos, child1Width,   child0.panelHeight);  // the new image is set below the "splitted" one

		}
		if(horizontalSplit == horizontallySplitable)	// if container's split direction = direction of current split: add a child
		{
			int index = children.indexOf(child0);

			addChild(index+1, child1);					// always add children successively!
			addChild(index+1, separator);
		}
		else // if container's split direction != current split direction: add the shrunken and the new image to a new container 
		{
			container.horizontallySplitable = !horizontallySplitable;	// has the inverse split direction than its parent container

			container.children.clear();			// remove default leaf
			container.addChild(child0);			// always add children successively!
			container.addChild(separator);
			container.addChild(child1);

			int child0Index = children.indexOf(child0);
			this.addChild(child0Index, container);
			this.removeChild(child0);
		}
	}

	protected void split(int nr, boolean horizontalSplit, LeafPanel child0){
		if(nr<2)
			return;

		int newLeafSideLenght;
		if(horizontalSplit) 
			newLeafSideLenght = (child0.panelHeight-(nr-1)*separatorWidth) / nr;
		else
			newLeafSideLenght = (child0.panelWidth -(nr-1)*separatorWidth) / nr;

		if(newLeafSideLenght < minLeafSideLength) {
			IJ.error("this image is too small to split it into "+nr);
			return;
		}

		if(children.size()<=1)
			horizontallySplitable = horizontalSplit;			// set split preference on first split
		
		ContainerPanel container = new ContainerPanel(child0.getX(), child0.getY(), child0.getW(), child0.getH());
		ArrayList<Panel> childrenTemp = new ArrayList<Panel>();
		int separatorPos = 0;
		
		if(horizontalSplit) {
			int oldChildH = child0.getH();
			child0.setH(newLeafSideLenght);
			childrenTemp.add(child0);
			separatorPos = child0.getH()+child0.getY();
			
			for(int i=0; i<nr-1; i++) {
				SeparatorPanel sep = new SeparatorPanel(child0.getX(), separatorPos, child0.getW(), separatorWidth);
				LeafPanel leaf = new LeafPanel(child0.getX(), separatorPos+separatorWidth, child0.getW(), newLeafSideLenght);
				childrenTemp.add(sep);
				childrenTemp.add(leaf);
				separatorPos = separatorPos+separatorWidth+newLeafSideLenght;
			}
			Panel lastNewChild = childrenTemp.get(childrenTemp.size()-1);
			lastNewChild.setH(lastNewChild.getH()+ (oldChildH-nr*newLeafSideLenght-(nr-1)*separatorWidth));	// handle rounding errors
		}
		else {
			int oldChildW = child0.getW();
			child0.setW(newLeafSideLenght);
			childrenTemp.add(child0);
			separatorPos = child0.getW() + child0.getX();
			for (int i=0; i<nr-1; i++) {
				SeparatorPanel sep = new SeparatorPanel(separatorPos, child0.getY(), separatorWidth, child0.getH());
				LeafPanel leafi = new LeafPanel(separatorPos+separatorWidth, child0.getY(), newLeafSideLenght, child0.getH());
				childrenTemp.add(sep);
				childrenTemp.add(leafi);
				separatorPos = separatorPos+separatorWidth+newLeafSideLenght;
			}
			Panel lastNewChild = childrenTemp.get(childrenTemp.size()-1);
			lastNewChild.setW(lastNewChild.getW()+ (oldChildW-nr*newLeafSideLenght-(nr-1)*separatorWidth));	// handle rounding errors
		}
		
		if(horizontalSplit == horizontallySplitable)	// if container's split direction = direction of current split: add a child
		{
			int index = children.indexOf(child0);

			for(int i=1; i<childrenTemp.size(); i++) { 	// i=1:  child0 on list position 0 is already a child
				 index++;
				addChild(index, childrenTemp.get(i)); 	// always add children successively!
			}
								
		}
		else // if container's split direction != current split direction: add the shrunken and the new image to a new container 
		{
			container.horizontallySplitable = !horizontallySplitable;	// has the inverse split direction than its parent container
			container.children.clear();
			
			for(int i=0; i<childrenTemp.size(); i++) {
				container.addChild(childrenTemp.get(i)); 	// always add children successively!
			}

			int child0Index = this.children.indexOf(child0);
			this.addChild(child0Index, container);
			this.removeChild(child0);
		}
	}


	/**@param mouseX the x position the separator is dragged to
	 * @param mouseY the y position the separator is dragged to
	 * @param separator the separator dragged around to reshape its neighbors
	 * realizes reshaping of panels by moving separators.
	 * panels can get dragged around as long as their width or height do not fall under a minimum.
	 * only neighbors that share sides with the dragged separator are affected. if neighbors reach
	 * minimal width or height no further reshaping is possible. */
	public void reShape(int mouseX, int mouseY, SeparatorPanel separator)  //TODO enforce that adding of children works successively
	{
		if(children.size()<=2) // if separators and leafs are stored in different lists, change argument
		{
			System.out.println("you try to reshape an one-child panel");
			if(children.size()==2)System.out.println("\tthe container has two children! that should not occur!"+
					" (you should always have sizes like: nr. of leafs (the leafs^^) + nr. of leafs -1 elements (separators))");
			return;	
		}
		Panel child0 = children.get(children.indexOf(separator)-1);		// the panels going to be reshaped
		Panel child1 = children.get(children.indexOf(separator)+1);

		if(horizontallySplitable)
		{	
			int seperatorY0 	= mouseY ;//- defaultSeparatorWidth/2;
			int child0Height 	= seperatorY0 - child0.getY();
			int child1Y0 		= seperatorY0 + separatorWidth;
			int child1Height;
			if(children.indexOf(child1)==children.size()-1)
				child1Height 	= this.panelHeight +this.getY() - child1Y0; 
			else 
				child1Height	= children.get(children.indexOf(child1)+1).getY() - child1Y0;

			if(child0Height<minLeafSideLength || child1Height <minLeafSideLength) { 	// insufficient for containers!
				System.out.println(smallSidelengthWarning);
				return;
			}

			// to have consistent data either every affected h and h should be changed or, if one would get smaller than allowed, none of them.
			if(child0.canSetH(child0Height) && child1.canSetY0PreservingY1(child1Y0)) {
				child0.setH(child0Height);
				child1.setY0PreservingY1(child1Y0);

				separator.setY(seperatorY0);
			}
		}

		else
		{
			int seperatorX0 	= mouseX ;//- defaultSeparatorWidth/2;; 
			int child0Width 	= seperatorX0 - child0.getX();
			int child1X0 		= seperatorX0 + separatorWidth;
			int child1Width; 
			if(children.indexOf(child1)==children.size()-1)
				child1Width 	= this.panelWidth +this.getX() - child1X0;
			else 
				child1Width		= children.get(children.indexOf(child1)+1).getX() - child1X0;

			if(child0Width<minLeafSideLength || child1Width <minLeafSideLength){ 	// insufficient for containers!
				System.out.println(smallSidelengthWarning);
				return;
			}

			// to have consistent data either every affected h and h should be changed or, if one would get smaller than allowed, none of them.
			// in case child0 is resized successfully and child1 fails, the old state of child0 has to be recovered.
			if(child0.canSetW(child0Width) && child1.canSetX0PreservingX1(child1X0)) {
				child0.setW(child0Width);
				child1.setX0PreservingX1(child1X0);
				separator.setX(seperatorX0); 
			}
		}
	}

	@Override
	public void remove(LeafPanel removeLeaf)
	{
		Panel growingPanel;

		if(children.size()<=1)
		{
			System.out.println("you can't remove your only child!");
			return;
		}

		int index = children.indexOf(removeLeaf);

		if(index < children.size()-1)  // not most right or lower child
		{
			growingPanel = children.get(index+2);
			this.removeChild(index+1);				// remove separator
			if(horizontallySplitable) 
				growingPanel.setY0PreservingY1(removeLeaf.getY());

			else
				growingPanel.setX0PreservingX1(removeLeaf.getX());
		}
		else {
			growingPanel = children.get(index-2);
			this.removeChild(index-1);
			if(horizontallySplitable)
				growingPanel.setH(growingPanel.getH()+removeLeaf.getH()+separatorWidth);
			else
				growingPanel.setW(growingPanel.getW()+removeLeaf.getW()+separatorWidth);
		}
		children.remove(removeLeaf);

		if(children.size()==1)				// only growing panel left
		{
			ContainerPanel parent = this.getParent();
			if(parent != null)				// parent == null asking root panel for his parent
			{
				int pos = parent.children.indexOf(this);
				parent.addChild(pos, growingPanel);
				parent.removeChild(this);
			}
		}
	}



	@Override
	public void setW(int w) //throws SideLengthTooSmallException {
	{
		if(w<minLeafSideLength) {
			System.out.println("EXCEPTION in container: setW!!! report that to edda please");
			//throw new SideLengthTooSmallException();
		}

		if(horizontallySplitable) {
			for(Panel p: children) // p = horizontally splitable if not leaf  	
				p.setW(w);
		}
		else {
			Panel myLastChild = children.get(children.size()-1);
			myLastChild.setW( myLastChild.getW()-panelWidth+w);
		}

		this.panelWidth = w;   //change h only if changing the children's h worked
	}


	@Override
	public void setH(int h) //throws SideLengthTooSmallException {
	{
		if(h<minLeafSideLength) {
			System.out.println("EXCEPTION in container: setH!!! report that to edda please");
			//throw new SideLengthTooSmallException();
		}

		if(!horizontallySplitable) {
			for(Panel p: children)	
				p.setH(h);
		}
		else {
			Panel myLastChild = children.get(children.size()-1); 
			myLastChild.setH(myLastChild.getH()-(panelHeight-h));
		}

		this.panelHeight = h;   //change h only if changing the children's h worked
	}

	@Override
	protected void setX0PreservingX1(int x0) //throws SideLengthTooSmallException {
	{
		if(xPos+panelWidth-x0<minLeafSideLength) {
			System.out.println("EXCEPTION in container: setX0PreservingX1!!! report that to edda please");
			//throw new SideLengthTooSmallException();
		}
		if(horizontallySplitable) {
			for(Panel p: children)	
				p.setX0PreservingX1(x0);
		}
		else {
			Panel myFirstChild = children.get(0);
			myFirstChild.setX0PreservingX1(x0);
		}

		panelWidth = xPos+panelWidth - x0;
		xPos = x0;   //change things only if changing the children's width worked
	}

	@Override
	protected void setY0PreservingY1(int y0) //throws SideLengthTooSmallException {
	{
		if(yPos+panelHeight-y0<minLeafSideLength) {
			System.out.println("EXCEPTION in container: setY0PreservingY1!!! report that to edda please");
			//throw new SideLengthTooSmallException();
		}
		if(!horizontallySplitable) {
			for(Panel p: children)
				p.setY0PreservingY1(y0);
		}
		else {
			Panel myFirstChild = children.get(0);
			myFirstChild.setY0PreservingY1(y0);
		}

		panelHeight = yPos+panelHeight - y0;
		yPos = y0;   //change things only if changing the children's height worked
	}


	public void addChild(Panel child) {
		child.setParent(this);
		children.add(child);	
	}

	protected void addChild(int i, Panel child) {
		child.setParent(this);
		children.add(i, child);	
	}

	protected void removeChild(int i) {
		children.remove(i);
	}

	protected void removeChild(Panel p) {
		children.remove(p);
	}

	@Override
	public boolean canSetW(int w) {
		if(w < minLeafSideLength)
			return false;
		if(horizontallySplitable)
			for(Panel p: children) {
				if(!p.canSetW(w)) 
					return false;
			}

		else {
			Panel myLastChild = children.get(children.size()-1);
			if(!myLastChild.canSetW( myLastChild.getW()-panelWidth+w))
				return false;
		}
		return true;
	}

	@Override
	public boolean canSetH(int h) {
		if(h < minLeafSideLength)
			return false;

		if(!horizontallySplitable) {
			for(Panel p: children)	
				if(!p.canSetH(h))
					return false;
		}
		else {
			Panel myLastChild = children.get(children.size()-1); 
			if(!myLastChild.canSetH(myLastChild.getH()-(panelHeight-h)))
				return false;
		}
		return true;
	}

	@Override
	protected boolean canSetX0PreservingX1(int x0) {
		if(xPos+panelWidth - x0 < minLeafSideLength)
			return false;

		if(horizontallySplitable) {
			for(Panel p: children)	
				if(p.canSetX0PreservingX1(x0)==false)
					return false;
		}
		else {
			Panel myFirstChild = children.get(0);
			if(myFirstChild.canSetX0PreservingX1(x0)==false)
				return false;
		}
		return true;
	}

	@Override
	protected boolean canSetY0PreservingY1(int y0) {
		if(yPos+panelHeight-y0<minLeafSideLength) 
			return false;

		if(!horizontallySplitable) {
			for(Panel p: children)
				if(p.canSetY0PreservingY1(y0)==false)
					return false;
		}
		else {
			Panel myFirstChild = children.get(0);
			if(myFirstChild.canSetY0PreservingY1(y0)==false)
				return false;
		}
		return true;
	}
	
	@Override
	public void hideLabel(Overlay o) {
		for(Panel p: children)
			p.hideLabel(o);
	}
	
	@Override
	public void hideScalebar(Overlay o) {
		for(Panel p: children)
			p.hideScalebar(o);
	}
}