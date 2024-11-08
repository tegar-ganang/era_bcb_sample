package com.lowagie.text.pdf;

import java.util.ArrayList;
import com.lowagie.text.ExceptionConverter;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfDate;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfPKCS7;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfString;
import com.lowagie.text.pdf.PdfWriter;

public class AcroFieldsExtended extends AcroFields {

    AcroFieldsExtended(PdfReader reader, PdfWriter writer) {
        super(reader, writer);
    }

    public PdfPKCS7Extended verifySignatureExtended(String name, String provider) {
        System.out.println("Extending PdfPKCS7Extended");
        PdfDictionary v = getSignatureDictionary(name);
        if (v == null) return null;
        try {
            PdfName sub = (PdfName) PdfReader.getPdfObject(v.get(PdfName.SUBFILTER));
            PdfString contents = (PdfString) PdfReader.getPdfObject(v.get(PdfName.CONTENTS));
            PdfPKCS7Extended pk = null;
            if (sub.equals(PdfName.ADBE_X509_RSA_SHA1)) {
                PdfString cert = (PdfString) PdfReader.getPdfObject(v.get(PdfName.CERT));
                pk = new PdfPKCS7Extended(contents.getOriginalBytes(), cert.getBytes(), provider);
            } else {
                pk = new PdfPKCS7Extended(contents.getOriginalBytes(), provider);
            }
            updateByteRange(pk, v);
            PdfString str = (PdfString) PdfReader.getPdfObject(v.get(PdfName.M));
            if (str != null) pk.setSignDate(PdfDate.decode(str.toString()));
            PdfObject obj = PdfReader.getPdfObject(v.get(PdfName.NAME));
            if (obj != null) {
                if (obj.isString()) pk.setSignName(((PdfString) obj).toUnicodeString()); else if (obj.isName()) pk.setSignName(PdfName.decodeName(obj.toString()));
            }
            str = (PdfString) PdfReader.getPdfObject(v.get(PdfName.REASON));
            if (str != null) pk.setReason(str.toUnicodeString());
            str = (PdfString) PdfReader.getPdfObject(v.get(PdfName.LOCATION));
            if (str != null) pk.setLocation(str.toUnicodeString());
            return pk;
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    private void updateByteRange(PdfPKCS7Extended pkcs7, PdfDictionary v) {
        PdfArray b = (PdfArray) PdfReader.getPdfObject(v.get(PdfName.BYTERANGE));
        RandomAccessFileOrArray rf = reader.getSafeFile();
        try {
            rf.reOpen();
            byte buf[] = new byte[8192];
            ArrayList ar = b.getArrayList();
            for (int k = 0; k < ar.size(); ++k) {
                int start = ((PdfNumber) ar.get(k)).intValue();
                int length = ((PdfNumber) ar.get(++k)).intValue();
                rf.seek(start);
                while (length > 0) {
                    int rd = rf.read(buf, 0, Math.min(length, buf.length));
                    if (rd <= 0) break;
                    length -= rd;
                    pkcs7.update(buf, 0, rd);
                }
            }
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        } finally {
            try {
                rf.close();
            } catch (Exception e) {
            }
        }
    }
}
