package structured;
import battlecode.common.*;
public class Laboratory extends RobotLogic{
	public boolean run(RobotController rc) throws GameActionException{
		postTransmutationRate(rc);
		
		if(shouldTransmute(rc)&&rc.canTransmute()) {
			rc.transmute();
		}
		
		//distress signal code
		distressSignal(rc);
		return true;
	}
	//todo: post transmutation rate
	private void postTransmutationRate(RobotController rc) throws GameActionException{
		int transmutationRate=rc.getTransmutationRate();
		//post it
		
	}
	//todo: get info from archon on whether to transmute
	private boolean shouldTransmute(RobotController rc) throws GameActionException{
		//??
		return false;
	}
	//todo: communicate distress
	private void distressSignal(RobotController rc) throws GameActionException{
		
	}
}
