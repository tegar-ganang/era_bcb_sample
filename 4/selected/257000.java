package gov.lanl.xmltape.create;

import gov.lanl.util.uuid.UUIDFactory;
import gov.lanl.util.DateUtil;
import gov.lanl.xmltape.SingleTapeWriter;
import gov.lanl.xmltape.TapeException;
import gov.lanl.xmltape.TapeRecord;
import java.io.CharArrayWriter;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

/**
 * TapeCreateInterface implementation using Regular Expressions to
 * parse input and generate XMLtape
 *
 */
public class RegExTapeCreateImpl implements TapeCreateInterface {

    public static final int INITIAL_BUFFER_SIZE = 1000 * 1024;

    static Logger log = Logger.getLogger(RegExTapeCreateImpl.class.getName());

    String recordmatch = null;

    String identifier_regex = null;

    String meta_regex = null;

    String datestamp_regex = null;

    boolean identifier_implicit = false;

    boolean datestamp_implicit = false;

    String datestamp_file = null;

    Pattern recordmatch_pattern = null;

    Pattern identifier_regex_pattern = null;

    Pattern meta_regex_pattern = null;

    Pattern datestamp_regex_pattern = null;

    TapeCreateConfig props = null;

    int sum_records = 0;

    boolean initialized = false;

    private void init() throws Exception {
        if (props == null) throw new Exception("Properties file must be initialized");
        recordmatch = props.getRecordLocationCriteria();
        identifier_regex = props.getRecordIdentifierCriteria();
        meta_regex = props.getRecordMetadataCriteria();
        datestamp_regex = props.getRecordDatestampCriteria();
        log.debug("recordmatch=" + recordmatch);
        log.debug("identifier_regex=" + identifier_regex);
        log.debug("datestamp_regex=" + datestamp_regex);
        log.debug("meta_regex=" + meta_regex);
        if (recordmatch != null && recordmatch.trim().length() > 0) recordmatch_pattern = Pattern.compile(recordmatch, Pattern.DOTALL);
        if (identifier_regex != null && identifier_regex.equals("AUTO")) identifier_implicit = true; else if (identifier_regex != null && identifier_regex.trim().length() > 0) identifier_regex_pattern = Pattern.compile(identifier_regex);
        if (datestamp_regex != null && datestamp_regex.equals("AUTO")) datestamp_implicit = true; else if (datestamp_regex != null && datestamp_regex.trim().length() > 0) datestamp_regex_pattern = Pattern.compile(datestamp_regex);
        if (meta_regex != null && meta_regex.trim().length() > 0) meta_regex_pattern = Pattern.compile(meta_regex);
        initialized = true;
    }

    /**
     * Reads input stream, performs regex pattern match, captures record, and writes
     * record to XMLtape
     */
    public int writeRecords(InputStreamReader reader, SingleTapeWriter tapewriter) throws Exception {
        if (!initialized) init();
        char[] buffer = new char[INITIAL_BUFFER_SIZE];
        int count = 0;
        CharArrayWriter charbuffer = new CharArrayWriter(INITIAL_BUFFER_SIZE);
        int recordend = 0;
        while ((count = reader.read(buffer)) != -1) {
            charbuffer.write(buffer, 0, count);
            String record = null;
            String records = charbuffer.toString();
            Matcher recordmatch_matcher = recordmatch_pattern.matcher(records);
            while (recordmatch_matcher.find()) {
                record = recordmatch_matcher.group();
                tapewriter.writeRecord(buildRecord(record));
                sum_records++;
                recordend = recordmatch_matcher.end();
            }
            if (recordend != 0) {
                charbuffer.reset();
                charbuffer.write(records.substring(recordend));
                recordend = 0;
            }
        }
        return sum_records;
    }

    private TapeRecord buildRecord(String record) throws TapeException {
        String datestamp = null;
        if (datestamp_regex_pattern != null && !datestamp_implicit) {
            Matcher datestamp_regex_matcher = datestamp_regex_pattern.matcher(record);
            if (datestamp_regex_matcher.find()) {
                datestamp = datestamp_regex_matcher.group(1);
            } else throw new TapeException("no datestamp match found for record");
            try {
                datestamp = TapeCreateUtilities.normalizeDate(datestamp);
            } catch (ParseException e) {
                throw new TapeException("Unable to parse datestamp format: \n " + e);
            }
        } else {
            datestamp = generateUTCDate();
        }
        String identifier = null;
        if (identifier_regex_pattern != null && !identifier_implicit) {
            Matcher identifier_regex_matcher = identifier_regex_pattern.matcher(record);
            if (identifier_regex_matcher.find()) {
                identifier = identifier_regex_matcher.group(1);
            } else throw new TapeException("no identifier match found for record");
            if (props.getRecordIdentifierPrefix() != null) identifier = props.getRecordIdentifierPrefix() + identifier;
            if (props.getRecordIdentifierSuffix() != null) {
                if (props.getRecordIdentifierSuffix().contains("${profile.record.datestamp}")) identifier = identifier + props.getRecordIdentifierSuffix().replace("${profile.record.datestamp}", datestamp); else identifier = identifier + props.getRecordIdentifierSuffix();
            }
            log.debug("Processing " + identifier);
        } else {
            identifier = createRecordInfoURI();
        }
        String meta = null;
        if (meta_regex_pattern != null) {
            Matcher meta_regex_matcher = meta_regex_pattern.matcher(record);
            if (meta_regex_matcher.find()) {
                meta = meta_regex_matcher.group(1);
            } else throw new TapeException("no metadata match found for record");
        } else meta = record;
        TapeRecord taperecord = new TapeRecord(identifier, datestamp, meta);
        return taperecord;
    }

    /**
     * Sets the properties object containing rexEx values
     * 
     * @param props
     *            an initialized TapeCreateConfig instance
     */
    public void setProcessingProperies(TapeCreateConfig props) {
        this.props = props;
    }

    private String createRecordInfoURI() {
        String uuid = UUIDFactory.generateUUID().toString();
        uuid = uuid.substring(9);
        String prefix = props.getLocalInfoURIPrefix();
        if (prefix != null) {
            uuid = prefix + "id/" + uuid;
        }
        return uuid;
    }

    /**
     * Gets a valid UTC Date as a string for the current time
     * 
     * @return current time as UTC string
     */
    public String generateUTCDate() {
        return DateUtil.date2UTC(new Date());
    }
}
