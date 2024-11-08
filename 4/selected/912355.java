package simpio;

import java.io.*;
import java.util.*;
import javax.swing.*;
import ds.Character;

public class CharParser {

    private String s_char_name;

    /**
	 * CharParser
	 * 
	 * Default constructor for the CharParser object.
	 */
    public CharParser() {
    }

    /**
	 * Single parameter constructor for CharParser
	 * objects.
	 * 
	 * @param s_init_name the name of the Character whose file to parse
	 */
    public CharParser(String s_init_name) {
        s_char_name = s_init_name;
    }

    /**
	 * Load a Character from a plaintext file.
	 * 
	 * @return the loaded Character
	 */
    public Character loadChar() {
        Character ch_loaded;
        try {
            Scanner file_scanner = new Scanner(new BufferedReader(new FileReader("sheets/" + s_char_name + ".gcs")));
            file_scanner.useDelimiter("\r\n");
            ch_loaded = new Character(file_scanner.next());
            ch_loaded.setSClass(file_scanner.next());
            ch_loaded.setSRace(file_scanner.next());
            ch_loaded.setILevel(Integer.parseInt(file_scanner.next()));
            ch_loaded.setIXP(Integer.parseInt(file_scanner.next()));
            ch_loaded.setIHP(Integer.parseInt(file_scanner.next()));
            ch_loaded.setIStr(Integer.parseInt(file_scanner.next()));
            ch_loaded.setIAgi(Integer.parseInt(file_scanner.next()));
            ch_loaded.setIInt(Integer.parseInt(file_scanner.next()));
            ch_loaded.setIFoc(Integer.parseInt(file_scanner.next()));
            ch_loaded.setICon(Integer.parseInt(file_scanner.next()));
            ch_loaded.setICha(Integer.parseInt(file_scanner.next()));
            ch_loaded.setICreds(Integer.parseInt(file_scanner.next()));
            ch_loaded.setSPlayer(file_scanner.next());
            file_scanner.close();
            return ch_loaded;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Character not found.");
            return null;
        }
    }

    /**
	 * Save a Character to a plaintext file.
	 * 
	 * @param ch_data the Character to save
	 */
    public void saveChar(Character ch_data) {
        String s_path = "";
        try {
            s_path = "sheets/" + s_char_name + ".gcs";
            File f_new = new File(s_path);
            if (f_new.createNewFile()) {
                save(ch_data, s_path);
            } else {
                int i_choice = JOptionPane.showConfirmDialog(null, "A character sheet for this character already exists." + "  Do you want to overwrite it?");
                if (i_choice == JOptionPane.CANCEL_OPTION || i_choice == JOptionPane.NO_OPTION) {
                    return;
                } else {
                    save(ch_data, s_path);
                }
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public Vector<String> getSaved() {
        Vector<String> sv_files = new Vector<String>();
        try {
            File f_sheets = new File("sheets/");
            File[] fa_sheets = f_sheets.listFiles();
            for (int i = 0; i < fa_sheets.length; i++) {
                if (fa_sheets[i].getName().length() > 4) {
                    sv_files.add(fa_sheets[i].getName().substring(0, fa_sheets[i].getName().length() - 4));
                }
            }
        } catch (Exception e) {
            System.err.println(e);
        }
        return sv_files;
    }

    public void delChar(String[] sa_chars) {
        String s_confirm_msg = "Are you sure you want to delete these sheets?  This action is irreversible.";
        int i_confirm = JOptionPane.showConfirmDialog(null, s_confirm_msg);
        if (i_confirm == JOptionPane.YES_OPTION) delete(sa_chars);
    }

    /**
	 * Actually write Character data to a file.
	 * 
	 * @param ch_data the Character to write to a file
	 * @param s_path the path to the save file
	 */
    private void save(Character ch_data, String s_path) {
        try {
            BufferedWriter bw_saver = new BufferedWriter(new FileWriter(s_path));
            bw_saver.write(ch_data.getSName() + "\r\n");
            bw_saver.write(ch_data.getSClass() + "\r\n");
            bw_saver.write(ch_data.getSRace() + "\r\n");
            bw_saver.write(Integer.toString(ch_data.getILevel()) + "\r\n");
            bw_saver.write(Integer.toString(ch_data.getIXP()) + "\r\n");
            bw_saver.write(Integer.toString(ch_data.getIHP()) + "\r\n");
            bw_saver.write(Integer.toString(ch_data.getIStr()) + "\r\n");
            bw_saver.write(Integer.toString(ch_data.getIAgi()) + "\r\n");
            bw_saver.write(Integer.toString(ch_data.getIInt()) + "\r\n");
            bw_saver.write(Integer.toString(ch_data.getIFoc()) + "\r\n");
            bw_saver.write(Integer.toString(ch_data.getICon()) + "\r\n");
            bw_saver.write(Integer.toString(ch_data.getICha()) + "\r\n");
            bw_saver.write(Integer.toString(ch_data.getICreds()) + "\r\n");
            bw_saver.write(ch_data.getSPlayer() + "\r\n");
            JOptionPane.showMessageDialog(null, "Character saved.");
            bw_saver.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
	 * Actually delete a saved character.
	 * 
	 * @param sa_del an array of characters to delete
	 */
    private void delete(String[] sa_del) {
        File f_del;
        for (int i = 0; i < sa_del.length; i++) {
            f_del = new File("sheets/" + sa_del[i] + ".gcs");
            f_del.delete();
        }
    }
}
