package task1;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Обоснование выбранной реализации многопоточной коллекции:
 * 1. В задаче подразумевается частое добавление элементов и более редкое их чтение несколькими потоками.
 *    Следовательно, коллекция не должна блокировать потоки. -> не Blocking
 * 2. После чтения элемента он перестаёт быть нужен другим потокам, поэтому логично использовать
 *    коллекцию интерфейса Queue с удалением из обработанных элементов из начала очереди. -> Queue/Deque
 * 3. Для реализации последовательной работы с коллекцией достаточно принципа FIFO. -> Qeque
 * 4. Перечисленным требованиям (Non-Blocking Queue) отвечает очередь ConcurrentLinkedQueue.
 *
 *
 * Обнаружил проблему. При большей части запусков программы при числе потоков-операторов > 1
 * некоторые из них простаивают. Т.е. вместо 2 может работать только 1, вместо 3 только 1-2 и т.д.
 * При этом поток не завершается и не зацикливается. Я не смог отследить, что происходит.
 * С одним оператором проблем нет. Подскажите, пожалуйста, в чём причина?
 */

public class Main_task1 {
    private static final String RESET_COLOR = "\033[m";
    private static final byte NUM_OF_OPERATORS = 3;
    protected static final byte TOTAL_CALLS = 30;
    protected static final short CALL_DELAY = 600;
    protected static final short ANSWER_TIME = 2_000;

    public static void main(String[] args) {
        System.out.println(RESET_COLOR + "\tЗадача 1. Call-центр\n");
        ExecutorService pool = Executors.newFixedThreadPool(NUM_OF_OPERATORS);
        Station station = new Station();

        Thread t = new Thread(station);
        t.start();
        for (int i = 0; i < NUM_OF_OPERATORS; i++) {
            pool.submit(new Operator(station.getCalls()));
        }
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        pool.shutdown();
    }
}

class Call {
    private final int ID;
    private static final AtomicInteger counter = new AtomicInteger();

    public Call() { ID = counter.incrementAndGet(); }

    public int getID() { return ID; }
}

class Station implements Runnable {
    private final Queue<Call> calls;
    private static final AtomicInteger counter = new AtomicInteger();
    public static final String COLOR = "\033[34m";

    Station() { calls = new ConcurrentLinkedQueue<>(); }

    public Queue<Call> getCalls() { return calls; }

    public void newCall(Call call) {
        System.out.println(COLOR + "Поступил новый звонок #" + counter.incrementAndGet());
        calls.add(call);
    }

    @Override
    public void run() {
        Thread.currentThread().setName("АТС");
        System.out.println(Station.COLOR + "АТС запущена");
        for (int i = 1; i <= Main_task1.TOTAL_CALLS; i++) {
            this.newCall(new Call());
            if (i == 1) Operator.setWork(true);
            try {
                Thread.sleep(Main_task1.CALL_DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Operator.setWork(false);
        System.out.println(Station.COLOR + "АТС больше не принимает звонки");
    }
}

class Operator implements Runnable {
    private static boolean isWork;
    private static final AtomicInteger counter = new AtomicInteger();
    private final Queue<Call> calls;
    private final int ID;
    private final String COLOR = "\033[32m";

    public Operator(Queue<Call> calls) {
        ID = counter.incrementAndGet();
        this.calls = calls;
    }

    public void answer() {
        System.out.printf(COLOR + "%s отвечает на звонок №%d%s\n",
                this, calls.poll().getID(), isWork ? "" : " сверхурочно");
        try {
            Thread.sleep(Main_task1.ANSWER_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        System.out.printf(COLOR + "%s освободился\n", this);
    }

    public static boolean isWork() { return isWork; }

    public static void setWork(boolean work) { isWork = work; }

    @Override
    public void run() {
        Thread.currentThread().setName(this.toString());
        System.out.printf(COLOR + "%s готов к работе\n", Thread.currentThread().getName());
        while (!Operator.isWork() && !Thread.currentThread().isInterrupted());
        while (Operator.isWork() && !Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!calls.isEmpty())
                this.answer();
        }
        System.out.println(this);
        while (!Operator.isWork() && !calls.isEmpty() && !Thread.currentThread().isInterrupted())
            this.answer();
        System.out.println(COLOR + this + " завершил работу");
    }

    @Override
    public String toString() { return "Оператор " + ID; }
}
