package com.mayank.krishnaapps.idt;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.Html;
import android.util.Log;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mayank.krishnaapps.idt.ExportHelper.encode;
import static com.mayank.krishnaapps.idt.InspectActivity.placesInDb;
import static com.mayank.krishnaapps.idt.InspectActivity.regXTORemoveFromAll;
import static com.mayank.krishnaapps.idt.InspectActivity.stems;
import static com.mayank.krishnaapps.idt.MainActivity.Lyrics;
import static com.mayank.krishnaapps.idt.MainActivity.LyricsIds;
import static com.mayank.krishnaapps.idt.MainActivity.bookPattern;
import static com.mayank.krishnaapps.idt.MainActivity.dateFormats;
import static com.mayank.krishnaapps.idt.MainActivity.datePattern;
import static com.mayank.krishnaapps.idt.MainActivity.format;
import static com.mayank.krishnaapps.idt.MainActivity.places;
import static com.mayank.krishnaapps.idt.MainActivity.specialPlaces;
import static com.mayank.krishnaapps.idt.albSql.getAlbum;

public class Audio {
    boolean isAlbum = false;
    public long id = -1;
    String title;
    public long parent;
    String url = "";
    public String arte = "";
    String date = "";
    String newLu = "";
    //    audio
    long size;
    String ref;
    int place = -1;
    //    album
    int lang;
    int replacement = -1;

//    private final static int song = 0, bg = 1, sb = 2, cc = 3, nod = 4; //{adi, ... = 4, cc_antya = 5;
//    private final static String[][] bookName = {{""}, {"BG ", "Bhagavad Gita "}, {"SB ", "Srimad Bhagavatam "}, {"CC ", "Chaitanya Charitamrita ", "Caitanya Caritamrita "}, {"NOD"}};

    public void copy(Audio other) {
        isAlbum = other.isAlbum;
        this.id = other.id;
        this.arte = other.arte;
        this.parent = other.parent;
        this.size = other.size;
        title = other.title;
        this.url = other.url;
        this.place = other.place;
        this.date = other.date;
        this.ref = other.ref;
        this.lang = other.lang;
    }

    private static int yr = Calendar.getInstance().get(Calendar.YEAR);

