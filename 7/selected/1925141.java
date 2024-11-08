package cardgames.control.maumau;

import cardgames.control.maumau.announcements.MauMau;
import cardgames.control.maumau.announcements.Mau;
import cardgames.control.maumau.announcements.MauMauPlayerAnnouncement;
import cardgames.control.maumau.announcements.SuitRequired;
import cardgames.control.maumau.exceptions.IllegalCard;
import cardgames.control.maumau.exceptions.IllegalToken;
import cardgames.control.maumau.exceptions.SubscriptionFailed;
import cardgames.control.maumau.exceptions.MauMauTableException;
import cardgames.control.maumau.exceptions.Revoke;
import cardgames.control.maumau.messages.SubscriptionSucceeded;
import cardgames.control.maumau.messages.CardPutDown;
import cardgames.control.maumau.messages.PenaltyCardsSent;
import cardgames.control.maumau.messages.FinishedGame;
import cardgames.control.maumau.messages.MauMauTableMessage;
import cardgames.control.maumau.messages.CardIntermitted;
import cardgames.control.maumau.messages.FinishedMatch;
import cardgames.control.maumau.messages.FinishedMatchAbnormal;
import cardgames.control.maumau.messages.FinishedMatchRegular;
import cardgames.control.maumau.messages.PlayerSequence;
import cardgames.control.maumau.messages.TalonRebuild;
import cardgames.model.cards.MauMauCard;
import cardgames.model.cards.MauMauDeck;
import cardgames.view.maumau.ConsoleMauMauTable;
import cardgames.view.maumau.MauMauTableViewer;
import de.root1.simon.Registry;
import de.root1.simon.Simon;
import de.root1.simon.annotation.SimonRemote;
import de.root1.simon.exceptions.NameBindingException;
import de.root1.simon.exceptions.SimonRemoteException;
import java.io.IOException;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Vector;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * The <code>MauMauTable</code> implements a Mau-Mau-table's functionality.
 * <p>
 * A table operates as server that provides players (clients) the opportunity to play the card game
 * <a href="http://en.wikipedia.org/wiki/Mau_Mau_(card_game)" target="_blank">Mau-Mau</a>.
 * The table implements the {@link MauMauTableInterface server's interface} that
 * communicates over a TCP/IP-connection with it's {@link MauMauPlayerInterface client}
 * using <a href="http://dev.root1.de/projects/simon/" target="_blank">SIMON</a>.
 * <p>
 * A table is {@link #register() registered} on {@link #MauMauTable(java.lang.String, int, boolean, boolean, boolean, boolean, boolean, int, boolean, int, int, long) creation}.
 * If this registration fails (the provided <code>port</code> could be occupied
 * for instance) the table will never get operational. If the table's registration
 * succeeds it operates in two different states:
 * <a newPlayersName="states"></a>
 * <ol>
 *      <li> first state <i>subscribing</i> <br>
 *           The table accepts a player's subscription if there is
 *           {@link #offersSubscription() a seat} left for a subscriber <strong>and</strong> there
 *           is no other player with the same {@link MauMauPlayerInterface#getName() name}.
 *           When a subscribing player occupies the last available seat the
 *           table starts to carry out a match. <br>
 *      <li> second state <i>playing</i> <br>
 *           The table runs a match which consists in a series of single games. The
 *           table starts these games and manages the <a href="MauMauTableInterface.html#polling">players' moves</a>
 *           according to the rules set on table's creation. <br>
 * </ol>
 * The table shuttles back and forth between subscribing and playing until it is
 * {@link #shutDown() shut down} or networking breaks down completely. If
 * networking crashes to an extend the internal wrapper cannot handle any more,
 * both {@link #register()} and {@link #runsMatch()} return <code>false</code>.
 * This is the only situation the table stops without been shut down explicitly.
 * <p>
 * The state of regular matches is indicated by {@link cardgames.control.maumau.messages.MauMauTableMessage messages}
 * the table sends to players. The beginning of each new game within a match is
 * indicated by a {@link PlayerSequence}-message {@link MauMauPlayerInterface#notify(cardgames.control.maumau.messages.MauMauTableMessage) sent to all subscribed players}
 * and the {@link MauMauTableViewer#handleMauMauTableMessage(cardgames.control.maumau.messages.MauMauTableMessage) the table's viewer}.
 * A {@link FinishedGame}-message occurs at the end of each game and the end of
 * a complete match is indicated by an other {@link FinishedMatch}-message. As
 * the table has finished a match he dismisses all players subscribed by a call
 * to their {@link MauMauPlayerInterface#leaveTable() leaveTable()}. After all
 * players have been dismissed the table resets networking and returns to
 * subscribing-state.
 * <p>
 */
@SimonRemote(value = { MauMauTableInterface.class })
public class MauMauTable extends Thread implements MauMauTableInterface {

    static final int MAU_MAU_MAX_PLAYERS_PER_SINGLE_DECK = 5;

    /**
     * Number of cards any competitor receives on every beginning of a game: <code>5</code>.
     */
    public static final int MAU_MAU_HAND_CARDS_COUNT = 5;

    /**
     * Amount of minus points that makes a player lose the current match: <code>100</code>.
     * <p>
     * Requires the table to
     * {@link #matchEndsWithMinusPointsBorn() end a match on first player bearing these minus points}.
     */
    public static final int MAU_MAU_MATCH_LOSING_MINUS_POINTS = 100;

    /**
     * Number of penalty-cards a player has to take if she/he misses a required "Mau!"-announcement: <code>1</code>.
     * <p>
     * Requires the table to
     * {@link #getRuleRequireMau() require a "Mau!" on penultimate card}.
     */
    public static final int MAU_MAU_PENALTY_CARDS_COUNT_FOR_MISSED_MAU = 1;

    /**
     * Number of penalty-cards a player has to take if she/he misses "MauMau!"-announcement: <code>2</code>.
     * <p>
     * Every player has to announce
     * {@link cardgames.control.maumau.announcements.MauMau "MauMau!"}
     * after putting her/his last card down. If a player misses this announcement
     * the table sends <code>2</code> penalty-cards.
     */
    public static final int MAU_MAU_PENALTY_CARDS_COUNT_FOR_MISSED_MAUMAU = 2;

    /**
     * Number of penalty-cards a player has to take if she/he on every unreturned <code>7</code>: <code>2</code>.
     * <p>
     * Requires the table to
     * {@link #getRuleSevenForcesSuccessorToAcceptPenaltyCardsFromTalon() treat cards with rank <code>7</code> as penalty-bearers}.
     */
    public static final int MAU_MAU_PENALTY_CARDS_COUNT_FOR_INTERMITTING_PENALTY_BEARER = 2;

    /**
     * Number of penalty-cards a player has to take if she/he intermits instead of putting down a card: <code>1</code>.
     */
    public static final int MAU_MAU_PENALTY_CARDS_COUNT_FOR_INTERMITTING_NORMAL_SERVING = 1;

    /**
     * Minutes the table waits for a player to move: <code>5</code>.
     * <p>
     * @see <a href="MauMauTableInterface.html#polling">moving</a>
     */
    public static final long MAU_MAU_MOVE_TIME = 5 * 60 * 1000;

    /**
     * Default-time slice the table sleeps while checking for a player to complete a move: <code>1</code> second.
     */
    static long SLEEP_TIME = 1111;

    static Logger LOGGER = Logger.getLogger(MauMauTable.class.getName());

    int matchLosingMinusPoints = MAU_MAU_MATCH_LOSING_MINUS_POINTS;

    int gamesToWin = -1;

    int numberOfMatchesStarted = 0;

    int port = MauMauTableInterface.SIMON_DEFAULT_PORT;

    MauMauDeck deck;

    Stack<MauMauCard> cardsTalon = new Stack<MauMauCard>();

    Stack<MauMauCard> cardsStack = new Stack<MauMauCard>();

    boolean ruleDenyJackOnJack = true;

    boolean ruleEnforceMau = true;

    boolean ruleEightSkipsSuccessor = true;

    boolean ruleEightSkippingBearsPenalty = false;

    boolean ruleSevenBearsPenaltyCards = true;

    boolean ruleWinnerIsBeginner = false;

    boolean conditionShutDownCaughtTerm = false;

    boolean conditionTerminateMatchCheaterDetected = false;

    boolean conditionTerminateMatchTalonExhausted = false;

    String conditionTerminateMatchNameOfPlayerTimedOut = null;

    long moveTime = MAU_MAU_MOVE_TIME;

    Registry simonRegistry = null;

    MauMauTableViewer viewer;

    MauMauTableMoveControl moveControl;

    MauMauTableMove move;

    /**
     * Minimal number of players that can carry out a match: <code>2</code>.
     */
    public static final int MAU_MAU_MIN_PLAYERS = 2;

    /**
     * Flag indicating that the winner of the previous game starts moving first in the next game
     */
    public static final int GAME_STARTS_WITH_WINNER = 0;

    /**
     * Flag indicating that the winner’s neighbour starts moving first in the next game
     */
    public static final int GAME_STARTS_WITH_WINNERS_SUCCESSOR = 1;

    /**
     * Initializes a newly created table with default-configuration.
     * <p>
     * @see #MauMauTable(java.lang.String, int, boolean, boolean, boolean, boolean, boolean, int, boolean, int, int, long) called constructor
     * @param tableName of this table, used to identify it on the network
     * @param numberOfPlayers count of players that can subscribe here for a match
     * @param port port to attach table to
     */
    public MauMauTable(String tableName, int numberOfPlayers, int port) {
        this(tableName, numberOfPlayers, true, true, false, true, true, MAU_MAU_MATCH_LOSING_MINUS_POINTS, true, GAME_STARTS_WITH_WINNERS_SUCCESSOR, port, MAU_MAU_MOVE_TIME);
    }

    /**
     * Initializes a newly created table with default-configuration.
     * <p>
     * @see #MauMauTable(java.lang.String, int, boolean, boolean, boolean, boolean, boolean, int, boolean, int, int, long) called constructor
     * @param tableName of this table, used to identify it on the network
     * @param numberOfPlayers count of players that can subscribe here for a match
     */
    public MauMauTable(String tableName, int numberOfPlayers) {
        this(tableName, numberOfPlayers, true, true, false, true, true, MAU_MAU_MATCH_LOSING_MINUS_POINTS, true, GAME_STARTS_WITH_WINNERS_SUCCESSOR, MauMauTableInterface.SIMON_DEFAULT_PORT, MAU_MAU_MOVE_TIME);
    }

    /**
     * Constructs a new table.
     * <p>
     * This table carries out matches. Calls to other constructors use these
     * (default) parameters:
     * <p>
     * <table border = "1">
     *  <tr>
     *   <td><code>tableName</code> &nbsp;
     *   <td><code>MauMauTable</code>
     *  </tr>
     *  <tr>
     *   <td><code>numberOfPlayers</code> &nbsp;
     *   <td>{@link #MAU_MAU_MIN_PLAYERS}: 2
     *  </tr>
     *  <tr>
     *   <td><code>denyJackOnJack</code> &nbsp;
     *   <td><code>true</code>
     *  </tr>
     *  <tr>
     *   <td><code>eightSkipsSuccessor</code> &nbsp;
     *   <td><code>true</code>
     *  </tr>
     *  <tr>
     *   <td><code>eightBearsPenaltyCard</code> &nbsp;
     *   <td><code>false</code>
     *  </tr>
     *  <tr>
     *   <td><code>sevenBearsPenaltyCards</code> &nbsp;
     *   <td><code>true</code>
     *  </tr>
     *  <tr>
     *   <td><code>requestMau</code> &nbsp;
     *   <td><code>true</code>
     *  </tr>
     *  <tr>
     *   <td><code>count</code> &nbsp;
     *   <td>{@link #MAU_MAU_MATCH_LOSING_MINUS_POINTS}: 100
     *  </tr>
     *  <tr>
     *   <td><code>takeCountToBeLosingMinusPoints</code> &nbsp;
     *   <td><code>true</code>
     *  </tr>
     *  <tr>
     *   <td><code>gameOpeningMode</code> &nbsp;
     *   <td>{@link #GAME_STARTS_WITH_WINNERS_SUCCESSOR}: the winner's successor is first to put a card in next game
     *  </tr>
     *  <tr>
     *   <td><code>movingTimeMS</code> &nbsp;
     *   <td>{@link #MAU_MAU_MOVE_TIME}: 300000 ms = 5 m
     *  </tr>
     * </table>
     * <p>
     * @param tableName of this table, used to identify it on the network
     * @param numberOfPlayers count of players that can subscribe here for a match
     * @param denyJackOnJack sets this table to deny a jack put on another jack
     * @param eightSkipsSuccessor sets this table to skip next player in line after a card with rank 8 has been put down
     * @param eightBearsPenaltyCard sets this table to send a single penalty-card to a player that does not answer a card rank 8 with the same rank
     * @param sevenBearsPenaltyCards sets this table to send two penalty-cards on every card with rank <code>7</code>
     * @param requestMau sets this table to request "Mau!" on penultimate card
     * @param count specifies a count that the first player reaches to win or bears to lose the complete match
     * @param takeCountToBeLosingMinusPoints sets <code>count</code> to denote the amount of minus points that makes a player losing the match
     * @param gameOpeningMode determines who is next when some player finishes a game
     * @param port port to attach table to
     * @param movingTimeMS milliseconds this table waits for a player to finish her / his current move
     * @see #register() registration carried out automatically
     */
    public MauMauTable(String tableName, int numberOfPlayers, boolean denyJackOnJack, boolean eightSkipsSuccessor, boolean eightBearsPenaltyCard, boolean sevenBearsPenaltyCards, boolean requestMau, int count, boolean takeCountToBeLosingMinusPoints, int gameOpeningMode, int port, long movingTimeMS) {
        super(new ThreadGroup(MauMauTable.class.getSimpleName()), tableName.trim());
        if (getName().isEmpty()) setName(MauMauTable.class.getSimpleName() + System.getProperty("user.name"));
        this.ruleDenyJackOnJack = denyJackOnJack;
        this.ruleEightSkipsSuccessor = eightSkipsSuccessor;
        if (ruleEightSkipsSuccessor) this.ruleEightSkippingBearsPenalty = eightBearsPenaltyCard;
        this.ruleSevenBearsPenaltyCards = sevenBearsPenaltyCards;
        this.ruleEnforceMau = requestMau;
        if ((port < 0) || (port > 65535)) this.port = MauMauTableInterface.SIMON_DEFAULT_PORT; else this.port = port;
        if (movingTimeMS >= 1000) this.moveTime = movingTimeMS;
        if (count > 0) {
            if (takeCountToBeLosingMinusPoints) {
                matchLosingMinusPoints = count;
                gamesToWin = -1;
            } else {
                matchLosingMinusPoints = -1;
                gamesToWin = count;
            }
        }
        moveControl = new MauMauTableMoveControl(numberOfPlayers, gameOpeningMode);
        deck = new MauMauDeck((moveControl.getSeats() / (MAU_MAU_MAX_PLAYERS_PER_SINGLE_DECK + 1)) + 1);
        cardsStack.ensureCapacity(deck.getCardsCount());
        cardsTalon.ensureCapacity(deck.getCardsCount());
        this.viewer = new ConsoleMauMauTable(this);
        currentThread().setContextClassLoader(this.getClass().getClassLoader());
        if (register()) LOGGER.info(getName() + " created, " + moveControl.getSeats() + " players expected at port " + this.port + ", " + deck.getCardsCount() + " cards prepared ..."); else {
            LOGGER.info(getName() + " failed to connect to network at port " + this.port);
            conditionShutDownCaughtTerm = true;
        }
    }

    /**
     * Sets a new viewer for this table.
     * <p>
     * A newly instantiated table just has a
     * {@link cardgames.view.maumau.ConsoleMauMauTable console}-viewer. This sets
     * another {@link cardgames.view.maumau.MauMauTableViewer viewer}.
     * <p>
     * @param v some {@link cardgames.view.maumau.MauMauTableViewer viewer}
     */
    public void setViewer(MauMauTableViewer v) {
        this.viewer = v;
    }

    /**
     * Returns the sum of all cards this table uses.
     * <p>
     * These cards are passed to the subscribed players or rest on talon /
     * stacking. The sum of all cards used in a match is returned here.
     * <p>
     * @return The sum of all cards used in a match
     */
    public int getCountOfAllCards() {
        return deck.getCardsCount();
    }

    public boolean getRuleRequireMau() {
        return ruleEnforceMau;
    }

    public boolean getRuleDeneyJackOnJack() {
        return ruleDenyJackOnJack;
    }

    public boolean getRuleEightSkipsSuccessor() {
        return ruleEightSkipsSuccessor;
    }

    public boolean getRuleSkippingEightPenalizesIntermittingPlayer() {
        return ruleEightSkippingBearsPenalty;
    }

    public boolean getRuleSevenForcesSuccessorToAcceptPenaltyCardsFromTalon() {
        return ruleSevenBearsPenaltyCards;
    }

    /**
     * Returns the number of milliseconds a player might take to finish a move before timing out
     * @return number of milliseconds a player might take to finish a move
     */
    public long getMoveTime() {
        return moveTime;
    }

    /**
     * Returns the amount of minus points bared that finishes the match.
     * <p>
     * The table might be set to end the match according to one of these both
     * criteria:
     * <p>
     * <ol>
     *  <li> Some player bears these minus points. The first player that reaches
     *       this amount is the loser, the ranking of all players is made up by
     *       their minus points. The one with the least minus points is the winner
     *       of this match.
     *       {@link #matchEndsWithMinusPointsBorn()} returns <code>true</code>.
     *  <li> Some player wins a specific count of single games. The first player
     *       that won this game is the winner of the match, the ranking of all
     *       other players is made up by the count of games they won within this
     *       match. {@link #matchEndsWithGamesWon()} returns <code>true</code>.
     * </ol>
     * If this table is <strong>not</strong> set to end the match depending on a
     * specific amount of minus points, <code>–1</code> is retruned.
     * <p>
     * @see #getMatchWinningGames()
     * @see #matchEndsWithMinusPointsBorn()
     * @return minus points bared that finish the match; if table does <strong>not</strong> end a match on a specific amount of minus points <code>–1</code> is returned
     */
    public int getMatchLosingMinusPoints() {
        return matchLosingMinusPoints;
    }

    /**
     * Test whether the match ends with a specific amount of minus points bared
     * @see #getMatchLosingMinusPoints()
     * @return <code>true</code> if bearing a specific amount of minus points ends the match; <code>false</code> otherwise
     */
    public boolean matchEndsWithMinusPointsBorn() {
        return matchLosingMinusPoints > 0;
    }

    /**
     * Returns the count of games won that finishes the match.
     * <p>
     * The table might be set to end the match according to one of these both
     * criteria:
     * <p>
     * <ol>
     *  <li> Some player bears these minus points. The first player that reaches
     *       this amount is the loser, the ranking of all players is made up by
     *       their minus points. The one with the least minus points is the winner
     *       of this match.
     *       {@link #matchEndsWithMinusPointsBorn()} returns <code>true</code>.
     *  <li> Some player wins a specific count of single games. The first player
     *       that won this game is the winner of the match, the ranking of all
     *       other players is made up by the count of games they won within this
     *       match. {@link #matchEndsWithGamesWon()} returns <code>true</code>.
     * </ol>
     * If this table is <strong>not</strong> set to end the match depending on a
     * specific count of games won, <code>–1</code> is retruned.
     * <p>
     * @see #getMatchLosingMinusPoints()
     * @see #matchEndsWithGamesWon()
     * @return count of games won that finishes the current match; if table does <strong>not</strong> end a match on a specify count of games <code>–1</code> is returned
     */
    public int getMatchWinningGames() {
        return gamesToWin;
    }

    /**
     * Test whether the match ends with a specific count of games won
     * @see #getMatchWinningGames()
     * @return <code>true</code> if winning a specific count of games ends the match; <code>false</code> otherwise
     */
    public boolean matchEndsWithGamesWon() {
        return gamesToWin > 0;
    }

    /**
     * Registers a player.
     * <p>
     * The calling {@link MauMauPlayerInterface player} is registered if
     * there is no other player with the same newPlayersName and there is still
     * {@link #offersSubscription() a seat available}. A succeeded subscription
     * automatically {@link #start() starts} the table if it is not
     * {@link #isAlive() running} yet.
     * <p>
     * @param subscribingPlayer taking a seat
     * @throws SubscriptionFailed
     */
    public void registerPlayer(MauMauPlayerInterface subscribingPlayer) throws SubscriptionFailed {
        LOGGER.info(getName() + " checks " + subscribingPlayer);
        if (moveControl.acceptsNewParticipants()) {
            MauMauTableWatchDog wd = new MauMauTableWatchDog(Thread.currentThread(), subscribingPlayer.toString(), "getName()");
            wd.start();
            String newPlayersName = null;
            try {
                newPlayersName = subscribingPlayer.getName();
                moveControl.subscribeNewParticipant(newPlayersName, subscribingPlayer);
                String s = getName() + " welcomed " + newPlayersName + ", " + moveControl.getSeatsAvailable() + " to go";
                notifyAll(new SubscriptionSucceeded(getName(), s));
                if (!isAlive()) start();
            } catch (SimonRemoteException s) {
                LOGGER.error(getName() + " failed to connect", s);
            } catch (InterruptedException i) {
                LOGGER.error(".getName()", i);
            } finally {
                wd.interrupt();
            }
        } else {
            SubscriptionFailed e = new SubscriptionFailed(getName() + " already serves " + moveControl.getSeats() + " players, please check back later");
            LOGGER.warn(getName() + " occupied", e);
            throw e;
        }
    }

    public int getCountOfPlayersCards(String name) {
        return moveControl.getPlayersHandCardCount(name);
    }

    public int getCountOfStackingsCards() {
        return cardsStack.size();
    }

    public int getCountOfTalonsCards() {
        return cardsTalon.size();
    }

    public MauMauCard getCurrentCardOnStack() {
        if (cardsStack.isEmpty()) return null; else return cardsStack.peek();
    }

    public SuitRequired getCurrentRequirement() {
        if (move == null) return null; else return move.required;
    }

    /**
     * Returns the number of players expected / seats available.
     * <p>
     * This returns the number of players that can subscribe to this table.
     * The number is set {@link #MauMauTable(java.lang.String, int, boolean, boolean, boolean, boolean, boolean, int, boolean, int, int, long) on creation}
     * and cannot be changed.
     * <p>
     * @return number of players expected / seats available
     */
    public int getNumberOfExpectedPlayers() {
        return moveControl.getSeats();
    }

    /**
     * Returns the number of players subscribed.
     * @return number of players subscribed
     */
    public int getNumberOfSubscribedPlayers() {
        return moveControl.getSeatsOccupied();
    }

    /**
     * Test whether there is still a seat available.
     * @return <code>true</code> if {@link #getNumberOfSubscribedPlayers()}<code> &lt; </code>{@link #getNumberOfExpectedPlayers()}; <code>false</code> otherwise
     */
    public boolean offersSubscription() {
        return register() && moveControl.acceptsNewParticipants();
    }

    /**
     * Forwards the provided message to all subscribed players.
     * @param m some message
     */
    public void notifyAll(MauMauTableMessage m) {
        moveControl.notifyAll(m);
        viewer.handleMauMauTableMessage(m);
        LOGGER.info(m.toString());
    }

    /**
     * Forwards the provided announcement to all subscribed players.
     * @param a some announcement
     */
    void notifyAll(MauMauPlayerAnnouncement a) {
        moveControl.notifyAll(a);
        viewer.handleMauMauPlayerAnnouncement(a);
        LOGGER.info(a.toString());
    }

    /**
     * Tests whether the table is alive.
     * <p>
     * If a table is running it offers subscriptions or carries out a match (see
     * <a href="#states">states</a> above). It gets automatically started once
     * all offered seats are occupied. If the table fails to {@link #register()}
     * on the net nobody will ever be able to subscribe. Then the table will not
     * start at all, neither automatically nor explicitly by {@link #start()}.
     * <p>
     * As the table relies on the network many things might go wrong. This method
     * checks if table is registered and therefore operational. A table is meant
     * to offer its service until it is explicitly {@link #shutDown() shut down}.
     * This test should be used instead of parent's {@link #isAlive()} for it
     * takes {@link #register()} into account.
     * <p>
     * @return <code>true</code> if table is operational; <code>false</code> otherwise
     */
    public boolean runsMatch() {
        return register() && isAlive();
    }

    boolean terminateTable() {
        return conditionShutDownCaughtTerm || (simonRegistry == null) || !simonRegistry.isRunning();
    }

    boolean terminateMatch() {
        return terminateTable() || !moveControl.isConnected() || conditionTerminateMatchCheaterDetected || (conditionTerminateMatchNameOfPlayerTimedOut != null) || conditionTerminateMatchTalonExhausted;
    }

    Long getNextToken() {
        Long token;
        do token = new Long(deck.getRandomLong()); while (moveControl.usedToken(token));
        return token;
    }

    void riffleTalon() {
        LOGGER.info(getName() + ".riffleTalon() - " + cardsTalon);
        if (cardsTalon.size() > 2) {
            MauMauCard c;
            int left = 0;
            int right = 0;
            for (int i = 0; i < (deck.getCardsCount()); i++) {
                while (left == right) {
                    left = deck.getRandomInt(cardsTalon.size());
                    right = deck.getRandomInt(cardsTalon.size());
                }
                c = cardsTalon.elementAt(left);
                cardsTalon.setElementAt(cardsTalon.elementAt(right), left);
                cardsTalon.setElementAt(c, right);
                left = right;
            }
        }
        LOGGER.info(getName() + ".riffledTalon() - " + cardsTalon);
    }

    /**
     * Services cardsTalon <strong>and</strong> cardsStack
     * @param count of cards to send to {@link #getMovingPlayer()}
     * @throws MauMauTableException if playersSequence do not play cards but take them and exhaust cardsTalon by this (abnormal) behaviour
     */
    void sendCardsFromTalonToMovingPlayer(int count) throws MauMauTableException {
        if ((cardsTalon.size() + cardsStack.size() - 1) < count) {
            conditionTerminateMatchTalonExhausted = true;
            String s = getName() + ".sendCardsFromTalonToMovingPlayer(" + count + ") - talon (" + cardsTalon.size() + ") / stack (" + cardsStack.size() + ") exhausted on move #" + move.getMoveNumber() + " while talking with " + moveControl.getMovingPlayer().getName();
            LOGGER.error(s);
            throw new MauMauTableException(s);
        }
        if (cardsTalon.size() < count) {
            LOGGER.info(getName() + ".sendCardsFromTalonToMovingPlayer(" + count + ") - talon just has " + cardsTalon.size() + " cards, stacking contains " + cardsStack.size());
            MauMauCard oldFirstCardOnStack = cardsStack.pop();
            while (!cardsStack.empty()) cardsTalon.push(cardsStack.pop());
            riffleTalon();
            cardsStack.push(oldFirstCardOnStack);
            notifyAll(new TalonRebuild(getName(), cardsTalon.size(), cardsStack.size()));
        }
        MauMauCard c;
        for (int i = 0; i < count; i++) {
            c = cardsTalon.pop();
            moveControl.getMovingPlayer().takeCardFromTalon(c);
        }
        LOGGER.info(getName() + ".sendCardsFromTalonToMovingPlayer(" + count + ") - " + count + " cards sent to " + moveControl.getMovingPlayer().getName() + ", holding " + moveControl.getMovingPlayer().handCardsCount() + " cards now");
    }

    MauMauPlayerWrapper getMovingPlayer() {
        return moveControl.getMovingPlayer();
    }

    /**
     * Checks and accepts a player's card.
     * <p>
     * This is called by some {@link MauMauPlayerInterface player} that must have
     * previously been {@link MauMauPlayerInterface#putCard(long) called} by this
     * table to optain the <code>moveToken</code> used for callback here.
     * <p>
     * If this table regards the <code>card</code> provided to be legal it accepts.
     * Otherwise it throws a {@link MauMauTableException} to reject it. This
     * exception is raised as well when the <code>moveToken</code> provided is
     * not the token that identifies the current move.
     * <p>
     * @param card a player wants to put down on this table
     * @param moveToken number the table sent to calling client
     * @throws MauMauTableException putting this <code>card</code> violates the rules or the calling player is not the one to move
     */
    public void acceptCard(MauMauCard card, long moveToken) throws MauMauTableException {
        guardToken(moveToken);
        guardCard(card);
        LOGGER.info(getName() + ".acceptCard(" + card + ", " + moveToken + ") - bound to " + getMovingPlayer() + " (" + getMovingPlayer().usedToken(moveToken) + ')');
        move.acceptCard(card, new Long(moveToken));
        cardsStack.push(card);
        moveControl.getMovingPlayer().removeCard(card);
        LOGGER.info(getName() + ".acceptCard(" + card + ", " + moveToken + ") - accepted " + card);
    }

    public void cardPut(long moveToken) throws MauMauTableException {
        guardToken(moveToken);
        LOGGER.info(getName() + ".cardPut(" + moveToken + ") - bound to " + getMovingPlayer() + " (" + getMovingPlayer().usedToken(moveToken) + ')');
        move.setSealed();
        if (move.acceptedCard()) {
            LOGGER.info(getName() + ".cardPut(" + moveToken + ") - accepted " + move.cardPutDown + " on move #" + move.getMoveNumber());
            notifyAll(new CardPutDown(moveControl.getMovingPlayer().getName(), move.cardPutDown));
            if (move.indicatedFinish != null) notifyAll(move.indicatedFinish);
            if ((move.required != null) && move.required.getAnnouncersName().equalsIgnoreCase(getMovingPlayer().getName())) notifyAll(move.required);
            if (getRuleRequireMau() && moveControl.getMovingPlayer().hasJustOneMoreCard() && !move.announcedMau()) {
                sendCardsFromTalonToMovingPlayer(MAU_MAU_PENALTY_CARDS_COUNT_FOR_MISSED_MAU);
                notifyAll(new PenaltyCardsSent(moveControl.getMovingPlayer().getName(), MAU_MAU_PENALTY_CARDS_COUNT_FOR_MISSED_MAU, "\"Mau!\" not announced"));
            }
            if (moveControl.getMovingPlayer().hasNoMoreCard() && !move.announcedMauMau()) {
                sendCardsFromTalonToMovingPlayer(MAU_MAU_PENALTY_CARDS_COUNT_FOR_MISSED_MAUMAU);
                notifyAll(new PenaltyCardsSent(moveControl.getMovingPlayer().getName(), MAU_MAU_PENALTY_CARDS_COUNT_FOR_MISSED_MAUMAU, "\"Mau Mau!\" not announced"));
            }
            if (!move.cardAcceptedIsJack()) move.dismissRequirement();
        } else {
            LOGGER.info(getName() + ".cardPut(" + moveToken + ") - intermit accepted on move #" + move.getMoveNumber());
            notifyAll(new CardIntermitted(moveControl.getMovingPlayer().getName(), move.parent.cardPutDown));
            int penaltyCardsCount = MAU_MAU_PENALTY_CARDS_COUNT_FOR_INTERMITTING_NORMAL_SERVING;
            MauMauTableMessage m = new PenaltyCardsSent(moveControl.getMovingPlayer().getName(), penaltyCardsCount, "Intermitting " + move.parent.cardPutDown);
            ;
            if (move.getPenalty() > 0) {
                penaltyCardsCount = move.getPenalty();
                m = new PenaltyCardsSent(moveControl.getMovingPlayer().getName(), penaltyCardsCount, "Intermitting " + move.parent.cardPutDown);
            } else if (move.parent.skips && !getRuleSkippingEightPenalizesIntermittingPlayer()) penaltyCardsCount = 0;
            move.cardPutDown = move.parent.cardPutDown;
            move.dismissSkipping();
            move.dismissPenalty();
            if (penaltyCardsCount > 0) {
                sendCardsFromTalonToMovingPlayer(penaltyCardsCount);
                notifyAll(m);
            }
        }
    }

    /**
     * verifies token.
     * <p>
     * If there is no game {@link #offersSubscription() started} an
     * {@link IllegalToken}-exception is fired. If the game is running and
     * the token is not valid the table considers to be attacked and shuts down.
     * <p>
     * @param token
     * @throws IllegalToken 
     */
    void guardToken(long token) throws IllegalToken {
        if (offersSubscription() || move == null || move.isSealed()) {
            IllegalToken illegalToken = new IllegalToken(getName(), token);
            LOGGER.warn(getName(), illegalToken);
            throw illegalToken;
        }
        if (!move.matchesActiveToken(token)) {
            conditionTerminateMatchCheaterDetected = true;
            IllegalToken illegalToken = new IllegalToken(getName(), token);
            LOGGER.error(getName(), illegalToken);
            throw illegalToken;
        }
    }

    /**
     * verifies card.
     * <p>
     * If there is no game {@link #offersSubscription() started} an
     * {@link IllegalCard}-exception is fired. If the game is running and the
     * moving player does not {@link MauMauPlayerWrapper#holdsCard(cardgames.model.cards.MauMauCard) hold}
     * provided card table considers to be attacked and shuts down.
     * <p>
     * @param card
     * @throws IllegalCard 
     */
    void guardCard(MauMauCard card) throws IllegalCard {
        if (offersSubscription() || move == null || move.isSealed()) {
            IllegalCard illegalCard = new IllegalCard(getName(), card);
            LOGGER.warn(getName(), illegalCard);
            throw illegalCard;
        }
        if ((moveControl == null) || (getMovingPlayer() == null) || !getMovingPlayer().holdsCard(card)) {
            conditionTerminateMatchCheaterDetected = true;
            IllegalCard illegalCard = new IllegalCard(getName(), card);
            LOGGER.error(getName(), illegalCard);
            throw illegalCard;
        }
    }

    /**
     * Accepts an player's announcement.
     * <p>
     * Requires {@link #acceptCard(cardgames.model.cards.MauMauCard, long)}
     * to succeed with same (valid) <code>moveToken</code> before this can
     * be executed.
     * <p>
     * @param an some announcement
     * @param moveToken number the table sent to calling client
     * @throws MauMauTableException if <code>moveToken</code> is invalid or table is not in appropriate state to accept any announcement
     */
    public void acceptAnnouncement(MauMauPlayerAnnouncement an, long moveToken) throws MauMauTableException {
        guardToken(moveToken);
        LOGGER.info(getName() + ".acceptAnnouncement(" + an + ", " + moveToken + ") - bound to " + getMovingPlayer() + " (" + getMovingPlayer().usedToken(moveToken) + ')');
        move.acceptAnnouncement(an, new Long(moveToken));
        LOGGER.info(getName() + ".acceptAnnouncement(" + an + ", " + moveToken + ") - accepted " + an);
    }

    /**
     * Starts table to carry out matches.
     * <p>
     * While this match is carried out, the table can be stopped by
     * {@link #shutDown() shutting down}.
     */
    public void run() {
        while (!terminateTable() && register()) {
            try {
                LOGGER.info(getName() + ".run() - waiting for " + getNumberOfExpectedPlayers() + " players on match #" + getStartedMatchesCount());
                MauMauTableWatchDog subscriptionWatchDog = new MauMauTableWatchDog(this, getName(), "suscribed()");
                while (!terminateTable() && register() && offersSubscription()) {
                    if ((moveControl.getSeatsOccupied() > 0) && !subscriptionWatchDog.isAlive()) subscriptionWatchDog.start();
                    sleep(SLEEP_TIME);
                }
                subscriptionWatchDog.interrupt();
                LOGGER.info(getName() + ".run() - got " + moveControl.getSeatsOccupied() + " subscribers");
                LOGGER.info(getName() + ".run() - initializing match #" + getStartedMatchesCount());
                initMatch();
                while (!terminateMatch() && !moveControl.matchIsDone()) {
                    LOGGER.info(getName() + ".run() - initializing game #" + getStartedGamesCount());
                    initGame();
                    do {
                        move = move.getSuccessor(getNextToken());
                        LOGGER.info(getName() + ".run() - prepared move #" + move.getMoveNumber() + ", token " + move.getActiveToken());
                        moveControl.getMovingPlayer().putCard(move.getActiveToken());
                        long timer = moveTime;
                        while (!terminateMatch() && !move.isSealed() && (timer > 0)) {
                            timer = timer - SLEEP_TIME;
                            sleep(SLEEP_TIME);
                        }
                        if (!move.isSealed()) try {
                            LOGGER.warn(getName() + ".run() - move #" + move.getMoveNumber() + " (token " + move.getActiveToken() + ") not sealed yet, timeout?");
                            cardPut(move.getActiveToken());
                        } catch (MauMauTableException m) {
                            LOGGER.error(getName() + ".run() - failed sealing move #" + move.getMoveNumber() + " (token " + move.getActiveToken() + ')', m);
                        }
                        if (moveControl.getMovingPlayer().hasNoMoreCard()) {
                            moveControl.setMovingToBeWinner();
                            notifyAll(new FinishedGame(moveControl.getMovingPlayer().getName(), moveControl.getMovingPlayer().getGamesWon(), moveControl.getMovingPlayer().getMinusPoints()));
                        } else moveControl.setNextMoving();
                    } while (!moveControl.getMovingPlayer().hasNoMoreCard() && !terminateMatch());
                }
                if (terminateMatch()) {
                    FinishedMatch f = new FinishedMatchAbnormal(getName(), numberOfMatchesStarted);
                    if (conditionTerminateMatchCheaterDetected) f = new FinishedMatchAbnormal(getName(), "Detected cheater", numberOfMatchesStarted);
                    if (conditionTerminateMatchNameOfPlayerTimedOut != null) f = new FinishedMatchAbnormal(getName(), "Player " + conditionTerminateMatchNameOfPlayerTimedOut + " timed out", numberOfMatchesStarted);
                    if (conditionTerminateMatchTalonExhausted) f = new FinishedMatchAbnormal(getName(), "Talon exhausted", numberOfMatchesStarted);
                    notifyAll(f);
                } else notifyAll(moveControl.getStatsMessage());
                LOGGER.info(getName() + ".run() - finished game #" + moveControl.startedGamesCount + ", match #" + getStartedMatchesCount() + " with " + getStartedMovesCount() + " moves in total");
                numberOfMatchesStarted++;
            } catch (InterruptedException i) {
                LOGGER.error(getName() + ".run() - failed with game #" + moveControl.startedGamesCount + ", match #" + getStartedMatchesCount() + " with " + getStartedMovesCount() + " moves in total");
                conditionShutDownCaughtTerm = true;
            } catch (OutOfMemoryError o) {
                LOGGER.error(getName() + ".run() - failed with game #" + moveControl.startedGamesCount + ", match #" + getStartedMatchesCount() + " with " + getStartedMovesCount() + " moves in total");
                conditionShutDownCaughtTerm = true;
            } finally {
                LOGGER.info(getName() + ".run() - dismissing " + moveControl.getSeatsOccupied() + " players ...");
                moveControl.reset();
            }
        }
        unregister();
    }

    /**
     * init is seperated here to enable testing
     */
    void initMatch() {
        conditionTerminateMatchCheaterDetected = false;
        conditionTerminateMatchNameOfPlayerTimedOut = null;
        conditionTerminateMatchTalonExhausted = false;
    }

    /**
     * init is seperated here to enable testing
     */
    void initGame() {
        moveControl.setUpSequence();
        notifyAll(moveControl.getSequenceMessage());
        cardsTalon.clear();
        cardsTalon.addAll(deck.getAllCards());
        riffleTalon();
        LOGGER.info("talon has " + cardsTalon.size() + " cards, these are " + cardsTalon);
        LOGGER.info("dealing out " + MAU_MAU_HAND_CARDS_COUNT + " cards to " + moveControl.getSeatsOccupied() + " subscribers");
        moveControl.sendCardsFromTalonToAllPlayers(MAU_MAU_HAND_CARDS_COUNT);
        cardsStack.clear();
        cardsStack.push(cardsTalon.pop());
        notifyAll(new CardPutDown(getName(), cardsStack.peek()));
        move = new MauMauTableMove(cardsStack.peek(), getNextToken());
        move.dismissPenalty();
        move.dismissRequirement();
        move.dismissSkipping();
        move.setSealed();
    }

    void unregister() {
        if (simonRegistry != null) {
            LOGGER.info(getName() + " shutting down " + simonRegistry.getClass().getSimpleName() + " ...");
            simonRegistry.unpublish(getName());
            simonRegistry.unbind(getName());
            simonRegistry.stop();
            simonRegistry = null;
        }
    }

    /**
     * Checks / achieves table's registration.
     * <p>
     * Any table created is set to serve a specific <code>port</code>. The
     * registration should succeed on {@link #MauMauTable(java.lang.String, int, boolean, boolean, boolean, boolean, boolean, int, boolean, int, int, long) creation}.
     * Any call of this method (re-) tries to register table on the net.
     * <code>true</code> is returned if the table already has been or actually
     * is registered and <code>false</code> if registration can not succeed for
     * any reason. Exceptions that might circumvent registration are sent to
     * {@link #LOGGER}, they don't occur here. If the table got
     * {@link #shutDown() shut down} <code>false</code> is returned.
     * <p>
     * @return <code>true</code> if the table is available on the net; <code>false</code> otherwise
     * @see #MauMauTable(java.lang.String, int, boolean, boolean, boolean, boolean, boolean, int, boolean, int, int, long) automatic registration on creation
     */
    public boolean register() {
        if (conditionShutDownCaughtTerm) return false;
        if ((simonRegistry != null) && simonRegistry.isRunning()) return true;
        try {
            simonRegistry = Simon.createRegistry(port);
            LOGGER.info("Created " + simonRegistry.getClass().getSimpleName() + " at port " + port);
        } catch (IOException i) {
            LOGGER.error("Failed to create registry at port " + port, i);
            return false;
        }
        try {
            simonRegistry.bindAndPublish(getName(), this);
            LOGGER.info(getName() + " bound to " + simonRegistry.getClass().getSimpleName());
            return true;
        } catch (NameBindingException n) {
            LOGGER.error("Failed to attach " + getName() + " to port " + port, n);
            return false;
        }
    }

    /**
     * Shuts down table and detaches it from network.
     * @see #shutDown() shutting down
     * @throws Throwable
     */
    public void finalize() throws Throwable {
        shutDown();
        moveControl.reset();
        unregister();
        super.finalize();
    }

    /**
     * Terminates current match and shuts down this table.
     */
    public void shutDown() {
        if (!conditionShutDownCaughtTerm) {
            LOGGER.info(getName() + " shutting down " + getClass().getSimpleName() + " ...");
            conditionShutDownCaughtTerm = true;
            try {
                interrupt();
            } catch (SecurityException s) {
                LOGGER.error(getName() + " did not wake up", s);
            }
        }
    }

    /**
     * Returns the number of moves started within current game.
     * @return number of moves started within current game
     */
    public int getStartedMovesCount() {
        return moveControl.startedMovesCount;
    }

    /**
     * Returns the number of games started within current match.
     * @return number of games started within current match
     */
    public int getStartedGamesCount() {
        return moveControl.startedGamesCount;
    }

    /**
     * Returns the number of started matches.
     * <p>
     * The table offers matches until it catches a {@link #shutDown() term-signal}.
     * @return number of started matches
     */
    public int getStartedMatchesCount() {
        return numberOfMatchesStarted;
    }

    /**
     * Sets up default-logging.
     * <p>
     * Sets logging to console on error-level. Notices and warnings will not be promoted.
     */
    public static void setUpLogger() {
        LOGGER.setLevel(Level.ALL);
    }

    /**
     * Returns static logger
     * @return classe's logger
     */
    public static Logger getLogger() {
        return LOGGER;
    }

    /**
     * Sets static logger
     * @param logger to use
     */
    public static void setLogger(Logger logger) {
        LOGGER = logger;
    }

    class MauMauTableWatchDog extends Thread {

        Thread watch;

        public MauMauTableWatchDog(Thread threadToWatch, String playersName, String operationToWatch) {
            super(threadToWatch.getThreadGroup(), playersName + '.' + operationToWatch);
            watch = threadToWatch;
        }

        public void run() {
            try {
                LOGGER.info(this.getName() + ", " + (moveTime / 1000) + " seconds to move ...");
                sleep(moveTime);
                watch.interrupt();
                LOGGER.warn(this.getName() + " interrupted " + watch.getName());
            } catch (InterruptedException i) {
                LOGGER.info(this.getName() + " stopped");
            }
        }
    }

    /**
     * Table's helper to control players moving.
     * <p>
     * As the table carries out a match it uses instances of this class to control
     * the sequence of players' moves and answer the question: Who is next? The
     * answer becomes most important when a single game is finished and next game
     * starts.
     * <p>
     * The second task of this control is to accept some remote-player's
     * {@link MauMauTable#registerPlayer(cardgames.control.maumau.MauMauPlayerInterface) subscription-request}
     * and set up that player's {@link MauMauPlayerWrapper wrapper} for hosting table.
     */
    class MauMauTableMoveControl {

        int gameStartingMode = GAME_STARTS_WITH_WINNERS_SUCCESSOR;

        MauMauPlayerWrapper[] players;

        int indexOfPlayerMoving = 0;

        int indexOfPlayerWonLastGame = 0;

        int indexOfNextSubscriberToAdd = 0;

        int startedGamesCount = 0;

        int startedMovesCount = 0;

        public MauMauTableMoveControl(int participants, int startGameMode) {
            if (startGameMode != GAME_STARTS_WITH_WINNERS_SUCCESSOR) gameStartingMode = GAME_STARTS_WITH_WINNER;
            if (participants < MAU_MAU_MIN_PLAYERS) players = new MauMauPlayerWrapper[MAU_MAU_MIN_PLAYERS]; else players = new MauMauPlayerWrapper[participants];
        }

        public MauMauTableMoveControl(int participants) {
            this(participants, GAME_STARTS_WITH_WINNERS_SUCCESSOR);
        }

        public synchronized void reset() {
            for (int i = 0; i < indexOfNextSubscriberToAdd; i++) {
                players[i].leaveTable();
                players[i] = null;
            }
            indexOfPlayerMoving = 0;
            indexOfPlayerWonLastGame = 0;
            indexOfNextSubscriberToAdd = 0;
            startedGamesCount = 0;
            startedMovesCount = 0;
        }

        public boolean winnerStartsNextGame() {
            return (gameStartingMode == GAME_STARTS_WITH_WINNER);
        }

        public boolean winnersSuccessorStartsNextGame() {
            return (gameStartingMode == GAME_STARTS_WITH_WINNERS_SUCCESSOR);
        }

        public boolean acceptsNewParticipants() {
            return indexOfNextSubscriberToAdd < players.length;
        }

        public void subscribeNewParticipant(String name, MauMauPlayerInterface playerInterface) throws SubscriptionFailed {
            if (!acceptsNewParticipants()) throw new SubscriptionFailed(players.length + " seats occupied, no more seat available");
            for (int i = 0; i < indexOfNextSubscriberToAdd; i++) if (players[i].getName().equalsIgnoreCase(name)) throw new SubscriptionFailed("A player named \"" + name + "\" already is subscribed");
            players[indexOfNextSubscriberToAdd] = new MauMauPlayerWrapper(name, playerInterface);
            indexOfNextSubscriberToAdd++;
        }

        public int getSeats() {
            return players.length;
        }

        public int getSeatsAvailable() {
            return players.length - indexOfNextSubscriberToAdd;
        }

        public int getSeatsOccupied() {
            return indexOfNextSubscriberToAdd;
        }

        /**
         * Returns the number of cards a player holds.
         * <p>
         * The table keeps track of the subscribed players and the cards they have
         * on hand. This returns the number of cards the player with the given
         * <code>playerName</code> holds. If there is no such player at the table,
         * a {@link NoSuchPlayer}-Exception is raised.
         * <code>-1</code> is returned.
         * <p>
         * @param playerName player's newPlayersName
         * @return number of cards that player holds
         * @throws NoSuchPlayer if there is no player named <code>playerName</code>
         */
        public int getPlayersHandCardCount(String playerName) {
            for (int i = 0; i < indexOfNextSubscriberToAdd; i++) if (players[i].getName().equalsIgnoreCase(playerName)) return players[i].handCardsCount();
            return -1;
        }

        /**
         * Forwards the provided message to all subscribed players.
         * @param m some message
         */
        public void notifyAll(MauMauTableMessage m) {
            for (int i = 0; i < indexOfNextSubscriberToAdd; i++) players[i].notify(m);
        }

        /**
         * Forwards the provided announcement to all subscribed players.
         * <p>
         * An announcement will be sent if the announcer's
         * {@link MauMauPlayerAnnouncement#getAnnouncersName() newPlayersName} does
         * <strong>not</strong> equal the (remote-) player's
         * {@link MauMauPlayerWrapper#getName() newPlayersName}. The names'
         * cases are not considered.
         * <p>
         * @param a some announcement
         */
        void notifyAll(MauMauPlayerAnnouncement a) {
            for (int i = 0; i < indexOfNextSubscriberToAdd; i++) if (!a.getAnnouncersName().equalsIgnoreCase(players[i].getName())) players[i].notify(a);
        }

        public void setNextMoving() {
            indexOfPlayerMoving = (indexOfPlayerMoving + 1) % indexOfNextSubscriberToAdd;
            startedMovesCount++;
        }

        public void setMovingToBeWinner() {
            indexOfPlayerWonLastGame = indexOfPlayerMoving;
            players[indexOfPlayerWonLastGame].incGamesWon();
            for (int i = 0; i < indexOfNextSubscriberToAdd; i++) players[i].sumUpMinusPoints();
        }

        public synchronized void setUpSequence() {
            if (startedGamesCount > 0) {
                if (gameStartingMode == GAME_STARTS_WITH_WINNER) indexOfPlayerMoving = indexOfPlayerWonLastGame; else indexOfPlayerMoving = (indexOfPlayerWonLastGame + 1) % indexOfNextSubscriberToAdd;
            }
            startedGamesCount++;
        }

        /**
         * needs {@link #setUpSequence()} to be called first
         * @return description of current move-sequence
         */
        public PlayerSequence getSequenceMessage() {
            Vector<String> playersNames = new Vector<String>(indexOfNextSubscriberToAdd);
            int start = indexOfPlayerMoving;
            for (int addIndex = start; addIndex < (start + indexOfNextSubscriberToAdd); addIndex++) playersNames.add(players[addIndex % indexOfNextSubscriberToAdd].getName());
            return new PlayerSequence(getName(), playersNames);
        }

        public MauMauPlayerWrapper getMovingPlayer() {
            return players[indexOfPlayerMoving];
        }

        public void sendCardsFromTalonToMovingPlayer(int cards) {
            for (int i = 0; i < cards; i++) players[indexOfPlayerMoving].takeCardFromTalon(cardsTalon.pop());
        }

        public void sendCardsFromTalonToAllPlayers(int cards) {
            int end = cards * indexOfNextSubscriberToAdd;
            for (int i = 0; i < end; i++) players[i % indexOfNextSubscriberToAdd].takeCardFromTalon(cardsTalon.pop());
        }

        public boolean usedToken(Long token) {
            for (int i = 0; i < indexOfNextSubscriberToAdd; i++) if (players[i].usedToken(token)) return true;
            return false;
        }

        public boolean isConnected() {
            for (int i = 0; i < indexOfNextSubscriberToAdd; i++) if (!players[i].isConnected()) return false;
            return true;
        }

        public boolean matchIsDone() {
            for (int i = 0; i < indexOfNextSubscriberToAdd; i++) {
                if (matchEndsWithMinusPointsBorn() && (players[i].getMinusPoints() >= getMatchLosingMinusPoints())) return true;
                if (matchEndsWithGamesWon() && (players[i].getGamesWon() >= getMatchWinningGames())) return true;
            }
            return false;
        }

        /**
         * Called once a match - when it is done.
         * <p>
         * Bad performance for bubblesorting possible twice to provied a stable sorted array.
         */
        FinishedMatchRegular getStatsMessage() {
            MauMauPlayerWrapper[] sort = new MauMauPlayerWrapper[indexOfNextSubscriberToAdd];
            System.arraycopy(players, 0, sort, 0, sort.length);
            MauMauPlayerWrapper s;
            for (int o = 0; o < sort.length; o++) for (int i = 0; i < sort.length - o - 1; i++) if (sort[i + 1].getMinusPoints() < sort[i].getMinusPoints()) {
                s = sort[i];
                sort[i] = sort[i + 1];
                sort[i + 1] = s;
            }
            if (matchEndsWithGamesWon()) {
                for (int o = 0; o < sort.length; o++) for (int i = 0; i < sort.length - o - 1; i++) if (sort[i].getGamesWon() < sort[i + 1].getGamesWon()) {
                    s = sort[i];
                    sort[i] = sort[i + 1];
                    sort[i + 1] = s;
                }
            }
            String[] playersNames = new String[sort.length];
            int[] playersMali = new int[sort.length];
            int[] playersGames = new int[sort.length];
            for (int i = 0; i < sort.length; i++) {
                playersNames[i] = sort[i].getName();
                playersMali[i] = sort[i].getMinusPoints();
                playersGames[i] = sort[i].getGamesWon();
            }
            return new FinishedMatchRegular(getName(), playersNames, playersMali, playersGames);
        }
    }

    /**
     * Table's helper to control players moving.
     * <p>
     * As the {@link MauMauTable table} provides single players the opportunity to
     * <a href="MauMauTableInterface.html#polling">put down some card</a> it uses
     * instances of this class to check if the
     * {@link MauMauTable#acceptCard(cardgames.model.cards.MauMauCard, long) card provided} /
     * {@link MauMauTable#acceptAnnouncement(cardgames.control.maumau.announcements.MauMauPlayerAnnouncement, long) requirement or announcement made}
     * is valid.
     */
    class MauMauTableMove {

        int number = 0;

        Long activeToken;

        MauMauCard cardPutDown;

        MauMauTableMove parent;

        SuitRequired required;

        MauMauPlayerAnnouncement indicatedFinish;

        boolean skips = false;

        int penaltyCardsCount = 0;

        boolean sealed = false;

        public MauMauTableMove(MauMauCard card, Long moveToken) {
            this.activeToken = moveToken;
            this.cardPutDown = card;
        }

        public MauMauTableMove getSuccessor(Long nextToken) {
            if (!sealed) {
                String errorMessage = ".getSuccessor(" + nextToken + ") - move #" + number + " not sealed" + System.getProperty("line.separator") + "token         :\t" + activeToken + System.getProperty("line.separator") + "parent's card :\t" + parent.cardPutDown + System.getProperty("line.separator") + "requirement   :\t" + required + System.getProperty("line.separator") + "skips         :\t" + skips + System.getProperty("line.separator") + "penalty       :\t" + penaltyCardsCount + System.getProperty("line.separator") + "stacking      :\t" + cardsStack.size() + '\t' + cardsStack + System.getProperty("line.separator") + "talon         :\t" + cardsTalon.size() + '\t' + cardsTalon;
                LOGGER.error(errorMessage);
                throw new IllegalStateException(errorMessage);
            }
            MauMauTableMove successor = new MauMauTableMove(null, nextToken);
            successor.number = number + 1;
            successor.activeToken = nextToken;
            successor.parent = this;
            successor.required = required;
            successor.skips = skips;
            successor.penaltyCardsCount = penaltyCardsCount;
            return successor;
        }

        public int getMoveNumber() {
            return number;
        }

        public int getPenalty() {
            return penaltyCardsCount;
        }

        public Long getActiveToken() {
            return activeToken;
        }

        public boolean matchesActiveToken(long token) {
            return activeToken.longValue() == token;
        }

        public void acceptCard(MauMauCard card, Long token) throws MauMauTableException {
            MauMauTableException e;
            if (isSealed()) {
                e = new Revoke(".acceptCard(" + card + ", " + token + ", " + getName() + ") - already sealed");
                LOGGER.warn(e);
                throw e;
            }
            if (!activeToken.equals(token)) {
                e = new IllegalToken(getName(), token);
                LOGGER.warn(e);
                throw e;
            }
            if (acceptedCard()) {
                e = new Revoke(".acceptCard(" + card + ", " + token + ", " + getName() + ") - already accepted a card on move " + activeToken);
                LOGGER.warn(e);
                throw e;
            }
            if (!getMovingPlayer().holdsCard(card)) {
                e = new IllegalCard(getName(), card);
                LOGGER.warn(e);
                throw e;
            }
            if (getRuleDeneyJackOnJack() && parent.cardAcceptedIsJack() && card.isRankJack()) {
                e = new Revoke(".acceptCard(" + card + ", " + token + ", " + getName() + ") - pushing " + card + " onto " + parent.cardPutDown + " is not allowed");
                LOGGER.warn(e);
                throw e;
            }
            if (parent.cardAcceptedIsJack() && !card.isRankJack() && (parent.required != null) && !parent.required.meetsRequirement(card)) {
                e = new Revoke(".acceptCard(" + card + ", " + token + ", " + getName() + ") - provided " + card + " does not meet requirement: " + parent.required);
                LOGGER.warn(e);
                throw e;
            }
            if (getRuleSevenForcesSuccessorToAcceptPenaltyCardsFromTalon() && parent.cardAcceptedIsPenaltyBearer() && parent.penaltyCardsCount > 0 && !parent.cardAcceptedHasSameRank(card)) {
                e = new Revoke(".acceptCard(" + card + ", " + token + ", " + getName() + ") - provided " + card + " does not responde to " + parent.cardPutDown + " that\'s bearing " + parent.penaltyCardsCount + " penalty-cards");
                LOGGER.warn(e);
                throw e;
            }
            if (getRuleEightSkipsSuccessor() && parent.skips && parent.cardAcceptedIsSkippingSuccessor() && !parent.cardAcceptedHasSameRank(card)) {
                e = new Revoke(".acceptCard(" + card + ", " + token + ", " + getName() + ") - provided " + card + " does not responde to " + parent.cardPutDown);
                LOGGER.warn(e);
                throw e;
            }
            if ((parent != null) && (parent.required != null)) {
                if (!card.isRankJack() && !parent.required.meetsRequirement(card)) {
                    e = new Revoke(".acceptCard(" + card + ", " + token + ", " + getName() + ") - provided " + card + " does not meet requirement: " + parent.required);
                    LOGGER.warn(e);
                    throw e;
                }
            } else {
                if (!card.isRankJack() && !parent.cardAcceptedHasSameRank(card) && !parent.cardAcceptedHasSameSuit(card)) {
                    e = new Revoke(".acceptCard(" + card + ", " + token + ", " + getName() + ") - provided " + card + " can not follow " + parent.cardPutDown);
                    LOGGER.warn(e);
                    throw e;
                }
            }
            cardPutDown = card;
            skips = cardPutDown.isRankEight();
            if (cardPutDown.isRankSeven()) penaltyCardsCount = parent.penaltyCardsCount + MauMauTable.MAU_MAU_PENALTY_CARDS_COUNT_FOR_INTERMITTING_PENALTY_BEARER;
        }

        boolean cardAcceptedIsJack() {
            if (cardPutDown == null) return false; else return cardPutDown.isRankJack();
        }

        /**
         * Does not determine if there actually is a
         * {@link #getPenalty() penalty pending} for the result is evaluated
         * from checking card put down. That card can be the card provided on
         * creation and creating a move does not increase the penalty.
         */
        boolean cardAcceptedIsPenaltyBearer() {
            if (cardPutDown == null) return false; else return cardPutDown.isRankSeven();
        }

        boolean cardAcceptedIsSkippingSuccessor() {
            if (cardPutDown == null) return false; else return cardPutDown.isRankEight();
        }

        boolean cardAcceptedHasSameRank(MauMauCard card) {
            if (cardPutDown == null) return false; else return cardPutDown.sameRank(card);
        }

        boolean cardAcceptedHasSameSuit(MauMauCard card) {
            if (cardPutDown == null) return false; else return cardPutDown.sameSuit(card);
        }

        public void acceptAnnouncement(MauMauPlayerAnnouncement an, Long token) throws MauMauTableException {
            MauMauTableException e;
            if (isSealed()) {
                e = new Revoke(".acceptAnnouncement(" + an + ", " + token + ") - move already finished (sealed)");
                LOGGER.warn(e);
                throw e;
            }
            if (!activeToken.equals(token)) {
                e = new IllegalToken(getName(), token);
                LOGGER.warn(e);
                throw e;
            }
            if (!acceptedCard()) {
                e = new Revoke(".acceptAnnouncement(" + an + ", " + token + ") - no card put down so far, nothing to announce");
                LOGGER.warn(e);
                throw e;
            }
            if (!cardPutDown.isRankJack() && an instanceof SuitRequired) {
                e = new Revoke(".acceptAnnouncement(" + an + ", " + token + ") - previous jack missing, nothing to require");
                LOGGER.warn(e);
                throw e;
            }
            if (!getMovingPlayer().hasJustOneMoreCard() && an instanceof Mau) {
                e = new Revoke(".acceptAnnouncement(" + an + ", " + token + ") - " + getMovingPlayer().getName() + " still holds " + getMovingPlayer().handCardsCount() + " cards, illegal announcement");
                LOGGER.warn(e);
                throw e;
            }
            if (!getMovingPlayer().hasNoMoreCard() && an instanceof MauMau) {
                e = new Revoke(".acceptAnnouncement(" + an + ", " + token + ") - " + getMovingPlayer().getName() + " still holds " + getMovingPlayer().handCardsCount() + " cards, illegal announcement");
                LOGGER.warn(e);
                throw e;
            }
            if (an instanceof SuitRequired) {
                required = (SuitRequired) an;
                return;
            } else if (an instanceof Mau) {
                indicatedFinish = (Mau) an;
                return;
            } else if (an instanceof MauMau) {
                indicatedFinish = (MauMau) an;
                return;
            }
            String errorMessage = ".acceptAnnouncement(" + an + ", " + token + ") - move #" + number + System.getProperty("line.separator") + "parent's card :\t" + parent.cardPutDown + System.getProperty("line.separator") + "requirement   :\t" + required + System.getProperty("line.separator") + "skips         :\t" + skips + System.getProperty("line.separator") + "penalty       :\t" + penaltyCardsCount + System.getProperty("line.separator") + "stacking      :\t" + cardsStack.size() + '\t' + cardsStack + System.getProperty("line.separator") + "talon         :\t" + cardsTalon.size() + '\t' + cardsTalon;
            LOGGER.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        public void dismissPenalty() {
            penaltyCardsCount = 0;
        }

        public void dismissSkipping() {
            skips = false;
        }

        public void dismissRequirement() {
            required = null;
        }

        public boolean acceptedCard() {
            return cardPutDown != null;
        }

        public boolean isSealed() {
            return sealed;
        }

        synchronized void setSealed() {
            if (sealed) {
                String errorMessage = ".setSealed() - move #" + number + " already is sealed" + System.getProperty("line.separator") + "token         :\t" + activeToken + System.getProperty("line.separator") + "card          :\t" + cardPutDown + System.getProperty("line.separator") + "parent's card :\t" + parent.cardPutDown + System.getProperty("line.separator") + "requirement   :\t" + required + System.getProperty("line.separator") + "skips         :\t" + skips + System.getProperty("line.separator") + "penalty       :\t" + penaltyCardsCount + System.getProperty("line.separator") + "stacking      :\t" + cardsStack.size() + '\t' + cardsStack + System.getProperty("line.separator") + "talon         :\t" + cardsTalon.size() + '\t' + cardsTalon;
                LOGGER.error(errorMessage);
                throw new IllegalStateException(errorMessage);
            }
            sealed = true;
        }

        public boolean hasRequirement() {
            return required != null;
        }

        public boolean announcedMau() {
            return ((indicatedFinish != null) && (indicatedFinish instanceof Mau));
        }

        public boolean announcedMauMau() {
            return ((indicatedFinish != null) && (indicatedFinish instanceof MauMau));
        }
    }

    /**
     * This proxy capsules <code>MauMauPlayer</code>'s networking.
     * <p>
     * This proxy wraps the remote player's {@link MauMauPlayerInterface interface}
     * and catches all exceptions networking might raise. These exceptions are logged
     * to table's {@link MauMauTable#LOGGER static LOGGER}.
     * <p>
     * The first exceptions toggles an internal flag and the wrapper does not
     * communicate with remote player any more. After an exception is caught
     * {@link #isConnected()} returns <code>false</code> and any further calls
     * to the wrapper are not forwarded any more.
     * <p>
     * Before the wrapper
     * {@link #takeCardFromTalon(cardgames.model.cards.MauMauCard) passes a card}
     * to remote player that card is cloned. Later on the table
     * {@link #holdsCard(cardgames.model.cards.MauMauCard) verifies} each and every
     * card a remote player tries to put down.
     */
    class MauMauPlayerWrapper implements MauMauPlayerInterface {

        TreeSet<Long> usedToken = new TreeSet<Long>();

        Vector<MauMauCard> handCards = new Vector<MauMauCard>(MauMauDeck.DECK_SIZE);

        MauMauPlayerInterface playersInterface;

        boolean connected = true;

        String playersName;

        int gamesWon = 0;

        int minusPoints = 0;

        public MauMauPlayerWrapper(String name, MauMauPlayerInterface playerInterface) {
            this.playersInterface = playerInterface;
            this.playersName = name;
            LOGGER.info(playersName + " created with " + playerInterface);
        }

        public String toString() {
            return getClass().getSimpleName() + ' ' + playersName;
        }

        public String getName() {
            return playersName;
        }

        public boolean equals(Object o) {
            if (o == this) return true;
            if (o instanceof MauMauPlayerWrapper) return ((MauMauPlayerWrapper) o).getName().equalsIgnoreCase(playersName);
            return false;
        }

        public void notify(MauMauTableMessage m) {
            if (connected) {
                MauMauTableWatchDog wd = new MauMauTableWatchDog(Thread.currentThread(), playersName, "notify(" + m + ")");
                wd.start();
                try {
                    playersInterface.notify(m);
                } catch (SimonRemoteException s) {
                    LOGGER.error(playersName + ".notify(" + m + ")", s);
                    connected = false;
                } catch (ClassCastException c) {
                    LOGGER.error(playersName + ".notify(" + m + ")", c);
                    connected = false;
                } catch (InterruptedException i) {
                    LOGGER.error(playersName + ".notify(" + m + ")", i);
                    conditionTerminateMatchNameOfPlayerTimedOut = playersName;
                } finally {
                    wd.interrupt();
                }
            } else LOGGER.warn(playersName + ".notify(" + m + ") - not connected");
        }

        public void notify(MauMauPlayerAnnouncement a) {
            if (connected) {
                MauMauTableWatchDog wd = new MauMauTableWatchDog(Thread.currentThread(), playersName, "notify(" + a + ")");
                wd.start();
                try {
                    playersInterface.notify(a);
                } catch (SimonRemoteException s) {
                    LOGGER.error(playersName + ".notify(" + a + ")", s);
                    connected = false;
                } catch (ClassCastException c) {
                    LOGGER.error(playersName + ".notify(" + a + ")", c);
                    connected = false;
                } catch (InterruptedException i) {
                    LOGGER.error(playersName + ".notify(" + a + ")", i);
                    conditionTerminateMatchNameOfPlayerTimedOut = playersName;
                } finally {
                    wd.interrupt();
                }
            } else LOGGER.warn(playersName + ".notify(" + a + ") - not connected");
        }

        public void takeCardFromTalon(MauMauCard card) {
            if (connected) {
                MauMauTableWatchDog wd = new MauMauTableWatchDog(Thread.currentThread(), playersName, "takeCardFromTalon(" + card + ")");
                wd.start();
                handCards.add(card);
                try {
                    playersInterface.takeCardFromTalon(card);
                } catch (SimonRemoteException s) {
                    LOGGER.error(playersName + ".takeCardFromTalon(" + card + ")", s);
                    connected = false;
                } catch (ClassCastException c) {
                    LOGGER.error(playersName + ".takeCardFromTalon(" + card + ")", c);
                    connected = false;
                } catch (InterruptedException i) {
                    LOGGER.error(playersName + ".takeCardFromTalon(" + card + ")", i);
                    conditionTerminateMatchNameOfPlayerTimedOut = playersName;
                } finally {
                    wd.interrupt();
                }
            } else LOGGER.warn(playersName + ".takeCardFromTalon(" + card + ") - not connected");
        }

        public int handCardsCount() {
            return handCards.size();
        }

        public boolean usedToken(Long token) {
            return usedToken.contains(token);
        }

        public boolean isConnected() {
            return connected;
        }

        public boolean hasJustOneMoreCard() {
            return handCards.size() == 1;
        }

        public boolean hasNoMoreCard() {
            return handCards.isEmpty();
        }

        public boolean holdsCard(MauMauCard card) {
            for (MauMauCard c : handCards) if (c.isClone(card)) return true;
            return false;
        }

        public boolean removeCard(MauMauCard card) {
            int removeIndex = -1;
            for (int i = 0; i < handCards.size(); i++) if (handCards.elementAt(i).isClone(card)) removeIndex = i;
            if (removeIndex < 0) return false;
            handCards.remove(removeIndex);
            return true;
        }

        public void sumUpMinusPoints() {
            for (MauMauCard c : handCards) minusPoints = minusPoints + c.getRank();
            handCards.clear();
        }

        public void putCard(long token) {
            putCard(new Long(token));
        }

        public void putCard(Long token) {
            if (connected) {
                MauMauTableWatchDog wd = new MauMauTableWatchDog(Thread.currentThread(), playersName, "putCard(" + token + ")");
                wd.start();
                usedToken.add(token);
                try {
                    playersInterface.putCard(token);
                } catch (SimonRemoteException s) {
                    LOGGER.error(playersName + ".putCard(" + token + ")", s);
                    connected = false;
                } catch (ClassCastException c) {
                    LOGGER.error(playersName + ".putCard(" + token + ")", c);
                    connected = false;
                } catch (InterruptedException i) {
                    LOGGER.error(playersName + ".putCard(" + token + ")", i);
                    conditionTerminateMatchNameOfPlayerTimedOut = playersName;
                } finally {
                    wd.interrupt();
                }
            } else LOGGER.warn(playersName + ".putCard(" + token + ") - not connected");
        }

        public void incGamesWon() {
            gamesWon++;
        }

        public int getGamesWon() {
            return gamesWon;
        }

        public int getMinusPoints() {
            return minusPoints;
        }

        public void leaveTable() {
            handCards.clear();
            if (connected && (playersInterface != null)) {
                MauMauTableWatchDog wd = new MauMauTableWatchDog(Thread.currentThread(), playersName, "leaveTable()");
                wd.start();
                try {
                    playersInterface.leaveTable();
                    LOGGER.info(playersName + " dismissed");
                } catch (SimonRemoteException s) {
                    LOGGER.warn("Failed to dismiss " + playersName, s);
                } catch (InterruptedException i) {
                    LOGGER.error(playersName + ".leaveTable()", i);
                    conditionTerminateMatchNameOfPlayerTimedOut = playersName;
                } finally {
                    wd.interrupt();
                }
            } else LOGGER.info(playersName + " - not connected");
            playersInterface = null;
            connected = false;
        }

        public void finalize() throws Throwable {
            leaveTable();
        }
    }
}
