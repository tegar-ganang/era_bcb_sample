package org.pagger.data.picture;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Comparator;
import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.pagger.util.ObjectUtils;

public class TestHelper {

    private TestHelper() {
    }

    public static File getResource(String resource) throws IOException, URISyntaxException {
        final URL resourceURL = TestHelper.class.getResource(resource);
        final File orig = new File(resourceURL.toURI());
        final File copy = File.createTempFile(orig.getName(), "");
        FileUtils.copyFile(orig, copy);
        return copy;
    }

    public static <T> void assertEquals(T o1, T o2, Comparator<T> c) {
        boolean equals = false;
        if (o1 != null && o2 != null) {
            equals = c.compare(o1, o2) == 0;
        } else if (o1 == null && o2 == null) {
            equals = true;
        }
        Assert.assertTrue(o1 + " != " + o2, equals);
    }

    public static void assertArrayEquals(Object array1, Object array2) {
        assertArrayEquals(array1, array2, "");
    }

    public static void assertArrayEquals(Object array1, Object array2, String message) {
        if (array1 != null && array2 != null) {
            if (array1.getClass().isArray() && array2.getClass().isArray() && array1.getClass().equals(array2.getClass())) {
                final int length1 = Array.getLength(array1);
                final int length2 = Array.getLength(array2);
                if (length1 == length2) {
                    for (int i = 0; i < length1; i++) {
                        Object obj1 = Array.get(array1, i);
                        Object obj2 = Array.get(array2, i);
                        boolean isEqual = ObjectUtils.dynamicEquals(obj1, obj2);
                        if (isEqual == false) {
                            Assert.assertTrue(message + String.format(" Element %s is not equal expected %s was %s.", i, obj1, obj2), false);
                        }
                    }
                } else {
                    Assert.assertTrue(message + " Array length is not the same.", false);
                }
            } else {
                Assert.assertTrue(message + " Types are not the same.", false);
            }
        } else {
            Assert.assertTrue(message + " One elements is null.", false);
        }
    }
}
