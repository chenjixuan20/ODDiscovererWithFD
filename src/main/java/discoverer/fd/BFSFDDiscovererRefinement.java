package discoverer.fd;

import dataStructures.*;

import java.util.*;

import dataStructures.fd.*;
import util.Timer;


public class BFSFDDiscovererRefinement {
    public  long keyPruneTimes = 0;

    /**
     * 使用refinement方法 check fd
     * @param data
     * @return
     */
    public List<FDCandidate> discoverCandidateRefinement(DataFrame data){
        List<FDCandidate> fdCandidates = new ArrayList<>();
        FDTree result = new FDTree(data.getColumnCount());
        FDTree.FDTreeNode node = result.getRoot();
        int attributeNum = data.getColumnCount();
        Queue<FDDiscoverNodeSavingInfo> queue = new LinkedList<>();
        FDTreeNodeEquivalenceClasses fde = new FDTreeNodeEquivalenceClasses();
        queue.offer(new FDDiscoverNodeSavingInfo(node, fde, new ArrayList<>()));

        while (!queue.isEmpty()){
            FDDiscoverNodeSavingInfo info = queue.poll();
            FDTree.FDTreeNode parent = info.nodeInResultTree;
            //设attribute set中的attribute升序排列
            //生成子节点，当parent的attribute一定时，其生成的子节点的attribute从比他大开始（避免重复12，21的情况），比如说当属性为2时，则只生成3，4，5
            for(int i = parent.attribute + 1; i < attributeNum; i++){
//               int index = 0;
                FDTree.FDTreeNode child = result.new FDTreeNode(parent, i, attributeNum);
                List<Integer> left = info.listDeepClone(info.left);
                FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses = info.fdTreeNodeEquivalenceClasses.deepClone();
                fdTreeNodeEquivalenceClasses.mergeLeftNode(i, data);
                left.add(i);
                for(int k = 0; k < attributeNum; k++){
                    //若已经在父结点成立，则必成立，不用验证
                    if(child.confirmed.get(k)){
                        //fdRHS初始全为true，left.parent->k成立，所以left->k也成立，fdRHS仍未true
                        continue;
                    }
//                    else if(isSubSet(left, k, fdCandidates)){
//                        continue;
//                    }
                    //k在left中（平凡）
                    else if(isValueInList(k,left)){
                        //虽然left->k成立，但是不能保证(left,attr)->k成立，所以要将对应位置的fdRHS设置为false,
                        child.fdRHSCandidate.set(k, false);
                    }else{
//                       fdTreeNodeEquivalenceClasses.initializeRight(k, data);
                        FDValidationResult fdResult = fdTreeNodeEquivalenceClasses.checkFDRefinement(k,data);
                        if(fdResult.status == "non-valid"){
                            child.fdRHSCandidate.set(k, false);
                        }
                    }
                }
                    //confirmed中包含了除平凡fd以外的所有fd（最小fd,非最小fd）,confirmed.get(k)为true
                    //这些右侧k,在node.child中是不需要验证的，必然也成立。
                child.confirmed = child.comfirmedDeepClone(child.fdRHSCandidate, attributeNum);
                child.mininal = checkFDMinimal(child, parent, left, fdCandidates);
                for(int j = 0; j < attributeNum; j++){
                    if(child.mininal.get(j)){
                        fdCandidates.add(new FDCandidate(left, j));
                    }
                }
//                parent.children[index] = child;
//                index++;
                queue.offer(new FDDiscoverNodeSavingInfo(child, fdTreeNodeEquivalenceClasses, left));
            }
        }
        return fdCandidates;
    }


    public void printArray(Boolean[] booleans){
        List<Integer> list = new ArrayList<>();
        for(Boolean b:booleans){
            if(b){
                list.add(1);
            }else list.add(0);
        }
        System.out.println(list);
    }

    public List<FDCandidate> getPartCandidate(int begin, List<FDCandidate> fdCandidates){
        List<FDCandidate> part = fdCandidates.subList(begin, fdCandidates.size());
        return part;
    }

    public int countNum(int index, int[] arr){
        int result = 0;
        for(int i = 0; i <= index; i++){
            result += arr[i];
        }
        return result;
    }

    public int[] countLevelNum(int attributeNum){
        int[] levelNum = new int[attributeNum + 1];
        for(int i = 0; i <= attributeNum; i++){
            levelNum[i] =combination(attributeNum, i);
        }
        return levelNum;
    }

    public int combination(int attributeNum, int k){
        int a = 1, b = 1;
        if(k > attributeNum / 2)
            k = attributeNum - k;
        for(int i = 1; i <= k; i++){
            a *= (attributeNum + 1 - i);
            b *= i;
        }
        return a / b;
    }

