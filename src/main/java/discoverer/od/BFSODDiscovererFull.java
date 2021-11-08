package discoverer.od;

import dataStructures.DataFrame;
import dataStructures.fd.FDCandidate;
import dataStructures.od.AttributeAndDirection;
import dataStructures.od.ODCandidate;
import dataStructures.od.ODTree;
import dataStructures.od.ODTree.ODTreeNode;
import dataStructures.od.ODTreeNodeEquivalenceClasses;
import discoverer.fd.Array.DiscoverResult;
import discoverer.fd.FDToODSavingInfo;
import discoverer.total.BFSTotalDiscovererArray;
import minimal.ODMinimalChecker;
import minimal.ODMinimalCheckerBruteForce;

import java.util.*;

public class BFSODDiscovererFull extends ODDiscoverer {

    public static List<AttributeAndDirection> cloneAttributeAndDirectionList(List<AttributeAndDirection> list) {
        List<AttributeAndDirection> newList = new ArrayList<>(list);
        return newList;
    }

    @Override
    public ODTree discover(DataFrame data, ODTree reference) {
        Queue<ODDiscovererNodeSavingInfo> queue = new LinkedList<>();
        int attributeCount = data.getColumnCount();
        ODTree result = new ODTree(attributeCount);
        ODMinimalChecker odMinimalChecker = new ODMinimalCheckerBruteForce();

        //第二层的所有结点的direction为UP
        for (int attribute = 0; attribute < attributeCount; attribute++) {
            if(reference!=null) {
                copyConfirmNode(result, result.getRoot().children[attribute]
                        , reference.getRoot().children[attribute]);
            }
            ODTreeNodeEquivalenceClasses odTreeNodeEquivalenceClasses = new ODTreeNodeEquivalenceClasses();
            odTreeNodeEquivalenceClasses.mergeNode(result.getRoot().children[attribute], data);
            queue.offer(new ODDiscovererNodeSavingInfo(result.getRoot().children[attribute], odTreeNodeEquivalenceClasses));
        }

        while (!queue.isEmpty()) {
            ODDiscovererNodeSavingInfo info = queue.poll();
            ODTree.ODTreeNode parent = info.nodeInResultTree;
            for (int attribute = 0; attribute < attributeCount * 2; attribute++) {
                ODTree.ODTreeNode child;
                if (parent.children[attribute] == null)
                    child = result.new ODTreeNode(parent, result.getAttributeAndDirectionFromIndex(attribute));
                else
                    child = parent.children[attribute];
                ODCandidate childCandidate = new ODCandidate(child);
                child.minimal = odMinimalChecker.isCandidateMinimal(childCandidate);
                if (!child.minimal)
                    continue;
                ODTreeNodeEquivalenceClasses odTreeNodeEquivalenceClasses =
                        info.odTreeNodeEquivalenceClasses.deepClone();
                odTreeNodeEquivalenceClasses.mergeNode(child, data);
                if (!child.confirm)
                    child.status = odTreeNodeEquivalenceClasses.check(data).status;
                if (child.status == ODTree.ODTreeNodeStatus.VALID) {
                    odMinimalChecker.insert(childCandidate);
                }
                if (child.status != ODTree.ODTreeNodeStatus.SWAP) {
                    queue.offer(new ODDiscovererNodeSavingInfo(child, odTreeNodeEquivalenceClasses));
                }
            }
        }
        return result;
    }

