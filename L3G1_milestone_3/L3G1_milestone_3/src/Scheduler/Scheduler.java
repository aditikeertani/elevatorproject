package Scheduler;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Scheduler class represents the interface for the elevator system. It manages
 * incoming requests from the floor subsystem and dispatches them to the
 * appropriate elevator handler.
 *
 * @author Aditi Keertani, Ryan Capstick, Oluwatobi Olwookere
 */
public class Scheduler {
	
	/* ## HEADER AND COMMAND IDENTIFIERS ## */
	public static final String ACK = "1";
	public static final String CMD = "2";
	public static final String DATA = "3";
	public static final String ERROR = "0";
	
	public static final String FLOOR_BUTTON = "0x10";						// button was pressed on floor
	public static final String ELEVATOR_ARRIVED = "0x11"; 					// elevator arrived
	public static final String UP_PICKUP = "0x33";							// going up to pick up
	public static final String UP_DROPOFF = "0x32"; 						// going up dropoff
	public static final String DOWN_PICKUP = "0x31";						// going down to pickup
	public static final String DOWN_DROPOFF = "0x30";						// going down to dropoff
	public static final String DOOR_OPEN = "0x3A";							// open car door
	public static final String DOOR_CLOSE = "0x3B";							// close car door
	public static final String STOP = "0x3C";								// stop elevator car

	final int HOSTPORT = 8008;
	final int EPORT1 = 3137;
	final int EPORT2 = 3237;
	final int EPORT3 = 3337;
	final int EPORT4 = 3437;
	final int FPORT = 6520;
	DatagramSocket hostSocket;												// receiving from floor system on this socket
	DatagramSocket floorSocket;												// sending to floor on this socket
	
	public static final int FLOORS = 22;									// floors are 1-22
	private BlockingQueue<String> upQueue1;										// queue with up requests (elevator 1)
	private BlockingQueue<String> downQueue1;									// queue with down requests (elevator 1)
	private BlockingQueue<String> upQueue2;										// queue with up requests (elevator 2)
	private BlockingQueue<String> downQueue2;									// queue with down requests (elevator 2)
	private BlockingQueue<String> upQueue3;										// queue with up requests (elevator 3)
	private BlockingQueue<String> downQueue3;									// queue with down requests (elevator 3)
	private BlockingQueue<String> upQueue4;										// queue with up requests (elevator 4)
	private BlockingQueue<String> downQueue4;									// queue with down requests (elevator 4)
	
	// objects performing elevator movement calculations for each elevator in separate thread
	private Host handler1;
	private Host handler2;
	private Host handler3;
	private Host handler4;
	/* ## ---------------------------- ## */


