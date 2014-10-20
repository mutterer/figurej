// Inkscape command lines to open a svg file or convert svg to png
// MAC an WIN ONLY 
// /Applications/Inkscape.app/Contents/Resources/bin/inkscape /Users/jerome/Desktop/test.svg -C --export-png=/Users/jerome/Desktop/ink2.png

arg = split(getArgument(),';');
cmd = arg[0];
file = arg[1];
inkscapePath = call("ij.Prefs.get", "figurej.inkscapePath","");
os = getInfo("os.name");
binary = "";

if (cmd=="open") {
	if (indexOf(os,"Win")>-1) {
		exec(inkscapePath, file);

	} else {
		exec("open","-a",inkscapePath, file);
	}
} else if (cmd=="grab") {
	
	outputfile = replace(file,'.svg','.png');
	if (indexOf(os,"Mac")>-1) {
		binary = "Contents/Resources/bin/inkscape";
		success = exec(inkscapePath+File.separator+binary, file, "-C", "--export-png="+outputfile);
	} else if (indexOf(os,"Win")>-1) {
		success = exec(inkscapePath, file, "-C", "--export-png="+outputfile);
	} else if (indexOf(os,"Lin")>-1) {
		exit("Inkscape not yet supported in Linux");
	}
	
}



