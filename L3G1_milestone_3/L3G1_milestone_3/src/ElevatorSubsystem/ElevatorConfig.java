package ElevatorSubsystem;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

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
	private static final String ACK = "1";
	private static final String CMD = "2";
	private static final String DATA = "3";
	private int num_elevator = 0;
	private int num_lamp = 0;
	private static int send = 0;
	private static int s_elevator = 0;

	private DatagramSocket sendSocket, receiveSocket;
	private DatagramPacket sendPacket, receivePacket, sendAPacket;
	private Elevator elevator;
	private ElevatorStateMachine stateMachine;

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

	/**
	 * Controls the elevator based on received commands and data.
	 */
	public void elevatorController() {
		byte _data[] = new byte[100];
		receivePacket = new DatagramPacket(_data, _data.length);
		String[] ins = packetToString(receivePacket.getData());
		String[] cmd = null;
		String[] data = null;

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
							elevator.open(num_elevator);
							send = 0;
							if (s_elevator != -1) {
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
							elevator.close(num_elevator);
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
							elevator.run();
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
