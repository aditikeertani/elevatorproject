package scheduler;

import java.awt.Color;
import java.net.*;
import java.util.*;

import static utils.Utilities.*;


/**
 * Scheduler class represents the interface for the elevator system. It manages
 * incoming requests from the floor subsystem and dispatches them to the
 * appropriate elevator handler.
 *
 * @author Aditi Keertani, Ryan Capstick, Oluwatobi Olwookere
 */
public class Scheduler {

	/* ## NETWORK INFO ## */
	final int HOSTPORT = 8008;
	final int EPORT1 = 3137;
	final int EPORT2 = 3237;
	final int EPORT3 = 3337;
	final int EPORT4 = 3437;
	final int FPORT = 6520;
	DatagramSocket hostSocket;												// receiving from floor system on this socket
	DatagramSocket floorSocket;												// sending to floor on this socket
	/* ## ------------ ## */

	/* ## ELEVATOR SYSTEM CONSTITUENTS ## */
	public static final int FLOORS = 22;									// floors are 1-22
	private List<String> pickList1;											// List containing requests (elevator 1)
	private List<String> pickList2;											// List containing requests (elevator 2)
	private List<String> pickList3;											// List containing requests (elevator 3)
	private List<String> pickList4;											// List containing requests (elevator 4)

	// objects performing elevator movement calculations for each elevator in separate thread
	private Host handler1;
	private Host handler2;
	private Host handler3;
	private Host handler4;

	private ElevatorGUI window;													// GUI class
	private int[][] guiLamps;												// lamps to light in GUI
	/* ## ---------------------------- ## */


	/**
	 * Constructor. Creates four handlers.
	 */
	public Scheduler() {

		try {
			hostSocket = new DatagramSocket(HOSTPORT);
			hostSocket.setSoTimeout(500);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		pickList1 = Collections.synchronizedList(new ArrayList<String>());
		pickList2 = Collections.synchronizedList(new ArrayList<String>());
		pickList3 = Collections.synchronizedList(new ArrayList<String>());
		pickList4 = Collections.synchronizedList(new ArrayList<String>());

		// initialize with corresponding elevator port, floor port, request List, starting floor, status, ID
		handler1 = new Host(EPORT1, FPORT, pickList1, 1, "IDLE", 1);
		handler2 = new Host(EPORT2, FPORT, pickList2, 1, "IDLE", 2);
		handler3 = new Host(EPORT3, FPORT, pickList3, 10, "IDLE", 3);
		handler4 = new Host(EPORT4, FPORT, pickList4, 20, "IDLE", 4);

		window = new ElevatorGUI();
		guiLamps = new int[4][22];
		System.out.println(guiLamps);
	}


	/**
	 * Main process. Creates the handlers and listens for service requests.
	 */
	public void run() {
		System.out.println("Main Process is Running.");
		boolean listening = true;
		boolean blink = true;

		byte[] buffer = new byte[8];
		DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);			// received packet

		handler1.start();
		handler2.start();
		handler3.start();
		handler4.start();

		// receive requests from FloorSubsystem
		while (listening) {
			try {
				int cFloor1 = handler1.currentFloor;
				int cFloor2 = handler2.currentFloor;
				int cFloor3 = handler3.currentFloor;
				int cFloor4 = handler4.currentFloor;

				for (int i = 0; i<22; i++) {
					for(int j=0; j<4; j++) {
						if (/*blink & */guiLamps[j][i] == 1) {
							window.getElevatorFloors(i, j).setBackground(Color.MAGENTA);//lamp on
						}
						else {
							window.getElevatorFloors(i, j).setBackground(Color.BLUE);//reset all floors
						}
					}
				}

				// elevator 1
				if (!handler1.status.equals("SUSPENDED")) {
					window.getElevatorDirections(0, 0).setBackground(Color.WHITE);//reset UP directions
					window.getElevatorDirections(1, 0).setBackground(Color.WHITE);//reset DOWN directions
					if (!handler1.liveDirection.equals("")) {
						window.getElevatorDirections((handler1.liveDirection.equals("UP") ? 0 : 1), 0).setBackground(Color.GREEN);
					}
					window.getElevatorFloors(22 - cFloor1, 0).setBackground(Color.BLACK);//elevator on this floor is shown black
					if (handler1.pickingUp) {guiLamps[0][22 - cFloor1] = 0;}
				}
				else {
					if (blink) {
						window.getElevatorFloors(22 - cFloor1, 0).setBackground(Color.RED);//elevator stuck
					}
				}

				// elevator 2
				if (!handler2.status.equals("SUSPENDED")) {
					window.getElevatorDirections(0, 1).setBackground(Color.WHITE);//reset UP directions
					window.getElevatorDirections(1, 1).setBackground(Color.WHITE);//reset DOWN directions
					if (!handler2.liveDirection.equals("")) {
						window.getElevatorDirections((handler2.liveDirection.equals("UP") ? 0 : 1), 1).setBackground(Color.GREEN);
					}
					window.getElevatorFloors(22 - cFloor2, 1).setBackground(Color.BLACK);//elevator on this floor is shown black
					if (handler2.pickingUp) {guiLamps[1][22 - cFloor2] = 0;}
				}
				else {
					if (blink) {
						window.getElevatorFloors(22 - cFloor2, 1).setBackground(Color.RED);//elevator stuck
					}
				}

				// elevator 3
				if (!handler3.status.equals("SUSPENDED")) {
					window.getElevatorDirections(0, 2).setBackground(Color.WHITE);//reset UP directions
					window.getElevatorDirections(1, 2).setBackground(Color.WHITE);//reset DOWN directions
					if (!handler3.liveDirection.equals("")) {
						window.getElevatorDirections((handler3.liveDirection.equals("UP") ? 0 : 1), 2).setBackground(Color.GREEN);
					}
					window.getElevatorFloors(22 - cFloor3, 2).setBackground(Color.BLACK);//elevator on this floor is shown black
					if (handler3.pickingUp) {guiLamps[2][22 - cFloor3] = 0;}
				}
				else {
					if (blink) {
						window.getElevatorFloors(22 - cFloor3, 2).setBackground(Color.RED);//elevator stuck
					}
				}

				// elevator 4
				if (!handler4.status.equals("SUSPENDED")) {
					window.getElevatorDirections(0, 3).setBackground(Color.WHITE);//reset UP directions
					window.getElevatorDirections(1, 3).setBackground(Color.WHITE);//reset DOWN directions
					if (!handler4.liveDirection.equals("")) {
						window.getElevatorDirections((handler4.liveDirection.equals("UP") ? 0 : 1), 3).setBackground(Color.GREEN);
					}
					window.getElevatorFloors(22 - cFloor4, 3).setBackground(Color.BLACK);//elevator on this floor is shown black
					if (handler4.pickingUp) {guiLamps[3][22 - cFloor4] = 0;}
				}
				else {
					if (blink) {
						window.getElevatorFloors(22 - cFloor4, 3).setBackground(Color.RED);//elevator stuck
					}
				}
				blink = !blink;
				/* ##     ## */

				try {
					hostSocket.receive(rPacket);
					System.out.println(String.format("main: received packet ( string >> %s, byte array >> %s ).\n", new String(rPacket.getData()), rPacket.getData()));
				} catch (SocketTimeoutException ste) {
					continue;
				}
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
			} catch (Exception e) {
				e.printStackTrace();
				listening = false;
			}
		}
		hostSocket.close();
	}


