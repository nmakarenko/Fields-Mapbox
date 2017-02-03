package com.cropiotest.fields;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "db_fields.db";
    private static MyDBHelper myDBHelper;
    private SQLiteDatabase mDatabase;
    private int mOpenCounter;

    public static synchronized MyDBHelper getInstance(Context context, String dbName) {
        if (myDBHelper == null) myDBHelper = new MyDBHelper(context, dbName);
        return myDBHelper;
    }

    public static synchronized MyDBHelper getInstance(Context context) {
        return getInstance(context, DB_NAME);
    }

    private MyDBHelper(Context context) {
        super(context, DB_NAME, null, 1);
    }

    private MyDBHelper(Context context, String dbName) {
        super(context, dbName, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE fields ("
                + "id INTEGER PRIMARY KEY NOT NULL, "
                + "name VARCHAR(50), "
                + "crop VARCHAR(50), "
                + "till_area double " + ");");
        db.execSQL("CREATE TABLE coordinates ("
                + "field_id INTEGER, "
                + "coord_x DOUBLE, "
                + "coord_y DOUBLE, "
                + "polygon_id INTEGER, "
                + "ring_id INTEGER " + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public synchronized SQLiteDatabase openDatabase() {
        mOpenCounter++;
        if (mOpenCounter == 1) {
            // Opening new database
            mDatabase = myDBHelper.getWritableDatabase();
        }
        return mDatabase;
    }

    public synchronized void closeDatabase() {
        mOpenCounter--;
        if (mOpenCounter == 0) {
            // Closing database
            mDatabase.close();
        }
    }
}