package Tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class AllTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AllTest.suite());
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for Elevator Simulator system");
        suite.addTest(new TestSuite(FloorTest.class));
        suite.addTest(new TestSuite(SchedulerTest.class));
        suite.addTest(new TestSuite(ElevatorTest.class));
        return suite;
    }

}