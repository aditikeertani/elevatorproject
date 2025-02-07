package scheduler;

import java.awt.*;
import javax.swing.*;

/**
 * Represents the graphical user interface (GUI) for the elevator simulation.
 * Provides visual representation of elevators, floors, direction indicators, and lamps.
 *
 * @author Nkechi Chukwuma, Jennifer Ogidi-Gbebaje
 */
public class ElevatorGUI extends JFrame {
	// Instance variables declaration
	private JPanel panel;
	private JTextField[][] elevatorFloors = new JTextField[22][4];
	private JTextField[][] elevatorDirections = new JTextField[2][4];
	private JTextField[] lamps = new JTextField[4];

	/**
	 * Constructor for creating the ElevatorGUI.
	 * Initializes the GUI components and sets up the JFrame.
	 */
	public ElevatorGUI() {
		panel = new JPanel();
		// Grid layout format
		panel.setLayout(new GridLayout(28, 4, 20, 0));

		// Adding labels for each elevator
		JLabel E1 = new JLabel("Elevator 1");
		panel.add(E1);

		JLabel E2 = new JLabel("Elevator 2");
		panel.add(E2);

		JLabel E3 = new JLabel("Elevator 3");
		panel.add(E3);

		JLabel E4 = new JLabel("Elevator 4");
		panel.add(E4);

		// Adding floors for each elevator
		for (int i = 0; i < 22; i++) {
			for (int j = 0; j < 4; j++) {
				elevatorFloors[i][j] = new JTextField(String.format("%d", 22 - i));
				elevatorFloors[i][j].setBackground(Color.gray); // Color of floors is gray
				panel.add(elevatorFloors[i][j]);
				elevatorFloors[i][j].setEnabled(false);
			}
		}

		// Adding direction indicators for each elevator (UP or DOWN)
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 4; j++) {
				if (i % 2 == 0) {
					elevatorDirections[i][j] = new JTextField("^"); // UP symbol
				} else {
					elevatorDirections[i][j] = new JTextField("v"); // DOWN symbol
				}
				panel.add(elevatorDirections[i][j]);
				elevatorDirections[i][j].setEnabled(false);
			}
		}

		// Adding lamps for each elevator
		for (int i = 0; i < 4; i++) {
			lamps[i] = new JTextField("Lamp");
			panel.add(lamps[i]);
			lamps[i].setEnabled(false);
		}

		// Configuring JFrame properties
		this.setVisible(true);
		panel.setVisible(true);
		panel.setBackground(Color.DARK_GRAY);
		this.add(panel);
		this.setTitle("Elevator Simulator");
		this.setBackground(Color.BLACK);
		this.setSize(400, 750);
		this.setResizable(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	/**
	 * Getter method to retrieve the JTextField representing a specific elevator floor.
	 *
	 * @param i The index of the elevator floor (0-21).
	 * @param j The index of the elevator (0-3).
	 * @return The JTextField representing the elevator floor.
	 */
	public JTextField getElevatorFloors(int i, int j) {
		return elevatorFloors[i][j];
	}

	/**
	 * Getter method to retrieve the JTextField representing a specific elevator direction.
	 *
	 * @param i The index of the elevator direction (0-1).
	 * @param j The index of the elevator (0-3).
	 * @return The JTextField representing the elevator direction.
	 */
	public JTextField getElevatorDirections(int i, int j) {
		return elevatorDirections[i][j];
	}
}
