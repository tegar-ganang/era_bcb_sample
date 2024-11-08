package be.lassi.control;

import java.util.ArrayList;
import java.util.List;
import be.lassi.base.Listener;
import be.lassi.context.ShowContext;
import be.lassi.cues.CueChannelLevel;
import be.lassi.cues.CueSubmasterLevel;
import be.lassi.cues.Cues;
import be.lassi.cues.LightCueDetail;
import be.lassi.util.Util;

public class SheetControl {

    private static final int FOCUS_NONE = 0;

    private static final int FOCUS_STAGE = 1;

    private static final int FOCUS_CHANNEL = 2;

    private static final int FOCUS_SUBMASTER = 3;

    private int focus = FOCUS_NONE;

    private final ShowContext context;

    private List<LevelControl> levelControls = new ArrayList<LevelControl>();

    private int currentRange = -1;

    private int currentCueIndex = -1;

    private int currentChannelIndexes[] = new int[0];

    private final LevelControlListener levelControlListener = new LevelControlListener();

    public SheetControl(final ShowContext context) {
        this.context = context;
    }

    public void setLevelControls(final List<LevelControl> levelControls) {
        for (LevelControl levelControl : this.levelControls) {
            levelControl.getLevelHolder().remove(levelControlListener);
        }
        this.levelControls = new ArrayList<LevelControl>(levelControls);
        for (LevelControl levelControl : levelControls) {
            levelControl.getLevelHolder().add(levelControlListener);
        }
    }

    public void setFocusNone() {
        if (focus != FOCUS_NONE) {
            deassign();
        }
        focus = FOCUS_NONE;
    }

    /**
     * 
     * 
     * 
     * @param cueIndex
     * @param rowIndex the index of the channel row in the sheet table
     * @param channellIndexes
     */
    public void setFocusChannel(final int cueIndex, final int rowIndex, final int[] channelIndexes) {
        StringBuilder b = new StringBuilder();
        b.append("channelIndexes: ");
        for (int i = 0; i < channelIndexes.length; i++) {
            b.append(i);
            b.append("/");
            b.append(channelIndexes[i]);
            b.append(" ");
        }
        Util.debug(b.toString());
        if (focus != FOCUS_CHANNEL) {
            deassign();
        }
        focus = FOCUS_CHANNEL;
        int newRange = rowIndex / levelControls.size();
        if (currentRange != newRange || currentCueIndex != cueIndex) {
            deassignChannels();
            currentRange = newRange;
            currentCueIndex = cueIndex;
            currentChannelIndexes = channelIndexes;
            int start = currentRange * levelControls.size();
            LightCueDetail detail = getDetail(currentCueIndex);
            for (int i = 0; i < levelControls.size(); i++) {
                LevelControl levelControl = levelControls.get(i);
                int index = start + i;
                if (index < channelIndexes.length) {
                    int channelIndex = channelIndexes[index];
                    CueChannelLevel level = detail.getChannelLevel(channelIndex);
                    level.setLevelControl(levelControl);
                    levelControl.getLevelHolder().setValue(level.getChannelIntValue(), levelControlListener);
                } else {
                    levelControl.setLevel(0);
                }
            }
        }
    }

    public void setFocusSubmaster(final int cueIndex, final int submasterIndex) {
        Util.debug("setFocusSubmaster(cueIndex=" + cueIndex + ", submasterIndex=" + submasterIndex + ")");
        if (focus != FOCUS_SUBMASTER) {
            deassign();
        }
        focus = FOCUS_SUBMASTER;
        int newRange = submasterIndex / levelControls.size();
        if (currentRange != newRange || currentCueIndex != cueIndex) {
            deassignSubmasters();
            currentRange = newRange;
            currentCueIndex = cueIndex;
            int start = currentRange * levelControls.size();
            LightCueDetail detail = getDetail(currentCueIndex);
            for (int i = 0; i < levelControls.size(); i++) {
                LevelControl levelControl = levelControls.get(i);
                int index = start + i;
                if (index < context.getShow().getNumberOfSubmasters()) {
                    Util.debug("setFocusSubmaster() assign=" + index);
                    CueSubmasterLevel level = detail.getSubmasterLevel(index);
                    level.setLevelControl(levelControl);
                    levelControl.getLevelHolder().setValue(level.getIntValue(), levelControlListener);
                } else {
                    levelControl.setLevel(0);
                }
            }
        }
    }

