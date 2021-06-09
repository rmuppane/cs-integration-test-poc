package com.redhat.internal.cases;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.ProcessServicesClient;
import org.kie.server.client.QueryServicesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.redhat.Document;
import com.redhat.GSWrapper;
import com.redhat.internal.pam.helpers.KieServicesClientHelper;

import cucumber.api.DataTable;
import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

public class CSSteps {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSSteps.class);

    private static final String URL = System.getProperty("kie.server.url",
            "http://localhost:8080/kie-server/services/rest/server");
    private static final String USERNAME = System.getProperty("kie.server.user", "rhdmAdmin");
    private static final String PASSWORD = System.getProperty("kie.server.password", "Pa$$w0rd");
    private static final String CONTAINER_ID = "POC_test-dmn_1.0.0-SNAPSHOT";

    RetryPolicy<Object> retryPolicy = new RetryPolicy<>() //
        .handle(Exception.class) //
        .withDelay(Duration.ofSeconds(1)) //
        .withMaxRetries(1);

    @Inject
    private CSSharedState csSharedState;

    @Before
    public void beforTest(Scenario scenario) {
        LOGGER.info("######################### INIT SCENARIO {} #########################", scenario.getName());
        final KieContainerResource containerResource = new KieContainerResource(CONTAINER_ID,
                new ReleaseId("com.redhat", "test-dmn", "1.0.0-SNAPSHOT"));
        getBaseServiceClient().createContainer(CONTAINER_ID, containerResource);
    }

    @After
    public void afterTest(Scenario scenario) {
    	final ProcessServicesClient processServicesClient = getBaseServiceClient().getServicesClient(ProcessServicesClient.class);
    	List<ProcessInstance> activeProcesses = processServicesClient.findProcessInstances(CONTAINER_ID, 0, Integer.MAX_VALUE);
    	activeProcesses.stream().forEach(pi -> processServicesClient.abortProcessInstance(CONTAINER_ID, pi.getId()));
        getBaseServiceClient().deactivateContainer(CONTAINER_ID);
        getBaseServiceClient().disposeContainer(CONTAINER_ID);
        LOGGER.info("######################### END SCENARIO {} #########################", scenario.getName());
    }

    private KieServicesClient getBaseServiceClient(){
        return KieServicesClientHelper.getInstance().getKieServicesClient(USERNAME, PASSWORD, URL, GSWrapper.class, Document.class);
    }

    @Given("^a request to check for '(.*?)' when the customer financial status is")
    public void startProcessInstance(String processDefinitionId, DataTable table) throws Throwable {
        LOGGER.info("a process instance for definition id '{}' is started$", processDefinitionId);
        final ProcessServicesClient processServicesClient = getBaseServiceClient().getServicesClient(ProcessServicesClient.class);
        final AtomicReference<Long> processId = new AtomicReference<>();
        final Map<String, Object> rows1 = table.asMap(String.class, Object.class);
        final Map<String, Object> rows = translate(rows1);
        Failsafe.with(retryPolicy).run(() -> processId.set(processServicesClient.startProcess(CONTAINER_ID, processDefinitionId, rows)));
        csSharedState.setProcessId(processId.get());
    }
    
    private Map<String, Object> translate(Map<String, Object> rows) {
    	Map<String, Object> transRows = new HashMap<String, Object>();
    	GSWrapper wrapper = new GSWrapper();
    	wrapper.setState((String)rows.get("state"));
    	wrapper.setZone((String)rows.get("zone"));
    	wrapper.setDateOfIncorporationMonths(Integer.parseInt((String)rows.get("dateOfIncorporationMonths")));
    	wrapper.setDateOfIncorporation(LocalDate.parse((String)rows.get("dateOfIncorporation"))); 
    	//TODO : How to pass the date  
    	wrapper.setCompanyTypeEnName((String)rows.get("companyTypeEnName"));
    	wrapper.setCountryCode(Integer.parseInt((String)rows.get("countryCode")));
    	transRows.put("InputPayload", wrapper);
		return transRows;
	}

    @And("^customer Loan might be '(.*?)'")
    public void validate(String varValue, DataTable table) throws Throwable {
		String varName = "pPojoOutput";
        LOGGER.info("validate process variable {} value is {}", varName, varValue);
        final QueryServicesClient queryServicesClient = getBaseServiceClient().getServicesClient(QueryServicesClient.class);
        ProcessInstance pi = queryServicesClient.findProcessInstanceById(csSharedState.getProcessId(), true);
        // ObjectMapper.
        Map<String, Object> values = pi.getVariables();
        String documents = (String)values.get(varName);
        //TODO : How to get the actual objects 
        assertEquals(documents, "[{docName=UK-Doc-1a, footNoteCodes=[3, 5]}, {docName=UK-Doc-2a, footNoteCodes=[3, 7]}]");
        
        // pPojoOutput=[{docName=UK-Doc-1a, footNoteCodes=[3, 5]}, {docName=UK-Doc-2a, footNoteCodes=[3, 7]}]
        
        //Failsafe.with(retryPolicy).run(() -> processId.set(processServicesClient.startProcess(CONTAINER_ID, processDefinitionId, rows)));
        //csSharedState.setProcessId(processId.get());
    }
    
    
    public static void main(String args[]) {
    	
    }
}
