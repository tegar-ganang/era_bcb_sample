package org.blue.star.base;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.blue.star.common.objects;
import org.blue.star.include.blue_h;
import org.blue.star.include.broker_h;
import org.blue.star.include.common_h;
import org.blue.star.include.config_h;
import org.blue.star.include.locations_h;
import org.blue.star.include.nebmodules_h;
import org.blue.star.include.objects_h;

public class utils {

    /** Logger instance */
    private static Logger logger = LogManager.getLogger("org.blue.base.utils");

    private static String cn = "org.blue.base.utils";

    public static String process_macros(String input_buffer, int options) {
        String temp_buffer;
        boolean in_macro;
        int x;
        int arg_index = 0;
        int user_index = 0;
        int address_index = 0;
        String selected_macro = null;
        boolean clean_macro = false;
        boolean found_macro_x = false;
        String output_buffer = "";
        logger.trace("entering " + cn + ".process_macros");
        if (input_buffer == null) return null;
        in_macro = false;
        logger.debug("**** BEGIN MACRO PROCESSING ***********");
        logger.debug("Processing:  " + input_buffer);
        String split[] = input_buffer.split("\\$");
        for (int s = 0; s < split.length; s++) {
            temp_buffer = split[s];
            logger.debug("  Processing part: '" + temp_buffer + "'");
            selected_macro = null;
            found_macro_x = false;
            clean_macro = false;
            if (in_macro == false) {
                output_buffer += temp_buffer;
                logger.debug("    Not currently in macro.  Running output (" + output_buffer.length() + "): '" + output_buffer + "'");
                in_macro = true;
            } else {
                for (x = 0; x < blue_h.MACRO_X_COUNT; x++) {
                    if (blue.macro_x_names[x] != null && temp_buffer.equals(blue.macro_x_names[x])) {
                        selected_macro = blue.macro_x[x];
                        found_macro_x = true;
                        if (x >= 16 && x <= 19) {
                            clean_macro = true;
                            options &= blue_h.STRIP_ILLEGAL_MACRO_CHARS | blue_h.ESCAPE_MACRO_CHARS;
                        }
                        break;
                    }
                }
                if (found_macro_x == true) x = 0; else if (temp_buffer.indexOf("HOST") > 0 && temp_buffer.indexOf(":") > 0) {
                    grab_on_demand_macro(temp_buffer);
                    selected_macro = blue.macro_ondemand;
                    if (temp_buffer.startsWith("HOSTOUTPUT:") || temp_buffer.indexOf("HOSTPERFDATA:") > 0) {
                        clean_macro = true;
                        options &= blue_h.STRIP_ILLEGAL_MACRO_CHARS | blue_h.ESCAPE_MACRO_CHARS;
                    }
                } else if (temp_buffer.contains("SERVICE") && temp_buffer.contains(":")) {
                    grab_on_demand_macro(temp_buffer);
                    selected_macro = blue.macro_ondemand;
                    if (temp_buffer.startsWith("SERVICEOUTPUT:") || temp_buffer.contains("SERVICEPERFDATA:")) {
                        clean_macro = true;
                        options &= blue_h.STRIP_ILLEGAL_MACRO_CHARS | blue_h.ESCAPE_MACRO_CHARS;
                    }
                } else if (temp_buffer.startsWith("ARG")) {
                    arg_index = atoi(temp_buffer.substring(3));
                    if (arg_index >= 1 && arg_index <= blue_h.MAX_COMMAND_ARGUMENTS) selected_macro = blue.macro_argv[arg_index - 1]; else selected_macro = null;
                } else if (temp_buffer.startsWith("USER")) {
                    user_index = atoi(temp_buffer.substring(4));
                    if (user_index >= 1 && user_index <= blue_h.MAX_USER_MACROS) selected_macro = blue.macro_user[user_index - 1]; else selected_macro = null;
                } else if (temp_buffer.startsWith("CONTACTADDRESS")) {
                    address_index = atoi(temp_buffer.substring(14));
                    if (address_index >= 1 && address_index <= objects_h.MAX_CONTACT_ADDRESSES) selected_macro = blue.macro_contactaddress[address_index - 1]; else selected_macro = null;
                } else if (temp_buffer.equals("")) {
                    logger.debug("    Escaped $. Running output (" + output_buffer.length() + "): '" + output_buffer + "'\n");
                    output_buffer += "$";
                } else {
                    logger.debug("    Non-macro.  Running output (" + output_buffer.length() + "): '" + output_buffer + "'\n");
                    output_buffer += "$";
                    output_buffer += temp_buffer;
                    output_buffer += "$";
                }
                if (selected_macro != null) {
                    if ((options & blue_h.URL_ENCODE_MACRO_CHARS) > 0) selected_macro = get_url_encoded_string(selected_macro);
                    if (clean_macro == true || (((options & blue_h.STRIP_ILLEGAL_MACRO_CHARS) > 0) || ((options & blue_h.ESCAPE_MACRO_CHARS) > 0))) output_buffer += ((selected_macro == null) ? "" : clean_macro_chars(selected_macro, options)); else output_buffer += ((selected_macro == null) ? "" : selected_macro);
                    if ((options & blue_h.URL_ENCODE_MACRO_CHARS) > 0) selected_macro = null;
                    logger.debug("    Just finished macro.  Running output (" + output_buffer.length() + "): '" + output_buffer + "'\n");
                }
                in_macro = false;
            }
        }
        logger.debug("Done.  Final output: '" + output_buffer + "'");
        logger.debug("**** END MACRO PROCESSING *************");
        logger.trace("exiting " + cn + ".process_macros");
        return output_buffer;
    }

    public static void grab_service_macros(objects_h.service svc) {
        objects_h.serviceextinfo temp_serviceextinfo;
        long current_time;
        long duration;
        int days;
        int hours;
        int minutes;
        int seconds;
        logger.trace("entering " + cn + ".grab_service_macros");
        blue.macro_x[blue_h.MACRO_SERVICEDESC] = svc.description;
        blue.macro_x[blue_h.MACRO_SERVICEOUTPUT] = svc.plugin_output;
        blue.macro_x[blue_h.MACRO_SERVICEPERFDATA] = svc.perf_data;
        blue.macro_x[blue_h.MACRO_SERVICECHECKCOMMAND] = svc.service_check_command;
        blue.macro_x[blue_h.MACRO_SERVICECHECKTYPE] = ((svc.check_type == common_h.SERVICE_CHECK_PASSIVE) ? "PASSIVE" : "ACTIVE");
        blue.macro_x[blue_h.MACRO_SERVICESTATETYPE] = ((svc.state_type == common_h.HARD_STATE) ? "HARD" : "SOFT");
        if (svc.current_state == blue_h.STATE_OK) blue.macro_x[blue_h.MACRO_SERVICESTATE] = "OK"; else if (svc.current_state == blue_h.STATE_WARNING) blue.macro_x[blue_h.MACRO_SERVICESTATE] = "WARNING"; else if (svc.current_state == blue_h.STATE_CRITICAL) blue.macro_x[blue_h.MACRO_SERVICESTATE] = "CRITICAL"; else blue.macro_x[blue_h.MACRO_SERVICESTATE] = "UNKNOWN";
        blue.macro_x[blue_h.MACRO_SERVICESTATEID] = "" + svc.current_state;
        blue.macro_x[blue_h.MACRO_SERVICEATTEMPT] = "" + svc.current_attempt;
        blue.macro_x[blue_h.MACRO_SERVICEEXECUTIONTIME] = "" + svc.execution_time;
        blue.macro_x[blue_h.MACRO_SERVICELATENCY] = "" + svc.latency;
        blue.macro_x[blue_h.MACRO_LASTSERVICECHECK] = "" + svc.last_check;
        blue.macro_x[blue_h.MACRO_LASTSERVICESTATECHANGE] = "" + svc.last_state_change;
        blue.macro_x[blue_h.MACRO_LASTSERVICEOK] = "" + svc.last_time_ok;
        blue.macro_x[blue_h.MACRO_LASTSERVICEWARNING] = "" + svc.last_time_warning;
        blue.macro_x[blue_h.MACRO_LASTSERVICEUNKNOWN] = "" + svc.last_time_unknown;
        blue.macro_x[blue_h.MACRO_LASTSERVICECRITICAL] = "" + svc.last_time_critical;
        blue.macro_x[blue_h.MACRO_SERVICEDOWNTIME] = "" + svc.scheduled_downtime_depth;
        blue.macro_x[blue_h.MACRO_SERVICEPERCENTCHANGE] = "" + svc.percent_state_change;
        current_time = utils.currentTimeInSeconds();
        duration = (current_time - svc.last_state_change);
        blue.macro_x[blue_h.MACRO_SERVICEDURATIONSEC] = "" + duration;
        days = (int) duration / 86400;
        duration -= (days * 86400);
        hours = (int) duration / 3600;
        duration -= (hours * 3600);
        minutes = (int) duration / 60;
        duration -= (minutes * 60);
        seconds = (int) duration;
        blue.macro_x[blue_h.MACRO_SERVICEDURATION] = days + "d " + hours + "h " + minutes + "m " + seconds + "s";
        for (objects_h.servicegroup temp_servicegroup : (ArrayList<objects_h.servicegroup>) objects.servicegroup_list) {
            if (objects.is_service_member_of_servicegroup(temp_servicegroup, svc) == common_h.TRUE) {
                blue.macro_x[blue_h.MACRO_SERVICEGROUPNAME] = temp_servicegroup.group_name;
                blue.macro_x[blue_h.MACRO_SERVICEGROUPALIAS] = temp_servicegroup.alias;
                break;
            }
        }
        temp_serviceextinfo = objects.find_serviceextinfo(svc.host_name, svc.description);
        if (temp_serviceextinfo != null) {
            blue.macro_x[blue_h.MACRO_SERVICEACTIONURL] = temp_serviceextinfo.action_url;
            blue.macro_x[blue_h.MACRO_SERVICENOTESURL] = temp_serviceextinfo.notes_url;
            blue.macro_x[blue_h.MACRO_SERVICENOTES] = temp_serviceextinfo.notes;
        }
        grab_datetime_macros();
        blue.macro_x[blue_h.MACRO_SERVICEOUTPUT] = strip(blue.macro_x[blue_h.MACRO_SERVICEOUTPUT]);
        blue.macro_x[blue_h.MACRO_SERVICEPERFDATA] = strip(blue.macro_x[blue_h.MACRO_SERVICEPERFDATA]);
        blue.macro_x[blue_h.MACRO_SERVICECHECKCOMMAND] = strip(blue.macro_x[blue_h.MACRO_SERVICECHECKCOMMAND]);
        blue.macro_x[blue_h.MACRO_SERVICENOTES] = strip(blue.macro_x[blue_h.MACRO_SERVICENOTES]);
        blue.macro_x[blue_h.MACRO_SERVICEACTIONURL] = process_macros(blue.macro_x[blue_h.MACRO_SERVICEACTIONURL], blue_h.URL_ENCODE_MACRO_CHARS);
        blue.macro_x[blue_h.MACRO_SERVICENOTESURL] = process_macros(blue.macro_x[blue_h.MACRO_SERVICENOTESURL], blue_h.URL_ENCODE_MACRO_CHARS);
        logger.trace("exiting " + cn + ".grab_service_macros");
    }

