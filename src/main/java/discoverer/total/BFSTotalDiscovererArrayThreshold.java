package discoverer.total;

import dataStructures.DataFrame;
import dataStructures.fd.Array.FDTreeArray;
import dataStructures.fd.Array.FDTreeArray.*;
import dataStructures.fd.FDCandidate;
import dataStructures.fd.FDTreeNodeEquivalenceClasses;
import dataStructures.fd.FDValidationResult;
import dataStructures.od.AttributeAndDirection;
import dataStructures.od.ODCandidate;
import dataStructures.od.ODTree;
import dataStructures.od.ODTree.ODTreeNode;
import dataStructures.od.ODTreeNodeEquivalenceClasses;
import discoverer.fd.Array.FDDiscoverNodeSavingInfoArray;
import discoverer.fd.FDDiscoverer;
import discoverer.od.ODDiscovererNodeSavingInfo;
import minimal.ODMinimalChecker;
import minimal.ODMinimalCheckerBruteForce;
import util.CloneUtil;

import java.util.*;

public class BFSTotalDiscovererArrayThreshold extends FDDiscoverer {
    private final CloneUtil cloneUtil = new CloneUtil();
    public static final int FD_INITIAL_RETURN_THRESHOLD =80;
    public static final int OD_INITIAL_RETURN_THRESHOLD =80;
    private boolean isFirstDone = false;
    private boolean isFDInterrup = false;
    private boolean isODInterrup = false;
    private InterruptStatus interruptStatus;
    private int fdReturnThreshold;
    private int odReturnThreshold;
    private final Map<String,List<List<Integer>>> fdMap = new HashMap<>();
    public ODTree odTree;
    Queue<QueueElementArray> queue = new LinkedList<>();
    private int newFoundOdCount;

    //todo 使用ODMinimalCheckTree实现 fd后推前
    static ODMinimalChecker odMinimalChecker = new ODMinimalCheckerBruteForce();
//    static ODMinimalChecker odMinimalChecker;

    int firstLever = 3;
    int afterLever = 3;

    public  BFSTotalDiscovererArrayThreshold(){
        this.interruptStatus = InterruptStatus.START_FIRST;
    }

    public void setInterruptStatus(InterruptStatus interruptStatus) {
        this.interruptStatus = interruptStatus;
    }

    @Override
    public void addFDCandidate(FDTreeNode node, int attributeNum, List<Integer> left) {
        for(int j = 0; j < attributeNum; j++){
            if(node.minimal[j]){
                //阈值中断思想，限制发现过多的fd
                newFoundFdCount++;
//                System.out.println(newFoundFdCount);
                fdCandidates.add(new FDCandidate(left, j, node));
                String J = String.valueOf(j);
                if(!fdMap.containsKey(J)){
                    List<List<Integer>> allLeft = new ArrayList<>();
                    List<Integer> nowLeft = cloneUtil.deepCloneList(left);
                    allLeft.add(nowLeft);
                    fdMap.put(J, allLeft);
                }else {
                    List<List<Integer>> allLeft = fdMap.get(J);
                    List<Integer> nowLeft = cloneUtil.deepCloneList(left);
                    allLeft.add(nowLeft);
                    fdMap.put(J, allLeft);
                }
            }
        }
    }

    //TODO 4个状态跳转的逻辑关系还没弄好
    public void discover(DataFrame data, ODTree odReference, FDTreeArray fdTree){
        if(!isFirstDone){
            if(interruptStatus.equals(InterruptStatus.START_FIRST)){
      System.out.println("-从头执行First算法-");
                discoverFirstTimes(data,odReference);
            }else if(interruptStatus.equals(InterruptStatus.CONTINUE_FIRST)) {
      System.out.println("-恢复执行当前First算法-");
                continueFirstTimes(data, odReference);
            }else {
      System.out.println("-First算法没有全部完成时，中断状态错误-");
            }
        }else {
            if(interruptStatus.equals(InterruptStatus.START_AFTER)){
      System.out.println("-从头执行After算法-");
                discoverAfterValidate(data,odReference,fdTree);
            }else if(interruptStatus.equals(InterruptStatus.CONTINUE_AFTER)) {
      System.out.println("-恢复执行当前After算法-");
                continueAfterValidate(data,odReference,fdTree);
            }else{
      System.out.println("-After阶段中断状态错误-");
            }
        }
    }

