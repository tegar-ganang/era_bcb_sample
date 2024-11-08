package org.esb.dao;

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
import org.esb.hive.DatabaseService;
import org.esb.jmx.JHiveRegistryException;
import org.esb.model.MediaFile;
import org.esb.model.MediaStream;
import org.esb.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaFilesDao {

    private static Logger _log = LoggerFactory.getLogger(MediaFilesDao.class);

    private MediaFilesDao() {
    }

    public static MediaFile getMediaFile(int id) throws JHiveRegistryException {
        MediaFile result = null;
        try {
            Connection con = DatabaseService.getConnection();
            PreparedStatement stmt = con.prepareStatement("select files.id as fid, streams.id as sid, streams.duration as sduration,files.duration as fduration, files.*, streams.* from files, streams where files.id = ? and files.id=streams.fileid");
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                MediaFile mf = new MediaFile();
                mf.setId(rs.getInt("fid"));
                mf.setFilename(rs.getString("filename"));
                mf.setPath(rs.getString("path"));
                mf.setSize(rs.getLong("size"));
                mf.setDuration(rs.getLong("fduration"));
                mf.setContainerType(rs.getString("container_type"));
                mf.setAlbum(rs.getString("metaalbum"));
                mf.setAuthor(rs.getString("author"));
                mf.setComment(rs.getString("comment"));
                mf.setCopyright(rs.getString("copyright"));
                mf.setBitrate(rs.getLong("bitrate"));
                mf.setFileType(rs.getInt("filetype"));
                mf.setGenre(rs.getInt("metagenre"));
                mf.setTitle(rs.getString("title"));
                mf.setTrack(rs.getInt("metatrack"));
                mf.setYear(rs.getInt("metayear"));
                mf.addStream(createStream(rs));
                while (rs.next()) {
                    mf.addStream(createStream(rs));
                }
                result = mf;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            _log.error("while reading MediaFile on server", ex);
            throw new JHiveRegistryException(ex);
        }
        return result;
    }

    private static MediaStream createStream(ResultSet rs) throws SQLException {
        MediaStream result = new MediaStream();
        result.setId(rs.getInt("sid"));
        result.setStartTime(rs.getLong("start_time"));
        result.setFirstDts(rs.getLong("first_dts"));
        result.setNumFrames(rs.getLong("nb_frames"));
        result.setTimeBaseNum(rs.getInt("time_base_num"));
        result.setTimeBaseDen(rs.getInt("time_base_den"));
        result.setCodecTimeBaseNum(rs.getInt("codec_time_base_num"));
        result.setCodecTimeBaseDen(rs.getInt("codec_time_base_den"));
        result.setTicksPerFrame(rs.getInt("ticks_per_frame"));
        result.setGopSize(rs.getInt("gop_size"));
        result.setPixelFormat(rs.getInt("pix_fmt"));
        result.setBitsPerCodedSample(rs.getInt("bits_per_coded_sample"));
        result.setExtraDataSize(rs.getInt("extra_data_size"));
        result.setExtraData(rs.getBytes("extra_data"));
        result.setStreamIndex(rs.getInt("stream_index"));
        result.setStreamType(rs.getInt("stream_type"));
        result.setCodecId(rs.getInt("codec"));
        result.setCodecType(rs.getInt("stream_type"));
        result.setCodecName(rs.getString("codec_name"));
        result.setWidth(rs.getInt("width"));
        result.setHeight(rs.getInt("height"));
        result.setBitrate(rs.getInt("bit_rate"));
        result.setFrameRateNum(rs.getInt("framerate_num"));
        result.setFrameRateDen(rs.getInt("framerate_den"));
        result.setSampleRate(rs.getInt("sample_rate"));
        result.setChannels(rs.getInt("channels"));
        result.setDuration(rs.getLong("sduration"));
        result.setSampleFormat(rs.getInt("sample_fmt"));
        result.setExtraCodecFlags(rs.getString("extra_profile_flags"));
        result.setFrameCount(rs.getInt("framecount"));
        result.setFlags(rs.getInt("flags"));
        return result;
    }

    public static int setMediaFile(MediaFile f) {
        int fileid = 0;
        try {
            Connection con = DatabaseService.getConnection();
            PreparedStatement stmt_file = con.prepareStatement("insert into files(filename, path, size, stream_count, container_type,title,author,copyright,comment,metaalbum,metayear,metatrack,metagenre,duration,bitrate,insertdate,filetype,parent) " + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            PreparedStatement stmt_stream = con.prepareStatement("insert into streams (fileid,stream_index, stream_type,codec, codec_name,framerate_num, framerate_den,start_time, first_dts,duration,nb_frames,time_base_num, time_base_den,codec_time_base_num,codec_time_base_den,ticks_per_frame, width, height, gop_size, pix_fmt,bit_rate, rate_emu, sample_rate, channels, sample_fmt, bits_per_coded_sample, priv_data_size, priv_data, extra_data_size,extra_data, extra_profile_flags, framecount, flags) values" + "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            File file = new File(f.getPath() + File.separator + f.getFilename());
            stmt_file.setString(1, file.getName());
            stmt_file.setString(2, file.getParent());
            stmt_file.setLong(3, f.getSize());
            stmt_file.setLong(4, f.getStreamCount());
            stmt_file.setString(5, f.getContainerType());
            stmt_file.setString(6, f.getTitle());
            stmt_file.setString(7, f.getAuthor());
            stmt_file.setString(8, f.getCopyright());
            stmt_file.setString(9, f.getComment());
            stmt_file.setString(10, f.getAlbum());
            stmt_file.setInt(11, f.getYear());
            stmt_file.setInt(12, f.getTrack());
            stmt_file.setInt(13, f.getGenre());
            stmt_file.setLong(14, f.getDuration());
            stmt_file.setLong(15, f.getBitrate());
            stmt_file.setDate(16, new Date(f.getInsertDate().getTime()));
            stmt_file.setLong(17, f.getFileType());
            if (f.getParent() != null) stmt_file.setLong(18, f.getParent().getId()); else stmt_file.setLong(18, 0L);
            stmt_file.execute();
            ResultSet gen = stmt_file.getGeneratedKeys();
            if (gen != null && gen.next()) fileid = gen.getInt(1);
            f.setId(fileid);
            for (MediaStream s : f.getStreams()) {
                stmt_stream.setInt(1, fileid);
                stmt_stream.setInt(2, s.getStreamIndex());
                stmt_stream.setInt(3, s.getCodecType());
                stmt_stream.setInt(4, s.getCodecId());
                stmt_stream.setString(5, s.getCodecName());
                stmt_stream.setInt(6, s.getFrameRateNum());
                stmt_stream.setInt(7, s.getFrameRateDen());
                stmt_stream.setLong(8, s.getStartTime());
                stmt_stream.setLong(9, s.getFirstDts());
                stmt_stream.setLong(10, s.getDuration());
                stmt_stream.setLong(11, s.getNumFrames());
                stmt_stream.setLong(12, s.getTimeBaseNum());
                stmt_stream.setLong(13, s.getTimeBaseDen());
                stmt_stream.setLong(14, s.getCodecTimeBaseNum());
                stmt_stream.setLong(15, s.getCodecTimeBaseDen());
                stmt_stream.setLong(16, s.getTicksPerFrame());
                stmt_stream.setLong(17, s.getWidth());
                stmt_stream.setLong(18, s.getHeight());
                stmt_stream.setLong(19, s.getGopSize());
                stmt_stream.setLong(20, s.getPixelFormat());
                stmt_stream.setLong(21, s.getBitrate());
                stmt_stream.setLong(22, 0L);
                stmt_stream.setLong(23, s.getSampleRate());
                stmt_stream.setLong(24, s.getChannels());
                stmt_stream.setLong(25, s.getSampleFormat());
                stmt_stream.setLong(26, s.getBitsPerCodedSample());
                stmt_stream.setLong(27, 0);
                stmt_stream.setNull(28, Types.BLOB);
                stmt_stream.setLong(29, s.getExtraDataSize());
                stmt_stream.setBytes(30, s.getExtraData());
                stmt_stream.setString(31, s.getExtraCodecFlags());
                stmt_stream.setInt(32, s.getFrameCount());
                stmt_stream.setInt(33, s.getFlags());
                stmt_stream.execute();
                ResultSet gens = stmt_stream.getGeneratedKeys();
                if (gens != null && gens.next()) s.setId(gens.getInt(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return fileid;
    }
}
