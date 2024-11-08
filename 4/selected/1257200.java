package net.os.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 
 * @Description��
 * @author: ���ҷ�
 * @Time: May 25, 2011 4:01:01 PM
 */
public final class IOUtils {

    public static final byte[] NBYTE = new byte[0];

    /**
	 * �����ļ���Properties����
	 * @param props
	 * @param filePath
	 */
    public static final void loadProperties(final Properties props, final String filePath) {
        try {
            loadProperties(props, new FileInputStream(filePath));
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static final void loadProperties(final Properties props, final InputStream inStream) {
        if (null == inStream) return;
        try {
            props.load(inStream);
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            closeIO(inStream);
        }
    }

    /**
	 * ���ڹ���Ŀ¼���ļ�������
	 */
    public static final FileFilter DIR_FILTER = new FileFilter() {

        public boolean accept(final File pathname) {
            return pathname.isDirectory();
        }
    };

    /**
	 * ���ڹ����ļ����ļ�������
	 */
    public static final FileFilter FILE_FILTER = new FileFilter() {

        public boolean accept(final File pathname) {
            return pathname.isFile();
        }
    };

    /**
	 * ��ȡָ��Ŀ¼�µ������ļ�,�ӹ�����
	 * @return
	 */
    public static List<File> listAllFiles(final File file, final FileFilter filter) {
        final List<File> filesList = new ArrayList<File>(8);
        if (file.isFile()) {
            filesList.add(file);
        } else {
            final File[] files = file.listFiles(filter);
            for (final File f : files) {
                filesList.addAll(listAllFiles(f, filter));
            }
        }
        return filesList;
    }

    /**
	 * ��װ�رն���������������������κ�IO��Դ
	 * @param ioObjects
	 */
    public static final void closeIO(final Closeable... ioObjects) {
        if (null == ioObjects || ioObjects.length == 0) return;
        for (final Closeable ioObject : ioObjects) {
            try {
                if (null != ioObject) ioObject.close();
            } catch (final Throwable e) {
            }
        }
    }

    /**
	 * ���ڽ��MapBuf���ͷ��ļ���������,<a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4724038">bug 4724038</a>
	 * 
	 * @param buffer the Buffer to release
	 */
    public static final void closeBuffer(final Buffer buffer) {
        if (null == buffer) return;
        AccessController.doPrivileged(new PrivilegedAction<Object>() {

            public Object run() {
                try {
                    final Method cleanerMethod = buffer.getClass().getMethod("cleaner");
                    if (null == cleanerMethod) return null;
                    cleanerMethod.setAccessible(true);
                    final Object cleanerObj = cleanerMethod.invoke(buffer);
                    if (null == cleanerObj) return null;
                    final Method cleanMethod = cleanerObj.getClass().getMethod("clean");
                    if (null == cleanMethod) return null;
                    cleanMethod.invoke(cleanerObj);
                } catch (final Throwable e) {
                }
                return null;
            }
        });
    }

    /**
	 * ����һ�����ļ�
	 * 
	 * @param file
	 * @throws IOException
	 */
    public static void createFile(final File file) throws IOException {
        if (file.exists()) return;
        if (file.getParentFile() != null) file.getParentFile().mkdirs();
        file.createNewFile();
    }

    /**
	 * �����������ж�ȡ��ݲ�д�뵽�����
	 * 
	 * @param in
	 * @param out
	 * @return �����̿������ֽ������
	 * @throws IOException
	 */
    public static int copyStream(final InputStream in, final OutputStream out) throws IOException {
        int readedCount = 0;
        final byte[] tmpBuf = new byte[10240];
        while (true) {
            final int numRead = in.read(tmpBuf);
            if (numRead == -1) break;
            out.write(tmpBuf, 0, numRead);
            out.flush();
            readedCount += numRead;
        }
        return readedCount;
    }

    /**
	 * �������������inд�����ļ�file��.
	 * 
	 * @param file
	 * @param in
	 * @throws IOException
	 */
    public static void writeToFile(final File file, final InputStream in) throws IOException {
        IOUtils.createFile(file);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            IOUtils.copyStream(in, fos);
        } finally {
            IOUtils.closeIO(fos);
        }
    }

    /**
	 * �����������bд�����ļ�file��.
	 * 
	 * @param file
	 * @param bytes
	 * @throws IOException
	 */
    public static void writeToFile(final File file, final byte[] bytes) throws IOException {
        IOUtils.createFile(file);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.flush();
        } finally {
            IOUtils.closeIO(fos);
        }
    }

