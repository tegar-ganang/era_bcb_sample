package gateway;

import java.util.*;
import java.io.*;

public class networkManager extends fileManager {

    private String ifconfig;

    private String iwconfig;

    private ArrayList allInterfaces;

    private ArrayList wirelessInterfaces;

    private shellManager shell;

    /**
   *
   * @param shell
   */
    public networkManager(shellManager shell) {
        setPaths();
        this.shell = shell;
        allInterfaces = findInterfaces();
        wirelessInterfaces = findWirelessInterfaces();
    }

    /**
   *
   */
    public void setPaths() {
        ifconfig = findLocation("ifconfig");
        iwconfig = findLocation("iwconfig");
    }

    /**
   *
   * @return
   */
    public ArrayList getInterfaces() {
        return allInterfaces;
    }

    /**
   *
   * @return
   */
    private ArrayList findInterfaces() {
        setPaths();
        ArrayList interfaces = new ArrayList();
        String[] line, eth;
        String temp = shell.sendCommand(ifconfig + " -a");
        line = temp.split("\n");
        for (int i = 0; i < line.length; i++) {
            eth = line[i].split(" ");
            if (!eth[0].equals("")) {
                interfaces.add(eth[0]);
            }
        }
        for (int i = 0; i < interfaces.size(); i++) {
            System.out.println("Available Wireless interfaces: \n" + interfaces.get(i));
        }
        return interfaces;
    }

    /**
   *
   * @return
   */
    public ArrayList getWirelessInterfaces() {
        return wirelessInterfaces;
    }

    /**
   *
   * @return
   */
    private ArrayList findWirelessInterfaces() {
        ArrayList interfaces = new ArrayList();
        String[] line, eth;
        String temp = shell.sendCommand(iwconfig);
        line = temp.split("\n");
        for (int i = 0; i < line.length; i++) {
            eth = line[i].split(" ");
            if (!eth[0].equals("")) {
                interfaces.add(eth[0]);
            }
        }
        for (int i = 0; i < interfaces.size(); i++) {
            System.out.println("Available Wireless interfaces: \n" + interfaces.get(i));
        }
        return interfaces;
    }

    /**
   *
   * @param interfce
   * @param ip
   */
    public void setIP(String interfce, String ip) {
        setPaths();
        shell.exeCommand(ifconfig + " " + interfce + " down");
        shell.exeCommand(ifconfig + " " + interfce + " " + ip);
        shell.exeCommand(ifconfig + " " + interfce + " up");
    }

    /**
   *
   * @param essid
   * @param interfce
   */
    public void createAdHoc(String essid, String interfce) {
        setPaths();
        shell.exeCommand(iwconfig + " " + interfce + " essid " + " " + essid);
        System.out.println(iwconfig + " " + interfce + " essid " + '"' + essid + '"');
        shell.exeCommand(iwconfig + " " + interfce + " mode Ad-Hoc");
        System.out.println(iwconfig + " " + interfce + " mode Ad-Hoc");
    }

    /**
   *
   * @param wirelessInterface
   * @return
   */
    public ArrayList getAvailableNetworks(String wirelessInterface) {
        String result;
        ArrayList networks = new ArrayList();
        String[] line, element, part;
        result = shell.sendCommand("iwlist " + wirelessInterface + " scanning");
        line = result.split("\n");
        String[] property = { "", "" };
        for (int i = 0; i < line.length; i++) {
            element = line[i].split(":");
            for (int j = 0; j < element.length; j++) {
                if (element[j].endsWith("ESSID") || element[j].endsWith("Mode")) {
                    if (element[j].endsWith("ESSID")) {
                        property[1] = element[j + 1];
                    } else {
                        property[0] = element[j + 1];
                        networks.add(property);
                        property = (String[]) property.clone();
                    }
                }
            }
        }
        return networks;
    }

    /**
   *
   * @param Interface
   * @return
   */
    public ArrayList getInterfaceAddress(String Interface) {
        setPaths();
        ArrayList interfaces = new ArrayList();
        String[] line, eth;
        String temp = shell.sendCommand(ifconfig + " " + Interface);
        String netBits = "";
        line = temp.split("\n");
        for (int i = 0; i < line.length; i++) {
            eth = line[i].split(" ");
            for (int j = 0; j < eth.length; j++) {
                if (eth[j].startsWith("Bcast")) {
                    String[] element = eth[j].split(":");
                    interfaces.add(element[1]);
                    String[] netpart = element[1].split("255");
                    netBits = netpart[0].substring(0, netpart[0].length() - 1);
                    String networkAddress = element[1].replaceAll("255", "0");
                    interfaces.add(networkAddress);
                    interfaces.add(netBits);
                }
                if (eth[j].startsWith("Mask")) {
                    String[] element = eth[j].split(":");
                    interfaces.add(element[1]);
                }
            }
        }
        return interfaces;
    }

    public String dhcpAddress(String interfName) {
        String path = "/var/lib/dhcp/dhclient-" + interfName + ".leases";
        fileManager fileman = new fileManager();
        String dhcpAddress = "";
        String contents = fileman.getContents(new File(path));
        String[] lines = contents.split("\n");
        boolean found = false;
        for (int i = 0; i < lines.length; i++) {
            String[] elements = lines[i].split(" ");
            for (int j = 0; j < elements.length; j++) {
                if (elements[j].equals("dhcp-server-identifier")) {
                    dhcpAddress = elements[j + 1].replaceAll(";", "");
                    found = true;
                    break;
                }
            }
            if (found) break;
        }
        return dhcpAddress;
    }
}
