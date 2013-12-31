/*
 * Copyright 2013 YMC AG. All rights reserved.
 */
package ch.ymc.task;

import org.junit.Ignore;

/**
 * Simple Task-Implementation for Unit-Tests in the Task-Framework.
 *
 * @author nkuebler, YMC AG
 * @since 2.0.0
 */
@Ignore
public class TestTask extends AbstractTask {

    private transient final TaskResult result;
    private transient boolean canceled;
    private int priority = 0;

    public TestTask(final TaskResult result) {
        super();
        this.result = result;
    }

    public TestTask(final TaskResult result, final int priority) {
        super();
        this.result = result;
        this.priority = priority;
    }

    @Override
    protected TaskResult run() {

        return isCanceled() ? new TaskResult(TaskResult.CANCEL) : getTestResult();
    }

    @Override
    protected void cancel() {
        setCanceled(true);
    }

    public boolean isCanceled() {
        return canceled;
    }

    private void setCanceled(final boolean isCanceled) {
        this.canceled = isCanceled;
    }

    public TaskResult getTestResult() {
        return result;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(final int priority) {
        this.priority = priority;
    }

}
