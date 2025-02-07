package FloorSubsystem;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import FloorSubsystem.FloorControl.Direction;

/**
 * This class is used to handle an input file filled with elevator requests.
 * The requests are placed into an array, and sent to the Scheduler based on the request timestamp
 *
 * @author Oluwatobi Olwookere, Nkechi Chukwuma
 */

public class HandleUserInput implements Runnable {

	DatagramSocket requestSocket;
	ArrayList<String> serviceReqList = new ArrayList<String>();
	FloorControl fs;

	/**
	 * Constructor to initialize the HandleUserInputHandler with the floor control subsystem.
	 * @param floorSubsystem The floor control subsystem instance.
	 */
	public HandleUserInput(FloorControl floorSubsystem) {
		fs = floorSubsystem;
        try {
        	requestSocket = new DatagramSocket();
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }
	}

	/**
	 * Parses the input file containing elevator requests.
	 * @param filePath The path to the input file.
	 * @param list The list to store parsed requests.
	 * @throws FileNotFoundException If the input file is not found.
	 * @throws IOException If an I/O error occurs while reading the file.
	 */
	public void parseInputFile(String filePath, ArrayList<String> list) throws FileNotFoundException, IOException {

		BufferedReader br = new BufferedReader(new FileReader(filePath));
		String line = null;
		while ((line = br.readLine()) != null) {
			Pattern pattern = Pattern.compile("\\d{2}:\\d{2}:\\d{2}.\\d*\\s\\d{1,2}\\s[A-z]{2,4}\\s\\d{1,2}");
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				list.add(new String(matcher.group()));
				System.out.println("UserInputHandler: Got a new service request! Adding to queue... " + list.get(list.size() - 1));
			}
		}
		br.close();
	}

	/**
	 * Parses the list of service request strings and sends each request to the scheduler.
	 * @param list The list of service request strings.
	 */
	public void parseServiceReqs(ArrayList<String> list) {
		int hour = 0;
		int min = 0;
		int sec = 0;
		int mSec = 0;
		int timeTotal = 0;
		int timeTotalPrev = 0;
		int timeDiff = 0;
		int startFloor = 0;
		int destFloor = 0;
		Direction targetDirection = Direction.IDLE;

//		Parse the previously generated list and separate each line into variables
		for (String s : list) {
			String data[] = s.split(" ");
			String time[] = data[0].split("[:.]");
			hour = Integer.parseInt(time[0]);
			min = Integer.parseInt(time[1]);
			sec = Integer.parseInt(time[2]);
			mSec = Integer.parseInt(time[3]);
//			Convert the time variables into a single time value in milliseconds
	        timeTotal = hour*60*60*1000 +
	                    min*60*1000 +
	                    sec*1000 +
	                    mSec;
	        if (timeTotalPrev == 0) timeDiff = 0;
	        else timeDiff = timeTotal - timeTotalPrev;
	        timeTotalPrev = timeTotal;
			startFloor = Integer.parseInt(data[1]);
			if (data[2].toUpperCase().equals("UP")) targetDirection = Direction.UP;
			else if (data[2].toUpperCase().equals("DOWN")) targetDirection = Direction.DOWN;
			destFloor = Integer.parseInt(data[3]);

			if (timeDiff != 0) {
				try {
					Thread.sleep(timeDiff);
				} catch (InterruptedException e ) {
					e.printStackTrace();
					System.exit(1);
				}
			}

			byte[] msg = new byte[100];

//			Iterate through the system's floors to find the start floor, then send the input request to the
//			scheduler and turn the floor's direction lamp on
			for (Floor f : fs.floors) {
				if (f.getFloorNum() == startFloor) {
					fs.sendServiceRequest(data[0], startFloor, destFloor, targetDirection, requestSocket);
					if (targetDirection == Direction.UP) f.setUpLampOn();
					else if (targetDirection == Direction.DOWN) f.setDownLampOn();
				}
			}
		}
	}

	/**
	 * Run method to start processing the input file and sending requests to the scheduler.
	 */
	public void run() {

		try {
			parseInputFile("src/FloorSubsystem/input.txt", serviceReqList);
		} catch (IOException e) {
			e.printStackTrace();
            System.exit(1);
		}

		parseServiceReqs(serviceReqList);

	}

}