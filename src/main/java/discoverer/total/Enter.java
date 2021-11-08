package discoverer.total;

import dataStructures.DataFrame;
import dataStructures.PartialDataFrame;
import dataStructures.fd.Array.FDTreeArray;
import dataStructures.fd.FDCandidate;
import dataStructures.fd.FDTreeNodeEquivalenceClasses;
import dataStructures.od.ODCandidate;
import dataStructures.od.ODTree;
import dataStructures.od.ODTreeNodeEquivalenceClasses;
import discoverer.fd.Array.BFSFDDiscovererArray;
import discoverer.od.BFSODDiscovererFull;
import minimal.ODMinimalCheckerBruteForce;
import sampler.OneLevelCheckingSampler;
import util.Timer;
import validator.fd.FDTreeIncrementalValidator;
import validator.od.ODBruteForceFullValidator;

import java.util.List;
import java.util.Set;

public class Enter {

    public static void main(String[] args) {
        long totalDiscoverTime=0;
        long totalValidateTime=0;

        DataFrame data = DataFrame.fromCsv("Data/ncv 1000 19-int.csv");
        OneLevelCheckingSampler sampler = new OneLevelCheckingSampler();
        PartialDataFrame sampleData = sampler.sample(data);
        System.out.println("抽样数据集大小：" + sampleData.getRowsCount());

        BFSTotalDiscovererArray discoverer = new BFSTotalDiscovererArray();

        FDTreeIncrementalValidator validator = new FDTreeIncrementalValidator();
        ODBruteForceFullValidator ODValidator = new ODBruteForceFullValidator();
        List<FDCandidate> fds;
        List<ODCandidate> ods;
        ODTree odReference = null;
        FDTreeArray fdReference = null;
        int i = 0;
        Timer timer = new Timer();
        while (true){
            i++;
            System.out.println("第" + i + "轮：");
            Timer subtimer=new Timer();
            if(i == 1){
                discoverer.discoverFirstTimes(sampleData, null);
                totalDiscoverTime += subtimer.getTimeUsed();
                odReference = discoverer.odTree;
                fdReference = discoverer.fdTreeArray;
                fds = discoverer.fdCandidates;
                ods = odReference.getAllOdsOrderByBFS();
            }else {
                discoverer.discoverAfterValidate(sampleData, odReference, fdReference);
                totalDiscoverTime += subtimer.getTimeUsed();
                odReference = discoverer.odTree;
                fdReference = discoverer.fdTreeArray;
                fds = discoverer.fdCandidates;
                ods = odReference.getAllOdsOrderByBFS();
            }

            System.out.println(fds);
            System.out.println("发现fd数量： " + fds.size());
            System.out.println(ods);
            System.out.println("发现od数量： " + ods.size());

            Timer vtimer=new Timer();
            Set<Integer> fdRows= validator.validate(fds,data);
            Set<Integer> ODRows = ODValidator.validate(odReference,data);
            totalValidateTime += vtimer.getTimeUsed();

            System.out.println("fd违约元组数:" + fdRows.size());
            sampleData.addRows(fdRows);
            System.out.println("增加fd违约后新数据集大小：" + sampleData.getRowCount());

            if(ODRows.isEmpty() && fdRows.isEmpty()){
                System.out.println("fd、od违约元组总数:" + 0);
                break;
            }
            System.out.println("od违约元组数:" + ODRows.size());
            sampleData.addRows(ODRows);
            System.out.println("增加od违约后新数据集大小：" + sampleData.getRowCount());
        }
        System.out.println("总时间： " + timer.getTimeUsed() /1000.0 + "s");
        System.out.println("discover总时间： " +  totalDiscoverTime/1000.0 + "s");
        System.out.println("validate总时间： " +  totalValidateTime/1000.0 + "s");
        System.out.println("最终发现fd总数：" + fds.size());
        System.out.println("最终发现od总数：" + ods.size());
    }
}
