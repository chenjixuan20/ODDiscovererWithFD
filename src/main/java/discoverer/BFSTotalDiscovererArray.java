package discoverer;

import dataStructures.DataFrame;
import dataStructures.fd.Array.FDTreeArray;
import dataStructures.fd.Array.FDTreeArray.FDTreeNode;
import dataStructures.fd.FDCandidate;
import dataStructures.fd.FDTreeNodeEquivalenceClasses;
import dataStructures.fd.FDValidationResult;
import dataStructures.od.ODCandidate;
import dataStructures.od.ODTree;
import dataStructures.od.ODTree.ODTreeNode;
import dataStructures.od.ODTreeNodeEquivalenceClasses;
import discoverer.fd.Array.FDDiscoverNodeSavingInfoArray;
import discoverer.od.ODDiscovererNodeSavingInfo;
import minimal.ODMinimalChecker;
import minimal.ODMinimalCheckerBruteForce;

import java.util.*;

public class BFSTotalDiscovererArray {
    public List<FDCandidate> fdCandidates = new ArrayList<>();
    public Map<Integer,List<List<Integer>>> fdMap = new HashMap<>();
    Map<Integer, Boolean[]> attributeToConfirmed = new HashMap<>();
    FDTreeArray fdTreeArray;
    public ODTree odTree;
    Queue<QueueElementArray> queue = new LinkedList<>();

    public void discoverFirstTime(DataFrame data, ODTree reference){
        fdCandidates.clear();
        int attributeNum = data.getColumnCount();
        fdTreeArray = new FDTreeArray(attributeNum);
        FDTreeNode fdRoot = fdTreeArray.getRoot();
        ODTree odResult = new ODTree(attributeNum);
        ODMinimalChecker odMinimalChecker = new ODMinimalCheckerBruteForce();

        //处理根节点和level1的节点
        processFDRootAndLevel1(data,fdRoot);

        //第二层的所有结点的direction为UP
        addLevel1ODNode(data,odResult,reference);

        while (!queue.isEmpty()){
            QueueElementArray element = queue.poll();
            if(element.flag == QueueElementArray.FD){
                FDDiscoverNodeSavingInfoArray info = element.fdInfo;
                FDTreeNode parent = info.nodeInResultTree;
                disposeFDNode(parent, data, info);
            }
            else {
                ODDiscovererNodeSavingInfo info = element.odInfo;
                ODTreeNode parent = info.nodeInResultTree;
                disposeODNode(odResult, parent, data, info, odMinimalChecker);
            }
        }
        odTree = odResult;
    }

    public void disposeFDNode(FDTreeNode parent, DataFrame data, FDDiscoverNodeSavingInfoArray info){
        //            prune规则2  key剪枝
        if(trueRHSCounts(parent.RHSCandidate) != data.getColumnCount()){
//                设attribute set中的attribute升序排列
//                生成子节点，当parent的attribute一定时，其生成的子节点的attribute从比他大开始（避免重复12，21的情况），比如说当属性为2时，则只生成3，4，5
            for(int i = parent.attribute + 1; i < data.getColumnCount(); i++){
                List<Integer> left = info.listDeepClone(info.left);
//                    prune规则4     XY->Z,则XYZ->M否由XY->M否决定，XY->M成立则不是最小，XY->M不成立则不成立
                if(hasSubSet(left,i,fdCandidates)){
                    continue;
                }
                FDDiscoverNodeSavingInfoArray infoArray = newAndTraverseNode(i,data,parent,attributeToConfirmed.get(i),false, info);
                queue.offer(new QueueElementArray(infoArray, QueueElementArray.FD));
            }
        }
    }

