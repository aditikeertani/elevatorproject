/**
 * Class acting as the interface for the FloorSubsystem. Submits new requests to the Scheduler.
 * This class receives commands from the floor system, processes them, and communicates with the elevator system.
 * It utilizes multithreading to handle requests concurrently.
 *
 * @author Aditi Keertani, Ryan Capstick, Oluwatobi Olwookere
 */
package Scheduler;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

public class Host {

    // Constants for header and command identifiers
    public static final String ACK = "1";
    public static final String CMD = "2";
    public static final String DATA = "3";
    public static final String ERROR = "0";

    // Constants representing various commands and states
    public static final String FLOOR_BUTTON = "0x10";        // button was pressed on floor
    public static final String ELEVATOR_ARRIVED = "0x11";    // elevator arrived
    public static final String UP_PICKUP = "0x33";           // going up to pick up
    public static final String UP_DROPOFF = "0x32";          // going up dropoff
    public static final String DOWN_PICKUP = "0x31";         // going down to pickup
    public static final String DOWN_DROPOFF = "0x30";        // going down to dropoff
    public static final String DOOR_OPEN = "0x3A";           // open door
    public static final String DOOR_CLOSE = "0x3B";          // close door
    public static final String STOP = "0x3C";                // stop elevator

    // Current floor of the elevator
    public static int currentFloor = 0;

    // Queues to store up and down requests
    BlockingQueue<String> upQueue = new ArrayBlockingQueue<>(100);        // queue with up requests
    BlockingQueue<String> downQueue = new ArrayBlockingQueue<>(100);      // queue with down requests

    final int HOSTPORT = 8008;      // Port for communication with the floor system
    final int elevatorPort = 3137;  // Port for communication with the elevator system
    final int floorPort = 6520;     // Port for communication with the floor system
    DatagramSocket hostSocket;      // Socket for receiving from the floor system
    DatagramSocket floorSocket;     // Socket for sending to the floor system

