package picoded.servlet;

import java.net.ServerSocket;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.lang3.RandomStringUtils;

///
/// The SQL configuration vars, this is to ensure a centralized place to change the various test configuration values
/// Value access were made as functions, to facilitate future migration to config files??
///
public class ServletTestConfig {
	
	///
	/// Randomly generated table prefix, used to prevent multiple running tests from colliding
	///
	static public String randomTablePrefix() {
		return "T" + RandomStringUtils.randomAlphanumeric(7).toUpperCase();
	}
	
	//
	// Issue a somewhat unique port number for use in test cases
	//
	public synchronized static int issuePortNumber() {
		// Start with a random port between 10k to 50k
		int portCounter = ThreadLocalRandom.current().nextInt(10000, 50000);
		
		// Increment if a conflict is found
		int checkTries = 0;
		while (isLocalPortInUse(portCounter)) {
			portCounter = ThreadLocalRandom.current().nextInt(10000, 50000);
			
			++checkTries;
			if (checkTries > 1000) {
				throw new RuntimeException("Attempted over " + checkTries
					+ " to get a local port =( sad");
			}
		}
		
		// Returns the port counter
		return portCounter;
	}
	
	/// Utility function used to test if a localhost port is in use, if so skip its "issue process"
	/// 
	/// @param   Port number to test
	///
	/// @return  true if its in use
	private static boolean isLocalPortInUse(int port) {
		try {
			// ServerSocket try to open a LOCAL port
			new ServerSocket(port).close();
			// local port can be opened, it's available
			return false;
		} catch (Exception e) {
			// local port cannot be opened, it's in use
			return true;
		}
	}
}