    public static byte[] getStreamBytes(final InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copyStream(in, out);
        return out.toByteArray();
    }

    public static byte[] getBytes(final Serializable o) throws IOException {
        return IOUtils.getBytes(o, true);
    }

    public static byte[] getBytes(final Serializable o, final boolean zip) throws IOException {
        if (o == null) {
            return IOUtils.NBYTE;
        }
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(o);
        if (zip) {
            final ByteArrayOutputStream ret = new ByteArrayOutputStream();
            final ZipOutputStream zos = new ZipOutputStream(ret);
            zos.putNextEntry(new ZipEntry("obj"));
            zos.write(bos.toByteArray());
            zos.close();
            return ret.toByteArray();
        }
        return bos.toByteArray();
    }

    public static Serializable unSerial(final byte[] b) throws Exception {
        return IOUtils.unSerial(b, true);
    }

    public static Serializable unSerial(final byte[] b, final boolean zip) throws Exception {
        if (b == null || b.length == 0) {
            return null;
        }
        final ByteArrayInputStream bis = new ByteArrayInputStream(b);
        if (zip) {
            ZipInputStream zis = null;
            try {
                zis = new ZipInputStream(bis);
                if (zis.getNextEntry() != null) {
                    int count;
                    final byte data[] = new byte[2048];
                    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    while ((count = zis.read(data, 0, 2048)) != -1) {
                        bos.write(data, 0, count);
                    }
                    final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
                    return (Serializable) ois.readObject();
                }
                return null;
            } finally {
                if (zis != null) {
                    zis.close();
                }
            }
        }
        final ObjectInputStream ois = new ObjectInputStream(bis);
        return (Serializable) ois.readObject();
    }

    /**
	 * �˷��������Ӹ�Ļ�����·���£�������������ļ����URI·��,
	 * Ĭ��ʹ��4����Ŀ¼
	 * 
	 * @see #listFiles(String, String, int)
	 * @param basePath
	 * @param fileNameEndStr
	 * @return
	 */
    public static final List<File> listFiles(final String basePath, final String... fileNameEndStr) {
        return IOUtils.listFiles(basePath, 4, fileNameEndStr);
    }

    public static final List<File> listsFiles(final String basePath, final String... fileNameEndStr) {
        final List<File> files = new ArrayList<File>();
        String[] paths = basePath.split("\\|");
        for (String path : paths) {
            final List<File> file = IOUtils.listFiles(path, fileNameEndStr);
            if (file.size() > 0) {
                files.addAll(file);
            } else {
                final File f = new File(path);
                if (f.isFile()) {
                    files.add(f);
                }
            }
        }
        return files;
    }

    public static List<File> listFiles(final File file, final FileFilter fileFilter) {
        final List<File> list = new ArrayList<File>();
        final File[] files = file.listFiles(fileFilter);
        list.addAll(Arrays.asList(files));
        if (files.length != 0) {
            for (final File f : files) {
                list.addAll(listFiles(f, fileFilter));
                return list;
            }
        } else return list;
        return list;
    }

