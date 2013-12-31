/*
 * Copyright 2013 YMC AG. All rights reserved.
 */
package ch.ymc.task;

import example.Main;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.locks.Condition;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test-class for the {@link ch.ymc.task.QueuedTaskRunner}.
 *
 * @author nkuebler, YMC AG
 * @since 2.0.0
 */
public class QueuedTaskRunnerTest {

    @Mock
    TaskEventListener<TestTask> taskEventListener;

    @Spy
    TestTask task = new TestTask(new TaskResult(TaskResult.SUCCESS));

    @Mock
    TaskRunner<TestTask> taskRunner;

    QueuedTaskRunner<TestTask> queuedTaskRunner;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        queuedTaskRunner = new QueuedTaskRunner<TestTask>(new Comparator<TestTask>() {
            @Override
            public int compare(final TestTask paramT1, final TestTask paramT2) {
                if (paramT1.getPriority() > paramT2.getPriority()) {
                    return -1;
                } else if (paramT2.getPriority() < paramT2.getPriority()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        queuedTaskRunner.setTaskRunner(taskRunner);
        queuedTaskRunner.setTaskEventListener(taskEventListener);
    }

    @Test
    public void executeShouldBeDelegatedToInnerTaskRunner() throws TaskException, InterruptedException {
        queuedTaskRunner.execute(task);
        verify(this.taskRunner, times(1)).execute(task);
    }

    @Test
    public void executeShouldEnqueueItems() throws TaskException, InterruptedException {
        queuedTaskRunner.setNumberOfConcurrentTasks(2);
        when(this.taskRunner.getNumberOfExecutingTasks()).thenReturn(2);
        final TestTask task1 = new TestTask(new TaskResult(TaskResult.SUCCESS), 1);
        queuedTaskRunner.execute(task1);
        verify(this.taskRunner, times(0)).execute(task1);
        assertEquals(1, this.queuedTaskRunner.getNumberOfEnqueuedTasks());
        assertEquals(2, this.queuedTaskRunner.getNumberOfRunningTasks());
        assertEquals(3, this.queuedTaskRunner.getNumberOfExecutingTasks());
    }

    @Test
    public void flushShouldPollJobsInOrder() throws TaskException, InterruptedException {
        queuedTaskRunner.setNumberOfConcurrentTasks(2);
        when(this.taskRunner.getNumberOfExecutingTasks()).thenReturn(2);
        final TestTask task1 = new TestTask(new TaskResult(TaskResult.SUCCESS), 1);
        final TestTask task2 = new TestTask(new TaskResult(TaskResult.SUCCESS), 2);
        queuedTaskRunner.execute(task1);
        queuedTaskRunner.execute(task2);
        when(this.taskRunner.getNumberOfExecutingTasks()).thenReturn(1);
        queuedTaskRunner.flush();
        verify(this.taskRunner, times(1)).execute(task2);
    }

    @Test
    public void flushShouldPollJobsInOrder2() throws TaskException, InterruptedException {
        queuedTaskRunner.setNumberOfConcurrentTasks(2);
        when(this.taskRunner.getNumberOfExecutingTasks()).thenReturn(2);
        final TestTask task1 = new TestTask(new TaskResult(TaskResult.SUCCESS), 2);
        final TestTask task2 = new TestTask(new TaskResult(TaskResult.SUCCESS), 1);
        queuedTaskRunner.execute(task1);
        queuedTaskRunner.execute(task2);
        when(this.taskRunner.getNumberOfExecutingTasks()).thenReturn(1);
        queuedTaskRunner.flush();
        verify(this.taskRunner, times(1)).execute(task1);
    }

    @Test
    public void isExecutingShouldBeTrueForEnqueuedItems() throws TaskException, InterruptedException {
        queuedTaskRunner.setNumberOfConcurrentTasks(2);
        when(this.taskRunner.getNumberOfExecutingTasks()).thenReturn(2);
        final TestTask task1 = new TestTask(new TaskResult(TaskResult.SUCCESS), 1);
        queuedTaskRunner.execute(task1);
        assertTrue(this.queuedTaskRunner.isExecuting(task1));
    }

    @Test
    public void isExecutingShouldBeTrueForRunningItems() throws TaskException, InterruptedException {
        final TestTask task1 = new TestTask(new TaskResult(TaskResult.SUCCESS), 1);
        when(this.taskRunner.isExecuting(task1)).thenReturn(true);
        assertTrue(this.queuedTaskRunner.isExecuting(task1));
        verify(this.taskRunner, times(1)).isExecuting(task1);
    }

    @Test
    public void isRunningShouldBeFalseForEnqueuedItems() throws TaskException, InterruptedException {
        queuedTaskRunner.setNumberOfConcurrentTasks(2);
        when(this.taskRunner.getNumberOfExecutingTasks()).thenReturn(2);
        final TestTask task1 = new TestTask(new TaskResult(TaskResult.SUCCESS), 1);
        queuedTaskRunner.execute(task1);
        assertFalse(this.queuedTaskRunner.isRunning(task1));
    }

    @Test
    public void isRunningShouldBeTrueForRunningItems() throws TaskException, InterruptedException {
        final TestTask task1 = new TestTask(new TaskResult(TaskResult.SUCCESS), 1);
        when(this.taskRunner.isExecuting(task1)).thenReturn(true);
        assertTrue(this.queuedTaskRunner.isRunning(task1));
        verify(this.taskRunner, times(1)).isExecuting(task1);
    }

    @Test
    public void isCompleteShouldDelegateToTaskRunner() {
        this.queuedTaskRunner.isComplete(task);
        verify(this.taskRunner, times(1)).isComplete(task);
    }

    @Test
    public void hasStartedShouldDelegateToTaskRunner() {
        this.queuedTaskRunner.hasStarted(task);
        verify(this.taskRunner, times(1)).hasStarted(task);
    }

    @Test
    public void hasStartedShouldBeTrueForEnqueuedItems() throws TaskException, InterruptedException {
        queuedTaskRunner.setNumberOfConcurrentTasks(2);
        when(this.taskRunner.getNumberOfExecutingTasks()).thenReturn(2);
        final TestTask task1 = new TestTask(new TaskResult(TaskResult.SUCCESS), 1);
        queuedTaskRunner.execute(task1);
        assertTrue(this.queuedTaskRunner.hasStarted(task1));
    }

    @Test
    public void taskCompleteShouldDelegateToTaskEventHandler() throws Exception {
        final TaskResult tr = new TaskResult(TaskResult.SUCCESS);
        this.queuedTaskRunner.taskComplete(task, tr);
        verify(this.taskEventListener, times(1)).taskComplete(task, tr);
    }

    @Test
    public void taskCompleteShouldFlushTheQueue() throws Exception {
        this.queuedTaskRunner.setNumberOfConcurrentTasks(0);
        this.queuedTaskRunner.execute(task);
        final TaskResult tr = new TaskResult(TaskResult.SUCCESS);
        verify(this.taskRunner, times(0)).execute(task);
        this.queuedTaskRunner.setNumberOfConcurrentTasks(1);
        this.queuedTaskRunner.taskComplete(new TestTask(new TaskResult(TaskResult.CANCEL)), tr);
        verify(this.taskRunner, times(1)).execute(task);
    }

    @Test
    public void cancelTaskNotInQueueShouldBeDelegatedToTaskRunner() throws TaskException {
        when(this.taskRunner.isExecuting(task)).thenReturn(true);
        task.setState(TaskState.RUNNING);
        this.queuedTaskRunner.cancel(task);
        verify(this.taskRunner, times(1)).cancel(task);
    }

    @Test
    public void cancelTaskNotInQueueShouldBeDelegatedToTaskRunner2() throws TaskException {
        when(this.taskRunner.isExecuting(task)).thenReturn(true);
        task.setState(TaskState.CANCELING);
        this.queuedTaskRunner.cancel(task);
        verify(this.taskRunner, times(1)).cancel(task);
    }

    @Test
    public void cancelTaskNotInQueueShouldBeDelegatedToTaskRunner3() throws TaskException {
        when(this.taskRunner.isExecuting(task)).thenReturn(true);
        task.setState(TaskState.COMPLETE);
        this.queuedTaskRunner.cancel(task);
        verify(this.taskRunner, times(1)).cancel(task);
    }

    @Test(expected = TaskException.class)
    public void cancelTaskNeitherInQueueNorRunningShouldThrowException() throws TaskException {
        task.setState(TaskState.INITIALIZED);
        this.queuedTaskRunner.cancel(task);
        verify(this.taskRunner, times(1)).cancel(task);
    }

    @Test
    public void cancelTaskInQueueShouldSetTaskCompleteAndFireEvent() throws TaskException, InterruptedException {
        queuedTaskRunner.setNumberOfConcurrentTasks(2);
        when(this.taskRunner.getNumberOfExecutingTasks()).thenReturn(2);
        queuedTaskRunner.execute(task);
        this.queuedTaskRunner.cancel(task);
        assertEquals(task.getState(), TaskState.COMPLETE);
        verify(this.taskEventListener, times(1)).taskComplete(task, new TaskResult(TaskResult.CANCEL));
    }

    @Test
    public void joinShouldDelegateToTaskRunner() throws InterruptedException, TaskException {
        task.setState(TaskState.RUNNING);
        this.queuedTaskRunner.join(task);
        verify(this.taskRunner, times(1)).join(task);
    }

    @Test(expected = TaskException.class)
    public void joinShouldThrowException() throws InterruptedException, TaskException {
        task.setState(TaskState.INITIALIZED);
        this.queuedTaskRunner.join(task);
    }

    @Test(expected = InterruptedException.class)
    public void joinShouldWaitUntilStartedTaskStateChanges() throws InterruptedException, TaskException {
        queuedTaskRunner.setNumberOfConcurrentTasks(2);
        when(this.taskRunner.getNumberOfExecutingTasks()).thenReturn(2);
        queuedTaskRunner.execute(task);
        queuedTaskRunner.setTaskStartedCondition(Mockito.mock(Condition.class));
        doThrow(new InterruptedException()).when(queuedTaskRunner.getTaskStartedCondition()).await();
        queuedTaskRunner.join(task);
        // TODO: find better way to test (this isn't completly testing the loop
        // behaviour with additon join call on taskrunner)
    }

    @Test
    public void joinMultipleTasksShouldCallJoinForEverySingleTask() throws TaskException, InterruptedException {
        final TestTask[] tasks = new TestTask[100];
        for (int i = 0; i < tasks.length; i++) {
            tasks[i] = new SleepingTestTask(new TaskResult(TaskResult.SUCCESS));
            tasks[i].setState(TaskState.COMPLETE);
            queuedTaskRunner.execute(tasks[i]);
        }
        queuedTaskRunner.join(Arrays.asList(tasks));
        for (final TestTask task2 : tasks) {
            verify(this.taskRunner, times(1)).join(eq(task2));
        }
    }

    @Test
    public void disposeShouldCallDisposeOnContainedTaskrunner() throws TaskException, InterruptedException {
        this.queuedTaskRunner.dispose();
        verify(this.taskRunner, times(1)).dispose();
    }
}
