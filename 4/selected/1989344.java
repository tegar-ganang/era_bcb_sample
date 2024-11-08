package com.aelitis.azureus.core.networkmanager.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

public class TransportHelperFilterStreamCipher extends TransportHelperFilterStream {

    private TransportCipher read_cipher;

    private TransportCipher write_cipher;

    public TransportHelperFilterStreamCipher(TransportHelper _transport, TransportCipher _read_cipher, TransportCipher _write_cipher) {
        super(_transport);
        read_cipher = _read_cipher;
        write_cipher = _write_cipher;
    }

    protected void cryptoOut(ByteBuffer source_buffer, ByteBuffer target_buffer) throws IOException {
        write_cipher.update(source_buffer, target_buffer);
    }

    protected void cryptoIn(ByteBuffer source_buffer, ByteBuffer target_buffer) throws IOException {
        read_cipher.update(source_buffer, target_buffer);
    }

    public boolean isEncrypted() {
        return (true);
    }

    public String getName(boolean verbose) {
        String proto_str = getHelper().getName(verbose);
        if (proto_str.length() > 0) {
            proto_str = " (" + proto_str + ")";
        }
        return (read_cipher.getName() + proto_str);
    }
}
