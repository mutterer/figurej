// Inkscape command lines to open a svg file or convert svg to png
// MAC DEMO ONLY PATHS NOT SAFELY ESCAPED 
// /Applications/Inkscape.app/Contents/Resources/bin/inkscape /Users/jerome/Desktop/test.svg -C --export-png=/Users/jerome/Desktop/ink2.png

arg = split(getArgument(),';');
cmd = arg[0];
file = arg[1];
if (cmd=="open") {
exec("open -a "+call("ij.Prefs.get", "figurej.inkscapePath","")+" "+file);
} else if (cmd=="grab") {
outputfile = replace(file,'.svg','.png');
success = exec(call("ij.Prefs.get", "figurej.inkscapePath","")+"Contents/Resources/bin/inkscape "+file+" -C --export-png="+outputfile);
}



