package com.paxos.server.service;

import com.paxos.server.model.AcceptResponse;
import com.paxos.server.model.PaxosState;
import com.paxos.server.model.PromiseResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class PaxosAcceptorServiceTest {

    @Test
    void testBasic() {
        PaxosAcceptorService svc = new PaxosAcceptorService();

        PaxosState state;
        PromiseResponse promise;

        state = svc.getState();
        verifyState(state, -1, -1, null);

        promise = svc.prepare(2);
        verifyPromise(promise, false, 2L, null, null);
        state = svc.getState();
        verifyState(state, 2, -1, null);

        AcceptResponse accept = svc.acceptRequest(2, "2val");
        verifyAccept(accept, false, 2L, "2val");
        state = svc.getState();
        verifyState(state, 2, 2, "2val");
    }

    @Test
    void testMultiplePromises() {
        PaxosAcceptorService svc = new PaxosAcceptorService();

        PaxosState state;
        PromiseResponse promise;

        state = svc.getState();
        verifyState(state, -1, -1, null);

        promise = svc.prepare(2);
        verifyPromise(promise, false, 2L, null, null);
        state = svc.getState();
        verifyState(state, 2, -1, null);

        promise = svc.prepare(1);
        // acceptor is 'nice' and gives us the promisedId despite ignoring.
        // not required by protocol, but also not prohibited
        verifyPromise(promise, true, 2L, null, null);
        state = svc.getState();
        verifyState(state, 2, -1, null);

        promise = svc.prepare(3);
        verifyPromise(promise, false, 3L, null, null);
        state = svc.getState();
        verifyState(state, 3, -1, null);
    }

    @Test
    void testAcceptBelowPromisedId() {
        PaxosAcceptorService svc = new PaxosAcceptorService();

        PaxosState state;
        PromiseResponse promise;
        AcceptResponse accept;

        state = svc.getState();
        verifyState(state, -1, -1, null);

        promise = svc.prepare(2);
        verifyPromise(promise, false, 2L, null, null);
        state = svc.getState();
        verifyState(state, 2, -1, null);


        // first try to accept and id below what was promised
        accept = svc.acceptRequest(1, "1val");
        // it should be ignored
        verifyAccept(accept, true, null, null);
        state = svc.getState();
        verifyState(state, 2, -1, null);
    }

    @Test
    void testAcceptEqualsPromisedId() {
        PaxosAcceptorService svc = new PaxosAcceptorService();

        PaxosState state;
        PromiseResponse promise;
        AcceptResponse accept;

        state = svc.getState();
        verifyState(state, -1, -1, null);

        promise = svc.prepare(2);
        verifyPromise(promise, false, 2L, null, null);
        state = svc.getState();
        verifyState(state, 2, -1, null);


        // try to accept and id same as what was promised
        accept = svc.acceptRequest(2, "2val");
        // it should be ignored
        verifyAccept(accept, false, 2L, "2val");
        state = svc.getState();
        verifyState(state, 2, 2, "2val");
    }

    @Test
    void testAcceptAbovePromisedId() {
        PaxosAcceptorService svc = new PaxosAcceptorService();

        PaxosState state;
        PromiseResponse promise;
        AcceptResponse accept;

        state = svc.getState();
        verifyState(state, -1, -1, null);

        promise = svc.prepare(2);
        verifyPromise(promise, false, 2L, null, null);
        state = svc.getState();
        verifyState(state, 2, -1, null);

        // try to accept id above what was promised
        accept = svc.acceptRequest(3, "3val");
        // it should be ignored
        verifyAccept(accept, false, 3L, "3val");
        state = svc.getState();
        verifyState(state, 3, 3, "3val");
    }

    private void verifyState(PaxosState state, long pId, long aId, String aVal) {
        assertEquals(pId, state.getPromisedId());
        assertEquals(aId, state.getAcceptedId());
        assertEquals(aVal, state.getAcceptedValue());
    }

    private void verifyPromise(PromiseResponse promise, boolean ignored, Long pId, Long aId, String aVal) {
        assertEquals(ignored, promise.isIgnored());
        assertEquals(pId, promise.getPromisedId());
        assertEquals(aId, promise.getAcceptedId());
        assertEquals(aVal, promise.getAcceptedValue());
    }

    private void verifyAccept(AcceptResponse accept, boolean ignored, Long aId, String aVal) {
        assertEquals(ignored, accept.isIgnored());
        assertEquals(aId, accept.getAcceptedId());
        assertEquals(aVal, accept.getAcceptedValue());
    }
}