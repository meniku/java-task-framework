/*
 * Copyright 2013 YMC AG. All rights reserved.
 */
package ch.ymc.task;

/**
 * The {@link TaskException} is thrown by the {@link TaskRunner} when a
 * particual task has a wrong state or other a task doesn't belong to the
 * {@link TaskRunner}-instance.
 *
 * @author nkuebler, YMC AG
 * @since 2.0.0
 */
public class TaskException extends Exception {

    /**
     * generated serialVersionUID.
     */
    private static final long serialVersionUID = -6968616348325838894L;

    /**
     * Default constructor.
     *
     * @param message the error-message
     */
    public TaskException(final String message) {
        super(message);
    }

}