    public void disposeODNode(ODTree odResult, ODTreeNode parent, DataFrame data,
                              ODDiscovererNodeSavingInfo info, ODMinimalChecker odMinimalChecker){
        int attributeNum = data.getColumnCount();
        for(int attribute = 0; attribute < attributeNum * 2; attribute++){
            ODTreeNode child;
            if(parent.children[attribute] == null)
                child = odResult.new ODTreeNode(parent, odResult.getAttributeAndDirectionFromIndex(attribute));
            else
                child = parent.children[attribute];
            ODCandidate childCandidate = new ODCandidate(child);
            child.minimal = odMinimalChecker.isCandidateMinimalFDMap(childCandidate, fdMap);
//            child.minimal = odMinimalChecker.isCandidateMinimalFD(childCandidate, fdCandidates);
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
                queue.offer(new QueueElementArray(new ODDiscovererNodeSavingInfo(child, odTreeNodeEquivalenceClasses),
                        QueueElement.OD));
            }
        }
    }

    public void addLevel1ODNode(DataFrame data, ODTree odResult, ODTree reference){
        int attributeNum = data.getColumnCount();
        for (int attribute = 0; attribute < attributeNum; attribute++) {
            if(reference!=null) {
                copyConfirmNode(odResult, odResult.getRoot().children[attribute]
                        , reference.getRoot().children[attribute]);
            }
            ODTreeNodeEquivalenceClasses odTreeNodeEquivalenceClasses = new ODTreeNodeEquivalenceClasses();
            odTreeNodeEquivalenceClasses.mergeNode(odResult.getRoot().children[attribute], data);
            queue.offer(new QueueElementArray(new ODDiscovererNodeSavingInfo(odResult.getRoot().children[attribute], odTreeNodeEquivalenceClasses),
                    QueueElementArray.OD));
        }
    }

    public void processFDRootAndLevel1(DataFrame data, FDTreeArray.FDTreeNode root){
        int attributeNum = data.getColumnCount();
        for(int i = 0; i < attributeNum; i++){
            FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses = new FDTreeNodeEquivalenceClasses();
            FDValidationResult fdResult = fdTreeNodeEquivalenceClasses.checkFDRefinement(i,data);
            if(fdResult.status.equals("valid")){
                root.RHSCandidate[i] = true;
                fdCandidates.add(new FDCandidate(new ArrayList<>(), i, root));
                List<List<Integer>> allLeft = new ArrayList<>();
                List<Integer> nowLeft = new ArrayList<>();
                allLeft.add(nowLeft);
                fdMap.put(i, allLeft);
            }
        }
        for(int i = 0; i < attributeNum; i++){
            FDDiscoverNodeSavingInfoArray infoArray = newAndTraverseNode(i, data, root, null,
                    true, null);
            queue.add(new QueueElementArray(infoArray,QueueElement.FD));
        }
    }

    public FDDiscoverNodeSavingInfoArray newAndTraverseNode(int expendInLeft, DataFrame data, FDTreeArray.FDTreeNode parent,
                                                            Boolean[] anotherFatherRHSCandidate, boolean isLevel1,
                                                            FDDiscoverNodeSavingInfoArray infoArray){
        FDTreeArray.FDTreeNode child;
        FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses;
        List<Integer> left;
        if(isLevel1){
            child = fdTreeArray.new FDTreeNode(parent, expendInLeft, data.getColumnCount(),null);
            fdTreeNodeEquivalenceClasses = new FDTreeNodeEquivalenceClasses();
            left = new ArrayList<>();
            fdTreeNodeEquivalenceClasses.mergeLeftNode(expendInLeft, data);
            left.add(expendInLeft);
        }else{
            child = fdTreeArray.new FDTreeNode(parent, expendInLeft, data.getColumnCount(), anotherFatherRHSCandidate);
            fdTreeNodeEquivalenceClasses = infoArray.fdTreeNodeEquivalenceClasses.deepClone();
            left = infoArray.listDeepClone(infoArray.left);
            fdTreeNodeEquivalenceClasses.mergeLeftNode(expendInLeft, data);
            left.add(expendInLeft);
        }

        for(int k = 0; k < data.getColumnCount(); k++){
            if(isLevel1 && expendInLeft == k) {
                //A->A成立（平凡函数依赖）
                child.RHSCandidate[expendInLeft] = true;
                continue;
            }
            if(isLevel1 || !child.RHSCandidate[k]){
                FDValidationResult fdResult = fdTreeNodeEquivalenceClasses.checkFDRefinement(k,data);
                if(fdResult.status.equals("non-valid")){
                    //右侧为k的fdCandidate无效
                    child.RHSCandidate[k] = false;
                }else{
                    child.RHSCandidate[k] = true;
                    if(isLevel1 && !parent.RHSCandidate[k])
                        child.minimal[k] = true;
                }
            }
        }

        if(isLevel1)
            attributeToConfirmed.put(expendInLeft,child.RHSCandidate);
        else {
            child.minimal = checkFDMinimalArray(child, parent, left, fdCandidates, anotherFatherRHSCandidate);
        }
        for(int j = 0; j < data.getColumnCount(); j++){
            if(child.minimal[j]){
                fdCandidates.add(new FDCandidate(left, j, child));
                if(!fdMap.containsKey(j)){
                    List<List<Integer>> allLeft = new ArrayList<>();
                    List<Integer> nowLeft = deepClone(left);
                    allLeft.add(nowLeft);
                    fdMap.put(j, allLeft);
                }else {
                    List<List<Integer>> allLeft = fdMap.get(j);
                    List<Integer> nowLeft = deepClone(left);
                    allLeft.add(nowLeft);
                    fdMap.put(j, allLeft);
                }
            }
        }
        parent.children.add(child);
        return new FDDiscoverNodeSavingInfoArray(child, fdTreeNodeEquivalenceClasses, left);
    }

    public boolean hasSubSet(List<Integer> left, int right, List<FDCandidate> fdCandidates){
        boolean result = false;
        for (FDCandidate fdCandidate : fdCandidates) {
            if (fdCandidate.right == right) {
                result = left.containsAll(fdCandidate.left);
                if (result) break;
            }
        }
        return result;
    }

    public Boolean[] checkFDMinimalArray(FDTreeArray.FDTreeNode child, FDTreeArray.FDTreeNode parent,
                                         List<Integer> left, List<FDCandidate> fdCandidates,
                                         Boolean[] anotherConfirmed){
//        List<Integer> fdRightOfParent = new ArrayList<>();
//        for(int i = 0; i < parent.fdRHSCandidate.length; i++){
//            if(parent.fdRHSCandidate[i]){
//                fdRightOfParent.add(i);
//            }
//        }
//        for(int i = 0; i < child.fdRHSCandidate.length; i++){
//            if(child.fdRHSCandidate[i] && !isValueInList(i,fdRightOfParent)){
//                child.mininal[i] = true;
//            }
//        }
//        for(int i = 0; i < child.fdRHSCandidate.length; i++){
//            if(isSubSet(left, i, fdCandidates)){
//                child.mininal[i] = false;
//            }
//        }
        Boolean[] allParentConfirmed = parent.RHSCandidate.clone();
        for(int i = 0; i < anotherConfirmed.length; i++){
            if(anotherConfirmed[i]){
                allParentConfirmed[i] = true;
            }
        }
//        System.out.println("parent_comfired:");
//        printArray(allParentConfirmed);
//        System.out.println("child_comfired:");
//        printArray(child.confirmed);
        for(int i = 0; i < child.RHSCandidate.length; i++){
            if(child.RHSCandidate[i] && !allParentConfirmed[i] &&!hasSubSet(left, i, fdCandidates)){
                child.minimal[i] = true;
            }else {
                child.minimal[i] = false;
            }
        }
//        System.out.println("child_minimal:");
//        printArray(child.minimal);
        return child.minimal;
    }

    public int trueRHSCounts(Boolean[] booleans){
        int result = 0;
        for(Boolean b : booleans){
            if(b) result++;
        }
        return result;
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

    private List<Integer> deepClone(List<Integer> list){
        return new ArrayList<>(list);
    }
}
