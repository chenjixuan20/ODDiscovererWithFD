package validator.fd;

import dataStructures.DataFrame;
import dataStructures.fd.FDCandidate;


import java.util.List;
import java.util.Set;

public abstract class FDValidator {
    public abstract Set<Integer> validate(List<FDCandidate> fds, DataFrame data);

}