    public static void grab_host_macros(objects_h.host hst) {
        objects_h.hostextinfo temp_hostextinfo;
        long current_time;
        long duration;
        int days;
        int hours;
        int minutes;
        int seconds;
        logger.trace("entering " + cn + ".grab_host_macros");
        blue.macro_x[blue_h.MACRO_HOSTNAME] = hst.name;
        blue.macro_x[blue_h.MACRO_HOSTALIAS] = hst.alias;
        blue.macro_x[blue_h.MACRO_HOSTADDRESS] = hst.address;
        if (hst.current_state == blue_h.HOST_DOWN) blue.macro_x[blue_h.MACRO_HOSTSTATE] = "DOWN"; else if (hst.current_state == blue_h.HOST_UNREACHABLE) blue.macro_x[blue_h.MACRO_HOSTSTATE] = "UNREACHABLE"; else blue.macro_x[blue_h.MACRO_HOSTSTATE] = "UP";
        blue.macro_x[blue_h.MACRO_HOSTSTATEID] = "" + hst.current_state;
        blue.macro_x[blue_h.MACRO_HOSTCHECKTYPE] = ((hst.check_type == common_h.HOST_CHECK_PASSIVE) ? "PASSIVE" : "ACTIVE");
        blue.macro_x[blue_h.MACRO_HOSTSTATETYPE] = ((hst.state_type == common_h.HARD_STATE) ? "HARD" : "SOFT");
        blue.macro_x[blue_h.MACRO_HOSTOUTPUT] = hst.plugin_output;
        blue.macro_x[blue_h.MACRO_HOSTPERFDATA] = hst.perf_data;
        blue.macro_x[blue_h.MACRO_HOSTCHECKCOMMAND] = hst.host_check_command;
        blue.macro_x[blue_h.MACRO_HOSTATTEMPT] = "" + hst.current_attempt;
        blue.macro_x[blue_h.MACRO_HOSTDOWNTIME] = "" + hst.scheduled_downtime_depth;
        blue.macro_x[blue_h.MACRO_HOSTPERCENTCHANGE] = "" + hst.percent_state_change;
        current_time = utils.currentTimeInSeconds();
        duration = (current_time - hst.last_state_change);
        blue.macro_x[blue_h.MACRO_HOSTDURATIONSEC] = "" + duration;
        days = (int) duration / 86400;
        duration -= (days * 86400);
        hours = (int) duration / 3600;
        duration -= (hours * 3600);
        minutes = (int) duration / 60;
        duration -= (minutes * 60);
        seconds = (int) duration;
        blue.macro_x[blue_h.MACRO_HOSTDURATION] = "" + days + " " + hours + " " + minutes + " " + seconds;
        blue.macro_x[blue_h.MACRO_HOSTEXECUTIONTIME] = "" + hst.execution_time;
        blue.macro_x[blue_h.MACRO_HOSTLATENCY] = "" + hst.latency;
        blue.macro_x[blue_h.MACRO_LASTHOSTCHECK] = "" + hst.last_check;
        blue.macro_x[blue_h.MACRO_LASTHOSTSTATECHANGE] = "" + hst.last_state_change;
        blue.macro_x[blue_h.MACRO_LASTHOSTUP] = "" + hst.last_time_up;
        blue.macro_x[blue_h.MACRO_LASTHOSTDOWN] = "" + hst.last_time_down;
        blue.macro_x[blue_h.MACRO_LASTHOSTUNREACHABLE] = "" + hst.last_time_unreachable;
        for (objects_h.hostgroup temp_hostgroup : (ArrayList<objects_h.hostgroup>) objects.hostgroup_list) {
            if (objects.is_host_member_of_hostgroup(temp_hostgroup, hst) == common_h.TRUE) {
                blue.macro_x[blue_h.MACRO_HOSTGROUPNAME] = temp_hostgroup.group_name;
                blue.macro_x[blue_h.MACRO_HOSTGROUPALIAS] = temp_hostgroup.alias;
                break;
            }
        }
        temp_hostextinfo = objects.find_hostextinfo(hst.name);
        if (temp_hostextinfo != null) {
            blue.macro_x[blue_h.MACRO_HOSTACTIONURL] = temp_hostextinfo.action_url;
            blue.macro_x[blue_h.MACRO_HOSTNOTESURL] = temp_hostextinfo.notes_url;
            blue.macro_x[blue_h.MACRO_HOSTNOTES] = temp_hostextinfo.notes;
        }
        grab_datetime_macros();
        blue.macro_x[blue_h.MACRO_HOSTOUTPUT] = strip(blue.macro_x[blue_h.MACRO_HOSTOUTPUT]);
        blue.macro_x[blue_h.MACRO_HOSTPERFDATA] = strip(blue.macro_x[blue_h.MACRO_HOSTPERFDATA]);
        blue.macro_x[blue_h.MACRO_HOSTCHECKCOMMAND] = strip(blue.macro_x[blue_h.MACRO_HOSTCHECKCOMMAND]);
        blue.macro_x[blue_h.MACRO_HOSTNOTES] = strip(blue.macro_x[blue_h.MACRO_HOSTNOTES]);
        blue.macro_x[blue_h.MACRO_HOSTACTIONURL] = process_macros(blue.macro_x[blue_h.MACRO_HOSTACTIONURL], blue_h.URL_ENCODE_MACRO_CHARS);
        blue.macro_x[blue_h.MACRO_HOSTNOTESURL] = process_macros(blue.macro_x[blue_h.MACRO_HOSTNOTESURL], blue_h.URL_ENCODE_MACRO_CHARS);
        logger.trace("exiting " + cn + ".grab_host_macros");
    }

    public static int grab_on_demand_macro(String str) {
        String macro = null;
        StringBuffer result_buffer = new StringBuffer();
        objects_h.host temp_host;
        objects_h.hostgroup temp_hostgroup;
        objects_h.hostgroupmember temp_hostgroupmember;
        objects_h.service temp_service;
        objects_h.servicegroup temp_servicegroup;
        objects_h.servicegroupmember temp_servicegroupmember;
        int return_val = common_h.ERROR;
        logger.trace("entering " + cn + ".grab_on_demand_macro");
        blue.macro_ondemand = null;
        String[] split = macro.split("\\:");
        if (macro.indexOf("HOST") >= 0) {
            if (split.length == 1) {
                temp_host = objects.find_host(split[0]);
                return_val = grab_on_demand_host_macro(temp_host, macro);
            } else {
                temp_hostgroup = objects.find_hostgroup(split[0]);
                if (temp_hostgroup == null) return common_h.ERROR;
                if (temp_hostgroup.members == null || temp_hostgroup.members.size() == 0) {
                    blue.macro_ondemand = "";
                    return common_h.OK;
                }
                return_val = common_h.OK;
                ListIterator iterator = temp_hostgroup.members.listIterator();
                temp_hostgroupmember = (objects_h.hostgroupmember) iterator.next();
                while (true) {
                    temp_host = objects.find_host(temp_hostgroupmember.host_name);
                    if (grab_on_demand_host_macro(temp_host, macro) == common_h.OK) {
                        result_buffer.append(blue.macro_ondemand);
                        if (!iterator.hasNext()) break;
                        temp_hostgroupmember = (objects_h.hostgroupmember) iterator.next();
                        result_buffer.append(split[1]);
                    } else {
                        return_val = common_h.ERROR;
                        if (!iterator.hasNext()) break;
                        temp_hostgroupmember = (objects_h.hostgroupmember) iterator.next();
                    }
                    blue.macro_ondemand = null;
                }
                blue.macro_ondemand = result_buffer.toString();
            }
        } else if (macro.indexOf("SERVICE") >= 0) {
            if (split.length == 1) return common_h.ERROR;
            temp_service = objects.find_service(split[0], split[1]);
            if (temp_service != null) return_val = grab_on_demand_service_macro(temp_service, macro); else {
                temp_servicegroup = objects.find_servicegroup(split[0]);
                if (temp_servicegroup == null) return common_h.ERROR;
                if (temp_servicegroup.members == null || temp_servicegroup.members.size() == 0) {
                    blue.macro_ondemand = "";
                    return common_h.OK;
                }
                return_val = common_h.OK;
                result_buffer = new StringBuffer();
                ListIterator iterator = temp_servicegroup.members.listIterator();
                temp_servicegroupmember = (objects_h.servicegroupmember) iterator.next();
                while (true) {
                    temp_service = objects.find_service(temp_servicegroupmember.host_name, temp_servicegroupmember.service_description);
                    if (grab_on_demand_service_macro(temp_service, macro) == common_h.OK) {
                        result_buffer.append(blue.macro_ondemand);
                        if (!iterator.hasNext()) break;
                        temp_servicegroupmember = (objects_h.servicegroupmember) iterator.next();
                        result_buffer.append(split[1]);
                    } else {
                        return_val = common_h.ERROR;
                        if (!iterator.hasNext()) break;
                        temp_servicegroupmember = (objects_h.servicegroupmember) iterator.next();
                    }
                    blue.macro_ondemand = null;
                }
                blue.macro_ondemand = result_buffer.toString();
            }
        } else return_val = common_h.ERROR;
        logger.trace("exiting " + cn + ".grab_on_demand_macro");
        return return_val;
    }

