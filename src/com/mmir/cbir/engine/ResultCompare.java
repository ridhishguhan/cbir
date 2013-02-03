package com.mmir.cbir.engine;
import java.util.Comparator;

public class ResultCompare implements Comparator<Result> {
	public int compare(Result t1, Result t2) {
		if (t1.mSimilarity > t2.mSimilarity) return -1;
		else if (t1.mSimilarity == t2.mSimilarity) return 0;
		else return 1;
	}
}