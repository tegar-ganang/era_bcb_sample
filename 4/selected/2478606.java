package me.buick.util.jmeter.snmpprocessvisualizers;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTable;
import me.buick.util.snmp.core.pojo.ProcessInfoPojo;
import org.apache.jorphan.reflect.Functor;

public class SortableObjectTableModel extends SNMPTableModel {

    private static final long serialVersionUID = 1204108108120389504L;

    private final OrderRule OR = new OrderRule();

    private Object lock = new Object();

    public SortableObjectTableModel(String[] headers, Class<ProcessInfoPojo> _objClass, Functor[] readFunctors, Functor[] writeFunctors, Class<Object>[] editorClasses) {
        super(headers, _objClass, readFunctors, writeFunctors, editorClasses);
    }

    private final Comparator<ProcessInfoPojo> COMPARE_WITH_PID = new Comparator<ProcessInfoPojo>() {

        public int compare(ProcessInfoPojo o1, ProcessInfoPojo o2) {
            if (o1 != null && o2 != null) {
                int pid1 = o1.getPid();
                int pid2 = o2.getPid();
                if (OR.getOrder() == 0) {
                    return new Integer(pid1).compareTo(new Integer(pid2));
                } else if (OR.getOrder() == 1) {
                    return new Integer(pid2).compareTo(new Integer(pid1));
                }
            }
            return 0;
        }
    };

    private final Comparator<ProcessInfoPojo> COMPARE_WITH_PROCNAME = new Comparator<ProcessInfoPojo>() {

        public int compare(ProcessInfoPojo o1, ProcessInfoPojo o2) {
            if (o1 != null && o2 != null) {
                String pname1 = o1.getProcName();
                String pname2 = o2.getProcName();
                if (OR.getOrder() == 0) {
                    return pname1.compareTo(pname2);
                } else if (OR.getOrder() == 1) {
                    return pname2.compareTo(pname1);
                }
            }
            return 0;
        }
    };

    private final Comparator<ProcessInfoPojo> COMPARE_WITH_PROCCPUUSAGE = new Comparator<ProcessInfoPojo>() {

        public int compare(ProcessInfoPojo o1, ProcessInfoPojo o2) {
            if (o1 != null && o2 != null) {
                double pcpu1 = o1.getCPUPerc();
                double pcpu2 = o2.getCPUPerc();
                if (OR.getOrder() == 0) {
                    return new Double(pcpu1).compareTo(new Double(pcpu2));
                } else if (OR.getOrder() == 1) {
                    return new Double(pcpu2).compareTo(new Double(pcpu1));
                }
            }
            return 0;
        }
    };

    private final Comparator<ProcessInfoPojo> COMPARE_WITH_PROCMEMUSAGE = new Comparator<ProcessInfoPojo>() {

        public int compare(ProcessInfoPojo o1, ProcessInfoPojo o2) {
            if (o1 != null && o2 != null) {
                double pcpu1 = o1.getCPUPerc();
                double pcpu2 = o2.getCPUPerc();
                if (OR.getOrder() == 0) {
                    return new Double(pcpu1).compareTo(new Double(pcpu2));
                } else if (OR.getOrder() == 1) {
                    return new Double(pcpu2).compareTo(new Double(pcpu1));
                }
            }
            return 0;
        }
    };

    private final Comparator<ProcessInfoPojo> COMPARE_WITH_PROCTYPE = new Comparator<ProcessInfoPojo>() {

        public int compare(ProcessInfoPojo o1, ProcessInfoPojo o2) {
            if (o1 != null && o2 != null) {
                int ptype1 = o1.getProcType();
                int ptype2 = o2.getProcType();
                if (OR.getOrder() == 0) {
                    return new Integer(ptype1).compareTo(new Integer(ptype2));
                } else if (OR.getOrder() == 1) {
                    return new Integer(ptype2).compareTo(new Integer(ptype1));
                }
            }
            return 0;
        }
    };

    private final Comparator<ProcessInfoPojo> COMPARE_WITH_PROCSTAT = new Comparator<ProcessInfoPojo>() {

        public int compare(ProcessInfoPojo o1, ProcessInfoPojo o2) {
            if (o1 != null && o2 != null) {
                int pstat1 = o1.getProcRunningStatus();
                int pstat2 = o2.getProcRunningStatus();
                if (OR.getOrder() == 0) {
                    return new Integer(pstat1).compareTo(new Integer(pstat2));
                } else if (OR.getOrder() == 1) {
                    return new Integer(pstat2).compareTo(new Integer(pstat1));
                }
            }
            return 0;
        }
    };

    public void addMouseListener(final JTable table) {
        table.getTableHeader().addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent event) {
                int tableColumn = table.columnAtPoint(event.getPoint());
                int modelColumn = table.convertColumnIndexToModel(tableColumn);
                synchronized (lock) {
                    if (OR.getColumn() == -1 && OR.getOrder() == -1) {
                        OR.setColumn(modelColumn);
                        OR.setOrder(0);
                    } else if (OR.getOrder() != -1) {
                        if (modelColumn == OR.getColumn()) {
                            if (OR.getOrder() == 0) {
                                OR.setOrder(1);
                            } else {
                                OR.setOrder(0);
                            }
                        } else {
                            OR.setColumn(modelColumn);
                            OR.setOrder(0);
                        }
                    }
                }
                SortableObjectTableModel.this.sort(false);
            }
        });
    }

    public synchronized void sort(boolean intervalFlag) {
        int column = OR.getColumn();
        switch(column) {
            case 0:
                Collections.sort(this.objects, COMPARE_WITH_PID);
                this.fireTableDataChanged();
                break;
            case 1:
                Collections.sort(this.objects, COMPARE_WITH_PROCNAME);
                this.fireTableDataChanged();
                break;
            case 3:
                Collections.sort(this.objects, COMPARE_WITH_PROCCPUUSAGE);
                this.fireTableDataChanged();
                break;
            case 4:
                Collections.sort(this.objects, COMPARE_WITH_PROCMEMUSAGE);
                this.fireTableDataChanged();
                break;
            case 5:
                Collections.sort(this.objects, COMPARE_WITH_PROCTYPE);
                this.fireTableDataChanged();
                break;
            case 6:
                Collections.sort(this.objects, COMPARE_WITH_PROCSTAT);
                this.fireTableDataChanged();
                break;
        }
    }

    public boolean hasOrdered() {
        return (OR.getOrder() == -1) ? false : true;
    }

    public List<ProcessInfoPojo> getRows() {
        return this.objects;
    }

    public Map<Integer, ProcessInfoPojo> getRowPIDMap() {
        Map<Integer, ProcessInfoPojo> map = new HashMap<Integer, ProcessInfoPojo>();
        for (ProcessInfoPojo pojo : this.objects) {
            map.put(pojo.getPid(), pojo);
        }
        return Collections.synchronizedMap(map);
    }
}
