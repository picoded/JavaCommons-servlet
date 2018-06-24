package picoded.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
// import org.apache.commons.io.IOUtils;

import picoded.core.struct.GenericConvertArrayList;
import picoded.core.struct.GenericConvertHashMap;
import picoded.core.conv.ConvertJSON;

/**
 * Represents a servlet page request parameters
 * 
 * If a request were to include multiple parameters of the same name, 
 * it would be converted into a List<Object> 
 *
 * @TODO: Optimize the class to do the conversion between String[] to String only ON DEMAND, and to cache the result
 **/
public class ServletRequestMap extends GenericConvertHashMap<String,Object> {
	
	//------------------------------------------------------------------------------
	//
	// Constructor
	//
	//------------------------------------------------------------------------------
	
	/**
	 * blank constructor
	 **/
	public ServletRequestMap() {
		super(new HashMap<String, Object>());
	}
	
	/**
	 * Takes in a httpServletRequest, and process it for its respective parameters
	 **/
	public ServletRequestMap(HttpServletRequest req) {
		super();
		processHttpServletRequest(req);
	}

	//------------------------------------------------------------------------------
	// 
	// Parameter handling (the whole point of this class)
	//
	//------------------------------------------------------------------------------
	
	/**
	 * Processes a HttpServletRequest, and extract its various possible parameters
	 * 
	 * @param  req servlet parameter
	 */
	private void processHttpServletRequest(HttpServletRequest req) {
			// This covers GET request,
			// and/or form POST request
			super.putAll(formParameterConversion(req.getParameterMap()));

			// Get the content type
			String contentType = req.getContentType();

			// No further processing if content type is null
			if( contentType == null ) {
				throw new IllegalArgumentException("Missing HTTP request contentType");
			}

			// Does specific processing for application/json
			if (contentType.contains("application/json")) {
				// Does processing of JSON request, and return
				processJsonParams(req);
				return;
			} 

			// Multipart processing, this covers file uploads
			// Used in the other post types
			multipartProcessing(req);
	}
	
	//-------------------------------------------------
	// Repeated argument handling
	//-------------------------------------------------

	/**
	 * Used internally to convert single name arguments,
	 * into repeated name arguments within a List
	 */
	private class ServletRequestList extends GenericConvertArrayList<Object> {
		public ServletRequestList() { super(); }
		public ServletRequestList(Collection<? extends Object> in) { super(in); }
	}

	//-------------------------------------------------
	// GET / POST form parameter handling
	//-------------------------------------------------

	/**
	 * Does the conversion of a Map<String,String[]> to a Map<String,Object>, for GET/POST form request arguments.
	 * If multiple string valeus are found in the String[], it will be converted into a list
	 * (facilitated by `flattenStringArray`)
	 * 
	 * @param  in  raw mapping with string array
	 * 
	 * @return  processed string object map
	 **/
	private static Map<String, Object> formParameterConversion(Map<String, String[]> in) {
		HashMap<String, Object> ret = new HashMap<String, Object>();
		for (Map.Entry<String, String[]> entry : in.entrySet()) {
			ret.put(entry.getKey(), flattenStringArray(entry.getValue()));
		}
		return ret;
	}
	
	/**
	 * Does the conversion from string array to string,
	 * Used internally for all the map conversion.
	 * 
	 * Null or empty string arrays are converted to null
	 * String array of size 1, is converted to a single string value
	 * String arrays larger then size 1, is converted to json array strings
	 * 
	 * ```
	 * flattenStringArray(new String[] {});  // returns null
	 * flattenStringArray(new String[] { "hello" } );  // returns hello
	 * flattenStringArray(new String[] { "a", "b"} );  // returns ArrayList of "a","b"
	 * ```
	 * 
	 * @param  in String[] to convert as a single string value
	 * 
	 * @return single string value, or an array list
	 **/
	private static Object flattenStringArray(String[] in) {
		if (in == null || in.length == 0) {
			return null;
		}
		
		if (in.length == 1) {
			return in[0];
		}

		return new ServletRequestList(Arrays.asList(in));
	}
	
	//-------------------------------------------------
	// binary data handling (pre-requisite for JSON)
	//-------------------------------------------------

	// Request body's content
	private byte[] reqBodyByteArray = null;
	
	/**
	 * @param input of the request
	 * @throws IOException
	 */
	private void setInputStreamToByteArray(InputStream input) throws IOException {
		// Save the request body as a byte array
		this.reqBodyByteArray = IOUtils.toByteArray(input);
	}
	
