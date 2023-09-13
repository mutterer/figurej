// Inkscape command lines to open a svg file or convert svg to png
// MAC an WIN ONLY 
// /Applications/Inkscape.app/Contents/Resources/bin/inkscape /Users/jerome/Desktop/test.svg -C --export-png=/Users/jerome/Desktop/ink2.png

arg = split(getArgument(),';');
cmd = arg[0];
file = arg[1];

inkscapePath = call("ij.Prefs.get", "figurej.inkscapePath","");
inkscapeCmd = call("ij.Prefs.get", "figurej.inkscapeCmd","");

os = getInfo("os.name");

if (cmd=="open") {
	if (indexOf(os,"Win")>-1) {
		exec(inkscapePath, file);
	} else {
		exec("open","-a",inkscapePath, file);
	}
} else if (cmd=="grab") {
	
	outputfile = replace(file,'.svg','.png');
	if (indexOf(os,"Mac")>-1) {
		
		inkscapeCmd = replace(inkscapeCmd,'SVGFILE', file);
		inkscapeCmd = replace(inkscapeCmd,'PNGFILE', outputfile);
				print(inkscapeCmd);
		
		args = split(inkscapeCmd,";");
		
	
		print(inkscapePath, file, args[1], args[2],args[3]);
		success = exec(inkscapePath, file, args[1], args[2],args[3]);
		
	} else if (indexOf(os,"Win")>-1) {
		success = exec(inkscapePath, file, "-C", "--export-png="+outputfile);
	} else if (indexOf(os,"Lin")>-1) {
		exit("Inkscape not yet supported in Linux");
	}
	
}



