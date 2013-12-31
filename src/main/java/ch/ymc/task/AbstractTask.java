/*
 * Copyright 2013 YMC AG. All rights reserved.
 */
package ch.ymc.task;

/**
 * A Task holds a piece of code in it's {@link #run()}-method, that is executed
 * in a separate thread by a {@link TaskRunner}. Tasks can be requested for
 * cancellation through the {@link TaskRunner}, it is up to the concrete
 * implementation of {@link #cancel()} to cancel the execution.
 *
 * @author nkuebler, YMC AG
 * @since 2.0.0
 */
public abstract class AbstractTask {

    private Thread thread;
    private TaskState state = TaskState.INITIALIZED;
    private TaskResult result;

    /**
     * Main Method for the task. The logic for the task has to be implemented
     * here.
     *
     * @return result of the task
     */
    protected abstract TaskResult run();

    /**
     * Called by the {@link TaskRunner} when cancellation is requested.
     */
    protected abstract void cancel();

    /**
     * Each Task runs in it's own Thread-instance.
     *
     * @return the thread in which this task is executed
     */
    protected final Thread getThread() {
        return thread;
    }

    /**
     * Package-Private method called by the {@link TaskRunner} when the task
     * will be started.
     *
     * @param thread the thread in which the task is being executed
     */
    final void setThread(final Thread thread) {
        this.thread = thread;
    }

    /**
     * Package-Private method called by the {@link TaskRunner} to keep informed
     * of the current task-state.
     *
     * @return the task-state
     */
    final TaskState getState() {
        return state;
    }

    /**
     * Package-Private method called by the {@link TaskRunner} when the state of
     * the task is changed.
     *
     * @param state the new state
     */
    final void setState(final TaskState state) {
        this.state = state;
    }

    /**
     * Result of the task after completed.
     *
     * @return the task-result
     */
    final public TaskResult getResult() {
        return result;
    }

    /**
     * Package-Private method called by the {@link TaskRunner} when the task is
     * completed.
     *
     * @param result the task-result
     */
    final void setResult(final TaskResult result) {
        this.result = result;
    }

}
