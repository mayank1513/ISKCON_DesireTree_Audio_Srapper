package com.mayank.krishnaapps.idt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.mayank.krishnaapps.idt.AlbAdapter.LyAdapter;
import static com.mayank.krishnaapps.idt.ExportHelper.ExportConvertedFileSize;
import static com.mayank.krishnaapps.idt.ExportHelper.ExportSongs;
import static com.mayank.krishnaapps.idt.ExportHelper.deleteDir;
import static com.mayank.krishnaapps.idt.ExportHelper.encode;
import static com.mayank.krishnaapps.idt.ExportHelper.encodeUrl;
import static com.mayank.krishnaapps.idt.ExportHelper.write;
import static com.mayank.krishnaapps.idt.ExportHelper.writeInfo;
import static com.mayank.krishnaapps.idt.albSql.PARENT_ALBUM;
import static com.mayank.krishnaapps.idt.baseSql.OFFICIAL_NAME;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class MainActivity extends BaseActivity {
    MainActivity mContext;
    public static File baseF;
    public static int dp10;
    public static SQLiteDatabase mDb;

    public static ArrayList<String> places;
    public static ArrayList<String> specialPlaces;
    public static ArrayList<String> Lyrics = new ArrayList<>();
    public static ArrayList<Integer> LyricsIds = new ArrayList<>();
    public static ArrayList<String> arteDes = new ArrayList<>();
    public static PowerManager.WakeLock mWakeLock;

    private Adapter adapter;
    private String def_db_name;

    int[] selectAlbIds = {R.id.bhajans, R.id.bg, R.id.bg_chapters, R.id.sb, R.id.sb_Cantos, R.id.cc, R.id.cc_lila, R.id.prerana, R.id.chetana};
    boolean[] selectedAlbs = new boolean[selectAlbIds.length];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("hk", "Created");
        mContext = this;
        Arrays.fill(selectedAlbs, false);
        baseF = getExternalFilesDir("IDT");
        mWakeLock = ((PowerManager) Objects.requireNonNull(mContext.getSystemService(Context.POWER_SERVICE))).
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
    }

    private boolean started = false;
    @Override
    protected void onStart() {
        super.onStart();
        Log.e("hk", "On Start");
        if(started) return;
        mSearch = findViewById(R.id.search);
        mSearchContainer = findViewById(R.id.searchContainer);
        dp10 = getResources().getDimensionPixelSize(R.dimen.dp10);
        compilePatterns();
        if(checkPermissions()) {
            keshav();
        }
        Log.e("hk", "Before get Intent");
        handleIntent(getIntent());
        started = true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(final Intent intent) {
        if(Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()) || Intent.ACTION_SEND.equals(intent.getAction())){
            (new AlertDialog.Builder(mContext)).setMessage("Add Images ???")
                    .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mProgressBar.setVisibility(View.VISIBLE);
                            (new saveImages(0)).execute(intent);
                        }
                    }).setNegativeButton("No", null).show();
        }
    }

    private void keshav() {
        mDb = (new baseSql(mContext)).getWritableDatabase();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                showProgressBar();
//
//                int n = new File(baseF + "/arte/x3").listFiles().length/2;
//                new File(baseF + "/arte/w").mkdirs();
//                for(int i = 0; i<n; i++){
//                    for(int x:new int[]{3, 6, 8, 12, 16}){
//                        String path = baseF + getString(R.string.arte_, x, encode(i));
//                        new File(path.replaceAll("'", ".")).renameTo(new File(path));
//                        path = baseF + getString(R.string.thumb_, x, encode(i));
//                        new File(path.replaceAll("z", ".")).renameTo(new File(path));
//                    }
//                    try {
//                        BitmapFactory.decodeFile(baseF + getString(R.string.arte_, 16, encode(i)))
//                                .compress(Bitmap.CompressFormat.PNG, 100,
//                                        new FileOutputStream(new File(baseF + "/arte/w/a_" + encode(i) + ".png")));
//                        BitmapFactory.decodeFile(baseF + getString(R.string.thumb_, 16, encode(i)))
//                                .compress(Bitmap.CompressFormat.PNG, 100,
//                                        new FileOutputStream(new File(baseF + "/arte/w/" + encode(i) + ".png")));
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    }
//                }
//                Cursor c = mDb.rawQuery("select * from alb", null);
//                if(c.moveToFirst()){
//                    do {
//                        ContentValues values = new ContentValues();
//                        String arte = c.getString(c.getColumnIndex("arte"));
//                        if(null != arte) {
//                            values.put("arte", arte.replaceAll("\\.", "'"));
//                            mDb.update("alb", values, "_id = ?", new String[]{c.getString(0)});
//                        }
//                    } while (c.moveToNext());
//                }
//                c.close();
//                hideProgressBar();
//            }
//        }).start();
        getSongs();
        getPlaces();
