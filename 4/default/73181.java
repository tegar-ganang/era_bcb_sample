import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Scanner;

public class ComputerActivTest {

    HashMap<String, String> trueMatching;

    static int pks = 1, spks = 1, pkt = 1, spkt = 1;

    public ComputerActivTest() {
        this.trueMatching = new HashMap<String, String>();
        this.trueMatching.put("Reading:hours", "ReadingsT:time");
        this.trueMatching.put("Reading:lread", "ReadingsT:reads");
        this.trueMatching.put("Reading:lwrite", "ReadingsT:writes");
        this.trueMatching.put("Reading:sread", "ReadingsT:sysRead");
        this.trueMatching.put("Reading:scall", "ReadingsT:sysCall");
        this.trueMatching.put("Reading:swrite", "ReadingsT:sysWrite");
        this.trueMatching.put("Reading:fork", "ReadingsT:random2");
        this.trueMatching.put("Reading:execCalls", "ReadingsT:numExecs");
        this.trueMatching.put("Reading:rchar", "ReadingsT:rchar");
        this.trueMatching.put("Reading:wchar", "ReadingsT:wchar");
        this.trueMatching.put("Reading:readingNumber", "ReadingsT:reading_id");
        this.trueMatching.put("PageReading:pgout", "PageReadingT:pageout");
        this.trueMatching.put("PageReading:ppgout", "PageReadingT:pagedout");
        this.trueMatching.put("PageReading:pgfree", "PageReadingT:pagefree");
        this.trueMatching.put("PageReading:pgscan", "PageReadingT:pagescan");
        this.trueMatching.put("PageReading:atch", "PageReadingT:pageattach");
        this.trueMatching.put("PageReading:pgin", "PageReadingT:pagein");
        this.trueMatching.put("PageReading:readingNumber", "PageReadingT:main_reading_id");
        this.trueMatching.put("PageReading:pageReadingNumber", "PageReadingT:page_reading_id");
    }

    public double precision(Matching matching) {
        double c = 0;
        for (MatchPair pair : matching.getMatches()) {
            if (trueMatching.get(pair.getSourceAtrribute()).equals(pair.getTargetAtrribute())) {
                c++;
            }
        }
        return c / trueMatching.size();
    }

    public double recall(Matching matching) {
        double c = 0;
        for (MatchPair pair : matching.getMatches()) {
            if (trueMatching.get(pair.getSourceAtrribute()).equals(pair.getTargetAtrribute())) {
                c++;
            }
        }
        return c / matching.size();
    }

