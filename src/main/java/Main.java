import dataStructures.DataFrame;
import dataStructures.fd.FDCandidate;
import dataStructures.od.ODCandidate;
import discoverer.fd.Array.BFSFDDiscovererArray;
import util.Timer;
import dataStructures.od.ODTree;
import discoverer.od.BFSODDiscovererFull;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        DataFrame data = DataFrame.fromCsv("Data/FLI 10K.csv");
//        DataFrame data = DataFrame.fromCsv("Data/echocardiogram.csv");

//        System.out.println("将FD中的等价类转化为OD中的");
//        System.gc();
//        Timer timer = new Timer();
//        Timer timerfd = new Timer();
//        FDToODSavingInfo info = new BFSFDDiscoverer().discoverCandidateRefinementPlus(data);
//        System.out.println("fd时间:"+ timerfd.getTimeUsedAndReset()/1000.0 + "s");
//        System.out.println("发现FDs的数量： " + info.fdCandidates.size());
//        Timer timerod = new Timer();
//        ODTree discover = new BFSODDiscovererFull().discoverFDPlus(data, info);
//        List<ODCandidate> ods = discover.getAllOdsOrderByBFS();
//        System.out.println("od时间:"+ timerod.getTimeUsedAndReset()/1000.0 + "s");
//        System.out.println("最终时间:"+ timer.getTimeUsedAndReset()/1000.0 + "s");
//        System.out.println("发现ODs的数量："+ods.size());

        System.out.println("-----------------");
        System.out.println("直接算OD的等价类");
        System.gc();
        Timer timer2 = new Timer();
        Timer timerfd2 = new Timer();
        List<FDCandidate> fds = new BFSFDDiscovererArray().discoverFirstTimes(data).fdCandidates;
//        for(FDCandidate fd:fds){
//            System.out.println(fd);
//        }
        System.out.println("fd时间:"+ timerfd2.getTimeUsedAndReset()/1000.0 + "s");
        System.out.println("发现FDs的数量： " + fds.size());
        Timer timerod2 = new Timer();
        ODTree discover2 = new BFSODDiscovererFull().discover(data);
        List<ODCandidate> ods2 = discover2.getAllOdsOrderByBFS();
        System.out.println("od时间:"+ timerod2.getTimeUsedAndReset()/1000.0 + "s");
        System.out.println("最终时间:"+ timer2.getTimeUsedAndReset()/1000.0 + "s");

//        for(ODCandidate od : ods2){
//            System.out.println(od);
//        }
        System.out.println("发现ODs的数量："+ods2.size());
    }
}