    /**
	 * ָ��������
	 * @return
	 */
    public static List<File> listFiles(final String basePath, final FileFilter fileFilter) {
        final List<File> list = new ArrayList<File>();
        final File baseFile = new File(basePath);
        if (baseFile.exists()) {
            final File[] files = baseFile.listFiles(fileFilter);
            if (null != files && files.length > 0) {
                for (final File subFile : files) {
                    if (subFile.isFile()) list.add(subFile);
                }
            }
        }
        return list;
    }

    /**
	 * �˷��������Ӹ�Ļ�����·���£�������������ļ����URI·��
	 * 
	 * @param basePath
	 *            ������·��
	 * @param fileNameEndStr
	 *            �����ļ���ƻ�����Ƶĺ���һ�����ַ�
	 * @param deep
	 *            �������ȣ���ʾ���Ŀ¼�»�����Ŀ¼�����ɴ����Ŀ¼�����
	 * @return
	 */
    public static final List<File> listFiles(final String basePath, int deep, final String... fileNameEndStr) {
        final List<File> list = new ArrayList<File>();
        final File baseFile = new File(basePath);
        File[] files = baseFile.listFiles(new SampleFileFilter(fileNameEndStr));
        if (null != files && files.length > 0) {
            for (final File subFile : files) {
                if (subFile.isFile()) list.add(subFile);
            }
        }
        if (deep <= 0) return list;
        files = baseFile.listFiles(IOUtils.DIR_FILTER);
        if (null != files && files.length > 0) {
            final int dp = --deep;
            for (final File subFile : files) {
                list.addAll(IOUtils.listFiles(subFile.getAbsolutePath(), dp, fileNameEndStr));
            }
        }
        return list;
    }

