// imagej macro that displays a credits message and takes you to fj website.
url='http://imagejdocu.tudor.lu/doku.php?id=plugin:utilities2:figurej:start';
Dialog.create("About FigureJ");
Dialog.addMessage("Easy article figures with FigureJ\n \nJerome Mutterer & Edda Zinck\nCNRS, 2012-2020");
Dialog.addMessage("\nClick 'Help' to proceed to FigureJ homepage with video tutorials.");
Dialog.addHelp(url);
Dialog.show();


