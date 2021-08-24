package discoverer;

import dataStructures.DataFrame;
import dataStructures.fd.FDCandidate;
import dataStructures.fd.FDTree;
import dataStructures.fd.FDTreeNodeEquivalenceClasses;
import dataStructures.fd.FDValidationResult;
import dataStructures.od.ODCandidate;
import dataStructures.od.ODTree;
import dataStructures.od.ODTreeNodeEquivalenceClasses;
import discoverer.fd.FDDiscoverNodeSavingInfo;
import discoverer.od.ODDiscovererNodeSavingInfo;
import minimal.ODMinimalChecker;
import minimal.ODMinimalCheckerBruteForce;
import util.Timer;
import java.util.*;

public class BFSTotalDiscoverer {

    static List<FDCandidate> fdCandidates = new ArrayList<>();
    static  ODTree odTree;
    static long fdTime = 0;
    static long odTime = 0;

    public boolean isSubSet(List<Integer> left, int right, List<FDCandidate> fdCandidates){
        boolean result = false;
        for(int i = 0; i < fdCandidates.size(); i++){
            if(fdCandidates.get(i).right == right){
                result = left.containsAll(fdCandidates.get(i).left);
                if(result == true) break;;
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
            if(isSubSet(left, i, fdCandidates)){
                child.mininal.set(i, false);
            }
        }
        return child.mininal;
    }

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

    public void discoverCandidate(DataFrame data){
        FDTree result = new FDTree(data.getColumnCount());
        FDTree.FDTreeNode node = result.getRoot();
        int attributeNum = data.getColumnCount();
        ODTree reslut = new ODTree(attributeNum);
        ODMinimalChecker odMinimalChecker = new ODMinimalCheckerBruteForce();

        Queue<QueueElement> queue = new LinkedList<>();
        FDTreeNodeEquivalenceClasses fde = new FDTreeNodeEquivalenceClasses();
        queue.offer(new QueueElement(new FDDiscoverNodeSavingInfo(node, fde, new ArrayList<>()),QueueElement.FD));

        for(int attribute = 0; attribute < attributeNum; attribute++){
            ODTreeNodeEquivalenceClasses odTreeNodeEquivalenceClasses = new ODTreeNodeEquivalenceClasses();
            odTreeNodeEquivalenceClasses.mergeNode(reslut.getRoot().children[attribute], data);
            queue.offer(new QueueElement(new ODDiscovererNodeSavingInfo(reslut.getRoot().children[attribute],
                    odTreeNodeEquivalenceClasses), QueueElement.OD));
        }

        while (!queue.isEmpty()){
            QueueElement element = queue.poll();
            if(element.flag == QueueElement.FD){
                Timer t1 = new Timer();
                FDDiscoverNodeSavingInfo info = element.fdInfo;
                FDTree.FDTreeNode parent = info.nodeInResultTree;
                //设attribute set中的attribute升序排列
                //生成子节点，当parent的attribute一定时，其生成的子节点数量一致，比如说当属性为2时，则只生成3，4，5
                for(int i = parent.attribute + 1; i < attributeNum; i++){
                    FDTree.FDTreeNode child = result.new FDTreeNode(parent, i, attributeNum);
                    List<Integer> left = info.listDeepClone(info.left);
                    FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses = info.fdTreeNodeEquivalenceClasses.deepClone();
                    fdTreeNodeEquivalenceClasses.mergeLeftNode(i, data);
                    left.add(i);
                    for(int k = 0; k < attributeNum; k++){
                        //若已经在父结点成立，则必成立，不用验证
                        if(child.confirmed.get(k)){
                            //fdRHS初始全为true，left.parent->k成立，所以left->k也成立，fdRHS仍为true
                            continue;
                        }
                        else if(isValueInList(k,left)){
                            //虽然left->k成立，但是不能保证(left,attr)->k成立，所以要将对应位置的fdRHS设置为false,
                            child.fdRHSCandidate.set(k, false);
//                        continue;
                        }else{
                            FDValidationResult fdResult = fdTreeNodeEquivalenceClasses.checkFDRefinement(k,data);
                            if(fdResult.status.equals("non-valid")){
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
                    queue.offer(new QueueElement(new FDDiscoverNodeSavingInfo(child, fdTreeNodeEquivalenceClasses, left),QueueElement.FD));
                }
                fdTime += t1.getTimeUsedAndReset();
            }
            else{
                Timer t2 = new Timer();
                ODDiscovererNodeSavingInfo info = element.odInfo;
                ODTree.ODTreeNode parent = info.nodeInResultTree;
                for(int attribute = 0; attribute < attributeNum * 2; attribute++){
                    ODTree.ODTreeNode child;
                    if(parent.children[attribute] == null)
                        child = reslut.new ODTreeNode(parent, reslut.getAttributeAndDirectionFromIndex(attribute));
                    else
                        child = parent.children[attribute];
                    ODCandidate childCandidate = new ODCandidate(child);
                    child.minimal = odMinimalChecker.isCandidateMinimalFD(childCandidate, fdCandidates);
                    if(!child.minimal)
                        continue;
                    ODTreeNodeEquivalenceClasses odTreeNodeEquivalenceClasses =
                            info.odTreeNodeEquivalenceClasses.deepClone();
                    odTreeNodeEquivalenceClasses.mergeNode(child, data);
                    if(!child.confirm)
                        child.status = odTreeNodeEquivalenceClasses.check(data).status;
                    if(child.status == ODTree.ODTreeNodeStatus.VALID){
                        odMinimalChecker.insert(childCandidate);
                    }
                    if(child.status != ODTree.ODTreeNodeStatus.SWAP){
                        queue.offer(new QueueElement(new ODDiscovererNodeSavingInfo(child, odTreeNodeEquivalenceClasses),
                                QueueElement.OD));
                    }
                }
                odTime += t2.getTimeUsedAndReset();
            }
        }
        odTree = reslut;
    }

    public static void main(String[] args) {
        DataFrame data = DataFrame.fromCsv("Data/FLI 200.csv");

        System.out.println("refinement方法整体发现：");
        System.gc();
        util.Timer timer = new Timer();
        BFSTotalDiscoverer discoverer = new BFSTotalDiscoverer();
        discoverer.discoverCandidate(data);
        System.out.println(timer.getTimeUsed() / 1000.0 + "s");

//        for(FDCandidate fd: fdCandidates){
//            System.out.println(fd);
//        }
        System.out.println(fdCandidates);
        System.out.println("发现fd数量："+fdCandidates.size());

        List<ODCandidate> ods = odTree.getAllOdsOrderByBFS();
//        for(ODCandidate od:ods){
//            System.out.println(od);
//        }
        System.out.println(ods);
        System.out.println("发现od数量："+ods.size());

        System.out.println("fdTime:" + fdTime/1000.0 + "s");

        System.out.println("fdRefinementTime:" +  FDTreeNodeEquivalenceClasses.refinementTime/1000.0+"s");
        System.out.println("fdMeregeTime:" +  FDTreeNodeEquivalenceClasses.mergeTime/1000.0+"s");
        System.out.println("fdCloneTime:" +  FDTreeNodeEquivalenceClasses.cloneTime/1000.0+"s");
        System.out.println("fdMinimalCheckTime:" +  ODMinimalCheckerBruteForce.fdMinimalCheckTime/1000.0+"s");

        System.out.println("odTime:" + odTime/1000.0 + "s");
        System.out.println("checkTime:" + ODTreeNodeEquivalenceClasses.checkTime / 1000.0 + "s");
        System.out.println("cloneTime:" + ODTreeNodeEquivalenceClasses.cloneTime / 1000.0 + "s");
        System.out.println("mergeTime:" + ODTreeNodeEquivalenceClasses.mergeTime / 1000.0 + "s");
        System.out.println("odMinimalCheckTime:" +  ODMinimalCheckerBruteForce.odMinimalCheckTime/1000.0+"s");

    }
}
