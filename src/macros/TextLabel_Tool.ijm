var label = 'label';

macro "TextLabel Tool (alt to remove) - C00c B00 P1c1ffffc0 T0b08T T6b08X Tbb08T" {
   
   getCursorLoc(x, y, z, flags);
   if(flags&8>0) {
      removeLabel();
      exit();
   } 
   getDateAndTime(yr, mo, dw, d, h, m, s, ms);
   uid = "labeltool_"+yr+""+mo+""+d+""+h+""+m+""+s+""+ms;
   nbefore = Overlay.size;
    getCursorLoc(x1, y1, z, flags);
   while (flags&16>0) {
      getCursorLoc(x1, y1, z, flags);
      drawItem();
      wait(30);
      while (Overlay.size>nbefore) Overlay.removeSelection(Overlay.size-1);
   }
   drawItem();
   label =getString("Enter label", label);
   while (Overlay.size>nbefore) Overlay.removeSelection(Overlay.size-1);
   drawItem();
   exit();
   function drawItem() {
      setFont("user");
      makeText(label, x1, y1-getValue("font.height"));
      Roi.setName(uid);
      Overlay.addSelection(""+hexCol());
      run("Select None");
   }
   function hexCol() {
      return IJ.pad(toHex(getValue("rgb.foreground")),6);
   }
   function removeLabel() {
      uid="nofound";
      n = Overlay.size;
      for (i=0;i<n;i++) {
         Overlay.activateSelection(i);
         if ((Roi.getType=="text")&&Roi.contains(x,y)) {
            uid = Roi.getName;
            break;
         }
      }
      for (i=0;i<n;i++) {
         Overlay.activateSelection(n-i-1);
         name = Roi.getName;
         if (name==uid) Overlay.removeSelection(n-i-1);
      }
      run("Select None"); 
      return;
   }
}


