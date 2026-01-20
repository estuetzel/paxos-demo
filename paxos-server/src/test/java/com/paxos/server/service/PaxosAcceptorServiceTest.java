package com.paxos.server.service;

import com.paxos.server.model.AcceptResponse;
import com.paxos.server.model.PaxosState;
import com.paxos.server.model.PromiseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Produced w/ Opus 4.5 using this prompt:
 * You are a software engineer in test specializing the in the Paxos protocol.
 * Your job is to write a comprehensive set of unit tests in
 * @paxos-server/src/test/java/com/paxos/server/service/PaxosAcceptorServiceTest.java
 * for @paxos-server/src/main/java/com/paxos/server/service/PaxosAcceptorService.java
 * expanding on what I have manually created so far. You are testing the behavior of a
 * single paxos node. You can assume that proposers sending commands to this acceptor
 * strictly adhere to the paxos protocol.  That is they will only send valid requests.
 * You may look at @paxos-server/src/main/java/com/paxos/server/service/PaxosAcceptorService.java
 * to observe the interface and check for any bugs, but do not make any changes to it.
 * If you see any bugs, simply numerate them in your response.
 *
 * Reviewed with GPT 5.2 Codex using this prompt:
 * As an expert in the paxos protocol, do you see any missing test acceptor cases in
 * @paxos-server/src/test/java/com/paxos/server/service/PaxosAcceptorServiceTest.java
 */
class PaxosAcceptorServiceTest {

    private PaxosAcceptorService svc;

    @BeforeEach
    void setUp() {
        svc = new PaxosAcceptorService();
    }

    // ==================== Initial State Tests ====================

    @Nested
    @DisplayName("Initial State Tests")
    class InitialStateTests {

        @Test
        @DisplayName("Fresh acceptor should have clean initial state")
        void testInitialState() {
            PaxosState state = svc.getState();
            verifyState(state, -1, -1, null);
        }

        @Test
        @DisplayName("getState should return consistent snapshot")
        void testGetStateReturnsNewInstance() {
            PaxosState state1 = svc.getState();
            PaxosState state2 = svc.getState();

            // Should be equal in value
            assertEquals(state1.getPromisedId(), state2.getPromisedId());
            assertEquals(state1.getAcceptedId(), state2.getAcceptedId());
            assertEquals(state1.getAcceptedValue(), state2.getAcceptedValue());

            // But different instances (defensive copy)
            assertNotSame(state1, state2);
        }
    }

    // ==================== Prepare Phase Tests ====================

    @Nested
    @DisplayName("Prepare Phase (Phase 1b) Tests")
    class PreparePhaseTests {

        @Test
        @DisplayName("First prepare should always succeed")
        void testFirstPrepareSucceeds() {
            PromiseResponse promise = svc.prepare(1);

            verifyPromise(promise, false, 1L, null, null);
            verifyState(svc.getState(), 1, -1, null);
        }

        @Test
        @DisplayName("Prepare with proposalId 0 should succeed on fresh acceptor")
        void testPrepareWithZeroProposalId() {
            PromiseResponse promise = svc.prepare(0);

            verifyPromise(promise, false, 0L, null, null);
            verifyState(svc.getState(), 0, -1, null);
        }

        @Test
        @DisplayName("Prepare with higher proposalId should succeed")
        void testPrepareWithHigherIdSucceeds() {
            svc.prepare(5);
            PromiseResponse promise = svc.prepare(10);

            verifyPromise(promise, false, 10L, null, null);
            verifyState(svc.getState(), 10, -1, null);
        }

        @Test
        @DisplayName("Prepare with lower proposalId should be ignored")
        void testPrepareWithLowerIdIsIgnored() {
            svc.prepare(10);
            PromiseResponse promise = svc.prepare(5);

            verifyPromise(promise, true, 10L, null, null);
            verifyState(svc.getState(), 10, -1, null);
        }

