package com.mayank.krishnaapps.idt;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;

import static com.mayank.krishnaapps.idt.BaseActivity.mDb;
import static com.mayank.krishnaapps.idt.ExportHelper.encode;
import static com.mayank.krishnaapps.idt.MainActivity.albAdapter;
import static com.mayank.krishnaapps.idt.MainActivity.arteDes;
import static com.mayank.krishnaapps.idt.MainActivity.baseF;
import static com.mayank.krishnaapps.idt.UpdateActivity.BOTTOM;
import static com.mayank.krishnaapps.idt.UpdateActivity.LEFT;
import static com.mayank.krishnaapps.idt.UpdateActivity.RIGHT;
import static com.mayank.krishnaapps.idt.UpdateActivity.TOP;
import static com.mayank.krishnaapps.idt.UpdateActivity.mAudioDb;

public class ArteAdapter extends BaseAdapter {
    TextWatcher tWatcher;
    private BaseActivity mContext;
    Audio mAudio;
    private long mAlbId = -1;
    ArteAdapter(BaseActivity context, Audio audio) {
        mContext = context;
        mAudio = audio;
        setSearch();
    }

    ArteAdapter(BaseActivity context, long albId) {
        mContext = context;
        mAlbId = albId;
        setSearch();
    }
    int i = 0;
    private void setSearch(){
        mContext.mSearchContainer.setVisibility(View.VISIBLE);
        mContext.mSearch.removeTextChangedListener(mContext.textWatcher);
        tWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int i = mContext.mGrid.getLastVisiblePosition()<getCount()-1?mContext.mGrid.getFirstVisiblePosition():0;
                do {
                    if (arteDes.get(i).toLowerCase().contains(s.toString().toLowerCase())) {
                        mContext.mGrid.setSelection(i);
                        break;
                    }
                } while (i++ < getCount()-1);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        mContext.mSearch.addTextChangedListener(tWatcher);
        View.OnClickListener clickListener = v -> {
            String s = mContext.mSearch.getText().toString();
            final int id = v.getId();
            int firstVisiblePosition = mContext.mGrid.getFirstVisiblePosition();
//                int count = mContext.mGrid.getChildCount();
            int lastVisiblePosition = mContext.mGrid.getLastVisiblePosition();
            if(i<firstVisiblePosition||i>lastVisiblePosition)
                i = id==R.id.down? firstVisiblePosition : lastVisiblePosition;
            else
                i += id==R.id.down?1:-1;
            do {
                if (arteDes.get(i).toLowerCase().contains(s.toLowerCase())) {
                    mContext.mGrid.setSelection(i);
                    i+=id==R.id.down?1:-1;
                    break;
                }
                i+=id==R.id.down?1:-1;
            } while (i>0 &&i < getCount()-1);
            if (i >= getCount()-1||i<=0) {
                new AlertDialog.Builder(mContext).setMessage("Reached End")
                        .setPositiveButton("Start from Beginning", (dialog, which) -> mContext.mGrid.setSelection(id==R.id.down?0:getCount())).setNegativeButton("Cancel", null).show();
            }
        };
        mContext.mSearchContainer.findViewById(R.id.down).setOnClickListener(clickListener);
        mContext.mSearchContainer.findViewById(R.id.up).setOnClickListener(clickListener);
    }

