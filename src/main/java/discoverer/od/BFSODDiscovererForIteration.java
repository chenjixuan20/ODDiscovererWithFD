package discoverer.od;

import dataStructures.DataFrame;
import dataStructures.fd.FDCandidate;
import dataStructures.od.ODCandidate;
import dataStructures.od.ODTree;
import dataStructures.od.ODTree.ODTreeNode;
import dataStructures.od.ODTreeNodeEquivalenceClasses;
import minimal.ODMinimalCheckTree;
import minimal.ODMinimalChecker;
import util.Timer;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * 直接计算SortedPatition版
 */
public class BFSODDiscovererForIteration extends ODDiscoverer{
    public static final int INITIAL_RETURN_THRESHOLD =200;
    private boolean complete=true;
    private Queue<ODDiscovererNodeSavingInfo> queue;
    private ODMinimalChecker odMinimalChecker;
    private int returnThreshold;

    public boolean isComplete() {
        return complete;
    }

    public ODTree restartDiscovering(DataFrame data,ODTree reference){
        System.out.println("restartDiscovering");
        odMinimalChecker=new ODMinimalCheckTree(data.getColumnCount());
//        odMinimalChecker=new ODMinimalCheckerBruteForce();
        queue=new LinkedList<>();
        ODTree result=new ODTree(data.getColumnCount());

        returnThreshold= INITIAL_RETURN_THRESHOLD;
        int attributeCount=data.getColumnCount();

        for (int attribute = 0; attribute < attributeCount; attribute++) {
            if(reference!=null) {
                copyConfirmNode(result, result.getRoot().children[attribute]
                        , reference.getRoot().children[attribute]);
            }
            ODTreeNodeEquivalenceClasses odTreeNodeEquivalenceClasses = new ODTreeNodeEquivalenceClasses();
            odTreeNodeEquivalenceClasses.mergeNode(result.getRoot().children[attribute], data);
            queue.offer(new ODDiscovererNodeSavingInfo(result.getRoot().children[attribute]
                    , odTreeNodeEquivalenceClasses));
        }
        BFSTraversing(data,result);
        return result;
    }

    public ODTree continueDiscovering(DataFrame data,ODTree tree){
        System.out.println("continueDiscovering");
        System.out.println(tree);
        returnThreshold*=2;
        BFSTraversing(data, tree);
        return tree;
    }

    @Override
    public ODTree discover(DataFrame data, ODTree reference) {
        System.out.println(reference);
        if (complete)
            return restartDiscovering(data,reference);
        else
            return continueDiscovering(data,reference);
    }

    private void BFSTraversing(DataFrame data,ODTree result){
        System.out.println("BFSTraversing");
        System.out.println(result);
        int attributeCount=data.getColumnCount();
        int newFoundOdCount=0;
        while (!queue.isEmpty()) {

            ODDiscovererNodeSavingInfo info=queue.poll();
            ODTreeNode parent=info.nodeInResultTree;

            for (int attribute = 0; attribute < attributeCount*2; attribute++) {
                ODTreeNode child;
                if(parent.children[attribute]==null)
                    child=result.new ODTreeNode(parent,result.getAttributeAndDirectionFromIndex(attribute));
                else
                    child=parent.children[attribute];
                ODCandidate childCandidate=new ODCandidate(child);
                child.minimal=odMinimalChecker.isCandidateMinimal(childCandidate);
                if(!child.minimal)
                    continue;
                ODTreeNodeEquivalenceClasses odTreeNodeEquivalenceClasses =
                        info.odTreeNodeEquivalenceClasses.deepClone();
                odTreeNodeEquivalenceClasses.mergeNode(child,data);
                if(!child.confirm)
                    child.status=odTreeNodeEquivalenceClasses.check(data).status;
                if(child.status!= ODTree.ODTreeNodeStatus.SWAP){
                    queue.offer(new ODDiscovererNodeSavingInfo(child
                            ,odTreeNodeEquivalenceClasses));
                }
                if(child.status== ODTree.ODTreeNodeStatus.VALID){
                    odMinimalChecker.insert(childCandidate);
                    if(!child.confirm){
                        newFoundOdCount++;
                    }
                }
            }
            if(newFoundOdCount>=returnThreshold){
                complete=false;
                return;
            }
        }
        complete=true;
    }

    private void copyConfirmNode(ODTree resultTree, ODTreeNode resultTreeNode, ODTreeNode referenceTreeNode){
        for (ODTreeNode referenceChildNode:referenceTreeNode.children) {
            if(referenceChildNode!=null && referenceChildNode.confirm){
                ODTreeNode resultChildNode =resultTree.new ODTreeNode
                        (resultTreeNode,referenceChildNode.attribute);
                resultChildNode.status = referenceChildNode.status;
                resultChildNode.confirm();
                copyConfirmNode(resultTree,resultChildNode,referenceChildNode);
            }
        }
    }

    @Override
    public ODTree discoverFD(DataFrame data, List<FDCandidate> fdCandidates) {
        return null;
    }

    public static void main(String[] args) {
        DataFrame data = DataFrame.fromCsv("Data/FLI 1000.csv");
        util.Timer timer = new Timer();
        BFSODDiscovererForIteration discoverer = new BFSODDiscovererForIteration();
        ODTree discover = discoverer.discover(data,null);
        System.out.println(timer.getTimeUsed() / 1000.0 + "s");
        List<ODCandidate> ods = discover.getAllOdsOrderByBFS();
        System.out.println(ods);
        System.out.println("发现od数量："+ods.size());

        System.out.println(discoverer.complete);
        System.out.println(discoverer.queue.size());
        discover = discoverer.discover(data,discover);
         ods = discover.getAllOdsOrderByBFS();
        System.out.println(ods);
        System.out.println("发现od数量："+ods.size());
    }
}
