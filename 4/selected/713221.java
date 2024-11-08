package com.sun.star.comp.connections;

import complexlib.ComplexTestCase;

public final class PipedConnection_Test extends ComplexTestCase {

    public String getTestObjectName() {
        return getClass().getName();
    }

    public String[] getTestMethodNames() {
        return new String[] { "test" };
    }

    public void test() throws Exception {
        PipedConnection rightSide = new PipedConnection(new Object[0]);
        PipedConnection leftSide = new PipedConnection(new Object[] { rightSide });
        byte theByte[] = new byte[1];
        Reader reader = new Reader(rightSide, theByte);
        Writer writer = new Writer(leftSide, theByte, reader);
        reader.start();
        writer.start();
        Thread.sleep(2000);
        writer.term();
        writer.join();
        reader.join();
        assure("", writer._state && reader._state);
    }

    static class Reader extends Thread {

        PipedConnection _pipedConnection;

        byte _theByte[];

        boolean _quit;

        boolean _state = false;

        Reader(PipedConnection pipedConnection, byte theByte[]) {
            _pipedConnection = pipedConnection;
            _theByte = theByte;
        }

        public void run() {
            try {
                byte bytes[][] = new byte[1][];
                while (!_quit) {
                    int read = _pipedConnection.read(bytes, 1);
                    if (read == 1) {
                        if (_theByte[0] != bytes[0][0]) throw new NullPointerException();
                        synchronized (this) {
                            notifyAll();
                        }
                    } else _quit = true;
                }
                _pipedConnection.close();
                _state = true;
            } catch (com.sun.star.io.IOException ioException) {
                System.err.println("#### Reader - unexpected:" + ioException);
            }
        }
    }

    static class Writer extends Thread {

        PipedConnection _pipedConnection;

        byte _theByte[];

        Reader _reader;

        boolean _quit;

        boolean _state = false;

        Writer(PipedConnection pipedConnection, byte theByte[], Reader reader) {
            _pipedConnection = pipedConnection;
            _reader = reader;
            _theByte = theByte;
        }

        public void run() {
            try {
                while (!_quit) {
                    synchronized (_reader) {
                        _pipedConnection.write(_theByte);
                        _pipedConnection.flush();
                        _reader.wait();
                    }
                    ++_theByte[0];
                }
                _pipedConnection.close();
                _state = true;
            } catch (com.sun.star.io.IOException ioException) {
                System.err.println("#### Writer:" + ioException);
            } catch (InterruptedException interruptedException) {
                System.err.println("#### Writer:" + interruptedException);
            }
        }

        public void term() {
            _quit = true;
        }
    }
}
