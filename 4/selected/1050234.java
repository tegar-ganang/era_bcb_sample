package eolus.client.Presenters;

import java.util.HashMap;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.HTML;
import eolus.client.FunctionObject;
import eolus.client.Parser;
import eolus.client.UI_Elements.HTMLwriter;
import eolus.client.UI_Elements.LightBox;
import eolus.client.UI_Elements.MyDropDown;
import eolus.client.UI_Elements.MyTable;

public class NetPresenter extends Presenter {

    String[] headings = { "Network Name", "Owner", "Actions" };

    private RadioButton subnet_rb;

    private RadioButton list_rb;

    private TextBox subnet;

    private TextBox name;

    private String[] UserList;

    public void showinfo(String network) {
        AsyncCallback<String> callback = new AsyncCallback<String>() {

            public void onFailure(Throwable caught) {
                HTML msg = HTMLwriter.getSystemMessage("red", "Problem contacting server " + caught.getMessage());
                LightBox l = new LightBox();
                l.add(msg);
                l.show();
            }

            public void onSuccess(String xml) {
                LightBox glass = new LightBox();
                HashMap<String, Object> map = Parser.parseNetworkInfo(xml);
                Boolean ispublic = (Boolean) map.get("IsPublic");
                String ispub = "No";
                if (ispublic) ispub = "Yes";
                String state = (String) map.get("State");
                String name = (String) map.get("Name");
                String[] iplist = (String[]) map.get("IPS");
                VerticalPanel v = new VerticalPanel();
                v.add(new Label("Network Info"));
                v.add(new HTML("<strong>Name :</strong>" + name + "<br/><strong>Is Public:</strong>" + ispub + "<br/><strong>State :</strong>" + state + "<br/><strong>IP Addresses in Use :</strong>" + iplist.length));
                VerticalPanel ippanel = new VerticalPanel();
                int i = 0, size = iplist.length;
                while (i < size) {
                    ippanel.add(new Label(iplist[i]));
                    i++;
                }
                DisclosurePanel disc = new DisclosurePanel("View all used IPs");
                ScrollPanel scr = new ScrollPanel(ippanel);
                scr.setHeight("75px");
                disc.setContent(scr);
                v.add(disc);
                glass.add(v);
                glass.show();
            }
        };
        serverCall.getNetworkInfo(network, callback);
    }

    public void remove(String network) {
        AsyncCallback<String> callback = new AsyncCallback<String>() {

            public void onFailure(Throwable caught) {
                HTML msg = HTMLwriter.getSystemMessage("red", "Problem perfoming this action " + caught.getMessage());
                LightBox l = new LightBox();
                l.add(msg);
                l.show();
            }

            public void onSuccess(String result) {
                if (result.equals("ok")) {
                    doRefresh();
                } else {
                    HTML msg = HTMLwriter.getSystemMessage("red", "Problem perfoming this action " + result);
                    LightBox l = new LightBox();
                    l.add(msg);
                    l.show();
                }
            }
        };
        if (user.isAdmin()) serverCall.removeNetwork(network, callback); else serverCall.removeNetworkUser(network, callback);
    }

    public void doassign(String network, String usr, final LightBox l) {
        AsyncCallback<String> callback = new AsyncCallback<String>() {

            public void onFailure(Throwable caught) {
                l.clear();
                HTML msg = HTMLwriter.getSystemMessage("red", "Problem performing this action: " + caught.getMessage());
                l.add(msg);
            }

            public void onSuccess(String result) {
                l.clear();
                HTML msg = new HTML();
                if (result.equals("ok")) msg = HTMLwriter.getSystemMessage("green", "Network assigned successfully"); else msg = HTMLwriter.getSystemMessage("red", "Problem assigning network: " + result);
                l.add(msg);
                if (result.equals("ok")) {
                    l.autoclose();
                    doRefresh();
                }
            }
        };
        serverCall.assignNetwork(network, usr, callback);
    }

