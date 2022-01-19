package structured;
import battlecode.common.*;
import java.util.Random;
public class Miner extends RobotLogic {
	private MapLocation[] zoneCorners= {null};
	private int commNum=-1;
	private int[] zoneLead= {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};
	private int[] zoneMiners={-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};
	private int[] zoneEnemies={-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};
	@Override
	public boolean run(RobotController rc) throws GameActionException{
		//read zone info (lead and miners)
		if(rc.getRoundNum()%2==1) {
			readZoneInfo(rc);
			//only reevaluates assigned zone if the miner has completely scouted its current zone
			//and it is disatisfied with its current zone
			//rc.setIndicatorString("c1: "+(numCornersNotSeen()==0)+", c2: "+(!zoneGood(rc,getZone(rc,rc.getLocation()))));
			if(zoneCorners.length==1||numCornersNotSeen()<=1&&!zoneGood(rc,getZone(rc,rc.getLocation()))) {
				processZoneInfo(rc);
			}
		}
		
		// Try to mine on squares around us.
        mine(rc);
        runAway(rc);
        //move towards assignment or unocupied nearby lead deposits
        if(numCornersNotSeen()>0) {
        	super.chooChooIgnore(rc,zoneCorners[closestCorner(rc)]);
        	//rc.setIndicatorString("zone: "+getZone(rc,rc.getLocation()));
        	lookForCorners(rc);
        }else {
        	//if it doesn't have an assignment, just move to the lowest rubble tile
        	if(zoneCorners.length==1)
        		super.superGreedy(rc);
        	else {
        		//mine
        		MapLocation random=rc.getLocation().add(directions[rng.nextInt(8)]);
        		super.chooChoo(rc,random);
        	}
        }
        
        // Try to mine again (just in case we moved within range of a deposit)
        mine(rc);
        
        
        
        //doesn't need to be in train mode to report enemy archons!
    	commEnemyArchonLoc(rc);
    	if(rc.getRoundNum()%2==0) {
    		reportZone(rc);
    	}
		return true;
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
	
	//sets zoneLead and zoneMiners based on the values in comm
	private void readZoneInfo(RobotController rc) throws GameActionException{
		for(int i=53;i>37;--i) {
			int index=53-i;
			int comm=rc.readSharedArray(i);
			if(comm!=0) {
				//make sure these get the final variables from robotlogic
				int commMinEnemy=(comm%enemyMinMod)/roundMod;
				int commMinLead=(comm%leadMinMod)/enemyMinMod;
	    		int commNumMiners=(comm%allyMinerMod)/leadMinMod;
	    		zoneLead[index]=commMinLead;
	    		zoneMiners[index]=commNumMiners;
	    		zoneEnemies[index]=commMinEnemy;
			}
		}
	}
	
	//evaluates destination based on the zoneInfo
	private void processZoneInfo(RobotController rc) throws GameActionException{
		//starting order favors middle zones
		//int[] indecies= {8,7,9,6,10,5,11,4,12,3,13,2,14,1,15,0};
		//new starting order favors corners
		int[] indecies= {0,15,3,11,8,7,9,6,10,5,4,12,13,2,14,1};
		//shuffles the indecies to make the order it checks the zones in random
		shuffle(rc,indecies);
		
		//for now, choose the first suitable zone it comes across (that is better than its current one)
		for(int i=0;i<15;++i) {
			if(zoneGood(rc,indecies[i])) {
				//assignment=super.getZoneCenter(rc,indecies[i]);
				zoneCorners=super.getZone(rc,indecies[i]);
				rc.setIndicatorString("zone: "+indecies[i]);
				return;
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
	
	private boolean zoneGood(RobotController rc,int zone) throws GameActionException{
		//prioritize exploration and lead
		//deprioritize enemies
		
		//lets say, saturation is 1 miner per 2-3 lead deposit
		
		//don't assign a zone with enemies
		//hopefully not all zones will be occupied with enemies
		if(zoneEnemies[zone]>0) {
			return false;
		}
		//rc.setIndicatorString("c1: "+(zoneMiners[zone]==-1)+" c2: "+(zoneMiners[zone]==0&&zoneLead[zone]>0)+" c3: "+(zoneMiners[zone]>0&&(zoneLead[zone]/zoneMiners[zone])>2));
		//zone is unexplored or     zone is undersaturated
		if(zoneMiners[zone]==-1||(zoneMiners[zone]==0&&zoneLead[zone]>0)||(zoneMiners[zone]>0&&(zoneLead[zone]/zoneMiners[zone])>2)) {
			return true;
		}
		return false;
	}
	
	//checks off from the corners array any corners the miner sees
	private void lookForCorners(RobotController rc) throws GameActionException{
		MapLocation me=rc.getLocation();
		for(int i=0;i<zoneCorners.length;++i) {
			if(zoneCorners[i]!=null&&me.distanceSquaredTo(zoneCorners[i])<21) {
				zoneCorners[i]=null;
			}
		}
	}
	
	//returns the number of corners the miner has yet to see
	private int numCornersNotSeen() {
		int counter=0;
		for(int i=0;i<zoneCorners.length;++i) {
			if(zoneCorners[i]!=null)
				counter++;
		}
		return counter;
	}
	
	//returns the index of the corner that is closest to the miner's current location
	private int closestCorner(RobotController rc) throws GameActionException{
		int closest=696969;
		int index=-1;
		MapLocation me=rc.getLocation();
		for(int i=0;i<zoneCorners.length;++i) {
			if(zoneCorners[i]!=null&&me.distanceSquaredTo(zoneCorners[i])<closest) {
				closest=me.distanceSquaredTo(zoneCorners[i]);
				index=i;
			}
		}
		return index;
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
				processZoneInfo(rc);
				return true;
			}
		}
		return false;
	}
}
