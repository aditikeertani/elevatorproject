/**
 * The ElevatorTest class contains unit tests for the Elevator class and associated methods.
 *
 * @author Nkechi Chukwuma, Jennifer Ogidi-Gbegbaje
 */
package ElevatorSubsystem;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ElevatorTest {
	private Elevator elevate;
	private ElevatorButton direction;
	private ElevatorStatus status;
	private ElevatorConfig control;

	/**
	 * Sets up the test environment before each test case.
	 *
	 * @throws Exception If an error occurs during setup.
	 */
	@Before
	public void setUp() throws Exception {
		elevate = new Elevator("4", direction.UP);
		control = new ElevatorConfig();
	}

	/**
	 * Cleans up the test environment after each test case.
	 *
	 * @throws Exception If an error occurs during teardown.
	 */
	@After
	public void tearDown() throws Exception {
		elevate = null;
		control = null;
	}

	/**
	 * Tests various aspects of the Elevator class and associated methods.
	 */
	@Test
	public void test() {
		assertEquals("4", elevate.getCurrentFloor());
		assertEquals(4, elevate.getIntFloor());
		assertEquals(ElevatorButton.UP, elevate.getDirection());
		Elevator e2 = new Elevator("4", direction.HOLD);
		assertEquals(ElevatorStatus.IN_USE, elevate.getStatus());
		assertEquals(4, control.toInt("4"));

		String a = "0";
		byte b[] = a.getBytes();
		String ans[] = {"0"};
		assertEquals(ans, control.packetToString(b));
	}
}
