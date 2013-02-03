package com.mmir.cbir;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class SearchProvider extends ContentProvider {

    private static final String AUTHORITY = "com.mmir.cbir.SearchProvider";
    public static final String CONTENT_URI_STRING = "content://" + AUTHORITY;
    public static final Uri CONTENT_URI = Uri.parse(CONTENT_URI_STRING);
    public static final String _ID = "_id";

    private static final String DB_NAME = "textsearch.db";
    private static final int DB_VERSION = 1;

    private static final int ANNOTATIONS = 1;
    private static final int IMAGEDATA = 2;

    private DatabaseHelper mDbHelper;
    private static UriMatcher mUriMatcher;

    public static class ContentUri {
        public static final Uri ANNOTATIONS = Uri.parse(CONTENT_URI_STRING + "/"
                + Annotations.TABLE);
        public static final Uri IMAGEDATA = Uri.parse(CONTENT_URI_STRING + "/"
                + ImageData.TABLE);
    }

    // SQL commands for creating tables
    public class Annotations {
        public static final String TABLE = "annotations";

        // columns
        public static final String NAME = "name";
        public static final String PATH = "path";
        public static final String CONCEPTS = "concepts";
        static final String CREATE_STRING = "CREATE VIRTUAL TABLE " + TABLE + " USING fts3("
                + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + NAME + " VARCHAR(255),"
                + PATH + " VARCHAR(255),"
                + CONCEPTS + " TEXT"
                + ");";
    }

    // SQL commands for creating tables
    public class ImageData {
        public static final String TABLE = "imageData";

        // columns
        public static final String NAME = "name";
        public static final String PATH = "path";
        public static final String HISTOGRAM = "histogram";
        public static final String CCV = "ccv";
        static final String CREATE_STRING = "CREATE TABLE " + TABLE + " ("
                + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + NAME + " VARCHAR(255),"
                + PATH + " VARCHAR(255),"
                + HISTOGRAM + " TEXT,"
                + CCV + " TEXT"
                + ");";
    }

    private class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.setLockingEnabled(true);
            db.execSQL(Annotations.CREATE_STRING);
            db.execSQL(ImageData.CREATE_STRING);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + Annotations.TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + ImageData.TABLE);
            // adding new tables
            db.execSQL(Annotations.CREATE_STRING);
            db.execSQL(ImageData.CREATE_STRING);
        }
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String table = null;
        int count;
        switch (mUriMatcher.match(uri)) {
        case ANNOTATIONS:
        	table = Annotations.TABLE;
        	break;
        case IMAGEDATA:
        	table = ImageData.TABLE;
        	break;        	
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        count = db.delete(table, where, whereArgs);
        notify(uri);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        String table = null;
        switch (mUriMatcher.match(uri)) {
        case ANNOTATIONS:
            table = Annotations.TABLE;
            break;
        case IMAGEDATA:
        	table = ImageData.TABLE;
        	break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (null == values) {
            throw new IllegalArgumentException("cannot bulk insert NOTHING!! "
                    + uri);
        }
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.beginTransaction();
        long rowId = 0;
        for (int i = 0; i < values.length; i++) {
            rowId = db.insert(table, null, values[i]);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        if (rowId > 0) {
            notify(uri);
        }
        return values.length;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        String table = null;
        switch (mUriMatcher.match(uri)) {
        case ANNOTATIONS:
            table = Annotations.TABLE;
            break;
        case IMAGEDATA:
        	table = ImageData.TABLE;
        	break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        ContentValues values;
        if (initialValues != null) {
            values = initialValues;
        } else {
            values = new ContentValues();
        }
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long rowId = db.insert(table, null, values);
        if (rowId > 0) {
            Uri newUri = ContentUris.withAppendedId(uri, rowId);
            notify(uri);
            return newUri;
        }
        return null;
    }

    private void notify(Uri uri) {
        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(uri, null);
    }

    private Uri parent(Uri uri) {
        Uri retUri = null;
        switch (mUriMatcher.match(uri)) {
        case ANNOTATIONS:
            retUri = ContentUri.ANNOTATIONS;
            break;
        case IMAGEDATA:
        	retUri = ContentUri.IMAGEDATA;
        	break;
        default:
            retUri = uri;
        }
        return retUri;
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String groupBy = null;
        String sortBy = sortOrder;
        String limit = null;
        switch (mUriMatcher.match(uri)) {
        case ANNOTATIONS:
            qb.setTables(Annotations.TABLE);
            sortBy = Annotations.NAME + " ASC";
            break;
        case IMAGEDATA:
            qb.setTables(ImageData.TABLE);
            sortBy = null;
        	break;
        default:
            return null;
        }
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor c = null;
        c = qb.query(db, projection, selection, selectionArgs, groupBy, null,
                sortBy, limit);
        ContentResolver cr = getContext().getContentResolver();
        c.setNotificationUri(cr, parent(uri));
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int count;
        String table = null;
        switch (mUriMatcher.match(uri)) {
        case ANNOTATIONS:
        	table = Annotations.TABLE;
        	break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        count = db.update(table, values, selection, selectionArgs);
        notify(uri);
        return count;
    }

    static {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(AUTHORITY, Annotations.TABLE, ANNOTATIONS);
        mUriMatcher.addURI(AUTHORITY, ImageData.TABLE, IMAGEDATA);
    }
}
