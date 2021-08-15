package discoverer.od;

import dataStructures.DataFrame;
import dataStructures.fd.FDCandidate;
import dataStructures.od.AttributeAndDirection;
import dataStructures.od.ODCandidate;
import dataStructures.od.ODTree;
import dataStructures.od.ODTreeNodeEquivalenceClasses;
import discoverer.fd.FDToODSavingInfo;
import minimal.ODMinimalChecker;
import minimal.ODMinimalCheckerBruteForce;
import util.Timer;

import java.util.*;

public class BFSODDiscovererFull extends ODDiscover {

    public static List<AttributeAndDirection> cloneAttributeAndDirectionList(List<AttributeAndDirection> list) {
        List<AttributeAndDirection> newList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            newList.add(list.get(i));
        }
        return newList;
    }

    @Override
    public ODTree discoverFD(DataFrame data, List<FDCandidate> fdCandidates) {
        Queue<ODDiscoverNodeSavingInfo> queue = new LinkedList<>();
        int attributeCount = data.getColumnCount();
        ODTree reslut = new ODTree(attributeCount);
        ODMinimalChecker odMinimalChecker = new ODMinimalCheckerBruteForce();

        //第二层的所有结点的direction为UP
        for (int attribute = 0; attribute < attributeCount; attribute++) {
            ODTreeNodeEquivalenceClasses odTreeNodeEquivalenceClasses = new ODTreeNodeEquivalenceClasses();
            odTreeNodeEquivalenceClasses.mergeNode(reslut.getRoot().children[attribute], data);
            queue.offer(new ODDiscoverNodeSavingInfo(reslut.getRoot().children[attribute], odTreeNodeEquivalenceClasses));
        }

        while (!queue.isEmpty()) {
            ODDiscoverNodeSavingInfo info = queue.poll();
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
                    queue.offer(new ODDiscoverNodeSavingInfo(child, odTreeNodeEquivalenceClasses));
                }
            }
        }
        return reslut;
    }

    public ODTree discover(DataFrame data) {
        Queue<ODDiscoverNodeSavingInfo> queue = new LinkedList<>();
        int attributeCount = data.getColumnCount();
        ODTree reslut = new ODTree(attributeCount);
        ODMinimalChecker odMinimalChecker = new ODMinimalCheckerBruteForce();

        //第二层的所有结点的direction为UP
        for (int attribute = 0; attribute < attributeCount; attribute++) {
            ODTreeNodeEquivalenceClasses odTreeNodeEquivalenceClasses = new ODTreeNodeEquivalenceClasses();
            odTreeNodeEquivalenceClasses.mergeNode(reslut.getRoot().children[attribute], data);
            queue.offer(new ODDiscoverNodeSavingInfo(reslut.getRoot().children[attribute], odTreeNodeEquivalenceClasses));
        }

        while (!queue.isEmpty()) {
            ODDiscoverNodeSavingInfo info = queue.poll();
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
                    queue.offer(new ODDiscoverNodeSavingInfo(child, odTreeNodeEquivalenceClasses));
                }
            }
        }
        return reslut;
    }

    public ODTree discoverFDPlus(DataFrame data, FDToODSavingInfo infoTransfer) {
        Queue<ODDiscoverNodeSavingInfo> queue = new LinkedList<>();
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
            queue.offer(new ODDiscoverNodeSavingInfo(reslut.getRoot().children[attribute],
                    odTreeNodeEquivalenceClasses,
                    left,
                    right));
        }

        while (!queue.isEmpty()) {
            ODDiscoverNodeSavingInfo info = queue.poll();
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
                    queue.offer(new ODDiscoverNodeSavingInfo(child, odTreeNodeEquivalenceClasses, newLeft, newRight));
                }
            }
        }
        return reslut;
    }


}
