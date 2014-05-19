package utilities;

/*
 * @author Edda Zinck
 * @author Jerome Mutterer
 * (c) IBMP-CNRS 
 * class handling the extraction of the image region selected with the ROI tool
 * it has to be rotated if the ROI selection tool was tilted and scaled to the size of the 
 * panel that will display the selected pixels 
 *
 */
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.PolygonRoi;
import ij.process.ImageProcessor;
import imagescience.image.Axes;
import imagescience.image.Image;
import imagescience.transform.Affine;
import imagescience.transform.Transform;

public class MyImageMath {

	public static int[] getPixels(ImagePlus openImage, PolygonRoi boundingRect,
			double angle, double scaleFactor, int panelW, int panelH,
			int interpolation) {

		// first remove all overlays in the source image
		ImageCanvas ic = openImage.getCanvas();
		if (ic != null)
			ic.setShowAllList(null);
		openImage.setOverlay(null);
		// then flatten the image, which always ends with an RGB result
		ImagePlus imp = openImage.flatten();

		// crop according to the tilted selection rectangle so we can rotate
		// around the result center, and transform a smaller image
		imp.setRoi(boundingRect);
		ImageProcessor ip = imp.getProcessor().crop();
		imp.setProcessor(ip);
		imp.updateImage();

		// the affine transform is done by imagescience
		Transform transform = new Transform();

		transform.rotate(angle * -1, Axes.Z);
		transform.scale(scaleFactor, Axes.X);
		transform.scale(scaleFactor, Axes.Y);

		Affine affine = new Affine();

		Image imageScienceImage;
		imageScienceImage = Image.wrap(imp);
		imageScienceImage = affine.run(imageScienceImage, transform,
				interpolation, true, false);

		// finally crop the transformed image to the panel dimension
		ImagePlus result = imageScienceImage.imageplus();
		result.setRoi((result.getWidth() - panelW) / 2,
				(result.getHeight() - panelH) / 2, panelW, panelH);
		ImageProcessor ipresult = result.getProcessor().crop()
				.resize(panelW, panelH);

		return (int[]) ipresult.getPixels();
	}

}