    @Override
    public ODTree discoverFD(DataFrame data, List<FDCandidate> fdCandidates) {
        Queue<ODDiscovererNodeSavingInfo> queue = new LinkedList<>();
        int attributeCount = data.getColumnCount();
        ODTree reslut = new ODTree(attributeCount);
        ODMinimalChecker odMinimalChecker = new ODMinimalCheckerBruteForce();

        //第二层的所有结点的direction为UP
        for (int attribute = 0; attribute < attributeCount; attribute++) {
            ODTreeNodeEquivalenceClasses odTreeNodeEquivalenceClasses = new ODTreeNodeEquivalenceClasses();
            odTreeNodeEquivalenceClasses.mergeNode(reslut.getRoot().children[attribute], data);
            queue.offer(new ODDiscovererNodeSavingInfo(reslut.getRoot().children[attribute], odTreeNodeEquivalenceClasses));
        }

        while (!queue.isEmpty()) {
            ODDiscovererNodeSavingInfo info = queue.poll();
            ODTree.ODTreeNode parent = info.nodeInResultTree;
            for (int attribute = 0; attribute < attributeCount * 2; attribute++) {
                ODTree.ODTreeNode child;
                if (parent.children[attribute] == null)
                    child = reslut.new ODTreeNode(parent, reslut.getAttributeAndDirectionFromIndex(attribute));
                else
                    child = parent.children[attribute];
                ODCandidate childCandidate = new ODCandidate(child);
                child.minimal = odMinimalChecker.isCandidateMinimalFD(childCandidate, fdCandidates);
                if (!child.minimal)
                    continue;
                ODTreeNodeEquivalenceClasses odTreeNodeEquivalenceClasses =
                        info.odTreeNodeEquivalenceClasses.deepClone();
                odTreeNodeEquivalenceClasses.mergeNode(child, data);
                if (!child.confirm)
                    child.status = odTreeNodeEquivalenceClasses.check(data).status;
                if (child.status == ODTree.ODTreeNodeStatus.VALID) {
                    odMinimalChecker.insert(childCandidate);
                }
                if (child.status != ODTree.ODTreeNodeStatus.SWAP) {
                    queue.offer(new ODDiscovererNodeSavingInfo(child, odTreeNodeEquivalenceClasses));
                }
            }
        }
        return reslut;
    }

    public ODTree discover(DataFrame data) {
        Queue<ODDiscovererNodeSavingInfo> queue = new LinkedList<>();
        int attributeCount = data.getColumnCount();
        ODTree reslut = new ODTree(attributeCount);
        ODMinimalChecker odMinimalChecker = new ODMinimalCheckerBruteForce();

        //第二层的所有结点的direction为UP
        for (int attribute = 0; attribute < attributeCount; attribute++) {
            ODTreeNodeEquivalenceClasses odTreeNodeEquivalenceClasses = new ODTreeNodeEquivalenceClasses();
            odTreeNodeEquivalenceClasses.mergeNode(reslut.getRoot().children[attribute], data);
            queue.offer(new ODDiscovererNodeSavingInfo(reslut.getRoot().children[attribute], odTreeNodeEquivalenceClasses));
        }

        while (!queue.isEmpty()) {
            ODDiscovererNodeSavingInfo info = queue.poll();
            ODTree.ODTreeNode parent = info.nodeInResultTree;
            for (int attribute = 0; attribute < attributeCount * 2; attribute++) {
                ODTree.ODTreeNode child;
                if (parent.children[attribute] == null)
                    child = reslut.new ODTreeNode(parent, reslut.getAttributeAndDirectionFromIndex(attribute));
                else
                    child = parent.children[attribute];
                ODCandidate childCandidate = new ODCandidate(child);
                child.minimal = odMinimalChecker.isCandidateMinimal(childCandidate);
                if (!child.minimal)
                    continue;
                ODTreeNodeEquivalenceClasses odTreeNodeEquivalenceClasses =
                        info.odTreeNodeEquivalenceClasses.deepClone();
                odTreeNodeEquivalenceClasses.mergeNode(child, data);
                if (!child.confirm)
                    child.status = odTreeNodeEquivalenceClasses.check(data).status;
                if (child.status == ODTree.ODTreeNodeStatus.VALID) {
                    odMinimalChecker.insert(childCandidate);
                }
                if (child.status != ODTree.ODTreeNodeStatus.SWAP) {
                    queue.offer(new ODDiscovererNodeSavingInfo(child, odTreeNodeEquivalenceClasses));
                }
            }
        }
        return reslut;
    }

