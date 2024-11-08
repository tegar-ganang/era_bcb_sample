package org.rdv.rbnb;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import org.rdv.data.NumericDataSample;
import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Sink;

/**
 * A class to read numeric data from a RBNB server.
 * 
 * @author Jason P. Hanley
 */
public class RBNBReader {

    /** the sink to get data from */
    private final Sink sink;

    /** the list of channels to get data from */
    private List<String> channels;

    /** the start and end times to get data */
    private double startTime, endTime;

    /** the channel map for the sink */
    private ChannelMap cmap;

    /** the time of the last data sample */
    private double time;

    /** a buffer of samples read from the server */
    private Queue<Sample> samples;

    /**
   * Connects to the RBNB server and preparse to get data.
   * 
   * @param rbnbServer      the address of the rbnb server
   * @param channels        the channels to get data for
   * @param startTime       the start time for the data
   * @param endTime         the end time of the data
   * @throws SAPIException  if there is an error connecting to the server
   */
    public RBNBReader(String rbnbServer, List<String> channels, double startTime, double endTime) throws SAPIException {
        this.channels = channels;
        this.startTime = startTime;
        this.endTime = endTime;
        sink = new Sink();
        sink.OpenRBNBConnection(rbnbServer, "RDVExport");
        cmap = new ChannelMap();
        for (String channel : channels) {
            cmap.Add(channel);
        }
        time = startTime;
        samples = new PriorityQueue<Sample>();
    }

    /**
   * Read a sample from the RBNB server. When there is no more data, this will
   * return null.
   * 
   * @return                a sample of data
   * @throws SAPIException  if there is an error getting the data
   */
    public NumericDataSample readSample() throws SAPIException {
        while (samples.isEmpty()) {
            if (time >= endTime) {
                return null;
            }
            fetchDataBlock();
        }
        Number[] data = new Number[channels.size()];
        for (int i = 0; i < channels.size(); i++) {
            data[i] = Double.NaN;
        }
        double t = samples.peek().getTime();
        while (!samples.isEmpty() && t == samples.peek().getTime()) {
            Sample sample = samples.remove();
            int channelIndex = channels.indexOf(sample.getChannel());
            data[channelIndex] = sample.getData();
        }
        NumericDataSample dataSample = new NumericDataSample(t, data);
        return dataSample;
    }

    /**
   * Fetches a block of data from the server and adds it to the sample queue.
   * 
   * @throws SAPIException  if there is an error fetching the data
   */
    private void fetchDataBlock() throws SAPIException {
        double duration = 2;
        if (time + duration > endTime) {
            duration = endTime - time;
        }
        sink.Request(cmap, time, duration, "absolute");
        ChannelMap dmap = sink.Fetch(-1);
        for (int i = 0; i < channels.size(); i++) {
            String channel = channels.get(i);
            int index = dmap.GetIndex(channel);
            if (index != -1) {
                int type = dmap.GetType(index);
                double[] times = dmap.GetTimes(index);
                for (int j = 0; j < times.length; j++) {
                    Sample sample;
                    if (times[j] > startTime && times[j] <= time) {
                        continue;
                    }
                    switch(type) {
                        case ChannelMap.TYPE_INT32:
                            sample = new Sample(channel, dmap.GetDataAsInt32(index)[j], times[j]);
                            break;
                        case ChannelMap.TYPE_INT64:
                            sample = new Sample(channel, dmap.GetDataAsInt64(index)[j], times[j]);
                            break;
                        case ChannelMap.TYPE_FLOAT32:
                            sample = new Sample(channel, dmap.GetDataAsFloat32(index)[j], times[j]);
                            break;
                        case ChannelMap.TYPE_FLOAT64:
                            sample = new Sample(channel, dmap.GetDataAsFloat64(index)[j], times[j]);
                            break;
                        default:
                            sample = new Sample(channel, null, times[j]);
                    }
                    samples.offer(sample);
                }
            }
        }
        time += duration;
    }

    /**
   * Closes the connection to the RBNB server.
   */
    public void close() {
        sink.CloseRBNBConnection();
    }

    /**
   * A class to hold a data sample with its name, sample time, and value.
   */
    class Sample implements Comparable<Sample> {

        String channel;

        Number data;

        double time;

        public Sample(String channel, Number data, double time) {
            this.channel = channel;
            this.data = data;
            this.time = time;
        }

        public String getChannel() {
            return channel;
        }

        public Number getData() {
            return data;
        }

        public double getTime() {
            return time;
        }

        public int compareTo(Sample sample2) {
            double t1 = getTime();
            double t2 = sample2.getTime();
            if (t1 == t2) {
                return 0;
            } else if (t1 < t2) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
