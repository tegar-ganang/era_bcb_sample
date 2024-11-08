package com.sdi.pws.codec;

import com.sdi.pws.db.*;
import com.sdi.crypto.blowfish.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.BufferUnderflowException;
import java.util.*;

public class Codec2 implements Codec {

    public static final String VERSION = "2.0";

    public PwsDatabase read(File aFile, String aPassphrase) throws CodecException {
        FileInputStream lStream = null;
        final PwsDatabaseImpl lDb = new PwsDatabaseImpl();
        lDb.setCodec(this);
        lDb.setPassphrase(aPassphrase);
        lDb.setFile(aFile);
        PwsRecordImpl lCurrentPwsRec = null;
        try {
            lStream = new FileInputStream(aFile);
            final ByteBuffer lDataBuf = ByteBuffer.allocateDirect(56);
            lDataBuf.order(ByteOrder.LITTLE_ENDIAN);
            lStream.getChannel().read(lDataBuf);
            lDataBuf.flip();
            final PwsFileHeader lPwsHeader = new PwsFileHeader();
            lPwsHeader.readFromBuffer(lDataBuf);
            if (CodecUtil.checkPassphrase(lPwsHeader, aPassphrase)) {
                final Blowfish lFish = CodecUtil.initBlowfish(lPwsHeader, aPassphrase);
                lCurrentPwsRec = new PwsRecordImpl();
                {
                    PwsField lPwsField = CodecUtil.readValue(lStream, lDataBuf, lFish);
                    if (lPwsField.getAsString().startsWith(" !!!Version 2 File Format!!!")) {
                        lPwsField = CodecUtil.readValue(lStream, lDataBuf, lFish);
                        if (!VERSION.equals(lPwsField.getAsString())) {
                            final String lMsg = "codec09";
                            throw new CodecException(lMsg);
                        }
                        lPwsField = CodecUtil.readValue(lStream, lDataBuf, lFish);
                        lDb.setParameters(lPwsField.getAsString());
                    } else {
                        final String lMsg = "codec08";
                        throw new CodecException(lMsg);
                    }
                }
                while (lDataBuf.remaining() + lStream.available() >= 8) {
                    PwsFieldImpl lPwsField = CodecUtil.readValue(lStream, lDataBuf, lFish);
                    if (lPwsField.getType().equals(PwsField.FIELD_EOR)) {
                        lDb.add(lCurrentPwsRec);
                        lCurrentPwsRec = new PwsRecordImpl();
                    } else {
                        lCurrentPwsRec.put(lPwsField);
                    }
                }
            } else {
                final String lMsg = "codec02";
                throw new CodecException(lMsg);
            }
        } catch (ModelException e) {
            final String lMsg = "codec03";
            throw new CodecException(lMsg);
        } catch (FileNotFoundException eEx) {
            final String lMsg = "codec00";
            throw new FileCodecException(lMsg, aFile.getAbsolutePath(), eEx);
        } catch (IOException eEx) {
            final String lMsg = "codec01";
            throw new FileCodecException(lMsg, aFile.getAbsolutePath(), eEx);
        } catch (BufferUnderflowException eEx) {
            final String lMsg = "codec01";
            throw new FileCodecException(lMsg, aFile.getAbsolutePath(), eEx);
        } finally {
            if (lStream != null) try {
                lStream.close();
            } catch (Exception e) {
            }
        }
        return lDb;
    }

    public void write(PwsDatabase aDb) throws CodecException {
        FileOutputStream lStream = null;
        File lFile = aDb.getFile();
        if (lFile == null) {
            final String lMsg = "codec05";
            throw new CodecException(lMsg);
        }
        final PwsFileHeader lPwsHeader = CodecUtil.initPwsHeader(aDb.getPassphrase());
        final Blowfish lFish = CodecUtil.initBlowfish(lPwsHeader, aDb.getPassphrase());
        try {
            lStream = new FileOutputStream(lFile);
            final ByteBuffer lDataBuf = ByteBuffer.allocateDirect(56);
            lDataBuf.order(ByteOrder.LITTLE_ENDIAN);
            lPwsHeader.writeToBuffer(lDataBuf);
            lStream.getChannel().write(lDataBuf);
            CodecUtil.writeValue(lStream, lDataBuf, lFish, new PwsFieldImpl(PwsField.FIELD_HEADER, " !!!Version 2 File Format!!! Please upgrade to PasswordSafe 2.0 or later".getBytes()));
            CodecUtil.writeValue(lStream, lDataBuf, lFish, new PwsFieldImpl(PwsField.FIELD_HEADER, getVersion().getBytes()));
            CodecUtil.writeValue(lStream, lDataBuf, lFish, new PwsFieldImpl(PwsField.FIELD_HEADER, aDb.getParameters() != null ? aDb.getParameters().getBytes() : new byte[0]));
            final Iterator lIter = aDb.iterator();
            while (lIter.hasNext()) {
                final PwsRecord lRecord = (PwsRecord) lIter.next();
                final Iterator lTypeIter = lRecord.typeIterator();
                while (lTypeIter.hasNext()) {
                    final Byte lFieldType = (Byte) lTypeIter.next();
                    CodecUtil.writeValue(lStream, lDataBuf, lFish, lRecord.get(lFieldType));
                }
                CodecUtil.writeValue(lStream, lDataBuf, lFish, new PwsFieldImpl(PwsField.FIELD_EOR, new byte[0]));
            }
            lDataBuf.flip();
            lStream.getChannel().write(lDataBuf);
            lDataBuf.flip();
        } catch (ModelException e) {
            final String lMsg = "codec07";
            throw new CodecException(lMsg, e);
        } catch (FileNotFoundException e) {
            final String lMsg = "codec06";
            throw new FileCodecException(lMsg, lFile.getAbsolutePath(), e);
        } catch (IOException e) {
            final String lMsg = "codec04";
            throw new FileCodecException(lMsg, lFile.getAbsolutePath(), e);
        } finally {
            if (lStream != null) try {
                lStream.flush();
                lStream.close();
            } catch (Exception eIgnore) {
            }
            ;
        }
    }

    public String getVersion() {
        return VERSION;
    }
}
