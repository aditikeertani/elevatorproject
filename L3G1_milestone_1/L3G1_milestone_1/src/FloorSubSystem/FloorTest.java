/**
 * The FloorTest class contains JUnit tests for the Floor class and related methods in the FloorControl class.
 *
 * @author Nkechi Chukwuma, Jennifer Ogidi-Gbebaje
 */
package FloorSubSystem;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import FloorSubSystem.Floor.Direction;
import junit.framework.TestCase;

/**
 * The FloorTest class extends TestCase and includes various test cases for the Floor class and
 * FloorControl methods.
 */
public class FloorTest extends TestCase {
	private Floor floor;
	private FloorControl fs;

	/**
	 * Set up the initial conditions for the tests.
	 *
	 * @throws Exception If an exception occurs during setup.
	 */
	@Before
	public void setUp() throws Exception {
		floor = new Floor(4, 2, true, false);
		fs = new FloorControl();
	}

	/**
	 * Clean up after the tests.
	 *
	 * @throws Exception If an exception occurs during teardown.
	 */
	@After
	public void tearDown() throws Exception {
		floor = null;
		fs = null;
	}

	/**
	 * Test basic functionality of the Floor class.
	 */
	@Test
	public void test() {
		int floorNo = 4;
		int numElev = 2;
		assertEquals(floorNo, floor.getFloorNumber());
		assertEquals(numElev, floor.getElevNum());
		floor.setUpLampOn();
		assertTrue(floor.upLamp);
		floor.setDownLampOn();
		assertTrue(floor.downLamp);
		floor.setUpLampOff();
		assertFalse(floor.upLamp);
		floor.setDownLampOff();
		assertFalse(floor.downLamp);
	}

	/**
	 * Test the parseInputFile method in FloorControl for file parsing.
	 *
	 * @throws FileNotFoundException If the file is not found.
	 * @throws IOException           If an I/O error occurs.
	 */
	@Test
	public void TestparseInputFile() throws FileNotFoundException, IOException {
		fs.parseInputFile("00:00:14:05 1 UP 4");
	}

	/**
	 * Test the getInputData method in FloorControl for extracting data from a string.
	 */
	@Test
	public void TestgetInputData() {
		Direction d = null;
		Object[] obj = new Object[7];
		obj[0] = 00;
		obj[1] = 00;
		obj[2] = 45;
		obj[3] = 674;
		obj[4] = 1;
		obj[5] = d.UP;
		obj[6] = 6;
		String s = "00:00:45:674 1 UP 6";
		assertEquals(obj, fs.getInputData(s));
	}

	/**
	 * Test the createPacketData method in FloorControl for creating packet data.
	 */
	@Test
	public void TestcreatePacketData() {
		String ins = "3";
		String data = "\0" + fs.ACK + "\0" + ins + "\0";
		byte[] dataB = data.getBytes();
		assertEquals(dataB, fs.createPacketData(1, "3"));
	}

	/**
	 * Test the readPacketData method in FloorControl for reading packet data from a byte array.
	 */
	@Test
	public void TestreadPacketData() {
		String a = "Group 4";
		byte b[] = a.getBytes();
		String ans[] = {"Group 4"};
		assertEquals(ans, fs.readPacketData(b));
	}
}
