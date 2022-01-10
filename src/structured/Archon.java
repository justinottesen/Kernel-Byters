package structured;
import battlecode.common.*;
public class Archon extends RobotLogic {
	private int turnNum = 0;
	private int pbBudget = 0;
	private int locCommIndex = 0;
	private int builtSoldiers = 0;
	private int builtMiners = 0;
	//0=rotation, 1=reflectionX, 2=reflectionY
	private boolean[] possibleSymmetries= {true,true,true};
	private int chosenSymmetry=-1;
	//stores the locations of the 4 team archons (friendlyArchonsLocs[locCommIndex] == rc.getLocation())
	MapLocation[] friendlyArchonLocs= {null,null,null,null};
	MapLocation[] enemyArchonLocs= {null,null,null,null};
	
	private void updateCommLoc(RobotController rc) throws GameActionException{
		while (rc.readSharedArray(locCommIndex) != 0) {
			locCommIndex++;
		}
		//Andrew change: fixed syntax here
		rc.writeSharedArray(locCommIndex,locToComm(rc.getLocation()));
	}
	
	private void updateBudgetLoc(RobotController rc) throws GameActionException {
		if (locCommIndex == 0) {
			//Andrew change: what if our lead amount is more than the max flag?
			int leadAmount=rc.getTeamLeadAmount(rc.getTeam());
			if(leadAmount<=65535)
				rc.writeSharedArray(4, rc.getTeamLeadAmount(rc.getTeam()));
			else
				rc.writeSharedArray(4,65535);
		}
	}
	
	private void commUnderAttack(RobotController rc) throws GameActionException {
		int prevValue = rc.readSharedArray(5);
		if ((prevValue/Math.pow(10, locCommIndex))%2 == 0) { //NOT UNDER ATTACK LAST TURN
			if (enemySoldiersNearby(rc) == true) {
				rc.writeSharedArray(5, prevValue + (int)Math.pow(10, locCommIndex));
			}
		}
		if ((prevValue/Math.pow(10, locCommIndex))%2 == 1) { //UNDER ATTACK LAST TURN
			if (enemySoldiersNearby(rc) == false) {
				rc.writeSharedArray(5, prevValue - (int)Math.pow(10, locCommIndex));
			}
		}
	}
	
	private boolean enemySoldiersNearby(RobotController rc) throws GameActionException {
		Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(34, opponent);
        for(int i=0;i<enemies.length;++i) {
        	RobotType type=enemies[i].getType();
    		if(type==RobotType.SOLDIER||type==RobotType.SAGE||type==RobotType.WATCHTOWER) {
    			return true;
    		}
        }
        return false;
	}
	
	private void createRobot(RobotController rc, RobotType type) throws GameActionException {
		Direction dir = null;
		boolean built = false;
		if (type == RobotType.MINER || type == RobotType.SOLDIER) { // SOLDIER IS TEMPORARY CODE LATER ITS LIKE 1AM IM TIRED
			dir = rc.getLocation().directionTo(rc.senseNearbyLocationsWithLead(34)[0]);
		}
		if (rc.canBuildRobot(type, dir) == true) {
			rc.buildRobot(type, dir);
			built = true;
		} else if (rc.canBuildRobot(type, dir.rotateLeft()) == true) {
			rc.buildRobot(type, dir.rotateLeft());
			built = true;
		} else if (rc.canBuildRobot(type, dir.rotateRight()) == true) {
			rc.buildRobot(type, dir.rotateRight());
			built = true;
		} else if (rc.canBuildRobot(type, dir.rotateLeft().rotateLeft()) == true) {
			rc.buildRobot(type, dir.rotateLeft().rotateLeft());
			built = true;
		} else if (rc.canBuildRobot(type, dir.rotateRight().rotateRight()) == true) {
			rc.buildRobot(type, dir.rotateRight().rotateRight());
			built = true;
		} else if (rc.canBuildRobot(type, dir.rotateLeft().rotateLeft().rotateLeft()) == true) {
			rc.buildRobot(type, dir.rotateLeft().rotateLeft().rotateLeft());
			built = true;
		} else if (rc.canBuildRobot(type, dir.rotateRight().rotateRight().rotateRight()) == true) {
			rc.buildRobot(type, dir.rotateRight().rotateRight().rotateRight());
			built = true;
		} else if (rc.canBuildRobot(type, dir.rotateRight().rotateRight().rotateRight().rotateRight()) == true) {
			rc.buildRobot(type, dir.rotateRight().rotateRight().rotateRight().rotateRight());
			built = true;
		}
		if (built == true) {
			if (type == RobotType.MINER) { //FIX LATER, DEADLINE COMING UP THIS SUCKS BUT IT WORKS
				builtMiners += 1;
			} else if (type == RobotType.SOLDIER) {
				builtSoldiers += 1;
			}
		}
	}
	
