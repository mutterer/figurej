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
import ij.gui.Roi;
import ij.process.ImageProcessor;
import imagescience.image.Image;
import imagescience.transform.Rotate;
import imagescience.transform.Scale;

import java.util.ArrayList;
import java.util.Arrays;

public class MyImageMath {

	public static int[] getPixels(ImagePlus openImage, double[] xVals, double[] yVals, int panelW, int panelH, String interPolation) {
		double minX 	= Double.MAX_VALUE;
		double minY 	= Double.MAX_VALUE;


		for(int i=0; i<4; i++) {
			if(xVals[i]<minX) {
				minX = xVals[i];
			}
			if(yVals[i]<minY) {
				minY = yVals[i];
			}
		}

		double angle = Math.atan((yVals[3]-yVals[0])/(xVals[3]-xVals[0]))*180/Math.PI;
		if(xVals[3]<xVals[0])
			angle += 180;

		double smallRoiW = Math.sqrt(Math.pow((xVals[3]-xVals[0]),2)+Math.pow((yVals[3]-yVals[0]),2));
		double smallRoiH = Math.sqrt(Math.pow((xVals[3]-xVals[2]),2)+Math.pow((yVals[3]-yVals[2]),2));

		double[] xRect = xVals.clone();
		double[] yRect = yVals.clone();
		Arrays.sort(xRect);
		Arrays.sort(yRect);

		openImage.getCanvas().setOverlay(null);
		ImagePlus imp = openImage.flatten();
		imp.setRoi(new Roi(xRect[0],yRect[0],xRect[xRect.length-1]- xRect[0],yRect[yRect.length-1]- yRect[0]));
		ImageProcessor ip = imp.getProcessor().crop();				
		imp.setProcessor(ip);
		imp.updateImage();

		Image isi;
		if(angle*-1 != 0) {
			isi = Image.wrap(imp);
			Rotate rotator = new Rotate();
			// rotation with the highest quality don't lead to loss of signal correctness or image content (*)
			isi = rotator.run(isi,angle*-1,0,0, Rotate.BSPLINE5, true, false);
			imp = isi.imageplus();
		}
		imp.setRoi(new Roi (imp.getWidth()/2-smallRoiW/2,imp.getHeight()/2-smallRoiH/2,smallRoiW,smallRoiH));
		ip = imp.getProcessor().crop();	
		imp.setProcessor(ip);
		imp.updateImage();

		isi = Image.wrap(imp); //(*)
		final Scale scaler = new Scale();
		double scaleFactor = panelW/(double)ip.getWidth();
		int interP = Scale.BSPLINE5;
		if(interPolation.equals("nearest neighbor")) 
			interP = Scale.NEAREST;
		else if(interPolation.equals("linear"))
			interP = Scale.LINEAR; 
		else if(interPolation.equals("cubic convolution"))
			interP = Scale.CUBIC; 
		else if(interPolation.equals("cubic B-spline"))
			interP = Scale.BSPLINE3; 
		else if(interPolation.equals("cubic O-MOMS"))
			interP = Scale.OMOMS3; 
		else if(interPolation.equals("quintic B-spline"))
			interP = Scale.BSPLINE5; 

		isi = scaler.run(isi, scaleFactor, scaleFactor, 1,1,1, interP);
		imp = isi.imageplus();
		ip = imp.getProcessor().resize(panelW, panelH);

		return (int[]) ip.getPixels();
	}

	public static ArrayList<String> getInterpolationTypes() {
		ArrayList<String> l = new ArrayList<String>();
		l.add("nearest neighbor");
		l.add("linear");
		l.add("cubic convolution");
		l.add("cubic B-spline");
		l.add("cubic O-MOMS");
		l.add("quintic B-spline");
		return l;
	}
	
	/* (*) sometimes pixels visualize signals and you want to avoid that those pixels are displayed 
	 * differently after changing the image in some way (scale, rotate); the pixels have to be displayed as 
	 * quadratic blocks, even if they get scaled; here the focus lays on the pixels values themselves;
	 * 
	 * when you display other content, like pictures of your family or your favorite cell culture,
	 * and you zoom in, you will want to avoid to get a pixelated image.
	 * when you focus on the objects your image pixels display you can slightly change some of these pixels 
	 * to ameliorate the quality of a scaled version of the image
	 * 
	 * ==> first take a high quality rotation algorithm in both cases: preserve your signal or preserve
	 * the objects visible on your image
	 * 
	 * afterwards scale your image:
	 * preserve the signal: use the most low quality algorithm you can find, like nearest neighbor
	 * preserve visible objects: use a high quality algorithm
	 */  
}
