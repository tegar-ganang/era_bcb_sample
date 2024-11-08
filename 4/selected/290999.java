package com.myjavalab.thinkinginjava4;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import com.myjavalab.util.PrintUtil;

public class CH18_TransferTo {

    private static final int BSIZE = 1024;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            PrintUtil.prt("arguments: sourcefile, destfile");
            System.exit(1);
        }
        FileChannel in = new FileInputStream(args[0]).getChannel(), out = new FileOutputStream(args[1]).getChannel();
        ByteBuffer buff = ByteBuffer.allocate(BSIZE);
        while (in.read(buff) != -1) {
            PrintUtil.prt("%%%");
            buff.flip();
            out.write(buff);
            buff.clear();
        }
    }
}
