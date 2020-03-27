package picoded.servlet;

import picoded.core.exception.ExceptionUtils;

import java.util.HashMap;
import java.util.Map;

public class ApiException extends RuntimeException {
	
	private int httpStatus = 200;
	private String errorType = "INTERNAL_SERVER_ERROR";
	private String errorMessage = null;
	private Map<String, Object> errorMap = null;
	
	public ApiException(int httpStatus, String errorType, String errorMessage) {
		this.httpStatus = httpStatus;
		this.errorType = errorType;
		this.errorMessage = errorMessage;
	}
	
	public ApiException(int httpStatus, String errorType) {
		this.httpStatus = httpStatus;
		this.errorType = errorType;
	}
	
	public ApiException(String errorType, String errorMessage) {
		this.errorType = errorType;
		this.errorMessage = errorMessage;
	}
	
	public ApiException(String errorType) {
		this.errorType = errorType;
	}
	
	public ApiException(Exception e) {
		super(e);
	}
	
	public int getHttpStatus() {
		return httpStatus;
	}
	
	public String getErrorMessage() {
		Throwable cause = this;
		if (ExceptionUtils.getRootCause(cause) != null) {
			cause = ExceptionUtils.getRootCause(cause);
		}
		
		return (errorMessage == null) ? cause.getMessage() : errorMessage;
	}
	
	public String getStackTraceString() {
		
		Throwable cause = this;
		if (ExceptionUtils.getRootCause(cause) != null) {
			cause = ExceptionUtils.getRootCause(cause);
		}
		String stackTrace = ExceptionUtils.getStackTrace(cause);
		return stackTrace;
	}
	
	public String getErrorType() {
		return errorType;
	}
	
	public Map<String, Object> getErrorMap() {
		if(errorMap == null){
			errorMap = new HashMap<>();
			errorMap.put("code", getErrorType());
			errorMap.put("message", getErrorMessage());
			errorMap.put("stack", getStackTraceString());
		}
		return errorMap;
	}

}
