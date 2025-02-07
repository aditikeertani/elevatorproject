package ElevatorSubsystem;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Objects;

/**
 * The ElevatorConfig class handles the reception of requests from the scheduler
 * and the corresponding elevator to fulfill the requests.
 *
 * @author Ryan Capstick, Oluwatobi Olowookere, Aditi Keertani
 */
public class ElevatorConfig {
	// Command identifiers
	private static final String PICKUP_UP = "0x33"; // Going up to pick up
	private static final String DROPOFF_UP = "0x32"; // Going up to drop off
	private static final String PICKUP_DOWN = "0x31"; // Going down to pick up
	private static final String DROPOFF_DOWN = "0x30"; // Going down to drop off
	private static final String OPEN_DOOR = "0x3A"; // Open door command
	private static final String CLOSE_DOOR = "0x3B"; // Close door command
	private static final String STOP = "0x3C"; // Stop command
	private static final String ACK = "1"; // Acknowledgment
	private static final String CMD = "2"; // Command
	private static final String DATA = "3"; // Data

	private static final int aport = 3137;
	private static final int MAX_FLOORS = 4;

	private static int numOfLamps = 0;
	private static int send = 0;
	private static int sendElevator = 0;
	int[] Lamp = new int[MAX_FLOORS];
	private DatagramSocket sendSocket, receiveSocket;
	private DatagramPacket sendPacket, receivePacket, sendAPacket;

	private final Elevator elevator = new Elevator("0", ElevatorButton.HOLD);

	/**
	 * Constructor: Initializes the socket and elevator lamp.
	 */
	public ElevatorConfig() {
		// Initialize sockets
		try {
			sendSocket = new DatagramSocket();
			receiveSocket = new DatagramSocket(aport);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}

		// Initialize Elevator Lamp
		Arrays.fill(Lamp, 0);
	}

	/**
	 * Creates a DatagramPacket using packetType: HEADER, code: COMMAND IDENTIFIERS, and port.
	 *
	 * @param packetType The type of packet (ACK, CMD, or DATA).
	 * @param code       The command identifier code.
	 * @param port       The port number.
	 * @return DatagramPacket object.
	 */
	public static DatagramPacket createPacket(String packetType, String code, int port) {
		String data = "";
		DatagramPacket packet = null;

		data = switch (packetType) {
			case "1" -> "\0" + ACK + "\0" + code + "\0";
			case "2" -> "\0" + CMD + "\0" + code + "\0";
			case "3" -> "\0" + DATA + "\0" + code + "\0";
			default -> data;
		};

		try {
			packet = new DatagramPacket(data.getBytes(), data.getBytes().length, InetAddress.getLocalHost(), port);
		} catch (UnknownHostException uhe) {
			System.out.println("ELEVATOR: unable to create packet (UnknownHostException), exiting.");
			System.exit(1);
		}

		return packet;
	}

	/**
	 * Converts packet data to a string array.
	 *
	 * @param data The byte array of packet data.
	 * @return String array containing packet information.
	 */
	public static String[] packetToString(byte[] data) {
		String info = new String(data);
		return info.replaceFirst("\0", "").split("\0");
	}

	/**
	 * Converts a string to an integer.
	 *
	 * @param string The string to be converted.
	 * @return The integer representation of the string.
	 */
	public int toInt(String string) {
		int a = 0;
		try {
			a = Integer.parseInt(string);
		} catch (NumberFormatException e) {
			System.out.println("ERROR current floor");
		}
		return a;
	}

