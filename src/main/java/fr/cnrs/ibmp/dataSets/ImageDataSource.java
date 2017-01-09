
package fr.cnrs.ibmp.dataSets;

import fr.cnrs.ibmp.treeMap.LeafPanel;

/**
 * TODO Documentation
 * 
 * @author Stefan Helfrich (University of Konstanz)
 */
public abstract class ImageDataSource extends AbstractDataSource {

	// position of the selected image in a multi channel or stack like image
	private int selectedChannel;
	private int selectedSlice;
	private int selectedFrame;
	private boolean[] activeChannels;
	private String actChs = "";

	// display range info (is not recorded by the IJ record macro function,
	// therefore stored here)
	private double lowerDisplRange = -1;
	private double upperDisplRange = -1;

	// coordinates of the region that got selected and displayed in the panel the
	// dataSource belongs to
	// TODO Why is array length 4?
	private double[] sourceX = new double[4];
	private double[] sourceY = new double[4];

	// transform info
	private double angle = 0.0;
	private double scaleFactor = 1.0;

	private String macroString = "";

	public ImageDataSource(final ImageDataSource dataSource) {
		super(dataSource);

		this.selectedChannel = dataSource.selectedChannel;
		this.selectedSlice = dataSource.selectedSlice;
		this.selectedFrame = dataSource.selectedFrame;

		this.sourceX = (dataSource.sourceX == null) ? null : dataSource.sourceX
			.clone();
		this.sourceY = (dataSource.sourceY == null) ? null : dataSource.sourceY
			.clone();

		this.activeChannels = dataSource.activeChannels;
		this.actChs = dataSource.actChs;

		this.angle = dataSource.angle;
		this.scaleFactor = dataSource.scaleFactor;
	}

	/**
	 * Creates an {@link ImageDataSource} from default values.
	 */
	public ImageDataSource() {
		super();
	}

	/**
	 * @return x coordinates of the rectangle image region that was selected to
	 *         fill the panel the dataSource object belongs to with
	 */
	public double[] getSourceX() {
		return sourceX;
	}

	/**
	 * @return y coordinates of the rectangle image region that was selected to
	 *         fill the panel the dataSource object belongs to with
	 */
	public double[] getSourceY() {
		return sourceY;
	}

	/**
	 * @return minimum RGB value of the image serving as source for the panel this
	 *         dataSource belongs to -1 if no image is assigned
	 */
	public double getLowerDisplayRange() {
		return lowerDisplRange;
	}

	/**
	 * @return maximum RGB value of the image serving as source for the panel this
	 *         dataSource belongs to -1 if no image is assigned
	 */
	public double getUpperDisplayRange() {
		return upperDisplRange;
	}

	/**
	 * @return selected channel of the image serving as source for the panel this
	 *         dataSource belongs to
	 */
	public int getChannel() {
		return selectedChannel;
	}

	/**
	 * @return selected slice of the image serving as source for the panel this
	 *         dataSource belongs to
	 */
	public int getSlice() {
		return selectedSlice;
	}

	/**
	 * @return selected frame of the image serving as source for the panel this
	 *         dataSource belongs to
	 */
	public int getFrame() {
		return selectedFrame;
	}

	/**
	 * @return array of hints about the activity of the channels of the image
	 *         serving as source for the panel this dataSource belongs to
	 */
	public boolean[] getActiveChannels() {
		return activeChannels;
	}

	/**
	 * @return hints about the activity of the channels of the image serving as
	 *         source for the panel this dataSource belongs to 1 for active; 0 for
	 *         inactive?!
	 */
	public String getActChs() {
		return actChs;
	}

	public double getAngle() {
		return angle;
	}

	public double getScaleFactor() {
		return scaleFactor;
	}

	/** @return image processing steps recorded while the ROI tool was open */
	public String getMacro() {
		return macroString;
	}

	public void setSlice(int currentSlice) {
		selectedSlice = currentSlice;

		notifyListeners();
	}

	public void setFrame(int currentFrame) {
		selectedFrame = currentFrame;

		notifyListeners();
	}

	public void setChannel(int currentChannel) {
		selectedChannel = currentChannel;

		notifyListeners();
	}

	/**
	 * @param channels 1s for the channels displayed, zeros for the invisible ones
	 */
	public void setActiveChannels(boolean[] channels) {
		activeChannels = channels;
	}

	/**
	 * @param channels 1s for the channels displayed, zeros for the invisible ones
	 */
	public void setActChs(String channels) {
		actChs = channels;

		notifyListeners();
	}

	/**
	 * @param lowerRange minimum color value of the image assigned to the panel
	 *          the data source belongs to
	 * @param upperRange maximum color value of the image assigned to the panel
	 *          the data source belongs to
	 */
	public void setDisplayRange(double lowerRange, double upperRange) {
		lowerDisplRange = lowerRange;
		upperDisplRange = upperRange;

		notifyListeners();
	}

	/**
	 * @param sourceX x coordinates of the ROI tool rectangle used to crop the
	 *          selected image region
	 * @param sourceY y coordinates of the ROI tool rectangle used to crop the
	 *          selected image region
	 */
	public void setCoords(double[] sourceX, double[] sourceY) {
		this.sourceX = sourceX;
		this.sourceY = sourceY;

		notifyListeners();
	}

	public void setAngle(double angle) {
		this.angle = angle;

		notifyListeners();
	}

	public void setScaleFactor(double scaleFactor) {
		this.scaleFactor = scaleFactor;

		notifyListeners();
	}

	/**@param macro pre-processing steps executed on the image before its pixels get transferred to a panel
	 */
	public void setMacro(String macro) {
		macroString = macro;

		notifyListeners();
	}

	/**
	 * Invalidates the stored coordinates of the selection ROI by setting
	 * {@link #sourceX} and {@link #sourceX}.
	 * <p>
	 * This method is usually called when the size of the {@link LeafPanel}
	 * associated with {@code this} changes. In that case the coordinates cannot
	 * be reused.
	 * </p>
	 */
	public void invalidateCoordinates() {
		sourceX = null;
		sourceY = null;
	}

	@Override
	public void clear() {
		super.clear();

		setCoords(null, null);
		setSlice(1);
		setChannel(1);
		setFrame(1);
		setDisplayRange(-1., -1.);
		setMacro("");
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "Display range: \t[" + lowerDisplRange + "-" + upperDisplRange + "]\n";
		s += "Current [Slice,Frame,Channel]: \t[" + selectedSlice + "," +
			selectedFrame + "," + selectedChannel + "]\n";
		s += "Active Channels: \t[" + actChs + "]\n";
		s += "Preprocessing:\n";
		s += macroString + "\n";

		return s;
	}

}