    /**
     * Constructor for the Host Class. Initializes the hostSocket.
     */
    public Host() {
        try {
            hostSocket = new DatagramSocket(HOSTPORT);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * The runnable method for the host class. Listens for commands from the floor system and processes them.
     */
    public void run() {
        System.out.println("main: running.");
        boolean listening = true;

        byte[] buffer = new byte[8];
        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);    // received packet

        // Object performing elevator movement calculations in a separate thread
        Scheduler scheduler = new Scheduler(elevatorPort, floorPort, upQueue, downQueue);
        scheduler.start();

        while (listening) {
            try {
                hostSocket.receive(receivePacket);
                System.out.printf("main: received packet (string >> %s, byte array >> %s).\n%n", new String(receivePacket.getData()), Arrays.toString(receivePacket.getData()));

                // Handling packet
                String[] receivePacketParsed = parsePacket(receivePacket.getData());

                // CMD
                if (receivePacketParsed[0].equals(CMD)) {
                    floorCommands(receivePacketParsed[1], receivePacket.getPort());
                } else {
                    // Unknown packet
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
     * Method to handle command received from the floor.
     *
     * @param cmd Command received from the floor system
     * @param port Port on which the command was received
     */
    public void floorCommands(String cmd, int port) {
        byte[] data = new byte[1024];
        DatagramPacket ackPacket = createPacket(ACK, cmd, port);
        DatagramPacket dPacket = new DatagramPacket(data, data.length);

        if (cmd.equals(FLOOR_BUTTON)) {
            try {
                floorSocket = new DatagramSocket();
                System.out.println("main: acknowledging.");
                floorSocket.send(ackPacket);

                // Get data
                hostSocket.receive(dPacket);

                // Handling packet
                String[] dPacketParsed = parsePacket(dPacket.getData());
                System.out.printf("main: received data (string >> %s, byte array >> %s).\n%n", new String(dPacket.getData()), Arrays.toString(dPacket.getData()));
                System.out.println(Arrays.toString(dPacketParsed));

                if (dPacketParsed[0].equals(DATA)) {
                    ackPacket = createPacket(ACK, dPacketParsed[1], port);
                    System.out.println("main: acknowledging data.");
                    floorSocket.send(ackPacket);

                    // Determine which queue to add request to
                    String[] temp = dPacketParsed[1].split(" ");
                    if (temp[2].equals("UP")) {
                        upQueue.add(dPacketParsed[1]);
                    } else {
                        downQueue.add(dPacketParsed[1]);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * Method to parse a packet.
     *
     * @param bytes Byte array of the packet
     * @return String array of data elements
     */
    public static String[] parsePacket(byte[] bytes) {
        String packet = new String(bytes);
        return packet.replaceFirst("\0", "").split("\0");
    }

    /**
     * Creates and returns a DatagramPacket.
     *
     * @param packetType Type of packet (ACK, CMD, DATA, ERROR)
     * @param ins        Instruction or data to be sent in the packet
     * @param port       Port to which the packet will be sent
     * @return DatagramPacket with the specified data
     */
    public static DatagramPacket createPacket(String packetType, String ins, int port) {
        String data;
        DatagramPacket packet = null;

        // Constructing packet based on packetType
        data = switch (packetType) {
            case "0" -> "\0" + ERROR + "\0" + ins + "\0";    // error
            case "1" -> "\0" + ACK + "\0" + ins + "\0";      // ack
            case "2" -> "\0" + CMD + "\0" + ins + "\0";      // cmd
            default -> "\0" + DATA + "\0" + ins + "\0";     // data
        };

        try {
            packet = new DatagramPacket(data.getBytes(), data.getBytes().length, InetAddress.getLocalHost(), port);
        } catch (UnknownHostException uhe) {
            System.out.println("unable to create packet (UnknownHostException), exiting.");
            System.exit(1);
        }
        return packet;
    }

    /**
     * The main method for the host class.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        Host host = new Host();
        host.run();
    }
}


/**
 * Scheduler Thread class to handle elevator movements.
 * It uses a BlockingQueue to monitor new requests.
 */
class Scheduler extends Thread {

    private DatagramSocket eSocket;            // socket to communicate with elevator system
    private int elevatorPort;                  // port of elevator system
    private int floorPort;                     // port of floor system
    private BlockingQueue<String> upQueue;     // queue with up requests
    private BlockingQueue<String> downQueue;   // queue with down requests

    /**
     * Constructor for the Scheduler class.
     * Initializes the socket, ports, and queues.
     *
     * @param elevatorPort Port number for communication with the elevator system
     * @param floorPort    Port number for communication with the floor system
     * @param upQueue      Queue containing up requests
     * @param downQueue    Queue containing down requests
     */
    public Scheduler(int elevatorPort, int floorPort, BlockingQueue<String> upQueue, BlockingQueue<String> downQueue) {
        super("request thread");
        try {
            eSocket = new DatagramSocket();
            this.upQueue = upQueue;
            this.downQueue = downQueue;
            this.elevatorPort = elevatorPort;
            this.floorPort = floorPort;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Runnable thread method. Executed on thread start.
     * Monitors the request queues for new requests and handles elevator movements accordingly.
     */
    @Override
    public void run() {
        boolean running = true;
        String pIns, dIns;
        String sourceFloor, direction;
        String[] parsedData;
        System.out.println("sub: a new thread is running.");

        while (running) {
            String uq = upQueue.poll();
            String dq = downQueue.poll();
            String request = (dq == null) ? uq : dq;

            if (request != null) {
                parsedData = request.split(" ");
                sourceFloor = parsedData[1];
                direction = parsedData[2];

                if (Integer.parseInt(sourceFloor) > Host.currentFloor) {
                    pIns = Host.UP_PICKUP;
                } else if (Integer.parseInt(sourceFloor) < Host.currentFloor) {
                    pIns = Host.DOWN_PICKUP;
                } else {
                    pIns = Host.STOP;
                }
                // drop off direction
                if (direction.equals("UP")) {
                    dIns = Host.UP_DROPOFF;
                } else if (direction.equals("DOWN")) {
                    dIns = Host.DOWN_DROPOFF;
                } else {
                    dIns = Host.STOP;
                }
                pickUp(pIns, request);
                dropOff(dIns, request);
            }
        }
        eSocket.close();
    }

    /**
     * Method handling the pickup operation.
     * Sends commands to the elevator system to pick up passengers.
     *
     * @param ins     Instruction for the elevator (pickup direction)
     * @param request Request data containing source and destination floors
     */
    public void pickUp(String ins, String request) {
        String sourceFloor, destFloor;
        String[] parsedData;
        boolean keepMoving = true;

        parsedData = request.split(" ");
        sourceFloor = parsedData[1];
        destFloor = parsedData[3];

        byte[] buffer = new byte[8];
        DatagramPacket cPacket = Host.createPacket(Host.CMD, ins, elevatorPort);
        DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length);
        DatagramPacket dPacket = Host.createPacket(Host.DATA, destFloor, elevatorPort);
        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
        String[] receivePacketParsed;

        System.out.printf("sub: forwarding command packet ( string >> %s, byte array >> %s ).%n", new String(cPacket.getData()), cPacket.getData());

        try {
            // send cmd
            eSocket.send(cPacket);

            // listen for ack
            eSocket.receive(ackPacket);

            // parsing ack
            String[] ackPacketParsed = Host.parsePacket(ackPacket.getData());
            System.out.printf("sub: received ack ( string >> %s, byte array >> %s ).%n", new String(ackPacket.getData()), ackPacket.getData());
            System.out.println(Arrays.toString(ackPacketParsed));

            // send elevator destination
            System.out.printf("sub: sending elevator button ( string >> %s, byte array >> %s ).%n", new String(dPacket.getData()), dPacket.getData());
            eSocket.send(dPacket);

            // listen for acknowledgement
            eSocket.receive(ackPacket);

            // parsing acknowledgement
            ackPacketParsed = Host.parsePacket(ackPacket.getData());
            System.out.printf("sub: received ack ( string >> %s, byte array >> %s ).%n", new String(ackPacket.getData()), ackPacket.getData());
            System.out.println(Arrays.toString(ackPacketParsed));

            // position updates
            while (keepMoving) {
                eSocket.receive(receivePacket);

                receivePacketParsed = Host.parsePacket(receivePacket.getData());
                System.out.printf("sub: received positional update ( string >> %s, byte array >> %s ).%n", new String(receivePacket.getData()), receivePacket.getData());
                System.out.println(Arrays.toString(receivePacketParsed));

                Host.currentFloor = Integer.parseInt(receivePacketParsed[1]);
                // if elevator is at required floor then stop
                if (receivePacketParsed[1].equals(sourceFloor)) {
                    cPacket = Host.createPacket(Host.CMD, Host.STOP, elevatorPort);
                    System.out.printf("sub: sending stop ( string >> %s, byte array >> %s ).\n%n", new String(cPacket.getData()), cPacket.getData());
                    keepMoving = false;
                } else {
                    cPacket = Host.createPacket(Host.ACK, receivePacketParsed[1], elevatorPort);
                    System.out.printf("sub: sending continue ( string >> %s, byte array >> %s ).\n%n", new String(cPacket.getData()), cPacket.getData());
                }
                eSocket.send(cPacket);
            }

            // listen for acknowledgement to stop
            eSocket.receive(ackPacket);

            // parsing acknowledgement
            ackPacketParsed = Host.parsePacket(ackPacket.getData());
            System.out.printf("sub: received acknowledgement ( string >> %s, byte array >> %s ).%n", new String(ackPacket.getData()), ackPacket.getData());
            System.out.println(Arrays.toString(ackPacketParsed));

            // send update to floor that the elevator has arrived
            sendPositionToFloor(sourceFloor);

            // sending open door
            cPacket = Host.createPacket(Host.CMD, Host.DOOR_OPEN, elevatorPort);
            System.out.printf("sub: sending open door ( string >> %s, byte array >> %s ).\n%n", new String(cPacket.getData()), cPacket.getData());
            eSocket.send(cPacket);

            // listen for ack to open
            eSocket.receive(ackPacket);

            // parsing ack
            ackPacketParsed = Host.parsePacket(ackPacket.getData());
            System.out.printf("sub: received ack ( string >> %s, byte array >> %s ).%n", new String(ackPacket.getData()), ackPacket.getData());
            System.out.println(Arrays.toString(ackPacketParsed));

            // sending close door
            cPacket = Host.createPacket(Host.CMD, Host.DOOR_CLOSE, elevatorPort);
            System.out.printf("sub: sending close door ( string >> %s, byte array >> %s ).\n%n", new String(cPacket.getData()), cPacket.getData());
            eSocket.send(cPacket);

            // listen for acknowledgement to close
            eSocket.receive(ackPacket);

            // parsing acknowledgement
            ackPacketParsed = Host.parsePacket(ackPacket.getData());
            System.out.printf("sub: received acknowledgement - done ( string >> %s, byte array >> %s ).%n", new String(ackPacket.getData()), ackPacket.getData());
            System.out.println(Arrays.toString(ackPacketParsed));
        } catch (Exception e) {
            System.out.println("sub: unable to communicate with elevator system, aborting request.");
            e.printStackTrace();
        }
    }

    /**
     * Method handling the drop-off operation.
     * Sends commands to the elevator system to drop off passengers.
     *
     * @param ins     Instruction for the elevator (drop-off direction)
     * @param request Request data containing source and destination floors
     */
    public void dropOff(String ins, String request) {
        String destFloor;
        String[] parsedData;
        boolean keepMoving = true;

        parsedData = request.split(" ");
        destFloor = parsedData[3];

        byte[] buffer = new byte[8];
        DatagramPacket cPacket = Host.createPacket(Host.CMD, ins, elevatorPort);
        DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length);
        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
        String[] receivePacketParsed;

        System.out.printf("sub: forwarding command packet ( string >> %s, byte array >> %s ).%n", new String(cPacket.getData()), cPacket.getData());

        try {
            // send cmd
            eSocket.send(cPacket);

            // listen for acknowledgement
            eSocket.receive(ackPacket);

            // parsing acknowledgement
            String[] ackPacketParsed = Host.parsePacket(ackPacket.getData());
            System.out.printf("sub: received ack ( string >> %s, byte array >> %s ).%n", new String(ackPacket.getData()), ackPacket.getData());
            System.out.println(Arrays.toString(ackPacketParsed));

            // position updates
            while (keepMoving) {
                eSocket.receive(receivePacket);

                receivePacketParsed = Host.parsePacket(receivePacket.getData());
                System.out.printf("sub: received position update ( string >> %s, byte array >> %s ).%n", new String(receivePacket.getData()), receivePacket.getData());
                System.out.println(Arrays.toString(receivePacketParsed));

                Host.currentFloor = Integer.parseInt(receivePacketParsed[1]);
                // if elevator is at required floor then stop
                if (receivePacketParsed[1].equals(destFloor)) {
                    cPacket = Host.createPacket(Host.CMD, Host.STOP, elevatorPort);
                    System.out.printf("sub: sending stop ( string >> %s, byte array >> %s ).\n%n", new String(cPacket.getData()), cPacket.getData());
                    keepMoving = false;
                } else {
                    cPacket = Host.createPacket(Host.ACK, receivePacketParsed[1], elevatorPort);
                    System.out.printf("sub: sending continue ( string >> %s, byte array >> %s ).\n%n", new String(cPacket.getData()), cPacket.getData());
                }
                eSocket.send(cPacket);
            }

            // listen for acknowledgement to stop
            eSocket.receive(ackPacket);

            // parsing acknowledgement
            ackPacketParsed = Host.parsePacket(ackPacket.getData());
            System.out.printf("sub: received acknowledgement ( string >> %s, byte array >> %s ).%n", new String(ackPacket.getData()), ackPacket.getData());
            System.out.println(Arrays.toString(ackPacketParsed));

            // sending open door
            cPacket = Host.createPacket(Host.CMD, Host.DOOR_OPEN, elevatorPort);
            System.out.printf("sub: sending open door ( string >> %s, byte array >> %s ).\n%n", new String(cPacket.getData()), cPacket.getData());
            eSocket.send(cPacket);

            // listen for acknowledgement to open
            eSocket.receive(ackPacket);

            // parsing acknowledgement
            ackPacketParsed = Host.parsePacket(ackPacket.getData());
            System.out.printf("sub: received acknowledgement ( string >> %s, byte array >> %s ).%n", new String(ackPacket.getData()), ackPacket.getData());
            System.out.println(Arrays.toString(ackPacketParsed));

            // sending close door
            cPacket = Host.createPacket(Host.CMD, Host.DOOR_CLOSE, elevatorPort);
            System.out.printf("sub: sending close door ( string >> %s, byte array >> %s ).\n%n", new String(cPacket.getData()), cPacket.getData());
            eSocket.send(cPacket);

            // listen for acknowledgment to close
            eSocket.receive(ackPacket);

            // parsing acknowledgement
            ackPacketParsed = Host.parsePacket(ackPacket.getData());
            System.out.printf("sub: received acknowledgement - done ( string >> %s, byte array >> %s ).%n", new String(ackPacket.getData()), ackPacket.getData());
            System.out.println(Arrays.toString(ackPacketParsed));
        } catch (Exception e) {
            System.out.println("sub: unable to communicate with elevator system, aborting request.");
            e.printStackTrace();
        }
    }

    /**
     * Sends the position update to the floor system indicating the elevator has arrived.
     *
     * @param floor The floor where the elevator has arrived
     */
    public void sendPositionToFloor(String floor) {
        byte[] buffer = new byte[8];
        DatagramPacket sendPacket = Host.createPacket(Host.CMD, Host.ELEVATOR_ARRIVED, floorPort);
        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
        String[] receivePacketParsed;

        try {
            DatagramSocket tempSocket = new DatagramSocket();
            System.out.printf("sub: sending position update to floor ( string >> %s, byte array >> %s ).\n%n", new String(sendPacket.getData()), sendPacket.getData());
            tempSocket.send(sendPacket);

            // listen for acknowledgement
            tempSocket.receive(receivePacket);

            // parsing acknowledgement
            receivePacketParsed = Host.parsePacket(receivePacket.getData());
            System.out.printf("sub: received acknowledgement ( string >> %s, byte array >> %s ).%n", new String(receivePacket.getData()), receivePacket.getData());
            System.out.println(Arrays.toString(receivePacketParsed));

            // sending floor number
            sendPacket = Host.createPacket(Host.DATA, floor, floorPort);
            System.out.printf("sub: sending floor number ( string >> %s, byte array >> %s ).\n%n", new String(sendPacket.getData()), sendPacket.getData());
            tempSocket.send(sendPacket);

            // listen for acknowledgement
            tempSocket.receive(receivePacket);

            // parsing acknowledgement
            receivePacketParsed = Host.parsePacket(receivePacket.getData());
            System.out.printf("sub: received acknowledgement - done ( string >> %s, byte array >> %s ).%n", new String(receivePacket.getData()), receivePacket.getData());
            System.out.println(Arrays.toString(receivePacketParsed));

            tempSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
