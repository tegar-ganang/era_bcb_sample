package be.lassi.domain;

import be.lassi.cues.Cue;
import be.lassi.cues.CueChannelLevel;
import be.lassi.cues.CueDetail;
import be.lassi.cues.CueSubmasterLevel;
import be.lassi.cues.Cues;
import be.lassi.cues.LightCueDetail;

public class ShowCopier {

    private final Show newShow;

    private final Show oldShow;

    public ShowCopier(final Show newShow, final Show oldShow) {
        this.newShow = newShow;
        this.oldShow = oldShow;
    }

    public void copy() {
        copyChannels();
        copyDimmers();
        copySubmasters();
        copyCues();
        copyGroups();
    }

    private void copyChannels() {
        Channels oldChannels = oldShow.getChannels();
        Channels newChannels = newShow.getChannels();
        int count = Math.min(oldChannels.size(), newChannels.size());
        for (int i = 0; i < count; i++) {
            Channel oldChannel = oldChannels.get(i);
            Channel newChannel = newChannels.get(i);
            newChannel.setName(oldChannel.getName());
        }
    }

    private void copyCues() {
        Cues oldCues = oldShow.getCues();
        Cues newCues = newShow.getCues();
        for (int i = 0; i < oldCues.size(); i++) {
            Cue oldCue = oldCues.get(i);
            Cue cue = new Cue(newShow.getDirty(), oldCue.getNumber(), oldCue.getPage(), oldCue.getPrompt(), oldCue.getDescription());
            CueDetail detail = null;
            if (oldCue.isLightCue()) {
                detail = copyLightCueDetail(oldCue);
            } else {
                detail = oldCue.getDetail().copy();
            }
            cue.setDetail(detail);
            newCues.add(cue);
        }
    }

    private CueDetail copyLightCueDetail(final Cue oldCue) {
        LightCueDetail oldDetail = (LightCueDetail) oldCue.getDetail();
        LightCueDetail newDetail = new LightCueDetail(oldDetail.getTiming(), newShow.getNumberOfChannels(), newShow.getNumberOfSubmasters());
        copyChannelLevels(oldDetail, newDetail);
        copySubmasterLevels(oldDetail, newDetail);
        defaultChannelLevels(oldDetail, newDetail);
        defaultSubmasterLevels(oldDetail, newDetail);
        return newDetail;
    }

