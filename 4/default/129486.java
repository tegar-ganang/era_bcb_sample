import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Created by IntelliJ IDEA.
 * User: vince
 * Date: 5/14/11
 * Time: 4:17 PM
 */
public class MainWindow {

    private JButton connectButton;

    private JButton disconnectButton;

    public JPanel mainPanel;

    private JTextField userNameField;

    private JTextField nickservPass;

    private JTextField serverField;

    private JTextField channelField;

    private JTextArea outputFieldArea;

    private JButton addToQueueButton;

    private JButton viewStatusButton;

    private JButton editQueueButton;

    private JScrollPane outputPane;

    private JButton sendMessageButton;

    public MainWindow() {
        connectButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent actionEvent) {
                if (XDCCConnectionManager.bot != null && XDCCConnectionManager.bot.isConnected()) {
                    DialogBuilder.showErrorDialog("Error", "Already Connected");
                    return;
                }
                Thread connectThread = new Thread() {

                    public void run() {
                        connectToServer();
                    }
                };
                connectThread.start();
            }
        });
        disconnectButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent actionEvent) {
                if (XDCCConnectionManager.bot != null && XDCCConnectionManager.bot.isConnected()) {
                    XDCCConnectionManager.bot.disconnect();
                    XDCCConnectionManager.bot = null;
                } else {
                    DialogBuilder.showErrorDialog("Error", "Must be connected to server & channel");
                }
            }
        });
        addToQueueButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent actionEvent) {
                if (XDCCConnectionManager.bot != null && XDCCConnectionManager.bot.isConnected()) {
                    JFrame frame = new JFrame("XDCC Manager - Add To Queue");
                    frame.setContentPane(new AddToQueue(frame, XDCCConnectionManager.bot.getUsers(XDCCConnectionManager.bot.getChannels()[0])).mainPanel);
                    frame.pack();
                    frame.setVisible(true);
                } else {
                    DialogBuilder.showErrorDialog("Error", "Must be connected to server & channel");
                }
            }
        });
        viewStatusButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent actionEvent) {
                if (XDCCConnectionManager.bot != null && XDCCConnectionManager.bot.isConnected()) {
                    JFrame frame = new JFrame("XDCC Manager - Current Status");
                    frame.setContentPane(new StatusWindow(frame).mainPanel);
                    frame.pack();
                    frame.setVisible(true);
                } else {
                    DialogBuilder.showErrorDialog("Error", "Must be connected to server & channel");
                }
            }
        });
        editQueueButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent actionEvent) {
                if (XDCCConnectionManager.bot != null && XDCCConnectionManager.bot.isConnected()) {
                    JFrame frame = new JFrame("XDCC Manager - Edit Queue");
                    frame.setContentPane(new EditQueue(frame).mainPanel);
                    frame.pack();
                    frame.setVisible(true);
                } else {
                    DialogBuilder.showErrorDialog("Error", "Must be connected to server & channel");
                }
            }
        });
        sendMessageButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent actionEvent) {
                if (XDCCConnectionManager.bot != null && XDCCConnectionManager.bot.isConnected()) {
                    JFrame frame = new JFrame("XDCC Manager - Send Message");
                    frame.setContentPane(new SendMessage(frame).mainPanel);
                    frame.pack();
                    frame.setVisible(true);
                } else {
                    DialogBuilder.showErrorDialog("Error", "Must be connected to server & channel");
                }
            }
        });
    }

    private void connectToServer() {
        writeRecentFieldsToFile();
        String userName = userNameField.getText();
        String server = serverField.getText();
        final String channel = channelField.getText();
        if (userName == null || userName.isEmpty() || server == null || server.isEmpty() || channel == null || channel.isEmpty()) {
            DialogBuilder.showErrorDialog("Error", "Please fill in all required fields");
            return;
        }
        XDCCConnectionManager.bot = new XDCCManagerBot(userName, outputFieldArea, outputPane);
        XDCCConnectionManager.bot.setVerbose(true);
        try {
            XDCCConnectionManager.bot.connect(server);
        } catch (Exception e) {
            DialogBuilder.showErrorDialog("Error connecting to: ", e.getMessage());
        }
        if (nickservPass.getText() != null && !nickservPass.getText().isEmpty()) {
            XDCCConnectionManager.bot.identify(nickservPass.getText());
            int i = 0;
            while (!XDCCConnectionManager.bot.isRegistered() && i++ < 10) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if (channel.charAt(0) == '#') {
            XDCCConnectionManager.bot.joinChannel(channel);
        } else {
            XDCCConnectionManager.bot.joinChannel("#" + channel);
        }
    }

    private void writeRecentFieldsToFile() {
        String userName = userNameField.getText();
        String server = serverField.getText();
        String channel = channelField.getText();
        String nickPass = nickservPass.getText();
        try {
            FileWriter fstream = new FileWriter("recent");
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(userName + "\n");
            out.write(server + "\n");
            out.write(channel);
            if (nickPass != null && !nickPass.isEmpty()) {
                out.write("\n" + nickPass);
            }
            out.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void createUIComponents() {
        try {
            userNameField = new JTextField();
            nickservPass = new JPasswordField();
            serverField = new JTextField();
            channelField = new JTextField();
            readRecentFileToFields();
        } catch (FileNotFoundException e) {
        }
    }

    private void readRecentFileToFields() throws FileNotFoundException {
        List<String> fields = new ArrayList<String>();
        Scanner scanner = new Scanner(new FileInputStream("recent"));
        try {
            while (scanner.hasNextLine()) {
                fields.add(scanner.nextLine());
            }
        } finally {
            scanner.close();
        }
        userNameField.setText(fields.get(0));
        serverField.setText(fields.get(1));
        channelField.setText(fields.get(2));
        if (fields.size() > 3) {
            nickservPass.setText(fields.get(3));
        }
    }
}
