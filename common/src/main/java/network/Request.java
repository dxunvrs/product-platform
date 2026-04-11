package network;

import com.fasterxml.jackson.annotation.JsonInclude;
import models.Product;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Request {
    private RequestType type;

    private String username;
    private String password;

    private String commandName;

    private String token;
    private int userId;

    private List<String> stringArgs;
    private List<Integer> intArgs;
    private List<Product> objectArgs;

    private String host;
    private int port;

    public Request() {}

    private Request(Builder builder) {
        this.type = builder.type;
        this.username = builder.username;
        this.password = builder.password;
        this.commandName = builder.commandName;
        this.token = builder.token;
        this.userId = builder.userId;
        this.stringArgs = builder.stringArgs;
        this.intArgs = builder.intArgs;
        this.objectArgs = builder.objectArgs;
        this.host = builder.host;
        this.port = builder.port;
    }

    public String getToken() {
        return token;
    }

    public int getUserId() { return userId; }

    public String getCommandName() {
        return commandName;
    }

    public RequestType getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public List<String> getStringArgs() {
        return stringArgs;
    }

    public List<Integer> getIntArgs() {
        return intArgs;
    }

    public List<Product> getObjectArgs() {
        return objectArgs;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public static class Builder {
        private RequestType type;

        private String username;
        private String password;

        private String commandName;

        private String token;
        private int userId;

        private List<String> stringArgs;
        private List<Integer> intArgs;
        private List<Product> objectArgs;

        private String host;
        private int port;

        public Builder() {}

        public Builder(Request request) {
            this.type = request.type;
            this.username = request.username;
            this.password = request.password;
            this.commandName = request.commandName;
            this.token = request.token;
            this.userId = request.userId;
            this.stringArgs = request.stringArgs;
            this.intArgs = request.intArgs;
            this.objectArgs = request.objectArgs;
            this.host = request.host;
            this.port = request.port;
        }

        public Builder type(RequestType type) {
            this.type = type;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder commandName(String commandName) {
            this.commandName = commandName;
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder userId(int userId) {
            this.userId = userId;
            return this;
        }

        public Builder stringArgs(List<String> stringArgs) {
            this.stringArgs = stringArgs;
            return this;
        }

        public Builder intArgs(List<Integer> intArgs) {
            this.intArgs = intArgs;
            return this;
        }

        public Builder objectArgs(List<Product> objectArgs) {
            this.objectArgs = objectArgs;
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

        public Request build() {
            return new Request(this);
        }
    }
}
