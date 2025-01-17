package org.xsocket.mail.smtp;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.xsocket.DataConverter;
import org.xsocket.mail.smtp.spi.IReceiptJournal;
import org.xsocket.mail.smtp.spi.ISmtpMessageSink;
import org.xsocket.mail.smtp.spi.ISmtpMessageSinkService;
import org.xsocket.mail.smtp.spi.SmtpRejectException;

final class DummyMessageSinkService implements ISmtpMessageSinkService {

    enum MODE {

        DEV0OUT, CONSOLE_OUT, CONSOLE_DOT_OUT, FILE_OUT
    }

    private MODE mode = MODE.DEV0OUT;

    DummyMessageSinkService(MODE mode) {
        this.mode = mode;
    }

    public void isForwardPathAccepted(IReceiptJournal receiptJournal) throws SmtpRejectException {
        for (String forwardPath : receiptJournal.getSmtpForwardPaths()) {
            if (forwardPath.indexOf('@') == -1) {
                throw new SmtpRejectException(550, "error receiver " + forwardPath + " not accepted");
            }
        }
    }

    public void isReversePathAccepted(IReceiptJournal receiptJournal) throws SmtpRejectException {
        if (receiptJournal.getSmtpReversePath().indexOf('@') == -1) {
            throw new SmtpRejectException(550, "error sender " + receiptJournal.getSmtpReversePath() + " not accepted");
        }
    }

    public ISmtpMessageSink newMessageSink(IReceiptJournal receiptJournal) throws IOException, SmtpRejectException {
        switch(mode) {
            case CONSOLE_OUT:
                return new SpamFilterChannel(new ConsoleOutChannel());
            case CONSOLE_DOT_OUT:
                return new SpamFilterChannel(new ConsoleDotOutChannel());
            case FILE_OUT:
                return new SpamFilterChannel(new FileOutChannel());
            default:
                return new SpamFilterChannel(new Dev0OutChannel());
        }
    }

    private static final class FileOutChannel implements ISmtpMessageSink {

        private File file = null;

        private FileChannel fileChannel = null;

        public FileOutChannel() throws IOException {
            file = File.createTempFile("xsockettest", ".tempMail");
            fileChannel = new RandomAccessFile(file, "rw").getChannel();
        }

        public boolean isOpen() {
            return fileChannel.isOpen();
        }

        public void close() throws IOException {
            fileChannel.close();
            File mailFile = new File(file.getAbsolutePath().substring(0, (file.getAbsolutePath().length() - ".tempMail".length() - 1)) + ".mail");
            file.renameTo(mailFile);
        }

        public void delete() {
            if (fileChannel.isOpen()) {
                try {
                    fileChannel.close();
                } catch (Exception ignore) {
                }
            }
            file.delete();
        }

        public int write(ByteBuffer buffer) throws IOException, SmtpRejectException {
            return fileChannel.write(buffer);
        }
    }

    private static final class ConsoleOutChannel implements ISmtpMessageSink {

        private boolean isOpen = true;

        private StringBuilder sb = new StringBuilder();

        public boolean isOpen() {
            return isOpen;
        }

        public int write(ByteBuffer buffer) throws IOException {
            int length = buffer.remaining();
            sb.append(DataConverter.toString(buffer, "US-ASCII"));
            return length;
        }

        public void close() throws IOException {
            isOpen = false;
            System.out.println("\n\n[MSG START]\n" + sb.toString() + "\n[MSG END]");
        }

        public void delete() {
        }
    }

    private static final class ConsoleDotOutChannel implements ISmtpMessageSink {

        private boolean isOpen = true;

        private StringBuilder sb = new StringBuilder();

        public boolean isOpen() {
            return isOpen;
        }

        public int write(ByteBuffer buffer) throws IOException {
            int length = buffer.remaining();
            sb.append(DataConverter.toString(buffer, "US-ASCII"));
            return length;
        }

        public void close() throws IOException {
            isOpen = false;
            System.out.print(".");
        }

        public void delete() {
        }
    }

    private static final class Dev0OutChannel implements ISmtpMessageSink {

        private boolean isOpen = true;

        public boolean isOpen() {
            return isOpen;
        }

        public int write(ByteBuffer buffer) throws IOException {
            return buffer.remaining();
        }

        public void close() throws IOException {
            isOpen = false;
        }

        public void delete() {
        }
    }

    private static final class SpamFilterChannel implements ISmtpMessageSink {

        private ISmtpMessageSink delegee = null;

        private int pos = 0;

        private String token = "THIS IS SPAM";

        SpamFilterChannel(ISmtpMessageSink delegee) {
            this.delegee = delegee;
        }

        public boolean isOpen() {
            return delegee.isOpen();
        }

        public void close() throws IOException {
            delegee.close();
        }

        public void delete() {
            delegee.delete();
        }

        public int write(ByteBuffer buffer) throws IOException, SmtpRejectException {
            while (buffer.remaining() > 0) {
                char c = (char) buffer.get();
                if (c == token.charAt(pos)) {
                    pos++;
                    if (pos == token.length()) {
                        delegee.delete();
                        throw new SmtpRejectException(505, "message will no accpeted (spam detected)");
                    }
                } else {
                    pos = 0;
                }
            }
            buffer.flip();
            return delegee.write(buffer);
        }
    }
}
