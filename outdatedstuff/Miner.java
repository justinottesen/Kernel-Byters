package structured;
import battlecode.common.*;

public class Miner extends RobotLogic {
	private static Direction dir = directions[rng.nextInt(directions.length)];
	private static MapLocation assignment=null;
	@Override
	public boolean run(RobotController rc) throws GameActionException{
		// Try to mine on squares around us.
        mine(rc);
        if(rc.getRoundNum()<super.TRANSITIONROUND) {
        	//recode basic mining here
        	move(rc);
        }else {
	        assignment=super.allAboard(rc);
	        if(assignment!=null)
	        	chooChoo(rc,assignment);
	        else
	        	move(rc);
        }
		return true;
	}
	private void mine(RobotController rc) throws GameActionException{
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
        	}
        }
	}
	///*
	//now used only for the first stage of the game
	private void move(RobotController rc) throws GameActionException{
		MapLocation me = rc.getLocation();
		//basic: move towards the assigned location m
		//if no assigned location
		//don't move if happily mining ore
		MapLocation[] gold=rc.senseNearbyLocationsWithGold(2);
		//use the new methods!
		MapLocation[] lead=rc.senseNearbyLocationsWithLead(2,2);
		//only sense lead with more than 1 lead
	    if(gold.length>0||lead.length>0){
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
	    //look for nearby deposits without nearby miners
		gold=rc.senseNearbyLocationsWithGold();
		lead=rc.senseNearbyLocationsWithLead(20,2);
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
           		super.pathFind(rc,gold[j]);
           		return;
           	}
		}
		for(int j=0;j<lead.length;++j) {
			//location has lead, check if it has an adjacent miner
            boolean pursue=true;
           	for(int i=0;i<miners.length&&pursue;++i) {
           		if(miners[i].getType()==RobotType.MINER&&miners[i].getLocation().distanceSquaredTo(lead[j])<=4) {
           			//don't pursue
           			pursue=false;
           		}
           	}
           	if(pursue) {
           		super.pathFind(rc,lead[j]);
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
	//*/
}
