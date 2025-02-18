package utils;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Utility class containing static methods for packet handling and calculation.
 */
public final class Utilities {

	/* ## HEADER AND COMMAND IDENTIFIERS ## */
	public static final String ACK = "1";
	public static final String CMD = "2";
	public static final String DATA = "3";
	public static final String ERROR = "0";

	public static final String FLOOR_BUTTON = "0x10"; // button was pressed on floor
	public static final String ELEVATOR_ARRIVED = "0x11"; // elevator arrived
	public static final String DOWN = "0x31"; // going down
	public static final String UP = "0x30"; // going up
	public static final String DOOR_OPEN = "0x3A"; // open car door
	public static final String DOOR_CLOSE = "0x3B"; // close car door
	public static final String STOP = "0x3C"; // stop elevator car
	public static final String ERROR_DOOR_JAM = "0xE0"; // error door jam
	public static final String ERROR_STUCK = "0xE1"; // elevator stuck
	/* ## ------------------------------ ## */

	/**
	 * Method to parse a packet. Returns a String array.
	 *
	 * @param bytes Byte array containing packet data
	 * @return String array of data elements
	 */
	public static String[] parsePacket(byte[] bytes) {

		String packet = new String(bytes);
		String[] parsed = packet.replaceFirst("\0", "").split("\0");

		return parsed;
	}

	/**
	 * Creates and returns a DatagramPacket.
	 *
	 * @param packetType Type of packet (ACK, CMD, DATA, ERROR)
	 * @param ins        Instruction to include in the packet
	 * @param port       Destination port number
	 * @return DatagramPacket with specified data
	 */
	public static DatagramPacket createPacket(String packetType, String ins, int port) {
		String data;
		DatagramPacket packet = null;

		// Construct packet data based on packet type
		if (packetType == "0") {
			data = "\0" + ERROR + "\0" + ins + "\0";
		} else if (packetType == "1") {
			data = "\0" + ACK + "\0" + ins + "\0";
		} else if (packetType == "2") {
			data = "\0" + CMD + "\0" + ins + "\0";
		} else {
			data = "\0" + DATA + "\0" + ins + "\0";
		}

		try {
			// Create DatagramPacket with specified data and destination address
			packet = new DatagramPacket(data.getBytes(), data.getBytes().length, InetAddress.getLocalHost(), port);
		} catch (UnknownHostException uhe) {
			// Handle unknown host exception
			System.out.println("Unable to create packet (UnknownHostException), exiting.");
			System.exit(1);
		}
		return packet;
	}

	/**
	 * Calculates the suitability of an elevator to service a request based on its
	 * distance and direction.
	 *
	 * @param n                  Number of floors
	 * @param currentFloor       Current floor of the elevator
	 * @param requestedFloor     Requested floor by the user
	 * @param currentElevatorDir Current direction of the elevator
	 * @param requestedDir       Requested direction by the user
	 * @param status             Status of the elevator (IDLE, SUSPENDED)
	 * @return Calculated suitability value
	 */
	public static int calculateSuitability(int n, int currentFloor, int requestedFloor, String currentElevatorDir,
										   String requestedDir, String status) {

		int calculated = 0;
		int distance = currentFloor - requestedFloor; // if -ive, call is above, if +ive, call is below

		// If elevator is suspended, return -1 indicating unsuitability
		if (status.equals("SUSPENDED")) {
			return -1;
		}

		// Calculate suitability based on current elevator status and direction
		if (status.equals("IDLE")) {
			// Elevator is idle, prioritize based on absolute distance
			distance = Math.abs(distance);
			calculated = n + 1 - distance;
		} else if (currentElevatorDir.equals("DOWN")) {
			// Elevator is moving down
			if (distance < 0) {
				calculated = 1;
			} else if (distance > 0 && requestedDir.equals(currentElevatorDir)) {
				calculated = n + 2 - distance;
			} else if (distance > 0 && !requestedDir.equals(currentElevatorDir)) {
				calculated = n + 1 - distance;
			}
		} else if (currentElevatorDir.equals("UP")) {
			// Elevator is moving up
			if (distance > 0) {
				calculated = 1;
			} else if (distance < 0 && requestedDir.equals(currentElevatorDir)) {
				distance = Math.abs(distance);
				calculated = n + 2 - distance;
			} else if (distance < 0 && !requestedDir.equals(currentElevatorDir)) {
				distance = Math.abs(distance);
				calculated = n + 1 - distance;
			}
		}
		return calculated;
	}
}
