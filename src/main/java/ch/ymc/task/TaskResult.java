/*
 * Copyright 2013 YMC AG. All rights reserved.
 */
package ch.ymc.task;

/**
 * The Result with which an {@link AbstractTask} may return.
 *
 * @author nkuebler, YMC AG
 * @since 2.0.0
 */
public class TaskResult {

    public static final byte SUCCESS = 1;
    public static final byte CANCEL = 2;
    public static final byte ERROR = 3;
    public static final byte FATAL = 4;

    private transient final byte status;
    private transient final Throwable exception;

    /**
     * Constructor without exception.
     *
     * @param status the status code
     */
    public TaskResult(final byte status) {
        this.status = status;
        this.exception = null;
    }

    /**
     * Default Constructor.
     *
     * @param status    the status code
     * @param exception an optional exception-attachment, usually only when completed
     *                  with {@link #FATAL} or {@link #ERROR} status
     */
    public TaskResult(final byte status, final Throwable exception) {
        this.status = status;
        this.exception = exception;
    }

    public byte getStatus() {
        return status;
    }

    public Throwable getException() {
        return exception;
    }

    // CHECKSTYLE:OFF

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (exception == null ? 0 : exception.hashCode());
        result = prime * result + status;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TaskResult other = (TaskResult) obj;
        if (exception == null) {
            if (other.exception != null) {
                return false;
            }
        } else if (!exception.equals(other.exception)) {
            return false;
        }
        if (status != other.status) {
            return false;
        }
        return true;
    }

    // CHECKSTYLE:ON
}
