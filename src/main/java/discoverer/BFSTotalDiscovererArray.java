package discoverer;

import dataStructures.DataFrame;
import dataStructures.fd.Array.FDTreeArray;
import dataStructures.fd.Array.FDTreeArray.FDTreeNode;
import dataStructures.fd.FDCandidate;
import dataStructures.fd.FDTree;
import dataStructures.fd.FDTreeNodeEquivalenceClasses;
import dataStructures.fd.FDValidationResult;
import dataStructures.od.ODCandidate;
import dataStructures.od.ODTree;
import dataStructures.od.ODTree.ODTreeNode;
import dataStructures.od.ODTreeNodeEquivalenceClasses;
import discoverer.fd.Array.FDDiscoverNodeSavingInfoArray;
import discoverer.fd.FDDiscoverer;
import discoverer.od.ODDiscovererNodeSavingInfo;
import minimal.ODMinimalChecker;
import minimal.ODMinimalCheckerBruteForce;

import java.util.*;

public class BFSTotalDiscovererArray extends FDDiscoverer {
    public Map<Integer,List<List<Integer>>> fdMap = new HashMap<>();
    public ODTree odTree;
    Queue<QueueElementArray> queue = new LinkedList<>();

    public void discoverFirstTimes(DataFrame data, ODTree reference){
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

    public void discoverAfterValidate(DataFrame data, ODTree odReference, FDTreeArray fdReference){
        fdCandidates.clear();
        fdMap.clear();
        int attributeNum = data.getColumnCount();
        fdTreeArray = fdReference;
        FDTreeNode fdRoot = fdTreeArray.getRoot();
        ODTree odResult = new ODTree(attributeNum);
        ODMinimalChecker odMinimalChecker = new ODMinimalCheckerBruteForce();

        processFDRootAndLevel1AfterValidate(data, fdRoot);
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

    public void processFDRootAndLevel1(DataFrame data, FDTreeNode root){
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

    public void processFDRootAndLevel1AfterValidate(DataFrame data, FDTreeNode root){
        int attributeNum = data.getColumnCount();
        for(int i = 0; i < attributeNum; i++){
            if(root.RHSCandidate[i]){
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
                    fdMap.put(i, allLeft);
                }
            }
        }

        /*先手动做第一层，得到map，完善后面的剪枝*/
        for(FDTreeNode child : root.children){
            FDDiscoverNodeSavingInfoArray infoArray = reTraverseNode(child, root, null, data, true);
            queue.add(new QueueElementArray(infoArray, QueueElementArray.FD));
        }
    }

    @Override
    public void addFDCandidate(FDTreeNode node, int attributeNum, List<Integer> left) {
        for(int j = 0; j < attributeNum; j++){
            if(node.minimal[j]){
                fdCandidates.add(new FDCandidate(left, j, node));
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
