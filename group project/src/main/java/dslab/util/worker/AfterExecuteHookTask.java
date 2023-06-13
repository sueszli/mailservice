package dslab.util.worker;

public class AfterExecuteHookTask implements Runnable {

    private Runnable task;
    private Hook afterExecuteHook;

    public AfterExecuteHookTask(Runnable task, Hook afterExecuteHook) {
        this.task = task;
        this.afterExecuteHook = afterExecuteHook;
    }

    @Override
    public void run() {
        task.run();
        afterExecuteHook.execute();
    }
}
