package structured;
import battlecode.common.*;
import java.util.Random;
public class Soldier extends RobotLogic {
	private int farFromHome=0;
	private MapLocation assignment=null;
	private int zoneAssignment=-1;
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
				//first set assignment
				if(rc.getRoundNum()%2==1) {//only for "read" rounds
					//reevaluates if it doesn't have an assignment or if its not currently in combat (or expecting combat)
					if(zoneAssignment==-1) {
						rc.setIndicatorString("new soldier");
						//sets zone as if it is dissatisfied with its current assignment
						setZone(rc,false);
					}else if(!zoneBetter(rc,53-zoneAssignment)) {//hopefully this doesn't take up too much bytecode
						setZone(rc,zoneGood(rc,53-zoneAssignment));
						rc.setIndicatorString("looked for better zones");
					}
				}
				if(assignment!=null) {
					super.pathFind(rc,assignment);
				}else {
					//super.randomMovement(rc);
					MapLocation middle=new MapLocation(rc.getMapWidth()/2,rc.getMapHeight()/2);
					super.pathFind(rc,middle);
				}
				
				//okay I'm mad, why didn't we make the bots attack after moving too
				makeLikeTheFireNation(rc);
			}
		}
    	commEnemyArchonLoc(rc);
    	if(rc.getRoundNum()%2==0)
    		reportZone(rc);
        return true;
	}
	//returns the robotinfo if it sees attacking units
	//todo: still return the robotinfo if it sees a target outside of attack range
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
	
	//for now, return true if hp < 1/3 max health
	private boolean retreat(RobotController rc) throws GameActionException{
		return (rc.getHealth()<rc.getType().getMaxHealth(0)/4);
	}
	
	//this might take a lot of bytecode
	//returns 2 if the soldier should advance, 1 if it should stay put, 0 if it should retreat
	private int advance(RobotController rc) throws GameActionException{
		MapLocation me=rc.getLocation();
		if(assignment.distanceSquaredTo(me)==0)
			return 1;
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

	private void setZone(RobotController rc,boolean satisfied) throws GameActionException{
		//starting order favors middle zones
		int[] indecies= {8,7,9,6,10,5,11,4,12,3,13,2,14,1,15,0};
		shuffle(rc,indecies);
		
		//for now, choose the first suitable zone it comes across (that is better than its current one)
		for(int i=0;i<15;++i) {
			//zone better looks for enemies
			if(zoneBetter(rc,53-indecies[i])) {
				assignment=super.getZoneCenter(rc,indecies[i]);
				zoneAssignment=indecies[i];
				//zoneCorners=super.getZone(rc,indecies[i]);
				rc.setIndicatorString("better zone: "+indecies[i]);
				return;
			}
		}
		if(!satisfied) {
			for(int i=0;i<15;++i) {
				//zone better looks for enemies
				if(zoneGood(rc,53-indecies[i])) {
					assignment=super.getZoneCenter(rc,indecies[i]);
					zoneAssignment=indecies[i];
					//zoneCorners=super.getZone(rc,indecies[i]);
					rc.setIndicatorString("good zone: "+indecies[i]);
					return;
				}
			}
		}
	}
	//shuffles an ordered set of indecies to provide a pseudorandom order
	private void shuffle(RobotController rc,int[] indecies) throws GameActionException{
		Random random=new Random(rc.getID()/rc.getRoundNum());
		//can shuffle less for less bytecode
		for(int i=0;i<15;++i) {
			int index1=random.nextInt(16);
			int index2=random.nextInt(16);
			//probably more bytecode efficient to leave out the if statement
			//if(index1!=index2) {
				//swap
				int temp=indecies[index1];
				indecies[index1]=indecies[index2];
				indecies[index2]=temp;
			//}
		}
			
	}
	
	//returns true if zone has enemies in it
	private boolean zoneBetter(RobotController rc, int zone) throws GameActionException{
		int comm=rc.readSharedArray(zone);
		if(comm!=0) {
			//make sure these get the final variables from robotlogic
			int commMinEnemy=(comm%enemyMinMod)/roundMod;
			if(commMinEnemy>0) {
				//rc.setIndicatorString("zone "+zone+" enemies: "+commMinEnemy+" comm: "+comm);
				return true;
			}
			
		}
		//rc.setIndicatorString("false");
		return false;
	}
	
	//returns true if the zone is worth defending? (has lead?)
	private boolean zoneGood(RobotController rc, int zone) throws GameActionException{
		int comm=rc.readSharedArray(zone);
		if(comm!=0) {
			//make sure these get the final variables from robotlogic
			int commMinLead=(comm%leadMinMod)/enemyMinMod;
			int commNumMiners=(comm%allyMinerMod)/leadMinMod;
			if(commMinLead>3||commNumMiners>3)
				return true;
			
		}
		return false;
	}
}