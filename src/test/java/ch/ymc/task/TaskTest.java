/*
 * Copyright 2013 YMC AG. All rights reserved.
 */
package ch.ymc.task;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;

/**
 * Test Class for {@link ch.ymc.task.AbstractTask}.
 *
 * @author nkuebler, YMC AG
 * @since 2.0.0
 */
public class TaskTest {

    private final AbstractTask task = new TestTask(new TaskResult(TaskResult.SUCCESS));

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void initialStateShouldBeInitialized() {
        assertEquals(TaskState.INITIALIZED, task.getState());
    }
}