    public void assign(final String network) {
        final LightBox glass = new LightBox();
        VerticalPanel v = new VerticalPanel();
        final MyDropDown menu = new MyDropDown(UserList, "Select user to assign Network " + network, "Assign");
        v.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        v.add(menu);
        glass.add(v);
        Button submit = menu.getButton();
        submit.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                String usr = menu.getSelected();
                if (usr == null) return;
                doassign(network, usr, glass);
            }
        });
        glass.show();
    }

    public void LoadAdminNetworks() {
        loadpage("View All Networks");
        view.add(new Label("View Networks:"));
        final AsyncCallback<String[]> callback = new AsyncCallback<String[]>() {

            public void onFailure(Throwable caught) {
                caught.getMessage();
                view.clear();
                HTML msg = HTMLwriter.getSystemMessage("red", "Error contacting server.");
                view.add(msg);
            }

            public void onSuccess(final String[] res) {
                UserList = res;
                loadpagination(res.length);
                loadUserTables();
            }
        };
        setRefreshTask(new FunctionObject() {

            public void function() {
                serverCall.getUsers(callback);
            }
        });
    }

    public void loadUserTables() {
        int i = (current_page - 1) * page_size, size = UserList.length;
        int itemsadded = 0;
        while (i < size) {
            loadUser(UserList[i]);
            i++;
            itemsadded++;
            if (itemsadded == page_size) return;
        }
    }

    public void reload() {
        loadpagination(UserList.length);
        hideContent();
        loadUserTables();
    }

    public void loadUser(final String userdata) {
        String divtitle = userdata + "'s Virtual Networks";
        final VerticalPanel content;
        final MyTable table;
        if (!divExists(userdata)) {
            table = new MyTable();
            content = (VerticalPanel) this.getContentWidgetNoClear(userdata, divtitle);
            content.add(table);
        } else {
            content = (VerticalPanel) this.getContentWidgetNoClear(userdata, divtitle);
            table = (MyTable) content.getWidget(0);
        }
        serverCall.getNetworksbyUser(userdata, new AsyncCallback<String[]>() {

            public void onFailure(Throwable caught) {
                contactError(caught);
            }

            public void onSuccess(String[] result) {
                int i = 0, size = result.length;
                while (i < size) {
                    loadVN(userdata, table, result[i], i + 1);
                    i++;
                }
                if (0 == size) {
                    table.getTable().setWidget(0, 0, new Label("This user has no virtual networks."));
                    while (table.getTable().getCellCount(0) > 1) {
                        table.getTable().removeCell(0, 1);
                    }
                    table.unsetHeaders();
                } else {
                    table.setHeaders(headings);
                }
                table.removeExtra(size);
                MyTable.styleRows(1, headings.length, table.getTable());
            }
        });
    }

    public void loadVN(final String username, MyTable table, final String network, final int row) {
        final FlexTable ft = table.getTable();
        ft.getRowFormatter().setVisible(row, true);
        ft.setWidget(row, 0, new Label(network));
        ft.setWidget(row, 1, new Label(username));
        int size = 2;
        FlowPanel actions = new FlowPanel();
        table.getTable().setWidget(row, size++, actions);
        Button rem = new Button("&nbsp;&nbsp;&nbsp;Remove");
        Button info = new Button("&nbsp;&nbsp;&nbsp;View Info");
        Button assign = new Button("&nbsp;&nbsp;&nbsp;Assign to User");
        rem.setStyleName("btn remove");
        info.setStyleName("btn info");
        assign.setStyleName("btn assign");
        if (user.isAdmin() || !username.equals("public")) {
            actions.add(rem);
        }
        actions.add(info);
        if (user.isAdmin()) actions.add(assign);
        rem.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                remove(network);
            }
        });
        info.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                showinfo(network);
            }
        });
        if (!user.isAdmin()) return;
        assign.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                assign(network);
            }
        });
    }

    public void LoadCreateNetwork() {
        loadpage("Create New Network");
        VerticalPanel panel = new VerticalPanel();
        view.add(panel);
        Label label1 = new Label("Network Name:");
        panel.add(label1);
        name = new TextBox();
        name.setName("vnet_name");
        panel.add(name);
        panel.add(new HTML("<br />"));
        final HTML error_name = HTMLwriter.getSystemMessage("red", "Name cannot be blank  or contain spaces.");
        final HTML error_ip = HTMLwriter.getSystemMessage("red", "Please specify at least one IP address.");
        final HTML error_ip2 = HTMLwriter.getSystemMessage("red", "Invalid IP address specified.");
        final HTML error_ipdup = HTMLwriter.getSystemMessage("red", "IP already in list.");
        final HTML error_subnet = HTMLwriter.getSystemMessage("red", "Invalid subnet specified.");
        error_name.setVisible(false);
        panel.add(error_name);
        panel.add(new Label("Network Type:"));
        subnet_rb = new RadioButton("myRadioGroup", "Subnet");
        list_rb = new RadioButton("myRadioGroup", "List of IPs");
        subnet_rb.setValue(true);
        VerticalPanel fpanel = new VerticalPanel();
        panel.add(subnet_rb);
        panel.add(list_rb);
        panel.add(fpanel);
        panel.add(new HTML("<br />"));
        final VerticalPanel subnetpanel = new VerticalPanel();
        Label label2 = new Label("Subnet Address:");
        subnetpanel.add(label2);
        FlowPanel f1 = new FlowPanel();
        subnet = new TextBox();
        subnet.setName("vnet_subnet");
        subnet.addStyleName("tinytext");
        HTML h1 = new HTML("192.168.");
        HTML h2 = new HTML(".x");
        h1.setStyleName("inline");
        h2.setStyleName("inline");
        f1.add(h1);
        f1.add(subnet);
        f1.add(h2);
        f1.add(error_subnet);
        error_subnet.setVisible(false);
        subnetpanel.add(f1);
        panel.add(subnetpanel);
        final VerticalPanel listpanel = new VerticalPanel();
        listpanel.add(new Label("Please type an IP address:"));
        FlowPanel f = new FlowPanel();
        final TextBox ip = new TextBox();
        ip.addFocusHandler(new FocusHandler() {

            public void onFocus(FocusEvent event) {
                error_ip2.setVisible(false);
                error_ipdup.setVisible(false);
            }
        });
        f.add(ip);
        Button add = new Button("Add to List");
        f.add(add);
        error_ip.setVisible(false);
        f.add(error_ip);
        error_ipdup.setVisible(false);
        f.add(error_ipdup);
        error_ip2.setVisible(false);
        f.add(error_ip2);
        listpanel.add(f);
        final ListBox lb = new ListBox();
        lb.setWidth("186px");
        FlowPanel f2 = new FlowPanel();
        lb.setVisibleItemCount(4);
        f2.add(lb);
        Button remove = new Button("Remove from List");
        f2.add(remove);
        listpanel.add(f2);
        listpanel.setVisible(false);
        panel.add(listpanel);
        add.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                error_ip2.setVisible(false);
                error_ipdup.setVisible(false);
                if (Parser.isvalidIP(ip.getText())) {
                    int i = 0;
                    while (i < lb.getItemCount()) {
                        String temp = lb.getItemText(i);
                        if (temp.equals(ip.getText())) {
                            error_ipdup.setVisible(true);
                            return;
                        }
                        i++;
                    }
                    lb.addItem(ip.getText());
                    ip.setText("");
                    error_ip.setVisible(false);
                } else {
                    error_ip2.setVisible(true);
                }
            }
        });
        remove.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                int a = lb.getSelectedIndex();
                if (a == -1) return;
                lb.removeItem(a);
            }
        });
        subnet_rb.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                subnetpanel.setVisible(true);
                listpanel.setVisible(false);
            }
        });
        list_rb.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                listpanel.setVisible(true);
                subnetpanel.setVisible(false);
            }
        });
        Button submit = new Button("Create network");
        final HashMap<String, Object> map_info = new HashMap<String, Object>();
        map_info.put("Action", "SubmitNet");
        map_info.put("Parent", panel);
        map_info.put("Name", name);
        map_info.put("IPs", lb);
        map_info.put("Name_error", error_name);
        map_info.put("IP_error", error_ip);
        map_info.put("Subnet_error", error_subnet);
        map_info.put("Subnet", subnet);
        map_info.put("IsList", list_rb);
        panel.add(new HTML("<br />"));
        panel.add(submit);
        submit.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                submit(map_info);
            }
        });
    }

    public void submit(final HashMap<String, Object> map) {
        HTML h2 = (HTML) map.get("Name_error");
        h2.setVisible(false);
        VerticalPanel a = (VerticalPanel) map.get("Parent");
        TextBox temp_name = (TextBox) map.get("Name");
        String netname = temp_name.getText();
        RadioButton list = (RadioButton) map.get("IsList");
        String type = "", info = "";
        if (list.getValue()) {
            HTML h3 = (HTML) map.get("IP_error");
            h3.setVisible(false);
            type = "list";
            int i = 0;
            ListBox lb = (ListBox) map.get("IPs");
            int size = lb.getItemCount();
            if (size == 0) {
                h3.setVisible(true);
                return;
            }
            while (i < size) {
                if (i != 0) info += '-';
                info += lb.getItemText(i);
                if (!(Parser.isvalidIP(lb.getItemText(i)))) {
                    h3.setVisible(true);
                    if (!(Parser.isvalidName(temp_name.getText()))) {
                        HTML h4 = (HTML) map.get("Name_error");
                        h4.setVisible(true);
                    }
                    return;
                }
                i++;
            }
        } else {
            HTML h3 = (HTML) map.get("Subnet_error");
            h3.setVisible(false);
            type = "subnet";
            TextBox temp_subnet = (TextBox) map.get("Subnet");
            info = temp_subnet.getText();
            HTML h = (HTML) map.get("Subnet_error");
            if (!(Parser.isvalidIPNumber(info))) {
                h.setVisible(true);
                if (!(Parser.isvalidName(temp_name.getText()))) {
                    HTML h4 = (HTML) map.get("Name_error");
                    h4.setVisible(true);
                }
                return;
            }
        }
        if (!(Parser.isvalidName(temp_name.getText()))) {
            HTML h = (HTML) map.get("Name_error");
            h.setVisible(true);
            return;
        }
        a.clear();
        boolean admin = user.isAdmin();
        AsyncCallback<String> callback = new AsyncCallback<String>() {

            public void onFailure(Throwable caught) {
                caught.getMessage();
                VerticalPanel a = (VerticalPanel) map.get("Parent");
                HTML msg = HTMLwriter.getSystemMessage("red", "Error contacting server.");
                a.add(msg);
            }

            public void onSuccess(String b) {
                VerticalPanel a = (VerticalPanel) map.get("Parent");
                HTML msg = new HTML();
                if (b.equals("ok")) msg = HTMLwriter.getSystemMessage("green", "Network created successfully." + " You can now view it."); else {
                    msg = HTMLwriter.getSystemMessage("red", "There was an error creating the Network. The name already exists.");
                }
                a.add(msg);
            }
        };
        if (admin) {
            serverCall.AdminCreateNetwork(netname, type, info, callback);
        } else {
            serverCall.UserCreateNetwork(netname, type, info, callback);
        }
    }

    public void LoadAdminNetworksUser(String user) {
        loadpage("View Networks By User");
        AsyncCallback<String[]> callback3 = new AsyncCallback<String[]>() {

            public void onFailure(Throwable caught) {
                view.clear();
                view.add(HTMLwriter.getSystemMessage("red", "Error contacting server"));
            }

            public void onSuccess(String[] result) {
                MyDropDown menu = new MyDropDown(result, "Select user");
                ListBox list = menu.getList();
                list.addChangeHandler(new ChangeHandler() {

                    public void onChange(ChangeEvent event) {
                        ListBox list = (ListBox) event.getSource();
                        int index = list.getSelectedIndex();
                        if (index < 0) return;
                        final String usr = list.getItemText(index);
                        setRefreshTask(new FunctionObject() {

                            public void function() {
                                hideContent();
                                loadUser(usr);
                            }
                        });
                    }
                });
                view.add(menu);
            }
        };
        serverCall.getUsers(callback3);
    }

    public void LoadUserNetworks() {
        loadpage("View My Networks");
        setRefreshTask(new FunctionObject() {

            public void function() {
                loadUser(user.getUsername());
                loadUser("public");
            }
        });
    }

    public void init() {
        String[] s1 = { "MyNetworks", "View My Networks" };
        String[] s2 = { "AdminNetworks", "Admin Networks" };
        String[] s3 = { "AdminNetworksByUser", "View Networks By User" };
        String[] s4 = { "CreateNet", "Create New Network" };
        admin_sidebar.add(s1);
        admin_sidebar.add(s2);
        admin_sidebar.add(s3);
        admin_sidebar.add(s4);
        user_sidebar.clear();
        user_sidebar.add(s1);
        user_sidebar.add(s4);
        user_icons.clear();
        String[] i1 = { "MyNetworks", "d4", "View My Networks" };
        String[] i2 = { "AdminNetworks", "d4", "Admin Networks" };
        String[] i3 = { "AdminNetworksByUser", "d4", "View Networks By User" };
        String[] i4 = { "CreateNet", "d4", "Create New Network" };
        admin_icons.add(i1);
        admin_icons.add(i2);
        admin_icons.add(i3);
        admin_icons.add(i4);
        user_icons.add(i1);
        user_icons.add(i4);
        category = "Users";
    }
}
