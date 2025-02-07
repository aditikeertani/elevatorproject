/**
 * The FloorControl class represents the FloorSubsystem side for a simple echo server based on UDP/IP.
 * The FloorSubsystem sends a character string to the echo server, then waits for the server to send it
 * back to the FloorSubsystem.
 *
 * This class handles communication with the elevator system's scheduler, sends service
 * requests, and processes elevator departure messages.
 *
 * @author Ryan Capstick, Oluwatobi Olowookere
 */

package FloorSubSystem;

import java.io.*;
import java.net.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The FloorControl class handles communication with the elevator system's scheduler, sends service
 * requests, and processes elevator departure messages.
 */
public class FloorControl {

	DatagramPacket sendPacket;
	DatagramPacket receivePacket;
	DatagramSocket sendReceiveSocket;

	/* Header and command identifiers */
	final int ACK = 1;
	final int CMD = 2;
	final int DATA = 3;
	final int ERROR = 0;

	final int FLOORPORT = 6520;     // Port of the floor subsystem
	final int SCHEDPORT = 8008;     // Port of the scheduler

	/**
	 * Represents the direction of elevator movement.
	 */
	public enum Direction {
		UP,
		DOWN,
		IDLE
	}

	int numFloors;
	int upperFloor;
	int lowerFloor;

	int floorMin = 0;
	int floorTotal = 5;
	int elevatorTotal = 1;

	Direction currentDirection = Direction.IDLE;
	int requestCount = 0;

	static ArrayList<Floor> floors;

