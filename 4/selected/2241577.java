package jdos.hardware;

import jdos.misc.Log;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;

public class IoHandler {

    public static final int IO_MAX = (64 * 1024 + 3);

    public static final int IO_MB = 0x1;

    public static final int IO_MW = 0x2;

    public static final int IO_MD = 0x4;

    public static final int IO_MA = (IO_MB | IO_MW | IO_MD);

    public static interface IO_ReadHandler {

        public int call(int port, int iolen);
    }

    public static interface IO_WriteHandler {

        public void call(int port, int val, int iolen);
    }

    private static class IO_Base {

        protected boolean installed = false;

        protected int m_port, m_mask, m_range;
    }

    public static class IO_ReadHandleObject extends IO_Base {

        public void Install(int port, IO_ReadHandler handler, int mask) {
            Install(port, handler, mask, 1);
        }

        public void Install(int port, IO_ReadHandler handler, int mask, int range) {
            if (!installed) {
                installed = true;
                m_port = port;
                m_mask = mask;
                m_range = range;
                IO_RegisterReadHandler(port, handler, mask, range);
            } else Log.exit("IO_readHandler allready installed port " + Integer.toString(port, 16));
        }

        public void destroy() {
            if (!installed) return;
            IO_FreeReadHandler(m_port, m_mask, m_range);
        }
    }

    public static class IO_WriteHandleObject extends IO_Base {

        public void Install(int port, IO_WriteHandler handler, int mask) {
            Install(port, handler, mask, 1);
        }

        public void Install(int port, IO_WriteHandler handler, int mask, int range) {
            if (!installed) {
                installed = true;
                m_port = port;
                m_mask = mask;
                m_range = range;
                IO_RegisterWriteHandler(port, handler, mask, range);
            } else Log.exit("IO_writeHandler allready installed port " + Integer.toString(port, 16));
        }

        public void destroy() {
            if (!installed) return;
            IO_FreeWriteHandler(m_port, m_mask, m_range);
        }
    }

    public static void IO_Write(int port, int val) {
        IO.IO_WriteB(port, val & 0xFF);
    }

    public static short IO_Read(int port) {
        return (short) (IO.IO_ReadB(port) & 0xFF);
    }

    public static IO_WriteHandler[][] io_writehandlers = new IO_WriteHandler[3][IO_MAX];

    public static IO_ReadHandler[][] io_readhandlers = new IO_ReadHandler[3][IO_MAX];

    private static IO_ReadHandler IO_ReadBlocked = new IO_ReadHandler() {

        public int call(int port, int iolen) {
            return ~0;
        }
    };

    private static IO_WriteHandler IO_WriteBlocked = new IO_WriteHandler() {

        public void call(int port, int val, int iolen) {
        }
    };

    private static IO_ReadHandler IO_ReadDefault = new IO_ReadHandler() {

        public int call(int port, int iolen) {
            switch(iolen) {
                case 1:
                    if (Log.level <= LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_IO, LogSeverities.LOG_WARN, "Read from port " + Integer.toString(port, 16));
                    io_readhandlers[0][port] = IO_ReadBlocked;
                    return 0xff;
                case 2:
                    return (io_readhandlers[0][port].call(port, 1)) | (io_readhandlers[0][port + 1].call(port + 1, 1) << 8);
                case 4:
                    return (io_readhandlers[1][port].call(port, 2)) | (io_readhandlers[1][port + 2].call(port + 2, 2) << 16);
            }
            return 0;
        }
    };

    private static IO_WriteHandler IO_WriteDefault = new IO_WriteHandler() {

        public void call(int port, int val, int iolen) {
            switch(iolen) {
                case 1:
                    if (Log.level <= LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_IO, LogSeverities.LOG_WARN, "Writing " + Integer.toString(val, 16) + " to port " + Integer.toString(port, 16));
                    io_writehandlers[0][port] = IO_WriteBlocked;
                    break;
                case 2:
                    io_writehandlers[0][port].call(port, (val) & 0xff, 1);
                    io_writehandlers[0][port + 1].call(port + 1, (val >> 8) & 0xff, 1);
                    break;
                case 4:
                    io_writehandlers[1][port].call(port, (val) & 0xffff, 2);
                    io_writehandlers[1][port + 2].call(port + 2, (val >> 16) & 0xffff, 2);
                    break;
            }
        }
    };

    public static void IO_RegisterReadHandler(int port, IO_ReadHandler handler, int mask) {
        IO_RegisterReadHandler(port, handler, mask, 1);
    }

    public static void IO_RegisterReadHandler(int port, IO_ReadHandler handler, int mask, int range) {
        while (range-- != 0) {
            if ((mask & IO_MB) != 0) io_readhandlers[0][port] = handler;
            if ((mask & IO_MW) != 0) io_readhandlers[1][port] = handler;
            if ((mask & IO_MD) != 0) io_readhandlers[2][port] = handler;
            port++;
        }
    }

    public static void IO_RegisterWriteHandler(int port, IO_WriteHandler handler, int mask) {
        IO_RegisterWriteHandler(port, handler, mask, 1);
    }

    public static void IO_RegisterWriteHandler(int port, IO_WriteHandler handler, int mask, int range) {
        while (range-- != 0) {
            if ((mask & IO_MB) != 0) io_writehandlers[0][port] = handler;
            if ((mask & IO_MW) != 0) io_writehandlers[1][port] = handler;
            if ((mask & IO_MD) != 0) io_writehandlers[2][port] = handler;
            port++;
        }
    }

    public static void IO_FreeReadHandler(int port, int mask) {
        IO_FreeReadHandler(port, mask, 1);
    }

    public static void IO_FreeReadHandler(int port, int mask, int range) {
        while (range-- != 0) {
            if ((mask & IO_MB) != 0) io_readhandlers[0][port] = IO_ReadDefault;
            if ((mask & IO_MW) != 0) io_readhandlers[1][port] = IO_ReadDefault;
            if ((mask & IO_MD) != 0) io_readhandlers[2][port] = IO_ReadDefault;
            port++;
        }
    }

    public static void IO_FreeWriteHandler(int port, int mask) {
        IO_FreeWriteHandler(port, mask, 1);
    }

    public static void IO_FreeWriteHandler(int port, int mask, int range) {
        while (range-- != 0) {
            if ((mask & IO_MB) != 0) io_writehandlers[0][port] = IO_WriteDefault;
            if ((mask & IO_MW) != 0) io_writehandlers[1][port] = IO_WriteDefault;
            if ((mask & IO_MD) != 0) io_writehandlers[2][port] = IO_WriteDefault;
            port++;
        }
    }
}
