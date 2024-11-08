package roboResearch;

import java.io.*;
import java.sql.*;
import java.util.*;
import roboResearch.engine.*;
import roboResearch.interfaces.*;
import roboResearch.server.*;
import simonton.utils.*;
import aaronr.utils.*;

/**
 * @author Eric Simonton
 */
public class CLI implements BattleRunner.Listener {

    public static final String USAGE = String.format("USAGE: BattleRunner [options...] [config-file]%n" + "where options are any of:%n" + "  -C challenge-file%n" + "  -c challenger%n" + "  -r num-rounds%n" + "  -s num-seasons%n" + "  -S (tells roboresearch to connect to an already-running database server)%n" + "     to run that server use this command, from the database directory:%n" + "     java -cp ../hsqldb.jar org.hsqldb.Server -database.0 file:roboresearch \\%n" + "          -dbname.0 roboresearch%n" + "  -t num-threads%n" + "     if more than one, you must use run-over-server along with it%n" + "%n" + "The options on the command line override those in the config file, if specified.");

    private final List<String> EXTRA_ARGS = Arrays.asList("-nodisplay");

    public static void main(String[] args) throws SQLException, UniversalException {
        new CLI(args);
    }

    private ChallengeRunSpecification spec;

    private boolean runOverServer;

    private int numThreads;

    private BattleQueue queue;

    private ScoreHistory history;

    private ChallengeResults results;

    private Stopwatch challengeTimer;

    public CLI(String[] args) throws SQLException, UniversalException {
        parseCommandLine(args);
        Database database;
        if (runOverServer) {
            database = new Database(Database.SERVER_CONNECTION);
        } else {
            database = new Database(Database.IN_MEMORY_CONNECTION);
        }
        queue = new FIFOBattleQueue(database, null);
        history = new LocalScoreHistory(database);
        results = new ChallengeResults(spec);
        history.feedResultsAndAddListener(results, spec);
        run();
    }

    public void run() throws UniversalException {
        challengeTimer = new Stopwatch();
        queue.schedule(spec, null, null);
        Battle nextBattle = chooseNextBattle();
        queue.cancelRunningBattle(nextBattle);
        if (numThreads == 0 || nextBattle == null) {
            challengeComplete();
            return;
        }
        copyInBotJars();
        if (numThreads > 1) {
            Bot challenger = spec.getChallenger();
            Battle battle = new Battle(0, challenger, challenger);
            new BattleRunner(battle, 0, this, Constants.ROBOCODE_DIR, EXTRA_ARGS).run();
        }
        for (int i = 0; i < numThreads; i++) {
            maybeSpawnThread(1 + i);
        }
    }

