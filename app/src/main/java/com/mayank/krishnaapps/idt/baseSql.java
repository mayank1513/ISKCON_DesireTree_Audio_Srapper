package com.mayank.krishnaapps.idt;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.mayank.krishnaapps.idt.MainActivity.baseF;

public class baseSql extends SQLiteOpenHelper {
    private static int version = 1;
    static String OFFICIAL_NAME = "o";
    baseSql(Context context) {
        super(context, baseF + "/db/hari", null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_list (_id INTEGER PRIMARY KEY AUTOINCREMENT, tbl TEXT, hasDefTables INTEGER DEFAULT 1, unique(tbl))");
        db.execSQL("create table alb (_id integer primary key autoincrement, title, arte, " +
                "parent integer default -1, sublabs integer default 0, audios integer default 0)");
        createDefaultTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
    private static void createDefaultTables(SQLiteDatabase db) {
        String specialAlbums[] = {"Vaiṣṇava Bhajans", "Bhagavad Gītā", "Śrīmad Bhāgavatam", "Caitanya Caritāmṛta", "Preraṇa", "Chetana"};
        for(String s:specialAlbums){
            ContentValues values = new ContentValues();
            values.put("title", s);
            db.insert("alb", null, values);
        }
        for(int i = 1; i<=18; i++){
            ContentValues values = new ContentValues();
            values.put("title", "Chapter " + i);
            values.put("parent", 2);
            db.insert("alb", null, values);
        }
        for(int i = 1; i<=12; i++){
            ContentValues values = new ContentValues();
            values.put("title", "Canto " + i);
            values.put("parent", 3);
            db.insert("alb", null, values);
        }
        String lila[] = {"Ādī Līlā", "Madhya Līlā", "Antya Līlā"};
        for(String s:lila){
            ContentValues values = new ContentValues();
            values.put("title", s);
            values.put("parent", 4);
            db.insert("alb", null, values);
        }
    }
}
