package com.dbcp.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;

/**
 * ����: ��ѹ���ļ�
 * @author xiongyongjie
 * @version
 * @since 2007-12-17
 */
public class ZipUtils {

    private static final Logger log = Logger.getLogger(ZipUtils.class);

    /**
	 * ��ĳ��Ŀ¼�µ������ļ������һ��ѹ���ļ�
	 * 
	 * @param inpput ������һ��Ŀ¼�����ļ�ȫ·��
	 * @param output ����ļ�ȫ·��
	 */
    public static void zip(String input, String output) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(output);
            zip(input, out);
        } catch (FileNotFoundException e) {
            log.error("�����ҵ��ļ�:" + output, e);
            throw new RuntimeException("�����ҵ��ļ�:" + output, e);
        } finally {
            if (null != out) try {
                out.close();
                out = null;
            } catch (IOException e) {
                log.error("�ر��ļ���ʧ��");
            }
        }
    }

    /**
	 * ��Դ�ļ�/Ŀ¼,ZIP����������
	 * 
	 * @param inpput ������һ��Ŀ¼�����ļ�ȫ·��
	 * @param out �����
	 */
    @SuppressWarnings("unchecked")
    public static void zip(String input, OutputStream out) {
        File file = new File(input);
        ZipOutputStream zip = null;
        FileInputStream in = null;
        try {
            if (file.exists()) {
                Collection<File> items = new ArrayList();
                if (file.isDirectory()) {
                    items = FileUtils.listFiles(file, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
                    zip = new ZipOutputStream(out);
                    zip.putNextEntry(new ZipEntry(file.getName() + "/"));
                    Iterator iter = items.iterator();
                    while (iter.hasNext()) {
                        File item = (File) iter.next();
                        in = new FileInputStream(item);
                        zip.putNextEntry(new ZipEntry(file.getName() + "/" + item.getName()));
                        IOUtils.copy(in, zip);
                        IOUtils.closeQuietly(in);
                        zip.closeEntry();
                    }
                    IOUtils.closeQuietly(zip);
                }
            } else {
                log.info("-->>���ļ���û���ļ�");
            }
        } catch (Exception e) {
            log.error("����ѹ��" + input + "�������", e);
            throw new RuntimeException("����ѹ��" + input + "�������", e);
        } finally {
            try {
                if (null != zip) {
                    zip.close();
                    zip = null;
                }
                if (null != in) {
                    in.close();
                    in = null;
                }
            } catch (Exception e) {
                log.error("�ر��ļ�������");
            }
        }
    }

    /**
	 * ��һ��ZIP�ļ���ѹ����ָ��Ŀ¼
	 * 
	 * @param input
	 * @param output
	 */
    @SuppressWarnings("unchecked")
    public static void unzip(String input, String output) {
        try {
            if (!output.endsWith("/")) output = output + "/";
            ZipFile zip = new ZipFile(input);
            Enumeration entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.isDirectory()) {
                    FileUtils.forceMkdir(new File(output + entry.getName()));
                } else {
                    FileOutputStream out = new FileOutputStream(output + entry.getName());
                    IOUtils.copy(zip.getInputStream(entry), out);
                    IOUtils.closeQuietly(out);
                }
            }
        } catch (Exception e) {
            log.error("�����ҵ��ļ�:" + output, e);
            throw new RuntimeException("�����ҵ��ļ�:" + output, e);
        }
    }

    /**
	 * ��ZIP��������ѹ����ָ��Ŀ¼
	 * 
	 * @param in ������
	 * @param output ���Ŀ¼
	 */
    public static void unzip(InputStream in, String output) {
        try {
            if (!output.endsWith("/")) output = output + "/";
            ZipInputStream zip = new ZipInputStream(in);
            ZipEntry entry = null;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    FileUtils.forceMkdir(new File(output + entry.getName()));
                } else {
                    FileOutputStream out = new FileOutputStream(output + entry.getName());
                    byte[] buffer = new byte[1024];
                    int length = -1;
                    while ((length = zip.read(buffer, 0, 1024)) > 0) {
                        out.write(buffer, 0, length);
                    }
                    IOUtils.closeQuietly(out);
                }
            }
            IOUtils.closeQuietly(in);
        } catch (Exception e) {
            log.error("���ܳɹ���ѹ��!", e);
            throw new RuntimeException("���ܳɹ���ѹ��!", e);
        }
    }
}
