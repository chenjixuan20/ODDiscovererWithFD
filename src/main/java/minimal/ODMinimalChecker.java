package minimal;

import dataStructures.fd.FDCandidate;
import dataStructures.od.AttributeAndDirection;
import dataStructures.od.ODCandidate;
import dataStructures.od.ODTree;
import discoverer.fd.FDToODSavingInfo;
import util.Timer;

import java.util.ArrayList;
import java.util.List;

public abstract class ODMinimalChecker {
    public static long odMinimalCheckTime=0;
    public static long fdMinimalCheckTime=0;

    public abstract void insert(ODCandidate candidate);

    public abstract boolean isListMinimal(List<AttributeAndDirection> list);

    public abstract boolean isListMinimalFD(List<AttributeAndDirection> list, List<FDCandidate> fdCandidates);

    public boolean isCandidateMinimal(ODCandidate candidate){
        Timer timer=new Timer();
        boolean result;
        List<AttributeAndDirection> expandSide, otherSide;
        if(candidate.odByODTreeNode.parent.status == ODTree.ODTreeNodeStatus.VALID){
            expandSide = candidate.leftAndRightAttributeList.right;
            otherSide = candidate.leftAndRightAttributeList.left;
        }else{
            expandSide = candidate.leftAndRightAttributeList.left;
            otherSide = candidate.leftAndRightAttributeList.right;
        }
        int expandAttribute = expandSide.get(expandSide.size() - 1).attribute;

        //左右两侧有交集则无效，平凡则非最小
        for(AttributeAndDirection x:otherSide){
            if(x.attribute == expandAttribute){
                return false;
            }
        }
        //拓展侧重复出现则无效
        for (int i = 0; i < expandSide.size()-1; i++) {
            if(expandAttribute == expandSide.get(i).attribute){
                return false;
            }
        }

        result = isListMinimal(expandSide);
        odMinimalCheckTime+=timer.getTimeUsed();
        return result;
    }

    public boolean isCandidateMinimalFD(ODCandidate candidate, List<FDCandidate> fdCandidates){
        Timer timer=new Timer();
        boolean result;
        List<AttributeAndDirection> expandSide, otherSide;
        if(candidate.odByODTreeNode.parent.status == ODTree.ODTreeNodeStatus.VALID){
            expandSide = candidate.leftAndRightAttributeList.right;
            otherSide = candidate.leftAndRightAttributeList.left;
        }else{
            expandSide = candidate.leftAndRightAttributeList.left;
            otherSide = candidate.leftAndRightAttributeList.right;
        }
        int expandAttribute = expandSide.get(expandSide.size() - 1).attribute;


        for(AttributeAndDirection x:otherSide){
            if(x.attribute == expandAttribute)
                return false;
        }

        for (int i = 0; i < expandSide.size()-1; i++) {
            if(expandAttribute == expandSide.get(i).attribute)
                return false;
        }

        result = isListMinimalFD(expandSide, fdCandidates);
        fdMinimalCheckTime+=timer.getTimeUsed();
        return result;
    }

    public boolean isCandidateMinimalFDPlus(ODCandidate candidate, FDToODSavingInfo infoTranfer){
        boolean result;
        List<AttributeAndDirection> expandSide, otherSide;
        if(candidate.odByODTreeNode.parent.status == ODTree.ODTreeNodeStatus.VALID){
            expandSide = candidate.leftAndRightAttributeList.right;
            otherSide = candidate.leftAndRightAttributeList.left;
        }else{
            expandSide = candidate.leftAndRightAttributeList.left;
            otherSide = candidate.leftAndRightAttributeList.right;
        }
        int expandAttribute = expandSide.get(expandSide.size() - 1).attribute;


        for(AttributeAndDirection x:otherSide){
            if(x.attribute == expandAttribute)
                return false;
        }

        for (int i = 0; i < expandSide.size()-1; i++) {
            if(expandAttribute == expandSide.get(i).attribute)
                return false;
        }

        result = isListMinimalFD(expandSide, infoTranfer.fdCandidates);
        return result;
    }


    protected boolean exactMatch(List<AttributeAndDirection> context,
                                 List<AttributeAndDirection> pattern, int beginPosition){
        if(beginPosition < 0) return false;
        if(beginPosition + pattern.size() > context.size()) return false;
        for(int i = 0; i < pattern.size(); i++){
            AttributeAndDirection x1 = context.get(beginPosition + i);
            AttributeAndDirection x2 = pattern.get(i);
            if(x1 != x2) return false;
        }
        return true;
    }

    protected List<AttributeAndDirection> reverseDirection(List<AttributeAndDirection> list){
        List<AttributeAndDirection> result = new ArrayList<>();
        for (AttributeAndDirection attributeAndDirection : list){
            result.add(AttributeAndDirection.getInstance(attributeAndDirection.attribute
                    , -attributeAndDirection.direction));
        }
        return result;
    }


}
