package com.monitor.common;

/**
 * Created by jakub on 19.08.16.
 */
public class MonitorMessages {

    public static class SaveStatistics {}
    public static class ResultsSaved {}
    public static class Connected {}
    public static class SendRemoteCommand {
        private final String command;

        public SendRemoteCommand(String command) {
            this.command = command;
        }

        public String getCommand() {
            return command;
        }
    }

    public static class Response {
        private final String response;

        public Response(String response) {
            this.response = response;
        }

        public String getResponse() {
            return response;
        }
    }
}
