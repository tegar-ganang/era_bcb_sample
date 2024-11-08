import java.util.*;

public class SoundTrainer {

    private int totalNum[] = new int[12];

    private int histsize = 12;

    private int history[][] = new int[12][histsize];

    private Vector notes;

    private int level = 0;

    public String ordering1[] = { "A", "E", "C", "F# | Gb", "D", "B", "F", "G", "D# | Eb", "A# | Bb", "C# | Db", "G# | Ab" };

    public String ordering2[] = { "F", "G", "D# | Eb", "A# | Bb", "C# | Db", "G# | Ab", "E", "A", "C", "F# | Gb", "D", "B" };

    public String ordering3[] = { "C", "C# | Db", "D", "D# | Eb", "E", "F", "F# | Gb", "G", "G# | Ab", "A", "A# | Bb", "B" };

    public String ordering[] = ordering1;

    public Vector getNotes() throws Exception {
        if (canAdvance() && level < 11) increaseLevel();
        return notes;
    }

    private void increaseLevel() throws Exception {
        level++;
        notes = vecFromNotes(ordering, level + 2);
    }

    public void setLevel(int newLevel) throws Exception {
        if (newLevel > 11 || newLevel < 0) throw new Exception("Invalid level");
        notes = vecFromNotes(ordering, newLevel + 1);
        level = newLevel - 1;
        for (int i = 0; i < 12; ++i) {
            totalNum[i] = 0;
            for (int j = 0; j < histsize; ++j) history[i][j] = 0;
        }
    }

    public void reportCorrect(String note) throws Exception {
        int key = MidiBank.noteToKey(note);
        if (totalNum[key] < histsize) totalNum[key]++;
        shiftleft(history[key], histsize);
        history[key][histsize - 1] = 1;
    }

    public void reportWrong(String note) throws Exception {
        int key = MidiBank.noteToKey(note);
        if (totalNum[key] < histsize) totalNum[key]++;
        shiftleft(history[key], histsize);
        history[key][histsize - 1] = 0;
    }

    public int levelID() throws Exception {
        if (canAdvance() && level < 11) increaseLevel();
        return level;
    }

    public int getLevel() {
        return level;
    }

    private boolean canAdvance() throws Exception {
        boolean can = true;
        for (int i = 0; i < notes.size(); ++i) {
            int key = MidiBank.noteToKey((String) notes.get(i));
            int numguessed = totalNum[key];
            int index1 = 0;
            if (numguessed < histsize) index1 = histsize - numguessed;
            int sum = sumInclusive(history[key], index1, histsize - 1);
            int numwrong = sum - numguessed;
            double ratio = 0;
            if (numguessed > 0) {
                ratio = (double) sum / numguessed;
                if (ratio < 0.85) can = false;
            }
            int numcorrect = numguessed - numwrong;
            if (numcorrect < 12 && !(numcorrect > 6 && ratio == 1.0)) can = false;
        }
        return can;
    }

    public SoundTrainer() {
        for (int i = 0; i < 12; ++i) {
            totalNum[i] = 0;
            for (int j = 0; j < histsize; ++j) history[i][j] = -100;
        }
        notes = vecFromNotes(ordering, level + 2);
    }

    public static Vector vecFromNotes(String[] s, int index) {
        Vector v = new Vector();
        for (int i = 0; i < index; ++i) v.add(s[i]);
        return v;
    }

    private static void shiftleft(int array[], int size) {
        if (size < 1) return;
        int zeroth = array[0];
        for (int i = 0; i < size - 1; ++i) array[i] = array[i + 1];
        array[size - 1] = zeroth;
    }

    private static int sumInclusive(int array[], int index1, int index2) {
        int sum = 0;
        for (int i = index1; i <= index2; ++i) sum += array[i];
        return sum;
    }

    public void switchOrdering(int newOrdering) {
        if (newOrdering == 1) ordering = ordering1; else if (newOrdering == 2) ordering = ordering2; else if (newOrdering == 3) ordering = ordering3; else return;
    }

    public static void main(String[] args) {
        SoundTrainer t = new SoundTrainer();
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
