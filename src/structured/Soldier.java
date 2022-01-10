package structured;
import battlecode.common.*;
public class Soldier extends RobotLogic {
	private static MapLocation assignment=null;
	public boolean run(RobotController rc) throws GameActionException{
		RobotInfo target=makeLikeTheFireNation(rc);
		//attack anyone within range
		if(target!=null) {
			//if it sees threat, micro
			microThatBitch(rc,target);
		}else {
			//if it doesn't see a threat, move normally
			assignment=super.allAboard(rc);
			if(rc.getRoundNum()>=super.TRANSITIONROUND&&assignment!=null) {
				chooChoo(rc,assignment);
			}else {
				super.randomMovement(rc);
			}
		}
		if(assignment!=null)
			commNoEnemyArchon(rc,assignment);
		//doesn't need to be in train mode to report enemy archons!
    	commEnemyArchonLoc(rc);
        return true;
	}
	//returns true if it sees attacking units
	private RobotInfo makeLikeTheFireNation(RobotController rc) throws GameActionException{
		RobotInfo waterTribe=null;
		// Try to attack the waterTribe (enemy robot)
		// Target fire weaker opponents
		// Target fire soldiers, then archons, then everything else
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
	                //extra logic for figuring out whether it killed
	                if(rc.canSenseRobotAtLocation(toAttack)) {
	                	//didn't kill
	                	waterTribe=enemies[targetFire];
	                }else {
	                	//did kill archon
	                	if(enemies[targetFire].getType()==RobotType.ARCHON) {
	                		//update the archon list
		                	for (int i = 6; i < 10; i++) {
		                		int commToAttack=rc.readSharedArray(i);
		                		if(commToAttack!=0) {
		                			MapLocation archonLoc=commToLoc(commToAttack);
		                			//within some error
		                			if (toAttack.distanceSquaredTo(archonLoc)<10) {
			                			rc.writeSharedArray(i, 0);
			                			//System.out.println("took out archon at "+archonLoc);
			                			break;
			                		}
		                		}
		                	}
	                	}
	                	//retarget
	                	return makeLikeTheFireNation(rc);
	                }
	            }
        	}
        }
        return waterTribe;
	}
	//flaw: only micros for stuff it can shoot at (ignores the rest of its vision radius)
	private void microThatBitch(RobotController rc,RobotInfo target) throws GameActionException{
		//only micro if it sees attacking threats
		RobotType type=target.getType();
		if(type==RobotType.SOLDIER||type==RobotType.SAGE||type==RobotType.WATCHTOWER) {
			//retreat if self is weak (1/3 health or less)
			if(rc.getHealth()<rc.getType().getMaxHealth(0)/3) {
				//retreat, which means moving in the opposite direction of the enemy
				MapLocation me=rc.getLocation();
				MapLocation targetLoc=target.getLocation();
				int dx=targetLoc.x-me.x;
				int dy=targetLoc.y-me.y;
				MapLocation retreatLoc=new MapLocation(me.x-2*dx,me.y-2*dy);
				super.pathFind(rc,retreatLoc);
			}else if(target.getHealth()<target.getType().getMaxHealth(target.getLevel())/3) {//pursue if enemy is weak (1/3 health or less)
				//pursue, which means moving in the direction of the enemy
				super.pathFind(rc,target.getLocation());
			}
		}else if(type==RobotType.ARCHON) {
			//pursue archons no matter what
			//go for the kill!
			super.pathFind(rc,target.getLocation());
			//also communicate its location
		}
	}
}
