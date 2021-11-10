package discoverer.total;

import dataStructures.DataFrame;
import dataStructures.PartialDataFrame;
import dataStructures.fd.Array.FDTreeArray;
import dataStructures.fd.FDCandidate;
import dataStructures.od.ODCandidate;
import dataStructures.od.ODTree;
import sampler.OneLevelCheckingSampler;
import util.Timer;
import validator.fd.FDTreeIncrementalValidator;
import validator.od.ODBruteForceFullValidator;
import discoverer.total.BFSTotalDiscovererArrayThreshold.*;
import validator.od.ODPrefixBasedIncrementalValidator;

import java.util.List;
import java.util.Set;

public class ThresholdEnter {

    public static void main(String[] args) {
        DataFrame data = DataFrame.fromCsv("Data/flights 50k.csv");
        OneLevelCheckingSampler sampler = new OneLevelCheckingSampler();
        PartialDataFrame sampleData = sampler.sample(data);
        System.out.println("抽样数据集大小：" + sampleData.getRowsCount());

        BFSTotalDiscovererArrayThreshold discoverer = new BFSTotalDiscovererArrayThreshold();

        FDTreeIncrementalValidator validator = new FDTreeIncrementalValidator();

//        ODBruteForceFullValidator ODValidator = new ODBruteForceFullValidator();
        ODPrefixBasedIncrementalValidator ODValidator = new ODPrefixBasedIncrementalValidator();

        List<FDCandidate> fds;
        List<ODCandidate> ods;
        ODTree odReference = null;
        FDTreeArray fdReference = null;
        int i = 0;
        Timer timer = new Timer();
        while (true){
            i++;
            System.out.println("第" + i + "轮：");
            discoverer.discover(sampleData,odReference,fdReference);
            odReference = discoverer.odTree;
            fdReference = discoverer.fdTreeArray;
            fds = discoverer.fdCandidates;
            ods = odReference.getAllOdsOrderByBFS();

            System.out.println(fds);
            System.out.println("发现fd数量： " + fds.size());
            System.out.println(ods);
            System.out.println("发现od数量： " + ods.size());

            Set<Integer> fdRows= validator.validate(fds,data);
            Set<Integer> ODRows = ODValidator.validate(odReference,data);


            if(!discoverer.getIsFirstDone()){
                if(discoverer.getIsInterrup() && ODRows.isEmpty() && fdRows.isEmpty())
                    discoverer.setInterruptStatus(InterruptStatus.CONTINUE_FIRST);
                else
                    discoverer.setInterruptStatus(InterruptStatus.START_FIRST);
            }else{
                if(!discoverer.getIsInterrup())
                    discoverer.setInterruptStatus(InterruptStatus.START_AFTER);
                else{
                    if(ODRows.isEmpty() && fdRows.isEmpty())
                        discoverer.setInterruptStatus(InterruptStatus.CONTINUE_AFTER);
                    else discoverer.setInterruptStatus(InterruptStatus.START_AFTER);
                }
            }

            if(ODRows.isEmpty() && fdRows.isEmpty()){
                System.out.println("fd、od违约元组总数:" + 0);
                if(!discoverer.getIsInterrup()){
                    System.out.println("没有发生中断，程序正常结束" );
                    break;
                }
                System.out.println("发生中断，程序继续运行");
            }

            System.out.println("fd违约元组数:" + fdRows.size());
            sampleData.addRows(fdRows);
            System.out.println("增加fd违约后新数据集大小：" + sampleData.getRowCount());

            System.out.println("od违约元组数:" + ODRows.size());
            sampleData.addRows(ODRows);
            System.out.println("增加od违约后新数据集大小：" + sampleData.getRowCount());
        }
        System.out.println("总时间： " + timer.getTimeUsed() /1000.0 + "s");
        System.out.println("最终发现fd总数：" + fds.size());
        System.out.println("最终发现od总数：" + ods.size());
    }
}
