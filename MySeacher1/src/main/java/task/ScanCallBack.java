package task;

import java.io.File;

public interface ScanCallBack {
    //对于文件夹的扫描任务进行回调
    void callBack(File dir);
}
