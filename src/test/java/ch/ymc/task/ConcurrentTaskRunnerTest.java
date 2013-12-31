/*
 * Copyright 2013 YMC AG. All rights reserved.
 */
package ch.ymc.task;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link ch.ymc.task.ConcurrentTaskRunner}.
 *
 * @author nkuebler, YMC AG
 * @since 2.0.0
 */
public class ConcurrentTaskRunnerTest {

    @Mock
    private TaskEventListener<AbstractTask> taskEventListener;

    @Spy
    private final LockingTestTask waitingTask = new LockingTestTask(new TaskResult(TaskResult.SUCCESS));

    @Spy
    private TestTask task = new TestTask(new TaskResult(TaskResult.SUCCESS));

    private ConcurrentTaskRunner<AbstractTask> taskRunner;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        taskRunner = new ConcurrentTaskRunner<AbstractTask>();
    }

    @Test(expected = TaskException.class, timeout = 60000)
    public void startShouldThrowException() throws TaskException, InterruptedException {
        task.setState(TaskState.COMPLETE);
        taskRunner.execute(task);
    }

    @Test(expected = TaskException.class, timeout = 60000)
    public void startShouldThrowException2() throws TaskException, InterruptedException {
        task.setState(TaskState.CANCELING);
        taskRunner.execute(task);
    }

    @Test(expected = TaskException.class, timeout = 60000)
    public void startShouldThrowException3() throws TaskException, InterruptedException {
        task.setState(TaskState.RUNNING);
        taskRunner.execute(task);
    }

    @Test(expected = TaskException.class, timeout = 60000)
    public void cancelShouldThrowException() throws TaskException {
        taskRunner.cancel(task);
    }

    @Test(expected = TaskException.class, timeout = 60000)
    public void cancelShouldThrowException2() throws TaskException {
        task.setState(TaskState.RUNNING);
        taskRunner.cancel(task);
    }

    @Test(timeout = 60000)
    public void cancelShouldInvokeCancel() throws TaskException, InterruptedException {
        task = spy(new SleepingTestTask(new TaskResult(TaskResult.SUCCESS)));
        taskRunner.execute(task);
        taskRunner.cancel(task);
        verify(task, times(1)).cancel();
    }

    @Test(timeout = 60000)
    public void cancelShouldNotInvokeCancel() throws TaskException, InterruptedException {
        task = new SleepingTestTask(new TaskResult(TaskResult.SUCCESS));
        taskRunner.execute(task);
        task.setState(TaskState.CANCELING);
        taskRunner.cancel(task);
        assertFalse(task.isCanceled());
    }

    @Test(timeout = 60000)
    public void cancelShouldNotInvokeCancel2() throws TaskException, InterruptedException {
        task.setState(TaskState.COMPLETE);
        taskRunner.cancel(task);
        verify(this.task, times(0)).cancel();
    }

    @Test(timeout = 60000)
    public void startShouldSetTheThread() throws TaskException, InterruptedException {
        taskRunner.execute(waitingTask);
        assertNotNull(waitingTask.getThread());
        waitingTask.getTestLock().lock();
        try {
            waitingTask.getTestCondition().signal();
        } finally {
            waitingTask.getTestLock().unlock();
        }
    }

    @Test(timeout = 60000)
    public void startShouldAddTaskToRunningTasks() throws TaskException, InterruptedException {
        taskRunner.execute(waitingTask);
        assertEquals(1, taskRunner.getRunningTasks().size());
        assertTrue(taskRunner.getRunningTasks().contains(waitingTask));
        waitingTask.getTestLock().lock();
        try {
            waitingTask.getTestCondition().signal();
        } finally {
            waitingTask.getTestLock().unlock();
        }
    }

    @Test(timeout = 60000)
    public void startShouldSetTaskToRunningState() throws TaskException, InterruptedException {
        taskRunner.execute(waitingTask);
        assertEquals(TaskState.RUNNING, waitingTask.getState());
        waitingTask.getTestLock().lock();
        try {
            waitingTask.getTestCondition().signal();
        } finally {
            waitingTask.getTestLock().unlock();
        }
    }

    @Test(timeout = 60000)
    public void taskShouldBeSetToCompleteState() throws TaskException, InterruptedException {
        taskRunner.setTaskEventListener(taskEventListener);
        taskRunner.execute(task);
        taskRunner.join(task);
        assertEquals(TaskState.COMPLETE, task.getState());
    }

    @Test(timeout = 60000)
    public void taskShouldBeRemovedFromRunningTasks() throws TaskException, InterruptedException {
        taskRunner.setTaskEventListener(taskEventListener);
        taskRunner.execute(task);
        taskRunner.join(task);
        assertEquals(0, taskRunner.getRunningTasks().size());
        assertFalse(taskRunner.getRunningTasks().contains(task));
    }

    @Test(timeout = 60000)
    public void taskCompleteEventShouldBeFired() throws TaskException, InterruptedException {
        taskRunner.setTaskEventListener(taskEventListener);
        taskRunner.execute(task);
        taskRunner.join(task);
        verify(this.taskEventListener, times(1)).taskComplete(task, task.getTestResult());
    }

    @Test(timeout = 60000)
    public void taskCompleteEventShouldBeFiredEvenOnRuntimeException() throws TaskException, InterruptedException {
        final Throwable exception = new SomeRuntimeExceptionForPMD();
        when(this.task.run()).thenThrow(exception);
        taskRunner.setTaskEventListener(taskEventListener);
        taskRunner.execute(task);
        taskRunner.join(task);
        verify(this.taskEventListener, times(1)).taskComplete(eq(task), eq(new TaskResult(TaskResult.FATAL, exception)));
    }

    @Test(timeout = 60000)
    public void taskShouldBeCanceledWhenCanceled() throws TaskException, InterruptedException {
        taskRunner.setTaskEventListener(taskEventListener);
        try {
            waitingTask.getTestLock().lock();
            taskRunner.execute(waitingTask);
            waitingTask.getTestCondition().await();
            taskRunner.cancel(waitingTask);
            assertEquals(TaskState.CANCELING, waitingTask.getState());
            waitingTask.getTestCondition().signal();
        } finally {
            waitingTask.getTestLock().unlock();
        }
        taskRunner.join(waitingTask);
    }

    @Test(timeout = 60000)
    public void taskShouldBeCompleteWhenCancelFinish() throws TaskException, InterruptedException {
        taskRunner.setTaskEventListener(taskEventListener);
        waitingTask.getTestLock().lock();
        try {
            taskRunner.execute(waitingTask);
            waitingTask.getTestCondition().await();
        } finally {
            waitingTask.getTestLock().unlock();
        }
        waitingTask.getTestLock().lock();
        try {
            taskRunner.cancel(waitingTask);
            waitingTask.getTestCondition().signal();
        } finally {
            waitingTask.getTestLock().unlock();
        }
        taskRunner.join(waitingTask);
        assertEquals(TaskState.COMPLETE, waitingTask.getState());
        verify(this.taskEventListener, times(1)).taskComplete(waitingTask, new TaskResult(TaskResult.CANCEL));
        assertEquals(0, taskRunner.getRunningTasks().size());
        assertFalse(taskRunner.getRunningTasks().contains(waitingTask));
    }

    @Test(timeout = 60000)
    public void shouldBeAbleToConcurrentlyRun100Tasks() throws TaskException, InterruptedException {
        final LockingTestTask[] tasks = new LockingTestTask[100];
        for (int i = 0; i < tasks.length; i++) {
            tasks[i] = new LockingTestTask(new TaskResult(TaskResult.SUCCESS));
            taskRunner.execute(tasks[i]);
        }
        assertEquals(100, taskRunner.getNumberOfExecutingTasks());
        // Thread.sleep(1000);
        for (final LockingTestTask task2 : tasks) {
            task2.getTestLock().lock();
            try {
                task2.getTestCondition().signal();
            } finally {
                task2.getTestLock().unlock();
            }
            taskRunner.join(task2);
        }
        for (final LockingTestTask task2 : tasks) {
            assertEquals(TaskState.COMPLETE, task2.getState());
        }
        assertEquals(0, taskRunner.getRunningTasks().size());
    }

    @Test(timeout = 60000)
    public void joinShouldWaitForAllTasks() throws TaskException, InterruptedException {
        final AbstractTask[] tasks = new AbstractTask[100];
        for (int i = 0; i < tasks.length; i++) {
            tasks[i] = new SleepingTestTask(new TaskResult(TaskResult.SUCCESS));
            taskRunner.execute(tasks[i]);
        }
        taskRunner.join(Arrays.asList(tasks));
        assertEquals(0, taskRunner.getNumberOfExecutingTasks());
    }

    @Test(timeout = 60000)
    public void isExecutingShouldBeTrue() {
        task.setState(TaskState.RUNNING);
        assertTrue(taskRunner.isExecuting(task));
    }

    @Test(timeout = 60000)
    public void isExecutingShouldBeFalse() {
        task.setState(TaskState.COMPLETE);
        assertFalse(taskRunner.isExecuting(task));
    }

    @Test(timeout = 60000)
    public void isExecutingShouldBeFalse2() {
        task.setState(TaskState.INITIALIZED);
        assertFalse(taskRunner.isExecuting(task));
    }

    @Test(timeout = 60000)
    public void isExecutingShouldBeTrue2() {
        task.setState(TaskState.CANCELING);
        assertTrue(taskRunner.isExecuting(task));
    }

    @Test(timeout = 60000)
    public void isCompleteShouldBeTrue() {
        task.setState(TaskState.COMPLETE);
        assertTrue(taskRunner.isComplete(task));
    }

    @Test(timeout = 60000)
    public void isCompleteShouldBeFalse() {
        task.setState(TaskState.RUNNING);
        assertFalse(taskRunner.isComplete(task));
    }

    @Test(timeout = 60000)
    public void isCompleteShouldBeFalse2() {
        task.setState(TaskState.INITIALIZED);
        assertFalse(taskRunner.isComplete(task));
    }

    @Test(timeout = 60000)
    public void isCompleteShouldBeFalse3() {
        task.setState(TaskState.CANCELING);
        assertFalse(taskRunner.isComplete(task));
    }

    @Test(timeout = 60000)
    public void hasStartedShouldBeTrue() {
        task.setState(TaskState.COMPLETE);
        assertTrue(taskRunner.hasStarted(task));
    }

    @Test(timeout = 60000)
    public void hasStartedShouldBeTrue2() {
        task.setState(TaskState.RUNNING);
        assertTrue(taskRunner.hasStarted(task));
    }

    @Test(timeout = 60000)
    public void hasStartedShouldBeFalse() {
        task.setState(TaskState.INITIALIZED);
        assertFalse(taskRunner.hasStarted(task));
    }

    @Test(timeout = 60000)
    public void hasStartedShouldBeTrue3() {
        task.setState(TaskState.CANCELING);
        assertTrue(taskRunner.hasStarted(task));
    }

    private class SomeRuntimeExceptionForPMD extends RuntimeException {
        /**
         * generated serial version uid.
         */
        private static final long serialVersionUID = -7893071487292006508L;

    }
}
