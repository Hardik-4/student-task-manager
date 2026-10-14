package ie.griffith.tasks.dao;

/** Wraps SQLException so the rest of the code doesn't need try/catch everywhere. */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
