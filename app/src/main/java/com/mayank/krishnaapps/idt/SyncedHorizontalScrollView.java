package com.mayank.krishnaapps.idt;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;

public class SyncedHorizontalScrollView extends HorizontalScrollView {
    SyncedScrollInterface syncedScrollInterface;

    public SyncedHorizontalScrollView(Context context){
        super(context);
        syncedScrollInterface = (SyncedScrollInterface)context;
        ((BaseActivity)context).syncedHorizontalScrollViews.add(this);
    }

    public SyncedHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        syncedScrollInterface = (SyncedScrollInterface)context;
        ((BaseActivity)context).syncedHorizontalScrollViews.add(this);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        syncedScrollInterface.scrollAll(l,t);
        super.onScrollChanged(l, t, oldl, oldt);
    }

    public interface SyncedScrollInterface{
        void scrollAll(int x, int y);
    }
}
