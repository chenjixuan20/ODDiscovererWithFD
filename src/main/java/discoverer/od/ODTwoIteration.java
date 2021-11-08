package discoverer.od;

import dataStructures.DataFrame;
import dataStructures.PartialDataFrame;
import dataStructures.od.ODTree;
import discoverer.od.BFSODDiscovererFull;
import sampler.OneLevelCheckingSampler;
import sampler.Sampler;
import util.Timer;
import validator.od.ODBruteForceFullValidator;
import validator.od.ODValidator;

import java.util.Set;

public class ODTwoIteration {
    protected Sampler sampler;
    protected ODValidator validator;
    protected boolean printDebugInfo;

    public ODTwoIteration() {
        this(false);
    }

    public ODTwoIteration(boolean printDebugInfo) {
        this(new OneLevelCheckingSampler(),new ODBruteForceFullValidator(),printDebugInfo);
    }

    public ODTwoIteration(Sampler sampler, ODValidator validator, boolean printDebugInfo) {
        this.sampler = sampler;
        this.validator = validator;
        this.printDebugInfo = printDebugInfo;
    }

    public ODTree discover(DataFrame data, ODTree reference) {
        Timer fullTimer=new Timer();
        PartialDataFrame sampledData=sampler.sample(data);
        for(int i = 0; i < sampledData.getRowCount(); i++){
            for (int j = 0; j < sampledData.getColumnCount(); j++){
                System.out.print(sampledData.getCell(i,j) + " ");
            }
            System.out.print("\n");
        }
        System.out.println("抽样大小： "+sampledData.getRowCount());
        Timer timer=new Timer();

//        ODTree tree=new BFSODDiscovererForIteration().discover(sampledData);
        ODTree tree= null;

        System.out.println("第一轮发现用时："+timer.getTimeUsedAndReset()/1000.0+"s");
        System.out.println("od数量:"+tree.getAllOdsOrderByDFS().size());
        Set<Integer> violationRows=new ODBruteForceFullValidator().validate(tree,data);
        System.out.println("验证用时:"+timer.getTimeUsedAndReset()/1000.0+"s");
        System.out.println("冲突集大小:"+violationRows.size());
        sampledData.addRows(violationRows);
        tree=new BFSODDiscovererFull().discover(data);
        System.out.println("第2轮发现用时："+timer.getTimeUsedAndReset()/1000.0+"s");
        System.out.println("算法结束");
        System.out.println("od个数:"+tree.getAllOdsOrderByBFS().size());
        System.out.println("总用时:"+fullTimer.getTimeUsed()/1000.0+"s");
        return tree;
    }


}
