package name.vampidroid;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

public class DatabaseHelper {

    public static final String VAMPIDROID_DB = "VampiDroid.db";

    public static final String KEY_DATABASE_VERSION = "database_version";

    public static final int DATABASE_VERSION = 3;

    public static final String[] STRING_ARRAY_NAME_DISCIPLINES_CAPACITY_INITIALCARDTEXT = new String[] { "Name", "Disciplines", "Capacity", "InitialCardText" };

    public static final String[] STRING_ARRAY_CRYPT_LIST_COLUMNS = new String[] { "Name", "Disciplines", "Capacity", "InitialCardText", "_Group" };

    public static final String[] FROM_STRING_ARRAY_DECK_CRYPT_LIST_COLUMNS = new String[] { "Name", "Disciplines", "Capacity", "InitialCardText", "_Group", "CardNum" };

    public static final String[] FROM_STRING_ARRAY_DECK_LIBRARY_LIST_COLUMNS = new String[] { "Name", "Type", "Clan", "Discipline", "CardNum" };

    static final String[] STRING_ARRAY_NAME_DISCIPLINES_CAPACITY_INITIALCARDTEXT_ADV = new String[] { "Name", "Disciplines", "Capacity", "InitialCardText", "Adv" };

    public static final String ALL_FROM_CRYPT_QUERY = "select _id, Name, Disciplines, Capacity, substr(CardText, 1, 40) as InitialCardText, _Group, Adv from crypt where 1=1";

    public static final String[] ALL_FROM_CRYPT_QUERY_AS_COLUMNS = new String[] { "_id", "Name", "Disciplines", "Capacity", "substr(CardText, 1, 40) as InitialCardText" };

    public static final String ALL_FROM_LIBRARY_QUERY = "select _id, Name, Type, Clan, Discipline from library where 1=1";

    public static final String[] STRING_ARRAY_NAME_DISCIPLINES_CAPACITY = new String[] { "Name", "Disciplines", "Capacity" };

    public static String ORDER_BY_NAME = " order by Name ";

    public static SQLiteDatabase DATABASE = null;

    public enum CardType {

        CRYPT, LIBRARY
    }

    private static Context APPLICATION_CONTEXT;

    /**
	 * Keeps reference of last deck selected when adding a card to deck.
	 */
    public static long LAST_SELECTED_DECK;

    public static HashSet<Long> FAVORITE_CRYPT_CARDS;

    public static HashSet<Long> FAVORITE_LIBRARY_CARDS;

    public static String ALL_FROM_CRYPT_FAVORITES_QUERY = "select _id, Name, Disciplines, Capacity, substr(CardText, 1, 40) as InitialCardText, _Group, Adv from crypt " + "	inner join favorite_cards on _id = CardId and CardType = " + String.valueOf(CardType.CRYPT.ordinal()) + "  where 1=1 ";

    public static String ALL_FROM_LIBRARY_FAVORITES_QUERY = "select _id, Name, Type, Clan, Discipline from library inner join favorite_cards on _id = CardId and CardType = " + String.valueOf(CardType.LIBRARY.ordinal()) + "  where 1=1 ";

    public static String ALL_FROM_CRYPT_DECK_QUERY = "select a._id, Name, " + "Disciplines, Capacity, substr(CardText, 1, 40) as InitialCardText, _Group, Adv, CardNum from crypt a inner join deck_cards b on a._id = b.CardId " + " and CardType = " + String.valueOf(CardType.CRYPT.ordinal()) + " and DeckId = ? ";

    public static String ALL_FROM_LIBRARY_DECK_QUERY = "select a._id, Name, Type, Clan, Discipline, CardNum from library a inner join deck_cards b on a._id = b.CardId " + " and CardType = " + String.valueOf(CardType.LIBRARY.ordinal()) + " and DeckId = ? ";

    public static String SELECT_CRYPT_CARD_NAME = "select Name from crypt where _id = ?";

    public static String SELECT_LIBRARY_CARD_NAME = "select Name from library where _id = ?";

