package org.jcuefile.cuefiles.MP3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import org.jcuefile.GUI.GUI;
import org.jcuefile.cuefiles.CueFile;

public final class SingleMP3 extends CueFile {

    private File saveFile;

    private File loadFile;

    private String artist;

    private String title;

    private String file;

    @Override
    public final void newCueFile() {
        GUI.getFileField().setText("");
        GUI.getArtistField().setText("");
        GUI.getTitleField().setText("");
        System.out.println(((DefaultTableModel) GUI.getTable()).getRowCount());
        GUI.getRowData().clear();
        GUI.getTable().fireTableDataChanged();
        Vector<Object> vec = new Vector<Object>();
        vec.add("01");
        vec.add("");
        vec.add("");
        vec.add("");
        GUI.getRowData().add(vec);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void saveCueFile() {
        String newLine = System.getProperty("line.separator");
        if (GUI.getArtistField().getText().equals("")) {
            JOptionPane.showMessageDialog(GUI.getContent(), "You must fill in an Artist!", "Error!", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (GUI.getTitleField().getText().equals("")) {
            JOptionPane.showMessageDialog(GUI.getContent(), "You must fill in a Title!", "Error!", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (GUI.getFileField().getText().equals("")) {
            JOptionPane.showMessageDialog(GUI.getContent(), "You must choose your file!", "Error!", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (GUI.getRowData().size() < 1) {
            JOptionPane.showMessageDialog(GUI.getContent(), "You must fill in at least 1 track!", "Error!", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String cueFile;
        String track = "";
        String completeTrack = "";
        cueFile = "FILE \"" + GUI.getFileField().getText() + "\" MP3" + newLine;
        cueFile += "PERFORMER \"" + GUI.getArtistField().getText() + "\"" + newLine;
        cueFile += "TITLE \"" + GUI.getTitleField().getText() + "\"" + newLine;
        for (int i = 0; i < GUI.getRowData().size(); i++) {
            for (int j = 0; j < 4; j++) {
                switch(j) {
                    case 0:
                        if (!isInteger(((Vector<String>) GUI.getRowData().get(i)).get(0))) {
                            JOptionPane.showMessageDialog(GUI.getContent(), "Track {Nr} must be a number!", "Error!", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        track = "  TRACK " + ((Vector<String>) GUI.getRowData().get(i)).get(0) + " AUDIO" + newLine;
                        break;
                    case 1:
                        if (((Vector<String>) GUI.getRowData().get(i)).get(1).equals("")) {
                            JOptionPane.showMessageDialog(GUI.getContent(), "Empty Artist!", "Error!", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        track = "    PERFORMER \"" + ((Vector<String>) GUI.getRowData().get(i)).get(1) + "\"" + newLine;
                        break;
                    case 2:
                        if (((Vector<String>) GUI.getRowData().get(i)).get(2).equals("")) {
                            JOptionPane.showMessageDialog(GUI.getContent(), "Empty Title!", "Error!", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        track = "    TITLE \"" + ((Vector<String>) GUI.getRowData().get(i)).get(2) + "\"" + newLine;
                        break;
                    case 3:
                        if (!checkIndex(((Vector<String>) GUI.getRowData().get(i)).get(3))) {
                            JOptionPane.showMessageDialog(GUI.getContent(), "Incorrect Index!", "Error!", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        track = "    INDEX 01 " + ((Vector<String>) GUI.getRowData().get(i)).get(3) + "" + newLine;
                        break;
                }
                completeTrack = completeTrack + track;
            }
            cueFile += completeTrack;
            completeTrack = "";
        }
        JFileChooser fileSave = new JFileChooser();
        FileFilter filter = new FileNameExtensionFilter("Cue Files (*.cue)", "cue");
        fileSave.setAcceptAllFileFilterUsed(false);
        fileSave.addChoosableFileFilter(filter);
        int ret = fileSave.showSaveDialog(GUI.getContent());
        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = fileSave.getSelectedFile();
            if (!checkFileExtension(file.getName(), "cue")) {
                this.saveFile = new File(file.getAbsoluteFile() + ".cue");
            } else {
                this.saveFile = file.getAbsoluteFile();
            }
        } else {
            return;
        }
        if (this.saveFile.exists() || this.saveFile.equals("")) {
            int dialog = JOptionPane.showConfirmDialog(GUI.getContent(), "The file you selected allready exists!" + newLine + "Are you sure you want to overwrite it?", "File Exists!", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (dialog == JOptionPane.NO_OPTION) {
                int fc = fileSave.showSaveDialog(GUI.getContent());
                if (fc == JFileChooser.CANCEL_OPTION) {
                    return;
                } else {
                    File file = fileSave.getSelectedFile();
                    if (!checkFileExtension(file.getName(), "cue")) {
                        this.saveFile = new File(file.getAbsoluteFile() + ".cue");
                    } else {
                        this.saveFile = file.getAbsoluteFile();
                    }
                }
            }
        }
        try {
            FileWriter fs = new FileWriter(this.saveFile);
            BufferedWriter out = new BufferedWriter(fs);
            out.write(cueFile);
            out.close();
            JOptionPane.showMessageDialog(GUI.getContent(), "Succesfully saved the file!", "Succes!", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void loadCueFile() {
        JFileChooser fileOpen = new JFileChooser();
        FileFilter filter = new FileNameExtensionFilter("Cue Files (*.cue)", "cue");
        fileOpen.setAcceptAllFileFilterUsed(false);
        fileOpen.addChoosableFileFilter(filter);
        int ret = fileOpen.showOpenDialog(GUI.getContent());
        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = fileOpen.getSelectedFile();
            if (!checkFileExtension(file.getName(), "cue")) {
                this.loadFile = new File(file.getAbsoluteFile() + ".cue");
            } else {
                this.loadFile = file.getAbsoluteFile();
            }
        } else {
            return;
        }
        try {
            String text = null;
            FileReader fis = new FileReader(this.loadFile);
            BufferedReader in = new BufferedReader(fis);
            this.artist = "";
            this.file = "";
            this.title = "";
            Vector<String> id = new Vector<String>();
            Vector<String> artist = new Vector<String>();
            Vector<String> title = new Vector<String>();
            Vector<String> index = new Vector<String>();
            int lineNumber = 0;
            while ((text = in.readLine()) != null) {
                if (text.contains("FILE")) {
                    lineNumber++;
                    String[] tokens = text.split("\"");
                    if (this.file.equals("")) {
                        this.file = tokens[1];
                    } else {
                        JOptionPane.showMessageDialog(GUI.getContent(), "BAD CUE FILE!", "ERROR!", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                if (text.contains("PERFORMER")) {
                    lineNumber++;
                    String[] tokens = text.split("\"");
                    if (this.artist.equals("")) {
                        this.artist = tokens[1];
                    }
                }
                if (text.contains("TITLE")) {
                    lineNumber++;
                    String[] tokens = text.split("\"");
                    if (this.title.equals("")) {
                        this.title = tokens[1];
                    }
                }
                if (text.contains("TRACK")) {
                    lineNumber++;
                    String[] tokens = text.split(" ");
                    id.add(tokens[3]);
                }
                if (text.contains("PERFORMER") && lineNumber > 3) {
                    lineNumber++;
                    String[] tokens = text.split("\"");
                    artist.add(tokens[1]);
                }
                if (text.contains("TITLE") && lineNumber > 3) {
                    lineNumber++;
                    String[] tokens = text.split("\"");
                    title.add(tokens[1]);
                }
                if (text.contains("INDEX")) {
                    lineNumber++;
                    String[] tokens = text.split(" ");
                    index.add(tokens[6]);
                }
            }
            in.close();
            GUI.getArtistField().setText(this.artist);
            GUI.getTitleField().setText(this.title);
            GUI.getFileField().setText(this.file);
            for (int i = 0; i < id.size(); i++) {
                if (i == 0) {
                    ((Vector<String>) GUI.getRowData().get(0)).set(1, artist.get(0));
                    ((Vector<String>) GUI.getRowData().get(0)).set(2, title.get(0));
                    ((Vector<String>) GUI.getRowData().get(0)).set(3, index.get(0));
                } else {
                    Vector<String> vec = new Vector<String>();
                    vec.add(id.get(i));
                    vec.add(artist.get(i));
                    vec.add(title.get(i));
                    vec.add(index.get(i));
                    GUI.getRowData().add(vec);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final boolean isInteger(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private final boolean checkIndex(String index) {
        String[] tokens = index.split(":");
        try {
            if (tokens.length == 0) {
                return false;
            }
            if (isInteger(tokens[0]) && isInteger(tokens[1]) && isInteger(tokens[2])) {
                if (tokens.length > 3) {
                    if (tokens[3].equals("") || !isInteger(tokens[3])) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    private final boolean checkFileExtension(String fileName, String extension) {
        String[] tokens = fileName.split("\\.");
        int count = tokens.length - 1;
        try {
            if (tokens.length == 0) {
                return false;
            }
            if (tokens[count].equals(extension)) {
                return true;
            }
            return false;
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }
}
