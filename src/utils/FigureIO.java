package utils;
/*
 * @author Edda Zinck
 * @author Jerome Mutterer
 * (c) CNRS 
 * the FigureIO class handles the process of storing and re-opening panel trees and the figure that belongs to a tree.
 * also creates image files
 */
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.Overlay;
import ij.io.FileSaver;
import ij.io.OpenDialog;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.awt.Rectangle;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import externaltools.Link;
import figure.ContainerPanel;
import figure.FigureWindow;
import figure.LeafPanel;
import figure.Panel;

public class FigureIO {

	private final String serFileExtension 	= ".figurej";
	private String roiFileName 			= "RoiSet.zip";
	/**@param xPos x position of the window that will be opened
	 * @param yPos y position of the window opened
	 * @return the window the figure is drawn on with images, separators, arrows and so on
	 * opens a file selection dialog. tries to open a serialization file and rebuilds the panel structure 
	 * as well as the overlay
	 */
	private FigureWindow figure;
	
	public FigureWindow open() {	

        OpenDialog od = new OpenDialog("Select FigureJ file to open", null);
        String directory = od.getDirectory();
        String name = od.getFileName();
        if (name==null)
            return null;
        
		String fileName = directory+name;
			// de-serializing of the panel tree and main window
			FileInputStream  fis = null;
			ObjectInputStream in = null;

			try {
				fis = new FileInputStream(fileName);
				in  = new ObjectInputStream(fis);
				FigureWindow figure = (FigureWindow) in.readObject();
				in.close();
				List<DataSource> list = figure.getDataSources();

				// if folder containing the source images was moved, adapt the file path 
				for(DataSource d: list) {
					
					if(!d.getFileDirectory().equals("") && !d.getFileDirectory().equals(directory))
						d.setFileDirectory(directory);
					if ((d.getFileName()=="")||d.getFileName()==null) 
						d.setFileDirectory("");
					if ((d.getFileDirectory()=="")||d.getFileDirectory()==null) 
						d.setFileName("");
				}
				figure.recover();
				figure.calibrateImage(figure.getDPI(),"cm");

				// display arrows, scale bars and so on
				if(new File(directory+roiFileName).exists())
					figure.readInOldOveray(directory+roiFileName);
				return figure;
			}
			catch(IOException e) {
				System.out.println("ioexc opening figure");
			}
			catch(ClassNotFoundException e) {
				System.out.println("classnotfound opening figure");

			}
			catch(Exception e) { 
				System.out.println(e.toString());
				e.printStackTrace();
			}


		return null;
	}

	/** opens a dialog to select a file name and directory; calls methods to store the result image as tif
	 * and to serialize the other information */
	public void save(FigureWindow figure2save) {
		figure = figure2save;
		String path = IJ.getDirectory("Select/Create Figure Output Folder");
        if (path == null) return;
        // IJ.log(path);
        String folderName = path.substring(0, path.length()-1);
        folderName = folderName.substring(folderName.lastIndexOf(File.separator)+1);
        // IJ.log(folderName);
        storeImageFiles(figure2save, path, folderName);
        figure2save.draw();
	}

	/**@param figure  result image
	 * @param dir directory result gets stored in
	 * @param filename result image file name
	 * stores the result as tif file in the indicated directory
	 * creates a folder in this directory that has the same name as the tif and contains the serialize file
	 * as well as the overlay and copies of every image used to create the result image; 
	 * the file path in the dataSources is updated  */
	private void storeImageFiles(FigureWindow figure, String dir, String filename) {
		System.out.println("writing the serialized file");

		// serialize the panel tree and the main window and store it in the new folder
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
			fos = new FileOutputStream(dir+filename+serFileExtension);   
			out = new ObjectOutputStream(fos);
			out.writeObject(figure);
			out.close();
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}
		// save image using the same name as for the serialization file
		// had to move this a little below, as everything is inside the folder now.
		figure.saveImage(dir,filename);

		// remove the text labels and save the overlay to this folder
		figure.hideAllLabelsAndScalebars();
		saveOverlay(figure.getImagePlus(), dir);


