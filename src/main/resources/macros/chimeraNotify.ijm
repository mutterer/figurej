// imagej macro recieves a message like "tempDir;chimera_script"
// creates a cmd file with script in tempdir
// and passes the cmd path to chimera
if (!File.exists(getDirectory('startup')+'temp'+File.separator)) done = File.makeDirectory(getDirectory('startup')+'temp');

arg = split(getArgument(),';');
file = arg[0]+"chimera.cmd";
if (File.exists(file)) done = File.delete(file);
f=File.open(file); print (f,arg[1]); File.close(f);
exec(call("ij.Prefs.get", "figurej.chimeraPath",""), "--send",file);
