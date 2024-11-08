package com.simpledata.filetools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.log4j.Logger;
import com.simpledata.filetools.encoders.SelfD;
import com.simpledata.filetools.encoders.SelfDC_DES;
import com.simpledata.filetools.encoders.SelfDC_Dummy;
import com.simpledata.filetools.encoders.SelfDC_GZIP;
import com.simpledata.filetools.encoders.SelfDC_IntConsumer;
import com.simpledata.filetools.encoders.SelfDConverter;
import com.simpledata.filetools.encoders.SelfDT_Serializable;
import com.simpledata.filetools.encoders.SelfDT_SerializableXMLArmored;
import com.simpledata.filetools.encoders.SelfDT_XMLEncoder;
import com.simpledata.filetools.encoders.SelfDTerminal;

/**
 * This is an extension of Secu that writes self describing streams<BR>
 * 
 * THE MAJOR DIFFERENCE WITH PREVIOUS VERSION IS THAT IS CONSUME MUCH LESS
 * MEMORY TO OPEN/CLOSE FILES AS THEY ARE TRULLY LINKED STREAMS
 * <BR>
 * Example of use:
 * <PRE>
 * String head = "Head";
        String body = "Body";
        SecuSelf ssdw 
        = new SecuSelf(new SelfDT_Serializable(head),
                new SelfDT_XMLEncoder(body));
        
        ssdw.insertDataEncoder(new SelfDC_RSA(er,ID_LICENSE));
        ssdw.insertDataEncoder(new SelfDC_Dummy());
        ssdw.insertDataEncoder(new SelfDC_DES(DESKEY));
        ssdw.insertDataEncoder(new SelfDC_GZIP());
        
        File f = new File("toto");
        try {
            ssdw.commit(new FileOutputStream(f));
         }catch.....
 * 
 * </PRE>
 * <HR>
 * <PRE>
 * Documentation about Secu and how file are saved.

This document does not describe SimpleData files structure from V1 to V3

[File TAG:]
Any file openable by Secu starts with a 27bytes tag that tells which version
of Secu stream to use to read this file. (Versionning)

[HEAD AND DATA]
Any file openable by Secu has two parts: The HEADER and the DATA
the HEADER should be a world readable SMALL amount of data describing the
DATA inside.. (mainly used for PREVIEW AND indexing)
HAVING A HEADER is NOT OPTIONAL but it can contains unused data.

head and data are JAVA OBJECTS SERIALIZED

head and data are encoded the same way.

[FILE STRUCTURE]
&lt;27 bytes:TAG>&lt;int:header length>&lt;part:header>&lt;part:data>

[PART STRUCTURE]
A part (header or data) starts with the list of readers to use to decrypt this
string, the number of readers is not limited, but it will finish by a terminal
reader id.
&lt;byte+:readers_ids>&lts;bytes>

[readers_ids]
They are two versions of readers id possible.. 
V0.. is a RAW id list of streams
V1.. [0x00:(byte:id,int:length_of_data,byte[]:data)+]

[READERS]
They are two types of readers : "Conversion" readers and "terminal" readers

Converion readers: manipulate streams and transform them to another Stream
Terminal readers: convert a stream to a java object
 * </PRE>
 */
public class SecuSelf {

    private static final Logger m_log = Logger.getLogger(SecuSelf.class);

    public static final byte[] SecuHeader() {
        return Secu.METHODS[Secu.METHOD_SELF_DESCRIBING_STRUCTURE];
    }

    private boolean commited;

    /**
     *  versioning tag .. it's added ot the begining of the TOC<BR>
     * V0 does not have a version TAG.<BR>
     * V1 starts with 0x00<BR>
     * V2 will start with 0x00,0x00<BR>
     * ....
    */
    public static final byte VERSION_TAG = (byte) 0x00;

    public static final byte C_GZIP = (byte) 0x01;

    public static final byte C_DES = (byte) 0x02;

    public static final byte C_RSA = (byte) 0x03;

    public static final byte C_IntConsumer = (byte) 0x04;

    public static final byte C_DUMMY = (byte) 0x05;

    public static final byte T_SERIALIZABLE = (byte) 0xFF;

    public static final byte T_XMLENCODER = (byte) 0xFE;

    public static final byte T_SERIALIZABLE_XMLARMORED = (byte) 0xFD;

