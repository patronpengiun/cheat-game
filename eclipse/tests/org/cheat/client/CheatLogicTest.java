package org.cheat.client;

import static com.google.common.base.Preconditions.checkArgument;
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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@RunWith(JUnit4.class)
public class CheatLogicTest {

  private void assertMoveOk(VerifyMove verifyMove) {
    VerifyMoveDone verifyDone = new CheatLogic().verify(verifyMove);
    assertEquals(new VerifyMoveDone(), verifyDone);
  }

  private void assertHacker(VerifyMove verifyMove) {
    VerifyMoveDone verifyDone = new CheatLogic().verify(verifyMove);
    assertEquals(new VerifyMoveDone(verifyMove.getLastMovePlayerId(), "Hacker found"), verifyDone);
  }

  private final int wId = 41;
  private final int bId = 42;
  private final String playerId = "playerId";
  private final String turn = "turn"; // turn of which player (either W or B)
  private static final String W = "W"; // White hand
  private static final String B = "B"; // Black hand
  private static final String M = "M"; // Middle pile
  private static final String C = "C"; // Card key (C1 .. C54)
  private final String claim = "claim"; // a claim has the form: [3cards, rankK]
  private final String isCheater = "isCheater"; // we claim we have a cheater
  private final String yes = "yes"; // we claim we have a cheater
  private final List<Integer> visibleToW = ImmutableList.of(wId);
  private final List<Integer> visibleToB = ImmutableList.of(bId);
  private final Map<String, Object> wInfo = ImmutableMap.<String, Object>of(playerId, wId);
  private final Map<String, Object> bInfo = ImmutableMap.<String, Object>of(playerId, bId);
  private final List<Map<String, Object>> playersInfo = ImmutableList.of(wInfo, bInfo);
  private final Map<String, Object> emptyState = ImmutableMap.<String, Object>of();
  private final Map<String, Object> nonEmptyState = ImmutableMap.<String, Object>of("k", "v");

  private final Map<String, Object> turnOfWEmptyMiddle = ImmutableMap.<String, Object>of(
      turn, W,
      W, getCardsInRange(1, 10),
      B, getCardsInRange(11, 54),
      M, ImmutableList.of());

  Map<String, Object> turnOfBEmptyMiddle = ImmutableMap.<String, Object>of(
      turn, B,
      W, getCardsInRange(1, 10),
      B, getCardsInRange(11, 54),
      M, ImmutableList.of());

  private final List<Operation> claimOfW = ImmutableList.<Operation>of(
      new Set(turn, B),
      new Set(W, getCardsInRange(1, 8)),
      new Set(M, getCardsInRange(9, 10)),
      new Set(claim, ImmutableList.of("2cards", "rankA")));

  private final List<Operation> claimOfB = ImmutableList.<Operation>of(
      new Set(turn, W),
      new Set(B, getCardsInRange(11, 51)),
      new Set(M, getCardsInRange(52, 54)),
      new Set(claim, ImmutableList.of("3cards", "rankJ")));

  private final List<Operation> illegalClaimWithWrongCards = ImmutableList.<Operation>of(
      new Set(turn, B),
      new Set(W, getCardsInRange(1, 8)),
      new Set(M, getCardsInRange(9, 10)),
      new Set(claim, ImmutableList.of("3cards", "rankA")));

  private final List<Operation> illegalClaimWithWrongW = ImmutableList.<Operation>of(
      new Set(turn, B),
      new Set(W, getCardsInRange(1, 7)),
      new Set(M, getCardsInRange(9, 10)),
      new Set(claim, ImmutableList.of("2cards", "rankA")));

  private final List<Operation> illegalClaimWithWrongM = ImmutableList.<Operation>of(
      new Set(turn, B),
      new Set(W, getCardsInRange(1, 8)),
      new Set(M, getCardsInRange(8, 10)),
      new Set(claim, ImmutableList.of("2cards", "rankA")));


  private VerifyMove move(
      int lastMovePlayerId, Map<String, Object> lastState, List<Operation> lastMove) {
    return new VerifyMove(wId, playersInfo,
        // in cheat we never need to check the resulting state (the server makes it, and the game
        // doesn't have any hidden decisions such in Battleships)
        emptyState,
        lastState, lastMove, lastMovePlayerId);
  }

  private List<String> getCardsInRange(int fromInclusive, int toInclusive) {
    List<String> keys = Lists.newArrayList();
    for (int i = fromInclusive; i <= toInclusive; i++) {
      keys.add(C + i);
    }
    return keys;
  }

  private List<String> concat(List<String> a, List<String> b) {
    return Lists.newArrayList(Iterables.concat(a, b));
  }

