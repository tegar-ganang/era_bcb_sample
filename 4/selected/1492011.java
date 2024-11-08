package org.esb.hive;

import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.esb.model.MediaFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileScanner implements Runnable, FileScannerMBean {

    static Connection con;

    static PreparedStatement stmt = null;

    static PreparedStatement stmt_file = null;

    static PreparedStatement stmt_stream = null;

    static IContainer container = IContainer.make();

    static List<String> _files = new ArrayList<String>();

    int _filesScanned = 0;

    int _filesImported = 0;

    boolean _process = true;

    private static Logger _log = LoggerFactory.getLogger(FileScanner.class);

    private static FileImporter _importer = new FileImporter();

    public FileScanner() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = null;
        try {
            name = new ObjectName("org.esb.jmx:type=Filescanner");
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        }
        try {
            mbs.registerMBean(this, name);
        } catch (InstanceAlreadyExistsException e) {
            e.printStackTrace();
        } catch (MBeanRegistrationException e) {
            e.printStackTrace();
        } catch (NotCompliantMBeanException e) {
            e.printStackTrace();
        }
    }

    static {
        try {
            con = DatabaseService.getConnection();
            stmt = con.prepareStatement("select * from watch_folder");
            stmt_file = con.prepareStatement("insert into files(filename, path, size, stream_count, container_type,title,author,copyright,comment,metaalbum,metayear,metatrack,metagenre,duration,bitrate,insertdate,filetype,parent) " + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            stmt_stream = con.prepareStatement("insert into streams (fileid,stream_index, stream_type,codec, codec_name,framerate_num, framerate_den,start_time, first_dts,duration,nb_frames,time_base_num, time_base_den,codec_time_base_num,codec_time_base_den,ticks_per_frame, width, height, gop_size, pix_fmt,bit_rate, rate_emu, sample_rate, channels, sample_fmt, bits_per_coded_sample, priv_data_size, priv_data, extra_data_size,extra_data) values" + "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            Statement stmtf = con.createStatement();
            ResultSet rs = stmtf.executeQuery("select filename, path from files where parent = 0");
            _log.debug("loading files from DB");
            while (rs.next()) {
                _files.add(rs.getString(1) + rs.getString(2));
            }
            _log.debug("loading files from DB ready");
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private void scanDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files == null) return;
            for (File file : files) {
                if (file.isDirectory()) scanDirectory(file); else {
                    if (_files.contains(file.getName() + file.getParent())) {
                        return;
                    }
                    _files.add(file.getName() + file.getParent());
                    _filesScanned++;
                    MediaFile f = new MediaFile(file.getParent(), file.getName());
                    if (_importer.importFile(f) <= 0) {
                        _log.error("could not import file: " + file.getAbsoluteFile());
                    } else {
                        _filesImported++;
                    }
                }
            }
        }
    }

    public void computeNewFile2(File file) {
        if (_files.contains(file.getName() + file.getParent())) {
            return;
        }
        _files.add(file.getName() + file.getParent());
        _filesScanned++;
        if (container.open(file.getAbsoluteFile().toString(), IContainer.Type.READ, null) < 0) _log.error("could not open file: " + file.getAbsoluteFile()); else try {
            stmt_file.setString(1, file.getName());
            stmt_file.setString(2, file.getParent());
            stmt_file.setLong(3, container.getFileSize());
            stmt_file.setLong(4, container.getNumStreams());
            stmt_file.setString(5, container.getContainerFormat().getInputFormatShortName());
            stmt_file.setString(6, container.getMetaData().getValue("title"));
            stmt_file.setString(7, container.getMetaData().getValue("author"));
            stmt_file.setString(8, container.getMetaData().getValue("copyright"));
            stmt_file.setString(9, container.getMetaData().getValue("comment"));
            stmt_file.setString(10, container.getMetaData().getValue("album"));
            stmt_file.setString(11, container.getMetaData().getValue("year"));
            stmt_file.setString(12, container.getMetaData().getValue("track"));
            stmt_file.setString(13, container.getMetaData().getValue("genre"));
            stmt_file.setLong(14, container.getDuration());
            stmt_file.setLong(15, container.getBitRate());
            stmt_file.setDate(16, new Date(new java.util.Date().getTime()));
            stmt_file.setLong(17, 0L);
            stmt_file.setLong(18, 0L);
            stmt_file.execute();
            ResultSet gen = stmt_file.getGeneratedKeys();
            int fileid = 0;
            if (gen != null && gen.next()) fileid = gen.getInt(1);
            int nb_streams = container.getNumStreams();
            for (int a = 0; a < nb_streams; a++) {
                IStream stream = container.getStream(a);
                IStreamCoder codec = stream.getStreamCoder();
                stmt_stream.setInt(1, fileid);
                stmt_stream.setInt(2, stream.getIndex());
                stmt_stream.setInt(3, codec.getCodecType().swigValue());
                stmt_stream.setInt(4, codec.getCodecID().swigValue());
                stmt_stream.setString(5, "implementd later");
                stmt_stream.setInt(6, stream.getFrameRate().getNumerator());
                stmt_stream.setInt(7, stream.getFrameRate().getDenominator());
                stmt_stream.setLong(8, stream.getStartTime());
                stmt_stream.setLong(9, stream.getFirstDts());
                stmt_stream.setLong(10, stream.getDuration());
                stmt_stream.setLong(11, stream.getNumFrames());
                stmt_stream.setLong(12, stream.getTimeBase().getNumerator());
                stmt_stream.setLong(13, stream.getTimeBase().getDenominator());
                stmt_stream.setLong(14, codec.getTimeBase().getNumerator());
                stmt_stream.setLong(15, codec.getTimeBase().getDenominator());
                stmt_stream.setLong(16, codec.getPropertyAsLong("ticks_per_frame"));
                stmt_stream.setLong(17, codec.getWidth());
                stmt_stream.setLong(18, codec.getHeight());
                stmt_stream.setLong(19, codec.getNumPicturesInGroupOfPictures());
                stmt_stream.setLong(20, codec.getPixelType().swigValue());
                stmt_stream.setLong(21, codec.getBitRate());
                stmt_stream.setLong(22, 0L);
                stmt_stream.setLong(23, codec.getSampleRate());
                stmt_stream.setLong(24, codec.getChannels());
                stmt_stream.setLong(25, codec.getSampleFormat().swigValue());
                stmt_stream.setLong(26, codec.getPropertyAsLong("bits_per_coded_sample"));
                stmt_stream.setLong(27, 0);
                stmt_stream.setNull(28, Types.BLOB);
                stmt_stream.setLong(29, codec.getExtraDataSize());
                stmt_stream.setNull(30, Types.BLOB);
                stmt_stream.execute();
            }
            _filesImported++;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        container.close();
    }

    public void run() {
        while (true) {
            ResultSet rs;
            try {
                rs = stmt.executeQuery();
                while (rs.next()) {
                    String filename = rs.getString("infolder");
                    if (filename != null) {
                        File f = new File(filename);
                        scanDirectory(f);
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public int getScannedFileCount() {
        return _filesScanned;
    }

    public int getImportedFileCount() {
        return _filesImported;
    }

    public void start() {
        _process = true;
    }

    public void stop() {
        _process = false;
    }
}
