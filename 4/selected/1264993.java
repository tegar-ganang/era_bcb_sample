package websiteschema.schedule;

import websiteschema.persistence.rdbms.ChannelMapper;
import org.quartz.JobKey;
import org.quartz.SimpleTrigger;
import websiteschema.persistence.rdbms.TaskMapper;
import org.quartz.Job;
import org.quartz.JobDataMap;
import websiteschema.persistence.rdbms.JobMapper;
import websiteschema.persistence.rdbms.StartURLMapper;
import websiteschema.persistence.rdbms.WrapperMapper;
import org.apache.log4j.Logger;
import websiteschema.model.domain.Schedule;
import java.text.ParseException;
import java.util.List;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import websiteschema.persistence.rdbms.ScheduleMapper;
import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.CronScheduleBuilder.*;
import static org.quartz.DateBuilder.*;

/**
 *
 * @author ray
 */
public class JobScheduler {

    public static final int Error = 0;

    public static final int Started = 1;

    public static final int Stopped = 2;

    public static final int Standby = 3;

    private Scheduler sched = null;

    private ScheduleMapper scheduleMapper = null;

    private JobMapper jobMapper = null;

    private ChannelMapper channelMapper = null;

    private WrapperMapper wrapperMapper = null;

    private TaskMapper taskMapper = null;

    private StartURLMapper startURLMapper = null;

    private final java.util.Random random = new java.util.Random();

    private final Logger l = Logger.getLogger(JobScheduler.class);

    private final String group = "group1";

    private SchedulerFactory schedulerFactory = new StdSchedulerFactory();

    public JobScheduler() {
    }

    /**
     * 从数据库中加载任务。
     * @throws SchedulerException
     */
    public void load() throws SchedulerException {
        sched = schedulerFactory.getScheduler();
        List<Schedule> all = scheduleMapper.getAll();
        for (Schedule sche : all) {
            load(sche);
        }
    }

    public void add(Schedule sche) throws SchedulerException {
        if (Started == status()) {
            load(sche);
        }
    }

    private void load(Schedule sche) throws SchedulerException {
        if (null != sched) {
            long jobId = sche.getJobId();
            long startURLId = sche.getStartURLId();
            int status = sche.getStatus();
            if (status == Schedule.STATUS_VALID && jobId > 0 && startURLId >= 0) {
                JobDetail job = createJob(sche, group);
                Trigger trigger = createTrigger(sche, group);
                if (null != job && null != trigger) {
                    sched.scheduleJob(job, trigger);
                    l.debug("add job: " + job.getKey());
                }
            } else {
                l.debug("schedule: " + sche.getId() + " is invalid. " + "which jobId is " + sche.getJobId() + " and startURLId is " + sche.getStartURLId());
            }
        }
    }

    public void remove(Schedule sche) throws SchedulerException {
        if (Started == status()) {
            long id = sche.getId();
            if (id > 0) {
                JobKey jobKey = new JobKey(String.valueOf(id), group);
                if (null != jobKey) {
                    sched.deleteJob(jobKey);
                    l.debug("delete job: " + jobKey);
                }
            } else {
                l.debug("schedule: " + sche.getId() + " is invalid.");
            }
        }
    }

    public void reload(Schedule sche) throws SchedulerException {
        if (Started == status()) {
            long jobId = sche.getJobId();
            long startURLId = sche.getStartURLId();
            int status = sche.getStatus();
            if (status == Schedule.STATUS_VALID && jobId > 0 && startURLId > 0) {
                JobDetail job = createJob(sche, group);
                Trigger trigger = createTrigger(sche, group);
                if (null != job && null != trigger) {
                    sched.deleteJob(job.getKey());
                    sched.scheduleJob(job, trigger);
                    l.debug("rescheduled job: " + job.getKey());
                }
            } else {
                l.debug("schedule: " + sche.getId() + " is invalid. " + "which jobId is " + sche.getJobId() + " and startURLId is " + sche.getStartURLId());
            }
        }
    }

    /**
     * 启动scheduler
     * @throws SchedulerException
     */
    public void startup() throws SchedulerException {
        sched.start();
    }

    /**
     * 检查scheduler的状态
     * @return
     * @throws SchedulerException
     */
    public int status() throws SchedulerException {
        if (null != sched) {
            if (sched.isShutdown()) {
                return Stopped;
            } else if (sched.isStarted()) {
                return Started;
            } else if (sched.isInStandbyMode()) {
                return Standby;
            }
        }
        return Error;
    }

    /**
     * 清空scheduler，并停止
     * @throws SchedulerException
     */
    public void shutdown() throws SchedulerException {
        sched.clear();
        sched.shutdown();
    }

    /**
     * 创建一个只执行一次的任务，并执行
     * @param sche
     * @return
     */
    public boolean createTempJob(Schedule sche) {
        try {
            if (Started == status()) {
                String g = "group2";
                JobDetail job = createJob(sche, g);
                Trigger trigger = createRunOnceTrigger(job.getKey(), g);
                sched.scheduleJob(job, trigger);
                return true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private JobDetail createJob(Schedule sche, String group) {
        JobDataMap jobDataMap = new JobDataMap();
        String jobType = jobMapper.getById(sche.getJobId()).getJobType();
        l.debug("jobId: " + sche.getJobId() + " jobType: " + jobType);
        jobDataMap.put("startURLMapper", startURLMapper);
        jobDataMap.put("jobMapper", jobMapper);
        jobDataMap.put("wrapperMapper", wrapperMapper);
        jobDataMap.put("taskMapper", taskMapper);
        jobDataMap.put("channelMapper", channelMapper);
        jobDataMap.put("schedulerId", sche.getId());
        jobDataMap.put("jobId", sche.getJobId());
        jobDataMap.put("startURLId", sche.getStartURLId());
        try {
            Class<? extends Job> jobClazz = (Class<? extends Job>) Class.forName(jobType);
            JobDetail job = newJob(jobClazz).withIdentity(String.valueOf(sche.getId()), group).usingJobData(jobDataMap).build();
            return job;
        } catch (Exception ex) {
            l.error(ex);
        }
        return null;
    }

    private Trigger createTrigger(Schedule sche, String group) {
        if (Schedule.TYPE_CRON == sche.getScheduleType()) {
            try {
                String schedule = getRandomSec() + " " + sche.getSchedule();
                Trigger trigger = newTrigger().withIdentity(String.valueOf(sche.getId()), group).withSchedule(cronSchedule(schedule)).forJob(String.valueOf(sche.getId()), group).startNow().build();
                return trigger;
            } catch (ParseException ex) {
                l.error(ex);
            }
        }
        return null;
    }

    private Trigger createRunOnceTrigger(JobKey jobKey, String group) {
        Trigger trigger = (SimpleTrigger) newTrigger().withIdentity(jobKey.getName(), group).startAt(futureDate(10, IntervalUnit.SECOND)).forJob(jobKey).build();
        return trigger;
    }

    private int getRandomSec() {
        return random.nextInt(60);
    }

    public void setScheduleMapper(ScheduleMapper scheduleMapper) {
        this.scheduleMapper = scheduleMapper;
    }

    public void setJobMapper(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    public void setStartURLMapper(StartURLMapper startURLMapper) {
        this.startURLMapper = startURLMapper;
    }

    public void setWrapperMapper(WrapperMapper wrapperMapper) {
        this.wrapperMapper = wrapperMapper;
    }

    public void setTaskMapper(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    public ChannelMapper getChannelMapper() {
        return channelMapper;
    }

    public void setChannelMapper(ChannelMapper channelMapper) {
        this.channelMapper = channelMapper;
    }
}
