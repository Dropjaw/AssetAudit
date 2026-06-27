package uk.co.hsim.assetaudit.domain.results;

public final class OperationResult<T> {
    private final boolean success;
    private final T value;
    private final ErrorCode errorCode;
    private final String message;

    private OperationResult(boolean success, T value, ErrorCode errorCode, String message) {
        this.success = success;
        this.value = value;
        this.errorCode = errorCode;
        this.message = message;
    }

    public static <T> OperationResult<T> ok(T value) {
        return new OperationResult<>(true, value, null, null);
    }

    public static <T> OperationResult<T> fail(ErrorCode code, String message) {
        return new OperationResult<>(false, null, code, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getValue() {
        return value;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }
}
