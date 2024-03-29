package utils;
import java.awt.Dimension;
/*
 * @author Edda Zinck
 * @author Jerome Mutterer
 * (c) IBMP-CNRS 
 * 
 */
import java.io.File;
import java.io.Serializable;

public class DataSource implements Serializable{

	private static final long serialVersionUID = 1L;
	
	// image file location
	private String 		fileDirectory 	= "";
	private String 		fileName 		= "";
	private String 		externalSource 		= "";

	// coordinates of the region that got selected and displayed in the panel the dataSource belongs to
	private double[] 	sourceX;
	private double[] 	sourceY;		

	// notes stored in a text file when the figure is saved
	private String 		notes = "";	
	public final String defaultNote = "<my image notes>";

	// selected serie from multiseries file formats, like *.lif files
	private int 		selectedSerie;
	public int getSelectedSerie() {
		return selectedSerie;
	}

	public void setSelectedSerie(int selectedSerie) {
		this.selectedSerie = selectedSerie;
	}
	// position of the selected image in a multi channel or stack like image
	private int 		selectedChannel;
	private int 		selectedSlice;
	private int 		selectedFrame;
	private boolean[]	activeChannels;
	private String		actChs = "";

	// calibration info
	private double 		pixelWidth = 1.0;
	private String		calbirationUnit = "pixel";
	
	// transform info
	private double 		angle = 0.0;
	private double 		scaleFactor = 1.0;
	
	
	// label info
	private String		label = "";
	private String		scalebarInfo = "";
	
	// display range info (is not recorded by the IJ record macro function, therefore stored here)
	private double		lowerDisplRange = -1;
	private double		upperDisplRange = -1;

	private String		macroString = "";
	
	private String 		fov ="";

	public DataSource() {
		this.sourceX = null;
		this.sourceY = null;
	}

