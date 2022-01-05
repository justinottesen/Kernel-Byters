package structured;
import battlecode.common.*;
public class Archon extends RobotLogic {
	private static final RobotType[] buildOrder={
	    	RobotType.MINER,
	    	RobotType.BUILDER,
	    	
	    	RobotType.SOLDIER
	};
	static int buildNum=0;
	public boolean run(RobotController rc) throws GameActionException{
		Direction dir = directions[1];
    	if (rc.canBuildRobot(buildOrder[buildNum], dir)) {
            rc.buildRobot(buildOrder[buildNum], dir);
            buildNum++;
            if(buildNum>=buildOrder.length)
            	buildNum=0;
        }
    	return true;
	}
}