    public synchronized void battleComplete(BattleResults results, BattleRunner source) {
        Battle battle = source.getBattle();
        if (battle.numRounds == 0) {
            return;
        }
        try {
            if (results == null) {
                System.err.format("Thead %d: battle was terminated%n", source.getId());
            } else {
                queue.cancelRunningBattle(battle);
                history.submitResults(battle, results);
                Score botScore = this.results.getBotScore(getCompetitor(battle));
                System.out.format("Thread %d: Result %s vs. %s: %.2f (battle %d), took %s\n", source.getId(), battle.getBots()[0], battle.getBots()[1], spec.getScore(results), botScore.getCount(), source.getTimer());
                System.out.println(WikiResults.getResults("|", this.results));
                System.out.println();
            }
            maybeSpawnThread(source.getId());
        } catch (UniversalException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void error(String message, Exception ex, BattleRunner source) {
        if (ex != null) {
            ex.printStackTrace();
        }
        if (message != null) {
            System.err.format("Thead %d: %s%n", source.getId(), message);
        }
        if (source.getBattle().numRounds == 0) {
            System.err.println("Problem running sample match, aborting");
            System.exit(-1);
        } else {
            System.err.format("Thread %d: will be terminated%n", source.getId());
        }
    }

    public void lineOutput(String line, BattleRunner source) {
    }

    private void maybeSpawnThread(int id) throws UniversalException {
        Battle nextBattle = chooseNextBattle();
        if (nextBattle == null) {
            System.out.format("Thread %d: no more battles; exiting%n", id);
            return;
        }
        Bot competitor = getCompetitor(nextBattle);
        if (competitor == null) {
            System.err.println("No competitor found");
            return;
        }
        System.out.format("Thread %d: Running %s vs. %s\n\n", id, spec.getChallenger(), competitor);
        new Thread(new BattleRunner(nextBattle, id, this, Constants.ROBOCODE_DIR, EXTRA_ARGS)).start();
    }

    private Battle chooseNextBattle() throws UniversalException {
        return queue.getBattleToRun("1", null, null, null);
    }

    private void challengeComplete() throws UniversalException {
        System.out.println("Challenge complete, took " + challengeTimer);
        System.out.println();
        System.out.println("Final Results");
        System.out.println("-------------");
        System.out.println("|-");
        System.out.println(WikiResults.getResults("||", results));
    }

    private void copyInBotJars() {
        Set<String> copyJars = new HashSet<String>();
        copyJars.add(spec.getChallenger().getJarName());
        for (Battle battle : spec.getSeasonBattles()) {
            copyJars.add(getCompetitor(battle).getJarName());
        }
        File destDir = new File(Constants.ROBOCODE_DIR, "robots");
        for (String jarName : copyJars) {
            File fromJarFile = new File("robocode_bots", jarName);
            if (fromJarFile.exists()) {
                File toJarFile = new File(destDir, jarName);
                try {
                    FileUtils.copyFile(fromJarFile, toJarFile);
                } catch (IOException e1) {
                    System.err.println("Could not copy JAR file: " + jarName);
                } finally {
                    if (toJarFile.exists()) {
                        toJarFile.deleteOnExit();
                    }
                }
            }
        }
    }

    private Bot getCompetitor(Battle battle) {
        return battle.getAnotherBot(spec.getChallenger());
    }

    private void parseCommandLine(String[] args) throws SQLException {
        try {
            String specPath = null;
            String challengePath = null;
            String challengerName = null;
            int rounds = -1;
            int seasons = -1;
            int threads = 1;
            for (int i = 0; i < args.length; ) {
                String flag = args[i++];
                if (flag.equals("-C")) {
                    challengePath = args[i++];
                } else if (flag.equals("-c")) {
                    challengerName = args[i++];
                } else if (flag.equals("-r")) {
                    rounds = Integer.parseInt(args[i++]);
                } else if (flag.equals("-s")) {
                    seasons = Integer.parseInt(args[i++]);
                } else if (flag.equals("-S")) {
                    runOverServer = true;
                } else if (flag.equals("-t")) {
                    threads = Integer.parseInt(args[i++]);
                } else if (flag.equals("-h") || flag.equals("--help")) {
                    System.out.println(USAGE);
                    System.exit(0);
                } else if (i == args.length) {
                    specPath = flag;
                } else {
                    throw new Exception(String.format("i = %d, command line = %s", i, Arrays.toString(args)));
                }
            }
            if (specPath == null) {
                if (challengePath == null) {
                    throw new Exception("If you do not specify a run configuration, you must specify a challenge.");
                }
                if (challengerName == null) {
                    throw new Exception("If you do not specify a run configuration, you must specify a challenger");
                }
                spec = new ChallengeRunSpecification(new Bot(challengerName), Challenge.load(challengePath), seasons);
            } else {
                spec = ChallengeRunSpecification.load(specPath);
                if (challengePath != null) {
                    spec.setChallenge(Challenge.load(challengePath));
                }
                if (challengerName != null) {
                    spec.setChallenger(new Bot(challengerName));
                }
                if (seasons != -1) {
                    spec.setSeasons(seasons);
                }
            }
            if (rounds != -1) {
                spec.setNumRounds(rounds);
            }
            if (threads != -1) {
                numThreads = threads;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Bad command line arguments.");
            System.out.println(USAGE);
            System.exit(-1);
        }
        System.out.println("Challenge Specifications:");
        System.out.println("Challenge: " + spec.getChallenge().getName());
        System.out.println("Bot: " + spec.getChallenger());
        System.out.println("Alias: " + spec.getProperty("alias"));
        System.out.println("Rounds: " + spec.getNumRounds());
        System.out.println("Threads: " + numThreads);
        System.out.println("Type: " + spec.getProperty("type"));
        System.out.println("Seasons: " + spec.getSeasons());
        System.out.println();
        if (runOverServer) {
            System.out.println("(Running using server)");
        }
    }
}
