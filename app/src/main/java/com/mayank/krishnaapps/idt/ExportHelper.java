package com.mayank.krishnaapps.idt;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static com.mayank.krishnaapps.idt.MainActivity.baseF;
import static com.mayank.krishnaapps.idt.MainActivity.format;
import static com.mayank.krishnaapps.idt.MainActivity.mWakeLock;
import static com.mayank.krishnaapps.idt.UpdateActivity.insertAlbum;
import static com.mayank.krishnaapps.idt.UpdateActivity.mAudioDb;
import static com.mayank.krishnaapps.idt.albSql.getAlbum;
import static com.mayank.krishnaapps.idt.audioSql.getAudio;

class ExportHelper {
    private static final ArrayList<String> albumList = new ArrayList<>();
    private static final ArrayList<String> hindi_albumList = new ArrayList<>();
    private static ExportInterface exportInterface;

    private static final StringBuilder writingErrors = new StringBuilder();

    static void setExportInterface(ExportInterface export_Interface) {
        exportInterface = export_Interface;
    }

    @SuppressLint("WakelockTimeout")
    static int Export(final String dbName) {
        if (null != exportInterface) {
            exportInterface.showProgressBar();
            exportInterface.display("Preparing Database ...");
        }
        ((new Thread(() -> {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(baseF + "/db/" + dbName, null, SQLiteDatabase.OPEN_READWRITE);
            SQLiteDatabase audioDb = SQLiteDatabase.openDatabase(baseF + "/db/" + dbName + "_audio", null, SQLiteDatabase.OPEN_READWRITE);
            db.execSQL("drop table alb");
            db.execSQL("create table alb (_id integer primary key autoincrement, title, hindi_title default '', arte, " +
                    "parent integer default -1, subalbs integer default 0, audios integer default 0)");
            refineAudioDb(audioDb, db);
            mWakeLock.acquire();
            albumList.clear();
            hindi_albumList.clear();
            audioDb.execSQL("update audio set alb = ' '");

            //                  export default albums here
            ExportDefaultAlbums(db, audioDb, dbName);

            Cursor c = db.rawQuery("select * from album", null);
            if (c.moveToFirst()) {
                ContentValues values = new ContentValues();
                values.put("version", 1);
                db.update("info", values, null, null);
                String arte = c.getString(c.getColumnIndex("arte"));
                String title = c.getString(c.getColumnIndex("title"));
                String hindi_title = c.getString(c.getColumnIndex("hindi_title"));
                ExportAlbum(db, audioDb, c.getLong(0), title, hindi_title, title.isEmpty() ? "" : arte == null ? "" : arte);
            }
            c.close();

            c = db.rawQuery("select * from alb", null);
            if (c.moveToFirst()) {
                do {
                    long id = c.getLong(0);
                    ContentValues values = new ContentValues();
                    Cursor cursor = audioDb.rawQuery("select * from audio where alb like '% " + encode(id).replaceAll("'", "''") + " %'", null);
                    values.put("audios", cursor.getCount());
                    cursor.close();
                    cursor = db.rawQuery("select * from alb where parent = ?", new String[]{id + ""});
                    values.put("subalbs", cursor.getCount());
                    cursor.close();
                    db.update("alb", values, "_id = ?", new String[]{id + ""});
                } while (c.moveToNext());
                c.close();
            }

            db.close();
            audioDb.close();
//        Writing to files
            writeToFiles(dbName);
            exportInterface.fetchWMA(0, "", dbName);
        }))).start();
        return 0;
    }

