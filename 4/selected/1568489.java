package be.lassi.lanbox.domain;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;
import be.lassi.support.ObjectBuilder;
import be.lassi.support.ObjectTest;

public class PatchParametersTestCase {

    @Test
    public void equals() {
        PatchParameters parameters1 = new PatchParameters();
        PatchParameters parameters2 = new PatchParameters();
        assertTrue(parameters1.equals(parameters2));
        parameters1.add(0, 0);
        assertFalse(parameters1.equals(parameters2));
        parameters2.add(0, 0);
        assertTrue(parameters1.equals(parameters2));
    }

    @Test
    public void object() {
        ObjectBuilder b = new ObjectBuilder() {

            public Object getObject1() {
                PatchParameters parameters = new PatchParameters();
                parameters.add(1, 2);
                return parameters;
            }

            public Object getObject2() {
                PatchParameters parameters = new PatchParameters();
                parameters.add(2, 1);
                return parameters;
            }
        };
        ObjectTest.test(b);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void addValidation1() {
        new PatchParameters().add(-1, 0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void addValidation2() {
        new PatchParameters().add(512, 0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void addValidation3() {
        new PatchParameters().add(0, -2);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void addValidation4() {
        new PatchParameters().add(0, 512);
    }

    @Test
    public void get() {
        PatchParameters parameters = new PatchParameters();
        parameters.add(1, 2);
        parameters.add(3, 4);
        assertEquals(parameters.getDimmerId(0), 1);
        assertEquals(parameters.getChannelId(0), 2);
        assertEquals(parameters.getDimmerId(1), 3);
        assertEquals(parameters.getChannelId(1), 4);
    }

    @Test
    public void split() {
        PatchParameters[] splitted = split(10);
        assertEquals(splitted.length, 1);
        assertEquals(splitted[0].size(), 10);
        splitted = split(255);
        assertEquals(splitted.length, 1);
        assertEquals(splitted[0].size(), 255);
        splitted = split(256);
        assertEquals(splitted.length, 2);
        assertEquals(splitted[0].size(), 255);
        assertEquals(splitted[1].size(), 1);
        splitted = split(510);
        assertEquals(splitted.length, 2);
        assertEquals(splitted[0].size(), 255);
        assertEquals(splitted[1].size(), 255);
        splitted = split(512);
        assertEquals(splitted.length, 3);
        assertEquals(splitted[0].size(), 255);
        assertEquals(splitted[1].size(), 255);
        assertEquals(splitted[2].size(), 2);
    }

    private PatchParameters[] split(final int pairCount) {
        PatchParameters parameters = new PatchParameters();
        for (int i = 0; i < pairCount; i++) {
            parameters.add(i, i);
        }
        return parameters.split();
    }
}
