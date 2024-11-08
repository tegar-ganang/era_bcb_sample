package conversion;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import prefwork.CommonUtils;
import prefwork.datasource.MySQLConnectionProvider;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class Sushi {

    static String path = "c:\\data\\datasets\\sushi\\";

    static MySQLConnectionProvider provider = new MySQLConnectionProvider();

    private static void insertData(String table, String columns, String path) {
        initSql();
        String nextLine[];
        try {
            CSVReader reader = new CSVReader(new FileReader(path), ';', '\"');
            while ((nextLine = reader.readNext()) != null) {
                String insert = "INSERT INTO " + table + "(" + columns + ") VALUES (";
                for (String s : nextLine) insert += "\"" + s + "\"" + ", ";
                insert = insert.substring(0, insert.length() - 2);
                insert += ") ";
                try {
                    Statement stat = provider.getConn().createStatement();
                    stat.executeUpdate(insert);
                    stat.close();
                } catch (SQLException e) {
                    System.out.print(java.util.Arrays.toString(nextLine));
                    e.printStackTrace();
                }
            }
            reader.close();
            try {
                provider.getConn().commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void initSql() {
        provider = new MySQLConnectionProvider();
        provider.setDb("db");
        provider.setPassword("pass");
        provider.setUserName("user");
        try {
            provider.connect();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void transformRatings(String inpath, String outpath) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(inpath));
            CSVWriter writer = new CSVWriter(new FileWriter(outpath), ';', '\"');
            String line;
            String outline[] = new String[3];
            int userId = 1;
            while ((line = in.readLine()) != null) {
                String[] ratings = line.split(" ");
                int count = 0;
                for (int i = 0; i < ratings.length; i++) {
                    if ("-1".equals(ratings[i])) continue;
                    outline[0] = Integer.toString(userId);
                    outline[1] = Integer.toString(i);
                    outline[2] = ratings[i];
                    writer.writeNext(outline);
                    count++;
                }
                userId++;
                if (count != 10) System.out.print("" + userId + ": " + count + "\n");
            }
            in.close();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void transformData(String inpathRatings, String inpathData, String outpath) {
        try {
            CSVReader inR = new CSVReader(new FileReader(inpathRatings), ';', '"');
            CSVReader inD = new CSVReader(new FileReader(inpathData), ';', '"');
            CSVWriter writer = new CSVWriter(new FileWriter(outpath), ';', '"');
            ArrayList<String[]> data = new ArrayList<String[]>();
            String s[];
            while ((s = inD.readNext()) != null) {
                data.add(s);
            }
            inD.close();
            String s2[];
            String s3[];
            while ((s = inR.readNext()) != null) {
                int sushiId = CommonUtils.objectToInteger(s[1]);
                s2 = data.get(sushiId);
                s3 = new String[s.length + s2.length - 1];
                s3[0] = s[0];
                s3[1] = s[1];
                s3[2] = s[2];
                for (int i = 1; i < s2.length; i++) {
                    s3[2 + i] = s2[i];
                }
                writer.writeNext(s3);
            }
            inR.close();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        transformData(path + "sushiRatings.csv", path + "sushiData.csv", path + "thSushi.data");
    }
}