    public static int grab_on_demand_host_macro(objects_h.host hst, String macro) {
        objects_h.hostgroup temp_hostgroup = null;
        objects_h.hostextinfo temp_hostextinfo;
        long current_time;
        long duration;
        int days;
        int hours;
        int minutes;
        int seconds;
        logger.trace("entering " + cn + ".grab_on_demand_host_macro");
        if (hst == null || macro == null) return common_h.ERROR;
        blue.macro_ondemand = null;
        current_time = utils.currentTimeInSeconds();
        duration = (current_time - hst.last_state_change);
        for (ListIterator iter = objects.hostgroup_list.listIterator(); iter.hasNext(); ) {
            temp_hostgroup = (objects_h.hostgroup) iter.next();
            if (objects.is_host_member_of_hostgroup(temp_hostgroup, hst) == common_h.TRUE) break;
        }
        if (macro.equals("HOSTALIAS")) blue.macro_ondemand = hst.alias; else if (macro.equals("HOSTADDRESS")) blue.macro_ondemand = hst.address; else if (macro.equals("HOSTSTATE")) {
            if (hst.current_state == blue_h.HOST_DOWN) blue.macro_ondemand = "DOWN"; else if (hst.current_state == blue_h.HOST_UNREACHABLE) blue.macro_ondemand = "UNREACHABLE"; else blue.macro_ondemand = "UP";
        } else if (macro.equals("HOSTSTATEID")) {
            blue.macro_ondemand = "" + hst.current_state;
        } else if (macro.equals("HOSTCHECKTYPE")) {
            blue.macro_ondemand = ((hst.check_type == common_h.HOST_CHECK_PASSIVE) ? "PASSIVE" : "ACTIVE");
        } else if (macro.equals("HOSTSTATETYPE")) {
            blue.macro_ondemand = ((hst.state_type == common_h.HARD_STATE) ? "HARD" : "SOFT");
        } else if (macro.equals("HOSTOUTPUT")) {
            blue.macro_ondemand = hst.plugin_output;
        } else if (macro.equals("HOSTPERFDATA")) {
            blue.macro_ondemand = hst.perf_data;
        } else if (macro.equals("HOSTATTEMPT")) {
            blue.macro_ondemand = "" + hst.current_attempt;
        } else if (macro.equals("HOSTDOWNTIME")) {
            blue.macro_ondemand = "" + hst.scheduled_downtime_depth;
        } else if (macro.equals("HOSTPERCENTCHANGE")) {
            blue.macro_ondemand = "" + hst.percent_state_change;
        } else if (macro.equals("HOSTDURATIONSEC")) {
            blue.macro_ondemand = "" + duration;
        } else if (macro.equals("HOSTDURATION")) {
            days = (int) duration / 86400;
            duration -= (days * 86400);
            hours = (int) duration / 3600;
            duration -= (hours * 3600);
            minutes = (int) duration / 60;
            duration -= (minutes * 60);
            seconds = (int) duration;
            blue.macro_ondemand = days + " " + hours + " " + minutes + " " + seconds;
        } else if (macro.equals("HOSTEXECUTIONTIME")) {
            blue.macro_ondemand = "" + hst.execution_time;
        } else if (macro.equals("HOSTLATENCY")) {
            blue.macro_ondemand = "" + hst.latency;
        } else if (macro.equals("LASTHOSTCHECK")) {
            blue.macro_ondemand = "" + hst.last_check;
        } else if (macro.equals("LASTHOSTSTATECHANGE")) {
            blue.macro_ondemand = "" + hst.last_state_change;
        } else if (macro.equals("LASTHOSTUP")) {
            blue.macro_ondemand = "" + hst.last_time_up;
        } else if (macro.equals("LASTHOSTDOWN")) {
            blue.macro_ondemand = "" + hst.last_time_down;
        } else if (macro.equals("LASTHOSTUNREACHABLE")) {
            blue.macro_ondemand = "" + hst.last_time_unreachable;
        } else if (macro.equals("HOSTGROUPNAME") && temp_hostgroup != null) {
            blue.macro_ondemand = temp_hostgroup.group_name;
        } else if (macro.equals("HOSTGROUPALIAS") && temp_hostgroup != null) {
            blue.macro_ondemand = temp_hostgroup.alias;
        } else if (macro.equals("HOSTACTIONURL") || macro.equals("HOSTNOTESURL") || macro.equals("HOSTNOTES")) {
            temp_hostextinfo = objects.find_hostextinfo(hst.name);
            if (temp_hostextinfo != null) {
                if (macro.equals("HOSTACTIONURL")) {
                    if (temp_hostextinfo.action_url != null) {
                        blue.macro_ondemand = process_macros(temp_hostextinfo.action_url, blue_h.URL_ENCODE_MACRO_CHARS);
                    }
                }
                if (macro.equals("HOSTNOTESURL")) {
                    if (temp_hostextinfo.notes_url != null) {
                        blue.macro_ondemand = process_macros(temp_hostextinfo.notes_url, blue_h.URL_ENCODE_MACRO_CHARS);
                    }
                }
                if (macro.equals("HOSTNOTES")) {
                    blue.macro_ondemand = temp_hostextinfo.notes;
                }
            }
        } else return common_h.ERROR;
        logger.trace("exiting " + cn + ".grab_on_demand_host_macro");
        return common_h.OK;
    }

    public static int grab_on_demand_service_macro(objects_h.service svc, String macro) {
        objects_h.servicegroup temp_servicegroup = null;
        objects_h.serviceextinfo temp_serviceextinfo;
        long current_time;
        long duration;
        int days;
        int hours;
        int minutes;
        int seconds;
        logger.trace("entering " + cn + ".grab_on_demand_service_macro");
        if (svc == null || macro == null) return common_h.ERROR;
        blue.macro_ondemand = null;
        current_time = utils.currentTimeInSeconds();
        duration = (current_time - svc.last_state_change);
        for (ListIterator iter = objects.servicegroup_list.listIterator(); iter.hasNext(); ) {
            temp_servicegroup = (objects_h.servicegroup) iter.next();
            if (objects.is_service_member_of_servicegroup(temp_servicegroup, svc) == common_h.TRUE) break;
        }
        if (macro.equals("SERVICEOUTPUT")) {
            blue.macro_ondemand = svc.plugin_output;
        } else if (macro.equals("SERVICEPERFDATA")) {
            blue.macro_ondemand = svc.perf_data;
        } else if (macro.equals("SERVICECHECKTYPE")) {
            blue.macro_ondemand = ((svc.check_type == common_h.SERVICE_CHECK_PASSIVE) ? "PASSIVE" : "ACTIVE");
        } else if (macro.equals("SERVICESTATETYPE")) {
            blue.macro_ondemand = ((svc.state_type == common_h.HARD_STATE) ? "HARD" : "SOFT");
        } else if (macro.equals("SERVICESTATE")) {
            if (svc.current_state == blue_h.STATE_OK) blue.macro_ondemand = "OK"; else if (svc.current_state == blue_h.STATE_WARNING) blue.macro_ondemand = "WARNING"; else if (svc.current_state == blue_h.STATE_CRITICAL) blue.macro_ondemand = "CRITICAL"; else blue.macro_ondemand = "UNKNOWN";
        } else if (macro.equals("SERVICESTATEID")) {
            blue.macro_ondemand = "" + svc.current_state;
        } else if (macro.equals("SERVICEATTEMPT")) {
            blue.macro_ondemand = "" + svc.current_attempt;
        } else if (macro.equals("SERVICEEXECUTIONTIME")) {
            blue.macro_ondemand = "" + svc.execution_time;
        } else if (macro.equals("SERVICELATENCY")) {
            blue.macro_ondemand = "" + svc.latency;
        } else if (macro.equals("LASTSERVICECHECK")) {
            blue.macro_ondemand = "" + svc.last_check;
        } else if (macro.equals("LASTSERVICESTATECHANGE")) {
            blue.macro_ondemand = "" + svc.last_state_change;
        } else if (macro.equals("LASTSERVICEOK")) {
            blue.macro_ondemand = "" + svc.last_time_ok;
        } else if (macro.equals("LASTSERVICEWARNING")) {
            blue.macro_ondemand = "" + svc.last_time_warning;
        } else if (macro.equals("LASTSERVICEUNKNOWN")) {
            blue.macro_ondemand = "" + svc.last_time_unknown;
        } else if (macro.equals("LASTSERVICECRITICAL")) {
            blue.macro_ondemand = "" + svc.last_time_critical;
        } else if (macro.equals("SERVICEDOWNTIME")) {
            blue.macro_ondemand = "" + svc.scheduled_downtime_depth;
        } else if (macro.equals("SERVICEPERCENTCHANGE")) {
            blue.macro_ondemand = "" + svc.percent_state_change;
        } else if (macro.equals("SERVICEDURATIONSEC")) {
            blue.macro_ondemand = "" + duration;
        } else if (macro.equals("SERVICEDURATION")) {
            days = (int) duration / 86400;
            duration -= (days * 86400);
            hours = (int) duration / 3600;
            duration -= (hours * 3600);
            minutes = (int) duration / 60;
            duration -= (minutes * 60);
            seconds = (int) duration;
            blue.macro_ondemand = "" + days + " " + hours + " " + minutes + "  " + seconds;
        } else if (macro.equals("SERVICEGROUPNAME") && temp_servicegroup != null) {
            blue.macro_ondemand = temp_servicegroup.group_name;
        } else if (macro.equals("SERVICEGROUPALIAS") && temp_servicegroup != null) {
            blue.macro_ondemand = temp_servicegroup.alias;
        } else if (macro.equals("SERVICEACTIONURL") || macro.equals("SERVICENOTESURL") || macro.equals("SERVICENOTES")) {
            temp_serviceextinfo = objects.find_serviceextinfo(svc.host_name, svc.description);
            if (temp_serviceextinfo != null) {
                if (macro.equals("SERVICEACTIONURL")) {
                    blue.macro_ondemand = process_macros(temp_serviceextinfo.action_url, blue_h.URL_ENCODE_MACRO_CHARS);
                }
                if (macro.equals("SERVICENOTESURL")) {
                    blue.macro_ondemand = process_macros(temp_serviceextinfo.notes_url, blue_h.URL_ENCODE_MACRO_CHARS);
                }
                if (macro.equals("SERVICENOTES")) {
                    blue.macro_ondemand = temp_serviceextinfo.notes;
                }
            }
        } else return common_h.ERROR;
        logger.trace("exiting " + cn + ".grab_on_demand_service_macro() end\n");
        return common_h.OK;
    }

    public static int grab_contact_macros(objects_h.contact cntct) {
        logger.trace("entering " + cn + ".grab_contact_macros");
        blue.macro_x[blue_h.MACRO_CONTACTNAME] = cntct.name;
        blue.macro_x[blue_h.MACRO_CONTACTALIAS] = cntct.alias;
        blue.macro_x[blue_h.MACRO_CONTACTEMAIL] = cntct.email;
        blue.macro_x[blue_h.MACRO_CONTACTPAGER] = cntct.pager;
        for (int x = 0; x < objects_h.MAX_CONTACT_ADDRESSES && x < cntct.address.length; x++) {
            blue.macro_contactaddress[x] = cntct.address[x];
        }
        grab_datetime_macros();
        logger.trace("exiting " + cn + ".grab_contact_macros");
        return common_h.OK;
    }

