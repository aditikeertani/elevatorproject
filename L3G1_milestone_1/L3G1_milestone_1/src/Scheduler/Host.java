/**
 * Class acting as the interface for the FloorSubsystem. Submits new requests to the Scheduler.
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
    /* ## ------------------------------ ## */

    public static int currentFloor = 0;
    BlockingQueue<String> upQueue = new ArrayBlockingQueue<String>(100);		// queue with up requests
    BlockingQueue<String> downQueue = new ArrayBlockingQueue<String>(100);		// queue with down requests

    final int HOSTPORT = 8008;
    final int elevatorPort = 3137;
    final int floorPort = 6520;
    DatagramSocket hostSocket;												// receiving from floor system on this socket
    DatagramSocket floorSocket;												// sending to floor on this socket


    /**
     * The Constructor for the Host Class
     */
    public Host() {

        try {
            hostSocket = new DatagramSocket(HOSTPORT);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }


    /**
     * The runnable method for the host class.
     */
    public void run() {
        System.out.println("main: running.");
        boolean listening = true;

        byte[] buffer = new byte[8];
        DatagramPacket recievePacket = new DatagramPacket(buffer, buffer.length);			// received packet

        // object performing elevator movement calculations in separate thread
        Scheduler scheduler = new Scheduler(elevatorPort, floorPort, upQueue, downQueue);
        scheduler.start();

        while (listening) {
            try {
                hostSocket.receive(recievePacket);
                System.out.println(String.format("main: received packet ( string >> %s, byte array >> %s ).\n", new String(recievePacket.getData()), recievePacket.getData()));

                // handling packet
                String[] recievePacketParsed = parsePacket(recievePacket.getData());

                // CMD
                if (recievePacketParsed[0].equals(CMD)) {

                    floorCommands(recievePacketParsed[1], recievePacket.getPort());
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
     * Method to handle command received from floor.
     *
     * @param cmd
     * @param port
     */
    public void floorCommands(String cmd, int port) {

        byte[] data = new byte[1024];
        DatagramPacket ackPacket = createPacket(ACK, cmd, port);
        DatagramPacket dPacket = new DatagramPacket(data, data.length);

        if (cmd.equals(FLOOR_BUTTON)) {
            try {
                floorSocket = new DatagramSocket();
                System.out.println("main: acking.");
                floorSocket.send(ackPacket);

                // get data
                hostSocket.receive(dPacket);

                // handling packet
                String[] dPacketParsed = parsePacket(dPacket.getData());
                System.out.println(String.format("main: received data ( string >> %s, byte array >> %s ).\n", new String(dPacket.getData()), dPacket.getData()));
                System.out.println(Arrays.toString(dPacketParsed));

                if (dPacketParsed[0].equals(DATA)) {
                    ackPacket = createPacket(ACK, dPacketParsed[1], port);
                    System.out.println("main: acking data.");
                    floorSocket.send(ackPacket);

                    // determine which queue to add request to
                    String[] temp = dPacketParsed[1].split(" ");
                    if (temp[2].equals("UP")) {
                        upQueue.add(dPacketParsed[1]);
                    }
                    else {
                        downQueue.add(dPacketParsed[1]);
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
     * Method to parse a packet.
     *
     * @param bytes
     * @return string array of data elements
     */
    public static String[] parsePacket(byte[] bytes) {

        String packet = new String(bytes);
        String[] parsed = packet.replaceFirst("\0", "").split("\0");

        return parsed;
    }


    /**
     * Creates and returns a DatagramPacket.
     *
     * @param packetType
     * @return packet with data
     */
    public static DatagramPacket createPacket(String packetType, String ins, int port) {
        String data;
        DatagramPacket packet = null;

        // error
        if (packetType == "0") {
            data = "\0" + ERROR + "\0" + ins + "\0";
        }
        // ack
        else if (packetType == "1") {
            data = "\0" + ACK + "\0" + ins + "\0";
        }
        // cmd
        else if (packetType == "2") {
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
            System.out.println("unable to create packet (UnknownHostException), exiting.");
            System.exit(1);
        }
        return packet;
    }


    /**
     * The main method for the host class
     *
     * @param args
     */
    public static void main(String[] args) {
        Host host = new Host();
        host.run();
    }
}



/**
 * Scheduler Thread class to handle elevator movements.
 * It uses a BlockingQueue to monitor new requests.
 *
 */
class Scheduler extends Thread {

    private DatagramSocket eSocket;				// socket to communicate with elevator system
    private int elevatorPort;							// port of elevator system
    private int floorPort;							// port of floor system
    private BlockingQueue<String> upQueue;			// queue with up requests
    private BlockingQueue<String> downQueue;		// queue with down requests


    /**
     * The schedulerConstructor.
     * It takes a port number, packet and an up and down request Queue.
     *
     * @param elevatorPort
     * @param floorPort
     * @param upQueue
     * @param downQueue
     */
    public Scheduler(int elevatorPort, int floorPort, BlockingQueue<String> upQueue, BlockingQueue<String> downQueue) {
        super("request thread");
        try {
            eSocket = new DatagramSocket();
            this.upQueue = upQueue;
            this.downQueue = downQueue;
            this.elevatorPort = elevatorPort;
            this.floorPort = floorPort;
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }


    /**
     * Runnable thread method. Executed on thread start.
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
                }
                else if (Integer.parseInt(sourceFloor) < Host.currentFloor) {
                    pIns = Host.DOWN_PICKUP;
                }
                else {
                    pIns = Host.STOP;
                }
                // drop off direction
                if (direction.equals("UP")) {
                    dIns = Host.UP_DROPOFF;
                }
                else if (direction.equals("DOWN")) {
                    dIns = Host.DOWN_DROPOFF;
                }
                else {
                    dIns = Host.STOP;
                }
                pickUp(pIns, request);
                dropOff(dIns, request);
            }
            else {
                continue;
            }
        }
        eSocket.close();
        return;
    }


    /**
     * Method doing the bulk of the socket operations for a pickup.
     *
     * @param ins
     * @param request
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
        DatagramPacket recievePacket = new DatagramPacket(buffer, buffer.length);
        String[] recievePacketParsed;

        System.out.println(String.format("sub: forwarding command packet ( string >> %s, byte array >> %s ).", new String(cPacket.getData()), cPacket.getData()));

        try {
            // send cmd
            eSocket.send(cPacket);

            // listen for ack
            eSocket.receive(ackPacket);

            // parsing ack
            String[] ackPacketParsed = Host.parsePacket(ackPacket.getData());
            System.out.println(String.format("sub: received ack ( string >> %s, byte array >> %s ).", new String(ackPacket.getData()), ackPacket.getData()));
            System.out.println(Arrays.toString(ackPacketParsed));

            // send elevator destination
            System.out.println(String.format("sub: sending elevator button ( string >> %s, byte array >> %s ).", new String(dPacket.getData()), dPacket.getData()));
            eSocket.send(dPacket);

            // listen for acknowledgement
            eSocket.receive(ackPacket);

            // parsing acknowledgement
            ackPacketParsed = Host.parsePacket(ackPacket.getData());
            System.out.println(String.format("sub: received ack ( string >> %s, byte array >> %s ).", new String(ackPacket.getData()), ackPacket.getData()));
            System.out.println(Arrays.toString(ackPacketParsed));

            // position updates
            while (keepMoving) {
                eSocket.receive(recievePacket);

                recievePacketParsed = Host.parsePacket(recievePacket.getData());
                System.out.println(String.format("sub: received positional update ( string >> %s, byte array >> %s ).", new String(recievePacket.getData()), recievePacket.getData()));
                System.out.println(Arrays.toString(recievePacketParsed));

                Host.currentFloor = Integer.parseInt(recievePacketParsed[1]);
                // if elevator is at required floor then stop
                if (recievePacketParsed[1].equals(sourceFloor)) {
                    cPacket = Host.createPacket(Host.CMD, Host.STOP, elevatorPort);
                    System.out.println(String.format("sub: sending stop ( string >> %s, byte array >> %s ).\n", new String(cPacket.getData()), cPacket.getData()));
                    keepMoving = false;
                }
                else {
                    cPacket = Host.createPacket(Host.ACK, recievePacketParsed[1], elevatorPort);
                    System.out.println(String.format("sub: sending continue ( string >> %s, byte array >> %s ).\n", new String(cPacket.getData()), cPacket.getData()));
                }
                eSocket.send(cPacket);
            }

            // listen for acknowledgement to stop
            eSocket.receive(ackPacket);

            // parsing acknowledgement
            ackPacketParsed = Host.parsePacket(ackPacket.getData());
            System.out.println(String.format("sub: received acknowledgement ( string >> %s, byte array >> %s ).", new String(ackPacket.getData()), ackPacket.getData()));
            System.out.println(Arrays.toString(ackPacketParsed));

            // send update to floor that the elevator has arrived
            sendPositionToFloor(sourceFloor);

            // sending open door
            cPacket = Host.createPacket(Host.CMD, Host.DOOR_OPEN, elevatorPort);
            System.out.println(String.format("sub: sending open door ( string >> %s, byte array >> %s ).\n", new String(cPacket.getData()), cPacket.getData()));
            eSocket.send(cPacket);

            // listen for ack to open
            eSocket.receive(ackPacket);

            // parsing ack
            ackPacketParsed = Host.parsePacket(ackPacket.getData());
            System.out.println(String.format("sub: received ack ( string >> %s, byte array >> %s ).", new String(ackPacket.getData()), ackPacket.getData()));
            System.out.println(Arrays.toString(ackPacketParsed));

            // sending close door
            cPacket = Host.createPacket(Host.CMD, Host.DOOR_CLOSE, elevatorPort);
            System.out.println(String.format("sub: sending close door ( string >> %s, byte array >> %s ).\n", new String(cPacket.getData()), cPacket.getData()));
            eSocket.send(cPacket);

            // listen for acknowledgement to close
            eSocket.receive(ackPacket);

            // parsing acknowledgement
            ackPacketParsed = Host.parsePacket(ackPacket.getData());
            System.out.println(String.format("sub: received acknowledgement - done ( string >> %s, byte array >> %s ).", new String(ackPacket.getData()), ackPacket.getData()));
            System.out.println(Arrays.toString(ackPacketParsed));
        }
        catch (Exception e) {
            System.out.println("sub: unable to communicate with elevator system, aborting request.");
            e.printStackTrace();
            return;
        }
    }


    /**
     * Performs the socket operations for a dropoff.
     *
     * @param ins
     * @param request
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
        DatagramPacket recievePacket = new DatagramPacket(buffer, buffer.length);
        String[] recievePacketParsed;

        System.out.println(String.format("sub: forwarding command packet ( string >> %s, byte array >> %s ).", new String(cPacket.getData()), cPacket.getData()));

        try {
            // send cmd
            eSocket.send(cPacket);

            // listen for acknowledgement
            eSocket.receive(ackPacket);

            // parsing acknowledgement
            String[] ackPacketParsed = Host.parsePacket(ackPacket.getData());
            System.out.println(String.format("sub: received ack ( string >> %s, byte array >> %s ).", new String(ackPacket.getData()), ackPacket.getData()));
            System.out.println(Arrays.toString(ackPacketParsed));

            // position updates
            while (keepMoving) {
                eSocket.receive(recievePacket);

                recievePacketParsed = Host.parsePacket(recievePacket.getData());
                System.out.println(String.format("sub: received position update ( string >> %s, byte array >> %s ).", new String(recievePacket.getData()), recievePacket.getData()));
                System.out.println(Arrays.toString(recievePacketParsed));

                Host.currentFloor = Integer.parseInt(recievePacketParsed[1]);
                // if elevator is at required floor then stop
                if (recievePacketParsed[1].equals(destFloor)) {
                    cPacket = Host.createPacket(Host.CMD, Host.STOP, elevatorPort);
                    System.out.println(String.format("sub: sending stop ( string >> %s, byte array >> %s ).\n", new String(cPacket.getData()), cPacket.getData()));
                    keepMoving = false;
                }
                else {
                    cPacket = Host.createPacket(Host.ACK, recievePacketParsed[1], elevatorPort);
                    System.out.println(String.format("sub: sending continue ( string >> %s, byte array >> %s ).\n", new String(cPacket.getData()), cPacket.getData()));
                }
                eSocket.send(cPacket);
            }

            // listen for acknowledgement to stop
            eSocket.receive(ackPacket);

            // parsing acknowledgement
            ackPacketParsed = Host.parsePacket(ackPacket.getData());
            System.out.println(String.format("sub: received acknowledgement ( string >> %s, byte array >> %s ).", new String(ackPacket.getData()), ackPacket.getData()));
            System.out.println(Arrays.toString(ackPacketParsed));

            // sending open door
            cPacket = Host.createPacket(Host.CMD, Host.DOOR_OPEN, elevatorPort);
            System.out.println(String.format("sub: sending open door ( string >> %s, byte array >> %s ).\n", new String(cPacket.getData()), cPacket.getData()));
            eSocket.send(cPacket);

            // listen for acknowledgement to open
            eSocket.receive(ackPacket);

            // parsing acknowledgement
            ackPacketParsed = Host.parsePacket(ackPacket.getData());
            System.out.println(String.format("sub: received acknowledgement ( string >> %s, byte array >> %s ).", new String(ackPacket.getData()), ackPacket.getData()));
            System.out.println(Arrays.toString(ackPacketParsed));

            // sending close door
            cPacket = Host.createPacket(Host.CMD, Host.DOOR_CLOSE, elevatorPort);
            System.out.println(String.format("sub: sending close door ( string >> %s, byte array >> %s ).\n", new String(cPacket.getData()), cPacket.getData()));
            eSocket.send(cPacket);

            // listen for acknowledgment to close
            eSocket.receive(ackPacket);

            // parsing acknowledgement
            ackPacketParsed = Host.parsePacket(ackPacket.getData());
            System.out.println(String.format("sub: received acknowledgement - done ( string >> %s, byte array >> %s ).", new String(ackPacket.getData()), ackPacket.getData()));
            System.out.println(Arrays.toString(ackPacketParsed));
        }
        catch (Exception e) {
            System.out.println("sub: unable to communicate with elevator system, aborting request.");
            e.printStackTrace();
            return;
        }
    }


    /**
     * Send position to floor system so the lamp can be turned off.
     *
     * @param floor
     */
    public void sendPositionToFloor(String floor) {

        byte[] buffer = new byte[8];
        DatagramPacket sendPacket = Host.createPacket(Host.CMD, Host.ELEVATOR_ARRIVED, floorPort);
        DatagramPacket recievePacket = new DatagramPacket(buffer, buffer.length);
        String[] recievePacketParsed;


        try {
            DatagramSocket tempSocket = new DatagramSocket();
            System.out.println(String.format("sub: sending position update to floor ( string >> %s, byte array >> %s ).\n", new String(sendPacket.getData()), sendPacket.getData()));
            tempSocket.send(sendPacket);

            // listen for acknowledgement
            tempSocket.receive(recievePacket);

            // parsing acknowledgement
            recievePacketParsed = Host.parsePacket(recievePacket.getData());
            System.out.println(String.format("sub: received acknowledgement ( string >> %s, byte array >> %s ).", new String(recievePacket.getData()), recievePacket.getData()));
            System.out.println(Arrays.toString(recievePacketParsed));

            // sending floor number
            sendPacket = Host.createPacket(Host.DATA, floor, floorPort);
            System.out.println(String.format("sub: sending floor number ( string >> %s, byte array >> %s ).\n", new String(sendPacket.getData()), sendPacket.getData()));
            tempSocket.send(sendPacket);

            // listen for acknowledgement
            tempSocket.receive(recievePacket);

            // parsing acknowledgement
            recievePacketParsed = Host.parsePacket(recievePacket.getData());
            System.out.println(String.format("sub: received acknowledgement - done ( string >> %s, byte array >> %s ).", new String(recievePacket.getData()), recievePacket.getData()));
            System.out.println(Arrays.toString(recievePacketParsed));

            tempSocket.close();
            return;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}


