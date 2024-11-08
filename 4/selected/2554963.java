package be.lassi.io;

import java.awt.Rectangle;
import java.io.Reader;
import be.lassi.base.Dirty;
import be.lassi.cues.Cue;
import be.lassi.cues.CueDetailFactory;
import be.lassi.cues.LightCues;
import be.lassi.domain.Channel;
import be.lassi.domain.Dimmer;
import be.lassi.domain.FrameProperties;
import be.lassi.domain.Group;
import be.lassi.domain.Level;
import be.lassi.domain.Show;
import be.lassi.domain.ShowBuilder;
import be.lassi.domain.Submaster;

/**
 * Interpretes text that represents the persistant show information,
 * and creates a <code>Show</code> object.
 * <p>
 * The text is read from a <code>Reader</code>.  This can be a
 * <code>FileReader</code>, or a <code>StringReader</code> for testing
 * the code.
 *
 * @see be.lassi.domain.Show
 * @see be.lassi.io.ShowFileReader
 */
public class ShowInterpreter extends Interpreter {

    private static final int CHANNELS = 1;

    private static final int DIMMERS = 2;

    private static final int SUBMASTERS = 3;

    private static final int CHANNEL = 4;

    private static final int DIMMER = 5;

    private static final int SUBMASTER = 7;

    private static final int CUE = 8;

    private static final int FRAME = 9;

    private static final int GROUP = 10;

    /**
     * The resulting Show object.
     */
    private Show show;

    private final Dirty dirty;

    /**
    *
    */
    private int numberOfChannels = -1;

    /**
     *
     */
    private int numberOfDimmers = -1;

    /**
     *
     */
    private int numberOfSubmasters = -1;

    /**
     * Create new instance.
     *
     * @param reader
     */
    public ShowInterpreter(final Dirty dirty, final Reader reader) throws ShowFileException {
        super(reader);
        this.dirty = dirty;
        nextShow();
    }

    /**
     * @return
     * @throws ShowFileException
     */
    public Show getShow() throws ShowFileException {
        if (show == null) {
            if (numberOfChannels == -1) {
                throw new ShowFileException(0, "CHANNELS missing");
            }
            if (numberOfDimmers == -1) {
                throw new ShowFileException(0, "DIMMERS missing");
            }
            if (numberOfSubmasters == -1) {
                throw new ShowFileException(0, "SUBMASTERS missing");
            }
            show = ShowBuilder.build(dirty, numberOfChannels, numberOfSubmasters, numberOfDimmers, "");
        }
        return show;
    }

    /**
     * Read all information in show file.  Dispatch to methods reading the
     * information that comes with the different identifiers.
     *
     * @throws ShowFileException
     */
    private void nextShow() throws ShowFileException {
        while (!eof()) {
            switch(nextIdentifier()) {
                case (CHANNELS):
                    nextNumberOfChannels();
                    break;
                case (DIMMERS):
                    nextNumberOfDimmers();
                    break;
                case (SUBMASTERS):
                    nextNumberOfSubmasters();
                    break;
                case (CHANNEL):
                    nextChannel();
                    break;
                case (DIMMER):
                    nextDimmer();
                    break;
                case (SUBMASTER):
                    nextSubMaster();
                    break;
                case (CUE):
                    nextCue();
                    break;
                case (FRAME):
                    nextFrame();
                    break;
                case (GROUP):
                    nextGroup();
                    break;
            }
        }
    }

    /**
     * Interprete CHANNEL line.
     *
     * @throws ShowFileException
     */
    private void nextChannel() throws ShowFileException {
        int channelNumber = nextInteger("channel number", 1, getShow().getNumberOfChannels());
        String name = nextString("channel name");
        Channel channel = getShow().getChannels().get(channelNumber - 1);
        channel.setName(name);
    }

    /**
     * Interprete DIMMER line.
     *
     * @throws ShowFileException if invalid patch information
     */
    private void nextDimmer() throws ShowFileException {
        int dimmerNumber = nextInteger("dimmer number", 1, getShow().getNumberOfDimmers());
        String name = nextString("dimmer name");
        int channelNumber = nextInteger("dimmer channel patch", 0, getShow().getNumberOfChannels());
        Dimmer dimmer = getShow().getDimmers().get(dimmerNumber - 1);
        dimmer.setName(name);
        if (channelNumber > 0) {
            Channel channel = getShow().getChannels().get(channelNumber - 1);
            dimmer.setChannel(channel);
        }
    }

    private void nextGroup() throws ShowFileException {
        String name = nextString("group name");
        int channelCount = nextInteger("group channel count");
        Group group = new Group(dirty, name);
        getShow().getGroups().add(group);
        for (int i = 0; i < channelCount; i++) {
            int channelNumber = nextInteger("group channel number", 1, getShow().getNumberOfChannels());
            Channel channel = getShow().getChannels().get(channelNumber - 1);
            group.add(channel);
        }
    }

    /**
     *
     *
     */
    private void nextSubmasterLevels(final Submaster submaster) throws ShowFileException {
        for (int i = 0; i < submaster.getNumberOfLevels(); i++) {
            Level level = submaster.getLevel(i);
            int value = nextIntegerOrDash("level number " + (i + 1));
            if (value == -1) {
                level.setActive(false);
            } else {
                if (value < 0 || value > 100) {
                    throwException("level invalid");
                }
                level.setIntValue(value);
                level.setActive(true);
            }
        }
    }

