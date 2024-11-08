package com.google.code.ebmlviewer.core;

import java.nio.ByteBuffer;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static oe.assertions.Assertions.assertThat;
import static oe.assertions.Predicates.contains;
import static oe.assertions.Predicates.isEqualTo;
import static oe.assertions.Predicates.isFalse;
import static oe.assertions.Predicates.isTrue;
import static oe.assertions.Predicates.startsWith;

public class VariableLengthIntegerTest {

    @Test(dataProvider = "data")
    public void fromPlain(long plainValue, int minimumEncodedValueLength, long[] encodedValues) {
        long encodedValue = encodedValues[0];
        VariableLengthInteger vli = VariableLengthInteger.fromPlain(plainValue);
        assertThat(vli.getPlainValue(), isEqualTo(plainValue));
        assertThat(vli.getEncodedValue(), isEqualTo(encodedValue));
        assertThat(vli.getEncodedLength(), isEqualTo(minimumEncodedValueLength));
    }

    @Test(dataProvider = "data")
    public void fromPlainWithLength(long plainValue, int minimumEncodedValueLength, long[] encodedValues) {
        for (int i = minimumEncodedValueLength; i <= 8; i++) {
            long encodedValue = encodedValues[i - minimumEncodedValueLength];
            VariableLengthInteger vli = VariableLengthInteger.fromPlain(plainValue, i);
            assertThat(vli.getPlainValue(), isEqualTo(plainValue));
            assertThat(vli.getEncodedValue(), isEqualTo(encodedValue));
            assertThat(vli.getEncodedLength(), isEqualTo(i));
            assertThat(vli.isIdentifier(), isEqualTo(i == minimumEncodedValueLength));
            assertThat(vli.isReserved(), isFalse());
            assertThat(vli.toString().length(), isEqualTo(2 + i * 2));
            assertThat(vli.toString(), startsWith("0x"), contains(Long.toHexString(encodedValue).toUpperCase()));
        }
    }

    @Test(dataProvider = "data", expectedExceptions = EbmlFormatException.class)
    public void fromPlainWithWrongLengthFails(long plainValue, int minimumEncodedValueLength, long[] encodedValues) {
        VariableLengthInteger vli = VariableLengthInteger.fromPlain(plainValue, minimumEncodedValueLength - 1);
    }

    @Test(dataProvider = "data")
    public void fromEncoded(long plainValue, int minimumEncodedValueLength, long[] encodedValues) {
        long encodedValue = encodedValues[0];
        VariableLengthInteger vli = VariableLengthInteger.fromEncoded(encodedValue);
        assertThat(vli.getPlainValue(), isEqualTo(plainValue));
        assertThat(vli.getEncodedValue(), isEqualTo(encodedValue));
        assertThat(vli.getEncodedLength(), isEqualTo(minimumEncodedValueLength));
    }

    @Test(dataProvider = "data")
    public void fromEncodedWithLength(long plainValue, int minimumEncodedValueLength, long[] encodedValues) {
        for (int i = minimumEncodedValueLength; i <= 8; i++) {
            long encodedValue = encodedValues[i - minimumEncodedValueLength];
            VariableLengthInteger vli = VariableLengthInteger.fromEncoded(encodedValue, i);
            assertThat(vli.getPlainValue(), isEqualTo(plainValue));
            assertThat(vli.getEncodedValue(), isEqualTo(encodedValue));
            assertThat(vli.getEncodedLength(), isEqualTo(i));
            assertThat(vli.isIdentifier(), isEqualTo(i == minimumEncodedValueLength));
            assertThat(vli.isReserved(), isFalse());
            assertThat(vli.toString().length(), isEqualTo(2 + i * 2));
            assertThat(vli.toString(), startsWith("0x"), contains(Long.toHexString(encodedValue).toUpperCase()));
        }
    }

    @Test(dataProvider = "data", expectedExceptions = EbmlFormatException.class)
    public void fromEncodedWithWrongLengthFails(long plainValue, int minimumEncodedValueLength, long[] encodedValues) {
        VariableLengthInteger vli = VariableLengthInteger.fromEncoded(encodedValues[0], minimumEncodedValueLength - 1);
    }

    @Test(dataProvider = "reserved")
    public void isReserved(int encodedValueLength, long encodedValue) {
        VariableLengthInteger vli = VariableLengthInteger.fromEncoded(encodedValue);
        assertThat(vli.getEncodedValue(), isEqualTo(encodedValue));
        assertThat(vli.getEncodedLength(), isEqualTo(encodedValueLength));
        assertThat(vli.isIdentifier(), isFalse());
        assertThat(vli.isReserved(), isTrue());
    }

    @Test(dataProvider = "data")
    public void decodeRoundtrip(long plainValue, int minimumEncodedValueLength, long[] encodedValues) {
        for (int i = minimumEncodedValueLength; i <= 8; i++) {
            long encodedValue = encodedValues[i - minimumEncodedValueLength];
            VariableLengthInteger vli = VariableLengthInteger.fromEncoded(encodedValue, i);
            String s = vli.toString();
            VariableLengthInteger decoded = VariableLengthInteger.fromString(s);
            assertThat(decoded, isEqualTo(vli));
        }
    }

