package minimal;

import dataStructures.fd.FDCandidate;
import dataStructures.od.AttributeAndDirection;
import dataStructures.od.ODCandidate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ODMinimalCheckerBruteForce extends ODMinimalChecker{
    public List<ODCandidate> ods;

    public ODMinimalCheckerBruteForce(){
        ods = new ArrayList<>();
    }

    @Override
    public boolean isListMinimalFD(List<AttributeAndDirection> expandList, List<FDCandidate>fdCandidates){
        if(expandList.size() == 1) return true;
        int expandAttribute = expandList.get(expandList.size() - 1).attribute;
        List<List<Integer>> leftOfExpandRight = new ArrayList<>();
        //这个地方可以优化，可以不传List，传一个Map会更快
        for (FDCandidate fdCandidate : fdCandidates) {
            if (fdCandidate.right == expandAttribute) {
                leftOfExpandRight.add(fdCandidate.left);
            }
        }

        List<Integer> expandListAttributes = new ArrayList<>();
        for(int i = 0; i < expandList.size() - 1; i++){
            expandListAttributes.add(expandList.get(i).attribute);
        }

        //FD前推后
        for (List<Integer> integers : leftOfExpandRight) {
            if (expandListAttributes.containsAll(integers))
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

    public boolean isListMinimalFDMap(List<AttributeAndDirection> expandList,
                                      Map<Integer, List<List<Integer>>> fdMap){
        if(expandList.size() == 1) return true;
        int expandAttribute = expandList.get(expandList.size() - 1).attribute;

        List<List<Integer>> leftOfExpandRight = fdMap.get(expandAttribute);
//        System.out.println("right: " + expandAttribute);
//        System.out.println("allLeft: " + leftOfExpandRight);
//        System.out.println();

        List<Integer> expandListAttributes = new ArrayList<>();
        for(int i = 0; i < expandList.size() - 1; i++){
            expandListAttributes.add(expandList.get(i).attribute);
        }

        if(leftOfExpandRight == null){
            return true;
        }

        //FD前推后
        for (List<Integer> integers : leftOfExpandRight) {
            if (expandListAttributes.containsAll(integers))
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

        for (List<Integer> integers : leftOfExpandRight) {
            if (expandList.containsAll(integers))
                return false;
        }
        return true;
    }

}
