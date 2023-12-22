package LAB_08;// Don't modif this...


import java.time.Instant;
import java.util.ArrayList;

import LAB_08.Simul.*;

// Class Network: Simple network emulation:
// Simply a list of nodes attached to a network.
public class Network {
	
	// Class Structure: 
	public String id;					// Name
	public ArrayList<Node> nodes;		// List of nodes attached to the network
	private Simul sim;					// Internal: Link to simulator (to generate new events)
	
	// Constructor
	public Network(String id, Simul s) {
		this.id=id;
		this.nodes=new ArrayList<Node>();
		this.sim=s;
	}
	// Static variable
	public static final int MAXLANDELAY=10; // In Milliseconds
	
	// Add a node to a network
	public void addNode( Node n)  {
		this.nodes.add(n);
	}
	// Check if a node is already in the network
	public boolean contains (String s) {
		for (int i=0; i<nodes.size();i++)
		{
			if (nodes.get(i).id.equals(s))
				return true;
		}
		return false;
	}
	// Attach a node to an network
	//	- create an interface to this network in the node
	//	- add this node to the list of connected devices in this network.
	public void attachNode(Node n) throws IllegalArgumentException {
		if (nodes.contains(n) || this.contains(n.id))
			throw new IllegalArgumentException("Node is already in the Network.");
		n.AddInterface(this);
		this.nodes.add(n);
	}
	
	// Packet must be broadcasted in the network, to all attached nodes.
	// This is emulated by a PACKET_RECIEVED event for each node (with same packet).
	// Remark: We add a small random delay (different for each node)
	public void sendPacket(Packet p) {
		for (int i=0; i<this.nodes.size();i++) {
			// Send to all the nodes except the source node
			if (!p.source.equals(nodes.get(i).id)) {

				Instant t=this.sim.addRandomTime(this.sim.CurrentTime(),MAXLANDELAY);
				Event e= Simul.Event.PACKET_RECEIVED;
				System.out.println("  packet sent to "+nodes.get(i).id+ " (arriving at "+this.sim.printInstant(t)+")");
				this.sim.addEvent(t,nodes.get(i),e,p);
			}
			
		}
	}
	
	@Override 
	public String toString() {
		return "Network "+this.id;
	}
	
	public void print() {
		System.out.print(this.toString());
		System.out.print(" containing nodes (");
		for (int i=0;i<nodes.size();i++) {
			if (i>0)
				System.out.print(", ");
			System.out.print(nodes.get(i).toString());
		}
		System.out.println(")");
	}
	
}