    @Override
    public int getCount() {
        return (new File(baseF + "/export/a/x16")).listFiles().length/2;
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
        final ViewHolder holder;
        if(convertView == null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.li_arte, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else
            holder = (ViewHolder)convertView.getTag();

        holder.bg.setImageURI(Uri.fromFile(new File(baseF + mContext.getString(R.string.arte_, 12, encode(position)))));
        holder.thumb.setImageURI(Uri.fromFile(new File(baseF + mContext.getString(R.string.thumb_, 12, encode(position)))));

        convertView.setOnClickListener(v -> {
            if(mContext.getClass() == UpdateActivity.class) {
                if (mAudio.isAlbum && ((UpdateActivity) mContext).mAlbum.id == mAudio.id) {
                    (new AlertDialog.Builder(mContext)).setMessage("Change all?")
                            .setPositiveButton("Change All", (dialog, which) -> {
                                for (Audio a : ((UpdateActivity) mContext).mAudioList)
                                    addArte(position, a);
                            }).show();
                }
                addArte(position, mAudio);
                if(mContext.getClass() == UpdateActivity.class){
                    mContext.mGrid.setSelection(((UpdateActivity) mContext).mAudioList.indexOf(mAudio));
                }
            } else if(mAlbId>0){
                ContentValues values = new ContentValues();
                values.put("arte", encode(position));
                String alb = "alb";
                switch (albAdapter.adapterType){
                    case AlbAdapter.AlbAdapter:
                        break;
                    case AlbAdapter.LyAdapter:
                        alb = "lyrics";
                        break;
                    case AlbAdapter.LyaAdapter:
                        alb = "authors";
                        break;
                }
                MainActivity.mDb.update(alb, values, "_id = ?", new String[]{mAlbId + ""});
                if(!alb.equals("lyrics"))
                    albAdapter.swapCursor(MainActivity.mDb.rawQuery("select * from " + alb, null));
                mContext.mGrid.setAdapter(albAdapter);
                mContext.mSearch.removeTextChangedListener(tWatcher);
            }
        });
        holder.replace.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*").putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false).setAction(Intent.ACTION_GET_CONTENT);
            mContext.startActivityForResult(Intent.createChooser(intent,"Select Images"), 10000 + position);
        });

