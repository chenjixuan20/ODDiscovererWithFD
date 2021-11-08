package discoverer.fd;

import dataStructures.DataFrame;
import dataStructures.DataWareHouse;
import dataStructures.fd.Array.FDTreeArray;
import dataStructures.fd.Array.FDTreeArray.FDTreeNode;
import dataStructures.fd.FDCandidate;
import dataStructures.fd.FDTreeNodeEquivalenceClasses;
import dataStructures.fd.FDValidationResult;
import dataStructures.od.AttributeAndDirection;
import discoverer.fd.Array.FDDiscoverNodeSavingInfoArray;

import java.util.*;

public abstract class FDDiscoverer {
    public List<FDCandidate> fdCandidates = new ArrayList<>();
    public static DataWareHouse dataWareHouse = DataWareHouse.getInstance();
    public Map<String, Boolean[]> attributeToConfirmed = new HashMap<>();
    public FDTreeArray fdTreeArray;

    public  int newFoundFdCount;

    public abstract void addFDCandidate(FDTreeNode node, int attributeNum, List<Integer> left);

    public FDDiscoverNodeSavingInfoArray newAndTraverseNode(int expendInLeft, DataFrame data, FDTreeNode parent,
                                                            Boolean[] anotherFatherRHSCandidate, boolean isLevel1,
                                                            FDDiscoverNodeSavingInfoArray infoArray){
        FDTreeNode child;
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
        /*
            fd的PLI的数据仓库改动
         */
        dataWareHouse.listEcMap.put(left.toString(),fdTreeNodeEquivalenceClasses.left);

        int attributeNum = data.getColumnCount();
        for(int k = 0; k < attributeNum; k++){
            if(isLevel1 && expendInLeft == k) {
                //A->A成立（平凡函数依赖）
                child.RHSCandidate[expendInLeft] = true;
                continue;
            }
            if(isLevel1 || !child.RHSCandidate[k]){
//                System.out.println(k);
                FDValidationResult fdResult = fdTreeNodeEquivalenceClasses.checkFDRefinement(k,data);
//                System.out.println(fdResult.status);
                if(fdResult.status.equals("non-valid")){
                    //右侧为k的fdCandidate无效
                    child.RHSCandidate[k] = false;
                }else{
                    child.RHSCandidate[k] = true;
                    if(isLevel1 && !parent.RHSCandidate[k]){
                        child.minimal[k] = true;
                    }
                }
            }
        }
        if(isLevel1)
            attributeToConfirmed.put(String.valueOf(expendInLeft),child.RHSCandidate);
        else
            child.minimal = checkFDMinimalArray(child, parent, left, fdCandidates, anotherFatherRHSCandidate);
        addFDCandidate(child,attributeNum,left);
        //将字节点挂在parent上，建立联系
        parent.children.add(child);
        return new FDDiscoverNodeSavingInfoArray(child, fdTreeNodeEquivalenceClasses, left);
    }

    //处理新增的子节点
    public FDDiscoverNodeSavingInfoArray vaildateNode(FDTreeNode child, DataFrame data,
                                                      FDDiscoverNodeSavingInfoArray info, FDTreeNode parent){
        //初始left
//        System.out.println("处理新增节点");
        List<Integer> left = info.listDeepClone(info.left);
//        System.out.println("从queue中初始的left：" + left );
        FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses = info.fdTreeNodeEquivalenceClasses.deepClone();
        int lastInLeft = child.attribute;
        String lastInLeftString = String.valueOf(lastInLeft);
//        System.out.println("当前节点属性：" + lastInLeft);
        List<Integer> attributeOfNewNodes = new ArrayList<>();
        //完善初始化RHSCandidate
        for(int i = 0; i < attributeToConfirmed.get(lastInLeftString).length; i++){
            if(attributeToConfirmed.get(lastInLeftString)[i]){
                child.RHSCandidate[i] = true;
            }
        }
        left.add(lastInLeft);
//        System.out.println("当前节点的fd左侧：" + left );
        fdTreeNodeEquivalenceClasses.mergeLeftNode(lastInLeft, data);
        int attributeNum = data.getColumnCount();
        for(int i = 0; i < attributeNum; i++){
            if(!child.RHSCandidate[i]){
                FDValidationResult fdResult = fdTreeNodeEquivalenceClasses.checkFDRefinement(i,data);
                if(fdResult.status.equals("non-valid")){
                    child.RHSCandidate[i] = false;
                    if(i > child.attribute) attributeOfNewNodes.add(i);
                }else{
                    child.RHSCandidate[i] = true;
                }
            }
        }
//        System.out.println("当前节点需要增加的子节点" + attributeOfNewNodes);
        child.minimal = checkFDMinimalArray(child, parent, left, fdCandidates, attributeToConfirmed.get(lastInLeftString));
        addFDCandidate(child,attributeNum,left);
        parent.children.add(child);
        FDDiscoverNodeSavingInfoArray infoArray = new FDDiscoverNodeSavingInfoArray(child, fdTreeNodeEquivalenceClasses, left, child.RHSCandidate);
        //child的子节点作为新增节点进行补充，queue.poll().node为child时，chid.children为空，直接进入新增新增子节点的遍历
        if(!attributeOfNewNodes.isEmpty()){
            infoArray.newChildren = initializeChildren(infoArray,data,attributeOfNewNodes,child);
        }
//        System.out.println();
        return infoArray;
    }