	/**
	 * Constructor for the Scheduler class.
	 */
	public Scheduler() {
		
		try {
			hostSocket = new DatagramSocket(HOSTPORT);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		upQueue1 = new ArrayBlockingQueue<String>(100);
		downQueue1 = new ArrayBlockingQueue<String>(100);
		upQueue2 = new ArrayBlockingQueue<String>(100);
		downQueue2 = new ArrayBlockingQueue<String>(100);
		upQueue3 = new ArrayBlockingQueue<String>(100);
		downQueue3 = new ArrayBlockingQueue<String>(100);
		upQueue4 = new ArrayBlockingQueue<String>(100);
		downQueue4 = new ArrayBlockingQueue<String>(100);
		
		handler1 = new Host(EPORT1, FPORT, upQueue1, downQueue1, 1, "IDLE", 1);
		handler2 = new Host(EPORT2, FPORT, upQueue2, downQueue2, 1, "IDLE", 2);
		handler3 = new Host(EPORT3, FPORT, upQueue3, downQueue3, 10, "IDLE", 3);
		handler4 = new Host(EPORT4, FPORT, upQueue4, downQueue4, 20, "IDLE", 4);
	}


	/**
	 * Runs the main process of the scheduler, continuously listening for incoming
	 * requests and dispatching them to the appropriate elevator handler.
	 */
	public void run() {
		System.out.println("main: running.");
		boolean listening = true;
		
		byte[] buffer = new byte[8];
		DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);			// received packet		
		
		handler1.start();
		handler2.start();
		handler3.start();
		handler4.start();
		
		while (listening) {
			try {
				hostSocket.receive(rPacket);
				System.out.println(String.format("main: received packet ( string >> %s, byte array >> %s ).\n", new String(rPacket.getData()), rPacket.getData()));
				
				// handling packet
				String[] rPacketParsed = parsePacket(rPacket.getData());
				
				// CMD
				if (rPacketParsed[0].equals(CMD)) {					
					
					handleFloorCommand(rPacketParsed[1], rPacket.getPort());				
				}
				// something else
				else {
					System.out.println("main: unknown packet.");
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				listening = false;
			}			
		}
		hostSocket.close();
		return;
	}


	/**
	 * Method to handle command received from floor. Computes the suitability of each elevator to service a floor request, adds the request to the corresponding elevator.
	 *
	 * @param cmd the command received from the floor
	 * @param port the port number
	 */
	public void handleFloorCommand(String cmd, int port) {
		
		byte[] data = new byte[1024];
		DatagramPacket aPacket = createPacket(ACK, cmd, port);
		DatagramPacket dPacket = new DatagramPacket(data, data.length);
				
		if (cmd.equals(FLOOR_BUTTON)) {
			try {
				floorSocket = new DatagramSocket();
				System.out.println("main: acking.");
				floorSocket.send(aPacket);
				
				// get data
				hostSocket.receive(dPacket);
				
				// handling packet
				String[] dPacketParsed = parsePacket(dPacket.getData());				
				System.out.println(String.format("main: received data ( string >> %s, byte array >> %s ).\n", new String(dPacket.getData()), dPacket.getData()));
				System.out.println(Arrays.toString(dPacketParsed));
				
				if (dPacketParsed[0].equals(DATA)) {
					aPacket = createPacket(ACK, dPacketParsed[1], port);
					System.out.println("main: acking data.");
					floorSocket.send(aPacket);
					
					String[] temp = dPacketParsed[1].split(" ");
					
					int FS1 = calculateSuitability(FLOORS, handler1.currentFloor, Integer.parseInt(temp[1]), handler1.currentDirection, temp[2]);
					int FS2 = calculateSuitability(FLOORS, handler2.currentFloor, Integer.parseInt(temp[1]), handler2.currentDirection, temp[2]);
					int FS3 = calculateSuitability(FLOORS, handler3.currentFloor, Integer.parseInt(temp[1]), handler3.currentDirection, temp[2]);
					int FS4 = calculateSuitability(FLOORS, handler4.currentFloor, Integer.parseInt(temp[1]), handler4.currentDirection, temp[2]);
					int maxFS = Math.max(Math.max(FS1, FS2), Math.max(FS3, FS4));
					
					if (maxFS == FS1) {
						if (temp[2].equals("UP")) {
							upQueue1.add(dPacketParsed[1]);
						}
						else {
							downQueue1.add(dPacketParsed[1]);
						}						
					}
					else if (maxFS == FS2) {
						if (temp[2].equals("UP")) {
							upQueue2.add(dPacketParsed[1]);
						}
						else {
							downQueue2.add(dPacketParsed[1]);
						}						
					}
					else if (maxFS == FS3) {
						if (temp[2].equals("UP")) {
							upQueue3.add(dPacketParsed[1]);
						}
						else {
							downQueue3.add(dPacketParsed[1]);
						}						
					}
					else {
						if (temp[2].equals("UP")) {
							upQueue4.add(dPacketParsed[1]);
						}
						else {
							downQueue4.add(dPacketParsed[1]);
						}						
					}				
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(1);				
			}						
		}
	}


	/**
	 * Calculates the suitability of an elevator to service a request based on its distance and direction.
	 *
	 * @param n the number of floors
	 * @param currentFloor the current floor of the elevator
	 * @param requestedFloor the floor requested by the user
	 * @param currentElevatorDir the current direction of the elevator ("IDLE", "UP", or "DOWN")
	 * @param requestedDir the requested direction by the user ("UP" or "DOWN")
	 * @return the calculated suitability of the elevator
	 */
	public int calculateSuitability(int n, int currentFloor, int requestedFloor, String currentElevatorDir, String requestedDir) {

		int calculated = 0;
		int distance = currentFloor - requestedFloor; // if -ive, call is above, if +ive, call is below

		// current direction of elevator is IDLE
		if (currentElevatorDir.equals("IDLE")) {
			distance = Math.abs(distance);
			calculated = n + 1 - distance;
		}
		// current direction of elevator is DOWN
		else if (currentElevatorDir.equals("DOWN")) {
			if (distance < 0) {
				calculated = 1;
			}
			// request is below and request direction is same as elevator direction
			else if (distance > 0 && requestedDir.equals(currentElevatorDir)) {
				calculated = n + 2 - distance;
			}
			// request is below and request direction is NOT the same as elevator direction
			else if (distance > 0 && !requestedDir.equals(currentElevatorDir)) {
				calculated = n + 1 - distance;
			}
		}
		// current direction of elevator is UP
		else if (currentElevatorDir.equals("UP")) {
			// request is below
			if (distance > 0) {
				calculated = 1;
			}
			// request is above and request direction is same as elevator direction
			else if (distance < 0 && requestedDir.equals(currentElevatorDir)) {
				distance = Math.abs(distance);
				calculated = n + 2 - distance;
			}
			// request is above an request direction is NOT the same as elevator direction
			else if (distance < 0 && !requestedDir.equals(currentElevatorDir)) {
				distance = Math.abs(distance);
				calculated = n + 1 - distance;
			}
		}
		return calculated; // return calculated suitability
	}

	/**
	 * Method to parse a packet. Returns a String array.
	 *
	 * @param bytes the byte array representing the packet
	 * @return a string array containing the parsed data elements
	 */
	public static String[] parsePacket(byte[] bytes) {

		String packet = new String(bytes);
		String[] parsed = packet.replaceFirst("\0", "").split("\0");

		return parsed;
	}

	/**
	 * Creates and returns a DatagramPacket.
	 *
	 * @param packetType the type of packet to create ("0" for error, "1" for ack, "2" for cmd, or any other value for data)
	 * @param ins the instruction or data to include in the packet
	 * @param port the port number to which the packet will be sent
	 * @return a DatagramPacket with the specified data
	 */
	public static DatagramPacket createPacket(String packetType, String ins, int port) {
		String data;
		DatagramPacket packet = null;

		// error
		if (packetType.equals("0")) {
			data = "\0" + ERROR + "\0" + ins + "\0";
		}
		// ack
		else if (packetType.equals("1")) {
			data = "\0" + ACK + "\0" + ins + "\0";
		}
		// cmd
		else if (packetType.equals("2")) {
			data = "\0" + CMD + "\0" + ins + "\0";
		}
		// data
		else {
			data = "\0" + DATA + "\0" + ins + "\0";
		}

		try {
			packet = new DatagramPacket(data.getBytes(), data.getBytes().length, InetAddress.getLocalHost(), port);
		}
		catch (UnknownHostException uhe) {
			System.out.println("unable to create packet (UnknownSchedulerException), exiting.");
			System.exit(1);
		}
		return packet;
	}



	/**
	 * Main method to start the Scheduler and run its main process.
	 *
	 * @param args command-line arguments (not used)
	 */
	public static void main(String[] args) {
		Scheduler scheduler = new Scheduler();
		scheduler.run();
	}

}

/**
 * Thread class to handle elevator movements. Uses a BlockingQueue to monitor new requests.
 */
class Host extends Thread {

	private DatagramSocket eSocket;                     // Socket for communicating with the elevator system
	private int eport;                                  // Port number of the elevator system
	private int fport;                                  // Port number of the floor system
	private BlockingQueue<String> upQueue;              // Queue for up requests
	private BlockingQueue<String> downQueue;            // Queue for down requests

	protected volatile String currentDirection;         // Current direction of the elevator
	protected volatile int currentFloor;                // Current floor of the elevator
	protected volatile int currentDestination;          // Last stop in the current request sequence
	protected int id;                                   // Identifier of the elevator


	/**
	 * Constructor for the ElevatorHandler class.
	 *
	 * @param eport Port number of the elevator system
	 * @param fport Port number of the floor system
	 * @param upQueue Queue for up requests
	 * @param downQueue Queue for down requests
	 * @param currentFloor Current floor of the elevator
	 * @param currentDirection Current direction of the elevator
	 * @param id Identifier of the elevator
	 */
	public Host(int eport, int fport, BlockingQueue<String> upQueue, BlockingQueue<String> downQueue, int currentFloor, String currentDirection, int id) {
		super("ElevatorHandlerThread");
		try {
			eSocket = new DatagramSocket();
			this.eport = eport;
			this.fport = fport;
			this.upQueue = upQueue;
			this.downQueue = downQueue;
			this.currentDirection = currentDirection;
			this.currentFloor = currentFloor;
			this.id = id;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Main thread process. Executed when the thread starts.
	 */
	@Override
	public void run() {

		boolean running = true;
		String pIns, dIns;
		String srcFloor, direction;
		String[] parsedData;
		System.out.println("ElevatorHandler: A new thread is running.");

		while (running) {

			String request = upQueue.poll();

			if (request == null) {
				request = downQueue.poll();
			}

			if (request != null) {
				parsedData = request.split(" ");
				srcFloor = parsedData[1];
				direction = parsedData[2];

				/*
				 *  Determining which way the elevator needs to go to pickup and drop off.
				 *  DATA packet format: '\3\"time src_floor direction dest_floor"\'
				 *
				 *  If the elevator is below src_floor, it needs to move up.
				 *  If the elevator is above src_floor, it needs to move down.
				 *
				 *  If dest_floor > src_floor, go up after pickup.
				 *  If dest_floor < src_floor, go down after pickup.
				 */
				if (Integer.parseInt(srcFloor) > currentFloor) {
					pIns = Scheduler.UP_PICKUP;
					currentDirection = "UP";
				} else if (Integer.parseInt(srcFloor) < currentFloor) {
					pIns = Scheduler.DOWN_PICKUP;
					currentDirection = "DOWN";
				} else {
					pIns = Scheduler.STOP;    // The elevator is already there
				}
				performPickup(pIns, request);

				// Drop off direction
				if (direction.equals("UP")) {
					dIns = Scheduler.UP_DROPOFF;
					currentDirection = "UP";
				} else { // Down
					dIns = Scheduler.DOWN_DROPOFF;
					currentDirection = "DOWN";
				}
				performDropoff(dIns, request);
			} else {
				currentDirection = "IDLE";
				continue;
			}
		}
		eSocket.close();
		return;
	}
	/**
	 * Method performing the bulk of the socket operations for a pickup.
	 *
	 * @param ins     Instruction for the elevator movement
	 * @param request Information about the request
	 */
	public void performPickup(String ins, String request) {

		String srcFloor, destFloor;
		String[] parsedData;
		boolean moving = !ins.equals(Scheduler.STOP); // If the elevator is already there, no need for positional updates

		parsedData = request.split(" ");
		srcFloor = parsedData[1];
		destFloor = parsedData[3];

		byte[] buffer = new byte[8];
		DatagramPacket cPacket = Scheduler.createPacket(Scheduler.CMD, ins, eport);
		DatagramPacket aPacket = new DatagramPacket(buffer, buffer.length);
		DatagramPacket dPacket = Scheduler.createPacket(Scheduler.DATA, destFloor, eport);
		DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);
		String[] rPacketParsed;

		System.out.println(String.format("ElevatorHandler-%d: Forwarding command packet (string >> %s, byte array >> %s).", this.id, new String(cPacket.getData()), cPacket.getData()));

		try {
			// Send command
			eSocket.send(cPacket);

			// Listen for acknowledgment
			eSocket.receive(aPacket);

			// Parsing acknowledgment
			String[] aPacketParsed = Scheduler.parsePacket(aPacket.getData());
			System.out.println(String.format("ElevatorHandler-%d: Received acknowledgment (string >> %s, byte array >> %s).", this.id, new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));

			// Send elevator destination
			System.out.println(String.format("ElevatorHandler-%d: Sending elevator button (string >> %s, byte array >> %s).", this.id, new String(dPacket.getData()), dPacket.getData()));
			eSocket.send(dPacket);

			// Listen for acknowledgment
			eSocket.receive(aPacket);

			// Parsing acknowledgment
			aPacketParsed = Scheduler.parsePacket(aPacket.getData());
			System.out.println(String.format("ElevatorHandler-%d: Received acknowledgment (string >> %s, byte array >> %s).", this.id, new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));

			// Positional updates
			while (moving) {
				eSocket.receive(rPacket);

				rPacketParsed = Scheduler.parsePacket(rPacket.getData());
				System.out.println(String.format("ElevatorHandler-%d: Received positional update (string >> %s, byte array >> %s).", this.id, new String(rPacket.getData()), rPacket.getData()));
				System.out.println(Arrays.toString(rPacketParsed));

				currentFloor = Integer.parseInt(rPacketParsed[1]);
				// If the elevator is at the required floor, then stop
				if (rPacketParsed[1].equals(srcFloor)) {
					cPacket = Scheduler.createPacket(Scheduler.CMD, Scheduler.STOP, eport);
					System.out.println(String.format("ElevatorHandler-%d: Sending stop (string >> %s, byte array >> %s).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
					moving = false;
				} else {
					cPacket = Scheduler.createPacket(Scheduler.ACK, rPacketParsed[1], eport);
					System.out.println(String.format("ElevatorHandler-%d: Sending continue (string >> %s, byte array >> %s).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
				}
				eSocket.send(cPacket);

				if (!moving) {
					// Listen for acknowledgment to stop
					eSocket.receive(aPacket);

					// Parsing acknowledgment
					aPacketParsed = Scheduler.parsePacket(aPacket.getData());
					System.out.println(String.format("ElevatorHandler-%d: Received acknowledgment (string >> %s, byte array >> %s).", this.id, new String(aPacket.getData()), aPacket.getData()));
					System.out.println(Arrays.toString(aPacketParsed));
				}
			}

			// Send update to floor that the elevator has arrived
			sendPositionToFloor(srcFloor);

			// Sending open door
			cPacket = Scheduler.createPacket(Scheduler.CMD, Scheduler.DOOR_OPEN, eport);
			System.out.println(String.format("ElevatorHandler-%d: Sending open door (string >> %s, byte array >> %s).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
			eSocket.send(cPacket);

			// Listen for acknowledgment to open
			eSocket.receive(aPacket);

			// Parsing acknowledgment
			aPacketParsed = Scheduler.parsePacket(aPacket.getData());
			System.out.println(String.format("ElevatorHandler-%d: Received acknowledgment (string >> %s, byte array >> %s).", this.id, new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));

			// Sending close door
			cPacket = Scheduler.createPacket(Scheduler.CMD, Scheduler.DOOR_CLOSE, eport);
			System.out.println(String.format("ElevatorHandler-%d: Sending close door (string >> %s, byte array >> %s).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
			eSocket.send(cPacket);

			// Listen for acknowledgment to close
			eSocket.receive(aPacket);

			// Parsing acknowledgment
			aPacketParsed = Scheduler.parsePacket(aPacket.getData());
			System.out.println(String.format("ElevatorHandler-%d: Received acknowledgment - done (string >> %s, byte array >> %s).", this.id, new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));
		} catch (Exception e) {
			System.out.println("ElevatorHandler: Unable to communicate with the elevator system, aborting request.");
			e.printStackTrace();
			return;
		}
	}

	/**
	 * Method performing the bulk of the socket operations for a drop-off.
	 *
	 * @param ins     Instruction for the elevator movement
	 * @param request Information about the request
	 */
	public void performDropoff(String ins, String request) {
		String destFloor;
		String[] parsedData;
		boolean moving = true;

		parsedData = request.split(" ");
		destFloor = parsedData[3];

		byte[] buffer = new byte[8];
		DatagramPacket cPacket = Scheduler.createPacket(Scheduler.CMD, ins, eport);
		DatagramPacket aPacket = new DatagramPacket(buffer, buffer.length);
		DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);
		String[] rPacketParsed;

		System.out.println(String.format("ElevatorHandler-%d: Forwarding command packet (string >> %s, byte array >> %s).", this.id, new String(cPacket.getData()), cPacket.getData()));

		try {
			// Send command
			eSocket.send(cPacket);

			// Listen for acknowledgment
			eSocket.receive(aPacket);

			// Parsing acknowledgment
			String[] aPacketParsed = Scheduler.parsePacket(aPacket.getData());
			System.out.println(String.format("ElevatorHandler-%d: Received acknowledgment (string >> %s, byte array >> %s).", this.id, new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));

			// Positional updates
			while (moving) {
				eSocket.receive(rPacket);

				rPacketParsed = Scheduler.parsePacket(rPacket.getData());
				System.out.println(String.format("ElevatorHandler-%d: Received positional update (string >> %s, byte array >> %s).", this.id, new String(rPacket.getData()), rPacket.getData()));
				System.out.println(Arrays.toString(rPacketParsed));

				currentFloor = Integer.parseInt(rPacketParsed[1]);
				// If the elevator is at the required floor, then stop
				if (rPacketParsed[1].equals(destFloor)) {
					cPacket = Scheduler.createPacket(Scheduler.CMD, Scheduler.STOP, eport);
					System.out.println(String.format("ElevatorHandler-%d: Sending stop (string >> %s, byte array >> %s).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
					moving = false;
				} else {
					cPacket = Scheduler.createPacket(Scheduler.ACK, rPacketParsed[1], eport);
					System.out.println(String.format("ElevatorHandler-%d: Sending continue (string >> %s, byte array >> %s).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
				}
				eSocket.send(cPacket);
			}

			// Listen for acknowledgment to stop
			eSocket.receive(aPacket);

			// Parsing acknowledgment
			aPacketParsed = Scheduler.parsePacket(aPacket.getData());
			System.out.println(String.format("ElevatorHandler-%d: Received acknowledgment (string >> %s, byte array >> %s).", this.id, new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));

			// Sending open door
			cPacket = Scheduler.createPacket(Scheduler.CMD, Scheduler.DOOR_OPEN, eport);
			System.out.println(String.format("ElevatorHandler-%d: Sending open door (string >> %s, byte array >> %s).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
			eSocket.send(cPacket);

			// Listen for acknowledgment to open
			eSocket.receive(aPacket);

			// Parsing acknowledgment
			aPacketParsed = Scheduler.parsePacket(aPacket.getData());
			System.out.println(String.format("ElevatorHandler-%d: Received acknowledgment (string >> %s, byte array >> %s).", this.id, new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));

			// Sending close door
			cPacket = Scheduler.createPacket(Scheduler.CMD, Scheduler.DOOR_CLOSE, eport);
			System.out.println(String.format("ElevatorHandler-%d: Sending close door (string >> %s, byte array >> %s).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
			eSocket.send(cPacket);

			// Listen for acknowledgment to close
			eSocket.receive(aPacket);

			// Parsing acknowledgment
			aPacketParsed = Scheduler.parsePacket(aPacket.getData());
			System.out.println(String.format("ElevatorHandler-%d: Received acknowledgment - done (string >> %s, byte array >> %s).", this.id, new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));

			currentDirection = "IDLE";
		} catch (Exception e) {
			System.out.println(String.format("ElevatorHandler-%d: Unable to communicate with the elevator system, aborting request.", this.id));
			e.printStackTrace();
			return;
		}
	}

	/**
	 * Sends position information to the floor system to turn off the corresponding lamp.
	 *
	 * @param floor The floor number where the elevator has arrived
	 */
	public void sendPositionToFloor(String floor) {
		byte[] buffer = new byte[8];
		DatagramPacket sPacket = Scheduler.createPacket(Scheduler.CMD, Scheduler.ELEVATOR_ARRIVED, fport);
		DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);
		String[] rPacketParsed;

		try {
			DatagramSocket tempSocket = new DatagramSocket();
			System.out.println(String.format("ElevatorHandler-%d: Sending position update to floor (string >> %s, byte array >> %s).\n", this.id, new String(sPacket.getData()), sPacket.getData()));
			tempSocket.send(sPacket);

			// Listen for acknowledgment
			tempSocket.receive(rPacket);

			// Parsing acknowledgment
			rPacketParsed = Scheduler.parsePacket(rPacket.getData());
			System.out.println(String.format("ElevatorHandler-%d: Received acknowledgment (string >> %s, byte array >> %s).", this.id, new String(rPacket.getData()), rPacket.getData()));
			System.out.println(Arrays.toString(rPacketParsed));

			// Sending floor number
			sPacket = Scheduler.createPacket(Scheduler.DATA, floor, fport);
			System.out.println(String.format("ElevatorHandler-%d: Sending floor number (string >> %s, byte array >> %s).\n", this.id, new String(sPacket.getData()), sPacket.getData()));
			tempSocket.send(sPacket);

			// Listen for acknowledgment
			tempSocket.receive(rPacket);

			// Parsing acknowledgment
			rPacketParsed = Scheduler.parsePacket(rPacket.getData());
			System.out.println(String.format("ElevatorHandler-%d: Received acknowledgment - done (string >> %s, byte array >> %s).", this.id, new String(rPacket.getData()), rPacket.getData()));
			System.out.println(Arrays.toString(rPacketParsed));

			tempSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}


