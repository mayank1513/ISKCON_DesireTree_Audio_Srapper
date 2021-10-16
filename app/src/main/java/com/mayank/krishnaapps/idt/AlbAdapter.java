package com.mayank.krishnaapps.idt;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

import static com.mayank.krishnaapps.idt.MainActivity.albAdapter;
import static com.mayank.krishnaapps.idt.MainActivity.baseF;

public class AlbAdapter extends CursorAdapter {
    private final BaseActivity mContext;
    final static int AlbAdapter = 0, LyAdapter = 1, LyaAdapter = 2;
    int adapterType;
    AlbAdapter(BaseActivity context, Cursor c, int adapterType) {
        super(context, c, 0);
        mContext = context;
        this.adapterType = adapterType;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LinearLayout layout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.li_alb, parent, false);
        layout.setTag(new ViewHolder(layout));
        return layout;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        final long albId = cursor.getLong(0);
        String arte = cursor.getString(cursor.getColumnIndex("arte"));
        final String id = cursor.getString(0);
        if(adapterType==AlbAdapter) {
            String title = cursor.getString(cursor.getColumnIndex("title"));
            int subalbs = cursor.getInt(cursor.getColumnIndex("sublabs"));
            int audios = cursor.getInt(cursor.getColumnIndex("audios"));
            holder.title.setText(title);
            holder.stats.setText("(" + subalbs + " albums, " + audios + " audio)");
        } else
            holder.title.setText(id + ", " + cursor.getString(cursor.getColumnIndex("name")));
        if(adapterType == LyaAdapter) {
            final BaseActivity context1 = (MainActivity) context;
            view.setOnClickListener(v -> context1.mGrid.setAdapter(albAdapter = new AlbAdapter(context1, MainActivity.mDb
                    .rawQuery("select * from lyrics where parent = ?", new String[]{id}), LyAdapter)));
        }
        if(null == arte || arte.isEmpty())
            holder.arte.setImageResource(R.mipmap.ic_launcher_round);
        else
            holder.arte.setImageURI(Uri.fromFile(new File(baseF + mContext.getString(R.string.thumb_, 6, arte))));

        holder.arte.setOnClickListener(v -> mContext.mGrid.setAdapter(new ArteAdapter(mContext, albId)));
    }

    class ViewHolder {
        TextView title, stats;
        ImageButton arte;
        ViewHolder(LinearLayout layout){
            title = layout.findViewById(R.id.title);
            stats = layout.findViewById(R.id.stats);
            arte = layout.findViewById(R.id.arte);
        }
    }
}
