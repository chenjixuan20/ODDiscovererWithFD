package dataStructures.od;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class ODTree {

    public static final String OUTPUT_DELIMETER=" |--> ";

    private int countAttribute;
    private ODTreeNode root;

    public ODTreeNode getRoot() {
        return root;
    }

    public ODTree(int countAttribute){
        this.countAttribute = countAttribute;
        root = new ODTreeNode(null, AttributeAndDirection.getInstance(0, AttributeAndDirection.UP)
                ,ODTreeNodeStatus.VALID);
        root.confirm();
        //前countAttribute位置存放UP
        for(int i = 0; i < countAttribute; i++){
            ODTreeNode node = new ODTreeNode(root, AttributeAndDirection.getInstance(i, AttributeAndDirection.UP)
                    , ODTreeNodeStatus.SPLIT);
            node.confirm();
        }
        for(int i = 0; i < countAttribute; i++){
            ODTreeNode node = new ODTreeNode(root,AttributeAndDirection.getInstance(i, AttributeAndDirection.DOWN)
                    , ODTreeNodeStatus.SPLIT);
            node.confirm();
            node.minimal = false;
        }
    }

    public int getIndexFromAttributeAndDirection(AttributeAndDirection attributeAndDirection){
        return attributeAndDirection.attribute +
                (attributeAndDirection.direction == AttributeAndDirection.DOWN?countAttribute:0);
    }

    public AttributeAndDirection getAttributeAndDirectionFromIndex(int attribute){
        int att = attribute % countAttribute;
        int direction = (attribute < countAttribute)?AttributeAndDirection.UP:AttributeAndDirection.DOWN;
        return  AttributeAndDirection.getInstance(att, direction);
    }

    public List<ODCandidate> getAllOdsOrderByBFS(){
        List<ODCandidate> result = getAllOdsOrderByDFS();
        Collections.sort(result);
        return result;
    }

    public List<ODCandidate> getAllOdsOrderByDFS(){
        return getAllNodesDFS(node -> node.status == ODTreeNodeStatus.VALID && !node.isRoot() && node.minimal);
    }
    public List<ODCandidate> getAllNodesDFS(Predicate<ODTreeNode> filter){
        List<ODCandidate> result=new ArrayList<>();
        root.getAllNodesOrderByDFSRecursion(result,filter);
        return result;
    }

    public class ODTreeNode{
        public ODTreeNode parent;
        public ODTreeNodeStatus status;
        public AttributeAndDirection attribute;
        public ODTreeNode[] children;
        public boolean confirm;
        public boolean minimal;

        public ODTreeNode getRoot(){
            return root;
        }

        public boolean isRoot(){
            return root == this;
        }

        public ODTreeNode(ODTreeNode parent, AttributeAndDirection attribute, ODTreeNodeStatus status){
            this.parent = parent;
            this.status = status;
            this.attribute = attribute;
            this.minimal = true;
            //前countAttribute个位置方向为up，后面为down
            this.children = new ODTreeNode[countAttribute * 2];
            if(parent != null){
                parent.children[getIndexFromAttributeAndDirection(attribute)] = this;
            }
            this.confirm = (status==ODTreeNodeStatus.SWAP);
        }

        public void confirm(){
            this.confirm = true;
        }

        public ODTreeNode(ODTreeNode parent, AttributeAndDirection attribute) {
            this(parent,attribute,ODTreeNodeStatus.UNKNOWN);
        }

        public void getAllNodesOrderByDFSRecursion(List<ODCandidate> ods, Predicate<ODTreeNode> filter){
            if(!minimal)
                return;
            if(filter.test(this))
                ods.add(new ODCandidate(this));
            for (ODTreeNode child:children) {
                if(child != null)
                    child.getAllNodesOrderByDFSRecursion(ods,filter);
            }
        }
    }

    public enum ODTreeNodeStatus{
        VALID,SWAP,SPLIT,UNKNOWN;

        @Override
        public String toString() {
            if(equals(VALID))
                return "V";
            else if(equals(SWAP))
                return "SW";
            else if(equals(SPLIT))
                return "SP";
            else if(equals(UNKNOWN))
                return "UK";
            return "?";
        }
    }

}
