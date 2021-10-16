package com.mayank.krishnaapps.idt;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import static com.mayank.krishnaapps.idt.BaseActivity.mDb;
import static com.mayank.krishnaapps.idt.ExportHelper.decode;
import static com.mayank.krishnaapps.idt.ExportHelper.encode;
import static com.mayank.krishnaapps.idt.InspectActivity.languages;
import static com.mayank.krishnaapps.idt.InspectActivity.placesInDb;
import static com.mayank.krishnaapps.idt.MainActivity.Lyrics;
import static com.mayank.krishnaapps.idt.MainActivity.LyricsIds;
import static com.mayank.krishnaapps.idt.MainActivity.baseF;
import static com.mayank.krishnaapps.idt.MainActivity.specialPlaces;
import static com.mayank.krishnaapps.idt.MainActivity.writeSpecialPlaces;
import static com.mayank.krishnaapps.idt.UpdateActivity.insertAlbum;
import static com.mayank.krishnaapps.idt.UpdateActivity.mAudioDb;
import static com.mayank.krishnaapps.idt.albSql.PARENT_ALBUM;

public class AudioAdapter extends BaseAdapter {
    private final UpdateActivity mContext;
    private final int[] maxWidths = new int[9];

    AudioAdapter(UpdateActivity mContext) {
        this.mContext = mContext;
    }

    @Override
    public int getCount() {
        return mContext.mAudioList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if(convertView == null){
            holder = new ViewHolder(convertView = LayoutInflater.from(mContext).inflate(R.layout.li_album, parent, false));
            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();

        final Audio a = mContext.mAudioList.get(position);

        holder.id.setText(a.isAlbum && a.id == mContext.mAlbum.id ? a.id + "(" + a.replacement + ", " + (getCount()-1) + ")":a.id+"");
        holder.id.setOnClickListener(v -> {
//                if(a.isAlbum)
//                    ExportAlbum(mDb, mAudioDb, a.id, "", "");
        });
        holder.parent.setText(a.parent + "");
        holder.parent.setOnLongClickListener(v -> {
            holder.url.setEnabled(true);
            holder.url.setText(holder.url.getText().toString().trim().replaceAll(" ", "_"));
            holder.url.setTag(a.id);
            return true;
        });

        setEditText(holder, a, "url");
        setEditText(holder, a, "title");
        setEditText(holder, a, "hindi_title");
        if (null == a.arte || a.arte.isEmpty()) {
            holder.arte.setImageResource(R.mipmap.ic_launcher_round);
        } else {
            holder.arte.setImageURI(Uri.fromFile(new File(baseF + mContext.getString(R.string.thumb_, 6, a.arte))));
        }
        holder.arte.setOnClickListener(v -> {
            mContext.mGrid.setAdapter(mContext.mArteAdapter = new ArteAdapter(mContext, a));
            mContext.mGrid.setSelection((int) decode(a.arte));
        });

        holder.add.setImageResource(a.id < 0 || (a.isAlbum && a.id == mContext.mAlbum.id && (a.newLu.isEmpty() || a.date.equals(a.newLu)))? R.drawable.ic_playlist_add_black_24dp : R.drawable.open);
        holder.add.setOnClickListener(v -> {
            if(a.title!=null && a.title.startsWith("#")){
                Toast.makeText(mContext, "Title starting with #\n...ignored album", Toast.LENGTH_SHORT).show();
            } else if(a.id < 0) {
                a.url = holder.url.getText().toString().trim().replaceAll(" ", "_");
                holder.url.setText(a.url);
                a.arte = mContext.mAlbum.arte;
                a.lang = mContext.mAlbum.lang;
                a.id = insertAlbum(mContext, a, false);
                holder.add.setImageResource(a.id < 0 ? R.drawable.ic_playlist_add_black_24dp : R.drawable.open);
                holder.id.setText(a.id + "");
            } else if(a.isAlbum && a.id == mContext.mAlbum.id && !(a.newLu.isEmpty() || a.date.equals(a.newLu))){
                a.newLu = a.date;
                ContentValues values = new ContentValues();
                values.put("lu", a.newLu);
                mDb.update("album", values, "_id = ?", new String[]{a.id+""});

                long parent1 = a.parent;
                while (parent1 >0){
                    mDb.update("album", values, "_id = ?", new String[]{parent1 +""});
                    Cursor c = mDb.rawQuery("select * from album where _id = ?",new String[]{parent1 +""});
                    if(c.moveToFirst()){
                        parent1 = c.getInt(c.getColumnIndex(PARENT_ALBUM));
                    } else
                        parent1 = -1;
                    c.close();
                }
            } else if (a.isAlbum && a.id == mContext.mAlbum.id){
                Audio audio = new Audio();
                audio.parent = a.id;
                audio.lang = a.lang;
                audio.isAlbum = true;
                mContext.mAudioList.add(audio);
                notifyDataSetChanged();
                mContext.mGrid.smoothScrollToPosition(getCount());
            } else if (a.isAlbum){
                mContext.startActivity(new Intent(mContext, UpdateActivity.class).putExtra("dir", mContext.dir)
                        .putExtra("db_name", mContext.dbName).putExtra("album_id", a.id));
//                    mContext.finish();
            } else {
                mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(a.getUrl(mDb))));
            }
        });
        setEditText(holder, a, "date");
        if(a.newLu.isEmpty() || !a.newLu.equals(a.date)) {
            holder.newLu.setVisibility(View.GONE);
            holder.newLu.setText(a.newLu);
            holder.add.setColorFilter(a.isAlbum && a.id == mContext.mAlbum.id ? Color.BLUE : a.isAlbum ? Color.BLACK : Color.BLUE);
        } else {
            holder.add.setColorFilter(Color.RED);
            holder.newLu.setVisibility(View.VISIBLE);
        }

