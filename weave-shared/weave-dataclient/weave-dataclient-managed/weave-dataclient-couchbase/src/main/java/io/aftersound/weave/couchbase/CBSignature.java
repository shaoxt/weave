package io.aftersound.weave.couchbase;

import io.aftersound.weave.dataclient.Signature;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class CBSignature implements Signature {

    private final Set<String> nodes;

    public CBSignature(Set<String> nodes) {
        this.nodes = nodes;
    }

    static CBSignature of(Settings settings) {
        Set<String> nodes = new HashSet<>(Arrays.asList(settings.getNodes()));
        return new CBSignature(nodes);
    }

    private boolean hasAnyNodeOf(CBSignature that) {
        boolean hasAnySameNode = false;
        for (String thatNode : that.nodes) {
            if (this.nodes.contains(thatNode)) {
                hasAnySameNode = true;
                break;
            }
        }
        return hasAnySameNode;
    }

    @Override
    public boolean match(Signature another) {
        if (!(another instanceof CBSignature)) {
            return false;
        }

        CBSignature that = (CBSignature) another;
        return this.hasAnyNodeOf(that);
    }

}
