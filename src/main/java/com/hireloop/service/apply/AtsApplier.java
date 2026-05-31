package com.hireloop.service.apply;

import com.hireloop.model.Job;
import com.hireloop.model.ResumeMaster;

public interface AtsApplier {
    ApplyResult apply(Job job, ResumeMaster resume, ApplyConfig config);
    boolean supports(String atsType);
}
