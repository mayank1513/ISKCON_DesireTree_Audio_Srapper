package com.mayank.krishnaapps.idt;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.mayank.krishnaapps.idt.MainActivity.baseF;

public class audioSql extends SQLiteOpenHelper {
    private static int version = 1;
    audioSql(Context context, String name) {
        super(context, baseF + "/db/" + name + "_audio", null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table audio (_id integer primary key autoincrement, title, parent integer, arte, url, " +
                "date datetime, place integer, lang integer, size integer, size1 integer, alb default ' ', ref)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    static Audio getAudio(Cursor c){
        Audio audio = new Audio();
        audio.id = c.getLong(0);
        audio.parent = c.getLong(c.getColumnIndex("parent"));
        audio.arte = c.getString(c.getColumnIndex("arte"));
        audio.title = c.getString(c.getColumnIndex("title"));
        audio.url = c.getString(c.getColumnIndex("url"));
        audio.date = c.getString(c.getColumnIndex("date"));
        audio.place = c.getInt(c.getColumnIndex("place"));
        audio.size = c.getLong(c.getColumnIndex("size"));
        audio.ref = c.getString(c.getColumnIndex("ref"));
        return audio;
    }
}
