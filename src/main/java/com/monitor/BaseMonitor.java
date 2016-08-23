package com.monitor;

import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import com.monitor.connection.ConnectionException;
import com.monitor.connection.RemoteCommandNotSendException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

import static akka.actor.SupervisorStrategy.*;

/**
 * @author jakub on 23.08.16.
 */
public abstract class BaseMonitor {
    private static final Logger LOGGER = LogManager.getLogger(BaseMonitor.class);
    protected static SupervisorStrategy strategy = new OneForOneStrategy(20, Duration.create(3, TimeUnit.MINUTES), t -> {
        if(t instanceof ConnectionException) {
            return stop();
        } else if(t instanceof RemoteCommandNotSendException){
            LOGGER.debug("Exception {}, restarting...",t.getMessage());
            return restart();
        }  else {
            return escalate();
        }
    });
}
