package validator.fd;

import dataStructures.DataFrame;
import dataStructures.fd.FDCandidate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FDIncrementalValidator extends FDBruteForceFullValidator{


    @Override
    protected List<FDCandidate> chooseFDs(List<FDCandidate> fds){
        List<FDCandidate> needValidate = new ArrayList<>();
        for(FDCandidate fd: fds){
            if(!fd.fdTreeNode.vaildRhsInTotal.contains(fd.right)){
                needValidate.add(fd);
            }
        }
        return needValidate;
    }

}

