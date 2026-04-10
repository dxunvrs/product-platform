package network;

import com.fasterxml.jackson.annotation.JsonInclude;
import commands.CommandDef;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response {
    private String message;
    private ResponseType type;

    private String token;

    private Map<String, CommandDef> syncData;

    private String host;
    private int port;

    public Response() {}

    private Response(Builder builder) {
        this.message = builder.message;
        this.type = builder.type;
        this.token = builder.token;
        this.syncData = builder.syncData;
        this.host = builder.host;
        this.port = builder.port;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, CommandDef> getSyncData() {
        return syncData;
    }

    public ResponseType getType() {
        return type;
    }

    public String getToken() {
        return token;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public static class Builder {
        private String message;
        private ResponseType type;

        private String token;

        private Map<String, CommandDef> syncData;

        private String host;
        private int port;

        public Builder() {}

        public Builder(Response response) {
            this.message = response.message;
            this.type = response.type;
            this.token = response.token;
            this.syncData = response.syncData;
            this.host = response.host;
            this.port = response.port;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder type(ResponseType type) {
            this.type = type;
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder syncData(Map<String, CommandDef> syncData) {
            this.syncData = syncData;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Response build() {
            return new Response(this);
        }
    }
}