  private String cardIdToString(int cardId) {
    checkArgument(cardId >= 0 && cardId < 54);
    int rank = (cardId / 4) + 2;
    String rankString = rank <= 10 ? String.valueOf(rank)
        : rank == 11 ? "J"
        : rank == 12 ? "Q"
        : rank == 13 ? "K" : "A";
    int suit = cardId % 4;
    String suitString = suit == 0 ? "h" : suit == 1 ? "s" : suit == 2 ? C : "d";
    return rankString + suitString;
  }

  private List<Operation> getInitialOperations() {
    List<Operation> operations = Lists.newArrayList();
    operations.add(new Set(turn, W));
    // sets all 54 cards: set(C1,2h), …, set(C54,Ac)
    for (int i = 1; i <= 54; i++) {
      operations.add(new Set(C + i, cardIdToString(i - 1)));
    }
    // shuffle(C1,...,C54)
    operations.add(new Shuffle(getCardsInRange(1, 54)));
    // set W and B hands
    operations.add(new Set(W, getCardsInRange(1, 28)));
    operations.add(new Set(B, getCardsInRange(29, 54)));
    // middle pile is empty
    operations.add(new Set(M, ImmutableList.of()));
    // sets visibility
    for (int i = 1; i <= 28; i++) {
      operations.add(new SetVisibility(C + i, visibleToW));
    }
    for (int i = 29; i <= 54; i++) {
      operations.add(new SetVisibility(C + i, visibleToB));
    }
    return operations;
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

  List<Operation> claimCheaterByW = ImmutableList.<Operation>of(
      new Set(turn, W),
      new Set(isCheater, yes),
      new SetVisibility("C53"), new SetVisibility("C54"));

  @Test
  public void testClaimCheaterByWhite() {
    Map<String, Object> state = ImmutableMap.<String, Object>of(
        turn, W,
        W, getCardsInRange(1, 10),
        B, getCardsInRange(11, 52),
        M, getCardsInRange(53, 54),
        claim, ImmutableList.of("2cards", "rankA"));

    assertMoveOk(move(wId, state, claimCheaterByW));
  }

  @Test
  public void testCannotClaimCheaterWhenMiddlePileIsEmpty() {
    assertHacker(move(wId, turnOfWEmptyMiddle, claimCheaterByW));
  }

  @Test
  public void testBlackIsIndeedCheater() {
    Map<String, Object> state = ImmutableMap.<String, Object>builder()
        .put(turn, W)
        .put(isCheater, yes)
        .put("C53", "Ah")
        .put("C54", "Kh")
        .put(W, getCardsInRange(1, 10))
        .put(B, getCardsInRange(11, 52))
        .put(M, getCardsInRange(53, 54))
        .put(claim, ImmutableList.of("2cards", "rankA"))
        .build();

    List<Operation> operations = ImmutableList.<Operation>of(
        new Set(turn, B),
        new Delete(isCheater),
        new Set(B, getCardsInRange(11, 54)),
        new Set(M, ImmutableList.of()),
        new SetVisibility("C53", visibleToB),
        new SetVisibility("C54", visibleToB),
        new Shuffle(getCardsInRange(11, 54)));

    assertMoveOk(move(wId, state, operations));
    assertHacker(move(bId, state, operations));
    assertHacker(move(wId, emptyState, operations));
    assertHacker(move(wId, turnOfWEmptyMiddle, operations));
  }

  @Test
  public void testBlackWasNotCheating() {
    Map<String, Object> state = ImmutableMap.<String, Object>builder()
        .put(turn, W)
        .put(isCheater, yes)
        .put("C53", "Ah")
        .put("C54", "Ah")
        .put(W, getCardsInRange(1, 10))
        .put(B, getCardsInRange(11, 52))
        .put(M, getCardsInRange(53, 54))
        .put(claim, ImmutableList.of("2cards", "rankA"))
        .build();

    List<String> wNewCards = concat(getCardsInRange(1, 10), getCardsInRange(53, 54));
    List<Operation> operations = ImmutableList.<Operation>of(
        new Set(turn, W),
        new Delete(isCheater),
        new Set(W, wNewCards),
        new Set(M, ImmutableList.of()),
        new SetVisibility("C53", visibleToW),
        new SetVisibility("C54", visibleToW),
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
        turn, W,
        W, getCardsInRange(1, 10),
        B, getCardsInRange(11, 52),
        M, getCardsInRange(53, 54),
        claim, ImmutableList.of("2cards", "rankA"));
    List<Operation> claimByW = ImmutableList.<Operation>of(
        new Set(turn, B),
        new Set(W, getCardsInRange(5, 10)),
        new Set(M, concat(getCardsInRange(53, 54), getCardsInRange(1, 4))),
        new Set(claim, ImmutableList.of("4cards", "rank" + newRank)));
    return move(wId, state, claimByW);
  }

}