    private static InputStream getDecoder(byte b, InputStream source, SelfD.DecodeFlow params, byte[] sparam) throws IOException, SimpleException {
        switch(b) {
            case C_DUMMY:
                return SelfDC_Dummy.getDecoder(source);
            case C_GZIP:
                return SelfDC_GZIP.getDecoder(source);
            case C_DES:
                return SelfDC_DES.getDecoder(source, params, sparam);
            case C_IntConsumer:
                return SelfDC_IntConsumer.getDecoder(source);
        }
        return null;
    }

    private static Object getObjectDecoder(byte b, InputStream source, SelfD.DecodeFlow params, byte[] sparam) throws IOException, SimpleException {
        switch(b) {
            case T_SERIALIZABLE:
                return SelfDT_Serializable.getObject(source);
            case T_XMLENCODER:
                return SelfDT_XMLEncoder.getObject(source, params);
            case T_SERIALIZABLE_XMLARMORED:
                return SelfDT_SerializableXMLArmored.getObject(source, params);
        }
        return null;
    }

    /**
     *for methods that know theymay not be able to decode a stream
     *@param at (-1) for head, (1 for body), 0 for undefine
     */
    protected static boolean checkCanDecode(byte b, SelfD.DecodeFlow params, byte[] sparams, int at) throws IOException, SimpleException {
        switch(b) {
            case C_DES:
                return SelfDC_DES.canDecode(params, sparams, at);
        }
        return true;
    }

    private ArrayList headerEncoders;

    private ArrayList dataEncoders;

    /** construct a Writer<BR>
     * @param headerTerminal the terminal encoder for the header
     * @param dataTerminal the terminal encoder for the data
     */
    public SecuSelf(SelfDTerminal headerTerminal, SelfDTerminal dataTerminal) {
        headerEncoders = new ArrayList();
        headerEncoders.add(headerTerminal);
        dataEncoders = new ArrayList();
        dataEncoders.add(dataTerminal);
        commited = false;
    }

    /** insert an encoder in the header encoding stream list <BR>
     * LAST inserted will be the FIRST used**/
    public void insertHeadEncoder(SelfDConverter enc) {
        assert commited == false;
        headerEncoders.add(0, enc);
    }

    /** insert an encoder in the data encoding stream list  <BR>
     * LAST inserted will be the FIRST used**/
    public void insertDataEncoder(SelfDConverter enc) {
        assert commited == false;
        dataEncoders.add(0, enc);
    }

    /** 
     * commit the writer.. <BR>
     * When all the inserts has been done<BR>
     * <B>Note OutputStream will be closed</B>
     * **/
    public void commit(OutputStream out) throws SimpleException, IOException {
        commit(Secu.METHODS[Secu.METHOD_SELF_DESCRIBING_STRUCTURE], out, true);
    }

    /**
     * Commit helper that can handle new and old fashion of saving 
     * @param stamp the 27 bytes long stamp
     * @param out the stream to print on
     * @param newFashion set to fasle if we are working with version 1 to 3
     */
    private void commit(byte[] stamp, OutputStream out, boolean newFashion) throws SimpleException, IOException {
        assert commited == false;
        commited = true;
        out.write(stamp);
        Part partHead = new Part(headerEncoders);
        ByteArrayOutputStream headBuff = new ByteArrayOutputStream();
        partHead.writeTo(headBuff);
        int headSize = headBuff.size() + partHead.getTOCContents().length;
        (new DataOutputStream(out)).writeInt(headSize);
        if (newFashion) out.write(partHead.getTOCContents());
        headBuff.writeTo(out);
        Part partData = new Part(dataEncoders);
        if (newFashion) out.write(partData.getTOCContents());
        partData.writeTo(out);
        out.close();
    }

    /** 
     * return true if this stream id correspond to a Terminal stream 
     * <BR>:b &lt; (byte) 0x00
     * **/
    static boolean isTerminal(byte b) {
        return b < (byte) 0x00;
    }

    /** get the header and data from this stream <BR>
     * (assuming the byte[27] stamp has been removed) 
     * <BR><B>IMPORTANT Stream is not closed!!</B>
     * @return o[0] is the header o[1] is the data**/
    public static Object[] getBoth(InputStream is, SelfD.DecodeFlow params) throws IOException, SimpleException {
        Object both[] = new Object[2];
        both[0] = getHeaderOnCourse(is, params);
        m_log.debug(".." + is.available());
        TOC dataTOC = getTOC(is);
        dataTOC.canBeRead(1, params);
        both[1] = SecuSelf.readPart(dataTOC, is, params);
        is.close();
        return both;
    }

