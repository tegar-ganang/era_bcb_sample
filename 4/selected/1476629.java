package eolus.client.Presenters;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import eolus.client.FunctionObject;
import eolus.client.Parser;
import eolus.client.UI_Elements.CheckList;
import eolus.client.UI_Elements.HTMLwriter;
import eolus.client.UI_Elements.LightBox;
import eolus.client.UI_Elements.MyTable;

public class ClusterPresenter extends Presenter {

    private String[] ClusterList;

    public void loadNewCluster() {
        loadpage("Create New Cluster");
        final VerticalPanel panel = new VerticalPanel();
        view.add(panel);
        panel.add(new Label("Cluster Name:"));
        final TextBox name = new TextBox();
        name.setName("vnet_name");
        panel.add(name);
        panel.add(new HTML("<br />"));
        final HTML error_name = HTMLwriter.getSystemMessage("red", "Name cannot be blank  or contain spaces.");
        final HTML error_nodata = HTMLwriter.getSystemMessage("red", "Please select at least one host.");
        final HTML error_dup = HTMLwriter.getSystemMessage("yellow", "Host already in list.");
        error_name.setVisible(false);
        panel.add(error_name);
        FlowPanel f = new FlowPanel();
        panel.add(f);
        final ListBox nets = new ListBox();
        Button add = new Button("Add Host to Cluster");
        Button remove = new Button("Remove Host from Cluster");
        final ListBox selected = new ListBox();
        nets.setWidth("186px");
        selected.setWidth("186px");
        VerticalPanel vb1 = new VerticalPanel();
        vb1.add(new Label("Select Hosts"));
        vb1.add(nets);
        vb1.add(add);
        f.add(vb1);
        VerticalPanel vb3 = new VerticalPanel();
        f.add(vb3);
        vb3.add(new Label("Selected Hosts:"));
        vb3.add(selected);
        vb3.add(remove);
        f.add(error_nodata);
        f.add(error_dup);
        error_nodata.setVisible(false);
        error_dup.setVisible(false);
        nets.setVisibleItemCount(8);
        selected.setVisibleItemCount(8);
        add.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                error_nodata.setVisible(false);
                error_dup.setVisible(false);
                if (nets.getSelectedIndex() == -1) return;
                String network = nets.getItemText(nets.getSelectedIndex());
                int i = 0, size = selected.getItemCount();
                while (i < size) {
                    if (selected.getItemText(i).equals(network)) {
                        error_dup.setVisible(true);
                        return;
                    }
                    i++;
                }
                selected.addItem(network);
            }
        });
        remove.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                error_dup.setVisible(false);
                if (selected.getSelectedIndex() == -1) return;
                selected.removeItem(selected.getSelectedIndex());
            }
        });
        final AsyncCallback<String[]> callback_clusters = new AsyncCallback<String[]>() {

            public void onFailure(Throwable caught) {
                caught.getMessage();
                HTML msg = HTMLwriter.getSystemMessage("red", "Error contacting server: " + caught.getMessage());
                panel.add(msg);
            }

            public void onSuccess(String[] hosts) {
                int i = 0, size = hosts.length;
                nets.clear();
                while (i < size) {
                    nets.addItem(hosts[i]);
                    i++;
                }
            }
        };
        setRefreshTask(new FunctionObject() {

            public void function() {
                serverCall.getHostNames(callback_clusters);
            }
        });
        Button submit = new Button("Create New Cluster");
        panel.add(submit);
        final AsyncCallback<String> callback = new AsyncCallback<String>() {

            public void onFailure(Throwable caught) {
                caught.getMessage();
                panel.clear();
                HTML msg = HTMLwriter.getSystemMessage("red", "Error contacting server: " + caught.getMessage());
                panel.add(msg);
                stopRefreshTask();
            }

            public void onSuccess(String b) {
                HTML msg = new HTML();
                if (b.equals("ok")) msg = HTMLwriter.getSystemMessage("green", "Cluster created successfully." + " You can now view it."); else {
                    msg = HTMLwriter.getSystemMessage("red", "There was an error creating the cluster." + b);
                }
                panel.clear();
                panel.add(msg);
                stopRefreshTask();
            }
        };
        submit.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                error_name.setVisible(false);
                if (selected.getItemCount() == 0) {
                    error_nodata.setVisible(true);
                    return;
                }
                if (!(Parser.isvalidName(name.getText()))) {
                    error_name.setVisible(true);
                    return;
                }
                String[] itemlist = Parser.createArrayFromListBox(selected);
                serverCall.adminNewCluster(name.getText(), itemlist, callback);
            }
        });
    }

    public void loadViewCluster() {
        loadpage("View Clusters");
        view.add(new Label("View Clusters:"));
        final AsyncCallback<String[]> callback = new AsyncCallback<String[]>() {

            public void onFailure(Throwable caught) {
                caught.getMessage();
                view.clear();
                HTML msg = HTMLwriter.getSystemMessage("red", "Error contacting server: " + caught.getMessage());
                view.add(msg);
            }

            public void onSuccess(final String[] clusters) {
                ClusterList = clusters;
                loadpagination(clusters.length);
                loadClusterTables();
            }
        };
        setRefreshTask(new FunctionObject() {

            public void function() {
                serverCall.adminGetClusters(callback);
            }
        });
    }

    public void reload() {
        loadpagination(ClusterList.length);
        hideContent();
        loadClusterTables();
    }

    public void loadClusterTables() {
        int i = (current_page - 1) * page_size, size = ClusterList.length;
        int itemsadded = 0;
        while (i < size) {
            loadCluster(ClusterList[i]);
            i++;
            itemsadded++;
            if (itemsadded == page_size) return;
        }
    }

    public void loadCluster(final String name) {
        final VerticalPanel content = (VerticalPanel) this.getContentWidgetNoClear(name, "Cluster: " + name);
        final Widget ctable = getContentNoClear(name, "Cluster: " + name);
        ctable.setVisible(true);
        final String[] headers = { "Host name", "Actions" };
        content.setVisible(true);
        final MyTable table;
        if (content.getWidgetCount() == 0) {
            table = new MyTable();
            content.add(table);
        } else {
            table = (MyTable) content.getWidget(content.getWidgetCount() - 1);
        }
        serverCall.adminGetHostsOfCluster(name, new AsyncCallback<String[]>() {

            public void onFailure(Throwable caught) {
                view.clear();
                HTML msg = HTMLwriter.getSystemMessage("red", "Problem contacting server. " + caught.getMessage());
                view.add(msg);
            }

            public void onSuccess(final String[] currhosts) {
                int i = 0, size = currhosts.length;
                table.setHeaders(headers);
                while (i < size) {
                    loadHost(name, table, currhosts[i], i + 1);
                    i++;
                }
                if (0 == size) {
                    while (table.getTable().getCellCount(0) > 1) {
                        table.getTable().removeCell(0, 1);
                    }
                    table.getTable().setWidget(0, 0, new Label("This cluster has no hosts yet."));
                }
                table.removeExtra(size);
                MyTable.styleRows(1, headers.length, table.getTable());
                Button info = new Button("&nbsp;&nbsp;View more info");
                table.getTable().setWidget(size + 1, 0, info);
                info.setStyleName("btn info");
                info.addClickHandler(new ClickHandler() {

                    public void onClick(ClickEvent event) {
                        serverCall.adminGetClusterInfo(name, new AsyncCallback<String[]>() {

                            public void onFailure(Throwable caught) {
                                LightBox l = new LightBox();
                                l.add(HTMLwriter.getSystemMessage("red", "Problem fetching info. " + caught.getMessage()));
                                l.show();
                            }

                            public void onSuccess(String[] result) {
                                if (result != null) {
                                    LightBox l = new LightBox();
                                    l.add(new Label("Cluster Info"));
                                    l.add(new Label("Migrate "));
                                    l.add(new HTML(result[0]));
                                    l.add(new Label("Live Migrate "));
                                    l.add(new HTML(result[1]));
                                    l.show();
                                } else {
                                    LightBox l = new LightBox();
                                    l.add(HTMLwriter.getSystemMessage("red", "Problem fetching info. " + result));
                                    l.show();
                                }
                            }
                        });
                    }
                });
                if (name.equals("default")) {
                    return;
                }
                Button add = new Button("&nbsp;&nbsp;&nbsp;Add more hosts to cluster");
                table.getTable().setWidget(size + 1, 1, add);
                add.setStyleName("btn add");
                Button remove = new Button("&nbsp;&nbsp;&nbsp;Delete Cluster");
                table.getTable().setWidget(size + 1, 2, remove);
                remove.setStyleName("btn remove");
                remove.addClickHandler(new ClickHandler() {

                    public void onClick(ClickEvent event) {
                        serverCall.adminRemoveCluster(name, new AsyncCallback<String>() {

                            public void onFailure(Throwable caught) {
                                LightBox l = new LightBox();
                                l.add(HTMLwriter.getSystemMessage("red", "Problem deleting. " + caught.getMessage()));
                                l.show();
                            }

                            public void onSuccess(String result) {
                                if (result.equals("ok")) {
                                    ctable.setVisible(false);
                                    doRefresh();
                                } else {
                                    LightBox l = new LightBox();
                                    l.add(HTMLwriter.getSystemMessage("red", "Problem deleting. " + result));
                                    l.show();
                                }
                            }
                        });
                    }
                });
                add.addClickHandler(new ClickHandler() {

                    public void onClick(ClickEvent event) {
                        final LightBox glass = new LightBox();
                        serverCall.getHosts(new AsyncCallback<String[]>() {

                            public void onFailure(Throwable caught) {
                                glass.add(HTMLwriter.getSystemMessage("red", "Error getting host list from server:" + caught.getMessage()));
                                glass.show();
                            }

                            public void onSuccess(String[] result) {
                                if (result != null) {
                                    String[] newhosts = new String[result.length - currhosts.length];
                                    int i = 0, newi = 0, size = result.length;
                                    while (i < size) {
                                        int j = 0, sizej = currhosts.length;
                                        boolean isAlreadyMember = false;
                                        while (j < sizej) {
                                            if (currhosts[j].equals(result[i])) {
                                                isAlreadyMember = true;
                                                break;
                                            }
                                            j++;
                                        }
                                        if (!isAlreadyMember) {
                                            newhosts[newi] = result[i];
                                            newi++;
                                        }
                                        i++;
                                    }
                                    final CheckList cl = new CheckList(newhosts, "Select hosts to add:");
                                    final VerticalPanel v = cl.getCheckList();
                                    glass.getPanel().add(v);
                                    Button submit = new Button("submit");
                                    glass.getPanel().add(submit);
                                    submit.addClickHandler(new ClickHandler() {

                                        public void onClick(ClickEvent event) {
                                            doaddhosts(cl.getChecked(), name, glass);
                                        }
                                    });
                                    glass.show();
                                } else {
                                    glass.add(HTMLwriter.getSystemMessage("red", "Error getting host list"));
                                    glass.show();
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    public void doaddhosts(String[] hosts, String clustername, final LightBox l) {
        int i = 0, size = hosts.length;
        while (i < size) {
            serverCall.adminAddHostToCluster(hosts[i], clustername, new AsyncCallback<String>() {

                public void onFailure(Throwable caught) {
                    l.clear();
                    HTML msg = HTMLwriter.getSystemMessage("red", errorServer + " " + caught.getMessage());
                    l.add(msg);
                }

                public void onSuccess(String result) {
                    l.clear();
                    HTML msg;
                    if (result.equalsIgnoreCase("ok")) msg = HTMLwriter.getSystemMessage("green", "Hosts successfully added"); else msg = HTMLwriter.getSystemMessage("red", errorRequest + " " + result);
                    l.add(msg);
                    if (result.equalsIgnoreCase("ok")) l.autoclose();
                }
            });
            i++;
        }
    }

    public void loadHost(final String clustername, MyTable table, final String hostname, final int row) {
        final FlexTable ft = table.getTable();
        ft.getRowFormatter().setVisible(row, true);
        int size = 1;
        while (ft.getCellCount(row) > 2) {
            ft.removeCell(row, 2);
        }
        ft.setWidget(row, 0, new Label(hostname));
        Button remove = new Button("&nbsp;&nbsp;&nbsp;Remove from Cluster");
        remove.setStyleName("btn remove");
        ft.setWidget(row, size++, remove);
        if (clustername.equals("default")) remove.setEnabled(false);
        remove.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                serverCall.adminRemoveHostfromCluster(hostname, clustername, new AsyncCallback<String>() {

                    public void onFailure(Throwable caught) {
                        LightBox l = new LightBox();
                        HTML msg = new HTML(errorServer + " " + caught.getMessage());
                        l.add(msg);
                        l.show();
                    }

                    public void onSuccess(String result) {
                        if (result.equalsIgnoreCase("ok")) {
                            doRefresh();
                        } else {
                            LightBox l = new LightBox();
                            l.add(new HTML(errorRequest + " " + result));
                            l.show();
                        }
                    }
                });
            }
        });
    }

    public void init() {
        String[] s1 = { "NewCluster", "Create Cluster" };
        String[] s2 = { "ViewCluster", "View Clusters" };
        admin_sidebar.add(s1);
        admin_sidebar.add(s2);
        user_sidebar.clear();
        user_icons.clear();
        String[] i1 = { "NewCluster", "d1_1", "Create Cluster" };
        String[] i2 = { "ViewCluster", "d1_1", "View Clusters" };
        admin_icons.add(i1);
        admin_icons.add(i2);
        category = "Clusters";
    }
}
