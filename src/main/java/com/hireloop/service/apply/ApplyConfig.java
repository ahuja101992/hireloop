package com.hireloop.service.apply;

public class ApplyConfig {
    private final String userName;
    private final String userEmail;
    private final String userPhone;
    private final String resumePath;
    private final boolean headless;

    public ApplyConfig(String userName, String userEmail, String userPhone, String resumePath, boolean headless) {
        this.userName = userName;
        this.userEmail = userEmail;
        this.userPhone = userPhone;
        this.resumePath = resumePath;
        this.headless = headless;
    }

    public String getUserName() { return userName; }
    public String getUserEmail() { return userEmail; }
    public String getUserPhone() { return userPhone; }
    public String getResumePath() { return resumePath; }
    public boolean isHeadless() { return headless; }

    public String getFirstName() {
        if (userName == null || userName.isBlank()) return "";
        String[] parts = userName.trim().split("\\s+");
        return parts[0];
    }

    public String getLastName() {
        if (userName == null || userName.isBlank()) return "";
        String[] parts = userName.trim().split("\\s+");
        return parts.length > 1 ? parts[parts.length - 1] : "";
    }
}
