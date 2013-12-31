/*
 * Copyright 2013 YMC AG. All rights reserved.
 */
package ch.ymc.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link TaskRunner} ensures that a maximum of {@link #numberOfConcurrentTasks}
 * may be executed at once.
 *
 * @param <T> The concrete {@link AbstractTask}-Type
 * @author nkuebler, YMC AG
 * @since 2.0.0
 */
public class QueuedTaskRunner<T extends AbstractTask> implements TaskRunner<T>, TaskEventListener<T> {

    private static final Logger LOG = LoggerFactory.getLogger(QueuedTaskRunner.class);
    private final Queue<T> queue;
    private TaskRunner<T> taskRunner = new ConcurrentTaskRunner<T>();
    private Lock queueLock = new ReentrantLock();
    private Condition taskStartCondition = getQueueLock().newCondition();
    private int numberOfConcurrentTasks = 50;
    private TaskEventListener<T> taskEventListener;

    /**
     * Default Constructor.
     */
    public QueuedTaskRunner() {
        this(new Comparator<T>() {
            @Override
            public int compare(T t, T t2) {
                return 0;
            }
        });
    }

    /**
     * Default Constructor.
     *
     * @param comperator describes the order to run the tasks
     */
    public QueuedTaskRunner(final Comparator<T> comperator) {
        queue = new PriorityQueue<T>(10, comperator);
        taskRunner.setTaskEventListener(this);
    }

    @Override
    public void execute(final T task) throws TaskException, InterruptedException {
        getQueueLock().lock();
        try {
            getQueue().add(task);
            flush();
        } finally {
            getQueueLock().unlock();
        }
    }

    @Override
    public boolean isExecuting(final T task) {
        getQueueLock().lock();
        try {
            return getTaskRunner().isExecuting(task) || getQueue().contains(task);
        } finally {
            getQueueLock().unlock();
        }
    }

    public boolean isRunning(final T task) {
        getQueueLock().lock();
        try {
            return getTaskRunner().isExecuting(task);
        } finally {
            getQueueLock().unlock();
        }
    }

    @Override
    public boolean isComplete(final T task) {
        getQueueLock().lock();
        try {
            return getTaskRunner().isComplete(task);
        } finally {
            getQueueLock().unlock();
        }
    }

    @Override
    public boolean hasStarted(final T task) {
        getQueueLock().lock();
        try {
            return getTaskRunner().hasStarted(task) || getQueue().contains(task);
        } finally {
            getQueueLock().unlock();
        }
    }

    @Override
    public void cancel(final T task) throws TaskException {
        boolean canceledBeforeExecution = false;
        getQueueLock().lock();
        try {
            if (!taskRunner.isExecuting(task) && task.getState() == TaskState.INITIALIZED) {
                if (getQueue().remove(task)) {
                    task.setState(TaskState.COMPLETE);
                    task.setResult(new TaskResult(TaskResult.CANCEL));
                    canceledBeforeExecution = true;
                } else {
                    throw new TaskException("Task doesn't belong to this TaskRunner");
                }
            }
        } finally {
            getQueueLock().unlock();
        }

        if (canceledBeforeExecution) {
            if (this.getTaskEventListener() != null) {
                this.getTaskEventListener().taskComplete(task, task.getResult());
            }
        } else {
            if (taskRunner.isExecuting(task)) {
                getTaskRunner().cancel(task);
            } else {
                LOG.error("could not cancel Task: '{}'", task);
            }
        }
    }

    /**
     * Polls jobs from the queue and starts them until the number of currently
     * executed tasks reaches {@link #getNumberOfEnqueuedTasks()}.
     *
     * @throws TaskException        when the task has a wrong state or doesn't belong to this
     *                              {@link TaskRunner}-Instance
     * @throws InterruptedException the current thread got interrupted
     */
    protected void flush() throws TaskException, InterruptedException {
        getQueueLock().lock();
        try {
            while (getTaskRunner().getNumberOfExecutingTasks() < getNumberOfConcurrentTasks() && !getQueue().isEmpty()) {
                getTaskRunner().execute(getQueue().poll());
            }
            getTaskStartedCondition().signalAll();
        } finally {
            getQueueLock().unlock();
        }
    }

    @Override
    public void join() throws InterruptedException, TaskException {
        HashSet<T> set = null;
        try {
            queueLock.lock();
            set = new HashSet<T>(queue);
        } finally {
            queueLock.unlock();
        }
        join(set);
        taskRunner.join();
    }

    @Override
    public void join(final Iterable<T> tasks) throws InterruptedException, TaskException {
        for (final T task : tasks) {
            join(task);
        }
    }

    @Override
    public void join(final T task) throws InterruptedException, TaskException {
        while (task.getState() == TaskState.INITIALIZED) {
            try {
                this.getQueueLock().lock();
                if (task.getState() == TaskState.INITIALIZED) {
                    if (!this.getQueue().contains(task)) {
                        throw new TaskException("Task doesn't belong to this taskRunner");
                    }
                    this.getTaskStartedCondition().await();
                }
            } finally {
                this.getQueueLock().unlock();
            }
        }
        getTaskRunner().join(task);
    }

    @Override
    public void taskComplete(final T task, final TaskResult result) {
        try {
            flush();
            if (this.getTaskEventListener() != null) {
                getTaskEventListener().taskComplete(task, result);
            }
        } catch (final TaskException e) {
            if (this.getTaskEventListener() != null) {
                getTaskEventListener().taskComplete(task, new TaskResult(TaskResult.FATAL, e));
            }
        } catch (final InterruptedException e) {
            if (this.getTaskEventListener() != null) {
                getTaskEventListener().taskComplete(task, new TaskResult(TaskResult.FATAL, e));
            }
        }
    }

    public int getNumberOfConcurrentTasks() {
        return numberOfConcurrentTasks;
    }

    public void setNumberOfConcurrentTasks(final int maximumNumberOfConcurrentTasks) {
        this.numberOfConcurrentTasks = maximumNumberOfConcurrentTasks;
    }

    @Override
    public int getNumberOfExecutingTasks() {
        return getNumberOfRunningTasks() + getNumberOfEnqueuedTasks();
    }

    public int getNumberOfEnqueuedTasks() {
        return this.getQueue().size();
    }

    public int getNumberOfRunningTasks() {
        return getTaskRunner().getNumberOfExecutingTasks();
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
        this.join();
        if(this.getTaskRunner() != null) {
            this.getTaskRunner().dispose();;
        }
        this.taskRunner = null;
    }

    protected TaskRunner<T> getTaskRunner() {
        return taskRunner;
    }

    protected void setTaskRunner(final TaskRunner<T> taskRunner) {
        this.taskRunner = taskRunner;
    }

    private Queue<T> getQueue() {
        return queue;
    }

    protected Lock getQueueLock() {
        return queueLock;
    }

    protected void setQueueLock(final Lock queueLock) {
        this.queueLock = queueLock;
    }

    protected Condition getTaskStartedCondition() {
        return taskStartCondition;
    }

    protected void setTaskStartedCondition(final Condition taskStartedCondition) {
        this.taskStartCondition = taskStartedCondition;
    }
}
