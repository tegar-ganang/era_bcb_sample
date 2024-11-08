package com.google.gdt.eclipse.designer.webkit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/**
 * WebKit deploying helper.
 * 
 * @author mitin_aa
 */
public class WebKitSupportWin32 {

    private static final String WEBKIT_VERSION_NAME = "webkit.version";

    private static File WEBKIT_DIR = new File(WebKitActivator.getDefault().getStateLocation().toFile(), "WebKit");

    private static boolean m_initialized;

    private static boolean m_available;

    private WebKitSupportWin32() {
    }

    public static void deployIfNeededAndLoad() {
        if (!m_initialized) {
            try {
                Bundle bundle = Platform.getBundle("com.google.gdt.eclipse.designer.hosted.2_0.webkit");
                URL resource = FileLocator.resolve(bundle.getResource("WebKit.zip"));
                ZipFile zipFile = new ZipFile(resource.getPath());
                try {
                    if (deployNeeded(zipFile)) {
                        extract(zipFile);
                    }
                } finally {
                    zipFile.close();
                }
                load();
                m_available = true;
            } catch (Throwable e) {
            }
            m_initialized = true;
        }
    }

    public static boolean isAvailable() {
        return m_available;
    }

    private static boolean deployNeeded(ZipFile zipFile) {
        if (WEBKIT_DIR.exists()) {
            try {
                File versionFile = new File(WEBKIT_DIR, WEBKIT_VERSION_NAME);
                if (!versionFile.exists()) {
                    return true;
                }
                String currentVersion = readString(new FileInputStream(versionFile));
                String newVersion = readString(zipFile.getInputStream(zipFile.getEntry(WEBKIT_VERSION_NAME)));
                return !currentVersion.equals(newVersion);
            } catch (Throwable e) {
            }
        }
        return true;
    }

    private static String readString(InputStream inputStream) throws IOException {
        String stringValue = IOUtils.toString(inputStream);
        IOUtils.closeQuietly(inputStream);
        return stringValue;
    }

    private static void load() {
        String webkitDir = WEBKIT_DIR.getAbsolutePath() + File.separator;
        System.load(webkitDir + "icudt40.dll");
        System.load(webkitDir + "icuuc40.dll");
        System.load(webkitDir + "icuin40.dll");
        System.load(webkitDir + "CFLite.dll");
        System.load(webkitDir + "pthreadVC2.dll");
        System.load(webkitDir + "JavaScriptCore.dll");
        System.load(webkitDir + "libxml2.dll");
        System.load(webkitDir + "libxslt.dll");
        System.load(webkitDir + "cairo.dll");
        System.load(webkitDir + "libcurl.dll");
        System.load(webkitDir + "WebKit.dll");
    }

    private static void extract(ZipFile zipFile) throws Exception {
        FileUtils.deleteQuietly(WEBKIT_DIR);
        WEBKIT_DIR.mkdirs();
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                new File(WEBKIT_DIR, entry.getName()).mkdirs();
                continue;
            }
            InputStream inputStream = zipFile.getInputStream(entry);
            File outputFile = new File(WEBKIT_DIR, entry.getName());
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            IOUtils.copy(inputStream, outputStream);
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outputStream);
        }
    }
}
