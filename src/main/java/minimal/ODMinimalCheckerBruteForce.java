package minimal;

import dataStructures.fd.FDCandidate;
import dataStructures.od.AttributeAndDirection;
import dataStructures.od.ODCandidate;

import java.util.ArrayList;
import java.util.List;

public class ODMinimalCheckerBruteForce extends ODMinimalChecker{
    public List<ODCandidate> ods;

    public ODMinimalCheckerBruteForce(){
        ods = new ArrayList<>();
    }

    @Override
    public boolean isListMinimalFD(List<AttributeAndDirection> expandList, List<FDCandidate> fdCandidates){
        if(expandList.size() == 1) return true;
        int expandAttribute = expandList.get(expandList.size() - 1).attribute;
        int beforeExpandAttribute = expandList.get(expandList.size() - 2).attribute;
        List<List<Integer>> leftOfExpandRight = new ArrayList<>();
        List<Integer> rightOfExpandInLeft = new ArrayList<>();
        for(int i = 0; i < fdCandidates.size(); i++){
            if(fdCandidates.get(i).right == expandAttribute){
                leftOfExpandRight.add(fdCandidates.get(i).left);
            }
            if(fdCandidates.get(i).left.size()== 1 && fdCandidates.get(i).left.get(0) == expandAttribute){
                rightOfExpandInLeft.add(fdCandidates.get(i).right);
            }
        }

        List<Integer> expandListAttributes = new ArrayList<>();
        for(int i = 0; i < expandList.size() - 1; i++){
            expandListAttributes.add(expandList.get(i).attribute);
        }

        //FD前推后
        for(int i = 0; i < leftOfExpandRight.size(); i++){
            if(expandListAttributes.containsAll(leftOfExpandRight.get(i)))
                return false;
        }

        //OD后推前
        for(ODCandidate od : ods){
            List<AttributeAndDirection> left = od.leftAndRightAttributeList.left;
            List<AttributeAndDirection> right = od.leftAndRightAttributeList.right;
            int leftIndex = getIndex(expandList, left);
            int rightIndex = getIndex(expandList, right);
            if(leftIndex != -1 && rightIndex != -1 &&
                   rightIndex + right.size() == leftIndex){
                return false;
            }
            expandList = reverseDirection(expandList);
            leftIndex = getIndex(expandList, left);
            rightIndex = getIndex(expandList, right);
            if(leftIndex != -1 && rightIndex != -1 &&
                     rightIndex + right.size() == leftIndex){
                return false;
            }

        }
        return true;
    }

    @Override
    public boolean isListMinimal(List<AttributeAndDirection> list){
        for(ODCandidate od : ods){
            List<AttributeAndDirection> left = od.leftAndRightAttributeList.left;
            List<AttributeAndDirection> right = od.leftAndRightAttributeList.right;

            int leftIndex = getIndex(list, left), rightIndex = getIndex(list, right);
            if(leftIndex != -1 && rightIndex != -1 &&
                    (leftIndex < rightIndex || rightIndex + right.size() == leftIndex)){
                return false;
            }

            list = reverseDirection(list);
            leftIndex = getIndex(list, left);
            rightIndex = getIndex(list, right);
            if(leftIndex != -1 && rightIndex != -1 &&
                    (leftIndex < rightIndex || rightIndex + right.size() == leftIndex)){
                return false;
            }
        }
        return true;
    }

    @Override
    public void insert(ODCandidate candidate){
        ods.add(candidate);
    }

    private int getIndex(List<AttributeAndDirection> context, List<AttributeAndDirection> pattern){
        if(context.size() < pattern.size()) return -1;
        int end = context.size() - pattern.size();
        for(int i = 0; i <= end; i++){
            if(exactMatch(context, pattern, i ))
                return i;
        }
        return -1;
    }


    public static boolean m(List<Integer> expandList, List<List<Integer>> leftOfExpandRight){

        for(int i = 0; i < leftOfExpandRight.size(); i++){
            if(expandList.containsAll(leftOfExpandRight.get(i)))
                return false;
        }
        return true;
    }

}