        @Test
        @DisplayName("Prepare with equal proposalId should be ignored")
        void testPrepareWithEqualIdIsIgnored() {
            svc.prepare(5);
            PromiseResponse promise = svc.prepare(5);

            verifyPromise(promise, true, 5L, null, null);
            verifyState(svc.getState(), 5, -1, null);
        }

        @Test
        @DisplayName("Prepare should return previously accepted value if exists")
        void testPrepareReturnsAcceptedValue() {
            svc.prepare(5);
            svc.acceptRequest(5, "value5");

            PromiseResponse promise = svc.prepare(10);

            verifyPromise(promise, false, 10L, 5L, "value5");
            verifyState(svc.getState(), 10, 5, "value5");
        }

        @Test
        @DisplayName("Ignored prepare should also return previously accepted value")
        void testIgnoredPrepareReturnsAcceptedValue() {
            svc.prepare(10);
            svc.acceptRequest(10, "value10");

            PromiseResponse promise = svc.prepare(5);

            // Ignored, but returns the promisedId and accepted values
            verifyPromise(promise, true, 10L, 10L, "value10");
        }

        @Test
        @DisplayName("Multiple sequential prepares with increasing IDs")
        void testMultipleSequentialPrepares() {
            for (int i = 1; i <= 10; i++) {
                PromiseResponse promise = svc.prepare(i);
                verifyPromise(promise, false, (long) i, null, null);
            }
            verifyState(svc.getState(), 10, -1, null);
        }

        @Test
        @DisplayName("Prepare with very large proposalId")
        void testPrepareWithLargeProposalId() {
            long largeId = Long.MAX_VALUE - 1;
            PromiseResponse promise = svc.prepare(largeId);

            verifyPromise(promise, false, largeId, null, null);
            verifyState(svc.getState(), largeId, -1, null);
        }
    }

    // ==================== Accept Phase Tests ====================

    @Nested
    @DisplayName("Accept Phase (Phase 2b) Tests")
    class AcceptPhaseTests {

        @Test
        @DisplayName("Accept without prior prepare should succeed (proposalId >= promisedId)")
        void testAcceptWithoutPriorPrepare() {
            // promisedId starts at -1, so any proposalId >= -1 should succeed
            AcceptResponse accept = svc.acceptRequest(0, "value0");

            verifyAccept(accept, false, 0L, "value0");
            verifyState(svc.getState(), 0, 0, "value0");
        }

        @Test
        @DisplayName("Accept with proposalId equal to promisedId should succeed")
        void testAcceptWithEqualProposalId() {
            svc.prepare(5);
            AcceptResponse accept = svc.acceptRequest(5, "value5");

            verifyAccept(accept, false, 5L, "value5");
            verifyState(svc.getState(), 5, 5, "value5");
        }

        @Test
        @DisplayName("Accept with proposalId greater than promisedId should succeed")
        void testAcceptWithHigherProposalId() {
            svc.prepare(5);
            AcceptResponse accept = svc.acceptRequest(10, "value10");

            verifyAccept(accept, false, 10L, "value10");
            // promisedId should also be updated to 10
            verifyState(svc.getState(), 10, 10, "value10");
        }

        @Test
        @DisplayName("Accept with proposalId less than promisedId should be ignored")
        void testAcceptWithLowerProposalId() {
            svc.prepare(10);
            AcceptResponse accept = svc.acceptRequest(5, "value5");

            verifyAccept(accept, true, null, null);
            verifyState(svc.getState(), 10, -1, null);
        }

        @Test
        @DisplayName("Multiple accepts with increasing proposalIds should all succeed")
        void testMultipleAcceptsIncreasing() {
            for (int i = 1; i <= 5; i++) {
                svc.prepare(i);
                AcceptResponse accept = svc.acceptRequest(i, "value" + i);
                verifyAccept(accept, false, (long) i, "value" + i);
            }
            verifyState(svc.getState(), 5, 5, "value5");
        }