    public static void grab_summary_macros(objects_h.contact temp_contact) {
        boolean authorized = true;
        boolean problem = true;
        int hosts_up = 0;
        int hosts_down = 0;
        int hosts_unreachable = 0;
        int hosts_down_unhandled = 0;
        int hosts_unreachable_unhandled = 0;
        int host_problems = 0;
        int host_problems_unhandled = 0;
        int services_ok = 0;
        int services_warning = 0;
        int services_unknown = 0;
        int services_critical = 0;
        int services_warning_unhandled = 0;
        int services_unknown_unhandled = 0;
        int services_critical_unhandled = 0;
        int service_problems = 0;
        int service_problems_unhandled = 0;
        logger.trace("entering " + cn + ".grab_summary_macros");
        for (objects_h.host temp_host : (ArrayList<objects_h.host>) objects.host_list) {
            if (temp_contact != null) authorized = objects.is_contact_for_host(temp_host, temp_contact);
            if (authorized == true) {
                problem = true;
                if (temp_host.current_state == blue_h.HOST_UP && temp_host.has_been_checked == common_h.TRUE) hosts_up++; else if (temp_host.current_state == blue_h.HOST_DOWN) {
                    if (temp_host.scheduled_downtime_depth > 0) problem = false;
                    if (temp_host.problem_has_been_acknowledged == common_h.TRUE) problem = false;
                    if (temp_host.checks_enabled == common_h.FALSE) problem = false;
                    if (problem == true) hosts_down_unhandled++;
                    hosts_down++;
                } else if (temp_host.current_state == blue_h.HOST_UNREACHABLE) {
                    if (temp_host.scheduled_downtime_depth > 0) problem = false;
                    if (temp_host.problem_has_been_acknowledged == common_h.TRUE) problem = false;
                    if (temp_host.checks_enabled == common_h.FALSE) problem = false;
                    if (problem == true) hosts_down_unhandled++;
                    hosts_unreachable++;
                }
            }
        }
        host_problems = hosts_down + hosts_unreachable;
        host_problems_unhandled = hosts_down_unhandled + hosts_unreachable_unhandled;
        for (objects_h.service temp_service : (ArrayList<objects_h.service>) objects.service_list) {
            if (temp_contact != null) authorized = objects.is_contact_for_service(temp_service, temp_contact);
            if (authorized == true) {
                problem = true;
                if (temp_service.current_state == blue_h.STATE_OK && temp_service.has_been_checked == common_h.TRUE) services_ok++; else if (temp_service.current_state == blue_h.STATE_WARNING) {
                    objects_h.host temp_host = objects.find_host(temp_service.host_name);
                    if (temp_host != null && (temp_host.current_state == blue_h.HOST_DOWN || temp_host.current_state == blue_h.HOST_UNREACHABLE)) problem = false;
                    if (temp_service.scheduled_downtime_depth > 0) problem = false;
                    if (temp_service.problem_has_been_acknowledged == common_h.TRUE) problem = false;
                    if (temp_service.checks_enabled == common_h.FALSE) problem = false;
                    if (problem == true) services_warning_unhandled++;
                    services_warning++;
                } else if (temp_service.current_state == blue_h.STATE_UNKNOWN) {
                    objects_h.host temp_host = objects.find_host(temp_service.host_name);
                    if (temp_host != null && (temp_host.current_state == blue_h.HOST_DOWN || temp_host.current_state == blue_h.HOST_UNREACHABLE)) problem = false;
                    if (temp_service.scheduled_downtime_depth > 0) problem = false;
                    if (temp_service.problem_has_been_acknowledged == common_h.TRUE) problem = false;
                    if (temp_service.checks_enabled == common_h.FALSE) problem = false;
                    if (problem == true) services_unknown_unhandled++;
                    services_unknown++;
                } else if (temp_service.current_state == blue_h.STATE_CRITICAL) {
                    objects_h.host temp_host = objects.find_host(temp_service.host_name);
                    if (temp_host != null && (temp_host.current_state == blue_h.HOST_DOWN || temp_host.current_state == blue_h.HOST_UNREACHABLE)) problem = false;
                    if (temp_service.scheduled_downtime_depth > 0) problem = false;
                    if (temp_service.problem_has_been_acknowledged == common_h.TRUE) problem = false;
                    if (temp_service.checks_enabled == common_h.FALSE) problem = false;
                    if (problem == true) services_critical_unhandled++;
                    services_critical++;
                }
            }
        }
        service_problems = services_warning + services_critical + services_unknown;
        service_problems_unhandled = services_warning_unhandled + services_critical_unhandled + services_unknown_unhandled;
        blue.macro_x[blue_h.MACRO_TOTALHOSTSUP] = "" + hosts_up;
        blue.macro_x[blue_h.MACRO_TOTALHOSTSDOWN] = "" + hosts_down;
        blue.macro_x[blue_h.MACRO_TOTALHOSTSUNREACHABLE] = "" + hosts_unreachable;
        blue.macro_x[blue_h.MACRO_TOTALHOSTSDOWNUNHANDLED] = "" + hosts_down_unhandled;
        blue.macro_x[blue_h.MACRO_TOTALHOSTSUNREACHABLEUNHANDLED] = "" + hosts_unreachable_unhandled;
        blue.macro_x[blue_h.MACRO_TOTALHOSTPROBLEMS] = "" + host_problems;
        blue.macro_x[blue_h.MACRO_TOTALHOSTPROBLEMSUNHANDLED] = "" + host_problems_unhandled;
        blue.macro_x[blue_h.MACRO_TOTALSERVICESOK] = "" + services_ok;
        blue.macro_x[blue_h.MACRO_TOTALSERVICESWARNING] = "" + services_warning;
        blue.macro_x[blue_h.MACRO_TOTALSERVICESCRITICAL] = "" + services_critical;
        blue.macro_x[blue_h.MACRO_TOTALSERVICESUNKNOWN] = "" + services_unknown;
        blue.macro_x[blue_h.MACRO_TOTALSERVICESWARNINGUNHANDLED] = "" + services_warning_unhandled;
        blue.macro_x[blue_h.MACRO_TOTALSERVICESCRITICALUNHANDLED] = "" + services_critical_unhandled;
        blue.macro_x[blue_h.MACRO_TOTALSERVICESUNKNOWNUNHANDLED] = "" + services_unknown_unhandled;
        blue.macro_x[blue_h.MACRO_TOTALSERVICEPROBLEMS] = "" + service_problems;
        blue.macro_x[blue_h.MACRO_TOTALSERVICEPROBLEMSUNHANDLED] = "" + service_problems_unhandled;
        logger.trace("exiting " + cn + ".grab_summary_macros() end\n");
    }

    public static void grab_datetime_macros() {
        logger.trace("entering " + cn + ".grab_datetime_macros");
        long t = utils.currentTimeInSeconds();
        blue.macro_x[blue_h.MACRO_LONGDATETIME] = get_datetime_string(t * 1000, common_h.LONG_DATE_TIME);
        blue.macro_x[blue_h.MACRO_SHORTDATETIME] = get_datetime_string(t * 1000, common_h.SHORT_DATE_TIME);
        blue.macro_x[blue_h.MACRO_DATE] = get_datetime_string(t * 1000, common_h.SHORT_DATE);
        blue.macro_x[blue_h.MACRO_TIME] = get_datetime_string(t * 1000, common_h.SHORT_TIME);
        blue.macro_x[blue_h.MACRO_TIMET] = "" + t;
        logger.trace("exiting " + cn + ".grab_datetime_macros");
    }

    public static int clear_argv_macros() {
        logger.trace("exiting " + cn + ".clear_argv_macros");
        for (int x = 0; x < blue_h.MAX_COMMAND_ARGUMENTS; x++) {
            blue.macro_argv[x] = null;
        }
        logger.trace("entering " + cn + ".clear_argv_macros");
        return common_h.OK;
    }

    public static void clear_volatile_macros() {
        logger.trace("entering " + cn + ".clear_volatile_macros");
        for (int x = 0; x < blue_h.MACRO_X_COUNT; x++) {
            switch(x) {
                case blue_h.MACRO_ADMINEMAIL:
                case blue_h.MACRO_ADMINPAGER:
                case blue_h.MACRO_MAINCONFIGFILE:
                case blue_h.MACRO_STATUSDATAFILE:
                case blue_h.MACRO_COMMENTDATAFILE:
                case blue_h.MACRO_DOWNTIMEDATAFILE:
                case blue_h.MACRO_RETENTIONDATAFILE:
                case blue_h.MACRO_OBJECTCACHEFILE:
                case blue_h.MACRO_TEMPFILE:
                case blue_h.MACRO_LOGFILE:
                case blue_h.MACRO_RESOURCEFILE:
                case blue_h.MACRO_COMMANDFILE:
                case blue_h.MACRO_HOSTPERFDATAFILE:
                case blue_h.MACRO_SERVICEPERFDATAFILE:
                case blue_h.MACRO_PROCESSSTARTTIME:
                    break;
                default:
                    blue.macro_x[x] = null;
                    break;
            }
        }
        for (int x = 0; x < blue_h.MAX_COMMAND_ARGUMENTS; x++) {
            blue.macro_argv[x] = null;
        }
        for (int x = 0; x < objects_h.MAX_CONTACT_ADDRESSES; x++) {
            blue.macro_contactaddress[x] = null;
        }
        blue.macro_ondemand = null;
        clear_argv_macros();
        logger.trace("exiting " + cn + ".clear_volatile_macros");
    }

