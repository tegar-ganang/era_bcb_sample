package com.nuts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import javax.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.springframework.transaction.annotation.Transactional;
import com.nuts.model.server.Food;
import com.nuts.model.server.Tag;
import com.nuts.model.server.Translation;
import com.nuts.services.DaoImpl;
import com.nuts.services.I18N;

public class TranslationsImporter {

    static class Chunk {

        private int placeholdersNo = 0;

        private String[] tokens;

        public Chunk(String s) {
            this.tokens = splitTranslation(s);
            for (int f = 0; f < tokens.length; f++) {
                if (isDotted(tokens[f])) {
                    tokens[f] = ".";
                    placeholdersNo++;
                }
            }
        }

        public void addTemplateTokens(int tokensToAddInFront) {
            String[] newTokens = new String[tokens.length + tokensToAddInFront];
            for (int f = 0; f < tokens.length; f++) {
                newTokens[f + tokensToAddInFront] = tokens[f];
            }
            tokens = newTokens;
            for (int f = 0; f < tokensToAddInFront; f++) {
                tokens[f] = ".";
            }
        }

        public int getNumberOfTemplates() {
            return placeholdersNo;
        }

        public int replaceDotsWith(Chunk original) {
            for (int f = 0; f < tokens.length && f < original.tokens.length; f++) {
                if (isDotted(tokens[f])) {
                    tokens[f] = original.tokens[f];
                } else return f;
            }
            return tokens.length;
        }

        @Override
        public String toString() {
            StringBuffer b = new StringBuffer();
            for (int f = 0; f < tokens.length; f++) {
                b.append(tokens[f]);
                if (f < tokens.length - 1) b.append(", ");
            }
            return b.toString();
        }

        public void validate() {
            for (int f = 0; f < tokens.length; f++) {
                if (isDotted(tokens[f])) {
                    throw new RuntimeException(this.toString());
                }
            }
        }
    }

    static class TagStatsMap {

        private final HashMap<String, Map<String, Integer>> map = new HashMap<String, Map<String, Integer>>();

        public String print() {
            StringBuilder ret = new StringBuilder();
            for (Map.Entry<String, Map<String, Integer>> me : map.entrySet()) {
                ret.append(me.getKey()).append("\n");
                for (Map.Entry<String, Integer> me2 : me.getValue().entrySet()) {
                    ret.append("\t\t\t\t\t");
                    ret.append("'").append(me2.getKey()).append("'").append("\t=");
                    ret.append(me2.getValue()).append("\n");
                }
                ret.append("\n");
            }
            return ret.toString();
        }

        public void put(String original, String translation) {
            Map<String, Integer> countersMap = map.get(original);
            if (countersMap == null) {
                countersMap = new HashMap<String, Integer>();
                map.put(original, countersMap);
            }
            Integer counter = countersMap.get(translation);
            if (null == counter) {
                counter = Integer.valueOf(1);
            } else {
                counter = counter + 1;
            }
            countersMap.put(translation, counter);
        }
    }

    static class Translations {

        private final List<Chunk> chunks = new ArrayList<Chunk>();

        private final Integer objectId;

        public Translations(Integer objectId, String translations, Translations prev) {
            this.objectId = objectId;
            String[] chunkedStrings = translations.split(Matcher.quoteReplacement(";"));
            for (String s : chunkedStrings) {
                chunks.add(new Chunk(s.trim()));
            }
            int numberOfTemplates = getEnglish().getNumberOfTemplates();
            if (numberOfTemplates > 0) {
                for (int f = 1; f < chunks.size(); f++) {
                    Chunk c = chunks.get(f);
                    c.addTemplateTokens(numberOfTemplates);
                }
                if (prev != null) for (int f = 0; f < chunks.size(); f++) {
                    Chunk c = chunks.get(f);
                    Chunk prevChunk = null;
                    if (prev.chunks.size() > f) {
                        prevChunk = prev.chunks.get(f);
                    } else {
                        if (f > 1) prevChunk = chunks.get(f - 1);
                    }
                    if (prevChunk != null) {
                        c.replaceDotsWith(prevChunk);
                        System.err.println(c);
                    }
                }
            }
        }

        public Chunk getEnglish() {
            return chunks.get(0);
        }

        public Integer getObjectId() {
            return objectId;
        }

        @Override
        public String toString() {
            StringBuffer b = new StringBuffer();
            b.append(objectId).append("\t");
            for (Chunk c : chunks) {
                b.append(c.toString());
                b.append(";\t ");
            }
            return b.toString();
        }

        public void validate() {
            if (chunks.size() == 0) throw new RuntimeException();
            for (int f = 0; f < chunks.size(); f++) {
                Chunk c = chunks.get(f);
                c.validate();
            }
        }
    }

    public static final String REG = "(\\.\\s+)|,";

    public static boolean isDotted(String s) {
        if (s == null || s.length() == 0) return false;
        for (int f = 0; f < s.length(); f++) {
            if (s.charAt(f) != '.') {
                return false;
            }
        }
        return true;
    }

    public static String[] splitTranslation(String translation) {
        String[] words = translation.split(TranslationsImporter.REG);
        for (int f = 0; f < words.length; f++) words[f] = words[f].trim();
        return words;
    }

    @Inject
    DaoImpl dao;

