package org.fao.fenix.persistence.measurementunit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.commons.collections.list.TypedList;
import org.fao.fenix.domain.measurementunit.MeasurementUnit;
import org.fao.fenix.domain.measurementunit.MeasurementUnitConversionRate;
import org.fao.fenix.domain.measurementunit.MeasurementUnitLabel;
import org.fao.fenix.domain.measurementunit.MeasurementUnitSystem;
import org.springframework.core.io.ClassPathResource;

public class MeasurementUnitSI {

    MeasurementUnitLabelDao measurementUnitLabelDao;

    MeasurementUnitSystemDao measurementUnitSystemDao;

    MeasurementUnitDao measurementUnitDao;

    MeasurementUnitOperationDao measurementUnitOperationDao;

    MeasurementUnitConversionRateDao measurementUnitConversionRateDao;

    MeasurementUnitSystem si;

    StringTokenizer tokenizer;

    String name;

    String symbol;

    String[] nameSymbol;

    int notDoubleValueCounter = 0;

    public void initiateSI() {
        si = createSI();
        ArrayList list = createMUSymbolsList();
        int counter = 1;
        ClassPathResource classpath = new ClassPathResource("measurement_units/InternationalSystemMeasurementUnits.csv");
        try {
            BufferedReader input = new BufferedReader(new FileReader(createTmpFile(classpath.getInputStream())));
            String line = null;
            while ((line = input.readLine()) != null) {
                tokenizer = new StringTokenizer(line, ":");
                name = tokenizer.nextToken().trim();
                symbol = tokenizer.nextToken().trim();
                nameSymbol = new String[] { name, deleteBrackets(symbol) };
                MeasurementUnit one = saveOrFind(list, nameSymbol);
                name = tokenizer.nextToken().trim();
                symbol = tokenizer.nextToken().trim();
                nameSymbol = new String[] { name, deleteBrackets(symbol) };
                MeasurementUnit two = saveOrFind(list, nameSymbol);
                try {
                    double rate = Double.parseDouble(fromCommaToDot(tokenizer.nextToken().trim()));
                    saveConversionRate(one, two, rate);
                } catch (NumberFormatException e) {
                    notDoubleValueCounter++;
                }
            }
            input.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("THERE ARE " + measurementUnitDao.findAll().size() + " MU STORED");
        System.out.println("THERE ARE " + measurementUnitConversionRateDao.findAll().size() + " CONVERSION RATES STORED");
    }

    public void saveConversionRate(MeasurementUnit one, MeasurementUnit two, double rate) {
        MeasurementUnitConversionRate conversionRate = new MeasurementUnitConversionRate();
        conversionRate.setFirstUnit(one);
        conversionRate.setSecondUnit(two);
        conversionRate.setPrecision("exact");
        conversionRate.setRate(rate);
        measurementUnitConversionRateDao.save(conversionRate);
    }

    public MeasurementUnit saveOrFind(List list, String[] line) {
        MeasurementUnit unit = new MeasurementUnit();
        if (measurementUnitDao.findBySymbol(line[1]).size() == 0) unit = saveMeasurementUnit(line); else unit = (MeasurementUnit) measurementUnitDao.findBySymbol(line[1]).get(0);
        return unit;
    }

    public MeasurementUnit saveMeasurementUnit(String[] line) {
        MeasurementUnitLabel label = new MeasurementUnitLabel(line[0]);
        measurementUnitLabelDao.save(label);
        MeasurementUnit unit = new MeasurementUnit();
        unit.setLabel(label);
        unit.setSymbol(deleteBrackets(line[1]));
        unit.setConstant(1);
        unit.setMeasurementUnitSystem(si);
        unit.setMeasurementUnitSystem(createSI());
        unit.setVisible(false);
        measurementUnitDao.save(unit);
        return measurementUnitDao.findById(unit.getId());
    }

    public MeasurementUnitSystem createSI() {
        MeasurementUnitSystem si = new MeasurementUnitSystem();
        MeasurementUnitLabel label = new MeasurementUnitLabel("International system of units", "Système international d'unités", "", "", "", "");
        measurementUnitLabelDao.save(label);
        si.setLabel(label);
        measurementUnitSystemDao.save(si);
        return si;
    }

    public String deleteBrackets(String symbol) {
        symbol = symbol.replace('(', ' ');
        symbol = symbol.replace(')', ' ');
        symbol = symbol.replace('[', ' ');
        symbol = symbol.replace(']', ' ');
        symbol = symbol.trim();
        return symbol;
    }

    public String fromCommaToDot(String symbol) {
        return symbol.replace(',', '.');
    }

    public ArrayList createMUSymbolsList() {
        ArrayList map = new ArrayList();
        ClassPathResource classpath = new ClassPathResource("measurement_units/InternationalSystemMeasurementUnits.csv");
        try {
            BufferedReader input = new BufferedReader(new FileReader(createTmpFile(classpath.getInputStream())));
            String line = null;
            while ((line = input.readLine()) != null) {
                StringTokenizer tokenizer = new StringTokenizer(line, ":");
                String name = tokenizer.nextToken().trim();
                String symbol = tokenizer.nextToken().trim();
                String[] nameSymbol = new String[] { name, symbol };
                if (!map.contains(nameSymbol)) map.add(nameSymbol);
            }
            input.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    public File createTmpFile(InputStream io) {
        try {
            File file = new File(System.getProperty("java.io.tmpdir") + File.separator + "tmp.csv");
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buf = new byte[256];
            int read = 0;
            while ((read = io.read(buf)) > 0) {
                fos.write(buf, 0, read);
            }
            return file;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new File("");
    }

    public void setMeasurementUnitLabelDao(MeasurementUnitLabelDao measurementUnitLabelDao) {
        this.measurementUnitLabelDao = measurementUnitLabelDao;
    }

    public void setMeasurementUnitSystemDao(MeasurementUnitSystemDao measurementUnitSystemDao) {
        this.measurementUnitSystemDao = measurementUnitSystemDao;
    }

    public void setMeasurementUnitDao(MeasurementUnitDao measurementUnitDao) {
        this.measurementUnitDao = measurementUnitDao;
    }

    public void setMeasurementUnitOperationDao(MeasurementUnitOperationDao measurementUnitOperationDao) {
        this.measurementUnitOperationDao = measurementUnitOperationDao;
    }

    public void setMeasurementUnitConversionRateDao(MeasurementUnitConversionRateDao measurementUnitConversionRateDao) {
        this.measurementUnitConversionRateDao = measurementUnitConversionRateDao;
    }
}
