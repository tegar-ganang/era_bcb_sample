package fr.insee.rome.io.genre;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimplifiedGenreGenerator implements GenreGenerator {

    private static GenreGenerator instance = null;

    private List<Pattern> patterns = null;

    private final int N = 10;

    public SimplifiedGenreGenerator() {
        patterns = new ArrayList<Pattern>(N);
        for (int n = 0; n < N; n++) {
            patterns.add(Pattern.compile("\\d{5}=((?:\\w+\\W){" + n + "}\\w+)/((?:\\w+\\W){" + n + "}\\w+)"));
        }
    }

    public static GenreGenerator getInstance() {
        if (instance == null) {
            instance = new SimplifiedGenreGenerator();
        }
        return instance;
    }

    private SortedSet<String> read(String input) throws IOException {
        SortedSet<String> genres = new TreeSet<String>();
        InputStream stream = new FileInputStream(input);
        Reader reader = new InputStreamReader(stream);
        BufferedReader buffer = new BufferedReader(reader);
        String line = null;
        while ((line = buffer.readLine()) != null) {
            for (Pattern pattern : this.patterns) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String genre = simplify(matcher.group(1) + "=" + matcher.group(2));
                    if (genre != null) {
                        genres.add(genre);
                    }
                    break;
                }
            }
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

    private String simplify(String genre) {
        String[] genres = genre.split("=");
        String masculin = genres[0], feminin = genres[1];
        if (masculin.equals(feminin)) {
            return null;
        }
        masculin = masculin.replaceAll("\\W", " ").replaceAll(" +", " ");
        feminin = feminin.replaceAll("\\W", " ").replaceAll(" +", " ");
        String[] masculins = masculin.split(" "), feminins = feminin.split(" ");
        String simplifiedMasculin = "", simplifiedFeminin = "";
        for (int n = 0; n < masculins.length; n++) {
            if (masculins[n].equals(feminins[n])) {
            } else {
                simplifiedMasculin = simplifiedMasculin + masculins[n] + " ";
                simplifiedFeminin = simplifiedFeminin + feminins[n] + " ";
            }
        }
        return simplifiedMasculin.trim() + "=" + simplifiedFeminin.trim();
    }
}
