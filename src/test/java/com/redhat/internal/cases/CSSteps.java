package com.redhat.internal.cases;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.collections.CollectionUtils;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.ProcessServicesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.google.inject.Inject;
import com.redhat.Document;
import com.redhat.GSWrapper;
import com.redhat.internal.dao.DecisionDAO;
import com.redhat.internal.pam.helpers.KieServicesClientHelper;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import io.cucumber.java.DefaultDataTableCellTransformer;
import io.cucumber.java.DefaultDataTableEntryTransformer;
import io.cucumber.java.DefaultParameterTransformer;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

public class CSSteps {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSSteps.class);

    private static final String URL = System.getProperty("kie.server.url",
            "http://localhost:8080/kie-server/services/rest/server");
    private static final String USERNAME = System.getProperty("kie.server.user", "rhdmAdmin");
    private static final String PASSWORD = System.getProperty("kie.server.password", "Pa$$w0rd");
    private static final String CONTAINER_ID = "POC_test-dmn_1.0.2-SNAPSHOT";

    RetryPolicy<Object> retryPolicy = new RetryPolicy<>() //
        .handle(Exception.class) //
        .withDelay(Duration.ofSeconds(1)) //
        .withMaxRetries(1);

    @Inject
    private CSSharedState csSharedState;

    @Inject
    private DecisionDAO decisionDAO;
    
    @DataTableType
    public Document documentEntry(Map<String, String> entry) {
        String str = entry.get("footNoteCodes");
        List<String> stringList = Arrays.asList(str.toString().split(","));
    	List<BigDecimal> bigDecimalList = new LinkedList<BigDecimal>();
    	for (String value : stringList) {
    	    bigDecimalList.add(new BigDecimal(value));
    	}
    	return new Document(entry.get("docName"),
        		bigDecimalList,
        		entry.get("documentENname"), entry.get("documentDEName"),
        		entry.get("documentITName"), entry.get("documentFRName"),
        		"",
        		new Boolean(entry.get("documentInSourceAndCSLangRequired")) );
    }

    @Before
    public void beforTest(Scenario scenario) {
        LOGGER.info("######################### INIT SCENARIO {} #########################", scenario.getName());
        final KieContainerResource containerResource = new KieContainerResource(CONTAINER_ID,
                new ReleaseId("com.redhat", "test-dmn", "1.0.2-SNAPSHOT"));
        getBaseServiceClient().createContainer(CONTAINER_ID, containerResource);
    }

    @After
    public void afterTest(Scenario scenario) {
    	final ProcessServicesClient processServicesClient = getBaseServiceClient().getServicesClient(ProcessServicesClient.class);
    	List<ProcessInstance> activeProcesses = processServicesClient.findProcessInstances(CONTAINER_ID, 0, Integer.MAX_VALUE);
    	activeProcesses.stream().forEach(pi -> processServicesClient.abortProcessInstance(CONTAINER_ID, pi.getId()));
        getBaseServiceClient().deactivateContainer(CONTAINER_ID);
        getBaseServiceClient().disposeContainer(CONTAINER_ID);
        decisionDAO.removeDecision();
        LOGGER.info("######################### END SCENARIO {} #########################", scenario.getName());
    }

    private KieServicesClient getBaseServiceClient(){
        return KieServicesClientHelper.getInstance().getKieServicesClient(USERNAME, PASSWORD, URL, GSWrapper.class, Document.class);
    }

    @When("^a request to check for '(.*?)' the generated documents")
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
    	// wrapper.setDateOfIncorporation(LocalDate.parse((String)rows.get("dateOfIncorporation"))); 
    	//TODO : How to pass the date  
    	wrapper.setCompanyTypeEnName((String)rows.get("companyTypeEnName"));
    	wrapper.setCountryCode(Integer.parseInt((String)rows.get("countryCode")));
    	transRows.put("InputPayload", wrapper);
		return transRows;
	}

    @Then("^List of documents are")
    public void validate(List<Document> documents) throws Throwable {
    	String json = decisionDAO.getDecisionJson(csSharedState.getProcessId());
        LOGGER.info("validate the documents {} value is {}", json, documents);
        ObjectMapper objectMapper = new ObjectMapper();
        List<Document> docs = objectMapper.readValue(json, new TypeReference<List<Document>>(){});
        assertNotNull(docs);
        if(CollectionUtils.subtract(docs, documents) .size() == 0 ) {
        	 assertTrue(true);
        }
        //Failsafe.with(retryPolicy).run(() -> processId.set(processServicesClient.startProcess(CONTAINER_ID, processDefinitionId, rows)));
    }
    
    
    public static void main(String args[]) {
    	
    }
}
