package discoverer;

import discoverer.fd.FDDiscoverNodeSavingInfo;
import discoverer.od.ODDiscovererNodeSavingInfo;


public class QueueElement {
    public static final int FD = 0;
    public static final int OD = 1;
    FDDiscoverNodeSavingInfo fdInfo;
    ODDiscovererNodeSavingInfo odInfo;
    int flag;

    public QueueElement(FDDiscoverNodeSavingInfo fdInfo, int flag){
        this.fdInfo = fdInfo;
        this.flag = flag;
    }

    public QueueElement(ODDiscovererNodeSavingInfo odInfo, int flag){
        this.odInfo = odInfo;
        this.flag = flag;
    }
}