    private String baseDir;

    public void extractContents(String fileContents) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(fileContents));
        String line = "";
        int i = 0;
        Translations prevT = null;
        TagStatsMap m = new TagStatsMap();
        try {
            while ((line = reader.readLine()) != null) {
                i++;
                if (!line.trim().isEmpty()) {
                    String[] tokens = line.split(Matcher.quoteReplacement("\t"));
                    final int foodId = Integer.parseInt(tokens[0].trim());
                    Translations t = new Translations(foodId, tokens[1], prevT);
                    t.validate();
                    prevT = t;
                    System.out.println(t);
                    putTranslations2TagStatsMap(m, t);
                    persist(t);
                }
            }
            System.out.println(m.print());
        } catch (Exception ex) {
            throw new RuntimeException("line: " + i + " " + line, ex);
        }
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void importFile(String filename) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        String contents = readFile(filename);
        validateFileContents(contents);
        extractContents(contents);
    }

    public void importTranslations() throws IOException {
        String filename = "food_commonNames_translated_500.txt";
        importFile(filename);
    }

    public String readFile(String filename) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        File f = new File(baseDir);
        f = new File(f, filename);
        StringWriter w = new StringWriter();
        Reader fr = new InputStreamReader(new FileInputStream(f), "UTF-8");
        IOUtils.copy(fr, w);
        fr.close();
        w.close();
        String contents = w.toString();
        return contents;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public void validateFileContents(String contents) throws IOException {
        StringReader sr = new StringReader(contents);
        BufferedReader reader = new BufferedReader(sr);
        String line;
        int i = 0;
        while ((line = reader.readLine()) != null) {
            i++;
            if (!line.trim().isEmpty()) {
                String[] tokens = line.split(Matcher.quoteReplacement("\t"));
                Integer id = Integer.parseInt(tokens[0].trim());
                String[] translations = tokens[1].split(Matcher.quoteReplacement(";"));
                validateTranslations(translations, i);
            }
        }
    }

    private void persist(Translations t) {
        for (int f = 0; f < t.chunks.size(); f++) {
            String key = Food.getI18nKey(t.objectId) + (f > 1 ? I18N.KEY_SEPARATOR + f : "");
            String lang = f == 0 ? "en" : "ru";
            saveOrUpdateTranslation(key, lang, t.chunks.get(f).toString());
        }
        persistTags(t);
    }

    @Transactional
    public void applyFoodTags() {
        List<Food> allfoods = dao.findAll(Food.class);
        for (Food food : allfoods) {
            String[] tagKeys = splitFoodName(food.getDescr());
            for (int j = 0; j < tagKeys.length; j++) {
                Tag tag = createOrFindTag(tagKeys[j]);
                food.getTags().add(tag);
            }
            dao.updateEntity(food);
        }
    }

    private String[] splitFoodName(String foodName) {
        String[] words = splitTranslation(foodName);
        for (int i = 0; i < words.length; i++) {
            words[i] = Tag.shortenKey(words[i]);
        }
        return words;
    }

    private void persistTags(Translations t) {
        Food food = dao.findFoodByUsdaId(t.getObjectId());
        int tagsNumber = t.getEnglish().tokens.length;
        for (int j = 0; j < tagsNumber; j++) {
            String key = t.getEnglish().tokens[j].toLowerCase();
            key = Tag.shortenKey(key);
            Tag tag = createOrFindTag(key);
            food.getTags().add(tag);
            for (int i = 0; i < t.chunks.size(); i++) {
                String tkey = tag.getI18nKey();
                String lang = i == 0 ? "en" : "ru";
                String value = t.chunks.get(i).tokens[j];
                saveOrUpdateTranslation(tkey, lang, value);
            }
        }
        dao.updateEntity(food);
    }

    private Tag createOrFindTag(String key) {
        Tag tag = null;
        tag = dao.find(Tag.class, key);
        if (tag == null) {
            tag = new Tag();
            tag.setKey(key);
            dao.persistEntity(tag);
        }
        return tag;
    }

    private void putTranslations2TagStatsMap(TagStatsMap map, Translations t) {
        for (int f = 1; f < t.chunks.size(); f++) {
            for (int j = 1; j < t.getEnglish().tokens.length; j++) {
                map.put(t.getEnglish().tokens[j], t.chunks.get(f).tokens[j]);
            }
        }
    }

    private void saveOrUpdateTranslation(String key, String lang, String value) {
        Translation tr = dao.findTranslation(key, lang);
        if (tr == null) {
            tr = new Translation();
            tr.getId().setLang(lang);
            tr.getId().setKey(key);
            tr.setValue(value);
            dao.persistEntity(tr);
        } else {
            tr.setValue(value);
            dao.updateEntity(tr);
        }
    }

    private void validateTranslations(String[] translations, int lineNo) {
        String original = translations[0];
        String[] originalWords = splitTranslation(original);
        int num = 0;
        for (String wrd : originalWords) {
            if (!isDotted(wrd)) num++;
        }
        for (int f = 1; f < translations.length; f++) {
            String[] words = splitTranslation(translations[f]);
            if (words.length != num) {
                throw new RuntimeException("line: " + lineNo + " " + translations[f] + " FORMAT does not match " + original);
            }
        }
    }
}
