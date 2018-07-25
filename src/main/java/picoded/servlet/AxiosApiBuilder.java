package picoded.servlet;

import picoded.core.conv.ArrayConv;
import picoded.core.conv.ConvertJSON;
import picoded.servlet.annotation.OptionalVariables;
import picoded.servlet.annotation.RequestType;
import picoded.servlet.annotation.RequiredVariables;
import picoded.servlet.internal.BasePageClassMap;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

public class AxiosApiBuilder {

	private BasePage corePage;
	private Map<String, Method> scannedApiEndpoints = null;
	private String toBeReplaced="SET_ENDPOINT_MAP_HERE";
	private StringBuilder endpointCollector = new StringBuilder();

	public AxiosApiBuilder(BasePage page){
		corePage = page;
	}

	public void load(){
		generateEndpointMap();
		obtainAxiosApiTemplate();
	}

	public String endpointMapInString(){
		return endpointCollector.toString();
	}

	public Map<String,Object> generateEndpointMap() {
		scanApiEndpoints();

		Map<String, Object> endpointMaps = new HashMap<>();
		for(String key : scannedApiEndpoints.keySet()){
			//
			// Retrieve method request types
			//
			Method method = scannedApiEndpoints.get(key);
			List<String> requestTypeStrings = new ArrayList<>();
			RequestType[] requestTypes = method.getAnnotationsByType(RequestType.class);
			if (requestTypes == null || requestTypes.length == 0){
				requestTypeStrings.add("GET");
				requestTypeStrings.add("POST");
			}

			for (RequestType endpointRequestType : requestTypes){
				String[] types = endpointRequestType.value();
				requestTypeStrings.addAll(Arrays.asList(types));
			}

			//
			// Retrieve the required variables
			//
			List<String> requiredVariablesList = new ArrayList<>();
			RequiredVariables[] requiredVariables = method.getAnnotationsByType(RequiredVariables.class);
			if(requiredVariables != null && requiredVariables.length > 0){
				for(RequiredVariables requiredVariable : requiredVariables){
					String[] variables = requiredVariable.value();
					requiredVariablesList.addAll(Arrays.asList(variables));
				}
			}

			//
			// Retrieve the optional variables
			//
			List<String> optionalVariablesList = new ArrayList<>();
			OptionalVariables[] optionalVariables = method.getAnnotationsByType(OptionalVariables.class);
			if(optionalVariables != null && optionalVariables.length > 0){
				for(OptionalVariables optionalVariable : optionalVariables){
					String[] variables = optionalVariable.value();
					optionalVariablesList.addAll(Arrays.asList(variables));
				}
			}

			//
			// Form an entry in the map
			//
			Map<String, Object> singleEndpoint = new HashMap<>();
			singleEndpoint.put("methods", requestTypeStrings);
			singleEndpoint.put("required", requiredVariablesList);
			singleEndpoint.put("optional", optionalVariablesList);
			endpointMaps.put(key, singleEndpoint);

			//
			// Append to a StringBuilder
			//
			endpointCollector.append("\""+key+"\" : "+convertEndpointMapToString(singleEndpoint)+",\n");
		}

		// Remove the last ",\n"
		endpointCollector = endpointCollector.delete(endpointCollector.length()-2, endpointCollector.length());
		return endpointMaps;
	}

	public String convertEndpointMapToString(Map<String, Object> endpointMap){
		return ConvertJSON.fromObject(endpointMap);
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

	public String obtainAxiosApiTemplate() {
		StringBuilder fileContents = new StringBuilder();
		try {
			InputStream is = AxiosApiBuilder.class.getResourceAsStream("axiosApi.js");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			while ((line = br.readLine()) != null) {
				fileContents.append(line + "\n");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return (fileContents == null) ? "" : fileContents.toString();
	}
}

/*


 */