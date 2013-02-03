package com.mmir.cbir.database;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.mmir.cbir.SearchProvider;
import com.mmir.cbir.engine.Constants;

import android.content.ContentValues;
import android.content.Context;

public class Annotations {
	private String conceptsFile = Constants.IMAGES_DIR + "Concepts81.txt";
	private String annotationsFile = Constants.IMAGES_DIR + "Annotation.txt";
	private String concepts[] = new String[81];

	private Context mContext;

	public Annotations(Context context) {
		mContext = context;
	}

	public int insertInDatabase(ArrayList<ContentValues> values) {
		mContext.getContentResolver().delete(SearchProvider.ContentUri.ANNOTATIONS, null, null);
		return mContext.getContentResolver().bulkInsert(SearchProvider.ContentUri.ANNOTATIONS, values.toArray(new ContentValues[0]));
	}

	public String[] readConceptsFromFile() {
		try {
			String line;
			int count = 0;
			FileReader f = new FileReader(conceptsFile);
			BufferedReader br = new BufferedReader(f);
			while(count<81) {
				line = br.readLine();
				concepts[count++] = line.toLowerCase();
			}
			return concepts;
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} catch(IOException e) {
			System.out.println(e);
		}
		return null;
	}

	public ArrayList<ContentValues> annotationValues() {
		ArrayList<ContentValues> images = new ArrayList<ContentValues>();
		try {
			FileReader f = new FileReader(annotationsFile);
			BufferedReader br = new BufferedReader(f);
			int index = 0;
			String conceptLine[];
			while(index < 500) {
				String line = br.readLine();
				conceptLine = line.split("\t");
				StringBuilder builder = new StringBuilder();
				for(int i=0; i < 81; i++) {
					if(Integer.parseInt(conceptLine[i].trim()) == 1) {
						if (builder.length() > 0) builder.append(" ");
						builder.append(concepts[i]);
					}
				}
				ContentValues values = new ContentValues();
				values.put(SearchProvider.Annotations.NAME, String.valueOf(index + 1) + ".jpg");
				values.put(SearchProvider.Annotations.CONCEPTS, builder.toString());
				images.add(values);
				index++;
			}
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} catch(IOException e) {
			System.out.println(e);
		}
        return images;
	}
}
