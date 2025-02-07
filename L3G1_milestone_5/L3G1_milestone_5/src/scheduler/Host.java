package scheduler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import static utils.Utilities.*;

/**
 * Thread class to handle elevator movements. Uses a BlockingQueue to monitor new requests.
 */
class Host extends Thread {

	private DatagramSocket eSocket;                    // socket to communicate with elevator system
	private int eport;                                  // port of elevator system
	private int fport;                                  // port of floor system
	private List<String> pickList;                      // ArrayList with requests

	protected volatile String currentDirection;         // variable representing current direction of the elevator, to be accessed by the main thread as well
	protected volatile int currentFloor;                // variable representing current floor of the elevator, to be accessed by the main thread as well
	protected volatile String status;                   // is elevator out of order or not
	protected int id;                                   // number identifier

	protected volatile String liveDirection;            // variable used by the GUI representing the direction of the elevator
	protected volatile boolean pickingUp = false;       // variable used by the GUI to determine whether the light at this floor should be turned off

	/**
	 * Constructor. Takes the elevator port, floor port, request List, starting floor, status, number id.
	 *
	 * @param eport Port number of the elevator system
	 * @param fport Port number of the floor system
	 * @param pickList List of pickup requests
	 * @param currentFloor Current floor of the elevator
	 * @param status Status of the elevator
	 * @param id Identifier of the elevator
	 */
	public Host(int eport, int fport, List<String> pickList, int currentFloor, String status, int id) {
		super("request thread");
		try {
			eSocket = new DatagramSocket();
			eSocket.setSoTimeout(10000);
			this.eport = eport;
			this.fport = fport;
			this.pickList = pickList;
			this.currentFloor = currentFloor;
			this.currentDirection = "";
			this.liveDirection = "";
			this.status = status;
			this.id = id;

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Main thread process. Executed on thread start. Initiates handling of service requests.
	 */
	@Override
	public void run() {
		// Thread running flag
		boolean running = true;
		//determining which request to serve first
		int bestDistance;
		// Current request being processed
		String request;
		// Best request determined by closest distance
		String bestRequest;
		// Parsed components of a request
		String[] requestParsed;
		// Temporary array for processing requests
		String[] temp;
		// Initial direction of the elevator
		String initialDirection = null;
		System.out.println(String.format("sub-%d: a new thread is running.", this.id));

		while (running) {
			if (status.equals("IDLE")) {
				if (!pickList.isEmpty()) {
					bestDistance = 100;
					bestRequest = "";
					int holderCurrentFloor = currentFloor;

					for (Iterator<String> iterator = pickList.iterator(); iterator.hasNext();) {
						request = iterator.next();
						temp = request.split(" ");

						// computing closest next available pickup
						if (Math.abs(Integer.parseInt(temp[0]) - holderCurrentFloor) < bestDistance) {
							bestRequest = request;
							bestDistance = Integer.parseInt(temp[0]) - currentFloor;
						}
					}
					pickList.remove(bestRequest);

					requestParsed = bestRequest.split(" ");

					// elevator is above first pickup in sequence
					if (Integer.parseInt(requestParsed[0]) < holderCurrentFloor) {
						status = "REPO";
						initialDirection = "DOWN";
					}
					// elevator is below first pickup in sequence
					else if (Integer.parseInt(requestParsed[0]) > holderCurrentFloor) {
						status = "REPO";
						initialDirection = "UP";
					}
					// elevator is already at first pickup in sequence
					else {
						status = "REPO";
						initialDirection = "STOP";
					}

					// pass the first pickup floor and direction to get there, and the dropoff associated for that pickup
					elevMoveControl(bestRequest, initialDirection);
				}
			}
		}
		eSocket.close();
    }

	/**
	 * Method to control elevator movement.
	 *
	 * @param initialRequest Initial request to process
	 * @param initialDirection Initial direction of the elevator
	 */
	public void elevMoveControl(String initialRequest, String initialDirection) {
		// Buffer for communication
		byte[] buffer = new byte[8];
		// DatagramPacket for communication
		DatagramPacket cPacket;
		// DatagramPacket for communication
		DatagramPacket aPacket;
		// DatagramPacket for communication
		DatagramPacket ePacket;
		// DatagramPacket for communication
		DatagramPacket dPacket;
		// DatagramPacket for communication
		DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);
		// Parsed components of a packet
		String[] rPacketParsed;

		// Splitting initial request
		String[] temp = initialRequest.split(" ");
		// Pickup floor
		String initialPickup = temp[0];
		// Direction of drop
		String dropDirection = temp[1];
		// Set to store dropoffs
		Set<String> drops = new HashSet<String>();
		// String to track elevator lamp changes
		String elevatorLampChanges = "";

		// Flags
		boolean stop = false;                   // Need to stop at this floor?
		boolean floorLampUpdate = false;        // Did someone get picked up?
		// Error type
		String error = ERROR_STUCK;

		try {
			try {
				// Repositioning elevator; sending to first pickup location
				if (!initialDirection.equals("STOP")) {
					liveDirection = initialDirection;
					cPacket = createPacket(CMD, (initialDirection.equals("UP") ? UP : DOWN), eport);

					while (status.equals("REPO")) {
						// Move towards pickup
						eSocket.send(cPacket);

						// Listen for ack
						eSocket.receive(rPacket);

						// Parsing ack
						rPacketParsed = parsePacket(rPacket.getData());

						// Listen for positional update
						eSocket.receive(rPacket);

						// Parsing positional update
						rPacketParsed = parsePacket(rPacket.getData());

						// Update currentFloor field
						currentFloor = Integer.parseInt(rPacketParsed[1]);

						// Acknowledge positional update
						aPacket = createPacket(ACK, rPacketParsed[1], eport);
						eSocket.send(aPacket);

						if (rPacketParsed[1].equals(initialPickup)) {
							status = "WORKING";
							currentDirection = dropDirection;
						}
					}
				} else {
					status = "WORKING";
					currentDirection = dropDirection;
					cPacket = createPacket(CMD, STOP, eport);
				}
				// GUI updates
				liveDirection = dropDirection;
				pickingUp = true;

				// Send stop
				cPacket = createPacket(CMD, STOP, eport);
				eSocket.send(cPacket);

				// Listen for ack to stop
				eSocket.receive(rPacket);

				// parsing ack
				rPacketParsed = parsePacket(rPacket.getData());

				// Send update to floor that the elevator has arrived
				sendPositionToFloor("" + currentFloor);

				error = ERROR_DOOR_JAM;

				// Sending open door
				cPacket = createPacket(CMD, DOOR_OPEN, eport);
				eSocket.send(cPacket);

				// Listen for ack to open
				eSocket.receive(rPacket);

				// parsing ack
				rPacketParsed = parsePacket(rPacket.getData());

				// Add initial drop
				drops.add(temp[2]);

				// Send elevator lamp updates
				dPacket = createPacket(DATA, temp[2], eport);
				eSocket.send(dPacket);

				// Listen for ack
				eSocket.receive(rPacket);

				// parsing ack
				rPacketParsed = parsePacket(rPacket.getData());

				// Sending close door
				cPacket = createPacket(CMD, DOOR_CLOSE, eport);
				eSocket.send(cPacket);

				// Listen for ack to close
				eSocket.receive(rPacket);

				// parsing ack
				rPacketParsed = parsePacket(rPacket.getData());

				// Get ready to perform dropoff, checking for more pickups along the way
				cPacket = createPacket(CMD, (dropDirection.equals("UP") ? UP : DOWN), eport);

				while (status.equals("WORKING")) {
					pickingUp = false;

					// Send cmd
					eSocket.send(cPacket);

					// Listen for ack
					eSocket.receive(rPacket);

					// parsing ack
					rPacketParsed = parsePacket(rPacket.getData());

					// Listen for positional update
					eSocket.receive(rPacket);

					// Parsing positional update
					rPacketParsed = parsePacket(rPacket.getData());

					// Update currentFloor field
					currentFloor = Integer.parseInt(rPacketParsed[1]);

					// Acknowledge positional update
					aPacket = createPacket(ACK, rPacketParsed[1], eport);
					eSocket.send(aPacket);

					// Checking for intermediate stops (must be a pickup going in the same direction as the elevator currently is, or a dropoff from the HashSet)
					if (!pickList.isEmpty()) {
						for (Iterator<String> iterator = pickList.iterator(); iterator.hasNext();) {
							temp = iterator.next().split(" ");
							if (temp[0].equals(rPacketParsed[1])) {
								if (temp[1].equals(dropDirection)) {
									stop = true; // Can pickup this request already
									floorLampUpdate = true; // Update floor lamps
									iterator.remove(); // Remove; request is being handled
									pickingUp = true; // Update GUI lamps

									// Turn on elevator lamp at destination floor; do not update if there's already a dropoff for that floor
									if (!drops.contains(temp[2])) {
										drops.add(temp[2]); // Add the dropoff to the HashSet
										elevatorLampChanges += temp[2] + " ";
									}
								} else {
									pickingUp = false; // Undo update GUI lamps if there's another pickup going the other way
								}
							}
						}
					}
					// Are there dropoffs for this floor?
					if (drops.contains(rPacketParsed[1])) {
						stop = true; // Will need to stop
						drops.remove(rPacketParsed[1]); // Remove; dropoff being handled

						// Turn off elevator lamp at this floor
						elevatorLampChanges += rPacketParsed[1] + " ";
					}

					// Perform operations based on above results and then reset
					if (stop) {
						// Send stop
						cPacket = createPacket(CMD, STOP, eport);
						eSocket.send(cPacket);

						// listen for ack to stop
						eSocket.receive(rPacket);

						// parsing ack
						rPacketParsed = parsePacket(rPacket.getData());

						// Send update to floor that the elevator has arrived
						if (floorLampUpdate) {
							sendPositionToFloor("" + currentFloor);
						}
						error = ERROR_DOOR_JAM; // Change error type

						// Sending open door
						cPacket = createPacket(CMD, DOOR_OPEN, eport);
						eSocket.send(cPacket);

						// listen for ack to open
						eSocket.receive(rPacket);

						// parsing ack
						rPacketParsed = parsePacket(rPacket.getData());

						// need to update lamps?
						if (!elevatorLampChanges.equals("")) {
							// send elevator lamp updates
							dPacket = createPacket(DATA, elevatorLampChanges, eport);
							eSocket.send(dPacket);

							// listen for ack
							eSocket.receive(rPacket);

							// parsing ack
							rPacketParsed = parsePacket(rPacket.getData());
						}

						// Sending close door
						cPacket = createPacket(CMD, DOOR_CLOSE, eport);
						eSocket.send(cPacket);

						// listen for ack to close
						eSocket.receive(rPacket);

						// parsing ack
						rPacketParsed = parsePacket(rPacket.getData());
					}

					// Is the elevator empty?
					if (drops.isEmpty()) {
						// End sequence
						status = "IDLE";
						liveDirection = "";
					} else {
						cPacket = createPacket(CMD, (dropDirection.equals("UP") ? UP : DOWN), eport);
						floorLampUpdate = false;
						stop = false;
						elevatorLampChanges = "";
						error = ERROR_STUCK;
					}
				}

			} catch (SocketTimeoutException ste) {
				ePacket = createPacket(ERROR, error, eport); // Sending error to elevator subsystem
				eSocket.send(ePacket);

				eSocket.receive(rPacket);
				currentFloor = (dropDirection.equals("UP")) ? currentFloor - 1 : currentFloor + 1;
					status = "SUSPENDED";

            }
		} catch (IOException ioe) {
			System.out.println(String.format("sub-%d: unable to communicate with elevator system, quitting.", this.id));
			ioe.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Send position to floor system so the lamp can be turned off.
	 *
	 * @param floor Floor number to send to the floor system
	 */
	public void sendPositionToFloor(String floor) {

		byte[] buffer = new byte[8];
		DatagramPacket sPacket = createPacket(CMD, ELEVATOR_ARRIVED, fport);
		DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);
		String[] rPacketParsed;

		try {
			DatagramSocket tempSocket = new DatagramSocket();
			tempSocket.send(sPacket);

			// Listen for ack
			tempSocket.receive(rPacket);

			// parsing ack
			rPacketParsed = parsePacket(rPacket.getData());

			// Send floor number
			sPacket = createPacket(DATA, floor, fport);
			tempSocket.send(sPacket);

			// Listen for ack
			tempSocket.receive(rPacket);

			// parsing ack
			rPacketParsed = parsePacket(rPacket.getData());

			tempSocket.close();
			return;

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
