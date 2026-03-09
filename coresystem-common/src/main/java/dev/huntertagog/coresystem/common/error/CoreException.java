package dev.huntertagog.coresystem.common.error;

public class CoreException extends RuntimeException {

    private final CoreError error;

    public CoreException(CoreError error) {
        super(error.technicalMessage(), error.cause());
        this.error = error;
    }

    public CoreException(CoreError error, Throwable cause) {
        super(error.technicalMessage(), cause);
        this.error = error.withCause(cause);
    }

    public CoreError error() {
        return error;
    }

    @Override
    public String toString() {
        return "CoreException{" +
                "error=" + error.toLogString() +
                '}';
    }
}