        if(a.isAlbum) {
            holder.langOrPlace.setText(a.lang + "");
            holder.langOrPlace.setOnClickListener(v -> {
                PopupMenu p = new PopupMenu(mContext, v);
                for(String l:languages) p.getMenu().add(l);
                p.setOnMenuItemClickListener(item -> {
                    a.lang = languages.indexOf(item.getTitle().toString());
                    holder.langOrPlace.setText(languages.get(a.lang));
                    final ContentValues values = new ContentValues();
                    values.put("lang", a.lang);
                    mDb.update("album", values, "_id = ?", new String[]{a.id + ""});
                    if(a.id == mContext.mAlbum.id){
                        new AlertDialog.Builder(mContext).setMessage("Update All Sub Alb")
                                .setPositiveButton("Update All in List", (dialog, which) -> {
                                    for(Audio a1:mContext.mAudioList){
                                        a1.lang = a.lang;
                                        mDb.update("album", values, "_id = ?", new String[]{a1.id + ""});
                                    }
                                    notifyDataSetChanged();
                                }).setNeutralButton("Update All subalbums", (dialog, which) -> {
                                    for(Audio a1:mContext.mAudioList){
                                        a1.lang = a.lang;
                                        mDb.update("album", values, "_id = ?", new String[]{a1.id + ""});
                                        Cursor c = mDb.rawQuery("select * from album where parent = ?", new String[]{a1.id+""});
                                        if(c.moveToFirst()) do {
                                            mDb.update("album", values, "_id = ?", new String[]{c.getString(0)});
                                        } while (c.moveToNext());
                                        c.close();
                                    }
                                    notifyDataSetChanged();
                                }).setNegativeButton("Cancel", null).show();
                    }
                    return false;
                });
                p.show();
            });
        } else {
            final String s = a.place < 1 ? "" : placesInDb.get(a.place - 1);
            holder.langOrPlace.setText(s);
            holder.langOrPlace.setOnClickListener(v -> {
                PopupMenu p = new PopupMenu(mContext, v);
                p.getMenu().add("Add to Special Places");
                p.getMenu().add("Select Place");
                p.setOnMenuItemClickListener(item -> {
                    if(item.getTitle().equals("Add to Special Places")){
                        if(s.isEmpty() || specialPlaces.contains(s)){
                            Toast.makeText(mContext, s + " is already a special place.", Toast.LENGTH_SHORT).show();
                        } else {
                            specialPlaces.add(s.trim());
                            writeSpecialPlaces();
                        }
                    }
                    return false;
                });
                p.show();
            });
            holder.langOrPlace.setOnLongClickListener(v -> {
                Toast.makeText(mContext, a.place+"", Toast.LENGTH_SHORT).show();
                for(String p:placesInDb)
                    Toast.makeText(mContext, p, Toast.LENGTH_SHORT).show();
                return true;
            });
        }
        int m = 1024*1024, k = 1024;
        holder.sz.setText(a.size>m ? a.size/m+"M" : a.size>k ? a.size/k+"k" : a.size + "bytes");
        setEditText(holder, a, "ref");
        if(a.isAlbum && a.id == mContext.mAlbum.id)
            convertView.setBackgroundColor(Color.RED);
        else
            convertView.setBackgroundColor(Color.GREEN);

        convertView.post(setWidths(holder));

