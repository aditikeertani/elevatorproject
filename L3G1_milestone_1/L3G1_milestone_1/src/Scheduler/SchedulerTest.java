/**
 * The SchedulerTest class contains JUnit tests for the Scheduler class and related methods in the Host class.
 *
 * @author Nkechi Chukwuma, Jennifer Ogidi-Gbebaje
 */
package Scheduler;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The SchedulerTest class includes various test cases for the Scheduler class and
 * Host methods.
 */
public class SchedulerTest {
	private Host host;
	private Scheduler s;

	/**
	 * Set up the initial conditions for the tests.
	 *
	 * @throws Exception If an exception occurs during setup.
	 */
	@Before
	public void setUp() throws Exception {
		host = new Host();

	}

	/**
	 * Clean up after the tests.
	 *
	 * @throws Exception If an exception occurs during teardown.
	 */
	@After
	public void tearDown() throws Exception {
		host = null;
	}

	/**
	 * Test the parsePacket method in the Host class for parsing a packet.
	 */
	@Test
	public void testparsePacket() {
		String a = "Group 4";
		byte[] b = a.getBytes();
		String[] ans = {"Group 4"};
		assertEquals(ans, host.parsePacket(b));
	}
}
