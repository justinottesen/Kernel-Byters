package structured;
import battlecode.common.*;
public class Archon extends RobotLogic {
	private int turnNum = 0;
	private int pbBudget = 0;
	private int locCommIndex = 0;
	
	private void updateCommLoc(RobotController rc) throws GameActionException{
		while (rc.readSharedArray(locCommIndex) == 0) {
			locCommIndex++;
		}
		rc.writeSharedArray(locToComm(locCommIndex, rc.getLocation()));
	}
	
	private void updateBudgetLoc(RobotController rc) throws GameActionException {
		if (locCommIndex == 0) {
			rc.writeSharedArray(4, rc.getTeamLeadAmount(rc.getTeam()));
		}
	}
	
	private void commUnderAttack(RobotController rc) throws GameActionException {
		int prevValue = rc.readSharedArray(5);
		if ((prevValue/Math.pow(10, locCommIndex))%2 == 0) { //NOT UNDER ATTACK LAST TURN
			if (enemySoldiersNearby(rc) == true) {
				rc.writeSharedArray(5, prevValue + Math.pow(10, locCommIndex));
			}
		}
		if ((prevValue/Math.pow(10, locCommIndex))%2 == 1) { //UNDER ATTACK LAST TURN
			if (enemySoldiersNearby(rc) == false) {
				rc.writeSharedArray(5, prevValue - Math.pow(10, locCommIndex));
			}
		}
	}
	
	private boolean enemySoldiersNearby(RobotController rc) throws GameActionException {
		Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(34, opponent);
        for(int i=0;i<enemies.length;++i) {
        	RobotType type=enemies[i].getType();
    		if(type==RobotType.SOLDIER||type==RobotType.SAGE||type==RobotType.WATCHTOWER) {
    			return true;
    		}
        }
        return false;
	}
	
	private void createRobot(RobotController rc, RobotType type) throws GameActionException {
		Direction dir = null;
		if (type == RobotType.MINER || type == RobotType.SOLDIER) { // SOLDIER IS TEMPORARY CODE LATER ITS LIKE 1AM IM TIRED
			dir = rc.getLocation().directionTo(rc.senseNearbyLocationsWithLead(34)[0]);
		}
		if (rc.canBuildRobot(type, dir) == true) {
			rc.buildRobot(type, dir);
		} else if (rc.canBuildRobot(type, dir.rotateLeft()) == true) {
			rc.buildRobot(type, dir.rotateLeft());
		} else if (rc.canBuildRobot(type, dir.rotateRight()) == true) {
			rc.buildRobot(type, dir.rotateRight());
		} else if (rc.canBuildRobot(type, dir.rotateLeft().rotateLeft()) == true) {
			rc.buildRobot(type, dir.rotateLeft().rotateLeft());
		} else if (rc.canBuildRobot(type, dir.rotateRight().rotateRight()) == true) {
			rc.buildRobot(type, dir.rotateRight().rotateRight());
		} else if (rc.canBuildRobot(type, dir.rotateLeft().rotateLeft().rotateLeft()) == true) {
			rc.buildRobot(type, dir.rotateLeft().rotateLeft().rotateLeft());
		} else if (rc.canBuildRobot(type, dir.rotateRight().rotateRight().rotateRight()) == true) {
			rc.buildRobot(type, dir.rotateRight().rotateRight().rotateRight());
		} else if (rc.canBuildRobot(type, dir.rotateRight().rotateRight().rotateRight().rotateRight()) == true) {
			rc.buildRobot(type, dir.rotateRight().rotateRight().rotateRight().rotateRight());
		}
	}
	
	public boolean run(RobotController rc) throws GameActionException{
		turnNum ++;
		if (turnNum == 1) {
			updateCommLoc(rc);
		}
		updateBudgetLoc(rc);
		commUnderAttack(rc);
		
		pbBudget = rc.readSharedArray(4)/rc.getArchonCount();
		if (enemySoldeirsNearby(rc) == true) { //PLACEHOLDER IM TIRED ILL DO IT TOMORROW
			createRobot(rc, RobotType.SOLDIER);
		} else if (turnNum < 50) {
			if (pbBudget >= 50) {
				createRobot(rc, RobotType.MINER);
			}
		}
		//Add in code for later turns (Maybe alternate soldier + miner for train or something
    	return true;
	}
}
