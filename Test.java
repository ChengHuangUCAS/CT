package ticketingsystem;

import java.util.ArrayList;
import java.util.Random;

public class Test {
    
    static int routenum = 5, coachnum = 8, seatnum = 100, stationnum = 10, threadnum = 16;
    
    static int testnum = 1000;

    public static void main(String[] args) throws InterruptedException {
        
        final TicketingDS tds = new TicketingDS(routenum, coachnum, seatnum, stationnum, threadnum);

        buyTicketTest(tds);
//        Thread[] threads = new Thread[threadnum];
           queryTest(tds);
        
    }
    
    private static void buyTicketTest(TicketingDS tds) {
        ArrayList<Ticket> soldTicket = new ArrayList<Ticket>();
        Random rand = new Random();
        Ticket ticket = new Ticket();
        String passenger = "admin";
        for (int i = 0; i < testnum; i++) {
            int route = rand.nextInt(routenum) + 1;
            int departure = rand.nextInt(stationnum - 1) + 1;
            int arrival = departure + rand.nextInt(stationnum - departure) + 1;
            if ((ticket = tds.buyTicket(passenger, route, departure, arrival)) != null) {
                soldTicket.add(ticket);
                System.out.println("TicketBought" + " " + ticket.tid + " " + ticket.passenger + " " + ticket.route + " " + ticket.coach + " " + ticket.departure + " " + ticket.arrival + " " + ticket.seat);
                System.out.flush();
            } else {
                System.out.println("TicketSoldOut" + " " + route+ " " + departure+ " " + arrival);
                System.out.flush();
            }
        }
    }
    
    private static void queryTest(TicketingDS tds) {
        Random rand = new Random();
        for (int i = 0; i < testnum; i++) {
            int route = rand.nextInt(routenum) + 1;
            int departure = rand.nextInt(stationnum - 1) + 1;
            int arrival = departure + rand.nextInt(stationnum - departure) + 1; // arrival is always greater than departure
            int leftTicket = tds.inquiry(route, departure, arrival);
            System.out.println("RemainTicket" + " " + leftTicket + " " + route+ " " + departure+ " " + arrival);
            System.out.flush();
        }
    }
}
