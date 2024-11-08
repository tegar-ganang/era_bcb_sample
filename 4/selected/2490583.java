package be.lassi.xml;

import java.awt.Rectangle;
import java.io.Reader;
import be.lassi.base.Dirty;
import be.lassi.cues.Cue;
import be.lassi.cues.CueChannelLevel;
import be.lassi.cues.CueDetailFactory;
import be.lassi.cues.CueSubmasterLevel;
import be.lassi.cues.LightCueDetail;
import be.lassi.domain.Channel;
import be.lassi.domain.Dimmer;
import be.lassi.domain.FrameProperties;
import be.lassi.domain.Group;
import be.lassi.domain.Level;
import be.lassi.domain.LevelValue;
import be.lassi.domain.Show;
import be.lassi.domain.Submaster;
import be.lassi.util.Util;

/**
 * Constructs <code>Show</code> object from xml representation of show.
 */
public class XmlShowReader extends XmlReader {

    /**
     * The comment lines that have been collected since the previous comment.
     */
    private final StringBuilder comment = new StringBuilder();

    /**
     * The cue that is being parsed (null if no cue is being parsed).
     */
    private Cue cue;

    /**
     * The dirty indicator to be used in the show.
     */
    private final Dirty dirty;

    /**
     * The group that is being parsed (null if no group is being parsed).
     */
    private Group group;

    /**
     * The light cue detail that is being parsed (null if no detail is being parsed).
     */
    private LightCueDetail lightCueDetail;

    /**
     * The show, the result of parsing the xml.
     */
    private Show show;

    /**
     * The submaster that is being parsed (null if no submaster is being parsed).
     */
    private Submaster submaster;

    public XmlShowReader(final Dirty dirty, final Reader reader) {
        super(reader);
        this.dirty = dirty;
    }

    public Show getShow() {
        return show;
    }

    private String comment() {
        String string = comment.toString();
        if (string.length() > 0) {
            if (string.charAt(string.length() - 1) == '\n') {
                string = string.substring(0, string.length() - 1);
            }
        }
        return string;
    }

    private void parseCommentLine() {
        String text = getString("text");
        comment.append(text);
        comment.append("\n");
    }

    private void parseCue() {
        String number = getString("number");
        String page = getString("page");
        String prompt = "";
        String description = getString("description");
        cue = new Cue(dirty, number, page, prompt, description);
        new CueDetailFactory(show.getNumberOfChannels(), show.getNumberOfSubmasters()).update(cue);
        show.getCues().add(cue);
        if (cue.isLightCue()) {
            lightCueDetail = (LightCueDetail) cue.getDetail();
            for (int i = 0; i < lightCueDetail.getNumberOfSubmasters(); i++) {
                lightCueDetail.getSubmasterLevel(i).getLevelValue().setActive(false);
                lightCueDetail.getSubmasterLevel(i).setDerived(false);
            }
            for (int i = 0; i < lightCueDetail.getNumberOfChannels(); i++) {
                lightCueDetail.getChannelLevel(i).getChannelLevelValue().setActive(false);
                lightCueDetail.getChannelLevel(i).setDerived(false);
                lightCueDetail.getChannelLevel(i).getSubmasterLevelValue().setActive(false);
            }
        }
    }

    private void parseCueChannelLevel() {
        if (lightCueDetail == null) {
            error("lightCueDetail not found");
        } else {
            int channelId = getInt("fixture-id");
            if (channelId <= 0 || channelId > show.getChannels().size()) {
                error("Unknown fixture with id \"" + channelId + "\"");
            } else {
                String value = getString("value");
                CueChannelLevel level = lightCueDetail.getChannelLevel(channelId - 1);
                LevelValue levelValue = level.getChannelLevelValue();
                levelValue.setActive(true);
                if ("derived".equals(value)) {
                    level.setDerived(true);
                    levelValue.setIntValue(0);
                } else {
                    level.setDerived(false);
                    levelValue.setIntValue(Util.toInt(value));
                }
            }
        }
    }

    private void parseCueList() {
        show.contructPart2();
    }

