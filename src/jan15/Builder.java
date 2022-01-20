package jan15;
import battlecode.common.*;
public class Builder extends RobotLogic {
    private MapLocation assignment=null;
    
    //if watchTower is false, the builder is a lab builder
    private RobotType buildType=RobotType.WATCHTOWER;
    
	public boolean run(RobotController rc) throws GameActionException{
        //repairs nearby buildings
        repair(rc);
        
        if(assignment==null) {
	        shouldBuildWatchTower(rc);
	        shouldBuildLaboratory(rc);
        }
        
        if(assignment!=null&&assignment.distanceSquaredTo(rc.getLocation())!=0) {
	        build(rc);
        }
        
        shmovement(rc);
        rc.setIndicatorString("assignment: "+assignment);
        return true;
	}
	private void shmovement(RobotController rc) throws GameActionException{
		//need to rework
		
		//fuck around until it finds an assignment
		if(assignment==null) {
			super.randomMovement(rc);
		}else {
			MapLocation me=rc.getLocation();
			int distance=me.distanceSquaredTo(assignment);
			//if it has an assignment, move towards it
			if(distance!=1||distance!=2) {
				if(distance==0) {
					//if its on top of the assignment, move to the lowest rubble tile
					int lowest=696969;
					int index=0;
					for(int i=0;i<directions.length;++i) {
						MapLocation adjacent=me.add(directions[i]);
						if(rc.onTheMap(adjacent)&&rc.senseRobotAtLocation(adjacent)==null) {
							int rubble=rc.senseRubble(adjacent);
							if(rubble<lowest) {
								lowest=rubble;
								index=i;
							}
						}
					}
					if(rc.canMove(directions[index])) {
						rc.move(directions[index]);
					}
				}else if(distance<=5){
					//close to assignment, move greedily
					super.greedy(rc,assignment);
				}else {
					//far from assignment, regular pathfind
					super.pathFind(rc,assignment);
				}
			}
		}
	}
	private void shouldBuildWatchTower(RobotController rc) throws GameActionException{
		//look at a tile and the tiles on the edge of the builder's vision
		//if the tile has low rubble compared to the tiles center-side of it
		//tldr: looks for locations that have the high ground
		MapLocation me = rc.getLocation();
		
		//might want to change the condition instead of just "double"
		//or not, maybe double is fine?
        if(rc.senseRubble(me)*2<getAvgRimRubble(rc)) {
        	assignment=me;
        }
	}
	
	//possible issue: ignores high standard deviation
	//solution: ultra penalize for low rubble
	private int getAvgRimRubble(RobotController rc) throws GameActionException{
		Direction dirToCenter=directionToCenter(rc);
		Direction dirLeft=dirToCenter.rotateLeft().rotateLeft();
		int dx=dirLeft.getDeltaX();
		int dy=dirLeft.getDeltaY();
		if(dx==0||dy==0) {
			//non-diagonal
			dx*=4;
			dy*=4;
		}else {
			//diagonal
			dx*=3;
			dy*=3;
		}
		//build a semicircle clockwise
		MapLocation rim=rc.getLocation().translate(dx,dy);
		if(rc.onTheMap(rim)) {
			int total=0;
			//try dirToCenter.rotateRight(), then dirToCenter.rotateRight().rotateRight(), 
			//then dirToCenter.rotateRight().rotateRight().rotateRight()
			for(int i=0;i<13;++i) {
				//add to total
				int rubble=rc.senseRubble(rim);
				total+=rc.senseRubble(rim);
				if(rubble<25) {//penalty for ultra-low rubble
					total-=(25-rubble);
				}
				//move around the rim
				Direction dirToNextRim=dirToCenter;
				while(!rc.canSenseLocation(rim.add(dirToNextRim))) {
					dirToNextRim=dirToNextRim.rotateRight();
				}
				rim=rim.add(dirToNextRim);
			}
			return total/13;
		}else {//if the first rim isn't on the map, be lazy for now
			return 0;
		}
	}
	
	//assumption: assignment isn't null
	private boolean build(RobotController rc) throws GameActionException{
		MapLocation me = rc.getLocation();
		if(me.distanceSquaredTo(assignment)<6) {
	       	Direction dir=me.directionTo(assignment);
	       	if(dir!=Direction.CENTER&&rc.canBuildRobot(buildType,dir)) {
	       		//note: watchtower placement is so crucial that we cannot place it anywhere but the assignment
		       	rc.setIndicatorString("trying to build a "+buildType+": "+dir);
		       	rc.buildRobot(RobotType.WATCHTOWER,dir);
		       	return true;
	        }
		}
        return false;
	}
	//todo:
	private void shouldBuildLaboratory(RobotController rc) throws GameActionException{
		//??
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