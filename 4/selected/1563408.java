package tradeWatch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Timer;
import java.util.Vector;
import javax.swing.table.AbstractTableModel;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class OrderRecordManager extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private TradeWatchCentral central;

    private String pathXML = "";

    private String fname = "orderRecordStatus";

    private Document doc;

    private Vector<ORMposition> positionList = new Vector<ORMposition>(10);

    private TimeStamp timeStamp = new TimeStamp();

    private String WLalertDateStamp = "";

    private String WLloadDateStamp = "";

    private int numberColumns = 10;

    private volatile int numberofRows = 0;

    private volatile boolean loadedXML = false;

    public OrderRecordManager(TradeWatchCentral central) {
        this.central = central;
    }

    public void updateStatus(WorkingOrder workingOrder) {
        ORMorder order = new ORMorder(workingOrder);
        this.updateStatus(order);
    }

    public void updateStatus(ORMorder order) {
        synchronized (positionList) {
            if (this.loadedXML) {
                int r = this.indexOfPosition(order.getSymbol());
                ORMposition position;
                if (r >= 0) {
                    position = this.positionList.get(r);
                    int k = position.indexOfOrder(order);
                    if (k >= 0) {
                        position.getOrder(k).update(order);
                    } else {
                        position.addOrder(order);
                    }
                } else {
                    position = new ORMposition(order.getSymbol());
                    this.positionList.add(position);
                    this.numberofRows = this.positionList.size();
                    position.addOrder(order);
                    this.forceUpdateFromPortfolio(order.getSymbol());
                }
                position.processChanges();
                fireTableDataChanged();
                saveToXML();
            }
        }
    }

    public void updateFromPortfolio(PortfolioTableRow portfolioRow) {
        synchronized (this.positionList) {
            if (this.loadedXML) {
                String symbol = portfolioRow.getContract().m_symbol;
                ORMposition position;
                ORMposition positionTemp = new ORMposition(symbol);
                int p = this.indexOfPosition(positionTemp);
                if (p == -1) {
                    position = new ORMposition(symbol);
                    this.positionList.add(position);
                    this.numberofRows = this.positionList.size();
                    this.fireTableRowsInserted(0, 0);
                    scheduleFuturePortfolioUpdate(symbol);
                } else {
                    position = this.positionList.get(p);
                    if (position.numberOfOrders() == 0) {
                        position.incrementNullUpdate();
                        if (position.getNullUpdateCount() >= 2) {
                            ORMorder order = new ORMorder(portfolioRow);
                            order.setStatus("Filled");
                            position.addOrder(order);
                        } else {
                            scheduleFuturePortfolioUpdate(symbol);
                        }
                    }
                    fireTableRowsUpdated(p, p);
                }
                position.reconcilePortfolio(portfolioRow.position);
                position.processChanges();
                saveToXML();
            }
        }
    }

    public void scheduleFuturePortfolioUpdate(String symbol) {
        OrderRecordTimerTask timerTask = new OrderRecordTimerTask(this, symbol);
        Timer timer = new Timer("ORMtimer_" + symbol);
        timer.schedule(timerTask, 5000);
    }

    public void forceUpdateFromPortfolio(String symbol) {
        int x = central.getPortfolioTable().indexOfRowSymbol(symbol);
        if (x >= 0) {
            PortfolioTableRow row = central.getPortfolioTable().getRow(x);
            this.updateFromPortfolio(row);
        }
    }

    public void forceUpdateFromPortfolioAll() {
        for (ORMposition position : this.positionList) {
            forceUpdateFromPortfolio(position.getSymbol());
        }
    }

    public boolean isInitDateToday(String symbol) {
        int ix = this.indexOfPosition(symbol);
        ORMposition position = this.getPosition(ix);
        return position.isInitDateToday();
    }

    public int indexOfPosition(String symbol) {
        ORMposition position = new ORMposition(symbol);
        return this.indexOfPosition(position);
    }

    public int indexOfPosition(ORMposition position) {
        return this.positionList.indexOf(position);
    }

    public ORMposition getPosition(int r) {
        return this.positionList.elementAt(r);
    }

    public boolean existingWLorderId(String symbol, Integer WLorderId) {
        if (symbol.equals("ALTR")) {
            int temp = 0;
            temp += 5.;
        }
        int p = this.indexOfPosition(symbol);
        if (p >= 0) {
            ORMposition position = this.positionList.get(p);
            if (position.existingWLorderId(WLorderId)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void reset() {
        this.positionList.clear();
        this.numberofRows = this.positionList.size();
        fireTableDataChanged();
    }

    public int getColumnCount() {
        return this.numberColumns;
    }

    public int getRowCount() {
        return this.numberofRows;
    }

    public String getColumnName(int c) {
        switch(c) {
            case 0:
                return "Symbol";
            case 1:
                return "Orders";
            case 2:
                return "Buy";
            case 3:
                return "Sold";
            case 4:
                return "Sell";
            case 5:
                return "Buy-Sell";
            case 6:
                return "Buy-Sold";
            case 7:
                return "Rec Port";
            case 8:
                return "Rec Sell-Sold";
            case 9:
                return "Resid";
            default:
                return null;
        }
    }

    public Object getValueAt(int r, int c) {
        ORMposition position = this.positionList.get(r);
        switch(c) {
            case 0:
                return position.getSymbol();
            case 1:
                return position.orderList.size();
            case 2:
                return position.shareCountBuy();
            case 3:
                return position.shareCountSold();
            case 4:
                return position.shareCountSell();
            case 5:
                return position.shareCountBuy() - position.shareCountSell();
            case 6:
                return position.shareCountBuy() - position.shareCountSold();
            case 7:
                return position.getReconcilePortfolio();
            case 8:
                return position.getReconcileSellSold();
            case 9:
                return position.getTallyDiff();
        }
        return 0;
    }

    public TradeWatchCentral getTradeWatchCentral() {
        return this.central;
    }

    public void setPath(String path) {
        pathXML = path;
    }

    public String fnameXML() {
        return pathXML + fname + "_" + central.getOptions().getUserName() + ".xml";
    }

    public void dailySummary() {
        try {
            String fname = pathXML + "dailySummary.txt";
            File file = new File(fname);
            FileOutputStream outStream = new FileOutputStream(file);
            PrintStream pStream = new PrintStream(outStream);
            pStream.println("TradeWatch Daily Summary: " + timeStamp.now("EEEE, MMMM dd, yyyy"));
            for (ORMposition position : this.positionList) {
                pStream.println("");
                pStream.println(position.symbol);
                pStream.println(" Current: " + (position.shareCountBuy() - position.shareCountSell()));
                int bought = 0;
                int sold = 0;
                for (ORMorder order : position.orderList) {
                    if (order.getDateTag().equals(timeStamp.now("yyyy-MM-dd"))) {
                        if (order.isBuy()) bought += order.getFilled();
                        if (order.isSell()) sold += order.getFilled();
                    }
                }
                pStream.println(" Bought: " + bought);
                pStream.println(" Sold:   " + sold);
            }
        } catch (Exception e) {
            central.inform("ORM.dailySummary: " + e.toString());
        }
    }

    public void backupXML() {
        try {
            TimeStamp timeStamp = new TimeStamp();
            String fnameIn = this.fnameXML();
            String pathBackup = this.pathXML + "\\Backup\\";
            String fnameOut = fnameIn.substring(fnameIn.indexOf(this.fname), fnameIn.length());
            fnameOut = fnameOut.substring(0, fnameOut.indexOf("xml"));
            fnameOut = pathBackup + fnameOut + timeStamp.now("yyyyMMdd-kkmmss") + ".xml";
            System.out.println("fnameIn: " + fnameIn);
            System.out.println("fnameOut: " + fnameOut);
            FileChannel in = new FileInputStream(fnameIn).getChannel();
            FileChannel out = new FileOutputStream(fnameOut).getChannel();
            in.transferTo(0, in.size(), out);
        } catch (Exception e) {
            central.inform("ORM.backupXML: " + e.toString());
        }
    }

    @SuppressWarnings("unchecked")
    public void loadFromXML() {
        central.inform("ORM.loadFromXML");
        backupXML();
        synchronized (positionList) {
            try {
                String fname = this.fnameXML();
                File xmlFile = new File(fname);
                if (xmlFile.isFile()) {
                    SAXBuilder sxb = new SAXBuilder();
                    doc = sxb.build(xmlFile);
                    Element root = doc.getRootElement();
                    Element WLalertDateStampEl = root.getChild("WLalertDateStamp");
                    Element WLloadDateStampEl = root.getChild("WLloadDateStamp");
                    this.WLalertDateStamp = WLalertDateStampEl.getText();
                    this.WLloadDateStamp = WLloadDateStampEl.getText();
                    List<Element> list = root.getChildren("ORMposition");
                    for (Element positionEl : list) {
                        ORMposition position = new ORMposition(positionEl);
                        int balance = position.shareCountBuy() - position.shareCountSell();
                        central.inform("ORM.loadFromXML: " + position.symbol + ", balance = " + balance);
                        boolean flagAdd = false;
                        if (balance <= 0) {
                            ORMorder orderLast = position.getOrderBySeqNo(position.getMaxSeqNo());
                            central.inform("ORM orderLast: " + orderLast.toString());
                            if (orderLast != null) {
                                if (orderLast.isToday()) flagAdd = true;
                            }
                        } else {
                            flagAdd = true;
                        }
                        if (flagAdd) {
                            for (ORMorder order : position.getOrderList()) {
                                order.flagAlertGenerated = false;
                            }
                            Vector<ORMorder> killList = new Vector<ORMorder>(1);
                            for (ORMorder order : position.getOrderList()) {
                                if (order.getFilled() == 0) {
                                    killList.add(order);
                                }
                            }
                            if (killList.size() > 0) {
                                for (ORMorder killOrder : killList) {
                                    central.inform("ORM.loadFromXML: kill unfilled order " + killOrder.getSymbol() + " " + killOrder.getQuantity());
                                    position.removeOrder(killOrder);
                                }
                            }
                            if (indexOfPosition(position) < 0) {
                                central.inform("ORM.loadFromXML add position: " + position.toString());
                                this.positionList.add(position);
                            } else {
                                central.inform("ORM.loadFromXML error adding position.  " + position.symbol + " is already listed in ORM.");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                central.inform("ORM.loadFromXML error: " + e.toString());
            }
            this.numberofRows = this.positionList.size();
        }
        this.fireTableDataChanged();
        this.loadedXML = true;
    }

    public void saveToXML() {
        Element root = new Element("TradeWatchOrderRecord");
        DocType dt = new DocType("TradeWatchOrderRecord");
        doc = new Document(root, dt);
        Element dateStampEl = new Element("DateStamp");
        dateStampEl.setText(timeStamp.now("yyyy-MM-dd"));
        root.addContent(dateStampEl);
        Element WLalertDateStampEl = new Element("WLalertDateStamp");
        WLalertDateStampEl.setText(this.WLalertDateStamp);
        root.addContent(WLalertDateStampEl);
        Element WLloadDateStampEl = new Element("WLloadDateStamp");
        WLloadDateStampEl.setText(this.WLloadDateStamp);
        root.addContent(WLloadDateStampEl);
        for (ORMposition position : positionList) {
            root.addContent(position.toElement());
        }
        XMLOutputter op = new XMLOutputter(Format.getPrettyFormat());
        try {
            PrintWriter out = new PrintWriter(new FileWriter(this.fnameXML()), true);
            op.output(doc, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            central.inform("ORM saveToXML: " + e.toString());
        }
    }

    public boolean verifyLoadWLalerts(String dateStampAlerts) {
        boolean bool;
        TimeStamp timeStamp = new TimeStamp();
        String dateStampNow = timeStamp.now("yyyy-MM-dd");
        if (this.WLalertDateStamp.equals(dateStampAlerts)) {
            if (this.WLloadDateStamp.equals(dateStampNow)) {
                bool = true;
            } else {
                bool = false;
            }
        } else {
            bool = true;
            this.WLalertDateStamp = dateStampAlerts;
            this.WLloadDateStamp = dateStampNow;
        }
        return bool;
    }

    public void setWLalertDateStamp(String dateStamp) {
        this.WLalertDateStamp = dateStamp;
    }

    public void setWLloadDateStamp(String dateStamp) {
        this.WLloadDateStamp = dateStamp;
    }

    public String getWLalertDateStamp() {
        return this.WLalertDateStamp;
    }

    public String getWLloadDateStamp() {
        return this.WLloadDateStamp;
    }

    class ORMposition {

        private String symbol;

        private Vector<ORMorder> orderList = new Vector<ORMorder>(2);

        private boolean reconcilePortfolio;

        private int nullUpdateCount = 0;

        private int maxSeqNo = 0;

        private int tallyDiff = 0;

        private String initDate = "";

        public ORMposition(String symbol) {
            this.symbol = symbol;
            this.initDate = timeStamp.now("yyyy-MM-dd");
        }

        @SuppressWarnings("unchecked")
        public ORMposition(Element positionEl) {
            Element symbolEl = positionEl.getChild("Symbol");
            this.symbol = symbolEl.getText();
            Element initDateEl = positionEl.getChild("InitDate");
            this.initDate = initDateEl.getText();
            List<Element> orderList = positionEl.getChildren("ORMorder");
            for (Element orderEl : orderList) {
                ORMorder order = new ORMorder(orderEl);
                this.addOrder(order);
                central.inform("ORMposition init El: " + order.toString());
            }
            this.getMaxSeqNo();
        }

        public Vector<ORMorder> getOrderList() {
            return this.orderList;
        }

        public String getSymbol() {
            return this.symbol;
        }

        public String getInitDate() {
            return this.initDate;
        }

        public void incrementAge() {
            for (ORMorder order : orderList) {
                order.incrementAge();
            }
        }

        public boolean isInitDateToday() {
            return this.initDate.equals(timeStamp.now("yyyy-MM-dd"));
        }

        public int getNullUpdateCount() {
            return this.nullUpdateCount;
        }

        public void incrementNullUpdate() {
            this.nullUpdateCount++;
        }

        public void resetNullUpdate() {
            this.nullUpdateCount = 0;
        }

        public boolean equals(Object position) {
            return this.symbol.equals(((ORMposition) position).getSymbol());
        }

        public int numberOfOrders() {
            return this.orderList.size();
        }

        public int indexOfOrder(ORMorder order) {
            return this.orderList.indexOf(order);
        }

        public boolean existingWLorderId(Integer WLorderId) {
            for (ORMorder order : this.orderList) {
                if (order.getWLorderId().equals(WLorderId)) {
                    return true;
                }
            }
            return false;
        }

        public ORMorder getOrder(int k) {
            return orderList.get(k);
        }

        public ORMorder getOrderBySeqNo(int q) {
            for (ORMorder order : this.orderList) {
                if (order.getSeqNo().equals(q)) {
                    return order;
                }
            }
            return null;
        }

        public void addOrder(ORMorder order) {
            if (order.getSeqNo() < 0) {
                order.setSeqNo(++this.maxSeqNo);
            }
            this.orderList.add(order);
        }

        public void removeOrder(ORMorder order) {
            this.orderList.remove(order);
        }

        public int shareCountBuy() {
            int count = 0;
            for (ORMorder order : this.orderList) {
                if (order.isBuy()) {
                    count += order.getFilled();
                } else {
                }
            }
            return count;
        }

        public int shareCountSell() {
            int count = 0;
            for (ORMorder order : this.orderList) {
                if (order.isBuy()) {
                } else {
                    count += order.getFilled();
                }
            }
            return count;
        }

        public int shareCountSold() {
            int count = 0;
            for (ORMorder order : this.orderList) {
                if (order.isBuy()) {
                    count += order.getSold();
                } else {
                }
            }
            return count;
        }

        public void reconcilePortfolio(int portfolioBalance) {
            int balance = this.shareCountBuy() - this.shareCountSell();
            if (balance == portfolioBalance) {
                this.reconcilePortfolio = true;
            } else {
                this.reconcilePortfolio = false;
            }
        }

        public boolean getReconcilePortfolio() {
            if (this.orderList.size() == 0) {
                return false;
            } else {
                return this.reconcilePortfolio;
            }
        }

        public boolean getReconcileSellSold() {
            int balance = this.shareCountSell() - this.shareCountSold();
            if (this.shareCountBuy() == 0 && this.shareCountSell() == 0 && this.shareCountSold() == 0) {
                balance = -1;
            }
            if (balance == 0 && this.orderList.size() != 0) {
                return true;
            } else {
                return false;
            }
        }

        public int getTallyDiff() {
            return this.tallyDiff;
        }

        public void computeSoldOld() {
            try {
                this.resetSold();
                for (ORMorder orderSell : this.orderList) if (orderSell.isSell()) {
                    for (ORMorder orderBuy : this.orderList) if (orderBuy.isBuy()) {
                        if (orderBuy.getFilled().equals(orderSell.getFilled()) && orderBuy.getSold().equals(0)) {
                            orderBuy.setSold(orderSell.getFilled());
                        }
                    }
                }
            } catch (Exception e) {
                central.inform("ORM.computeSold: " + this.symbol + " " + e.toString());
            }
        }

        public void computeSold() {
            try {
                this.resetSold();
                for (ORMorder orderSell : this.orderList) {
                    if (orderSell.isSell() && orderSell.getFilled() > 0) {
                        for (ORMorder orderBuy : this.orderList) {
                            if (orderBuy.isBuy() && orderBuy.getFilled() > 0) {
                                if (orderSell.getQuantity().equals(orderBuy.getQuantity()) && orderSell.getParentId().equals(orderBuy.getSeqNo())) {
                                    orderBuy.setSold(orderSell.getQuantity());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                central.inform("ORM.computeSold: " + this.symbol + " " + e.toString());
            }
        }

        public void genSellAlerts() {
            try {
                if (getReconcilePortfolio() && getReconcileSellSold()) {
                    for (ORMorder order : this.orderList) {
                        if (order.isBuy() && !order.isSold() && !order.getAlertFlag() && order.getFilled() > 0 && order.getFillPrice() > 0) {
                            central.inform("ORM.genSellAlerts: id=" + order.getOrderId());
                            boolean bool = central.generateAutoSell(order);
                            if (bool) order.setAlertFlag(true);
                        }
                    }
                }
            } catch (Exception e) {
                central.inform("ORM.genSellAlerts: " + this.symbol + " " + e.toString());
            }
        }

        public int getMaxSeqNo() {
            int maxSeqNoTemp = 0;
            int seqNo;
            for (ORMorder order : this.orderList) {
                seqNo = order.getSeqNo();
                if (seqNo > maxSeqNoTemp) maxSeqNoTemp = seqNo;
            }
            this.maxSeqNo = maxSeqNoTemp;
            return this.maxSeqNo;
        }

        public void processChanges() {
            getMaxSeqNo();
            computeSold();
            genSellAlerts();
        }

        public void resetAlertFlags() {
            for (ORMorder order : this.orderList) {
                order.setAlertFlag(false);
            }
        }

        public void resetSold() {
            for (ORMorder order : this.orderList) {
                order.setSold(0);
            }
        }

        public Element toElement() {
            Element positionEl = new Element("ORMposition");
            Element symbolEl = new Element("Symbol");
            symbolEl.setText(this.symbol);
            Element initDateEl = new Element("InitDate");
            initDateEl.setText(this.initDate);
            positionEl.addContent(symbolEl);
            positionEl.addContent(initDateEl);
            for (ORMorder order : this.orderList) {
                positionEl.addContent(order.toElement());
            }
            return positionEl;
        }

        public String toString() {
            String str = this.getSymbol() + " " + this.initDate;
            return str;
        }
    }

    class ORMorder {

        private String symbol;

        private Integer orderId;

        private Integer WLorderId;

        private String direction;

        private Integer quantity;

        private Integer quantityFilled;

        private Integer quantityRemaining;

        private Double fillPrice;

        private String status;

        private String dateTag;

        private Integer age;

        private Integer quantitySold;

        private boolean flagAlertGenerated;

        private Integer seqNo = -1;

        private Integer parentId = -1;

        private Integer strategyCode = -1;

        public ORMorder(WorkingOrder workingOrder) {
            this.symbol = workingOrder.getSymbol();
            this.orderId = workingOrder.getOrderId();
            this.WLorderId = workingOrder.getWLorderID();
            this.strategyCode = workingOrder.getStrategyCode();
            this.parentId = workingOrder.getParentId();
            if (workingOrder.getDirection().equals("BUY") || workingOrder.getDirection().equals("BOT")) {
                this.direction = "BUY";
            } else {
                this.direction = "SELL";
            }
            this.quantity = workingOrder.getQuantity();
            this.quantityFilled = workingOrder.getFilled();
            this.quantityRemaining = workingOrder.getRemaining();
            this.fillPrice = workingOrder.getFillPrice();
            this.status = workingOrder.getStatus();
            this.dateTag = timeStamp.now("yyyy-MM-dd");
            this.age = 0;
            this.quantitySold = 0;
            this.flagAlertGenerated = false;
        }

        public ORMorder(PortfolioTableRow portfolioRow) {
            this.symbol = portfolioRow.getContract().m_symbol;
            this.orderId = -1;
            this.WLorderId = -1;
            this.direction = "BUY";
            this.quantity = portfolioRow.position;
            this.quantityFilled = portfolioRow.position;
            this.quantityRemaining = 0;
            this.fillPrice = portfolioRow.averageCost;
            this.dateTag = timeStamp.now("yyyy-MM-dd");
            this.parentId = -1;
            this.age = 0;
            this.quantitySold = 0;
            this.flagAlertGenerated = false;
        }

        public ORMorder(Element orderEl) {
            Element symbolEl = orderEl.getChild("Symbol");
            Element orderIdEl = orderEl.getChild("OrderId");
            Element WLorderIdEl = orderEl.getChild("WLorderId");
            Element strategyCodeEl = orderEl.getChild("StrategyCode");
            Element parentIdEl = orderEl.getChild("ParentId");
            Element directionEl = orderEl.getChild("Direction");
            Element quantityEl = orderEl.getChild("Quantity");
            Element quantityFilledEl = orderEl.getChild("QuantityFilled");
            Element quantityRemainingEl = orderEl.getChild("QuantityRemaining");
            Element fillPriceEl = orderEl.getChild("FillPrice");
            Element statusEl = orderEl.getChild("Status");
            Element dateTagEl = orderEl.getChild("DateTag");
            Element quantitySoldEl = orderEl.getChild("QuantitySold");
            Element seqNoEl = orderEl.getChild("SeqNo");
            Element flagAlertGeneratedEl = orderEl.getChild("FlagAlertGenerated");
            this.symbol = symbolEl.getText();
            this.orderId = new Integer(orderIdEl.getText());
            this.WLorderId = new Integer(WLorderIdEl.getText());
            this.strategyCode = new Integer(strategyCodeEl.getText());
            this.parentId = new Integer(parentIdEl.getText());
            this.direction = directionEl.getText();
            this.quantity = new Integer(quantityEl.getText());
            this.quantityFilled = new Integer(quantityFilledEl.getText());
            this.quantityRemaining = new Integer(quantityRemainingEl.getText());
            this.fillPrice = new Double(fillPriceEl.getText());
            this.status = statusEl.getText();
            this.dateTag = dateTagEl.getText();
            this.quantitySold = new Integer(quantitySoldEl.getText());
            this.flagAlertGenerated = new Boolean(flagAlertGeneratedEl.getText());
            this.seqNo = new Integer(seqNoEl.getText());
            this.age = timeStamp.getWeekdaysSinceDate(this.dateTag);
        }

        public String getDateTag() {
            return this.dateTag;
        }

        public void setSeqNo(Integer seqNo) {
            this.seqNo = seqNo;
        }

        public Integer getSeqNo() {
            return this.seqNo;
        }

        public boolean isBuy() {
            return this.direction.equals("BUY");
        }

        public boolean isSell() {
            return this.direction.equals("SELL");
        }

        public boolean isSold() {
            boolean bool = this.getFilled() > 0 && this.getFilled().equals(this.getSold());
            return bool;
        }

        public boolean isToday() {
            TimeStamp timeStamp = new TimeStamp();
            return this.dateTag.equals(timeStamp.now("yyyy-MM-dd"));
        }

        public Integer getOrderId() {
            return this.orderId;
        }

        public Integer getWLorderId() {
            return this.WLorderId;
        }

        public String getSymbol() {
            return this.symbol;
        }

        public boolean equals(Object order) {
            boolean flagOrderId = this.getOrderId().equals(((ORMorder) order).getOrderId());
            return flagOrderId;
        }

        public void update(ORMorder order) {
            if (!this.getStatus().equals("Filled")) {
                this.setStatus(order.getStatus());
                if (order.getStatus().equals("Filled")) {
                    this.quantityFilled = this.quantity;
                    this.fillPrice = order.getFillPrice();
                } else {
                    this.quantityFilled = 0;
                    this.fillPrice = 0.;
                }
            }
        }

        public void incrementAge() {
            this.age++;
        }

        public Integer getAge() {
            return this.age;
        }

        public String getDirection() {
            return this.direction;
        }

        public Integer getQuantity() {
            return this.quantity;
        }

        public Integer getFilled() {
            return this.quantityFilled;
        }

        public Integer getRemaining() {
            return this.quantityRemaining;
        }

        public Double getFillPrice() {
            return this.fillPrice;
        }

        public String getStatus() {
            return this.status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getSold() {
            return this.quantitySold;
        }

        public void setSold(Integer shares) {
            this.quantitySold = shares;
        }

        public Integer getStrategyCode() {
            return this.strategyCode;
        }

        public Integer getParentId() {
            return this.parentId;
        }

        public boolean getAlertFlag() {
            return this.flagAlertGenerated;
        }

        public void setAlertFlag(boolean flag) {
            this.flagAlertGenerated = flag;
        }

        public String toString() {
            String str = "ORMorder: " + this.symbol + " " + this.direction + " id=" + this.orderId + " seqNo=" + this.seqNo + " parentId=" + this.parentId + " QuantFilled=" + this.quantityFilled + " QuantSold=" + this.quantitySold + " Status=" + this.status + " FlagAlertGen=" + this.flagAlertGenerated + " isToday=" + this.isToday();
            return str;
        }

        public Element toElement() {
            Element orderEl = new Element("ORMorder");
            Element symbolEl = new Element("Symbol");
            orderEl.addContent(symbolEl);
            symbolEl.setText(this.symbol);
            Element orderIdEl = new Element("OrderId");
            orderEl.addContent(orderIdEl);
            orderIdEl.setText(this.orderId.toString());
            Element WLorderIdEl = new Element("WLorderId");
            orderEl.addContent(WLorderIdEl);
            WLorderIdEl.setText(this.WLorderId.toString());
            Element strategyCodeEl = new Element("StrategyCode");
            orderEl.addContent(strategyCodeEl);
            strategyCodeEl.setText(this.strategyCode.toString());
            Element parentIdEl = new Element("ParentId");
            orderEl.addContent(parentIdEl);
            parentIdEl.setText(this.parentId.toString());
            Element directionEl = new Element("Direction");
            orderEl.addContent(directionEl);
            directionEl.setText(this.direction);
            Element quantityEl = new Element("Quantity");
            orderEl.addContent(quantityEl);
            quantityEl.setText(this.quantity.toString());
            Element quantityFilledEl = new Element("QuantityFilled");
            orderEl.addContent(quantityFilledEl);
            quantityFilledEl.setText(this.quantityFilled.toString());
            Element quantityRemainingEl = new Element("QuantityRemaining");
            orderEl.addContent(quantityRemainingEl);
            quantityRemainingEl.setText(this.quantityRemaining.toString());
            Element fillPriceEl = new Element("FillPrice");
            orderEl.addContent(fillPriceEl);
            fillPriceEl.setText(this.fillPrice.toString());
            Element statusEl = new Element("Status");
            orderEl.addContent(statusEl);
            statusEl.setText(this.status);
            Element dateTagEl = new Element("DateTag");
            orderEl.addContent(dateTagEl);
            dateTagEl.setText(this.dateTag);
            Element ageEl = new Element("Age");
            orderEl.addContent(ageEl);
            ageEl.setText(this.age.toString());
            Element quantitySoldEl = new Element("QuantitySold");
            orderEl.addContent(quantitySoldEl);
            quantitySoldEl.setText(this.quantitySold.toString());
            Element flagAlertGeneratedEl = new Element("FlagAlertGenerated");
            orderEl.addContent(flagAlertGeneratedEl);
            flagAlertGeneratedEl.setText(Boolean.toString(this.flagAlertGenerated));
            Element seqNoEl = new Element("SeqNo");
            orderEl.addContent(seqNoEl);
            seqNoEl.setText(this.seqNo.toString());
            return orderEl;
        }
    }
}
