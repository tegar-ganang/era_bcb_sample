package au.jSummit.Modules.FileHandler;

import au.jSummit.Core.*;
import au.jSummit.MainGUI.*;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.lang.IndexOutOfBoundsException;

public class FileHandlerModule extends JSModule {

    private static FileHandlerGUI guiWindow;

    private JSCore jscCore;

    public FileHandlerModule(JSCore jsc1) {
        super(jsc1);
        jscCore = jsc1;
        if (guiWindow == null) {
            guiWindow = new FileHandlerGUI(jscCore);
            guiWindow.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        }
    }

    public void run() {
    }

    public void updateSummitInfo(SummitInfo newSumInfo) {
    }

    public void showFrame() {
        if (guiWindow != null && !guiWindow.isVisible()) guiWindow.setVisible(true);
    }

    public void showFrame(Person person) {
        Vector tmp = new Vector();
        tmp.add(person);
        showFrame(tmp);
    }

    public void showFrame(Vector vPersons) {
        if (guiWindow != null && !guiWindow.isVisible()) guiWindow.setVisible(true);
        Vector transfers = guiWindow.getTransfers();
        int availableSends = 11 - transfers.size();
        if (availableSends < vPersons.size()) {
            JOptionPane.showMessageDialog(guiWindow, "Sorry, max sends have been reached");
            return;
        }
        int result;
        JFileChooser fileChooser;
        while (1 == 1) {
            fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File("."));
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            result = fileChooser.showOpenDialog(guiWindow);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = new File(fileChooser.getSelectedFile().getPath());
                if (file.exists() == true) {
                    break;
                } else {
                    JOptionPane.showMessageDialog(guiWindow, "Oops! That file doesnt exists!");
                }
            } else {
                return;
            }
        }
        if (result == JFileChooser.APPROVE_OPTION) {
            String selectedFile = fileChooser.getSelectedFile().getPath();
            for (int i = 0; i < vPersons.size(); i++) {
                Person person = (Person) vPersons.elementAt(i);
                initSend(selectedFile, person.getUsername(), jscCore.getMe().getUsername());
            }
        }
    }

    public void initSend(String filename, String receiver, String sender) {
        Vector files = guiWindow.getTransfers();
        int i = 24280;
        boolean foundPort = false;
        while (i <= 24291 && foundPort == false) {
            foundPort = true;
            for (int j = 0; j < files.size(); j++) {
                FileTransfer tmp = (FileTransfer) files.get(i);
                if (tmp.getPort() == i) {
                    foundPort = false;
                    break;
                }
            }
        }
        if (foundPort == false) {
            JOptionPane.showMessageDialog(guiWindow, "Sorry, max sends have been reached");
            return;
        }
        FileInitPacket packet = new FileInitPacket();
        try {
            packet.sender = sender;
            packet.filename = filename;
            packet.address = new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), i);
            ServerSocket sSocket = new ServerSocket(i);
            sSocket.setReuseAddress(true);
            addSend(sSocket, packet.filename, packet.sender);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(guiWindow, "Sorry, " + filename + " was unable to be sent to " + receiver);
            return;
        }
        JSPacket jsPacket = new JSPacket();
        jsPacket.moduleName = getModuleName();
        jsPacket.modulePacket = packet;
        Person destination = (Person) jscCore.getSummitInfo().getMembers().get(receiver);
        if (destination == null) {
            return;
        }
        jsPacket.destination = destination;
        jscCore.sendPacket(jsPacket);
    }

    public void receivePacket(JSPacket jsp) {
        JSPacket jsPacket = jsp;
        FileInitPacket packet;
        if (jsp.modulePacket instanceof FileInitPacket) {
            packet = (FileInitPacket) jsp.modulePacket;
            showFrame();
            File tmpFile = new File(packet.filename);
            String tmpFilename = tmpFile.getName();
            int selectedValue = JOptionPane.showConfirmDialog(guiWindow.getContentPane(), "Would you like to accept the file " + tmpFilename + " from " + packet.sender, "Incoming File", JOptionPane.YES_NO_OPTION);
            if (selectedValue != JOptionPane.YES_OPTION) return;
            JFileChooser fileSaver = new JFileChooser();
            fileSaver.setDialogTitle("Save " + tmpFilename + "to where?");
            fileSaver.setAcceptAllFileFilterUsed(false);
            fileSaver.setCurrentDirectory(new File("."));
            fileSaver.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            String selectedFile;
            int result = fileSaver.showSaveDialog(guiWindow);
            while (1 == 1) {
                if (result == JFileChooser.APPROVE_OPTION) {
                    selectedFile = new String(fileSaver.getSelectedFile().getPath());
                    File file = new File(selectedFile);
                    if (file.isDirectory() == false || file.exists() == false) {
                        JOptionPane.showMessageDialog(fileSaver, "Oops!, that location doesnt exists!");
                        result = fileSaver.showSaveDialog(guiWindow);
                    } else {
                        file = new File(selectedFile + tmpFilename);
                        if (file.exists() == true) {
                            selectedValue = JOptionPane.showConfirmDialog(guiWindow.getContentPane(), "File exists there already! Overwrite the file? No, select a different location, or cancel the file", "Overwrite File?", JOptionPane.YES_NO_CANCEL_OPTION);
                            if (selectedValue != JOptionPane.YES_OPTION) {
                                file.delete();
                                break;
                            }
                            if (selectedValue != JOptionPane.NO_OPTION) {
                                result = fileSaver.showSaveDialog(guiWindow);
                            } else if (selectedValue == JOptionPane.CANCEL_OPTION || selectedValue == JOptionPane.CLOSED_OPTION) {
                                return;
                            }
                        } else {
                            break;
                        }
                    }
                } else return;
            }
            addReceive(packet.address, new String(selectedFile + "\\" + tmpFilename), jsp.destination.getUsername(), packet.sender);
        }
    }

    public void addSend(ServerSocket sSocket, String filename, String receiver) {
        guiWindow.addSend(sSocket, filename, receiver);
    }

    public void addReceive(InetSocketAddress address, String filename, String receiver, String sender) {
        guiWindow.addReceive(address, filename, receiver, sender);
    }

    public static String getModuleName() {
        return new String("jsFileHandler");
    }
}
