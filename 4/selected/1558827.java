package com.google.code.ebmlviewer.core;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static oe.assertions.Assertions.assertThat;
import static oe.assertions.Predicates.isEqualTo;
import static oe.assertions.Predicates.isSameAs;

public class FloatingPointTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void decodeWithNullBufferFails() {
        EbmlDecoder decoder = new EbmlDecoder();
        decoder.decodeFloatingPoint(null, 0);
    }

    @Test(expectedExceptions = BufferUnderflowException.class)
    public void decodeWithBufferUnderflow() {
        EbmlDecoder decoder = new EbmlDecoder();
        ByteBuffer buffer = ByteBuffer.allocate(32);
        buffer.limit(7);
        int position = buffer.position();
        try {
            decoder.decodeFloatingPoint(buffer, 8);
        } finally {
            assertThat(buffer.position(), isEqualTo(position));
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void encodeWithNullBufferFails() {
        EbmlEncoder encoder = new EbmlEncoder();
        encoder.encodeFloatingPoint(null, 0.0, 8);
    }

    @Test(expectedExceptions = BufferOverflowException.class)
    public void encodeWithBufferOverflow() {
        EbmlEncoder encoder = new EbmlEncoder();
        ByteBuffer buffer = ByteBuffer.allocate(32);
        buffer.limit(7);
        int position = buffer.position();
        try {
            encoder.encodeFloatingPoint(buffer, 0.0, 8);
        } finally {
            assertThat(buffer.position(), isEqualTo(position));
        }
    }

    @Test(dataProvider = "doublePrecisionData")
    public void roundtripSinglePrecision(double value) {
        EbmlEncoder encoder = new EbmlEncoder();
        EbmlDecoder decoder = new EbmlDecoder();
        ByteBuffer buffer = ByteBuffer.allocate(32);
        float write = (float) value;
        encoder.encodeFloatingPoint(buffer, write, 4);
        buffer.flip();
        float read = (float) decoder.decodeFloatingPoint(buffer, 4);
        assertThat(read, isSameAs(write));
    }

    @Test(dataProvider = "doublePrecisionData")
    public void roundtripDoublePrecision(double value) {
        EbmlEncoder encoder = new EbmlEncoder();
        EbmlDecoder decoder = new EbmlDecoder();
        ByteBuffer buffer = ByteBuffer.allocate(32);
        encoder.encodeFloatingPoint(buffer, value, 8);
        buffer.flip();
        double read = decoder.decodeFloatingPoint(buffer, 8);
        assertThat(read, isSameAs(value));
    }

    @DataProvider(name = "doublePrecisionData")
    public Object[][] getDoublePrecisionData() {
        return new Object[][] { { 0.0 }, { -0.0 }, { 1.0 }, { -1.0 }, { Math.PI }, { Math.E }, { Double.MAX_VALUE }, { Double.MIN_NORMAL }, { Double.MIN_VALUE }, { Double.NaN }, { Double.NEGATIVE_INFINITY }, { Double.POSITIVE_INFINITY } };
    }
}
