/*
 * Copyright 2013 YMC AG. All rights reserved.
 */
package ch.ymc.task;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The {@link TaskRunner} that uses the Executor-Framework of J2SE to run
 * tasks in parallel.
 *
 * @param <T> The concrete {@link AbstractTask}-Type
 * @author nkuebler, YMC AG
 * @since 2.0.0
 */
public class ConcurrentTaskRunner<T extends AbstractTask> implements TaskRunner<T> {

    private transient final Set<T> runningTasks;
    private transient final ExecutorService threadPool;
    private TaskEventListener<T> taskEventListener;
    private final transient Lock runningTasksLock = new ReentrantLock();
    private final transient Condition taskDoneCondition = runningTasksLock.newCondition();
    private final transient Condition taskStartCondition = runningTasksLock.newCondition();

    /**
     * Constructor.
     */
    public ConcurrentTaskRunner() {
        threadPool = Executors.newCachedThreadPool();
        runningTasks = new HashSet<T>();
    }


    @Override
    public void execute(final T task) throws TaskException, InterruptedException {
        if (task.getState() != TaskState.INITIALIZED) {
            throw new TaskException("Task '" + task + "' is not in initialzed state, state is: '" + task.getState() + "'.");
        }
        runningTasksLock.lock();
        try {
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        runningTasksLock.lock();
                        task.setThread(Thread.currentThread());
                        taskStartCondition.signal();
                    } finally {
                        runningTasksLock.unlock();
                    }
                    TaskResult result = null;
                    try {
                        result = task.run();
                    } catch (final Throwable t) {
                        result = new TaskResult(TaskResult.FATAL, t);
                    } finally {
                        complete(task, result);
                    }
                }
            };

            runningTasks.add(task);
            task.setState(TaskState.RUNNING);
            threadPool.execute(runnable);
            taskStartCondition.await();
        } finally {
            runningTasksLock.unlock();
        }
    }

    @Override
    public void cancel(final T task) throws TaskException {
        runningTasksLock.lock();
        try {
            if (!this.runningTasks.contains(task) && task.getState() != TaskState.COMPLETE) {
                throw new TaskException("Task doesn't belong to this TaskRunner");
            } else if (task.getState() == TaskState.RUNNING) {
                task.setState(TaskState.CANCELING);
                task.cancel();
            }
        } finally {
            runningTasksLock.unlock();
        }
    }

    @Override
    public boolean isExecuting(final T task) {
        runningTasksLock.lock();
        try {
            return task.getState() == TaskState.RUNNING || task.getState() == TaskState.CANCELING;
        } finally {
            runningTasksLock.unlock();
        }
    }

    @Override
    public boolean isComplete(final T task) {
        runningTasksLock.lock();
        try {
            return task.getState() == TaskState.COMPLETE;
        } finally {
            runningTasksLock.unlock();
        }
    }

    @Override
    public boolean hasStarted(final T task) {
        runningTasksLock.lock();
        try {
            return task.getState() != TaskState.INITIALIZED;
        } finally {
            runningTasksLock.unlock();
        }
    }

    @Override
    public int getNumberOfExecutingTasks() {
        try {
            runningTasksLock.lock();
            return getRunningTasks().size();
        } finally {
            runningTasksLock.unlock();
        }
    }

    @Override
    public void join() throws InterruptedException, TaskException {
        HashSet<T> set = null;
        try {
            runningTasksLock.lock();
            set = new HashSet<T>(getRunningTasks());
        } finally {
            runningTasksLock.unlock();
        }
        join(set);
    }

    @Override
    public void join(final Iterable<T> tasks) throws InterruptedException, TaskException {
        for (final T task : tasks) {
            join(task);
        }
    }

    @Override
    public void join(final T task) throws InterruptedException, TaskException {
        if (task.getState() == TaskState.INITIALIZED) {
            throw new TaskException("Task '" + task + "' has not yet been started.");
        }
        while (task.getState() != TaskState.COMPLETE) {
            try {
                runningTasksLock.lock();
                if (task.getState() != TaskState.COMPLETE) {
                    taskDoneCondition.await();
                }
            } finally {
                runningTasksLock.unlock();
            }
        }
    }

    /**
     * Package-Private method which is called when a task has completed it's
     * execution.
     *
     * @param task   the task that completed execution
     * @param result the execution-result
     */
    private void complete(final T task, final TaskResult result) {
        TaskResult taskResult = result;
        runningTasksLock.lock();
        try {
            if (!runningTasks.remove(task)) {
                taskResult = new TaskResult(TaskResult.FATAL, new TaskException("couldn't remove Task from runningTasks"));
            }
            task.setResult(taskResult);
            task.setState(TaskState.COMPLETE);
        } finally {
            runningTasksLock.unlock();
        }
        // TODO: SIDE-EFFECTS MAY HAPPEN, IF DEADLOCK OCCUR MOVE
        // TASK-DONE-CONDITION BACK BEFORE THIS
        if (this.taskEventListener != null) {
            this.taskEventListener.taskComplete(task, taskResult);
        }
        runningTasksLock.lock();
        try {
            taskDoneCondition.signalAll();
        } finally {
            runningTasksLock.unlock();
        }
    }

    public Set<T> getRunningTasks() {
        return runningTasks;
    }

    @Override
    public TaskEventListener<T> getTaskEventListener() {
        return taskEventListener;
    }

    @Override
    public void setTaskEventListener(final TaskEventListener<T> taskEventListener) {
        this.taskEventListener = taskEventListener;
    }

    @Override
    public void dispose() throws TaskException, InterruptedException {
        join();
        threadPool.shutdown();
    }
}
