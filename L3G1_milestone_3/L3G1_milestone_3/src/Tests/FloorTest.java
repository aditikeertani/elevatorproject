package Tests;


import FloorSubsystem.Floor;
import FloorSubsystem.FloorControl;
import FloorSubsystem.HandleUserInput;
import junit.framework.TestCase;

import java.util.Arrays;

public class FloorTest extends TestCase{
    //declaring instance variables
    private Floor floor;
    private FloorControl fs;
    private HandleUserInput hui;

    //Initializing Objects being tested
    public void setUp() throws Exception {
        floor = new Floor(2, 5, true, false);
        fs = new FloorControl();
        hui = new HandleUserInput(fs);
    }

    //Closing objects being tested
    public void tearDown() throws Exception {
        floor = null;
        fs = null;
    }


    public void test() {
        //testing Floor class
        int floorNo = 5;
        int numElev = 2;
        assertEquals(floorNo, floor.getFloorNum());
        assertEquals(numElev, floor.getElevNum());

        //Testing createPacketData method in FloorSubsystem class
        String ins = "3";
        String data = "\0" + 1 + "\0" + ins + "\0";
        byte[] expectedDataB = data.getBytes();
        byte[] actualDataB = fs.createPacketData(1, "3");
        assertTrue(Arrays.equals(expectedDataB, fs.createPacketData(1, "3")));

        //Testing readPacketData method in FloorSubsystem class
        String a = "Group 1";
        byte[] b = a.getBytes();
        String[] ans = {"Group 1"};
        assertTrue(Arrays.equals(ans, fs.readPacketData(b)));
    }

}