    /**
	 * ���ٿ����ļ��ķ���.
	 * @param sourceFile
	 * @param destFile
	 * @throws IOException
	 */
    public static void copyFile(final File sourceFile, final File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        FileInputStream inStream = null;
        FileOutputStream outStream = null;
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = (inStream = new FileInputStream(sourceFile)).getChannel();
            destination = (outStream = new FileOutputStream(destFile)).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            closeIO(source);
            closeIO(inStream);
            closeIO(destination);
            closeIO(outStream);
        }
    }

    /**
	 * ���ٿ���һ��Ŀ¼�ṹ
	 * @param sourceDir
	 * @param destDir
	 * @throws IOException
	 */
    public static void copyDirectory(final File sourceDir, final File destDir) throws IOException {
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        final File[] children = sourceDir.listFiles();
        for (final File sourceChild : children) {
            final String name = sourceChild.getName();
            final File destChild = new File(destDir, name);
            if (sourceChild.isDirectory()) {
                copyDirectory(sourceChild, destChild);
            } else {
                copyFile(sourceChild, destChild);
            }
        }
    }

    /**
	 * ��һ���ļ������ݶ�ȡ��һ���ַ��������κ��쳣������null
	 * 
	 * @param file
	 * @return
	 */
    public static final String readFromFileAsString(final File file) {
        try {
            return new String(IOUtils.readFromFile(file));
        } catch (final Exception e) {
            return null;
        }
    }

    public static byte[] readFromFile(final File file) throws IOException {
        return IOUtils.getStreamBytes(new FileInputStream(file));
    }

    /**
	 * ��һ�������������ȡ��һ���ַ�
	 * 
	 * @param inputStream
	 * @return
	 */
    public static final String readFromStreamAsString(final InputStream inputStream) {
        try {
            return new String(IOUtils.getStreamBytes(inputStream));
        } catch (final Exception e) {
            return null;
        }
    }

    /**
	 * ���ļ��ж�ȡһ���ַ��б�
	 * 
	 * @param filename
	 *            String
	 * @return String[]
	 */
    public static List<String> readListFromFile(final String filename) throws IOException {
        return readListFromFile(new File(filename));
    }

    public static List<String> readListFromFile(final InputStream stream) throws IOException {
        return readListFromFile(stream, null);
    }

    /**
	 * @param file
	 *            Ҫ�������ļ�
	 * @param split
	 *            ��ÿһ����ݽ��зָ����ַ�
	 * @return ����б�
	 * @throws IOException
	 */
    public static List<List<String>> readListFromFile(final File file, final String split) throws IOException {
        final List<String> lines = readListFromFile(file);
        final List<List<String>> datasList = new ArrayList<List<String>>();
        for (final String line : lines) {
            final List<String> dataList = new ArrayList<String>();
            dataList.addAll(Arrays.asList(line.split(split)));
            datasList.add(dataList);
        }
        return datasList;
    }

    public static List<String> readListFromFile(final InputStream stream, final String[] exceptStartwith) throws IOException {
        final List<String> result = new ArrayList<String>();
        final BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(stream));
        String s;
        while ((s = bufferedreader.readLine()) != null) {
            if (exceptStartwith != null) {
                boolean contain = false;
                for (final String except : exceptStartwith) {
                    if (s.startsWith(except)) {
                        contain = true;
                        break;
                    }
                }
                if (contain) continue;
            }
            result.add(s);
        }
        return result;
    }

    public static List<String> readListFromFile(final File file) throws IOException {
        return readListFromFile(new FileInputStream(file), null);
    }

    public static List<String> readListFromFile(final File file, final String[] except) throws IOException {
        return readListFromFile(new FileInputStream(file), except);
    }

    /**
	 * ��ȫɾ��һ���ļ�����Ŀ¼��
	 * @see #delete(File)
	 * @param file
	 */
    public static final void delete(final String file) {
        delete(new File(file));
    }

    /**
	 * ��ȫɾ��һ���ļ�����Ŀ¼��
	 * @param fileOrDir
	 */
    public static final void delete(final File fileOrDir) {
        if (!fileOrDir.exists()) return;
        if (fileOrDir.isFile()) {
            fileOrDir.delete();
            fileOrDir.deleteOnExit();
            return;
        }
        final File subFiles[] = fileOrDir.listFiles();
        if (null != subFiles && subFiles.length > 0) {
            for (final File subFile : subFiles) delete(subFile);
        }
        fileOrDir.delete();
        fileOrDir.deleteOnExit();
    }

    public static String getFileNameWithoutExtendsion(final String s) {
        String ext = null;
        final int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(0, i);
        }
        return ext;
    }

    public static final String read(final InputStream is, final int length) throws Exception {
        final byte[] buf = new byte[length];
        is.read(buf);
        return new String(buf);
    }

    public static boolean validateExtension(final File file, final String... exten) {
        if (exten == null) {
            return true;
        }
        final String fName = file.getName();
        final String extension = fName.substring(fName.lastIndexOf("."), fName.length()).toLowerCase();
        for (final String ext : exten) if (ext.toLowerCase().endsWith(extension)) return true;
        return false;
    }

    /**
	 * ����ļ��ֽڳ�����ʾ��ǩ,��:1KB,1MB,1GB,
	 * 
	 * @param fileSize
	 * @return
	 */
    public static final String getFileSizeLabel(final long fileSize) {
        double label;
        String danWei;
        final long kb = 1024;
        if (fileSize < kb) return fileSize + "Byte";
        final long mb = kb * 1024;
        final long gb = mb * 1024;
        if (fileSize < mb) {
            label = (double) fileSize / (double) kb;
            danWei = "KB";
        } else if (fileSize < gb) {
            label = (double) fileSize / (double) mb;
            danWei = "MB";
        } else {
            label = (double) fileSize / (double) gb;
            danWei = "GB";
        }
        return NumberFormat.getNumberInstance().format(label) + danWei;
    }

    /**
	 * @param out �����ӡ�ӿ�
	 * @param indentLevel ÿһ����λ��Ӧ2���ո�λ��
	 * @param args Ҫ�������ݼ�
	 */
    public static final void println(final PrintStream out, final int indentLevel, final Object... args) {
        final int j = indentLevel * 2;
        int i = 0;
        final StringBuilder buf = new StringBuilder(32);
        do {
            if (i >= j) break;
            buf.append(' ');
            i++;
        } while (true);
        for (final Object obj : args) buf.append(obj);
        out.println(buf.toString());
    }

    /**
	 * ʹ��Java��׼Zip�ӿڽ�ѹZip�ļ�.
	 * @param zipFile
	 * @return
	 */
    public static final Collection<File> unZip(final String zipFile) {
        return unZip(new File(zipFile));
    }

    /**
	 * ʹ��Java��׼Zip�ӿڽ�ѹZip�ļ�.
	 * @param zipFile
	 * @return
	 */
    public static final Collection<File> unZip(final File zipFile) {
        return unZip(zipFile, new File(zipFile + "_unzip"), Charset.forName("GBK"));
    }

    /**
	 * ʹ��Java��׼Zip�ӿڽ�ѹZip�ļ�.
	 * @param zipFile
	 * @param outDir
	 * @param charset
	 * @return
	 */
    public static final Collection<File> unZip(final File zipFile, final File outDir, final Charset charset) {
        try {
            ZipInputStream zis = null;
            zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
            return unZip(zis, outDir);
        } catch (final IOException e) {
            return new ArrayList<File>(0);
        }
    }

    /**
	 * ʹ��Java��׼Zip�ӿڽ�ѹZip������.
	 * @param in
	 * @param outDir
	 * @return
	 */
    public static final Collection<File> unZip(final ZipInputStream in, final File outDir) {
        final Collection<File> result = new ArrayList<File>(4);
        try {
            ZipEntry entry;
            BufferedOutputStream out = null;
            final byte data[] = new byte[10240];
            while (true) {
                out = null;
                try {
                    entry = in.getNextEntry();
                    if (null == entry) break;
                    int count;
                    final File outFile = new File(outDir, entry.getName());
                    outFile.getParentFile().mkdirs();
                    out = new BufferedOutputStream(new FileOutputStream(outFile));
                    while ((count = in.read(data)) != -1) out.write(data, 0, count);
                    out.flush();
                    result.add(outFile);
                } catch (final Exception ioex) {
                    ioex.printStackTrace();
                } finally {
                    closeIO(out);
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            closeIO(in);
        }
        return result;
    }

    /**
	 * ʹ��Java��׼GZip�ӿڽ�ѹGZip�ļ�.
	 * @param gzipFile
	 * @return
	 */
    public static final File unGZip(final String gzipFile) {
        return unGZip(new File(gzipFile));
    }

    /**
	 * ʹ��Java��׼GZip�ӿڽ�ѹGZip�ļ�.
	 * @param gzipFile
	 * @return
	 * @throws IOException 
	 * @throws  
	 */
    public static final File unGZip(final File gzipFile) {
        try {
            return unGZip(new GZIPInputStream(new BufferedInputStream(new FileInputStream(gzipFile))), new File(gzipFile.getParentFile(), gzipFile.getName() + ".unGzip"));
        } catch (final IOException ioex) {
            ioex.printStackTrace();
            return null;
        }
    }

    /**
	 * ʹ��Java��׼GZip�ӿڽ�ѹGZip������.
	 * @param in
	 * @param outFile
	 * @return
	 */
    public static final File unGZip(final GZIPInputStream in, final File outFile) {
        BufferedOutputStream out = null;
        try {
            final byte data[] = new byte[10240];
            out = new BufferedOutputStream(new FileOutputStream(outFile));
            int count;
            while ((count = in.read(data)) != -1) out.write(data, 0, count);
            out.flush();
            return outFile;
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeIO(in);
        }
    }

    /**
	 * ��øô��̵�ʣ��ռ�
	 * @param file
	 * @return
	 */
    public static int getFreeSpaceM(final File file) {
        if (!file.exists()) {
            file.mkdirs();
        }
        return (int) (file.getFreeSpace() / (1024 * 1024));
    }
}
