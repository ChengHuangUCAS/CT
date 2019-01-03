package ticketingsystem;

//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class ThreadId2 {
    // Atomic integer containing the next thread ID to be assigned
    private static final AtomicInteger nextId = new AtomicInteger(0);

    // Thread local variable containing each thread's ID
    private static final ThreadLocal<Integer> threadId =
        new ThreadLocal<Integer>() {
            @Override protected Integer initialValue() {
                return nextId.getAndIncrement();
        }
    };

    // Returns the current thread's unique ID, assigning it if necessary
    public static int get() {
        return threadId.get();
    }
}

public class Test {
	
//	final static int threadnum = 16; // concurrent thread number
    final static int routenum = 5; // route is designed from 1 to 3
    final static int coachnum = 8; // coach is arranged from 1 to 5
    final static int seatnum = 100; // seat is allocated from 1 to 20
    final static int stationnum = 10; // station is designed from 1 to 5

    final static int testnum = 1500000;
    final static int retpc = 10; // return ticket operation is 10% percent
    final static int buypc = 40; // buy ticket operation is 30% percent
    final static int inqpc = 100; //inquiry ticket operation is 60% percent
    
    final static String passenger = "passenger";
    

    public static void main(String[] args) throws InterruptedException {

        Thread[] threads;
        TicketingDS tds;
        
        tds = new TicketingDS(1, 2, 5, 10, 1);
        smallTest(tds);
        
        for (int t = 1; t <= 128; t *= 2) {
        	threads = new Thread[t];
        	tds = new TicketingDS(routenum, coachnum, seatnum, stationnum, t);
        	long start_time = System.currentTimeMillis();
        	test(tds, threads);
        	long end_time = System.currentTimeMillis();
        	double elapsed = (end_time - start_time) / 1000.0;
        	int ops = testnum;
        	double throughput = ops / elapsed;
        	System.out.println("#threads: " + t + ", #operations: " + ops + ", time: " + elapsed +
        			"s, throughtput = " + throughput + "ops/s");
        }
        System.out.println("finish");
        
    }
    
    private static void smallTest(TicketingDS tds) {
    	Random rand = new Random();
    	Ticket ticket = new Ticket();
    	ArrayList<Ticket> soldTicket = new ArrayList<Ticket>();
    	for (int i = 0; i < 1000; i++) {
    		int departure = rand.nextInt(9) + 1;
    		int arrival = departure + rand.nextInt(10 - departure) + 1;
    		if((ticket = tds.buyTicket(passenger, 1, departure, arrival)) != null)
    			soldTicket.add(ticket);
    	}
    	for (Ticket t : soldTicket) {
    		if(t == null)
    			System.out.println("ErrOfRefund1");
    		if(!tds.refundTicket(t))
    			System.out.println("ErrOfRefund2");
    	}
    }
 
    private static void test(TicketingDS tds, Thread[] threads) {
        for (int i = 0; i< threads.length; i++) {
            threads[i] = new Thread(new Runnable() {
                public void run() {
//                	String filename = "thread_" + ThreadId2.get() + "_in_" + threads.length;
//                	File file = new File(filename);
//                try {
//					PrintStream ps = new PrintStream(new FileOutputStream(file));

                    Random rand = new Random();
                    Ticket ticket = new Ticket();
                    ArrayList<Ticket> soldTicket = new ArrayList<Ticket>();
                    
                    //System.out.println(ThreadId.get());
                    for (int i = 0; i < testnum / threads.length; i++) {
                        int sel = rand.nextInt(inqpc);
                        if (0 <= sel && sel < retpc && soldTicket.size() > 0) { // return ticket
                            int select = rand.nextInt(soldTicket.size());
                        if ((ticket = soldTicket.remove(select)) != null) {
                                if (tds.refundTicket(ticket)) {
//                                    System.out.println("TicketRefund" + " " + ticket.tid + " " + ticket.passenger + " " + ticket.route + " " + ticket.coach  + " " + ticket.departure + " " + ticket.arrival + " " + ticket.seat);
//                                    System.out.flush();
                                } else {
                                    System.out.println("ErrOfRefund1");
                                    System.out.println(ticket.tid + " " + ticket.passenger + 
                                    		" route:" + ticket.route + " coach:" + ticket.coach + 
                                    		" from:" + ticket.departure + " to:" + ticket.arrival + 
                                    		" seat:" + ticket.seat);
                                    System.out.flush();
                                }
                            } else {
                                System.out.println("ErrOfRefund2");
                                System.out.flush();
                            }
                        } else if (retpc <= sel && sel < buypc) { // buy ticket
                            int route = rand.nextInt(routenum) + 1;
                            int departure = rand.nextInt(stationnum - 1) + 1;
                            int arrival = departure + rand.nextInt(stationnum - departure) + 1; // arrival is always greater than departure
                            if ((ticket = tds.buyTicket(passenger, route, departure, arrival)) != null) {
                                soldTicket.add(ticket);
//                                ps.println(ticket.tid + " " + ticket.passenger + 
//                                    		" route:" + ticket.route + " coach:" + ticket.coach + 
//                                    		" from:" + ticket.departure + " to:" + ticket.arrival + 
//                                    		" seat:" + ticket.seat);
//                                System.out.println("TicketBought" + " " + ticket.tid + " " + ticket.passenger + " " + ticket.route + " " + ticket.coach + " " + ticket.departure + " " + ticket.arrival + " " + ticket.seat);
//                                System.out.flush();
                            } else {
//                                System.out.println("TicketSoldOut" + " " + route+ " " + departure+ " " + arrival);
//                                System.out.flush();
                            }
                        } else if (buypc <= sel && sel < inqpc) { // inquiry ticket
                            
                            int route = rand.nextInt(routenum) + 1;
                            int departure = rand.nextInt(stationnum - 1) + 1;
                            int arrival = departure + rand.nextInt(stationnum - departure) + 1; // arrival is always greater than departure
                            tds.inquiry(route, departure, arrival);
//                            int leftTicket = tds.inquiry(route, departure, arrival);
//                            System.out.println("RemainTicket" + " " + leftTicket + " " + route+ " " + departure+ " " + arrival);
//                            System.out.flush();  
                                                
                        }
                    }
//                } catch (FileNotFoundException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
                }
            });
              threads[i].start();
        }
    
        for (int i = 0; i< threads.length; i++) {
            try {
				threads[i].join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
    }
}
