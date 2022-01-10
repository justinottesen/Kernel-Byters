package structured;
import battlecode.common.*;
public class Archon extends RobotLogic {
	private int turnNum = 0;
	private int pbBudget = 0;
	private int locCommIndex = 0;
	
	private void updateCommLoc(RobotController rc) throws GameActionException{
		while (rc.readSharedArray(locCommIndex) != 0) {
			locCommIndex++;
		}
		//Andrew change: fixed syntax here
		rc.writeSharedArray(locCommIndex,locToComm(rc.getLocation()));
	}
	
	private void updateBudgetLoc(RobotController rc) throws GameActionException {
		if (locCommIndex == 0) {
			//Andrew change: what if our lead amount is more than the max flag?
			int leadAmount=rc.getTeamLeadAmount(rc.getTeam());
			if(leadAmount<=65535)
				rc.writeSharedArray(4, rc.getTeamLeadAmount(rc.getTeam()));
			else
				rc.writeSharedArray(4,65535);
		}
	}
	
	private void commUnderAttack(RobotController rc) throws GameActionException {
		int prevValue = rc.readSharedArray(5);
		if ((prevValue/Math.pow(10, locCommIndex))%2 == 0) { //NOT UNDER ATTACK LAST TURN
			if (enemySoldiersNearby(rc) == true) {
				rc.writeSharedArray(5, prevValue + (int)Math.pow(10, locCommIndex));
			}
		}
		if ((prevValue/Math.pow(10, locCommIndex))%2 == 1) { //UNDER ATTACK LAST TURN
			if (enemySoldiersNearby(rc) == false) {
				rc.writeSharedArray(5, prevValue - (int)Math.pow(10, locCommIndex));
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
	//Andrew change: added back calculating the train destination
	//does some quick maffs to find a 180 degree rotation of the base (regardless of symmetry)
	private MapLocation calculateChooChooDestination(RobotController rc,MapLocation archon) throws GameActionException{
		int height=rc.getMapHeight();
		int width=rc.getMapWidth();
		MapLocation middle=new MapLocation(width/2,height/2);
		int dx=middle.x-archon.x;
		int dy=middle.y-archon.y;
		return new MapLocation(archon.x+2*dx,archon.y+2*dy);
		
	}
	//goes through all ally archons to decide on one train destination (the closest to all archons)
	private MapLocation calculateChooChooDestination(RobotController rc) throws GameActionException{
		MapLocation[] otherArchons= {null,null,null,null};
		for(int i=0;i<4;++i) {
			if(rc.readSharedArray(i)!=0) {
				otherArchons[i]=commToLoc(rc.readSharedArray(i));
			}
		}
		int index=-1;
		int lowest=69696969;
		MapLocation chosenChooChoo=null;
		for(int i=0;i<4;++i) {
			if(otherArchons[i]!=null) {
				//calculate average distance to that archon's chosen destination
				int distance=0;
				MapLocation possibleDestination=calculateChooChooDestination(rc,otherArchons[i]);
				//System.out.println("archon "+i+"'s location: "+otherArchons[i]);
				//System.out.println("archon "+i+"'s chosen destination: "+possibleDestination);
				//System.out.println("pov of archon "+orderValue);
				for(int j=0;j<4;++j) {
					if(otherArchons[j]!=null) {
						//System.out.println("archon "+j+" loc: "+otherArchons[j]);
						distance+=possibleDestination.distanceSquaredTo(otherArchons[j]);
					}
				}
				//System.out.println("distance: "+distance);
				if(distance<lowest) {
					index=i;
					lowest=distance;
					chosenChooChoo=possibleDestination;
				}
			}
		}
		return chosenChooChoo;
	}
	public boolean run(RobotController rc) throws GameActionException{
		turnNum ++;
		if (turnNum == 1) {
			updateCommLoc(rc);
		}
		updateBudgetLoc(rc);
		commUnderAttack(rc);
		
		pbBudget = rc.readSharedArray(4)/rc.getArchonCount();
		//Andrew change: fixed a misspelling of soldier
		if (enemySoldiersNearby(rc) == true) { //PLACEHOLDER IM TIRED ILL DO IT TOMORROW
			createRobot(rc, RobotType.SOLDIER);
		} else if (turnNum < 50) {
			if (pbBudget >= 50) {
				createRobot(rc, RobotType.MINER);
			}
		} else {
			if (turnNum % 2 == 1 && pbBudget >= 75) {
				createRobot(rc, RobotType.SOLDIER);
			} else {
				createRobot(rc, RobotType.MINER);
			}
		}
		//Add in code for later turns (Maybe alternate soldier + miner for train or something
		rc.setIndicatorString("train destination"+calculateChooChooDestination(rc));
    	return true;
	}
}
