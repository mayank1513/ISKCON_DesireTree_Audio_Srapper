package com.mayank.krishnaapps.idt;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.mayank.krishnaapps.idt.MainActivity.baseF;

public class songAudioSql extends SQLiteOpenHelper {
    private static int version = 1;
    songAudioSql(Context context, String name, String dir) {
        super(context, baseF + "/" + dir + "_db/" + name + "_audio", null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table audio (_id integer primary key, title, parent integer, arte, url, " +
                "date datetime, place integer, size integer, size1 integer, alb default ' ', ref, unique(_id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