    private void setDate(SQLiteDatabase db) {
        Date tempDate = null;
        for (int i = 0; i < dateFormats.length; i++) {
            Matcher m = datePattern[i].matcher(title);
            if (m.find()) {
                try {
                    String dateStr = m.group();
                    tempDate = format[i].parse(dateStr);
                    Calendar c = Calendar.getInstance();
                    c.setTime(tempDate);
                    if (c.get(Calendar.YEAR) < 1900 || c.get(Calendar.YEAR) > yr)
                        continue;
                    int pos;
                    if ((pos = title.indexOf(dateStr)) > 5) {
                        String p = title.substring(pos).replace(dateStr, "").trim();
                        if (place < 0 && !p.isEmpty()) {
                            if (placesInDb.contains(p)) {
                                place = placesInDb.indexOf(p) + 1;
                                title = title.replace(p, "");
                            }
                            if (place < 0 && places.contains(p)) {
                                ContentValues values1 = new ContentValues();
                                values1.put("place", p);
                                if (db.insert("places", null, values1) >= 0) {
                                    placesInDb.add(p);
                                    place = placesInDb.indexOf(p) + 1;
                                    title = title.replace(p, "")
                                            .replace(" {2,}", " ").trim();
                                }
                            }
                        }
                    }
                    title = title.replace(dateStr, "").trim();
                    if (title.isEmpty()) title = "Lecture";
                    if (title.trim().equals("Lecture"))
                        title = title + (place < 0 ? "" : " at " + places.get(place));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        if (tempDate != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(tempDate);
            if (c.get(Calendar.YEAR) > 1900 && c.get(Calendar.YEAR) < yr)
                this.date = format[0].format(tempDate);
        }
    }

    void setFromUrl(SQLiteDatabase db, boolean isBhajan, final Audio audio, boolean removeParent) {
        int replacement = audio.replacement;
        String url = this.url.trim();
        if (replacement > 0)
            url = url.replaceAll("#", stems.get(replacement - 1));
        int b = url.toLowerCase().substring(url.lastIndexOf('.') + 1).length();
        if (!(url.contains(".") && b > 2 && b < 5)) {
            isAlbum = true;
            String s = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
            title = Html.fromHtml(s.replaceAll("_", " ").trim()).toString();
            if (removeParent)
                title = title.replaceAll("^" + audio.title.trim(), "");
            hari();
            return;
        }
        title = Html.fromHtml(url.substring(0, url.length() - 4).replaceAll("_", " ")).toString();
        if (removeParent)
            title = title.replaceAll("^" + audio.title.trim(), "");
        hari();
        date = "";
        place = -2;
        if (!isBhajan) this.ref = null;
        setRef(isBhajan);
        setDate(db);
        title = title.replace("IDesireTree", "")
                .replace("Radhanath Sw ", "")
                .replace(" {2,}", " ").trim();
        setPlace(db);
        do {
            touchUp(isBhajan);
        } while (title.matches("^[\\-.\\s]*") || title.endsWith("-") || title.startsWith("0"));
    }

    void hari() {
        title = (" " + title + " ").replaceAll("Gita", "Gītā")
                .replaceAll("(?i)(Srimad|Shrimad)", "Śrīmad")
                .replaceAll("Bhagavat(h)?(am|a)?", "Bhāgavatam")
                .replaceAll("Chaitanya", "Caitanya")
                .replaceAll("(?i) Lila", " Līlā")
                .replaceAll("(?i) Adi", " Ādī")
                .replaceAll("(?i)CC A ", "CC Ādī ")
                .replaceAll("(?i)CC M ", "CC Madhya ")
                .replaceAll("Chapter[\\s-]?0?", "Chapter ")
                .replaceAll("Canto[\\s-]?0?", "Canto ")
                .replaceAll("(?i) Ar(a)?ti", " Ārati")
                .replaceAll("(?i)(Krishna|Krsna)", "Kṛṣṇa")
                .replaceAll("(?i)Vr(i)?ndavan(a)?", "Vṛndāvana")
                .replaceAll("Radha", "Rādhā")
                .replaceAll("Jayapataka", "Jayapatākā")
                .replaceAll("(Rasamrit(a)?)", "Rasāmṛta")
                .replaceAll("(Radheyshyam)", "Rādheyshyam")
                .replaceAll("(Gopinath)", "Gopināth")
                .replaceAll("(Swami)", "Swāmi")
                .replaceAll("(Prabhupada)", "Prabhupāda")
                .replaceAll("vais(h)?nav(a)?", "vaiṣṇava")
                .replaceAll("Vais(h)?nav(a)?", "Vaiṣṇava")
                .replaceAll("vedant(a)?", "vedānta")
                .replaceAll("Sri", "Śrī")
                .replaceAll("(?i)prer(a)?na", "Preraṇa")
                .replaceAll("(?i)chet(a)?na", "Chetana")
                .replaceAll("(?i)is(h)?opanis(h)?ad(a)?", "Īśopaniṣada")
                .replaceAll("%20|%e2|%80|%|%93|'|=", " ")
                .replaceAll(" BOM ", " Bombay ")
                .replaceAll(" HON ", " Honolulu ")
                .replaceAll(" LA ", " Los Angeles ")
                .replaceAll(" MAY ", " Mayapur ")
                .replaceAll(" DAL ", " Dallas ")
                .replaceAll(" MEL ", " Melbourne ")
                .replaceAll(" GOR ", " Gorakhpur ")
                .replaceAll(" SF ", " San Francisco ")
                .replaceAll(" NDEL ", " New Delhi ")
                .replaceAll(" TOK ", " Tokyo ")
                .replaceAll(" AHM ", " Ahmedabad ")
                .replaceAll(" ALI ", " Aligarh ")
                .replaceAll(" ATL ", " Atlanta ")
                .replaceAll(" AUC ", " Auckland ")
                .replaceAll(" CHA ", " Chandigarh ")
                .replaceAll(" COL ", " Columbus ")
                .replaceAll(" HYD ", " Hyderabad ")
                .replaceAll(" LON ", " London ")
                .replaceAll(" MEX ", " Mexico ")
                .replaceAll(" DEN ", " Denver ")
                .replaceAll(" DET ", " Detroit ")
                .replaceAll(" VRN ", " Vṛndāvana ")
                .replaceAll("Addr Addr", "Address")
                .replaceAll(" (Arrival|Arr) (A2|AD|AR) ", " Arrival Address ")
                .replaceAll(" Departure (DP|Address) ", " Arrival Address ")
                .replaceAll("C(h)?arit(h)?amr(i)?(tha|ta|t)", "Caritāmṛta").trim();

        if (title.replaceFirst("Śrīmad Bhāgavatam", "").contains("Śrīmad Bhāgavatam"))
            title = title.replaceFirst("Śrīmad Bhāgavatam", "");
    }

    void setFromTitle(SQLiteDatabase database, boolean isBhajan) {
//        title = title.replaceAll(audio.title, "");
        Log.w("hari", "setFromTitle");
        hari();
        if (title.startsWith(")")) title = title.substring(1).replace("(", " (").trim() + ")";
        String s = title.startsWith("(") ? title.substring(0, title.indexOf(")") + 1) : "";
        title = title.replace(s, "").trim();
        if (isAlbum) {
            Log.e("hari", "returning from setfromtitle");
            return;
        }
        if (null == ref || ref.isEmpty()) setRef(isBhajan);
        if (null == date || date.isEmpty()) setDate(database);
        if (place < 0) setPlace(database);
        Log.w("hari", "setFromTitle --------");
        do {
            touchUp(isBhajan);
        } while (title.matches("^[\\-.0-9]") || title.endsWith("-"));
        if (title.trim().length() < 5 || title.equals("Lecture") || title.matches("^(?i)Kirtan(a)?$"))
            title = (isBhajan ? "Kirtana" : "Lecture" + (ref != null && !ref.isEmpty() ? " on " +
                    ref.toUpperCase().replaceFirst("\\.", " ") : "")) + (place <= 0 ? "" : " at " + placesInDb.get(place - 1));
        if (title.trim().equals("Arrival Address"))
            title = title + (place <= 0 ? "" : " - " + placesInDb.get(place - 1)) + (ref != null && !ref.isEmpty() ? "("
                    + ref.toUpperCase().replaceFirst("\\.", " ") : "");
        title = title + " " + s;
        if (title.startsWith("(")) title = title.substring(1).replace(")", " ");
        if (title.startsWith(")")) {
            title = title.substring(1).replace("(", " (").trim();
            setFromTitle(database, isBhajan);
        }
    }

    private void setPlace(SQLiteDatabase database) {
        String t = " " + this.title + " ", p1 = "", p2 = "";
        if (place < 0) {
            for (String p : places) {
                p = " " + p;
                if (t.toLowerCase().contains(("iskcon" + p).toLowerCase())) {
                    p2 = ("ISKCON" + p).trim();
                } else if (t.toLowerCase().replaceAll("-", " ").contains(p.toLowerCase())) {
                    if (specialPlaces.contains(p.trim())) {
                        t = t.replaceAll(p, "");
                        p1 = p.trim();
                        continue;
                    }
                    p2 = p.trim();
                    break;
                }
            }

            String p = "";
            if (!p2.isEmpty())
                p = p2;
            else if (!p1.isEmpty())
                p = p1;

            if (!p.isEmpty()) {
                ContentValues values1 = new ContentValues();
                values1.put("place", p);
                if (!placesInDb.contains(p) && database.insert("places", null, values1) >= 0) {
                    placesInDb.add(p);
                }
                place = placesInDb.indexOf(p) + 1;
                if (!p2.isEmpty())
                    this.title = this.title.replaceFirst("(?i)" + p2, "")
                            .replaceAll(" {2,}", " ").trim();
            }
        }
    }

    private void setRef(boolean isBhajan) {
        if (isBhajan) {
            for (int i = 0; i < Lyrics.size(); i++) {
                title = title.replaceAll("Jaye |Jay ", "Jaya ")
                        .replaceAll("Radhe(y)?", "Rādhe")
                        .replaceAll("naam(a)?", "nāma")
                        .replaceAll("Pra(a)?n(a)?", "prāṇa")
                        .replaceAll("pra(a)?n(a)?", "Prāṇa")
                        .replaceAll("Kundala", "Kuṇḍala")
                        .replaceAll("(?i)Jinka", "Jinaka")
                        .replaceAll("Vis(h)?al(a)?", "Viśāla")
                        .replaceAll("(?i)Yas(h)?omati", "Yaśomatī")
                        .replaceAll("(?i)sunder(a)?", "Sundara")
                        .replaceAll("(?i)vi(ba)?bha[vb](a)?ri(\\s)?s(h)?es(ha|a)?", "Vibhavari Śeṣa");
                String s = title.toLowerCase().replaceAll("[0-9\\-]+", "").replaceAll("(krishna|krsna)", "kṛṣṇa");
                String s1 = Lyrics.get(i).toLowerCase().replaceAll("a ", " ").replaceFirst("a$", "");
                String s2 = s.replaceAll("a ", " ").replaceFirst("a$", "").trim();
                if (s.matches(".*" + Lyrics.get(i).toLowerCase().replaceAll("śrī ", "").trim() + ".*")
                        || Lyrics.get(i).toLowerCase().contains(s.trim())
                        || s2.matches(".*" + s1.replaceAll("śrī ", "").trim() + ".*")
                        || s1.contains(s2)) {
                    ref = encode(LyricsIds.get(i));
                }
            }
        } else {
            for (Pattern aBookPattern : bookPattern) {
                Matcher m = aBookPattern.matcher(title);
                if (m.find()) {
                    ref = m.group().toLowerCase()
                            .replaceAll("(canto|chapter|ch|text|to|līlā|lila|mantra|reading)", " ")
                            .replaceAll("(bhagavad([\\-\\s])*)?gītā", "bg")
                            .replaceAll("(śrīmad[\\-\\s]?)?bhāgavatam", "sb")
                            .replaceAll("(śrī([\\-\\s])*)?caitanya caritāmṛta", "cc")
                            .replaceAll("(al|ādī)", "adi")
                            .replaceAll("kṛṣṇa book([\\-\\s]*dict)?", "kb")
                            .replaceAll("ml", "madhya")
                            .replaceAll("nectar of devotion", "nod")
                            .replaceAll("nectar of instruction(s)?", "noi")
                            .replaceAll("(śrī )?īśopaniṣad(a)?", "iso").trim()
                            .replaceAll("(\\s+|-)", ".");

                    String[] rr = ref.split("\\.");
                    StringBuilder sb = new StringBuilder();
                    for (String r : rr) {
                        try {
                            int i = Integer.parseInt(r.trim());
                            r = i == 0 ? "" : i + "";
                        } catch (Exception ignored) {
                        }
                        if (!r.isEmpty())
                            sb.append(".").append(r);
                    }
                    ref = sb.toString().substring(1);
                    String replacement = m.group().replaceAll("(?i)(canto|chapter|ch|text|to|" +
                            "līlā|mantra|(bhagavad )?gītā|(śrīmad)? bhāgavatam|(śrī)? caitanya caritāmṛta|" +
                            "nectar of (devotion|instruction(s)?)|(śrī)? īśopaniṣada)", "").trim();
                    title = title.replace(replacement, "").trim();
                    break;
                }
            }
        }
    }

    private void touchUp(boolean isBhajan) {
        title = title.replaceAll("(?i)(HHRNS(M)?|RNS|Amrit Droplets|Bhajans -|Various -|IDT)", "").trim();

        for (String s : regXTORemoveFromAll)
            title = title.replaceAll(s, "");

        title = title.replaceAll("%20", " ");
        title = title.replaceFirst("^[0-9\\-\\s.]+", "").trim();
        title = title.replaceFirst(" NV", "").trim();

        if (title.endsWith("-"))
            title = title.substring(0, title.length() - 1).trim();
        if (title.isEmpty() || title.equals("Lecture") || title.matches("^(?i)Kirtan(a)?$"))
            title = (isBhajan ? "Kirtana" : "Lecture" + (ref != null && !ref.isEmpty() ? " on " +
                    ref.toUpperCase().replaceFirst("\\.", " ") : "")) + (place <= 0 ? "" : " at " + placesInDb.get(place - 1));
    }

    String getUrl(SQLiteDatabase db) {
        if (url.startsWith("http://")) url = url.replace("http://", "https://");
        String u = isAlbum ? url + "/" : url;
        if (!u.matches("http(s)?://audio.iskcondesiretree.com.*")) {
            Cursor c = db.rawQuery("select * from album where _id = ?", new String[]{parent + ""});
            c.moveToFirst();
            Audio parentA = getAlbum(c);
            if (parentA.replacement > 0)
                u = u.replaceAll("#", stems.get(parentA.replacement - 1));
            c.close();
            u = parentA.getUrl(db) + u;
        }
        u = u.replaceAll("&amp;", "%26");
        Log.d("getUrl", u);
        return u;
    }

    static String findStem(ArrayList<Audio> audios, int ind) {
        int n = audios.size();

        String s = audios.get(1).url;
        if (ind >= 0 && !stems.isEmpty())
            s = s.replaceAll("#", stems.get(ind));
        int len = s.length();

        String res = "";

        for (int i = 0; i < len; i++) {
            for (int j = i + 1; j <= len; j++) {
                // generating all possible substrings
                // of our reference string arr[0] i.e s
                String stem = s.substring(i, j);
                int k;

                if (ind < 0) {
                    for (k = 2; k < n; k++)
                        if (!audios.get(k).url.contains(stem))
                            break;
                } else {
                    for (k = 2; k < n; k++)
                        if (!audios.get(k).url.replaceAll("#", stems.get(ind)).contains(stem))
                            break;
                }
                // If current substring is present in
                // all strings and its length is greater
                // than current result
                if (k == n && res.length() < stem.length())
                    res = stem;
            }
        }
        return res;
    }
}
