package LAB_08;

import java.util.Map;
import java.util.HashMap;

public class RoutingTable {
	// Routing Entry
	public static class RoutingEntry {
		int dist; 			// Distance
		String next_hop;	// Next-hop 
        
        // Constructor
		public RoutingEntry () {
        	this.dist = Integer.MAX_VALUE;
        	this.next_hop = null;
		}
		// Constructor
        public RoutingEntry(int dist, String nh) {
        	this.dist = dist;
        	this.next_hop = nh;
        }
        
	}
	
	// Class Structure
	public Map<String,RoutingEntry> rt;
	
	public RoutingTable() {
		this.rt= new HashMap<String,RoutingEntry>();
	}
	
	public void addRoute(String n, RoutingEntry e) {
		rt.put(n,e);		// Built-in ArrayList
	}
	
	public void addRoute(String n, int d, String nh) {
		RoutingEntry re=new RoutingEntry(d,nh);
		rt.put(n,re);		// Built-in ArrayList
	}
	
	
	// Modify a route (or create it if does not exist)
	public void setRoute(String n, RoutingEntry e) {
		rt.put(n,e);
	}
	
	public RoutingEntry get(String n) {
		return rt.get(n);
	}
	
	public boolean contains(String n) {
		return rt.containsKey(n);
	}
	
	// Print Routing Table
     public void print() {
    	 for (Map.Entry<String, RoutingEntry> i : this.rt.entrySet()) {
			    String dest=i.getKey();
			    RoutingEntry r=i.getValue();
			    if (r.next_hop!=null) 
			    	System.out.println(dest+" ("+r.dist+") via "+r.next_hop);
			    else
			    	System.out.println(dest+" ("+r.dist+") via ---");
     }   
   }
}
