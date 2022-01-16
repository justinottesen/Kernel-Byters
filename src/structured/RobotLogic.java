package structured;
import battlecode.common.*;
import java.util.Random;
public abstract class RobotLogic {
	//COMMUNICATION ARRAY VALUES
	public static final int ARCHON_1_LOC = 0;
	public static final int ARCHON_2_LOC = 1;
	public static final int ARCHON_3_LOC = 2;
	public static final int ARCHON_4_LOC = 3;
	public static final int LEAD_BUDGET = 4;
	public static final int UNDER_ATTACK = 5;
	public static final int ENEMY_ARCHON_1_LOC = 6;
	public static final int ENEMY_ARCHON_2_LOC = 7;
	public static final int ENEMY_ARCHON_3_LOC = 8;
	public static final int ENEMY_ARCHON_4_LOC = 9;
	public static final int TRAIN_DESTINATION = 10;
	public static final int TRAIN_CORRECTION = 11;
	public static final int ARCHON_COOLDOWN = 12;
	public static final int LEAD_INCOME = 13;
	public static final int MINER_LEAD_COUNTER = 14;
	public static final int ENEMY_SOLDIER_SEEN = 15;
	public static final int PRIMARY_ARCHON = 16;
	public static final int WATCHTOWERS = 17;
	
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
	public static Random rng = new Random(6147);
	public static final int TRANSITIONROUND = 50;
	abstract boolean run(RobotController rc) throws GameActionException;

	public MapLocation getMapCenter(RobotController rc) throws GameActionException {
		return new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
	}
	
	public Direction directionToCenter(RobotController rc) throws GameActionException {
		return rc.getLocation().directionTo(getMapCenter(rc));
	}

	public Direction rotateClockwise(Direction dir, int numTimes) {
		for (int i = 0; i < numTimes; i++) {
			dir = dir.rotateRight();
		}
		return dir;
	}

	public Direction closestAvailableDir(RobotController rc, Direction goalDir) throws GameActionException {
		if (rc.canSenseRobotAtLocation(rc.adjacentLocation(goalDir)) == false) {
			return goalDir;
		}
		for (int i = 1; i < 5; i++) {
			for (int j = 0; j < 2; j++) {
				if (j == 1) {
					i = 8 - i;
				}
				if (rc.canSenseRobotAtLocation(rc.adjacentLocation(rotateClockwise(goalDir, i))) == false) {
					return rotateClockwise(goalDir, i);
				}
			}
		}
		return goalDir; //returns the goal direction if no directions are available, may change later
	}
	

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
	
	public void commEnemyArchonLoc(RobotController rc) throws GameActionException {
		final int twoToTheTwelth=(int)Math.pow(2,12);
		int enemyLocCommVal = 0;
		int enemyArchonId=0;
		RobotInfo[] enemyList = rc.senseNearbyRobots(20, rc.getTeam().opponent());
		for (int i = 0; i < enemyList.length; i++) {
			if (enemyList[i].getType() == RobotType.ARCHON) {
				enemyLocCommVal = locToComm(enemyList[i].getLocation());
				enemyArchonId=enemyList[i].getID();
				break;
			}
		}
		int index=0;
		for (int j = 6; j < 10; j++) {
			//looks to see if the id isn't unique
			if (enemyArchonId == rc.readSharedArray(j)/twoToTheTwelth) {
				index=j;
				break;
			}
		}
		
		//if id is unique, look for first available slot
		if(index==0) {
			for (int j = 6; j < 10; j++) {
				//looks to see if the id isn't unique
				if (rc.readSharedArray(j) == 0) {
					index=j;
					break;
				}
			}
		}
		//uses modulo to embed the id
		rc.writeSharedArray(index, enemyLocCommVal+enemyArchonId*twoToTheTwelth);
	}
	
