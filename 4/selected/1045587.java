package com.busfm.widget;

import java.util.HashMap;
import com.busfm.R;
import com.busfm.activity.HomeActivity.ChannelAdapter;
import com.busfm.model.ChannelEntity;
import com.busfm.util.Constants;
import com.busfm.util.LogUtil;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FlingTab extends LinearLayout {

    private int top, bottom;

    private LayoutInflater mInflater;

    private OnItemClickListener clickListener;

    private int defaultTab;

    private boolean move = false;

    private HashMap<Integer, Integer[]> childPointCache = new HashMap<Integer, Integer[]>();

    int childWidth = 0;

    private int gleft, currentwidth, currentleft;

    private Integer[] current = { 0, 0 };

    private Drawable tabpictrue;

    private int tabpicpadding;

    private int moveunit;

    private int duration;

    private int height;

    private View myCurrentView;

    private View myPreView;

    public FlingTab(Context context, AttributeSet attrs) {
        super(context, attrs);
        mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FlingTab);
        tabpictrue = a.getDrawable(R.styleable.FlingTab_tabpictrue);
        if (tabpictrue == null) {
            tabpictrue = context.getResources().getDrawable(R.drawable.channel_text_bg);
        }
        tabpicpadding = a.getDimensionPixelSize(R.styleable.FlingTab_tabpicpadding, 20);
        moveunit = a.getDimensionPixelSize(R.styleable.FlingTab_moveunit, 5);
        duration = a.getInt(R.styleable.FlingTab_duration, 10);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isDrawItem()) return;
        tabpictrue.setBounds(current[0] + tabpicpadding, ((bottom - top) - height) / 2, current[0] + currentwidth - tabpicpadding, ((bottom - top) - height) / 2 + height);
        tabpictrue.draw(canvas);
        if (currentleft == current[0] && myCurrentView != null && !move) {
            if (myCurrentView instanceof TextView) {
                LogUtil.i(Constants.TAG, "Ondraw:");
                ((TextView) myCurrentView).setTextColor(getResources().getColor(R.color.white));
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        top = t;
        bottom = b;
        int count = getChildCount();
        if (0 == count) return;
        int w = r - l;
        childWidth = w / count;
        View v = null;
        Integer[] points = null;
        for (int i = 0; i < count; i++) {
            v = getChildAt(i);
            height = v.getHeight();
            points = scalcChildPoint(v, i);
            v.layout(points[0], ((bottom - top) - v.getHeight()) / 2, points[1], ((bottom - top) - v.getHeight()) / 2 + v.getHeight());
            v.setTag(new Integer(i));
            v.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (!move) {
                        move = true;
                        itemOnclick(v, ((Integer) v.getTag()).intValue());
                    }
                }
            });
        }
        View mDefaultTab = getChildAt(defaultTab);
        if (null != mDefaultTab) {
            itemOnclick(mDefaultTab, defaultTab);
        }
    }

    public synchronized void update(View v, int position) {
        currentwidth = v.getWidth();
        if (gleft == current[0]) {
            move = false;
            return;
        }
        defaultTab = position;
        final boolean pathleft = gleft > current[0] ? true : false;
        final int num = Math.abs((gleft - current[0]) / moveunit);
        int i = 0;
        while (i < num) {
            if (pathleft) {
                gleft = gleft - moveunit;
                current[0] = gleft;
            } else {
                gleft = gleft + moveunit;
                current[0] = gleft;
            }
            try {
                Thread.sleep(duration);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            postInvalidate();
            i++;
        }
        if (gleft != currentleft) {
            current[0] = currentleft;
            postInvalidate();
        }
        gleft = current[0];
        move = false;
    }

    /**
     * 当点击一项时,移动背景坐标
     * 
     * @param v
     */
    public synchronized void itemOnclick(final View v, final int position) {
        Log.i(Constants.TAG, "itemOnclick position:" + position);
        myPreView = myCurrentView;
        if (myPreView != null) {
            ((TextView) myPreView).setTextColor(getResources().getColor(R.color.channel_text_color));
        }
        myCurrentView = null;
        myCurrentView = v;
        clickListener.onItemClickListener(v, ((Integer) v.getTag()).intValue());
        current = childPointCache.get(new Integer(position));
        currentleft = current[0];
        new Thread(new Runnable() {

            @Override
            public void run() {
                update(v, position);
            }
        }).start();
    }

    /**
     * 设置数据适配器
     * 
     * @param adapter
     */
    public void setAdapter(ChannelAdapter adapter) {
        Log.i("ScrcoolTab", "setAdapter");
        if (null != adapter && adapter.getChannelEntity() != null) {
            View view = null;
            for (ChannelEntity entity : adapter.getChannelEntity()) {
                view = mInflater.inflate(R.layout.gallery_item_channel, this, false);
                if (view instanceof TextView) {
                    ((TextView) view).setText(entity.getCNAME());
                }
                this.addView(view);
            }
        }
    }

    /**
     * 计算每个子控件的坐标
     * 
     * @param view
     * @param position
     * @return
     */
    public Integer[] scalcChildPoint(View view, int position) {
        Integer[] points = new Integer[2];
        points[0] = childWidth * position;
        points[1] = points[0] + view.getWidth();
        childPointCache.put(new Integer(position), points);
        return points;
    }

    /**
     * 是否画背景图片taab
     * 
     * @return
     */
    private boolean isDrawItem() {
        if (current[0] >= 0) return true; else return false;
    }

    /**
     * 设置默认选中
     * 
     * @param tab
     */
    public void setDefaultTab(int tab) {
        this.defaultTab = tab;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    /**
     * 得到当前选中的项
     * 
     * @return
     */
    public int getFocus() {
        return defaultTab;
    }

    /**
     * 当点一项时 调用事件
     * 
     * @param clickListener
     */
    public void setOnItemClickListener(OnItemClickListener clickListener) {
        this.clickListener = clickListener;
    }

    /**
     * 事件接口
     * 
     */
    public interface OnItemClickListener {

        void onItemClickListener(View v, int position);
    }
}