		List<DataSource> list = figure.getDataSources();
		Set<String>	filesFound = new HashSet<String>();
		System.out.println("datasources list"+list.toString());
		// save copies of all source images to this folder
		int n=0;
		for(DataSource dataSource:list)
		{
			String beforeSavePath = dataSource.getFileDirectory();
			if(dataSource.getFileName() != "" && !dataSource.getFileName().isEmpty())
			{
				File source = new File(dataSource.getFileDirectory()+dataSource.getFileName());
				//if there are files with the same name but stored in different directories, one is renamed 
				//so that both can be stored in the same directory
				for(String s: filesFound)
					if(s.endsWith(dataSource.getFileName()) && !s.equals(dataSource.getFileDirectory()+dataSource.getFileName())) {
						{
							rename(dataSource);
						}
					}

				filesFound.add(dataSource.getFileDirectory()+dataSource.getFileName());
				File duplicate = new File(dir+dataSource.getFileName());
				try {
					copyFile(source, duplicate);
					dataSource.setFileDirectory(dir);
				}
				catch (IOException e) {
					IJ.error("couldn't store "+dataSource.getFileName());
				}
			}
			if(dataSource.getExternalSource() != ""){
				//String tempDir = System.getProperty("java.io.tmpdir");
				File source = new File(beforeSavePath+dataSource.getExternalSource());
				File duplicate = new File(dir+dataSource.getExternalSource());
				try {
					// IJ.log(source.toString());
					// IJ.log(duplicate.toString());
					copyFile(source, duplicate);
					dataSource.setFileDirectory(dir);
				}
				catch (IOException e) {
					IJ.error("couldn't store "+dataSource.getExternalSource());
				}
			}
			n++;
			IJ.showStatus("Done saving data files panel "+(n+1)+"/"+list.size());

		}

		// store a text file with the image notes in the new folder
		saveImageNotes(dir, figure);
		IJ.showStatus("Done saving notes.");