	/**
	 * Method to handle a command received from a floor. Computes the suitability of each elevator to service a floor request, gives the request to the corresponding elevator.
	 *
	 * @param cmd
	 * @param port
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

					// determine which elevator List to add request to

					/* Elevator Scheduling Algorithm */
					String[] temp = dPacketParsed[1].split(" ");

					int FS1 = calculateSuitability(FLOORS, handler1.currentFloor, Integer.parseInt(temp[1]), handler1.currentDirection, temp[2], handler1.status);
					int FS2 = calculateSuitability(FLOORS, handler2.currentFloor, Integer.parseInt(temp[1]), handler2.currentDirection, temp[2], handler2.status);
					int FS3 = calculateSuitability(FLOORS, handler3.currentFloor, Integer.parseInt(temp[1]), handler3.currentDirection, temp[2], handler3.status);
					int FS4 = calculateSuitability(FLOORS, handler4.currentFloor, Integer.parseInt(temp[1]), handler4.currentDirection, temp[2], handler4.status);
					int maxFS = Math.max(Math.max(FS1, FS2), Math.max(FS3, FS4));

					if (maxFS == FS1) {
						System.out.println("SUB 1 HANDLING IT");
						pickList1.add(temp[1] + " " + temp[2] + " " + temp[3]);						// add to request List
						guiLamps[0][22 - Integer.parseInt(temp[1])] = 1;							// update GUI
					}
					else if (maxFS == FS2) {
						System.out.println("SUB 2 HANDLING IT");
						pickList2.add(temp[1] + " " + temp[2] + " " + temp[3]);
						guiLamps[1][22 - Integer.parseInt(temp[1])] = 1;
					}
					else if (maxFS == FS3) {
						System.out.println("SUB 3 HANDLING IT");
						pickList3.add(temp[1] + " " + temp[2] + " " + temp[3]);
						guiLamps[2][22 - Integer.parseInt(temp[1])] = 1;
					}
					else {
						System.out.println("SUB 4 HANDLING IT");
						pickList4.add(temp[1] + " " + temp[2] + " " + temp[3]);
						guiLamps[3][22 - Integer.parseInt(temp[1])] = 1;
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
	 * Main.
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		Scheduler scheduler = new Scheduler();
		scheduler.run();
	}
}