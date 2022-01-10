package structured;
import battlecode.common.*;
import java.util.Random;
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
	        if(assignment!=null) {
	        	chooChoo(rc,assignment);
	        	commNoEnemyArchon(rc,assignment);
	        }else
	        	move(rc);
        }
        //doesn't need to be in train mode to report enemy archons!
    	commEnemyArchonLoc(rc);
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
		//set destination to a random spot on the map
		if(assignment==null||rc.getLocation().distanceSquaredTo(assignment)<5) {
			//tries to go in a unique direction than other miners
			Random random=new Random(rc.getRoundNum());
			assignment=new MapLocation(random.nextInt(rc.getMapWidth()),random.nextInt(rc.getMapHeight()));
		}
		rc.setIndicatorString("random loc: "+assignment);
		chooChoo(rc,assignment);
	}
	//*/
}