    private void parseCueSubmasterLevel() {
        if (lightCueDetail == null) {
            error("lightCueDetail not found");
        } else {
            int submasterId = getInt("id") - 1;
            String value = getString("value");
            CueSubmasterLevel level = lightCueDetail.getSubmasterLevel(submasterId);
            if ("derived".equals(value)) {
                level.setDerived(true);
                level.getLevelValue().setActive(true);
                level.getLevelValue().setIntValue(0);
            } else {
                level.setDerived(false);
                level.getLevelValue().setActive(true);
                level.getLevelValue().setIntValue(Util.toInt(value));
            }
        }
    }

    private void parseFixture() {
        if ("group".equals(get(-2))) {
            parseGroupFixture();
        } else {
            int id = getInt("id") - 1;
            String name = getString("name");
            Channel channel = new Channel(dirty, id, name);
            show.getChannels().add(channel);
        }
    }

    private void parseGroup() {
        String name = getString("name");
        group = new Group(dirty, name);
        show.getGroups().add(group);
    }

    private void parseGroupFixture() {
        if (group == null) {
            error("Group not found");
        } else {
            int id = getInt("id");
            if (id <= 0 || id > show.getChannels().size()) {
                error("Unknown fixture with id \"" + id + "\"");
            } else {
                Channel channel = show.getChannels().get(id - 1);
                group.add(channel);
            }
        }
    }

    private void parsePatchLine() {
        int id = getInt("id") - 1;
        String name = getString("name");
        int fixtureId = getOptionalNumber("fixture-id") - 1;
        Dimmer dimmer = new Dimmer(dirty, id, name);
        if (fixtureId != -1) {
            Channel channel = show.getChannels().get(fixtureId);
            dimmer.setChannel(channel);
        }
        show.getDimmers().add(dimmer);
    }

    private void parseSet() {
        if ("submaster".equals(get(-2))) {
            parseSubmasterChannelLevel();
        } else if ("cue".equals(get(-2))) {
            parseCueChannelLevel();
        }
    }

    private void parseShow() {
        String name = getString("name");
        show = new Show(dirty, name);
    }

    private void parseSubmaster() {
        if ("cue".equals(get(-2))) {
            parseCueSubmasterLevel();
        } else if ("show".equals(get(-2))) {
            int id = getInt("id") - 1;
            String name = getString("name");
            int channelCount = show.getNumberOfChannels();
            submaster = new Submaster(dirty, id, channelCount, name);
            show.getSubmasters().add(submaster);
            for (int i = 0; i < submaster.getNumberOfLevels(); i++) {
                submaster.getLevel(i).setActive(false);
            }
        }
    }

    private void parseSubmasterChannelLevel() {
        if (submaster == null) {
            error("Submaster not found");
        } else {
            int fixtureId = getInt("fixture-id");
            if (fixtureId <= 0 || fixtureId > show.getChannels().size()) {
                error("Unknown fixture with id \"" + fixtureId + "\"");
            } else {
                int value = getInt("value");
                Level level = submaster.getLevel(fixtureId - 1);
                level.setActive(true);
                level.setIntValue(value);
            }
        }
    }

    private void parseWindow() {
        String id = getString("id");
        int x = getInt("x");
        int y = getInt("y");
        int width = getInt("width");
        int height = getInt("height");
        boolean visible = getBoolean("visible");
        Rectangle bounds = new Rectangle(x, y, width, height);
        FrameProperties properties = new FrameProperties(id, bounds, visible);
        show.setFrameProperties(id, properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void endElement(final String name) {
        if ("submaster".equals(name)) {
            if ("show".equals(get(-2))) {
                submaster.setComment(comment());
                submaster = null;
            }
        } else if ("group".equals(name)) {
            group.setComment(comment());
            group = null;
        } else if ("cue".equals(name)) {
            cue.setPrompt(comment());
            cue = null;
            lightCueDetail = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void startElement(final String name) {
        if ("show".equals(name)) {
            parseShow();
        } else if ("fixture".equals(name)) {
            parseFixture();
        } else if ("patch-line".equals(name)) {
            parsePatchLine();
        } else if ("submaster".equals(name)) {
            parseSubmaster();
        } else if ("comment".equals(name)) {
            comment.setLength(0);
        } else if ("set".equals(name)) {
            parseSet();
        } else if ("group".equals(name)) {
            parseGroup();
        } else if ("cue-list".equals(name)) {
            parseCueList();
        } else if ("cue".equals(name)) {
            parseCue();
        } else if ("window".equals(name)) {
            parseWindow();
        } else if ("line".equals(name)) {
            parseCommentLine();
        }
    }
}
