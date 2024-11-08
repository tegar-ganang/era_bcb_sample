package com.timenes.clips.model;

import com.timenes.clips.model.utils.Utils;
import com.timenes.clips.platform.Platform;
import com.timenes.clips.platform.model.ModelObject;
import com.timenes.clips.platform.utils.List;

/**
 * @author helge@timenes.com
 *
 */
public class Track extends ModelObject {

    private List<Clip> clips;

    private String title;

    private int channel;

    private int instrument;

    public Track() {
        clips = Platform.getUtilsFactory().createList(Clip.class);
        title = "Untitled";
    }

    /**
	 * @return the rythmInstances
	 */
    public List<Clip> getClips() {
        return clips;
    }

    /**
	 * @param rythmInstances the rythmInstances to set
	 */
    public void setClips(List<Clip> rythmInstances) {
        this.clips = rythmInstances;
    }

    /**
	 * @return the title
	 */
    public String getTitle() {
        return title;
    }

    /**
	 * @param title the title to set
	 */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
	 * @return the channel
	 */
    public int getChannel() {
        return channel;
    }

    /**
	 * @param channel the channel to set
	 */
    public void setChannel(int channel) {
        this.channel = channel;
    }

    /**
	 * @return the instrument
	 */
    public int getInstrument() {
        return instrument;
    }

    /**
	 * @param instrument the instrument to set
	 */
    public void setInstrument(int instrument) {
        this.instrument = instrument;
    }

    @Override
    public ModelObject copy() {
        return Utils.copy(this);
    }
}
