package picoded.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.fileupload.disk.DiskFileItem;

import picoded.core.struct.GenericConvertList;
import picoded.core.struct.GenericConvertArrayList;
import picoded.core.conv.ConvertJSON;

/**
 * ServletRequestFile - representing a binary file upload in a requeest
 **/
public class ServletRequestFile {
	
	/**
	 * Apache disk file item, representing the actual file
	 **/
	protected DiskFileItem diskItem = null;

	/**
	 * ServletRequestFile constructor with DiskFileItem
	 * 
	 * @param  inFile  input file
	 */
	public ServletRequestFile(DiskFileItem inFile) {
		diskItem = inFile;
	}

	/**
	 * Writes the file content to another file
	 *
	 * @param  File to write into
	 **/
	public void writeToFile(File file) {
		try {
			diskItem.write(file);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Get the declared file name (if provided)
	 *
	 * @return  String representing the declared file name (maybe null)
	 **/
	public String getName() {
		try {
			return diskItem.getName();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Get the raw input stream
	 *
	 * @return Input Stream representing the file
	 **/
	public InputStream getInputStream(int idx) {
		try {
			return diskItem.getInputStream();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Get the raw byte array.
	 *
	 * For extremely large files.
	 * Please use getInputStream or writeToFile instead
	 *
	 * @return Byte Arra representing the file
	 **/
	public byte[] getByteArray(int idx) {
		return diskItem.get();
	}
	
	// Memoizer for toString
	private String _toString = null;

	/**
	 * Gets the file, as a UTF-8 decoded string
	 *
	 * For extremely large files.
	 * Please use getInputStream or writeToFile instead
	 *
	 * @return  String representing the file
	 **/
	public String toString() {
		try {
			// Result was previously stored
			if (_toString != null && _toString.length() > 0) {
				return _toString;
			}
			
			// Get and cache result
			_toString = diskItem.getString("UTF-8");

			// Return result
			return _toString;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}