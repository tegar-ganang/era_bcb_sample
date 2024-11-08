package org.spnt.echo.repository;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spantus.core.wav.AudioManagerFactory;
import org.spantus.utils.FileUtils;
import org.spnt.dto.CorporaEntry;
import org.spnt.dto.CorporaEntryList;
import org.spnt.servlet.service.SpntEchoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Order;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import com.mongodb.DB;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import edu.mit.csail.sls.wami.audio.WamiResampleAudioInputStream;

@Repository
public class SpntEchoRepositoryImpl implements SpntEchoRepository {

    private static Logger LOG = LoggerFactory.getLogger(SpntEchoRepositoryImpl.class);

    @Autowired
    MongoOperations mongoOperations;

    @Autowired
    MongoDbFactory mongoDbFactory;

    @Override
    public CorporaEntryList findAll() {
        CorporaEntryList list = new CorporaEntryList();
        list.setCorporaEntry(mongoOperations.findAll(CorporaEntry.class));
        return list;
    }

    @Override
    public InputStream findLastEventAudio(Long lastEvent) throws FileNotFoundException {
        CorporaEntry corporaEntry = findLastEntry();
        if (corporaEntry != null) {
            GridFSDBFile gfsFile = findFile(corporaEntry.getFileName());
            if (gfsFile != null) {
                LOG.debug("[findLasEventAudio]Audio for {}  found", corporaEntry.getFileName());
                return gfsFile.getInputStream();
            } else {
                LOG.debug("[findLasEventAudio]Audio for {} not found", corporaEntry.getFileName());
            }
        }
        return null;
    }

    private GridFS newGridFS(DB db) {
        return new GridFS(db);
    }

    @Override
    public Long findLastEvent() {
        CorporaEntry corporaEntry = findLastEntry();
        if (corporaEntry != null) {
            return corporaEntry.getTimeStamp();
        }
        return null;
    }

    private CorporaEntry findLastEntry() {
        Query query = new Query();
        query.sort().on("timeStamp", Order.DESCENDING);
        query.limit(1);
        List<CorporaEntry> entry = mongoOperations.find(query, CorporaEntry.class);
        for (CorporaEntry corporaEntry : entry) {
            LOG.debug("[findLastEntry] last entry: {}", corporaEntry.getFileName());
            return corporaEntry;
        }
        LOG.debug("[findLastEntry] last entry not found");
        return null;
    }

    @Override
    public CorporaEntry store(AudioInputStream audioIn) {
        String tmpFile = storeTmp(audioIn);
        CorporaEntry entry;
        try {
            entry = store(new FileInputStream(tmpFile));
        } catch (FileNotFoundException e) {
            LOG.error("File not found", e);
            throw new IllegalArgumentException(e);
        }
        LOG.debug("Saved: {}", entry.getFileName());
        return entry;
    }

    /**
	 * @return 
	 * 
	 */
    @Override
    public CorporaEntry store(InputStream inputStream) {
        CorporaEntry entry = new CorporaEntry();
        String fileName = createName(entry.getObjectId());
        entry.setFileName(fileName);
        entry.setTimeStamp(System.currentTimeMillis());
        GridFSInputFile audioForInput;
        audioForInput = createFile(inputStream, fileName);
        audioForInput.save();
        mongoOperations.insert(entry);
        return entry;
    }

    /**
	 * @return
	 * 
	 */
    public String storeTmp(AudioInputStream audioIn) {
        File rootDir = new File(System.getProperty("java.io.tmpdir"));
        File file = FileUtils.findNextAvaibleFile(new File(rootDir, "out.wav").getAbsolutePath());
        String tmpFile = AudioManagerFactory.createAudioManager().save(audioIn, file.getAbsolutePath());
        return tmpFile;
    }

    /**
	 * 
	 * @param audioIn
	 * @param fileName
	 * @param InputStream
	 * @return
	 */
    private GridFSInputFile createFile(InputStream InputStream, String fileName) {
        GridFS gridFS = newGridFS(mongoDbFactory.getDb());
        GridFSInputFile audioForInput = gridFS.createFile(InputStream, fileName);
        return audioForInput;
    }

    /**
	 * 
	 * @param fileName
	 * @return
	 */
    private GridFSDBFile findFile(String fileName) {
        GridFS gridFS = newGridFS(mongoDbFactory.getDb());
        GridFSDBFile audioForOutput = gridFS.findOne(fileName);
        return audioForOutput;
    }

    private String createName(ObjectId objectId) {
        return objectId.toString() + ".wav";
    }

    @Override
    public CorporaEntry findById(String id) {
        Query query = new BasicQuery(String.format("{'_id' : { '$oid' : '%s' }}", id));
        CorporaEntry entry = mongoOperations.findOne(query, CorporaEntry.class);
        return entry;
    }

    @Override
    public GridFSDBFile findOutputById(String id) {
        CorporaEntry entry = findById(id);
        return findFile(entry.getFileName());
    }

    @Override
    public CorporaEntry delete(String id) {
        CorporaEntry corporaEntry = findById(id);
        if (corporaEntry == null) {
            mongoOperations.remove(corporaEntry);
            GridFS gridFS = newGridFS(mongoDbFactory.getDb());
            gridFS.remove(corporaEntry.getFileName());
        }
        return corporaEntry;
    }

    @Override
    public CorporaEntry store(InputStream inputStream, AudioFormat audioFormat, AudioFormat requiredFormat) {
        AudioInputStream audioIn = new AudioInputStream(new BufferedInputStream(inputStream), audioFormat, AudioSystem.NOT_SPECIFIED);
        if (audioFormat.getEncoding() != requiredFormat.getEncoding() || audioFormat.getSampleRate() != requiredFormat.getSampleRate() || audioFormat.getSampleSizeInBits() != requiredFormat.getSampleSizeInBits() || audioFormat.getChannels() != requiredFormat.getChannels() || audioFormat.getFrameSize() != requiredFormat.getFrameSize() || audioFormat.getFrameRate() != requiredFormat.getFrameRate() || audioFormat.isBigEndian() != requiredFormat.isBigEndian()) {
            LOG.debug("[doPost] Resampling");
            audioIn = new WamiResampleAudioInputStream(requiredFormat, audioIn);
        }
        CorporaEntry entry = store(audioIn);
        return entry;
    }
}