    public ODTree discoverFDPlus(DataFrame data, FDToODSavingInfo infoTransfer) {
        Queue<ODDiscovererNodeSavingInfo> queue = new LinkedList<>();
        int attributeCount = data.getColumnCount();
        ODTree reslut = new ODTree(attributeCount);
        ODMinimalChecker odMinimalChecker = new ODMinimalCheckerBruteForce();

        //第二层的所有结点的direction为UP
        for (int attribute = 0; attribute < attributeCount; attribute++) {
            ODTreeNodeEquivalenceClasses odTreeNodeEquivalenceClasses = new ODTreeNodeEquivalenceClasses();
            odTreeNodeEquivalenceClasses.mergeNode(reslut.getRoot().children[attribute], data);
            /*
            改动部分
             */
            List<AttributeAndDirection> left = new ArrayList<>();
            List<AttributeAndDirection> right = new ArrayList<>();
            right.add(AttributeAndDirection.getInstance(attribute, AttributeAndDirection.UP));
            queue.offer(new ODDiscovererNodeSavingInfo(reslut.getRoot().children[attribute],
                    odTreeNodeEquivalenceClasses,
                    left,
                    right));
        }

        while (!queue.isEmpty()) {
            ODDiscovererNodeSavingInfo info = queue.poll();
            ODTree.ODTreeNode parent = info.nodeInResultTree;
            /*
            改动部分
             */
            List<AttributeAndDirection> left = info.listDeepClone(info.left);
            List<AttributeAndDirection> right = info.listDeepClone(info.right);

            for (int attribute = 0; attribute < attributeCount * 2; attribute++) {
                ODTree.ODTreeNode child;
                if (parent.children[attribute] == null)
                    child = reslut.new ODTreeNode(parent, reslut.getAttributeAndDirectionFromIndex(attribute));
                else
                    child = parent.children[attribute];
                ODCandidate childCandidate = new ODCandidate(child);
                child.minimal = odMinimalChecker.isCandidateMinimalFDPlus(childCandidate, infoTransfer);
                if (!child.minimal)
                    continue;
                ODTreeNodeEquivalenceClasses odTreeNodeEquivalenceClasses =
                        info.odTreeNodeEquivalenceClasses.deepClone();
                /*
                改动部分
                 */
//                odTreeNodeEquivalenceClasses.mergeNode(child, data);
                List<AttributeAndDirection> newLeft = cloneAttributeAndDirectionList(left);
                List<AttributeAndDirection> newRight = cloneAttributeAndDirectionList(right);
                if (parent.status == ODTree.ODTreeNodeStatus.VALID) {
                    if (attribute < attributeCount)
                        newRight.add(AttributeAndDirection.getInstance(attribute, AttributeAndDirection.UP));
                    else
                        newRight.add(AttributeAndDirection.getInstance(attribute - attributeCount, AttributeAndDirection.DOWN));
                    odTreeNodeEquivalenceClasses.findAndHandleEquivalenceClass(child, infoTransfer.attibuteListToEqcMap, newRight, data);
                } else if (parent.status == ODTree.ODTreeNodeStatus.SPLIT) {
                    if (attribute < attributeCount)
                        newLeft.add(AttributeAndDirection.getInstance(attribute, AttributeAndDirection.UP));
                    else
                        newLeft.add(AttributeAndDirection.getInstance(attribute - attributeCount, AttributeAndDirection.DOWN));
                    odTreeNodeEquivalenceClasses.findAndHandleEquivalenceClass(child, infoTransfer.attibuteListToEqcMap, newLeft, data);
                }

                if (!child.confirm)
                    child.status = odTreeNodeEquivalenceClasses.check(data).status;
                if (child.status == ODTree.ODTreeNodeStatus.VALID) {
                    odMinimalChecker.insert(childCandidate);
                }
                if (child.status != ODTree.ODTreeNodeStatus.SWAP) {
                    queue.offer(new ODDiscovererNodeSavingInfo(child, odTreeNodeEquivalenceClasses, newLeft, newRight));
                }
            }
        }
        return reslut;
    }

