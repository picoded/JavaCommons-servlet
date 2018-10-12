package picoded.servlet;

import picoded.core.exception.ExceptionUtils;

import java.util.HashMap;
import java.util.Map;

public class ApiException extends RuntimeException {
	private int httpStatus = 500;
	private String errorCode = "INTERNAL_SERVER_ERROR";
	private String errorMessage = null;

	public ApiException(int httpStatus, String errorCode, String errorMessage) {
		this.httpStatus = httpStatus;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	public ApiException(String errorCode, String errorMessage){
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	public ApiException(String errorCode) {
		this.errorCode = errorCode;
	}

	public ApiException(Exception e) {
		super(e);
	}

	public int getHttpStatus(){
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

	public String getErrorCode() {
		return errorCode;
	}

	public Map<String, Object> getErrorMap() {
		Map<String, Object> errorMap = new HashMap<>();
		errorMap.put("code", getErrorCode());
		errorMap.put("message", getErrorMessage());
		errorMap.put("stack", getStackTraceString());
		return errorMap;
	}
}
