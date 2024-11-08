package com.java_internals.fileupload;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.log4j.Category;

/**
 * All of the parsing code is handled in this class.  The only public method
 * for this class retrieves the list of UploadResults.
 */
class UploadState implements FileUploadResourceConstants {

    private static Category LOG = Category.getInstance(UploadState.class.getName());

    private RandomAccessFile file;

    private File temp_dir;

    private byte[] start_boundary;

    private byte[] end_boundary;

    private byte[] buffer;

    private long start_boundary_length;

    private long end_boundary_length;

    private List list;

    private List upload_results;

    public List getResults() {
        return new ArrayList(upload_results);
    }

    UploadState(RandomAccessFile file, File temp_dir, byte[] start_boundary, byte[] end_boundary, byte[] buffer) throws UploadException, IOException {
        this.file = file;
        this.temp_dir = temp_dir;
        this.start_boundary = start_boundary;
        this.end_boundary = end_boundary;
        this.buffer = buffer;
        start_boundary_length = start_boundary.length;
        end_boundary_length = end_boundary.length;
        findNewLines();
        go();
        cleanup();
    }

    private void cleanup() {
        file = null;
        temp_dir = null;
    }

    private UploadResults _walkHeaders(List list) {
        boolean isFile = false;
        HashMap map = new HashMap();
        for (Iterator iter = list.iterator(); iter.hasNext(); ) {
            String value = (String) iter.next();
            StringTokenizer st = new StringTokenizer(value, ";");
            while (st.hasMoreTokens()) {
                String attr = st.nextToken();
                int index_of_equals = attr.indexOf("=");
                if (index_of_equals > -1) {
                    map.put(attr.substring(0, index_of_equals).trim(), attr.substring(index_of_equals + 1).trim());
                } else {
                    int index_of_colon = attr.indexOf(":");
                    map.put(attr.substring(0, index_of_colon).trim(), attr.substring(index_of_colon + 1).trim());
                }
            }
        }
        return new UploadResults(map, temp_dir);
    }

    /**
     * The nasty parsing code... it <b>is</b> nasty.  There's a state machine that tracks
     * where we're at in the underlying file (using integer state values and
     * a switch statement for making state transitions).
     *
     * @exception UploadException if something is wrong with the file data
     * @exception IOException if something goes wrong while parsing the file and writing
     *                        the content to a new file
     */
    private void go() throws UploadException, IOException {
        int state = BEFORE_FILE_STATE;
        Comparator comparator = new ByteArrayComparator();
        int count = 0;
        long previous_newline_pos = 0L;
        long start_pos = 0L;
        long stop_pos = 0L;
        List headers = new LinkedList();
        upload_results = new LinkedList();
        UploadResults current_results = null;
        for (Iterator iter = list.iterator(); iter.hasNext(); ) {
            Long current = (Long) iter.next();
            long current_newline_pos = current.longValue();
            long difference = current_newline_pos - previous_newline_pos;
            switch(state) {
                case BEFORE_FILE_STATE:
                    if (difference != start_boundary_length) {
                        throw new UploadException("Not a file upload: First line not a boundary");
                    } else {
                        previous_newline_pos = current_newline_pos + 2;
                        state = READ_HEADERS_STATE;
                    }
                    break;
                case READ_HEADERS_STATE:
                    if (difference == 0) {
                        current_results = _walkHeaders(headers);
                        upload_results.add(current_results);
                        headers.clear();
                        state = READ_FILE_STATE;
                        start_pos = current_newline_pos;
                    } else {
                        file.seek(previous_newline_pos);
                        count = file.read(buffer, 0, (int) difference);
                        headers.add(new String(buffer, 0, count));
                    }
                    previous_newline_pos = current_newline_pos + 2;
                    break;
                case READ_FILE_STATE:
                    if (difference == start_boundary_length) {
                        file.seek(previous_newline_pos);
                        file.read(buffer, 0, (int) difference);
                        if (comparator.compare(start_boundary, buffer) == 0) {
                            try {
                                stop_pos = previous_newline_pos;
                                _readwrite(start_pos, stop_pos, current_results);
                            } catch (Exception oops) {
                            }
                            state = READ_HEADERS_STATE;
                        }
                    } else if (difference == end_boundary_length) {
                        file.seek(previous_newline_pos);
                        file.read(buffer, 0, (int) difference);
                        if (comparator.compare(end_boundary, buffer) == 0) {
                            try {
                                stop_pos = previous_newline_pos;
                                _readwrite(start_pos, stop_pos, current_results);
                            } catch (Exception oops) {
                            }
                            state = READ_HEADERS_STATE;
                        }
                    }
                    previous_newline_pos = current_newline_pos + 2;
                    break;
            }
        }
    }

    /**
     * Writing a series of bytes to the underlying UploadResults
     *
     * @param start_pos the beginning offset to read data
     * @param stop_pos  the end position where to stop reading
     * @param current_results
     *                  The UploadResults object where the content should be written
     * @exception IOException if something goes wrong while writing to the underlying UploadResults
     *                        object
     */
    private void _readwrite(long start_pos, long stop_pos, UploadResults current_results) throws IOException {
        start_pos += 2;
        stop_pos -= 2;
        int count = 0;
        file.seek(start_pos);
        long file_length = stop_pos - start_pos;
        long num_reads = file_length / buffer.length;
        int last_read = (int) (file_length % buffer.length);
        for (long loop = 0; loop < num_reads; loop++) {
            if ((count = file.read(buffer, 0, buffer.length)) == -1) {
                LOG.error("ERROR! Read operation has failed!");
            } else {
                current_results.write(buffer, 0, buffer.length);
            }
        }
        file.read(buffer, 0, last_read);
        current_results.write(buffer, 0, last_read);
        current_results.close();
    }

    private void findNewLines() throws UploadException, IOException {
        list = new ArrayList(50);
        int count = 0;
        long previous_pos = 0L;
        boolean found13 = false;
        while (count >= 0) {
            previous_pos = file.getFilePointer();
            count = file.read(buffer, 0, buffer.length);
            for (int loop = 0; loop < count; loop++) {
                if (found13) {
                    if (buffer[loop] == (byte) 10) {
                        list.add(new Long(previous_pos + loop - 1));
                    }
                    found13 = false;
                } else if (buffer[loop] == (byte) 13) {
                    found13 = true;
                    if (loop + 1 < count) {
                        if (buffer[loop + 1] == (byte) 10) {
                            list.add(new Long(previous_pos + loop));
                        }
                        found13 = false;
                    }
                }
            }
        }
        file.seek(0);
    }
}