	//returns the maplocation on the other half of the map
	//symmetryType 0=rotation, 1=reflectionX, 2=reflectionY
	private MapLocation calculateSymmetricalLocation(RobotController rc, MapLocation archon,int symmetryType) throws GameActionException{
		if(symmetryType==0) {
			return calculateRotation(rc,archon);
		}else if(symmetryType==1) {
			return calculateReflectionX(rc,archon);
		}
		return calculateReflectionY(rc,archon);
	}
	//does some quick maffs to find a 180 degree rotation of the base
	private MapLocation calculateRotation(RobotController rc,MapLocation archon) throws GameActionException{
		int height=rc.getMapHeight();
		int width=rc.getMapWidth();
		MapLocation middle=new MapLocation(width/2,height/2);
		int dx=middle.x-archon.x;
		int dy=middle.y-archon.y;
		return new MapLocation(archon.x+2*dx,archon.y+2*dy);
	}
	//does some quick maffs to find a reflection across X of the base
	private MapLocation calculateReflectionX(RobotController rc,MapLocation archon) throws GameActionException{
		int height=rc.getMapHeight();
		int width=rc.getMapWidth();
		MapLocation middle=new MapLocation(width/2,height/2);
		int dx=middle.x-archon.x;
		return new MapLocation(archon.x+2*dx,archon.y);
	}
	//does some quick maffs to find a reflection across Y of the base
	private MapLocation calculateReflectionY(RobotController rc,MapLocation archon) throws GameActionException{
		int height=rc.getMapHeight();
		int width=rc.getMapWidth();
		MapLocation middle=new MapLocation(width/2,height/2);
		int dy=middle.y-archon.y;
		return new MapLocation(archon.x,archon.y+2*dy);
	}
	//goes through all ally archons to decide on one train destination (the closest to all archons)
	private MapLocation calculateChooChooDestination(RobotController rc) throws GameActionException{
		//only recalculates when info is updated
		if(rc.getRoundNum()<10||updateSymmetryPossibilities(rc)) {
			int lowest=69696969;
			MapLocation chosenChooChoo=null;
			for(int i=0;i<4;++i) {
				if(friendlyArchonLocs[i]!=null) {
					//iterate through the 3 symmetries
					for(int k=0;k<3;k++) {
						if(possibleSymmetries[k]) {
							//System.out.println("symmetry "+k);
							//calculate average distance to that archon's chosen destination
							int distance=0;
							MapLocation possibleDestination=calculateSymmetricalLocation(rc,friendlyArchonLocs[i],k);
							//System.out.println("loc: "+possibleDestination);
							for(int j=0;j<4;++j) {
								if(friendlyArchonLocs[j]!=null) {
									distance=possibleDestination.distanceSquaredTo(friendlyArchonLocs[j]);
									//special case: overrides if the possible destination is within vision range of the archon
									if(distance<35) {
										//possibleDestination within vision range of a friendly archon (bad)
										distance+=69696969;
										//hopefully that overrides
									}else {//act normally
										//adds the distance
										distance+=possibleDestination.distanceSquaredTo(friendlyArchonLocs[j]);
									}
								}
							}
							//System.out.println("distance: "+distance);
							if(distance<lowest) {
								lowest=distance;
								chosenChooChoo=possibleDestination;
								chosenSymmetry=k;
							}
						}
					}
				}
			}
			if(chosenChooChoo!=null) {
				rc.setIndicatorString("train destination: "+chosenChooChoo);
				rc.writeSharedArray(10,locToComm(chosenChooChoo));
			}
			return chosenChooChoo;
		}
		return null;
	}
	
