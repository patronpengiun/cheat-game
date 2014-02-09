package org.cheat.client;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.cheat.client.GameApi.Delete;
import org.cheat.client.GameApi.Operation;
import org.cheat.client.GameApi.Set;
import org.cheat.client.GameApi.SetVisibility;
import org.cheat.client.GameApi.Shuffle;
import org.cheat.client.GameApi.VerifyMove;
import org.cheat.client.GameApi.VerifyMoveDone;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@RunWith(JUnit4.class)
public class CheatLogicTest {
  /** The object under test. */
  CheatLogic cheatLogic = new CheatLogic();

  private void assertMoveOk(VerifyMove verifyMove) {
    cheatLogic.checkMoveIsLegal(verifyMove);
  }

  private void assertHacker(VerifyMove verifyMove) {
    VerifyMoveDone verifyDone = cheatLogic.verify(verifyMove);
    assertEquals(verifyMove.getLastMovePlayerId(), verifyDone.getHackerPlayerId());
  }

  private static final String PLAYER_ID = "playerId";
  /* The entries used in the cheat game are:
   *   turn:W/B, isCheater:yes, W, B, M, claim, C0...C51
   * When we send operations on these keys, it will always be in the above order.
   */
  private static final String TURN = "turn"; // turn of which player (either W or B)
  private static final String W = "W"; // White hand
  private static final String B = "B"; // Black hand
  private static final String M = "M"; // Middle pile
  private static final String CLAIM = "claim"; // a claim has the form: [3cards, rankK]
  private static final String IS_CHEATER = "isCheater"; // we claim we have a cheater
  private static final String YES = "yes"; // we claim we have a cheater
  private final int wId = 41;
  private final int bId = 42;
  private final List<Integer> visibleToW = ImmutableList.of(wId);
  private final List<Integer> visibleToB = ImmutableList.of(bId);
  private final Map<String, Object> wInfo = ImmutableMap.<String, Object>of(PLAYER_ID, wId);
  private final Map<String, Object> bInfo = ImmutableMap.<String, Object>of(PLAYER_ID, bId);
  private final List<Map<String, Object>> playersInfo = ImmutableList.of(wInfo, bInfo);
  private final Map<String, Object> emptyState = ImmutableMap.<String, Object>of();
  private final Map<String, Object> nonEmptyState = ImmutableMap.<String, Object>of("k", "v");

  private final Map<String, Object> turnOfWEmptyMiddle = ImmutableMap.<String, Object>of(
      TURN, W,
      W, getIndicesInRange(0, 10),
      B, getIndicesInRange(11, 51),
      M, ImmutableList.of());

  Map<String, Object> turnOfBEmptyMiddle = ImmutableMap.<String, Object>of(
      TURN, B,
      W, getIndicesInRange(0, 10),
      B, getIndicesInRange(11, 51),
      M, ImmutableList.of());

  // The order of operations: turn, isCheater, W, B, M, claim, C0...C51
  private final List<Operation> claimOfW = ImmutableList.<Operation>of(
      new Set(TURN, B),
      new Set(W, getIndicesInRange(0, 8)),
      new Set(M, getIndicesInRange(9, 10)),
      new Set(CLAIM, ImmutableList.of("2cards", "rankA")));

  private final List<Operation> claimOfB = ImmutableList.<Operation>of(
      new Set(TURN, W),
      new Set(B, getIndicesInRange(11, 48)),
      new Set(M, getIndicesInRange(49, 51)),
      new Set(CLAIM, ImmutableList.of("3cards", "rankJ")));

  private final List<Operation> illegalClaimWithWrongCards = ImmutableList.<Operation>of(
      new Set(TURN, B),
      new Set(W, getIndicesInRange(0, 8)),
      new Set(M, getIndicesInRange(9, 10)),
      new Set(CLAIM, ImmutableList.of("3cards", "rankA")));

  private final List<Operation> illegalClaimWithWrongW = ImmutableList.<Operation>of(
      new Set(TURN, B),
      new Set(W, getIndicesInRange(0, 7)),
      new Set(M, getIndicesInRange(9, 10)),
      new Set(CLAIM, ImmutableList.of("2cards", "rankA")));

  private final List<Operation> illegalClaimWithWrongM = ImmutableList.<Operation>of(
      new Set(TURN, B),
      new Set(W, getIndicesInRange(0, 8)),
      new Set(M, getIndicesInRange(8, 10)),
      new Set(CLAIM, ImmutableList.of("2cards", "rankA")));


