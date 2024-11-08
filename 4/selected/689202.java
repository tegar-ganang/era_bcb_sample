package com.san.stock.icicidirect.portfoliotracker;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.WebDriver;
import com.san.utils.DateUtil;
import com.san.utils.NotificationUtil;
import com.san.utils.PropsUtil;
import com.san.utils.WebDriverUtil;

public class PortfolioManager {

    private static Log logger = LogFactory.getLog(PortfolioManager.class);

    public void downloadPortfolio() {
        WebDriverUtil.defaultGet(PropsUtil.get("portfolio.url"));
        WebDriverUtil.defaultClickLink("Equity", 1);
        clearPreviousDownloadedCsv();
        WebDriverUtil.defaultClickLink("Summary", 1);
        WebDriverUtil.sleep();
        List<PortfolioItem> targetReachedItems = checkAndGetItemsWhoseTargetReached();
        String sellUrl = PropsUtil.get("sell.url");
        for (PortfolioItem portfolioItem : targetReachedItems) {
            String symbol = portfolioItem.getStockSymbol();
            logger.info("Selling : " + symbol + " qty: " + portfolioItem.getQty() + " profit " + portfolioItem.getUnrealizedProfitLossPercent());
            String sellStockUrl = sellUrl + symbol;
            WebDriverUtil.defaultGet(sellStockUrl);
            WebDriverUtil.sleep();
            WebDriverUtil.defaultSelectFromMultiple("TEMP", "BSE");
            WebDriverUtil.defaultSendKeysToInput("FML_QTY", portfolioItem.getQty());
            WebDriverUtil.defaultSelectFromMultiple("FML_ORD_TYP", "M");
            WebDriverUtil.defaultButtonClick("Submit");
            WebDriverUtil.sleep();
            WebDriverUtil.defaultButtonClick("Submit");
        }
        if (!targetReachedItems.isEmpty()) {
            NotificationUtil.getInstance().notify("Sold stocks", ReflectionToStringBuilder.toString(targetReachedItems, ToStringStyle.MULTI_LINE_STYLE));
        }
    }

    protected void clearPreviousDownloadedCsv() {
        String portfolioCsvFile = getCsvFilePath();
        String portfolioBackupFolder = PropsUtil.get("portfolio.backup.folder.path");
        File backupFolder = new File(portfolioBackupFolder);
        if (!backupFolder.isDirectory() && !backupFolder.exists()) {
            logger.info("Creating backup dir " + portfolioBackupFolder);
            backupFolder.mkdirs();
        }
        File csvFile = new File(portfolioCsvFile);
        if (csvFile.exists()) {
            logger.info("Moving file " + csvFile.getAbsolutePath() + " to " + portfolioBackupFolder);
            try {
                String pathname = backupFolder + "/" + DateUtil.getSimpleTimePath() + csvFile.getName();
                logger.info(pathname);
                FileUtils.copyFile(csvFile, new File(pathname));
            } catch (IOException e) {
                logger.error(e, e);
            }
            csvFile.delete();
        }
    }

    public List<PortfolioItem> checkAndGetItemsWhoseTargetReached() {
        List<PortfolioItem> portfolioItems = new ArrayList<PortfolioItem>();
        String portfolioCsvFile = getCsvFilePath();
        try {
            String portfolioContent = FileUtils.readFileToString(new File(portfolioCsvFile));
            logger.info("File content : " + portfolioContent);
            Scanner portfolioScanner = new Scanner(portfolioContent);
            portfolioScanner.nextLine();
            while (portfolioScanner.hasNextLine()) {
                String encodedQuote = portfolioScanner.nextLine();
                logger.info("Processing csv line " + encodedQuote);
                String[] decodedTokens = StringUtils.split(encodedQuote, ",");
                if (decodedTokens == null || decodedTokens.length != 10) {
                    logger.error("Expected 10 tokens. Found tokens : " + decodedTokens.length);
                    continue;
                }
                PortfolioItem portfolioItem = new PortfolioItem();
                int i = 0;
                portfolioItem.setStockSymbol(decodedTokens[i++]);
                portfolioItem.setQty(decodedTokens[i++]);
                portfolioItem.setAverageCostPrice(decodedTokens[i++]);
                portfolioItem.setCurrentMarketPrice(decodedTokens[i++]);
                portfolioItem.setPercentChangeOverPrevClose(decodedTokens[i++]);
                portfolioItem.setValueAtCost(decodedTokens[i++]);
                portfolioItem.setValueAtMarketPrice(decodedTokens[i++]);
                portfolioItem.setRealizedProfitLoss(decodedTokens[i++]);
                portfolioItem.setUnrealizedProfitLoss(decodedTokens[i++]);
                portfolioItem.setUnrealizedProfitLossPercent(decodedTokens[i++]);
                Integer targetPercent = PropsUtil.getInteger("portfolio.target.percentage");
                String profitLossPercent = portfolioItem.getUnrealizedProfitLossPercent();
                DecimalFormat format = new DecimalFormat();
                Number profitOrLoss = format.parse(profitLossPercent);
                logger.info("Profit or loss percent for " + portfolioItem.getStockSymbol() + " : " + profitOrLoss.doubleValue());
                if (profitOrLoss.doubleValue() >= targetPercent) {
                    logger.info("Target reached. Adding " + portfolioItem.getStockSymbol());
                    portfolioItems.add(portfolioItem);
                }
            }
        } catch (Exception e) {
            logger.error(e, e);
            NotificationUtil.getInstance().notify("Failed to process CSV ", "Date : " + new Date(), e);
        }
        return portfolioItems;
    }

    public String getCsvFilePath() {
        String portfolioCsvPath = PropsUtil.get("portfolio.file.path");
        String portfolioCsvFile = "";
        logger.info("Retreiving csv file from :" + portfolioCsvPath);
        File dir = new File(portfolioCsvPath);
        if (dir.isDirectory()) {
            File[] csvs = dir.listFiles(new FilenameFilter() {

                public boolean accept(File dirFile, String name) {
                    return name.toLowerCase().endsWith(".csv");
                }
            });
            for (File csv : csvs) {
                Date date = new Date(csv.lastModified());
                logger.info("File " + csv.getAbsolutePath() + " " + date.getDate());
                Date today = new Date();
                if (date.getDate() == today.getDate()) {
                    portfolioCsvFile = csv.getAbsolutePath();
                    logger.info("Todays file is : " + portfolioCsvFile);
                    break;
                }
            }
        }
        return portfolioCsvFile;
    }
}
