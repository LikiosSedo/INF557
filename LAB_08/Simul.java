package LAB_08;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.PriorityQueue;
import java.util.Random;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

// Core Simulator

public class Simul {

	// Currently only two types of Events
	enum Event {
		PACKET_RECEIVED,  TIMER_EXPIRED
	}

	private  boolean StepByStep=true;
	
	
	// Modify this field whether you wnt to hve triggered update enabled or not.
	// (not enabled = false) 
	public  boolean TriggeredUpdates=true;
	// Refresh period
	public int RefreshPeriod=30;
	
	// Internal (handle_event must be defined to interract with simulator)
	public interface Action {

		  public void handle_event(Event e, Packet p);

		}
	// Definition of an Event (Entry in the Discrete Event Simulator)

	public class SimEvent implements Comparable<SimEvent> {

	    private Instant time;			// Time when the event shall occur
	    private Node node;				// Node when event shall occur
	    private Event event;			// Type of Event
	    private Packet packet;			// Packet (may be null (for Timer_Expired event)

	    public SimEvent(Instant time, Node n, Event e, Packet p) {
	    	super();
	        this.time = time;
	        this.node=n;
	        this.event=e;
	        this.packet=p;
	    }

	    // In a PriorityQueue, sort elements based on time.
	    @Override
	    public int compareTo(SimEvent e) {
	        return this.time.compareTo(e.time);
	    }

	    @Override
	    public String toString() {
	        return "@"+printInstant(this.time)+ "Event: "+this.event.toString()+" at Node "+this.node.toString();
	    }
	}

	// Discrete Event Simulator Structure:
	PriorityQueue<SimEvent> events;			// The list of events, sorted by time
	private Instant time;					// Current time
	private Duration simDuration;			// Duration of the simulation
	private Duration maxDuration;			// Max Duration (simulation stops after)
	Random r;								// Random Number Generator

	// Constructor
	public Simul(Duration max, boolean s) {
		this.events = new PriorityQueue<>();
		this.time=Instant.now();
		this.simDuration=Duration.ZERO;
		this.maxDuration=max;
		this.r=new Random();
		this.StepByStep=s;
		System.out.println("New Simulation starting... Stopping after "+max.toMinutes()+" minutes");
	}
	
	// Add a random delay to a time t
	// Range m: in milliseconds
	public Instant addRandomTime (Instant t, int m) {
		int rd =r.nextInt(m);
		return  t.plusMillis(rd);
	}

	// Add Event in the Discrete Event Simulator
	public void addEvent(Instant time, Node n, Event e, Packet p)
	{
		SimEvent ev = new SimEvent (time, n,e,p);
		this.events.add(ev);
	}

	// Gives current time of the simulation
	public Instant CurrentTime() {
		return time;
	}
	// Main loop:
	// Stops when no longer event exist or mac duration is exceeded
	// otherwise:
	//		- remove next event from list of events (sorted by time)
	//		- handle event (in associated node )
	public void runSimulation () {
		// if event list if not empty
		while (!events.isEmpty()) {
			if (StepByStep) {
				System.out.println("Press any key to continue (to jump to next Event)");
				try{System.in.read();}
				catch(Exception e){}
			}

			// pick next event
			SimEvent ev =events.poll();

			// moving to new time instant

			simDuration=simDuration.plus(Duration.between(time,ev.time));
			time=ev.time;
			
			System.out.println(" Simulation now at "+printInstant(time)+" ("+printDuration(simDuration)+" )");
			// Stop simulation if MaxDuration exceeded
			if (simDuration.compareTo(maxDuration)>0)
			{
				System.out.println(" *** Max Simulation Duration exceeded. Stopping Simulation.");
				System.exit(0);
			}
			else {
				System.out.println(ev.toString());
				// Call the node method to handle the event
				ev.node.handle_event(ev.event,ev.packet);
			}
		}
	}

	public String printInstant(Instant t) {
		//DateTimeFormatter formatter =
		//	    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
		//	    .withLocale(Locale.US)
        //        .withZone( ZoneId.systemDefault() );
		DateTimeFormatter formater = DateTimeFormatter.ofPattern("HH:mm:ss:SSS")
	            .withZone(ZoneId.systemDefault());
		return formater.format( t );
	}
	
	public static String printDuration(Duration duration) {
	    return duration.toString()
	            .substring(2)
	            .replaceAll("(\\d[HMS])(?!$)", "$1 ")
	            .toLowerCase();
	}
	
	public static void main(String[] args) throws IOException {
		Duration d=Duration.ofMinutes(2);
		Simul s=new Simul(d,true);
		
		// Create Topology
		// - Nodes
		Node A=new Node("A",s);
		Node B=new Node("B",s);
		Node C=new Node("C",s);
		Node D=new Node("D",s);
		Node E=new Node("E",s);
		Node F=new Node("F",s);
		// - Networks
		//   Stub Networks First
		Network N1=new Network("Net1",s);
		N1.attachNode(A);
		Network N2=new Network("Net2",s);
		N2.attachNode(B);
		Network N3=new Network("Net3",s);
		N3.attachNode(C);
		Network N4=new Network("Net4",s);
		N4.attachNode(D);
		Network N5=new Network("Net5",s);
		N5.attachNode(E);
		Network N6=new Network("Net6",s);
		N6.attachNode(F);
		//    Interconnection (Graph)
		Network AB=new Network("NetAB",s);
		AB.attachNode(A);
		AB.attachNode(B);
		Network AC=new Network("NetAC",s);
		AC.attachNode(A);
		AC.attachNode(C);
		Network BC=new Network("NetBC",s);
		BC.attachNode(B);
		BC.attachNode(C);
		Network BD=new Network("NetBD",s);
		BD.attachNode(B);
		BD.attachNode(D);
		Network CE=new Network("NetCE",s);
		CE.attachNode(C);
		CE.attachNode(E);
		Network DE=new Network("NetDE",s);
		DE.attachNode(D);
		DE.attachNode(E);
		Network DF=new Network("NetDF",s);
		DF.attachNode(D);
		DF.attachNode(F);
		Network EF=new Network("NetEF",s);
		EF.attachNode(E);
		EF.attachNode(F);
		
		// Start nodes (Build init Routing Table & Generate First events)
		A.start(10);B.start(20);C.start(30);D.start(40);E.start(50);F.start(60);
		
		// Debug:
		A.print();B.print();C.print();D.print();E.print();F.print();
		AB.print();AC.print();BC.print();BD.print();CE.print();DE.print();DF.print();EF.print();


		// Run Simulation
		s.runSimulation();
		

	}
}
