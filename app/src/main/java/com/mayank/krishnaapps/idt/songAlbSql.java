package com.mayank.krishnaapps.idt;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.mayank.krishnaapps.idt.MainActivity.baseF;

public class songAlbSql extends SQLiteOpenHelper {
    private static final int version = 1;
    private String Args[], Values[];

    songAlbSql(Context context, String name, String[] Args, String[] Values, String dir) {
        super(context, baseF + "/" + dir + "_db/" + name, null, version);
        this.Args = Args;
        this.Values = Values;
        getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table info (_id integer primary key, name, info, fb, yt, yt_base, " +
                "linkedIn, twit, base, version integer default 1)");

        ContentValues values = new ContentValues();
        for(int i = 0; i<Args.length; i++) values.put(Args[i], Values[i]);
        db.insert("info", null, values);

        db.execSQL("create table album (_id integer primary key, title, arte, parent integer default -1" +
                ", lang integer default -1, url, url_rep_id integer default -1, lu datetime, unique(_id))");

        db.execSQL("create table places (_id integer primary key autoincrement, place, unique(place))");

        db.execSQL("create table _ (_id integer primary key autoincrement, t, unique(t))");

        db.execSQL("create table regx (_id integer primary key autoincrement, t, unique(t))");

        db.execSQL("create table alb (_id integer primary key autoincrement, title, arte, " +
                "parent integer default -1, subalbs integer default 0, audios integer default 0)");

        db.execSQL("create table lang (_id integer primary key autoincrement, lang, short)");

        String[] lang = {"Bhajans", "Hare Krishna Dhun"}, shrt = {"", "HK"};
        for (int i = 0; i<lang.length; i++){
            values = new ContentValues();
            values.put("lang", lang[i]);
            values.put("short", shrt[i]);
            db.insert("lang", null, values);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
