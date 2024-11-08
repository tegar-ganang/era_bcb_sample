package com.googlecode.jamr.plug;

public class FusionTablesPlug implements com.googlecode.jamr.spi.Outlet {

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FusionTablesPlug.class);

    private FusionTablesConfig ftc;

    private java.net.URL postUrl;

    private com.google.gdata.client.GoogleService service;

    private String jamrTable;

    public FusionTablesPlug() {
        log.trace("init");
        com.googlecode.jamr.PlugUtils pu = new com.googlecode.jamr.PlugUtils();
        com.thoughtworks.xstream.XStream xstream = new com.thoughtworks.xstream.XStream();
        java.io.File file = pu.getConfigFile("fusion");
        try {
            java.io.FileInputStream fis = new java.io.FileInputStream(file);
            ftc = (FusionTablesConfig) xstream.fromXML(fis);
        } catch (java.io.FileNotFoundException fnfe) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            fnfe.printStackTrace(pw);
            log.error(sw.toString());
            ftc = new FusionTablesConfig();
        }
        try {
            postUrl = new java.net.URL("https://www.google.com/fusiontables/api/query?encid=true");
            service = new com.google.gdata.client.GoogleService("fusiontables", "fusiontables.ApiExample");
            service.setUserCredentials(ftc.getEmail(), ftc.getPassword(), com.google.gdata.client.ClientLoginAccountType.GOOGLE);
            java.net.URL url = new java.net.URL("https://www.google.com/fusiontables/api/query?sql=" + java.net.URLEncoder.encode("SHOW TABLES", "UTF-8") + "&encid=true");
            com.google.gdata.client.Service.GDataRequest show = service.getRequestFactory().getRequest(com.google.gdata.client.Service.GDataRequest.RequestType.QUERY, url, com.google.gdata.util.ContentType.TEXT_PLAIN);
            show.execute();
            java.util.List<String[]> rows = getRows(show);
            for (int i = 0; i < rows.size(); i++) {
                String[] data = (String[]) rows.get(i);
                log.warn(data[0]);
                log.warn(data[1]);
                if (data[1].equals("jamr")) {
                    jamrTable = data[0];
                }
            }
            if (jamrTable == null) {
                com.google.gdata.client.Service.GDataRequest request = service.getRequestFactory().getRequest(com.google.gdata.client.Service.GDataRequest.RequestType.INSERT, postUrl, new com.google.gdata.util.ContentType("application/x-www-form-urlencoded"));
                java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(request.getRequestStream());
                writer.append("sql=" + java.net.URLEncoder.encode("CREATE TABLE jamr (serial:STRING, recorded_at:DATETIME, reading:NUMBER)", "UTF-8"));
                writer.flush();
                request.execute();
                java.util.List<String[]> createRows = getRows(request);
                jamrTable = createRows.get(0)[0];
                log.warn("New table id: " + jamrTable);
            }
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            log.error(sw.toString());
        }
    }

    public void received(com.googlecode.jamr.model.EncoderReceiverTransmitterMessage ert) {
        String serial = ert.getSerial();
        log.trace("received serial: " + serial);
        try {
            com.google.gdata.client.Service.GDataRequest request = service.getRequestFactory().getRequest(com.google.gdata.client.Service.GDataRequest.RequestType.INSERT, postUrl, new com.google.gdata.util.ContentType("application/x-www-form-urlencoded"));
            java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(request.getRequestStream());
            writer.append("sql=" + java.net.URLEncoder.encode("INSERT INTO " + jamrTable + " (serial, recorded_at, reading) VALUES ('" + serial + "', '" + java.text.DateFormat.getInstance().format(ert.getDate()) + "', '" + ert.getReading() + "')", "UTF-8"));
            writer.flush();
            request.execute();
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            log.error(sw.toString());
        }
    }

    private java.util.List<String[]> getRows(com.google.gdata.client.Service.GDataRequest request) throws Exception {
        java.io.InputStreamReader inputStreamReader = new java.io.InputStreamReader(request.getResponseStream());
        java.io.BufferedReader bufferedStreamReader = new java.io.BufferedReader(inputStreamReader);
        au.com.bytecode.opencsv.CSVReader reader = new au.com.bytecode.opencsv.CSVReader(bufferedStreamReader);
        java.util.List<String[]> csvLines = reader.readAll();
        java.util.List<String> columns = java.util.Arrays.asList(csvLines.get(0));
        java.util.List<String[]> rows = csvLines.subList(1, csvLines.size());
        return (rows);
    }
}
