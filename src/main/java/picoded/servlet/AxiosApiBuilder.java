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

	public AxiosApiBuilder(BasePage page){
		corePage = page;
	}

	public void generateAxiosJS() {

	}

	public void generateAxiosFunction() {

	}

	public Map<String, Method> scanApiEndpoints() {
		BasePageClassMap classMap = BasePageClassMap.setupAndCache(corePage);
		Map<String, Method> apiEndpoint = new HashMap<>();
		classMap.getApiEndpointsFromClass("", corePage.getClass(), apiEndpoint);

		return apiEndpoint;
	}

	public void obtainAxiosApiTemplate() {

	}
}

/*


 */