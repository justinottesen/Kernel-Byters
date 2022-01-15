package structured;
import battlecode.common.*;
public class Soldier extends RobotLogic {
	private static int farFromHome=0;
	private static MapLocation assignment=null;
	public boolean run(RobotController rc) throws GameActionException{
		RobotInfo target=makeLikeTheFireNation(rc);
		distanceToNearestArchon(rc);
		
		//doesn't move if getting healed (for now)
		if(!gettingHealed(rc)) {
			//attack anyone within range
			if(target!=null) {
				//if it sees threat, micro
				microThatBitch(rc,target);
			}else { 
				if(retreat(rc)){
					//retreating
					assignment=getNearestArchon(rc);
					super.pathFind(rc,assignment);
				
					//todo: search for high health soldiers to support and follow
				}else {
					//if it doesn't see a threat, move normally
					assignment=super.allAboard(rc);
					if(rc.getRoundNum()>=super.TRANSITIONROUND&&assignment!=null) {
						super.pathFind(rc,assignment);
					}else {
						super.randomMovement(rc);
					}
				}
				
				//okay I'm mad, why didn't we make the bots attack after moving too
				makeLikeTheFireNation(rc);
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
		                		//commToAttack is the id of the archon from comms
		                		final int twoToTheTwelth=(int)Math.pow(2,12);
		                		int commToAttack=rc.readSharedArray(i)/twoToTheTwelth;
		                		if(rc.readSharedArray(i)!=0) {
		                			if (enemies[targetFire].getID()==commToAttack) {
			                			rc.writeSharedArray(i, 0);
			                			//System.out.println("took out archon "+commToAttack);
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
	//another flaw: doesn't explicitly support fellow soldiers
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
				//note, doesn't use regular pathfinding, prioritizes short term greed
				super.greedy(rc,retreatLoc);
			}else if(target.getHealth()<target.getType().getMaxHealth(target.getLevel())/3) {//pursue if enemy is weak (1/3 health or less)
				//pursue, which means moving in the direction of the enemy
				super.greedy(rc,target.getLocation());
			}
		}else if(type==RobotType.ARCHON) {
			//pursue archons no matter what
			//go for the kill!
			super.greedy(rc,target.getLocation());
		}
	}
	
	//sets farFromHome to the distance squared to the nearest archon
	//note: doesn't really work when an archon has been killed
	private void distanceToNearestArchon(RobotController rc) throws GameActionException{
		MapLocation me = rc.getLocation();
		farFromHome=696969;
		int index=-1;
		for(int i=0;i<rc.getArchonCount();++i) {
			MapLocation archon=commToLoc(rc.readSharedArray(i));
			int distance=me.distanceSquaredTo(archon);
			if(distance<farFromHome) {
				farFromHome=distance;
				index=i;
			}
		}
	}
	
	//returns the maplocation of the nearest archon
	//note: doesn't really work when an archon has been killed
	private MapLocation getNearestArchon(RobotController rc) throws GameActionException{
		MapLocation me = rc.getLocation();
		for(int i=0;i<rc.getArchonCount();++i) {
			MapLocation archon=commToLoc(rc.readSharedArray(i));
			int distance=me.distanceSquaredTo(archon);
			if(distance==farFromHome) {
				return archon;
			}
		}
		return null;
	}
	
	
	//for now, return true if hp < 1/3 max health
	private boolean retreat(RobotController rc) throws GameActionException{
		return (rc.getHealth()<rc.getType().getMaxHealth(0)/3);
	}
	
	//for now, return true if within healing range of archon and hp < max health
	private boolean gettingHealed(RobotController rc) throws GameActionException{
		return(farFromHome<21&&rc.getHealth()<rc.getType().getMaxHealth(0));
	}
	
	//follows nearby soldier with high health
	private boolean supportSoldier(RobotController rc) throws GameActionException{
		//function only call if low health, 
		//look for a higher health unit to follow
		RobotInfo[] allies=rc.senseNearbyRobots(20,rc.getTeam());
		for(int i=0;i<allies.length;++i) {
			if((allies[i].getType()==RobotType.SOLDIER)&&(allies[i].getHealth()>RobotType.SOLDIER.getMaxHealth(0)/2)) {
				super.pathFind(rc,allies[i].getLocation());
				return true;
			}
		}
		return false;
	}
}
