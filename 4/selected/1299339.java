package org.jostraca.format;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import org.jostraca.BasicWriterFormat;
import org.jostraca.DefaultWriterFormats;
import org.jostraca.Property;
import org.jostraca.Tools;
import org.jostraca.WriterFormat;
import org.jostraca.WriterFormatException;
import org.jostraca.util.FileUtil;
import org.jostraca.util.PropertySet;
import org.jostraca.util.Standard;
import org.jostraca.util.TextUtil;

public class BasicFormatManager implements FormatManager {

    private HashMap mFormatMap = new HashMap();

    public void addFormat(WriterFormat pWriterFormat) {
        mFormatMap.put(pWriterFormat.getPath(), pWriterFormat);
    }

    public WriterFormat getFormat(PropertySet pPropertySet) throws Exception {
        String formatname = pPropertySet.get(Property.main_CodeWriterFormat);
        WriterFormat wf = (WriterFormat) mFormatMap.get(formatname);
        if (null == wf) {
            wf = load(pPropertySet);
        }
        return wf;
    }

    private WriterFormat load(PropertySet pPropertySet) {
        String main_CodeWriterFormat = pPropertySet.get(Property.main_CodeWriterFormat);
        String writerFormatSource = Standard.EMPTY;
        String writerFormatPath = Standard.EMPTY;
        if (TextUtil.hasValue(main_CodeWriterFormat)) {
            String cwfname = main_CodeWriterFormat;
            if (!cwfname.endsWith(WriterFormat.WRITER_FORMAT_EXTENSION)) {
                cwfname += WriterFormat.WRITER_FORMAT_EXTENSION;
            }
            File canonicalwfpath = new File(Tools.resolvePath(pPropertySet.get(Property.jostraca_FormatFolder), cwfname));
            File wfpath = canonicalwfpath;
            if (!wfpath.exists()) {
                ArrayList alternatives = new ArrayList();
                alternatives.add(new File(Tools.resolvePath(pPropertySet.get(Property.jostraca_FormatFolder), main_CodeWriterFormat)));
                alternatives.add(new File(Tools.resolvePath(pPropertySet.get(Property.jostraca_Location), cwfname)));
                alternatives.add(new File(Tools.resolvePath(pPropertySet.get(Property.jostraca_Location), main_CodeWriterFormat)));
                int numAlt = alternatives.size();
                for (int altI = 0; altI < numAlt; altI++) {
                    File alt = (File) alternatives.get(altI);
                    if (alt.exists()) {
                        wfpath = alt;
                        break;
                    }
                }
            }
            try {
                writerFormatPath = wfpath.getAbsolutePath();
                writerFormatSource = FileUtil.readFile(writerFormatPath);
            } catch (Exception e) {
                String canonicalWriterFormatName = DefaultWriterFormats.makeCanonicalWriterFormatName(main_CodeWriterFormat);
                if (DefaultWriterFormats.hasWriterFormat(canonicalWriterFormatName)) {
                    writerFormatPath = "default-internal-" + canonicalWriterFormatName;
                    writerFormatSource = DefaultWriterFormats.getWriterFormat(canonicalWriterFormatName);
                } else {
                    throw WriterFormatException.CODE_writer_format_load(canonicalwfpath);
                }
            }
        } else {
            throw WriterFormatException.CODE_prop_missing("propname", main_CodeWriterFormat);
        }
        BasicWriterFormat bwf = new BasicWriterFormat(writerFormatPath, writerFormatSource);
        return bwf;
    }
}
