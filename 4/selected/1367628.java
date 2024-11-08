package org.esb.hive;

import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import java.io.File;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import org.esb.dao.MediaFilesDao;
import org.esb.model.MediaFile;
import org.esb.model.MediaStream;
import org.esb.util.Util;
import org.hibernate.classic.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileImporter {

    static Connection con;

    static PreparedStatement stmt_file = null;

    static PreparedStatement stmt_stream = null;

    private static Logger _log = LoggerFactory.getLogger(FileImporter.class);

    static {
        try {
            con = DatabaseService.getConnection();
            stmt_file = con.prepareStatement("insert into files(filename, path, size, stream_count, container_type,title,author,copyright,comment,metaalbum,metayear,metatrack,metagenre,duration,bitrate,insertdate,filetype,parent) " + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            stmt_stream = con.prepareStatement("insert into streams (fileid,stream_index, stream_type,codec, codec_name,framerate_num, framerate_den,start_time, first_dts,duration,nb_frames,time_base_num, time_base_den,codec_time_base_num,codec_time_base_den,ticks_per_frame, width, height, gop_size, pix_fmt,bit_rate, rate_emu, sample_rate, channels, sample_fmt, bits_per_coded_sample, priv_data_size, priv_data, extra_data_size,extra_data) values" + "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public FileImporter() {
        super();
    }

    public static int importFile(MediaFile f) {
        int fileid = 0;
        File file = new File(f.getPath() + File.separator + f.getFilename());
        _log.debug("try importing file:" + file.getAbsoluteFile().toString());
        IContainer container = IContainer.make();
        if (container.open(file.getAbsoluteFile().toString(), IContainer.Type.READ, null) < 0) _log.error("could not open file: " + file.getAbsoluteFile()); else try {
            Util.toMediaFile(container, f);
            fileid = MediaFilesDao.setMediaFile(f);
            if (false) {
                stmt_file.setString(1, f.getFilename());
                stmt_file.setString(2, f.getPath());
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
                if (gen != null && gen.next()) fileid = gen.getInt(1);
                f.setId(fileid);
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
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        container.close();
        return fileid;
    }
}
