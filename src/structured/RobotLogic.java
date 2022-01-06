package structured;
import battlecode.common.*;
import java.util.Random;
public abstract class RobotLogic {
	public static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };
	public static final Random rng = new Random(6147);
	abstract boolean run(RobotController rc) throws GameActionException;
	
	//returns true if it successfully moves, false if it doesn't
	public boolean follow(RobotController rc, int id) throws GameActionException{
		RobotInfo follow=null;
		if(rc.canSenseRobot(id))
        	follow=rc.senseRobot(id);
        //moves in the miner's direction
        if(follow!=null) {
        	Direction dir=rc.getLocation().directionTo(follow.getLocation());
        	rc.setIndicatorString("attempting to follow "+dir);
        	if(rc.canMove(dir)) {
        		rc.move(dir);
            	return true;
        	}else if(rc.canMove(dir.rotateLeft())) { //if it can't move in the direction, tries to move 45 degrees to the left
        		rc.move(dir.rotateLeft());
            	return true;
        	}else if(rc.canMove(dir.rotateRight())) {//tries to move 45 degrees to the right
        		rc.move(dir.rotateRight());
            	return true;
        	}
        }
        return false;
	}
	
    public int locToComm(MapLocation loc) {
    	return 60*loc.x + loc.y;
    }
    
    public MapLocation commToLoc(int commVal) {
    	int y = commVal % 60;
    	int x = commVal-y / 60;
    	return new MapLocation(x, y);
    }

}
