package com.mmir.cbir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.mmir.cbir.database.Annotations;
import com.mmir.cbir.database.ImageDataStorage;
import com.mmir.cbir.database.ImageRetrieve;
import com.mmir.cbir.engine.KeywordWeight;
import com.mmir.cbir.engine.Result;

public class SearchActivity extends ListActivity {
	ImageView mQueryImage;
	EditText mTextQueryField;
	Button mSearchButton;
	ProgressDialog mProgressDialog;
	String mPathToQueryImage;
	String mTextQuery;

	ResultAdapter mAdapter;
	private class SearchTask extends AsyncTask<String, Integer, ArrayList<Result>> {
		@Override
		protected ArrayList<Result> doInBackground(String... arg0) {
			Map<String, KeywordWeight> weightedKeywords = new HashMap<String, KeywordWeight>();

			if (mAdapter != null && mAdapter.mRelevanceFeedback > 0) {
				int numRel = 0;
				int numNRel = 0;

				// use relevance feedback for this query
				ArrayList<Result> results = mAdapter.mResults;
				for (Result result : results) {
					String concepts[] = result.mConcepts;
					if (result.mIsRelevant) {
						for (String concept : concepts) {
							KeywordWeight weight = weightedKeywords.get(concept);
							if (weight == null) {
								weight = new KeywordWeight();
								weight.keyword = concept;
								weight.weight = 1f;
								weight.numRel++;
								weightedKeywords.put(concept, weight);
							} else {
								weight.numRel++;
							}
						}
						numRel++;
					} else {
						for (String concept : concepts) {
							KeywordWeight weight = weightedKeywords.get(concept);
							if (weight == null) {
								weight = new KeywordWeight();
								weight.keyword = concept;
								weight.numNRel++;
								weightedKeywords.put(concept, weight);
							} else {
								weight.numNRel++;
							}
						}
						numNRel++;
					}

				}
				for (KeywordWeight weight : weightedKeywords.values()) {
					// 0.5 formula
					weight.weight = (weight.numRel + 0.5)
						* (numNRel - (weight.numNRel + 2*weight.numRel)+ 0.5)
						/ ((weight.numNRel + 0.5) * (numRel - weight.numRel + 0.5));
					if (weight.weight < 0) weight.weight = Math.log(-1 * weight.weight);
					else if (weight.weight > 0) weight.weight = Math.log(weight.weight);
				}
			} else if (!TextUtils.isEmpty(mTextQuery)){
				// no relevance feedback. perform normal query
				mTextQuery = mTextQuery.toLowerCase();
				String keywords[] = mTextQuery.split(" ");
				for (String keyword : keywords) {
					keyword = keyword.trim();
					KeywordWeight wkw = new KeywordWeight();
					wkw.keyword = keyword;
					wkw.weight = 1f;
					weightedKeywords.put(keyword, wkw);
				}
			}
			return ImageRetrieve.findSimilar(SearchActivity.this, arg0, weightedKeywords);
		}

		@Override
		protected void onPostExecute(ArrayList<Result> result) {
			super.onPostExecute(result);
			mAdapter = new ResultAdapter(SearchActivity.this, result);
			getListView().setAdapter(mAdapter);
			if (mProgressDialog != null && mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
			}
		}
		
	}

	private class IndexTask extends AsyncTask<String, Integer, Boolean> {
		@Override
		protected Boolean doInBackground(String... arg0) {
			ImageDataStorage.buildImageIndex(SearchActivity.this);
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (mProgressDialog != null && mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
				Toast.makeText(SearchActivity.this, R.string.built_index, Toast.LENGTH_SHORT).show();
			}
		}
		
	}
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mQueryImage = (ImageView) findViewById(R.id.imageView1);
        mTextQueryField = (EditText) findViewById(R.id.editText1);
        mSearchButton = (Button) findViewById(R.id.searchButton);
        mSearchButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mTextQuery = mTextQueryField.getText().toString().trim();
				if (!TextUtils.isEmpty(mPathToQueryImage) || !TextUtils.isEmpty(mTextQuery)) {
					mProgressDialog = new ProgressDialog(SearchActivity.this);
					mProgressDialog.setTitle("Search");
					mProgressDialog.setMessage("Searching for matches..");
					mProgressDialog.show();
					(new SearchTask()).execute(mPathToQueryImage);
				}
			}
		});
        mQueryImage.setOnClickListener(new View.OnClickListener() {
			
            public void onClick(View arg0) {
                // in onCreate or any event where your want the user to
                // select a file
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,
                        "Select Picture"), 1);
            }
        });
    }

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
		    if (requestCode == 1) {
		        Uri selectedImageUri = data.getData();
		        mPathToQueryImage = getPath(selectedImageUri);
		        mQueryImage.setImageBitmap(BitmapFactory.decodeFile(mPathToQueryImage));
		    }
		}
	}

	public String getPath(Uri uri) {
		String[] projection = { MediaStore.Images.Media.DATA };
		Cursor cursor = managedQuery(uri, projection, null, null, null);
		int column_index = cursor
		        .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case 1:
			mProgressDialog = new ProgressDialog(this);
			mProgressDialog.setTitle("Index");
			mProgressDialog.setMessage("Building index ....");
			mProgressDialog.show();
			(new IndexTask()).execute("mmir");
			break;
		case 2:
			Annotations annotations = new Annotations(SearchActivity.this);
			annotations.readConceptsFromFile();
			ArrayList<ContentValues> images = annotations.annotationValues();
			annotations.insertInDatabase(images);
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.clear();
		menu.add(0, 1, 0, R.string.rebuild_index);
		menu.add(0, 2, 0, R.string.add_annotation);
		return super.onPrepareOptionsMenu(menu);
	}

}