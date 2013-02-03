package com.mmir.cbir.engine;

import java.util.Stack;

public class ColorCoherenceVector {
	
	private static int colorInterval = 8;
	private static int numIntervals;
	
	private static int[][] colorTable;
	private static int[][] colorIndexTable;
	private static int colorIndexCount = 0;
	
	public static int[] getCCV(int[][][] DCTMatrix) {
		
		int width = DCTMatrix[0][0].length;//image width(horizontal)
		int height = DCTMatrix[0].length;//image height(vertical)
		int span = 8;//8 x 8 block
		
		numIntervals = width * height / 5000;
		
		//the 2 parameters below you can modify to scale the dimension of the histogram
		int binNum = 8;//number of bins in each color subspace
		int interval = 258000/binNum;//the maximum and the minimum 129000 and -129000
		
		int blknum_h = width/span;
		int blknum_v = height/span;
		
		colorTable = new int[blknum_h][blknum_v];
		colorIndexTable = new int[blknum_h][blknum_v];
		int[] ccv = new int[binNum*binNum*binNum*2/colorInterval];
		
		for( int j = 0; j < blknum_h; j ++) {
			for( int k = 0; k < blknum_v; k ++) {
				int binY = DCTMatrix[0][k*span][j*span]/interval+4;//calculate where the DC coefficient locates in Y subspace
				int binU = DCTMatrix[1][k*span][j*span]/interval+4;//calculate where the DC coefficient locates in U subspace
				int binV = DCTMatrix[2][k*span][j*span]/interval+4;//calculate where the DC coefficient locates in V subspace

				colorTable[j][k] = binY*binNum*binNum+binU*binNum+binV;
			}
		}
		
		//quantize the color values
		for( int x = 0; x < blknum_h; x ++) {
			for( int y = 0; y < blknum_v; y ++) {
				colorTable[x][y] /= colorInterval;
			}
		}
		
		//number the colors
		for( int h = 0; h < blknum_h; h ++) {
			for( int v = 0; v < blknum_v; v ++) {
				if(colorIndexTable[h][v] == 0) {
					colorIndexCount++;
					findCoherentBlocksAndMark(h, v);
				}
			}
		}
		
		int[] colorIndexCountArray = new int[colorIndexCount];
		int[] colorIndexColorMap = new int[colorIndexCount];

		// map the color indexes to the actual colors
		for( int a = 0; a < blknum_h; a ++) {
			for( int b = 0; b < blknum_v; b ++)	{
				if(colorIndexColorMap[colorIndexTable[a][b] - 1] == 0) {
					colorIndexColorMap[colorIndexTable[a][b] - 1] = colorTable[a][b];
				}
				colorIndexCountArray[colorIndexTable[a][b] - 1]++;
			}
		}
		
		for( int z = 0; z < colorIndexCountArray.length; z ++ ) {
			if(colorIndexCountArray[z] > numIntervals) {
				ccv[colorIndexColorMap[z] * 2] += colorIndexCountArray[z];
			} else {
				ccv[colorIndexColorMap[z] * 2 + 1] += colorIndexCountArray[z];
			}
		}
		
		return ccv;
	
	}

	public static void findCoherentBlocksAndMark(int h, int v) {
		Stack<Coordinates> stack = new Stack<Coordinates>();
		stack.push(new Coordinates(h, v));
		while(!stack.empty()) {
			Coordinates coord = stack.pop();
			h = coord.h;
			v = coord.v;
			if(colorIndexTable[h][v] == 0) {
				int max_h = colorIndexTable.length - 1;
				int max_v = colorIndexTable[0].length - 1;
				
				colorIndexTable[h][v] = colorIndexCount;
				
				if(h != 0) {
					if(v != 0) {
						if(colorTable[h][v] == colorTable[h-1][v-1] && colorIndexTable[h-1][v-1] == 0) {
							stack.push(new Coordinates(h-1, v-1));
						}
					}
					if(colorTable[h][v] == colorTable[h-1][v] && colorIndexTable[h-1][v] == 0) {
						stack.push(new Coordinates(h-1, v));
					}
					if(v != max_v) {
						if(colorTable[h][v] == colorTable[h-1][v+1]  && colorIndexTable[h-1][v+1] == 0) {
							stack.push(new Coordinates(h-1, v+1));
						}
					}
				}
				
				if(v != 0) {
					if(colorTable[h][v] == colorTable[h][v-1] && colorIndexTable[h][v-1] == 0) {
						stack.push(new Coordinates(h, v-1));
					}
				}
				if(v != max_v) {
					if(colorTable[h][v] == colorTable[h][v+1] && colorIndexTable[h][v+1] == 0) {
						stack.push(new Coordinates(h, v+1));
					}
				}
				
				if(h != max_h) {
					if(v != 0) {
						if(colorTable[h][v] == colorTable[h+1][v-1] && colorIndexTable[h+1][v-1] == 0) {
							stack.push(new Coordinates(h+1, v-1));
						}
					}
					if(colorTable[h][v] == colorTable[h+1][v] && colorIndexTable[h+1][v] == 0) {
						stack.push(new Coordinates(h+1, v));
					}
					if(v != max_v) {
						if(colorTable[h][v] == colorTable[h+1][v+1] && colorIndexTable[h+1][v+1] == 0) {
							stack.push(new Coordinates(h+1, v+1));
						}
					}
				}
				
			}
		}
	}
}
