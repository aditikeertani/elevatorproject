/**
 * @author Jennifer Ogidi-Gbegbaje, Nkechi Chukwuma
 */


package tests;
import java.net.UnknownHostException;

import junit.framework.*;
import utils.*;

public class SchedulerTest extends TestCase{
	//declaring instance variables
	private Utilities util;
	
	//Initializing Objects being tested
	public void setUp() throws Exception {
		util = new Utilities();
	}

	//Closing objects being tested
	public void tearDown() throws Exception {
		util = null;
	}

	//testing Utilities
	public void test() throws UnknownHostException {
		//testing calculateSuitability method
		assertEquals(1, Utilities.calculateSuitability(7, 5, 6, "DOWN", "IDLE", "WORKING"));
		assertEquals(7, Utilities.calculateSuitability(7, 5, 3, "DOWN", "DOWN", "WORKING"));
		assertEquals(6, Utilities.calculateSuitability(7, 5, 3, "DOWN", "IDLE", "WORKING"));
		assertEquals(1, Utilities.calculateSuitability(7, 2, 1, "UP", "UP", "WORKING"));
		assertEquals(5, Utilities.calculateSuitability(7, 2, 5, "UP", "IDLE", "WORKING"));
		assertEquals(6, Utilities.calculateSuitability(7, 2, 5, "UP", "UP", "WORKING"));
		assertEquals(-1, Utilities.calculateSuitability(7, 2, 5, "UP", "UP", "SUSPENDED"));

		
	}
	
}
