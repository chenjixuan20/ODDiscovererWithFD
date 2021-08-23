package discoverer.fd.Array;

import dataStructures.DataFrame;
import dataStructures.fd.Array.FDTreeArray;
import dataStructures.fd.FDCandidate;
import dataStructures.fd.FDTreeNodeEquivalenceClasses;
import dataStructures.fd.FDValidationResult;

import java.util.*;

public class BFSFDDiscovererArray {
    List<FDCandidate> fdCandidates = new ArrayList<>();
    Map<Integer, Boolean[]> attributeToConfirmed = new HashMap<>();
    FDTreeArray result;
    Queue<FDDiscoverNodeSavingInfoArray> queue = new LinkedList<>();

    public DiscoverResult discoverCandidateRefinementFromNull(DataFrame data){
        fdCandidates.clear();
        result = new FDTreeArray(data.getColumnCount());
        FDTreeArray.FDTreeNode root = result.getRoot();
        //手动做第0层
        for(int i = 0; i < data.getColumnCount(); i++){
            FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses = new FDTreeNodeEquivalenceClasses();
            FDValidationResult fdResult = fdTreeNodeEquivalenceClasses.checkFDRefinement(i,data);
            if(fdResult.status.equals("valid")){
                root.RHSCandidate[i] = true;
                fdCandidates.add(new FDCandidate(new ArrayList<>(), i, root));
            }
        }

        //先手动做第一层的|lhs|=1
        for(int i = 0; i < data.getColumnCount(); i++){
            FDDiscoverNodeSavingInfoArray infoArray = newAndTraverseNodeFromZero(i, data, root, null,
                    true, null);
            queue.add(infoArray);
        }

        while (!queue.isEmpty()){
            FDDiscoverNodeSavingInfoArray info = queue.poll();
            FDTreeArray.FDTreeNode parent = info.nodeInResultTree;
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
                    FDDiscoverNodeSavingInfoArray infoArray = newAndTraverseNodeFromZero(i,data,parent,attributeToConfirmed.get(i),false, info);
                    queue.offer(infoArray);
                }
            }
        }
        return new DiscoverResult(result, fdCandidates);
    }


    public FDDiscoverNodeSavingInfoArray newAndTraverseNodeFromZero(int expendInLeft, DataFrame data, FDTreeArray.FDTreeNode parent,
                                                                    Boolean[] anotherFatherRHSCandidate, boolean isLevel1,
                                                                    FDDiscoverNodeSavingInfoArray infoArray){
        FDTreeArray.FDTreeNode child;
        FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses;
        List<Integer> left;
        if(isLevel1){
            child = result.new FDTreeNode(parent, expendInLeft, data.getColumnCount(),null);
            fdTreeNodeEquivalenceClasses = new FDTreeNodeEquivalenceClasses();
            left = new ArrayList<>();
            fdTreeNodeEquivalenceClasses.mergeLeftNode(expendInLeft, data);
            left.add(expendInLeft);
        }else{
            child = result.new FDTreeNode(parent, expendInLeft, data.getColumnCount(), anotherFatherRHSCandidate);
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
            }
        }
        parent.children.add(child);
        return new FDDiscoverNodeSavingInfoArray(child, fdTreeNodeEquivalenceClasses, left);
    }

    public DiscoverResult discoverCandidateRefinementMorePruneAfterValidate(DataFrame data, FDTreeArray reference){
        fdCandidates.clear();
        result = reference;
        FDTreeArray.FDTreeNode root = result.getRoot();
        /*处理根节点*/
        for(int i = 0; i < data.getColumnCount(); i++){
            if(root.RHSCandidate[i]){
                //存在[]->x,需要重新检查
                FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses = new FDTreeNodeEquivalenceClasses();
                FDValidationResult fdResult = fdTreeNodeEquivalenceClasses.checkFDRefinement(i,data);
                if(fdResult.status.equals("non-valid")){
                    //结果变为non-valid,子节点中所有rhs[i]变为false
                    root.RHSCandidate[i] = false;
                }else {
                    fdCandidates.add(new FDCandidate(new ArrayList<>(), i, root));
                }
            }
        }

        /*先手动做第一层，得到map，完善后面的剪枝*/
        for(FDTreeArray.FDTreeNode child : root.children){
            FDDiscoverNodeSavingInfoArray infoArray = reTraverseNode(child, root, null, data, true);
            queue.add(infoArray);
        }

        while(!queue.isEmpty()){
            FDDiscoverNodeSavingInfoArray info = queue.poll();
            FDTreeArray.FDTreeNode parent = info.nodeInResultTree;

            //处理该parent节点中本来就存在的子节点
            for(FDTreeArray.FDTreeNode child : parent.children){
                FDDiscoverNodeSavingInfoArray infoArray = reTraverseNode(child, parent, info, data, false);
                queue.add(infoArray);

            }
            //在补充并处理需要加上子节点
            if(info.hasChildren){
                for(FDTreeArray.FDTreeNode child : info.newChildren){
                    FDDiscoverNodeSavingInfoArray infoArray= vaildateNode(child,data,info,parent);
                    queue.offer(infoArray);
                }
            }

        }
        return new DiscoverResult(result, fdCandidates);
    }

    //处理新增的子节点
    public FDDiscoverNodeSavingInfoArray vaildateNode(FDTreeArray.FDTreeNode child, DataFrame data,
                             FDDiscoverNodeSavingInfoArray info, FDTreeArray.FDTreeNode parent){
        //初始left
//        System.out.println("处理新增节点");
        List<Integer> left = info.listDeepClone(info.left);
//        System.out.println("从queue中初始的left：" + left );
        FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses = info.fdTreeNodeEquivalenceClasses.deepClone();
        int lastInLeft = child.attribute;
//        System.out.println("当前节点属性：" + lastInLeft);
        List<Integer> attributeOfNewNodes = new ArrayList<>();
        //完善初始化RHSCandidate
        for(int i = 0; i < attributeToConfirmed.get(lastInLeft).length; i++){
            if(attributeToConfirmed.get(lastInLeft)[i]){
                child.RHSCandidate[i] = true;
            }
        }
        left.add(lastInLeft);
//        System.out.println("当前节点的fd左侧：" + left );
        fdTreeNodeEquivalenceClasses.mergeLeftNode(lastInLeft, data);
        for(int i = 0; i < data.getColumnCount(); i++){
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
        child.minimal = checkFDMinimalArray(child, parent, left, fdCandidates, attributeToConfirmed.get(lastInLeft));
        for(int j = 0; j < data.getColumnCount(); j++){
            if(child.minimal[j]){
                fdCandidates.add(new FDCandidate(left, j, child));
            }
        }
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
    public FDDiscoverNodeSavingInfoArray reTraverseNode(FDTreeArray.FDTreeNode node, FDTreeArray.FDTreeNode parent,
                               FDDiscoverNodeSavingInfoArray info, DataFrame data, Boolean isLevel1){
        FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses;
        List<Integer> left;
        Boolean[] parentRHSCandidate;
        //还原minimal
        Arrays.fill(node.minimal, false);
        if(isLevel1){
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
//        System.out.println("当前节点的属性： " + node.attribute);
//        System.out.println("处理到该节点时fd的左侧： " + left);
        //对这个节点对右侧进行对应的操作
        for(int i = 0; i < data.getColumnCount(); i++){
            //若右侧对应的fd之前是成立的 &&
            // 不是平凡函数依赖 &&
            //（其已经在新数据集中处理过的父节点中右侧对应的fd均不成立 || 该节点位于第一层） 理由：若父节点中成立，那么子节点中肯定也成立，就不用验证了
            // 则需要重新checkFD;
            boolean flag = isLevel1  || !(parentRHSCandidate[i] || attributeToConfirmed.get(lastInLeft)[i]);
            if(node.RHSCandidate[i] && !left.contains(i) && flag){
                FDValidationResult fdResult = fdTreeNodeEquivalenceClasses.checkFDRefinement(i, data);
                //新数据集中变成不成立，则子节点中的初始不用遍历的rhs变少，需要补充；并且会有新的子节点生成
                if(fdResult.status.equals("non-valid")){
                    //child的子节点的初始RHSCandidate需要更新
                    node.RHSCandidate[i] = false;
                    //子节点中左侧为(left,i)的结点需要生成
                    if(i > lastInLeft)
                        attributeOfNewNodes.add(i);
                }else {
                    if(isLevel1 && !parent.RHSCandidate[i])
                        node.minimal[i] = true;
                }
            }
        }

        if(isLevel1) attributeToConfirmed.put(node.attribute, node.RHSCandidate);
        //验证是否为最小
        else {
            node.minimal = checkFDMinimalArray(node, parent, left, fdCandidates, attributeToConfirmed.get(node.attribute));
        }

        for(int j = 0; j <  data.getColumnCount(); j++){
            if(node.minimal[j]){
                fdCandidates.add(new FDCandidate(left, j, node));
            }
        }
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

    public List<FDTreeArray.FDTreeNode> initializeChildren(FDDiscoverNodeSavingInfoArray infoArray, DataFrame data,
                              List<Integer> attributeOfNewNodes, FDTreeArray.FDTreeNode parent){
        infoArray.hasChildren = true;
        List<FDTreeArray.FDTreeNode> newChildren = new ArrayList<>();
        for(Integer right : attributeOfNewNodes){
            //map里的RHS还没加入newChild的初始化中
            FDTreeArray.FDTreeNode newChild = result.new FDTreeNode(parent,right,data.getColumnCount(),null);
//            System.out.println("left in queue:" + infoArray.left);
//            System.out.println("newChild attribute: " + newChild.attribute);
//            System.out.println("left from newChild: " + getLeftFromNode(newChild));
//            System.out.println("---------------------------");
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


    /**
     * 从第一层开始，没有考虑[]->x这类fds
     * @param data
     * @param reference
     * @return
     */
//    public FDDiscoverNodeSavingInfoArray newAndTraverseNode(int expendInLeft, DataFrame data, FDTreeArray.FDTreeNode parent,
//                                                            Boolean[] anotherFatherRHSCandidate, boolean isLevel1,
//                                                            FDDiscoverNodeSavingInfoArray infoArray){
//        FDTreeArray.FDTreeNode child;
//        FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses;
//        List<Integer> left;
//        if(isLevel1){
//            child = result.new FDTreeNode(parent, expendInLeft, data.getColumnCount(),null);
//            fdTreeNodeEquivalenceClasses = new FDTreeNodeEquivalenceClasses();
//            left = new ArrayList<>();
//            fdTreeNodeEquivalenceClasses.mergeLeftNode(expendInLeft, data);
//            left.add(expendInLeft);
//        }else{
//            child = result.new FDTreeNode(parent, expendInLeft, data.getColumnCount(), anotherFatherRHSCandidate);
//            fdTreeNodeEquivalenceClasses = infoArray.fdTreeNodeEquivalenceClasses.deepClone();
//            left = infoArray.listDeepClone(infoArray.left);
//            fdTreeNodeEquivalenceClasses.mergeLeftNode(expendInLeft, data);
//            left.add(expendInLeft);
//        }
//
//        for(int k = 0; k < data.getColumnCount(); k++){
//            if(isLevel1 && expendInLeft == k) {
//                //A->A成立（平凡函数依赖）
//                child.RHSCandidate[expendInLeft] = true;
//                continue;
//            }
//            if(isLevel1 || !child.RHSCandidate[k]){
//                FDValidationResult fdResult = fdTreeNodeEquivalenceClasses.checkFDRefinement(k,data);
//                if(fdResult.status.equals("non-valid")){
//                    //右侧为k的fdCandidate无效
//                    child.RHSCandidate[k] = false;
//                }else{
//                    child.RHSCandidate[k] = true;
//                    if(isLevel1){
//                        fdCandidates.add(new FDCandidate(left, k, child));
//                    }
//                }
//            }
//        }
//
//        if(isLevel1)
//            attributeToConfirmed.put(expendInLeft,child.RHSCandidate);
//        else{
//            child.minimal = checkFDMinimalArray(child, parent, left, fdCandidates, attributeToConfirmed.get(expendInLeft));
//            for(int j = 0; j < data.getColumnCount(); j++){
//                if(child.minimal[j]){
//                    fdCandidates.add(new FDCandidate(left, j, child));
//                }
//            }
//        }
//        parent.children.add(child);
//        return new FDDiscoverNodeSavingInfoArray(child, fdTreeNodeEquivalenceClasses, left);
//    }
//
//    public DiscoverResult discoverCandidateRefinementMorePrune(DataFrame data){
//        fdCandidates.clear();
//        result = new FDTreeArray(data.getColumnCount());
//        FDTreeArray.FDTreeNode root = result.getRoot();
//
//
//        //先手动做第一层的|lhs|=1
//        for(int i = 0; i < data.getColumnCount(); i++){
//            FDDiscoverNodeSavingInfoArray infoArray = newAndTraverseNode(i, data, root, null,
//                    true, null);
//            queue.add(infoArray);
//        }
//
//        while (!queue.isEmpty()){
//            FDDiscoverNodeSavingInfoArray info = queue.poll();
//            FDTreeArray.FDTreeNode parent = info.nodeInResultTree;
////            prune规则2  key剪枝
//            if(trueRHSCounts(parent.RHSCandidate) != data.getColumnCount()){
////                设attribute set中的attribute升序排列
////                生成子节点，当parent的attribute一定时，其生成的子节点的attribute从比他大开始（避免重复12，21的情况），比如说当属性为2时，则只生成3，4，5
//                for(int i = parent.attribute + 1; i < data.getColumnCount(); i++){
//                    List<Integer> left = info.listDeepClone(info.left);
////                    prune规则4     XY->Z,则XYZ->M否由XY->M否决定，XY->M成立则不是最小，XY->M不成立则不成立
//                    if(hasSubSet(left,i,fdCandidates)){
//                        continue;
//                    }
//                    FDDiscoverNodeSavingInfoArray infoArray = newAndTraverseNode(i,data,parent,attributeToConfirmed.get(i),false, info);
//                    queue.offer(infoArray);
//                }
//            }
//        }
//        return new DiscoverResult(result, fdCandidates);
//    }
}
