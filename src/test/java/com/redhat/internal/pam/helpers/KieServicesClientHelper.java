package com.redhat.internal.pam.helpers;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;

public final class KieServicesClientHelper {

	private KieServicesClientHelper() {
	}

	public static final KieServicesClientHelper getInstance() {
		return new KieServicesClientHelper();
	}
	
	public KieServicesClient getKieServicesClient(String username, String password, String url, Class<?>... remoteClasses) {
		KieServicesConfiguration config = KieServicesFactory.newRestConfiguration(url, username, password);
		config.addExtraClasses(Arrays.asList(remoteClasses).stream().collect(Collectors.toSet()));
		config.setMarshallingFormat(MarshallingFormat.JSON);
		config.setTimeout(100000l);
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("X-KIE-ContentType", "JSON");
		config.setHeaders(headers);
		return KieServicesFactory.newKieServicesClient(config);
	}

}