    //处理原先已经存在的子节点
    public FDDiscoverNodeSavingInfoArray reTraverseNode(FDTreeNode node, FDTreeNode parent,
                                                        FDDiscoverNodeSavingInfoArray info, DataFrame data, Boolean isLevel2){
        FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses;
        List<Integer> left;
        Boolean[] parentRHSCandidate;
        //还原minimal
        Arrays.fill(node.minimal, false);
        if(isLevel2){
            fdTreeNodeEquivalenceClasses = new FDTreeNodeEquivalenceClasses();
            left = new ArrayList<>();
            parentRHSCandidate = new Boolean[data.getColumnCount()];
        }else {
            fdTreeNodeEquivalenceClasses = info.fdTreeNodeEquivalenceClasses.deepClone();
            left = info.listDeepClone(info.left);
            parentRHSCandidate = info.parentRHs;
        }
        //由于（left,i)由成立变为不成立，之前剪掉的节点需要补充
        List<Integer> attributeOfNewNodes = new ArrayList<>();
        //每个节点都需要重新算PLI
        fdTreeNodeEquivalenceClasses.mergeLeftNode(node.attribute, data);
        left.add(node.attribute);
        int lastInLeft = node.attribute;
        String lastInLeftString = String.valueOf(lastInLeft);
        int attributeNum = data.getColumnCount();
//        System.out.println("当前节点的属性： " + node.attribute);
//        System.out.println("处理到该节点时fd的左侧： " + left);
        //对这个节点对右侧进行对应的操作
        for(int i = 0; i < attributeNum; i++){
            //若右侧对应的fd之前是成立的 &&
            // 不是平凡函数依赖 &&
            //（其已经在新数据集中处理过的父节点中右侧对应的fd均不成立 || 该节点位于第2层） 理由：若父节点中成立，那么子节点中肯定也成立，就不用验证了
            // 则需要重新checkFD;
            boolean flag = isLevel2  || !(parentRHSCandidate[i] || attributeToConfirmed.get(lastInLeftString)[i]);
            if(node.RHSCandidate[i] && !left.contains(i) && flag){
                FDValidationResult fdResult = fdTreeNodeEquivalenceClasses.checkFDRefinement(i, data);
                //新数据集中变成不成立，则子节点中的初始不用遍历的rhs变少(右侧之前被剪掉，现在不能），需要补充；并且会有新的子节点生成
                if(fdResult.status.equals("non-valid")){
                    //child的子节点的初始RHSCandidate需要更新
                    node.RHSCandidate[i] = false;
                    //子节点中左侧为(left,i)的结点需要生成
                    if(i > lastInLeft)
                        attributeOfNewNodes.add(i);
                }else{
                    if(isLevel2 && !parent.RHSCandidate[i])
                        node.minimal[i] = true;
                }
            }
        }

        if(isLevel2) attributeToConfirmed.put(String.valueOf(node.attribute), node.RHSCandidate);
            //验证是否为最小
        else {
            node.minimal = checkFDMinimalArray(node, parent, left, fdCandidates, attributeToConfirmed.get(String.valueOf(node.attribute)));
        }
        addFDCandidate(node,attributeNum,left);

//        System.out.println("当前节点需要增加的子节点" + attributeOfNewNodes);
        //该节点所有的右侧都处理完后，再将完整的RHSCandidate保存再queue中，传给其子节点
        FDDiscoverNodeSavingInfoArray infoArray = new FDDiscoverNodeSavingInfoArray(node, fdTreeNodeEquivalenceClasses, left, node.RHSCandidate);
        //再看有没有需要新增的子节点
        if(!attributeOfNewNodes.isEmpty()){
            infoArray.newChildren = initializeChildren(infoArray,data,attributeOfNewNodes,node);
        }
//        System.out.println();
        return infoArray;
    }

    public List<FDTreeNode> initializeChildren(FDDiscoverNodeSavingInfoArray infoArray, DataFrame data,
                                                           List<Integer> attributeOfNewNodes, FDTreeNode parent){
        infoArray.hasChildren = true;
        List<FDTreeNode> newChildren = new ArrayList<>();
        for(Integer right : attributeOfNewNodes){
            //map里的RHS还没加入newChild的初始化中
            FDTreeNode newChild = fdTreeArray.new FDTreeNode(parent,right,data.getColumnCount(),null);
            newChildren.add(newChild);
        }
        return newChildren;
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

    public Boolean[] checkFDMinimalArray(FDTreeNode child, FDTreeNode parent,
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
}
