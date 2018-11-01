package picoded.server;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import picoded.core.struct.GenericConvertList;
import picoded.core.struct.GenericConvertArrayList;
import picoded.core.conv.ConvertJSON;

/**
 * RequestFile interface
 * 
 * Representing a single binary, passed in together with a reqeust.
 */
public interface RequestFile {

	/**
	 * Writes the binary content into a file
	 *
	 * @param  File to write into
	 **/
	public void writeToFile(File file);

	/**
	 * Get the declared file name (if provided)
	 *
	 * @return  String representing the declared file name (maybe null)
	 **/
	public String getName();

	/**
	 * Get the raw input stream
	 *
	 * @return Input Stream representing the file
	 **/
	public InputStream getInputStream();

	/**
	 * Get the raw byte array.
	 *
	 * For extremely large files.
	 * Please use getInputStream or writeToFile instead
	 *
	 * @return Byte Arra representing the file
	 **/
	public byte[] getByteArray();

	/**
	 * Gets the file, as a UTF-8 decoded string
	 *
	 * For extremely large files.
	 * Please use getInputStream or writeToFile instead
	 *
	 * @return  String representing the file
	 **/
	public String toString();

}
