package discoverer.total;

import dataStructures.DataFrame;
import dataStructures.fd.FDCandidate;
import dataStructures.fd.FDTree;
import dataStructures.fd.FDTreeNodeEquivalenceClasses;
import dataStructures.fd.FDValidationResult;
import dataStructures.od.ODCandidate;
import dataStructures.od.ODTree;
import dataStructures.od.ODTree.ODTreeNode;
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
        for (Integer integer : list) {
            if (value == integer) {
                reslut = true;
                break;
            }
        }
        return reslut;
    }

    public void discoverCandidate(DataFrame data, ODTree reference){
        FDTree fdResult = new FDTree(data.getColumnCount());
        FDTree.FDTreeNode node = fdResult.getRoot();
        int attributeNum = data.getColumnCount();
        ODTree odResult = new ODTree(attributeNum);
        ODMinimalChecker odMinimalChecker = new ODMinimalCheckerBruteForce();

        Queue<QueueElement> queue = new LinkedList<>();

        FDTreeNodeEquivalenceClasses fde = new FDTreeNodeEquivalenceClasses();
        queue.offer(new QueueElement(new FDDiscoverNodeSavingInfo(node, fde, new ArrayList<>()),QueueElement.FD));

        for(int attribute = 0; attribute < attributeNum; attribute++){
            if(reference!=null) {
                copyConfirmNode(odResult, odResult.getRoot().children[attribute]
                        , reference.getRoot().children[attribute]);
            }
            ODTreeNodeEquivalenceClasses odTreeNodeEquivalenceClasses = new ODTreeNodeEquivalenceClasses();
            odTreeNodeEquivalenceClasses.mergeNode(odResult.getRoot().children[attribute], data);
            queue.offer(new QueueElement(new ODDiscovererNodeSavingInfo(odResult.getRoot().children[attribute],
                    odTreeNodeEquivalenceClasses), QueueElement.OD));
        }

        while (!queue.isEmpty()){
            QueueElement element = queue.poll();
            if(element.flag == QueueElement.FD){
                Timer t1 = new Timer();
                FDDiscoverNodeSavingInfo info = element.fdInfo;
                FDTree.FDTreeNode parent = info.nodeInResultTree;
                //???attribute set??????attribute????????????
                //?????????????????????parent???attribute?????????????????????????????????????????????????????????????????????2??????????????????3???4???5
                for(int i = parent.attribute + 1; i < attributeNum; i++){
                    FDTree.FDTreeNode child = fdResult.new FDTreeNode(parent, i, attributeNum);
                    List<Integer> left = info.listDeepClone(info.left);
                    FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses = info.fdTreeNodeEquivalenceClasses.deepClone();
                    fdTreeNodeEquivalenceClasses.mergeLeftNode(i, data);
                    left.add(i);
                    for(int k = 0; k < attributeNum; k++){
                        //?????????????????????????????????????????????????????????
                        if(child.confirmed.get(k)){
                            //fdRHS????????????true???left.parent->k???????????????left->k????????????fdRHS??????true
                            continue;
                        }
                        else if(isValueInList(k,left)){
                            //??????left->k???????????????????????????(left,attr)->k????????????????????????????????????fdRHS?????????false,
                            child.fdRHSCandidate.set(k, false);
//                        continue;
                        }else{
                            FDValidationResult fdValidationResult= fdTreeNodeEquivalenceClasses.checkFDRefinement(k,data);
                            if(fdValidationResult.status.equals("non-valid")){
                                child.fdRHSCandidate.set(k, false);
                            }
                        }
                    }
                    //confirmed?????????????????????fd???????????????fd?????????fd,?????????fd???,confirmed.get(k)???true
                    //????????????k,???node.child?????????????????????????????????????????????
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
                ODTreeNode parent = info.nodeInResultTree;
                for(int attribute = 0; attribute < attributeNum * 2; attribute++){
                    ODTreeNode child;
                    if(parent.children[attribute] == null)
                        child = odResult.new ODTreeNode(parent, odResult.getAttributeAndDirectionFromIndex(attribute));
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
        odTree = odResult;
    }

    private void copyConfirmNode(ODTree resultTree,ODTreeNode resultTreeNode,ODTreeNode referenceTreeNode){
        for (ODTreeNode referenceChildNode:referenceTreeNode.children) {
            if(referenceChildNode!=null && referenceChildNode.confirm){
                ODTreeNode resultChildNode =resultTree.new ODTreeNode
                        (resultTreeNode,referenceChildNode.attribute);
                resultChildNode.status=referenceChildNode.status;
                resultChildNode.confirm();
                copyConfirmNode(resultTree,resultChildNode,referenceChildNode);
            }
        }
    }

    public static void main(String[] args) {
        DataFrame data = DataFrame.fromCsv("Data/test.csv");

        System.out.println("refinement?????????????????????");
        System.gc();
        util.Timer timer = new Timer();
        BFSTotalDiscoverer discoverer = new BFSTotalDiscoverer();
        discoverer.discoverCandidate(data, null);
        System.out.println(timer.getTimeUsed() / 1000.0 + "s");
        System.out.println(fdCandidates);
        System.out.println("??????fd?????????"+fdCandidates.size());
        List<ODCandidate> ods = odTree.getAllOdsOrderByBFS();
        System.out.println(ods);
        System.out.println("??????od?????????"+ods.size());
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
