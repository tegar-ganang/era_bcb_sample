package com.dustedpixels.jasmin.asm;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.ASMifierClassVisitor;

/**
 * @author micapolos@gmail.com (Michal Pociecha-Los)
 */
public final class AsmClassLoader extends ClassLoader {

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        print(name);
        return super.loadClass(name);
    }

    private void print(String className) {
        try {
            InputStream input = getClass().getResourceAsStream("/" + className.replace('.', '/') + ".class");
            System.out.println("Loaded byte code");
            ClassReader reader;
            reader = new ClassReader(input);
            ASMifierClassVisitor writer = new ASMifierClassVisitor(new PrintWriter(System.out));
            reader.accept(writer, 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        ClassLoader loader = new AsmClassLoader();
        loader.loadClass("com.dustedpixels.jasmin.asm.NandGateImpl");
    }
}
