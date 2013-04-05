// used from link store method, opens an img, checks dimensions against last
// selected panel, adjusts if necessary, and pastes to figure.
setBatchMode(1);
id=getImageID;
open(getArgument());
w=call('ij.Prefs.get','selectedPanel.w',1);
h=call('ij.Prefs.get','selectedPanel.h',1);
if ((getWidth!=w)||(getHeight!=h)) {
	run('Size...', 'width=&w height=&h');
} 
run('Copy');
selectImage(id);
run('Paste');
