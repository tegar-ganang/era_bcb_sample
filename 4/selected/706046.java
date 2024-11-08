package org.marcont.marc21;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.text.BadLocationException;

/**
 * @author skruk
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Marc21Load {

    /**
	 * Binary version of Marc21 (may contain several instances of descriptions)
	 */
    protected byte[] abmarc = null;

    /**
	 * Array of MARC21 descriptions
	 */
    protected List lstmarc = new ArrayList();

    /**
	 * Array of Marc21 descriptions in internal form
	 */
    protected Marc21Description[] admarcdescription = null;

    public static void main(String[] args) {
        Marc21Load load = new Marc21Load(args[0]);
        for (int i = 0; i < load.getDescriptionsCount(); i++) {
            try {
                String s = load.processDocument(i).createXml("marcxml_" + i + ".xml");
                File f = new File(args[1] + i + ".xml");
                FileOutputStream fos = new FileOutputStream(f);
                OutputStreamWriter osw = new OutputStreamWriter(fos);
                osw.write(s);
                osw.close();
            } catch (IOException ioex) {
                ioex.printStackTrace();
            }
        }
    }

    /**
	 * 
	 * @param sFileName
	 */
    public Marc21Load(String sFileName) {
        try {
            File f = new File(sFileName);
            FileInputStream fis = new FileInputStream(f);
            FileChannel fch = fis.getChannel();
            ByteArrayOutputStream baos = new ByteArrayOutputStream((int) f.length());
            WritableByteChannel wbc = Channels.newChannel(baos);
            long pos = 0;
            long cnt = 0;
            while ((cnt = fch.transferTo(pos, f.length(), wbc)) > 0) {
                pos += cnt;
            }
            fis.close();
            this.abmarc = baos.toByteArray();
            int start = 0;
            for (int i = 1; i < this.abmarc.length; i++) {
                if (this.abmarc[i] == 0x1D) {
                    ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                    try {
                        baos2.write(this.abmarc, start, i - 1 - start);
                    } catch (Exception ex) {
                        System.exit(0);
                    }
                    lstmarc.add(baos2.toByteArray());
                    start = ++i;
                }
            }
            String tmpmarc = new String(abmarc, "US-ASCII");
            this.admarcdescription = new Marc21Description[this.lstmarc.size()];
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }
    }

    public byte[] getAbmarc() {
        return abmarc;
    }

    public Marc21Description[] getAdmarcdescription() {
        return admarcdescription;
    }

    public int getMarc21Length() {
        return this.abmarc.length;
    }

    public int getDescriptionsCount() {
        return this.lstmarc.size();
    }

    /**
	 *  Description of the Method
	 * @throws UnsupportedEncodingException
	 *
	 * @exception  BadLocationException  Description of the Exception
	 */
    protected Marc21Description processDocument(int id) throws UnsupportedEncodingException {
        Marc21Description description = null;
        if (id > -1 && id < this.admarcdescription.length) {
            if (this.admarcdescription[id] == null) {
                this.admarcdescription[id] = new Marc21Description();
                description = this.admarcdescription[id];
                byte[] a_marc = (byte[]) this.lstmarc.get(id);
                String marc = new String(a_marc, "UTF-8");
                description.setLeader(marc.substring(0, 24));
                description.setLength(Integer.valueOf(marc.substring(0, 5)).intValue());
                description.setDataStart(Integer.valueOf(marc.substring(12, 17)).intValue());
                int dstart = description.getDataStart();
                int i = 24;
                while (i + 11 < dstart) {
                    Marc21Field field;
                    int tag = Integer.valueOf(marc.substring(i, i + 3)).intValue();
                    if (tag < 10) {
                        field = new Marc21Controlfield();
                    } else {
                        field = new Marc21Datafield();
                    }
                    field.setTag(tag);
                    field.setLength(Integer.valueOf(marc.substring(i + 3, i + 7)).intValue());
                    field.setStart(Integer.valueOf(marc.substring(i + 7, i + 12)).intValue());
                    description.addField(field);
                    i += 12;
                }
                Iterator it = description.fieldsIterator();
                while (it.hasNext()) {
                    Marc21Field f = (Marc21Field) it.next();
                    int start = f.getStart();
                    int length = f.getLength();
                    try {
                        String text = null;
                        if (dstart + start + length - 1 >= a_marc.length) {
                            length = a_marc.length - dstart - start;
                        }
                        if (a_marc[dstart + start + length - 1] == '') {
                            text = new String(a_marc, dstart + start, length - 1, "UTF8");
                        } else {
                            text = new String(a_marc, dstart + start, length, "UTF8");
                        }
                        f.setText(text);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (f instanceof Marc21Datafield) {
                        Marc21Datafield df = (Marc21Datafield) f;
                        String ptr = new String(a_marc, dstart + start, 2, "UTF8");
                        df.setPtr1(ptr.charAt(0));
                        df.setPtr2(ptr.charAt(1));
                        String[] sbfields = f.getText().split("");
                        for (int j = 1; j < sbfields.length; j++) {
                            char key = sbfields[j].charAt(0);
                            int len = sbfields[j].length();
                            df.putSubfield(key, sbfields[j].substring(1, len));
                        }
                    }
                }
            } else {
                description = admarcdescription[id];
            }
        }
        return description;
    }
}
