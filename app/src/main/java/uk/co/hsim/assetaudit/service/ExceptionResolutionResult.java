package uk.co.hsim.assetaudit.service;

public final class ExceptionResolutionResult {
    private final String message;
    private final DepartmentAuditContext departmentContext;

    public ExceptionResolutionResult(String message, DepartmentAuditContext departmentContext) {
        this.message = message;
        this.departmentContext = departmentContext;
    }

    public String getMessage() {
        return message;
    }

    public DepartmentAuditContext getDepartmentContext() {
        return departmentContext;
    }
}
