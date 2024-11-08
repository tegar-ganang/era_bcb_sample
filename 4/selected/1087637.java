package org.jtv.backend;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import org.jtv.common.RecordingData;
import org.jtv.common.RecordingTimeWindow;
import org.jtv.common.TvChannel;
import org.jtv.common.TvChannels;
import org.jtv.common.TvController;
import org.jtv.common.TvControllerEvent;
import org.jtv.common.TvControllerObservers;
import org.jtv.common.TvControllerResult;
import org.jtv.common.TvRecordingInfo;

public class MasterController implements TvController, TvControllerMXBean {

    private static final int IOS_BUFFER_SIZE = 64 * 1024;

    private class RecordingTask extends Timer.Task {

        private int channelNumber;

        private String fileName;

        private long end;

        public RecordingTask(long start, long end, int channelNumber, String fileName) {
            super(start);
            this.channelNumber = channelNumber;
            this.fileName = fileName;
            this.end = end;
        }

        public void execute(Timer timer) {
            try {
                OutputStream out = new FileOutputStream(fileName);
                int tunerNumber = startRecording(channelNumber, out);
                timer.schedule(new StopRecordingTask(end, tunerNumber, out));
            } catch (Exception e) {
                e.printStackTrace();
                TvControllerEvent event = new TvControllerEvent(TvControllerEvent.Type.ERROR, "start recording: cannot start recording");
                event.addProperty("exception", e);
                observers.event(event);
            }
        }

        public String toString() {
            return "recording: start " + new Date(getStart()) + " end " + new Date(end) + " channelNumber " + channelNumber + " name " + fileName;
        }
    }

    private class StopRecordingTask extends Timer.Task {

        private int tunerNumber;

        private OutputStream out;

        public StopRecordingTask(long start, int tunerNumber, OutputStream out) {
            super(start, -10);
            this.tunerNumber = tunerNumber;
            this.out = out;
        }

        public void execute(Timer timer) {
            stopRecording(tunerNumber, out);
        }

        public String toString() {
            return "stop recording: " + new Date(getStart()) + " tuner " + tunerNumber;
        }
    }

    private TvControllerObservers observers;

    private Timer timer;

    private List<TvTuner> tuners;

    private InputToOutputStreams[] ioss;

    private TvRecordingInfo recordingInfo;

    private TvChannels tvChannels;

    private TvChannel[] tunedChannels;

    public MasterController(List<TvTuner> tuners, TvChannels tvChannels, TvRecordingInfo recordingInfo) {
        super();
        this.observers = new TvControllerObservers();
        this.tuners = tuners;
        initIos();
        this.timer = new Timer();
        timer.start();
        this.tvChannels = tvChannels;
        this.tunedChannels = new TvChannel[tuners.size()];
        for (int i = 0; i < tunedChannels.length; i++) {
            changeChannel(i, tvChannels.getFirst().getNumber());
        }
        this.recordingInfo = recordingInfo;
        timer.schedule(new Timer.Task(System.currentTimeMillis() + 500) {

            public void execute(Timer timer) {
                synchronizeRecordingInfo();
            }
        });
    }

    private void initIos() {
        ioss = new InputToOutputStreams[tuners.size()];
        for (int i = 0; i < tuners.size(); i++) {
            final TvTuner tuner = tuners.get(i);
            ioss[i] = new InputToOutputStreams(new InputToOutputStreams.InputStreamFactory() {

                public synchronized InputStream create() throws IOException {
                    InputStream inputStream = tuner.getInputStream();
                    if (inputStream == null) {
                        TvControllerEvent event = new TvControllerEvent(TvControllerEvent.Type.ERROR, "cannot get input stream from " + tuner);
                        observers.event(event);
                    }
                    return inputStream;
                }
            }, IOS_BUFFER_SIZE, tuner.toString());
            ioss[i].start();
        }
    }

    public TvControllerObservers getObservers() {
        return observers;
    }

    public synchronized void close() {
        recordingInfo = null;
        timer.close();
        timer = null;
        for (InputToOutputStreams ios : ioss) {
            ios.close();
        }
        for (TvTuner tuner : tuners) {
            tuner.close();
        }
        tuners = null;
        TvControllerEvent event = new TvControllerEvent(TvControllerEvent.Type.STOPPED, "close");
        observers.event(event);
        observers = null;
    }

    public synchronized TvControllerResult watch(int tunerNumber, OutputStream out) {
        ioss[tunerNumber].addOutputStream(out);
        return new TvControllerResult(TvController.Operation.WATCH, true, "watching tuner " + tunerNumber);
    }

    public synchronized TvControllerResult synchronizeRecordingInfo() {
        timer.clear(new Timer.Condition() {

            public boolean check(Timer.Task task) {
                return task instanceof RecordingTask;
            }
        });
        SortedSet<RecordingData> datas = recordingInfo.getFuture(System.currentTimeMillis());
        for (RecordingData data : datas) {
            RecordingTask task = new RecordingTask(data.getStart(), data.getEnd(), data.getChannel(), data.getName());
            timer.schedule(task);
        }
        TvControllerEvent event = new TvControllerEvent(TvControllerEvent.Type.SYNCHRONIZED_RECORDINGS, "synchronizeRecordingInfo: synchronized recordings");
        event.addProperty("recordingCount", datas.size());
        observers.event(event);
        return new TvControllerResult(TvController.Operation.SYNCHRONIZE_RECORDING_INFO, true, datas.size() + " recordings");
    }

