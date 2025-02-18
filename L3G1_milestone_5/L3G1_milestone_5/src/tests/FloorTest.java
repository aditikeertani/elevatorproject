/*
 * @author Jennifer Ogidi-Gbegbaje, Nkechi Chukwuma
 * Test Case for FloorSystem 
 */

package tests;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import FloorSubsystem.HandleUserInput;
import FloorSubsystem.*;
import FloorSubsystem.FloorControl.Direction;

import java.io.*;

import junit.framework.*;

public class FloorTest extends TestCase{
	//declaring instance variables
	private Floor floor;
	private FloorControl fs;
	private HandleUserInput uih;
	
	//Initializing Objects being tested
	public void setUp() throws Exception {
		floor = new Floor(2, 5, true, false);
		fs = new FloorControl();
		uih = new HandleUserInput(fs);
	}

	//Closing objects being tested
	public void tearDown() throws Exception {
		floor = null;
		fs = null;
	}

	//testing Floor class
	public void testFloor() {
		int floorNo = 5;
		int numElev = 2;
		boolean lampUP = true;
		boolean lampDOWN = false;
		assertEquals(floorNo, floor.getFloorNum());
		assertEquals(numElev, floor.getElevNum());
		assertFalse(floor.getUpLampStatus());
		assertFalse(floor.getDownLampStatus());
	}
	
	public void TestparseInputFile() throws FileNotFoundException, IOException {
		//fs.parseInputFile("00:00:45:674 1 UP 6");
	}

	//Testing createPacketData method in FloorSubsystem class
	public void TestcreatePacketData(){
		String ins = "3";
		String data = "\0" + 1 + "\0" + ins + "\0";
		byte[] dataB = data.getBytes();
		assertEquals(dataB, fs.createPacketData(1, "3"));
		
		//Testing createServiceRequest method
		byte msg[] = new byte[100];
		String message = "" + " " + 3 + " " + Direction.UP + " " + 5;
		msg = fs.createPacketData(3, message);
		assertEquals(msg, fs.createServiceRequest("", 3, 5, Direction.UP));
	}
	
	
	public void Test() throws SocketException{
		//Testing readPacketData method in FloorSubsyetm class
		String a = "Group 4";
		byte b[] = a.getBytes();
		String ans[] = {"Group 4"};
		assertEquals(ans, fs.readPacketData(b));
		
		//Testing receive method
		String s = "Group4";
		byte msg[] = s.getBytes();
		DatagramPacket packet = new DatagramPacket(msg, msg.length);
		DatagramSocket socket = new DatagramSocket();
		assertEquals(packet, fs.receive(socket, msg));
		
	}
	
}
