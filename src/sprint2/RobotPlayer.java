package sprint2;
import battlecode.common.*;
public class RobotPlayer {
	private static RobotLogic robot;
	@SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
		switch (rc.getType()) {
	        case ARCHON:     robot=new Archon();  break;
	        case MINER:      robot=new Miner();   break;
	        case SOLDIER:    robot=new Soldier(); break;
	        case LABORATORY: 
	        case WATCHTOWER: robot=new WatchTower(); break;
	        case BUILDER:	robot=new Builder(); break;
	        case SAGE:       break;
		}

        while (true) {
            try {
                robot.run(rc);
            } catch (GameActionException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
        }
    }
}
