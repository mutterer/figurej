// imagej macro that sets some preferences for figurej
Dialog.create("FigureJ Preferences");
Dialog.addCheckbox('Enable External tools', call("ij.Prefs.get","figurej.externalTools",false));
Dialog.addString("Path to USCF Chimera", call("ij.Prefs.get","figurej.chimeraPath","not set"),60);
Dialog.addString("Path to Inkscape binary", call("ij.Prefs.get","figurej.inkscapePath","not set"),60);
Dialog.addString("Inkscape export command", call("ij.Prefs.get","figurej.inkscapeCmd","SVGFILE;-C;--export-type=png;--export-filename=PNGFILE"),60);
Dialog.addCheckbox('Allow Slicing Panels by Dragging the Mouse', call("ij.Prefs.get","figurej.mousePanelSlicing",false));
Dialog.show();
call("ij.Prefs.set", "figurej.externalTools", Dialog.getCheckbox());
call("ij.Prefs.set", "figurej.chimeraPath", Dialog.getString());
call("ij.Prefs.set", "figurej.inkscapePath", Dialog.getString());
call("ij.Prefs.set", "figurej.inkscapeCmd", Dialog.getString());
call("ij.Prefs.set", "figurej.mousePanelSlicing", Dialog.getCheckbox());
