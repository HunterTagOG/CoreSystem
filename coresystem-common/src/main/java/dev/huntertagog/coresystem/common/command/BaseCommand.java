package dev.huntertagog.coresystem.common.command;

import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;

public abstract class BaseCommand {

    protected BaseCommand() {
    }

    public static Logger logger(String className) {
        return LoggerFactory.get(className);
    }
}

