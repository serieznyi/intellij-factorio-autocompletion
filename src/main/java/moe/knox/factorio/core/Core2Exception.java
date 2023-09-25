package moe.knox.factorio.core;

public class Core2Exception extends RuntimeException {
    public Core2Exception(String message, Throwable e) {
        super(message, e);
    }

    public Core2Exception(String message) {
        super(message);
    }
}
