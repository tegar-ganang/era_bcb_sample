package com.ghostsq.commander.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.PatternMatcher;
import android.util.Log;

public final class MnfUtils {

    private static final String TAG = "MnfUtils";

    private ApplicationInfo ai;

    private Resources rr;

    private String apk_path;

    private String mans;

    public MnfUtils(PackageManager pm, String app_name) {
        try {
            ai = pm.getApplicationInfo(app_name, 0);
            rr = pm.getResourcesForApplication(ai);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public MnfUtils(String apk_path_) {
        apk_path = apk_path_;
    }

    public final String extractManifest() {
        try {
            if (mans != null) return mans;
            if (ai != null) apk_path = ai.publicSourceDir;
            if (apk_path == null) return null;
            ZipFile zip = new ZipFile(apk_path);
            ZipEntry entry = zip.getEntry("AndroidManifest.xml");
            if (entry != null) {
                InputStream is = zip.getInputStream(entry);
                if (is != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream((int) entry.getSize());
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
                    is.close();
                    mans = decompressXML(baos.toByteArray());
                    return mans;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public final Drawable extractIcon() {
        try {
            if (apk_path == null) return null;
            ZipFile zip = new ZipFile(apk_path);
            ZipEntry entry = zip.getEntry("res/drawable/icon.png");
            if (entry != null) {
                InputStream is = zip.getInputStream(entry);
                return is != null ? new BitmapDrawable(is) : null;
            }
            Enumeration<? extends ZipEntry> entries = zip.entries();
            if (entries != null) {
                while (entries.hasMoreElements()) {
                    entry = entries.nextElement();
                    if (entry == null) continue;
                    String efn = entry.getName();
                    if (efn == null || !efn.startsWith("res/drawable")) continue;
                    if (efn.contains("icon")) {
                        InputStream is = zip.getInputStream(entry);
                        return is != null ? new BitmapDrawable(is) : null;
                    }
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "Can't get icon for " + apk_path, e);
        }
        return null;
    }

    private static final int endDocTag = 0x00100101;

    private static final int startTag = 0x00100102;

    private static final int endTag = 0x00100103;

    private final String decompressXML(byte[] xml) {
        StringBuffer xml_sb = new StringBuffer(8192);
        int numbStrings = LEW(xml, 4 * 4);
        int sitOff = 0x24;
        int stOff = sitOff + numbStrings * 4;
        int xmlTagOff = LEW(xml, 3 * 4);
        for (int ii = xmlTagOff; ii < xml.length - 4; ii += 4) {
            if (LEW(xml, ii) == startTag) {
                xmlTagOff = ii;
                break;
            }
        }
        int off = xmlTagOff;
        int indent = 0;
        int startTagLineNo = -2;
        while (off < xml.length) {
            int tag0 = LEW(xml, off);
            int lineNo = LEW(xml, off + 2 * 4);
            int nameNsSi = LEW(xml, off + 4 * 4);
            int nameSi = LEW(xml, off + 5 * 4);
            if (tag0 == startTag) {
                int tag6 = LEW(xml, off + 6 * 4);
                int numbAttrs = LEW(xml, off + 7 * 4);
                off += 9 * 4;
                String name = compXmlString(xml, sitOff, stOff, nameSi);
                startTagLineNo = lineNo;
                StringBuffer sb = new StringBuffer();
                for (int ii = 0; ii < numbAttrs; ii++) {
                    int attrNameNsSi = LEW(xml, off);
                    int attrNameSi = LEW(xml, off + 1 * 4);
                    int attrValueSi = LEW(xml, off + 2 * 4);
                    int attrFlags = LEW(xml, off + 3 * 4);
                    int attrResId = LEW(xml, off + 4 * 4);
                    off += 5 * 4;
                    String attrName = compXmlString(xml, sitOff, stOff, attrNameSi);
                    String attrValue = null;
                    if (attrValueSi != -1) attrValue = compXmlString(xml, sitOff, stOff, attrValueSi); else {
                        if (rr != null) try {
                            attrValue = rr.getString(attrResId);
                        } catch (NotFoundException e) {
                        }
                        if (attrValue == null) attrValue = "0x" + Integer.toHexString(attrResId);
                    }
                    sb.append("\n").append(spaces(indent + 1)).append(attrName).append("=\"").append(attrValue).append("\"");
                }
                xml_sb.append("\n").append(spaces(indent)).append("<").append(name);
                if (sb.length() > 0) xml_sb.append(sb);
                xml_sb.append(">");
                indent++;
            } else if (tag0 == endTag) {
                indent--;
                off += 6 * 4;
                String name = compXmlString(xml, sitOff, stOff, nameSi);
                xml_sb.append("\n").append(spaces(indent)).append("</").append(name).append(">");
            } else if (tag0 == endDocTag) {
                break;
            } else {
                Log.e(TAG, "  Unrecognized tag code '" + Integer.toHexString(tag0) + "' at offset " + off);
                break;
            }
        }
        Log.v(TAG, "    end at offset " + off);
        return xml_sb.toString();
    }

    private final String compXmlString(byte[] xml, int sitOff, int stOff, int strInd) {
        if (strInd < 0) return null;
        int strOff = stOff + LEW(xml, sitOff + strInd * 4);
        return compXmlStringAt(xml, strOff);
    }

    private final String spaces(int i) {
        char[] dummy = new char[i * 2];
        Arrays.fill(dummy, ' ');
        return new String(dummy);
    }

    private final String compXmlStringAt(byte[] arr, int strOff) {
        int strLen = arr[strOff + 1] << 8 & 0xff00 | arr[strOff] & 0xff;
        byte[] chars = new byte[strLen];
        for (int ii = 0; ii < strLen; ii++) {
            chars[ii] = arr[strOff + 2 + ii * 2];
        }
        return new String(chars);
    }

    private final int LEW(byte[] arr, int off) {
        return arr[off + 3] << 24 & 0xff000000 | arr[off + 2] << 16 & 0xff0000 | arr[off + 1] << 8 & 0xff00 | arr[off] & 0xFF;
    }

    public final IntentFilter[] getIntentFilters(String act_name) {
        try {
            if (mans == null) mans = extractManifest();
            if (mans != null && mans.length() > 0) {
                ArrayList<IntentFilter> list = new ArrayList<IntentFilter>();
                XmlPullParserFactory factory;
                factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(new StringReader(mans));
                int et;
                while ((et = xpp.next()) != XmlPullParser.END_DOCUMENT) {
                    if (et == XmlPullParser.START_TAG && "activity".equals(xpp.getName())) {
                        String can = xpp.getAttributeValue(null, "name");
                        if (act_name.indexOf(can) >= 0) {
                            int d = xpp.getDepth();
                            while ((et = xpp.next()) != XmlPullParser.END_DOCUMENT && (d < xpp.getDepth() || et != XmlPullParser.END_TAG)) {
                                if ("intent-filter".equals(xpp.getName())) {
                                    IntentFilter inf = new IntentFilter();
                                    initIntentFilterFromXml(inf, xpp);
                                    list.add(inf);
                                }
                            }
                            break;
                        }
                    }
                }
                if (list.size() > 0) {
                    IntentFilter[] ret = new IntentFilter[list.size()];
                    return list.toArray(ret);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static final boolean initIntentFilterFromXml(IntentFilter inf, XmlPullParser xpp) {
        try {
            int outerDepth = xpp.getDepth();
            int type;
            while ((type = xpp.next()) != XmlPullParser.END_DOCUMENT && (type != XmlPullParser.END_TAG || xpp.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) continue;
                final String NAME = "name";
                String tag = xpp.getName();
                if (tag.equals("action")) {
                    String name = xpp.getAttributeValue(null, NAME);
                    if (name != null) inf.addAction(name);
                } else if (tag.equals("category")) {
                    String name = xpp.getAttributeValue(null, NAME);
                    if (name != null) inf.addCategory(name);
                } else if (tag.equals("data")) {
                    int na = xpp.getAttributeCount();
                    for (int i = 0; i < na; i++) {
                        String port = null;
                        String an = xpp.getAttributeName(i);
                        String av = xpp.getAttributeValue(i);
                        if ("mimeType".equals(an)) {
                            try {
                                inf.addDataType(av);
                            } catch (MalformedMimeTypeException e) {
                            }
                        } else if ("scheme".equals(an)) {
                            inf.addDataScheme(av);
                        } else if ("host".equals(an)) {
                            inf.addDataAuthority(av, port);
                        } else if ("port".equals(an)) {
                            port = av;
                        } else if ("path".equals(an)) {
                            inf.addDataPath(av, PatternMatcher.PATTERN_LITERAL);
                        } else if ("pathPrefix".equals(an)) {
                            inf.addDataPath(av, PatternMatcher.PATTERN_PREFIX);
                        } else if ("pathPattern".equals(an)) {
                            inf.addDataPath(av, PatternMatcher.PATTERN_SIMPLE_GLOB);
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
