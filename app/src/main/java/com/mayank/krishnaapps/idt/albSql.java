package com.mayank.krishnaapps.idt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.mayank.krishnaapps.idt.MainActivity.baseF;

public class albSql extends SQLiteOpenHelper {
    final static String PARENT_ALBUM = "parent";

    private static final int version = 1;
    private final String[] Args, Values;
    private final boolean hasDefTables;

    albSql(Context context, String name, String[] Args, String[] Values, boolean hasDefTables) {
        super(context, baseF + "/db/" + name, null, version);
        this.Args = Args;
        this.Values = Values;
        this.hasDefTables = hasDefTables;
        getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table info (_id integer primary key autoincrement, info, fb, yt, yt_base, " +
                "linkedIn, twit, base, hasDefTables, version integer default 0)");

        ContentValues values = new ContentValues();
        for (int i = 0; i < Args.length; i++) values.put(Args[i], Values[i]);
        values.put("hasDefTables", hasDefTables ? 1 : 0);
        db.insert("info", null, values);

        db.execSQL("create table album (_id integer primary key autoincrement, title, hindi_title default '~', arte, parent integer default -1" +
                ", lang integer default -1, url, url_rep_id integer default -1, lu datetime)");

        db.execSQL("create table places (_id integer primary key autoincrement, place, unique(place))");

        db.execSQL("create table _ (_id integer primary key autoincrement, t, unique(t))");

        db.execSQL("create table regx (_id integer primary key autoincrement, t, unique(t))");

        db.execSQL("create table alb (_id integer primary key autoincrement, title, hindi_title default '', arte, " +
                "parent integer default -1, subalbs integer default 0, audios integer default 0)");

        db.execSQL("create table lang (_id integer primary key autoincrement, lang, short)");

        String[] lang = {"Vaiṣṇava Bhajans", "Hare Kṛṣṇa Dhun"}, shrt = {"", "Kirtan"};
        for (int i = 0; i < lang.length; i++) {
            values = new ContentValues();
            values.put("lang", lang[i]);
            values.put("short", shrt[i]);
            db.insert("lang", null, values);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    static Audio getAlbum(Cursor c) {
        Audio album = new Audio();
        album.isAlbum = true;
        album.id = c.getInt(0);
        album.parent = c.getInt(c.getColumnIndex(PARENT_ALBUM));
        album.arte = c.getString(c.getColumnIndex("arte"));
        album.lang = c.getInt(c.getColumnIndex("lang"));
        album.title = c.getString(c.getColumnIndex("title"));
        album.hindi_title = c.getString(c.getColumnIndex("hindi_title"));
        album.url = c.getString(c.getColumnIndex("url"));
        album.date = c.getString(c.getColumnIndex("lu"));
        album.replacement = c.getInt(c.getColumnIndex("url_rep_id"));
        return album;
    }
}
