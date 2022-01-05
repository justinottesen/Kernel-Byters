package structured;
import battlecode.common.*;
public class Builder extends RobotLogic {
	
	static boolean newBorn=true;
    static int idFollow=-1;
	public boolean run(RobotController rc) throws GameActionException{
		//finds the miner to follow
        findMiner(rc);

        //repairs nearby buildings
        repair(rc);
        
        //builds watchtower if necessary
        buildWatchTower(rc);
        
        //follow the miner
        if(!super.follow(rc,idFollow)) {
        	//if it can't follow miner, it moves randomly
        	//rc.setIndicatorString("miner out of range :(");
        	Direction dir = directions[rng.nextInt(directions.length)];
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
        }
        return true;
	}
	private void findMiner(RobotController rc) throws GameActionException{
		if(newBorn) {
        	//searches every nearby robot for the closest miner
        	RobotInfo[] nearby=rc.senseNearbyRobots(20,rc.getTeam());
        	int closest=21;
        	for(int i=0;i<nearby.length;++i) {
        		if(nearby[i].getType()==RobotType.MINER&&nearby[i].getLocation().distanceSquaredTo(rc.getLocation())<closest) {
        			closest=nearby[i].getLocation().distanceSquaredTo(rc.getLocation());
        			idFollow=nearby[i].getID();
        		}
        	}
        	//System.out.println("idFollow: "+idFollow);
        	rc.setIndicatorString("following "+idFollow);
        	newBorn=false;
        }
	}
	private boolean buildWatchTower(RobotController rc) throws GameActionException{
		//builds watchtowers around lead deposits
		boolean build=false;
		MapLocation me = rc.getLocation();
        for (int dx = -2; dx <= 2&&!build; dx++) {
            for (int dy = -2; dy <= 2&&!build; dy++) {
                MapLocation searchLocation = new MapLocation(me.x + dx, me.y + dy);
                if(rc.canSenseLocation(searchLocation))
                	build=(rc.senseLead(searchLocation)>0);
            }
        }
        //if it deems appropriate to build...
        if(build) {
        	//scans every available location
        	//prioritizes building in locations with low lead
        	int lead=6969;
        	Direction dir=null;
        	for(int i=0;i<8;++i) {
        		MapLocation buildLocation=me.add(directions[i]);
        		if(rc.canBuildRobot(RobotType.WATCHTOWER,directions[i])&&rc.senseLead(buildLocation)<lead) {
        			//make sure there are no buildings adjacent to buildLocation
        			boolean adjacent=false;
        			RobotInfo[] adjacentRobots=rc.senseNearbyRobots(buildLocation,2,rc.getTeam());
        			for(int j=0;j<adjacentRobots.length&&!adjacent;++j) {
        				if(adjacentRobots[j].getType().isBuilding())
        					adjacent=true;
        			}
        			if(!adjacent) {
	        			lead=rc.senseLead(buildLocation);
	        			dir=directions[i];
        			}
        		}
        	}
        	if(dir!=null) {
	        	rc.setIndicatorString("trying to build "+dir);
	        	rc.buildRobot(RobotType.WATCHTOWER,dir);
	        	return true;
        	}
        }
        return false;
	}
	private boolean repair(RobotController rc) throws GameActionException{
		boolean repaired=false;
		//searches every tile in range for a building and repairs it
		RobotInfo[] buildings=rc.senseNearbyRobots(5,rc.getTeam());
		for(int i=0;i<buildings.length;++i) {
			//only repairs buildings that aren't at max health
			if(buildings[i].getType().isBuilding()&&buildings[i].getHealth()<buildings[i].getType().getMaxHealth(1)) {
				if(rc.canRepair(buildings[i].getLocation())) {
					rc.repair(buildings[i].getLocation());
					repaired=true;
					rc.setIndicatorString("repairing");
				}
			}
		}
		return repaired;
	}
}
