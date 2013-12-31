/*
 * Copyright 2013 YMC AG. All rights reserved.
 */
package ch.ymc.task;

/**
 * Utility Class to synchronize Unit-Tests in the Task-Framework.
 *
 * @author nkuebler, YMC AG
 * @since 2.0.0
 */
public class SleepingTestTask extends TestTask {

    public SleepingTestTask(final TaskResult result) {
        super(result);
    }

    public SleepingTestTask(final TaskResult result, final int priority) {
        super(result, priority);
    }

    @Override
    protected TaskResult run() {
        TaskResult result = null;
        try {
            Thread.sleep(150);
            if (isCanceled()) {
                result = new TaskResult(TaskResult.CANCEL);
            } else {
                result = getTestResult();
            }
        } catch (final InterruptedException e) {
            result = new TaskResult(TaskResult.ERROR, e);
        }
        return result;
    }

}
