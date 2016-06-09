dpi=getArgument();
getPixelSize(unit, pixelWidth, pixelHeight);
run("Set Scale...", "distance="+dpi+" known=1 pixel=1 unit=inch");
run("Page Setup...", "scale=100 center print_actual");
doCommand("Print...");
setVoxelSize(pixelWidth, pixelHeight, 1, unit);

