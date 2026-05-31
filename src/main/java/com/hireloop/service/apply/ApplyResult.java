package com.hireloop.service.apply;

public class ApplyResult {
    private final boolean success;
    private final String jobId;
    private final String companyName;
    private final String errorMessage;
    private final String applicationId;

    private ApplyResult(boolean success, String jobId, String companyName, String errorMessage, String applicationId) {
        this.success = success;
        this.jobId = jobId;
        this.companyName = companyName;
        this.errorMessage = errorMessage;
        this.applicationId = applicationId;
    }

    public static ApplyResult success(String jobId, String companyName, String applicationId) {
        return new ApplyResult(true, jobId, companyName, null, applicationId);
    }

    public static ApplyResult failure(String jobId, String companyName, String errorMessage) {
        return new ApplyResult(false, jobId, companyName, errorMessage, null);
    }

    public boolean isSuccess() { return success; }
    public String getJobId() { return jobId; }
    public String getCompanyName() { return companyName; }
    public String getErrorMessage() { return errorMessage; }
    public String getApplicationId() { return applicationId; }

    @Override
    public String toString() {
        return success
            ? String.format("ApplyResult[SUCCESS job=%s company=%s appId=%s]", jobId, companyName, applicationId)
            : String.format("ApplyResult[FAILED job=%s company=%s error=%s]", jobId, companyName, errorMessage);
    }
}
