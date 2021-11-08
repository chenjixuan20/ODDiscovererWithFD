package discoverer.total;

import discoverer.fd.Array.FDDiscoverNodeSavingInfoArray;
import discoverer.od.ODDiscovererNodeSavingInfo;

public class QueueElementArray {
    public static final int FD = 0;
    public static final int OD = 1;
    FDDiscoverNodeSavingInfoArray fdInfo;
    ODDiscovererNodeSavingInfo odInfo;
    int flag;

    public QueueElementArray(FDDiscoverNodeSavingInfoArray fdInfo, int flag){
        this.fdInfo = fdInfo;
        this.flag = flag;
    }

    public QueueElementArray(ODDiscovererNodeSavingInfo odInfo, int flag){
        this.odInfo = odInfo;
        this.flag = flag;
    }
}