        holder.selectLyrics.setVisibility(!a.isAlbum && (mContext.mAlbum.lang == 0 || mContext.mAlbum.lang == 1)? View.VISIBLE : View.GONE);
        holder.selectLyrics.setOnClickListener(v -> {
            mContext.temp = new ArrayList<>(Lyrics);
            mContext.mGrid.setAdapter(new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, mContext.temp));
            mContext.mSearchContainer.setVisibility(View.VISIBLE);
            mContext.mSearch.addTextChangedListener(mContext.textWatcher);
            mContext.mGrid.setOnItemClickListener((parent12, view, i, id) -> {
                String l = mContext.temp.get(i);
                a.ref = encode(LyricsIds.get(Lyrics.indexOf(l)));
                ContentValues values = new ContentValues();
                values.put("ref", a.ref);
                mAudioDb.update("audio", values, "_id = ?", new String[]{a.id + ""});
                holder.ref.setText(a.ref);
                mContext.mGrid.setAdapter(mContext.mAudioAdapter);
                mContext.mGrid.smoothScrollToPosition(position);
                mContext.mGrid.setOnItemClickListener(null);
                mContext.mSearchContainer.setVisibility(View.GONE);
            });
        });
        return convertView;
    }

    private void setEditText(ViewHolder holder, final Audio a, final String colName) {
        final EditText t;
        switch (colName){
            case "url":
                t = holder.url;
                t.setText(a.url);
                t.setEnabled((null != t.getTag() && (long)t.getTag() == a.id) || a.url==null || a.url.isEmpty());
                break;
            case "title":
                t = holder.title;
                t.setText(a.title);
                break;
            case "hindi_title":
                t = holder.hindi_title;
                t.setText(a.hindi_title);
                break;
            case "ref":
                t = holder.ref;
                t.setText(a.ref);
                t.setEnabled(!a.isAlbum);
                break;
            case "date":
                t = holder.lu;
                t.setText(a.date);
                t.setEnabled(!a.isAlbum);
                break;
            default:
                return;
        }
        t.setEnabled(t.isEnabled() && mContext.editMode);
        t.setOnFocusChangeListener((v, hasFocus) -> {
            switch (colName){
                case "url":
                    a.url = t.getText().toString();
                    break;
                case "title":
                    a.title = t.getText().toString();
                    break;
                case "hindi_title":
                    a.hindi_title = t.getText().toString();
                    break;
                case "ref":
                    a.ref = t.getText().toString();
                    break;
                case "date":
                    a.date = t.getText().toString();
                    break;
            }
            if(a.id > 0 && !(a.isAlbum && colName.equals("date"))){
                ContentValues values = new ContentValues();
                values.put(colName, t.getText().toString());
                if(a.isAlbum && !colName.equals("ref"))
                    mDb.update("album", values, "_id = ?", new String[]{a.id + ""});
                else if(!a.isAlbum)
                    mAudioDb.update("audio", values, "_id = ?", new String[]{a.id + ""});
            }
        });
    }

    public static class ViewHolder {
        TextView id, parent, newLu, sz;
        EditText url, title, hindi_title, ref, lu;
        ImageView arte;
        ImageButton add;
        Button langOrPlace;
        Button selectLyrics;
        ViewHolder(View v){
            id = v.findViewById(R.id._id);
            parent = v.findViewById(R.id.parent);
            url = v.findViewById(R.id.url);
            title = v.findViewById(R.id.title);
            hindi_title = v.findViewById(R.id.hindi_title);
            arte = v.findViewById(R.id.arte);
            add = v.findViewById(R.id.add_fetch);
            lu = v.findViewById(R.id.lastUpdate);
            newLu = v.findViewById(R.id.newLastUpdate);
            langOrPlace = v.findViewById(R.id.lang_or_place);
            sz = v.findViewById(R.id.size);
            ref = v.findViewById(R.id.ref);
            selectLyrics = v.findViewById(R.id.setLyrics);
        }
    }

    private Runnable setWidths(final ViewHolder holder){
        return () -> {
            setWidth(holder.id, 0);
            setWidth(holder.parent, 1);
            setWidth(holder.url, 2);
            setWidth(holder.title, 3);
            setWidth(holder.lu, 4);
            setWidth(holder.langOrPlace, 5);
            setWidth(holder.sz, 6);
            setWidth(holder.ref, 7);
            setWidth(holder.hindi_title, 8);
        };
    }
    private void setWidth(TextView v, int i) {
        int w = v.getMeasuredWidth();
        if(w > maxWidths[i]){
            maxWidths[i] = w;
            notifyDataSetChanged();
        } else {
            v.setWidth(maxWidths[i]);
        }
    }
}
