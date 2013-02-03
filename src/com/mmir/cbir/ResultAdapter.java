package com.mmir.cbir;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.mmir.cbir.engine.Constants;
import com.mmir.cbir.engine.Result;

public class ResultAdapter extends BaseAdapter {
	LinkedHashMap<Result, Bitmap> mImageCache = new LinkedHashMap<Result, Bitmap>(10, 0.75f, true);
	ArrayList<Result> mResults;
	Context mContext;
	int mRelevanceFeedback = 0;
	class ResultTag {
		ImageView mImage;
		TextView mName, mSimilarity;
		CheckBox mCheck;
	}
	public ResultAdapter(Context context, ArrayList<Result> results) {
		mContext = context;
		mResults = results;
		mRelevanceFeedback = 0;
	}
	public int getCount() {
		return mResults!= null ? mResults.size() : 0;
	}

	public Object getItem(int position) {
		return mResults.get(position);
	}

	public long getItemId(int position) {
		return 0;
	}

	public View getView(int position, View convertView, ViewGroup root) {
		if (convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem, null);
			ResultTag tag = new ResultTag();
			tag.mImage = (ImageView) convertView.findViewById(R.id.imageView1);
			tag.mName = (TextView) convertView.findViewById(R.id.textView1);
			tag.mSimilarity = (TextView) convertView.findViewById(R.id.textView2);
			tag.mCheck = (CheckBox) convertView.findViewById(R.id.checkBox1);
			convertView.setTag(tag);
		}
		final ResultTag tag = (ResultTag) convertView.getTag();
		final Result tuple = mResults.get(position);
		Bitmap bmp = mImageCache.get(tuple);
		if (bmp == null) {
			bmp = BitmapFactory.decodeFile(Constants.IMAGES_DIR + tuple.mFilename);
			mImageCache.put(tuple, bmp);
		}
		tag.mImage.setImageBitmap(bmp);
		tag.mImage.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.parse("file://" + Constants.IMAGES_DIR + tuple.mFilename), "image/*");
				mContext.startActivity(intent);
			}
		});
		tag.mCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				tuple.mIsRelevant = isChecked;
				if (isChecked) mRelevanceFeedback++;
				else mRelevanceFeedback--;
			}
		});
		tag.mName.setText(tuple.mFilename);
		tag.mSimilarity.setText("Similarity : " + tuple.mSimilarity);
		tag.mCheck.setChecked(tuple.mIsRelevant);
		return convertView;
	}

}