    public boolean hasSubSet(List<Integer> left, int right, List<FDCandidate> fdCandidates){
        boolean result = false;
        for(int i = 0; i < fdCandidates.size(); i++){
            if(fdCandidates.get(i).right == right){
                result = left.containsAll(fdCandidates.get(i).left);
                if(result) break;
            }
        }
        return result;
    }


    public BitSet checkFDMinimal(FDTree.FDTreeNode child, FDTree.FDTreeNode parent,
                                 List<Integer> left, List<FDCandidate> fdCandidates){
        List<Integer> fdRightOfParent = new ArrayList<>();
        for(int i = 0; i < parent.fdRHSCandidate.size(); i++){
            if(parent.fdRHSCandidate.get(i)){
                fdRightOfParent.add(i);
            }
        }
        for(int i = 0; i < child.fdRHSCandidate.size(); i++){
            if(child.fdRHSCandidate.get(i) && !isValueInList(i,fdRightOfParent)){
                child.mininal.set(i);
            }
        }
        for(int i = 0; i < child.fdRHSCandidate.size(); i++){
            if(hasSubSet(left, i, fdCandidates)){
                child.mininal.set(i, false);
            }
        }
        return child.mininal;
    }

    /**
     * 判断value是否在list中，在返回true,不在返回false
     * @param value
     * @param list
     * @return
     */
    public boolean isValueInList(int value, List<Integer> list){
        boolean reslut = false;
        for(int i = 0; i < list.size(); i++){
            if(value == list.get(i)){
                reslut = true;
                break;
            }
        }
        return reslut;
    }


    /**
     * 测试从fd中找到并且变化的EC是否与直接算的相同
     * @param args
     */
    public static void main(String[] args) {
//        DataFrame data = DataFrame.fromCsv("IntegerData/ncvoter 1000 19.csv");
//        DataFrame data = DataFrame.fromCsv("IntegerData/letter 20000 17.csv");
//        DataFrame data = DataFrame.fromCsv("IntegerData/bridges.csv");
        DataFrame data = DataFrame.fromCsv("IntegerData/echocardiogram.csv");
//        DataFrame data = DataFrame.fromCsv("IntegerData/abalone.csv");
//        DataFrame data = DataFrame.fromCsv("IntegerData/nursery.csv");
//        DataFrame data = DataFrame.fromCsv("IntegerData/horse.csv");
//        DataFrame data = DataFrame.fromCsv("IntegerData/test.csv");

        System.out.println("传统方法：");
        System.gc();
        Timer timer = new Timer();
        List<FDCandidate> result = new BFSFDDiscovererOld().discoverCandidate(data);
        System.out.println(timer.getTimeUsedAndReset() / 1000.0 + "s");
        System.out.println(result);
        System.out.println(result.size());

        System.out.println("refinement方法：");
        System.gc();
        Timer timer1 = new Timer();
        List<FDCandidate> result1 = new BFSFDDiscovererRefinement().discoverCandidateRefinement(data);
        System.out.println(timer1.getTimeUsedAndReset() / 1000.0 + "s");
        System.out.println(result1);
        System.out.println(result1.size());

//        System.out.println("全存：");
//        System.gc();
//        FDToODSavingInfo result = new BFSFDDiscoverer().discoverCandidateRefinementPlus(data);
//        System.out.println(timer.getTimeUsedAndReset() / 1000.0 + "s");
//        System.out.println(result.attibuteListToEqcMap);
//        System.out.println(result.attibuteListToEqcMap.size());
//        System.out.println(result.fdCandidates.size());
//        List<AttributeAndDirection> list = new ArrayList<>();
//        list.add(AttributeAndDirection.getInstance(6, AttributeAndDirection.UP));
//        list.add(AttributeAndDirection.getInstance(4, AttributeAndDirection.DOWN));
//        ODTreeNodeEquivalenceClasses odt = new ODTreeNodeEquivalenceClasses();
//        odt.mergeNodeTest(AttributeAndDirection.getInstance(6, AttributeAndDirection.UP), data, ODTree.ODTreeNodeStatus.SPLIT);
//        odt.mergeNodeTest(AttributeAndDirection.getInstance(4, AttributeAndDirection.DOWN), data, ODTree.ODTreeNodeStatus.SPLIT);
//        System.out.println(odt);
//        System.out.println("---");
//        ODTreeNodeEquivalenceClasses odt2 = new ODTreeNodeEquivalenceClasses();
//        odt2.findAndHandleEquivalenceClass(result.attibuteListToEqcMap, list, data);
//        System.out.println(odt2);
//        System.out.println(odt.left.equals(odt2.left));

    }
}