    public synchronized TvControllerResult scheduleRecording(int channelNumber, long start, long end, String name) {
        if (end <= start) {
            return new TvControllerResult(TvController.Operation.SCHEDULE_RECORDING, false, "end time must be greater than start time");
        }
        int numTuners = getNumberOfTuners();
        int tunersNeeded = calculateRecordingWindowInternal(start, end).tunersNeeded();
        if (tunersNeeded < numTuners) {
            recordingInfo.scheduleRecording(channelNumber, start, end, name);
            synchronizeRecordingInfo();
            TvControllerEvent event = new TvControllerEvent(TvControllerEvent.Type.SCHEDULED_RECORDING, "scheduleRecording: scheduled recording");
            event.addProperty("channelNumber", channelNumber);
            event.addProperty("start", start);
            event.addProperty("end", end);
            event.addProperty("name", name);
            observers.event(event);
            return new TvControllerResult(TvController.Operation.SCHEDULE_RECORDING, true, "recording scheduled");
        } else {
            return new TvControllerResult(TvController.Operation.SCHEDULE_RECORDING, false, "no tuner available");
        }
    }

    public int getNumberOfTuners() {
        return tuners.size();
    }

    public synchronized int getChannel(int tunerNumber) {
        return tunedChannels[tunerNumber].getNumber();
    }

    public synchronized TvControllerResult changeChannel(int tunerNumber, int channelNumber) {
        TvTuner tuner = getTuner(tunerNumber);
        if (tuner.isFrequencyLocked()) {
            TvControllerEvent event = new TvControllerEvent(TvControllerEvent.Type.ERROR, "changeChannel: tuner locked");
            event.addProperty("tunerNumber", tunerNumber);
            event.addProperty("channelNumber", channelNumber);
            observers.event(event);
            return new TvControllerResult(TvController.Operation.CHANGE_CHANNEL, false, "tuner " + tuner + " is locked");
        } else {
            TvChannel channel = tvChannels.get(channelNumber);
            tuner.setFrequency(channel.getFrequency());
            tunedChannels[tunerNumber] = channel;
            TvControllerEvent event = new TvControllerEvent(TvControllerEvent.Type.CHANNEL_CHANGED, "changeChannel: channel changed");
            event.addProperty("tunerNumber", tunerNumber);
            event.addProperty("channelNumber", channelNumber);
            observers.event(event);
            return new TvControllerResult(TvController.Operation.CHANGE_CHANNEL, true, "changed channel to " + channel + " for tuner " + tunerNumber);
        }
    }

    private TvTuner getTuner(int tunerNumber) {
        TvTuner tuner = tuners.get(tunerNumber);
        return tuner;
    }

    private synchronized int startRecording(int channelNumber, OutputStream out) {
        for (int i = 0; i < getNumberOfTuners(); i++) {
            TvTuner tuner = tuners.get(i);
            if (!tuner.isFrequencyLocked()) {
                int tunerNumber = tuners.indexOf(tuner);
                TvChannel channel = tvChannels.get(channelNumber);
                tuner.setFrequency(channel.getFrequency());
                tuner.lockFrequency();
                tunedChannels[tunerNumber] = channel;
                TvControllerEvent event = new TvControllerEvent(TvControllerEvent.Type.CHANNEL_CHANGED, "startRecording: changed channel and locked tuner");
                event.addProperty("tunerNumber", tunerNumber);
                event.addProperty("channelNumber", channelNumber);
                observers.event(event);
                ioss[tunerNumber].addOutputStream(out);
                TvControllerEvent event2 = new TvControllerEvent(TvControllerEvent.Type.STARTED_RECORDING, "startRecording: started recording");
                event2.addProperty("tunerNumber", tunerNumber);
                observers.event(event2);
                return i;
            }
        }
        TvControllerEvent event = new TvControllerEvent(TvControllerEvent.Type.ERROR, "startRecording: no tuner free");
        event.addProperty("channelNumber", channelNumber);
        observers.event(event);
        return -1;
    }

    private synchronized void stopRecording(int tunerNumber, OutputStream out) {
        TvTuner tuner = getTuner(tunerNumber);
        if (tuner.isFrequencyLocked()) {
            ioss[tunerNumber].removeOutputStream(out);
            tuner.unlockFrequency();
            TvControllerEvent event2 = new TvControllerEvent(TvControllerEvent.Type.STOPPED_RECORDING, "stopRecording: recording");
            event2.addProperty("tunerNumber", tunerNumber);
            observers.event(event2);
        } else {
            TvControllerEvent event = new TvControllerEvent(TvControllerEvent.Type.ERROR, "stopRecording: tuner is not recording");
            event.addProperty("tunerNumber", tunerNumber);
            observers.event(event);
        }
    }

    private RecordingTimeWindow calculateRecordingWindowInternal(long start, long end) {
        RecordingTimeWindow timeWindow = new RecordingTimeWindow(start, end);
        Iterator<RecordingData> iter = recordingInfo.getFuture(0).iterator();
        while (iter.hasNext()) {
            RecordingData data = iter.next();
            if (data.getStart() > end) {
                break;
            }
            timeWindow.add(data);
        }
        return timeWindow;
    }

    public synchronized SortedSet<RecordingData> getRecordingsFrom(long from) {
        return recordingInfo.getFuture(from);
    }

    public synchronized TvControllerResult cancelRecordingId(int id) {
        recordingInfo.cancelRecordingId(id);
        synchronizeRecordingInfo();
        return new TvControllerResult(TvController.Operation.CANCEL_RECORDING_ID, true, "cancelled recording with id " + id);
    }

    public long getTime() {
        return System.currentTimeMillis();
    }
}
