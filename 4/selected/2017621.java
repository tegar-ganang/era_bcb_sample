package com.ohua.clustering.checkpoint.online;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

public class CheckpointProtocol {

    public static final byte MASTER_INIT_CP_SIGNAL = 0x0070;

    public static final byte MASTER_RESUME_FROM_CP_SIGNAL = 0x0071;

    public static final byte SLAVE_RESUME_SUCESS = 0x0072;

    public static final byte SLAVE_RESUME_FAILURE = 0x0073;

    public static void sendCPSignal(Socket inputSocket, ByteBuffer byteBuffer) {
        try {
            inputSocket.getChannel().write(byteBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int readResumeSignal(ByteBuffer buffer) {
        return buffer.getInt();
    }

    public static void writeResumeSignal(ByteBuffer buffer, int resumeCheckpointID) {
        buffer.put(MASTER_RESUME_FROM_CP_SIGNAL);
        buffer.putInt(resumeCheckpointID);
    }
}
