package be.lassi.test;

public class TestShow {

    private final int CUES = 15;

    private final int CHANNELS = 24;

    private final int SUBMASTERS = 6;

    private TestMemory channels;

    private TestMemories cues;

    private TestMemories subMasters;

    private int currentCue = -1;

    public TestShow() {
        channels = new TestMemory("Channels", CHANNELS);
        cues = new TestMemories();
        for (int i = 0; i < CUES; i++) {
            cues.add(new TestMemory("Cue " + (i + 1), CHANNELS));
        }
        subMasters = new TestMemories();
        for (int i = 0; i < SUBMASTERS; i++) {
            subMasters.add(new TestMemory("SubMaster " + (i + 1), SUBMASTERS));
        }
    }

    public TestMemory getChannels() {
        return channels;
    }

    public TestMemory getCue(int index) {
        return cues.get(index);
    }

    public int getCurrentCue() {
        return currentCue;
    }

    public int getNumberOfChannels() {
        return channels.size();
    }

    public int getNumberOfCues() {
        return cues.size();
    }

    public int getNumberOfSubMasters() {
        return subMasters.size();
    }

    public TestMemory getSubMaster(int index) {
        return subMasters.get(index);
    }

    public void reset(int cueIndex, int channelIndex) {
        TestLevel level = getCue(cueIndex).get(channelIndex);
        level.setActual(false);
        int index = -1;
        for (int i = cueIndex - 1; (index == -1) && (i >= 0); i--) {
            level = getCue(i).get(channelIndex);
            if (level.isActual()) {
                index = i;
            }
        }
        int value = 0;
        if (index > -1) {
            level = getCue(index).get(channelIndex);
            value = level.getValue();
        }
        boolean update = true;
        for (int i = index + 1; update && i < getNumberOfCues(); i++) {
            TestMemory cue = getCue(i);
            update = !cue.get(channelIndex).isActual();
            if (update) {
                cue.get(channelIndex).setValue(value);
            }
        }
    }

    public void setCurrentCue(final int index) {
        currentCue = index;
        for (int cueIndex = 0; cueIndex < getNumberOfCues(); cueIndex++) {
            for (int channelIndex = 0; channelIndex < getNumberOfChannels(); channelIndex++) {
                TestMemory cue = getCue(cueIndex);
                cue.get(channelIndex).setCurrentCue(index == cueIndex);
            }
        }
    }

    public void setValue(final int cueIndex, final int channelIndex, final int value) {
        TestLevel level = getCue(cueIndex).get(channelIndex);
        level.setActual(true);
        level.setValue(value);
        boolean update = true;
        for (int i = cueIndex + 1; update && i < getNumberOfCues(); i++) {
            TestMemory cue = getCue(i);
            update = !cue.get(channelIndex).isActual();
            if (update) {
                cue.get(channelIndex).setValue(value);
            }
        }
    }
}
