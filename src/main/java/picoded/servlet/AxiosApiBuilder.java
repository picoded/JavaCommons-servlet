package picoded.servlet;

import picoded.core.conv.ConvertJSON;
import picoded.servlet.annotation.RequestPath;
import picoded.servlet.internal.BasePageClassMap;
import picoded.servlet.internal.EndpointMap;

import java.lang.reflect.Method;
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

	public void scanApiEndpoints() {
		BasePageClassMap classMap = BasePageClassMap.setupAndCache(corePage);
		Map<String, Method> apiEndpoint = classMap.apiEndpoints("");
		Map<String, Class<?>> reroutePaths = classMap.reroutePaths();

		for(String key : reroutePaths.keySet()){
			apiEndpoint.putAll(classMap.getApiEndpointsFromReroutePath(key, reroutePaths.get(key)));
		}

		System.out.println(ConvertJSON.fromObject(apiEndpoint.keySet()));
	}

	public void obtainAxiosApiTemplate() {

	}
}

/*


 */