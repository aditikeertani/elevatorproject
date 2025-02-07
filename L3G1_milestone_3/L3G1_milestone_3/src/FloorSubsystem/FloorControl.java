package FloorSubsystem;


import java.net.*;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class represents the FloorSubsystem side for a simple echo server based on UDP/IP.
 * The FloorSubsystem sends a character string to the echo server, then waits for the server
 * to send it back to the FloorSubsystem.
 *
 * @author Ryan CapStick
 * @co-author Oluwatobi Olwookere
 */

public class FloorControl {

	DatagramPacket sendPacket;
	DatagramPacket receivePacket;
	DatagramSocket floorSocket;
	final int ACK = 1;
	final int CMD = 2;
	final int DATA = 3;
	final int ERROR = 0;

	final int FLOORPORT = 6520;					// port of floor subsystem
	final int SCHEDPORT = 8008;					// port of scheduler
	
	public enum Direction {
		UP,
		DOWN,
		IDLE
	}
	
	int numFloors;
	int upperFloor;
	int lowerFloor;
	
	int floorMin = 0;
	int floorTotal = 22;
	int elevatorTotal = 4;
	
	Direction currentDirection = Direction.IDLE;
	int requestCount = 0;
	
	ArrayList<Floor> floors = new ArrayList<Floor>();

	/**
	 * Constructor for FloorControl class.
	 */
	public FloorControl() {
			
        try {
        	floorSocket = new DatagramSocket(FLOORPORT);
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }
		
	}



	/**
	 * Places string in a byte array for sending.
	 *
	 * @param packetType The type of packet (ACK, CMD, DATA, ERROR).
	 * @param ins The array of data.
	 * @return The byte array.
	 */
	public byte[] createPacketData(int packetType, String ins) {
		
		String data;
		
		// error
		if (packetType == 0) {			
			data = "\0" + ERROR + "\0" + ins + "\0";					
		}
		// ack
		else if (packetType == 1) {			
			data = "\0" + ACK + "\0" + ins + "\0";
		}
		// cmd
		else if (packetType == 2) {
			data = "\0" + CMD + "\0" + ins + "\0";
		}
		// data
		else {
			data = "\0" + DATA + "\0" + ins + "\0";
		}		
		
		return data.getBytes();
	}

	/**
	 * Converts byte array into string array using default charset.
	 *
	 * @param msg The byte array.
	 * @return The string array.
	 */
	public String[] readPacketData(byte[] msg) {
		// Converts byte array into string array using default charset.
		// data[0] is the header, data[1] is the data or command (ex "0x10")
		String data = new String(msg);
		String[] str;
		str = data.replaceFirst("\0", "").split("\0");
		
		return str;
	}


	/**
	 * Set a datagram packet and block until message is received.
	 *
	 * @param socket The DatagramSocket object.
	 * @param msg The byte array for the message.
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
	 * Create a service request to send to the scheduler, setting lamps as necessary.
	 *
	 * @param time The current time.
	 * @param start The starting floor.
	 * @param destination The destinationination floor.
	 * @param direction The direction.
	 * @return The message byte array.
	 */
	public byte[] createServiceRequest(String time, int start, int destination, Direction direction) {
		
		byte msg[] = new byte[100];
		String message = time + " " + start + " " + direction + " " + destination;
		msg = createPacketData(DATA, message);

		if (direction == direction.UP) floors.get(start).setUpLampOn();
		else if (direction == direction.DOWN) floors.get(start).setDownLampOn();
		
		System.out.println("Floor Subsystem: Sending elevator request to go from floor " + 
								start + " to " + destination + ", heading " + direction + ". Turning direction lamp on.");
		 
		return msg;
	}

	/**
	 * Send a byte array message to the specified port.
	 *
	 * @param msg  The message to send.
	 * @param port The destinationination port.
	 */
	public void send(byte[] msg, int port, DatagramSocket socket) {
		
		//create a service request message
		try {
	        sendPacket = new DatagramPacket(msg, msg.length,
	                                        InetAddress.getLocalHost(), port);
	     } catch (UnknownHostException e) {
	        e.printStackTrace();
	        System.exit(1);
	     }
		 
		//send the service request message
		try {
	        socket.send(sendPacket);

	     } catch (IOException e) {
	        e.printStackTrace();
	        System.exit(1);
	     }
		 //System.out.println("Floor Subsystem: Sending ");
		 System.out.println(String.format("Floor Subsystem: Sending packet ( string >> %s, byte array >> %s ).", 
				 new String(sendPacket.getData()), sendPacket.getData()));
		 
		
	}