    @Test(dataProvider = "data")
    public void roundtrip(long plainValue, int minimumEncodedValueLength, long[] encodedValues) {
        EbmlEncoder encoder = new EbmlEncoder();
        EbmlDecoder decoder = new EbmlDecoder();
        ByteBuffer buffer = ByteBuffer.allocate(32);
        for (int i = minimumEncodedValueLength; i <= 8; i++) {
            buffer.clear();
            long encodedValue = encodedValues[i - minimumEncodedValueLength];
            VariableLengthInteger write = VariableLengthInteger.fromEncoded(encodedValue, i);
            encoder.encodeVariableLengthInteger(buffer, write);
            buffer.flip();
            VariableLengthInteger read = decoder.decodeVariableLengthInteger(buffer);
            assertThat(read, isEqualTo(write));
        }
    }

    @DataProvider(name = "data")
    public Object[][] getData() {
        return new Object[][] { { 0x0000000000000000L, 1, new long[] { 0x80L, 0x4000L, 0x200000L, 0x10000000L, 0x0800000000L, 0x040000000000L, 0x02000000000000L, 0x0100000000000000L } }, { 0x0000000000000001L, 1, new long[] { 0x81L, 0x4001L, 0x200001L, 0x10000001L, 0x0800000001L, 0x040000000001L, 0x02000000000001L, 0x0100000000000001L } }, { 0x000000000000007eL, 1, new long[] { 0xfeL, 0x407eL, 0x20007eL, 0x1000007eL, 0x080000007eL, 0x04000000007eL, 0x0200000000007eL, 0x010000000000007eL } }, { 0x000000000000007fL, 2, new long[] { 0x407fL, 0x20007fL, 0x1000007fL, 0x080000007fL, 0x04000000007fL, 0x0200000000007fL, 0x010000000000007fL } }, { 0x0000000000000080L, 2, new long[] { 0x4080L, 0x200080L, 0x10000080L, 0x0800000080L, 0x040000000080L, 0x02000000000080L, 0x0100000000000080L } }, { 0x0000000000003ffeL, 2, new long[] { 0x7ffeL, 0x203ffeL, 0x10003ffeL, 0x0800003ffeL, 0x040000003ffeL, 0x02000000003ffeL, 0x0100000000003ffeL } }, { 0x0000000000003fffL, 3, new long[] { 0x203fffL, 0x10003fffL, 0x0800003fffL, 0x040000003fffL, 0x02000000003fffL, 0x0100000000003fffL } }, { 0x0000000000004000L, 3, new long[] { 0x204000L, 0x10004000L, 0x0800004000L, 0x040000004000L, 0x02000000004000L, 0x0100000000004000L } }, { 0x00000000001ffffeL, 3, new long[] { 0x3ffffeL, 0x101ffffeL, 0x08001ffffeL, 0x0400001ffffeL, 0x020000001ffffeL, 0x01000000001ffffeL } }, { 0x00000000001fffffL, 4, new long[] { 0x101fffffL, 0x08001fffffL, 0x0400001fffffL, 0x020000001fffffL, 0x01000000001fffffL } }, { 0x0000000000200000L, 4, new long[] { 0x10200000L, 0x0800200000L, 0x040000200000L, 0x02000000200000L, 0x0100000000200000L } }, { 0x000000000ffffffeL, 4, new long[] { 0x1ffffffeL, 0x080ffffffeL, 0x04000ffffffeL, 0x0200000ffffffeL, 0x010000000ffffffeL } }, { 0x000000000fffffffL, 5, new long[] { 0x080fffffffL, 0x04000fffffffL, 0x0200000fffffffL, 0x010000000fffffffL } }, { 0x0000000010000000L, 5, new long[] { 0x0810000000L, 0x040010000000L, 0x02000010000000L, 0x0100000010000000L } }, { 0x00000007fffffffeL, 5, new long[] { 0x0ffffffffeL, 0x0407fffffffeL, 0x020007fffffffeL, 0x01000007fffffffeL } }, { 0x00000007ffffffffL, 6, new long[] { 0x0407ffffffffL, 0x020007ffffffffL, 0x01000007ffffffffL } }, { 0x0000000800000000L, 6, new long[] { 0x040800000000L, 0x02000800000000L, 0x0100000800000000L } }, { 0x000003fffffffffeL, 6, new long[] { 0x07fffffffffeL, 0x0203fffffffffeL, 0x010003fffffffffeL } }, { 0x000003ffffffffffL, 7, new long[] { 0x0203ffffffffffL, 0x010003ffffffffffL } }, { 0x0000040000000000L, 7, new long[] { 0x02040000000000L, 0x0100040000000000L } }, { 0x0001fffffffffffeL, 7, new long[] { 0x03fffffffffffeL, 0x0101fffffffffffeL } }, { 0x0001ffffffffffffL, 8, new long[] { 0x0101ffffffffffffL } }, { 0x0002000000000000L, 8, new long[] { 0x0102000000000000L } }, { 0x00fffffffffffffeL, 8, new long[] { 0x01fffffffffffffeL } } };
    }

    @DataProvider(name = "reserved")
    public Object[][] getReserved() {
        return new Object[][] { { 1, 0xffL }, { 2, 0x7fffL }, { 3, 0x3fffffL }, { 4, 0x1fffffffL }, { 5, 0x0fffffffffL }, { 6, 0x07ffffffffffL }, { 7, 0x03ffffffffffffL }, { 8, 0x01ffffffffffffffL } };
    }
}