    public void discoverFirstTimes(DataFrame data, ODTree odReference){
    System.out.println("--startFirstTimes--");
        fdReturnThreshold = FD_INITIAL_RETURN_THRESHOLD;
        odReturnThreshold = OD_INITIAL_RETURN_THRESHOLD;
        init();

    System.out.println("fdReturnThreshold: " + fdReturnThreshold);
    System.out.println("odReturnThreshold: " + odReturnThreshold);
    System.out.println("newFoundFdCount: "+newFoundFdCount);
    System.out.println("newFoundOdCount: "+ newFoundOdCount);

        int attributeNum = data.getColumnCount();
        fdTreeArray = new FDTreeArray(attributeNum);
        FDTreeArray.FDTreeNode fdRoot = fdTreeArray.getRoot();
        ODTree odResult = new ODTree(attributeNum);

        //处理fd根节点和level2的节点
        processFDRootAndLevel2(data,fdRoot);
    System.out.println("处理完fd根节点和level2的节点");
        //od第二层的所有结点的direction为UP
        processLevel1AddLevel2ODNode(data,odResult,odReference);
    System.out.println("处理完od第一层节点,添加第二层进入队列");

        BFSFirstTime(data, odResult);
        odTree = odResult;
    }

    public void continueFirstTimes(DataFrame data, ODTree odReference){
      System.out.println("--continueFirstTimes--");
        disposeThreshold();
      System.out.println("fdReturnThreshold: " + fdReturnThreshold);
      System.out.println("odReturnThreshold: " + odReturnThreshold);
      System.out.println("newFoundFdCount: "+newFoundFdCount);
      System.out.println("newFoundOdCount: "+ newFoundOdCount);
        BFSFirstTime(data, odReference);
        odTree = odReference;
    }

    public void BFSFirstTime(DataFrame data, ODTree odResult){
        while (!queue.isEmpty()){
            System.out.println("--处理第" + (firstLever++) +"层--");
            int size = queue.size();
            for(int i = 0; i < size; i++){
                QueueElementArray element = queue.poll();
                //断言
                assert element != null;
                if(element.flag == QueueElementArray.FD){
                    FDDiscoverNodeSavingInfoArray fdInfo = element.fdInfo;
                    FDTreeArray.FDTreeNode fdParent = fdInfo.nodeInResultTree;
                    disposeFDNode(fdParent, data, fdInfo);
                }
                else if(element.flag == QueueElementArray.OD) {
                    ODDiscovererNodeSavingInfo odInfo = element.odInfo;
                    ODTree.ODTreeNode odParent = odInfo.nodeInResultTree;
                    disposeODNode(odResult, odParent, data, odInfo);
                }
            }
            System.out.println("FdCount： " + newFoundFdCount);
            System.out.println("OdCount： " + newFoundOdCount);
            if(newFoundFdCount >= fdReturnThreshold || newFoundOdCount >= odReturnThreshold){
                if(newFoundFdCount >= fdReturnThreshold){
                    System.out.println("****first发现阶段fd中断发生****");
                    isFDInterrup = true;
                }
                if(newFoundOdCount >= odReturnThreshold){
                    System.out.println("****first发现阶段od中断发生****");
                    isODInterrup = true;
                }
                return;
            }

        }
        isFDInterrup = false;
        isODInterrup = false;
        isFirstDone = true;
        //first没有中断的进行完，将状态设为START_AFTER，开始从头执行after算法
        interruptStatus = InterruptStatus.START_AFTER;
    }