    /** 
     * get the header from this stream <BR>
     * (assuming the byte[27] stamp has been removed) 
     * <BR><B>Will also check for validity of the data</B>
     * <BR><B>IMPORTANT Stream is not closed!!</B>**/
    public static Object getHeader(InputStream is, SelfD.DecodeFlow params) throws IOException, SimpleException {
        Object result = getHeaderOnCourse(is, params);
        TOC dataTOC = getTOC(is);
        dataTOC.canBeRead(1, params);
        return result;
    }

    /** 
     * get the header from this stream <BR>
     * (assuming the byte[27] stamp has been removed) 
     * <BR><B>Will also check for validity of the data</B>
     * <BR><B>IMPORTANT Stream is not closed!!</B>
     * This one does not check the DATA TOC validity
     * **/
    private static Object getHeaderOnCourse(InputStream is, SelfD.DecodeFlow params) throws IOException, SimpleException {
        int size = (new DataInputStream(is)).readInt();
        InputStream bais = new BoundedInputStream(is, size, false);
        TOC headTOC = getTOC(bais);
        headTOC.canBeRead(-1, params);
        return SecuSelf.readPart(headTOC, bais, params);
    }

    /** get the data from this stream <BR>
     * (assuming the byte[27] stamp has been removed) 
     * <BR><B>IMPORTANT Stream is not closed!!</B>**/
    public static Object getData(InputStream is, SelfD.DecodeFlow params) throws IOException, SimpleException {
        int size = (new DataInputStream(is)).readInt();
        is.skip(size);
        TOC dataTOC = getTOC(is);
        dataTOC.canBeRead(1, params);
        Object result = SecuSelf.readPart(dataTOC, is, params);
        return result;
    }

    /** get the TOC from the stream **/
    private static TOC getTOC(InputStream is) throws IOException, SimpleException {
        return new TOC(is);
    }

    /** 
     * This is a fraction of readPart, made public for backward compliance
     * with Secu.. <BR>
     * Secu simulate it has read the byte[] describing the streams flow<BR>
     * <B>IMPORTANT Stream is not closed!!</B>
     */
    public static Object readPart(byte[] toc, InputStream is, SelfD.DecodeFlow params) throws IOException, SimpleException {
        return readPart(new TOC(toc), is, params);
    }

    /** 
     * This is a fraction of readPart<BR>
     * <B>IMPORTANT Stream is not closed!!</B>
     */
    private static Object readPart(TOC encoders, InputStream is, SelfD.DecodeFlow params) throws IOException, SimpleException {
        InputStream last = is;
        for (int j = 0; j < (encoders.length() - 1) && last != null; j++) {
            last = getDecoder(encoders.getId(j), last, params, encoders.getSParam(j));
            if (last == null) throw new IOException("Cannot find decoder!!");
        }
        int j = encoders.length() - 1;
        return getObjectDecoder(encoders.getId(j), last, params, encoders.getSParam(j));
    }

    /** 
     * write the 27 byte signature to this stream
     */
    public static void start(OutputStream os) throws SimpleException {
        if (os == null) return;
        try {
            os.write(SecuHeader());
        } catch (IOException e) {
            throw new SimpleException(SimpleException.IOException, e);
        }
    }