        @Test
        @DisplayName("Accept should overwrite previous accepted value with higher ID")
        void testAcceptOverwritesPreviousValue() {
            svc.prepare(5);
            svc.acceptRequest(5, "first");
            verifyState(svc.getState(), 5, 5, "first");

            svc.prepare(10);
            svc.acceptRequest(10, "second");
            verifyState(svc.getState(), 10, 10, "second");
        }

        @Test
        @DisplayName("Accept with empty string value")
        void testAcceptWithEmptyString() {
            svc.prepare(1);
            AcceptResponse accept = svc.acceptRequest(1, "");

            verifyAccept(accept, false, 1L, "");
            verifyState(svc.getState(), 1, 1, "");
        }

        @Test
        @DisplayName("Accept with null value")
        void testAcceptWithNullValue() {
            svc.prepare(1);
            AcceptResponse accept = svc.acceptRequest(1, null);

            verifyAccept(accept, false, 1L, null);
            verifyState(svc.getState(), 1, 1, null);
        }

        @Test
        @DisplayName("Accept with very long value")
        void testAcceptWithLongValue() {
            String longValue = "x".repeat(10000);
            svc.prepare(1);
            AcceptResponse accept = svc.acceptRequest(1, longValue);

            verifyAccept(accept, false, 1L, longValue);
            verifyState(svc.getState(), 1, 1, longValue);
        }
    }

    // ==================== Complete Paxos Round Tests ====================

    @Nested
    @DisplayName("Complete Paxos Round Tests")
    class CompletePaxosRoundTests {

        @Test
        @DisplayName("Basic complete round: prepare -> accept")
        void testBasicCompleteRound() {
            PromiseResponse promise = svc.prepare(1);
            verifyPromise(promise, false, 1L, null, null);

            AcceptResponse accept = svc.acceptRequest(1, "consensus_value");
            verifyAccept(accept, false, 1L, "consensus_value");

            verifyState(svc.getState(), 1, 1, "consensus_value");
        }

        @Test
        @DisplayName("Multiple complete rounds with increasing IDs")
        void testMultipleCompleteRounds() {
            // Round 1
            svc.prepare(1);
            svc.acceptRequest(1, "round1");
            verifyState(svc.getState(), 1, 1, "round1");

            // Round 2
            PromiseResponse promise2 = svc.prepare(2);
            // Should return previously accepted value
            verifyPromise(promise2, false, 2L, 1L, "round1");
            svc.acceptRequest(2, "round2");
            verifyState(svc.getState(), 2, 2, "round2");

            // Round 3
            PromiseResponse promise3 = svc.prepare(3);
            verifyPromise(promise3, false, 3L, 2L, "round2");
            svc.acceptRequest(3, "round3");
            verifyState(svc.getState(), 3, 3, "round3");
        }

        @Test
        @DisplayName("Interleaved prepare and accept from competing proposers")
        void testInterleavedPrepareAccept() {
            // Proposer A prepares with ID 1
            PromiseResponse promiseA = svc.prepare(1);
            verifyPromise(promiseA, false, 1L, null, null);

            // Proposer B prepares with higher ID 2
            PromiseResponse promiseB = svc.prepare(2);
            verifyPromise(promiseB, false, 2L, null, null);

            // Proposer A tries to accept with ID 1 (should be ignored)
            AcceptResponse acceptA = svc.acceptRequest(1, "valueA");
            verifyAccept(acceptA, true, null, null);

            // Proposer B accepts with ID 2 (should succeed)
            AcceptResponse acceptB = svc.acceptRequest(2, "valueB");
            verifyAccept(acceptB, false, 2L, "valueB");

            verifyState(svc.getState(), 2, 2, "valueB");
        }

        @Test
        @DisplayName("Proposer recovers previously accepted value")
        void testProposerRecoversPreviousValue() {
            // First proposer successfully completes
            svc.prepare(1);
            svc.acceptRequest(1, "original_value");

            // New proposer comes in, should learn about accepted value
            PromiseResponse promise = svc.prepare(2);
            assertFalse(promise.isIgnored());
            assertEquals(Long.valueOf(2), promise.getPromisedId());
            assertEquals(Long.valueOf(1), promise.getAcceptedId());
            assertEquals("original_value", promise.getAcceptedValue());
        }
    }

