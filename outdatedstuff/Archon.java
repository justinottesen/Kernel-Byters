package structured;
import battlecode.common.*;
public class Archon extends RobotLogic {
	private static int turnNum = 0;
	private static int previous = 0;
	private static int orderValue = 0;
	private static MapLocation[] otherArchons= {null,null,null,null};
	private static final RobotType[] buildOrder={
	    	RobotType.MINER,
	    	RobotType.SOLDIER
	};
	private static int buildNum=0;
	
	//communicates base locations in the turn queue
	//also resolves collisions
	private void communicate(RobotController rc) throws GameActionException {
		int commVal = locToComm(rc.getLocation());
		if (turnNum%4 == orderValue && (rc.readSharedArray(0) == previous)) {
			rc.writeSharedArray(0, commVal);
			//otherArchons[turnNum%4]=rc.getLocation();
		} else if (turnNum%4 == orderValue) {
			orderValue ++;
		}
	}
	//does some quick maffs to find a 180 degree rotation of the base (regardless of symmetry)
	private MapLocation calculateChooChooDestination(RobotController rc,MapLocation archon) throws GameActionException{
		int height=rc.getMapHeight();
		int width=rc.getMapWidth();
		MapLocation middle=new MapLocation(width/2,height/2);
		int dx=middle.x-archon.x;
		int dy=middle.y-archon.y;
		return new MapLocation(archon.x+2*dx,archon.y+2*dy);
		
	}
	//goes through all ally archons to decide on one train destination (the closest to all archons)
	private MapLocation calculateChooChooDestination(RobotController rc) throws GameActionException{
		int index=-1;
		int lowest=69696969;
		MapLocation chosenChooChoo=null;
		for(int i=0;i<4;++i) {
			if(otherArchons[i]!=null) {
				//calculate average distance to that archon's chosen destination
				int distance=0;
				MapLocation possibleDestination=calculateChooChooDestination(rc,otherArchons[i]);
				//System.out.println("archon "+i+"'s location: "+otherArchons[i]);
				//System.out.println("archon "+i+"'s chosen destination: "+possibleDestination);
				System.out.println("pov of archon "+orderValue);
				for(int j=0;j<4;++j) {
					if(otherArchons[j]!=null) {
						System.out.println("archon "+j+" loc: "+otherArchons[j]);
						distance+=possibleDestination.distanceSquaredTo(otherArchons[j]);
					}
				}
				//System.out.println("distance: "+distance);
				if(distance<lowest) {
					index=i;
					lowest=distance;
					chosenChooChoo=possibleDestination;
				}
			}
		}
		return chosenChooChoo;
	}
	public boolean run(RobotController rc) throws GameActionException{
		/*
		if(turnNum<=5) {
			communicate(rc);
		}
		turnNum ++;
		*/
		///*
		Direction dir = directions[1];
    	if (rc.canBuildRobot(buildOrder[buildNum], dir)) {
            rc.buildRobot(buildOrder[buildNum], dir);
            buildNum++;
            if(buildNum>=buildOrder.length)
            	buildNum=0;
        }
        //*/
    	/*
    	previous = rc.readSharedArray(0);
    	rc.setIndicatorString("orderVal: "+orderValue+" first bit: "+previous);
		if(turnNum==4||turnNum==5) {
			rc.setIndicatorString("train destination: "+calculateChooChooDestination(rc));
		}
		*/
    	return true;
	}
}