    public void discoverAfterValidate(DataFrame data, ODTree odReference, FDTreeArray fdReference){
        System.out.println("-----after------");
        init();
        System.out.println("fdReturnThreshold: " + fdReturnThreshold);
        System.out.println("odReturnThreshold: " + odReturnThreshold);
        System.out.println("newFoundFdCount: "+newFoundFdCount);
        System.out.println("newFoundOdCount: "+ newFoundOdCount);

        int attributeNum = data.getColumnCount();
        fdTreeArray = fdReference;
        FDTreeNode fdRoot = fdTreeArray.getRoot();
        ODTree odResult = new ODTree(attributeNum);
        odMinimalChecker = new ODMinimalCheckerBruteForce();

        processFDRootAndLevel2AfterValidate(data, fdRoot);
        //第二层的所有结点的direction为UP
        processLevel1AddLevel2ODNode(data,odResult,odReference);
        BFSAfter(data,odResult);
        odTree = odResult;
    }

    public void continueAfterValidate(DataFrame data, ODTree odReference, FDTreeArray fdReference){
      System.out.println("--continueAfter--");
        disposeThreshold();
      System.out.println("fdReturnThreshold: " + fdReturnThreshold);
      System.out.println("odReturnThreshold: " + odReturnThreshold);
      System.out.println("newFoundFdCount: "+newFoundFdCount);
      System.out.println("newFoundOdCount: "+ newFoundOdCount);
        BFSAfter(data, odReference);
        odTree = odReference;
    }

    public void BFSAfter(DataFrame data, ODTree odResult){
        while(!queue.isEmpty()){
      System.out.println("--处理第" + (afterLever++) +"层--");
            int size = queue.size();
            for(int i = 0; i < size; i++) {
                QueueElementArray element = queue.poll();
                //断言
                assert element != null;
                if (element.flag == QueueElementArray.FD) {
                    FDDiscoverNodeSavingInfoArray info = element.fdInfo;
                    FDTreeNode parent = info.nodeInResultTree;
                    //处理该parent节点中本来就存在的子节点
                    for (FDTreeNode child : parent.children) {
                        FDDiscoverNodeSavingInfoArray infoArray = reTraverseNode(child, parent, info, data, false);
                        queue.add(new QueueElementArray(infoArray, QueueElementArray.FD));
                    }
                    //在补充并处理需要加上子节点
                    if (info.hasChildren) {
                        for (FDTreeNode child : info.newChildren) {
                            FDDiscoverNodeSavingInfoArray infoArray = vaildateNode(child, data, info, parent);
                            queue.offer(new QueueElementArray(infoArray, QueueElementArray.FD));
                        }
                    }
                } else {
                    ODDiscovererNodeSavingInfo info = element.odInfo;
                    ODTreeNode parent = info.nodeInResultTree;
                    disposeODNode(odResult, parent, data, info);
                }
            }
      System.out.println("FdCount： " + newFoundFdCount);
      System.out.println("OdCount： " + newFoundOdCount);
            if(newFoundFdCount >= fdReturnThreshold || newFoundOdCount >= odReturnThreshold){
                if(newFoundFdCount >= fdReturnThreshold){
      System.out.println("****after发现阶段fd中断发生****");
                    isFDInterrup = true;
                }
                if(newFoundOdCount >= odReturnThreshold){
      System.out.println("****after发现阶段od中断发生****");
                    isODInterrup = true;
                }
                return;
            }
        }
        isFDInterrup = false;
        isODInterrup = false;
    }

    public void disposeFDNode(FDTreeNode parent, DataFrame data, FDDiscoverNodeSavingInfoArray info){
        //prune规则2  key剪枝
        if(trueRHSCounts(parent.RHSCandidate) != data.getColumnCount()){
            //设attribute set中的attribute升序排列
            //生成子节点，当parent的attribute一定时，其生成的子节点的attribute从比他大开始（避免重复12，21的情况），比如说当属性为2时，则只生成3，4，5
            for(int i = parent.attribute + 1; i < data.getColumnCount(); i++){
                String I = String.valueOf(i);
                List<Integer> left = info.listDeepClone(info.left);
                //prune规则4     XY->Z,则XYZ->M否由XY->M否决定，XY->M成立则不是最小，XY->M不成立则不成立
                if(hasSubSet(left,i,fdCandidates)){
                    continue;
                }
                FDDiscoverNodeSavingInfoArray infoArray = newAndTraverseNode(i,data,parent,attributeToConfirmed.get(I),false, info);
                queue.offer(new QueueElementArray(infoArray, QueueElementArray.FD));
            }
        }
    }