    public void setFocusStage(final int channelIndex, final int[] channelIndexes) {
        if (focus != FOCUS_STAGE) {
            deassign();
        }
    }

    private LightCueDetail getDetail(final int cueIndex) {
        return getCues().getLightCues().getDetail(cueIndex);
    }

    private Cues getCues() {
        return context.getShow().getCues();
    }

    private void deassign() {
        if (focus == FOCUS_STAGE) {
            deassignStage();
        } else if (focus == FOCUS_CHANNEL) {
            deassignChannels();
        } else if (focus == FOCUS_SUBMASTER) {
            deassignSubmasters();
        } else {
        }
    }

    private void deassignStage() {
    }

    private void deassignChannels() {
        if (currentRange >= 0 && currentCueIndex >= 0) {
            int start = currentRange * levelControls.size();
            int end = start + levelControls.size();
            if (end > currentChannelIndexes.length) {
                end = currentChannelIndexes.length;
            }
            LightCueDetail detail = getDetail(currentCueIndex);
            for (int i = start; i < end; i++) {
                int channelIndex = currentChannelIndexes[i];
                CueChannelLevel level = detail.getChannelLevel(channelIndex);
                level.setLevelControl(null);
            }
        }
        currentRange = -1;
        currentCueIndex = -1;
        currentChannelIndexes = new int[0];
    }

    private void deassignSubmasters() {
        if (currentRange >= 0 && currentCueIndex >= 0) {
            int start = currentRange * levelControls.size();
            int end = start + levelControls.size();
            if (end > context.getShow().getNumberOfSubmasters()) {
                end = context.getShow().getNumberOfSubmasters();
            }
            LightCueDetail detail = getDetail(currentCueIndex);
            for (int i = start; i < end; i++) {
                CueSubmasterLevel level = detail.getSubmasterLevel(i);
                level.setLevelControl(null);
                Util.debug("deassignSubmasters() assign=" + i);
            }
        }
        currentRange = -1;
        currentCueIndex = -1;
    }

    private void levelChanged() {
        Util.debug("levelChanged() focus=" + focus);
        if (focus == FOCUS_CHANNEL) {
            channelLevelChanged();
        } else if (focus == FOCUS_SUBMASTER) {
            submasterLevelChanged();
        }
    }

    private void channelLevelChanged() {
        if (currentCueIndex != -1) {
            int start = currentRange * levelControls.size();
            for (int i = 0; i < levelControls.size(); i++) {
                LevelControl levelControl = levelControls.get(i);
                int index = start + i;
                if (index < currentChannelIndexes.length) {
                    int channelIndex = currentChannelIndexes[index];
                    int old = getCues().getLightCues().getDetail(currentCueIndex).getChannelLevel(channelIndex).getChannelIntValue();
                    if (old != levelControl.getLevel()) {
                        float value = levelControl.getLevel() / 100f;
                        getCues().getLightCues().setChannel(currentCueIndex, channelIndex, value);
                    }
                }
            }
        }
    }

    private void submasterLevelChanged() {
        Util.debug("submasterLevelChanged()");
        if (currentCueIndex != -1) {
            int start = currentRange * levelControls.size();
            Util.debug("    submasterLevelChanged() start=" + start);
            for (int i = 0; i < levelControls.size(); i++) {
                LevelControl levelControl = levelControls.get(i);
                int submasterIndex = start + i;
                Util.debug("    submasterLevelChanged() submasterIndex=" + submasterIndex);
                if (submasterIndex < context.getShow().getNumberOfSubmasters()) {
                    Util.debug("    submasterIndex=" + submasterIndex + ", level=" + levelControl.getLevel());
                    int old = getCues().getLightCues().getDetail(currentCueIndex).getSubmasterLevel(submasterIndex).getIntValue();
                    if (old != levelControl.getLevel()) {
                        float value = levelControl.getLevel() / 100f;
                        getCues().getLightCues().setSubmaster(currentCueIndex, submasterIndex, value);
                    }
                }
            }
        }
    }

    private class LevelControlListener implements Listener {

        public void changed() {
            Util.debug("LevelControlListener.changed()");
            levelChanged();
        }
    }
}
