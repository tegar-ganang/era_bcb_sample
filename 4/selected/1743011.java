package be.lassi.io;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import be.lassi.cues.Cue;
import be.lassi.cues.CueChannelLevel;
import be.lassi.cues.CueSubmasterLevel;
import be.lassi.cues.Cues;
import be.lassi.cues.LightCueDetail;
import be.lassi.domain.Channel;
import be.lassi.domain.Channels;
import be.lassi.domain.Dimmer;
import be.lassi.domain.Dimmers;
import be.lassi.domain.FrameProperties;
import be.lassi.domain.Group;
import be.lassi.domain.Groups;
import be.lassi.domain.Level;
import be.lassi.domain.Show;
import be.lassi.domain.Submaster;
import be.lassi.domain.Submasters;

/**
 * Writes a show to a file on disk.
 *
 *
 */
public class ShowWriter {

    /**
     * The file that the show is written to.
     */
    private FileWriter file;

    /**
     * Constructs a new writer.
     *
     * @param filename the name of the file on disk
     * @throws ShowFileException if problem
     */
    public ShowWriter(final String filename) throws ShowFileException {
        try {
            file = new FileWriter(filename);
        } catch (FileNotFoundException e) {
            throw new ShowFileException(0, "Could not open file " + filename + " " + e.getMessage());
        } catch (IOException e) {
            throw new ShowFileException(0, "Could not open file " + filename + " " + e.getMessage());
        }
    }

    /**
     * Writes given show to disk.
     *
     * @param show the show to be written to disk
     * @throws ShowFileException if problem
     */
    public void write(final Show show) throws ShowFileException {
        writeln("DIMMERS " + show.getNumberOfDimmers());
        writeln("CHANNELS " + show.getNumberOfChannels());
        writeln("SUBMASTERS " + show.getNumberOfSubmasters());
        write(show.getDimmers());
        write(show.getChannels());
        write(show.getSubmasters());
        write(show.getGroups());
        writeCues(show);
        write(show.getFrameProperties());
        try {
            file.close();
        } catch (IOException e) {
        }
    }

    /**
     *
     *
     */
    private void write(final String string) throws ShowFileException {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            char character = string.charAt(i);
            if (character != '\n') {
                buffer.append(character);
            } else {
                buffer.append('\\');
                buffer.append('n');
            }
        }
        try {
            file.write(buffer.toString());
        } catch (IOException e) {
            throw new ShowFileException(0, e.getMessage());
        }
    }

    private void write(final Show show, final Cue cue) throws ShowFileException {
        write("CUE");
        writeQuoted(cue.getNumber());
        writeQuoted(cue.getPage());
        writeQuoted(cue.getPrompt());
        writeQuoted(cue.getDescription());
        if (cue.isLightCue()) {
            int lightCueIndex = cue.getLightCueIndex();
            LightCueDetail detail = show.getCues().getLightCues().getDetail(lightCueIndex);
            for (int i = 0; i < detail.getNumberOfSubmasters(); i++) {
                CueSubmasterLevel level = detail.getSubmasterLevel(i);
                if (level.getLevelValue().isActive()) {
                    if (level.isDerived()) {
                        write(" x");
                    } else {
                        write(" " + level.getIntValue());
                    }
                } else {
                    write(" -");
                }
            }
            for (int i = 0; i < detail.getNumberOfChannels(); i++) {
                CueChannelLevel level = detail.getChannelLevel(i);
                if (level.getChannelLevelValue().isActive()) {
                    if (level.isDerived()) {
                        write(" x");
                    } else {
                        write(" " + level.getChannelIntValue());
                    }
                } else {
                    write(" -");
                }
            }
        }
        writeln("");
    }

    /**
     *
     */
    private void writeCues(final Show show) throws ShowFileException {
        Cues cues = show.getCues();
        for (int i = 0; i < cues.size(); i++) {
            write(show, cues.get(i));
        }
    }

    /**
     *
     */
    private void write(final Channels channels) throws ShowFileException {
        for (int i = 0; i < channels.size(); i++) {
            Channel channel = channels.get(i);
            write("CHANNEL");
            write(" " + (i + 1));
            writeQuoted(channel.getName());
            writeln("");
        }
    }

    /**
     *
     */
    private void write(final Dimmers dimmers) throws ShowFileException {
        for (int i = 0; i < dimmers.size(); i++) {
            Dimmer dimmer = dimmers.get(i);
            write("DIMMER");
            write(" " + (i + 1));
            writeQuoted(dimmer.getName());
            write(" " + (dimmer.getChannelId() + 1));
            writeln("");
        }
    }

    /**
     *
     *
     */
    private void write(final Submaster submaster) throws ShowFileException {
        write("\"" + submaster.getName() + "\"");
        for (int index = 0; index < submaster.getNumberOfLevels(); index++) {
            Level level = submaster.getLevel(index);
            if (level.isActive()) {
                write(" " + level.getIntValue());
            } else {
                write(" -");
            }
        }
    }

    private void write(final Submasters submasters) throws ShowFileException {
        for (int i = 0; i < submasters.size(); i++) {
            Submaster submaster = submasters.get(i);
            write("SUBMASTER");
            write(" " + (i + 1) + " ");
            write(submaster);
            writeln("");
        }
    }

    private void write(final Groups groups) throws ShowFileException {
        for (int i = 0; i < groups.size(); i++) {
            Group group = groups.get(i);
            write("GROUP");
            write(" \"" + group.getName() + "\" ");
            write(" " + group.size());
            Channel[] channels = group.getChannels();
            for (Channel channel : channels) {
                write(" " + (channel.getId() + 1));
            }
            writeln("");
        }
    }

    private void write(final FrameProperties[] propertiess) throws ShowFileException {
        for (FrameProperties properties : propertiess) {
            write("FRAME ");
            write(properties.getId());
            write(" " + properties.getBounds().x);
            write(" " + properties.getBounds().y);
            write(" " + properties.getBounds().width);
            write(" " + properties.getBounds().height);
            writeln(properties.isVisible() ? " visible" : " hidden");
        }
    }

    /**
     *
     *
     */
    private void writeln(final String string) throws ShowFileException {
        write(string);
        try {
            file.write("\n");
        } catch (IOException e) {
            throw new ShowFileException(0, e.getMessage());
        }
    }

    /**
     *
     *
     */
    private void writeQuoted(final String string) throws ShowFileException {
        write(" \"" + string + "\"");
    }
}
