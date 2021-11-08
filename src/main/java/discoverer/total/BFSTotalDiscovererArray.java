package discoverer.total;

import dataStructures.DataFrame;
import dataStructures.fd.Array.FDTreeArray;
import dataStructures.fd.Array.FDTreeArray.FDTreeNode;
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
import util.Timer;

import java.util.*;

/**
 * 使用数据仓库记录pli,先查再算
 */
public class BFSTotalDiscovererArray extends FDDiscoverer {
    public Map<String,List<List<Integer>>> fdMap = new HashMap<>();
    public ODTree odTree;
    Queue<QueueElementArray> queue = new LinkedList<>();

    public void discoverFirstTimes(DataFrame data, ODTree odReference){
        clear();
        int attributeNum = data.getColumnCount();
        fdTreeArray = new FDTreeArray(attributeNum);
        FDTreeNode fdRoot = fdTreeArray.getRoot();
        ODTree odResult = new ODTree(attributeNum);
        ODMinimalChecker odMinimalChecker = new ODMinimalCheckerBruteForce();

        //处理fd根节点和level1的节点
        processFDRootAndLevel2(data,fdRoot);
        System.out.println("处理完fd根节点和level1的节点");

        //od第二层的所有结点的direction为UP
        addLevel1ODNode(data,odResult,odReference);
        System.out.println("处理完od第1层节点");

        while (!queue.isEmpty()){
            QueueElementArray element = queue.poll();
            if(element.flag == QueueElementArray.FD){
                FDDiscoverNodeSavingInfoArray fdInfo = element.fdInfo;
                FDTreeNode fdParent = fdInfo.nodeInResultTree;
                disposeFDNode(fdParent, data, fdInfo);
            }
            else {
                ODDiscovererNodeSavingInfo odInfo = element.odInfo;
                ODTreeNode odParent = odInfo.nodeInResultTree;
                disposeODNode(odResult, odParent, data, odInfo, odMinimalChecker);
            }
        }
        odTree = odResult;
    }

