package structured;
import battlecode.common.*;

public class Miner extends RobotLogic {
	Direction dir = directions[rng.nextInt(directions.length)];
	MapLocation m=null;
	@Override
	public boolean run(RobotController rc) throws GameActionException{
		// Try to mine on squares around us.
        mine(rc);

        move(rc);
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
                while (rc.canMineLead(mineLocation)) {
                    rc.mineLead(mineLocation);
                }
            }
        }
	}
	private void move(RobotController rc) throws GameActionException{
		MapLocation me = rc.getLocation();
		//basic: move towards the assigned location m
		if(m!=null) {
			rc.setIndicatorString("assigned location: "+m);
			//if we're there, remove the assignment
			if(m.x==me.x&&m.y==me.y) {
				m=null;
			}else { //else move towards the location
				Direction dir=me.directionTo(m);
	        	//rc.setIndicatorString("attempting to move "+dir);
	        	if(rc.canMove(dir)) {
	        		rc.move(dir);
	        	}else if(rc.canMove(dir.rotateLeft())) { //if it can't move in the direction, tries to move 45 degrees to the left
	        		rc.move(dir.rotateLeft());
	        	}else if(rc.canMove(dir.rotateRight())) {//tries to move 45 degrees to the right
	        		rc.move(dir.rotateRight());
	        	}
			}
		}else {//if no assigned location
			//don't move if happily mining ore
			for (int dx = -1; dx <= 1; dx++) {
	            for (int dy = -1; dy <= 1; dy++) {
	                MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
	                if(rc.canSenseLocation(mineLocation)&&(rc.senseLead(mineLocation)>0||rc.senseGold(mineLocation)>0)){
	                	rc.setIndicatorString("mining :)");
	                	return;
	                }
	            }
			}
			//look for nearby deposits without nearby miners
			RobotInfo[] miners=rc.senseNearbyRobots(20,rc.getTeam());
			for (int dx = -4; dx <= 4; dx++) {
	            for (int dy = -4; dy <= 4; dy++) {
	            	if(dx*dx+dy+dy>2) {
		                MapLocation searchLocation = new MapLocation(me.x + dx, me.y + dy);
		                if(rc.canSenseLocation(searchLocation)&&(rc.senseLead(searchLocation)>0||rc.senseGold(searchLocation)>0)){
		                	//location has ore, check if it has an adjacent miner
		                	boolean pursue=true;
		                	for(int i=0;i<miners.length&&pursue;++i) {
		                		if(miners[i].getType()==RobotType.MINER&&miners[i].getLocation().distanceSquaredTo(searchLocation)<=2) {
		                			//don't pursue
		                			pursue=false;
		                		}
		                	}
		                	if(pursue) {
		                		m=searchLocation;
		                		move(rc);
		                		return;
		                	}
		                	
		                }
	            	}
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
}