    public void disposeODNode(ODTree odResult, ODTreeNode parent, DataFrame data,
                              ODDiscovererNodeSavingInfo info){
        int attributeNum = data.getColumnCount();
        /*
            数据仓库版改动部分
         */
        List<AttributeAndDirection> left = cloneUtil.deepCloneList(info.left);
        List<AttributeAndDirection> right = cloneUtil.deepCloneList(info.right);

        for(int attribute = 0; attribute < attributeNum * 2; attribute++){
            ODTree.ODTreeNode child;
            if(parent.children[attribute] == null)
                //new child的时候会把child挂在parent的children[]里 -- 建立parent和child的联系
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
            /*
                数据仓库版改动部分
             */
//            odTreeNodeEquivalenceClasses.mergeNode(child, data);
            List<AttributeAndDirection> newLeft = cloneUtil.deepCloneList(left);
            List<AttributeAndDirection> newRight = cloneUtil.deepCloneList(right);
            if (parent.status == ODTree.ODTreeNodeStatus.VALID) {
                if (attribute < attributeNum)
                    newRight.add(AttributeAndDirection.getInstance(attribute, AttributeAndDirection.UP));
                else
                    newRight.add(AttributeAndDirection.getInstance(attribute - attributeNum, AttributeAndDirection.DOWN));
                odTreeNodeEquivalenceClasses.findAndHandleEquivalenceClass(child, dataWareHouse.listEcMap, newRight, data);
            } else if (parent.status == ODTree.ODTreeNodeStatus.SPLIT) {
                if (attribute < attributeNum)
                    newLeft.add(AttributeAndDirection.getInstance(attribute, AttributeAndDirection.UP));
                else
                    newLeft.add(AttributeAndDirection.getInstance(attribute - attributeNum, AttributeAndDirection.DOWN));
                odTreeNodeEquivalenceClasses.findAndHandleEquivalenceClass(child, dataWareHouse.listEcMap, newLeft, data);
            }

            if(!child.confirm)
                child.status = odTreeNodeEquivalenceClasses.check(data).status;
            if(child.status == ODTree.ODTreeNodeStatus.VALID){
                odMinimalChecker.insert(childCandidate);
                newFoundOdCount++;
            }
            if(child.status != ODTree.ODTreeNodeStatus.SWAP){
                ODDiscovererNodeSavingInfo nodeSavingInfo = new ODDiscovererNodeSavingInfo(child, odTreeNodeEquivalenceClasses,newLeft,newRight);
                queue.offer(new QueueElementArray(nodeSavingInfo, QueueElement.OD));
            }
        }
    }

    public void processLevel1AddLevel2ODNode(DataFrame data, ODTree odResult, ODTree odReference){
        int attributeNum = data.getColumnCount();
        for (int attribute = 0; attribute < attributeNum; attribute++) {
            if(odReference!=null) {
                cloneUtil.copyConfirmNode(odResult, odResult.getRoot().children[attribute]
                        , odReference.getRoot().children[attribute]);
            }
            //fd的PLI放入数据仓库版改动
            List<AttributeAndDirection> left = new ArrayList<>();
            List<AttributeAndDirection> right = new ArrayList<>();
            right.add(AttributeAndDirection.getInstance(attribute, AttributeAndDirection.UP));
            //todo 第一层的merge也可以从map中找
            ODTreeNodeEquivalenceClasses odTreeNodeEquivalenceClasses = new ODTreeNodeEquivalenceClasses();
            odTreeNodeEquivalenceClasses.mergeNode(odResult.getRoot().children[attribute], data);
            //生成第二层的节点
            ODTreeNode odParent = odResult.getRoot().children[attribute];
            ODDiscovererNodeSavingInfo odInfo = new ODDiscovererNodeSavingInfo(odParent, odTreeNodeEquivalenceClasses,left,right);
            disposeODNode(odResult, odParent, data, odInfo);
            //直接将第一层的节点放入queue，现弃用
//            queue.offer(new QueueElementArray(
//                    new ODDiscovererNodeSavingInfo(odResult.getRoot().children[attribute], odTreeNodeEquivalenceClasses,left,right),
//                    QueueElementArray.OD));
        }
    }

