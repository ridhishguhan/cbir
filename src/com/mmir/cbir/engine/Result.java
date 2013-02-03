package com.mmir.cbir.engine;

public class Result {
	
	public double mSimilarity;
	public String mFilename;
	public String[] mConcepts;
	public boolean mIsRelevant = false;
	
	public Result() {
		mSimilarity = 0;
		mFilename = "";
	}
}