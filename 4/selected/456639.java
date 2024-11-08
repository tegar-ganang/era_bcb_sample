package test.banking.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import test.banking.Bank;

public class BankingServer {

    Bank bank = Bank.getBank();

    public static void main(String[] args) throws IOException {
        BankingServer bankingServer = new BankingServer();
        bankingServer.start();
    }

    private boolean showtime = true;

    private void start() throws IOException {
        int portNumber = 80;
        ServerSocket serverSocket = new ServerSocket(portNumber);
        while (showtime) {
            Socket socket = serverSocket.accept();
            beginCustomerSession(socket);
        }
    }

    private void beginCustomerSession(Socket socket) throws IOException {
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        NetworkBankingSession session = new NetworkBankingSession(reader, writer);
        Thread sessionThread = new Thread(session);
        sessionThread.start();
    }
}