		// store a svg version with just the panels in the new folder
		exportAsSVG(dir, filename,  figure);
		IJ.showStatus("Done saving figure.");
		
	}
	private int level;
	private int nPanels;
	private StringBuffer svg;
	
	private void exportAsSVG(String dir, String filename, FigureWindow figure) {
		// TODO Auto-generated method stub
		if (IJ.debugMode) IJ.log("export to svg");
		level = 0;
		nPanels = 0;
		svg = new StringBuffer(Link.getTextFromJar("imgs/empty_figure.svg"));
		// not necessary for now
		// dumpPanel(dir, figure.getRootPanel()); // for the root panel
		svg = svg.replace(svg.indexOf("$FIGUREJDOCNAME"), svg.indexOf("$FIGUREJDOCNAME")+15, filename+".svg");

		listPanels(figure.getRootPanel()); // for root panel children
		// remove last occurrence  of placeholder
		svg = svg.replace(svg.indexOf("$FIGUREJIMAGEPANELS"), svg.indexOf("$FIGUREJIMAGEPANELS"),"");
		Link.createFile (dir+filename+".svg",new String(svg), true);
	}

	private void listPanels(Panel rootPanel) {
		level++;
		ArrayList<Panel> pa = rootPanel.getChildren();
		for (Panel p : pa) {
			dumpPanel(p);
			if (p instanceof ContainerPanel)
				listPanels(p);
		}
		level--;
	}

	private void dumpPanel(Panel p) {
		Rectangle r = p.getRectangle();
		String shape = r.toString();
		shape = shape.substring(shape.indexOf("["));
		String pType = p.toString();
		pType = pType.substring(0, pType.indexOf("@"));
		System.out.println(levelString() + pType + " " + shape);
		if (p instanceof LeafPanel) {
			nPanels++;
			double res = Prefs.get("figure.resolution", 300);
			StringBuffer panelSvg = new StringBuffer("");
			add(panelSvg,"<g");
			add(panelSvg,"inkscape:label","Layer_"+nPanels);
			add(panelSvg,"inkscape:groupmode","layer");
			add(panelSvg,"id","g"+p.hashCode());
			add(panelSvg,">");
			add(panelSvg,"<image");
			add(panelSvg,"y",""+(148-(25.4*this.figure.getImagePlus().getHeight()/res)/2+25.4*p.getY()/res));
			add(panelSvg,"x",""+(105-(25.4*this.figure.getImagePlus().getWidth()/res)/2+25.4*p.getX()/res));
			add(panelSvg,"width",""+25.4*p.getW()/res);
			add(panelSvg,"height",""+25.4*p.getH()/res);
			add(panelSvg,"preserveAspectRatio","xMidYMid");
			add(panelSvg,"xlink:href","$FIGUREJPANELBASE64DATA");
			//TU0AK =="
			add(panelSvg,"id",p.toString());
			add(panelSvg,"sodipodi:insensitive","true");
			add(panelSvg,"inkscape:svg","1");
			add(panelSvg,"style","image-rendering:pixelated");
			add(panelSvg,"/>");
			add(panelSvg,"</g>");
			ImageProcessor ip = figure.getImagePlus().getProcessor();
			ip.setRoi(r);
			ip = ip.crop();
			ImagePlus imp = new ImagePlus("panel", ip);
			byte[] bytes = new FileSaver(imp).serialize();
			String b64 = Base64.getEncoder().encodeToString(bytes);
			b64 = "data:image/tif;base64, "+b64;
			// b64 = b64.replaceAll(".{100}(?=.)", "$0\n");
			// IJ.log(panelSvg.toString());
			panelSvg = panelSvg.replace(panelSvg.indexOf("$FIGUREJPANELBASE64DATA"), panelSvg.indexOf("$FIGUREJPANELBASE64DATA")+23, b64+"\n");
			svg = svg.replace(svg.indexOf("$FIGUREJIMAGEPANELS"), svg.indexOf("$FIGUREJIMAGEPANELS"),panelSvg.toString());
		}
	}

	private void add(StringBuffer panelSvg, String string, String string2) {
		panelSvg.append(string+"=\""+string2+"\"\n");		
	}

	private void add(StringBuffer panelSvg, String string) {
		panelSvg.append(string+"\n");		
	}

	private String levelString() {
		StringBuffer outputBuffer = new StringBuffer();
		for (int i = 0; i < level; i++) {
			outputBuffer.append("-");
		}
		return outputBuffer.toString();
	}
	/**@param d data source which has an image that should be renamed
	 * adds an underscore and 2 random letters + a number to the file name of the data source*/
	private void rename(DataSource d) {
		Random r = new Random();
		String name = d.getFileName();
		String ending = name.substring(name.lastIndexOf("."), name.length());
		name = name.replace(ending, "");
		//add three random numbers as chars; 48 is the ASCII value of 0 
		d.setFileName(name+"_"+(char)(r.nextInt(25)+97)+(char)(r.nextInt(10)+48)+(char)(r.nextInt(25)+97)+ending);
	}

	/**overlay of an image is stored as zip file
	 * @param imp image of which the overlay has to be stored
	 * @param dir directory the overlay is stored to
	 */
	private void saveOverlay(ImagePlus imp, String dir) {
		Overlay overlay = imp.getOverlay();
		if(overlay==null || overlay.size()==0)
		{
			System.out.println("null overlay");
			return;
		}
		RoiManager roiManager = new RoiManager(false);
		for(int i=0; i<overlay.size(); i++)
			roiManager.addRoi(overlay.get(i));
		roiManager.runCommand("Save", dir+roiFileName );
	}

	/**write all image notes/details to a text file
	 * @param path directory the file is stored in
	 * @param figure result window containing the images
	 */
	private void saveImageNotes(String path, FigureWindow figure) {
		String s = figure.getAllImageNotes();
		if(s!=null)
		{
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(path+"imageNotes.txt"));
				out.write(s);
				out.close();
			}
			catch (IOException e){
				IJ.error("could not store the image notes");
			}
		}
	}

	/** http://stackoverflow.com/questions/106770/standard-concise-way-to-copy-a-file-in-java
	 * 
	 * @param  sourceFile file which's content is going to be copied
	 * @param  destFile   file the content is copied to
	 * @throws IOException
	 * if a file with the same name as destFile exists in the target directory, the method does nothing */
	@SuppressWarnings("resource")
	private void copyFile(File sourceFile, File destFile) throws IOException {
		if(!destFile.exists()) {
			destFile.createNewFile();
		}
		else 
			return;

		FileChannel source = null;
		FileChannel destination = null;

		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		}
		finally {
			if(source != null) {
				source.close();
			}
			if(destination != null) {
				destination.close();
			}
		}
	}
}
