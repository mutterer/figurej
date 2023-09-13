macro "Scrubbing Tool - C00cT0f20S" { 
  size = 7; 
  id = getImageID( ); 
  setBatchMode( true ); 
  getCursorLoc( x, y, z, flags ); 
  while( flags & 16 > 0 ) { 
    getCursorLoc( x1, y1, z, flags ); 
    size = size + 1 *( flags & 1 ); 
    size = size - 1 *( flags & 2 ); 
    size = maxOf( 2, size ); 
    selectImage( id ); 
    makeRectangle( minOf( x, x1 ), minOf( y, y1 ), abs( x1 - x ), abs( y1 - y ) ); 
    run( "Duplicate...", "title=temp" ); 
    id2 = getImageID( ); 
    run( "Bin...", "x=" + getWidth / size + " y=" + getWidth / size + " bin=Average" );   
    if( getWidth * getHeight > 0 ) { 
      run( "Size...", "width=" + abs( x1 - x ) + " height=" + abs( y1 - y ) + " depth=1 average interpolation=None" ); 
      selectImage( id ); 
      run( "Remove Overlay" ); 
      run( "Add Image...", "image=temp x=" + minOf( x, x1 )+ " y=" + minOf( y, y1 )+ " opacity=100" ); 
    } 
    selectImage( id2 ); 
    close( ); 
  } 
  run( "Select None" ); 
} 