	public FloorControl() {

		ArrayList<byte[]> serviceReqs = new ArrayList<byte[]>();
		floors = new ArrayList<Floor>();

		try {
			sendReceiveSocket = new DatagramSocket(FLOORPORT);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Parse the input file containing service requests.
	 *
	 * @param filePath The path to the input file.
	 * @throws FileNotFoundException If the file is not found.
	 * @throws IOException           If an I/O error occurs.
	 */
	public void parseInputFile(String filePath) throws FileNotFoundException, IOException {

		int hour = 0;
		int min = 0;
		int sec = 0;
		int mSec = 0;
		int startFloor = 0;
		int destFloor = 0;
		Direction targetDirection = Direction.IDLE;

		ArrayList<String> serviceReqList = new ArrayList<String>();

		BufferedReader br = new BufferedReader(new FileReader(filePath));
		String line = null;
		while ((line = br.readLine()) != null) {
			Pattern pattern = Pattern.compile("'\\d{2}:\\d{2}:\\d{2}.\\d*\\s\\d{1,2}\\s[A-z]{2,4}\\s\\d{1,2}'");
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				serviceReqList.add(new String(matcher.group()));
				System.out.println("Got a new service request! " + serviceReqList.get(serviceReqList.size() - 1));
			}
		}
		br.close();
		for (String s : serviceReqList) {
			String data[] = s.split(" ");
			String time[] = data[0].split("[:.]");
			hour = Integer.parseInt(time[0]);
			min = Integer.parseInt(time[1]);
			sec = Integer.parseInt(time[2]);
			mSec = Integer.parseInt(time[3]);
			startFloor = Integer.parseInt(data[1]);
			if (data[2].toUpperCase().equals("UP")) targetDirection = Direction.UP;
			else if (data[2].toUpperCase().equals("DOWN")) targetDirection = Direction.DOWN;
			destFloor = Integer.parseInt(data[3]);

			for (Floor f : floors) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e ) {
					e.printStackTrace();
					System.exit(1);
				}
				if (f.getFloorNumber() == startFloor) {
					createServiceRequest(startFloor, destFloor, targetDirection);
					if (targetDirection == Direction.UP) f.setUpLampOn();
					else if (targetDirection == Direction.DOWN) f.setDownLampOn();
				}
			}
		}
	}

	/**
	 * Get input data from the provided string.
	 *
	 * @param dataString The input string.
	 * @return An array of objects containing the parsed data.
	 */
	public Object[] getInputData(String dataString) {
		int numParameters = 7;
		Object[] data = new Object[numParameters];
		String[] input = dataString.split(" ");
		String[] time = input[0].split("[:.]");
		data[0] = Integer.parseInt(time[0]);
		data[1] = Integer.parseInt(time[1]);
		data[2] = Integer.parseInt(time[2]);
		data[3] = Integer.parseInt(time[3]);
		data[4] = Integer.parseInt(input[1]);
		if (input[2].equalsIgnoreCase("UP")) data[5] = Direction.UP;
		else if (input[2].equalsIgnoreCase("DOWN")) data[5] = Direction.DOWN;
		data[6] = Integer.parseInt(input[3]);

		return data;
	}

	/**
	 * Create packet data for sending.
	 *
	 * @param packetType The type of packet (ACK, CMD, DATA, ERROR).
	 * @param instruction The instruction or data to include in the packet.
	 * @return The byte array representing the packet data.
	 */
	public byte[] createPacketData(int packetType, String instruction) {

		String data;

		// error
		if (packetType == 0) {
			data = "\0" + ERROR + "\0" + instruction + "\0";
		}
		// ack
		else if (packetType == 1) {
			data = "\0" + ACK + "\0" + instruction + "\0";
		}
		// cmd
		else if (packetType == 2) {
			data = "\0" + CMD + "\0" + instruction + "\0";
		}
		// data
		else {
			data = "\0" + DATA + "\0" + instruction + "\0";
		}

		return data.getBytes();
	}

	/**
	 * Read packet data from the received message.
	 *
	 * @param msg The received message as a byte array.
	 * @return An array of strings representing the packet data.
	 */
	public String[] readPacketData(byte[] msg) {
		// Converts byte array into string array using default charset.
		// data[0] is the header, data[1] is the data or command (ex "0x10")
		String data = new String(msg);
		String[] strArray;
		strArray = data.replaceFirst("\0", "").split("\0");

		return strArray;
	}

	/**
	 * Receive a datagram packet.
	 *
	 * @param socket The socket to receive the packet on.
	 * @param msg    The byte array to store the received message.
	 * @return The received DatagramPacket.
	 */
	public DatagramPacket receive(DatagramSocket socket, byte[] msg) {
		DatagramPacket packet = new DatagramPacket(msg, msg.length);

		// Block until a datagram packet is received
		try {
			socket.receive(packet);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return packet;
	}

	/**
	 * Create a service request and send it to the scheduler.
	 *
	 * @param start     The starting floor.
	 * @param dest      The destination floor.
	 * @param direction The direction of the elevator.
	 * @return The byte array representing the service request.
	 */
	public byte[] createServiceRequest(int start, int dest, Direction direction) {

		byte[] msg = new byte[100];
		String message = "00:00:00.0 " + start + " " + direction + " " + dest;
		msg = createPacketData(DATA, message);

		if (direction == Direction.UP) floors.get(start).setUpLampOn();
		else if (direction == Direction.DOWN) floors.get(start).setDownLampOn();

		System.out.println("Floor Subsystem: Sending elevator request to go from floor " +
				start + " to " + dest + ", heading " + direction + ". Turning direction lamp on.");

		return msg;
	}

	/**
	 * Send a byte array message to the specified port.
	 *
	 * @param msg  The message to send.
	 * @param port The destination port.
	 */
	public void send(byte[] msg, int port) {

		// Create a service request message
		try {
			sendPacket = new DatagramPacket(msg, msg.length,
					InetAddress.getLocalHost(), port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}

		// Send the service request message
		try {
			sendReceiveSocket.send(sendPacket);

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println(String.format("Floor Subsystem: Sending packet (string >> %s, byte array >> %s ).",
				new String(sendPacket.getData()), sendPacket.getData()));
	}

	/**
	 * Send a service request and wait for acknowledgment before listening begins.
	 *
	 * @param start     The starting floor.
	 * @param destination      The destination floor.
	 * @param dir The direction of the elevator.
	 */
	public void sendServiceRequest(int start, int destination, Direction dir) {

		byte[] buffer = new byte[100];
		byte[] response = new byte[100];
		String[] msg = new String[2];
		String[] data = new String[2];
		String[] acknowledgment = new String[2];
		buffer = createPacketData(CMD, "0x10");
		send(buffer, SCHEDPORT);
		System.out.println("Floor Subsystem: Requesting to send elevator input. Waiting for acknowledgment...");
		receive(sendReceiveSocket, buffer);
		msg = readPacketData(buffer);
		if (Integer.parseInt(msg[0]) == ACK) {
			if (msg[1].equals("0x10")) {
				System.out.println("Floor Subsystem: Acknowledgment received. Sending input to Scheduler");
				response = createServiceRequest(start, destination, dir);
				send(response, SCHEDPORT);
				System.out.println("Waiting for acknowledgment of data packet...");
				receive(sendReceiveSocket, buffer);
				data = readPacketData(buffer);
				acknowledgment = readPacketData(response);
				System.out.println("Floor Subsystem: Data packet acknowledged. Scheduler data is: " + data[1]);
			}
		}
	}

	/**
	 * Respond to an incoming CMD message.
	 *
	 * @param msg           The CMD message received.
	 * @param temporaryPort The temporary port to send the acknowledgment.
	 */
	public void cmdRequest(String[] msg, int temporaryPort) {

		byte[] buffer = new byte[100];
		byte[] response = new byte[100];
		String[] data = new String[2];
		String direction = "";
		if (Integer.parseInt(msg[0]) == CMD) {
			if (msg[1].equals("0x11")) {
				System.out.println("Floor Subsystem: Elevator departure message received. Sending acknowledgment");
				response = createPacketData(ACK,"0x11");
				send(response, temporaryPort);
				System.out.println("Waiting for floor number...");
				receive(sendReceiveSocket, buffer);
				data = readPacketData(buffer);
				if (currentDirection == Direction.UP) {
					floors.get(Integer.parseInt(data[1])).setUpLampOff();
					direction = "up";
				}
				else if (currentDirection == Direction.DOWN) {
					floors.get(Integer.parseInt(data[1])).setDownLampOff();
					direction = "down";
				}
				System.out.printf("Floor Subsystem: Floor number received. Turning %s direction lamp off for floor %d and sending acknowledgment \n", direction, Integer.parseInt(data[1]));
				response = createPacketData(ACK,data[1]);
				send(response, temporaryPort);
			}
		}
	}

	/**
	 * Run the floor subsystem.
	 */
	public void running() {
		boolean listening = true;
		byte[] buffer = new byte[100];
		String[] data = new String[2];
		int temporaryPort = 0;

		// Send service request and wait for acknowledgment before listening begins
		currentDirection = Direction.UP;
		sendServiceRequest(1, 4, Direction.UP);
		requestCount++;

		while (listening) {
			try {
				temporaryPort = receive(sendReceiveSocket, buffer).getPort();
				data = readPacketData(buffer);
				if (Integer.parseInt(data[0]) == CMD) {
					cmdRequest(data, temporaryPort);
					if (requestCount < 5) {
						if (requestCount == 1) {
							currentDirection = Direction.DOWN;
							sendServiceRequest(4, 3, Direction.DOWN);
						}
						if (requestCount == 2) {
							currentDirection = Direction.UP;
							sendServiceRequest(2, 4, Direction.UP);
						}
						if (requestCount == 3) {
							currentDirection = Direction.UP;
							sendServiceRequest(1, 3, Direction.UP);
						}
						if (requestCount == 4) {
							currentDirection = Direction.DOWN;
							sendServiceRequest(3, 1, Direction.DOWN);
						}
						requestCount++;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				listening = false;
			}
		}
	}

	/**
	 * The main method to run the floor subsystem.
	 *
	 * @param args Command-line arguments (not used).
	 * @throws IOException If an I/O error occurs.
	 */
	public static void main(String[] args) throws IOException {

		FloorControl floorSubsystem = new FloorControl();

		for (int i = 0; i < floorSubsystem.floorTotal; i++) {
			if (i == floorSubsystem.floorMin) floors.add(new Floor(1, i+1, false, true));
			else if (i == floorSubsystem.floorTotal - floorSubsystem.floorMin) floors.add(new Floor(1, i+1, true, false));
			else floors.add(new Floor(1, i+1, false, false));
		}

		floorSubsystem.running();
	}
}