	//updates the possibleSymmetries array to reflect the enemy archon locations
	//ideally, it should narrow it down to only 1 of the possible symmetries
	//returns true if it actually updates something
	private boolean updateSymmetryPossibilities(RobotController rc) throws GameActionException{
		//System.out.println("updating symmetry");
		boolean updated=false;
		//update enemy archon arrays from comms
		MapLocation[] prevEnemyArchons=enemyArchonLocs;
		buildEnemyArchonArray(rc,enemyArchonLocs);
		//only updateSymmetry if the updated array is different and if there's more than one symmetry possibility left
		if(!equalsArray(prevEnemyArchons,enemyArchonLocs)) {
			System.out.println("detected array change");
			if(shouldReevaluateSymmetry()) {
				//goes through each of the enemy archon slots
				for (int j = 6; j < 10; j++) {
					int enemyArchonComm=rc.readSharedArray(j);
					if (enemyArchonComm!=0) {
						//if comm slot isn't empty...
						MapLocation enemyArchonLoc=commToLoc(enemyArchonComm);
						for(int i=0;i<3;++i) {
							if(possibleSymmetries[i]) {
								boolean symmetryWorks=false;
								//check that one of the symmetries matches with enemyArchonLoc
								for(int k=0;k<4&&!symmetryWorks;++k) {
									if(friendlyArchonLocs[k]!=null) {
										MapLocation projectedEnemyArchonLoc=calculateSymmetricalLocation(rc,friendlyArchonLocs[k],i);
										//if the projection matches reality (with a little leeway)
										if(projectedEnemyArchonLoc.isWithinDistanceSquared(enemyArchonLoc,3)) {
											symmetryWorks=true;
										}
									}
								}
								possibleSymmetries[i]=symmetryWorks;
							}
						}
					}
				}
			}
			updated=true;
		}
		if(rc.readSharedArray(11)==1) {//hey broski, the archon ain't there
			System.out.println("gotchu homie");
			//a little extra boundary check
			if(chosenSymmetry>=0&&chosenSymmetry<3) {
				possibleSymmetries[chosenSymmetry]=false;
				rc.writeSharedArray(11,0);
				updated=true;
			}
		}
		return updated;
	}
	//assuming archons is an array of 4 nulls
	//fills the array with the 4 locations of the friendly archons (leaves them null if they don't exist in comms)
	private void buildFriendlyArchonArray(RobotController rc, MapLocation[] archons) throws GameActionException{
		for(int i=0;i<4;++i) {
			int archonComm=rc.readSharedArray(i);
			if(archonComm!=0) {
				archons[i]=commToLoc(archonComm);
			}
		}
	}
	//assuming archons is an array of 4 nulls
	//fills the array with the 4 locations of the enemy archons (leaves them null if they don't exist in comms)
	private void buildEnemyArchonArray(RobotController rc, MapLocation[] enemyArchons) throws GameActionException{
		//the only difference from building the friendly array is the index
		for(int i=6;i<10;++i) {
			int archonComm=rc.readSharedArray(i);
			if(archonComm!=0) {
				enemyArchons[i-6]=commToLoc(archonComm);
			}
		}
	}
	//returns true if the contents of the two arrays are the same, false if they're not
	private boolean equalsArray(MapLocation[] array1,MapLocation[] array2) {
		if(array1.length!=array2.length)
			return false;
		for(int i=0;i<array1.length;++i) {
			//if both are null or both are the same
			if(array1[i]==null&&array2[i]==null)
				continue;
			if(((array1[i]==null)!=(array2[i]==null))||(array1[i].x!=array2[i].x||array1[i].y!=array2[i].y))
				return false;
		}
		return true;
	}
	private boolean shouldReevaluateSymmetry() {
		int numRemaining=0;
		for(int i=0;i<3;++i) {
			if(possibleSymmetries[i]) {
				numRemaining++;
			}
		}
		//numRemaining == 0 is bad
		return (numRemaining>1);
	}
	public boolean run(RobotController rc) throws GameActionException{
		turnNum ++;
		if (turnNum == 1) {
			updateCommLoc(rc);
			createRobot(rc, RobotType.MINER);
		}
		if(turnNum==2) {
			buildFriendlyArchonArray(rc,friendlyArchonLocs);
		}
		updateBudgetLoc(rc);
		commUnderAttack(rc);
		calculateChooChooDestination(rc);
		pbBudget = rc.readSharedArray(4)/rc.getArchonCount();
		//Andrew change: fixed a misspelling of soldier
		if (enemySoldiersNearby(rc) == true) {
			createRobot(rc, RobotType.SOLDIER);
		} else if (turnNum < super.TRANSITIONROUND) {//TRANSITIONROUND can be found and changed in RobotLogic (it's currently 50)
			if (pbBudget >= 50+2*turnNum) {
				createRobot(rc, RobotType.MINER);
			}
		} else if (turnNum % 2 == 0) {
			if (pbBudget >= 75) {
				createRobot(rc, RobotType.SOLDIER);
			} else if (builtSoldiers > 0.75*builtMiners) {
				createRobot(rc, RobotType.MINER);
			}
		}
    	return true;
	}
}
