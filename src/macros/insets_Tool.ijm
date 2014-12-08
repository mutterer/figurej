var cmd = newMenu("Insets Menu Tool",
	newArray("Mark Last Source Region", "Define Inset Source", "Place Inset","-","1px temp. frame around selection","1px temp. frame around all panels"));

macro "Insets Menu Tool - C037T0b16FT6b10iT8b10gTfb10J" {
	cmd = getArgument();
	if (cmd=="Define Inset Source") {
		run ("Copy");
	} else if (cmd=="Mark Last Source Region") {
   		x = split (call("ij.Prefs.get","figurej.xRoi", ""),",");
   		y = split (call("ij.Prefs.get","figurej.yRoi", ""),",");
   		makeSelection("polygon",x,y);
   		run ("Draw");
   	} else if (cmd=="Place Inset") {
   		getSelectionBounds(x,y,sw,sh);
   		id=getImageID;
   		setBatchMode(true);
   		ffp=sw/sh;
   		run("Internal Clipboard");
   		run("RGB Color");
   		ffc=getWidth/getHeight;
   		if (ffc>ffp) { 
			h=sh; w=h*ffc; 
		} else { 
			w=sw; h=w/ffc; 
		}
   		run("Size...", "width="+w+" height="+h+" constrain interpolate bicubic");
   		run("Canvas Size...", "width=&sw height=&sh position=Center zero");
   		rename("tempInset");
   		idInset = getImageID();
   		selectImage(id);
   		run("Add Image...", "image=[tempInset] x="+x+" y="+y+" opacity=100");
   		selectImage(idInset);
   		close;
   		selectImage(id);
   		run("Select None");

   	}	else if (cmd=="1px temp. frame around all panels") {
   		panels = split(getMetadata('info'),'\n');
   		ta=newArray(-1,-1,-1,-1,-1,-1,-1,-1);
   		for (p=0;p<panels.length;p++){
   			panel = split (panels[p],',');
   			if (panel.length>4) {
   				makeRectangle (panel[0],panel[1],panel[2]-1,panel[3]-1);
   				run("Draw");
   			}
   		}
   		run("Select None");
   		
   	}  else if (cmd=="1px temp. frame around selection") {
   		getSelectionBounds(x,y,sw,sh);
   		makeRectangle(x-1,y-1,sw+1,sh+1);
   		run("Properties... ", "  stroke=white");
   		run("Add Selection...");
   		run("Select None");
   	}
}