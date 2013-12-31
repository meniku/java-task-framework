# Task Framework

The task framework is useful for scheduling and executing independent tasks in a controlled manner. For instance, when you want to build a web-crawler and want to ensure that there are 20 download tasks executing in paralellel at most, then the task framework is particulary useful.

## Features
* limit maximum number of parallel tasks
* priorize tasks
* cancel tasks
* wait for single tasks, a list of tasks or all tasks to finish
* retrieve events for failed, canceled or finished tasks.

## when to use the task framework
Use the task framework when you want to parallize tasks that can run independent of each other and that have no realtime requirements.

## when not to use the task framework
Don't use the task framework for an incredible number of short running tasks. If you will use it for parallizing tasks that take only fractions of milliseconds, the overhead of the management of the tasks is much higher. Consider things like Fork-Join instead. 

## Simple Example

### SampleTask.java

```
public class SampleTask extends AbstractTask {
    private String name;

    public SampleTask(String name) {
        super();
        this.name = name;
    }

    @Override
    protected TaskResult run() {
        try {
            // do something fancy
            Thread.sleep((long) (1000));
        } catch (final InterruptedException e) {
            return new TaskResult(TaskResult.ERROR, e);
        }
        return new TaskResult(TaskResult.SUCCESS);
    }

    @Override
    protected void cancel() {
        // should cancel the task, but we ignore this for this example
    }

    public String getName() {
        return name;
    }
}
```

### Main.java

```
public class Main implements TaskEventListener<SampleTask> {
    public static void main(String[] argv) throws TaskException, InterruptedException {
        new Main().run();
    }

    private long startedAt;

    public void run() throws TaskException, InterruptedException {
        QueuedTaskRunner<SampleTask> taskRunner = new QueuedTaskRunner<SampleTask>();

        taskRunner.setTaskEventListener(this);      // retrieve callbacks from the taskrunners
        taskRunner.setNumberOfConcurrentTasks(3);   // allow 3 concurrent tasks at once

        startedAt = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            taskRunner.execute(new SampleTask("Foo Task " + i));
        }

        // wait for all tasks to complete
        taskRunner.join();
        
        // dispose the taskrunner
        taskRunner.dispose();
    }

    @Override
    public void taskComplete(SampleTask task, TaskResult result) {
        long time = System.currentTimeMillis() - this.startedAt;
        if (result.getStatus() == TaskResult.SUCCESS) {
            System.out.println("Task " + task.getName() + " completed successful after " + time + "ms");
        } else {
            System.out.println("Problem completing Task " + task.getName());
        }
    }
}


```

# Final Notes

The task framework was inspired by the [Eclipse Jobs API](http://www.eclipse.org/articles/Article-Concurrency/jobs-api.html) and the [AS3 Task Framework](http://www.spicefactory.org/spicelib/docs/as3/current/manual/?page=overview&section=swc).