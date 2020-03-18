import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public class WaitTest {

    /**
     * 等待所有线程执行完毕：
     * 1.CountDownlatch：初始化一个数值，countDown()对数值进行自减操作，await()等待阻塞直到i=0
     * 2.Semaphore：release()进行一定数量许可的颁发，acquire()阻塞并等待一定数量的许可，如果许可数值足够则继续执行，不够则等待许可数量足够
     * 相比而言，semaphore功能更强大更灵活
     *
     * @param args
     */
    private static int COUNT = 5;
    private static CountDownLatch LATCH = new CountDownLatch(COUNT);
    private static Semaphore SEMAPHORE = new Semaphore(0);


    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < COUNT; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println(Thread.currentThread().getName());
//                    LATCH.countDown();  //i--操作 安全的
                    SEMAPHORE.release();    //颁发一定数量许可证，无参则颁发一个数量
                }
            }).start();
        }
//        LATCH.await();  //await()会阻塞并一直等待，直到LATCH的值为0
        SEMAPHORE.acquire(COUNT);    //无参代表请求资源数量为1，也可以请求指定数量的资源
        //main在所有子线程执行完毕后再执行以下代码
        System.out.println(Thread.currentThread().getName());
    }
}
