package library.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import library.LibraryBaseClientLibrarian;
import library.proxies.ActualizeBookedItem;
import library.proxies.LibraryServerLibrarian;
import library.utils.BookedItemShort;
import library.utils.ReaderAccountInfo;
import library.utils.bean.UserSession;

public class LibrarySocketClientLibrarian extends LibraryBaseClientLibrarian {

    protected ExecutorService exec;

    public LibrarySocketClientLibrarian(Socket sendSocket, ObjectInputStream outInput, ObjectOutputStream outOutput) {
        server = new LibrarianSocketStub(sendSocket, outInput, outOutput);
        exec = Executors.newFixedThreadPool(1);
        exec.execute((Runnable) server);
    }

    public void setActualizable(ActualizeBookedItem actualizable) {
        ((LibrarianSocketStub) server).setActualizable(actualizable);
    }

    class LibrarianSocketStub implements LibraryServerLibrarian, Runnable {

        protected boolean isInitialized;

        protected ServerSocket serverSocket;

        protected Socket receiveSocket;

        protected Socket sendSocket;

        protected ActualizeBookedItem actualizable;

        protected ObjectInputStream inInput;

        protected ObjectOutputStream inOutput;

        protected ObjectInputStream outInput;

        protected ObjectOutputStream outOutput;

        public LibrarianSocketStub(Socket sendSocket, ObjectInputStream outInput, ObjectOutputStream outOutput) {
            this.sendSocket = sendSocket;
            this.outInput = outInput;
            this.outOutput = outOutput;
        }

        public void setActualizable(ActualizeBookedItem actualizable) {
            this.actualizable = actualizable;
        }

        @Override
        public void run() {
            int methodID;
            try {
                initialize();
                while (!Thread.interrupted()) {
                    methodID = inInput.readInt();
                    try {
                        handleMethodInvocation(methodID);
                    } catch (Exception e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    sendSocket.close();
                    receiveSocket.close();
                } catch (IOException e) {
                }
            }
        }

        protected synchronized void setInitialized() {
            isInitialized = true;
        }

        protected synchronized boolean isInitialized() {
            return isInitialized;
        }

        protected void testInitialized() {
            while (!isInitialized()) {
                Thread.yield();
            }
        }

        protected void initialize() throws IOException {
            serverSocket = new ServerSocket(0);
            outOutput.writeInt(serverSocket.getLocalPort());
            outOutput.flush();
            InetAddress sendaddr;
            InetAddress recvaddr;
            InputStream is;
            OutputStream os;
            Socket s = serverSocket.accept();
            sendaddr = sendSocket.getInetAddress();
            recvaddr = s.getInetAddress();
            if (sendaddr.equals(recvaddr)) {
                receiveSocket = s;
                is = receiveSocket.getInputStream();
                os = receiveSocket.getOutputStream();
                inOutput = new ObjectOutputStream(os);
                inInput = new ObjectInputStream(is);
                setInitialized();
            }
        }

        protected void handleMethodInvocation(int methodID) throws IOException, ClassNotFoundException {
            switch(methodID) {
                case LibrarySocketServer.INVOKE_ACTUALIZE_BOOKED_ITEMS:
                    handleActualizeBookedItems();
                    break;
            }
        }

        protected void handleActualizeBookedItems() throws IOException, ClassNotFoundException {
            int itemID = inInput.readInt();
            boolean actualized = actualizable.actualizeBookedItem(itemID);
            inOutput.writeBoolean(actualized);
            inOutput.flush();
        }

        @Override
        public Integer[] confirmLoan(Integer... itemIDs) throws IOException, ClassNotFoundException {
            testInitialized();
            outOutput.writeInt(LibrarySocketServer.INVOKE_CONFIRM_LOAN);
            outOutput.writeInt(itemIDs.length);
            for (int id : itemIDs) {
                outOutput.writeInt(id);
            }
            outOutput.flush();
            int length = outInput.readInt();
            Integer[] omitted = new Integer[length];
            for (int i = 0; i < length; i++) {
                omitted[i] = outInput.readInt();
            }
            return omitted;
        }

        @Override
        public BookedItemShort[] getBookedItems() throws IOException, ClassNotFoundException {
            testInitialized();
            outOutput.writeInt(LibrarySocketServer.INVOKE_GET_BOOKED_ITEMS);
            outOutput.flush();
            BookedItemShort[] booked = (BookedItemShort[]) outInput.readObject();
            return booked;
        }

        @Override
        public ReaderAccountInfo getReaderAccountInfo() throws IOException, ClassNotFoundException {
            testInitialized();
            outOutput.writeInt(LibrarySocketServer.INVOKE_GET_READER_ACCOUNT_INFO);
            outOutput.flush();
            ReaderAccountInfo info = (ReaderAccountInfo) outInput.readObject();
            return info;
        }

        @Override
        public boolean validateReader(int readerID) throws IOException {
            testInitialized();
            outOutput.writeInt(LibrarySocketServer.INVOKE_VALIDATE_READER);
            outOutput.writeInt(readerID);
            outOutput.flush();
            boolean valide = outInput.readBoolean();
            return valide;
        }

        @Override
        public void exitReaderAccount() throws IOException {
            testInitialized();
            outOutput.writeInt(LibrarySocketServer.INVOKE_EXIT_READER_ACCOUNT);
        }

        @Override
        public boolean logout() throws IOException {
            testInitialized();
            outOutput.writeInt(LibrarySocketServer.INVOKE_LOGOUT);
            outOutput.flush();
            boolean requestPerformed = outInput.readBoolean();
            return requestPerformed;
        }

        @Override
        public boolean storeSession(UserSession usession) throws Exception {
            testInitialized();
            outOutput.writeInt(LibrarySocketServer.INVOKE_STORE_SESSION);
            outOutput.writeObject(usession);
            outOutput.flush();
            boolean requestPerformed = outInput.readBoolean();
            return requestPerformed;
        }

        @Override
        public UserSession readSession() throws Exception {
            testInitialized();
            outOutput.writeInt(LibrarySocketServer.INVOKE_READ_SESSION);
            outOutput.flush();
            UserSession session = (UserSession) outInput.readObject();
            return session;
        }
    }
}