	/**
	 * Send a service request and wait for acknowledgment before listening begins.
	 *
	 * @param start     The starting floor.
	 * @param destination    The destination floor.
	 * @param dir The direction of the elevator.
	 */
	public void sendServiceRequest(String time, int start, int destination, Direction dir, DatagramSocket socket) {
		
		byte[] buffer = new byte[100];
		byte[] response = new byte[100];
		String[] msg = new String[2];
		String[] data = new String[2];
		String[] acknowledgment = new String[2];
		buffer = createPacketData(CMD, "0x10");
		send(buffer, SCHEDPORT, socket);
		System.out.println("Floor Subsystem: Requesting to send elevator input. Waiting for acknowledgment...");
//		Block until acknowledgment message is received
		receive(socket, buffer);
		msg = readPacketData(buffer);
		if (Integer.parseInt(msg[0]) == ACK) {
			if (msg[1].equals("0x10")) {
				System.out.println("Floor Subsystem: CMD acknowledgment received. Sending input to Scheduler");
				response = createServiceRequest(time, start, destination, dir);
				send(response, SCHEDPORT, socket);
				System.out.println("Floor Subsystem: Waiting for acknowledgment of data packet...");
//				Block until acknowledgment message is received
				receive(socket, buffer);
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
				send(response, temporaryPort, floorSocket);
				System.out.println("Floor Subsystem: Waiting for floor number...");
//				Block until Scheduler sends floor number elevator has departed from
				receive(floorSocket, buffer);
				data = readPacketData(buffer);
				if (floors.get(Integer.parseInt(data[1])).getUpLampStatus()) {
					floors.get(Integer.parseInt(data[1])).setUpLampOff();
					direction = "up";
				}
				else if (floors.get(Integer.parseInt(data[1])).getDownLampStatus()) {
					floors.get(Integer.parseInt(data[1])).setDownLampOff();
					direction = "down";
				}
				System.out.printf("Floor Subsystem: Floor number received. Turning %s direction lamp off for floor %d and sending acknowledgment \n", direction, Integer.parseInt(data[1]));
				response = createPacketData(ACK,data[1]);
				send(response, temporaryPort, floorSocket);
			}
		}
	}
	
	/**
	 * Listen for messages sent from the scheduler
	 */
	public void running() {
		boolean listening = true;
		byte[] buffer = new byte[100];
		String[] data = new String[2];
		int temporaryPort = 0;
		
		while (listening) {
			try {
//				Block until Scheduler starts exchange signaling elevator has arrived
				temporaryPort = receive(floorSocket, buffer).getPort();
				data = readPacketData(buffer);
				if (Integer.parseInt(data[0]) == CMD) {
					cmdRequest(data, temporaryPort);
					
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
	public static void main(String args[]) throws IOException {
		
		FloorControl floorSubsystem = new FloorControl();
		Thread inputHandler;
		
//		Add floors to the floorSubsystem
		for (int i = 0; i < floorSubsystem.floorTotal; i++) {
			if (i == floorSubsystem.floorMin) floorSubsystem.floors.add(new Floor(1, i+1, false, true));
			else if (i == floorSubsystem.floorTotal - floorSubsystem.floorMin) floorSubsystem.floors.add(new Floor(1, i+1, true, false));
			else floorSubsystem.floors.add(new Floor(1, i+1, false, false));
		}
		
		System.out.println(System.getProperty("user.dir"));
//		Create an input handler for reading user requests from a text file, and sending the 
//		requests to the scheduler on a timed basis
		inputHandler = new Thread(new HandleUserInput(floorSubsystem), "User Input Handler");
		inputHandler.start();
		
		System.out.println("Starting Floor Subsystem");
		floorSubsystem.running();
		
   }
}



