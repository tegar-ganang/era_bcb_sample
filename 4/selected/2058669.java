package de.reichhold.jrehearsal;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;

/**
 * An implementation of <code>MidiParser</code> that parses using the
 * <code>javax.sound.midi</code> API. As such it is robust, but slow.
 * 
 * @author Sean Owen (srowen@yahoo.com)
 * @see PJLMidiParser
 */
public class SequenceHandler extends MidiEventInterpreter {

    private static class EventList {

        private List<Long> timeStamps = new ArrayList<Long>();

        protected void add(long argTicks) {
            timeStamps.add(argTicks);
        }

        protected int getDataIndex(long argPositionInTicks) {
            int result = 0;
            while ((result < timeStamps.size()) && (timeStamps.get(result) <= argPositionInTicks)) {
                result++;
            }
            return result - 1;
        }

        protected long getTimeStamp(int argIndex) {
            return timeStamps.get(argIndex);
        }
    }

    public static class TempoData extends EventList {

        private List<Long> tempi = new ArrayList<Long>();

        public void add(long argTicks, long argMicroSecondsPerQuarter) {
            super.add(argTicks);
            tempi.add(argMicroSecondsPerQuarter);
        }

        public long getMicroSecondsPerQuarter(long argPositionInTicks) {
            long result = tempi.get(getDataIndex(argPositionInTicks));
            return result;
        }

        public long getBPM(long argPositionInTicks) {
            double result = 60000000.0 / getMicroSecondsPerQuarter(argPositionInTicks);
            return (long) result;
        }
    }

    public static class BarTypeData extends EventList {

        private class Data {

            int nominator;

            long denominator;

            int clicksPerClock;

            int numberOfThirtySecondsPerQuarter;

            long getTicksPerBar() {
                long result = ticksPerQuarter * 4 * nominator / denominator;
                return result;
            }

            public long getTicksPerClock() {
                return clicksPerClock * ticksPerQuarter / 24;
            }
        }

        public static class PositionInBar {

            public int bar;

            public int click;
        }

        private List<Data> barTypes = new ArrayList<Data>();

        private int ticksPerQuarter;

        private long lengthInTicks;

        private List<MidiEvent> metronomeEvents;

        public BarTypeData(int argResolution, long argLengthInTicks) {
            ticksPerQuarter = argResolution;
            lengthInTicks = argLengthInTicks;
        }

        public void add(long argTicks, int argNominator, long argDenominator, int argTicksPerClock, int argNumberOfThirtySecondsPerQuarter) {
            super.add(argTicks);
            Data newEntry = new Data();
            newEntry.nominator = argNominator;
            newEntry.denominator = argDenominator;
            newEntry.clicksPerClock = argTicksPerClock;
            newEntry.numberOfThirtySecondsPerQuarter = argNumberOfThirtySecondsPerQuarter;
            barTypes.add(newEntry);
        }

        public void setComplete() {
            super.add(lengthInTicks + 1);
        }

        public PositionInBar getBarNumber(long argPositionInTicks) {
            PositionInBar result = new PositionInBar();
            int lastEventIndex = getDataIndex(argPositionInTicks);
            if (lastEventIndex >= 0) {
                long lastTimeStamp = 0;
                for (int i = 0; i < lastEventIndex; i++) {
                    Data frame = barTypes.get(i);
                    lastTimeStamp = getTimeStamp(i + 1);
                    long duration = lastTimeStamp - getTimeStamp(i);
                    result.bar += duration / frame.getTicksPerBar();
                }
                if (lastEventIndex < barTypes.size()) {
                    Data lastBarType = barTypes.get(lastEventIndex);
                    long durationInLastBarType = argPositionInTicks - lastTimeStamp;
                    long completeBarsInLastBarType = durationInLastBarType / lastBarType.getTicksPerBar();
                    result.bar += completeBarsInLastBarType;
                    result.click = (int) ((durationInLastBarType - (completeBarsInLastBarType * lastBarType.getTicksPerBar())) / lastBarType.getTicksPerClock());
                }
            }
            result.click++;
            return result;
        }

