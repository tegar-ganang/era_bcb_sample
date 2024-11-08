package com.pspkvm.system;

public class WifiStatus {

    public static native boolean isPowerOn();

    public static native String getMACAddr();

    public static native boolean isSwitchOn();

    public static native String getBSSID();

    public static native String getProfileName();

    public static native String getSSID();

    public static native String getIP();

    public static native String getSubnetMask();

    public static native String getGateway();

    public static native String getPrimaryDNS();

    public static native String getSecondaryDNS();

    public static native String getProxyURL();

    public static native int getSecurityType();

    public static native int getSignalStrength();

    public static native int getChannel();

    public static native int getPowerSave();

    public static native int getUseProxy();

    public static native int getProxyPort();

    public static native int getEAPType();

    public static native int getStartBrowser();

    public static native int getUseWiFiSP();

    public static final int PSP_NET_UNKNOWN = -1;

    public static final int PSP_SECURITY_TYPE_NONE = 0;

    public static final int PSP_SECURITY_TYPE_WEP = 1;

    public static final int PSP_SECURITY_TYPE_WPA = 2;

    public static final int PSP_EAP_TYPE_NONE = 0;

    public static final int PSP_EAP_TYPE_EAP_MD5 = 1;

    public static final int PSP_NET_TRUE = 1;

    public static final int PSP_NET_FALSE = 0;

    public static String netBoolToString(int i) {
        switch(i) {
            case PSP_NET_UNKNOWN:
                return "<unknown>";
            case PSP_NET_TRUE:
                return "yes";
            case PSP_NET_FALSE:
                return "no";
            default:
                return "<unknown>";
        }
    }

    public static String getEA5ModeStr() {
        int i = getEAPType();
        switch(i) {
            case PSP_NET_UNKNOWN:
                return "<unknown>";
            case PSP_EAP_TYPE_NONE:
                return "None";
            case PSP_EAP_TYPE_EAP_MD5:
                return "EAP-MD5";
            default:
                return "<unknown>";
        }
    }

    public static String getSecurityTypeStr() {
        int i = getSecurityType();
        switch(i) {
            case PSP_NET_UNKNOWN:
                return "<unknown>";
            case PSP_SECURITY_TYPE_NONE:
                return "None";
            case PSP_SECURITY_TYPE_WEP:
                return "WEP";
            case PSP_SECURITY_TYPE_WPA:
                return "WPA";
            default:
                return "<unknown>";
        }
    }

    public static String getSignalStrengthStr() {
        int i = getSignalStrength();
        if (i == -1) {
            return "<unknown>";
        }
        return Integer.toString(i) + "%";
    }

    static String netIntToString(int i) {
        if (i == -1) {
            return "<unknown>";
        }
        return Integer.toString(i);
    }

    static String formatConfigString(String n, String v) {
        return n + ": " + ((v == null) ? "<unknown>" : v);
    }

    static String formatConfigString(String n, boolean b) {
        Boolean bl = new Boolean(b);
        return n + ": " + bl.toString();
    }

    public static String statusReport() {
        return formatConfigString("WLAN power", isPowerOn()) + "\n" + formatConfigString("WLAN switch", isSwitchOn()) + "\n" + formatConfigString("MAC", getMACAddr()) + "\n" + formatConfigString("Profile", getProfileName()) + "\n" + formatConfigString("BSSID", getBSSID()) + "\n" + formatConfigString("SSID", getSSID()) + "\n" + formatConfigString("Security", getSecurityTypeStr()) + "\n" + formatConfigString("Signal Str", getSignalStrengthStr()) + "\n" + formatConfigString("Channel", netIntToString(getChannel())) + "\n" + formatConfigString("Power Save", netBoolToString(getPowerSave())) + "\n" + formatConfigString("IP", getIP()) + "\n" + formatConfigString("Subnet", getSubnetMask()) + "\n" + formatConfigString("Gateway", getGateway()) + "\n" + formatConfigString("DNS 1", getPrimaryDNS()) + "\n" + formatConfigString("DNS 2", getSecondaryDNS()) + "\n" + formatConfigString("EAP Mode", getEA5ModeStr());
    }
}
