package sprint1;
import battlecode.common.*;
public class Soldier extends RobotLogic {
	private static MapLocation assignment=null;
	public boolean run(RobotController rc) throws GameActionException{
		//attack anyone within range
		makeLikeTheFireNation(rc);
		assignment=super.allAboard(rc);
        chooChoo(rc,assignment);
		//move
        //shmovement(rc);
        return true;
	}
	private void makeLikeTheFireNation(RobotController rc) throws GameActionException{
		// Try to attack someone
		// Target fire weaker opponents
		// Target fire soldiers, then sages (unlikely), then archons, then everything else
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(13, opponent);
        if (enemies.length > 0) {
        	//separate enemies into 3 categories: attack units, archons, and other units
        	//stores the indecies+1 since 0 is used as empty
        	int attackUnit=-1;
        	int lowestAttack=696969;
        	int archon=-1;
        	int other=-1;
        	int lowestOther=696969;
        	for(int i=0;i<enemies.length;++i) {
        		RobotType type=enemies[i].getType();
        		int hp=enemies[i].getHealth();
        		if(type==RobotType.SOLDIER||type==RobotType.SAGE||type==RobotType.WATCHTOWER) {
        			if(hp<lowestAttack) {
        				lowestAttack=hp;
        				attackUnit=i;
        			}
        		}else if(type==RobotType.ARCHON) {
        			archon=i;
        		}else {
        			if(hp<lowestOther) {
        				lowestOther=hp;
        				other=i;
        			}
        		}
        	}
        	int targetFire=-1;
        	//now looks for the lowest hp unit to target fire
        	if(attackUnit!=-1) {
        		//it found an attack unit
        		targetFire=attackUnit;
        	}else if(archon!=-1) {
        		//it found an archon
        		targetFire=archon;
        	}else if(other!=-1) {//shouldn't really need the condition here
        		//it found other unit
        		targetFire=other;
        	}
        	rc.setIndicatorString("attack: "+attackUnit+", archon: "+archon+", other: "+other+", target: "+targetFire);
        	if(targetFire>=0) {
	            MapLocation toAttack = enemies[targetFire].location;
	            if (rc.canAttack(toAttack)) {
	                rc.attack(toAttack);
	            }
        	}
        }
	}
	private void shmovement(RobotController rc) throws GameActionException{
		if(assignment!=null) {//if has assignment
			//move towards assignment
			//also effectively stays at assignment once there
			rc.setIndicatorString("assigned location: "+assignment);
			super.pathFind(rc,assignment);
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
