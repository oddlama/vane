package org.oddlama.vane.core.command.params;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.Executor;
import org.oddlama.vane.core.command.Param;
import org.oddlama.vane.core.command.check.CheckResult;
import org.oddlama.vane.core.command.check.ErrorCheckResult;
import org.oddlama.vane.core.command.check.ExecutorCheckResult;
import org.oddlama.vane.core.functional.ErasedFunctor;
import org.oddlama.vane.core.functional.Function1;
import org.oddlama.vane.core.functional.GenericsFinder;

public class SentinelExecutorParam<T> extends BaseParam implements Executor {

    private T function;
    private Function1<CommandSender, Boolean> check_requirements;
    private Function1<Integer, Boolean> skip_argument_check;

    public SentinelExecutorParam(Command<?> command, T function) {
        this(command, function, x -> true);
    }

    public SentinelExecutorParam(Command<?> command, T function, Function1<CommandSender, Boolean> check_requirements) {
        this(command, function, check_requirements, i -> false);
    }

    public SentinelExecutorParam(
        Command<?> command,
        T function,
        Function1<CommandSender, Boolean> check_requirements,
        Function1<Integer, Boolean> skip_argument_check
    ) {
        super(command);
        this.function = function;
        this.check_requirements = check_requirements;
        this.skip_argument_check = skip_argument_check;
    }

    private boolean check_signature(final Method method, final List<Object> args) {
        // Assert the same number of given and expected parameters
        if (args.size() != method.getParameters().length) {
            throw new RuntimeException(
                "Invalid command functor " +
                method.getDeclaringClass().getName() +
                "::" +
                method.getName() +
                "!" +
                "\nFunctor takes " +
                method.getParameters().length +
                " parameters, but " +
                args.size() +
                " were given." +
                "\nRequired: " +
                Arrays.stream(method.getParameters()).map(p -> p.getType().getName()).toList() +
                "\nGiven: " +
                args.stream().map(p -> p.getClass().getName()).toList()
            );
        }

        // Assert assignable types
        for (int i = 0; i < args.size(); ++i) {
            if (skip_argument_check.apply(i)) {
                continue;
            }
            var needs = method.getParameters()[i].getType();
            var got = args.get(i).getClass();
            if (!needs.isAssignableFrom(got)) {
                throw new RuntimeException(
                    "Invalid command functor " +
                    method.getDeclaringClass().getName() +
                    "::" +
                    method.getName() +
                    "!" +
                    "\nArgument " +
                    (i + 1) +
                    " (" +
                    needs.getName() +
                    ") is not assignable from " +
                    got.getName()
                );
            }
        }
        return true;
    }

    @Override
    public boolean is_executor() {
        return true;
    }

    @Override
    public boolean execute(Command<?> command, CommandSender sender, List<Object> parsed_args) {
        // Replace command name argument (unused) with sender
        parsed_args.set(0, sender);

        // Disable logger while reflecting on the lambda.
        // FIXME? This is an ugly workaround to prevent Spigot from displaying
        // a warning, that we load a class from a plugin we do not depend on,
        // but this is absolutely intended, and erroneous behavior in any way.
        var log = command.get_module().core.log;
        var saved_filter = log.getFilter();
        log.setFilter(record -> false);
        // Get method reflection
        var gf = (GenericsFinder) function;
        var method = gf.method();
        log.setFilter(saved_filter);

        // Check method signature against given argument types
        check_signature(method, parsed_args);

        // Check external requirements on the sender
        if (!check_requirements.apply(sender)) {
            return false;
        }

        // Execute functor
        try {
            var result = ((ErasedFunctor) function).invoke(parsed_args);
            // Map null to "true" for consuming functions
            return result == null || (boolean) result;
        } catch (Exception e) {
            throw new RuntimeException(
                "Error while invoking functor " + method.getDeclaringClass().getName() + "::" + method.getName() + "!",
                e
            );
        }
    }

    @Override
    public void add_param(Param param) {
        throw new RuntimeException("Cannot add element to sentinel executor! This is a bug.");
    }

    @Override
    public CheckResult check_parse(CommandSender sender, String[] args, int offset) {
        return null;
    }

    @Override
    public List<String> completions_for(CommandSender sender, String[] args, int offset) {
        return Collections.emptyList();
    }

    @Override
    public CheckResult check_accept(CommandSender sender, String[] args, int offset) {
        if (args.length > offset) {
            // Excess arguments are an error of the previous level, so we subtract one from the
            // offset (depth)
            // This will cause invalid arguments to be prioritized on optional arguments.
            // For example /vane reload [module], with an invalid module name should show "invalid
            // module" over
            // excess arguments.
            return new ErrorCheckResult(
                offset - 1,
                "§6excess arguments: {" +
                Arrays.stream(args, offset, args.length).map(s -> "§4" + s + "§6").collect(Collectors.joining(", ")) +
                "}§r"
            );
        } else if (args.length < offset) {
            throw new RuntimeException("Sentinel executor received missing arguments! This is a bug.");
        }
        return new ExecutorCheckResult(offset, this);
    }
}
