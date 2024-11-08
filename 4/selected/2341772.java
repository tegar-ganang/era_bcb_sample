package org.esb.hive;

import com.xuggle.xuggler.Global;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IRational;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import org.esb.av.Packet;
import org.esb.jmx.JHiveRegistry;
import org.esb.jmx.JHiveRegistryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessUnitController extends Thread implements ProcessUnitControllerMBean {

    private static Logger _log = LoggerFactory.getLogger(ProcessUnitController.class);

    private PreparedStatement _jobs = null;

    private PreparedStatement _streams = null;

    private PreparedStatement _stmtpu = null;

    private PreparedStatement _stmtpu_upd = null;

    private Connection con = null;

    private Statement _stmt = null;

    private boolean _run;

    private Hashtable<Integer, StreamData> _stream_map = new Hashtable<Integer, StreamData>();

    private static ArrayBlockingQueue<ProcessUnit> _puqueue = new ArrayBlockingQueue<ProcessUnit>(30);

    private static ArrayBlockingQueue<ProcessUnit> _videoqueue = new ArrayBlockingQueue<ProcessUnit>(10);

    private static ArrayBlockingQueue<ProcessUnit> _audioqueue = new ArrayBlockingQueue<ProcessUnit>(50);

    private static Object _waiter = new Object();

    public ProcessUnitController() throws SQLException {
        setName("ProcessUnitController");
        try {
            JHiveRegistry.registerMBean(this, "org.esb.jmx:type=ProcessUnitController");
        } catch (JHiveRegistryException e) {
            _log.error("register MBean", e);
        }
        con = DatabaseService.getConnection();
        _jobs = con.prepareStatement("select jobs.id, files.path, files.filename, jobs.deinterlace, jobs.keep_display_aspect from jobs, files where jobs.inputfile=files.id and jobs.jobcomplete ='1970-01-01 00:00:00.0'");
        _streams = con.prepareStatement("select * from job_details, streams where job_id=? and streams.id=instream");
        _stmtpu = con.prepareStatement("insert into process_units (source_stream, target_stream, start_ts, end_ts, frame_count, send, complete, priority) values (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, null,0)", Statement.RETURN_GENERATED_KEYS);
        _stmtpu_upd = con.prepareStatement("update process_units set complete = CURRENT_TIMESTAMP where id=?");
        _run = true;
    }

    public void run() {
        while (_run) {
            ResultSet rs;
            try {
                rs = _jobs.executeQuery();
                while (rs.next()) {
                    _stream_map.clear();
                    _stmt = con.createStatement();
                    String filename = rs.getString("path") + "/" + rs.getString("filename");
                    _log.debug("opening file:" + filename);
                    IContainer container = IContainer.make();
                    if (container.open(filename, IContainer.Type.READ, null) < 0) {
                        _log.error("could not open file: " + filename);
                        continue;
                    }
                    _streams.setInt(1, rs.getInt("ID"));
                    ResultSet rsstr = _streams.executeQuery();
                    _stmt.execute("update jobs set jobbegin=CURRENT_TIMESTAMP where id=" + rs.getInt("ID"));
                    while (rsstr.next()) {
                        Integer index = rsstr.getInt("stream_index");
                        _stream_map.put(index, new StreamData());
                        _stream_map.get(index).deinterlace = rs.getInt("deinterlace");
                        _stream_map.get(index).instream = rsstr.getInt("instream");
                        _stream_map.get(index).outstream = rsstr.getInt("outstream");
                        _stream_map.get(index).type = rsstr.getInt("stream_type");
                        _stream_map.get(index).decoder = CodecFactory.getDecoder(_stream_map.get(index).instream);
                        _stream_map.get(index).encoder = CodecFactory.getEncoder(_stream_map.get(index).outstream);
                        _stream_map.get(index).encoder.open();
                        IRational ar = IRational.make(rsstr.getInt("time_base_num"), rsstr.getInt("time_base_den"));
                        _stream_map.get(index).stream_time_base = ar;
                        _stream_map.get(index).last_start_dts = rsstr.getLong("first_dts") - 1;
                        _stream_map.get(index).last_start_pts = rsstr.getLong("start_time") - 1;
                        _stream_map.get(index).packet_count = 0;
                        _stream_map.get(index).last_bytes_offset = 0;
                        _stream_map.get(index).process_unit_count = 0;
                        _stream_map.get(index).frameRateCompensateBase = 0;
                        _stream_map.get(index).b_frame_offset = 0;
                        container.getStream(index).getStreamCoder().open();
                        _stream_map.get(index).decoder.getStreamCoder().setDefaultAudioFrameSize(container.getStream(index).getStreamCoder().getAudioFrameSize());
                        _stream_map.get(index).out_frame_size = _stream_map.get(index).encoder.getStreamCoder().getAudioFrameSize();
                        container.getStream(index).getStreamCoder().close();
                    }
                    long all_start_time = Global.NO_PTS;
                    IRational r = IRational.make(1, 1000000);
                    for (Integer key : _stream_map.keySet()) {
                        StreamData data = _stream_map.get(key);
                        all_start_time = Math.max(r.rescale(data.last_start_dts, data.stream_time_base), all_start_time);
                    }
                    _log.debug("All Start Time=" + all_start_time);
                    for (Integer key : _stream_map.keySet()) {
                        StreamData data = _stream_map.get(key);
                        data.last_start_dts = data.stream_time_base.rescale(all_start_time, r);
                    }
                    Packetizer packetizer = new Packetizer(_stream_map);
                    IPacket packet = IPacket.make();
                    int result = 0;
                    ProcessUnitBuilder pu_builder = new ProcessUnitBuilder(_stream_map);
                    while ((result = container.readNextPacket(packet)) == 0) {
                        if (_stream_map.get(packet.getStreamIndex()) == null || (packet.getDts() < _stream_map.get(packet.getStreamIndex()).last_start_dts)) {
                            if (_stream_map.get(packet.getStreamIndex()) != null) _log.debug("starttime not reached, dropping packet idx=" + packet.getStreamIndex() + " - > " + packet.getDts() + "<" + _stream_map.get(packet.getStreamIndex()).last_start_dts);
                            continue;
                        }
                        if (packetizer.putPacket(new Packet(packet))) {
                            List<Packet> packets = packetizer.getPacketList();
                            ProcessUnit pu = pu_builder.buildProcessUnit(packets);
                            if (pu != null) {
                                try {
                                    _puqueue.put(pu);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    int count = packetizer.getListCount();
                    for (int a = 0; a < count; a++) {
                        List<Packet> packets = packetizer.getPacketList();
                        ProcessUnit pu = pu_builder.buildProcessUnit(packets);
                        if (pu != null) {
                            try {
                                _puqueue.put(pu);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    container.close();
                    container.delete();
                    try {
                        _log.debug("file complete and waiting now to receive pending packets");
                        synchronized (this) {
                            wait();
                        }
                        _log.debug("pending packets received");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    _stmt.execute("update jobs set jobcomplete=CURRENT_TIMESTAMP, status='completed' where id=" + rs.getInt("ID"));
                    _log.debug("setting job# as completed:" + rs.getInt("ID"));
                    _log.debug("file completed:" + filename);
                    _stmt.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void buildProcessUnit(List<Packet> packets, boolean flush) {
        int idx = packets.get(0).getStreamIndex();
        ICodec.Type codec_type = _stream_map.get(idx).decoder.getStreamCoder().getCodecType();
        ProcessUnit pu;
        if (codec_type == ICodec.Type.CODEC_TYPE_VIDEO) {
            pu = new VideoProcessUnit();
        } else if (codec_type == ICodec.Type.CODEC_TYPE_AUDIO) {
            pu = new AudioProcessUnit();
        } else {
            return;
        }
        pu.setInputPackets(packets);
        pu.setDecoder(_stream_map.get(idx).decoder);
        pu.setEncoder(_stream_map.get(idx).encoder);
        pu.setSourceStream(_stream_map.get(idx).instream);
        pu.setTargetStream(_stream_map.get(idx).outstream);
        if (codec_type == ICodec.Type.CODEC_TYPE_VIDEO) {
            pu.setRateCompensateBase(_stream_map.get(idx).frameRateCompensateBase);
            IRational inrate = _stream_map.get(idx).decoder.getFrameRate();
            IRational outrate = _stream_map.get(idx).encoder.getFrameRate();
            int packet_count = packets.size() - (_stream_map.get(idx).decoder.getStreamCoder().getCodecID() == ICodec.ID.CODEC_ID_MPEG2VIDEO ? 1 : 0);
            double in = ((double) packet_count * inrate.getDenominator() / inrate.getNumerator()) * ((double) outrate.getNumerator() / (double) outrate.getDenominator());
            in += _stream_map.get(idx).frameRateCompensateBase;
            _stream_map.get(idx).frameRateCompensateBase = in - Math.floor(in);
            pu.setEsteminatedPacketCount((int) Math.floor(in));
            _log.debug("new VideoProcessUnit queued with size:" + pu.getPacketCount());
        } else if (codec_type == ICodec.Type.CODEC_TYPE_AUDIO) {
            pu.setRateCompensateBase(_stream_map.get(idx).frameRateCompensateBase);
            int channels = _stream_map.get(idx).decoder.getStreamCoder().getChannels();
            channels = channels > 2 ? 2 : channels;
            double inframe_size = (double) (_stream_map.get(idx).in_frame_size * 2 * channels) / (double) (_stream_map.get(idx).decoder.getStreamCoder().getSampleRate()) * (double) (_stream_map.get(idx).encoder.getStreamCoder().getSampleRate());
            long outframe_size = _stream_map.get(idx).out_frame_size * 2 * _stream_map.get(idx).encoder.getStreamCoder().getChannels();
            long size = packets.size() * (long) inframe_size;
            size += _stream_map.get(idx).frameRateCompensateBase;
            long rest = size % outframe_size;
            int estimated_packet_count = Long.valueOf(size / outframe_size).intValue();
            pu.setEsteminatedPacketCount(estimated_packet_count);
            _stream_map.get(idx).frameRateCompensateBase = rest;
            _log.debug("new AudioProcessUnit queued with size:" + pu.getPacketCount());
        }
        try {
            _puqueue.put(pu);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized ProcessUnit getProcessUnit() throws SQLException {
        if (_puqueue.size() == 0) notifyAll();
        ProcessUnit unit = _puqueue.poll();
        if (unit != null) {
            _stmtpu.setInt(1, unit.getSourceStream());
            _stmtpu.setInt(2, unit.getTargetStream());
            _stmtpu.setLong(3, unit.getStartTimeStamp());
            _stmtpu.setLong(4, unit.getEndTimeStamp());
            _stmtpu.setLong(5, unit.getPacketCount());
            _stmtpu.execute();
            ResultSet rs = _stmtpu.getGeneratedKeys();
            if (rs.next()) unit.setPuId(rs.getLong(1));
        }
        return unit;
    }

    public synchronized ProcessUnit getVideoProcessUnit() throws SQLException {
        if (_videoqueue.size() == 0 && _audioqueue.size() == 0) notifyAll();
        ProcessUnit unit = _videoqueue.poll();
        if (unit != null) {
            _stmtpu.setInt(1, unit.getSourceStream());
            _stmtpu.setInt(2, unit.getTargetStream());
            _stmtpu.setLong(3, unit.getStartTimeStamp());
            _stmtpu.setLong(4, unit.getEndTimeStamp());
            _stmtpu.setLong(5, unit.getPacketCount());
            _stmtpu.execute();
            ResultSet rs = _stmtpu.getGeneratedKeys();
            if (rs.next()) unit.setPuId(rs.getLong(1));
        }
        return unit;
    }

    public synchronized ProcessUnit getAudioProcessUnit() throws SQLException {
        if (_videoqueue.size() == 0 && _audioqueue.size() == 0) notifyAll();
        ProcessUnit unit = _audioqueue.poll();
        if (unit != null) {
            _stmtpu.setInt(1, unit.getSourceStream());
            _stmtpu.setInt(2, unit.getTargetStream());
            _stmtpu.setLong(3, unit.getStartTimeStamp());
            _stmtpu.setLong(4, unit.getEndTimeStamp());
            _stmtpu.setLong(5, unit.getPacketCount());
            _stmtpu.execute();
            ResultSet rs = _stmtpu.getGeneratedKeys();
            if (rs.next()) unit.setPuId(rs.getLong(1));
        }
        return unit;
    }

    public synchronized void putProcessUnit(ProcessUnit unit) throws SQLException {
        String filename = System.getProperty("user.dir");
        filename += File.separator;
        filename += "tmp" + File.separator;
        filename += unit.getPuId() % 10;
        filename += File.separator;
        filename += unit.getPuId() + ".unit";
        File outfile = new File(filename);
        outfile.getParentFile().mkdirs();
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(outfile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(unit);
            oos.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        _stmtpu_upd.setLong(1, unit.getPuId());
        _stmtpu_upd.execute();
    }

    public int getVideoProcessUnitCount() {
        return _videoqueue.size();
    }

    public int getAudioProcessUnitCount() {
        return _audioqueue.size();
    }
}