    // ==================== Edge Cases and Boundary Tests ====================

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Prepare followed by many ignored prepares")
        void testManyIgnoredPrepares() {
            svc.prepare(100);

            for (int i = 0; i < 100; i++) {
                PromiseResponse promise = svc.prepare(i);
                verifyPromise(promise, true, 100L, null, null);
            }

            verifyState(svc.getState(), 100, -1, null);
        }

        @Test
        @DisplayName("Accept followed by many ignored accepts")
        void testManyIgnoredAccepts() {
            svc.prepare(100);
            svc.acceptRequest(100, "accepted");

            for (int i = 0; i < 100; i++) {
                AcceptResponse accept = svc.acceptRequest(i, "ignored" + i);
                verifyAccept(accept, true, null, null);
            }

            verifyState(svc.getState(), 100, 100, "accepted");
        }

        @Test
        @DisplayName("Alternating prepare and accept pattern")
        void testAlternatingPrepareAccept() {
            for (int i = 1; i <= 10; i++) {
                PromiseResponse promise = svc.prepare(i * 2 - 1);
                verifyPromise(promise, false, (long) (i * 2 - 1),
                        i > 1 ? (long) ((i - 1) * 2 - 1) : null,
                        i > 1 ? "value" + ((i - 1) * 2 - 1) : null);

                AcceptResponse accept = svc.acceptRequest(i * 2 - 1, "value" + (i * 2 - 1));
                verifyAccept(accept, false, (long) (i * 2 - 1), "value" + (i * 2 - 1));
            }
        }

        @Test
        @DisplayName("Prepare at promisedId boundary")
        void testPrepareAtBoundary() {
            svc.prepare(5);

            // Just below - ignored
            PromiseResponse below = svc.prepare(4);
            assertTrue(below.isIgnored());

            // Equal - ignored
            PromiseResponse equal = svc.prepare(5);
            assertTrue(equal.isIgnored());

            // Just above - succeeds
            PromiseResponse above = svc.prepare(6);
            assertFalse(above.isIgnored());
            assertEquals(Long.valueOf(6), above.getPromisedId());
        }

        @Test
        @DisplayName("Accept at promisedId boundary")
        void testAcceptAtBoundary() {
            svc.prepare(5);

            // Just below - ignored
            AcceptResponse below = svc.acceptRequest(4, "below");
            assertTrue(below.isIgnored());
            verifyState(svc.getState(), 5, -1, null);

            // Equal - succeeds
            AcceptResponse equal = svc.acceptRequest(5, "equal");
            assertFalse(equal.isIgnored());
            verifyState(svc.getState(), 5, 5, "equal");
        }

        @Test
        @DisplayName("State remains consistent after mixed operations")
        void testStateConsistencyAfterMixedOps() {
            svc.prepare(1);
            svc.prepare(2);
            svc.acceptRequest(1, "ignored"); // ignored
            svc.prepare(3);
            svc.acceptRequest(3, "accepted");
            svc.prepare(2); // ignored
            svc.acceptRequest(2, "ignored2"); // ignored

            verifyState(svc.getState(), 3, 3, "accepted");
        }
    }

    // ==================== Original Tests (Preserved) ====================

    @Nested
    @DisplayName("Original Tests")
    class OriginalTests {

        @Test
        void testBasic() {
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
            // it should be accepted
            verifyAccept(accept, false, 2L, "2val");
            state = svc.getState();
            verifyState(state, 2, 2, "2val");
        }

        @Test
        void testAcceptAbovePromisedId() {
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
            // it should be accepted
            verifyAccept(accept, false, 3L, "3val");
            state = svc.getState();
            verifyState(state, 3, 3, "3val");
        }
    }

    // ==================== Helper Methods ====================

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