  private VerifyMove move(
      int lastMovePlayerId, Map<String, Object> lastState, List<Operation> lastMove) {
    return new VerifyMove(wId, playersInfo,
        // in cheat we never need to check the resulting state (the server makes it, and the game
        // doesn't have any hidden decisions such in Battleships)
        emptyState,
        lastState, lastMove, lastMovePlayerId);
  }

  private List<Integer> getIndicesInRange(int fromInclusive, int toInclusive) {
    return cheatLogic.getIndicesInRange(fromInclusive, toInclusive);
  }

  @Test
  public void testGetIndicesInRange() {
    assertEquals(ImmutableList.of(3, 4), cheatLogic.getIndicesInRange(3, 4));
  }

  private List<String> getCardsInRange(int fromInclusive, int toInclusive) {
    return cheatLogic.getCardsInRange(fromInclusive, toInclusive);
  }

  @Test
  public void testCardsInRange() {
    assertEquals(ImmutableList.of("C3", "C4"), cheatLogic.getCardsInRange(3, 4));
  }

  private <T> List<T> concat(List<T> a, List<T> b) {
    return cheatLogic.concat(a, b);
  }

  @Test
  public void testCardIdToString() {
    assertEquals("2c", cheatLogic.cardIdToString(0));
    assertEquals("2d", cheatLogic.cardIdToString(1));
    assertEquals("2h", cheatLogic.cardIdToString(2));
    assertEquals("2s", cheatLogic.cardIdToString(3));
    assertEquals("As", cheatLogic.cardIdToString(51));
  }

  private List<Operation> getInitialOperations() {
    return cheatLogic.getInitialMove(wId, bId);
  }

  @Test
  public void testGetInitialOperationsSize() {
    assertEquals(4 + 52 + 1 + 52, cheatLogic.getInitialMove(wId, bId).size());
  }

  @Test
  public void testInitialMove() {
    assertMoveOk(move(wId, emptyState, getInitialOperations()));
  }

  @Test
  public void testInitialMoveByWrongPlayer() {
    assertHacker(move(bId, emptyState, getInitialOperations()));
  }

  @Test
  public void testInitialMoveFromNonEmptyState() {
    assertHacker(move(wId, nonEmptyState, getInitialOperations()));
  }

  @Test
  public void testInitialMoveWithExtraOperation() {
    List<Operation> initialOperations = getInitialOperations();
    initialOperations.add(new Set(M, ImmutableList.of()));
    assertHacker(move(wId, emptyState, initialOperations));
  }

  @Test
  public void testNormalClaimByWhite() {
    assertMoveOk(move(wId, turnOfWEmptyMiddle, claimOfW));
  }

  @Test
  public void testNormalClaimByBlack() {
    assertMoveOk(move(bId, turnOfBEmptyMiddle, claimOfB));
  }

  @Test
  public void testIllegalClaimByWrongColor() {
    assertHacker(move(bId, turnOfWEmptyMiddle, claimOfW));
    assertHacker(move(wId, turnOfBEmptyMiddle, claimOfB));
    assertHacker(move(wId, turnOfBEmptyMiddle, claimOfW));
    assertHacker(move(bId, turnOfWEmptyMiddle, claimOfB));
    assertHacker(move(bId, turnOfBEmptyMiddle, claimOfW));
    assertHacker(move(wId, turnOfWEmptyMiddle, claimOfB));
  }

  @Test
  public void testClaimWithWrongCards() {
    assertHacker(move(wId, turnOfWEmptyMiddle, illegalClaimWithWrongCards));
  }

  @Test
  public void testClaimWithWrongW() {
    assertHacker(move(wId, turnOfWEmptyMiddle, illegalClaimWithWrongW));
  }

  @Test
  public void testClaimWithWrongM() {
    assertHacker(move(wId, turnOfWEmptyMiddle, illegalClaimWithWrongM));
  }

  // The order of operations: turn, isCheater, W, B, M, claim, C0...C51
  List<Operation> claimCheaterByW = ImmutableList.<Operation>of(
      new Set(TURN, W),
      new Set(IS_CHEATER, YES),
      new SetVisibility("C50"), new SetVisibility("C51"));

  @Test
  public void testClaimCheaterByWhite() {
    Map<String, Object> state = ImmutableMap.<String, Object>of(
        TURN, W,
        W, getIndicesInRange(0, 10),
        B, getIndicesInRange(11, 51),
        M, getIndicesInRange(50, 51),
        CLAIM, ImmutableList.of("2cards", "rankA"));

    assertMoveOk(move(wId, state, claimCheaterByW));
  }

