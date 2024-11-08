package com.jz.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

/**
 * <p>
 * <a href="SelfClassLoader.java.html"><i>View Source</i></a>
 * </p>
 *
 * @author 5jxiang
 * @version $Id$
 */
public class SelfClassLoader extends ClassLoader {

    protected Class findClass(String name) throws ClassNotFoundException {
        byte[] bytes = loadClassBytes(name);
        Class theClass = defineClass(name, bytes, 0, bytes.length);
        if (theClass == null) throw new ClassFormatError();
        return theClass;
    }

    private byte[] loadClassBytes(String className) throws ClassNotFoundException {
        try {
            String classFile = getClassFile(className);
            FileInputStream fis = new FileInputStream(classFile);
            FileChannel fileC = fis.getChannel();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            WritableByteChannel outC = Channels.newChannel(baos);
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (true) {
                int i = fileC.read(buffer);
                if (i == 0 || i == -1) {
                    break;
                }
                buffer.flip();
                outC.write(buffer);
                buffer.clear();
            }
            fis.close();
            return baos.toByteArray();
        } catch (IOException fnfe) {
            throw new ClassNotFoundException(className);
        }
    }

    private String getClassFile(String name) {
        StringBuffer sb = new StringBuffer(SelfClassLoader.class.getResource("/").getPath());
        name = name.replace('.', File.separator.charAt(0)) + ".class";
        sb.append(File.separator + name);
        return sb.toString();
    }
}
