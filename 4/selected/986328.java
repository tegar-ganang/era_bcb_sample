package net.sf.pulse.android;

import java.util.Set;
import java.util.TreeSet;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

public class SourceView extends ViewGroup implements Comparable {

    private final Set<ChannelView> channels = new TreeSet<ChannelView>();

    public SourceView(Context context) {
        super(context);
    }

    public SourceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SourceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private Set<ChannelView> getChannels() {
        return channels;
    }

    @Override
    public int compareTo(Object another) {
        return 0;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    }
}
