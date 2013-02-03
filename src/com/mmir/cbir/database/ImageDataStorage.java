package com.mmir.cbir.database;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.mmir.cbir.SearchProvider;
import com.mmir.cbir.engine.ColorCoherenceVector;
import com.mmir.cbir.engine.Constants;
import com.mmir.cbir.engine.DCT;
import com.mmir.cbir.engine.Histogram;
import com.mmir.cbir.engine.Result;

/**
 * Interface class for retrieving image data
 * @author Ridhish Guhan
 *
 */
public class ImageDataStorage {
	
	private static final String IMAGE_DIRECTORY = Constants.IMAGES_DIR;
	
	/**
	 * Compare CCV Similarity with images in database 
	 * @param queryImageHist
	 * @return
	 * @throws FileNotFoundException
	 */
	public static ArrayList<Result> getCCVBasedSimilarity(Context context, int[] queryImageHist) throws FileNotFoundException {
		ArrayList<Result> rv = new ArrayList<Result>();
		
		Cursor cursor = context.getContentResolver().query(SearchProvider.ContentUri.IMAGEDATA
				, null
				, null
				, null
				, null);

		if (cursor != null && cursor.moveToFirst()) {
			while (!cursor.isAfterLast()) {
				String line = cursor.getString(cursor.getColumnIndex(SearchProvider.ImageData.CCV));
				String[] data = line.split(",");
				// parse data
				String imageName = cursor.getString(cursor.getColumnIndex(SearchProvider.ImageData.NAME));
				int[] valueArray = new int[data.length];
				for (int i = 0; i < data.length; i++) {
					valueArray[i] = Integer.parseInt(data[i]);
				}
				try {
					double intersection = Histogram.Intersection(valueArray, queryImageHist);
				
					// create result object
			    	Result result = new Result();
			    	result.mFilename = imageName;
			    	result.mSimilarity = intersection;
			    	
			    	// store result
			    	rv.add(result);
				} catch (Exception ex) {}
		    	cursor.moveToNext();
			}
		}
		return rv;
	}

	/**
	 * Compare Color Histogram Similarity with images in database
	 * @param queryImageHist
	 * @return
	 * @throws FileNotFoundException
	 */
	public static ArrayList<Result> getColorHistogramSimilarity(Context context, int[] queryImageHist) throws FileNotFoundException {
		ArrayList<Result> rv = new ArrayList<Result>();
		
		Cursor cursor = context.getContentResolver().query(SearchProvider.ContentUri.IMAGEDATA
				, null
				, null
				, null
				, null);
		if (cursor != null && cursor.moveToFirst()) {
			while (!cursor.isAfterLast()) {
				String line = cursor.getString(cursor.getColumnIndex(SearchProvider.ImageData.HISTOGRAM));
				String[] data = line.split(",");
				// parse data
				String imageName = cursor.getString(cursor.getColumnIndex(SearchProvider.ImageData.NAME));
				int[] valueArray = new int[data.length];
				for (int i = 0; i < data.length; i++) {
					valueArray[i] = Integer.parseInt(data[i]);
				}
				
				double intersection = Histogram.Intersection(valueArray, queryImageHist);
				
				// create result object
		    	Result result = new Result();
		    	result.mFilename = imageName;
		    	result.mSimilarity = intersection;
		    	
		    	// store result
		    	rv.add(result);
		    	cursor.moveToNext();
			}
		}
		return rv;
	}
	
	/**
	 * Get list of images in database
	 * @return ArrayList<String> filenames
	 */
	private static ArrayList<String> getImagesInFileSystem() {
		// get file list in database
		ArrayList<String> fileNameList = new ArrayList<String>();
		
		File folder = new File(IMAGE_DIRECTORY);
	    File[] listOfFiles = folder.listFiles();
	    
	    for (File currFile : listOfFiles) {
	      if (currFile.isFile() && !currFile.getName().startsWith(".")) {
	    	  fileNameList.add(currFile.getName());
	      }
	    }
	    
	    return fileNameList;
	}

	/**
	 * Build all indexes
	 */
	public static void buildImageIndex(Context context) {
		long initial = System.currentTimeMillis();
		ArrayList<String> fileNameList = getImagesInFileSystem();

	    ArrayList<ContentValues> contentValues = new ArrayList<ContentValues>();
	    for (String name : fileNameList) {
	    	String path = IMAGE_DIRECTORY + name;
			try {
		    	DCT imageDCT = new DCT(path);
		    	int[] chArray = getColorHistogramFromDCT(imageDCT);
		    	int[] ccvArray = getCCVFromDCT(imageDCT);
		    	ContentValues value = new ContentValues();
		    	value.put(SearchProvider.ImageData.NAME, name);
		    	// write ch file
		    	if (chArray != null) {
		    		StringBuilder string = new StringBuilder();
		    		for (int v : chArray) {
		    			if (string.length() > 0) string.append(",");
		    			string.append(v);
		    		}
		    		value.put(SearchProvider.ImageData.HISTOGRAM, string.toString());
		    	}
		    	// write ccv file
		    	if (ccvArray != null) {
		    		StringBuilder string = new StringBuilder();
		    		for (int v : ccvArray) {
		    			if (string.length() > 0) string.append(",");
		    			string.append(v);
		    		}
		    		value.put(SearchProvider.ImageData.CCV, string.toString());
		    	}
		    	contentValues.add(value);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
		if (contentValues.size() > 0) {
			context.getContentResolver().delete(SearchProvider.ContentUri.IMAGEDATA, null, null);
			context.getContentResolver().bulkInsert(SearchProvider.ContentUri.IMAGEDATA
					, contentValues.toArray(new ContentValues[0]));
		}
		long last = System.currentTimeMillis();
	    System.out.println("Indexing for each takes : " + ((last - initial)/(fileNameList.size())));
	}



	public static int[] getColorHistogramFromDCT(DCT imageDCT) {
		int[] imageHist = Histogram.GetYUVHist(imageDCT.DCTmatrix);
		return imageHist;
	}
	

	public static int[] getCCVFromDCT(DCT imageDCT) {
		int[] imageCCV = ColorCoherenceVector.getCCV(imageDCT.DCTmatrix);
		return imageCCV;
	}
}