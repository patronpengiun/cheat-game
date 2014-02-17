package org.cheat.client;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.cheat.client.GameApi.Delete;
import org.cheat.client.GameApi.EndGame;
import org.cheat.client.GameApi.Operation;
import org.cheat.client.GameApi.Set;
import org.cheat.client.GameApi.SetTurn;
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
   *   isCheater:yes, W, B, M, claim, C0...C51
   * When we send operations on these keys, it will always be in the above order.
   */
  private static final String W = "W"; // White hand
  private static final String B = "B"; // Black hand
  private static final String M = "M"; // Middle pile
  private static final String CLAIM = "claim"; // a claim has the form: [3cards, rankK]
  private static final String IS_CHEATER = "isCheater"; // we claim we have a cheater
  private static final String YES = "yes"; // we claim we have a cheater
  private final int wId = 41;
  private final int bId = 42;
  private final ImmutableList<Integer> visibleToW = ImmutableList.of(wId);
  private final ImmutableList<Integer> visibleToB = ImmutableList.of(bId);
  private final ImmutableMap<String, Object> wInfo =
      ImmutableMap.<String, Object>of(PLAYER_ID, wId);
  private final ImmutableMap<String, Object> bInfo =
      ImmutableMap.<String, Object>of(PLAYER_ID, bId);
  private final ImmutableList<Map<String, Object>> playersInfo =
      ImmutableList.<Map<String, Object>>of(wInfo, bInfo);
  private final ImmutableMap<String, Object> emptyState = ImmutableMap.<String, Object>of();
  private final ImmutableMap<String, Object> nonEmptyState =
      ImmutableMap.<String, Object>of("k", "v");

  private final ImmutableMap<String, Object> emptyMiddle = ImmutableMap.<String, Object>of(
      W, getIndicesInRange(0, 10),
      B, getIndicesInRange(11, 51),
      M, ImmutableList.of());

  // The order of operations: turn, isCheater, W, B, M, claim, C0...C51
  private final ImmutableList<Operation> claimOfW = ImmutableList.<Operation>of(
      new SetTurn(bId),
      new Set(W, getIndicesInRange(0, 8)),
      new Set(M, getIndicesInRange(9, 10)),
      new Set(CLAIM, ImmutableList.of("2cards", "rankA")));

  private final ImmutableList<Operation> claimOfB = ImmutableList.<Operation>of(
      new SetTurn(wId),
      new Set(B, getIndicesInRange(11, 48)),
      new Set(M, getIndicesInRange(49, 51)),
      new Set(CLAIM, ImmutableList.of("3cards", "rankJ")));

  private VerifyMove move(
      int lastMovePlayerId, Map<String, Object> lastState, List<Operation> lastMove) {
    return new VerifyMove(playersInfo,
        // in cheat we never need to check the resulting state (the server makes it, and the game
        // doesn't have any hidden decisions such in Battleships)
        emptyState,
        lastState, lastMove, lastMovePlayerId, ImmutableMap.<Integer, Integer>of());
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
    return cheatLogic.getMoveInitial(ImmutableList.of(wId, bId));
  }

  @Test
  public void testGetInitialOperationsSize() {
    assertEquals(4 + 52 + 1 + 52, cheatLogic.getMoveInitial(ImmutableList.of(wId, bId)).size());
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
    assertMoveOk(move(wId, emptyMiddle, claimOfW));
  }

  @Test
  public void testNormalClaimByBlack() {
    assertMoveOk(move(bId, emptyMiddle, claimOfB));
  }

  @Test
  public void testIllegalClaimByWrongColor() {
    assertHacker(move(bId, emptyMiddle, claimOfW));
    assertHacker(move(wId, emptyMiddle, claimOfB));
  }

  @Test
  public void testClaimWithWrongCards() {
    ImmutableList<Operation> illegalClaimWithWrongCards = ImmutableList.<Operation>of(
        new SetTurn(bId),
        new Set(W, getIndicesInRange(0, 8)),
        new Set(M, getIndicesInRange(9, 10)),
        new Set(CLAIM, ImmutableList.of("3cards", "rankA")));
    assertHacker(move(wId, emptyMiddle, illegalClaimWithWrongCards));
  }

  @Test
  public void testClaimWithWrongW() {
    ImmutableList<Operation> illegalClaimWithWrongW = ImmutableList.<Operation>of(
        new SetTurn(bId),
        new Set(W, getIndicesInRange(0, 7)),
        new Set(M, getIndicesInRange(9, 10)),
        new Set(CLAIM, ImmutableList.of("2cards", "rankA")));
    assertHacker(move(wId, emptyMiddle, illegalClaimWithWrongW));
  }

  @Test
  public void testClaimWithWrongM() {
    ImmutableList<Operation> illegalClaimWithWrongM = ImmutableList.<Operation>of(
        new SetTurn(bId),
        new Set(W, getIndicesInRange(0, 8)),
        new Set(M, getIndicesInRange(8, 10)),
        new Set(CLAIM, ImmutableList.of("2cards", "rankA")));
    assertHacker(move(wId, emptyMiddle, illegalClaimWithWrongM));
  }

  @Test
  public void testAnnouceCheaterByWhite() {
    Map<String, Object> state = ImmutableMap.<String, Object>of(
        W, getIndicesInRange(0, 10),
        B, getIndicesInRange(11, 51),
        M, getIndicesInRange(50, 51),
        CLAIM, ImmutableList.of("2cards", "rankA"));
    ImmutableList<Operation> annouceCheaterByW = ImmutableList.<Operation>of(
        new SetTurn(wId),
        new Set(IS_CHEATER, YES),
        new SetVisibility("C50"), new SetVisibility("C51"));
    assertMoveOk(move(wId, state, annouceCheaterByW));
    assertHacker(move(wId, emptyMiddle, annouceCheaterByW));
  }

  @Test
  public void testBlackIsIndeedCheater() {
    Map<String, Object> state = ImmutableMap.<String, Object>builder()
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
        new SetTurn(bId),
        new Delete(IS_CHEATER),
        new Set(B, getIndicesInRange(11, 51)),
        new Set(M, ImmutableList.of()),
        new SetVisibility("C50", visibleToB),
        new SetVisibility("C51", visibleToB),
        new Shuffle(getCardsInRange(11, 51)));

    assertMoveOk(move(wId, state, operations));
    assertHacker(move(bId, state, operations));
    assertHacker(move(wId, emptyState, operations));
  }

  @Test
  public void testBlackWasNotCheating() {
    Map<String, Object> state = ImmutableMap.<String, Object>builder()
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
        new SetTurn(wId),
        new Delete(IS_CHEATER),
        new Set(W, wNewIndices),
        new Set(M, ImmutableList.of()),
        new SetVisibility("C50", visibleToW),
        new SetVisibility("C51", visibleToW),
        new Shuffle(wNewCards));

    assertMoveOk(move(wId, state, operations));
    assertHacker(move(bId, state, operations));
    assertHacker(move(wId, emptyState, operations));
    assertHacker(move(wId, emptyMiddle, operations));
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
        W, getIndicesInRange(0, 10),
        B, getIndicesInRange(11, 49),
        M, getIndicesInRange(50, 51),
        CLAIM, ImmutableList.of("2cards", "rankA"));
    // The order of operations: turn, isCheater, W, B, M, claim, C0...C51
    List<Operation> claimByW = ImmutableList.<Operation>of(
        new SetTurn(bId),
        new Set(W, getIndicesInRange(4, 10)),
        new Set(M, concat(getIndicesInRange(50, 51), getIndicesInRange(0, 3))),
        new Set(CLAIM, ImmutableList.of("4cards", "rank" + newRank)));
    return move(wId, state, claimByW);
  }

  @Test
  public void testMustAnnouceCheaterWhenOpponentHandIsEmpty() {
    Map<String, Object> whiteHasNoCardsState = ImmutableMap.<String, Object>of(
        W, ImmutableList.of(),
        B, getIndicesInRange(2, 51),
        M, getIndicesInRange(0, 1),
        CLAIM, ImmutableList.of("1cards", "rankA"));
    Map<String, Object> whiteHasCardsState = ImmutableMap.<String, Object>of(
        W, getIndicesInRange(50, 51),
        B, getIndicesInRange(2, 49),
        M, getIndicesInRange(0, 1),
        CLAIM, ImmutableList.of("1cards", "rankA"));

    List<Operation> annouceCheaterByB = ImmutableList.<Operation>of(
        new SetTurn(bId),
        new Set(IS_CHEATER, YES),
        new SetVisibility("C0"), new SetVisibility("C1"));
    List<Operation> claimByBWhenWhiteHasNoCards = ImmutableList.<Operation>of(
        new SetTurn(wId),
        new Set(B, getIndicesInRange(3, 51)),
        new Set(M, getIndicesInRange(0, 2)),
        new Set(CLAIM, ImmutableList.of("1cards", "rankA")));
    List<Operation> claimByBWhenWhiteHasCards = ImmutableList.<Operation>of(
        new SetTurn(wId),
        new Set(B, getIndicesInRange(3, 49)),
        new Set(M, getIndicesInRange(0, 2)),
        new Set(CLAIM, ImmutableList.of("1cards", "rankA")));

    // When W has cards, B can both announce cheater or make a normal claim
    // When W has NO cards, B can only announce cheater
    assertMoveOk(move(bId, whiteHasCardsState, annouceCheaterByB));
    assertMoveOk(move(bId, whiteHasCardsState, claimByBWhenWhiteHasCards));
    assertMoveOk(move(bId, whiteHasNoCardsState, annouceCheaterByB));
    assertHacker(move(bId, whiteHasNoCardsState, claimByBWhenWhiteHasNoCards));
  }

  @Test
  public void testEndGame() {
    Map<String, Object> state = ImmutableMap.<String, Object>builder()
        .put(IS_CHEATER, YES)
        .put("C50", "Ah")
        .put("C51", "As")
        .put(W, getIndicesInRange(0, 49))
        .put(B, ImmutableList.of())
        .put(M, getIndicesInRange(50, 51))
        .put(CLAIM, ImmutableList.of("2cards", "rankA"))
        .build();
    // The order of operations: turn, isCheater, W, B, M, claim, C0...C51
    List<Operation> operations = ImmutableList.of(
        new SetTurn(wId),
        new Delete(IS_CHEATER),
        new Set(W, getIndicesInRange(0, 51)),
        new Set(M, ImmutableList.of()),
        new SetVisibility("C50", visibleToW),
        new SetVisibility("C51", visibleToW),
        new Shuffle(getCardsInRange(0, 51)),
        new EndGame(bId));

    assertMoveOk(move(wId, state, operations));
  }
}
