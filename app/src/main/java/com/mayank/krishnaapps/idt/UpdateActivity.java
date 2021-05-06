package com.mayank.krishnaapps.idt;
/*
 * Opened recursively to update database
 * */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.PopupMenu;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.regex.Matcher;

import static com.mayank.krishnaapps.idt.Audio.findStem;
import static com.mayank.krishnaapps.idt.InspectActivity.languages;
import static com.mayank.krishnaapps.idt.InspectActivity.optimizeStem;
import static com.mayank.krishnaapps.idt.InspectActivity.regXTORemoveFromAll;
import static com.mayank.krishnaapps.idt.InspectActivity.removeParent;
import static com.mayank.krishnaapps.idt.InspectActivity.stems;
import static com.mayank.krishnaapps.idt.MainActivity.Lyrics;
import static com.mayank.krishnaapps.idt.MainActivity.baseF;
import static com.mayank.krishnaapps.idt.MainActivity.dPattern;
import static com.mayank.krishnaapps.idt.MainActivity.linkPattern;
import static com.mayank.krishnaapps.idt.MainActivity.places;
import static com.mayank.krishnaapps.idt.MainActivity.writePlaces;
import static com.mayank.krishnaapps.idt.albSql.PARENT_ALBUM;
import static com.mayank.krishnaapps.idt.albSql.getAlbum;
import static com.mayank.krishnaapps.idt.audioSql.getAudio;

public class UpdateActivity extends BaseActivity {
    UpdateActivity mContext;
    AudioAdapter mAudioAdapter;
    static SQLiteDatabase mAudioDb;
    String dbName, dir;
    Audio mAlbum;
    ArrayList<Audio> mAudioList = new ArrayList<>();
    public ArrayList<String> temp;

    private PowerManager.WakeLock mWakeLock;
    boolean editMode = false;

    private boolean started = false;