        public long convertBarPositionToTicks(long argBar) {
            long result = 0;
            int bars = 0;
            Data currentBarType = null;
            for (int i = 0; i < barTypes.size(); i++) {
                currentBarType = (Data) barTypes.get(i);
                int numberOfBarsInBarType = (int) ((getTimeStamp(i + 1) - getTimeStamp(i)) / currentBarType.getTicksPerBar());
                if (bars + numberOfBarsInBarType > argBar) {
                    result = getTimeStamp(i) + (argBar - bars) * currentBarType.getTicksPerBar();
                    break;
                } else {
                    bars += numberOfBarsInBarType;
                }
            }
            return result;
        }

        private List<MidiEvent> getMetronomeTrack(long argMaxTick) throws InvalidMidiDataException {
            if (metronomeEvents == null) {
                metronomeEvents = new ArrayList<MidiEvent>();
                long currentClockPosition = 0;
                for (int i = 0; i < barTypes.size(); i++) {
                    Data data = (Data) barTypes.get(i);
                    long clickDistance = data.getTicksPerClock();
                    long endPosition = (i < barTypes.size() - 1) ? getTimeStamp(i + 1) : argMaxTick;
                    while (currentClockPosition < endPosition) {
                        ShortMessage newClick = new ShortMessage();
                        newClick.setMessage(ShortMessage.NOTE_ON, 9, 77, 127);
                        metronomeEvents.add(new MidiEvent(newClick, currentClockPosition));
                        ShortMessage newClick2 = new ShortMessage();
                        newClick2.setMessage(ShortMessage.NOTE_OFF, 9, 77, 127);
                        metronomeEvents.add(new MidiEvent(newClick2, currentClockPosition + clickDistance / 2));
                        currentClockPosition += clickDistance;
                    }
                }
            }
            return metronomeEvents;
        }

        public List<MidiEvent> getCountInTrack(long argPosition) throws InvalidMidiDataException {
            List<MidiEvent> result = new ArrayList<MidiEvent>();
            Data data = (Data) barTypes.get(getDataIndex(argPosition));
            MetaMessage timeSignature = new MetaMessage();
            int denomPower = 0;
            long denominator = data.denominator;
            while (denominator > 1) {
                denomPower++;
                denominator /= 2;
            }
            byte dataArray[] = new byte[] { (byte) (data.nominator & 0xff), (byte) (denomPower & 0xff), (byte) (data.clicksPerClock & 0xff), (byte) (data.numberOfThirtySecondsPerQuarter & 0xff) };
            timeSignature.setMessage(MidiEventInterpreter.TYPE_META_TIME_SIGNATURE, dataArray, 4);
            result.add(new MidiEvent(timeSignature, 0));
            long clickDistance = data.getTicksPerClock();
            long currentClockPosition = 0;
            for (int i = 0; i < data.nominator; i++) {
                ShortMessage newClick = new ShortMessage();
                newClick.setMessage(ShortMessage.NOTE_ON, 9, 77, 127);
                result.add(new MidiEvent(newClick, currentClockPosition));
                ShortMessage newClick2 = new ShortMessage();
                currentClockPosition += clickDistance;
                newClick2.setMessage(ShortMessage.NOTE_OFF, 9, 77, 127);
                result.add(new MidiEvent(newClick2, currentClockPosition - 1));
            }
            return result;
        }
    }

    public static class ChannelMetaData {

        public int channel;

        public Set<Integer> programs = new HashSet<Integer>();

        public ChannelMetaData(int argChannel) {
            channel = argChannel;
        }

        public String toString() {
            return "" + channel + ": ";
        }
    }

    private Map<Integer, ChannelMetaData> channelData;

    private BarTypeData barData;

    private TempoData tempoData;

