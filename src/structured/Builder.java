package structured;
import battlecode.common.*;
public class Builder extends RobotLogic {
	
	private static boolean newBorn=true;
    private static int idFollow=-1;
    private static MapLocation assignment=null;
	public boolean run(RobotController rc) throws GameActionException{
		//finds the miner to follow
		if(newBorn) {
			findMiner(rc);
			newBorn=false;
		}

        //repairs nearby buildings
        repair(rc);
        
        //builds laboratory if necessary
        if(assignment!=null) {
        	buildLaboratory(rc);
        }else
        //builds watchtower if necessary (also java is whitespace insensitive so you can do else ifs like this lol)
        if(shouldBuildWatchTower(rc)) {
        	buildWatchTower(rc);
        }
        
        shmovement(rc);
        return true;
	}
	private boolean findMiner(RobotController rc) throws GameActionException{
        //searches every nearby robot for the closest miner
        RobotInfo[] nearby=rc.senseNearbyRobots(20,rc.getTeam());
        int closest=21;
        boolean found=false;
        for(int i=0;i<nearby.length;++i) {
       		if(nearby[i].getType()==RobotType.MINER&&nearby[i].getLocation().distanceSquaredTo(rc.getLocation())<closest) {
       			closest=nearby[i].getLocation().distanceSquaredTo(rc.getLocation());
       			idFollow=nearby[i].getID();
       			found=true;
       		}
       	}
        //System.out.println("idFollow: "+idFollow);
        rc.setIndicatorString("following "+idFollow);
        return found;
	}
	private void shmovement(RobotController rc) throws GameActionException{
		//hopefully, the builder won't get stuck on standing on the place where it should build
		MapLocation me = rc.getLocation();
		if(assignment!=null&&(me.x!=assignment.x&&me.y!=assignment.y)) {//if it has somewhere to build a laboratory (and it isn't already on the assigment tile)
			//move towards assignment
			//also effectively stays at assignment once there
			rc.setIndicatorString("assigned location: "+assignment);
			Direction dir=me.directionTo(assignment);
			if(rc.canMove(dir)) {
        		rc.move(dir);
        	}else if(rc.canMove(dir.rotateLeft())) { //if it can't move in the direction, tries to move 45 degrees to the left
        		rc.move(dir.rotateLeft());
        	}else if(rc.canMove(dir.rotateRight())) {//tries to move 45 degrees to the right
        		rc.move(dir.rotateRight());
        	}
		}else {
			//follow the miner
	        if(!super.follow(rc,idFollow)) {
	        	//if it can't follow miner, it tries to find a new miner
	        	if(findMiner(rc)) {
	        		super.follow(rc,idFollow);
	        	}else {
		        	//if it can't, then it moves randomly
		        	rc.setIndicatorString("miner out of range :(");
		        	Direction dir = directions[rng.nextInt(directions.length)];
		            if (rc.canMove(dir)) {
		                rc.move(dir);
		            }
	        	}
	        }
		}
	}
	private boolean shouldBuildWatchTower(RobotController rc) throws GameActionException{
		boolean build=false;
		MapLocation me = rc.getLocation();
        for (int dx = -2; dx <= 2&&!build; dx++) {
            for (int dy = -2; dy <= 2&&!build; dy++) {
                MapLocation searchLocation = new MapLocation(me.x + dx, me.y + dy);
                if(rc.canSenseLocation(searchLocation))
                	build=(rc.senseLead(searchLocation)>0);
            }
        }
        return build;
	}
	private boolean buildWatchTower(RobotController rc) throws GameActionException{
       	//scans every available location
       	//prioritizes building in locations with low lead
		MapLocation me = rc.getLocation();
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
	       	rc.setIndicatorString("trying to build watchtower "+dir);
	       	rc.buildRobot(RobotType.WATCHTOWER,dir);
	       	return true;
        }
        return false;
	}
	//todo: sets assignment
	private boolean shouldBuildLaboratory(RobotController rc) throws GameActionException{
		//??
		return false;
	}
	private boolean buildLaboratory(RobotController rc) throws GameActionException{
		MapLocation me = rc.getLocation();
		if(assignment.isAdjacentTo(me)) {
			//build
			Direction dir=me.directionTo(assignment);
			if(rc.canBuildRobot(RobotType.LABORATORY,dir)) {
				rc.setIndicatorString("trying to build laboratory "+dir);
		       	rc.buildRobot(RobotType.LABORATORY,dir);
		       	return true;
			}else {
				//maybe report back that something is in the way
				//or maybe improvise and try placing the laboratory somewhere else and notifying the archon
				return false;
			}
		}else {
			return false;
		}
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