//        (new File(baseF + "/export/a/x3")).mkdirs();
        (new File(baseF + "/export/a/x6")).mkdirs();
        (new File(baseF + "/export/a/x8")).mkdirs();
        (new File(baseF + "/export/a/x12")).mkdirs();
        (new File(baseF + "/export/a/x16")).mkdirs();
        mDb.execSQL("create table if not exists arte (_id integer primary key, des)");
        Cursor c = mDb.rawQuery("select * from arte", null);
        if(c.moveToFirst()){
            Log.d("arteDb", "arte---");
            do{
                Log.d("arteDb", c.getString(0) + ", " + c.getString(1));
                arteDes.add(c.getString(1));
            } while (c.moveToNext());
        } else {
            int n = new File(baseF + "/export/a/x16").listFiles().length;
            for(int i = 0; i<n; i++){
                arteDes.add(" ");
                ContentValues values = new ContentValues();
                values.put("_id", i + 1);
                values.put("des", " ");
                mDb.insert("arte", null, values);
            }
        }
        c.close();
        mArteAdapter = new ArteAdapter(mContext, null);
        mGrid.setAdapter(adapter = new Adapter(mDb.rawQuery("select * from tbl_list", null)));
    }

    @Override
    public void onBackPressed() {
        findViewById(R.id.db_name).setEnabled(true);
        if(findViewById(R.id.cdb).getVisibility() == View.VISIBLE){
            findViewById(R.id.cdb).setVisibility(View.GONE);
        } else if(findViewById(R.id.def_alb).getVisibility() == View.VISIBLE){
            StringBuilder sb = new StringBuilder();
            for (boolean selectedAlb : selectedAlbs) sb.append("~").append(selectedAlb ? 1 : 0);
            new File(baseF + "/export/" + def_db_name ).mkdirs();
            write(new File(baseF + "/export/" + def_db_name + "/def_db"), sb);
            findViewById(R.id.def_alb).setVisibility(View.GONE);
        } else if(mGrid.getAdapter().getClass() == ArteAdapter.class){
            mGrid.setAdapter(adapter);
        } else if(mGrid.getAdapter().getClass() == AlbAdapter.class){
            if(albAdapter.adapterType==LyAdapter)
                mGrid.setAdapter(albAdapter = new AlbAdapter(mContext, mDb.rawQuery("select * from authors", null), AlbAdapter.LyaAdapter));
            else
                mGrid.setAdapter(adapter);
        } else
            super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Export Prerana");
        menu.add("Export Chetana");
        menu.add("Export Songs");
        menu.add("Set Default Tables' Arte");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getTitle().toString()){
            case "Export Songs":
                ExportSongs(mContext, new String[]{"rns"}, new String[]{"Radhanath Swami"});
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getSongs(){
        Lyrics.clear();
        LyricsIds.clear();
        if(!(new File(baseF, "l")).exists()) {
            (new File(baseF, "l")).mkdirs();
            createLyricsDb();
        }
        Cursor cursor;
        try{
            (cursor = mDb.rawQuery("select * from lyrics", null)).moveToFirst();
        } catch (Exception e){
            (new File(baseF, "l")).mkdirs();
            createLyricsDb();
            cursor = mDb.rawQuery("select * from lyrics", null);
        }
        if(cursor.moveToFirst()){
            do {
                Lyrics.add(cursor.getString(cursor.getColumnIndex("name")));
                LyricsIds.add(cursor.getInt(0));
                String officialName = cursor.getString(cursor.getColumnIndex(OFFICIAL_NAME));
                if(officialName != null && !officialName.isEmpty()) {
                    Lyrics.add(officialName);
                    LyricsIds.add(cursor.getInt(0));
                }
                String string = cursor.getString(cursor.getColumnIndex("matcher"));
                if(string == null) continue;
                String[] s = string.replaceAll("~", " ~ ").split("~");
                for(String s1:s){
                    if(!s1.isEmpty()) {
                        Lyrics.add(s1.trim());
                        LyricsIds.add(cursor.getInt(0));
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        writeLyricsDb();
    }

    private void writeLyricsDb(){
        StringBuilder stringBuilder = new StringBuilder();
        Cursor c = mDb.rawQuery("select * from lyrics", null);
        if(c.moveToFirst()){
            do{
                stringBuilder.append("\n").append(encodeUrl(c.getString(1).trim())).append("~")
                        .append(encodeUrl(c.getString(2))).append("~")
                        .append(encodeUrl(c.getString(3))).append("~")
                        .append(encodeUrl(c.getString(4))).append("~")
                        .append(encodeUrl(c.getString(5))).append("~")
                        .append(encodeUrl(c.getString(6)));
            } while (c.moveToNext());
        }
        c.close();
        write(new File(baseF, "ly_ind"), stringBuilder);
        stringBuilder = new StringBuilder();
        c = mDb.rawQuery("select * from authors", null);
        if(c.moveToFirst()){
            do{
                try {
                    final String url = c.getString(3);
                    stringBuilder.append("\n").append(encodeUrl(c.getString(1).replace("Songs by ", "").trim())).append("~")
                            .append(encodeUrl(c.getString(2).trim())).append("~")
                            .append(encodeUrl(url == null ? "" : url.trim()));
                } catch (Exception e) {
                    Log.e("Exp", e.getMessage());
                }
            } while (c.moveToNext());
        }
        c.close();
        write(new File(baseF, "lya_ind"), stringBuilder);
    }
    private void createLyricsDb(){
//        mDb.execSQL("drop table if exists lyrics");
//        mDb.execSQL("drop table if exists authors");

        mDb.execSQL("create table if not exists lyrics (_id integer primary key autoincrement, name text, " + PARENT_ALBUM + " integer, " +
                OFFICIAL_NAME + " text, book text, url text, arte text, matcher text default '')");
        mDb.execSQL("create table if not exists authors (_id integer primary key autoincrement, name text, arte text, url text)");
        try {
            Uri uri = Uri.parse("android.resource://" + getPackageName() + "/raw/" + "song_book");
            BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getContentResolver().openInputStream(uri))));
            String line;
            long i = -1, parentId = -1;
            File f = null;
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("*")) {
                    ContentValues values = new ContentValues();
                    values.put("name", line.substring(1));
                    parentId = mDb.insert("authors", null, values);
                } else if (line.startsWith("~")) {
                    ContentValues values = new ContentValues();
                    values.put("name", line.substring(1));
                    values.put(PARENT_ALBUM, parentId);
                    i = mDb.insert("lyrics", null, values);
                    if (f != null) {
                        FileOutputStream fileOutputStream = new FileOutputStream(f);
                        fileOutputStream.write(sb.toString().getBytes());
                        fileOutputStream.close();
                        sb = new StringBuilder();
                    }
                    f = new File(baseF + "/l/" + encode(i));
                    sb.append(line.substring(1)).append("\n");
                } else if (line.startsWith("Official Name:")) {
                    ContentValues values = new ContentValues();
                    values.put(OFFICIAL_NAME, line.replaceAll("Official Name:", "").trim());
                    mDb.update("lyrics", values, "_id = ?", new String[]{i + ""});
                    sb.append(line.substring(1)).append("\n");
                } else if (line.startsWith("Book Name:")) {
                    ContentValues values = new ContentValues();
                    values.put("book", line.replaceAll("Book Name:", "").trim());
                    mDb.update("lyrics", values, "_id = ?", new String[]{i + ""});
                    sb.append(line.substring(1)).append("\n");
                } else if (line.startsWith("u:")) {
                    ContentValues values = new ContentValues();
                    values.put("url", line.replaceAll("u:", "").trim());
                    mDb.update("lyrics", values, "_id = ?", new String[]{i + ""});
                    sb.append(line.substring(1)).append("\n");
                } else if (line.startsWith("e:")) {
                    ContentValues values = new ContentValues();
                    values.put("matcher", line.replaceAll("e:", "").trim());
                    mDb.update("lyrics", values, "_id = ?", new String[]{i + ""});
                    sb.append(line.substring(1)).append("\n");
                } else
                    sb.append(line).append("\n");
            }
            if (f != null) {
                FileOutputStream fileOutputStream = new FileOutputStream(f);
                fileOutputStream.write(sb.toString().getBytes());
                fileOutputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getPlaces() {
        File f = new File(baseF, "places.csv");
        if(!f.exists()){
            Uri uri = Uri.parse("android.resource://"+getPackageName()+"/raw/" + "places");
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getContentResolver().openInputStream(uri))));
                (new FileOutputStream(f)).write(reader.readLine().getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File f1 = new File(baseF, "special_places.csv");
        if(!f1.exists()){
            Uri uri = Uri.parse("android.resource://"+getPackageName() + "/raw/" + "special_places");
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getContentResolver().openInputStream(uri))));
                (new FileOutputStream(f1)).write(reader.readLine().getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f1)));
            specialPlaces = new ArrayList<>(Arrays.asList(reader.readLine().split(",")));
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            places = new ArrayList<>(Arrays.asList(reader.readLine().split(",")));
            Collections.sort(places, new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return o2.length() - o1.length();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writePlaces(){
        StringBuilder s = new StringBuilder();
        for(String p:places)
            s.append(",").append(p);

        File f = new File(baseF, "places.csv");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(f);
            fileOutputStream.write(s.toString().substring(1).getBytes());
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeSpecialPlaces(){
        StringBuilder s = new StringBuilder();
        for(String p:specialPlaces)
            s.append(",").append(p.trim());

        File f = new File(baseF, "special_places.csv");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(f);
            fileOutputStream.write(s.toString().substring(1).getBytes());
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class Adapter extends CursorAdapter{
        Adapter(Cursor c) {
            super(mContext, c, 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.li_dblist, parent, false);
        }

        @Override
        public void bindView(final View view, Context context, final Cursor cursor) {
            final String dbName = cursor.getString(cursor.getColumnIndex("tbl"));
            ((TextView)view.findViewById(R.id.name)).setText(dbName);
            view.findViewById(R.id.update).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(mContext, InspectActivity.class).putExtra("db_name", dbName)
                            .putExtra("skip", true).putExtra("dir", "")
                            .putExtra("hasDefTables", cursor.getInt(cursor.getColumnIndex("hasDefTables"))));
                    finish();
                }
            });
            view.findViewById(R.id.menu).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu p = new PopupMenu(mContext, v);
                    p.getMenu().add("Release Updates");
                    p.getMenu().add("Update Info");
                    p.getMenu().add("Write Info");
                    p.getMenu().add("Inspect");
                    p.getMenu().add("Test Exports in Shravanotsava App");
                    p.getMenu().add("Preview");
                    p.getMenu().add("Inspect SongsDb");
                    p.getMenu().add("Update SongsDb");
                    p.getMenu().add("Select Default Albums");
                    p.getMenu().add("Export and Write to Files");
                    p.getMenu().add("Write to Files");
                    p.getMenu().add("Download WMA");
                    p.getMenu().add("Export Converted Size");
                    p.getMenu().add("delete");
                    p.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getTitle().toString()){
                                case "Update Info":
                                    int [] ids = {R.id.info, R.id.fb, R.id.yt, R.id.yt_base, R.id.in, R.id.twit, R.id.b};
                                    final String[] args = {"info", "fb", "yt", "yt_base", "linkedIn", "twit", "base"};
                                    SQLiteDatabase database = SQLiteDatabase.openDatabase(baseF + "/db/" + dbName, null, SQLiteDatabase.OPEN_READWRITE);
                                    Cursor c = database.rawQuery("select * from info", null);
                                    if(c.moveToFirst()){
                                        for(int i = 0; i<ids.length; i++)
                                            ((EditText)findViewById(ids[i])).setText(c.getString(c.getColumnIndex(args[i])));
                                        ((Switch)findViewById(R.id.hasDefault)).setChecked(c.getInt(c.getColumnIndex("hasDefTables"))==1);
                                    }
                                    c.close();
                                    ((EditText)findViewById(R.id.db_name)).setText(dbName);
                                    findViewById(R.id.db_name).setEnabled(false);
                                    findViewById(R.id.cdb).setVisibility(View.VISIBLE);
                                    break;
                                case "Release Updates":
                                    break;
                                case "Inspect":
                                    startActivity(new Intent(mContext, InspectActivity.class).putExtra("db_name", dbName).putExtra("dir", "")
                                            .putExtra("hasDefTables", cursor.getInt(cursor.getColumnIndex("hasDefTables"))));
//                                    finish();
                                    break;
                                case "Preview":
                                    startActivity(new Intent(mContext, PreviewActivity.class).putExtra("db_name", dbName));
                                    break;
                                case "Test Exports in Shravanotsava App":
                                    testDb(dbName);
                                    break;
                                case "Inspect SongsDb":
                                    startActivity(new Intent(mContext, InspectActivity.class).putExtra("db_name", dbName).putExtra("dir", "songs_")
                                            .putExtra("hasDefTables", cursor.getInt(cursor.getColumnIndex("hasDefTables"))));
                                    break;
                                case "Select Default Albums":
                                    def_db_name = dbName;
                                    findViewById(R.id.def_alb).setVisibility(View.VISIBLE);
                                    try {
                                        BufferedReader reader = new BufferedReader(new InputStreamReader(
                                                new FileInputStream(new File(baseF + "/export/" + def_db_name + "/def_db"))));
                                        String s = reader.readLine();
                                        if(null != s && !s.isEmpty()) {
                                            String[] defAlbs = s.split("~");
                                            for(int i = 0; i<defAlbs.length; i++)
                                                ((CheckBox)findViewById(selectAlbIds[i])).setChecked(selectedAlbs[i] = defAlbs[i].equals("1"));
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                case "Write Info":
                                    SQLiteDatabase db = SQLiteDatabase.openDatabase(baseF + "/db/" + dbName, null, SQLiteDatabase.OPEN_READWRITE);
                                    writeInfo(db, new File(baseF + "/export/" + dbName));
                                    db.close();
                                    break;
                                case "Export and Write to Files":
                                    startActivity(new Intent(mContext, InspectActivity.class).putExtra("db_name", dbName)
                                            .putExtra("exp", "exp").putExtra("dir", ""));
                                    finish();
                                    break;
                                case "Write to Files":
                                    startActivity(new Intent(mContext, InspectActivity.class).putExtra("db_name", dbName)
                                            .putExtra("exp", "write").putExtra("dir", ""));
                                    break;
                                case "Download WMA":
                                    startActivity(new Intent(mContext, InspectActivity.class).putExtra("db_name", dbName)
                                            .putExtra("exp", "download_wma").putExtra("dir", ""));
                                    break;
                                case "Export Converted Size":
                                    ExportConvertedFileSize(dbName);
                                    break;
                                case "delete":
                                    (new AlertDialog.Builder(mContext)).setMessage("Delete Database?")
                                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    deleteDatabase(baseF + "/db/" + dbName + "_audio");
                                                    deleteDatabase(baseF + "/db/" + dbName);
                                                    String[] dirs = {"songs", "prerana", "chetana"};
                                                    for(String d:dirs){
                                                        deleteDatabase(baseF + "/" + d +"_db/" + dbName);
                                                        deleteDatabase(baseF + "/" + d +"_db/" + dbName + "_audio");
                                                    }
                                                    deleteDir(new File(baseF + "/export/" + dbName));
                                                    mDb.delete("tbl_list", "tbl = ?", new String[]{dbName});
                                                    Toast.makeText(mContext, "deleted " + dbName, Toast.LENGTH_SHORT).show();
                                                    adapter.swapCursor(mDb.rawQuery("select * from tbl_list", null));
                                                }
                                            }).setNeutralButton("Delete Exports", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    String[] dirs = {"songs", "prerana", "chetana"};
                                                    for(String d:dirs){
                                                        deleteDatabase(baseF + "/" + d +"_db/" + dbName);
                                                        deleteDatabase(baseF + "/" + d +"_db/" + dbName + "_audio");
                                                    }
                                                    deleteDir(new File(baseF + "/export/" + dbName));
                                                    ContentValues values = new ContentValues();
                                                    values.put("version", 0);
                                                    SQLiteDatabase db = SQLiteDatabase.openDatabase(baseF + "/db/" + dbName, null, SQLiteDatabase.OPEN_READWRITE);
                                                    db.update("info", values, null, null);
                                                    db.close();
                                                    Toast.makeText(mContext, "deleted exported files" + dbName, Toast.LENGTH_SHORT).show();
                                                }
                                            }).setNegativeButton("Cancel", null).show();
                            }
                            return false;
                        }
                    });
                    p.show();
                }
            });
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    view.findViewById(R.id.menu).performClick();
                    return true;
                }
            });
        }
    }
    static AlbAdapter albAdapter;
    public void onClick(View v){
        if(v.getId()==R.id.yt_base_label){
            startActivity(new Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse("https://youtube.com/"+((EditText)findViewById(R.id.yt_base)).getText().toString())));
        } else if(v.getId()==R.id.yt_label){
            startActivity(new Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse("https://youtube.com/"+((EditText)findViewById(R.id.yt)).getText().toString())));
        } else if(v.getId() == R.id.add){
            if(mGrid.getAdapter().getClass() == ArteAdapter.class){
                Intent intent = new Intent();
                intent.setType("image/*").putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true).setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Images"), RESULT_LOAD_IMAGE);
            } else {
                PopupMenu p = new PopupMenu(mContext, v);
                p.getMenu().add("New Database");
                p.getMenu().add("Add Arte");
                p.getMenu().add("Set Default Albums' Arte");
                p.getMenu().add("Set Lyrics' Arte");
                p.getMenu().add("Download SP Quotes");
                p.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getTitle().toString()) {
                            case "New Database":
                                findViewById(R.id.cdb).setVisibility(View.VISIBLE);
                                break;
                            case "Add Arte":
                                mGrid.setAdapter(mArteAdapter);
                                break;
                            case "Set Default Albums' Arte":
                                mGrid.setAdapter(albAdapter = new AlbAdapter(mContext, mDb.rawQuery("select * from alb", null), AlbAdapter.AlbAdapter));
                                break;
                            case "Set Lyrics' Arte":
                                mGrid.setAdapter(albAdapter = new AlbAdapter(mContext, mDb.rawQuery("select * from authors", null), AlbAdapter.LyaAdapter));
                                break;
                            case "Download SP Quotes":
                                Toast.makeText(mContext, "Already Fetched", Toast.LENGTH_SHORT).show();
//                                new Thread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        fetchSrilaPrabhupadaQuotes();
//                                    }
//                                }).start();
                        }
                        return false;
                    }
                });
                p.show();
            }
        } else if(v.getId() == R.id.createDB){
            int [] ids = {R.id.info, R.id.fb, R.id.yt, R.id.yt_base, R.id.in, R.id.twit, R.id.b};
            final String[] args = {"info", "fb", "yt", "yt_base", "linkedIn", "twit", "base"};
            final String[] base = {"", "https://www.facebook.com/", "https://www.youtube.com/", "https://www.youtube.com/",
                                        "https://www.linkedin.com/", "https://twitter.com/", ""};
            final String[] vals = new String[ids.length];

            for(int i = 0; i<ids.length; i++){
                vals[i] = ((EditText)findViewById(ids[i])).getText().toString().replace(base[i], "");
                if(vals[i].isEmpty())
                    vals[i] = ((EditText)findViewById(ids[i])).getHint().toString();
            }
            final boolean removeParent = ((Switch)findViewById(R.id.hasDefault)).isChecked();

            if(!vals[6].startsWith("https://audio.iskcondesiretree.com/")) {
                (new AlertDialog.Builder(this)).setMessage("Invalid Base Url.").show();
                return;
            }
            String name = ((EditText)findViewById(R.id.db_name)).getText().toString();
            if(name.isEmpty())
                name = ((EditText)findViewById(R.id.db_name)).getHint().toString();

            name = name.toLowerCase().replaceAll(" ", "_");
            Cursor cursor = mDb.rawQuery("Select * from tbl_list where tbl = ?", new String[]{name});
            if(cursor.moveToFirst()){
                final String finalName = name;
                (new AlertDialog.Builder(this))
                        .setMessage("Database Already Exists.\nUpdate Database Info?")
                        .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SQLiteDatabase database = SQLiteDatabase.openDatabase(baseF + "/db/" + finalName, null, SQLiteDatabase.OPEN_READWRITE);
                                ContentValues values = new ContentValues();
                                for(int i = 0; i<args.length; i++) values.put(args[i], vals[i]);
                                values.put("hasDefTables", removeParent?1:0);
                                database.update("info", values, null, null);
                                values = new ContentValues();
                                values.put("hasDefTables", removeParent?1:0);
                                mDb.update("tbl_list", values, "tbl = ?", new String[]{finalName});
                            }
                        }).show();
                cursor.close();
                onBackPressed();
                return;
            } else {
                ContentValues values = new ContentValues();
                values.put("tbl", name);
                values.put("hasDefTables", removeParent ? 1 : 0);
                mDb.insert("tbl_list", null, values);
                adapter.swapCursor(mDb.rawQuery("SELECT  * FROM tbl_list", null));
                values = new ContentValues();
                values.put("url", vals[6]);
                (new albSql(this, name, args, vals, removeParent)).getWritableDatabase()
                        .insert("album", null, values);
                (new audioSql(mContext, name)).getWritableDatabase().close();
                onBackPressed();
            }
            cursor.close();
        } else {
            for(int i = 0; i<selectedAlbs.length; i++) {
                if(v.getId() == selectAlbIds[i]) {
                    ((CheckBox) v).setChecked(selectedAlbs[i] = !selectedAlbs[i]);
                    if(i == 1 && !selectedAlbs[i])
                        ((CheckBox)findViewById(selectAlbIds[i + 1])).setChecked(selectedAlbs[i+1] = false);
                    else if(i == 2 && selectedAlbs[i])
                        ((CheckBox)findViewById(selectAlbIds[i - 1])).setChecked(selectedAlbs[i-1] = true);
                    else if(i == 3 && !selectedAlbs[i])
                        ((CheckBox)findViewById(selectAlbIds[i + 1])).setChecked(selectedAlbs[i+1] = false);
                    else if(i == 4 && selectedAlbs[i])
                        ((CheckBox)findViewById(selectAlbIds[i - 1])).setChecked(selectedAlbs[i-1] = true);
                    break;
                }
            }

            if(v.getId() == R.id.alb_selected){
                onBackPressed();
            }
        }
    }

    static final String ch = "( reading)?[\\s\\-](chapter|ch)?[\\s\\-]?\\d{1,2}",
            txt = "([\\s\\-]|[\\s\\-]?(text|vers(e)?)[\\s\\-]?)\\d{1,2}",
            cc_txt = "([\\s\\-]|[\\s\\-]?text[\\s\\-]?)\\d{1,3}",
            to = "([\\s\\-]?to[\\s\\-]?|[\\s\\-])",
            bg = "(bg|(bhagavad)?(-|\\s)gītā|^gītā )",
            canto = "[\\s\\-](canto)?[\\s\\-]?\\d{1,2}",
            sb = "(sb|(śrīmad )?bhāgavatam)",
            cc = "(cc|(śrī )?caitanya caritāmṛta)",
            lila = "[\\s\\-]?(((ādī|antya|madhya)([\\s\\-]?līlā)?)|al|ml)[\\s\\-]?",
            nod = "(nod|nectar of devotion)",
            noi = "(noi|nectar of instruction(s)?)",
            mantra = "([\\s\\-]|[\\s\\-]?(text|mantra)[\\s\\-]?)\\d{1,2}",
            iso = "((śrī )?īśopaniṣad(a)?|iso)",
            kb = "(kṛṣṇa book( dict)?|kb)";
    public static final String[] bookParsers = {
//            Bhagavad Gita
            bg + ch + txt + to + bg + "?" + ch + txt,
            bg + ch + txt + to + txt,
            bg + ch + txt,
            bg + ch,
            bg,
//            Srimad Bhagavatam
            sb + canto + ch + txt + to + sb + "?" + canto + ch + txt,
            sb + canto + ch + txt + to + ch + txt,
            sb + canto + ch + txt + to + txt,
            sb + canto + ch + txt,
            sb + canto + ch,
            sb + canto,
            sb,
//            cc
            cc + lila + ch + cc_txt + to + cc + "?" + lila + "?" + ch + cc_txt,
            cc + lila + ch + cc_txt + to + ch + cc_txt,
            cc + lila + ch + cc_txt + to + cc_txt,
            cc + lila + ch + cc_txt,
            cc + lila + ch,
            cc + lila,
            cc,
//            nod
            nod + ch + to + ch,
            nod + ch,
            nod,
//            noi
            noi + txt + to + txt,
            noi + txt,
            noi,
//            iso
            iso + mantra + to + mantra,
            iso + mantra,
            iso,
//            kb
            kb + ch,
            kb
    };

    static Pattern linkPattern, dPattern;
    public static final String[] dateFormats = new String[]{"yyyy-MM-dd", "dd-MM-yyyy", "yy-MM-dd", "MM-yyyy", "yyyy-MM", "yyyy", "dd-MM-yy"};
    public static final String[] dateParser = new String[]{"\\d{4}-\\d{1,2}-\\d{1,2}", "\\d{1,2}-\\d{1,2}-\\d{4}",
            "\\d\\d-\\d{1,2}-\\d{1,2}", "\\d{1,2}-\\d{4}", "\\d{4}-\\d{1,2}", "\\d{4}", "\\d\\d-\\d{1,2}-\\d{1,2}"};

    public static SimpleDateFormat[] format = new SimpleDateFormat[dateFormats.length];
    public static Pattern[] datePattern = new Pattern[dateFormats.length];
    public static Pattern[] bookPattern = new Pattern[bookParsers.length];

    @SuppressLint("SimpleDateFormat")
    private void compilePatterns(){
        linkPattern = Pattern.compile("<a\\s+(?:[^>]*?\\s+)?href=([\"'])(.*?)\\1",  Pattern.CASE_INSENSITIVE| Pattern.DOTALL);
        dPattern = Pattern.compile("\\d\\d\\d\\d-\\d\\d-\\d\\d \\d\\d:\\d\\d");
        for(int i = 0; i<dateFormats.length; i++) {
            format[i] = new SimpleDateFormat(dateFormats[i]);
            format[i].setLenient(false);
            datePattern[i] = Pattern.compile(dateParser[i], Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
        }
        for (int i = 0; i< bookParsers.length; i++) {
            bookPattern[i] = Pattern.compile(bookParsers[i], Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
        }
    }

    public boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            return false;
        } else return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(checkPermissions())
            keshav();
        else
            finish();
    }

    static File spQuoteDir;
