// imagej macro recieves a message like "tempDir;chimera_script"
// creates a cmd file with script in tempdir
// and passes the cmd path to chimera
if (!File.exists(getDirectory('startup')+'temp'+File.separator)) done = File.makeDirectory(getDirectory('startup')+'temp');

arg = split(getArgument(),';');
file = arg[0];
if (File.exists(file)) done = File.delete(file);
f=File.open(file); 
script = "// This macro code creates panel content\n";
script += "width = " + arg[1]+"; height ="+arg[2]+";\n";
script += "newImage('Untitled','8-bit Ramp', width, height, 1);\n";
print (f,script); 
File.close(f);


	