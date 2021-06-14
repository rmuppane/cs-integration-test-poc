package com.redhat.internal.cases;

import io.cucumber.guice.ScenarioScoped;

@ScenarioScoped
public class CSSharedState {
    
    private Long processId;

	public Long getProcessId() {
		return processId;
	}

	public void setProcessId(Long processId) {
		this.processId = processId;
	}


}