    public static int init_macrox_names() {
        logger.trace("entering " + cn + ".init_macrox_names()");
        for (int x = 0; x < blue_h.MACRO_X_COUNT; x++) blue.macro_x_names[x] = null;
        add_macrox_name(blue_h.MACRO_HOSTNAME, "HOSTNAME");
        add_macrox_name(blue_h.MACRO_HOSTALIAS, "HOSTALIAS");
        add_macrox_name(blue_h.MACRO_HOSTADDRESS, "HOSTADDRESS");
        add_macrox_name(blue_h.MACRO_SERVICEDESC, "SERVICEDESC");
        add_macrox_name(blue_h.MACRO_SERVICESTATE, "SERVICESTATE");
        add_macrox_name(blue_h.MACRO_SERVICESTATEID, "SERVICESTATEID");
        add_macrox_name(blue_h.MACRO_SERVICEATTEMPT, "SERVICEATTEMPT");
        add_macrox_name(blue_h.MACRO_LONGDATETIME, "LONGDATETIME");
        add_macrox_name(blue_h.MACRO_SHORTDATETIME, "SHORTDATETIME");
        add_macrox_name(blue_h.MACRO_DATE, "DATE");
        add_macrox_name(blue_h.MACRO_TIME, "TIME");
        add_macrox_name(blue_h.MACRO_TIMET, "TIMET");
        add_macrox_name(blue_h.MACRO_LASTHOSTCHECK, "LASTHOSTCHECK");
        add_macrox_name(blue_h.MACRO_LASTSERVICECHECK, "LASTSERVICECHECK");
        add_macrox_name(blue_h.MACRO_LASTHOSTSTATECHANGE, "LASTHOSTSTATECHANGE");
        add_macrox_name(blue_h.MACRO_LASTSERVICESTATECHANGE, "LASTSERVICESTATECHANGE");
        add_macrox_name(blue_h.MACRO_HOSTOUTPUT, "HOSTOUTPUT");
        add_macrox_name(blue_h.MACRO_SERVICEOUTPUT, "SERVICEOUTPUT");
        add_macrox_name(blue_h.MACRO_HOSTPERFDATA, "HOSTPERFDATA");
        add_macrox_name(blue_h.MACRO_SERVICEPERFDATA, "SERVICEPERFDATA");
        add_macrox_name(blue_h.MACRO_CONTACTNAME, "CONTACTNAME");
        add_macrox_name(blue_h.MACRO_CONTACTALIAS, "CONTACTALIAS");
        add_macrox_name(blue_h.MACRO_CONTACTEMAIL, "CONTACTEMAIL");
        add_macrox_name(blue_h.MACRO_CONTACTPAGER, "CONTACTPAGER");
        add_macrox_name(blue_h.MACRO_ADMINEMAIL, "ADMINEMAIL");
        add_macrox_name(blue_h.MACRO_ADMINPAGER, "ADMINPAGER");
        add_macrox_name(blue_h.MACRO_HOSTSTATE, "HOSTSTATE");
        add_macrox_name(blue_h.MACRO_HOSTSTATEID, "HOSTSTATEID");
        add_macrox_name(blue_h.MACRO_HOSTATTEMPT, "HOSTATTEMPT");
        add_macrox_name(blue_h.MACRO_NOTIFICATIONTYPE, "NOTIFICATIONTYPE");
        add_macrox_name(blue_h.MACRO_NOTIFICATIONNUMBER, "NOTIFICATIONNUMBER");
        add_macrox_name(blue_h.MACRO_HOSTEXECUTIONTIME, "HOSTEXECUTIONTIME");
        add_macrox_name(blue_h.MACRO_SERVICEEXECUTIONTIME, "SERVICEEXECUTIONTIME");
        add_macrox_name(blue_h.MACRO_HOSTLATENCY, "HOSTLATENCY");
        add_macrox_name(blue_h.MACRO_SERVICELATENCY, "SERVICELATENCY");
        add_macrox_name(blue_h.MACRO_HOSTDURATION, "HOSTDURATION");
        add_macrox_name(blue_h.MACRO_SERVICEDURATION, "SERVICEDURATION");
        add_macrox_name(blue_h.MACRO_HOSTDURATIONSEC, "HOSTDURATIONSEC");
        add_macrox_name(blue_h.MACRO_SERVICEDURATIONSEC, "SERVICEDURATIONSEC");
        add_macrox_name(blue_h.MACRO_HOSTDOWNTIME, "HOSTDOWNTIME");
        add_macrox_name(blue_h.MACRO_SERVICEDOWNTIME, "SERVICEDOWNTIME");
        add_macrox_name(blue_h.MACRO_HOSTSTATETYPE, "HOSTSTATETYPE");
        add_macrox_name(blue_h.MACRO_SERVICESTATETYPE, "SERVICESTATETYPE");
        add_macrox_name(blue_h.MACRO_HOSTPERCENTCHANGE, "HOSTPERCENTCHANGE");
        add_macrox_name(blue_h.MACRO_SERVICEPERCENTCHANGE, "SERVICEPERCENTCHANGE");
        add_macrox_name(blue_h.MACRO_HOSTGROUPNAME, "HOSTGROUPNAME");
        add_macrox_name(blue_h.MACRO_HOSTGROUPALIAS, "HOSTGROUPALIAS");
        add_macrox_name(blue_h.MACRO_SERVICEGROUPNAME, "SERVICEGROUPNAME");
        add_macrox_name(blue_h.MACRO_SERVICEGROUPALIAS, "SERVICEGROUPALIAS");
        add_macrox_name(blue_h.MACRO_HOSTACKAUTHOR, "HOSTACKAUTHOR");
        add_macrox_name(blue_h.MACRO_HOSTACKCOMMENT, "HOSTACKCOMMENT");
        add_macrox_name(blue_h.MACRO_SERVICEACKAUTHOR, "SERVICEACKAUTHOR");
        add_macrox_name(blue_h.MACRO_SERVICEACKCOMMENT, "SERVICEACKCOMMENT");
        add_macrox_name(blue_h.MACRO_LASTSERVICEOK, "LASTSERVICEOK");
        add_macrox_name(blue_h.MACRO_LASTSERVICEWARNING, "LASTSERVICEWARNING");
        add_macrox_name(blue_h.MACRO_LASTSERVICEUNKNOWN, "LASTSERVICEUNKNOWN");
        add_macrox_name(blue_h.MACRO_LASTSERVICECRITICAL, "LASTSERVICECRITICAL");
        add_macrox_name(blue_h.MACRO_LASTHOSTUP, "LASTHOSTUP");
        add_macrox_name(blue_h.MACRO_LASTHOSTDOWN, "LASTHOSTDOWN");
        add_macrox_name(blue_h.MACRO_LASTHOSTUNREACHABLE, "LASTHOSTUNREACHABLE");
        add_macrox_name(blue_h.MACRO_SERVICECHECKCOMMAND, "SERVICECHECKCOMMAND");
        add_macrox_name(blue_h.MACRO_HOSTCHECKCOMMAND, "HOSTCHECKCOMMAND");
        add_macrox_name(blue_h.MACRO_MAINCONFIGFILE, "MAINCONFIGFILE");
        add_macrox_name(blue_h.MACRO_STATUSDATAFILE, "STATUSDATAFILE");
        add_macrox_name(blue_h.MACRO_COMMENTDATAFILE, "COMMENTDATAFILE");
        add_macrox_name(blue_h.MACRO_DOWNTIMEDATAFILE, "DOWNTIMEDATAFILE");
        add_macrox_name(blue_h.MACRO_RETENTIONDATAFILE, "RETENTIONDATAFILE");
        add_macrox_name(blue_h.MACRO_OBJECTCACHEFILE, "OBJECTCACHEFILE");
        add_macrox_name(blue_h.MACRO_TEMPFILE, "TEMPFILE");
        add_macrox_name(blue_h.MACRO_LOGFILE, "LOGFILE");
        add_macrox_name(blue_h.MACRO_RESOURCEFILE, "RESOURCEFILE");
        add_macrox_name(blue_h.MACRO_COMMANDFILE, "COMMANDFILE");
        add_macrox_name(blue_h.MACRO_HOSTPERFDATAFILE, "HOSTPERFDATAFILE");
        add_macrox_name(blue_h.MACRO_SERVICEPERFDATAFILE, "SERVICEPERFDATAFILE");
        add_macrox_name(blue_h.MACRO_HOSTACTIONURL, "HOSTACTIONURL");
        add_macrox_name(blue_h.MACRO_HOSTNOTESURL, "HOSTNOTESURL");
        add_macrox_name(blue_h.MACRO_HOSTNOTES, "HOSTNOTES");
        add_macrox_name(blue_h.MACRO_SERVICEACTIONURL, "SERVICEACTIONURL");
        add_macrox_name(blue_h.MACRO_SERVICENOTESURL, "SERVICENOTESURL");
        add_macrox_name(blue_h.MACRO_SERVICENOTES, "SERVICENOTES");
        add_macrox_name(blue_h.MACRO_TOTALHOSTSUP, "TOTALHOSTSUP");
        add_macrox_name(blue_h.MACRO_TOTALHOSTSDOWN, "TOTALHOSTSDOWN");
        add_macrox_name(blue_h.MACRO_TOTALHOSTSUNREACHABLE, "TOTALHOSTSUNREACHABLE");
        add_macrox_name(blue_h.MACRO_TOTALHOSTSDOWNUNHANDLED, "TOTALHOSTSDOWNUNHANDLED");
        add_macrox_name(blue_h.MACRO_TOTALHOSTSUNREACHABLEUNHANDLED, "TOTALHOSTSUNREACHABLEUNHANDLED");
        add_macrox_name(blue_h.MACRO_TOTALHOSTPROBLEMS, "TOTALHOSTPROBLEMS");
        add_macrox_name(blue_h.MACRO_TOTALHOSTPROBLEMSUNHANDLED, "TOTALHOSTPROBLEMSUNHANDLED");
        add_macrox_name(blue_h.MACRO_TOTALSERVICESOK, "TOTALSERVICESOK");
        add_macrox_name(blue_h.MACRO_TOTALSERVICESWARNING, "TOTALSERVICESWARNING");
        add_macrox_name(blue_h.MACRO_TOTALSERVICESCRITICAL, "TOTALSERVICESCRITICAL");
        add_macrox_name(blue_h.MACRO_TOTALSERVICESUNKNOWN, "TOTALSERVICESUNKNOWN");
        add_macrox_name(blue_h.MACRO_TOTALSERVICESWARNINGUNHANDLED, "TOTALSERVICESWARNINGUNHANDLED");
        add_macrox_name(blue_h.MACRO_TOTALSERVICESCRITICALUNHANDLED, "TOTALSERVICESCRITICALUNHANDLED");
        add_macrox_name(blue_h.MACRO_TOTALSERVICESUNKNOWNUNHANDLED, "TOTALSERVICESUNKNOWNUNHANDLED");
        add_macrox_name(blue_h.MACRO_TOTALSERVICEPROBLEMS, "TOTALSERVICEPROBLEMS");
        add_macrox_name(blue_h.MACRO_TOTALSERVICEPROBLEMSUNHANDLED, "TOTALSERVICEPROBLEMSUNHANDLED");
        add_macrox_name(blue_h.MACRO_PROCESSSTARTTIME, "PROCESSSTARTTIME");
        add_macrox_name(blue_h.MACRO_HOSTCHECKTYPE, "HOSTCHECKTYPE");
        add_macrox_name(blue_h.MACRO_SERVICECHECKTYPE, "SERVICECHECKTYPE");
        logger.trace("exiting " + cn + ".init_macrox_names()");
        return common_h.OK;
    }

    public static int add_macrox_name(int i, String name) {
        blue.macro_x_names[i] = name;
        return common_h.OK;
    }

    public static int free_macrox_names() {
        logger.trace("entering " + cn + ".free_macrox_names");
        for (int x = 0; x < blue_h.MACRO_X_COUNT; x++) {
            blue.macro_x_names[x] = null;
        }
        logger.trace("exiting " + cn + ".free_macrox_names");
        return common_h.OK;
    }

    public static void set_all_macro_environment_vars(HashMap<String, String> envHashMap) {
        logger.trace("entering " + cn + ".set_all_macro_environment_vars");
        set_macrox_environment_vars(envHashMap);
        set_argv_macro_environment_vars(envHashMap);
        logger.trace("exiting " + cn + ".set_all_macro_environment_vars");
    }

    public static void set_macrox_environment_vars(HashMap<String, String> envHashMap) {
        logger.trace("entering " + cn + ".set_macrox_environment_vars");
        for (int x = 0; x < blue_h.MACRO_X_COUNT; x++) {
            if (x >= 16 && x <= 19) envHashMap.put(blue.macro_x_names[x], clean_macro_chars(blue.macro_x[x], blue_h.STRIP_ILLEGAL_MACRO_CHARS | blue_h.ESCAPE_MACRO_CHARS)); else envHashMap.put(blue.macro_x_names[x], blue.macro_x[x]);
        }
        logger.trace("exiting " + cn + ".set_macrox_environment_vars");
    }

    public static void set_argv_macro_environment_vars(HashMap<String, String> envHashMap) {
        logger.trace("entering " + cn + ".set_argv_macro_environment_vars ");
        for (int x = 0; x < blue_h.MAX_COMMAND_ARGUMENTS; x++) {
            envHashMap.put("ARG" + (x + 1), blue.macro_argv[x]);
        }
        logger.trace("exiting " + cn + ".set_argv_macro_environment_vars");
    }

    public static class system_result {

        public double exec_time = 0.0;

