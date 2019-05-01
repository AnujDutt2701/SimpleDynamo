package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SimpleDynamoDbHelper extends SQLiteOpenHelper {

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + SimpleDynamoContract.KeyValueEntry.TABLE_NAME + " (" +
                    SimpleDynamoContract.KeyValueEntry.COLUMN_NAME_KEY + " TEXT PRIMARY KEY, " +
                    SimpleDynamoContract.KeyValueEntry.COLUMN_NAME_VALUE + " TEXT)";


    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + SimpleDynamoContract.KeyValueEntry.TABLE_NAME;


    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "SimpleDynamos.db";

    public SimpleDynamoDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public ContentValues formatToSqlContentValues(ContentValues contentValues) {
        ContentValues newContentValue = new ContentValues();
        newContentValue.put("key_string", contentValues.getAsString("key"));
        newContentValue.put("value_string", contentValues.getAsString("value"));
        return newContentValue;
    }

    public ContentValues formatFromSqlContentValues(ContentValues contentValues) {
        ContentValues newContentValue = new ContentValues();
        newContentValue.put("key", contentValues.getAsString("key_string"));
        newContentValue.put("value", contentValues.getAsString("value_string"));
        return newContentValue;
    }
}