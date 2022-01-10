package structured;
import battlecode.common.*;

public class Miner extends RobotLogic {
	int turnNum = 0;
	private static Direction dir = directions[rng.nextInt(directions.length)];
	private static MapLocation assignment=null;
	@Override
	public boolean run(RobotController rc) throws GameActionException{
		turnNum ++;
		// Try to mine on squares around us.
        mine(rc);
        assignment=super.allAboard(rc);
        chooChoo(rc,assignment);
        //move(rc);
		return true;
	}
	private void mine(RobotController rc) throws GameActionException{
		MapLocation me = rc.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                // Notice that the Miner's action cooldown is very low.
                // You can mine multiple times per turn!
                while (rc.canMineGold(mineLocation)) {
                    rc.mineGold(mineLocation);
                }
                while (rc.canMineLead(mineLocation)&&rc.senseLead(mineLocation)>1) {
                    rc.mineLead(mineLocation);
                }
            }
        }
	}
	/*
	private void move(RobotController rc) throws GameActionException{
		MapLocation me = rc.getLocation();
		//basic: move towards the assigned location m
		if(m!=null) {
			rc.setIndicatorString("assigned location: "+m);
			//if we're there, remove the assignment
			if(m.x==me.x&&m.y==me.y) {
				m=null;
			}else { //else move towards the location
				super.pathFind(rc,m);
			}
		}else {//if no assigned location
			//don't move if happily mining ore
			for (int dx = -1; dx <= 1; dx++) {
	            for (int dy = -1; dy <= 1; dy++) {
	                MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
	                if(rc.canSenseLocation(mineLocation)&&(rc.senseLead(mineLocation)>0||rc.senseGold(mineLocation)>0)){
	                	rc.setIndicatorString("mining :)");
	                	//make sure miner isn't blocking archon
	                	boolean nextToArchon=false;
	                	RobotInfo[] archon=rc.senseNearbyRobots(5);
	                	for(int i=0;i<archon.length&&!nextToArchon;++i) {
	                		if(archon[i].getType()==RobotType.ARCHON&&archon[i].getMode()==RobotMode.TURRET) {
	                			nextToArchon=true;
	                		}
	                	}
	                	if(!nextToArchon)
	                		return;
	                }
	            }
			}
			//look for nearby deposits without nearby miners
			MapLocation[] gold=rc.senseNearbyLocationsWithGold(20);
			MapLocation[] lead=rc.senseNearbyLocationsWithLead(20);
			RobotInfo[] miners=rc.senseNearbyRobots(20,rc.getTeam());
			for(int j=0;j<gold.length;++j) {
				//location has gold, check if it has an adjacent miner
            	boolean pursue=true;
            	for(int i=0;i<miners.length&&pursue;++i) {
            		if(miners[i].getType()==RobotType.MINER&&miners[i].getLocation().distanceSquaredTo(gold[j])<=2) {
            			//don't pursue
            			pursue=false;
            		}
            	}
            	if(pursue) {
            		m=gold[j];
            		move(rc);
            		return;
            	}
			}
			for(int j=0;j<lead.length;++j) {
				//location has lead, check if it has an adjacent miner
            	boolean pursue=true;
            	for(int i=0;i<miners.length&&pursue;++i) {
            		if(miners[i].getType()==RobotType.MINER&&miners[i].getLocation().distanceSquaredTo(lead[j])<=2) {
            			//don't pursue
            			pursue=false;
            		}
            	}
            	if(pursue) {
            		m=lead[j];
            		move(rc);
            		return;
            	}
			}
			//if moving towards wall or enemy, reevaluate direction
			if(me.x-4<0&&dir.getDeltaX()<0) {//if can see wall in -x and moving towards it
				dir = directions[rng.nextInt(directions.length)];
			}
			if(me.y-4<0&&dir.getDeltaY()<0) {//if can see wall in -y and moving towards it
				dir = directions[rng.nextInt(directions.length)];
			}
			if(me.y+4>rc.getMapHeight()&&dir.getDeltaY()>0) {//if can see wall in +x and moving towards it
				dir = directions[rng.nextInt(directions.length)];
			}	
			if(me.x+4>rc.getMapWidth()&&dir.getDeltaX()>0) {//if can see wall in +y and moving towards it
				dir = directions[rng.nextInt(directions.length)];
			}
			RobotInfo[] opponents=rc.senseNearbyRobots(20,rc.getTeam().opponent());
			if(opponents.length>0) {//move away from enemy
				dir=opponents[0].getLocation().directionTo(me);
			}
			//if no unavailable deposits are found and there is no set destination, move in the random direction
			rc.setIndicatorString("moving randomly");
			if(rc.canMove(dir)) {
        		rc.move(dir);
        	}else if(rc.canMove(dir.rotateLeft())) { //if it can't move in the direction, tries to move 45 degrees to the left
        		rc.move(dir.rotateLeft());
        	}else if(rc.canMove(dir.rotateRight())) {//tries to move 45 degrees to the right
        		rc.move(dir.rotateRight());
        	}
		}
	}
	*/
}
