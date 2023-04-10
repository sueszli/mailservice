package dslab.transfer;

import dslab.model.ServerSpecificEmail;
import dslab.nameserver.INameserverRemote;
import dslab.util.worker.AfterExecuteHookTask;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ForwardService {
    private List<TransferSenderTask> activeTasks = new ArrayList<>();
    private ExecutorService forwardPool;
    private MonitoringService monitoringService;
    private InetAddress inetAddress;

    private INameserverRemote rootNS;

    public ForwardService(ExecutorService forwardPool, MonitoringService monitoringService, InetAddress inetAddress, INameserverRemote rootNs) {
        this.forwardPool = forwardPool;
        this.monitoringService = monitoringService;
        this.inetAddress = inetAddress;
        this.rootNS = rootNs;
    }

    public void forward(ServerSpecificEmail email) {
        TransferSenderTask forwardTask = new TransferSenderTask(email, inetAddress, monitoringService, rootNS);
        activeTasks.add(forwardTask);
        AfterExecuteHookTask wrappedTask = new AfterExecuteHookTask(forwardTask, () -> activeTasks.remove(forwardTask));
        forwardPool.execute(wrappedTask);
    }

    public void shutdown() {
        for (TransferSenderTask task : activeTasks) {
            task.closeConnection();
        }
    }
}
