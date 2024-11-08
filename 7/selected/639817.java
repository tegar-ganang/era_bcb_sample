package ch.olsen.servicecontainer.gwt.client;

import java.io.Serializable;
import ch.olsen.servicecontainer.commongwt.client.SCNodeWebIfcAsync;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class WebLog extends Sink {

    public static final String NAME = "Logs";

    TabPanel tabs;

    int selectedTab;

    LogPanel panels[] = new LogPanel[30];

    final WebLogIfcAsync weblog;

    public static SinkInfo init(final SCNodeWebIfcAsync sc, final WebLogIfcAsync weblog) {
        return new SinkInfo(NAME, "Read and acknowledge logs") {

            public Sink createInstance() {
                return new WebLog(sc, weblog);
            }
        };
    }

    public WebLog(final SCNodeWebIfcAsync sc, final WebLogIfcAsync weblog) {
        this.weblog = weblog;
        VerticalPanel panel = new VerticalPanel();
        panel.setWidth("100%");
        panel.setSpacing(8);
        panel.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
        tabs = new TabPanel();
        tabs.setSize("100%", "100%");
        tabs.add(new HTML(""), "new");
        createTab();
        tabs.addTabListener(new TabListener() {

            public boolean onBeforeTabSelected(SourcesTabEvents arg0, int index) {
                if (index == tabs.getWidgetCount() - 1) {
                    createTab();
                    return false;
                }
                return true;
            }

            public void onTabSelected(SourcesTabEvents arg0, int arg1) {
                panels[selectedTab].panelVisible = false;
                selectedTab = arg1;
                panels[selectedTab].panelVisible = true;
                if (panels[selectedTab].autoUpdate) panels[selectedTab].autoUpdateTimer.run();
            }
        });
        panel.add(tabs);
        initWidget(panel);
    }

    private void createTab() {
        int n = 0;
        for (n = 0; panels[n] != null; n++) ;
        panels[n] = new LogPanel(n);
        tabs.insert(panels[n], Integer.toString(n + 1), n);
        tabs.selectTab(n);
    }

    public void onShow(String restToken) {
        panels[selectedTab].panelVisible = true;
        if (panels[selectedTab].autoUpdate) panels[selectedTab].autoUpdateTimer.run();
    }

    public void onHide() {
        for (int n = 0; panels[n] != null; n++) panels[n].panelVisible = false;
    }

    static class Column {

        static final Column LEVEL = new Column("Log Level", 0);

        static final Column DATE = new Column("Date", 1);

        static final Column URI = new Column("URI", 2);

        static final Column LOGGERNAME = new Column("Logger Name", 3);

        static final Column CLASSMETHOD = new Column("Class and Method names", 4);

        static final Column MESSAGE = new Column("Message", 5);

        static final Column ACK = new Column("Ack status", 6);

        static final Column values[] = new Column[] { LEVEL, DATE, URI, LOGGERNAME, CLASSMETHOD, MESSAGE, ACK };

        final String name;

        final int ordinal;

        private Column(String name, int ordinal) {
            this.name = name;
            this.ordinal = ordinal;
        }

        ;

        public String toString() {
            return name;
        }
    }

    public class LogPanel extends Composite implements ClickListener {

        boolean panelVisible = false;

        private MenuBar topmenu;

        private VerticalPanel filtersPanel;

        private Filter filters[] = new Filter[30];

        private FlexTable table;

        boolean visibleColumns[] = new boolean[Column.values.length];

        private HTML countLabel = new HTML();

        private HTML newerButton = new HTML("<a href='javascript:;'>&lt;</a>", true);

        private HTML olderButton = new HTML("<a href='javascript:;'>&gt;</a>", true);

        private HTML newerPage = new HTML("<a href='javascript:;'>&lt;&lt;</a>", true);

        private HTML olderPage = new HTML("<a href='javascript:;'>&gt;&gt;</a>", true);

        private HTML firstPage = new HTML("<a href='javascript:;'>&Iota;&lt;&lt;</a>", true);

        private HTML lastPage = new HTML("<a href='javascript:;'>&gt;&gt;&Iota;</a>", true);

        private HorizontalPanel navBar = new HorizontalPanel();

        private int visibleLogs = 20;

        private CheckBox selects[];

        private int navBarColumn;

        private WebLogData data;

        private boolean autoUpdate = false;

        private MenuItem autoUpdateMenu;

        Timer autoUpdateTimer;

        public LogPanel(final int tabposition) {
            VerticalPanel panel;
            panel = new VerticalPanel();
            panel.setWidth("100%");
            for (int n = 0; n < Column.values.length; n++) visibleColumns[n] = true;
            visibleColumns[Column.CLASSMETHOD.ordinal] = false;
            visibleColumns[Column.LOGGERNAME.ordinal] = false;
            buildMenu(tabposition);
            panel.add(topmenu);
            filtersPanel = new VerticalPanel();
            filtersPanel.setWidth("100%");
            panel.add(filtersPanel);
            table = new FlexTable();
            table.setCellSpacing(0);
            table.setCellPadding(2);
            table.setWidth("100%");
            newerButton.addClickListener(this);
            olderButton.addClickListener(this);
            newerPage.addClickListener(this);
            olderPage.addClickListener(this);
            firstPage.addClickListener(this);
            lastPage.addClickListener(this);
            FlexTable innerNavBar = new FlexTable();
            innerNavBar.setStyleName("log-ListNavBar");
            innerNavBar.setCellSpacing(2);
            innerNavBar.setWidget(0, 0, countLabel);
            innerNavBar.getFlexCellFormatter().setWordWrap(0, 0, false);
            innerNavBar.getFlexCellFormatter().setColSpan(0, 0, 6);
            innerNavBar.setWidget(1, 0, firstPage);
            innerNavBar.setWidget(1, 1, newerPage);
            innerNavBar.setWidget(1, 2, newerButton);
            innerNavBar.setWidget(1, 3, olderButton);
            innerNavBar.setWidget(1, 4, olderPage);
            innerNavBar.setWidget(1, 5, lastPage);
            navBar.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
            navBar.add(innerNavBar);
            navBar.setWidth("100%");
            panel.add(table);
            initWidget(panel);
            setStyleName("log-List");
            getData(WebLogIfc.END, -visibleLogs);
            initTable();
            autoUpdateTimer = new Timer() {

                public void run() {
                    if (panelVisible && autoUpdate) getData(WebLogIfc.END, -visibleLogs);
                }
            };
        }

        private void addFilter() {
            disableAutoUpdate();
            int n = 0;
            for (; n < filters.length && filters[n] != null; n++) ;
            filters[n] = new Filter(this);
            filtersPanel.add(filters[n]);
        }

        public void removeFilter(Filter filter) {
            int n = 0;
            for (; n < filters.length && filters[n] != filter; n++) ;
            if (n != filters.length) {
                filtersPanel.remove(filters[n]);
                for (; n < filters.length - 1; n++) filters[n] = filters[n + 1];
            }
        }

        private void buildMenu(final int tabposition) {
            topmenu = new MenuBar();
            MenuItem closeTab = new MenuItem("Close tab", new Command() {

                public void execute() {
                    tabs.remove(tabposition);
                    panels[tabposition] = null;
                }
            });
            topmenu.addItem(closeTab);
            MenuBar logLines = new MenuBar();
            logLines.addItem("10", new Command() {

                public void execute() {
                    setLogLines(10);
                }
            });
            logLines.addItem("20", new Command() {

                public void execute() {
                    setLogLines(20);
                }
            });
            logLines.addItem("50", new Command() {

                public void execute() {
                    setLogLines(50);
                }
            });
            logLines.addItem("100", new Command() {

                public void execute() {
                    setLogLines(100);
                }
            });
            topmenu.addItem("Log Lines", logLines);
            MenuBar columns = new MenuBar();
            final MenuItem cms[] = new MenuItem[Column.values.length];
            for (int n = 0; n < Column.values.length; n++) {
                final int column = n;
                final Column c = Column.values[n];
                final String html = c.toString();
                final String shtml = "<img src='selected.png' />" + c;
                cms[n] = new MenuItem((visibleColumns[n] == true ? shtml : html), true, new Command() {

                    public void execute() {
                        visibleColumns[column] ^= true;
                        cms[column].setHTML(visibleColumns[column] ? shtml : html);
                        resetTable();
                    }
                });
                columns.addItem(cms[n]);
            }
            topmenu.addItem("Visible Columns", columns);
            final String auhtml = "Auto Update";
            final String sauhtml = "<img src='selected.png' />Auto Update";
            autoUpdateMenu = new MenuItem(autoUpdate ? sauhtml : auhtml, true, new Command() {

                public void execute() {
                    autoUpdate ^= true;
                    autoUpdateMenu.setHTML(autoUpdate ? sauhtml : auhtml);
                    if (autoUpdate) autoUpdateTimer.run();
                }
            });
            topmenu.addItem(autoUpdateMenu);
            MenuBar selection = new MenuBar();
            selection.addItem("Select All", new Command() {

                public void execute() {
                    selectAll();
                }
            });
            selection.addItem("Deselect All", new Command() {

                public void execute() {
                    deselectAll();
                }
            });
            selection.addItem("Ack", new Command() {

                public void execute() {
                    int tot = 0;
                    for (int n = 0; n < data.lines.length; n++) {
                        if (selects[n].isChecked()) tot++;
                    }
                    if (tot == 0) return;
                    long ids[] = new long[tot];
                    tot = 0;
                    for (int n = 0; n < data.lines.length; n++) {
                        if (selects[n].isChecked()) ids[tot++] = data.lines[n].id;
                    }
                    AsyncCallback cb = new AsyncCallback() {

                        public void onFailure(Throwable arg0) {
                        }

                        public void onSuccess(Object arg0) {
                            getData(0);
                        }
                    };
                    weblog.ack(ids, cb);
                }
            });
            topmenu.addItem("Selecion", selection);
            topmenu.addItem("Add Filter", new Command() {

                public void execute() {
                    addFilter();
                }
            });
            topmenu.addItem("Refresh", new Command() {

                public void execute() {
                    getData(0);
                }
            });
        }

        protected void deselectAll() {
            for (int n = 0; n < data.lines.length; n++) selects[n].setChecked(false);
        }

        protected void selectAll() {
            for (int n = 0; n < data.lines.length; n++) selects[n].setChecked(true);
        }

        private void disableAutoUpdate() {
            final String html = "Auto Update";
            final String shtml = "<img src='selected.png' />Auto Update";
            autoUpdate = false;
            autoUpdateMenu.setHTML(autoUpdate ? shtml : html);
        }

        public void onClick(Widget sender) {
            if (sender == olderButton) {
                disableAutoUpdate();
                deselectAll();
                getData(visibleLogs / 3);
            } else if (sender == newerButton) {
                disableAutoUpdate();
                deselectAll();
                getData(-visibleLogs / 3);
            } else if (sender == olderPage) {
                disableAutoUpdate();
                deselectAll();
                getData(visibleLogs);
            } else if (sender == newerPage) {
                disableAutoUpdate();
                deselectAll();
                getData(-visibleLogs);
            } else if (sender == firstPage) {
                disableAutoUpdate();
                deselectAll();
                getData(WebLogIfc.START, 0);
            } else if (sender == lastPage) {
                disableAutoUpdate();
                deselectAll();
                getData(WebLogIfc.END, -visibleLogs);
            }
        }

        private void resetTable() {
            for (int n = 0; n < visibleLogs + 1; n++) table.removeRow(0);
            initTable();
            update();
        }

        /**
		 * Initializes the table so that it contains enough rows for a full page of
		 * emails. Also creates the images that will be used as 'read' flags.
		 */
        private void initTable() {
            int n = 0;
            table.setText(0, n++, "");
            if (visibleColumns[Column.LEVEL.ordinal]) {
                table.setText(0, n, "!");
                table.getColumnFormatter().setWidth(n, "1em");
                n++;
            }
            if (visibleColumns[Column.DATE.ordinal]) table.setText(0, n++, "Date");
            if (visibleColumns[Column.URI.ordinal]) table.setText(0, n++, "URI");
            if (visibleColumns[Column.LOGGERNAME.ordinal]) table.setText(0, n++, "Origin");
            if (visibleColumns[Column.CLASSMETHOD.ordinal]) table.setText(0, n++, "Class::Method");
            if (visibleColumns[Column.MESSAGE.ordinal]) {
                table.setText(0, n, "Message");
                table.getColumnFormatter().setWidth(n, "100%");
                n++;
            }
            int colSpanNavBar = 1;
            if (visibleColumns[Column.ACK.ordinal]) colSpanNavBar = 2;
            navBarColumn = Column.values.length + 1;
            table.setWidget(0, n, navBar);
            table.getRowFormatter().setStyleName(0, "log-ListHeader");
            if (colSpanNavBar > 1) table.getFlexCellFormatter().setColSpan(0, n, 2);
            selects = new CheckBox[visibleLogs];
            for (int i = 0; i < visibleLogs; ++i) {
                if (i % 2 == 1) table.getRowFormatter().addStyleName(i, "log-List-1");
                selects[i] = new CheckBox("");
                table.setWidget(i + 1, 0, selects[i]);
                table.getFlexCellFormatter().addStyleName(i + 1, 0, "log-List-Cell");
                int realCol = 1;
                for (n = 1; n < navBarColumn; n++) {
                    if (visibleColumns[n - 1]) {
                        table.setText(i + 1, realCol, "");
                        if (n - 1 == Column.MESSAGE.ordinal) table.getFlexCellFormatter().setWordWrap(i + 1, realCol, true); else table.getFlexCellFormatter().setWordWrap(i + 1, realCol, false);
                        table.getFlexCellFormatter().addStyleName(i + 1, realCol, "log-List-Cell");
                        realCol++;
                    }
                }
                table.getFlexCellFormatter().setColSpan(i + 1, realCol - 1, 2);
            }
        }

        private void setLogLines(int newlines) {
            if (newlines < visibleLogs) {
                for (int i = newlines; i < visibleLogs; i++) {
                    table.removeRow(newlines + 1);
                }
                CheckBox newselects[] = new CheckBox[newlines];
                for (int i = 0; i < newlines; i++) newselects[i] = selects[i];
                selects = newselects;
                visibleLogs = newlines;
                getData(0);
            } else if (newlines > visibleLogs) {
                CheckBox newselects[] = new CheckBox[newlines];
                for (int i = 0; i < visibleLogs; i++) newselects[i] = selects[i];
                selects = newselects;
                for (int i = visibleLogs; i < newlines; i++) {
                    if (i % 2 == 1) table.getRowFormatter().addStyleName(i, "log-List-1");
                    selects[i] = new CheckBox("");
                    table.setWidget(i + 1, 0, selects[i]);
                    table.getFlexCellFormatter().addStyleName(i + 1, 0, "log-List-Cell");
                    int realCol = 1;
                    for (int n = 1; n < navBarColumn; n++) {
                        if (visibleColumns[n - 1]) {
                            table.setText(i + 1, realCol, "");
                            if (n - 1 == Column.MESSAGE.ordinal) table.getFlexCellFormatter().setWordWrap(i + 1, realCol, true); else table.getFlexCellFormatter().setWordWrap(i + 1, realCol, false);
                            table.getFlexCellFormatter().addStyleName(i + 1, realCol, "log-List-Cell");
                            realCol++;
                        }
                    }
                    table.getFlexCellFormatter().setColSpan(i + 1, realCol - 1, 2);
                }
                visibleLogs = newlines;
                getData(0);
            }
        }

        private void update() {
            int count = getLogsCount();
            int max = data.start + visibleLogs;
            if (max > count) max = count;
            countLabel.setText("" + (data.start + 1) + " - " + max + " of " + count);
            int i = 0;
            for (; i < visibleLogs; ++i) {
                if (data.start + i >= getLogsCount()) break;
                table.setWidget(i + 1, 0, selects[i]);
                if (data == null || data.lines.length <= i || data.lines[i] == null) {
                    int realCol = 1;
                    for (int n = 1; n < navBarColumn; n++) {
                        if (visibleColumns[n - 1]) {
                            table.setText(i + 1, realCol, "");
                            realCol++;
                        }
                    }
                } else {
                    String color = "";
                    if (data.lines[i].level.startsWith("F")) color = "log-debug"; else if (data.lines[i].level.startsWith("I")) color = "log-info"; else if (data.lines[i].level.startsWith("W")) color = "log-warn"; else {
                        color = "log-err";
                    }
                    int realCol = 1;
                    if (visibleColumns[Column.LEVEL.ordinal]) table.setText(i + 1, realCol++, data.lines[i].level.toString().substring(0, 1));
                    if (visibleColumns[Column.DATE.ordinal]) {
                        String date = data.lines[i].dateFormatted;
                        if (date.length() != 17) table.setHTML(i + 1, realCol++, date); else {
                            String yearmonth = date.substring(0, 9);
                            String dayhourmin = date.substring(9, 14);
                            String secs = date.substring(14);
                            String html = "<font style='font-size: smaller;'>" + yearmonth + "</font>" + dayhourmin + "<font style='font-size: smaller;'>" + secs + "</font>";
                            table.setHTML(i + 1, realCol++, html);
                        }
                    }
                    if (visibleColumns[Column.URI.ordinal]) {
                        String uri = data.lines[i].uri;
                        boolean regular = true;
                        if (!uri.startsWith("//")) regular = false; else {
                            int first = uri.indexOf("/", 2);
                            if (first < 0) regular = false; else {
                                int second = uri.indexOf("/", first + 1);
                                if (second < 0) regular = false; else {
                                    String begin = uri.substring(0, second);
                                    String end = uri.substring(second);
                                    String html = "<font style='font-size: smaller;'>" + begin + "</font>" + end;
                                    table.setHTML(i + 1, realCol++, html);
                                }
                            }
                        }
                        if (!regular) table.setText(i + 1, realCol++, uri);
                    }
                    if (visibleColumns[Column.LOGGERNAME.ordinal]) table.setText(i + 1, realCol++, data.lines[i].loggerName);
                    if (visibleColumns[Column.CLASSMETHOD.ordinal]) table.setText(i + 1, realCol++, data.lines[i].className + "::" + data.lines[i].methodName);
                    if (visibleColumns[Column.MESSAGE.ordinal]) {
                        String message = data.lines[i].message;
                        int secondlineat = message.indexOf("<br />");
                        if (secondlineat > 0) {
                            HorizontalPanel hp = new HorizontalPanel();
                            hp.setWidth("100%");
                            HTML begin = new HTML(message.substring(0, secondlineat));
                            begin.addStyleName(color);
                            hp.add(begin);
                            HTML more = new HTML("<a href='javascript:;'>...</a>");
                            hp.add(more);
                            hp.setCellHorizontalAlignment(more, HasHorizontalAlignment.ALIGN_RIGHT);
                            final VerticalPanel vp = new VerticalPanel();
                            vp.setWidth("100%");
                            vp.add(hp);
                            final HTML rest = new HTML(message.substring(secondlineat + 6));
                            rest.addStyleName(color);
                            more.addClickListener(new ClickListener() {

                                public void onClick(Widget sender) {
                                    if (vp.getWidgetCount() == 1) vp.add(rest); else vp.remove(1);
                                }
                            });
                            table.setWidget(i + 1, realCol++, vp);
                        } else table.setHTML(i + 1, realCol++, message);
                    }
                    if (visibleColumns[Column.ACK.ordinal]) table.setText(i + 1, realCol++, "");
                    table.getRowFormatter().removeStyleName(i + 1, "log-debug");
                    table.getRowFormatter().removeStyleName(i + 1, "log-info");
                    table.getRowFormatter().removeStyleName(i + 1, "log-warn");
                    table.getRowFormatter().removeStyleName(i + 1, "log-err");
                    if (visibleColumns[Column.ACK.ordinal]) {
                        if (!data.lines[i].acked) {
                            table.getFlexCellFormatter().setColSpan(i + 1, realCol - 2, 2);
                            final long id = data.lines[i].id;
                            HTML ack = new HTML("<a href='javascript:;'>Acknowledge</a>;");
                            ack.addClickListener(new ClickListener() {

                                public void onClick(Widget arg0) {
                                    AsyncCallback cb = new AsyncCallback() {

                                        public void onFailure(Throwable arg0) {
                                        }

                                        public void onSuccess(Object arg0) {
                                            getData(0);
                                        }
                                    };
                                    weblog.ack(id, cb);
                                }
                            });
                            ack.addStyleName("log-ack");
                            table.setWidget(i + 1, realCol - 1, ack);
                        } else {
                            table.setText(i + 1, realCol - 1, "");
                            table.removeCell(i + 1, realCol - 1);
                            table.getFlexCellFormatter().setColSpan(i + 1, realCol - 2, 3);
                        }
                    }
                    table.getRowFormatter().addStyleName(i + 1, color);
                }
            }
            for (; i < visibleLogs; ++i) {
                table.setHTML(i + 1, 0, "&nbsp;");
                table.setHTML(i + 1, 1, "&nbsp;");
                table.setHTML(i + 1, 2, "&nbsp;");
                table.setHTML(i + 1, 3, "&nbsp;");
                table.setHTML(i + 1, 4, "&nbsp;");
            }
        }

        private int getLogsCount() {
            if (data == null) return 0;
            return data.logsCount;
        }

        private void getData(int relativeMove) {
            long startID = WebLogIfc.END;
            if (data != null && data.lines.length > 0) {
                startID = data.lines[0].id;
            } else relativeMove = -visibleLogs;
            getData(startID, relativeMove);
        }

        private void getData(final long startID, int relativeMove) {
            AsyncCallback cb = new AsyncCallback() {

                public void onSuccess(Object ret) {
                    WebLogData retData = (WebLogData) ret;
                    data = retData;
                    update();
                    if ((startID == WebLogIfc.END || data == null) && autoUpdate) autoUpdateTimer.schedule(1000);
                }

                public void onFailure(Throwable arg0) {
                }
            };
            FilterData filters[] = getFilters();
            if (data == null) {
                relativeMove = -visibleLogs;
            }
            weblog.getLogs(data == null ? WebLogIfc.END : startID, relativeMove, visibleLogs, filters, cb);
        }

        private FilterData[] getFilters() {
            int n = 0;
            for (n = 0; n < filters.length && filters[n] != null; n++) ;
            FilterData ret[] = new FilterData[n];
            for (n = 0; n < filters.length && filters[n] != null; n++) {
                Filter f = filters[n];
                ret[n] = new FilterData();
                ret[n].scope = f.scope.getSelectedIndex();
                ret[n].match = f.matchType.getSelectedIndex() == 0;
                if (f.scope.getSelectedIndex() == FilterData.LEVEL) {
                    ret[n].expr = f.levels.getItemText(f.levels.getSelectedIndex());
                } else if (f.scope.getSelectedIndex() == FilterData.ACK) {
                    ret[n].expr = f.select.isChecked() ? "1" : "0";
                } else ret[n].expr = f.expr.getText();
            }
            return ret;
        }
    }

    public class Filter extends Composite implements ClickListener, ChangeListener {

        final LogPanel parent;

        HorizontalPanel panel;

        ListBox scope;

        ListBox matchType;

        TextBox expr;

        Button remove;

        ListBox levels;

        CheckBox select;

        public Filter(LogPanel parent) {
            this.parent = parent;
            panel = new HorizontalPanel();
            panel.addStyleName("log-List");
            panel.setWidth("100%");
            scope = new ListBox();
            scope.addChangeListener(this);
            scope.addStyleName("log-List-Cell");
            scope.setVisibleItemCount(1);
            for (int n = 0; n < FilterData.values().length; n++) scope.addItem(FilterData.values()[n]);
            scope.setSelectedIndex(4);
            panel.add(scope);
            matchType = new ListBox();
            matchType.addStyleName("log-List-Cell");
            matchType.setVisibleItemCount(1);
            matchType.addItem("contains");
            matchType.addItem("does not");
            panel.add(matchType);
            expr = new TextBox();
            expr.addStyleName("log-List-Cell");
            expr.setWidth("100%");
            panel.add(expr);
            remove = new Button("Remove", this);
            remove.addStyleName("log-List-Cell");
            panel.add(remove);
            panel.setCellWidth(expr, "100%");
            levels = new ListBox();
            levels.addStyleName("log-List-Cell");
            levels.setVisibleItemCount(1);
            levels.addItem("FINE");
            levels.addItem("INFO");
            levels.addItem("WARNING");
            levels.addItem("SEVERE");
            select = new CheckBox("Is Acknowledged");
            initWidget(panel);
        }

        public void onClick(Widget arg0) {
            parent.removeFilter(this);
        }

        public void onChange(Widget arg0) {
            switch(scope.getSelectedIndex()) {
                case FilterData.LEVEL:
                    setLevel();
                    break;
                case FilterData.URI:
                    setNormal();
                    break;
                case FilterData.LOGGER:
                    setNormal();
                    break;
                case FilterData.CLASS:
                    setNormal();
                    break;
                case FilterData.METHOD:
                    setNormal();
                    break;
                case FilterData.MESSAGE:
                    setNormal();
                    break;
                case FilterData.ACK:
                    setAck();
                    break;
            }
        }

        private void setAck() {
            panel.remove(matchType);
            panel.remove(expr);
            panel.remove(remove);
            panel.remove(levels);
            panel.add(select);
            panel.add(remove);
        }

        private void setNormal() {
            panel.remove(levels);
            panel.remove(select);
            panel.remove(remove);
            panel.add(matchType);
            panel.add(expr);
            panel.add(remove);
            panel.setCellWidth(expr, "100%");
        }

        private void setLevel() {
            panel.remove(matchType);
            panel.remove(expr);
            panel.remove(remove);
            panel.remove(select);
            panel.add(levels);
            panel.add(remove);
        }
    }

    public static class WebLogData implements Serializable {

        private static final long serialVersionUID = 1L;

        public int logsCount;

        public int start;

        public LogLine lines[];
    }

    public static class LogLine implements Serializable {

        private static final long serialVersionUID = 1L;

        public String uri;

        public String loggerName;

        public String className;

        public String methodName;

        public long millis;

        public String dateFormatted;

        public String level;

        public int nLevel;

        public String message;

        public long id;

        public boolean acked = false;
    }
}
