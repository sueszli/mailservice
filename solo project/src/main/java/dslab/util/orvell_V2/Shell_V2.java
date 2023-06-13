package dslab.util.orvell_V2;

import at.ac.tuwien.dsg.orvell.Command;
import at.ac.tuwien.dsg.orvell.CommandUsageException;
import at.ac.tuwien.dsg.orvell.Context;
import at.ac.tuwien.dsg.orvell.Input;
import at.ac.tuwien.dsg.orvell.io.InputParser;
import at.ac.tuwien.dsg.orvell.reflection.CommandClass;
import at.ac.tuwien.dsg.orvell.reflection.CommandMethod;
import dslab.exception.ProtocolException;
import dslab.exception.ValidationException;
import dslab.workerAbstract.Worker;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Very similar to the Shell.class from Orvell.core.
 * Catches Exceptions and prints them.
 * ------
 * USED IN WORKER
 * takes (Worker worker, PrintStream input) in constructor.
 * callWorker(String req) - reads requests: 'command arg1 arg2 ... argn'.
 */
public class Shell_V2 {

    // converts string -> Input
    private final InputParser parser = new InputParser();

    // <command-name, Command (= commandMethod, Worker that executes it)>
    private final Map<String, Command> commands;

    private final Context context;
    private final Worker worker;

    public Shell_V2(Worker worker, PrintStream in) {
        this.worker = worker;

        this.commands = new HashMap<>();
        context = new Context(in, System.err, this.commands);

        // gets List<commandsMethod> from the workers class
        CommandClass commandClass = new CommandClass_V2(worker.getClass()); // <-----------------------------
        List<CommandMethod> commandMethods = commandClass.getCommandMethods();

        commandMethods.stream().unordered().forEach(
                // converts (commandMethod, Worker) -> Command
                // stores <command name, Command> in Map
                m -> this.commands.put(m.getName(), new CommandMethodAdapter_V2(m, worker)) // <-----------------------------
        );
    }

    public void callWorker(String req) {
        // Parse to Input: '<command-name> <arguments>' -> Input
        Input input;
        try {
            input = parser.parse(req);
        } catch (Exception e) {
            context.out().println("protocol or server error");
            worker.quitOnError();
            return;
        }

        Command command = this.commands.get(input.getCommand());

        if (command == null) {
            context.out().println("unknown command");
            worker.quitOnError();
            return;
        }

        try {
            command.execute(input, context);

        } catch (ValidationException e) { // new

            // invalid arguments
            context.out().println("error " + e.getMessage());

        } catch (CommandUsageException | ProtocolException e) { // new

            // invalid commands - quit connection
            context.out().println("error " + e.getMessage());
            worker.quitOnError();

        }
    }
}
