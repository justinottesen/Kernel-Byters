package structured;
import battlecode.common.*;
public class Soldier extends RobotLogic {
	private static MapLocation assignment=null;
	public boolean run(RobotController rc) throws GameActionException{
		//attack anyone within range
		makeLikeTheFireNation(rc);

		//move
        shmovement(rc);
        return true;
	}
	private void makeLikeTheFireNation(RobotController rc) throws GameActionException{
		// Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length > 0) {
            MapLocation toAttack = enemies[0].location;
            if (rc.canAttack(toAttack)) {
                rc.attack(toAttack);
            }
        }
	}
	private void shmovement(RobotController rc) throws GameActionException{
		if(assignment!=null) {//if has assignment
			//move towards assignment
			//also effectively stays at assignment once there
			rc.setIndicatorString("assigned location: "+assignment);
			MapLocation me = rc.getLocation();
			Direction dir=me.directionTo(assignment);
			if(rc.canMove(dir)) {
        		rc.move(dir);
        	}else if(rc.canMove(dir.rotateLeft())) { //if it can't move in the direction, tries to move 45 degrees to the left
        		rc.move(dir.rotateLeft());
        	}else if(rc.canMove(dir.rotateRight())) {//tries to move 45 degrees to the right
        		rc.move(dir.rotateRight());
        	}
		}else {
			//move randomly
			rc.setIndicatorString("no assigned location, moving randomly");
			Direction dir = directions[rng.nextInt(directions.length)];
	        if (rc.canMove(dir)) {
	            rc.move(dir);
	        }
		}
			
	}
	//todo: set assignment
	private void getAssignment(RobotController rc) {
		//??
	}
}
