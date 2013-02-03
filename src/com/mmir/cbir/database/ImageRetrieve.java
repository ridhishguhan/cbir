package com.mmir.cbir.database;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.mmir.cbir.SearchProvider;
import com.mmir.cbir.engine.ColorCoherenceVector;
import com.mmir.cbir.engine.Constants;
import com.mmir.cbir.engine.DCT;
import com.mmir.cbir.engine.Histogram;
import com.mmir.cbir.engine.KeywordWeight;
import com.mmir.cbir.engine.Result;
import com.mmir.cbir.engine.ResultCompare;

public class ImageRetrieve {

	/**
	 * @param args
	 */
	public static ArrayList<Result> findSimilar(Context context, String[] args, Map<String, KeywordWeight> keywords) {
		ArrayList<Result> resultArray = new ArrayList<Result>();

		Map<String, String> annotationMatches = new HashMap<String, String>();

		float histogramWeight = Constants.WEIGHT_COLOR_HISTOGRAM;
		float ccvWeight = Constants.WEIGHT_COLOR_COHERENCE_VECTOR;

		String queryPath = args[0];
		if (TextUtils.isEmpty(queryPath)) {
			histogramWeight = 0; ccvWeight = 0; // no need to do image comparisons as no query image provided
		}
		
		// remove keywords with 0 weight
		ArrayList<KeywordWeight> toRemove = new ArrayList<KeywordWeight>();
		for (KeywordWeight weight : keywords.values()) {
			if (weight.weight == 0) {
				toRemove.add(weight);
			}
		}
		for (int i = 0; i < toRemove.size(); i++) {
			keywords.remove(toRemove.get(i));
		}
		toRemove.clear();
		if (!keywords.isEmpty()) {
			String keywordlist[] = keywords.keySet().toArray(new String[0]);
			for (int i = 0; i < keywordlist.length; i++) {
				StringBuilder builder = new StringBuilder();
				builder.append(SearchProvider.Annotations.CONCEPTS + " MATCH '"); 
				builder.append(keywordlist[i].trim());
				builder.append("'");
				Cursor matches = context.getContentResolver().query(SearchProvider.ContentUri.ANNOTATIONS
						, null
						, builder.toString()
						, null
						, null);
				if (matches != null && matches.moveToFirst()) {
					while (!matches.isAfterLast()) {
						String name = matches.getString(matches.getColumnIndex(SearchProvider.Annotations.NAME));
						String concepts = matches.getString(matches.getColumnIndex(SearchProvider.Annotations.CONCEPTS));
						annotationMatches.put(name, concepts);
						matches.moveToNext();
					}
					matches.close();
				}
			}
		}
		try {
			
			ArrayList<Result> histogramResults = null;
			if (histogramWeight != 0) {
				histogramResults = getHistogram(context, queryPath);
				resultArray = histogramResults;
			}
			
			ArrayList<Result> ccvResults = null;
			if (ccvWeight != 0) {
				ccvResults = getCCV(context, queryPath);
				resultArray = ccvResults;
			}
			
			ArrayList<Result> weightedResults = new ArrayList<Result>();
			for (int i = 0; i < resultArray.size(); i++) {
				Result currHist = histogramResults == null ? new Result() : histogramResults.get(i);
				Result currCCV = ccvResults == null ? new Result() : ccvResults.get(i);
				float wSim = 0;
				Result currW = new Result();
				currW.mFilename = resultArray.get(i).mFilename;
				String concepts = annotationMatches.get(currW.mFilename);
				if (!TextUtils.isEmpty(concepts) && !keywords.isEmpty()) {
					for (KeywordWeight weight : keywords.values()) {
						String word = weight.keyword;
						if (concepts.contains(word)) {
							wSim += weight.weight;
						}
					}
					annotationMatches.remove(currW.mFilename);
				}
				currW.mSimilarity = (histogramWeight * currHist.mSimilarity + ccvWeight * currCCV.mSimilarity)/(histogramWeight + ccvWeight) + wSim;
				if (currW.mSimilarity > 0) weightedResults.add(currW);
			}
			for (Map.Entry<String, String> remaining : annotationMatches.entrySet()) {
				float wSim = 0;
				Result tuple = new Result();
				tuple.mFilename = remaining.getKey();
				String concepts = remaining.getValue();
				concepts = concepts.toLowerCase();
				for (KeywordWeight weight : keywords.values()) {
					String word = weight.keyword;
					if (concepts.contains(word)) {
						wSim += weight.weight;
					}
				}
				tuple.mSimilarity = wSim;
				if (tuple.mSimilarity > 0) weightedResults.add(tuple);
			}
		    ResultCompare tc = new ResultCompare();
			Collections.sort(weightedResults, tc);

			// get annotations for all the files returned
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < ((weightedResults.size() <= 30) ? weightedResults.size() : 30); i++) {
				if (builder.length() > 0) builder.append(",");
				builder.append("'" + weightedResults.get(i).mFilename + "'");
			}
			builder.insert(0, SearchProvider.Annotations.NAME + " IN (");
			builder.append(")");
			Cursor matches = context.getContentResolver().query(SearchProvider.ContentUri.ANNOTATIONS
					, null
					, builder.toString()
					, null
					, null);
			if (matches != null && matches.moveToFirst()) {
				while (!matches.isAfterLast()) {
					String name = matches.getString(matches.getColumnIndex(SearchProvider.Annotations.NAME));
					String concepts = matches.getString(matches.getColumnIndex(SearchProvider.Annotations.CONCEPTS));
					annotationMatches.put(name, concepts);
					matches.moveToNext();
				}
				matches.close();
				for (int i = 0; i < ((weightedResults.size() <= 30) ? weightedResults.size() : 30); i++) {
					String concepts = annotationMatches.get(weightedResults.get(i).mFilename);
					weightedResults.get(i).mConcepts = concepts.split(" ");
				}				
			}
			if (weightedResults.size() > 30) return new ArrayList<Result>(weightedResults.subList(0, 30));
			else return weightedResults;
		}
		
		// any exception comes
		catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Compute Histogram
	 * @param context
	 * @param queryPath
	 * @param imageDir
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<Result> getHistogram(Context context, String queryPath) throws Exception {
		DCT queryImage = new DCT(queryPath);
		int[] queryImageHist = Histogram.GetYUVHist(queryImage.DCTmatrix);
	    // calculate color histogram for each image and get the similarity
	    ArrayList<Result> rv = ImageDataStorage.getColorHistogramSimilarity(context, queryImageHist);
	    return rv;
	}
	
	
	/**
	 * Get the Color Coherence Vector
	 * @param context
	 * @param queryPath
	 * @param imageDir
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<Result> getCCV(Context context, String queryPath) throws Exception {
		DCT queryImage = new DCT(queryPath);
		int[] queryImageHist = ColorCoherenceVector.getCCV(queryImage.DCTmatrix);
	    // calculate color histogram for each image and get the similarity
	    ArrayList<Result> rv = ImageDataStorage.getCCVBasedSimilarity(context, queryImageHist);
	    return rv;
	}

}