        public int result = -1;

        public boolean early_timeout = false;

        public String output = null;
    }

    public static void main(String[] args) {
        System.out.println("Exec " + args[0]);
        my_system(args[0], 50000);
    }

    /** 
    * executes a system command - used for service checks and notifications
    * 
    *  @param cmd command to be executed, it is expected this command has already been prepped.
    *  @param timeout timeout to kill spawned processes if it takes too long
    */
    public static utils.system_result my_system(String cmd, int timeout) {
        logger.trace("entering " + cn + ".my_system");
        utils.system_result result = new system_result();
        HashMap<String, String> envHashMap = new HashMap(System.getenv());
        set_all_macro_environment_vars(envHashMap);
        blue_h.timeval start_time = new blue_h.timeval();
        Process process = null;
        try {
            broker.broker_system_command(broker_h.NEBTYPE_SYSTEM_COMMAND_START, broker_h.NEBFLAG_NONE, broker_h.NEBATTR_NONE, start_time, new blue_h.timeval(0, 0), result.exec_time, timeout, result.early_timeout ? common_h.TRUE : common_h.FALSE, result.result, cmd, null, null);
            String[] cmdArray = utils.processCommandLine(cmd);
            logger.debug("\tCMD : " + cmd);
            logger.debug("\tCMDArray : " + cmdArray.length);
            process = Runtime.getRuntime().exec(cmdArray, getEnv(envHashMap));
            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String buffer = input.readLine();
            logger.debug("\tLINE : " + buffer);
            result.output = buffer;
            while (buffer != null) buffer = input.readLine();
            process.waitFor();
            result.result = process.exitValue();
            logger.debug("\tRESULT : " + result.result);
        } catch (Throwable e) {
            e.printStackTrace();
            result.result = -1;
            logger.warn("Warning: " + e.getMessage().toString());
            result.result = blue_h.STATE_CRITICAL;
        }
        result.exec_time = (start_time.time - System.currentTimeMillis());
        if (result.result == 126 || result.result == 127) {
            logger.warn("Warning: Attempting to execute the command \"" + cmd + "\" resulted in a return code of " + result.result + ".  Make sure the script or binary you are trying to execute actually exists...");
        }
        if (result.result == 255 || result.result == -1) result.result = blue_h.STATE_UNKNOWN;
        if (result.result < -1 || result.result > 3) result.result = blue_h.STATE_UNKNOWN;
        broker.broker_system_command(broker_h.NEBTYPE_SYSTEM_COMMAND_END, broker_h.NEBFLAG_NONE, broker_h.NEBATTR_NONE, start_time, new blue_h.timeval(), result.exec_time, timeout, result.early_timeout ? common_h.TRUE : common_h.FALSE, result.result, cmd, result.output, null);
        logger.trace("exiting " + cn + ".my_system");
        return result;
    }

    public static String get_raw_command_line(String cmd, int macro_options) {
        String raw_command;
        objects_h.command temp_command;
        logger.trace("entering " + cn + ".get_raw_command_line");
        logger.debug("\tInput: " + cmd);
        clear_argv_macros();
        if (cmd == null) {
            logger.debug("\tWe don't have enough data to get the raw command line!");
            return null;
        }
        String[] split = cmd.split("\\!");
        raw_command = split[0];
        temp_command = objects.find_command(raw_command);
        if (temp_command == null) return null;
        raw_command = temp_command.command_line.trim();
        for (int x = 1; x < split.length; x++) {
            blue.macro_argv[x - 1] = process_macros(split[x], macro_options);
        }
        logger.debug("\tOutput: " + raw_command);
        logger.trace("exiting " + cn + ".get_raw_command_line");
        return raw_command;
    }

    public static int check_time_against_period(long check_time, String period_name) {
        logger.trace("entering " + cn + ".check_time_against_period");
        if (period_name == null || period_name.length() == 0) return common_h.OK;
        objects_h.timeperiod temp_period = objects.find_timeperiod(period_name);
        if (temp_period == null) return common_h.OK;
        Calendar t = Calendar.getInstance();
        t.setTimeInMillis(check_time * 1000);
        t.set(Calendar.SECOND, 0);
        t.set(Calendar.MINUTE, 0);
        t.set(Calendar.HOUR, 0);
        long midnight_today = utils.getTimeInSeconds(t);
        for (ListIterator iter = temp_period.days[t.get(Calendar.DAY_OF_WEEK) - 1].listIterator(); iter.hasNext(); ) {
            objects_h.timerange temp_range = (objects_h.timerange) iter.next();
            if ((check_time >= midnight_today + temp_range.range_start) && (check_time <= midnight_today + temp_range.range_end)) return common_h.OK;
        }
        logger.trace("exiting " + cn + ".check_time_against_period");
        return common_h.ERROR;
    }

    public static long get_next_valid_time(long preferred_time, String period_name) {
        long earliest_next_valid_time = 0L;
        long valid_time = 0L;
        logger.trace("entering " + cn + ".get_next_valid_time");
        logger.debug("\tPreferred Time: " + preferred_time + " -. " + preferred_time);
        if (check_time_against_period(preferred_time, period_name) == common_h.OK) {
            valid_time = preferred_time;
        } else {
            objects_h.timeperiod temp_timeperiod = objects.find_timeperiod(period_name);
            if (temp_timeperiod == null) {
                return preferred_time;
            }
            Date t = new Date();
            t.setSeconds(0);
            t.setMinutes(0);
            t.setHours(0);
            long midnight_today = t.getTime();
            int today = t.getDay();
            boolean has_looped = false;
            for (int weekday = today, days_into_the_future = 0; ; weekday++, days_into_the_future++) {
                if (weekday >= 7) {
                    weekday -= 7;
                    has_looped = true;
                }
                for (ListIterator iter = temp_timeperiod.days[weekday].listIterator(); iter.hasNext(); ) {
                    objects_h.timerange temp_timerange = (objects_h.timerange) iter.next();
                    long this_time_range_start = (midnight_today + (days_into_the_future * 3600 * 24) + temp_timerange.range_start);
                    if ((earliest_next_valid_time == 0 || (this_time_range_start < earliest_next_valid_time)) && (this_time_range_start >= preferred_time)) earliest_next_valid_time = this_time_range_start;
                }
                if (has_looped == true && weekday >= today) break;
            }
            if (earliest_next_valid_time == 0) valid_time = preferred_time; else valid_time = earliest_next_valid_time;
        }
        logger.debug("\tNext Valid Time: " + valid_time + " -. " + new Date(valid_time * 1000).toString());
        logger.trace("exiting " + cn + ".get_next_valid_time");
        return valid_time;
    }

