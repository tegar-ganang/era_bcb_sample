package com.fastaop.transform.discovery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import com.fastaop.instrument.TestClass1;
import com.fastaop.instrument.testannotations.TestAnnotation1;
import com.fastaop.instrument.testannotations.TestMethodAnnotation1;
import com.fastaop.instrument.testannotations.TestMethodAnnotation2;
import junit.framework.TestCase;

public class AnnotationFinderTest extends TestCase {

    public void testFindAllAnnotations() throws Exception {
        ClassReader cr = new ClassReader(loadClass(TestClass1.class.getName()));
        final AnnotationFinder finder = new AnnotationFinder();
        cr.accept(finder, ClassReader.SKIP_CODE);
        assertTrue(finder.getClassAnnotations().contains("[C]" + TestAnnotation1.class.getName()));
        Set<String> annotationsForMethod = finder.getMethodAnnotations().get("method1(Ljava/lang/String;I)V");
        assertTrue(annotationsForMethod.contains("[M]" + TestMethodAnnotation1.class.getName()));
        assertTrue(annotationsForMethod.contains("[M]" + TestMethodAnnotation2.class.getName()));
    }

    private byte[] loadClass(String clazz) {
        try {
            InputStream resourceAsStream = getClass().getResourceAsStream("/" + clazz.replace('.', '/') + ".class");
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] byteArray = new byte[1024];
            int readBytes = -1;
            while ((readBytes = resourceAsStream.read(byteArray, 0, byteArray.length)) != -1) out.write(byteArray, 0, readBytes);
            resourceAsStream.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
