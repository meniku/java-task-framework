/*
 * Copyright 2013 YMC AG. All rights reserved.
 */
package ch.ymc.task;

/**
 * Event-Listener-Interface for a {@link TaskRunner}.
 *
 * @param <T> The concrete {@link AbstractTask}-Type
 * @author nkuebler, YMC AG
 * @since 2.0.0
 */
public interface TaskEventListener<T extends AbstractTask> {

    /**
     * Called when a Task has finished it's execution, regardless of the result
     * (error, cancel, error or fatal).
     *
     * @param task   the task that has completed it's execution
     * @param result the result of the execution
     */
    void taskComplete(T task, TaskResult result);
}