    public ODTree discoverFDPlusArray(DataFrame data, DiscoverResult infoTransfer) {
        Queue<ODDiscovererNodeSavingInfo> queue = new LinkedList<>();
        int attributeCount = data.getColumnCount();
        ODTree reslut = new ODTree(attributeCount);
        ODMinimalChecker odMinimalChecker = new ODMinimalCheckerBruteForce();

        //第二层的所有结点的direction为UP
        for (int attribute = 0; attribute < attributeCount; attribute++) {
            ODTreeNodeEquivalenceClasses odTreeNodeEquivalenceClasses = new ODTreeNodeEquivalenceClasses();
            odTreeNodeEquivalenceClasses.mergeNode(reslut.getRoot().children[attribute], data);
            /*
            改动部分
             */
            List<AttributeAndDirection> left = new ArrayList<>();
            List<AttributeAndDirection> right = new ArrayList<>();
            right.add(AttributeAndDirection.getInstance(attribute, AttributeAndDirection.UP));
            queue.offer(new ODDiscovererNodeSavingInfo(reslut.getRoot().children[attribute],
                    odTreeNodeEquivalenceClasses,
                    left,
                    right));
        }

        while (!queue.isEmpty()) {
            ODDiscovererNodeSavingInfo info = queue.poll();
            ODTree.ODTreeNode parent = info.nodeInResultTree;
            /*
            改动部分
             */
            List<AttributeAndDirection> left = info.listDeepClone(info.left);
            List<AttributeAndDirection> right = info.listDeepClone(info.right);

            for (int attribute = 0; attribute < attributeCount * 2; attribute++) {
                ODTree.ODTreeNode child;
                if (parent.children[attribute] == null)
                    child = reslut.new ODTreeNode(parent, reslut.getAttributeAndDirectionFromIndex(attribute));
                else
                    child = parent.children[attribute];
                ODCandidate childCandidate = new ODCandidate(child);
                child.minimal = odMinimalChecker.isCandidateMinimalFDPlusArray(childCandidate, infoTransfer);
                if (!child.minimal)
                    continue;
                ODTreeNodeEquivalenceClasses odTreeNodeEquivalenceClasses =
                        info.odTreeNodeEquivalenceClasses.deepClone();
                /*
                改动部分
                 */
//                odTreeNodeEquivalenceClasses.mergeNode(child, data);
                List<AttributeAndDirection> newLeft = cloneAttributeAndDirectionList(left);
                List<AttributeAndDirection> newRight = cloneAttributeAndDirectionList(right);
                if (parent.status == ODTree.ODTreeNodeStatus.VALID) {
                    if (attribute < attributeCount)
                        newRight.add(AttributeAndDirection.getInstance(attribute, AttributeAndDirection.UP));
                    else
                        newRight.add(AttributeAndDirection.getInstance(attribute - attributeCount, AttributeAndDirection.DOWN));
                    odTreeNodeEquivalenceClasses.findAndHandleEquivalenceClass(child, infoTransfer.listEcMap, newRight, data);
                } else if (parent.status == ODTree.ODTreeNodeStatus.SPLIT) {
                    if (attribute < attributeCount)
                        newLeft.add(AttributeAndDirection.getInstance(attribute, AttributeAndDirection.UP));
                    else
                        newLeft.add(AttributeAndDirection.getInstance(attribute - attributeCount, AttributeAndDirection.DOWN));
                    odTreeNodeEquivalenceClasses.findAndHandleEquivalenceClass(child, infoTransfer.listEcMap, newLeft, data);
                }

                if (!child.confirm)
                    child.status = odTreeNodeEquivalenceClasses.check(data).status;
                if (child.status == ODTree.ODTreeNodeStatus.VALID) {
                    odMinimalChecker.insert(childCandidate);
                }
                if (child.status != ODTree.ODTreeNodeStatus.SWAP) {
                    queue.offer(new ODDiscovererNodeSavingInfo(child, odTreeNodeEquivalenceClasses, newLeft, newRight));
                }
            }
        }
        return reslut;
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

    }
}