    public void discoverAfterValidate(DataFrame data, ODTree odReference, FDTreeArray fdReference){
        clear();
        int attributeNum = data.getColumnCount();
        fdTreeArray = fdReference;
        FDTreeNode fdRoot = fdTreeArray.getRoot();
        ODTree odResult = new ODTree(attributeNum);
        ODMinimalChecker odMinimalChecker = new ODMinimalCheckerBruteForce();

        processFDRootAndLevel2AfterValidate(data, fdRoot);
        //第二层的所有结点的direction为UP
        addLevel1ODNode(data,odResult,odReference);

        while(!queue.isEmpty()){
            QueueElementArray element = queue.poll();
            if(element.flag == QueueElementArray.FD){
                FDDiscoverNodeSavingInfoArray info = element.fdInfo;
                FDTreeNode parent = info.nodeInResultTree;

                //处理该parent节点中本来就存在的子节点
                for(FDTreeNode child : parent.children){
                    FDDiscoverNodeSavingInfoArray infoArray = reTraverseNode(child, parent, info, data, false);
                    queue.add(new QueueElementArray(infoArray, QueueElementArray.FD));

                }
                //在补充并处理需要加上子节点
                if(info.hasChildren){
                    for(FDTreeNode child : info.newChildren){
                        FDDiscoverNodeSavingInfoArray infoArray= vaildateNode(child,data,info,parent);
                        queue.offer(new QueueElementArray(infoArray, QueueElementArray.FD));
                    }
                }
            }else {
                ODDiscovererNodeSavingInfo info = element.odInfo;
                ODTreeNode parent = info.nodeInResultTree;
                disposeODNode(odResult, parent, data, info, odMinimalChecker);
            }
        }
        odTree = odResult;
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
                              ODDiscovererNodeSavingInfo info, ODMinimalChecker odMinimalChecker){
        int attributeNum = data.getColumnCount();
        /*
            数据仓库版改动部分
         */
        List<AttributeAndDirection> left = info.listDeepClone(info.left);
        List<AttributeAndDirection> right = info.listDeepClone(info.right);
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
            /*
                数据仓库版改动部分
             */
//            odTreeNodeEquivalenceClasses.mergeNode(child, data);
            List<AttributeAndDirection> newLeft = cloneAttributeAndDirectionList(left);
            List<AttributeAndDirection> newRight = cloneAttributeAndDirectionList(right);
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
            }
            if(child.status != ODTree.ODTreeNodeStatus.SWAP){
                ODDiscovererNodeSavingInfo nodeSavingInfo = new ODDiscovererNodeSavingInfo(child, odTreeNodeEquivalenceClasses,newLeft,newRight);
                queue.offer(new QueueElementArray(nodeSavingInfo, QueueElement.OD));
            }
        }
    }

    public void addLevel1ODNode(DataFrame data, ODTree odResult, ODTree odReference){
        int attributeNum = data.getColumnCount();
        for (int attribute = 0; attribute < attributeNum; attribute++) {
            if(odReference!=null) {
                copyConfirmNode(odResult, odResult.getRoot().children[attribute]
                        , odReference.getRoot().children[attribute]);
            }
            /*
                fd的PLI放入数据仓库版改动
             */
            List<AttributeAndDirection> left = new ArrayList<>();
            List<AttributeAndDirection> right = new ArrayList<>();
            right.add(AttributeAndDirection.getInstance(attribute, AttributeAndDirection.UP));
            //todo 第一层的merge也可以从map中找
            ODTreeNodeEquivalenceClasses odTreeNodeEquivalenceClasses = new ODTreeNodeEquivalenceClasses();
            odTreeNodeEquivalenceClasses.mergeNode(odResult.getRoot().children[attribute], data);
//            System.out.println(odTreeNodeEquivalenceClasses);

            queue.offer(new QueueElementArray(
                    new ODDiscovererNodeSavingInfo(odResult.getRoot().children[attribute], odTreeNodeEquivalenceClasses,left,right),
                    QueueElementArray.OD));
        }
    }

    public void processFDRootAndLevel2(DataFrame data, FDTreeNode root){
        int attributeNum = data.getColumnCount();
        for(int i = 0; i < attributeNum; i++){
            FDTreeNodeEquivalenceClasses fdTreeNodeEquivalenceClasses = new FDTreeNodeEquivalenceClasses();
            FDValidationResult fdResult = fdTreeNodeEquivalenceClasses.checkFDRefinement(i,data);
            if(fdResult.status.equals("valid")){
                String I = String.valueOf(i);
                root.RHSCandidate[i] = true;
                fdCandidates.add(new FDCandidate(new ArrayList<>(), i, root));
                List<List<Integer>> allLeft = new ArrayList<>();
                List<Integer> nowLeft = new ArrayList<>();
                allLeft.add(nowLeft);
                fdMap.put(I, allLeft);
            }
        }
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

    @Override
    public void addFDCandidate(FDTreeNode node, int attributeNum, List<Integer> left) {
        for(int j = 0; j < attributeNum; j++){
            if(node.minimal[j]){
                String J = String.valueOf(j);
                fdCandidates.add(new FDCandidate(left, j, node));
                if(!fdMap.containsKey(J)){
                    List<List<Integer>> allLeft = new ArrayList<>();
                    List<Integer> nowLeft = deepClone(left);
                    allLeft.add(nowLeft);
                    fdMap.put(J, allLeft);
                }else {
                    List<List<Integer>> allLeft = fdMap.get(J);
                    List<Integer> nowLeft = deepClone(left);
                    allLeft.add(nowLeft);
                    fdMap.put(J, allLeft);
                }
            }
        }
    }

    public void clear(){
        fdCandidates.clear();
        dataWareHouse.listEcMap.clear();
        fdMap.clear();
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

    public static List<AttributeAndDirection> cloneAttributeAndDirectionList(List<AttributeAndDirection> list) {
        return new ArrayList<>(list);
    }

    public static void main(String[] args) {
        DataFrame data = DataFrame.fromCsv("Data/test.csv");
        BFSTotalDiscovererArray discoverer = new BFSTotalDiscovererArray();
        Timer timer = new Timer();
        discoverer.discoverFirstTimes(data,null);
        System.out.println(timer.getTimeUsedAndReset() / 1000.0 + "s");
        System.out.println(discoverer.fdCandidates.size());
        System.out.println(discoverer.odTree.getAllOdsOrderByBFS().size());
//        for(String key : dataWareHouse.listEcMap.keySet()){
//            System.out.println(key + " " + dataWareHouse.listEcMap.get(key));
//        }
    }
}