    public static String get_datetime_string(long raw_time, int type) {
        String buffer;
        if (type == common_h.LONG_DATE_TIME) buffer = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy").format(new Long(raw_time)); else if (type == common_h.SHORT_DATE_TIME) {
            if (blue.date_format == common_h.DATE_FORMAT_EURO) buffer = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Long(raw_time)); else if (blue.date_format == common_h.DATE_FORMAT_ISO8601) buffer = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Long(raw_time)); else if (blue.date_format == common_h.DATE_FORMAT_STRICT_ISO8601) buffer = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Long(raw_time)); else buffer = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(new Long(raw_time));
        } else if (type == common_h.SHORT_DATE) {
            if (blue.date_format == common_h.DATE_FORMAT_EURO) buffer = new SimpleDateFormat("dd-MM-yyyy").format(new Long(raw_time)); else if (blue.date_format == common_h.DATE_FORMAT_ISO8601 || blue.date_format == common_h.DATE_FORMAT_STRICT_ISO8601) buffer = new SimpleDateFormat("yyyy-MM-dd").format(new Long(raw_time)); else buffer = new SimpleDateFormat("MM-dd-yyyy").format(new Long(raw_time));
        } else buffer = new SimpleDateFormat("HH:mm:ss").format(new Long(raw_time));
        return buffer;
    }

    public static long get_next_log_rotation_time() {
        logger.trace("entering " + cn + ".get_next_log_rotation_time");
        Calendar now = Calendar.getInstance();
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        switch(blue.log_rotation_method) {
            case common_h.LOG_ROTATION_HOURLY:
                now.add(Calendar.HOUR_OF_DAY, 1);
                break;
            case common_h.LOG_ROTATION_DAILY:
                now.add(Calendar.DAY_OF_MONTH, 1);
                now.set(Calendar.HOUR_OF_DAY, 0);
                break;
            case common_h.LOG_ROTATION_WEEKLY:
                now.add(Calendar.DAY_OF_MONTH, 7 - (now.get(Calendar.DAY_OF_WEEK) - 1));
                now.set(Calendar.HOUR_OF_DAY, 0);
                break;
            case common_h.LOG_ROTATION_MONTHLY:
            default:
                now.add(Calendar.MONTH, 1);
                now.set(Calendar.DAY_OF_MONTH, 1);
                now.set(Calendar.HOUR_OF_DAY, 0);
                break;
        }
        logger.debug("\tNext Log Rotation Time: " + now.toString());
        logger.trace("exiting " + cn + ".get_next_log_rotation_time");
        return getTimeInSeconds(now);
    }

    public static void setup_sighandler() {
        logger.trace("entering " + cn + ".setup_sighandler");
        blue.sigshutdown = common_h.FALSE;
        Runtime.getRuntime().addShutdownHook(new shutdown_handler());
        logger.trace("exiting " + cn + ".setup_sighandler");
        return;
    }

    public static class shutdown_handler extends Thread {

        public void run() {
            logger.trace("entering shutdown_handler");
            if (blue.sigshutdown == common_h.FALSE) {
                blue.sigshutdown = common_h.TRUE;
                logger.info("Caught Shutdown, shutting down...");
                try {
                    blue.blue_file_lock.release();
                    blue.blue_file_lock_channel.close();
                    File lock = new File(blue.lock_file);
                    lock.delete();
                } catch (Exception e) {
                }
            }
            close_command_file();
            logger.trace("exiting shutdown_handler");
        }
    }

    public static blue_h.service_message read_svc_message() {
        blue_h.service_message message = null;
        logger.trace("entering " + cn + ".read_svc_message");
        synchronized (blue.service_result_buffer.buffer_lock) {
            if (blue.service_result_buffer.buffer.isEmpty()) message = null; else {
                message = (blue_h.service_message) blue.service_result_buffer.buffer.poll();
            }
        }
        logger.trace("exiting " + cn + ".read_svc_message");
        return message;
    }

    /**
    * writes a service message to the message pipe
    * 
    * Interesting about this method is it was based on PIPE's and specifically a forked process
    * writing to a pipe connected to the partent process.  The issue of even needing to fork vs create thread 
    * is still in my mind. 
    * 
    */
    public static int write_svc_message(blue_h.service_message message) {
        int write_result = common_h.OK;
        logger.trace("entering " + cn + ".write_svc_message");
        if (message == null) return 0;
        blue.ipc_queue.offer(message);
        logger.trace("exiting " + cn + ".write_svc_message");
        return write_result;
    }

    public static int open_command_file() {
        logger.trace("entering " + cn + ".open_command_file() start\n");
        logger.debug("open_command_file check_external_commands " + blue.check_external_commands);
        if (blue.check_external_commands == common_h.FALSE) return common_h.OK;
        logger.debug("open_command_file command_file_created " + blue.command_file_created);
        if (blue.command_file_created == common_h.TRUE) return common_h.OK;
        try {
            logger.debug("open_command_file command_file_channel " + blue.command_file);
            blue.command_file_channel = new RandomAccessFile(blue.command_file, "rw").getChannel();
        } catch (IOException ioE) {
            logger.fatal(" Error: Could not create external command file '" + blue.command_file + "' as named pipe:  If this file already exists and you are sure that another copy of Blue is not running, you should delete this file.", ioE);
            return common_h.ERROR;
        }
        if (command_file_worker_thread.init_command_file_worker_thread() == common_h.ERROR) {
            logger.fatal("Error: Could not initialize command file worker thread.");
            try {
                blue.command_file_channel.close();
            } catch (IOException ioE) {
            }
            new File(blue.command_file).delete();
            return common_h.ERROR;
        }
        blue.command_file_created = common_h.TRUE;
        logger.trace("exiting " + cn + ".open_command_file");
        return common_h.OK;
    }

    public static int close_command_file() {
        logger.trace("entering " + cn + ".close_command_file");
        if (blue.check_external_commands == common_h.FALSE) return common_h.OK;
        if (blue.command_file_created == common_h.FALSE) return common_h.OK;
        blue.command_file_created = common_h.FALSE;
        command_file_worker_thread.shutdown_command_file_worker_thread();
        try {
            blue.command_file_channel.close();
        } catch (Exception e) {
            logger.error("warning: " + e.getMessage(), e);
        }
        logger.trace("exiting " + cn + ".close_command_file");
        return common_h.OK;
    }

    public static int contains_illegal_object_chars(String name) {
        if (name == null) return common_h.FALSE;
        for (int x = name.length() - 1; x >= 0; x--) {
            char ch = name.charAt(x);
            if (ch < 32 || ch == 127) return common_h.TRUE;
            if (blue.illegal_object_chars != null) for (int y = 0; y < blue.illegal_object_chars.length(); y++) if (ch == blue.illegal_object_chars.charAt(y)) return common_h.TRUE;
        }
        return common_h.FALSE;
    }

    public static String clean_macro_chars(String macro, int options) {
        if (macro == null) return "";
        if ((options & blue_h.STRIP_ILLEGAL_MACRO_CHARS) > 0) {
            return macro.replace(blue.illegal_output_chars, "");
        }
        return macro;
    }

    public static String get_url_encoded_string(String input) {
        try {
            return URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException ueE) {
            logger.warn("warnging: utils.get_url_encoded_string illegal encoding UTF-8");
            return input;
        }
    }

    public static class file_functions {

        public static int my_rename(String source, String dest) {
            logger.debug("RENAME " + source + " to " + dest);
            if (source == null || dest == null) return -1;
            {
                logger.debug("\tMoving file across file systems.");
                FileChannel srcChannel = null;
                FileChannel dstChannel = null;
                FileLock lock = null;
                try {
                    srcChannel = new FileInputStream(source).getChannel();
                    dstChannel = new FileOutputStream(dest).getChannel();
                    lock = dstChannel.lock();
                    dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                    dstChannel.force(true);
                } catch (IOException e) {
                    logger.fatal("Error while copying file '" + source + "' to file '" + dest + "'. " + e.getMessage(), e);
                    return common_h.ERROR;
                } finally {
                    try {
                        lock.release();
                    } catch (Throwable t) {
                        logger.fatal("Error releasing file lock - " + dest);
                    }
                    try {
                        srcChannel.close();
                    } catch (Throwable t) {
                    }
                    try {
                        dstChannel.close();
                    } catch (Throwable t) {
                    }
                }
            }
            return common_h.OK;
        }

        public static blue_h.mmapfile mmap_fopen(String filename) {
            logger.debug("OPEN " + filename);
            blue_h.mmapfile new_mmapfile = null;
            try {
                new_mmapfile = new blue_h.mmapfile();
                new_mmapfile.path = filename;
                new_mmapfile.current_line = 0L;
                new_mmapfile.fc = Channels.newChannel(new FileInputStream(filename));
                new_mmapfile.reader = new BufferedReader(Channels.newReader(new_mmapfile.fc, "ISO-8859-1"));
            } catch (IOException ioE) {
                logger.error("SYSTEM: " + ioE.getMessage() + ";WARNING; File Set to null.");
                new_mmapfile = null;
            }
            return new_mmapfile;
        }

        public static int mmap_fclose(blue_h.mmapfile temp_mmapfile) {
            logger.debug("CLOSE " + temp_mmapfile.path);
            if (temp_mmapfile == null) return common_h.ERROR;
            try {
                temp_mmapfile.fc.close();
            } catch (IOException ioE) {
            }
            try {
                temp_mmapfile.reader.close();
            } catch (IOException ioE) {
                logger.error("warning: " + ioE.getMessage(), ioE);
                return common_h.ERROR;
            }
            return common_h.OK;
        }

        public static String mmap_fgets(blue_h.mmapfile temp_mmapfile) {
            if (temp_mmapfile == null) return null;
            String buffer = null;
            try {
                buffer = temp_mmapfile.reader.readLine();
            } catch (IOException ioE) {
                logger.fatal(ioE.getMessage(), ioE);
            }
            temp_mmapfile.current_line++;
            return buffer;
        }
    }

    public static boolean submit_external_command(String cmd, int delay) {
        boolean result = true;
        try {
            result = blue.external_command_buffer.buffer.offer(cmd, delay, TimeUnit.MICROSECONDS);
        } catch (InterruptedException iE) {
            result = false;
        } catch (NullPointerException npE) {
            result = false;
        }
        return result;
    }

    public static String get_program_version() {
        return common_h.PROGRAM_VERSION;
    }

    public static String get_program_modification_date() {
        return common_h.PROGRAM_MODIFICATION_DATE;
    }

    public static void cleanup() {
        logger.trace("entering " + cn + ".cleanup");
        if (blue.test_scheduling == common_h.FALSE && blue.verify_config == common_h.FALSE) {
            nebmods.neb_free_callback_list();
            nebmods.neb_unload_all_modules(nebmodules_h.NEBMODULE_FORCE_UNLOAD, (blue.sigshutdown == common_h.TRUE) ? nebmodules_h.NEBMODULE_NEB_SHUTDOWN : nebmodules_h.NEBMODULE_NEB_RESTART);
            nebmods.neb_free_module_list();
            nebmods.neb_deinit_modules();
        }
        free_memory();
        logger.trace("exiting " + cn + ".cleanup");
        return;
    }

    public static void free_memory() {
        logger.trace("entering " + cn + ".free_memory");
        events.event_list_high.clear();
        events.event_list_low.clear();
        logger.debug("\tevent lists freed");
        blue.global_host_event_handler = null;
        blue.global_service_event_handler = null;
        logger.debug("\tglobal event handlers freed\n");
        free_notification_list();
        logger.debug("\tnotification_list freed");
        blue.ocsp_command = null;
        blue.ochp_command = null;
        for (int x = 0; x < blue_h.MAX_COMMAND_ARGUMENTS; x++) blue.macro_argv[x] = null;
        for (int x = 0; x < blue_h.MAX_USER_MACROS; x++) blue.macro_user[x] = null;
        for (int x = 0; x < blue_h.MACRO_X_COUNT; x++) blue.macro_x[x] = null;
        blue.illegal_object_chars = null;
        blue.illegal_output_chars = null;
        blue.nagios_user = null;
        blue.nagios_group = null;
        blue.log_file = null;
        blue.temp_file = null;
        blue.command_file = null;
        blue.lock_file = null;
        blue.auth_file = null;
        blue.p1_file = null;
        blue.log_archive_path = null;
        logger.trace("exiting " + cn + ".free_memory");
        return;
    }

    public static void free_notification_list() {
        logger.trace("entering " + cn + ".free_notification_list");
        blue.notification_list.clear();
        logger.trace("exiting " + cn + ".free_notification_list");
    }

    public static int reset_variables() {
        logger.trace("entering " + cn + ".reset_variables()");
        blue.log_file = locations_h.DEFAULT_LOG_FILE;
        blue.temp_file = locations_h.DEFAULT_TEMP_FILE;
        blue.command_file = locations_h.DEFAULT_COMMAND_FILE;
        blue.lock_file = locations_h.DEFAULT_LOCK_FILE;
        blue.auth_file = locations_h.DEFAULT_AUTH_FILE;
        blue.p1_file = locations_h.DEFAULT_P1_FILE;
        blue.log_archive_path = locations_h.DEFAULT_LOG_ARCHIVE_PATH;
        blue.nagios_user = config_h.DEFAULT_NAGIOS_USER;
        blue.nagios_group = config_h.DEFAULT_NAGIOS_GROUP;
        blue.use_regexp_matches = common_h.FALSE;
        blue.use_true_regexp_matching = common_h.FALSE;
        blue.use_syslog = blue_h.DEFAULT_USE_SYSLOG;
        blue.log_service_retries = blue_h.DEFAULT_LOG_SERVICE_RETRIES;
        blue.log_host_retries = blue_h.DEFAULT_LOG_HOST_RETRIES;
        blue.log_initial_states = blue_h.DEFAULT_LOG_INITIAL_STATES;
        blue.log_notifications = blue_h.DEFAULT_NOTIFICATION_LOGGING;
        blue.log_event_handlers = blue_h.DEFAULT_LOG_EVENT_HANDLERS;
        blue.log_external_commands = blue_h.DEFAULT_LOG_EXTERNAL_COMMANDS;
        blue.log_passive_checks = blue_h.DEFAULT_LOG_PASSIVE_CHECKS;
        blue.logging_options = blue_h.NSLOG_RUNTIME_ERROR | blue_h.NSLOG_RUNTIME_WARNING | blue_h.NSLOG_VERIFICATION_ERROR | blue_h.NSLOG_VERIFICATION_WARNING | blue_h.NSLOG_CONFIG_ERROR | blue_h.NSLOG_CONFIG_WARNING | blue_h.NSLOG_PROCESS_INFO | blue_h.NSLOG_HOST_NOTIFICATION | blue_h.NSLOG_SERVICE_NOTIFICATION | blue_h.NSLOG_EVENT_HANDLER | blue_h.NSLOG_EXTERNAL_COMMAND | blue_h.NSLOG_PASSIVE_CHECK | blue_h.NSLOG_HOST_UP | blue_h.NSLOG_HOST_DOWN | blue_h.NSLOG_HOST_UNREACHABLE | blue_h.NSLOG_SERVICE_OK | blue_h.NSLOG_SERVICE_WARNING | blue_h.NSLOG_SERVICE_UNKNOWN | blue_h.NSLOG_SERVICE_CRITICAL | blue_h.NSLOG_INFO_MESSAGE;
        blue.syslog_options = blue_h.NSLOG_RUNTIME_ERROR | blue_h.NSLOG_RUNTIME_WARNING | blue_h.NSLOG_VERIFICATION_ERROR | blue_h.NSLOG_VERIFICATION_WARNING | blue_h.NSLOG_CONFIG_ERROR | blue_h.NSLOG_CONFIG_WARNING | blue_h.NSLOG_PROCESS_INFO | blue_h.NSLOG_HOST_NOTIFICATION | blue_h.NSLOG_SERVICE_NOTIFICATION | blue_h.NSLOG_EVENT_HANDLER | blue_h.NSLOG_EXTERNAL_COMMAND | blue_h.NSLOG_PASSIVE_CHECK | blue_h.NSLOG_HOST_UP | blue_h.NSLOG_HOST_DOWN | blue_h.NSLOG_HOST_UNREACHABLE | blue_h.NSLOG_SERVICE_OK | blue_h.NSLOG_SERVICE_WARNING | blue_h.NSLOG_SERVICE_UNKNOWN | blue_h.NSLOG_SERVICE_CRITICAL | blue_h.NSLOG_INFO_MESSAGE;
        blue.service_check_timeout = blue_h.DEFAULT_SERVICE_CHECK_TIMEOUT;
        blue.host_check_timeout = blue_h.DEFAULT_HOST_CHECK_TIMEOUT;
        blue.event_handler_timeout = blue_h.DEFAULT_EVENT_HANDLER_TIMEOUT;
        blue.notification_timeout = blue_h.DEFAULT_NOTIFICATION_TIMEOUT;
        blue.ocsp_timeout = blue_h.DEFAULT_OCSP_TIMEOUT;
        blue.ochp_timeout = blue_h.DEFAULT_OCHP_TIMEOUT;
        blue.sleep_time = blue_h.DEFAULT_SLEEP_TIME;
        blue.interval_length = blue_h.DEFAULT_INTERVAL_LENGTH;
        blue.service_inter_check_delay_method = blue_h.ICD_SMART;
        blue.host_inter_check_delay_method = blue_h.ICD_SMART;
        blue.service_interleave_factor_method = blue_h.ILF_SMART;
        blue.max_service_check_spread = blue_h.DEFAULT_SERVICE_CHECK_SPREAD;
        blue.max_host_check_spread = blue_h.DEFAULT_HOST_CHECK_SPREAD;
        blue.use_aggressive_host_checking = blue_h.DEFAULT_AGGRESSIVE_HOST_CHECKING;
        blue.soft_state_dependencies = common_h.FALSE;
        blue.retain_state_information = common_h.FALSE;
        blue.retention_update_interval = blue_h.DEFAULT_RETENTION_UPDATE_INTERVAL;
        blue.use_retained_program_state = common_h.TRUE;
        blue.use_retained_scheduling_info = common_h.FALSE;
        blue.retention_scheduling_horizon = blue_h.DEFAULT_RETENTION_SCHEDULING_HORIZON;
        blue.modified_host_process_attributes = common_h.MODATTR_NONE;
        blue.modified_service_process_attributes = common_h.MODATTR_NONE;
        blue.command_check_interval = blue_h.DEFAULT_COMMAND_CHECK_INTERVAL;
        blue.service_check_reaper_interval = blue_h.DEFAULT_SERVICE_REAPER_INTERVAL;
        blue.service_freshness_check_interval = blue_h.DEFAULT_FRESHNESS_CHECK_INTERVAL;
        blue.host_freshness_check_interval = blue_h.DEFAULT_FRESHNESS_CHECK_INTERVAL;
        blue.auto_rescheduling_interval = blue_h.DEFAULT_AUTO_RESCHEDULING_INTERVAL;
        blue.auto_rescheduling_window = blue_h.DEFAULT_AUTO_RESCHEDULING_WINDOW;
        blue.check_external_commands = blue_h.DEFAULT_CHECK_EXTERNAL_COMMANDS;
        blue.check_orphaned_services = blue_h.DEFAULT_CHECK_ORPHANED_SERVICES;
        blue.check_service_freshness = blue_h.DEFAULT_CHECK_SERVICE_FRESHNESS;
        blue.check_host_freshness = blue_h.DEFAULT_CHECK_HOST_FRESHNESS;
        blue.auto_reschedule_checks = blue_h.DEFAULT_AUTO_RESCHEDULE_CHECKS;
        blue.log_rotation_method = common_h.LOG_ROTATION_NONE;
        blue.last_command_check = 0L;
        blue.last_command_status_update = 0L;
        blue.last_log_rotation = 0L;
        blue.max_parallel_service_checks = blue_h.DEFAULT_MAX_PARALLEL_SERVICE_CHECKS;
        blue.currently_running_service_checks = 0;
        blue.enable_notifications = common_h.TRUE;
        blue.execute_service_checks = common_h.TRUE;
        blue.accept_passive_service_checks = common_h.TRUE;
        blue.execute_host_checks = common_h.TRUE;
        blue.accept_passive_service_checks = common_h.TRUE;
        blue.enable_event_handlers = common_h.TRUE;
        blue.obsess_over_services = common_h.FALSE;
        blue.obsess_over_hosts = common_h.FALSE;
        blue.enable_failure_prediction = common_h.TRUE;
        blue.aggregate_status_updates = common_h.TRUE;
        blue.status_update_interval = blue_h.DEFAULT_STATUS_UPDATE_INTERVAL;
        blue.event_broker_options = broker_h.BROKER_NOTHING;
        blue.time_change_threshold = blue_h.DEFAULT_TIME_CHANGE_THRESHOLD;
        blue.enable_flap_detection = blue_h.DEFAULT_ENABLE_FLAP_DETECTION;
        blue.low_service_flap_threshold = blue_h.DEFAULT_LOW_SERVICE_FLAP_THRESHOLD;
        blue.high_service_flap_threshold = blue_h.DEFAULT_HIGH_SERVICE_FLAP_THRESHOLD;
        blue.low_host_flap_threshold = blue_h.DEFAULT_LOW_HOST_FLAP_THRESHOLD;
        blue.high_host_flap_threshold = blue_h.DEFAULT_HIGH_HOST_FLAP_THRESHOLD;
        blue.process_performance_data = blue_h.DEFAULT_PROCESS_PERFORMANCE_DATA;
        blue.date_format = common_h.DATE_FORMAT_US;
        for (int x = 0; x < blue_h.MACRO_X_COUNT; x++) blue.macro_x[x] = null;
        for (int x = 0; x < blue_h.MAX_COMMAND_ARGUMENTS; x++) blue.macro_argv[x] = null;
        for (int x = 0; x < blue_h.MAX_USER_MACROS; x++) blue.macro_user[x] = null;
        for (int x = 0; x < objects_h.MAX_CONTACT_ADDRESSES; x++) blue.macro_contactaddress[x] = null;
        blue.macro_ondemand = null;
        utils.init_macrox_names();
        blue.global_host_event_handler = null;
        blue.global_service_event_handler = null;
        blue.ocsp_command = null;
        blue.ochp_command = null;
        logger.trace("exiting " + cn + ".reset_variables()");
        return common_h.OK;
    }

    public static int lock_file_exists() {
        File lock = new File(blue.lock_file);
        if (lock.exists()) return common_h.OK;
        return common_h.ERROR;
    }

    public static int atoi(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfE) {
            logger.error("warning: " + nfE.getMessage(), nfE);
            return 0;
        }
    }

    public static String[] getEnv(HashMap<String, String> envHashMap) {
        String[] result = new String[envHashMap.size()];
        int x = 0;
        for (Iterator<Map.Entry<String, String>> iter = envHashMap.entrySet().iterator(); iter.hasNext(); x++) {
            Map.Entry<String, String> e = iter.next();
            result[x] = e.getKey() + "=" + e.getValue();
        }
        return result;
    }

    public static String strip(String value) {
        if (value != null) value = value.trim();
        return value;
    }

    public static long strtoul(String value, Object ignore, int base) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException nfE) {
            logger.error("warning: " + nfE.getMessage(), nfE);
            return 0L;
        }
    }

    public static long currentTimeInSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    public static long getTimeInSeconds(Calendar t) {
        return t.getTimeInMillis() / 1000;
    }

    /** 
    * Parses a command like string converting into an array of command parameters.
    * Supports basic "java -jar x.jar -s "test me" -x file" would be 7 parameters
    * java, -jar, x.jar, -s, test me, -x, file
    */
    public static String[] processCommandLine(String command) {
        ArrayList<String> list = new ArrayList<String>();
        String pattern = "\"([^\"]+?)\" ?|([^ ]+) ?| ";
        Pattern cliRE = Pattern.compile(pattern);
        Matcher m = cliRE.matcher(command);
        while (m.find()) {
            String match = m.group();
            if (match == null) break;
            if (match.endsWith(" ")) {
                match = match.substring(0, match.length() - 1);
            }
            if (match.startsWith("\"")) {
                match = match.substring(1, match.length() - 1);
            }
            if (match.length() != 0) list.add(match);
        }
        return list.toArray(new String[list.size()]);
    }

    public static int copyDirectory(File srcDir, File destDir) {
        if (!srcDir.isDirectory()) return common_h.ERROR;
        if (!destDir.canWrite()) return common_h.ERROR;
        String[] contents = srcDir.list();
        for (int i = 0; i < contents.length; i++) {
            if (new File(srcDir.getAbsolutePath(), contents[i]).isDirectory()) copyDirectory(new File(srcDir.getAbsolutePath(), contents[i]), new File(destDir.getAbsolutePath(), contents[i])); else {
                try {
                    InputStream in = new FileInputStream(srcDir.getAbsolutePath() + "/" + contents[i]);
                    OutputStream out = new FileOutputStream(destDir.getAbsolutePath() + "/" + contents[i]);
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();
                } catch (Exception e) {
                    return common_h.ERROR;
                }
            }
        }
        return common_h.OK;
    }

    public static int replaceString(String oldString, String newString, File filename) throws IOException {
        String line;
        StringBuffer buffer = new StringBuffer();
        newString = newString.replace("\\", "/");
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
        while ((line = reader.readLine()) != null) {
            line = line.replace(oldString, newString);
            buffer.append(line + "\n");
        }
        reader.close();
        BufferedWriter out = new BufferedWriter(new FileWriter(filename));
        out.write(buffer.toString());
        out.close();
        return common_h.OK;
    }

    public static int lockConfigTool() {
        BufferedWriter out;
        String installDir = System.getProperty("user.dir");
        installDir = installDir.replace("\\", "/");
        try {
            out = new BufferedWriter(new FileWriter(installDir + "/blueconfig.log"));
            out.write(installDir + "/etc/basic");
            out.close();
            out = new BufferedWriter(new FileWriter(installDir + "/blueconfig.lock"));
            out.write("New Config");
            out.close();
        } catch (Exception e) {
            return common_h.ERROR;
        }
        return common_h.OK;
    }
}
