package picoded.servlet.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import picoded.servlet.*;

/**
 * Just a simple implmentation of a file server in java, which supports the following
 *
 * + Client side file Caching
 * + GZIP of data
 *
 * # If used directly in WEB.xml
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * <servlet>
 *     <servlet-name>fileServlet</servlet-name>
 *     <servlet-class>picoded.servletUtils.FileServlet</servlet-class>
 *     <init-param>
 *         <param-name>basePath</param-name>
 *         <param-value>/path/to/files</param-value>
 *     </init-param>
 * </servlet>
 *
 * <servlet-mapping>
 *     <servlet-name>fileServlet</servlet-name>
 *     <url-pattern>/files/*</url-pattern>
 * </servlet-mapping>
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * # If used directly inside another servlet
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * (new FileServlet("/path/to/files/")).processRequest(servletRequest, servletResponse);
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 **/
public class FileServlet extends HttpServlet {
	
	///////////////////////////////////////////////////////
	//
	// Static variables
	//
	///////////////////////////////////////////////////////
	
	private static final String MULTIPART_BYTERANGES = "MULTIPART_BYTERANGES";
	
	///////////////////////////////////////////////////////
	//
	// Instance variables
	//
	///////////////////////////////////////////////////////
	
	/**
	 * The file path of the servlet, to fetch the files from,
	 * This can either be set at initializing, or a constructor.
	 **/
	public String basePath = null;
	
	/**
	 * The base file folder
	 **/
	public File baseFolder = null;
	
	/**
	 * The default expire time (disabled)
	 **/
	public long fileExpireTime = 0; //604800000L = 1 week.
	
	/**
	 * Network jitter tolerance for cache handling
	 **/
	public long cacheNetworkJitterTolerance = 1000L;
	
	/**
	 * The gzip buffer size
	 **/
	public int gzipBufferSize = 10240;
	
	///////////////////////////////////////////////////////
	//
	// Constructor
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Default base constructor (does nothing)
	 **/
	public FileServlet() {
		super();
	}
	
	/**
	 * Custom constructor with basePath
	 **/
	public FileServlet(String inBasePath) {
		super();
		// Setup
		basePath = inBasePath;
		// Validate
		validateBasePath();
	}
	
	/**
	 * Custom constructor with basePath
	 **/
	public FileServlet(File inBaseFolder) {
		super();
		// Setup
		baseFolder = inBaseFolder;
		// Validate
		validateBasePath();
	}
	
	///////////////////////////////////////////////////////
	//
	// Core functions
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Process the full file fetch request
	 *
	 * @param  HttpServletRequest to process
	 * @param  HttpServletResponse to response
	 * @param  Indicates if data should be return (HEAD) request (instead of GET)
	 **/
	public void processRequest( //
		HttpServletRequest servletRequest, //
		HttpServletResponse servletResponse, //
		boolean headersOnly //
	) throws IOException {
		
		// requested file by path info,
		// relative to the servlet wildcard in web.xml
		String requestPath = servletRequest.getPathInfo();
		
		// Pass the requestPath
		processRequest(servletRequest, servletResponse, headersOnly, requestPath);
	}
	
