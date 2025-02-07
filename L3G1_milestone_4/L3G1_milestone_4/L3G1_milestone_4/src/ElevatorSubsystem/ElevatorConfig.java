package ElevatorSubsystem;

import java.io.IOException;
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Represents the configuration and control of an elevator.
 * Extends Thread to allow concurrent execution.
 * Manages elevator actions and communication.
 * Responsible for sending and receiving elevator commands and data.
 * Uses DatagramSocket for communication.
 * Uses Elevator and ElevatorStateMachine for elevator control.
 * Uses ElevatorButton for elevator direction.
 *
 * @author Ryan Capstick, Oluwatobi Olowookere, Aditi Keertani
 */
public class ElevatorConfig extends Thread {

	private static final String PICKUP_UP = "0x33";
	private static final String DROPOFF_UP = "0x32";
	private static final String PICKUP_DOWN = "0x31";
	private static final String DROPOFF_DOWN = "0x30";
	private static final String OPEN_DOOR = "0x3A";
	private static final String CLOSE_DOOR = "0x3B";
	private static final String STOP = "0x3C";
	private static final String ERROR_DOOR_JAM = "0xE0";
	private static final String ERROR_STUCK = "0xE1";
	private static final String ACK = "1";
	private static final String CMD = "2";
	private static final String DATA = "3";
	private static final String ERROR = "0";
	private int num_elevator = 0;
	private int num_lamp = 0;
	private static int send = 0;
	private static int s_elevator = 0;

	private DatagramSocket sendSocket, receiveSocket;
	private DatagramPacket sendPacket, receivePacket, sendAPacket;
	private Elevator elevator;
	private ElevatorStateMachine stateMachine;

	private Timer floorTimer;
	private Timer doorTimer;
	private static final long FLOOR_TIMEOUT = 10000; // 10 seconds
	private static final long DOOR_TIMEOUT = 5000; // 5 seconds
	private boolean isStuck = false;

