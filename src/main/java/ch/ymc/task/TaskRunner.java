/*
 * Copyright 2013 YMC AG. All rights reserved.
 */
package ch.ymc.task;

/**
 * The {@link TaskRUnner} is used to start or cancel {@link AbstractTask}s. It
 * keeps track of which tasks are running and notifies the
 * {@link TaskEventListener} about finished, canceled or failed Tasks. It is up
 * to the concrete implementation to decide in which order the tasks are
 * processed.
 *
 * @param <T> The concrete {@link AbstractTask}-Type
 * @author nkuebler, YMC AG
 * @since 2.0.0
 */
public interface TaskRunner<T extends AbstractTask> {

    /**
     * Starts the execution of a {@link AbstractTask}.
     *
     * @param task the task to start
     * @throws TaskException        when the task has a wrong state
     * @throws InterruptedException when the current thread got interrupted
     */
    void execute(final T task) throws TaskException, InterruptedException;

    /**
     * Cancel a running {@link AbstractTask}.
     *
     * @param task the task to start
     * @throws TaskException when the task has a wrong state or doesn't belong to this
     *                       {@link TaskRunner}-Instance
     */
    void cancel(final T task) throws TaskException;

    /**
     * Test whether the given task is currently executed by this TaskRunner.
     *
     * @param task the task to test
     * @return true when the task is executing
     */
    boolean isExecuting(final T task);

    /**
     * Test whether the given task is already completed by this TaskRunner.
     *
     * @param task the task to test
     * @return true when the task is completed
     */
    boolean isComplete(final T task);

    /**
     * Test whether the given task is currently executing or is already
     * finished.
     *
     * @param task the task to test
     * @return true when the task is executing or completed
     */
    boolean hasStarted(final T task);

    /**
     * Return the number of currently processed tasks.
     *
     * @return the number of currently processed tasks
     */
    int getNumberOfExecutingTasks();

    /**
     * Wait for a collection of tasks.
     *
     * @param tasks the iterator for the tasks
     * @throws InterruptedException when the current thread got interrupted while waiting
     * @throws TaskException        when a task was in a wrong state
     */
    void join(final Iterable<T> tasks) throws InterruptedException, TaskException;

    /**
     * Wait for one task to be completed.
     *
     * @param task the task to wait for
     * @throws InterruptedException when the current thread got interrupted while waiting
     * @throws TaskException        when a task was in a wrong state
     */
    void join(final T task) throws InterruptedException, TaskException;


    /**
     * Wait for all tasks to be completed.
     *
     * @throws InterruptedException when the current thread got interrupted while waiting
     * @throws TaskException        when a task was in a wrong state
     */
    void join() throws InterruptedException, TaskException;

    /**
     * Getter for the TaskEventListener.
     *
     * @return the listener
     */
    TaskEventListener<T> getTaskEventListener();

    /**
     * Setter for the TaskEventLIstener.
     *
     * @param taskEventListener the listener
     */
    void setTaskEventListener(final TaskEventListener<T> taskEventListener);

    /**
     * Disposes this task runenr
     */
    void dispose() throws TaskException, InterruptedException;
}
