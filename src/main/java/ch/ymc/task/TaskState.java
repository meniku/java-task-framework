/*
 * Copyright 2013 YMC AG. All rights reserved.
 */
package ch.ymc.task;

/**
 * The enumeration for the current execution-state of a {@link AbstractTask}.
 *
 * @author nkuebler, YMC AG
 * @since 2.0.0
 */
public enum TaskState {
    /**
     * The task has not yet been submitted to the {@link TaskRunner}.
     */
    INITIALIZED,

    /**
     * The task is running or is about to be started.
     */
    RUNNING,

    /**
     * Cancelation has been requested, it is up to the
     * {@link AbstractTask#doCancel()} implementation to stop execution.
     */
    CANCELING,

    /**
     * The Task has completed.
     */
    COMPLETE
}
