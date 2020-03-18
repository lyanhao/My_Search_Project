package task;

import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FileScanner {

    //1.核心线程数：始终运行的线程数量（正式工）
    //2.最大线程数：有新任务，并且当前运行线程数小于最大线程数，会创建新的线程来处理任务（正式工+临时工）
    //（2-1）（最大线程数-核心线程数）这些线程（临时工）就会关闭
    //3-4：超过3数量4单位的时间
    //5.工作的阻塞队列
    //6.如果超出工作队列的长度，任务要进行处理的方式【4种策略】
//    private ThreadPoolExecutor pool = new ThreadPoolExecutor(
//            3, 3, 0,
//            TimeUnit.MICROSECONDS, new LinkedBlockingQueue<>(),
//            new ThreadPoolExecutor.AbortPolicy());

    private ExecutorService pool = Executors.newFixedThreadPool(4);

    //计数器，不传入数值默认为0，传入数值为初始值
    private volatile AtomicInteger count = new AtomicInteger();

    //线程等待的锁对象
    private Object lock = new Object(); //第一种：synchronized(lock)进行wait等待
    private CountDownLatch latch = new CountDownLatch(1);   //第二种：await()阻塞等待直到latch=0
    private Semaphore semaphore = new Semaphore(0); //第三种：acquire()阻塞等待一定数量的许可

    private ScanCallBack callback;

    public FileScanner(ScanCallBack callBack) {
        this.callback = callBack;
    }

    /**
     * 扫描文件目录
     *
     * @param path 待扫描路径
     */
    public void scan(String path) {
        count.incrementAndGet();    //计数器++i操作
        doScan(new File(path));
    }

    /**
     * 实现扫描细节
     *
     * @param dir 待处理的文件夹
     */
    private void doScan(File dir) {
        pool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.callBack(dir);
                    File[] children = dir.listFiles();  //下一级文件和文件夹
                    if (children != null) {
                        for (File child : children) {
                            if (child.isDirectory()) {
                                count.incrementAndGet();    //启动子文件夹扫描任务，计数器++i
                                System.out.println("当前任务数：" + count.get());
                                doScan(child);
                            }
                        }
                    }
                } finally {
                    //保证线程计数不管是否出现异常，都能够进行自减操作
                    int r = count.decrementAndGet();
                    if (r == 0) {
//                        第一种
//                        synchronized (lock) {
//                            lock.notify();
//                        }
//                        第二种
//                        latch.countDown();
//                        第三种
                        semaphore.release();
                    }
                }
            }
        });
    }

    /**
     * 等待扫描任务（scan方法）结束
     * 多线程的任务等待：
     * 1.join()：需要使用Thread类的引用对象来使用
     * 2.wait()：线程间的等待
     */
    public void waitFinish() throws InterruptedException {
        //第一种
//        synchronized (lock) {
//            lock.wait();
//        }
        //第二种
//        latch.await();
        try {
            //第三种
            semaphore.acquire();
        } finally {
            //阻塞等待直到任务完成，完成后需要关闭线程池
            System.out.println("正在关闭线程池...");

            /*以下两种关闭线程池的方式，内部原理都是通过内部Thread.interrupt()来中断线程
             如果正在扫描一个大目录，又重新选定了另一个小目录，应该将扫描大目录的任务终止
             而shutdown()方法内部会将正在跑的任务和队列里的任务执行完，才能真正停止。
             shutdoNow()会忽略队列里的任务，并将正在执行的任务 interrupt 中断*/

//            pool.shutdown();
            pool.shutdownNow();
        }
    }
}
