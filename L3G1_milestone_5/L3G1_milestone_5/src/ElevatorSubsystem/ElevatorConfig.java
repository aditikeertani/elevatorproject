package ElevatorSubsystem;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import static utils.Utilities.*;

/**
 * Class modeling an Elevator Subsystem. The system consists of four Elevators being managed concurrently
 * which will service requests in real-time. Only elevator movement is handled in this class.
 *
 * @author Ryan Capstick, Oluwatobi Olowookere, Aditi Keertani
 */
public class ElevatorConfig {

	/* ## NETWORK INFO ## */
	final int EPORT1 = 3137;    // each Elevator has its own port
	final int EPORT2 = 3237;
	final int EPORT3 = 3337;
	final int EPORT4 = 3437;
	/* ## ------------ ## */

	private Elevator Elv_1;        // there are four elevators
	private Elevator Elv_2;
	private Elevator Elv_3;
	private Elevator Elv_4;

	/* ## HEADER AND COMMAND IDENTIFIERS ## */
	public static final String ACK = "1";
	public static final String CMD = "2";
	public static final String DATA = "3";
	public static final String ERROR = "0";
	public static final String DOWN = "0x31";								// going down
	public static final String UP = "0x30";									// going up
	public static final String DOOR_OPEN = "0x3A";							// open car door
	public static final String DOOR_CLOSE = "0x3B";							// close car door
	public static final String STOP = "0x3C";								// stop elevator car
	public static final String ERROR_DOOR_JAM = "0xE0";						// error door jam
	public static final String ERROR_STUCK = "0xE1";						// elevator stuck
	/* ## ------------------------------ ## */


	/**
	 * Constructor. Creates four Elevators with corresponding ports and origin floors.
	 */
	public ElevatorConfig() {
		Elv_1 = new Elevator(EPORT1, 1, 1);
		Elv_2 = new Elevator(EPORT2, 2, 1);
		Elv_3 = new Elevator(EPORT3, 3, 10);
		Elv_4 = new Elevator(EPORT4, 4, 20);
	}


	/**
	 * Main loop. Starts the Elevator Subsystem/Elevators.
	 */
	public void run() {
		Elv_1.start();
		Elv_2.start();
		Elv_3.start();
		Elv_4.start();
	}


	/**
	 * Main.
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		ElevatorConfig elevatorSubsystem = new ElevatorConfig();
		elevatorSubsystem.run();
	}


	/**
	 * Threaded class modeling an elevator car. Simulates semi-realistic movement, buttons, door actions.
	 */
	private class Elevator extends Thread {

		final int PORT;
		final int ID;
		int currentFloor;
		Set<String> lampSet = new HashSet<>();        // Set of currently active button lamps

		DatagramSocket elSocket;