    private float bpm;

    /** The number of elapsed ticks at the current point in parsing */
    protected long currentTick;

    private MidiEvent curentEvent;

    private long sequenceLength;

    /**
   * Creates a new JavaxMidiParser to parse the specified MIDI file.
   * 
   * @param f
   *          the MIDI file to be parsed
   * @throws FileNotFoundException
   *           if the file cannot be found
   */
    public SequenceHandler() {
        super();
        currentTick = 0;
    }

    public Map<Integer, ChannelMetaData> getChannelData() {
        return channelData;
    }

    /**
   * @return Returns the frameData.
   */
    public BarTypeData getFrameData() {
        return barData;
    }

    /**
   * @return Returns the tempoData.
   */
    public TempoData getTempoData() {
        return tempoData;
    }

    public float getBPM() {
        return bpm;
    }

    /**
   * Initiates parsing of the MIDI file.
   * 
   * @throws MidiParserException
   *           if an error occurs during parsing
   * @throws InvalidMidiDataException 
   */
    public void parse(Sequence sequence) throws MidiParserException, InvalidMidiDataException {
        initializeLastMetaData(sequence);
        Track[] tracks = sequence.getTracks();
        for (int i = 0; i < tracks.length; i++) {
            parseOneTrack(tracks[i]);
        }
        sequenceLength = sequence.getTickLength();
        List<MidiEvent> metronomeEvents = barData.getMetronomeTrack(sequenceLength);
        Track metronomeTrack = sequence.createTrack();
        for (int i = 0; i < metronomeEvents.size(); i++) {
            metronomeTrack.add((MidiEvent) metronomeEvents.get(i));
        }
        initChanneMetalData(9);
        parseOneTrack(metronomeTrack);
        barData.setComplete();
    }

    private void parseOneTrack(Track argTrack) throws MidiParserException {
        for (int i = 0; i < argTrack.size(); i++) {
            curentEvent = argTrack.get(i);
            currentTick = curentEvent.getTick();
            MidiMessage message = curentEvent.getMessage();
            byte[] data = message.getMessage();
            int status = message.getStatus();
            switch(status) {
                case SysexMessage.SYSTEM_EXCLUSIVE:
                case SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE:
                    sysexEvent(((SysexMessage) message).getData());
                    break;
                case MetaMessage.META:
                    MetaMessage mMessage = (MetaMessage) message;
                    metaEvent(mMessage.getType(), mMessage.getData());
                    break;
                default:
                    int eventChannel = channel(status);
                    switch(type(status)) {
                        case TYPE_NOTE_OFF:
                            noteOff(eventChannel, (int) (data[1] & 0x7F), (int) (data[2] & 0x7F));
                            break;
                        case TYPE_NOTE_ON:
                            noteOn(eventChannel, (int) (data[1] & 0x7F), (int) (data[2] & 0x7F));
                            break;
                        case TYPE_NOTE_AFTERTOUCH:
                            noteAftertouch(eventChannel, (int) (data[1] & 0x7F), (int) (data[2] & 0x7F));
                            break;
                        case TYPE_CONTROLLER:
                            controller(eventChannel, (int) (data[1] & 0x7F), (int) (data[2] & 0x7F));
                            break;
                        case TYPE_PROGRAM_CHANGE:
                            programChange(eventChannel, (int) (data[1] & 0x7F));
                            break;
                        case TYPE_CHANNEL_AFTERTOUCH:
                            channelAftertouch(eventChannel, (int) (data[1] & 0x7F));
                            break;
                        case TYPE_PITCH_BEND:
                            pitchBend(eventChannel, data[2] + data[1] * 256);
                            break;
                        default:
                            throw new MidiParserException("Invalid status byte: " + status);
                    }
                    break;
            }
        }
    }

