package ticketingsystem;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class TicketingDS implements TicketingSystem {
    
    private int routeNum;
    private int coachNum;
    private int seatNum;
    private int stationNum;
    @SuppressWarnings("unused")
    private int threadNum;
    
    // 每个座位一个锁，买票/退票时先加锁
    private AtomicBoolean[][][] isSeatLocked;
    // 记录每个座位在每个基本区间是否被出售
//    private boolean[][][][] isSeatStationSold;
    private BitSet[][][] isSeatSold;
    // 每个车次 route 维护一个自身已售出的id，用来计算实际的tid
    // TODO: 可能的串行瓶颈：获取tid
    private AtomicLong[] ticketIds;
    // 每个车厢 coach 维护一个已售车票列表
    private ArrayList<ArrayList<ArrayList<Ticket>>> soldTickets;
    
    
    public TicketingDS(int routenum, int coachnum, int seatnum, int stationnum, int threadnum) {
        this.routeNum = routenum;
        this.coachNum = coachnum;
        this.seatNum = seatnum;
        this.stationNum = stationnum;
        this.threadNum = threadnum;
        
        this.isSeatLocked = new AtomicBoolean[routenum][coachnum][seatnum];
//        this.isSeatStationSold = new boolean[routenum][coachnum][seatnum][stationnum - 1];
        this.isSeatSold = new BitSet[routenum][coachnum][seatnum];
        this.ticketIds = new AtomicLong[routenum];
        this.soldTickets = new ArrayList<>();
        
        for (int i = 0; i < routenum; i++) {
            this.ticketIds[i] = new AtomicLong(0);
            this.soldTickets.add(new ArrayList<>());
            for (int j = 0; j < coachnum; j++) {
                this.soldTickets.get(i).add(new ArrayList<Ticket>());
                for (int k = 0; k < seatnum; k++) {
                    this.isSeatLocked[i][j][k] = new AtomicBoolean(false);
                    this.isSeatSold[i][j][k] = new BitSet(stationnum - 1);
//                    for (int l = 0; l < stationnum - 1; l++)
//                        this.isSeatStationSold[i][j][k][l] = false;
                }
            }
        }
        
    }

    @Override
    public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
        // 参数检查
        if (route > this.routeNum || departure > this.stationNum || arrival > this.stationNum 
                || route <= 0 || departure <= 0 || arrival < 0)
            return null;
        
        int seat = -1, coach = -1;
        BitSet want = new BitSet(this.stationNum - 1);
        want.set(departure - 1, arrival - 1);
        ArrayList<Integer> lockedSeats = new ArrayList<Integer>();
        for (int i = 0; i < this.coachNum; i++) {
            for (int j = 0; j < this.seatNum; j++) {
//                for (int k = departure - 1; k < arrival - 1; k++)
//                	// 某段被售出，座位不可用
//                	if (this.isSeatStationSold[route - 1][i][j][k])
//                		continue;
            	// 测试座位是否被售出
            	if (this.isSeatSold[route - 1][i][j].intersects(want))
            		continue;
                // 发现可用座位，尝试锁定。如果锁定成功，退出查询；如果锁定失败，记录座位，稍后重新尝试
                if (!this.isSeatLocked[route - 1][i][j].get() && 
                		this.isSeatLocked[route - 1][i][j].compareAndSet(false, true)) {
//                	// 锁定成功后重新检查
//                	boolean sold = false;
//                	for (int k = departure - 1; k < arrival - 1; k++) {
//                    	if (this.isSeatStationSold[route - 1][i][j][k]) {
//                    		this.isSeatLocked[route - 1][i][j].set(false);
//                    		sold = true;
//                    		break;
//                    	}
//                	}
//                	if (sold)
//                		continue;
                	// 锁定成功后重新检查
                	if (this.isSeatSold[route - 1][i][j].intersects(want)) {
                		this.isSeatLocked[route - 1][i][j].set(false);
                		continue;
                	}
                	
                	// 检查通过
                    coach = i + 1;
                    seat = j + 1;
                    break;
                } else
                    lockedSeats.add(i * this.seatNum + j);
            }
            // 已经成功锁定座位，不再继续查找
            if (coach != -1)
                break;
        }
        
        // 如果锁定座位失败，从记录中重新尝试锁定
        if (coach == -1) {
            while (lockedSeats.size() != 0) {
                // 逐个检查
                int tmp = lockedSeats.remove(0);
                int i = tmp / this.seatNum, j = tmp % this.seatNum;
                if (this.isSeatSold[route - 1][i][j].intersects(want))
            		continue;
//                for (int k = departure - 1; k < arrival - 1; k++)
//                	// 某段被售出，座位不可用
//                	if (isSeatStationSold[route - 1][i][j][k])
//                		continue;
                // 座位仍然可用，再次尝试锁定；锁定失败时需要再次放回列表
                if (!this.isSeatLocked[route - 1][i][j].get() && 
                		this.isSeatLocked[route - 1][i][j].compareAndSet(false, true)) {
//                	// 锁定成功后重新检查
//                	boolean sold = false;
//                	for (int k = departure - 1; k < arrival - 1; k++) {
//                    	if (this.isSeatStationSold[route - 1][i][j][k]) {
//                    		this.isSeatLocked[route - 1][i][j].set(false);
//                    		sold = true;
//                    		break;
//                    	}
//                	}
//                	if (sold)
//                		continue;
                	if (this.isSeatSold[route - 1][i][j].intersects(want)) {
                		this.isSeatLocked[route - 1][i][j].set(false);
                		continue;
                	}
                	
                    coach = i + 1;
                    seat = j + 1;
                    break;
                } else
                    lockedSeats.add(tmp);
            }
        }
        
        // 如果成功锁定座位，则出票；否则购票失败
        if (coach != -1) {
//            for (int k = departure - 1; k < arrival - 1; k++)
//            	this.isSeatStationSold[route - 1][coach -1][seat - 1][k] = true;
        	this.isSeatSold[route - 1][coach - 1][seat - 1].or(want);
            this.isSeatLocked[route - 1][coach - 1][seat - 1].set(false);
            //出票
            Ticket ticket = new Ticket();
            long routeTid = this.ticketIds[route - 1].getAndIncrement();
            ticket.tid = routeTid * this.routeNum + route;
            ticket.passenger = passenger;
            ticket.route = route;
            ticket.coach = coach;
            ticket.seat = seat;
            ticket.departure = departure;
            ticket.arrival = arrival;
            // 加入已售出列表
            ArrayList<Ticket> soldList = this.soldTickets.get(route - 1).get(coach - 1);
            synchronized(soldList) {
                soldList.add(ticket);
            }
            return ticket;
        } else 
            return null;
    }

    @Override
    public int inquiry(int route, int departure, int arrival) {
        // 参数检查
        if (route > this.routeNum || departure > this.stationNum || arrival > this.stationNum 
                || route <= 0 || departure <= 0 || arrival < 0)
            return 0;
        
        int count = 0;
        BitSet want = new BitSet(this.stationNum - 1);
        want.set(departure - 1, arrival - 1);
        // 无锁遍历
        for (int i = 0; i < this.coachNum; i++) {
            for (int j = 0; j < this.seatNum; j++) {
//            	for (int k = departure - 1; k < arrival - 1; k++)
//                	if (this.isSeatStationSold[route - 1][i][j][k])
//                		continue;
//                count++;
            	if (!this.isSeatSold[route - 1][i][j].intersects(want))
            		count++;
            }
        }
        
        return count;
    }

    @Override
    public boolean refundTicket(Ticket ticket) {
        int route = ticket.route, coach = ticket.coach, seat = ticket.seat;
        int departure = ticket.departure, arrival = ticket.arrival;
        
        // 无效票
        if (route <= 0 || route > this.routeNum || coach <= 0 || coach > this.coachNum)
        	return false;
        if (!soldTickets.get(route - 1).get(coach - 1).contains(ticket))
            return false;

        BitSet sold = new BitSet(this.stationNum - 1);
        sold.set(departure - 1, arrival - 1);
        // 把这个座位锁住！然后改为未出售！然后赶快释放锁！
        while (this.isSeatLocked[route - 1][coach - 1][seat - 1].get() || 
        		!this.isSeatLocked[route - 1][coach - 1][seat - 1].compareAndSet(false, true)) ;
//        for (int k = ticket.departure - 1; k < ticket.arrival - 1; k++)
//        	this.isSeatStationSold[route - 1][coach -1][seat - 1][k] = false;
        this.isSeatSold[route - 1][coach -1][seat - 1].andNot(sold);
        this.isSeatLocked[route - 1][coach - 1][seat - 1].set(false);
        
        // 这张票！不要了！
        ArrayList<Ticket> soldList = this.soldTickets.get(route - 1).get(coach - 1);
        synchronized(soldList) {
            soldList.remove(ticket);
        }
        
        return true;
    }


}