	public void commNoEnemyArchon(RobotController rc, MapLocation assignment) throws GameActionException {
		if (rc.getLocation().distanceSquaredTo(assignment) > 12) {
			return;
		}
		RobotInfo[] enemies = rc.senseNearbyRobots(assignment, 6, rc.getTeam().opponent());
		for (int i = 0; i < enemies.length; i++) {
			if (enemies[i].getType() == RobotType.ARCHON) {
				return;
			}
		}
		rc.writeSharedArray(11, 1);
	}
	//greedy pathfinding, ideal for soldier combat
	public void greedy(RobotController rc, MapLocation loc) throws GameActionException{
		MapLocation me = rc.getLocation();
		Direction dir=me.directionTo(loc);
		//get the rubble of the 3 possible movement directions + current location
		int[] rubble= {6969,6969,6969,6969};
		if(rc.onTheMap(me.add(dir)))
			rubble[0]=rc.senseRubble(me.add(dir));
		if(rc.onTheMap(me.add(dir.rotateLeft())))
			rubble[1]=rc.senseRubble(me.add(dir.rotateLeft()));
		if(rc.onTheMap(me.add(dir.rotateRight())))
			rubble[2]=rc.senseRubble(me.add(dir.rotateRight()));
		rubble[3]=rc.senseRubble(me);
		Direction[] dirs= {dir,dir.rotateLeft(),dir.rotateRight(),Direction.CENTER};

		//just use bubble sort cuz its easy and the list is small
		boolean noSwitch=false;
		while(!noSwitch) {
			noSwitch=true;
			for(int i=0;i<3;++i) {
				//bubble sort: swap if order is incorrect
				if(rubble[i]>rubble[i+1]) {
					//swaps both rubble and locs
					noSwitch=false;
					int temp=rubble[i];
					rubble[i]=rubble[i+1];
					rubble[i+1]=temp;
					Direction dirTemp=dirs[i];
					dirs[i]=dirs[i+1];
					dirs[i+1]=dirTemp;
				}
			}
		}
		for(int i=0;i<4;++i) {
			if(dirs[i]==Direction.CENTER)
				return;
			if(rc.canMove(dirs[i])) {
				rc.move(dirs[i]);
				return;
			}
		}
	}
	
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
    	//}
    	return index;
    }
    
    public MapLocation allAboard(RobotController rc) throws GameActionException{
		//we'll assume we put train destination in spot 10
		int comm=rc.readSharedArray(10);
		if(comm!=0) {
			MapLocation assignment=commToLoc(comm);
			//commented out code until teh devs fix onTheMap
			//if(rc.onTheMap(assignment)) {
				return assignment;
			//}else {
			//	return null;
			//}
		}else {
			return null;
		}
		
	}
    public void chooChoo(RobotController rc, MapLocation assignment) throws GameActionException{
		rc.setIndicatorString("train to "+assignment);
    	MapLocation me = rc.getLocation();
		MapLocation[] gold=rc.senseNearbyLocationsWithGold(20);
		MapLocation[] lead=rc.senseNearbyLocationsWithLead(20,2);
		MapLocation ore=null;
		if(gold.length!=0) {
			ore=gold[0];
		}else if(lead.length>20) {//special case for maptestsmall (and maybe other similar situations)
			//there be a lot of lead, don't get distracted
			for(int i=0;i<lead.length;++i) {
				//extra case: make sure lead is in the general direction of the destination
				Direction dirToAssignment=me.directionTo(assignment);
				Direction dirToLead=me.directionTo(lead[i]);
				if(dirToAssignment.getDeltaX()==dirToLead.getDeltaX()&&dirToAssignment.getDeltaY()==dirToLead.getDeltaY()) {
					ore=lead[i];
					break;
				}
			}
		}else if(lead.length!=0){
			//rc.setIndicatorString("lead length: "+lead.length);
			ore=lead[0];
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
    public void chooChooIgnore(RobotController rc, MapLocation assignment) throws GameActionException{
		rc.setIndicatorString("train to "+assignment);
    	MapLocation me = rc.getLocation();
		MapLocation[] gold=rc.senseNearbyLocationsWithGold(20);
		MapLocation[] lead=rc.senseNearbyLocationsWithLead(20,2);
		MapLocation ore=null;
		if(gold.length!=0) {
			ore=gold[0];
		}else if(lead.length>20) {//special case for maptestsmall (and maybe other similar situations)
			//there be a lot of lead, don't get distracted
			for(int i=0;i<lead.length;++i) {
				//extra case: make sure lead is in the general direction of the destination
				Direction dirToAssignment=me.directionTo(assignment);
				Direction dirToLead=me.directionTo(lead[i]);
				if(dirToAssignment.getDeltaX()==dirToLead.getDeltaX()&&dirToAssignment.getDeltaY()==dirToLead.getDeltaY()) {
					ore=lead[i];
					break;
				}
			}
		}else if(lead.length!=0){
			//rc.setIndicatorString("lead length: "+lead.length);
			//I know this seems like a lot of loops, but it really isn't that bad
			//lead[] is guaranteed to be smaller than 20 and adjacentToLead is guaranteed to be less than 10
			for(int i=0;i<lead.length;++i) {
				//ignore the lead if its adjacent to a miner
				RobotInfo[] adjacentToLead=rc.senseNearbyRobots(lead[i],2,rc.getTeam());
				boolean containsMiner=false;
				for(int j=0;j<adjacentToLead.length&&!containsMiner;++j) {
					containsMiner=(adjacentToLead[j].getType()==RobotType.MINER);
				}
				if(!containsMiner) {
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
    	rng=new Random(rc.getID()*rc.getRoundNum());
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