    public Sequence getModifiedSequence(Sequence argSource, List<MySequencer.Data> data, int masterGain) throws MidiParserException, InvalidMidiDataException {
        Sequence result = null;
        Map<Integer, MySequencer.Data> channelMap = new HashMap<Integer, MySequencer.Data>();
        for (MySequencer.Data channelData : data) {
            channelMap.put(channelData.channel, channelData);
        }
        try {
            result = new Sequence(argSource.getDivisionType(), argSource.getResolution());
        } catch (InvalidMidiDataException e) {
            throw new MidiParserException(e);
        }
        Track[] tracks = argSource.getTracks();
        for (Track track : tracks) {
            Track newTrack = result.createTrack();
            for (int j = 0; j < track.size(); j++) {
                MidiEvent event = track.get(j);
                currentTick = event.getTick();
                MidiMessage message = event.getMessage();
                int status = message.getStatus();
                switch(status) {
                    case SysexMessage.SYSTEM_EXCLUSIVE:
                    case SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE:
                    case MetaMessage.META:
                        newTrack.add(event);
                        break;
                    default:
                        int eventChannel = channel(status);
                        MySequencer.Data channelDate = channelMap.get(eventChannel);
                        if (channelDate.active) {
                            byte[] rawMessage = message.getMessage();
                            ShortMessage newClick = new ShortMessage();
                            switch(type(status)) {
                                case TYPE_NOTE_OFF:
                                    newClick.setMessage(ShortMessage.NOTE_OFF, eventChannel, (int) (rawMessage[1] & 0x7F), (int) (rawMessage[2] & 0x7F) * channelDate.gain / MidiPlayer.GAIN_RANGE * masterGain / MidiPlayer.GAIN_RANGE);
                                    newTrack.add(new MidiEvent(newClick, currentTick));
                                    break;
                                case TYPE_NOTE_ON:
                                    newClick.setMessage(ShortMessage.NOTE_ON, eventChannel, (int) (rawMessage[1] & 0x7F), (int) (rawMessage[2] & 0x7F) * channelDate.gain / MidiPlayer.GAIN_RANGE * masterGain / MidiPlayer.GAIN_RANGE);
                                    newTrack.add(new MidiEvent(newClick, currentTick));
                                    break;
                                default:
                                    newTrack.add(event);
                                    break;
                            }
                        }
                        break;
                }
            }
        }
        return result;
    }

    public Sequence copy(Sequence argSource) throws MidiParserException {
        Sequence result = null;
        try {
            result = new Sequence(argSource.getDivisionType(), argSource.getResolution());
        } catch (InvalidMidiDataException e) {
            throw new MidiParserException(e);
        }
        Track[] tracks = argSource.getTracks();
        for (Track track : tracks) {
            Track newTrack = result.createTrack();
            for (int j = 0; j < track.size(); j++) {
                newTrack.add(track.get(j));
            }
        }
        return result;
    }

    private void initializeLastMetaData(Sequence argSequence) {
        channelData = new HashMap<Integer, ChannelMetaData>();
        if (argSequence.getDivisionType() == Sequence.PPQ) {
            barData = new BarTypeData(argSequence.getResolution(), argSequence.getTickLength());
            bpm = argSequence.getTickLength() / argSequence.getResolution() * 60000000 / argSequence.getMicrosecondLength();
        } else {
            barData = null;
        }
        tempoData = new TempoData();
    }

    private void initChanneMetalData(int argChannel) {
        if (channelData.get(argChannel) == null) {
            channelData.put(argChannel, new ChannelMetaData(argChannel));
        }
    }

    protected void programChange(int channel, int programNumber) {
        initChanneMetalData(channel);
        channelData.get(channel).programs.add(programNumber);
    }

    protected void timeSignature(int numerator, int denominator, int metronome, int thirtySeconds) {
        barData.add(currentTick, numerator, denominator, metronome, thirtySeconds);
    }

    protected void setTempo(int msPerQuarterNote) {
        tempoData.add(currentTick, msPerQuarterNote);
    }
}
