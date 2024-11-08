package net.sf.karatasi.codecs;

import java.io.File;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.sf.japi.swing.action.ActionBuilder;
import net.sf.japi.swing.action.ActionBuilderFactory;
import net.sf.karatasi.KaratasiPreferences;
import net.sf.karatasi.database.Card;
import net.sf.karatasi.database.Category;
import net.sf.karatasi.database.Database;
import net.sf.karatasi.databasecheck.ProgressInformationHandlerInterface;
import net.sf.karatasi.desktop.GuiMain;
import org.jetbrains.annotations.NotNull;

public class PdbDatabaseImporter extends PdbDatabaseCodec implements DatabaseImporter {

    /** Maximum number of lines before a commit of the database transactions. */
    private static final int LINES_PER_TRANSACTION = 10;

    /** Category to store the layout data of a category. */
    public static class CategoryLayoutData {

        /** The answer format. */
        private final String answer;

        /** The question format. */
        private final String question;

        /** Create a category layout data structure.
         *
         * @param answer the answer string.
         * @param question the question string.
         */
        public CategoryLayoutData(final String answer, final String question) {
            super();
            this.answer = answer;
            this.question = question;
        }

        /** Getter for the answer string.
         *
         * @return the answer format.
         */
        public String getAnswer() {
            return answer;
        }

        /** Getter for the question string.
         *
         * @return the question format.
         */
        public String getQuestion() {
            return question;
        }
    }

    /** The ActionBuilder for string localization. */
    private final ActionBuilder actionBuilder = ActionBuilderFactory.getInstance().getActionBuilder(GuiMain.class);

    /** The string encoding of the Palm. */
    @NotNull
    private String pdbEncoding = KaratasiPreferences.create().get(KaratasiPreferences.PREFS_KEY_PDB_ENCODING, PDB_ENCODING_EUROPE);

    /** Indicator whether the database is committed */
    private boolean isDbCommitted;

    /** Database commit date */
    private int dbCommitDate;

