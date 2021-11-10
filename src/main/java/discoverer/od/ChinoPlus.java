package discoverer.od;


import dataStructures.DataFrame;
import dataStructures.PartialDataFrame;
import dataStructures.fd.FDCandidate;
import dataStructures.od.ODCandidate;
import dataStructures.od.ODTree;
import dataStructures.od.ODTreeNodeEquivalenceClasses;
import discoverer.od.ODDiscoverer;
import minimal.ODMinimalCheckTree;
import minimal.ODMinimalChecker;
import sampler.OneLevelCheckingSampler;
import sampler.Sampler;
import util.Timer;
import validator.od.ODBruteForceFullValidator;
import validator.od.ODPrefixBasedIncrementalValidator;
import validator.od.ODValidator;

import java.util.List;
import java.util.Set;

public class ChinoPlus extends ODDiscoverer {

    protected Sampler sampler;
    protected ODValidator validator;
    protected boolean printDebugInfo;

    private long totalDiscoverTime=0;
    private long totalValidateTime=0;
    private long totalProductTime=0;
    private long totalCloneTime=0;
    private long totalCheckTime=0;
    private long totalMinimalTime=0;


    public ChinoPlus(Sampler sampler, ODValidator validator) {
        this(sampler,validator,false);
    }
    public ChinoPlus(Sampler sampler, ODValidator validator, boolean printDebugInfo) {
        this.sampler = sampler;
        this.validator = validator;
        this.printDebugInfo = printDebugInfo;
    }


    public void setSamplerRandomSeed(long seed){
        sampler.setRandomSeed(seed);
    }

    @Override
    public ODTree discover(DataFrame data, ODTree reference) {
        System.out.println("数据集大小"+data.getRowCount()+"行"+data.getColumnCount()+"列");
        Timer timer=new Timer();
        ODTree odTree=new ODTree(data.getColumnCount());
        Timer sampleTimer=new Timer();

        PartialDataFrame sampledData=sampler.sample(data);
        System.out.println("抽样用时"+sampleTimer.getTimeUsedAndReset()/1000.0+"s");
        System.out.println("抽样数量"+sampledData.getRowCount());
        int round=0;
        while (true){
            round++;
            System.out.println("------");
            System.out.println("第"+round+"轮开始");
            Timer roundTimer=new Timer();
            int subRound=0;
            BFSODDiscovererForIteration discoverer=new BFSODDiscovererForIteration();
            while (true){
                subRound++;
                System.out.println("\n第"+subRound+"次迭代");
                Timer subtimer=new Timer();
                odTree=discoverer.discover(sampledData,odTree);
                totalDiscoverTime+=subtimer.getTimeUsed();
                System.out.println("发现用时"+subtimer.getTimeUsedAndReset()/1000.0+"s");
                System.out.println("OD数量"+odTree.getAllOdsOrderByBFS().size());
                Set<Integer> violateRowIndexes=validator.validate(odTree,data);
                totalValidateTime+=subtimer.getTimeUsed();
                System.out.println("检测用时"+subtimer.getTimeUsedAndReset()/1000.0+"s");
                System.out.println("剩余OD数量"+odTree.getAllOdsOrderByBFS().size());
                dealPartTime();
                if(violateRowIndexes.size()==0){
                    if(discoverer.isComplete()){
                        System.out.println("------");
                        System.out.println("第"+round+"轮结束");
                        System.out.println("本轮用时"+roundTimer.getTimeUsed()/1000+"s");
                        System.out.println("新数据集大小"+sampledData.getRowCount());
                        System.out.println("------");
                        System.out.println("-----------------------------------------------------");
                        System.out.println("最终统计");
                        System.out.println("用时"+timer.getTimeUsed()/1000.0+"s");
                        System.out.println("OD数量"+odTree.getAllOdsOrderByBFS().size());
                        System.out.println("数据集大小"+sampledData.getRowCount());
                        System.out.println("discover时间:"+totalDiscoverTime/1000.0+"s");
                        System.out.println("validate时间:"+totalValidateTime/1000.0+"s");

                        System.out.println("check时间:"+totalCheckTime/1000.0+"s");
                        System.out.println("minimal检查时间:"+totalMinimalTime/1000.0+"s");
                        System.out.println("product时间:"+totalProductTime/1000.0+"s");
                        System.out.println("clone时间:"+totalCloneTime/1000.0+"s");
                        System.out.println("-----------------------------------------------------");
                        return odTree;
                    }else {
                        System.out.println("violateRow size is 0 && discoverer is not complete");
                        if (odTree.getAllOdsOrderByDFS().size()>100000){
                            for (ODCandidate od : odTree.getAllOdsOrderByBFS()) {
                                System.out.println(od);
                            }
                            return odTree;
                        }
                    }
                }
                else {
                    sampledData.addRows(violateRowIndexes);
                    System.out.println("------");
                    System.out.println("第"+round+"轮结束");
                    System.out.println("本轮用时"+roundTimer.getTimeUsed()/1000.0+"s");
                    System.out.println("新数据集大小"+sampledData.getRowCount());
                    System.out.println("------");
                    break;
                }
            }
        }
    }

    void dealPartTime(){
        System.out.println("check时间 "+ ODTreeNodeEquivalenceClasses.checkTime/1000.0+"s");
        System.out.println("minimal检查时间 "+ ODMinimalCheckTree.odMinimalCheckTime/1000.0+"s");
        System.out.println("product时间 "+ODTreeNodeEquivalenceClasses.mergeTime/1000.0+"s");
        System.out.println("clone时间 "+ODTreeNodeEquivalenceClasses.cloneTime/1000.0+"s");
        totalCheckTime+=ODTreeNodeEquivalenceClasses.checkTime;
        totalMinimalTime+= ODMinimalCheckTree.odMinimalCheckTime;
        totalProductTime+=ODTreeNodeEquivalenceClasses.mergeTime;
        totalCloneTime+=ODTreeNodeEquivalenceClasses.cloneTime;
        ODTreeNodeEquivalenceClasses.checkTime=0;
        ODTreeNodeEquivalenceClasses.cloneTime=0;
        ODTreeNodeEquivalenceClasses.mergeTime=0;
        ODMinimalChecker.odMinimalCheckTime=0;
    }

    @Override
    public ODTree discoverFD(DataFrame data, List<FDCandidate> fdCandidates) {
        return null;
    }


    public static void main(String[] args) {
        DataFrame dataFrame=DataFrame.fromCsv("Data/flights 50k.csv");
        Timer timer=new Timer();
        ODTree discover = new ChinoPlus(
                new OneLevelCheckingSampler(),
                new ODPrefixBasedIncrementalValidator(),
                true).discover(dataFrame, null);
        timer.outTimeAndReset();
        List<ODCandidate> ods = discover.getAllOdsOrderByBFS();
        System.out.println("od数量:" + ods.size());
    }
}
