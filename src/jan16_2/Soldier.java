package jan16_2;
import battlecode.common.*;
public class Soldier extends RobotLogic {
	private static int farFromHome=0;
	private static MapLocation assignment=null;
	public boolean run(RobotController rc) throws GameActionException{
		RobotInfo target=makeLikeTheFireNation(rc);
		distanceToNearestArchon(rc);
		
		//doesn't move if getting healed (for now)
		if(!gettingHealed(rc)) {
			//attack anyone within attacking range
			if(target!=null) {
				//engage
				//if it sees threat, micro
				microThatBitch(rc,target);
			}else { 
				assignment=super.allAboard(rc);
				//mayber remove first condition?
				if(rc.getRoundNum()>=super.TRANSITIONROUND&&assignment!=null) {
					//todo: look for enemies outside of attack radius and decide whether to engage
					//also make sure its not about to run into a bunch of rubble
					int decision=advance(rc);
					//0 = retreat, 1 = stay put, 2 = advance
					if(decision==0){
						//retreating
						assignment=getNearestArchon(rc);
						super.pathFind(rc,assignment);
					}else if(decision==1){
						//stay put and defend/wait for reinforcements
						super.superGreedy(rc);
						//move to the lowest-rubble adjacent tile just in case
					}else {
						//if it doesn't see a threat, move normally
						super.pathFind(rc,assignment);
					}
				}else {
					super.randomMovement(rc);
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
        	//rc.setIndicatorString("attack: "+attackUnit+", archon: "+archon+", other: "+other+", target: "+targetFire);
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
		MapLocation me=rc.getLocation();
		MapLocation targetLoc=target.getLocation();
		if(type==RobotType.SOLDIER||type==RobotType.SAGE||type==RobotType.WATCHTOWER) {
			//retreat if self is weak (1/3 health or less)
			if(rc.getHealth()<rc.getType().getMaxHealth(0)/3) {
				//retreat, which means moving in the opposite direction of the enemy
				int dx=targetLoc.x-me.x;
				int dy=targetLoc.y-me.y;
				MapLocation retreatLoc=new MapLocation(me.x-2*dx,me.y-2*dy);
				//note, doesn't use regular pathfinding, prioritizes short term greed
				super.greedy(rc,retreatLoc);
			}else if(target.getHealth()<target.getType().getMaxHealth(target.getLevel())/3) {//pursue if enemy is weak (1/3 health or less)
				//pursue, which means moving in the direction of the enemy
				if(me.distanceSquaredTo(targetLoc)<16) {
					super.superGreedy(rc);
				}else {
					super.greedy(rc,target.getLocation());
				}
			}
		}else if(type==RobotType.ARCHON) {
			//pursue archons no matter what
			//go for the kill!
			if(me.distanceSquaredTo(targetLoc)<16) {
				super.superGreedy(rc);
			}else {
				super.greedy(rc,target.getLocation());
			}
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
	
	//for now, return true if hp < 1/3 max health
	private boolean retreat(RobotController rc) throws GameActionException{
		return (rc.getHealth()<rc.getType().getMaxHealth(0)/4);
	}
	
	//this might take a lot of bytecode
	//returns 2 if the soldier should advance, 1 if it should stay put, 0 if it should retreat
	private int advance(RobotController rc) throws GameActionException{
		MapLocation me=rc.getLocation();
		//take into account unit health, terrain in front, number of allies, and number of enemies
		int health=rc.getHealth();
		RobotInfo[] allies=rc.senseNearbyRobots(20,rc.getTeam());
		RobotInfo[] enemies=rc.senseNearbyRobots(20,rc.getTeam().opponent());
		
		int selfRubble=rc.senseRubble(me);
		
		TileData[] hemisphere=super.getHemisphereTiles(rc,assignment);
		int rubble=super.getAveragePassibility(hemisphere);
		
		int allyCount=1; //including self
		int allyHp=health;//also including self
		int allyRubble=selfRubble;//also including self
		for(int i=0;i<allies.length;++i) {
			RobotType type=allies[i].getType();
			//include watchtowers?
			if(type==RobotType.SOLDIER||type==RobotType.SAGE) {
				allyCount++;
				allyHp+=allies[i].getHealth();
				allyRubble+=rc.senseRubble(allies[i].getLocation());
			}
		}
		
		int enemyCount=0;
		int enemyHp=0;
		int enemyRubble=0;
		for(int i=0;i<enemies.length;++i) {
			RobotType type=enemies[i].getType();
			//include watchtowers?
			if(type==RobotType.SOLDIER||type==RobotType.SAGE) {
				enemyCount++;
				enemyHp+=enemies[i].getHealth();
				enemyRubble+=rc.senseRubble(enemies[i].getLocation());
			}
		}
		
		rc.setIndicatorString("rubble "+rubble+", ally "+(allyCount*20)/((allyRubble/10)+1));
		//advance if rubble is low and allies are more than enemies
		
		//if enemy looks overwhelming or self-hp is low, retreat
		if(allyHp<allyCount*RobotType.SOLDIER.getMaxHealth(0)/4&&health<RobotType.SOLDIER.getMaxHealth(0)/4) {
			return 0;
		}
		
		//don't worry about hp for now (+1 is just so we don't have divide by 0 errors)
		//okay, for some reason, full 100 rubble ahead is only 25 in the rubble variable. Who knows
		if(rubble<10||((allyCount*20)/((allyRubble/10)+1)>=rubble)){
			return 2;
		}
		
		
		//else, stick around
		return 1;
	}
}