    public static String ALL_DECKS_WITH_CARDS_COUNT = "select _id, Name, " + "(select coalesce(sum(CardNum), 0) from deck_cards where DeckId = a._id and CardType = " + String.valueOf(CardType.CRYPT.ordinal()) + " ) as CryptCardsCount, " + "(select coalesce(sum(CardNum), 0) from deck_cards where DeckId = a._id and CardType = " + String.valueOf(CardType.LIBRARY.ordinal()) + " ) as LibraryCardsCount, " + "(select round(cast(sum(Capacity * CardNum) as real) / sum(CardNum), 2) from crypt c inner join deck_cards dc on " + "c._id = dc.CardId and DeckId = a._id and CardType = " + String.valueOf(CardType.CRYPT.ordinal()) + " ) as CryptCapacityAvg, " + "(select min(cast(Capacity as integer)) from crypt c inner join deck_cards dc on " + "c._id = dc.CardId and DeckId = a._id and CardType = " + String.valueOf(CardType.CRYPT.ordinal()) + " ) as CryptCapacityMin, " + "(select max(cast(Capacity as integer)) from crypt c inner join deck_cards dc on " + "c._id = dc.CardId and DeckId = a._id and CardType = " + String.valueOf(CardType.CRYPT.ordinal()) + " ) as CryptCapacityMax " + "from decks a order by Name";

    public static String SELECT_DECK_CRYPT_FOR_SHARING = "select CardNum, Name from crypt a inner join deck_cards b on a._id = b.CardId " + " and CardType = " + String.valueOf(CardType.CRYPT.ordinal()) + " and DeckId = ? ";

    public static String SELECT_DECK_LIBRARY_FOR_SHARING = "select CardNum, Name from library a inner join deck_cards b on a._id = b.CardId " + " and CardType = " + String.valueOf(CardType.LIBRARY.ordinal()) + " and DeckId = ? ";

