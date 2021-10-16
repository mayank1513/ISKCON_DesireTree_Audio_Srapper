package com.mayank.krishnaapps.idt;
/*
 * Inspect and update database tables
 * */

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

import static com.mayank.krishnaapps.idt.ExportHelper.Export;
import static com.mayank.krishnaapps.idt.ExportHelper.FetchWMA;
import static com.mayank.krishnaapps.idt.ExportHelper.writeToFiles;
import static com.mayank.krishnaapps.idt.MainActivity.baseF;
import static com.mayank.krishnaapps.idt.MainActivity.dp10;

public class InspectActivity extends BaseActivity {
    SQLiteDatabase mAudioDb;
    String dbName, dir;
    InspectActivity mContext;
    public static ArrayList<String> languages = new ArrayList<>();
    public static ArrayList<String> placesInDb = new ArrayList<>();
    public static ArrayList<String> regXTORemoveFromAll = new ArrayList<>();
    public static ArrayList<String> stems = new ArrayList<>();
    public static boolean optimizeStem = true, enableEditing = false;
    public static boolean removeParent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            dbName = Objects.requireNonNull(getIntent().getExtras()).getString("db_name");
            dir = Objects.requireNonNull(getIntent().getExtras()).getString("dir");
            mDb = SQLiteDatabase.openDatabase(baseF + "/" + dir + "db/" + dbName, null, SQLiteDatabase.OPEN_READWRITE);
            mAudioDb = SQLiteDatabase.openDatabase(baseF + "/" + dir + "db/" + dbName + "_audio", null, SQLiteDatabase.OPEN_READWRITE);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open database: " +
                    baseF + "/" + dir + "db/" + dbName + "; " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
        mContext = this;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Export");
        menu.add("Download WMA");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getTitle().toString()) {
            case "Export":
                Export(dbName);
                break;
            case "Download WMA":
                FetchWMA(dbName);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    boolean started = false;

    @Override
    protected void onStart() {
        super.onStart();
        if (started) return;
        started = true;
        mGrid = findViewById(R.id.grid);
        mGrid.setAdapter(new Adapter());

        load(placesInDb, mDb.rawQuery("select * from places", null));
        load(stems, mDb.rawQuery("select * from _", null));
        load(languages, mDb.rawQuery("select * from lang", null));
        load(regXTORemoveFromAll, mDb.rawQuery("select * from regx", null));
        Collections.sort(regXTORemoveFromAll, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o2.length() - o1.length();
            }
        });
        Cursor c = mDb.rawQuery("select * from info", null);
        c.moveToFirst();
        try {
            optimizeStem = c.getInt(c.getColumnIndex("version")) == 0;
        } catch (Exception ignored) {
        }
        c.close();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                showProgressBar();
