package dslab.util.orvell_V2;


import at.ac.tuwien.dsg.orvell.*;
import at.ac.tuwien.dsg.orvell.reflection.CommandMethod;
import dslab.exception.ProtocolException;
import dslab.exception.ValidationException;
import dslab.workerAbstract.Worker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.List;

/**
 * Almost identical to the CommandMethodAdapter.class in orvell.core with the difference, that
 * it allows a single List as the argument of a Method and catches more Exceptions.
 * ------
 * USED IN SHELL_V2
 * Takes (CommandMethod,  Worker) in constructor -> calls Command in Worker with Input and Context.
 * Implements Command with 'execute(Input, Context)' function.
 * Can call command with the passed worker on runtime.
 */
public class CommandMethodAdapter_V2 implements Command {

    private final CommandMethod method; // Method with Context-Type Parameter
    private final Worker worker; // contains Method

    public CommandMethodAdapter_V2(CommandMethod method, Worker worker) {
        this.method = method;
        this.worker = worker;
    }

    /**
     * Translates all Exceptions into
     */
    @Override
    public void execute(Input input, Context context) {
        // validate #arguments = #parameters
        this.validateArgumentCount(input);

        try {
            //get arguments
            Object[] args = this.prepareArguments(input);
            if (this.method.hasContextParameter()) {
                args[0] = context;
            }

            //execute in worker with Input
            Object result = this.method.getMethod().invoke(this.worker, args);
            if (result != null) {
                //print result in context
                context.out().println(result);
            }

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof StopShellException) {
                throw (StopShellException) e.getCause();
            } else if (e.getCause() instanceof ExitException) {
                throw (ExitException) e.getCause();
            } else if (e.getCause() instanceof ProtocolException) { // new
                throw (ProtocolException) e.getCause();
            } else if (e.getCause() instanceof ValidationException) { // new
                throw (ValidationException) e.getCause();
            } else {
                throw new CommandExecutionException(e.getCause());
            }
        }
    }

    /**
     * Changes original prepareArguments():
     * by allowing only a single argument if it's a List
     */
    private Object[] prepareArguments(Input input) {
        Parameter[] parameters = this.method.getParameters();
        Object[] arguments = new Object[parameters.length];

        //remove context argument -> will be added by execute
        int offset = this.method.hasContextParameter() ? 1 : 0;

        // if only 1 argument - List.class must be a super-class of the argument
        // return arguments as list
        if (parameters.length == 1 && List.class.isAssignableFrom(parameters[0].getType())) {
            return new List[]{input.getArguments()};
        }

        // else, turn each argument into a list
        for (int i = 0; i < input.argc(); ++i) {
            Parameter parameter = parameters[i + offset];

            Class<?> type = parameter.getType();
            String inputValue = input.getArguments().get(i);
            Object value = inputValue;
            if (Integer.TYPE.isAssignableFrom(type) || Integer.class.isAssignableFrom(type)) {
                try {
                    value = Integer.parseInt(inputValue);
                } catch (NumberFormatException e) {
                    throw new CommandExecutionException(e);
                }
            }

            arguments[i + offset] = value;
        }

        return arguments;
    }

    /**
     * Allows a single argument if it's a List
     */
    private void validateArgumentCount(Input input) {
        Parameter[] parameters = this.method.getParameters();

        // if only 1 argument - List.class must be a super-class of the argument
        if (parameters.length == 1 && List.class.isAssignableFrom(parameters[0].getType())) {
            return;
        }

        // if >1 argument
        // Context Argument not required
        // this.method's parameters == input.arguments.size()
        int expected = parameters.length - (this.method.hasContextParameter() ? 1 : 0);
        if (expected != input.argc()) {
            throw new CommandUsageException("Expected " + expected + " arguments");
        }
    }
}