		/**
		 * Constructor. Takes a port, id number, origin floor.
		 *
		 * @param port
		 * @param id
		 * @param originFloor
		 */
		Elevator(int port, int id, int originFloor) {
			PORT = port;
			ID = id;
			currentFloor = originFloor;
			try {
				elSocket = new DatagramSocket(PORT);
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}


		/**
		 * Overridden Thread start method. State machine.
		 */
		@Override
		public void run() {

			boolean running = true;

			DatagramPacket aPacket;
			DatagramPacket dPacket;

			byte[] buffer = new byte[8];
			DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);                            // what we're receiving
			String[] rPacketParsed;

			boolean floorChange = false;                                                                // was there a floor change?
			boolean lampChange = false;                                                                 // was there a lamp change?

			System.out.println("ELEVATOR " + ID + ": Elevator Waiting ...");                            // so we know we're waiting

			while (running) {
				try {
					elSocket.receive(rPacket);
				} catch (IOException e) {
					System.out.print("ELEVATOR " + ID + ": IOException");
					e.printStackTrace();
					System.exit(1);
				}

				rPacketParsed = parsePacket(rPacket.getData());

				switch (rPacketParsed[0]) {
					case CMD:
						/*====== START CMD packet received ======*/
						switch (rPacketParsed[1]) {
							/*--- cases for different CMD code ---**/
							case UP:
								floorChange = true;                                                        // send elevator location
								break;      // end UP                

							case DOWN:
								floorChange = true;
								break;      // end DOWN

							case DOOR_OPEN:
								System.out.println("ELEVATOR " + ID + ": OPENING at " + currentFloor);

								floorChange = false;
								break;      // end DOOR_OPEN                

							case DOOR_CLOSE:
								if (ID == 3 & currentFloor == 8) {
									try {
										System.out.println("ELEVATOR " + ID + " is stuck while closing at Floor " + currentFloor);
										Thread.sleep(10000);
									}
									catch (Exception e) {
										e.printStackTrace();
									}
								}
								else {
									System.out.println("ELEVATOR " + ID + ": CLOSING at " + currentFloor);

									floorChange = false;
								}
								break;      //end DOOR_CLOSE

							case STOP:
								System.out.println("ELEVATOR " + ID + ": STOPPED at " + currentFloor);

								floorChange = false;
								break;      // end STOP        
						}

						/*--- send ACK for CMD ---*/
						move(rPacketParsed[1]);
						aPacket = createPacket(ACK, rPacketParsed[1], rPacket.getPort());
						try {
							elSocket.send(aPacket);
						} catch (IOException ioe) {
							ioe.printStackTrace();
							System.exit(1);
						}

						if (floorChange) {
							dPacket = createPacket(DATA, "" + currentFloor, rPacket.getPort());
							try {
								elSocket.send(dPacket);
							} catch (IOException ioe) {
								ioe.printStackTrace();
								System.exit(1);
							}
							System.out.println("ELEVATOR " + ID + ": " + (rPacketParsed[1].equals(UP) ? "UP" : "DOWN") + " to " + currentFloor);
						}

						break;
					/*====== END CMD packet received ======*/

					case ACK:
						/*====== START ACK packet received ======*/

						/*--- only get an ACK on a position update ---*/
						floorChange = false;
						break;
					/*====== END ACK packet received ======*/

					case DATA:
						/*====== START DATA packet received ======*/
						String[] lamps = rPacketParsed[1].split(" ");

						for (int i = 0; i < lamps.length; i++) {
							if (!lampSet.remove(lamps[i])) {
								lampSet.add(lamps[i]);
							}
						}
						lampChange = true;

						/*--- send ACK message ---*/
						aPacket = createPacket(ACK, rPacketParsed[1], rPacket.getPort());
						try {
							elSocket.send(aPacket);
						} catch (IOException ioe) {
							ioe.printStackTrace();
							System.exit(1);
						}

						if (lampChange) {
							System.out.println("ELEVATOR " + ID + ": lamps on at [" + lampSet.toString() + "]");
							lampChange = false;
						}

						break;
					/*====== END DATA packet received ======*/

					case ERROR:
						/*====== START ERROR packet received ======*/
						switch (rPacketParsed[1]) {
							case ERROR_DOOR_JAM:
								System.out.println("ERROR DOOR IS JAMMED! ELEVATOR OUT OF SERVICE");
								break;

							case ERROR_STUCK:
								System.out.println("ERROR ELEVATOR IS STUCK! ELEVATOR OUT OF SERVICE");
								break;
						}
						try {
							aPacket = createPacket(ACK, rPacketParsed[1], rPacket.getPort());
						} catch (Exception e) {
							e.printStackTrace();
						}

						break;
					/*====== END ERROR packet received ======*/
				}
			}
		}


		/**
		 * Method to simulate the elevator's movement. Sleeps for 6 seconds on move or 2 seconds on door operations.
		 *
		 * @param cmd
		 * @return
		 */
		int move(String cmd) {

			if (cmd.equals(UP) | cmd.equals(DOWN)) {

				try {
					Thread.sleep(6000);
				} catch (InterruptedException ie) {
					ie.printStackTrace();
				}

				currentFloor = (cmd.equals(UP) ? currentFloor + 1 : currentFloor - 1);

			}
			else if (cmd.equals(DOOR_OPEN) | cmd.equals(DOOR_CLOSE)) {

				try {
					// Simulate door closing delay
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			/* ### ELEVATOR 4 STUCK AT FLOOR 15 ### */
			if (ID == 4 & currentFloor == 15) {
				try {
					System.out.println("ELEVATOR " + ID + " is STUCK WHILE MOVING at Floor " + currentFloor);
					Thread.sleep(10000);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}

			return 1;
		}
	}
}
