package LAB_08;// To be modfied. See TODO: sections.


import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import LAB_08.RoutingTable.RoutingEntry;
import LAB_08.Simul.*;

public class Node implements Simul.Action {

 // Interface Sub Class: allows to attach a Node to a Network
 public class Interface {
	 private String id;
	 private Network net;


	 public Interface (String s, Network n) {
		 this.id=s;
		 this.net=n;
	 }

	 @Override
	 public String toString() {
		 return "Interface "+id+" connecting to network "+net.toString();
	 }

	 public void sendPacket(Packet p) {
		 this.net.sendPacket(p);
	 }

 }

 // Class Structure: 
 String id;									// Node name
 ArrayList<Interface> interfaces;			// The list of Interfaces
 private Simul sim;							// Internal: link to simulator (to add events)
 private RoutingTable table;
 
 // Constructor
 public Node(String name, Simul s) {
	 this.id=name;
	 this.interfaces=new ArrayList<>();
	 this.sim=s;
	 this.table=new RoutingTable();
 }
 
 // Internal: find an Interface to a Network
 private int findInterface(Network net) {
	 for (int i=0; i<interfaces.size(); i++) {
		 if (net.id.equals(interfaces.get(i).net.id))
			 return i;
	 }
	 return -1;
 }
 // Add Interface (to a given network) to this node.
 // (One can't have several interfaces to the same Network, verification is made)
 public void AddInterface(Network net) throws IllegalArgumentException  {
	 if (this.findInterface(net)>=0)
		 throw new IllegalArgumentException("Network"+net.id+" is already attached to node"+this.id);
	 int i=this.interfaces.size();
	 String s="eth"+i;
	 Interface newInt=new Interface(s,net);
	 interfaces.add(newInt);
}

 @Override
 public String toString() {
	 return this.id;
 }

 // Printing the node Characteristics
 public void print() {
	 int nbInt=this.interfaces.size();
	 String s;
	 if (nbInt>1)
		 s=" Interfaces";
	 else
		 s=" Interface";
	 System.out.println("Node "+this.id+" with "+nbInt+s);
	 for (int i=0;i<nbInt;i++)
	 {
		 System.out.println(" - "+this.interfaces.get(i).id+ " -> "+this.interfaces.get(i).net.id);
	 }
	 System.out.println("Routing table:");
	 this.table.print();

 }
 
// Generate First Event (Generation of first DV)
 public void start(int t) {
	 // Add event 
	 Instant tt=this.sim.CurrentTime().plusSeconds(t);
	 Simul.Event ev= Simul.Event.TIMER_EXPIRED;
	 this.sim.addEvent(tt,this,ev,null);
	 //Add Initial Entries in Routing Table
	 for (int i=0; i<this.interfaces.size();i++) {
		 this.table.addRoute(this.interfaces.get(i).net.id, 0, null);
	 }
	 
 }

	// TODO: Handling event (called by simulator)
	public void handle_event(Event e, Packet p) {
		boolean somethingChanged = false; // To be set to true if the routing table is modified when a DV is received.
		switch (e) {
			case PACKET_RECEIVED:
				System.out.println("Packet " + p.toString() + " received by Node " + this.id);

				// TODO: DV packet was received... It must be treated
				for (Map.Entry<String, Integer> entry : p.getDistanceVector().entrySet()) {
					String destination = entry.getKey();
					Integer distance = entry.getValue();

					if (!this.table.contains(destination) ||
							this.table.get(destination).dist > distance + 1) {
						this.table.setRoute(destination, new RoutingEntry(distance + 1, p.getSender()));
						somethingChanged = true;
					}
				}

				// Debug:
				System.out.println("Routing Table of Node " + this.id + " is now:");
				this.table.print();

				// (DON'T CHANGE THIS)
				// If triggeredUpdate option, send immediately and update to neighbors in case the routing table changed.
				if (this.sim.TriggeredUpdates && somethingChanged) {
					// Generate DV
					Packet newp = generateDistanceVector();
					newp.print();
					// Then we send the DV Packet to neighbors
					sendPacketToAllInterfaces(newp);
				}

				break;

			// (DON'T CHANGE THIS)
			// A timeout expired (DVs are generally sent periodically)
			case TIMER_EXPIRED:
				System.out.println("TIMER EXPIRED at Node " + this.id);
				// Send DV to all neighbors every 30s.
				// Generate the DV Packet (from Routing Table)
				System.out.println("  Generating DV");
				Packet newp = generateDistanceVector();
				newp.print();

				// Then we send the DV Packet
				sendPacketToAllInterfaces(newp);
				// Then we generate next TIMER_event in RefreshPeriod  (+1/-1 second)
				// (RefreshPeriod defined in Simul.java)
				Instant rt = this.sim.addRandomTime(this.sim.CurrentTime(), 1000); // 1000ms hence 1s
				Instant t = rt.plusSeconds(this.sim.RefreshPeriod);
				Event ev = Simul.Event.TIMER_EXPIRED;
				this.sim.addEvent(t, this, ev, null);
				break;
		}
		// If you want to send a DV to neighbors, use the function sendPacketToAllNeighbors(Packet p)
	}



	//Generating the DV from Routing Table
  // TODO: 
  Packet generateDistanceVector()
  {
	 Packet p=new Packet(this.id);
	 // Create the new packet from your routing table.
	 return p;
  }

 // Internal: send a packet to all interfaces
 private void sendPacketToAllInterfaces(Packet p) {
	 for (int i=0;i<interfaces.size();i++)
	 {
		 //Send packet to the network
		 interfaces.get(i).sendPacket(p);
	 }
 }
 
 
}