    @Override
    protected void onStart() {
        super.onStart();
        MainActivity.baseF = getExternalFilesDir("IDT");
        if (started) return;
        mContext = this;
        dbName = getIntent().getStringExtra("db_name");
        dir = Objects.requireNonNull(getIntent().getExtras()).getString("dir");
        String path = baseF + "/" + dir + "db/" + dbName;
        if (mAudioDb != null && mAudioDb.isOpen()) {
            mDb.close();
            mAudioDb.close();
        }
        mDb = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE);
        mAudioDb = SQLiteDatabase.openDatabase(path + "_audio", null, SQLiteDatabase.OPEN_READWRITE);
        Cursor c = mDb.rawQuery("select * from album where _id = ?", new String[]{getIntent().getLongExtra("album_id", 1) + ""});
        if (c.moveToFirst())
            mAlbum = getAlbum(c);
        else
            finish();
        c.close();
        mWakeLock = ((PowerManager) Objects.requireNonNull(getSystemService(Context.POWER_SERVICE))).
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        mAudioList.clear();
        addFromDb(mAudioList, mAlbum);
        mGrid.setAdapter(mAudioAdapter = new AudioAdapter(mContext));
        if (mAudioList.size() < 2) (new FetchTask(mAudioList, false)).execute(mAlbum);
        else shortenUrls(mAudioList, mAlbum, false);
        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                temp = new ArrayList<>();
                for (String l : Lyrics) {
                    if (l.toLowerCase().contains(s.toString().toLowerCase().trim()))
                        temp.add(l);
                }
                mGrid.setAdapter(new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, temp));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        mSearch = findViewById(R.id.search);
        mSearchContainer = findViewById(R.id.searchContainer);
        mSearch.addTextChangedListener(textWatcher);
        started = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Update");
        menu.add("Add from file");
        menu.add("Go to next empty ref");
        menu.add("Rebuild");
        menu.add("Rebuild from Url");
        menu.add("Add Language");
        menu.add("Add Place");
        menu.add("Add RegX to remove from All");
        menu.add("Shorten Url");
        menu.add("Replace from All");
        menu.add("Add All");
        return super.onCreateOptionsMenu(menu);
    }

    int emptyRef = 0;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int size = mAudioList.size();
        switch (item.getTitle().toString()) {
            case "Update":
                (new FetchTask(mAudioList, false)).execute(mAlbum);
                break;
            case "Add from file":
//                if (mAudioList.size() < 2) (new FetchTask(mAudioList, false)).execute(mAlbum);
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("text/plain");
                startActivityForResult(intent, PICKFILE_RESULT_CODE);
                break;
            case "Go to next empty ref":
                int i = emptyRef + 1;
                for (; i < mAudioList.size(); i++) {
                    if (null == mAudioList.get(i).ref || mAudioList.get(i).ref.isEmpty()) {
                        mGrid.smoothScrollToPosition(emptyRef = i);
                        Toast.makeText(mContext, i + "", Toast.LENGTH_LONG).show();
                        break;
                    }
                }
                if (i == mAudioList.size()) emptyRef = 0;
                break;
            case "Rebuild":
                (new Thread(() -> {
                    showProgressBar();
                    for (int i1 = 1; i1 < size; i1++) {
                        Audio a = mAudioList.get(i1);
                        Log.w("hari", "Up: " + i1);
                        a.setFromTitle(mDb, mAlbum.lang < 2);
                        Log.w("hari", "Up: " + a.title);
                        insertAlbum(mContext, a, true);
                        display(i1 + " of " + size + " ...: " + a.title);
                    }
                    runOnUiThread(() -> {
                        mAudioAdapter.notifyDataSetChanged();
                        mProgressBar.setVisibility(View.GONE);
//                                mStatusText.setVisibility(View.GONE);
                    });
                })).start();
                break;
            case "Rebuild from Url":
                new AlertDialog.Builder(mContext).setMessage("Remove parent title from all?")
                        .setPositiveButton("Remove from All", (dialog, which) -> rebuildFromUrl(size, true)).setNeutralButton("Don't remove", (dialog, which) -> rebuildFromUrl(size, false)).setNegativeButton("Cancel", null).show();
                break;
            case "Add Language":
                addLang();
                break;
            case "Add Place":
                addPlace();
                break;
            case "Add RegX to remove from All":
                addRegX();
                break;
            case "Add All":
                addAllWithPrompt();
                break;
            case "Shorten Url":
                shortenUrl(findViewById(R.id.add));
                break;
            case "Replace from All":
                replaceFromAll();
                break;
        }
        return false;
    }

    private void rebuildFromUrl(final int size, final boolean removeParent) {
        (new Thread(() -> {
            showProgressBar();
            for (int i = 1; i < size; i++) {
                Audio a = mAudioList.get(i);
                a.setFromUrl(mDb, mAlbum.lang < 2, mAlbum, removeParent);
                insertAlbum(mContext, a, true);
                display(i + " of " + size + " ...: " + a.title);
            }
            runOnUiThread(() -> {
                mAudioAdapter.notifyDataSetChanged();
                mProgressBar.setVisibility(View.GONE);
                mStatusText.setVisibility(View.GONE);
            });
        })).start();
    }

    private void addRegX() {
        final EditText regX = new EditText(mContext);
        (new AlertDialog.Builder(mContext)).setTitle("RegX to remove from All")
                .setPositiveButton("Add", (dialog, which) -> {
                    String rX = regX.getText().toString();
                    if (!rX.trim().isEmpty() && !regXTORemoveFromAll.contains(rX)) {
                        regXTORemoveFromAll.add(rX);
                        Collections.sort(regXTORemoveFromAll, (o1, o2) -> o2.length() - o1.length());
                        ContentValues values = new ContentValues();
                        values.put("t", rX);
                        mDb.insert("regx", null, values);
                        Toast.makeText(mContext, rX + " added", Toast.LENGTH_SHORT).show();
                    } else
                        Toast.makeText(mContext, "Already Exists", Toast.LENGTH_SHORT).show();
                }).setNegativeButton("Cancel", null).setView(regX).show();
    }

    private void addLang() {
        final LinearLayout layout = new LinearLayout(mContext);
        layout.setGravity(Gravity.CENTER);
        layout.addView(new EditText(mContext));
        layout.addView(new EditText(mContext));
        (new AlertDialog.Builder(mContext)).setTitle("Add Language").setView(layout)
                .setPositiveButton("Add", (dialog, which) -> {
                    String l = ((EditText) layout.getChildAt(0)).getText().toString();
                    String short_l = ((EditText) layout.getChildAt(1)).getText().toString();
                    Cursor cursor = mDb.rawQuery("Select * from lang where lang = ? OR short = ?", new String[]{l, short_l});
                    if (cursor.getCount() > 0 || l.isEmpty() || short_l.isEmpty()) {
                        Toast.makeText(mContext, "Language Already Exists", Toast.LENGTH_LONG).show();
                    } else {
                        ContentValues values = new ContentValues();
                        values.put("lang", l);
                        values.put("short", short_l);
                        mDb.insert("lang", null, values);
                        languages.add(l);
                    }
                    cursor.close();
                }).setNegativeButton("Cancel", null).show();
    }

    @Override
    public void onBackPressed() {
        Class<? extends ListAdapter> adapterClass = mGrid.getAdapter().getClass();
        if (adapterClass == ArteAdapter.class || adapterClass == ArrayAdapter.class) {
            mGrid.setAdapter(mAudioAdapter);
            mSearch.setVisibility(View.GONE);
        } else
            super.onBackPressed();
    }

    private void addFromDb(ArrayList<Audio> audioList, Audio album) {
        Cursor c;
        audioList.add(album);
        c = mDb.rawQuery("select * from album where " + PARENT_ALBUM + " = ?", new String[]{album.id + ""});
        if (c.moveToFirst()) {
            do {
                audioList.add(getAlbum(c));
            } while (c.moveToNext());
        }
        c.close();
        c = mAudioDb.rawQuery("select * from audio where " + PARENT_ALBUM + " = ?", new String[]{album.id + ""});
        if (c.moveToFirst()) {
            do {
                audioList.add(getAudio(c));
            } while (c.moveToNext());
        }
        c.close();
    }

    int runningTasks = 0, updates = 0, newEntries = 0;

    @SuppressLint("StaticFieldLeak")
    public class FetchTask extends AsyncTask<Audio, String, String> {
        Audio mAlbum;
        ArrayList<Audio> mAudioList;
        boolean mFetchAll;

        FetchTask(ArrayList<Audio> mAudioList, boolean mFetchAll) {
            this.mAudioList = mAudioList;
            this.mFetchAll = mFetchAll;
        }

        @SuppressLint("WakelockTimeout")
        @Override
        protected void onPreExecute() {
            mWakeLock.acquire();
            mProgressBar.setVisibility(View.VISIBLE);
            runningTasks++;
        }

        @Override
        protected String doInBackground(Audio... albums) {
            try {
                final String url;
                mAlbum = albums[0];
                url = mAlbum.getUrl(mDb);
                URLConnection urlConnection = new URL(url).openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line;

                //noinspection StatementWithEmptyBody
                while ((line = reader.readLine()) != null && !Html.fromHtml(line).toString().contains("Parent Directory"))
                    ;

                while ((line = reader.readLine()) != null) {
                    Matcher m = linkPattern.matcher(line);
                    if (!m.find()) break;
                    String u = m.group().replace("<a href=\"", "").replace("\"", "");
                    line = Html.fromHtml(line).toString();
                    m = dPattern.matcher(line);
                    if (!m.find()) break;
                    String lu = m.group();
                    String size = line.substring(line.indexOf(lu)).replace(lu, "");
                    update(u, lu, size);
//                    publishProgress(link.url, link.lu, link.size);
                }
            } catch (final IOException e) {
                e.printStackTrace();
                display(e.getMessage());
                runOnUiThread(() -> Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_LONG).show());
            }
            return null;
        }

        private void update(String url, String lu, String size) {
            for (Audio a : mAudioList) {
                if (a.url.replaceAll("#", mAlbum.replacement > 0 ? stems.get(mAlbum.replacement - 1) : "")
                        .equals(url.replaceAll("/", "").trim())) {
                    if (a.isAlbum && !a.date.equals(lu.trim())) {
                        a.newLu = lu.trim();
                        updates++;
                    }
                    if (optimizeStem)
                        a.url = url.replaceAll("/", "").trim();
                    if (!a.isAlbum && a.size == 0) {
                        String s = size.replaceAll("(?i)([Mk])", "").trim();
                        String s1 = s.replaceAll("\\s+", "");
                        float sz = Float.parseFloat(s1);
                        if (size.toLowerCase().contains("m"))
                            a.size = (long) (sz * 1024 * 1024);
                        else if (s.toLowerCase().contains("k"))
                            a.size = (long) (sz * 1024);
                        else
                            a.size = (long) sz;
                    }
                    return;
                }
            }
            Audio audio = new Audio();
            audio.parent = mAlbum.id;
            audio.arte = mAlbum.arte;
            audio.url = url.trim();
            if (url.toLowerCase().endsWith(".mp3") || url.toLowerCase().endsWith(".wav") || url.toLowerCase().endsWith(".wma")) {
                String s = size.replaceAll("M", "").replaceAll("(?i)k", "").trim();
                String s1 = s.replaceAll("\\s", "");
                float sz = Float.parseFloat(s1);
                if (size.toLowerCase().contains("m"))
                    audio.size = (long) (sz * 1024 * 1024);
                else if (s.toLowerCase().contains("k"))
                    audio.size = (long) (sz * 1024);
                else
                    audio.size = (long) sz;
                audio.setFromUrl(mDb, mAlbum.lang < 2, mAlbum, removeParent);
            } else if (url.endsWith("/") || !url.contains(".")) {
                audio.url = audio.url.replaceAll("/", "");
                audio.isAlbum = true;
                audio.lang = mAlbum.lang;
                audio.title = Html.fromHtml(audio.url.replaceAll("_", " ").trim()).toString();
                audio.hari();
                audio.date = lu;
                Cursor cursor = mDb.rawQuery("select * from album where " +
                        PARENT_ALBUM + " = ? AND url = ?", new String[]{audio.parent + "", audio.url});
                if (cursor.moveToFirst()) {
                    audio.id = cursor.getInt(0);
                    audio.newLu = audio.date;
                    audio.date = cursor.getString(cursor.getColumnIndex("lu"));
                    cursor.close();
                }
            } else
                return;

            if (mAlbum.replacement > 0)
                audio.url = audio.url.replaceAll(stems.get(mAlbum.replacement - 1), "#");

            if (optimizeStem)
                audio.url = url.replaceAll("/", "").trim();
            mAudioList.add(audio);
            newEntries++;
            display("Updates: " + updates + " New: " + newEntries + "\n" + audio.title);
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String s) {
            display("Updates: " + updates + " New: " + newEntries);
//            shorten url
            shortenUrls(mAudioList, mAlbum, mFetchAll);
            mGrid.setAdapter(mAudioAdapter = new AudioAdapter(mContext));
            runningTasks--;
            if (runningTasks <= 0) {
                findViewById(R.id.progress).setVisibility(View.GONE);
                try {
                    mWakeLock.release();
                } catch (Exception e) {
                    Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    public class FetchFromFileTask extends AsyncTask<Uri, String, String> {
        @SuppressLint("WakelockTimeout")
        @Override
        protected void onPreExecute() {
            mWakeLock.acquire();
            mProgressBar.setVisibility(View.VISIBLE);
            runningTasks++;
        }

        @Override
        protected String doInBackground(Uri... uris) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uris[0])));
                String line;

                while ((line = reader.readLine()) != null) {
                    update(line.trim().replaceAll(" ", "_") + (line.trim().endsWith("/") ? "" : "/"), "1900-04-13", "");
//                    publishProgress(link.url, link.lu, link.size);
                }
            } catch (final IOException e) {
                e.printStackTrace();
                display(e.getMessage());
                runOnUiThread(() -> Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_LONG).show());
            }
            return null;
        }

        private void update(String url, String lu, String size) {
            for (Audio a : mAudioList) {
                if (a.url.replaceAll("#", mAlbum.replacement > 0 ? stems.get(mAlbum.replacement - 1) : "")
                        .equals(url.replaceAll("/", "").trim())) {
                    if (a.isAlbum && !a.date.equals(lu.trim())) {
                        a.newLu = lu.trim();
                        updates++;
                    }
                    if (optimizeStem)
                        a.url = url.replaceAll("/", "").trim();
                    if (!a.isAlbum && a.size == 0) {
                        String s = size.replaceAll("(?i)([Mk])", "").trim();
                        String s1 = s.replaceAll("\\s+", "");
                        float sz = Float.parseFloat(s1);
                        if (size.toLowerCase().contains("m"))
                            a.size = (long) (sz * 1024 * 1024);
                        else if (s.toLowerCase().contains("k"))
                            a.size = (long) (sz * 1024);
                        else
                            a.size = (long) sz;
                    }
                    return;
                }
            }
            Audio audio = new Audio();
            audio.parent = mAlbum.id;
            audio.arte = mAlbum.arte;
            audio.url = url.trim();
            if (url.toLowerCase().endsWith(".mp3") || url.toLowerCase().endsWith(".wav") || url.toLowerCase().endsWith(".wma")) {
                String s = size.replaceAll("M", "").replaceAll("(?i)k", "").trim();
                String s1 = s.replaceAll("\\s", "");
                float sz = Float.parseFloat(s1);
                if (size.toLowerCase().contains("m"))
                    audio.size = (long) (sz * 1024 * 1024);
                else if (s.toLowerCase().contains("k"))
                    audio.size = (long) (sz * 1024);
                else
                    audio.size = (long) sz;
                audio.setFromUrl(mDb, mAlbum.lang < 2, mAlbum, removeParent);
            } else if (url.endsWith("/") || !url.contains(".")) {
                audio.url = audio.url.replaceAll("/", "");
                audio.isAlbum = true;
                audio.lang = mAlbum.lang;
                audio.title = Html.fromHtml(audio.url.replaceAll("_", " ").trim()).toString();
                audio.hari();
                audio.date = lu;
                Cursor cursor = mDb.rawQuery("select * from album where " +
                        PARENT_ALBUM + " = ? AND url = ?", new String[]{audio.parent + "", audio.url});
                if (cursor.moveToFirst()) {
                    audio.id = cursor.getInt(0);
                    audio.newLu = audio.date;
                    audio.date = cursor.getString(cursor.getColumnIndex("lu"));
                    cursor.close();
                }
            } else
                return;

            if (mAlbum.replacement > 0)
                audio.url = audio.url.replaceAll(stems.get(mAlbum.replacement - 1), "#");

            if (optimizeStem)
                audio.url = url.replaceAll("/", "").trim();
            mAudioList.add(audio);
            newEntries++;
            display("Updates: " + updates + " New: " + newEntries + "\n" + audio.title);
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String s) {
            display("Updates: " + updates + " New: " + newEntries);
//            shorten url
            shortenUrls(mAudioList, mAlbum, false);
            mGrid.setAdapter(mAudioAdapter = new AudioAdapter(mContext));
            runningTasks--;
            if (runningTasks <= 0) {
                findViewById(R.id.progress).setVisibility(View.GONE);
                try {
                    mWakeLock.release();
                } catch (Exception e) {
                    Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    void shortenUrls(final ArrayList<Audio> mAudioList, final Audio mAlbum, final boolean fetchAll) {
        new Thread(() -> shortenUrl_(mAudioList, mAlbum, fetchAll)).start();
    }

    private void shortenUrl_(ArrayList<Audio> mAudioList, Audio mAlbum, boolean fetchAll) {
        String s;
        if (mAudioList.size() > 2 && optimizeStem) {
            showProgressBar();
            s = findStem(mAudioList, mAlbum.replacement - 1).trim();
            ArrayList<String> tempStems = new ArrayList<>(stems);
            Collections.sort(tempStems, (o1, o2) -> o2.length() - o1.length());

            if (tempStems.size() == 0 || s.length() > tempStems.get(0).length()) {
                addStem(s, mAlbum, mAudioList);
            } else {
                int c = 0, len = s.length() - 1;
                if (len > 0 && !tempStems.contains(s)) {
                    for (int j = 1; j < mAudioList.size(); j++) {
                        String url = mAudioList.get(j).url;
                        if (url.contains("#")) {
                            try {
                                url = url.replaceAll("#", stems.get(mAlbum.replacement - 1));
                            } catch (final Exception e) {
                                display(url);
                                final String finalUrl = url;
                                runOnUiThread(() -> Toast.makeText(mContext, finalUrl + "\n" + e.getMessage(), Toast.LENGTH_LONG).show());
                            }
                        }
                        c += url.length() - url.replaceAll(s, "#").length();
                    }
                }
                int maxC = c;

                for (int i = 0; i < tempStems.size(); i++) {
                    String s1 = tempStems.get(i);
                    c = 0;
                    for (int j = 1; j < mAudioList.size(); j++) {
                        String url = mAudioList.get(j).url;
                        if (url.contains("#"))
                            try {
                                url = url.replaceAll("#", stems.get(mAlbum.replacement - 1));
                            } catch (final Exception e) {
                                display(url);
                                final String finalUrl = url;
                                runOnUiThread(() -> Toast.makeText(mContext, finalUrl + "\n" + e.getMessage(), Toast.LENGTH_LONG).show());
                            }
                        c += url.length() - url.replaceAll(s1, "#").length();
                    }
                    if (c > maxC) {
                        maxC = c;
                        s = s1;
                    }
                }
                addStem(s, mAlbum, mAudioList);
            }
        }
        for (Audio a : mAudioList) {
            a.title = a.title == null ? "" : a.title;
            a.title = a.title.replaceAll("\\s{2,}", " ");
            a.id = insertAlbum(mContext, a, true);
        }
        if (!fetchAll) {
            runOnUiThread(() -> {
                mAudioAdapter.notifyDataSetChanged();
                mProgressBar.setVisibility(View.GONE);
            });
        }
    }

    @SuppressLint("StaticFieldLeak")
    public class FetchAll extends AsyncTask<Audio, String, String> {
        int rebuild;
        boolean removeP;

        FetchAll(int rebuild, boolean removeP) {
            this.rebuild = rebuild;
            this.removeP = removeP;
        }

        @SuppressLint("WakelockTimeout")
        protected void onPreExecute() {
            mWakeLock.acquire();
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Audio... audio) {
            addAll(audio[0], removeP);
            hideProgressBar();
            if (mWakeLock.isHeld())
                mWakeLock.release();
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mProgressBar.setVisibility(View.GONE);
        }

        private void addAll(Audio a, boolean removeParent) {
            Log.d("addAll", "begin");
            ArrayList<Audio> list = new ArrayList<>();
            addFromDb(list, a);
            int size = list.size();
            if (size < 2)
                fetch(a, list);
            else {
                if (rebuild == 0) {
                    for (int i = 1; i < size; i++) {
                        Audio a1 = list.get(i);
                        a1.url = a1.url.replaceAll(" ", "_");
                        a1.setFromUrl(mDb, a.lang == 0 || a.lang == 1, a, removeParent);
                        insertAlbum(mContext, a1, true);
                        display(i + " of " + size + " ... " + a.title + "::" + a1.title);
                    }
                } else if (rebuild == 1) {
                    for (int i = 1; i < size; i++) {
                        Audio a1 = list.get(i);
                        a1.url = a1.url.replaceAll(" ", "_");
                        a1.setFromTitle(mDb, a.lang == 0 || a.lang == 1);
                        insertAlbum(mContext, a1, true);
                        display(i + " of " + size + " ... " + a.title + "::" + a1.title);
                    }
                }
            }
            display("\nUpdates: " + updates + " New: " + newEntries + "\n" + a.title);
            shortenUrl_(list, a, true);
            for (int i = 1; i < size; i++) {
                Audio a1 = list.get(i);
                if (a1.isAlbum)
                    addAll(a1, removeParent);
            }
        }

        private void fetch(Audio a, ArrayList<Audio> list) {
            String url = a.getUrl(mDb);
            try {
                URLConnection urlConnection = new URL(url).openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line;

                //noinspection StatementWithEmptyBody
                while ((line = reader.readLine()) != null && !Html.fromHtml(line).toString().contains("Parent Directory"))
                    ;

                while ((line = reader.readLine()) != null) {
                    Matcher m = linkPattern.matcher(line);
                    if (!m.find()) break;
                    String u = m.group().replace("<a href=\"", "").replace("\"", "");
                    line = Html.fromHtml(line).toString();
                    m = dPattern.matcher(line);
                    if (!m.find()) break;
                    String lu = m.group();
                    String size = line.substring(line.indexOf(lu)).replace(lu, "");
                    Audio audio = update(u, lu, size, a, removeParent);
                    if (audio != null)
                        list.add(audio);
                    newEntries++;
//                    publishProgress(link.url, link.lu, link.size);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private Audio update(String url, String lu, String size, Audio a, boolean removeParent) {
            Audio audio = new Audio();
            audio.parent = a.id;
            audio.arte = a.arte;
            audio.url = url.trim();
            if (url.toLowerCase().endsWith(".mp3") || url.toLowerCase().endsWith(".wav") || url.toLowerCase().endsWith(".wma")) {
                String s = size.replaceAll("M", "").replaceAll("(?i)k", "").trim();
                String s1 = s.replaceAll("\\s", "");
                float sz = Float.parseFloat(s1);
                if (size.toLowerCase().contains("m"))
                    audio.size = (int) (sz * 1024 * 1024);
                else if (s.toLowerCase().contains("k"))
                    audio.size = (int) (sz * 1024);
                audio.setFromUrl(mDb, a.lang == 0 || a.lang == 1, a, removeParent);
            } else if (url.endsWith("/") || !url.contains(".")) {
                audio.url = audio.url.replaceAll("/", "");
                audio.isAlbum = true;
                audio.lang = a.lang;
                audio.title = Html.fromHtml(audio.url.replaceAll("_", " ").replaceFirst("^[0-9\\-\\s.]+", "").trim()).toString();
                audio.hari();
                audio.date = lu;
                Cursor cursor = mDb.rawQuery("select * from album where " +
                        PARENT_ALBUM + " = ? AND url = ?", new String[]{audio.parent + "", audio.url});
                if (cursor.moveToFirst()) {
                    audio.id = cursor.getInt(0);
                    audio.newLu = audio.date;
                    audio.date = cursor.getString(cursor.getColumnIndex("lu"));
                    cursor.close();
                }
                audio.setFromUrl(mDb, a.lang == 0 || a.lang == 1, a, removeParent);
            } else
                return null;

            if (a.replacement > 0)
                audio.url = audio.url.replaceAll(stems.get(a.replacement - 1), "#");

            if (optimizeStem)
                audio.url = url.replaceAll("/", "").trim();
            display("\nUpdates: " + updates + " New: " + newEntries + "\n" + audio.title);
            return audio;
        }
    }

    private void shortenUrl(String s, Audio mAlbum, ArrayList<Audio> mAudioList) {
        if (mAlbum.replacement > 0) {
            for (int i = 1; i < mAudioList.size(); i++) {
                Audio a = mAudioList.get(i);
                a.url = a.url.replaceAll("#",
                        stems.get(mAlbum.replacement - 1)).replaceAll(s, "#");
                ContentValues values = new ContentValues();
                values.put("url", a.url);
                if (a.isAlbum)
                    mDb.update("album", values, "_id = ?", new String[]{a.id + ""});
                else
                    mAudioDb.update("audio", values, "_id = ?", new String[]{a.id + ""});
            }
        } else {
            for (int i = 1; i < mAudioList.size(); i++) {
                Audio a = mAudioList.get(i);
                a.url = a.url.replaceAll(s, "#");
                ContentValues values = new ContentValues();
                values.put("url", a.url);
                if (a.isAlbum)
                    mDb.update("album", values, "_id = ?", new String[]{a.id + ""});
                else
                    mAudioDb.update("audio", values, "_id = ?", new String[]{a.id + ""});
            }
        }

        ContentValues values = new ContentValues();
        values.put("url_rep_id", stems.indexOf(s) + 1);
        mDb.update("album", values, "_id = ?", new String[]{mAlbum.id + ""});
        mAlbum.replacement = stems.indexOf(s) + 1;
    }

    private void addStem(String s, Audio mAlbum, ArrayList<Audio> mAudioList) {
        if (s.length() < 3) return;
        Cursor cursor = mDb.rawQuery("select * from _ where t = ?", new String[]{s});
        ContentValues values = new ContentValues();
        values.put("t", s);
        if (!cursor.moveToFirst()) {
            long ind = mDb.insert("_", null, values);
            if (ind > 0) {
                stems.add(s);
                shortenUrl(s, mAlbum, mAudioList);
            }
        } else {
            shortenUrl(s, mAlbum, mAudioList);
//            Toast.makeText(mContext, "already exists", Toast.LENGTH_SHORT).show();
        }
        cursor.close();
    }

    public static long insertAlbum(Activity mContext, final Audio audio, boolean confirmUpdate) {
        if (audio.url.isEmpty()) {
            Toast.makeText(mContext, "Please Enter Url", Toast.LENGTH_LONG).show();
            return -1;
        }
        final ContentValues values = new ContentValues();
        values.put("title", audio.title);
        values.put("parent", audio.parent);
        values.put("arte", audio.arte);
        String url = audio.url;
        values.put("url", url);

        if (audio.isAlbum) {
            if (null != audio.date && !audio.date.isEmpty())
                values.put("lu", audio.date);
            values.put("lang", audio.lang);
            values.put("url_rep_id", audio.replacement);
            final Cursor cursor = mDb.rawQuery("Select * from album where url = ? AND " + PARENT_ALBUM + " = ?",
                    new String[]{url, audio.parent + ""});
            if (!cursor.moveToFirst()) {
                cursor.close();
                return mDb.insert("album", null, values);
            } else {
                if (confirmUpdate) {
                    mDb.update("album", values, "_id = ?", new String[]{cursor.getString(0)});
                } else {
                    (new AlertDialog.Builder(mContext)).setMessage("Album Already Exists")
                            .setPositiveButton("Update", (dialog, which) -> mDb.update("album", values, "_id = ?", new String[]{cursor.getString(0)})).show();
                }
                return cursor.getInt(0);
            }
        } else {
            if (null != audio.date)
                values.put("date", audio.date);
            values.put("place", audio.place);
            values.put("ref", audio.ref);
            values.put("size", audio.size);
            final Cursor cursor = mAudioDb.rawQuery("Select * from audio where url = ? AND parent = ?",
                    new String[]{url, audio.parent + ""});
            if (!cursor.moveToFirst()) {
                cursor.close();
                return mAudioDb.insert("audio", null, values);
            } else {
                if (confirmUpdate) {
                    mAudioDb.update("audio", values, "_id = ?", new String[]{cursor.getString(0)});
                } else {
                    (new AlertDialog.Builder(mContext)).setMessage("Already Exists")
                            .setPositiveButton("Update", (dialog, which) -> mAudioDb.update("audio", values, "_id = ?", new String[]{cursor.getString(0)})).show();
                }
                final int id = cursor.getInt(0);
                cursor.close();
                return id;
            }
        }
    }

    public void onClick(final View view) {
        if (view.getId() == R.id.add) {
            if (mGrid.getAdapter().getClass() == ArteAdapter.class) {
                Intent intent = new Intent();
                intent.setType("image/*").putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true).setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Images"), RESULT_LOAD_IMAGE);
            } else if (!editMode) {
                new AlertDialog.Builder(mContext).setMessage("Enter Edit Mode?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            editMode = true;
                            mGrid.setAdapter(mAudioAdapter = new AudioAdapter(mContext));
                        }).setNegativeButton("No", null).show();
            } else {
                PopupMenu p = new PopupMenu(mContext, view);
                p.getMenu().add("Add Language");
                p.getMenu().add("Add Place");
                p.getMenu().add("Add RegX to remove from All");
                p.getMenu().add("Shorten Url");
                p.getMenu().add("Replace from All");
                p.getMenu().add("Add All");
                p.getMenu().add("Close Edit Mode");
                p.setOnMenuItemClickListener(item -> {
                    switch (item.getTitle().toString()) {
                        case "Add Language":
                            addLang();
                            break;
                        case "Add Place":
                            addPlace();
                            break;
                        case "Add RegX to remove from All":
                            addRegX();
                            break;
                        case "Add All":
                            addAllWithPrompt();
                            break;
                        case "Shorten Url":
                            shortenUrl(view);
                            break;
                        case "Replace from All":
                            replaceFromAll();
                            break;
                        case "Close Edit Mode":
                            editMode = false;
                            mGrid.setAdapter(mAudioAdapter = new AudioAdapter(mContext));
                            break;
                    }
                    return false;
                });
                p.show();
            }
        }
    }

    private void addAllWithPrompt() {
        (new AlertDialog.Builder(mContext)).setTitle("Rebuild From Url or Title?")
                .setItems(new CharSequence[]{"Rebuild from Url", "Rebuild From Title", "Fetch but Don't rebuild existing", "Cancel"}, (dialog, which) -> {
                    switch (which) {
                        case 3:
                            break;
                        default:
                            new AlertDialog.Builder(mContext).setMessage("Remove Parent from all?")
                                    .setPositiveButton("Remove from all", (dialog1, which1) -> (new FetchAll(which, true)).execute(mAlbum))
                                    .setNegativeButton("Don't remove", (dialog12, which1) -> (new FetchAll(which, false)).execute(mAlbum))
                                    .setNeutralButton("Cancel", null).show();
                    }
                }).create().show();
    }

    private void shortenUrl(View view) {
        PopupMenu p = new PopupMenu(mContext, view);
        p.getMenu().add("Auto");
        for (String r : stems) p.getMenu().add(r);
        p.getMenu().add("Create New");
        p.setOnMenuItemClickListener(item -> {
            String s = item.getTitle().toString();
            switch (s) {
                case "Create New":
                    final EditText editText = new EditText(mContext);
                    editText.setText(findStem(mAudioList, mAlbum.replacement - 1));
                    (new AlertDialog.Builder(mContext)).setTitle("Add Url Stem")
                            .setPositiveButton("Add", (dialog, which) -> addStem(editText.getText().toString().trim(), mAlbum, mAudioList)).setNegativeButton("Cancel", null).setView(editText).show();
                    break;
                case "Auto":
                    shortenUrls(mAudioList, mAlbum, false);
                    break;
                default:
                    shortenUrl(s, mAlbum, mAudioList);
                    mAudioAdapter.notifyDataSetChanged();
                    break;
            }
            return false;
        });
        p.show();
    }

    private void replaceFromAll() {
        final LinearLayout lt = new LinearLayout(mContext);
        lt.setGravity(Gravity.CENTER);
        lt.addView(new EditText(mContext));
        lt.addView(new EditText(mContext));
        (new AlertDialog.Builder(mContext)).setTitle("Replace from All").setView(lt)
                .setPositiveButton("Replace", (dialog, which) -> {
                    String regX = ((EditText) lt.getChildAt(0)).getText().toString();
                    String replacement = ((EditText) lt.getChildAt(1)).getText().toString();
                    for (int i = 1; i < mAudioList.size(); i++) {
                        Audio a = mAudioList.get(i);
                        a.title = a.title.replaceFirst(regX, replacement);
                        ContentValues values = new ContentValues();
                        values.put("title", a.title);
                        if (a.isAlbum)
                            mDb.update("album", values, "_id = ?", new String[]{a.id + ""});
                        else
                            mAudioDb.update("audio", values, "_id = ?", new String[]{a.id + ""});
                    }
                    mAudioAdapter.notifyDataSetChanged();
                }).setNegativeButton("Cancel", null).show();
    }

    private void addPlace() {
        final EditText input = new EditText(mContext);
        (new AlertDialog.Builder(mContext)).setTitle("Add Place").setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String p = input.getText().toString().trim();
                    if (!p.isEmpty() && !places.contains(p)) {
                        places.add(p);
                        Collections.sort(places, (o1, o2) -> o2.length() - o1.length());
                        writePlaces();
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    static final int LEFT = 1, TOP = 2, RIGHT = 4, BOTTOM = 8;//CENTER = 0,


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICKFILE_RESULT_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            (new FetchFromFileTask()).execute(uri);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