    /** 
     * open a file and check if the header is valid (27 first bytes)
     * It will return a Stream withou the header
     * @return null if file is valid
     */
    public static InputStream open(File f) throws SimpleException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            throw new SimpleException(SimpleException.IOException, e);
        }
        return open(fis) ? fis : null;
    }

    /** 
     * open a  stream and check if the header is valid (27 first bytes)
     * it will consume the 27 first bytes of the header!!
     * @return true if file is valid
     */
    public static boolean open(InputStream fis) throws SimpleException {
        if (fis == null) return false;
        byte[] temp2 = new byte[Secu.STAMP_SIZE];
        try {
            if (fis.read(temp2) != Secu.STAMP_SIZE) {
                return false;
            }
        } catch (IOException e) {
            throw new SimpleException(SimpleException.IOException, "" + e);
        }
        return (Secu.getMethod(temp2) == Secu.METHOD_SELF_DESCRIBING_STRUCTURE);
    }

    /**
     * Convert a Stream Element to another one.. without taking care of the
     * data inside WHICH MAKE IT BE FAST<BR>
     * it is quite restricitive as it excpect a Stream without the 27 byte 
     * header (as getXXX methods). and the convertion will be applied only
     * on the DATA part.<BR>
     * Only the FIRST (last used) Stream can be converted.. but that should
     * be enough for everyday use ;) as normaly we just need to convert
     * encoding 1 to encoding 2...
     * <BR><B>Note: OutputStream will be closed</B>
     * @param with the converter to use for the convertion ;)
     * @param expectedFirstEncoder this is made for verification, 
     * you must specifiy what is the encoding method you wanted to replace
     * <BR>If you want to bypass this check .. use a 0b;
     * 
     */
    public static void convert(InputStream is, SelfDConverter with, SelfD.DecodeFlow sd, OutputStream destination, byte expectedFirstEncoder) throws IOException, SimpleException {
        int size = (new DataInputStream(is)).readInt();
        (new DataOutputStream(destination)).writeInt(size);
        copy(new BoundedInputStream(is, size, false), destination);
        TOC toc = getTOC(is);
        if (toc.length() <= 1 || toc.getId(0) != expectedFirstEncoder) {
            if (expectedFirstEncoder == (byte) 0x00) {
                m_log.warn("ByPassing check");
            } else {
                throw new SimpleException(0, "Cannot find desired Encoder");
            }
        }
        is = getDecoder(toc.getId(0), is, sd, toc.getSParam(0));
        toc.setStream(0, with);
        destination.write(toc.getTOC(1));
        destination = with.setDestination(destination);
        copy(is, destination);
        destination.close();
    }

    /**
     * Remove the first Stream of a passed one<BR>
     * it is quite restricitive as it excpect a Stream without the 27 byte 
     * header (as getXXX methods). and the removall will be applied only
     * on the DATA part.<BR>
     * Only the FIRST (last used) Stream will be removed.. <BR>
     * If an Terminal Encoder is found it will have no effect
     * <BR><B>Note: OutputStream will be closed</B>
     * @param expectedFirstEncoder this is made for verification, 
     * you must specifiy what is the encoding method you wanted to replace
     * <BR>If you want to bypass this check .. use a 0b;
     * 
     */
    public static void reduction(InputStream is, SelfD.DecodeFlow sd, OutputStream destination, byte expectedFirstEncoder) throws IOException, SimpleException {
        int size = (new DataInputStream(is)).readInt();
        (new DataOutputStream(destination)).writeInt(size);
        copy(new BoundedInputStream(is, size, false), destination);
        TOC toc = getTOC(is);
        if (toc.length() <= 1 || toc.getId(0) != expectedFirstEncoder) {
            if (expectedFirstEncoder == (byte) 0x00) {
                m_log.warn("ByPassing check");
            } else {
                throw new SimpleException(0, "Cannot find desired Encoder");
            }
        }
        if (!isTerminal(toc.getId(0))) {
            is = getDecoder(toc.getId(0), is, sd, toc.getSParam(0));
            toc.dropStream(0);
        }
        destination.write(toc.getTOC(1));
        copy(is, destination);
        destination.close();
    }

    /** 
     * add a stream on top of this one: same than convert but does
     * not remove the first stream<BR>
     * it is quite restricitive as it excpect a Stream without the 27 byte 
     * header (as getXXX methods). and the augmentation will be applied only
     * on the DATA part.<BR>
     * <B>Note: OutputStream will be closed</B>
     * @param with the converter to use for the augmentation ;)
     * **/
    public static void augment(InputStream is, SelfDConverter with, OutputStream destination) throws IOException, SimpleException {
        int size = (new DataInputStream(is)).readInt();
        (new DataOutputStream(destination)).writeInt(size);
        copy(new BoundedInputStream(is, size, false), destination);
        TOC toc = getTOC(is);
        toc.insertStream(false, with);
        destination.write(toc.getTOC(1));
        destination = with.setDestination(destination);
        copy(is, destination);
        destination.close();
    }

    /** utility to fully copy a stream into another one **/
    private static void copy(InputStream is, OutputStream os) throws IOException {
        int l = 0;
        for (byte[] b = new byte[1024]; (l = is.read(b)) > 0; ) os.write(b, 0, l);
    }
}

/** tool that manage Table Of contents **/
class TOC {

    private int version;

    ArrayList streams;

    TOC() {
        streams = new ArrayList();
    }

    /** read the TOC from this toc**/
    TOC(byte[] toc) throws IOException, SimpleException {
        this(new ByteArrayInputStream(toc));
    }

