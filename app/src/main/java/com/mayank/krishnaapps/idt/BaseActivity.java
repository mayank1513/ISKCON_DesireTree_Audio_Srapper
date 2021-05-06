package com.mayank.krishnaapps.idt;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Objects;

import static com.mayank.krishnaapps.idt.ArteAdapter.getCroppedBitmap;
import static com.mayank.krishnaapps.idt.ArteAdapter.getScaledBitmap;
import static com.mayank.krishnaapps.idt.ExportHelper.FetchWMA;
import static com.mayank.krishnaapps.idt.ExportHelper.encode;
import static com.mayank.krishnaapps.idt.ExportHelper.setExportInterface;
import static com.mayank.krishnaapps.idt.MainActivity.arteDes;
import static com.mayank.krishnaapps.idt.MainActivity.baseF;
import static com.mayank.krishnaapps.idt.MainActivity.mWakeLock;
import static com.mayank.krishnaapps.idt.UpdateActivity.TOP;

public abstract class BaseActivity extends Activity implements SyncedHorizontalScrollView.SyncedScrollInterface, ExportHelper.ExportInterface {
    ArrayList<SyncedHorizontalScrollView> syncedHorizontalScrollViews = new ArrayList<>();
    protected static final int RESULT_LOAD_IMAGE = 0, PICKFILE_RESULT_CODE = 1;
    public GridView mGrid;
    ArteAdapter mArteAdapter;
    ProgressBar mProgressBar;
    TextView mStatusText;
    EditText mSearch;
    View mSearchContainer;
    TextWatcher textWatcher;
    static SQLiteDatabase mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGrid = findViewById(R.id.grid);
        mProgressBar = findViewById(R.id.progress);
        mStatusText = findViewById(R.id.status);
        setExportInterface(this);
    }

    public void scrollAll(int x, int y){
        for(SyncedHorizontalScrollView s:syncedHorizontalScrollViews)
            s.scrollTo(x, y);
    }
    @SuppressLint("StaticFieldLeak")
    public class saveImages extends AsyncTask<Intent, Integer, Integer> {
        int requestCode;

        saveImages(int requestCode) {
            this.requestCode = requestCode;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mStatusText.setVisibility(View.VISIBLE);
            mStatusText.setText(R.string.processing_images);
        }

        @Override
        protected Integer doInBackground(Intent... intents) {
            Intent data = intents[0];
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount(); //evaluate the count before the for loop --- otherwise, the count is evaluated every loop.
                for (int i = 0; i < count; i++) {
                    Uri selectedImage = data.getClipData().getItemAt(i).getUri();
                    saveRequestedImage(selectedImage, requestCode);
                }
            } else if (data.getData()!=null) {
                Uri selectedImage = data.getData();
                saveRequestedImage(selectedImage, requestCode);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            mGrid.setAdapter(mArteAdapter = new ArteAdapter(BaseActivity.this, mArteAdapter.mAudio));
            mProgressBar.setVisibility(View.GONE);
            mStatusText.setVisibility(View.GONE);
            mGrid.setSelection(mArteAdapter.getCount());
        }
    }
    private void saveRequestedImage(Uri selectedImage, int requestCode) {
        File f = new File(baseF + "/export/a/x16");
        int count = f.listFiles().length/2;
        if(requestCode != RESULT_LOAD_IMAGE )
            count = requestCode - 10000;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, options);
            options.inSampleSize = calculateInSampleSize(options, 2880, 2880);
            options.inJustDecodeBounds = false;

            Bitmap b = BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, options);
            Objects.requireNonNull(b).compress(Bitmap.CompressFormat.WEBP, 75, new FileOutputStream(
                    new File(baseF + getString(R.string.arte_, 16, encode(count)))));
            b.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(
                    new File(baseF + "/export/a/w/a_" + encode(count) + ".png")));

            Bitmap b1 = getCroppedBitmap(getScaledBitmap(b, 160, 160), TOP);
            b1.compress(Bitmap.CompressFormat.WEBP, 50, new FileOutputStream(
                    new File(baseF + getString(R.string.thumb_, 16, encode(count)))));
            b1.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(
                    new File(baseF + "/export/a/w/" + encode(count) + ".png")));

            int width = b.getWidth(), height = b.getHeight();
            int width1 = b1.getWidth(), height1 = b1.getHeight();
            int[] scales = {6, 8, 12};
            for(int s:scales){
                Bitmap.createScaledBitmap(b,width*s/16, height*s/16, true)
                        .compress(Bitmap.CompressFormat.WEBP, 75, new FileOutputStream(
                                new File(baseF + getString(R.string.arte_, s, encode(count)))));
                Bitmap.createScaledBitmap(b1,width1*s/16, height1*s/16, true)
                        .compress(Bitmap.CompressFormat.WEBP, 50, new FileOutputStream(
                                new File(baseF + getString(R.string.thumb_, s, encode(count)))));
            }
            if (requestCode == RESULT_LOAD_IMAGE) {
                arteDes.add(" ");
                ContentValues values = new ContentValues();
                values.put("_id", arteDes.size());
                values.put("des", " ");
                MainActivity.mDb.insert("arte", null, values);
            }
        } catch (Exception e) {
            e.printStackTrace();
            display(e.getMessage());
        }
    }
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 108 && resultCode==RESULT_OK){
            final String shortName = data.getStringExtra("shortName");
            File f = new File(getExternalFilesDir("files") +
                    "/com/" + shortName + "/s"),
                    f1 = new File(baseF + "/export/" + shortName);
            Log.e("hari", "f = "+f);
            Log.e("hari", "f1 = "+f1);
            String[] s = {"a", "b", "m", "l", "h", "p", "i"};
            for(String s1:s){
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(f1, s1))));
                    String line;
                    StringBuilder sb = new StringBuilder();
                    while ((line=reader.readLine())!=null){
                        sb.append("\n").append(line);
                    }
                    new FileOutputStream(new File(f, s1)).write(sb.toString().substring(1).getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("hari",e.getMessage());
                }
            }
            startActivity(getPackageManager().getLaunchIntentForPackage("com.mayank.krishnaapps.shravanotsava")
                    .putExtra("s", shortName));
            finish();
        } else if (resultCode == RESULT_OK && null != data) {
            (new saveImages(requestCode)).execute(data);
        }
    }
    @Override
    public void display(final String s) {
        runOnUiThread(() -> {
            mStatusText.setVisibility(View.VISIBLE);
            mStatusText.setText(s);
        });
    }

    @Override
    public void fetchWMA(final int totalSize, final String expList, final String dbName) {
        runOnUiThread(() -> {
            if(expList.isEmpty()){
                (new AlertDialog.Builder(BaseActivity.this)).setTitle("Download and Convert WMA files")
                        .setPositiveButton("Fetch", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                FetchWMA(dbName);
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(mWakeLock.isHeld()) mWakeLock.release();
                        hideProgressBar();
                        testDb(dbName);
//                            finish();
                    }
                }).show();
            } else if(totalSize > 10) {
                (new AlertDialog.Builder(BaseActivity.this)).setTitle("Download and Convert WMA files")
                        .setMessage("Estimated download size = " + (totalSize/1024/1024) + "MB")
                        .setPositiveButton("Download and Convert to MP3", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = getPackageManager().getLaunchIntentForPackage("com.mayank.krishnaapps.audioeditor");
                                Objects.requireNonNull(intent).putExtra("dbName", dbName)
                                        .putExtra("expList", expList).putExtra("convert", true);
                                if(mWakeLock.isHeld()) mWakeLock.release();
                                startActivity(intent);
                                finish();
                            }
                        }).setNeutralButton("Download", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = getPackageManager().getLaunchIntentForPackage("com.mayank.krishnaapps.audioeditor");
                        Objects.requireNonNull(intent).putExtra("dbName", dbName)
                                .putExtra("expList", expList).putExtra("convert", false);
                        if(mWakeLock.isHeld()) mWakeLock.release();
                        startActivity(intent);
                        finish();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(mWakeLock.isHeld()) mWakeLock.release();
                        hideProgressBar();
                        testDb(dbName);
//                            finish();
                    }
                }).setCancelable(false).show();
            }
        });
    }

    public void testDb(final String dbName) {
        new AlertDialog.Builder(BaseActivity.this).setMessage("Test in Shravanotsava App")
                .setPositiveButton("Test", (dialog, which) -> startActivityForResult(new Intent().setAction("shravanotsava.delete")
                        .putExtra("shortName", dbName), 108)).setNeutralButton("Open Termux", (dialog, which) -> {
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("label", "cd ~/storage/shared/Android/data/com.mayank.krishnaapps.idt/files/IDT/export/" + dbName +
                                    "\ngit add .\ngit commit -m hari\ngit push origin master\n");
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(BaseActivity.this, "Termux initial commands copied:\n\n", Toast.LENGTH_LONG).show();
                            startActivity(getPackageManager().getLaunchIntentForPackage("com.termux"));
                            finish();
                        }).setNegativeButton("Cancel", null).show();
    }

    @Override
    public void showProgressBar() {
        runOnUiThread(() -> mProgressBar.setVisibility(View.VISIBLE));
    }

    @Override
    public void hideProgressBar() {
        runOnUiThread(() -> mProgressBar.setVisibility(View.GONE));
    }
}
