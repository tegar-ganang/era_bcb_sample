package org.archive.crawler.reporting;

import java.io.PrintWriter;

/**
 * Traditional report of all ToeThread call-stacks, as often consulted
 * to diagnose live crawl issues. 
 * 
 * @contributor gojomo
 */
public class ToeThreadsReport extends Report {

    @Override
    public void write(PrintWriter writer, StatisticsTracker stats) {
        writer.print(stats.controller.getToeThreadReport());
    }

    @Override
    public String getFilename() {
        return "threads-report.txt";
    }
}
