package structured;
import battlecode.common.*;
public class TileData{
	private MapLocation location;
	private int passibility;
	private boolean occupied;
	
	public TileData(MapLocation l, int p, boolean o) {
		location=l;
		passibility=p;
		occupied=o;
	}
	public MapLocation getLocation() {
		return location;
	}
	public int getPassibility() {
		return passibility;
	}
	public boolean getOccupied() {
		return occupied;
	}
	public void setOccupied(boolean o) {
		occupied=o;
	}
}