	/**
	 * Process the full file fetch request
	 *
	 * @param  HttpServletRequest to process
	 * @param  HttpServletResponse to response
	 * @param  Indicates if data should be return (HEAD) request (instead of GET)
	 * @param Request file path
	 **/
	public void processRequest( //
		HttpServletRequest servletRequest, //
		HttpServletResponse servletResponse, //
		boolean headersOnly, //
		String requestPath //
	) throws IOException {
		
		// Prepare to fetch the file
		//-------------------------------------------
		
		// Throws a 404 error if requestedFile is not provided
		if (requestPath == null) {
			servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		// URL Decode the requestPath
		requestPath = URLDecoder.decode(requestPath, "UTF-8");
		
		// Does some path cleanup (for what???)
		// requestedFile.replaceAll("\/\.\/", "/").replaceAll("\/\/","\/");
		
		// 404 error if directory traversal / esclation as a security measure
		// Also blocks ".private" file access
		if (requestPath.contains("/.") || requestPath.contains("..")) {
			servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		// 404 error if accessing possible java servlet protected files
		String requestPath_lowerCase = requestPath.toLowerCase();
		if (requestPath_lowerCase.contains("/web-inf/")
			|| requestPath_lowerCase.contains("/meta-inf/")) {
			servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		// Fetch THE file, and validate
		//-------------------------------------------
		File file = new File(basePath, requestPath);
		
		// Index.html support for directory request
		//-------------------------------------------
		if (file.isDirectory()) {
			file = new File(file, "index.html");
		}
		
		// Validate the file and output it
		processRequest(servletRequest, servletResponse, headersOnly, file);
	}
	
	/**
	 * Process the full file fetch request
	 *
	 * @param  HttpServletRequest to process
	 * @param  HttpServletResponse to response
	 * @param  Indicates if data should be return (HEAD) request (instead of GET)
	 * @param  Request file
	 **/
	public void processRequest( //
		HttpServletRequest servletRequest, //
		HttpServletResponse servletResponse, //
		boolean headersOnly, //
		File file) throws IOException {
		
		// Validate the file
		//-------------------------------------------
		
		// Check if file exists, or is a directory
		if (!file.exists() || file.isDirectory()) {
			// 404 error if file not found
			servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		// Cache Headers handling and validation
		//-------------------------------------------
		
		// Prepare some variables. The ETag is an unique identifier of the file.
		String fileName = file.getName();
		long length = file.length();
		long lastModified = file.lastModified();
		String eTag = fileName + "-" + length + "-" + lastModified;
		long expires = (fileExpireTime > 0) ? (System.currentTimeMillis() + fileExpireTime) : 0;
		
		// If-None-Match header should contain "*" or ETag. If so, then return 304.
		String ifNoneMatch = servletRequest.getHeader("If-None-Match");
		if (ifNoneMatch != null && headerMatch(ifNoneMatch, eTag)) {
			servletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			servletResponse.setHeader("ETag", eTag); // Required in 304.
			if (expires > 0) {
				servletResponse.setDateHeader("Expires", expires);
			}
			return;
		}
		
		// If-Modified-Since header should be greater than LastModified. If so, then return 304.
		// This header is ignored if any If-None-Match header is specified.
		long ifModifiedSince = servletRequest.getDateHeader("If-Modified-Since");
		if (ifNoneMatch == null && ifModifiedSince != -1
			&& ifModifiedSince + cacheNetworkJitterTolerance > lastModified) {
			servletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			servletResponse.setHeader("ETag", eTag); // Required in 304.
			if (expires > 0) {
				servletResponse.setDateHeader("Expires", expires);
			}
			return;
		}
		
		// Download resume validation
		//-------------------------------------------
		
		// If-Match header should contain "*" or ETag. If not, then return 412.
		String ifMatch = servletRequest.getHeader("If-Match");
		if (ifMatch != null && !headerMatch(ifMatch, eTag)) {
			servletResponse.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
			return;
		}
		
		// If-Unmodified-Since header should be greater than LastModified. If not, then return 412.
		long ifUnmodifiedSince = servletRequest.getDateHeader("If-Unmodified-Since");
		if (ifUnmodifiedSince != -1 && ifUnmodifiedSince + 1000 <= lastModified) {
			servletResponse.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
			return;
		}
		
		// Data range handling (download resume)
		//-------------------------------------------
		
		// Prepare some variables. The full Range represents the complete file.
		Range full = new Range(0, length - 1, length);
		List<Range> ranges = new ArrayList<Range>();
		
		// Validate and process Range and If-Range headers.
		String range = servletRequest.getHeader("Range");
		if (range != null) {
			// Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
			if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
				servletResponse.setHeader("Content-Range", "bytes */" + length); // Required in 416.
				servletResponse.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
				return;
			}
			
			// If-Range header should either match ETag or be greater then LastModified. If not,
			// then return full file.
			String ifRange = servletRequest.getHeader("If-Range");
			if (ifRange != null && !ifRange.equals(eTag)) {
				try {
					long ifRangeTime = servletRequest.getDateHeader("If-Range"); // Throws IAE if invalid.
					if (ifRangeTime != -1 && ifRangeTime + 1000 < lastModified) {
						ranges.add(full);
					}
				} catch (IllegalArgumentException ignore) {
					ranges.add(full);
				}
			}
			
			// If any valid If-Range header, then process each part of byte range.
			if (ranges.isEmpty()) {
				for (String part : range.substring(6).split(",")) {
					// Assuming a file with length of 100, the following examples returns bytes at:
					// 50-80 (50 to 80), 40- (40 to length=100), -20 (length-20=80 to length=100).
					long start = sublong(part, 0, part.indexOf("-"));
					long end = sublong(part, part.indexOf("-") + 1, part.length());
					
					if (start == -1) {
						start = length - end;
						end = length - 1;
					} else if (end == -1 || end > length - 1) {
						end = length - 1;
					}
					
					// Check if Range is syntactically valid. If not, then return 416.
					if (start > end) {
						servletResponse.setHeader("Content-Range", "bytes */" + length); // Required in 416.
						servletResponse.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
						return;
					}
					
					// Add range.
					ranges.add(new Range(start, end, length));
				}
			}
		}
		
		// Download type and gzip support check
		//-------------------------------------------
		
		// Get content type by file name and set default GZIP support and content disposition.
		String contentType = servletRequest.getServletContext().getMimeType(fileName);
		boolean acceptsGzip = false;
		String disposition = "inline";
		
		// If content type is unknown, then set the default value.
		// For all content types, see: http://www.w3schools.com/media/media_mimeref.asp
		// To add new content types, add new mime-mapping entry in web.xml.
		if (contentType == null) {
			contentType = "application/octet-stream";
		}
		
		if (contentType.startsWith("text")) {
			//
			// If content type is text, then determine whether GZIP content encoding is supported by
			// the browser and expand content type with the one and right character encoding.
			//
			String acceptEncoding = servletRequest.getHeader("Accept-Encoding");
			acceptsGzip = acceptEncoding != null && headerAccept(acceptEncoding, "gzip");
			contentType += ";charset=UTF-8";
		} else if (!contentType.startsWith("image")) {
			//
			// Else, expect for images, determine content disposition. If content type is supported by
			// the browser, then set to inline, else attachment which will pop a 'save as' dialogue.
			//
			String accept = servletRequest.getHeader("Accept");
			disposition = accept != null && headerAccept(accept, contentType) ? "inline"
				: "attachment";
		}
		
		// Return headers
		//-------------------------------------------
		
		// Initialize servletResponse.
		servletResponse.reset();
		//servletResponse.setBufferSize(10240);
		servletResponse.setHeader("Content-Disposition", disposition + ";filename=\"" + fileName
			+ "\"");
		servletResponse.setHeader("Accept-Ranges", "bytes");
		servletResponse.setHeader("ETag", eTag);
		servletResponse.setDateHeader("Last-Modified", lastModified);
		servletResponse.setDateHeader("Expires", expires);
		
		// Return headers
		//-------------------------------------------
		// Prepare streams.
		RandomAccessFile input = null;
		OutputStream output = null;
		
		try {
			// Open streams.
			input = new RandomAccessFile(file, "r");
			output = servletResponse.getOutputStream();
			
			if (ranges.isEmpty() || ranges.get(0) == full) {
				// Return full file.
				Range r = full;
				servletResponse.setContentType(contentType);
				servletResponse.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/"
					+ r.total);
				
				if (!headersOnly) {
					if (acceptsGzip) {
						// Use GZIP in response
						servletResponse.setHeader("Content-Encoding", "gzip");
						output = new GZIPOutputStream(output, gzipBufferSize);
					} else {
						// Direct raw ouput (length is predictable, while gzip it isnt)
						servletResponse.setHeader("Content-Length", String.valueOf(r.length));
					}
					
					// Copy full range.
					copy(input, output, r.start, r.length);
				}
			} else if (ranges.size() == 1) {
				// Return single part of file.
				Range r = ranges.get(0);
				servletResponse.setContentType(contentType);
				servletResponse.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/"
					+ r.total);
				servletResponse.setHeader("Content-Length", String.valueOf(r.length));
				servletResponse.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.
				
				if (!headersOnly) {
					// Copy single part range.
					copy(input, output, r.start, r.length);
				}
			} else {
				
				// Return multiple parts of file.
				servletResponse
					.setContentType("multipart/byteranges; boundary=" + MULTIPART_BYTERANGES);
				servletResponse.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.
				
				if (!headersOnly) {
					// Cast back to ServletOutputStream to get the easy println methods.
					ServletOutputStream sos = (ServletOutputStream) output;
					
					// Copy multi part range.
					for (Range r : ranges) {
						// Add multipart boundary and header fields for every range.
						sos.println();
						sos.println("--" + MULTIPART_BYTERANGES);
						sos.println("Content-Type: " + contentType);
						sos.println("Content-Range: bytes " + r.start + "-" + r.end + "/" + r.total);
						
						// Copy single part range of multi part range.
						copy(input, output, r.start, r.length);
					}
					
					// End with multipart boundary.
					sos.println();
					sos.println("--" + MULTIPART_BYTERANGES + "--");
				}
			}
		} finally {
			close(output);
			close(input);
		}
	}
	
	///////////////////////////////////////////////////////
	//
	// Utility functions
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Utility function called by init, or paramtirized startup. To validate basePath
	 **/
	public void validateBasePath() {
		// Validate base path.
		if (baseFolder == null) {
			if (basePath == null) {
				throw new RuntimeException("FileServlet init param 'basePath' is required.");
			}
			baseFolder = new File(basePath);
		}
		
		if (!baseFolder.exists()) {
			throw new RuntimeException("FileServlet init param 'basePath' (" + baseFolder.toString()
				+ ") does actually not exist in file system.");
		}
		
		if (!baseFolder.isDirectory()) {
			throw new RuntimeException("FileServlet init param 'basePath' value ("
				+ baseFolder.toString() + ") is actually not a directory in file system.");
		}
		
		if (!baseFolder.canRead()) {
			throw new RuntimeException("FileServlet init param 'basePath' value ("
				+ baseFolder.toString() + ") is actually not readable in file system.");
		}
		
		// Use provided file, to extract filepath
		if (basePath == null) {
			basePath = baseFolder.toString();
		}
	}
	
	/**
	 * Returns true if the header matches the given value, or is "*" wild
	 *
	 * @param Header to match
	 * @param Value to be matched
	 *
	 * @return boolean true if valid
	 **/
	private static boolean headerMatch(String header, String toMatch) {
		// Header pattern: grpA/typA, grpB/typB, grpC/typC .....
		
		// Shorten less efficent refence implmentation
		//-------------------------------------------------------------------
		// return (header.indexOf("*/*") > -1 || header.indexOf( toMatch ) > -1 || header.indexOf( toMatch.replaceAll("/.*$", "/*") ) > -1);
		
		// Longer more "efficent" version for multiple entry?
		//-------------------------------------------------------------------
		String[] headerValues = header.split("\\s*(,|;)\\s*");
		Arrays.sort(headerValues);
		
		return Arrays.binarySearch(headerValues, toMatch) > -1
			|| Arrays.binarySearch(headerValues, toMatch.replaceAll("/.*$", "/*")) > -1
			|| Arrays.binarySearch(headerValues, "*/*") > -1;
	}
	
	/**
	 * Returns true if the header accept the given value, or is "* / *" wild
	 *
	 * @param Header to match
	 * @param Value to be matched
	 *
	 * @return boolean true if valid
	 **/
	private static boolean headerAccept(String header, String toMatch) {
		// Header pattern: valueA, valueB, valueC .....
		
		// Shorten less efficent refence implmentation
		//-------------------------------------------------------------------
		// return (header.indexOf("*") > -1 || header.indexOf(toMatch) > -1);
		
		// Longer more "efficent" version for multiple entry?
		//-------------------------------------------------------------------
		String[] headerValues = header.split("\\s*,\\s*");
		Arrays.sort(headerValues);
		
		return Arrays.binarySearch(headerValues, toMatch) > -1
			|| Arrays.binarySearch(headerValues, "*") > -1;
	}
	
	/**
	 * Inner class representing a byte range
	 **/
	protected class Range {
		long start;
		long end;
		long length;
		long total;
		
		/**
		 * Construct a byte range.
		 * @param Start of the byte range.
		 * @param End of the byte range.
		 * @param Total length of the byte source.
		 **/
		public Range(long start, long end, long total) {
			this.start = start;
			this.end = end;
			this.length = end - start + 1;
			this.total = total;
		}
	}
	
	/**
	 * Returns a substring of the given string value from the given begin index to the given end
	 * index as a long. If the substring is empty, then -1 will be returned
	 *
	 * @param Value to return a substring as long from.
	 * @param begin index of the substring to be returned as long.
	 * @param end index of the substring to be returned as long.
	 *
	 * @return A substring of the given string value as long or -1 if substring is empty.
	 **/
	private static long sublong(String value, int beginIndex, int endIndex) {
		String substring = value.substring(beginIndex, endIndex);
		return (substring.length() > 0) ? Long.parseLong(substring) : -1;
	}
	
	/**
	 * Copy the given byte range of the given input to the given output.
	 *
	 * @param RandomAccessFile input to copy the given range to the given output for.
	 * @param OutputStream to copy the given range from the given input for.
	 * @param Start of the byte range.
	 * @param Length of the byte range.
	 *
	 * @throws IOException If something fails at I/O level.
	 **/
	private static void copy(RandomAccessFile input, OutputStream output, long start, long length)
		throws IOException {
		byte[] buffer = new byte[64]; //line cache size
		int read;
		
		if (input.length() == length) {
			// Write full range.
			while ((read = input.read(buffer)) > 0) {
				output.write(buffer, 0, read);
			}
		} else {
			// Write partial range.
			input.seek(start);
			long toRead = length;
			
			while ((read = input.read(buffer)) > 0) {
				if ((toRead -= read) > 0) {
					output.write(buffer, 0, read);
				} else {
					output.write(buffer, 0, (int) toRead + read);
					break;
				}
			}
		}
	}
	
	/**
	 * Close the given resource.
	 *
	 * @param  resource to be closed.
	 **/
	private static void close(Closeable resource) {
		if (resource != null) {
			try {
				resource.close();
			} catch (IOException ignore) {
				// Ignore IOException. This normally occurs when connection download is aborted
			}
		}
	}
	
	///////////////////////////////////////////////////////
	//
	// HttpServlet implmentation (if used directly)
	//
	///////////////////////////////////////////////////////
	
	/**
	 * Initialize the server with the basePath
	 **/
	public void init() throws ServletException {
		// Get base path from web.xml
		basePath = getInitParameter("basePath");
		// Validate the base path
		validateBasePath();
	}
	
	/**
	 * Process HEAD servletRequest. This returns the same headers as GET request, but without content.
	 **/
	protected void doHead(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		processRequest(request, response, true);
	}
	
	/**
	 * Process GET request, with headers and content
	 **/
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		processRequest(request, response, false);
	}
	
}