  @Test
  public void testCannotClaimCheaterWhenMiddlePileIsEmpty() {
    assertHacker(move(wId, turnOfWEmptyMiddle, claimCheaterByW));
  }

  @Test
  public void testBlackIsIndeedCheater() {
    Map<String, Object> state = ImmutableMap.<String, Object>builder()
        .put(TURN, W)
        .put(IS_CHEATER, YES)
        .put("C50", "Ah")
        .put("C51", "Kh")
        .put(W, getIndicesInRange(0, 10))
        .put(B, getIndicesInRange(11, 49))
        .put(M, getIndicesInRange(50, 51))
        .put(CLAIM, ImmutableList.of("2cards", "rankA"))
        .build();

    // The order of operations: turn, isCheater, W, B, M, claim, C0...C51
    List<Operation> operations = ImmutableList.<Operation>of(
        new Set(TURN, B),
        new Delete(IS_CHEATER),
        new Set(B, getIndicesInRange(11, 51)),
        new Set(M, ImmutableList.of()),
        new SetVisibility("C50", visibleToB),
        new SetVisibility("C51", visibleToB),
        new Shuffle(getCardsInRange(11, 51)));

    assertMoveOk(move(wId, state, operations));
    assertHacker(move(bId, state, operations));
    assertHacker(move(wId, emptyState, operations));
    assertHacker(move(wId, turnOfWEmptyMiddle, operations));
  }

  @Test
  public void testBlackWasNotCheating() {
    Map<String, Object> state = ImmutableMap.<String, Object>builder()
        .put(TURN, W)
        .put(IS_CHEATER, YES)
        .put("C50", "Ah")
        .put("C51", "Ah")
        .put(W, getIndicesInRange(0, 10))
        .put(B, getIndicesInRange(11, 49))
        .put(M, getIndicesInRange(50, 51))
        .put(CLAIM, ImmutableList.of("2cards", "rankA"))
        .build();

    List<String> wNewCards = concat(getCardsInRange(0, 10), getCardsInRange(50, 51));
    List<Integer> wNewIndices = concat(getIndicesInRange(0, 10), getIndicesInRange(50, 51));
    // The order of operations: turn, isCheater, W, B, M, claim, C0...C51
    List<Operation> operations = ImmutableList.<Operation>of(
        new Set(TURN, W),
        new Delete(IS_CHEATER),
        new Set(W, wNewIndices),
        new Set(M, ImmutableList.of()),
        new SetVisibility("C50", visibleToW),
        new SetVisibility("C51", visibleToW),
        new Shuffle(wNewCards));

    assertMoveOk(move(wId, state, operations));
    assertHacker(move(bId, state, operations));
    assertHacker(move(wId, emptyState, operations));
    assertHacker(move(wId, turnOfWEmptyMiddle, operations));
  }

  @Test
  public void testIncreasePreviousClaim() {
    assertMoveOk(getChangePreviousClaim("2"));
  }

  @Test
  public void testDecreasePreviousClaim() {
    assertMoveOk(getChangePreviousClaim("K"));
  }

  @Test
  public void testKeepPreviousClaim() {
    assertMoveOk(getChangePreviousClaim("A"));
  }

  @Test
  public void testIllegalNextClaim() {
    assertHacker(getChangePreviousClaim("Q"));
    assertHacker(getChangePreviousClaim("10"));
    assertHacker(getChangePreviousClaim("3"));
  }

  private VerifyMove getChangePreviousClaim(String newRank) {
    Map<String, Object> state = ImmutableMap.<String, Object>of(
        TURN, W,
        W, getIndicesInRange(0, 10),
        B, getIndicesInRange(11, 49),
        M, getIndicesInRange(50, 51),
        CLAIM, ImmutableList.of("2cards", "rankA"));
    // The order of operations: turn, isCheater, W, B, M, claim, C0...C51
    List<Operation> claimByW = ImmutableList.<Operation>of(
        new Set(TURN, B),
        new Set(W, getIndicesInRange(4, 10)),
        new Set(M, concat(getIndicesInRange(50, 51), getIndicesInRange(0, 3))),
        new Set(CLAIM, ImmutableList.of("4cards", "rank" + newRank)));
    return move(wId, state, claimByW);
  }

}
