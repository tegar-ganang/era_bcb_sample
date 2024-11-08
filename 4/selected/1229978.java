package org.jmule.core.protocol.donkey;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.logging.*;
import java.util.Map;
import org.jmule.core.Download;
import org.jmule.core.DownloadManager;
import org.jmule.core.partialfile.*;
import org.jmule.util.Convert;
import org.jmule.util.LogUtil;

/** Reads *.met files. Can be used to import downloads from *.part.met to the downloadmanger.
 * @author emarant
 * @author andyl
 * @version $Revision: 1.1.1.1 $
 * <br>Last changed by $Author: jmartinc $ on $Date: 2005/04/22 21:44:59 $
 */
public class MetFileReader implements DonkeyPacketConstants {

    static final Logger log = Logger.getLogger(MetFileReader.class.getPackage().getName());

    private static final byte PARTFILE_VERSION = (byte) 0xe0;

    private static final byte NEW_PARTFILE_VERSION = (byte) 0xe1;

    public void loadPartMet(File file) throws Exception {
        LogUtil.entering(log, file);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            mbb.order(ByteOrder.LITTLE_ENDIAN);
            if (PARTFILE_VERSION != mbb.get()) {
                throw new Exception("FIXME: *.part.met version not supported or " + file.getName() + " currupt");
            }
            ;
            mbb.getInt();
            byte[] hash = new byte[16];
            mbb.get(hash);
            DonkeyFileHash donkeyhash = new DonkeyFileHash(hash);
            log.info("FileID " + donkeyhash);
            int count = ((int) mbb.getShort()) & 0xffFF;
            log.fine("parthashCount=" + count);
            Map hashes = new HashMap();
            hashes.put("DonkeyHash", donkeyhash);
            byte[][] hashset = new byte[(count == 0) ? 1 : count + 1][16];
            System.arraycopy(hash, 0, hashset[0], 0, 16);
            for (int i = 1; i <= count; i++) {
                log.finer("count" + i);
                mbb.get(hashset[i]);
                log.finer("parthash(" + i + ")=" + Convert.bytesToHexString(hashset[i]));
            }
            ;
            count = mbb.getInt();
            log.fine("tagCount=" + count);
            Tag tag;
            Tag[] tags = new Tag[count];
            for (int i = 0; i < count; i++) {
                tags[i] = Tag.readFrom(mbb);
                log.finer(tags[i].toString());
            }
            ;
            String fileName = null;
            String tempFileName = null;
            long fileSize = -1;
            int pos = 0;
            GapList gaplist = new GapList();
            while (pos < count) {
                tag = tags[pos++];
                log.finer(" parse " + pos + " : " + tag + (tag.isSpecialTag() ? " specialValue: " + tag.getSpecialValue() : " no specialValue"));
                if (tag.isStringTag()) {
                    if (tag.getTagName().equals("Name")) {
                        fileName = tag.getStringValue();
                        continue;
                    } else if (tag.getTagName().equals("Partfilename")) {
                        tempFileName = tag.getStringValue();
                        continue;
                    }
                } else if (tag.isIntTag() && tag.isSpecialTag()) {
                    if (tag.getSpecialValue() == Tag.FT_Filesize) {
                        fileSize = ((long) tag.getIntValue()) & 0xffFFffFFL;
                        continue;
                    }
                    if (tag.getSpecialValue() == Tag.FT_GapStart && pos < count) {
                        long start = ((long) tag.getIntValue()) & 0xffFFffFFL;
                        tag = tags[pos++];
                        if (!(tag.isSpecialTag() && tag.getSpecialValue() == Tag.FT_GapEnd)) {
                            throw new Exception("FIXME: " + file.getName() + " gaplist currupt.");
                        }
                        long end = ((long) tag.getIntValue()) & 0xffFFffFFL;
                        gaplist.addGap(start, end);
                        continue;
                    }
                }
            }
            log.info("fileName: " + fileName + " tempFileName: " + tempFileName + " fileSize: " + fileSize + " remaining:" + gaplist.byteSize());
            if (fileName != null && tempFileName != null && fileSize >= 0) {
                gaplist.sort();
                if (!(hashset.length == 1 && fileSize > PARTSIZE)) {
                    hashes.put("DonkeyHashes", new DonkeyFileHashSet(hashset));
                }
                if ((new File(tempFileName)).getParent() == null) {
                    tempFileName = file.getParent() + File.separator + tempFileName;
                }
                Download dl = DownloadManager.getInstance().getDownloadList().addDownload(fileName, tempFileName, fileSize, new HashMap(hashes), gaplist);
            } else {
                throw new Exception("FIXME: " + file.getName() + " currupt");
            }
            LogUtil.exiting(log, file);
        } catch (IOException err) {
            try {
                log.warning(err.getMessage());
                fis.close();
            } catch (IOException _err) {
                log.warning(_err.getMessage());
            }
            ;
        }
        ;
    }

    public FilenameFilter getFilenameFilter() {
        return new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith(".met");
            }

            ;
        };
    }
}
