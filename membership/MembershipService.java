package membership;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import common.Node;

public class MembershipService {
    private final Set<Node> members = ConcurrentHashMap.newKeySet();


    public void addNode(Node node){
        members.add(node);
    }

    public void removeNode(Node node){
        members.remove(node);
       
    }

    public boolean contains(Node node){
        return members.contains(node);
    }

    public Set<Node> getAllNodes(){
        return Collections.unmodifiableSet(members);
    }

    public int size(){
        return members.size();
    }
}
