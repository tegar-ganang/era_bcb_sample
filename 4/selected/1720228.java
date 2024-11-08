package net.masamic.free_forth_j.words;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import net.masamic.free_forth_j.Environment;
import net.masamic.free_forth_j.dictionary_elements.Word;

/**
 * @author masamic
 *
 */
public class Emit extends Word {

    protected static final long serialVersionUID = 1;

    protected static Word aInstance = new Emit();

    /**
	 * 
	 */
    protected Emit() {
    }

    public void doIt(BufferedReader in, BufferedWriter out) throws Exception {
        out.write((Integer) ((Environment) Thread.currentThread()).getDs().pop());
    }
}