    private static void refineAudioDb(SQLiteDatabase audioDb, SQLiteDatabase db) {
        Cursor c1 = db.rawQuery("select * from lang", null);
        final int count = c1.getCount();
        c1.close();
        final int yr = Calendar.getInstance().get(Calendar.YEAR);
        Log.w("hari", yr + "");
        for (int i = 0; i < count; i++) {
            Cursor cursor = audioDb.rawQuery("select * from audio where lang = ?", new String[]{i + ""});
            if (cursor.moveToFirst()) do {
                Audio a = getAudio(cursor);
                if (a.date != null)
                    try {
                        Date tempDate = format[0].parse(a.date);
                        Calendar c = Calendar.getInstance();
                        c.setTime(tempDate);
                        if (c.get(Calendar.YEAR) < 1900 || c.get(Calendar.YEAR) > yr)
                            a.date = "";
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                if (a.title.trim().equals("BOM")) a.title = "Bombay";
                else if (a.title.length() < 5) a.title = "";
                mAudioDb = audioDb;
                UpdateActivity.mDb = db;
                a.setFromTitle(audioDb, i < 2);
                insertAlbum(null, a, true);
                exportInterface.display("Refining Db...\n" + a.title);
            } while (cursor.moveToNext());
        }
    }

    private static void ExportDefaultAlbums(SQLiteDatabase db, SQLiteDatabase audioDb, String dbName) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(new File(baseF + "/export/" + dbName + "/def_db"))));
            String s = reader.readLine();
            if (null != s && !s.isEmpty()) {
                db.execSQL("attach database '" + baseF + "/db/hari' as tempdb");
                String[] defAlbs = s.split("~");
                int bhajan = 0, bg = 1, bg_ch = 2, sb = 3, sb_ch = 4, cc = 5, cc_lila = 6, prerana = 7, chetana = 8;
                if (defAlbs[bhajan].equals("1")) {
                    int id = 1;
                    db.execSQL("insert into main.alb(title, arte, parent) select title, arte, parent from tempdb.alb where _id = ?", new String[]{id + ""});
                    albumList.add("Vaiṣṇava Bhajans");
                    hindi_albumList.add("वैष्णव भजन");
                    id = albumList.size();
                    ContentValues values = new ContentValues();
                    values.put("alb", " " + encode(id) + " ");
                    Cursor c = db.rawQuery("select * from album where lang = ?", new String[]{"0"});
                    if (c.moveToFirst()) {
                        do {
                            Cursor c1 = audioDb.rawQuery("select * from audio where parent = ?", new String[]{c.getString(0)});
                            if (c1.moveToFirst()) {
                                do {
                                    audioDb.update("audio", values, "_id = ?", new String[]{c.getString(0)});
                                    if (null != exportInterface)
                                        exportInterface.display("Preparing Database ...\n\n" + c1.getString(1));
                                } while (c1.moveToNext());
                            }
                            c1.close();
                        } while (c.moveToNext());
                    }
                    c.close();
                }
//                not else if
                if (defAlbs[bg].equals("1")) {
                    int id = 2;
                    db.execSQL("insert into main.alb(title, arte, parent) select title, arte, parent from tempdb.alb where _id = ?", new String[]{id + ""});
                    albumList.add("Bhagavad Gītā");
                    hindi_albumList.add("भगवद गीता");
                    id = albumList.size();
                    ContentValues values = new ContentValues();
                    values.put("alb", " " + encode(id) + " ");
                    Cursor c = audioDb.rawQuery("select * from audio where ref like 'bg%'", null);
                    if (c.moveToFirst()) {
                        do {
                            audioDb.update("audio", values, "_id = ?", new String[]{c.getString(0)});
                            if (null != exportInterface)
                                exportInterface.display("Preparing Database ...\n\n" + c.getString(1));
                        } while (c.moveToNext());
                    }
                    c.close();
                }
//                not else if
                if (defAlbs[sb].equals("1")) {
                    int id = 3;
                    db.execSQL("insert into main.alb(title, arte, parent) select title, arte, parent from tempdb.alb where _id = ?", new String[]{id + ""});
                    albumList.add("Śrīmad Bhāgavatam");
                    hindi_albumList.add("श्रीमद भागवत");
                    id = albumList.size();
                    ContentValues values = new ContentValues();
                    values.put("alb", " " + encode(id) + " ");
                    Cursor c = audioDb.rawQuery("select * from audio where ref like 'sb%'", null);
                    if (c.moveToFirst()) {
                        do {
                            audioDb.update("audio", values, "_id = ?", new String[]{c.getString(0)});
                            if (null != exportInterface)
                                exportInterface.display("Preparing Database ...\n\n" + c.getString(1));
                        } while (c.moveToNext());
                    }
                    c.close();
                }
//                not else if
                if (defAlbs[cc].equals("1")) {
                    int id = 4;
                    db.execSQL("insert into main.alb(title, arte, parent) select title, arte, parent from tempdb.alb where _id = ?", new String[]{id + ""});
                    albumList.add("Caitanya Caritāmṛta");
                    hindi_albumList.add("चैतन्य चरितामृत");
                    id = albumList.size();
                    ContentValues values = new ContentValues();
                    values.put("alb", " " + encode(id) + " ");
                    Cursor c = audioDb.rawQuery("select * from audio where ref like 'cc%'", null);
                    if (c.moveToFirst()) {
                        do {
                            audioDb.update("audio", values, "_id = ?", new String[]{c.getString(0)});
                            if (null != exportInterface)
                                exportInterface.display("Preparing Database ...\n\n" + c.getString(1));
                        } while (c.moveToNext());
                    }
                    c.close();
                }
//                not else if
                if (defAlbs[prerana].equals("1")) {
                    int id = 5;
                    db.execSQL("insert into main.alb(title, arte, parent) select title, arte, parent from tempdb.alb where _id = ?", new String[]{id + ""});
                    albumList.add("Preraṇa");
                    hindi_albumList.add("प्रेरणा");
                    id = albumList.size();
                    ContentValues values = new ContentValues();
                    values.put("alb", " " + encode(id) + " ");
                    Cursor c = audioDb.rawQuery("select * from audio where title like '%Preraṇa%'", null);
                    if (c.moveToFirst()) {
                        do {
                            audioDb.update("audio", values, "_id = ?", new String[]{c.getString(0)});
                            if (null != exportInterface)
                                exportInterface.display("Preparing Database ...\n\n" + c.getString(1));
                        } while (c.moveToNext());
                    }
                    c.close();
                }
//                not else if
                if (defAlbs[chetana].equals("1")) {
                    int id = 6;
                    db.execSQL("insert into main.alb(title, arte, parent) select title, arte, parent from tempdb.alb where _id = ?", new String[]{id + ""});
                    albumList.add("Chetana");
                    hindi_albumList.add("चेतना");
                    id = albumList.size();
                    ContentValues values = new ContentValues();
                    values.put("alb", " " + encode(id) + " ");
                    Cursor c = audioDb.rawQuery("select * from audio where title like '%Chetana%'", null);
                    if (c.moveToFirst()) {
                        do {
                            audioDb.update("audio", values, "_id = ?", new String[]{c.getString(0)});
                            if (null != exportInterface)
                                exportInterface.display("Preparing Database ...\n\n" + c.getString(1));
                        } while (c.moveToNext());
                    }
                    c.close();
                }
//                not else if
                if (defAlbs[bg_ch].equals("1")) {
                    for (int i = 1; i <= 18; i++) {
                        int id = i + 6;
                        db.execSQL("insert into main.alb(title, arte, parent) select title, arte, parent from tempdb.alb where _id = ?", new String[]{id + ""});
                        albumList.add("Chapter " + i);
                        hindi_albumList.add("अध्याय " + i);
                        id = albumList.size();
                        ContentValues values = new ContentValues();
                        values.put("alb", " " + encode(id) + " ");
                        Cursor c = audioDb.rawQuery("select * from audio where ref like '%bg." + i + "%'", null);
                        if (c.moveToFirst()) {
                            do {
                                audioDb.update("audio", values, "_id = ?", new String[]{c.getString(0)});
                                if (null != exportInterface)
                                    exportInterface.display("Preparing Database ...\n\n" + c.getString(1));
                            } while (c.moveToNext());
                        }
                        c.close();
                    }
                }
//                not else if
                if (defAlbs[sb_ch].equals("1")) {
                    for (int i = 1; i <= 12; i++) {
                        int id = i + 24;
                        db.execSQL("insert into main.alb(title, arte, parent) select title, arte, parent from tempdb.alb where _id = ?", new String[]{id + ""});
                        albumList.add("Canto " + i);
                        hindi_albumList.add("स्कंद " + i);
                        id = albumList.size();
                        ContentValues values = new ContentValues();
                        values.put("alb", " " + encode(id) + " ");
                        Cursor c = audioDb.rawQuery("select * from audio where ref like '%sb." + i + "%'", null);
                        if (c.moveToFirst()) {
                            do {
                                audioDb.update("audio", values, "_id = ?", new String[]{c.getString(0)});
                                if (null != exportInterface)
                                    exportInterface.display("Preparing Database ...\n\n" + c.getString(1));
                            } while (c.moveToNext());
                        }
                        c.close();
                    }
                }
//                not else if
                if (defAlbs[cc_lila].equals("1")) {
                    String[] lila = {"adi", "madhya", "antya"};
                    String[] lila1 = {"Ādī Līlā", "Madhya Līlā", "Antya Līlā"};
                    String[] hindi_lila = {"आदि लीला", "मध्य लीला", "अंत्य लीला"};
                    for (int i = 1; i <= 3; i++) {
                        int id = i + 36;
                        db.execSQL("insert into main.alb(title, arte, parent) select title, arte, parent from tempdb.alb where _id = ?", new String[]{id + ""});
                        albumList.add(lila1[i - 1]);
                        hindi_albumList.add(hindi_lila[i - 1]);
                        id = albumList.size();
                        ContentValues values = new ContentValues();
                        values.put("alb", " " + encode(id) + " ");
                        Cursor c = audioDb.rawQuery("select * from audio where ref like '%cc." + lila[i - 1] + "%'", null);
                        if (c.moveToFirst()) {
                            do {
                                audioDb.update("audio", values, "_id = ?", new String[]{c.getString(0)});
                                if (null != exportInterface)
                                    exportInterface.display("Preparing Database ...\n\n" + c.getString(1));
                            } while (c.moveToNext());
                        }
                        c.close();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    static void writeToFiles(String dbName) {
        SQLiteDatabase db = SQLiteDatabase.openDatabase(baseF + "/db/" + dbName, null, SQLiteDatabase.OPEN_READWRITE);
        SQLiteDatabase audioDb = SQLiteDatabase.openDatabase(baseF + "/db/" + dbName + "_audio", null, SQLiteDatabase.OPEN_READWRITE);
        Cursor c;
        if (exportInterface != null)
            exportInterface.display("Database prepared.\nWriting to files ...");
        c = audioDb.rawQuery("select * from audio", null);
        if (c.getColumnIndex("lang") < 0)
            audioDb.execSQL("alter table audio add column lang integer default 2");
        c.close();

        File f = new File(baseF + "/export/" + dbName);
        if (f.exists() || f.mkdirs()) {
            StringBuilder stringBuilder = new StringBuilder();
            File f1 = new File(f + "/b");
            c = db.rawQuery("select * from album", null);
            if (c.moveToFirst()) {
                do {
                    Audio a = getAlbum(c);
                    if (a.id % 500 == 1) {
                        f1 = new File(f + "/b" + (a.id / 500));
                        if (!f1.exists()) f1.mkdirs();
                    }
                    StringBuilder sb1 = new StringBuilder();
                    sb1.append(a.parent < 0 ? " " : encode(a.parent)).append("~").append(decodeUrl(a.url)).append("~")
                            .append(decodeUrl(a.date)).append("~").append(a.replacement < 0 ? "" : encode(a.replacement))
                            .append("~").append(a.lang < 0 ? "" : encode(a.lang));
                    if (exportInterface != null)
                        exportInterface.display("Database prepared.\nWriting to files ...\n\n/b" + (a.id / 500) + "/" + encode(a.id));
                    write(new File(f1, encode(a.id)), sb1);
                    stringBuilder.append("\n").append(sb1);

                    Cursor cursor = audioDb.rawQuery("select * from audio where parent = ?", new String[]{c.getString(0)});
                    if (cursor.moveToFirst()) {
                        do {
                            ContentValues values = new ContentValues();
                            values.put("lang", a.lang);
                            audioDb.update("audio", values, "_id=?", new String[]{cursor.getString(0)});
                        } while (cursor.moveToNext());
                    }
                    cursor.close();
                } while (c.moveToNext());
                write(new File(f, "b"), stringBuilder);
            }
            c.close();

            f1 = null;
            stringBuilder = new StringBuilder();
            c = audioDb.rawQuery("select * from audio", null);
            if (c.moveToFirst()) {
                do {
                    Audio a = getAudio(c);
                    if (a.id % 500 == 1) {
                        f1 = new File(f + "/a" + (a.id / 500));
                        if (!f1.exists()) f1.mkdirs();
                    }
                    StringBuilder sb1 = new StringBuilder();
                    sb1.append(encodeUrl(a.title)).append("~").append(encode(a.parent)).append("~")
                            .append(decodeUrl(a.url)).append("~").append(a.arte).append("~")
                            .append(decodeUrl(a.date)).append("~").append(a.size < 20 ? "" : encode(a.size)).append("~")
                            .append(decodeUrl(a.ref)).append("~").append(a.place < 0 ? "" : encode(a.place)).append("~")
                            .append(encodeUrl(c.getString(c.getColumnIndex("alb")).trim())).append(a.hindi_title);

                    if (exportInterface != null)
                        exportInterface.display("Database prepared.\nWriting to files ...\n\n/a" + (a.id / 500) + "/" + encode(a.id));
                    write(new File(f1, encode(a.id)), sb1);
                    stringBuilder.append("\n").append(sb1);
                } while (c.moveToNext());
                write(new File(f, "a"), stringBuilder);
            }
            c.close();

            StringBuilder stringBuilder1 = new StringBuilder();
            Cursor ccc = db.rawQuery("select * from lang", null);
            db.execSQL("create table if not exists l1 (_id integer primary key autoincrement, title, arte, " +
                    "parent integer default -1, subalbs integer default 0, audios integer default 0)");
            if (ccc.moveToFirst()) {
                int i = 0;
                do {
                    String lang = ccc.getString(1);
                    stringBuilder1.append("~").append(encodeUrl(lang))
                            .append(";").append(encodeUrl(ccc.getString(2)));

                    if (lang.toLowerCase().contains("bhajan")) lang = "Vaiṣṇava Bhajans";
                    else if (lang.toLowerCase().contains("hare kṛṣṇa")) lang = "Hare Kṛṣṇa Dhun";
                    else if (lang.toLowerCase().contains("ringtone")) lang = "Ringtone";
                    else if (lang.toLowerCase().contains("translation"))
                        lang = "Lectures with " + lang;
                    else lang = lang + " Lectures";

                    db.execSQL("drop table if exists hk" + i);
                    db.execSQL("create table if not exists hk" + i + " (_id integer primary key, title, arte, parent, " +
                            "subalbs integer default 0, audios integer default 0)");
                    Cursor cursor1 = db.rawQuery("select * from alb", null);
                    if (cursor1.moveToFirst()) {
                        do {
                            long id = cursor1.getLong(0);
                            Cursor cursor = audioDb.rawQuery("select * from audio where alb like '% " +
                                    encode(id).replaceAll("'", "''") + " %' and lang = ?", new String[]{i + ""});
                            int audios = cursor.getCount();
                            cursor.close();
                            if (audios > 0) {
                                ContentValues values = new ContentValues();
                                String[] args = new String[]{"title", "arte", "parent"/*, SUBALBS, AUDIOS*/};
                                for (String a : args)
                                    values.put(a, cursor1.getString(cursor1.getColumnIndex(a)));
                                values.put("_id", id);
                                values.put("audios", encode(audios));
                                db.insert("hk" + i, null, values);
                                if (exportInterface != null)
                                    exportInterface.display("Counting ... " + decodeUrl(cursor1.getString(1)));
                            }
                        } while (cursor1.moveToNext());
                    }
                    cursor1.close();
                    cursor1 = db.rawQuery("select * from hk" + i, null);
                    if (cursor1.moveToFirst()) {
                        do {
                            ContentValues values = new ContentValues();
                            Cursor cursor = db.rawQuery("select * from hk" + i + " where parent=?", new String[]{encode(cursor1.getLong(0))});
                            values.put("subalbs", encode(cursor.getCount()));
                            cursor.close();
                            db.update("hk" + i, values, "_id = ?", new String[]{cursor1.getString(0)});
                        } while (cursor1.moveToNext());
                    }
                    cursor1.close();

                    ContentValues values = new ContentValues();
                    values.put("_id", i);
                    values.put("title", lang);
                    values.put("parent", "");
                    cursor1 = audioDb.rawQuery("select * from audio where lang=?", new String[]{i + ""});
                    int audios = cursor1.getCount();
                    Log.d("hari", i + ", " + audios);
                    values.put("audios", audios);
                    if (cursor1.moveToFirst())
                        values.put("arte", cursor1.getString(cursor1.getColumnIndex("arte")));
                    cursor1.close();
                    cursor1 = db.rawQuery("select * from hk" + i, null);
                    int albs = cursor1.getCount();
                    values.put("subalbs", i < 2 && albs < 2 ? 0 : albs);
                    cursor1.close();
                    if (audios > 0) db.insert("l1", null, values);
                    else {
                        i++;
                        continue;
                    }

                    stringBuilder = new StringBuilder();
                    File f2 = new File(f, "k" + i);
                    f2.mkdirs();
                    f1 = new File(f2, "m0");
                    if (!f1.exists()) f1.mkdirs();
                    c = db.rawQuery("select * from hk" + i + " where parent = ?", new String[]{"-1"});
                    if (c.moveToFirst()) {
                        stringBuilder.append(encodeUrl(lang)).append("~")
                                .append(c.getString(c.getColumnIndex("arte"))).append("~")
                                .append("").append("~")
                                .append(encode(i < 2 && albs < 2 ? 0 : albs)).append("~")
                                .append(encode(audios));
                        if (albs > 1)
                            do {
                                stringBuilder.append("\n").append(encode(c.getInt(0))).append("~")
                                        .append(encodeUrl(c.getString(c.getColumnIndex("title")))).append("~")
                                        .append(c.getString(c.getColumnIndex("arte"))).append("~")
                                        .append(encode(c.getInt(c.getColumnIndex("parent")))).append("~")
                                        .append(encode(c.getInt(c.getColumnIndex("subalbs")))).append("~")
                                        .append(encode(c.getInt(c.getColumnIndex("audios"))));
                            } while (c.moveToNext());
                    }
                    c.close();
                    stringBuilder.append("\n---");
                    c = audioDb.rawQuery("select * from audio where lang = ?", new String[]{i + ""});
                    if (c.moveToFirst()) {
                        do {
                            Audio a = getAudio(c);
                            stringBuilder.append("\n").append(encode(a.id)).append("~")
                                    .append(encodeUrl(a.title)).append("~").append(encode(a.parent)).append("~")
                                    .append(decodeUrl(a.url)).append("~").append(a.arte).append("~")
                                    .append(encodeUrl(a.date)).append("~").append(a.size < 20 ? "" : encode(a.size)).append("~")
                                    .append(decodeUrl(a.ref)).append("~").append(a.place < 0 ? "" : encode(a.place)).append("~")
                                    .append(encodeUrl(c.getString(c.getColumnIndex("alb")).trim()));
                        } while (c.moveToNext());
                    }
                    c.close();
                    write(new File(f1, encode(0)), stringBuilder);

                    c = db.rawQuery("select * from hk" + i, null);
                    if (c.moveToFirst()) {
                        do {
                            int id = c.getInt(0);
                            if (id % 500 == 1) {
                                f1 = new File(f2 + "/m" + (id / 500));
                                if (!f1.exists()) f1.mkdirs();
                            }
                            StringBuilder sb1 = new StringBuilder();
                            sb1.append(encodeUrl(c.getString(c.getColumnIndex("title")))).append("~")
                                    .append(c.getString(c.getColumnIndex("arte"))).append("~")
                                    .append(encode(c.getInt(c.getColumnIndex("parent")))).append("~");
                            if (exportInterface != null)
                                exportInterface.display("Database prepared.\nWriting to files ...\n\n/m" + (id / 500) + "/" + encode(id));
                            sb1.append(encode(c.getInt(c.getColumnIndex("subalbs")))).append("~")
                                    .append(encode(c.getInt(c.getColumnIndex("audios"))));
                            Cursor c1 = db.rawQuery("select * from hk" + i + " where parent = ?", new String[]{c.getString(0)});
                            if (c1.moveToFirst()) {
                                do {
                                    sb1.append("\n").append(encode(c1.getInt(0))).append("~")
                                            .append(encodeUrl(c1.getString(c.getColumnIndex("title")))).append("~")
                                            .append(c1.getString(c.getColumnIndex("arte"))).append("~")
                                            .append(encode(c1.getInt(c.getColumnIndex("parent")))).append("~")
                                            .append(encode(c1.getInt(c.getColumnIndex("subalbs")))).append("~")
                                            .append(encode(c1.getInt(c.getColumnIndex("audios"))));
                                } while (c1.moveToNext());
                            }
                            c1.close();
                            sb1.append("\n---");

                            c1 = audioDb.rawQuery("select * from audio where alb like '% " + encode(id).replaceAll("'", "''") +
                                    " %' and lang = ?", new String[]{i + ""});
                            if (c1.moveToFirst()) {
                                do {
                                    Audio a = getAudio(c1);
                                    sb1.append("\n").append(encode(a.id)).append("~")
                                            .append(encodeUrl(a.title)).append("~").append(encode(a.parent)).append("~")
                                            .append(decodeUrl(a.url)).append("~").append(a.arte).append("~")
                                            .append(encodeUrl(a.date)).append("~").append(a.size < 20 ? "" : encode(a.size)).append("~")
                                            .append(decodeUrl(a.ref)).append("~").append(a.place < 0 ? "" : encode(a.place)).append("~")
                                            .append(encodeUrl(c1.getString(c1.getColumnIndex("alb")).trim()));
                                } while (c1.moveToNext());
                            }
                            c1.close();
                            write(new File(f1, encode(id)), sb1);
                        } while (c.moveToNext());
//                        write(new File(f, "m"), stringBuilder);
                    }
                    c.close();
//                    db.execSQL("drop table hk" + i);
                    i++;
                } while (ccc.moveToNext());
                write(new File(f, "l"), stringBuilder1);
                stringBuilder = new StringBuilder();
                c = db.rawQuery("select * from l1", null);
                if (c.moveToFirst()) {
                    stringBuilder.append("-hari-");
                    do {
                        stringBuilder.append("\n").append(encode(c.getInt(0))).append("~")
                                .append(encodeUrl(c.getString(c.getColumnIndex("title")))).append("~")
                                .append(c.getString(c.getColumnIndex("arte"))).append("~")
                                .append(encode(c.getInt(c.getColumnIndex("parent")))).append("~")
                                .append(encode(c.getInt(c.getColumnIndex("subalbs")))).append("~")
                                .append(encode(c.getInt(c.getColumnIndex("audios"))));
                        Log.d("hari 1", c.getString(c.getColumnIndex("title")) + ", " + c.getInt(c.getColumnIndex("audios")) + "");
                    } while (c.moveToNext());
                }
                c.close();
                write(new File(f, "hari"), stringBuilder.append("\n---"));
                db.execSQL("drop table l1");
            }
            ccc.close();

            stringBuilder = new StringBuilder();
            c = db.rawQuery("select * from alb where parent = ?", new String[]{"-1"});
            f1 = new File(f + "/m0");
            if (!f1.exists()) f1.mkdirs();
            if (c.moveToFirst()) {
                stringBuilder.append("-hari-");
                do {
                    stringBuilder.append("\n").append(encode(c.getInt(0))).append("~")
                            .append(encodeUrl(c.getString(c.getColumnIndex("title")))).append("~")
                            .append(c.getString(c.getColumnIndex("arte"))).append("~")
                            .append(encode(c.getInt(c.getColumnIndex("parent")))).append("~")
                            .append(encode(c.getInt(c.getColumnIndex("subalbs")))).append("~")
                            .append(encode(c.getInt(c.getColumnIndex("audios"))));
                } while (c.moveToNext());
            }
            c.close();
            write(new File(f1, encode(0)), stringBuilder.append("\n---"));

            stringBuilder = new StringBuilder();
            c = db.rawQuery("select * from alb", null);
            if (c.moveToFirst()) {
                do {
                    int id = c.getInt(0);
                    if (id % 500 == 1) {
                        f1 = new File(f + "/m" + (id / 500));
                        if (!f1.exists()) f1.mkdirs();
                    }
                    StringBuilder sb1 = new StringBuilder();
                    sb1.append(encodeUrl(c.getString(c.getColumnIndex("title")))).append("~")
                            .append(c.getString(c.getColumnIndex("arte"))).append("~")
                            .append(encode(c.getInt(c.getColumnIndex("parent")))).append("~");
                    if (exportInterface != null)
                        exportInterface.display("Database prepared.\nWriting to files ...\n\n/m" + (id / 500) + "/" + encode(id));
                    stringBuilder.append("\n").append(sb1);
                    sb1.append(encode(c.getInt(c.getColumnIndex("subalbs")))).append("~")
                            .append(encode(c.getInt(c.getColumnIndex("audios"))));
                    Cursor c1 = db.rawQuery("select * from alb where parent = ?", new String[]{c.getString(0)});
                    if (c1.moveToFirst()) {
                        do {
                            sb1.append("\n").append(encode(c1.getInt(0))).append("~")
                                    .append(encodeUrl(c1.getString(c.getColumnIndex("title")))).append("~")
                                    .append(c1.getString(c.getColumnIndex("arte"))).append("~")
                                    .append(encode(c1.getInt(c.getColumnIndex("parent")))).append("~")
                                    .append(encode(c1.getInt(c.getColumnIndex("subalbs")))).append("~")
                                    .append(encode(c1.getInt(c.getColumnIndex("audios"))));
                        } while (c1.moveToNext());
                    }
                    c1.close();
                    sb1.append("\n---");

                    c1 = audioDb.rawQuery("select * from audio where alb like '% " + encode(id).replaceAll("'", "''") + " %'", null);
                    if (c1.moveToFirst()) {
                        do {
                            Audio a = getAudio(c1);
                            sb1.append("\n").append(encode(a.id)).append("~")
                                    .append(encodeUrl(a.title)).append("~").append(encode(a.parent)).append("~")
                                    .append(decodeUrl(a.url)).append("~").append(a.arte).append("~")
                                    .append(encodeUrl(a.date)).append("~").append(a.size < 20 ? "" : encode(a.size)).append("~")
                                    .append(decodeUrl(a.ref)).append("~").append(a.place < 0 ? "" : encode(a.place)).append("~")
                                    .append(encodeUrl(c1.getString(c1.getColumnIndex("alb")).trim()));
                        } while (c1.moveToNext());
                    }
                    c1.close();
                    write(new File(f1, encode(id)), sb1);
                } while (c.moveToNext());
                write(new File(f, "m"), stringBuilder);
            }
            c.close();
            stringBuilder = new StringBuilder();
            c = db.rawQuery("select * from places", null);
            if (c.moveToFirst()) {
                do {
                    stringBuilder.append("~").append(encodeUrl(c.getString(1)));
                } while (c.moveToNext());
                write(new File(f, "p"), stringBuilder);
            }
            c.close();

            stringBuilder = new StringBuilder();
            c = db.rawQuery("select * from _", null);
            if (c.moveToFirst()) {
                do {
                    stringBuilder.append("~").append(encodeUrl(c.getString(1)));
                } while (c.moveToNext());
                write(new File(f, "h"), stringBuilder);
            }
            c.close();
            writeInfo(db, f);
        }
        c = audioDb.rawQuery("select * from audio", null);
        int audioCount = c.getCount();
        c.close();
        c = db.rawQuery("select * from album", null);
        int albCount = c.getCount();
        c.close();
        db.close();
        audioDb.close();

        writingErrors.append("audio count = ").append(audioCount).append("album count = ").append(albCount);
        write(new File(f, "errors"), writingErrors);
        if (exportInterface != null)
            exportInterface.display("Database Exported :) \n\naudio count = " + audioCount +
                    "\n\nalbum count = " + albCount + "\n\nPreparing WMA List");
        ExportConvertedFileSize(dbName);
    }

    static void writeInfo(SQLiteDatabase db, File f) {
        StringBuilder stringBuilder;
        Cursor c;
        stringBuilder = new StringBuilder();
        c = db.rawQuery("select * from info", null);
        if (c.moveToFirst()) {
            do {
                String yt = c.getString(c.getColumnIndex("yt"));
                String yt_base = c.getString(c.getColumnIndex("yt_base"));
                if (yt_base.trim().isEmpty()) yt_base = yt;
                stringBuilder.append(encodeUrl(c.getString(c.getColumnIndex("info")))).append("~")
                        .append(encodeUrl(c.getString(c.getColumnIndex("base")))).append("~")
                        .append(encodeUrl(c.getString(c.getColumnIndex("fb")))).append("~")
                        .append(encodeUrl(yt)).append("~")
                        .append(encodeUrl(yt_base)).append("~")
                        .append(encodeUrl(c.getString(c.getColumnIndex("linkedIn")))).append("~")
                        .append(encodeUrl(c.getString(c.getColumnIndex("twit")))).append("~")
                        .append(encode(new File(f, "a").length())).append("~")
                        .append(encode(new File(f, "b").length())).append("~")
                        .append(encode(new File(f, "m").length())).append("~")
                        .append(encode(new File(f, "l").length())).append("~")
                        .append(encode(new File(f, "h").length())).append("~")
                        .append(encode(new File(f, "p").length())).append("~");
            } while (c.moveToNext());
            write(new File(f, "i"), stringBuilder);
        }
        c.close();
    }

    static void ExportConvertedFileSize(final String dbName) {
        File f = new File(baseF + "/export/" + dbName + "/files");
        if (f.exists()) {
            StringBuilder stringBuilder = new StringBuilder();
            for (File f1 : f.listFiles())
                stringBuilder.append("\n").append(f1.getName().replace(".mp3", "")).append("~").append(encode(f1.length()));
            write(new File(baseF + "/export/" + dbName + "/s"), stringBuilder);
        }
    }


    static void FetchWMA(String dbName) {
        SQLiteDatabase db = SQLiteDatabase.openDatabase(baseF + "/db/" + dbName, null, SQLiteDatabase.OPEN_READWRITE);
        SQLiteDatabase audioDb = SQLiteDatabase.openDatabase(baseF + "/db/" + dbName + "_audio", null, SQLiteDatabase.OPEN_READWRITE);
        Cursor c;
        int totalSize = 0;
        StringBuilder exportList = new StringBuilder();
        c = audioDb.rawQuery("select * from audio", null);
        if (c.moveToFirst()) {
            do {
                Audio a = getAudio(c);
                String url = a.getUrl(db);
                if (url.toLowerCase().endsWith(".wma")) {
                    exportList.append(";").append(url).append("~").append(encode(a.id)).append("~").append(encode(a.size));
                    totalSize += a.size;
                }
            } while (c.moveToNext());
        }
        String s = exportList.toString();
        Log.d("WMAexportList", s);
        exportInterface.fetchWMA(totalSize, s.isEmpty() ? "empty" : s.substring(1), dbName);
    }

    private static void ExportAlbum(SQLiteDatabase db, SQLiteDatabase audioDb, long ind, String alb, String hindi_alb, String arte) {
//        update audio
        Cursor c = db.rawQuery("select * from album where parent = ?", new String[]{ind + ""});
        if (c.moveToFirst()) {
            do {
                String s = !alb.endsWith("/") ? "/" : "";
                String hs = !hindi_alb.endsWith("/") ? "/" : "";
                String title = c.getString(c.getColumnIndex("title")).trim();
                String hindi_title = c.getString(c.getColumnIndex("hindi_title")).trim();
                String arte1 = c.getString(c.getColumnIndex("arte"));
                arte1 = arte1 == null ? "" : arte1;
                ExportAlbum(db, audioDb, c.getLong(0), alb + s + title,hindi_alb + hs + hindi_title,
                        arte + s + (title.isEmpty() ? "" : arte1));
            } while (c.moveToNext());
        }
        c.close();

        if (alb.isEmpty()) return;
        if (alb.startsWith("/")) alb = alb.substring(1);
        if (arte.startsWith("/")) arte = arte.substring(1);

        String[] albums = alb.split("/");
        String[] hi_albums = hindi_alb.split("/");
        String[] artes = arte.replaceAll("/", " / ").split("/");
        StringBuilder s = new StringBuilder(" ");
        for (int i = 0; i < albums.length; i++) {
            String a = albums[i];
            if (a.isEmpty()) continue;
            int x = albumList.indexOf(a);
            if (x < 0) {
                albumList.add(a);
                hindi_albumList.add(hi_albums[i]);
                for (int j = 0; j < albumList.size(); j++) {
                    if (albumList.get(j).toLowerCase().equals(a.toLowerCase())) {
                        x = j;
                        break;
                    }
                }
                ContentValues values = new ContentValues();
                values.put("title", a.trim());
                values.put("arte", artes[i].trim());
                if (i > 0) {
                    values.put("parent", albumList.indexOf(albums[i - 1]) + 1);
                }
                db.insert("alb", null, values);
                if (exportInterface != null)
                    exportInterface.display("Preparing database...\n\n" + a);
            }
            s.append(encode(x + 1)).append(" ");
        }

        c = audioDb.rawQuery("select * from audio where parent = ?", new String[]{ind + ""});
        if (c.moveToFirst()) {
            do {
                ContentValues values = new ContentValues();
                values.put("alb", " " + c.getString(c.getColumnIndex("alb")).trim() + " " + s.toString().trim() + " ");
                audioDb.update("audio", values, "_id = ?", new String[]{c.getString(0)});
                if (exportInterface != null)
                    exportInterface.display("Preparing database...\n\n" + c.getString(1));
            } while (c.moveToNext());
        }
        c.close();
    }

    static int ExportPrerana() {
        return 0;
    }

    static int ExportChetana() {
        return 0;
    }

    static int ExportSongs(final BaseActivity activity, final String[] dbNames, final String[] names) {
        (new Thread(new Runnable() {
            @Override
            public void run() {
                mWakeLock.acquire();
                exportInterface.showProgressBar();
                exportInterface.display("Exporting songs");
                for (int j = 0; j < dbNames.length; j++) {
                    String dbName = dbNames[j];
                    SQLiteDatabase db = SQLiteDatabase.openDatabase(baseF + "/db/" + dbName, null, SQLiteDatabase.OPEN_READWRITE);
                    SQLiteDatabase audioDb = SQLiteDatabase.openDatabase(baseF + "/db/" + dbName + "_audio", null, SQLiteDatabase.OPEN_READWRITE);

                    String[] args = {"name", "info", "fb", "yt", "yt_base", "linkedIn", "twit", "base"};
                    String[] vals = new String[args.length];
                    vals[0] = names[j];
                    Cursor c = db.rawQuery("select * from info", null);
                    if (c.moveToFirst()) {
                        for (int i = 1; i < args.length; i++)
                            vals[i] = c.getString(c.getColumnIndex(args[i]));
                    } else {
                        c.close();
                        exportInterface.display("Could not copy info");
                        return;
                    }
                    c.close();

                    SQLiteDatabase sAlbDb = (new songAlbSql(activity, dbName, args, vals, "songs")).getWritableDatabase();
                    SQLiteDatabase sAudioDb = (new songAudioSql(activity, dbName, "songs")).getWritableDatabase();

                    ArrayList<String> sAlbIds = new ArrayList<>();
                    c = db.rawQuery("select * from album where lang = ? or lang = ?", new String[]{"0", "1"});
                    if (c.moveToFirst()) {
                        do {
                            args = new String[]{"_id", "title", "arte", "parent", "lang", "url", "url_rep_id", "lu"};
                            ContentValues values = new ContentValues();
                            sAlbIds.add(c.getString(0));
                            for (String a : args)
                                values.put(a, c.getString(c.getColumnIndex(a)));
                            sAlbDb.insert("album", null, values);
                        } while (c.moveToNext());
                    }
                    c.close();

//                    add songs
                    for (String s : sAlbIds) {
                        c = audioDb.rawQuery("select * from audio where parent = ?", new String[]{s});
                        if (c.moveToFirst()) {
                            do {
                                args = new String[]{"_id", "title", "parent", "arte", "url", "date", "place", "size", "size1", "alb", "ref"};
                                ContentValues values = new ContentValues();
                                for (String a : args)
                                    values.put(a, c.getString(c.getColumnIndex(a)));
                                sAudioDb.insert("audio", null, values);
                            } while (c.moveToNext());
                        }
                    }
//                    add parent albums
                    c = sAlbDb.rawQuery("select distinct parent from album", null);
                    exportInterface.display("======== " + c.getCount() + " ============");
                    while (c.moveToFirst()) {
                        int count = 0;
                        do {
                            Cursor cursor = db.rawQuery("select * from album where _id = ?",
                                    new String[]{c.getString(c.getColumnIndex("parent"))});
                            if (cursor.moveToFirst()) {
                                args = new String[]{"_id", "title", "arte", "parent", "lang", "url", "url_rep_id", "lu"};
                                ContentValues values = new ContentValues();
                                for (String arg : args)
                                    values.put(arg, cursor.getString(cursor.getColumnIndex(arg)));
                                if (sAlbDb.insertWithOnConflict("album", null, values, SQLiteDatabase.CONFLICT_IGNORE) > 0)
                                    count++;
                            }
                            cursor.close();
                        } while (c.moveToNext());
                        if (count > 0)
                            c = sAlbDb.rawQuery("select distinct parent from album", null);
                        else
                            break;
                    }
                    c.close();

                    String s[] = {"_", "places", "alb"};
                    for (String s1 : s) {
                        c = db.rawQuery("select * from " + s1, null);
                        if (c.moveToFirst()) {
                            String n[] = c.getColumnNames();
                            do {
                                ContentValues values = new ContentValues();
                                for (String n1 : n)
                                    values.put(n1, c.getString(c.getColumnIndex(n1)));
                                sAlbDb.insertWithOnConflict(s1, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                            } while (c.moveToNext());
                        }
                        c.close();
                    }
                    sAlbDb.close();
                    sAudioDb.close();
                    db.close();
                    audioDb.close();
                }
                exportInterface.hideProgressBar();
            }
        })).start();
        return 0;
    }

    private static ArrayList<String> charSet = new ArrayList<>();

    static String encode(long k) {
        if (charSet.isEmpty()) {
            getCharset();
        }
        if (k < 0) return "";
        int base = charSet.size();
        StringBuilder str = new StringBuilder();
        while (k >= base) {
            str.append(charSet.get((int) (k % base)));
            k = k / base;
        }
        str.append(charSet.get((int) (k % base)));
        return str.toString();
    }

    static long decode(String str) {
        if (str == null || str.trim().isEmpty()) return -1;
        if (charSet.isEmpty()) {
            getCharset();
        }
        long k = 0, base = charSet.size();
        for (int i = 0; i < str.length(); i++) {
            k += Math.pow(base, i) * (charSet.indexOf(str.charAt(i) + ""));
        }
        return k;
    }

    private static void getCharset() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 26; i++) {
            charSet.add((char) ('a' + i) + "");
            sb.append("'").append((char) ('a' + i)).append("', ");
        }
        for (int i = 0; i < 10; i++) {
            charSet.add(i + "");
            sb.append("'").append(i).append("', ");
        }
        charSet.add("-");
        charSet.add("'");
        sb.append("'").append("-").append("', ");
        sb.append("'").append("\\'").append("', ");
        write(new File(baseF, "char_set"), sb);
    }

    static void write(File f, StringBuilder stringBuilder) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(f);
            String s = stringBuilder.toString();
            if (s.startsWith("~") || s.startsWith("\n")) s = s.substring(1);
            fileOutputStream.write(s.getBytes());
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            writingErrors.append(e.getMessage()).append("\n");
        }
    }

    public static String encodeUrl(String str) {
        if (null == str) return "";
        boolean[] key = {true, false, false, false, true, false, true, true, false, false, true};
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0, j = 0; i < str.length(); i++, j++) {
            if (str.charAt(i) == ' ')
                stringBuilder.append(str.charAt(i));
            else if (key[j % key.length])
                stringBuilder.append((char) (str.charAt(i) - 1));
            else
                stringBuilder.append((char) (str.charAt(i) + 1));
        }
        return stringBuilder.toString();
    }

    private static String decodeUrl(String str) {
        boolean[] key = {true, false, false, false, true, false, true, true, false, false, true};
        int keyLength = key.length, strLen = null == str ? 0 : str.length();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < strLen; i++) {
            if (str.charAt(i) == ' ')
                stringBuilder.append(str.charAt(i));
            else if (key[i % keyLength])
                stringBuilder.append((char) (str.charAt(i) + 1));
            else
                stringBuilder.append((char) (str.charAt(i) - 1));
        }
        return stringBuilder.toString();
    }

    public interface ExportInterface {
        void display(String s);

        void fetchWMA(int totalSize, String expList, String dbName);

        void showProgressBar();

        void hideProgressBar();
    }

    static boolean deleteDir(File f) {
        if (f.isDirectory()) {
            for (File f1 : f.listFiles())
                deleteDir(f1);
            return f.delete();
        } else if (!f.getName().matches("(?i).*(mp3|wma|def_db)$"))
            return f.delete();
        return false;
    }
}
