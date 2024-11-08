package com.sdi.pws.codec;

import com.sdi.crypto.blowfish.Blowfish;
import com.sdi.pws.db.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.BufferUnderflowException;
import java.util.Iterator;

public class Codec1 implements Codec {

    public static final String VERSION = "1.0";

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
                int lValueCounter = 0;
                while (lDataBuf.remaining() + lStream.available() >= 8) {
                    final byte[] lValueBuf = CodecUtil.readValue(lStream, lDataBuf, lFish).getValue();
                    lValueCounter++;
                    int lSelection = lValueCounter % 3;
                    if (lSelection == 0) {
                        final PwsFieldImpl lNotesField = new PwsFieldImpl(PwsField.FIELD_NOTES, lValueBuf);
                        lCurrentPwsRec.put(lNotesField);
                    } else if (lSelection == 1) {
                        lCurrentPwsRec = new PwsRecordImpl();
                        lDb.add(lCurrentPwsRec);
                        int lSplitBoundary = 0;
                        for (; (lSplitBoundary < lValueBuf.length) && (lValueBuf[lSplitBoundary] != (byte) 0xAD); lSplitBoundary++) ;
                        if (lSplitBoundary < lValueBuf.length) {
                            final int lTitleLen = lSplitBoundary;
                            final int lLoginLen = lValueBuf.length - lSplitBoundary - 1;
                            if (lTitleLen >= 2 && lLoginLen >= 2) {
                                final byte[] lTitle = new byte[lTitleLen - 2];
                                System.arraycopy(lValueBuf, 0, lTitle, 0, lTitleLen - 2);
                                final PwsFieldImpl lTitleField = new PwsFieldImpl(PwsField.FIELD_TITLE, lTitle);
                                lCurrentPwsRec.put(lTitleField);
                                final byte[] lLogin = new byte[lLoginLen - 2];
                                System.arraycopy(lValueBuf, lSplitBoundary + 3, lLogin, 0, lLoginLen - 2);
                                final PwsFieldImpl lLoginField = new PwsFieldImpl(PwsField.FIELD_UID, lLogin);
                                lCurrentPwsRec.put(lLoginField);
                            } else {
                                final byte[] lTitle = new byte[lTitleLen];
                                System.arraycopy(lValueBuf, 0, lTitle, 0, lTitleLen);
                                final PwsFieldImpl lTitleField = new PwsFieldImpl(PwsField.FIELD_TITLE, lTitle);
                                lCurrentPwsRec.put(lTitleField);
                                final byte[] lLogin = new byte[lLoginLen];
                                System.arraycopy(lValueBuf, lSplitBoundary + 1, lLogin, 0, lLoginLen);
                                final PwsFieldImpl lLoginField = new PwsFieldImpl(PwsField.FIELD_UID, lLogin);
                                lCurrentPwsRec.put(lLoginField);
                            }
                        } else {
                            int lDefaultBoundary = 0;
                            for (; (lDefaultBoundary < lValueBuf.length) && (lValueBuf[lDefaultBoundary] != (byte) 0xA0); lDefaultBoundary++) ;
                            if (lDefaultBoundary < lValueBuf.length) {
                                byte[] lTitle = new byte[lDefaultBoundary];
                                System.arraycopy(lValueBuf, 0, lTitle, 0, lDefaultBoundary);
                                final PwsFieldImpl lTitleField = new PwsFieldImpl(PwsField.FIELD_TITLE, lTitle);
                                lCurrentPwsRec.put(lTitleField);
                                final PwsFieldImpl lLoginField = new PwsFieldImpl(PwsField.FIELD_DEFAULT_UID, new byte[0]);
                                lCurrentPwsRec.put(lLoginField);
                            } else {
                                final PwsFieldImpl lTitleField = new PwsFieldImpl(PwsField.FIELD_TITLE, lValueBuf);
                                lCurrentPwsRec.put(lTitleField);
                            }
                        }
                    } else if (lSelection == 2) {
                        final PwsFieldImpl lPwdField = new PwsFieldImpl(PwsField.FIELD_PWD, lValueBuf);
                        lCurrentPwsRec.put(lPwdField);
                    }
                }
            } else {
                final String lMsg = "codec02";
                throw new CodecException(lMsg);
            }
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
            final Iterator lIter = aDb.iterator();
            while (lIter.hasNext()) {
                final PwsRecord lRecord = (PwsRecord) lIter.next();
                {
                    byte[] lTitleBuf = null;
                    byte[] lUidBuf = null;
                    byte[] lValueBuf = null;
                    if (lRecord.hasType(PwsField.FIELD_DEFAULT_UID)) {
                        if (lRecord.hasType(PwsField.FIELD_TITLE)) lTitleBuf = lRecord.get(PwsField.FIELD_TITLE).getValue(); else lTitleBuf = new byte[0];
                        lUidBuf = new byte[] { (byte) 0x20, (byte) 0x20, (byte) 0xA0 };
                        lValueBuf = new byte[lTitleBuf.length + lUidBuf.length];
                        System.arraycopy(lTitleBuf, 0, lValueBuf, 0, lTitleBuf.length);
                        System.arraycopy(lUidBuf, 0, lValueBuf, lTitleBuf.length, lUidBuf.length);
                    } else {
                        if (lRecord.hasType(PwsField.FIELD_TITLE)) lTitleBuf = lRecord.get(PwsField.FIELD_TITLE).getValue(); else lTitleBuf = new byte[0];
                        if (lRecord.hasType(PwsField.FIELD_UID)) lUidBuf = lRecord.get(PwsField.FIELD_UID).getValue(); else lUidBuf = new byte[0];
                        if ((lTitleBuf.length > 0) && (lUidBuf.length > 0)) {
                            lValueBuf = new byte[lTitleBuf.length + lUidBuf.length + 5];
                            System.arraycopy(lTitleBuf, 0, lValueBuf, 0, lTitleBuf.length);
                            lValueBuf[lTitleBuf.length + 0] = 32;
                            lValueBuf[lTitleBuf.length + 1] = 32;
                            lValueBuf[lTitleBuf.length + 2] = (byte) 0xAD;
                            lValueBuf[lTitleBuf.length + 3] = 32;
                            lValueBuf[lTitleBuf.length + 4] = 32;
                            System.arraycopy(lUidBuf, 0, lValueBuf, lTitleBuf.length + 5, lUidBuf.length);
                        } else if ((lTitleBuf.length > 0) && (lUidBuf.length == 0)) {
                            lValueBuf = new byte[lTitleBuf.length];
                            System.arraycopy(lTitleBuf, 0, lValueBuf, 0, lTitleBuf.length);
                        } else {
                            lValueBuf = new byte[lTitleBuf.length + lUidBuf.length + 1];
                            System.arraycopy(lTitleBuf, 0, lValueBuf, 0, lTitleBuf.length);
                            lValueBuf[lTitleBuf.length + 0] = (byte) 0xAD;
                            System.arraycopy(lUidBuf, 0, lValueBuf, lTitleBuf.length + 1, lUidBuf.length);
                        }
                    }
                    CodecUtil.writeValue(lStream, lDataBuf, lFish, new PwsFieldImpl(PwsField.FIELD_HEADER, lValueBuf));
                }
                {
                    byte[] lPwdbuf = null;
                    if (lRecord.hasType(PwsField.FIELD_PWD)) lPwdbuf = lRecord.get(PwsField.FIELD_PWD).getValue(); else lPwdbuf = new byte[0];
                    CodecUtil.writeValue(lStream, lDataBuf, lFish, new PwsFieldImpl(PwsField.FIELD_HEADER, lPwdbuf));
                }
                {
                    byte[] lNoteBuf = null;
                    if (lRecord.hasType(PwsField.FIELD_NOTES)) lNoteBuf = lRecord.get(PwsField.FIELD_NOTES).getValue(); else lNoteBuf = new byte[0];
                    CodecUtil.writeValue(lStream, lDataBuf, lFish, new PwsFieldImpl(PwsField.FIELD_HEADER, lNoteBuf));
                }
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
