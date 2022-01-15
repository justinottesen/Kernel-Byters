package structured;
import battlecode.common.*;
import java.util.Random;
public class Miner extends RobotLogic {
	private static Direction dir = directions[rng.nextInt(directions.length)];
	private static MapLocation assignment=null;
	private int commNum=-1;
	private int role=-1;
	@Override
	public boolean run(RobotController rc) throws GameActionException{
		RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		if (enemies.length > 0) {
			for (int i = 0; i < enemies.length; i++) {
				if (enemies[i].type == RobotType.SOLDIER) {
					rc.writeSharedArray(ENEMY_SOLDIER_SEEN, 1);
					break;
				}
			}
		}
		if(role==-1||(role==0&&rc.getRoundNum()==super.TRANSITIONROUND+1)) {
			determineRole(rc);
		}
		rc.setIndicatorString("role: "+role);
		// Try to mine on squares around us.
        mine(rc);
        if(role==0) {
        	//basic mining
        	move(rc);
        	communicateRubble(rc);
        }else {
        	//train
	        assignment=super.allAboard(rc);
	        if(assignment!=null) {
	        	chooChoo(rc,assignment);
	        	commNoEnemyArchon(rc,assignment);
	        }else
	        	move(rc);
        }
        //doesn't need to be in train mode to report enemy archons!
    	commEnemyArchonLoc(rc);
		/*
		if (rc.readSharedArray(LEAD_INCOME) == 0) {
			if (rc.senseNearbyLocationsWithLead(-1, 2).length < 3 && rc.getRobotCount() > rc.getArchonCount()*5) {
				RobotInfo[] nearby = rc.senseNearbyRobots();
				int visibleMinerCount = 0;
				for (int j = 0; j < 2; j++) {
					for (int i = 0; i < nearby.length; i++) {
						if (j == 0 && nearby[i].team == rc.getTeam().opponent()) {
							break;
						}
						if (j == 0 && nearby[i].type == RobotType.MINER) {
							visibleMinerCount ++;
						}
						if (j == 1 && nearby[i].type == RobotType.ARCHON && visibleMinerCount > 1 && rc.getLocation().distanceSquaredTo(nearby[i].location) < 6) {
							rc.disintegrate();
							return true;
						}
					}
				}
			}
		}*/
		return true;
	}
	private void determineRole(RobotController rc) throws GameActionException{
		if(rc.getRoundNum()<super.TRANSITIONROUND) {
			role=0;
		}else {
			Random random=new Random(rc.getID());
			int i=random.nextInt(10);
			if(i>2) {
				role=0;
			}else {
				role=1;
			}
		}
	}
	private void mine(RobotController rc) throws GameActionException{
		int minedLead = 0;
		MapLocation[] gold=rc.senseNearbyLocationsWithGold(2);
		//use the new methods!
		MapLocation[] lead=rc.senseNearbyLocationsWithLead(2,2);
		//only sense lead with more than 1 lead
		//mine everything
        for(int i=0;i<gold.length;++i) {
        	while(rc.canMineGold(gold[i])) {
        		rc.mineGold(gold[i]);
        	}
        }
        for(int i=0;i<lead.length;++i) {
        	//leave 1 lead remaining
			while(rc.canMineLead(lead[i])&&rc.senseLead(lead[i])>1) {
        		rc.mineLead(lead[i]);
				minedLead ++;
        	}
        }
		rc.writeSharedArray(MINER_LEAD_COUNTER, rc.readSharedArray(MINER_LEAD_COUNTER)+minedLead);
	}
	//now used for exploration mining
	private void move(RobotController rc) throws GameActionException{
		//additionally, run away from enemy units
		if(!runAway(rc)) {
			//set destination to a random spot on the map
			if(assignment==null||rc.getLocation().distanceSquaredTo(assignment)<5) {
				//generates a pseudorandom location on the map
				Random random=new Random(rc.getID()/rc.getRoundNum());
				MapLocation me=rc.getLocation();
				
				//make ddx and ddy such that rng will generate a location on the map
				int ddx=rc.getMapWidth()/2;
				int ddy=rc.getMapHeight()/2;
				if(me.x<ddx) {
					ddx=me.x;
				}
				if(me.y<ddy) {
					ddy=me.y;
				}
				if(rc.getMapWidth()-me.x<ddx) {
					ddx+=ddx-(rc.getMapWidth()-me.x);
				}
				if(rc.getMapHeight()-me.y<ddy) {
					ddy+=ddy-(rc.getMapHeight()-me.y);
				}
				int dx=random.nextInt(rc.getMapWidth())-ddx;
				int dy=random.nextInt(rc.getMapHeight())-ddy;
				assignment=new MapLocation(me.x+dx,me.y+dy);
			}
			int distance=rc.getLocation().distanceSquaredTo(assignment);
			rc.setIndicatorString("random loc: "+assignment+", distance: "+distance);
			
			//choo choo but ignore lead deposits with adjacent miners
			chooChooIgnore(rc,assignment);
		}
	}
	//if theres room in the comms, communicates
	private void communicateRubble(RobotController rc) throws GameActionException{
		if(commNum>=0) {
			MapLocation me=rc.getLocation();
			//communicate the current rubble
			int locNum=locToComm(me);
			int rubbleNum=rc.senseRubble(me)/7;
			int commRubble=locNum+((int)Math.pow(2,12))*rubbleNum;
			//rc.setIndicatorString("comm: "+commRubble+", commNum: "+commNum);
			rc.writeSharedArray(commNum,commRubble);
		}else {
			//if there's an available spot in comms, claim it
			for(int i=63;i>53;--i) {
				if(rc.readSharedArray(i)==0) {
					commNum=i;
					communicateRubble(rc);
					break;
				}
			}
		}
	}
	
	private boolean runAway(RobotController rc) throws GameActionException{
		RobotInfo[] enemies=rc.senseNearbyRobots(20,rc.getTeam().opponent());
		for(int i=0;i<enemies.length;++i) {
			RobotType type=enemies[i].getType();
			if(type==RobotType.SOLDIER||type==RobotType.WATCHTOWER||type==RobotType.SAGE) {
				//if it sees an enemy, runs away
				MapLocation me=rc.getLocation();
				MapLocation enemyLoc=enemies[i].getLocation();
				int dx=enemyLoc.x-me.x;
				int dy=enemyLoc.y-me.y;
				MapLocation retreatLoc=new MapLocation(me.x-2*dx,me.y-2*dy);
				super.pathFind(rc,retreatLoc);
				return true;
			}
		}
		return false;
	}
}
