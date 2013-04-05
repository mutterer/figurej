
macro "panel sticking Tool - C00fL00f0L000fCf00R5577C000L7722L2242L2224" {
   getSelectionBounds(rx,ry,rw,rh);
   getCursorLoc(mx, my, z, m);
   while (m&16!=0) {
      getCursorLoc(mx, my, z, m);
      panel = getPanelCoordinates(mx,my); 
      px=parseInt(panel[0]);py=parseInt(panel[1]);
      pw=parseInt(panel[2]);ph=parseInt(panel[3]);
      if (px>-1) {
         left=(mx<px+pw/2); up=(my<py+ph/2);
         makeRectangle(px*left+(px+pw-rw)*!left,py*up+(py+ph-rh)*!up,rw,rh);
      }
   }
}

function getPanelCoordinates(x,y) {
   panels = split(getMetadata('info'),'\n');
   ta=newArray(-1,-1,-1,-1,-1,-1,-1,-1);
   for (p=0;p<panels.length;p++){
      panel = split (panels[p],',');
      if (panel.length>4)
         if (x>=panel[0])
         if (x<=panel[0]+panel[2])
         if (y>=panel[1])
         if (y<=panel[1]+panel[3])
         ta = panel;
   }
   return ta;
}
*/