//    public static void fetchSrilaPrabhupadaQuotes(){
//        spQuoteDir = new File(baseF + "/export/sp/q");
//        spQuoteDir.mkdirs();
//        Calendar calendar = Calendar.getInstance();
//        for(int i = 0; i< 366; i++) {
//            fetchSrilaPrabhupadaQuotes(calendar);
//            calendar.add(Calendar.DAY_OF_YEAR, 1);
//        }
//    }
//    final static String spQuoteBaseUrl = "http://harekrishnacalendar.com/wp-content/uploads/2012/09/Srila-Prabhupada-Quotes-For-Month-";
//    public static void fetchSrilaPrabhupadaQuotes(Calendar calendar){
//        URL url = null;
//        String date = android.text.format.DateFormat.format("MMMM-dd", calendar.getTime()).toString();
//        final File file = new File(spQuoteDir, date.replace('-', '_') + ".png");
//        if (!file.exists()) {
//            try {
//                url = new URL(spQuoteBaseUrl + date + ".png");
//                new BufferedInputStream(url.openStream());
//            } catch (IOException e) {
//                //e.printStackTrace();
//                try {
//                    date = date.replace("-", "");
//                    url = new URL(spQuoteBaseUrl + date + ".png");
//                } catch (IOException e1) {
//                    e1.printStackTrace();
//                }
//            }
//            if (url != null) new DownloadTask(url, file, new DownloadTask.PostExecuteListener() {
//                @Override
//                public void postExecute() {
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            try {
//                                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
//                                if(bitmap!=null)
//                                    bitmap.compress(Bitmap.CompressFormat.WEBP,
//                                        100, new FileOutputStream(new File(file.getAbsolutePath().replaceAll("\\.png", ""))));
//                            } catch (FileNotFoundException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }).start();
//                }
//            }).execute("");
//        }
//    }

    static int runningDownloadTasks = 0;
    static class DownloadTask extends AsyncTask<String, String, String> {
        URL mUrl;
        File mFile;
        PostExecuteListener mPost;
        DownloadTask(URL url, File file, PostExecuteListener postExecuteListener){
            mUrl = url;
            mFile = file;
            mPost = postExecuteListener;
        }

        @SuppressLint("WakelockTimeout")
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mWakeLock.acquire();
            runningDownloadTasks ++;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            runningDownloadTasks --;
            if(mPost!=null){
                mPost.postExecute();
            }
            if(mWakeLock.isHeld() && runningDownloadTasks<=0) mWakeLock.release();
        }

        @Override
        protected String doInBackground(String... strings) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            if(mFile.exists()) return null;
            try {
                connection = (HttpURLConnection) mUrl.openConnection();
                connection.connect();
                input = connection.getInputStream();
                output = new FileOutputStream(mFile);

                byte[] data = new byte[4098];
                int count;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null) output.close();
                    if (input != null) input.close();
                } catch (IOException ignored) {}

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        interface PostExecuteListener {
            void postExecute();
        }
    }
}
