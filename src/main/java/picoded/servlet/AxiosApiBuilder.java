package picoded.servlet;

import picoded.core.conv.ConvertJSON;
import picoded.servlet.annotation.RequestPath;
import picoded.servlet.internal.BasePageClassMap;
import picoded.servlet.internal.EndpointMap;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AxiosApiBuilder {

	private BasePage corePage;
	private Map<String, Method> scannedApiEndpoints = null;

	public AxiosApiBuilder(BasePage page){
		corePage = page;
	}

	public void generateAxiosJS() {

	}

	public void generateAxiosFunction() {

	}

	public Map<String, Method> scanApiEndpoints() {
		if(scannedApiEndpoints != null) {
			return scannedApiEndpoints;
		}

		BasePageClassMap classMap = BasePageClassMap.setupAndCache(corePage);
		scannedApiEndpoints = new HashMap<>();
		classMap.getApiEndpointsFromClass("", corePage.getClass(), scannedApiEndpoints);

		return scannedApiEndpoints;
	}

	public void obtainAxiosApiTemplate() {

	}
}

/*


 */