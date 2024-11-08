package server.service.impl;

import static db.DB.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javolution.util.FastMap;
import org.hibernate.Session;
import org.slf4j.Logger;
import server.managed.ChannelData;
import server.managed.Presence;
import server.manager.ClassroomManager;
import util.Log;
import lang.*;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.Channel;
import com.sun.sgs.app.ChannelManager;
import com.sun.sgs.app.DataManager;
import com.sun.sgs.app.Delivery;
import com.sun.sgs.impl.util.AbstractKernelRunnable;
import com.sun.sgs.kernel.ComponentRegistry;
import com.sun.sgs.kernel.TaskOwner;
import com.sun.sgs.kernel.TaskScheduler;
import com.sun.sgs.service.Service;
import com.sun.sgs.service.TransactionProxy;
import com.sun.sgs.service.TransactionRunner;
import common.Constants;

public class ClassroomServiceImpl implements ClassroomManager, Service {

    private static final Logger logger = Log.getLogger(ClassroomServiceImpl.class);

    private TaskScheduler taskScheduler;

    private ExecutorService executor;

    private TaskOwner taskOwner;

    public ClassroomServiceImpl(Properties properties, ComponentRegistry registry) {
        logger.info("init");
        taskScheduler = registry.getComponent(TaskScheduler.class);
        executor = Executors.newFixedThreadPool(3);
    }

    public void configure(ComponentRegistry registry, TransactionProxy proxy) {
        taskOwner = proxy.getCurrentOwner();
    }

    public void loadAllClassrooms(final VoidCallback callback) {
        executor.execute(new Runnable() {

            public void run() {
                final Map<db.Channel, Set<Presence>> classroomMap = executor_prepareAllClassrooms(callback);
                if (classroomMap != null) {
                    taskScheduler.scheduleTask(new TransactionRunner(new AbstractKernelRunnable() {

                        public void run() throws Exception {
                            scheduler_loadAllClassrooms(classroomMap, callback);
                        }
                    }), taskOwner);
                }
            }
        });
    }

    private Map<db.Channel, Set<Presence>> executor_prepareAllClassrooms(final VoidCallback callback) {
        try {
            Session s = begin();
            List classrooms = getAll(s, db.Channel.class);
            final Map<db.Channel, Set<Presence>> classroomMap = new FastMap<db.Channel, Set<Presence>>();
            for (Object o : classrooms) {
                db.Channel c = (db.Channel) o;
                Set<Presence> presenceSet = new HashSet<Presence>();
                for (db.Account a : c.getMembers()) {
                    presenceSet.add(new Presence(a.getAccountName(), false));
                }
                classroomMap.put(c, presenceSet);
                logger.info(String.format("loaded '%s' from db", c.getChannelName()));
            }
            commit(s);
            return classroomMap;
        } catch (final Exception ex) {
            taskScheduler.scheduleTask(new TransactionRunner(new AbstractKernelRunnable() {

                public void run() throws Exception {
                    if (callback != null) {
                        callback.exceptionThrown(ex);
                    }
                }
            }), taskOwner);
            return null;
        }
    }

    private void scheduler_loadAllClassrooms(final Map<db.Channel, Set<Presence>> classroomMap, final VoidCallback callback) {
        try {
            ChannelManager cman = AppContext.getChannelManager();
            DataManager dman = AppContext.getDataManager();
            for (db.Channel c : classroomMap.keySet()) {
                Set<Presence> presenceSet = classroomMap.get(c);
                Channel newChannel = cman.createChannel(c.getChannelName(), null, Delivery.RELIABLE);
                String boundName = Constants.CHANNEL_DATA + newChannel.getName();
                ChannelData data = new ChannelData(newChannel);
                data.addPresences(presenceSet);
                dman.setBinding(boundName, data);
                logger.info(String.format("bound '%s' to '%s'", newChannel.getName(), boundName));
            }
            if (callback != null) {
                callback.callback();
            }
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
            if (callback != null) {
                callback.exceptionThrown(ex);
            }
        }
    }

    public String getName() {
        return toString();
    }

    public boolean shutdown() {
        return false;
    }
}
