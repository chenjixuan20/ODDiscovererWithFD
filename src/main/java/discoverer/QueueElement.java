package discoverer;

import discoverer.fd.FDDiscoverNodeSavingInfo;
import discoverer.od.ODDiscoverNodeSavingInfo;


public class QueueElement {
    public static final int FD = 0;
    public static final int OD = 1;
    FDDiscoverNodeSavingInfo fdInfo;
    ODDiscoverNodeSavingInfo odInfo;
    int flag;

    public QueueElement(FDDiscoverNodeSavingInfo fdInfo, int flag){
        this.fdInfo = fdInfo;
        this.flag = flag;
    }

    public QueueElement(ODDiscoverNodeSavingInfo odInfo, int flag){
        this.odInfo = odInfo;
        this.flag = flag;
    }
}
