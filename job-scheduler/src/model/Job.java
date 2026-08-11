package model;

/**
 * A unit of work the scheduler can run. This is the Command pattern: a Job
 * knows only how to execute() itself, not when or how often -- that's the
 * Trigger's job (see strategy.Trigger).
 */
@FunctionalInterface
public interface Job {
    void execute() throws Exception;
}
