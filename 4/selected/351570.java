package de.lukas;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.joda.time.LocalDate;
import de.lukas.dienstplan.Dienstplan;
import de.lukas.dienstplan.Tag;
import de.lukas.dienstplan.Tag.DayCategory;
import de.lukas.schnittstellen.ExcelReader;

/**
 *  
 *
 */
public class GeneriereKalender {

    /**
	 * @param args
	 * @throws IOException
	 */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            showHelp();
            System.exit(0);
        }
        String fileName = args[0];
        int jahr = 0;
        try {
            jahr = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            showHelp();
            System.exit(0);
        }
        System.out.println("file name = " + fileName);
        System.out.println("Jahr = " + jahr);
        addQuartalskalenderAndCalculateSollSummen(fileName + ".xls", fileName + "Quartale" + jahr + ".xls", jahr);
        System.out.println("Datei " + fileName + "Quartale" + jahr + ".xls wurde erstellt.");
    }

    public static void addQuartalskalenderAndCalculateSollSummen(String inFileName, String outFileName, int jahr) throws FileNotFoundException, IOException {
        ExcelReader er = new ExcelReader(new FileInputStream(inFileName), jahr);
        for (int i = 1; i <= 4; i++) {
            if (er.existsSheet("" + i + "_Quartal")) {
                System.out.println("Tabelle " + i + "_Quartal existiert schon!");
                System.exit(0);
            }
        }
        er.addQuartalsheet("" + jahr + "_Q1", jahr, 1);
        er.addQuartalsheet("" + jahr + "_Q2", jahr, 2);
        er.addQuartalsheet("" + jahr + "_Q3", jahr, 3);
        er.addQuartalsheet("" + jahr + "_Q4", jahr, 4);
        for (DayCategory dayCat : DayCategory.values()) {
            for (int quartal = 1; quartal <= 4; quartal++) {
                int existingValue = er.getSollSumme(quartal, dayCat);
                LocalDate date = Dienstplan.firstDate(jahr, quartal);
                LocalDate lastDate = Dienstplan.lastDate(jahr, quartal);
                int sollSumme = 0;
                while (!date.isAfter(lastDate)) {
                    Tag t = new Tag(date);
                    if (t.is(date, dayCat)) sollSumme += 1;
                    date = date.plusDays(1);
                }
                if (existingValue == 0) er.setSollSumme(quartal, dayCat, sollSumme); else if (existingValue == sollSumme) ; else {
                    er.setSollSumme(quartal, dayCat, sollSumme);
                    System.out.println("Achtung: die Sollsumme f�r " + jahr + " Q" + quartal + ", " + dayCat.name() + " ist " + sollSumme + " und �berschreibt den vorhandenen Wert " + existingValue + "!");
                }
            }
        }
        er.toFile(outFileName);
    }

    private static void showHelp() {
        System.out.println("Generiert 4 Quartalskalender in die angegebene Exel-Datei");
        System.out.println("1. Parameter: Dateiname ohne Endung");
        System.out.println("2. Parameter: Jahr(4-Stellig)");
    }

    public static void copy(String fromFile, String toFile) throws IOException {
        File inputFile = new File(fromFile);
        File outputFile = new File(toFile);
        FileReader in = new FileReader(inputFile);
        FileWriter out = new FileWriter(outputFile);
        int c;
        while ((c = in.read()) != -1) out.write(c);
        in.close();
        out.close();
    }
}