    /** {@inheritDoc} */
    public boolean canAddToDatabase() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        setPalmEncoding(KaratasiPreferences.create().get(KaratasiPreferences.PREFS_KEY_PDB_ENCODING, PDB_ENCODING_EUROPE));
    }

    /** method with new interface for importing a new database from a file.
     * @param targetDatabase the open target database (sqlite database with karatasi-specific structure but no cards and categories so far)
     * @param srcFile the source file with the data to be imported
     * @progressHandler the progress handler
     * @throws ImportFailedException if import failed
     * @throws UnsupportedOperationException in case this Importer doesn't support importing with the new interface.
     */
    public void importNewDatabase(final Database targetDatabase, final File srcFile, final ProgressInformationHandlerInterface progressHandler) throws ImportFailedException {
        try {
            final PdbReader pdbReader = new PdbReader(srcFile, pdbEncoding);
            try {
                if (progressHandler != null) {
                    progressHandler.setStepCount(pdbReader.getRecordCount() + 1);
                    progressHandler.setActivityDescription(actionBuilder.getString("importPdbDatabase.progress.reading.records"));
                }
                processPdbData(targetDatabase, pdbReader, progressHandler);
            } finally {
                pdbReader.close();
            }
        } catch (final Exception e) {
            targetDatabase.kill();
            if (progressHandler != null) {
                progressHandler.notifyError("", e.getMessage());
                return;
            } else {
                throw new ImportFailedException(e.getMessage());
            }
        }
        if (progressHandler != null) {
            progressHandler.notifyFinished();
        }
    }

    /** {@inheritDoc} */
    public void importIntoDatabase(@NotNull final Database database, @NotNull final File file, final ProgressInformationHandlerInterface progressHandler) throws Exception {
        throw new UnsupportedOperationException("append of a pdb file to a database not supported");
    }

    /** Stop the import, and quit the import function.
     * This method is intended to stop imports running in an separate thread.
     * For fast imports this method does nothing.
     */
    public void cancelImport() {
    }

    /** Getter for encoding.
     * @return the string encoding of the pdb file.
     */
    public String getPalmEncoding() {
        return pdbEncoding;
    }

    /** Setter for encoding. Use the PdbDatabaseCodec.PDB_ENCODING_* constants.
     * @param pdbEncoding the string encoding of the pdb file.
     */
    public void setPalmEncoding(@NotNull final String pdbEncoding) {
        this.pdbEncoding = pdbEncoding;
    }

    /** Do the real processing of the pdb database.
     *
     * @param database the open target database.
     * @param pdbReader the reader for the pdb file.
     * @param progressHandler the human interface class
     * @throws ImportFailedException if the import has failed
     */
    private void processPdbData(final Database database, final PdbReader pdbReader, final ProgressInformationHandlerInterface progressHandler) throws ImportFailedException {
        int stepCount = 0;
        try {
            try {
                database.beginTransaction();
                final String newDatabaseName = pdbReader.getDatabaseName().replaceFirst("^sm_", "");
                if (database.getFullName().length() == 0 && newDatabaseName.length() != 0) {
                    database.renameTo(newDatabaseName);
                }
                if (progressHandler != null) {
                    progressHandler.stepForwardByOne();
                }
                stepCount++;
                if (stepCount % LINES_PER_TRANSACTION == 0) {
                    database.commitTransaction();
                    database.beginTransaction();
                }
                final Map<Integer, Integer> record2categoryIdx = new HashMap<Integer, Integer>();
                final Map<Integer, Category> categoryIdx2category = new HashMap<Integer, Category>();
                final Set<Integer> recordsInDrill = new HashSet<Integer>();
                final Map<Integer, CategoryLayoutData> record2categoryLayout = new HashMap<Integer, CategoryLayoutData>();
                for (int recordIdx = 0; recordIdx < pdbReader.getRecordCount(); recordIdx++) {
                    final PdbRecord record = pdbReader.getRecord(recordIdx);
                    if (recordIdx == 0) {
                        processGeneralInformationRecord(record, database);
                    } else if (recordIdx == 10) {
                        processCategoryOrderRecord(record, database);
                    } else if (recordIdx == 11) {
                        processCategoryReferencesRecord(record, record2categoryIdx, categoryIdx2category, database);
                    } else if (recordIdx == 12) {
                    } else if (recordIdx == 13) {
                        processDrillListRecord(record, recordsInDrill);
                    } else if (recordIdx > 13) {
                        final int recordTag = record.read16Bit(0);
                        if (recordTag == 0x0002 || recordTag == 0x8002) {
                            processCardRecord(record, categoryIdx2category, recordsInDrill, database);
                        } else if (recordTag == 0x0003) {
                            processCategoryRecord(record, record2categoryIdx, categoryIdx2category, record2categoryLayout, database);
                        } else if (recordTag == 0x0004) {
                            processCategoryLayoutRecord(record, record2categoryLayout);
                        } else {
                            throw new Exception("invalid record type " + recordTag);
                        }
                    }
                    if (progressHandler != null) {
                        progressHandler.stepForwardByOne();
                    }
                    stepCount++;
                }
                database.commitTransaction();
            } catch (final Exception e) {
                database.rollbackTransaction();
                throw e;
            }
        } catch (final Exception e) {
            database.kill();
            String errMsg = "";
            if (progressHandler != null) {
                if (stepCount == 0) {
                    errMsg = actionBuilder.getString("importPdbDatabase.progress.failed.database");
                } else {
                    errMsg = String.format(actionBuilder.getString("importPdbDatabase.progress.failed.record"), (stepCount - 1));
                }
                progressHandler.notifyError(errMsg, e.getMessage());
            }
            throw new ImportFailedException(e.getMessage());
        }
    }

    /** Process the general information record (tag 1, idx 0).
     * @param record the input record.
     * @param database the output database.
     * @throws Exception if anything fails.
     */
    private void processGeneralInformationRecord(final PdbRecord record, final Database database) throws Exception {
        database.writeCreationDate(record.readDbDate(0x08));
        dbCommitDate = record.readDbDate(0x0a);
        isDbCommitted = dbCommitDate != 0;
        if (isDbCommitted) {
            database.writeCommitDate(dbCommitDate);
        }
    }

    /** Process the category ordering record (tag 1, idx 10).
     * @param record the input record.
     * @param database the output database.
     * @throws Exception if anything fails.
     */
    private void processCategoryOrderRecord(final PdbRecord record, final Database database) throws Exception {
    }

    /** Process the category references record (tag 1, idx 11).
     * @param record the input record.
     * @param record2categoryIdx the lookup table for the categories.
     * @throws Exception if anything fails.
     */
    private void processCategoryReferencesRecord(final PdbRecord record, @NotNull final Map<Integer, Integer> record2categoryIdx, final Map<Integer, Category> categoryIdx2category, final Database database) throws Exception {
        record.setReadPointer(0x20);
        int categoryIdx = 0;
        while (record.readPointerIsValid()) {
            final int recordNr = record.read16Bit();
            if (recordNr > 0) {
                record2categoryIdx.put(Integer.valueOf(recordNr), Integer.valueOf(categoryIdx));
                final Category newCategory = database.makeNewCategory();
                categoryIdx2category.put(Integer.valueOf(categoryIdx), newCategory);
            }
            categoryIdx++;
        }
        record2categoryIdx.put(Integer.valueOf(15), Integer.valueOf(0xffff));
        final Category newCategory = database.makeNewCategory();
        categoryIdx2category.put(Integer.valueOf(0xffff), newCategory);
    }

    /** Process the test drill record (tag 1, idx 13).
     * @param record the input record.
     * @param database the output database.
     * @throws Exception if anything fails.
     */
    private void processDrillListRecord(final PdbRecord record, final Set<Integer> recordsInDrill) throws Exception {
        record.setReadPointer(0x20);
        while (record.readPointerIsValid()) {
            recordsInDrill.add(record.read16Bit());
        }
    }

    /** Process a category record (tag 3) in Palm SuperMemo database format.
     * @param record the input record.
     * @param record2categoryIdx the lookup table for the categories.
     * @param categoryIdx2Record the lookup table for the categories.
     * @param database the output database.
     * @throws Exception if anything fails.
     */
    private void processCategoryRecord(final PdbRecord record, @NotNull final Map<Integer, Integer> record2categoryIdx, final Map<Integer, Category> categoryIdx2category, final Map<Integer, CategoryLayoutData> record2categoryLayout, final Database database) throws Exception {
        final Integer recordIdxInt = Integer.valueOf(record.getIndex());
        final Integer categoryIdxInt = record2categoryIdx.get(recordIdxInt).intValue();
        assert categoryIdxInt != null;
        final Category category = categoryIdx2category.get(categoryIdxInt);
        assert category != null;
        category.setCategoryName(record.read0String(0x02));
        for (int n = 0; n < 6; n++) {
            category.setFieldTitle(n, record.read0String(0x22 + n * 0x10));
        }
        int leftColumn = record.read16Bit(0x88);
        if (leftColumn == 0xFFFF) {
            leftColumn = 0;
        }
        int rightColumn = record.read16Bit(0x84);
        if (leftColumn == rightColumn) {
            rightColumn = record.read16Bit(0x86);
        }
        category.setDisplayLeft(leftColumn);
        category.setDisplayRight(rightColumn);
        final CategoryLayoutData layoutPair = record2categoryLayout.get(recordIdxInt);
        if (layoutPair != null) {
            category.setAnswerForm(layoutPair.getAnswer());
            category.setQuestionForm(layoutPair.getQuestion());
        }
    }

    /** Process a card record (tag 3).
     * @param record the input record.
     * @param categoryIdx2Record the lookup table for the categories.
     * @param database the output database.
     * @throws Exception if anything fails.
     */
    private void processCardRecord(final PdbRecord record, final Map<Integer, Category> categoryIdx2category, final Set<Integer> recordsInDrill, final Database database) throws Exception {
        final int categoryIdx = record.read16Bit(0x02);
        final Category category = categoryIdx2category.get(Integer.valueOf(categoryIdx));
        assert category != null;
        final Card card = database.makeNewCard();
        assert card != null;
        card.setCategory(category);
        card.setCreationDate(record.readDbDate(0x06));
        final int cardCommitDate = record.readDbDate(0x04);
        if (isDbCommitted && cardCommitDate >= dbCommitDate) {
            card.commitCard(cardCommitDate);
            final int ratings = record.read8Bit(0x12);
            card.setLearningData(record.readDbDate(0x08), record.read8Bit(0x11), record.read8Bit(0x10), record.read16Bit(0x0a), record.read16Bit(0x0c), record.read8Bit(0x0e), record.read8Bit(0x0f), ratings >> 5 & 0x07, ratings >> 2 & 0x07);
        } else {
            card.uncommitCard();
        }
        record.setReadPointer(0x14);
        for (int n = 0; n < 6; n++) {
            card.setDataField(n, record.read0String());
        }
        if (recordsInDrill.contains(Integer.valueOf(record.getIndex()))) {
            card.addToDrill();
        }
    }

    /** Process the category layout record (tag 4).
     * @param record the input record.
     * @throws Exception if anything fails.
     */
    private void processCategoryLayoutRecord(final PdbRecord record, final Map<Integer, CategoryLayoutData> record2categoryLayout) throws Exception {
        final byte[][] formatFlags = new byte[8][20];
        final String[] formatStrings = new String[20];
        final String[] qaStrings = new String[2];
        record.setReadPointer(0x20);
        for (int n = 0; n < 20; n++) {
            for (int m = 0; m < 8; m++) {
                formatFlags[m][n] = (byte) record.read8Bit();
            }
        }
        for (int n = 0; n < 20; n++) {
            formatStrings[n] = record.read0String();
        }
        for (int qa = 0; qa < 2; qa++) {
            String resultString = "";
            for (int n = 0; n < 10; n++) {
                final int idx = n + qa * 10;
                String formatString = recodePdbFormatToHtml(formatStrings[idx]);
                if (formatString.length() > 0) {
                    String fontOptions = "";
                    final byte flags = formatFlags[4][idx];
                    final byte alignment = formatFlags[5][idx];
                    if ((flags & 0x40) != 0) {
                        formatString = "<b>" + formatString + "</b>";
                    }
                    if ((flags & 0x0c) != 0) {
                        formatString = "<u>" + formatString + "</u>";
                    }
                    if ((flags & 0x10) != 0) {
                        formatString = "<i>" + formatString + "</i>";
                    }
                    if ((flags & 0x20) != 0) {
                        fontOptions += " face=\"arial narrow,sans-serif\"";
                    } else {
                        fontOptions += " face=\"sans-serif\"";
                    }
                    if ((flags & 0x03) != 0) {
                        fontOptions += "  size=5";
                    } else if ((flags & 0x80) != 0) {
                        fontOptions += "  size=3";
                    } else {
                        fontOptions += "  size=4";
                    }
                    formatString = "<font " + fontOptions + ">" + formatString + "</font>";
                    if (alignment == 0x00) {
                        formatString = "<p align=left>" + formatString + "</p>";
                    } else if (alignment == 0x01) {
                        formatString = "<p align=center>" + formatString + "</p>";
                    } else if (alignment == 0x02) {
                        formatString = "<p align=right>" + formatString + "</p>";
                    }
                    if (resultString.length() > 0) {
                        resultString += "\n";
                        resultString += formatString;
                    } else {
                        resultString = formatString;
                    }
                }
            }
            qaStrings[qa] = resultString;
        }
        final CategoryLayoutData data = new CategoryLayoutData(qaStrings[1], qaStrings[0]);
        record2categoryLayout.put(Integer.valueOf(record.getIndex() + 1), data);
    }

    /** Recode a format string to a html format string.
     * The method is public to be accessible by the tests.
     *
     * @param the pdb format string.
     * @return the html format string.
     */
    @NotNull
    public static String recodePdbFormatToHtml(@NotNull final String htmlString) {
        final StringBuilder builder = new StringBuilder();
        final StringCharacterIterator iterator = new StringCharacterIterator(htmlString);
        int cnt = 0;
        char fieldNr = ' ';
        for (char c = iterator.current(); c != CharacterIterator.DONE; c = iterator.next()) {
            if (c == '<') {
                builder.append("&lt;");
                cnt = 0;
            } else if (c == '>') {
                builder.append("&gt;");
                cnt = 0;
            } else if (c == '&') {
                builder.append("&amp;");
                cnt = 0;
            } else if (c == '\n') {
                builder.append("<br>");
                cnt = 0;
            } else if (c == '{') {
                builder.append(c);
                cnt = 1;
            } else if (cnt == 1 && c == '^') {
                builder.append(c);
                cnt = 2;
            } else if (cnt == 2) {
                builder.append(c);
                fieldNr = c;
                cnt = 3;
            } else if (cnt == 3 && c == '}') {
                builder.delete(builder.length() - 3, builder.length());
                builder.append("<!--");
                builder.append(fieldNr);
                builder.append("-->");
                cnt = 0;
            } else {
                builder.append(c);
                cnt = 0;
            }
        }
        return builder.toString();
    }
}
