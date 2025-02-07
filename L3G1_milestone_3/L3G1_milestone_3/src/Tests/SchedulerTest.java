package Tests;

import Scheduler.Scheduler;
import junit.framework.TestCase;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class SchedulerTest extends TestCase{
    //Declaring instance variables
    private Scheduler s;

    //Initializing object being tested
    public void setUp() throws Exception {
        s = new Scheduler();

    }

    //closing object being tested
    public void tearDown() throws Exception {
        s = null;
    }

    //testing all return type methods in Scheduler.java
    public void test() throws UnknownHostException {
        //Testing createPacker method in Scheduler class
        String expectedData = "\0" + "1" + "\0" + "3" + "\0"; // packetType = 1

        DatagramPacket expectedPacket = new DatagramPacket(expectedData.getBytes(), expectedData.getBytes().length,
                InetAddress.getLocalHost(), 8008);

        DatagramPacket actualPacket = Scheduler.createPacket("1", "3", 8008);

        assertNotNull(actualPacket);
        assertEquals(expectedPacket.getAddress(), actualPacket.getAddress());
        assertEquals(expectedPacket.getPort(), actualPacket.getPort());
        assertTrue(Arrays.equals(expectedPacket.getData(), actualPacket.getData()));
        assertEquals(expectedPacket.getLength(), actualPacket.getLength());

        //testing calculateSuitability method in Scheduler class
        assertEquals(8, s.calculateSuitability(7, 5, 5, "IDLE", "IDLE"));
        assertEquals(1, s.calculateSuitability(7, 5, 6, "DOWN", "IDLE"));
        assertEquals(7, s.calculateSuitability(7, 5, 3, "DOWN", "DOWN"));
        assertEquals(6, s.calculateSuitability(7, 5, 3, "DOWN", "IDLE"));
        assertEquals(1, s.calculateSuitability(7, 2, 1, "UP", "UP"));
        assertEquals(5, s.calculateSuitability(7, 2, 5, "UP", "IDLE"));
        assertEquals(6, s.calculateSuitability(7, 2, 5, "UP", "UP"));
    }
}
