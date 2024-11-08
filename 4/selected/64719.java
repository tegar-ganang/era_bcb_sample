package org.openXpertya.print.pdf.text.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import org.openXpertya.print.pdf.text.Document;
import org.openXpertya.print.pdf.text.ExceptionConverter;

public class PRStream extends PdfStream {

    protected PdfReader reader;

    protected int offset;

    protected int length;

    protected int objNum = 0;

    protected int objGen = 0;

    public PRStream(PRStream stream, PdfDictionary newDic) {
        reader = stream.reader;
        offset = stream.offset;
        length = stream.length;
        compressed = stream.compressed;
        streamBytes = stream.streamBytes;
        bytes = stream.bytes;
        objNum = stream.objNum;
        objGen = stream.objGen;
        if (newDic != null) putAll(newDic); else hashMap.putAll(stream.hashMap);
    }

    public PRStream(PRStream stream, PdfDictionary newDic, PdfReader reader) {
        this(stream, newDic);
        this.reader = reader;
    }

    public PRStream(PdfReader reader, int offset) {
        this.reader = reader;
        this.offset = offset;
    }

    public PRStream(PdfReader reader, byte conts[]) {
        this.reader = reader;
        this.offset = -1;
        if (Document.compress) {
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                DeflaterOutputStream zip = new DeflaterOutputStream(stream);
                zip.write(conts);
                zip.close();
                bytes = stream.toByteArray();
            } catch (IOException ioe) {
                throw new ExceptionConverter(ioe);
            }
            put(PdfName.FILTER, PdfName.FLATEDECODE);
        } else bytes = conts;
        setLength(bytes.length);
    }

    /**Sets the data associated with the stream
     * @param data raw data, decrypted and uncompressed.
     */
    public void setData(byte[] data) {
        remove(PdfName.FILTER);
        this.offset = -1;
        if (Document.compress) {
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                DeflaterOutputStream zip = new DeflaterOutputStream(stream);
                zip.write(data);
                zip.close();
                bytes = stream.toByteArray();
            } catch (IOException ioe) {
                throw new ExceptionConverter(ioe);
            }
            put(PdfName.FILTER, PdfName.FLATEDECODE);
        } else bytes = data;
        setLength(bytes.length);
    }

    public void setLength(int length) {
        this.length = length;
        put(PdfName.LENGTH, new PdfNumber(length));
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public PdfReader getReader() {
        return reader;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setObjNum(int objNum, int objGen) {
        this.objNum = objNum;
        this.objGen = objGen;
    }

    int getObjNum() {
        return objNum;
    }

    int getObjGen() {
        return objGen;
    }

    public void toPdf(PdfWriter writer, OutputStream os) throws IOException {
        superToPdf(writer, os);
        os.write(STARTSTREAM);
        if (length > 0) {
            PdfEncryption crypto = null;
            if (writer != null) crypto = writer.getEncryption();
            if (offset < 0) {
                if (crypto == null) os.write(bytes); else {
                    crypto.prepareKey();
                    byte buf[] = new byte[length];
                    System.arraycopy(bytes, 0, buf, 0, length);
                    crypto.encryptRC4(buf);
                    os.write(buf);
                }
            } else {
                byte buf[] = new byte[Math.min(length, 4092)];
                RandomAccessFileOrArray file = writer.getReaderFile(reader);
                boolean isOpen = file.isOpen();
                try {
                    file.seek(offset);
                    int size = length;
                    PdfEncryption decrypt = reader.getDecrypt();
                    if (decrypt != null) {
                        decrypt.setHashKey(objNum, objGen);
                        decrypt.prepareKey();
                    }
                    if (crypto != null) crypto.prepareKey();
                    while (size > 0) {
                        int r = file.read(buf, 0, Math.min(size, buf.length));
                        size -= r;
                        if (decrypt != null) decrypt.encryptRC4(buf, 0, r);
                        if (crypto != null) crypto.encryptRC4(buf, 0, r);
                        os.write(buf, 0, r);
                    }
                } finally {
                    if (!isOpen) try {
                        file.close();
                    } catch (Exception e) {
                    }
                }
            }
        }
        os.write(ENDSTREAM);
    }
}
