package structured;
import battlecode.common.*;
public class Archon extends RobotLogic {
	private static int turnNum = 0;
	private static int previous = -1;
	private static int orderValue = 0;
	private static final RobotType[] buildOrder={
	    	RobotType.MINER,
	    	RobotType.BUILDER,
	    	
	    	RobotType.SOLDIER
	};
	private static int buildNum=0;
	public void communicate(RobotController rc) throws GameActionException {
		int commVal = locToComm(rc.getLocation());
		if (turnNum%4 == orderValue && rc.readSharedArray(0) == previous) {
			rc.writeSharedArray(0, commVal);
		} else if (turnNum%4 == orderValue) {
			orderValue ++;
		}
	}
	public boolean run(RobotController rc) throws GameActionException{
		turnNum ++;
		Direction dir = directions[1];
    	if (rc.canBuildRobot(buildOrder[buildNum], dir)) {
            rc.buildRobot(buildOrder[buildNum], dir);
            buildNum++;
            if(buildNum>=buildOrder.length)
            	buildNum=0;
        }
    	previous = rc.readSharedArray(0);
    	return true;
	}
}