    /** read the TOC from this input Stream **/
    TOC(InputStream is) throws IOException, SimpleException {
        this();
        version = 0;
        byte[] last_encoder = new byte[1];
        while (is.read(last_encoder) > 0 && (last_encoder[0] == SecuSelf.VERSION_TAG)) {
            version++;
        }
        boolean ok = false;
        switch(version) {
            case 0:
                do {
                    ok = SecuSelf.isTerminal(last_encoder[0]);
                    streams.add(new Stream(last_encoder[0], new byte[0]));
                } while ((!ok) && (is.read(last_encoder) > 0));
                break;
            case 1:
                DataInputStream dais = new DataInputStream(is);
                do {
                    ok = SecuSelf.isTerminal(last_encoder[0]);
                    byte[] contents = new byte[dais.readInt()];
                    if (contents.length > 0) dais.read(contents);
                    streams.add(new Stream(last_encoder[0], contents));
                } while ((!ok) && (is.read(last_encoder) > 0));
                break;
            default:
                throw new SimpleException(0, "TOC IS NOT VALID version:" + version);
        }
        if (!ok) throw new SimpleException(0, "Cannot find terminal encoder");
    }

    /** check if this stream can be read, pass information to StreamFlow 
     * @param at (-1 for head) (1 for data) (0 for unkown)
     * **/
    boolean canBeRead(int at, SelfD.DecodeFlow sd) throws IOException, SimpleException {
        for (Iterator i = streams.iterator(); i.hasNext(); ) {
            Stream s = ((Stream) i.next());
            if (!SecuSelf.checkCanDecode(s.id, sd, s.contents, at)) {
                return false;
            }
        }
        return true;
    }

    /** get the number of streams in this TOC **/
    int length() {
        return streams.size();
    }

    /** get the id of stream i **/
    byte getId(int i) {
        return ((Stream) streams.get(i)).id;
    }

    /** get description of stream i (if any)**/
    byte[] getSParam(int i) {
        return ((Stream) streams.get(i)).contents;
    }

    /** 
     * insert a Stream on the TOC 
     * @param trailing if true appended at the end of the STream list, if false
     * at the begining
     * **/
    void insertStream(boolean trailing, SelfD stream) throws IOException {
        if (trailing) streams.add(new Stream(stream.getID())); else streams.add(0, new Stream(stream.getID()));
    }

    /** change the stream at position i to this one **/
    void setStream(int i, SelfD stream) throws IOException {
        streams.set(i, new Stream(stream.getID()));
    }

    /** drop the stream at this position **/
    void dropStream(int i) {
        streams.remove(i);
    }

    /** get the TOC contents **/
    byte[] getTOC(int desiredVersionOutput) throws SimpleException, IOException {
        desiredVersionOutput = 1;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        switch(desiredVersionOutput) {
            case 0:
                for (Iterator i = streams.iterator(); i.hasNext(); ) {
                    Stream s = ((Stream) i.next());
                    dos.write(s.id);
                }
                break;
            case 1:
                dos.write(SecuSelf.VERSION_TAG);
                for (Iterator i = streams.iterator(); i.hasNext(); ) {
                    Stream s = ((Stream) i.next());
                    dos.write(s.id);
                    dos.writeInt(s.contents.length);
                    dos.write(s.contents);
                }
                break;
            default:
                throw new SimpleException(0, "UNKOWN VERSION" + desiredVersionOutput);
        }
        byte[] res = baos.toByteArray();
        return res;
    }

    public String toString() {
        String res = new String();
        for (Iterator i = streams.iterator(); i.hasNext(); ) {
            Stream s = ((Stream) i.next());
            res += "[" + s.id + ":" + s.contents.length + "]";
        }
        return res;
    }

    class Stream {

        public Stream(byte[] fullId) throws IOException {
            ByteArrayInputStream bais = new ByteArrayInputStream(fullId);
            DataInputStream dais = new DataInputStream(bais);
            byte idt[] = new byte[1];
            dais.read(idt);
            id = idt[0];
            contents = new byte[dais.readInt()];
            if (contents.length > 0) dais.read(contents);
        }

        public Stream(byte id, byte[] contents) {
            this.id = id;
            this.contents = contents;
        }

        byte id;

        byte[] contents;
    }
}

/** tool that link streams one to another (for writing puposes) **/
class Part {

    /** contains the ids of the streams **/
    private TOC toc;

    private ArrayList encoders;

    Part(ArrayList encs) throws IOException {
        encoders = encs;
        toc = new TOC();
        for (int i = 0; i < encs.size(); i++) {
            toc.insertStream(true, ((SelfD) encs.get(i)));
        }
    }

    /** get the id list of the streams **/
    public byte[] getTOCContents() throws IOException, SimpleException {
        return toc.getTOC(1);
    }

    /** commit (write everything to this stream **/
    public void writeTo(OutputStream out) throws SimpleException, IOException {
        for (int i = 0; i < encoders.size(); i++) out = ((SelfD) encoders.get(i)).setDestination(out);
    }
}