    public void processFDRootAndLevel2(DataFrame data, FDTreeArray.FDTreeNode root){
        int attributeNum = data.getColumnCount();
        //根节点
        for(int i = 0; i < attributeNum; i++){
            FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses = new FDTreeNodeEquivalenceClasses();
            FDValidationResult fdResult = fdTreeNodeEquivalenceClasses.checkFDRefinement(i,data);
            if(fdResult.status.equals("valid")){
                String I = String.valueOf(i);
                //阈值中断理念
                newFoundFdCount++;
                root.RHSCandidate[i] = true;
                fdCandidates.add(new FDCandidate(new ArrayList<>(), i, root));
                //加入map（map用来minimal list检查）
                List<List<Integer>> allLeft = new ArrayList<>();
                List<Integer> nowLeft = new ArrayList<>();
                allLeft.add(nowLeft);
                fdMap.put(I, allLeft);
            }
        }
        //第二层
        for(int i = 0; i < attributeNum; i++){
            FDDiscoverNodeSavingInfoArray infoArray = newAndTraverseNode(i, data, root, null,
                    true, null);
            queue.add(new QueueElementArray(infoArray,QueueElement.FD));
        }
    }

    public void processFDRootAndLevel2AfterValidate(DataFrame data, FDTreeNode root){
        int attributeNum = data.getColumnCount();
        for(int i = 0; i < attributeNum; i++){
            if(root.RHSCandidate[i]){
                String I = String.valueOf(i);
                //存在[]->x,需要重新检查
                FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses = new FDTreeNodeEquivalenceClasses();
                FDValidationResult fdResult = fdTreeNodeEquivalenceClasses.checkFDRefinement(i,data);
                if(fdResult.status.equals("non-valid")){
                    //结果变为non-valid,子节点中所有rhs[i]变为false
                    root.RHSCandidate[i] = false;
                }else {
                    //阈值中断理念
                    newFoundFdCount++;
                    fdCandidates.add(new FDCandidate(new ArrayList<>(), i, root));
                    List<List<Integer>> allLeft = new ArrayList<>();
                    List<Integer> nowLeft = new ArrayList<>();
                    allLeft.add(nowLeft);
                    fdMap.put(I, allLeft);
                }
            }
        }

        /*先手动做第二层，得到map，完善后面的剪枝*/
        for(FDTreeNode child : root.children){
            FDDiscoverNodeSavingInfoArray infoArray = reTraverseNode(child, root, null, data, true);
            queue.add(new QueueElementArray(infoArray, QueueElementArray.FD));
        }
    }

    public void init(){
        queue.clear();
        fdCandidates.clear();
        dataWareHouse.listEcMap.clear();
        fdMap.clear();
        newFoundFdCount = 0;
        newFoundOdCount = 0;
        firstLever = 3;
        afterLever = 3;
        disposeThreshold();
    }

    public void disposeThreshold(){
        if(isFDInterrup) fdReturnThreshold*=2;
        if(isODInterrup) odReturnThreshold*=2;
    }

    enum InterruptStatus
    {
        START_FIRST(0,"从头执行First算法"),
        CONTINUE_FIRST(1,"恢复执行当前First算法"),
        START_AFTER(2,"从头执行After算法"),
        CONTINUE_AFTER(3,"恢复执行After算法"),
        END(4,"执行完毕");


        private final int code;
        private final String status;

        InterruptStatus(int code, String status){
            this.code = code;
            this.status = status;
        }

        public int getCode() {
            return code;
        }

        public String getStatus() {
            return status;
        }
    }

    public boolean getIsFirstDone(){
        return this.isFirstDone;
    }

    public boolean getIsInterrup(){
        return this.isFDInterrup || this.isODInterrup;
    }
}