	/**
	 * Constructs an ElevatorConfig object with the specified parameters.
	 *
	 * @param port         The port for communication.
	 * @param num_elevator The number of the elevator.
	 * @param floor        The current floor of the elevator.
	 */
	public ElevatorConfig(int port, int num_elevator, String floor) {
		this.num_elevator = num_elevator;
		elevator = new Elevator(floor, ElevatorButton.HOLD);
		stateMachine = new ElevatorStateMachine();

		floorTimer = new Timer();
		doorTimer = new Timer();

		try {
			sendSocket = new DatagramSocket();
			receiveSocket = new DatagramSocket(port);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Starts the elevator controller thread.
	 */
	public void run() {
		elevatorController();
	}

	/**
	 * Creates a DatagramPacket with the specified parameters.
	 *
	 * @param packetType The type of the packet.
	 * @param code       The code for the packet.
	 * @param port       The port for communication.
	 * @return The created DatagramPacket.
	 */
	public static DatagramPacket createPacket(String packetType, String code, int port) {
		String data = "";
		DatagramPacket packet = null;

		if (packetType.equals("1")) {
			data = "\0" + ACK + "\0" + code + "\0";
		} else if (packetType.equals("2")) {
			data = "\0" + CMD + "\0" + code + "\0";
		} else if (packetType.equals("3")) {
			data = "\0" + DATA + "\0" + code + "\0";
		} else if (packetType.equals("0")) {
			data = "\0" + ERROR + "\0" + code + "\0"; // ERROR
		}

		try {
			packet = new DatagramPacket(data.getBytes(), data.getBytes().length, InetAddress.getLocalHost(), port);
		} catch (UnknownHostException uhe) {
			System.out.println("ELEVATOR: unable to create packet (UnknownHostException), exiting.");
			System.exit(1);
		}

		return packet;
	}

	/**
	 * Converts a string to an integer.
	 *
	 * @param string The string to convert.
	 * @return The integer value.
	 */
	public int toInt(String string) {
		int a = 0;
		try {
			a = Integer.parseInt(string);
		} catch (NumberFormatException e) {
			System.out.println("ELEVATOR " + num_elevator + "ERROR current floor");
		}
		return a;
	}

	// Method to start the floor arrival timer
	private void startFloorTimer() {
		floorTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				handleFloorTimeout();
			}
		}, FLOOR_TIMEOUT);
	}

	// Method to start the door operation timer
	private void startDoorTimer() {
		doorTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				handleDoorTimeout();
			}
		}, DOOR_TIMEOUT);
	}

	// Method to handle floor arrival timeout
	private void handleFloorTimeout() {
		// Elevator is stuck between floors
		System.out.println("ELEVATOR " + num_elevator + ": Floor arrival timeout. Elevator is stuck.");
		isStuck = true;

		// Send error packet
		sendErrorPacket(ERROR_STUCK);
	}

	// Method to handle door operation timeout
	private void handleDoorTimeout() {
		// Door is not opening or closing properly
		System.out.println("ELEVATOR " + num_elevator + ": Door operation timeout. Possible door jam.");
		// Handle this situation gracefully
		// For now, let's attempt to close the door again
		elevator.close(num_elevator);

		// Send error packet
		sendErrorPacket(ERROR_DOOR_JAM);
	}
	// Method to stop the floor arrival timer
	private void stopFloorTimer() {
		floorTimer.cancel();
		floorTimer.purge();  // Purge the cancelled timer tasks from the timer's task queue
		floorTimer = new Timer(); // Reinitialize the timer for future use
	}

	// Method to stop the door operation timer
	private void stopDoorTimer() {
		doorTimer.cancel();
		doorTimer.purge();  // Purge the cancelled timer tasks from the timer's task queue
		doorTimer = new Timer(); // Reinitialize the timer for future use
	}

	// Method to send error packet
	private void sendErrorPacket(String errorCode) {
		sendPacket = createPacket(ERROR, errorCode, receivePacket.getPort());
		try {
			sendSocket.send(sendPacket);
			System.out.println("ELEVATOR " + num_elevator + ": Error packet sent: " + errorCode);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Controls the elevator based on received commands and data.
	 */
	public void elevatorController() {
		byte _data[] = new byte[100];
		receivePacket = new DatagramPacket(_data, _data.length);
		String[] ins = packetToString(receivePacket.getData());
		String[] cmd = null;
		String[] data = null;
		String[] error = null;

		while (true) {
			try {
				System.out.println("ELEVATOR " + num_elevator + ": Elevator Waiting ...");
				receiveSocket.receive(receivePacket);
				System.out.print("ELEVATOR " + num_elevator + ": received ");
			} catch (IOException e) {
				System.out.print("ELEVATOR " + num_elevator + ": IO Exception: likely:");
				System.out.println("Receive Socket Timed Out.\n" + e);
				e.printStackTrace();
				System.exit(1);
			}

			ins = packetToString(receivePacket.getData());

			switch (ins[0]) {
				case CMD:
					cmd = ins;
					switch (cmd[1]) {
						case DROPOFF_UP:
							System.out.println("cmd, UP for drop off");
							elevator.direction = ElevatorButton.UP;
							elevator.run();
							sendPacket = createPacket(DATA, elevator.getCurrentFloor(), receivePacket.getPort());
							System.out.println("ELEVATOR " + num_elevator + ":Elevator moved up, now at Floor " + elevator.getCurrentFloor());
							send = 1;
							s_elevator = 0;
							break;

						case PICKUP_UP:
							System.out.println("cmd, UP for pick up");
							System.out.println("ELEVATOR " + num_elevator + ": wait for elevator data ");
							s_elevator = 1;
							send = 0;
							break;

						case DROPOFF_DOWN:
							System.out.println("cmd, DOWN for drop off");
							elevator.direction = ElevatorButton.DOWN;
							elevator.run();
							System.out.println("ELEVATOR " + num_elevator + ":Elevator move DOWN, now at Floor " + elevator.getCurrentFloor());
							sendPacket = createPacket(DATA, elevator.getCurrentFloor(), receivePacket.getPort());
							send = 1;
							s_elevator = 0;
							break;

						case PICKUP_DOWN:
							System.out.println("cmd, UP for drop off");
							System.out.println("ELEVATOR " + num_elevator + ": wait for elevator data ");
							s_elevator = 1;
							send = 0;
							break;

						case OPEN_DOOR:
							System.out.println("cmd, OPEN door");
							startDoorTimer();
							elevator.open(num_elevator);
							stopDoorTimer();
							send = 0;

							/* Door not opening */
							if (num_elevator == 2 && elevator.getCurrentFloor().equals("3")) {
								try {
									System.out.println("ELEVATOR " + num_elevator + " is STUCK at Floor " + elevator.getCurrentFloor());
									// Simulate door jam by sleeping for 10 seconds
									Thread.sleep(3000);
									sendPacket = createPacket(ERROR, ERROR_DOOR_JAM, receivePacket.getPort());
									sendSocket.send(sendPacket); // Send ERROR_DOOR_JAM packet
									System.out.println("ELEVATOR " + num_elevator + ": ERROR_DOOR_JAM sent.");
								} catch (Exception e) {
									e.printStackTrace();
								}
							}

							else if (s_elevator != -1) {
								if (s_elevator == 1) {
									elevator.Lamp[num_lamp - 1] = s_elevator;
									System.out.println("ELEVATOR " + num_elevator + ": Elevator Lamp ON at " + num_lamp);
								} else if (s_elevator == 0) {
									elevator.Lamp[elevator.getIntFloor() - 1] = s_elevator;
									System.out.println("ELEVATOR " + num_elevator + ": Elevator Lamp OFF at " + num_lamp);
								} else {
									System.out.println("ELEVATOR " + num_elevator + ": ERROR elevator status");
								}
								s_elevator = -1;
							}
							break;

						case CLOSE_DOOR:
							System.out.println("cmd, CLOSE door");
							startDoorTimer();
							elevator.close(num_elevator);
							stopDoorTimer();
							send = 0;
							s_elevator = -1;
							break;

						case STOP:
							System.out.println("cmd, STOP");
							elevator.direction = ElevatorButton.HOLD;
							elevator.run();
							System.out.println("ELEVATOR " + num_elevator + ":Elevator STOPPED at " + elevator.getCurrentFloor());
							send = 0;
							break;
					}

					sendAPacket = createPacket(ACK, ins[1], receivePacket.getPort());
					try {
						sendSocket.send(sendAPacket);
					} catch (IOException e1) {
						e1.printStackTrace();
						System.exit(1);
					}

					if (send == 1) {
						try {
							sendSocket.send(sendPacket);
						} catch (IOException e1) {
							e1.printStackTrace();
							System.exit(1);
						}
						send = 0;
					}
					break;

				case ACK:
					System.out.println("ack");
					switch (cmd[1]) {
						case PICKUP_UP:
							elevator.direction = ElevatorButton.UP;
							elevator.run();
							System.out.println("ELEVATOR " + num_elevator + ":Elevator moved UP, now at Floor " + elevator.getCurrentFloor());
							sendPacket = createPacket(DATA, elevator.getCurrentFloor(), receivePacket.getPort());
							s_elevator = 1;
							break;

						case PICKUP_DOWN:
							elevator.direction = ElevatorButton.DOWN;
							elevator.run();
							System.out.println("ELEVATOR " + num_elevator + ":Elevator moved DOWN, now at Floor " + elevator.getCurrentFloor());
							sendPacket = createPacket(DATA, elevator.getCurrentFloor(), receivePacket.getPort());
							s_elevator = 1;
							break;

						case DROPOFF_UP:
							elevator.direction = ElevatorButton.UP;
							elevator.run();
							System.out.println("ELEVATOR " + num_elevator + ":Elevator moved UP, now at Floor " + elevator.getCurrentFloor());
							sendPacket = createPacket(DATA, elevator.getCurrentFloor(), receivePacket.getPort());
							s_elevator = 0;
							break;

						case DROPOFF_DOWN:
							elevator.direction = ElevatorButton.DOWN;
							elevator.run();
							System.out.println("ELEVATOR " + num_elevator + ":Elevator moved DOWN, now at Floor " + elevator.getCurrentFloor());
							sendPacket = createPacket(DATA, elevator.getCurrentFloor(), receivePacket.getPort());
							s_elevator = 0;
							break;
					}

					try {
						sendSocket.send(sendPacket);
					} catch (IOException e1) {
						e1.printStackTrace();
						System.exit(1);
					}

					/* ### ELEVATOR 4 STUCK AT FLOOR 15 ### */
					if (num_elevator == 4 && elevator.getCurrentFloor().equals("15")) {
						try {
							System.out.println("ELEVATOR " + num_elevator + " is STUCK at Floor " + elevator.getCurrentFloor());

							sendPacket = createPacket(ERROR, ERROR_STUCK, receivePacket.getPort());
							sendSocket.send(sendPacket); // Send ERROR_STUCK packet
							System.out.println("ELEVATOR " + num_elevator + ": ERROR_STUCK sent.");
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					break;

				case DATA:
					System.out.println("data");
					data = ins;
					switch (cmd[1]) {
						case PICKUP_UP:
							elevator.direction = ElevatorButton.UP;
							elevator.run();
							sendPacket = createPacket(DATA, elevator.getCurrentFloor(), receivePacket.getPort());
							System.out.println("ELEVATOR " + num_elevator + ":Elevator moved UP, now at Floor " + elevator.getCurrentFloor());
							s_elevator = 1;
							num_lamp = toInt(data[1]);
							send = 1;
							break;

						case PICKUP_DOWN:
							elevator.direction = ElevatorButton.DOWN;
							elevator.run();
							sendPacket = createPacket(DATA, elevator.getCurrentFloor(), receivePacket.getPort());
							System.out.println("ELEVATOR " + num_elevator + ":Elevator moved DOWN, now at Floor " + elevator.getCurrentFloor());
							s_elevator = 1;
							num_lamp = toInt(data[1]);
							send = 1;
							break;

						case STOP:
							startFloorTimer();
							elevator.run();
							stopFloorTimer();
							s_elevator = 1;
							num_lamp = toInt(data[1]);
							send = 0;
							break;
					}

					sendAPacket = createPacket(ACK, data[1], receivePacket.getPort());
					try {
						sendSocket.send(sendAPacket);
					} catch (IOException e1) {
						e1.printStackTrace();
						System.exit(1);
					}

					if (send == 1) {
						try {
							sendSocket.send(sendPacket);
						} catch (IOException e1) {
							e1.printStackTrace();
							System.exit(1);
						}
					}
					break;
				case ERROR:
					error = ins;
					switch (error[1]) {
						case ERROR_DOOR_JAM:
							System.out.println("ERROR: Door Jam - Trying Doors Again");
							break;
						case ERROR_STUCK:
							System.out.println("ERROR: Elevator Stuck - Disabling Elevator");
							break;
					}
					try {
						sendPacket = createPacket(ACK, error[1], receivePacket.getPort());
					}
					catch (Exception e) {
						e.printStackTrace();
					}
					break;

				default:
					break;
			}
		}
	}

	/**
	 * Converts a DatagramPacket's data to a string array.
	 *
	 * @param data The data of the DatagramPacket.
	 * @return The string array.
	 */
	public String[] packetToString(byte[] data) {
		String info = new String(data);
		String[] msg = info.replaceFirst("\0", "").split("\0");
		return msg;
	}

	/**
	 * The main method to start elevator configurations for multiple elevators.
	 *
	 * @param args Command line arguments.
	 */
	public static void main(String[] args) {
		ElevatorConfig Elv_1 = new ElevatorConfig(3137, 1, "1");
		ElevatorConfig Elv_2 = new ElevatorConfig(3237, 2, "1");
		ElevatorConfig Elv_3 = new ElevatorConfig(3337, 3, "10");
		ElevatorConfig Elv_4 = new ElevatorConfig(3437, 4, "20");

		Elv_1.start();
		Elv_2.start();
		Elv_3.start();
		Elv_4.start();
	}
}
