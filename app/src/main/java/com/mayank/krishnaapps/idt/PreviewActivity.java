package com.mayank.krishnaapps.idt;
/*
* Preview how database will look in shravanotsava app*/
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import static com.mayank.krishnaapps.idt.ExportHelper.encode;
import static com.mayank.krishnaapps.idt.MainActivity.baseF;

public class PreviewActivity extends BaseActivity {

    SQLiteDatabase mAudioDb;
    String dbName;
    PreviewActivity mContext;
    albAdapter mAdapter;
    String mParent = "-1";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        handleIntent(getIntent());
    }

    @Override
    public void onBackPressed() {
        if(!mParent.equals("-1")){
            Cursor c = mDb.rawQuery("select * from alb where _id = ?", new String[]{mParent});
            if(c.moveToFirst()) {
                mParent = c.getString(c.getColumnIndex("parent"));
                mAdapter.swapCursor(mDb.rawQuery("select * from alb where parent = ?", new String[]{mParent}));
                mAdapter.isAlb = true;
            }
            c.close();
        } else
            super.onBackPressed();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    public void handleIntent(Intent intent){
        dbName = intent.getStringExtra("db_name");
        mDb = SQLiteDatabase.openDatabase(baseF + "/db/" + dbName, null, SQLiteDatabase.OPEN_READWRITE);
        mAudioDb = SQLiteDatabase.openDatabase(baseF + "/db/" + dbName + "_audio", null, SQLiteDatabase.OPEN_READWRITE);
        mAdapter = new albAdapter(mDb.rawQuery("select * from alb where parent = ?", new String[]{mParent}));
        mGrid.setAdapter(mAdapter);
    }

    private class albAdapter extends CursorAdapter{
        boolean isAlb = true;
        albAdapter(Cursor c) {
            super(mContext, c, 1);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.li_alb, parent, false);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final int id = cursor.getInt(0);
            ((ImageButton)view.findViewById(R.id.arte)).setImageURI(Uri.fromFile(new File(baseF +
                    getString(R.string.thumb_, 6, cursor.getString(cursor.getColumnIndex("arte"))))));
//            ((TextView)view.findViewById(R.id.stats)).setText("(" + cursor.getString(3) + "," + cursor.getString(4));
            ((TextView)view.findViewById(R.id.title)).setText(id + "," + cursor.getString(1));
            if(isAlb)
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Cursor c = mDb.rawQuery("select * from alb where parent = ?", new String[]{mParent = (id + "")});
                    if(c.getCount()>0) {
                        isAlb = true;
                        mAdapter.swapCursor(c);
                    } else {
                        isAlb = false;
                        c = mAudioDb.rawQuery("select * from audio where alb like '% " + encode(id).replaceAll("'", "''") + " %'", null);
                        Toast.makeText(mContext, c.getCount() + " Audios", Toast.LENGTH_LONG).show();
                        mAdapter.swapCursor(c);
                    }
                }
            });
        }
    }
}
