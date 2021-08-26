import dataStructures.DataFrame;
import dataStructures.EquivalenceClass;
import dataStructures.PartialDataFrame;
import dataStructures.fd.Array.FDTreeArray;
import dataStructures.fd.FDCandidate;
import dataStructures.fd.FDTreeNodeEquivalenceClasses;
import dataStructures.fd.FDValidationResult;
import dataStructures.od.ODCandidate;
import dataStructures.od.ODTree;
import discoverer.BFSTotalDiscovererArray;
import discoverer.fd.Array.BFSFDDiscovererArray;
import discoverer.fd.Array.DiscoverResult;
import discoverer.fd.BFSFDDiscovererRefinement;
import discoverer.fd.PLI.BFSFDDiscovererPLI;
import discoverer.fd.BFSFDDiscovererOld;
import discoverer.od.BFSODDiscovererFull;
import org.junit.Test;
import sampler.OneLevelCheckingSampler;
import sampler.RandomSampler;
import util.Timer;
import validator.fd.FDBruteForceFullValidator;
import validator.fd.FDTreeIncrementalValidator;
import validator.od.ODBruteForceFullValidator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import static dataStructures.fd.PLI.FDEquivalenceClass.changeToPLI;

public class test {
    @Test
    public void testBFSODDis() {
        DataFrame data = DataFrame.fromCsv("Data/FLI 10K.csv");
        util.Timer timer = new Timer();
        ODTree discover = new BFSODDiscovererFull().discover(data);
        System.out.println(timer.getTimeUsed() / 1000.0 + "s");
        List<ODCandidate> ods = discover.getAllOdsOrderByBFS();
        System.out.println(ods);
        System.out.println("发现od数量："+ods.size());
    }

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
    public void testFDRefinement(){
        DataFrame data = DataFrame.fromCsv("Data/Atom 10 1000.csv");
        Timer time = new Timer();
        BFSFDDiscovererRefinement discoverer = new BFSFDDiscovererRefinement();
        List<FDCandidate> fds = discoverer.discoverCandidateRefinement(data);
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
            fds = discoverer.discoverFirstTimes(result).fdCandidates;
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
        DataFrame data = DataFrame.fromCsv("Data/plista-int.csv");
        Timer timer = new Timer();
        OneLevelCheckingSampler sampler = new OneLevelCheckingSampler();
        PartialDataFrame result = sampler.sample(data);
        System.out.println("抽样数据集大小：" + result.getRowsCount());
        List<FDCandidate> fds;
        List<ODCandidate> ods;
        FDTreeIncrementalValidator validator = new FDTreeIncrementalValidator();
        BFSFDDiscovererArray discoverer = new BFSFDDiscovererArray();
        FDTreeArray reference = null;
        int i = 0;
        while (true){
            i++;
            System.out.println("第" + i + "轮：");
            if(i == 1){
                DiscoverResult discoverResult = discoverer.discoverFirstTimes(result);
                fds = discoverResult.fdCandidates;
                reference = discoverResult.FDTree;
            }else {
                DiscoverResult discoverResult = discoverer.discoverAfterVaildate(result, reference);
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
    public void testTotalSamplePro(){
        DataFrame data = DataFrame.fromCsv("Data/echocardiogram-int.csv");
        Timer timer = new Timer();
        OneLevelCheckingSampler sampler = new OneLevelCheckingSampler();
        PartialDataFrame result = sampler.sample(data);
        System.out.println("抽样数据集大小：" + result.getRowsCount());
        List<FDCandidate> fds;
        List<ODCandidate> ods;
        FDTreeIncrementalValidator validator = new FDTreeIncrementalValidator();
        BFSTotalDiscovererArray discoverer = new BFSTotalDiscovererArray();
        FDTreeArray reference = null;
        int i = 0;
        while (true){
            i++;
            System.out.println("第" + i + "轮：");

            discoverer.discoverFirstTimes(result, null);
            fds = discoverer.fdCandidates;
            reference = discoverer.fdTreeArray;

            System.out.println(fds);
            System.out.println("发现fd数量： " + fds.size());

            ods =  discoverer.odTree.getAllOdsOrderByDFS();
            System.out.println(ods);
            System.out.println("发现od数量： " + ods.size());

            //fd验证
            Set<Integer> fdRows= validator.validate(fds,data);
            System.out.println("fd违约元组数:" + fdRows.size());
            result.addRows(fdRows);
            System.out.println("增加fd违约后新数据集大小：" + result.getRowCount());

            //od验证
            ODBruteForceFullValidator ODValidator = new ODBruteForceFullValidator();
            Set<Integer> ODRows = ODValidator.validate(discoverer.odTree,data);
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
        DataFrame data = DataFrame.fromCsv("Data/plista-int.csv");
        Timer timer = new Timer();
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
        System.out.println("总时间： " + timer.getTimeUsed() /1000.0 + "s");
    }

    @Test
    public void  testOD(){
        DataFrame data = DataFrame.fromCsv("Data/ncv 1000 19-int.csv");
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
        DataFrame data = DataFrame.fromCsv("Data/echocardiogram-int.csv");
        Timer timer = new Timer();
        BFSFDDiscovererArray discoverer = new BFSFDDiscovererArray();
        List<FDCandidate> fds = discoverer.discoverFirstTimes(data).fdCandidates;
        System.out.println(timer.getTimeUsedAndReset() / 1000.0 + "s");
        System.out.println(fds);
        System.out.println(fds.size());
        for(int i = 0; i < 20; i++){
            FDCandidate fd = fds.get(i);
            System.out.println("fd: "+fd);
            System.out.println("left form node: "+getLeftFormNode(fd.fdTreeNode));
            System.out.println("node attribute: " + fd.fdTreeNode);
            System.out.println("node parent attribute: "+fd.fdTreeNode.parent);
        }
    }

    public List<Integer> getLeftFormNode(FDTreeArray.FDTreeNode node){
        List<Integer> left = new ArrayList<>();
        FDTreeArray.FDTreeNode index = node;
        while (index.parent != null){
            left.add(index.attribute);
            index = index.parent;
        }
        Collections.reverse(left);
        return left;
    }

    @Test
    public void testTwoTime() throws IOException {
        File f=new File("out.txt");
        f.createNewFile();
        FileOutputStream fileOutputStream = new FileOutputStream(f);
        PrintStream printStream = new PrintStream(fileOutputStream);
        System.setOut(printStream);
        System.out.println("默认输出到控制台的这一句，输出到了文件 out.txt");
        DataFrame data = DataFrame.fromCsv("Data/echocardiogram-int.csv");
        Timer timer = new Timer();
        OneLevelCheckingSampler sampler = new OneLevelCheckingSampler();
        PartialDataFrame result = sampler.sample(data);
        System.out.println("抽样数据集大小：" + result.getRowsCount());
        System.out.println("抽样数据集原始索引： " + result.getRealIndexes());
        List<FDCandidate> fds;
        FDTreeIncrementalValidator validator = new FDTreeIncrementalValidator();
        BFSFDDiscovererArray discoverer = new BFSFDDiscovererArray();
        FDTreeArray reference = null;
        DiscoverResult discoverResult = discoverer.discoverFirstTimes(result);
        fds = discoverResult.fdCandidates;
        reference = discoverResult.FDTree;
        System.out.println(fds);
        System.out.println("发现fd数量： " + fds.size());
        Set<Integer> rows= validator.validate(fds,data);
        System.out.println("违约元组数:" + rows.size());
        System.out.println("违约元组: "+ rows);
        result.addRows(rows);
        System.out.println("新数据集大小：" + result.getRowCount());
        System.out.println("新数据集原始索引： " + result.getRealIndexes());
        System.out.println("新数据集原始索引是否保护违约元组： " + result.getRealIndexes().containsAll(rows));
        System.out.println("-------------------第二轮----------------------");
        System.out.println("-------------------第二轮----------------------");
        System.out.println("-------------------第二轮----------------------");
        System.out.println("-------------------第二轮----------------------");
        System.out.println("-------------------第二轮----------------------");
        System.out.println("-------------------第二轮----------------------");
        System.out.println("-------------------第二轮----------------------");
        System.out.println("-------------------第二轮----------------------");
        System.out.println("-------------------第二轮----------------------");
        System.out.println("-------------------第二轮----------------------");
        discoverResult = discoverer.discoverAfterVaildate(result, reference);
        fds = discoverResult.fdCandidates;
        reference = discoverResult.FDTree;
        System.out.println(fds);
        System.out.println("发现fd数量： " + fds.size());
        rows= validator.validate(fds,data);
        System.out.println("违约元组数:" + rows.size());
        result.addRows(rows);
        System.out.println("新数据集大小：" + result.getRowCount());
        System.out.println("新数据集原始索引： " + result.getRealIndexes());
    }

    @Test
    public void  testFDValidatorPro() throws IOException {
//        File f=new File("out.txt");
//        f.createNewFile();
//        FileOutputStream fileOutputStream = new FileOutputStream(f);
//        PrintStream printStream = new PrintStream(fileOutputStream);
//        System.setOut(printStream);
//        System.out.println("默认输出到控制台的这一句，输出到了文件 out.txt");
        DataFrame data = DataFrame.fromCsv("Data/ncv 1000 19-int.csv");
        Timer timer = new Timer();
        OneLevelCheckingSampler sampler = new OneLevelCheckingSampler();
        PartialDataFrame sampleData = sampler.sample(data);
        System.out.println("抽样数据集大小：" + sampleData.getRowsCount());
        List<FDCandidate> fds;
        FDTreeIncrementalValidator validator = new FDTreeIncrementalValidator();
        BFSFDDiscovererArray discoverer = new BFSFDDiscovererArray();
        FDTreeArray reference = null;
        int i = 0;
        while (true){
            i++;
            System.out.println("第" + i + "轮：");
            if(i == 1){
                DiscoverResult discoverResult = discoverer.discoverFirstTimes(sampleData);
                fds = discoverResult.fdCandidates;
                reference = discoverResult.FDTree;
            }else {
                DiscoverResult discoverResult = discoverer.discoverAfterVaildate(sampleData, reference);
                fds = discoverResult.fdCandidates;
                reference = discoverResult.FDTree;
            }
            System.out.println(fds);
            System.out.println("发现fd数量： " + fds.size());
            Set<Integer> rows= validator.validate(fds,data);
            if(rows.isEmpty()){
                System.out.println("违约元组数:" + rows.size());
                break;
            }
            System.out.println("违约元组数:" + rows.size());
            sampleData.addRows(rows);
            System.out.println("新数据集大小：" + sampleData.getRowCount());
        }
        System.out.println("最终发现fd数量： " + fds.size());
        System.out.println("总时间： " + timer.getTimeUsed() /1000.0 + "s");
    }

    @Test
    public void testt(){
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
            if(i == 1){
                discoverer.discoverFirstTimes(sampleData, null);
                odReference = discoverer.odTree;
                fdReference = discoverer.fdTreeArray;
                fds = discoverer.fdCandidates;
                ods = odReference.getAllOdsOrderByBFS();
            }else {
                discoverer.discoverAfterValidate(sampleData, odReference, fdReference);
                odReference = discoverer.odTree;
                fdReference = discoverer.fdTreeArray;
                fds = discoverer.fdCandidates;
                ods = odReference.getAllOdsOrderByBFS();
            }

            System.out.println(fds);
            System.out.println("发现fd数量： " + fds.size());
            System.out.println(ods);
            System.out.println("发现od数量： " + ods.size());

            Set<Integer> fdRows= validator.validate(fds,data);
            System.out.println("fd违约元组数:" + fdRows.size());
            sampleData.addRows(fdRows);
            System.out.println("增加fd违约后新数据集大小：" + sampleData.getRowCount());

            Set<Integer> ODRows = ODValidator.validate(odReference,data);
            if(ODRows.isEmpty() && fdRows.isEmpty()){
                System.out.println("fd、od违约元组总数:" + 0);
                break;
            }
            System.out.println("od违约元组数:" + ODRows.size());
            sampleData.addRows(ODRows);
            System.out.println("增加od违约后新数据集大小：" + sampleData.getRowCount());
        }
        System.out.println("总时间： " + timer.getTimeUsed() /1000.0 + "s");
        System.out.println("最终发现fd总数：" + fds.size());
        System.out.println("最终发现od总数：" + ods.size());
    }


}