    public static void add100RowsToSourceDB(int block) {
        Scanner source = null;
        Connection connection = null;
        try {
            try {
                Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
                String dbUrl = "jdbc:odbc:conn";
                connection = DriverManager.getConnection(dbUrl);
                source = new Scanner(new File("D:/Priyanka/My/Data/SourceData.csv"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            int counter = 0;
            source.nextLine();
            int cursor = block * 100;
            while (cursor != 0) {
                cursor--;
                source.nextLine();
            }
            while (source.hasNextLine() && counter < 100) {
                Scanner ls = new Scanner(source.nextLine());
                ls.useDelimiter(",");
                String query = "INSERT INTO [CompSource].[dbo].[Reading]([hours],[lread],[lwrite],[scall],[sread],[swrite],[fork],[execCalls],[rchar],[wchar],[readingNumber]) VALUES ";
                query += "( '" + ls.next() + "', " + ls.nextInt() + ", " + ls.nextInt() + ", " + ls.nextInt() + ", " + ls.nextInt() + ", " + +ls.nextInt() + ", " + ls.nextDouble() + ", " + ls.nextDouble() + ", " + ls.nextDouble() + ", " + ls.nextDouble() + ", " + pks + ")";
                String query2 = "INSERT INTO [CompSource].[dbo].[PageReading]([pgout],[ppgout],[pgfree],[pgscan],[atch],[pgin],[readingNumber],[pageReadingNumber]) VALUES";
                query2 += "( " + ls.nextDouble() + ", " + ls.nextDouble() + ", " + ls.nextDouble() + ", " + ls.nextDouble() + ", " + ls.nextDouble() + ", " + ls.nextDouble() + ", " + pks + ", " + spks + ")";
                pks++;
                spks++;
                Statement statement = ((Connection) connection).createStatement();
                try {
                    statement.executeQuery(query);
                } catch (SQLException e) {
                }
                try {
                    Statement statement2 = ((Connection) connection).createStatement();
                    statement2.executeQuery(query2);
                } catch (SQLException e) {
                }
                counter++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void add100RowsToTargetDB(int block) {
        Scanner source = null;
        Connection connection = null;
        try {
            try {
                Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
                String dbUrl = "jdbc:odbc:conn";
                connection = DriverManager.getConnection(dbUrl);
                source = new Scanner(new File("D:/Priyanka/My/Data/TargetData.csv"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            int counter = 0;
            source.nextLine();
            int cursor = block * 100;
            while (cursor != 0) {
                cursor--;
                source.nextLine();
            }
            while (source.hasNextLine() && counter < 100) {
                Scanner ls = new Scanner(source.nextLine());
                ls.useDelimiter(",");
                String query = "INSERT INTO [CompTarget].[dbo].[ReadingsT] ([time],[reads],[writes],[sysCall],[sysRead],[sysWrite],[random1],[numExecs] ,[rchar],[wchar],[reading_id]) VALUES ";
                query += "( '" + ls.next() + "', " + ls.nextInt() + ", " + ls.nextInt() + ", " + ls.nextInt() + ", " + ls.nextInt() + ", " + +ls.nextInt() + ", " + ls.nextDouble() + ", " + ls.nextDouble() + ", " + ls.nextDouble() + ", " + ls.nextDouble() + ", " + pkt + ")";
                String query2 = "INSERT INTO [CompTarget].[dbo].[PageReadingT]([pageout],[pagedout],[pagefree],[pagescan],[pageattach],[pagein],[main_reading_id],[page_reading_id]) VALUES";
                query2 += "( " + ls.nextDouble() + ", " + ls.nextDouble() + ", " + ls.nextDouble() + ", " + ls.nextDouble() + ", " + ls.nextDouble() + ", " + ls.nextDouble() + ", " + pkt + ", " + spkt + ")";
                pkt++;
                spkt++;
                Statement statement = ((Connection) connection).createStatement();
                try {
                    statement.executeQuery(query);
                } catch (SQLException e) {
                }
                try {
                    Statement statement2 = ((Connection) connection).createStatement();
                    statement2.executeQuery(query2);
                } catch (SQLException e) {
                }
                counter++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Matching instanceBased(Database source, Database target) {
        GraphMatcher instanceMatcher;
        Matching match = null;
        try {
            instanceMatcher = new GraphMatcher(source, target, false);
            match = instanceMatcher.mutualInformationBasedRanking();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return match;
    }

    public static Matching elementBased(Database source, Database target) {
        FactMatcher factMatcher = new FactMatcher(source, target, IntegratedMatcher.getPseudoFactTable(target), false);
        Relation matchedFactTable = factMatcher.getFactCandidate();
        NAryTree tree = new NAryTree(matchedFactTable, source);
        BinaryTree binaryTree = new BinaryTree(tree);
        DimensionMatcher elementMatcher = new DimensionMatcher(target, source, binaryTree, matchedFactTable, false);
        Matching match = elementMatcher.getFinalMatching();
        return match;
    }

    public static Matching integratedBased(Database source, Database target) {
        IntegratedMatcher integrated = new IntegratedMatcher(source, target);
        Matching match = integrated.calculateWeightedScores();
        return match;
    }

    public static void main(String[] args) {
        System.out.println("Adding");
        ComputerActivTest test = new ComputerActivTest();
        String header = "Tuples, Instance Based Approach, Element Based Approach, Integrated Approach";
        PrintWriter precision;
        try {
            precision = new PrintWriter(new FileWriter("D:/Priyanka/My/Data/Run2/Precision.csv"), true);
            precision.println(header);
            PrintWriter recall = new PrintWriter(new FileWriter("D:/Priyanka/My/Data/Run2/Recall.csv"), true);
            recall.println(header);
            for (int i = 0; i < 10; i++) {
                add100RowsToTargetDB(i);
                add100RowsToSourceDB(i);
                Database source = new Database("CompSource");
                Database target = new Database("CompTarget");
                int rows = target.getRelation("ReadingsT").getNumRows();
                System.out.println("Number of rows " + rows);
                Matching instance = instanceBased(source, target);
                double instancePrecision = test.precision(instance);
                System.out.println("Instance Precision: " + instancePrecision);
                double instanceRecall = test.recall(instance);
                System.out.println("Instance Recall: " + instanceRecall + "\n");
                Matching element = elementBased(source, target);
                double elementPrecision = test.precision(element);
                System.out.println("Element Precision: " + elementPrecision);
                double elementRecall = test.recall(element);
                System.out.println("Element Recall: " + elementRecall + "\n");
                Matching integrated = integratedBased(source, target);
                double integratedPrecision = test.precision(integrated);
                System.out.println("Integrated Precision: " + integratedPrecision);
                double integratedRecall = test.recall(integrated);
                System.out.println("Integrated Recall: " + integratedRecall);
                precision.println(rows + ", " + instancePrecision + ", " + elementPrecision + ", " + integratedPrecision);
                recall.println(rows + ", " + instanceRecall + ", " + elementRecall + ", " + integratedRecall);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