	/**
	 * Function called in the main method to control the corresponding elevator to perform the received request.
	 */
	public void elevatorController() {
		byte[] _data = new byte[100];
		receivePacket = new DatagramPacket(_data, _data.length);
		String[] ins = packetToString(receivePacket.getData());
		String[] cmd = null;
		String[] data = null;

		while (true) {
			try {
				//System.out.println("ELEVATOR: Is Waiting to be used");
				receiveSocket.receive(receivePacket);
				System.out.print("ELEVATOR: received ");
			} catch (IOException e) {
				System.out.print("ELEVATOR: IO Exception: likely:");
				System.out.println("Receive Socket Timed Out.\n" + e);
				e.printStackTrace();
				System.exit(1);
			}

			ins = packetToString(receivePacket.getData());

			switch (ins[0]) {
				case CMD:
					/*====== CMD packet received ======*/
					cmd = ins;

					switch (cmd[1]) {
						/*--- cases for different CMD code ---**/
						case DROPOFF_UP:
							System.out.println("cmd, UP for drop off");
							elevator.direction = ElevatorButton.UP; // move up
							elevator.run();
							sendPacket = createPacket(DATA, elevator.getCurrentFloor(), receivePacket.getPort());
							System.out.println("ELEVATOR: Elevator moved up, now at Floor " + elevator.getCurrentFloor());
							send = 1; // send elevator location
							sendElevator = 0; // elevator job drop off
							break; // end DROPOFF_UP
						case PICKUP_UP:
							System.out.println("cmd, UP for pick up");
							System.out.println("ELEVATOR: wait for elevator data ");
							sendElevator = 1; // elevator job pick up
							send = 0;
							break; // end PICKUP_UP
						case DROPOFF_DOWN:
							System.out.println("cmd, DOWN for drop off");
							elevator.direction = ElevatorButton.DOWN; // move down
							elevator.run();
							System.out.println("ELEVATOR:Elevator move DOWN, now at Floor " + elevator.getCurrentFloor());
							sendPacket = createPacket(DATA, elevator.getCurrentFloor(), receivePacket.getPort());
							send = 1; // send elevator location
							sendElevator = 0; // elevator job drop off
							break; // end DROPOFF_DOWN
						case PICKUP_DOWN:
							System.out.println("cmd, UP for drop off");
							System.out.println("ELEVATOR: wait for elevator data ");
							sendElevator = 1; // elevator job pick up
							send = 0;
							break; // end PICKUP_DOWN
						case OPEN_DOOR:
							System.out.println("cmd, OPEN door");
							elevator.open();
							send = 0;
							if (sendElevator != -1) {
								if (sendElevator == 1) {
									/*--- door open for pickup, elevator lamp ON ---*/
									Lamp[numOfLamps - 1] = sendElevator;
									System.out.println("ELEVATOR: Elevator Lamp ON at " + numOfLamps);
								} else if (sendElevator == 0) {
									/*--- door open for drop off, elevator lamp OFF ---*/
									Lamp[elevator.getIntFloor() - 1] = sendElevator;
									System.out.println("ELEVATOR: Elevator Lamp OFF at " + numOfLamps);
								} else {
									System.out.println("ELEVATOR: ERROR elevator status");
								}
								sendElevator = -1; // elevator job open door
							}// end if not -1
							break; // end OPEN_DOOR
						case CLOSE_DOOR:
							System.out.println("cmd, CLOSE door");
							elevator.close();
							send = 0;
							sendElevator = -1; // elevator job open close
							break; // end CLOSE_DOOR
						case STOP:
							System.out.println("cmd, STOP");
							elevator.direction = ElevatorButton.HOLD;
							elevator.run();
							System.out.println("ELEVATOR:Elevator STOPPED at " + elevator.getCurrentFloor());
							send = 0;
							break; // end STOP
					}// end CMD switch

					sendAPacket = createPacket(ACK, ins[1], receivePacket.getPort());
					try {
						sendSocket.send(sendAPacket);
					} catch (IOException e1) {
						e1.printStackTrace();
						System.exit(1);
					}

					//send elevator location
					if (send == 1) {
						try {
							sendSocket.send(sendPacket);
						} catch (IOException e1) {
							e1.printStackTrace();
							System.exit(1);
						}
						send = 0;
					}// end if (location update)
					break; // end CMD
				case ACK:
					System.out.println("ack");
					/*----- ACK packet received -----*/
					switch (Objects.requireNonNull(cmd)[1]) {
						case PICKUP_UP:
							elevator.direction = ElevatorButton.UP; // move up
							elevator.run();
							System.out.println("ELEVATOR:Elevator moved UP, now at Floor " + elevator.getCurrentFloor());
							sendPacket = createPacket(DATA, elevator.getCurrentFloor(), receivePacket.getPort()); // send elevator location
							sendElevator = 1;
							break; // end PICKUP_UP
						case PICKUP_DOWN:
							elevator.direction = ElevatorButton.DOWN; // move down
							elevator.run();
							System.out.println("ELEVATOR:Elevator moved DOWN, now at Floor " + elevator.getCurrentFloor());
							sendPacket = createPacket(DATA, elevator.getCurrentFloor(), receivePacket.getPort());
							sendElevator = 1;
							break; // end PICKUP_DOWN
						case DROPOFF_UP:
							elevator.direction = ElevatorButton.UP; // move up
							elevator.run();
							System.out.println("ELEVATOR:Elevator moved UP, now at Floor " + elevator.getCurrentFloor());
							sendPacket = createPacket(DATA, elevator.getCurrentFloor(), receivePacket.getPort());
							sendElevator = 0;
							break; // end DROPOFF_UP
						case DROPOFF_DOWN:
							elevator.direction = ElevatorButton.DOWN; // move down
							elevator.run();
							System.out.println("ELEVATOR:Elevator moved DOWN, now at Floor " + elevator.getCurrentFloor());
							sendPacket = createPacket(DATA, elevator.getCurrentFloor(), receivePacket.getPort());
							sendElevator = 0; // elevator job drop off
							break; // end DROPOFF_DOWN
					}// end CMD switch

					/*--- send elevator location message ---*/
					try {
						sendSocket.send(sendPacket);
					} catch (IOException e1) {
						e1.printStackTrace();
						System.exit(1);
					}
					break; // end ACK

				case DATA:
					System.out.println("data");
					/*----- DATA packet received -----*/
					data = ins;
					switch (Objects.requireNonNull(cmd)[1]) {
						case PICKUP_UP:
							elevator.direction = ElevatorButton.UP; // move up
							elevator.run();
							sendPacket = createPacket(DATA, elevator.getCurrentFloor(), receivePacket.getPort());
							System.out.println("ELEVATOR:Elevator moved UP, now at Floor " + elevator.getCurrentFloor());
							sendElevator = 1; // elevator job pick up
							numOfLamps = toInt(data[1]); // record elevator lamp
							break;
						case PICKUP_DOWN:
							elevator.direction = ElevatorButton.DOWN; // move down
							elevator.run();
							sendPacket = createPacket(DATA, elevator.getCurrentFloor(), receivePacket.getPort());
							System.out.println("ELEVATOR:Elevator moved DOWN, now at Floor " + elevator.getCurrentFloor());
							sendElevator = 1; // elevator job pick up
							numOfLamps = toInt(data[1]); // record elevator lamp
							break;
						case STOP:
							sendElevator = 1; // elevator job pick up
							numOfLamps = toInt(data[1]); // record elevator lamp
							break;
					}// end CMD switch

					/*--- send ACK message ---*/
					sendAPacket = createPacket(ACK, data[1], receivePacket.getPort());
					try {
						sendSocket.send(sendAPacket);
					} catch (IOException e1) {
						e1.printStackTrace();
						System.exit(1);
					}
					/*--- send elevator location ---*/
					try {
						sendSocket.send(sendPacket);
					} catch (IOException e1) {
						e1.printStackTrace();
						System.exit(1);
					}
			}
		}
	}

	/**
	 * Main function of the ElevatorConfig class.
	 *
	 * @param args Command line arguments.
	 */
	public static void main(String[] args) {
		ElevatorConfig config = new ElevatorConfig();
		config.elevatorController();
	}
}
