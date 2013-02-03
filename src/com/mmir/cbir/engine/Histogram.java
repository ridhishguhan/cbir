package com.mmir.cbir.engine;

public class Histogram {
public static int[] GetYUVHist(int[][][] DCTMatrix)
{
	
	//int yuv = 3; it is the first dimension of array DCTMatrix
	int width = DCTMatrix[0][0].length;//image width(horizontal)
	int height = DCTMatrix[0].length;//image height(vertical)
	int span = 8;//8 x 8 block
	
	//the 2 parameters below you can modify to scale the dimension of the histogram
	int binNum = 8;//number of bins in each color subspace
	int interval = 258000/binNum;//the maximum and the minimum 129000 and -129000
	
	int blknum_h = width/span;
	
	int blknum_v = height/span;
	
	
	
	int[] hist = new int[binNum*binNum*binNum];
	for( int j = 0; j < blknum_h; j ++)
		for( int k = 0; k < blknum_v; k ++)
		{
			int binY = DCTMatrix[0][k*span][j*span]/interval+4;//calculate where the DC coefficient locates in Y subspace
			int binU = DCTMatrix[1][k*span][j*span]/interval+4;//calculate where the DC coefficient locates in U subspace
			int binV = DCTMatrix[2][k*span][j*span]/interval+4;//calculate where the DC coefficient locates in V subspace
			
			hist[binY*binNum*binNum+binU*binNum+binV]++;
			
		}
	
	return hist;

}
// histogram intersection
public static double Intersection(int[] h1, int[] h2)
{
	int norm1=0, norm2=0;
	for( int i = 0; i < h1.length; i ++)
	{
		norm1+=h1[i];
		norm2+=h2[i];
	}
	
	double intersection = 0.0;
	
	double[] norm_h1 = new double[h1.length];
	double[] norm_h2 = new double[h1.length];
	
	//normalize each histogram
	for( int i = 0; i < h1.length; i ++)
	{

		norm_h1[i] = (double)h1[i]/(double)norm1;
		norm_h2[i] = (double)h2[i]/(double)norm2;
	}
	
	for( int i = 0; i < h1.length; i++)
	{
		if( norm_h1[i]>norm_h2[i])
			intersection += norm_h2[i];
		else
			intersection += norm_h1[i];
		
	}
	
	return intersection;

}
}
