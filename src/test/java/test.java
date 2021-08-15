import dataStructures.DataFrame;
import dataStructures.EquivalenceClass;
import dataStructures.PartialDataFrame;
import dataStructures.fd.Array.FDTreeArray;
import dataStructures.fd.FDCandidate;
import dataStructures.fd.FDTreeNodeEquivalenceClasses;
import dataStructures.od.ODCandidate;
import dataStructures.od.ODTree;
import discoverer.fd.Array.BFSFDDiscovererArray;
import discoverer.fd.Array.DiscoverResult;
import discoverer.fd.BFSFDDiscoverer;
import discoverer.fd.PLI.BFSFDDiscovererPLI;
import discoverer.fd.BFSFDDiscovererOld;
import discoverer.od.BFSODDiscovererFull;
import org.junit.Test;
import sampler.OneLevelCheckingSampler;
import sampler.RandomSampler;
import util.Timer;
import validator.fd.FDBruteForceFullValidator;
import validator.fd.FDIncrementalValidator;
import validator.od.ODBruteForceFullValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class test {
    @Test
    public void testBFSODDis() {
        DataFrame data = DataFrame.fromCsv("Data/fd 15 1000.csv");
        util.Timer timer = new Timer();
        ODTree discover = new BFSODDiscovererFull().discover(data);
        System.out.println(timer.getTimeUsed() / 1000.0 + "s");
        List<ODCandidate> ods = discover.getAllOdsOrderByBFS();
        System.out.println(ods);
        System.out.println("发现od数量："+ods.size());
    }

//    @Test
//    public static void testBFSTotalDiscoverer() {
//
//        DataFrame data = DataFrame.fromCsv("Data/FLI 200.csv");
//
//        System.out.println("refinement方法整体发现：");
//        System.gc();
//        util.Timer timer = new Timer();
//        BFSTotalDiscoverer discoverer = new BFSTotalDiscoverer();
//        discoverer.discoverCandidate(data);
//        System.out.println(timer.getTimeUsed() / 1000.0 + "s");
//
////        for(FDCandidate fd: fdCandidates){
////            System.out.println(fd);
////        }
//        System.out.println(fdCandidates);
//        System.out.println("发现fd数量："+fdCandidates.size());
//
//        List<ODCandidate> ods = odTree.getAllOdsOrderByBFS();
////        for(ODCandidate od:ods){
////            System.out.println(od);
////        }
//        System.out.println(ods);
//        System.out.println("发现od数量："+ods.size());
//
//        System.out.println("fdTime:" + fdTime/1000.0 + "s");
//
//        System.out.println("fdRefinementTime:" +  FDTreeNodeEquivalenceClasses.refinementTime/1000.0+"s");
//        System.out.println("fdMeregeTime:" +  FDTreeNodeEquivalenceClasses.mergeTime/1000.0+"s");
//        System.out.println("fdCloneTime:" +  FDTreeNodeEquivalenceClasses.cloneTime/1000.0+"s");
//        System.out.println("fdMinimalCheckTime:" +  ODMinimalCheckerBruteForce.fdMinimalCheckTime/1000.0+"s");
//
//        System.out.println("odTime:" + odTime/1000.0 + "s");
//        System.out.println("checkTime:" + ODTreeNodeEquivalenceClasses.checkTime / 1000.0 + "s");
//        System.out.println("cloneTime:" + ODTreeNodeEquivalenceClasses.cloneTime / 1000.0 + "s");
//        System.out.println("mergeTime:" + ODTreeNodeEquivalenceClasses.mergeTime / 1000.0 + "s");
//        System.out.println("odMinimalCheckTime:" +  ODMinimalCheckerBruteForce.odMinimalCheckTime/1000.0+"s");
//    }
    @Test
    public void testRandomSampler(){
        DataFrame data = DataFrame.fromCsv("Data/FLI 10K.csv");
        RandomSampler sampler = new RandomSampler();
        PartialDataFrame result = sampler.sample(data);
        int row = result.getRowCount();
        int col = result.getColumnCount();
        System.out.println("抽样行数： " + row);
        System.out.println("抽样数据集：");
        for(List<Integer> s : result.getData()){
            System.out.println(s);
        }
        System.out.println("---");
        System.out.println(result.getRealIndexes());
        System.out.println(result.getRowIndexes());
    }

    @Test
    public void testOneLevelCheckingSampler(){
        DataFrame data = DataFrame.fromCsv("Data/FLI 1000.csv");
        OneLevelCheckingSampler sampler = new OneLevelCheckingSampler();
        PartialDataFrame result = sampler.sample(data);
        int row = result.getRowCount();
        System.out.println("抽样行数： " + row);
        System.out.println("抽样数据集：");
        for(List<Integer> s : result.getData()){
            System.out.println(s);
        }
        System.out.println("---");
        System.out.println(result.getRealIndexes());
    }

    @Test
    public void testTwoTime(){
        DataFrame data = DataFrame.fromCsv("Data/fd 15 1000.csv");
        OneLevelCheckingSampler sampler = new OneLevelCheckingSampler();
//        RandomSampler sampler = new RandomSampler();
        PartialDataFrame result = sampler.sample(data);
        System.out.println("抽样大小：" + result.getRowCount());
        util.Timer timer = new Timer();
        ODTree discover = new BFSODDiscovererFull().discover(result);
        System.out.println(timer.getTimeUsed() / 1000.0 + "s");
        ODBruteForceFullValidator validator = new ODBruteForceFullValidator();
        Set<Integer> newVios = validator.validate(discover,data);
        System.out.println(newVios);
        List<ODCandidate> ods = discover.getAllOdsOrderByBFS();
        System.out.println(ods);
        System.out.println("发现od数量："+ods.size());
    }

    @Test
    public void testFDRefinement(){
        DataFrame data = DataFrame.fromCsv("Data/Atom 10 1000.csv");
        Timer time = new Timer();
        BFSFDDiscoverer discoverer = new BFSFDDiscoverer();
        List<FDCandidate> fds = discoverer.discoverCandidateRefinement(data);
        System.out.println(time.getTimeUsed() / 1000.0 + "s");
        System.out.println(fds);
        System.out.println(fds.size());
    }

    @Test
    public void testFDRefinementArray(){
        DataFrame data = DataFrame.fromCsv("Data/Atom 10 1000.csv");
        Timer time = new Timer();
        BFSFDDiscovererArray discoverer = new BFSFDDiscovererArray();
        List<FDCandidate> fds = discoverer.discoverCandidateRefinementMorePrune(data).fdCandidates;
        System.out.println(time.getTimeUsed() / 1000.0 + "s");
        System.out.println(fds);
        System.out.println(fds.size());
    }

    @Test
    public void testFDCheck(){
        DataFrame data = DataFrame.fromCsv("Data/fd 15 1000.csv");
        Timer timer = new Timer();
        List<FDCandidate> result = new BFSFDDiscovererOld().discoverCandidate(data);
        System.out.println(timer.getTimeUsedAndReset() / 1000.0 + "s");
        System.out.println(result);
        System.out.println(result.size());
    }

    @Test
    public void testFDPLI(){
        DataFrame data = DataFrame.fromCsv("Data/fd 15 1000.csv");
        Timer time = new Timer();
        List<FDCandidate> fds = new BFSFDDiscovererPLI().discoverCandidateRefinement(data);
        System.out.println(time.getTimeUsed()/ 1000.0 + "s");
        System.out.println(fds);
        System.out.println(fds.size());
    }

    @Test
    public void getRowToRight(){
        DataFrame data = DataFrame.fromCsv("Data/test1.csv");
        EquivalenceClass right = new EquivalenceClass();
        right.fdMerge(3,data);
        System.out.println(right);
        FDTreeNodeEquivalenceClasses fdec = new FDTreeNodeEquivalenceClasses();
        fdec.right = right;
        int[] arry = fdec.getRowToRightClusterIndex();
        for(Integer s:arry){
            System.out.print(s + " ");
        }
    }

    @Test
    public void testFDEc(){
        DataFrame data = DataFrame.fromCsv("Data/test1.csv");
        EquivalenceClass left = new EquivalenceClass();
        left.fdMerge(0,data);
        System.out.println(left.toString());
//        left.fdMerge(2,data);
//        System.out.println(left.toString());
        FDTreeNodeEquivalenceClasses fdec = new FDTreeNodeEquivalenceClasses();
        fdec.left = left;
        List<Integer> result = fdec.checkFDRefinement(2,data).violationRows;
        System.out.println(result);
    }

    @Test
    public void  testfdTreeNode(){
        DataFrame data = DataFrame.fromCsv("Data/test.csv");
        BFSFDDiscovererArray discoverer = new BFSFDDiscovererArray();
        DiscoverResult discoverResult = discoverer.discoverCandidateRefinementMorePrune(data);
        List<FDCandidate> fds = discoverResult.fdCandidates;
        System.out.println(fds);
        for(FDCandidate fd : fds){
            System.out.println(fd.left);
            System.out.println(fd.right);
            System.out.println(fd.fdTreeNode);
        }
        System.out.println(fds.size());

    }

    @Test
    public void  testFDValidatorPro(){
        DataFrame data = DataFrame.fromCsv("Data/FLI 10K.csv");
        Timer timer = new Timer();
        OneLevelCheckingSampler sampler = new OneLevelCheckingSampler();
        PartialDataFrame result = sampler.sample(data);
        System.out.println("抽样数据集大小：" + result.getRowsCount());
        List<FDCandidate> fds;
        FDIncrementalValidator validator = new FDIncrementalValidator();
        BFSFDDiscovererArray discoverer = new BFSFDDiscovererArray();
        FDTreeArray reference = null;
        int i = 0;
        while (true){
            i++;
            System.out.println("第" + i + "轮：");
            if(i == 1){
                DiscoverResult discoverResult = discoverer.discoverCandidateRefinementMorePrune(result);
                fds = discoverResult.fdCandidates;
                reference = discoverResult.FDTree;
            }else {
                DiscoverResult discoverResult = discoverer.discoverCandidateRefinementMorePruneAfterValidateBrief(result, reference);
                fds = discoverResult.fdCandidates;
                reference = discoverResult.FDTree;
            }
          
            System.out.println(fds);
            System.out.println("发现fd数量： " + fds.size());

            Set<Integer> rows= validator.validate(fds,data);
            if(rows.isEmpty())
                break;
            System.out.println("违约元组数:" + rows.size());
            result.addRows(rows);
            System.out.println("新数据集大小：" + result.getRowCount());
        }
        System.out.println("总时间： " + timer.getTimeUsed() /1000.0 + "s");
    }

    @Test
    public void  testFDValidator(){
        DataFrame data = DataFrame.fromCsv("Data/FLI 100K.csv");
        Timer timer = new Timer();
        OneLevelCheckingSampler sampler = new OneLevelCheckingSampler();
        PartialDataFrame result = sampler.sample(data);
        System.out.println("抽样数据集大小：" + result.getRowsCount());
        List<FDCandidate> fds;
        FDBruteForceFullValidator validator = new FDBruteForceFullValidator();
//        FDIncrementalValidator validator = new FDIncrementalValidator();
        BFSFDDiscovererArray discoverer = new BFSFDDiscovererArray();
        FDTreeArray reference = null;
        int i = 0;
        while (true){
            i++;
            System.out.println("第" + i + "轮：");
            fds = discoverer.discoverCandidateRefinementMorePrune(result).fdCandidates;
            System.out.println(fds);
            System.out.println("发现fd数量： " + fds.size());

            Set<Integer> rows= validator.validate(fds,data);
            if(rows.isEmpty())
                break;
            System.out.println("违约元组数:" + rows.size());
            result.addRows(rows);
            System.out.println("新数据集大小：" + result.getRowCount());
        }
        System.out.println("总时间： " + timer.getTimeUsed() /1000.0 + "s");
    }

    @Test
    public void testTotalSample(){
        DataFrame data = DataFrame.fromCsv("Data/FLI 100K.csv");
        Timer timer = new Timer();
        OneLevelCheckingSampler sampler = new OneLevelCheckingSampler();
        PartialDataFrame result = sampler.sample(data);
        System.out.println("抽样数据集大小：" + result.getRowsCount());
        List<FDCandidate> fds;
        List<ODCandidate> ods;
        FDIncrementalValidator validator = new FDIncrementalValidator();
        BFSFDDiscovererArray discoverer = new BFSFDDiscovererArray();
        FDTreeArray reference = null;
        int i = 0;
        while (true){
            i++;
            System.out.println("第" + i + "轮：");
            if(i == 1){
                DiscoverResult discoverResult = discoverer.discoverCandidateRefinementMorePrune(result);
                fds = discoverResult.fdCandidates;
                reference = discoverResult.FDTree;
            }else {
                DiscoverResult discoverResult = discoverer.discoverCandidateRefinementMorePruneAfterValidateBrief(result, reference);
                fds = discoverResult.fdCandidates;
                reference = discoverResult.FDTree;
            }

            System.out.println(fds);
            System.out.println("发现fd数量： " + fds.size());
            //od 发现
            ODTree discover = new BFSODDiscovererFull().discoverFD(result, fds);
            ods =  discover.getAllOdsOrderByDFS();
            System.out.println(ods);
            System.out.println("发现od数量： " + ods.size());

            //fd验证
            Set<Integer> fdRows= validator.validate(fds,data);
            System.out.println("fd违约元组数:" + fdRows.size());
            result.addRows(fdRows);
            System.out.println("增加fd违约后新数据集大小：" + result.getRowCount());

            //od验证
            ODBruteForceFullValidator ODValidator = new ODBruteForceFullValidator();
            Set<Integer> ODRows = ODValidator.validate(discover,data);
            if(ODRows.isEmpty() && fdRows.isEmpty())
                break;
            System.out.println("od违约元组数:" + ODRows.size());
            result.addRows(ODRows);
            System.out.println("增加od违约后新数据集大小：" + result.getRowCount());

        }
        System.out.println("总时间： " + timer.getTimeUsed() /1000.0 + "s");
    }


    @Test
    public void  testODValidator(){
        DataFrame data = DataFrame.fromCsv("Data/FLI 10K.csv");
        OneLevelCheckingSampler sampler = new OneLevelCheckingSampler();
        PartialDataFrame result = sampler.sample(data);
        System.out.println("抽样数据集大小：" + result.getRowsCount());
        List<ODCandidate> ods;
        ODTree tree = new BFSODDiscovererFull().discover(data);
        while (true){
            ods =  tree.getAllOdsOrderByDFS();
            System.out.println(ods);
            System.out.println("发现od数量： " + ods.size());
            ODBruteForceFullValidator validator = new ODBruteForceFullValidator();
            Set<Integer> rows = validator.validate(tree,data);
            if(rows.isEmpty())
                break;
            System.out.println("违约元组" + rows);
            result.addRows(rows);
            System.out.println("新数据集大小：" + result.getRowCount());
        }
    }

    @Test
    public void  testOD(){
        DataFrame data = DataFrame.fromCsv("Data/fd 15 1000.csv");
        OneLevelCheckingSampler sampler = new OneLevelCheckingSampler();
        PartialDataFrame result = sampler.sample(data);
        System.out.println("result: " + result.getRowsCount());
        Timer timer = new Timer();
        ODTree discover = new BFSODDiscovererFull().discover(result);
        List<ODCandidate> ods = discover.getAllOdsOrderByBFS();
        System.out.println(ods.size());
        System.out.println(ods);
        ODBruteForceFullValidator validator = new ODBruteForceFullValidator();
        System.out.println("违约元组" + validator.validate(discover,data));
        System.out.println(timer.getTimeUsedAndReset() /1000.0 + "s");
        System.out.println(ods.size());
        System.out.println(ods);
    }

    @Test
    public void  testVioHelper(){
        List<Integer> leftBegin = new ArrayList<>();
        List<Integer> newLeftBegin = new ArrayList<>();
      //leftBegin [0,3,7,10]
        leftBegin.add(0);
        leftBegin.add(3);
        leftBegin.add(7);
        leftBegin.add(15);
      //newLeftBegin [0,2,3,4]
        newLeftBegin.add(0);
        newLeftBegin.add(2);
        newLeftBegin.add(3);
        newLeftBegin.add(4);
        newLeftBegin.add(7);
        newLeftBegin.add(10);
        newLeftBegin.add(12);
        newLeftBegin.add(14);
        newLeftBegin.add(15);

//         List<Integer> result = new FDTreeNodeEquivalenceClasses().getVioHelper(leftBegin,newLeftBegin);
         List<Set<Integer>> result = new FDTreeNodeEquivalenceClasses().getVioHelperMoreCluster(leftBegin,newLeftBegin);
        System.out.println(result);
    }

    @Test
    public void testMorePlune(){
        DataFrame data = DataFrame.fromCsv("Data/fd 30.csv");
        Timer timer = new Timer();
        BFSFDDiscovererArray discoverer = new BFSFDDiscovererArray();
        List<FDCandidate> fds = discoverer.discoverCandidateRefinementMorePrune(data).fdCandidates;
        System.out.println(timer.getTimeUsedAndReset() / 1000.0 + "s");
        System.out.println(fds);
        System.out.println(fds.size());
    }

}