    public static SQLiteDatabase getDatabase() {
        if (DATABASE == null) {
            checkAndCreateDatabaseFile();
            DATABASE = SQLiteDatabase.openDatabase(APPLICATION_CONTEXT.getFileStreamPath(VAMPIDROID_DB).getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
            DATABASE.execSQL("PRAGMA case_sensitive_like = true;");
        }
        return DATABASE;
    }

    public static void setApplicationContext(Context context) {
        APPLICATION_CONTEXT = context.getApplicationContext();
    }

    public static void closeDatabase() {
        if (DATABASE != null) {
            DATABASE.close();
            DATABASE = null;
        }
    }

    private static void checkAndCreateDatabaseFile() {
        File databaseFile = APPLICATION_CONTEXT.getFileStreamPath(VAMPIDROID_DB);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(APPLICATION_CONTEXT);
        int databaseVersion = settings.getInt(KEY_DATABASE_VERSION, 1);
        if (databaseVersion < DATABASE_VERSION || !databaseFile.exists()) {
            createDatabaseFile();
            Editor editor = settings.edit();
            editor.putInt(KEY_DATABASE_VERSION, DATABASE_VERSION);
            editor.commit();
        }
    }

    private static void createDatabaseFile() {
        AssetManager am = APPLICATION_CONTEXT.getAssets();
        try {
            InputStream in = am.open("VampiDroid.mp3");
            OutputStream out = APPLICATION_CONTEXT.openFileOutput(VAMPIDROID_DB, Context.MODE_PRIVATE);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void removeFavoriteCard(Long cardId, CardType cardType) {
        getDatabase().delete("favorite_cards", "CardId = ? and CardType = ?", new String[] { String.valueOf(cardId), String.valueOf(cardType.ordinal()) });
    }

    public static void addFavoriteCard(long cardId, CardType cardType) {
        ContentValues row = new ContentValues();
        row.put("CardType", cardType.ordinal());
        row.put("CardId", cardId);
        getDatabase().insert("favorite_cards", null, row);
    }

    public static boolean containsCryptFavorite(long id) {
        Cursor c = getDatabase().rawQuery("select count(*) from favorite_cards where CardId = ? and CardType = ?", new String[] { String.valueOf(id), String.valueOf(CardType.CRYPT.ordinal()) });
        c.moveToFirst();
        boolean result = c.getInt(0) > 0;
        c.close();
        return result;
    }

    public static HashSet<Long> getFavoriteCryptCards() {
        if (FAVORITE_CRYPT_CARDS == null) {
            FAVORITE_CRYPT_CARDS = new HashSet<Long>();
            Cursor c = getDatabase().rawQuery("select CardId from favorite_cards where CardType = ?", new String[] { String.valueOf(CardType.CRYPT.ordinal()) });
            while (c.moveToNext()) {
                FAVORITE_CRYPT_CARDS.add(c.getLong(0));
            }
            c.close();
        }
        return FAVORITE_CRYPT_CARDS;
    }

    public static boolean containsFavoriteCard(long cardId, CardType cardType) {
        Cursor c = getDatabase().rawQuery("select count(*) from favorite_cards where CardId = ? and CardType = ?", new String[] { String.valueOf(cardId), String.valueOf(cardType.ordinal()) });
        c.moveToFirst();
        boolean result = c.getInt(0) > 0;
        c.close();
        return result;
    }

    public static boolean containsLibraryFavorite(long id) {
        Cursor c = getDatabase().rawQuery("select count(*) from favorite_cards where CardId = ? and CardType = ?", new String[] { String.valueOf(id), String.valueOf(CardType.LIBRARY.ordinal()) });
        boolean result = c.getInt(0) > 0;
        c.close();
        return result;
    }

    public static HashSet<Long> getFavoriteLibraryCards() {
        if (FAVORITE_LIBRARY_CARDS == null) {
            FAVORITE_LIBRARY_CARDS = new HashSet<Long>();
            Cursor c = getDatabase().rawQuery("select CardId from favorite_cards where CardType = ?", new String[] { String.valueOf(CardType.LIBRARY.ordinal()) });
            while (c.moveToNext()) {
                FAVORITE_LIBRARY_CARDS.add(c.getLong(0));
            }
            c.close();
        }
        return FAVORITE_LIBRARY_CARDS;
    }

    public static Cursor getDecks() {
        return getDatabase().rawQuery("select _id, Name from decks order by Name", null);
    }

    public static String getDeckName(long deckId) {
        String result = "";
        Cursor c = getDatabase().rawQuery("select Name from decks where _id = ?", new String[] { String.valueOf(deckId) });
        if (c.moveToFirst()) {
            result = c.getString(0);
        }
        c.close();
        return result;
    }

    public static Cursor getDecksWithCardsCount() {
        return getDatabase().rawQuery(ALL_DECKS_WITH_CARDS_COUNT, null);
    }

    public static void createDeck(String deckName) {
        ContentValues row = new ContentValues();
        row.put("Name", deckName);
        getDatabase().insert("decks", null, row);
    }

    public static void addDeckCard(long deckId, long cardId, CardType cardType, int cardNum) {
        getDatabase().delete("deck_cards", "DeckId = ? and CardType = ? and CardId = ?", new String[] { String.valueOf(deckId), String.valueOf(cardType.ordinal()), String.valueOf(cardId) });
        if (cardNum > 0) {
            ContentValues row = new ContentValues();
            row.put("DeckId", deckId);
            row.put("CardType", cardType.ordinal());
            row.put("CardId", cardId);
            row.put("CardNum", cardNum);
            getDatabase().insert("deck_cards", null, row);
        }
    }

    public static int getCountCardsInDeck(long deckId, long cardId, CardType cardType) {
        Cursor c = getDatabase().rawQuery("select CardNum from deck_cards where DeckId = ? and CardType = ? and CardId = ?", new String[] { String.valueOf(deckId), String.valueOf(cardType.ordinal()), String.valueOf(cardId) });
        int result = 0;
        if (c.getCount() != 0) {
            c.moveToFirst();
            result = c.getInt(0);
        }
        c.close();
        return result;
    }

    public static String getCardName(long cardId, CardType cardType) {
        String sql = "";
        switch(cardType) {
            case CRYPT:
                sql = SELECT_CRYPT_CARD_NAME;
                break;
            case LIBRARY:
                sql = SELECT_LIBRARY_CARD_NAME;
                break;
        }
        Cursor c = getDatabase().rawQuery(sql, new String[] { String.valueOf(cardId) });
        c.moveToFirst();
        String result = c.getString(0);
        c.close();
        return result;
    }

    public static void removeDeck(long deckId) {
        getDatabase().delete("deck_cards", "DeckId = ?", new String[] { String.valueOf(deckId) });
        getDatabase().delete("decks", "_id = ?", new String[] { String.valueOf(deckId) });
    }
}
