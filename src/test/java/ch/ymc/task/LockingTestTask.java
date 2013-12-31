/*
 * Copyright 2013 YMC AG. All rights reserved.
 */
package ch.ymc.task;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Utility Class to synchronize Unit-Tests in the Task-Framework.
 *
 * @author nkuebler, YMC AG
 * @since 2.0.0
 */
public class LockingTestTask extends AbstractTask {

    private transient final TaskResult result;
    private boolean canceled;
    private transient final Lock testLock = new ReentrantLock();
    private transient final Condition testCondition = getTestLock().newCondition();

    public LockingTestTask(final TaskResult result) {
        super();
        this.result = result;
    }

    @Override
    protected TaskResult run() {
        TaskResult result = null;
        getTestLock().lock();
        try {
            getTestCondition().signal();
            getTestCondition().await();
            if (isCanceled()) {
                result = new TaskResult(TaskResult.CANCEL);
            } else {
                result = getTestResult();
            }
        } catch (final InterruptedException e) {
            result = new TaskResult(TaskResult.ERROR, e);
        } finally {
            getTestLock().unlock();

        }
        return result;
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

    public Condition getTestCondition() {
        return testCondition;
    }

    public Lock getTestLock() {
        return testLock;
    }

}