        holder.thumb.setOnClickListener(v -> {
            PopupMenu p = new PopupMenu(mContext, v);
            String[] options = {"CENTER", "TOP", "TOP | RIGHT", "RIGHT", "RIGHT|BOTTOM", "BOTTOM", "BOTTOM|LEFT", "LEFT", "LEFT|TOP"};
            int[] opts = {0, 2, 6, 4, 12, 8, 9, 1, 3};
            for(int i = 0; i<opts.length; i++){
                p.getMenu().add(0, opts[i], opts[i], options[i]);
            }
            p.setOnMenuItemClickListener(item -> {
                File f = new File(baseF + mContext.getString(R.string.arte_, 16, encode(position)));
                Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
                b = getCroppedBitmap(getScaledBitmap(b, 160, 160), item.getOrder());

                holder.thumb.setImageBitmap(b);

                try {
                    b.compress(Bitmap.CompressFormat.WEBP, 67, new FileOutputStream(
                            new File(baseF + mContext.getString(R.string.thumb_, 16, encode(position)))));
                    int width = b.getWidth(), height = b.getHeight();
                    int[] scales = {6, 8, 12};
                    for(int s:scales) {
                        Bitmap.createScaledBitmap(b, width * s / 16, height * s / 16, true)
                                .compress(Bitmap.CompressFormat.WEBP, 67, new FileOutputStream(
                                        new File(baseF + mContext.getString(R.string.thumb_, s, encode(position)))));
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                return false;
            });
            p.show();
        });

        try {
            holder.arteDescription.setText(arteDes.get(position));
        } catch (Exception ignored){}
        holder.arteDescription.setOnFocusChangeListener((v, hasFocus) -> {
                String s = holder.arteDescription.getText().toString();
                if(position >= arteDes.size()) {
                    for(int i = arteDes.size()-1; i <= position; i++) {
                        arteDes.add("");
                    }
                }
                arteDes.set(position, s);
                ContentValues values = new ContentValues();
                values.put("des", s);
                Log.d("arteUpdate", (position+1)+","+s + ", " +
                        MainActivity.mDb.update("arte", values, "_id=?", new String[]{(position + 1)+ ""}));
        });
        return convertView;
    }

    private void addArte(int position, Audio mAudio) {
        mAudio.arte = encode(position);
        mContext.mGrid.setAdapter(((UpdateActivity)mContext).mAudioAdapter);
        mContext.mSearch.removeTextChangedListener(tWatcher);
        ContentValues values = new ContentValues();
        values.put("arte", encode(position));
        /*if(mAudio.title.equals("__albs")){
            mDatabase.update(ALBUMS, values, "_id = " + mAudio.id, null);
            mContext.mAlbumAdapter.notifyDataSetChanged();
            mContext.mList.setAdapter(mContext.mAlbumAdapter);
        } else */
        if(mAudio.isAlbum && mAudio.id>0) {
            mDb.update("album", values, "_id = " + mAudio.id, null);
        } else if(mAudio.id>0) {
            mAudioDb.update("audio", values, "_id = " + mAudio.id, null);
        }
    }


    static Bitmap getCroppedBitmap(Bitmap bitmap, int pos) {
        if (bitmap == null) return null;
        Bitmap output;
        if (bitmap.getWidth() > bitmap.getHeight()) {
            output = Bitmap.createBitmap(bitmap.getHeight(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        } else {
            output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getWidth(), Bitmap.Config.ARGB_8888);
        }
        Canvas canvas = new Canvas(output);
        final int color = 0xff424242;
        final Paint paint = new Paint();
        int r;
        if (bitmap.getWidth() > bitmap.getHeight()) {
            r = bitmap.getHeight() / 2;
        } else {
            r = bitmap.getWidth() / 2;
        }
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        Rect rect = new Rect(bitmap.getWidth()/2 - r, bitmap.getHeight()/2 - r, bitmap.getWidth()/2 + r, bitmap.getHeight()/2 + r);
        final Rect destRect = new Rect(0, 0, 2*r, 2*r);
        canvas.drawCircle(r, r, r, paint);
        switch (pos){
            case LEFT:
                rect = new Rect(0, bitmap.getHeight()/2 - r, 2*r, bitmap.getHeight()/2 + r);
                break;
            case LEFT|TOP:
                rect = new Rect(0, 0, 2*r, 2*r);
                break;
            case TOP:
                rect = new Rect(bitmap.getWidth()/2-r, 0, bitmap.getWidth()/2 + r, 2*r);
                break;
            case TOP|RIGHT:
                rect = new Rect(bitmap.getWidth() - 2*r, 0, bitmap.getWidth(), 2 * r);
                break;
            case RIGHT:
                rect = new Rect(bitmap.getWidth() - 2*r, bitmap.getHeight()/2 - r, bitmap.getWidth(), bitmap.getHeight()/2 + r);
                break;
            case RIGHT|BOTTOM:
                rect = new Rect(bitmap.getWidth() - 2*r, bitmap.getHeight() - 2*r, bitmap.getWidth(), bitmap.getHeight());
                break;
            case BOTTOM:
                rect = new Rect(bitmap.getWidth()/2 - r, bitmap.getHeight() - 2*r, bitmap.getWidth()/2 + r, bitmap.getHeight());
                break;
            case BOTTOM|LEFT:
                rect = new Rect(0, bitmap.getHeight() - 2*r, 2*r, bitmap.getHeight());
                break;
            default:
        }
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, destRect, paint);
        return output;
    }
    static Bitmap getScaledBitmap(Bitmap bitmap, float w, float h) {
        if (bitmap == null) return null;
        float width = bitmap.getWidth(), height = bitmap.getHeight();
        if(height/h > 1) {
            if (height / h > width / w){
                height = height*(w / width);   width = w;
            } else {
                width = width*(h/height);   height = h;
            }
        }
        return Bitmap.createScaledBitmap(bitmap, (int)width, (int)height, true);
    }
    class ViewHolder{
        ImageView bg, thumb;
        Button replace;
        EditText arteDescription;
        ViewHolder(View v){
            bg = v.findViewById(R.id.bg);
            thumb = v.findViewById(R.id.thumb);
            replace = v.findViewById(R.id.replace);
            arteDescription = v.findViewById(R.id.description);
        }
    }
}