//                Cursor c = mAudioDb.rawQuery("select * from audio", null);
//                if(c.moveToFirst()){
//                    do {
//                        ContentValues values = new ContentValues();
//                        String arte = c.getString(c.getColumnIndex("arte"));
//                        if(null != arte) {
//                            values.put("arte", arte.replaceAll("\\.", "'"));
//                            mAudioDb.update("audio", values, "_id = ?", new String[]{c.getString(0)});
//                        }
//                    } while (c.moveToNext());
//                }
//                c.close();
//                c = mDb.rawQuery("select * from album", null);
//                if(c.moveToFirst()){
//                    do {
//                        ContentValues values = new ContentValues();
//                        String arte = c.getString(c.getColumnIndex("arte"));
//                        if(null!=arte) {
//                            values.put("arte", arte.replaceAll("\\.", "'"));
//                            mDb.update("album", values, "_id = ?", new String[]{c.getString(0)});
//                        }
//                    } while (c.moveToNext());
//                }
//                c.close();
//                hideProgressBar();
//            }
//        }).start();
        try {
            Intent intent = getIntent();
            if (Objects.requireNonNull(intent.getExtras()).containsKey("skip")) {
                startActivity(new Intent(mContext, UpdateActivity.class).putExtra("dir", dir)
                        .putExtra("db_name", dbName).putExtra("album_id", 1L));
                mDb.close();
                mAudioDb.close();
                finish();
            }
            removeParent = intent.getIntExtra("hasDefTables", 0) == 1;
            String s = intent.getStringExtra("exp");
            Toast.makeText(mContext, "Hare Krishna - hari hari : " + s, Toast.LENGTH_LONG).show();
            if (s != null) {
                mDb.close();
                mAudioDb.close();
                switch (s) {
                    case "exp":
                        Export(dbName);
                        break;
                    case "write":
                        writeToFiles(dbName);
                        break;
                    default:
                        FetchWMA(dbName);
                        break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void load(ArrayList<String> into, Cursor from) {
        into.clear();
        if (from.moveToFirst()) {
            do {
                into.add(from.getString(1));
            } while (from.moveToNext());
        }
        from.close();
    }

    @Override
    public void onBackPressed() {
        if (mGrid.getAdapter().getClass() != Adapter.class)
            mGrid.setAdapter(new Adapter());
        else
            super.onBackPressed();
    }

    public void onClick(View view) {
        if (view.getId() == R.id.add) {
            final PopupMenu p = new PopupMenu(mContext, view);
            p.getMenu().add("Add Albums");
            p.getMenu().add("Edit RegX");
            p.getMenu().add("Enable Editing");
            p.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getTitle().toString()) {
                        case "Enable Editing":
                            enableEditing = !enableEditing;
                            p.getMenu().removeItem(item.getItemId());
                            p.getMenu().add(enableEditing ? "Disable Editing" : "Enable Editing");
                            break;
                        case "Add Albums":
                            startActivity(new Intent(mContext, UpdateActivity.class).putExtra("dir", dir)
                                    .putExtra("db_name", dbName).putExtra("album_id", 1L));
                            break;
                        case "Edit RegX":
                            mGrid.setAdapter(new RegXAdapter(mDb.rawQuery("select * from regx", null)));
                            break;
                    }
                    return false;
                }
            });
            p.show();
        }
    }

    class Adapter extends BaseAdapter {
        ArrayList<String> tables = new ArrayList<>();

        Adapter() {
            tables.add("audio");
            Cursor c = mDb.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name!='android_metadata' AND name!='sqlite_sequence' order by name", null);
            if (c.moveToFirst()) {
                while (!c.isAfterLast()) {
                    tables.add(c.getString(0));
                    c.moveToNext();
                }
                c.close();
            }
        }

        @Override
        public int getCount() {
            return tables.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            TextView view = new TextView(mContext);
            final String t = tables.get(position);
            view.setText(t);
            view.setTextSize(24);
            view.setPadding(dp10, dp10, dp10, dp10);
            view.setTypeface(null, Typeface.BOLD);
            view.setOnClickListener(v -> {
                if (position == 0) {
                    mGrid.setAdapter(new TableAdapter(mAudioDb.rawQuery("select * from " + t, null), mAudioDb, t));
                } else {
                    mGrid.setAdapter(new TableAdapter(mDb.rawQuery("select * from " + t, null), mDb, t));
                }
            });
            return view;
        }
    }

    class TableAdapter extends CursorAdapter {
        ArrayList<String> columns;
        int[] widths;
        SQLiteDatabase db;
        String table;

        TableAdapter(Cursor c, SQLiteDatabase db, String table) {
            super(mContext, c, 1);
            columns = new ArrayList<>(Arrays.asList(c.getColumnNames()));
            widths = new int[columns.size()];
            Arrays.fill(widths, 0);
            this.db = db;
            this.table = table;
            View headerView = newView(mContext, null, null);
            ((ViewGroup)mGrid.getParent()).addView(headerView);
            LinearLayout layout = (LinearLayout) ((SyncedHorizontalScrollView)headerView).getChildAt(0);
            for (int i = 0; i<layout.getChildCount(); i++) {
                final TextView textView = (TextView) layout.getChildAt(i);
                textView.setText(String.format("%s ", columns.get(i)));
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            SyncedHorizontalScrollView view = new SyncedHorizontalScrollView(mContext);
            LinearLayout layout = new LinearLayout(mContext);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setPadding(dp10, dp10, dp10, dp10);
            layout.setBackgroundColor(Color.GRAY);
            view.addView(layout);
            for (int i = 0; i < columns.size(); i++) {
                EditText textView = new EditText(mContext);
                layout.addView(textView);
                textView.setTextSize(20);
                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) textView.getLayoutParams();
                p.setMargins(dp10 / 2, dp10 / 2, dp10 / 2, dp10 / 2);
                textView.setLayoutParams(p);
                textView.setEnabled(false);
                textView.setBackgroundColor(Color.WHITE);
            }
            syncedHorizontalScrollViews.add(view);
            return view;
        }

        @Override
        public void bindView(View view, final Context context, final Cursor cursor) {
            LinearLayout layout = (LinearLayout) ((SyncedHorizontalScrollView) view).getChildAt(0);
            final String id = cursor.getString(0);
            for (int i = 0; i < layout.getChildCount(); i++) {
                final TextView textView = (TextView) layout.getChildAt(i);
                textView.setText(String.format("%s ", cursor.getString(i)));
                textView.setEnabled(enableEditing);
            }
            view.post(() -> {
                for (int i = 0; i < layout.getChildCount(); i++) {
                    final TextView textView = (TextView) layout.getChildAt(i);
                    int w = textView.getMeasuredWidth();
                    if(w>widths[i]) widths[i] = w;
                    else textView.setWidth(widths[i]);
                }
//                notifyDataSetChanged();
            });
        }
    }

    class RegXAdapter extends CursorAdapter {
        RegXAdapter(Cursor c) {
            super(mContext, c, 1);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LinearLayout layout = new LinearLayout(mContext);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            EditText editText = new EditText(mContext);
            editText.setPadding(dp10, dp10, dp10, dp10);
            editText.setTypeface(null, Typeface.BOLD);
            editText.setTextSize(20);
            editText.setId(0);
            layout.addView(editText);
            ImageButton button = new ImageButton(mContext);
            button.setImageResource(R.drawable.ic_delete_black_24dp);
            button.setId(1);
            layout.addView(button);
            return layout;
        }

        @Override
        public void bindView(View view, Context context, final Cursor cursor) {
            EditText editText = view.findViewById(0);
            ImageButton btn = view.findViewById(1);
            editText.setText(cursor.getString(1));
            final String id = cursor.getString(0);
            editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) return;
                    String s = ((EditText) v).getText().toString().trim();
                    Cursor c = mDb.rawQuery("select * from regx where t = ?", new String[]{s});
                    if (c.moveToFirst()) {
                        c.close();
                        return;
                    }
                    c.close();
                    ContentValues values = new ContentValues();
                    values.put("t", s);
                    mDb.update("regx", values, "_id = ?", new String[]{id});
                }
            });
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDb.delete("regx", "_id = ?", new String[]{id});
                }
            });
        }
    }
}
