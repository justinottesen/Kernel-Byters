package structured;
import battlecode.common.*;
public class Laboratory extends RobotLogic{
	public boolean run(RobotController rc) throws GameActionException{
		int leadToTransmute=getAmountToTransmute(rc);
		int rate=rc.getTransmutationRate();
		int numOfTransmutations=leadToTransmute/rate;
		for(int i=0;i<numOfTransmutations;++i) {
			if(rc.canTransmute()) {
				rc.transmute();
			}
		}
		
		//distress signal code
		distressSignal(rc);
		return true;
	}
	//todo: get the amount of lead to transmute
	private int getAmountToTransmute(RobotController rc) throws GameActionException{
		//??
		return 69;
	}
	//todo: communicate distress
	private void distressSignal(RobotController rc) throws GameActionException{
		
	}
}