package sprint1;
import battlecode.common.*;
import java.util.Random;
public abstract class RobotLogic {
	public static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };
	public static final Random rng = new Random(6147);
	abstract boolean run(RobotController rc) throws GameActionException;
	
	//returns true if it successfully moves, false if it doesn't
	public boolean follow(RobotController rc, int id) throws GameActionException{
		RobotInfo follow=null;
		if(rc.canSenseRobot(id))
        	follow=rc.senseRobot(id);
        //moves in the miner's direction
        if(follow!=null) {
        	Direction dir=rc.getLocation().directionTo(follow.getLocation());
        	rc.setIndicatorString("attempting to follow "+dir);
        	if(rc.canMove(dir)) {
        		rc.move(dir);
            	return true;
        	}else if(rc.canMove(dir.rotateLeft())) { //if it can't move in the direction, tries to move 45 degrees to the left
        		rc.move(dir.rotateLeft());
            	return true;
        	}else if(rc.canMove(dir.rotateRight())) {//tries to move 45 degrees to the right
        		rc.move(dir.rotateRight());
            	return true;
        	}
        }
        return false;
	}
	/*
	//for now, just put the dummy dumb pathfind
	public void pathFind(RobotController rc, MapLocation loc) throws GameActionException{
		MapLocation me = rc.getLocation();
		Direction dir=me.directionTo(loc);
		if(rc.canMove(dir)) {
    		rc.move(dir);
    	}else if(rc.canMove(dir.rotateLeft())) { //if it can't move in the direction, tries to move 45 degrees to the left
    		rc.move(dir.rotateLeft());
    	}else if(rc.canMove(dir.rotateRight())) {//tries to move 45 degrees to the right
    		rc.move(dir.rotateRight());
    	}
	}
	*/
	///*
	//better pathfinding
	public void pathFind(RobotController rc, MapLocation loc) throws GameActionException{
		MapLocation me = rc.getLocation();
		if(me.x!=loc.x||me.y!=loc.y) {
			Direction dir=me.directionTo(loc);
			
			int chosenDir=decideDirection(getHemisphereTiles(rc,loc),loc,me,rc);
			if(chosenDir==0){
				//straight
				if(rc.canMove(dir)) {
	    			rc.move(dir);
	    		}else if(rc.canMove(dir.rotateLeft())) { //if it can't move in the direction, tries to move 45 degrees to the left
	    			rc.move(dir.rotateLeft());
	    		}else if(rc.canMove(dir.rotateRight())) {//tries to move 45 degrees to the right
	    			rc.move(dir.rotateRight());
	    		}
			}else if(chosenDir==1) {
				//left
				if(rc.canMove(dir.rotateLeft())) {
	    			rc.move(dir.rotateLeft());
	    		}else if(rc.canMove(dir)) { //if it can't move in the direction, tries to move straight
	    			rc.move(dir);
	    		}else if(rc.canMove(dir.rotateRight())) {//tries to move 45 degrees to the right
	    			rc.move(dir.rotateRight());
	    		}
			}else if(chosenDir==2) {
				//right
				if(rc.canMove(dir.rotateRight())) {
	    			rc.move(dir.rotateRight());
	    		}else if(rc.canMove(dir)) { //if it can't move in the direction, tries to move straight
	    			rc.move(dir);
	    		}else if(rc.canMove(dir.rotateLeft())) {//tries to move 45 degrees to the left
	    			rc.move(dir.rotateLeft());
	    		}
			}
		}
	}
	//*/
	//bytecode cost: around 3000
	private TileData[] getHemisphereTiles(RobotController rc, MapLocation assignment) throws GameActionException{
		MapLocation me=rc.getLocation();
		Direction dir=me.directionTo(assignment);
		if(dir!=Direction.CENTER) {
			//hemisphere is 38 tiles (37 if dir is diagonal)
			TileData[] hemisphere=new TileData[39];
			int currentIndex=0;
			Direction left=dir.rotateLeft().rotateLeft();
			Direction right=dir.rotateRight().rotateRight();
			//forwards moves in the direction of dir, while left and right go off to either side to get all the locations
			MapLocation forwards=rc.getLocation();
			int loops=0;
			while(rc.onTheMap(forwards)&&me.distanceSquaredTo(forwards)<10) {
				//add forwards to hemisphere
				//rc.setIndicatorDot(forwards,0,0,255);
				hemisphere[currentIndex]=new TileData(forwards,rc.senseRubble(forwards),rc.isLocationOccupied(forwards));
				currentIndex++;
				//iterate forwards
				//forwards moves in a straight line if not diagonal
				if(dir.getDeltaX()==0||dir.getDeltaY()==0) {
					forwards=forwards.add(dir);
				}
				//forwards doesn't move diagonal if dir is diagonal (it moves in a staircase pattern)
				else {
					if(loops%2==0) {
						forwards=forwards.add(dir.rotateLeft());
					}else {
						forwards=forwards.add(dir.rotateRight());
					}
				}
				loops++;
				//get all the tiles left of forwards
				MapLocation leftOfForwards=forwards.add(left);
				while(rc.onTheMap(leftOfForwards)&&me.distanceSquaredTo(leftOfForwards)<8) {
					//add left to hemisphere
					hemisphere[currentIndex]=new TileData(leftOfForwards,rc.senseRubble(leftOfForwards),rc.isLocationOccupied(leftOfForwards));
					currentIndex++;
					//rc.setIndicatorDot(leftOfForwards,0,255,0);
					
					//iterate left
					leftOfForwards=leftOfForwards.add(left);
				}
				//get all the tiles right of forwards
				MapLocation rightOfForwards=forwards.add(right);
				while(rc.onTheMap(rightOfForwards)&&me.distanceSquaredTo(rightOfForwards)<8) {
					//add right to hemisphere
					hemisphere[currentIndex]=new TileData(rightOfForwards,rc.senseRubble(rightOfForwards),rc.isLocationOccupied(rightOfForwards));;
					currentIndex++;
					//rc.setIndicatorDot(rightOfForwards,255,0,0);
					
					//iterate right
					rightOfForwards=rightOfForwards.add(right);
				}
			}
			return hemisphere;
		}
		return null;
	}
    
	//note: currently unused
	private int getAveragePassibility(TileData[] hemisphere){
    	int total=0;
    	for(int i=0;i<hemisphere.length;++i) {
    		if(hemisphere[i]!=null) {
    			total+=hemisphere[i].getPassibility();
    		}else {
    			//System.out.println("null index: "+i);
    			break;
    		}
    	}
    	total/=hemisphere.length;
    	return total;
    }

    private int decideDirection(TileData[] hemisphere,MapLocation assignment,MapLocation me,RobotController rc) {
    	Direction travelDir=me.directionTo(assignment);

    	//0 is straight, 1 is left, 2 is right
    	int[] passabilityScores= {0,0,0};
    	int[] sizes= {0,0,0};
    	for(int i=0;i<hemisphere.length;++i) {
    		if(hemisphere[i]!=null) {
    			Direction dirToPoint=me.directionTo(hemisphere[i].getLocation());
    			if(dirToPoint==travelDir.rotateLeft()||dirToPoint==travelDir.rotateLeft().rotateLeft()) {
    				//left
    				rc.setIndicatorDot(hemisphere[i].getLocation(),255,0,0);
    				passabilityScores[1]+=hemisphere[i].getPassibility();
    				sizes[1]++;
    				//gives double influence if the space is adjacent to me
    				if(me.isAdjacentTo(hemisphere[i].getLocation())) {
    					passabilityScores[1]+=hemisphere[i].getPassibility();
        				sizes[1]++;
    				}
    			}else if(dirToPoint==travelDir.rotateRight()||dirToPoint==travelDir.rotateRight().rotateRight()) {
    				//right
    				rc.setIndicatorDot(hemisphere[i].getLocation(),0,255,0);
    				passabilityScores[2]+=hemisphere[i].getPassibility();
    				sizes[2]++;
    				//gives double influence if the space is adjacent to me
    				if(me.isAdjacentTo(hemisphere[i].getLocation())) {
    					passabilityScores[2]+=hemisphere[i].getPassibility();
        				sizes[2]++;
    				}
    			}else if(dirToPoint!=Direction.CENTER){
    				//straight
    				rc.setIndicatorDot(hemisphere[i].getLocation(),0,0,255);
    				passabilityScores[0]+=hemisphere[i].getPassibility();
    				sizes[0]++;
    				//gives double influence if the space is adjacent to me
    				if(me.isAdjacentTo(hemisphere[i].getLocation())) {
    					passabilityScores[0]+=hemisphere[i].getPassibility();
        				sizes[0]++;
    				}
    			}
    		}
    	}
    	int lowest=696969;
    	int index=1;
    	for(int i=0;i<sizes.length;++i) {
        	//catch cases where sizes are 0
    		if(sizes[i]==0) {
    			passabilityScores[i]=696969;
    		}else { //make the average score for left, straight and right
    			passabilityScores[i]/=sizes[i];
    	    	//figure out which has the lowest score
    			if(passabilityScores[i]<lowest) {
    				lowest=passabilityScores[i];
    				index=i;
    			}
    		}
    	}
    	//rc.setIndicatorString("straight: "+passabilityScores[0]+", left: "+passabilityScores[1]+", right: "+passabilityScores[2]+", chosen: "+index);
    	//if(index!=1) {
    	//	System.out.println("straight not chosen!");
    	//}
    	return index;
    }
    
    /*
    public MapLocation allAboard(RobotController rc) throws GameActionException{
		//we'll assume we put train destination in spot 10
		int comm=rc.readSharedArray(10);
		if(comm!=0) {
			MapLocation assignment=commToLoc(comm);
			if(rc.onTheMap(assignment)) {
				return assignment;
			}else {
				return null;
			}
		}else {
			return null;
		}
		
	}
    */
    //temporary all aboard while no communications
    //only works for player 1
    public MapLocation allAboard(RobotController rc) throws GameActionException{
    	if(rc.getMapHeight()>50) {
    		//eckleburg
    		return new MapLocation(53,6);
    	}else if(rc.getMapHeight()>30) {
    		//maptestsmall
    		return new MapLocation(26,26);
    	}
    	//intersection
    	return new MapLocation(46,2);
    }
    
    public void chooChoo(RobotController rc, MapLocation assignment) throws GameActionException{
    	MapLocation me = rc.getLocation();
		MapLocation[] gold=rc.senseNearbyLocationsWithGold(20);
		MapLocation[] lead=rc.senseNearbyLocationsWithLead(20);
		MapLocation ore=null;
		if(gold.length!=0) {
			ore=gold[0];
		}else if(lead.length!=0&&lead.length<20){
			//rc.setIndicatorString("lead length: "+lead.length);
			for(int i=0;i<lead.length;++i) {
				if(rc.senseLead(lead[i])>1) {
					ore=lead[i];
					break;
				}
			}
		}
		//move towards the assigned location m
		//unless it sees some ore, then move there instead
		if(ore!=null) {
			pathFind(rc,ore);
		}else if(assignment!=null){
			pathFind(rc,assignment);
		}else {
			randomMovement(rc);
		}
	}
    public void randomMovement(RobotController rc) throws GameActionException{
    	Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }
    public int locToComm(MapLocation loc) {
    	return 60*loc.x + loc.y;
    }
    
    public MapLocation commToLoc(int commVal) {
    	int y = commVal % 60;
    	int x = (commVal-y) / 60;
    	return new MapLocation(x, y);
    }

}