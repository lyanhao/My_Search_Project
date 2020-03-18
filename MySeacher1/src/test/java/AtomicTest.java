import java.util.concurrent.atomic.AtomicInteger;

public class AtomicTest {
    /**
     * 多线程下-线程安全的计数器
     */
    private static volatile AtomicInteger COUNT = new AtomicInteger();

    public static void main(String[] args) {
        for (int i = 0; i < 20; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 10000; j++) {
                        COUNT.incrementAndGet();    //++i; 先加再获取
//                        COUNT.getAndIncrement();    //i++; 先获取再加
                    }
                }
            }).start();
        }
        while (Thread.activeCount() > 1) {
            Thread.yield();
        }
        System.out.println(COUNT);
    }
}
