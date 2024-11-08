import static java.lang.System.err;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Jonhnny Weslley - jonhnny@lsd.ufcg.edu.br
 *
 */
public class ScheduledRanking {

    static class RankingTask implements Runnable {

        private final String workDirectory;

        private final String rankingFile;

        public RankingTask(String workDirectory, String rankingFile) {
            this.workDirectory = workDirectory;
            this.rankingFile = rankingFile;
        }

        @Override
        public void run() {
            try {
                Ranking.main(workDirectory + "alunos.properties", workDirectory + "alunos.rank.properties", rankingFile);
            } catch (IOException e) {
                err.printf("Unable to generate ranking: %s -> %s", DateFormat.getDateInstance().format(new Date()), e);
            }
        }
    }

    static class GraphTask implements Runnable {

        final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm");

        private final String workDirectory;

        private final String rankingFile;

        public GraphTask(String workDirectory, String rankingFile) {
            this.workDirectory = workDirectory;
            this.rankingFile = rankingFile;
        }

        void copyFile(String from, String to) throws IOException {
            File destFile = new File(to);
            if (!destFile.exists()) {
                destFile.createNewFile();
            }
            FileChannel source = null;
            FileChannel destination = null;
            try {
                source = new FileInputStream(from).getChannel();
                destination = new FileOutputStream(destFile).getChannel();
                destination.transferFrom(source, 0, source.size());
            } finally {
                if (source != null) {
                    source.close();
                }
                if (destination != null) {
                    destination.close();
                }
            }
        }

        void generateGraphs() {
            try {
                Runtime.getRuntime().exec("sh " + workDirectory + "bin/generate-graph").waitFor();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            String filename = String.format(workDirectory + "data/result_%s.xml", dateFormat.format(new Date()));
            try {
                copyFile(rankingFile, filename);
            } catch (IOException e) {
                err.printf("Unable to generate graphs: %s -> %s", DateFormat.getDateInstance().format(new Date()), e);
            }
        }
    }

    public static void main(String[] args) {
        String workDir = args[0];
        String rankingFile = args[1];
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleWithFixedDelay(new RankingTask(workDir, rankingFile), 0, 1, TimeUnit.HOURS);
        service.scheduleWithFixedDelay(new GraphTask(workDir, rankingFile), 0, 1, TimeUnit.DAYS);
    }
}
