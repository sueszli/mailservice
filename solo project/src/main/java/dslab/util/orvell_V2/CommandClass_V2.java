package dslab.util.orvell_V2;

import at.ac.tuwien.dsg.orvell.annotation.Command;
import at.ac.tuwien.dsg.orvell.reflection.CommandClass;
import at.ac.tuwien.dsg.orvell.reflection.CommandDeclarationException;
import at.ac.tuwien.dsg.orvell.reflection.CommandMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Almost identical to the CommandClass.class in orvell.core with the difference, that the
 * parents @Command methods get added to the commandMethods List aswell.
 * ------
 * USED IN SHELL_V2
 * Creates List<CommandMethod> from class where they are annotated with @Command
 * CommandMethods must have one Parameter with the Type 'Context'
 */
public class CommandClass_V2 extends CommandClass {

    private final List<CommandMethod> commandMethods = new ArrayList<>();

    public CommandClass_V2(Class<?> clazz) throws CommandDeclarationException {
        super(clazz);
        this.getCommandMethods(clazz);
    }

    @Override
    public List<CommandMethod> getCommandMethods() {
        return this.commandMethods;
    }

    /**
     * Changes original initMethods():
     * by also recursively adding all methods from the classes parents to the commandMethods.
     */
    private void getCommandMethods(Class<?> clazz) {
        if (clazz == null || clazz == Objects.class) { // required if it has no parents
            return;
        }

        Method[] methods = clazz.getDeclaredMethods();

        Arrays.stream(methods).unordered().forEach(
                m -> {
                    // if Method has @Command
                    if (m.isAnnotationPresent(Command.class)) {
                        // check if Method is already in list
                        boolean novel = commandMethods.stream().unordered().noneMatch(c -> c.getMethod().getName().equals(m.getName()));
                        if (novel) {
                            commandMethods.add(new CommandMethod(m));
                        }
                    }
                }
        );

        this.getCommandMethods(clazz.getSuperclass()); // runs recursively for each superclass
    }
}
