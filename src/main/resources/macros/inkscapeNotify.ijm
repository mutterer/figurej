// Inkscape command lines to open a svg file or convert svg to png
// /Applications/Inkscape.app/Contents/Resources/bin/inkscape /Users/jerome/Desktop/test.svg -C --export-png=/Users/jerome/Desktop/ink2.png

arg = split(getArgument(),';');
cmd = arg[0];
file = arg[1];
inkscapePath = call("ij.Prefs.get", "figurej.inkscapePath","");
os = getInfo("os.name");
binary = "";

if (cmd == "open") {
	if (os == "Mac OS X")
		exec("open","-a",inkscapePath, file);
	else
		exec(inkscapePath, file);
} else if (cmd == "grab") {
	outputfile = replace(file,'.svg','.png');
	if (os == "Mac OS X") {
		binary = "Contents/Resources/bin/inkscape";
		success = exec(inkscapePath+File.separator+binary, file, "-C", "--export-png="+outputfile);
	} else {
		// Works both on Windows and Linux
		success = exec(inkscapePath, file, "-C", "--export-png="+outputfile);
	}
}