	/**@return minimum RGB value of the image serving as source for the panel this dataSource belongs to
	 * -1 if no image is assigned */
	public double getLowerDisplayRange() {
		return lowerDisplRange;
	}
	/**@return maximum RGB value of the image serving as source for the panel this dataSource belongs to
	 * -1 if no image is assigned */
	public double getUpperDisplayRange() {
		return upperDisplRange;
	}
	/**@return selected channel of the image serving as source for the panel this dataSource belongs to */
	public int getChannel() {
		return selectedChannel;
	}
	/**@return selected slice of the image serving as source for the panel this dataSource belongs to */
	public int getSlice() {
		return selectedSlice;
	}
	/**@return selected frame of the image serving as source for the panel this dataSource belongs to */
	public int getFrame() {
		return selectedFrame;
	}
	/**@return array of hints about the activity of the channels of the image serving as source for the panel this dataSource belongs to */
	public boolean[] getActiveChannels() {
		return activeChannels;
	}
	/**@return hints about the activity of the channels of the image serving as source for the panel this dataSource belongs to
	 * 1 for active; 0 for inactive?! */
	public String getActChs() {
		return actChs;
	}
	/**@return directory the image serving as source for the panel this dataSource belongs to is saved in
	 * directory changes to the one the serialized file is stored in upon first hit of the "save" button */
	public String getFileDirectory() {
		return fileDirectory;
	}
	/**@return name of the image serving as source for the panel this dataSource belongs to
	 * name eventually changes upon first hit of the "save" button, if an image with same name and different directory is used */
	public String getFileName() {
		return fileName;
	}
	/**@return x coordinates of the rectangle image region that was selected to fill the panel the dataSource object belongs to with */
	public double[] getSourceX() {
		return sourceX;
	}
	/**@return y coordinates of the rectangle image region that was selected to fill the panel the dataSource object belongs to with */
	public double[] getSourceY() {
		return sourceY;
	}
	/**@return pixel width in @see getUnit() units */
	public double getPixelWidth() {
		return pixelWidth;
	}
	/**@return measure of the pixel width */
	public String getUnit() {
		return calbirationUnit;
	}
	/**@return notes written to the panels's notes field on the GUI or empty string
	 */
	public String getNotes() {
		return notes;
	}
	/**@return image processing steps recorded while the ROI tool was open */
	public String getMacro() {
		return macroString;
	}

	
	/**@param sourceX x coordinates of the ROI tool rectangle used to crop the selected image region
	 * @param sourceY y coordinates of the ROI tool rectangle used to crop the selected image region */
	public void setCoords(double[] sourceX, double[] sourceY) {
		this.sourceX = sourceX;
		this.sourceY = sourceY;
	}
	/**@param filePath path of the image chosen to fill the panel this data source belongs to with */
	public void setFileDirectory(String filePath) {
		if(!filePath.endsWith(File.separator))
			filePath += File.separator;
		this.fileDirectory = filePath;
	}
	/**@param name of the image chosen to fill the panel this data source belongs to with (without path!)*/
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	/**@param text notes on the image (e.g. from the GUI notes field) */
	public void setNotes(String text) {
		notes = text;
	}
	/**calibration information
	 * @param pixelSize
	 * @param unit */
	public void setPixelCalibration(double pixelSize, String unit) {
		this.calbirationUnit = unit;
		pixelWidth = pixelSize;
	}
	/**@param macro pre-processing steps executed on the image before its pixels get transferred to a panel
	 */
	public void setMacro(String macro) {
		macroString = macro;
	}
	/**@see ij.ImagePlus.setSlice(int currentSlice)*/
	public void setSlice(int currentSlice) {
		selectedSlice = currentSlice;
	}
	/**@see ij.ImagePlus.setFrame(int currentFrame)*/
	public void setFrame(int currentFrame) {
		selectedFrame = currentFrame;
	}
	/**@see ij.ImagePlus.setChannel(int currentChannel)*/
	public void setChannel(int currentChannel) {
		selectedChannel = currentChannel;
	}
	/** @param channels 1s for the channels displayed, zeros for the invisible ones */
	public void setActiveChannels(boolean[] channels) {
		activeChannels = channels;
	}
	/** @param channels 1s for the channels displayed, zeros for the invisible ones */
	public void setActChs(String channels) {
		actChs = channels;
	}
	/**@param lowerRange minimum color value of the image assigned to the panel the data source belongs to
	 * @param upperRange maximum color value of the image assigned to the panel the data source belongs to */
	public void setDisplayRange(double lowerRange, double upperRange) {
		lowerDisplRange = lowerRange;
		upperDisplRange = upperRange;
	}
	/**@param label String label of the panel or "" */
	public void setLabel(String label) {
		this.label = label;
	}
	/**@param sInfo length of the scale bar, -1 if no scale bar */
	public void setScalebarLength(double l) {
		if(l <= 0)
			scalebarInfo = "";
		else scalebarInfo = (l)+" "+calbirationUnit;
	}
	/**@return data source values arranged in a string */
	public String createLog() {
		String s = "";
		
			s += "Panel: "+(label==""?"Untitled":label)+ "\n";
			s += "Image Datasource: "+fileName + "\n";
			if (sourceX!=null) s += "Source rect: ["+"("+(int)sourceX[0]+","+(int)sourceY[0]+")"+","+"("+(int)sourceX[1]+","+(int)sourceY[1]+")"+","+"("+(int)sourceX[2]+","+(int)sourceY[2]+")"+","+"("+(int)sourceX[3]+","+(int)sourceY[3]+")"+"]\n";
			s += "External Datasource: "+(externalSource==""?"none":externalSource) + "\n";
			s += "Original folder: "+fileDirectory + "\n";
			s += "Display range: \t["+lowerDisplRange+"-"+upperDisplRange + "]\n";
			s += "Current [Slice,Frame,Channel]: \t["+selectedSlice+","+selectedFrame+","+selectedChannel+"]\n";
			s += "Active Channels: \t["+actChs+"]\n";			
			s += "User notes:\n";
			s += (notes!=""?(notes+"\n"):"");
			s += "Scale bar: "+(scalebarInfo==null?"Undefined":scalebarInfo)+ "\n";
			s += "Field of view: "+fov+" "+calbirationUnit+ "\n";
			s += "Preprocessing:\n";
			s += macroString + "\n";
			s += "-------------------------\n";
		
		return s;
	}

	/**@return deep copy if the current object */
	public DataSource clone() {
		DataSource newDS 	= new DataSource();
		newDS.fileDirectory = this.fileDirectory;
		newDS.fileName 		= this.fileName;

		newDS.sourceX		= (sourceX == null) ? null : this.sourceX.clone();
		newDS.sourceY		= (sourceY == null) ? null : this.sourceY.clone();		

		newDS.notes 		= this.notes;

		newDS.selectedChannel = this.selectedChannel;
		newDS.selectedSlice	  = this.selectedSlice;
		newDS.selectedFrame	  = this.selectedFrame;

		newDS.activeChannels  = this.activeChannels;
		newDS.actChs  		  = this.actChs;

		newDS.angle  		  = this.angle;
		newDS.scaleFactor  		  = this.scaleFactor;

		newDS.pixelWidth	= this.pixelWidth;
		newDS.calbirationUnit			= this.calbirationUnit;
		newDS.fov = this.fov;
		return newDS;
	}

	public void setExternalSource(String source) {
		this.externalSource = source;	
	}
	public String getExternalSource() {
		return this.externalSource;	
	}

	public double getAngle() {
		return angle;
	}

	public void setAngle(double angle) {
		this.angle = angle;
	}

	public double getScaleFactor() {
		return scaleFactor;
	}

	public void setScaleFactor(double scaleFactor) {
		this.scaleFactor = scaleFactor;
	}

	public String getFov() {
		return fov;
	}

	public void setFov(String fov) {
		this.fov = fov;
	}


}
