package org.llama.jmex.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;
import com.jme.util.Timer;

public class TaskManager {

    private Task[] tasks;

    private int tasklimit;

    Timer timer;

    private HashSet identifiers = new HashSet();

    private TreeSet currenttasks = new TreeSet();

    public TaskManager(Timer timer) {
        tasks = new Task[10];
        if (timer == null) this.timer = Timer.getTimer(); else this.timer = timer;
    }

    public TaskManager() {
        this(null);
    }

    public synchronized boolean containsTask(Object identifier) {
        return identifiers.contains(identifier);
    }

    public synchronized void addTask(Task task, TaskRunner taskrunner) {
        task.setManagerAndRunner(this, taskrunner);
        identifiers.add(task.identifier());
        if (tasklimit + 1 >= tasks.length) {
            Task[] temptasks = new Task[tasks.length * 2];
            System.arraycopy(tasks, 0, temptasks, 0, tasks.length);
            tasks = temptasks;
        }
        tasks[tasklimit] = task;
        tasklimit++;
    }

    public synchronized void sortTasks() {
        for (int k = 0; k < tasklimit; k++) {
            tasks[k].updatePriority();
        }
        Arrays.sort(tasks, 0, tasklimit);
    }

    float start, end;

    private void completedTask(Task task) {
        task.reset();
        synchronized (this) {
            identifiers.remove(task.identifier());
        }
    }

    private Object[] taskrunners = new TaskRunner[0];

    private ArrayList taskrunnerslist = new ArrayList();

    public synchronized void addTaskRunner(TaskRunner runner) {
        if (taskrunnerslist.add(runner)) taskrunners = taskrunnerslist.toArray();
    }

    public synchronized void removeTaskRunner(TaskRunner runner) {
        if (taskrunnerslist.remove(runner)) taskrunners = taskrunnerslist.toArray();
    }

    public float timeLeft() {
        return end - timer.getTimeInSeconds();
    }

    public void runTasks(float seconds) {
        runTasks(seconds, taskrunners);
    }

    public void runTasks(float seconds, Object[] runners) {
        start = timer.getTimeInSeconds();
        end = start + seconds;
        currenttasks.clear();
        for (int k = 0; k < runners.length; k++) {
            Task[] tasks = ((TaskRunner) runners[k]).getTasks();
            for (int i = 0; i < tasks.length; i++) {
                Task task = tasks[i];
                if (task != null && task.isUsingManager(this)) {
                    if (task.isCompleted()) {
                        completedTask(task);
                    } else if (task.isPaused()) {
                        task.prepareRun();
                        currenttasks.add(tasks[i]);
                    }
                }
            }
        }
        for (int k = 0; k < runners.length; k++) {
            TaskRunner runner = (TaskRunner) runners[k];
            int tasksearch = tasklimit;
            int reserve = runner.reserveTask();
            while (reserve != -1 && --tasksearch >= 0) {
                if (tasks[tasksearch].isUsingRunner(runner)) {
                    Task task = tasks[tasksearch];
                    tasks[tasksearch] = null;
                    tasklimit--;
                    for (int i = tasksearch; i < tasklimit; i++) {
                        tasks[i] = tasks[i + 1];
                    }
                    task.assign(reserve);
                    task.prepareRun();
                    currenttasks.add(task);
                    reserve = runner.reserveTask();
                } else {
                }
            }
            if (reserve != -1) runner.cancelReserveTask(reserve);
        }
        Iterator it = currenttasks.iterator();
        while (it.hasNext() && timeLeft() > 0) {
            Task task = (Task) it.next();
            task.resume();
        }
        it = currenttasks.iterator();
        while (it.hasNext()) {
            Task task = (Task) it.next();
            task.attemptPause();
        }
    }
}