    /**
     * Interprete CHANNELS line.
     *
     * @throws ShowFileException
     */
    private void nextNumberOfChannels() throws ShowFileException {
        numberOfChannels = nextInteger("number of channels");
    }

    /**
     * Interprete DIMMERS line.
     *
     * @throws ShowFileException
     */
    private void nextNumberOfDimmers() throws ShowFileException {
        numberOfDimmers = nextInteger("number of dimmers");
    }

    /**
     * Interprete SUBMASTERS line.
     *
     * @throws ShowFileException
     */
    private void nextNumberOfSubmasters() throws ShowFileException {
        numberOfSubmasters = nextInteger("number of submasters");
    }

    /**
     * Interprete SUBMASTER line.
     *
     * @throws ShowFileException
     */
    private void nextSubMaster() throws ShowFileException {
        if (getShow().getNumberOfChannels() == 0) {
            throwException("Number of channels not defined yet while reading submaster levels");
        }
        int submasterNumber = nextInteger("submaster number");
        String name = nextString("submaster name");
        int numberOfLevels = getShow().getNumberOfChannels();
        Submaster submaster = new Submaster(dirty, submasterNumber - 1, numberOfLevels, name);
        try {
            nextSubmasterLevels(submaster);
        } catch (ShowFileException e) {
            e.addContext("Reading levels for submaster " + name);
            throw e;
        }
        getShow().getSubmasters().get(submasterNumber - 1).set(submaster);
    }

    /**
     * Interprete FRAME line.
     *
     * @throws ShowFileException
     */
    private void nextFrame() throws ShowFileException {
        String id = nextString("frame id");
        int x = nextInteger("frame x");
        int y = nextInteger("frame x");
        int width = nextInteger("frame width");
        int height = nextInteger("frame height");
        String visible = nextString("frame visibility");
        Rectangle bounds = new Rectangle(x, y, width, height);
        boolean isVisible = visible.equals("visible");
        FrameProperties p = new FrameProperties(id, bounds, isVisible);
        getShow().setFrameProperties(p.getId(), p);
    }

    /**
     * Interprete CUE line.
     *
     * @throws ShowFileException
     */
    private void nextCue() throws ShowFileException {
        String number = nextString("cue number");
        String page = nextString("cue page");
        String prompt = nextString("cue prompt");
        String description = nextString("cue description");
        int c = getShow().getNumberOfChannels();
        int s = getShow().getNumberOfSubmasters();
        Cue cue = new Cue(number, page, prompt, description);
        new CueDetailFactory(c, s).update(cue);
        getShow().getCues().add(cue);
        if (cue.isLightCue()) {
            int lightCueIndex = cue.getLightCueIndex();
            nextCueSubmasterLevels(lightCueIndex);
            nextChannelLevels(lightCueIndex);
        }
    }

    private void nextChannelLevels(final int lightCueIndex) throws ShowFileException {
        LightCues lightCues = getShow().getCues().getLightCues();
        for (int i = 0; i < getShow().getNumberOfChannels(); i++) {
            float level = 0f;
            int intLevel = nextIntegerOrDashOrCross("cue channel level " + (i + 1));
            if (intLevel == -1) {
                lightCues.deactivateChannel(lightCueIndex, i);
            } else if (intLevel == -2) {
                lightCues.resetChannel(lightCueIndex, i);
            } else {
                level = intLevel / 100f;
                lightCues.setChannel(lightCueIndex, i, level);
            }
        }
    }

    private void nextCueSubmasterLevels(final int lightCueIndex) throws ShowFileException {
        LightCues lightCues = getShow().getCues().getLightCues();
        for (int i = 0; i < getShow().getNumberOfSubmasters(); i++) {
            float level = 0f;
            int intLevel = nextIntegerOrDashOrCross("cue submaster level " + (i + 1));
            if (intLevel == -1) {
                lightCues.deactivateCueSubmaster(lightCueIndex, i);
            } else if (intLevel == -2) {
                lightCues.resetSubmaster(lightCueIndex, i);
            } else {
                level = intLevel / 100f;
                lightCues.setCueSubmaster(lightCueIndex, i, level);
            }
        }
    }

    /**
     * Interprete the line identifier (first token on line).
     *
     * @return an identifier constant
     * @throws ShowFileException
     */
    private int nextIdentifier() throws ShowFileException {
        int identifier = 0;
        String string;
        string = nextString("identifier");
        if ("CHANNEL".equals(string)) {
            identifier = CHANNEL;
        } else if ("DIMMER".equals(string)) {
            identifier = DIMMER;
        } else if ("CHANNELS".equals(string)) {
            identifier = CHANNELS;
        } else if ("DIMMERS".equals(string)) {
            identifier = DIMMERS;
        } else if ("SUBMASTERS".equals(string)) {
            identifier = SUBMASTERS;
        } else if ("SUBMASTER".equals(string)) {
            identifier = SUBMASTER;
        } else if ("CUE".equals(string)) {
            identifier = CUE;
        } else if ("FRAME".equals(string)) {
            identifier = FRAME;
        } else if ("GROUP".equals(string)) {
            identifier = GROUP;
        } else {
            throwException("Unknown Identifier <" + string + ">");
        }
        return identifier;
    }
}
