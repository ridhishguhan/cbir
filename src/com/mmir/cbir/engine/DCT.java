package com.mmir.cbir.engine;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
public class DCT implements JPEGDecoder.DCTArray{
    
	
	//Each entry of this matrix is the DCT coefficient. Recall that the (0,0) of each 8*8 block is the DC coefficient and the rest are AC
	//The corresponding indices are (Y,Cb, Cr), width, height, respectively
	public int[][][]  DCTmatrix;
	
	
    String file;
    JPEGDecoder j=null;

    // Implementation of DCTArray

   
    int width,height;

    public void setSize(int width, int height){
        this.width = width;
        this.height = height;
       
        DCTmatrix = new int[3][height][width];
    }
    
   

    public void setPixel(int x, int y, int component,int value){
        DCTmatrix[component][y][x]=value;
    }

public DCT(String s) throws Exception{
        file = s;
        j = new JPEGDecoder();
        
        
     
        
        FileInputStream in = null;
		
        try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		j.decode(in,this);
        
		
		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

   }

}