	/**
	 * @return request body in byte array
	 */
	protected byte[] getRequestBodyByteArray() {
		return reqBodyByteArray;
	}
	
	//-------------------------------------------------
	// JSON request handling
	//-------------------------------------------------

	/**
	 * Extract the JSON data from the input stream and converts it into parameters
	 * @param  req servlet parameter
	 */
	private void processJsonParams(HttpServletRequest req) {
		try {
			// get the request body / input stream
			setInputStreamToByteArray(req.getInputStream());
			
			// get the JSON string from the body
			String requestJSON = new String(getRequestBodyByteArray());
			
			// Convert into jsonMap : currently we only support top level maps
			Map<String, Object> jsonMap = ConvertJSON.toMap(requestJSON);
			
			// Store the data, and return
			this.putAll(jsonMap);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	//-------------------------------------------------
	// Multipart upload settings
	//-------------------------------------------------

	// Upload size treshold, in which the file system decides
	// to store the current upload request into memory (if below threshold)
	// or into a temporary file (if above threshold)
	private static final int MEMORY_THRESHOLD = 1024 * 1024 * 4; // 4MB
	
	// // The following is ignored, as MAX request size should be configured by server not application
	// private static final int MAX_FILE_SIZE = 1024 * 1024 * 40; // 40MB
	// private static final int MAX_REQUEST_SIZE = 1024 * 1024 * 50; // 50MB
	
	//-------------------------------------------------
	// Multipart processing
	//-------------------------------------------------

	/**
	 * Given a fieldname, append the value to param map
	 * If an existing value is found, the value is appended into a List instead
	 * 
	 * @param  name to append using
	 * @param  value of object to append
	 */
	private void appendRequestParameter(String name, Object value) {
		// Get the existing value
		Object existing = super.get(name);

		// Check if no existing value stored
		if(existing == null) {
			// Store it and return
			super.put(name, fieldvalue);
			return;
		}

		// There is an existing value, time to map into a list
		ServletRequestList reqList = null;
		if( existing instanceof ServletRequestList ) {
			reqList = (ServletRequestList)existing;
		} else {
			reqList = new ServletRequestList();
			reqList.add(existing);
		}

		// Add the new value to list
		reqList.add(value);

		// Store it, and return
		super.put(name, reqList);
	}

	/**
	 * Processes the multipart request parameters
	 * 
	 * @param  req servlet parameter
	 **/
	private boolean multipartProcessing(HttpServletRequest request) {

		// Only work when there is multi part
		if (!ServletFileUpload.isMultipartContent(request)) {
			return false;
		}
		
		// configures upload settings
		DiskFileItemFactory factory = new DiskFileItemFactory();
		// sets memory threshold - beyond which files are stored into "disk" temp file
		factory.setSizeThreshold(MEMORY_THRESHOLD);
		
		// sets temporary location to store files (Already done by default)
		// factory.setRepository(new File(System.getProperty("java.io.tmpdir")));
		
		// Get the servlet file uploader handler class
		ServletFileUpload upload = new ServletFileUpload(factory);
		
		//
		// The following is ignored, as MAX request size should be configured by server not application
		//
		// // sets maximum size of upload file
		// upload.setFileSizeMax(MAX_FILE_SIZE);
		// // sets maximum size of request (include file + form data)
		// upload.setSizeMax(MAX_REQUEST_SIZE);
		//
		
		try {
			// List of form file items to process
			List<FileItem> formItems = upload.parseRequest(request);
			
			// Detect request encoding format, by default set to UTF-8
			String encoding = (request.getCharacterEncoding() != null) ? request
				.getCharacterEncoding() : "UTF-8";
			
			// Process the various multipart form items
			if (formItems != null && formItems.size() > 0) {
				for (FileItem item : formItems) {
					// Field name to handle
					// This is not the "same" as "file name"
					String fieldname = item.getFieldName();

					// processes only fields that are not form fields
					if (item.isFormField()) {
						// Get the field value and store it
						appendRequestParameter(fieldname, item.getString(encoding));
					} else {
						// Process the file, and store it
						appendRequestParameter(fieldname, new ServletRequestFile((DiskFileItem)item) );
					}
				}
			}
			// There are no more multipart form items?
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		
		// Finish processing
		return true;
	}
	
}