    private void copyChannelLevels(final LightCueDetail oldDetail, final LightCueDetail newDetail) {
        int channelCount = Math.min(oldShow.getNumberOfChannels(), newShow.getNumberOfChannels());
        for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
            CueChannelLevel oldLevel = oldDetail.getChannelLevel(channelIndex);
            CueChannelLevel newLevel = newDetail.getChannelLevel(channelIndex);
            newLevel.setDerived(oldLevel.isDerived());
            LevelValue oldChannelLevel = oldLevel.getChannelLevelValue();
            LevelValue newChannelLevel = newLevel.getChannelLevelValue();
            newChannelLevel.setActive(oldChannelLevel.isActive());
            newChannelLevel.setValue(oldChannelLevel.getValue());
            LevelValue oldSubmasterLevel = oldLevel.getSubmasterLevelValue();
            LevelValue newSubmasterLevel = newLevel.getSubmasterLevelValue();
            newSubmasterLevel.setActive(oldSubmasterLevel.isActive());
            newSubmasterLevel.setValue(oldSubmasterLevel.getValue());
        }
    }

    private void defaultChannelLevels(final LightCueDetail oldDetail, final LightCueDetail newDetail) {
        int oldChannelCount = oldShow.getNumberOfChannels();
        int newChannelCount = newShow.getNumberOfChannels();
        for (int channelIndex = oldChannelCount; channelIndex < newChannelCount; channelIndex++) {
            CueChannelLevel newLevel = newDetail.getChannelLevel(channelIndex);
            newLevel.setDerived(true);
            LevelValue newChannelLevel = newLevel.getChannelLevelValue();
            newChannelLevel.setValue(0);
            newChannelLevel.setActive(false);
            LevelValue newSubmasterLevel = newLevel.getSubmasterLevelValue();
            newSubmasterLevel.setValue(0);
            newSubmasterLevel.setActive(false);
        }
    }

    private void copySubmasterLevels(final LightCueDetail oldDetail, final LightCueDetail newDetail) {
        int submasterCount = Math.min(oldShow.getNumberOfSubmasters(), newShow.getNumberOfSubmasters());
        for (int submasterIndex = 0; submasterIndex < submasterCount; submasterIndex++) {
            CueSubmasterLevel oldLevel = oldDetail.getSubmasterLevel(submasterIndex);
            CueSubmasterLevel newLevel = newDetail.getSubmasterLevel(submasterIndex);
            newLevel.setDerived(oldLevel.isDerived());
            LevelValue oldLevelValue = oldLevel.getLevelValue();
            LevelValue newLevelValue = newLevel.getLevelValue();
            newLevelValue.setActive(oldLevelValue.isActive());
            newLevelValue.setValue(oldLevelValue.getValue());
        }
    }

    private void defaultSubmasterLevels(final LightCueDetail oldDetail, final LightCueDetail newDetail) {
        int oldSubmasterCount = oldShow.getNumberOfSubmasters();
        int newSubmasterCount = newShow.getNumberOfSubmasters();
        for (int i = oldSubmasterCount; i < newSubmasterCount; i++) {
            CueSubmasterLevel level = newDetail.getSubmasterLevel(i);
            level.setDerived(true);
            LevelValue levelValue = level.getLevelValue();
            levelValue.setValue(0);
            levelValue.setActive(false);
        }
    }

    private void copyDimmers() {
        Dimmers oldDimmers = oldShow.getDimmers();
        Dimmers newDimmers = newShow.getDimmers();
        int count = Math.min(oldDimmers.size(), newDimmers.size());
        for (int i = 0; i < count; i++) {
            Dimmer oldDimmer = oldDimmers.get(i);
            Dimmer newDimmer = newDimmers.get(i);
            newDimmer.setName(oldDimmer.getName());
            int channelId = oldDimmer.getChannelId();
            if (channelId >= 0) {
                if (channelId < newShow.getNumberOfChannels()) {
                    Channel channel = newShow.getChannels().get(channelId);
                    newDimmer.setChannel(channel);
                }
            }
        }
    }

    private void copyGroups() {
        Groups oldGroups = oldShow.getGroups();
        Groups newGroups = newShow.getGroups();
        for (Group oldGroup : oldGroups) {
            Group newGroup = new Group(newShow.getDirty(), oldGroup.getName());
            newGroups.add(newGroup);
            for (Channel oldChannel : oldGroup.getChannels()) {
                if (oldChannel.getId() < newShow.getNumberOfChannels()) {
                    Channel newChannel = newShow.getChannels().get(oldChannel.getId());
                    newGroup.add(newChannel);
                }
            }
        }
    }

    private void copySubmasters() {
        Submasters oldSubmasters = oldShow.getSubmasters();
        Submasters newSubmasters = newShow.getSubmasters();
        int count = Math.min(oldSubmasters.size(), newSubmasters.size());
        for (int i = 0; i < count; i++) {
            Submaster oldSubmaster = oldSubmasters.get(i);
            Submaster newSubmaster = newSubmasters.get(i);
            newSubmaster.setName(oldSubmaster.getName());
            copySubmasterLevels(oldSubmaster, newSubmaster);
        }
    }

    private void copySubmasterLevels(final Submaster oldSubmaster, final Submaster newSubmaster) {
        int channelCount = Math.min(oldShow.getNumberOfChannels(), newShow.getNumberOfChannels());
        for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
            Level oldLevel = oldSubmaster.getLevel(channelIndex);
            Level newLevel = newSubmaster.getLevel(channelIndex);
            newLevel.setActive(oldLevel.isActive());
            newLevel.setValue(oldLevel.getValue());
        }
    }
}
