package Tests;

import ElevatorSubsystem.Elevator;
import ElevatorSubsystem.ElevatorButton;
import ElevatorSubsystem.ElevatorConfig;
import ElevatorSubsystem.ElevatorStatus;
import junit.framework.TestCase;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class ElevatorTest extends TestCase{
    //declaring instance variables
    private Elevator e;

    private ElevatorConfig control;

    //Initializing the objects being tested
    public void setUp() throws Exception {

        e = new Elevator("4", ElevatorButton.UP);
        control = new ElevatorConfig(5000, 3, "4");
    }

    //Closing the objects being tested
    public void tearDown() throws Exception {
        e = null;
        control = null;
    }

    public void test() throws UnknownHostException {
        //testing all return type methods in Elevator.java and the toInt(String) method in the ElevatorControl.java
        assertEquals("4", e.getCurrentFloor());
        assertEquals(4, e.getIntFloor());
        assertEquals(ElevatorButton.UP, e.getDirection());
        Elevator e2 = new Elevator("4", ElevatorButton.HOLD); //creating second elevator object
        assertEquals(ElevatorStatus.IN_USE, e.getStatus());
        assertEquals(4, control.toInt("4"));

        //Testing the created packet
        String expectedData = "\0" + "1" + "\0" + "3" + "\0"; // packetType = 1

        DatagramPacket expectedPacket = new DatagramPacket(expectedData.getBytes(), expectedData.getBytes().length,
                InetAddress.getLocalHost(), 5000);

        DatagramPacket actualPacket = ElevatorConfig.createPacket("1", "3", 5000);

        assertNotNull(actualPacket);
        assertEquals(expectedPacket.getAddress(), actualPacket.getAddress());
        assertEquals(expectedPacket.getPort(), actualPacket.getPort());
        assertTrue(Arrays.equals(expectedPacket.getData(), actualPacket.getData()));
        assertEquals(expectedPacket.getLength(), actualPacket.getLength());
    }
}
