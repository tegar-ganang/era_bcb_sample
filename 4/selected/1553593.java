package fr.insee.rome.io.genre;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import fr.insee.rome.factory.RomeFactory;
import fr.insee.rome.string.comp.StringComparator;

public class ControlGenreGenerator implements GenreGenerator {

    private static GenreGenerator instance = null;

    private StringComparator comparator = null;

    public ControlGenreGenerator() {
        comparator = RomeFactory.getComparator();
    }

    public static GenreGenerator getInstance() {
        if (instance == null) {
            instance = new ControlGenreGenerator();
        }
        return instance;
    }

    private SortedSet<String> read(String input) throws IOException {
        SortedSet<String> genres = new TreeSet<String>();
        Set<String> masculins = new HashSet<String>(), feminins = new HashSet<String>();
        InputStream stream = new FileInputStream(input);
        Reader reader = new InputStreamReader(stream);
        BufferedReader buffer = new BufferedReader(reader);
        String line = null;
        while ((line = buffer.readLine()) != null) {
            String[] tokens = line.split("=");
            if (masculins.contains(tokens[0])) {
                genres.add("MAS : " + line);
            }
            if (feminins.contains(tokens[1])) {
                genres.add("FEM : " + line);
            }
            if (!comparator.matches(tokens[0], tokens[1], 50)) {
                genres.add("DIF : " + line + "(" + comparator.likeness(tokens[0], tokens[1]) + ")");
            }
            masculins.add(tokens[0]);
            feminins.add(tokens[1]);
        }
        buffer.close();
        reader.close();
        stream.close();
        return genres;
    }

    private void write(String output, SortedSet<String> genres) throws IOException {
        Writer writer = new FileWriter(output);
        for (String genre : genres) {
            writer.write(genre + "\n");
        }
        writer.close();
    }

    public void generer(String input, String output) throws IOException {
        this.write(output, this.read(input));
    }
}
