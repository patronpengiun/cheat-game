package org.cheat.client;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.cheat.client.Card.Rank;
import org.cheat.client.Card.Suit;
import org.cheat.client.CheatPresenter.CheaterMessage;
import org.cheat.client.CheatPresenter.View;
import org.cheat.client.GameApi.Container;
import org.cheat.client.GameApi.Operation;
import org.cheat.client.GameApi.SetTurn;
import org.cheat.client.GameApi.UpdateUI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/** Tests for {@link CheatPresenter}.
 * Test plan:
 * There are several interesting states:
 * 1) empty state
 * 2) empty middle (no previous claim)
 * 3) non-empty middle (previous claim - can either claim or declare cheater)
 * 4) about-to-end (opponent must declare cheater)
 * 5) declared-cheater and indeed cheated
 * 6) declared-cheater and did not cheat
 * 7) game-over
 * There are several interesting yourPlayerId:
 * 1) white player
 * 2) black player
 * 3) viewer
 * For each one of these states and for each yourPlayerId,
 * I will test what methods the presenters calls on the view and container.
 * In addition I will also test the interactions between the presenter and view, i.e.,
 * the view can call one of these methods:
 * 1) cardSelected
 * 2) finishedSelectingCards
 * 3) rankSelected
 * 4) declaredCheater
 */
@RunWith(JUnit4.class)
public class CheatPresenterTest {
  /** The class under test. */
  private CheatPresenter cheatPresenter;
  private final CheatLogic cheatLogic = new CheatLogic();
  private View mockView;
  private Container mockContainer;

  private static final String PLAYER_ID = "playerId";
  /* The entries used in the cheat game are:
   *   isCheater:yes, W, B, M, claim, C0...C51
   */
  private static final String C = "C"; // Card i
  private static final String W = "W"; // White hand
  private static final String B = "B"; // Black hand
  private static final String M = "M"; // Middle pile
  private static final String CLAIM = "claim"; // a claim has the form: [3cards, rankK]
  private static final String IS_CHEATER = "isCheater"; // we claim we have a cheater
  private static final String YES = "yes"; // we claim we have a cheater
  private final int viewerId = GameApi.VIEWER_ID;
  private final int wId = 41;
  private final int bId = 42;
  private final ImmutableList<Integer> playerIds = ImmutableList.of(wId, bId);
  private final ImmutableMap<String, Object> wInfo =
      ImmutableMap.<String, Object>of(PLAYER_ID, wId);
  private final ImmutableMap<String, Object> bInfo =
      ImmutableMap.<String, Object>of(PLAYER_ID, bId);
  private final ImmutableList<Map<String, Object>> playersInfo =
      ImmutableList.<Map<String, Object>>of(wInfo, bInfo);

  /* The interesting states that I'll test. */
  private final ImmutableMap<String, Object> emptyState = ImmutableMap.<String, Object>of();
  private final ImmutableMap<String, Object> emptyMiddle =
      createState(10, 42, false, Optional.<Claim>absent());
  private final ImmutableMap<String, Object> nonEmptyMiddle =
      createState(10, 10, false, Optional.of(new Claim(Rank.ACE, 4)));
  private final ImmutableMap<String, Object> mustDeclareCheater =
      createState(0, 10, false, Optional.of(new Claim(Rank.ACE, 4)));
  private final ImmutableMap<String, Object> declaredCheaterAndIndeedCheated =
      createState(0, 10, true, Optional.of(new Claim(Rank.KING, 4)));
  private final ImmutableMap<String, Object> declaredCheaterAndDidNotCheat =
      createState(0, 10, true, Optional.of(new Claim(Rank.ACE, 4)));
  private final ImmutableMap<String, Object> gameOver =
      createState(0, 52, false, Optional.<Claim>absent());

  @Before
  public void runBefore() {
    mockView = Mockito.mock(View.class);
    mockContainer = Mockito.mock(Container.class);
    cheatPresenter = new CheatPresenter(mockView, mockContainer);
    verify(mockView).setPresenter(cheatPresenter);
  }

  @After
  public void runAfter() {
    // This will ensure I didn't forget to declare any extra interaction the mocks have.
    verifyNoMoreInteractions(mockContainer);
    verifyNoMoreInteractions(mockView);
  }

  @Test
  public void testEmptyStateForW() {
    cheatPresenter.updateUI(createUpdateUI(wId, 0, emptyState));
    verify(mockContainer).sendMakeMove(cheatLogic.getMoveInitial(playerIds));
  }

  @Test
  public void testEmptyStateForB() {
    cheatPresenter.updateUI(createUpdateUI(bId, 0, emptyState));
  }

  @Test
  public void testEmptyStateForViewer() {
    cheatPresenter.updateUI(createUpdateUI(viewerId, 0, emptyState));
  }

  @Test
  public void testEmptyMiddleStateForWTurnOfW() {
    cheatPresenter.updateUI(createUpdateUI(wId, wId, emptyMiddle));
    verify(mockView).setPlayerState(42, 0, getCards(0, 10), CheaterMessage.INVISIBLE);
    verify(mockView).chooseNextCard(ImmutableList.<Card>of(), getCards(0, 10));
  }

  @Test
  public void testEmptyMiddleStateForWTurnOfB() {
    cheatPresenter.updateUI(createUpdateUI(wId, bId, emptyMiddle));
    verify(mockView).setPlayerState(42, 0, getCards(0, 10), CheaterMessage.INVISIBLE);
  }

  @Test
  public void testEmptyMiddleStateForBTurnOfB() {
    cheatPresenter.updateUI(createUpdateUI(bId, bId, emptyMiddle));
    verify(mockView).setPlayerState(10, 0, getCards(10, 52), CheaterMessage.INVISIBLE);
    verify(mockView).chooseNextCard(ImmutableList.<Card>of(), getCards(10, 52));
  }

  @Test
  public void testEmptyMiddleStateForBTurnOfW() {
    cheatPresenter.updateUI(createUpdateUI(bId, wId, emptyMiddle));
    verify(mockView).setPlayerState(10, 0, getCards(10, 52), CheaterMessage.INVISIBLE);
  }

  @Test
  public void testEmptyMiddleStateForViewerTurnOfW() {
    cheatPresenter.updateUI(createUpdateUI(viewerId, wId, emptyMiddle));
    verify(mockView).setViewerState(10, 42, 0, CheaterMessage.INVISIBLE);
  }

  @Test
  public void testEmptyMiddleStateForViewerTurnOfB() {
    cheatPresenter.updateUI(createUpdateUI(viewerId, bId, emptyMiddle));
    verify(mockView).setViewerState(10, 42, 0, CheaterMessage.INVISIBLE);
  }

  @Test
  public void testNonEmptyMiddleStateForWTurnOfW() {
    cheatPresenter.updateUI(createUpdateUI(wId, wId, nonEmptyMiddle));
    verify(mockView).setPlayerState(10, 32, getCards(0, 10), CheaterMessage.IS_OPPONENT_CHEATING);
    verify(mockView).chooseNextCard(ImmutableList.<Card>of(), getCards(0, 10));
  }

  @Test
  public void testNonEmptyMiddleStateForWTurnOfB() {
    cheatPresenter.updateUI(createUpdateUI(wId, bId, nonEmptyMiddle));
    verify(mockView).setPlayerState(10, 32, getCards(0, 10), CheaterMessage.INVISIBLE);
  }

  @Test
  public void testNonEmptyMiddleStateForBTurnOfB() {
    cheatPresenter.updateUI(createUpdateUI(bId, bId, nonEmptyMiddle));
    verify(mockView).setPlayerState(10, 32, getCards(10, 20), CheaterMessage.IS_OPPONENT_CHEATING);
    verify(mockView).chooseNextCard(ImmutableList.<Card>of(), getCards(10, 20));
  }

  @Test
  public void testNonEmptyMiddleStateForBTurnOfW() {
    cheatPresenter.updateUI(createUpdateUI(bId, wId, nonEmptyMiddle));
    verify(mockView).setPlayerState(10, 32, getCards(10, 20), CheaterMessage.INVISIBLE);
  }

  @Test
  public void testNonEmptyMiddleStateForViewerTurnOfW() {
    cheatPresenter.updateUI(createUpdateUI(viewerId, wId, nonEmptyMiddle));
    verify(mockView).setViewerState(10, 10, 32, CheaterMessage.INVISIBLE);
  }

  @Test
  public void testMustDeclareCheaterStateForW() {
    cheatPresenter.updateUI(createUpdateUI(wId, bId, mustDeclareCheater));
    verify(mockView).setPlayerState(10, 42, getCards(0, 0), CheaterMessage.INVISIBLE);
  }

  @Test
  public void testMustDeclareCheaterStateForB() {
    cheatPresenter.updateUI(createUpdateUI(bId, bId, mustDeclareCheater));
    verify(mockView).setPlayerState(0, 42, getCards(0, 10), CheaterMessage.IS_OPPONENT_CHEATING);
    // Note that B doesn't have chooseNextCard, because he has to declare cheater.
  }

  @Test
  public void testMustDeclareCheaterStateForViewer() {
    cheatPresenter.updateUI(createUpdateUI(viewerId, bId, mustDeclareCheater));
    verify(mockView).setViewerState(0, 10, 42, CheaterMessage.INVISIBLE);
  }

  @Test
  public void testDeclaredCheaterAndIndeedCheatedStateForW() {
    cheatPresenter.updateUI(createUpdateUI(wId, bId, declaredCheaterAndIndeedCheated));
    verify(mockView).setPlayerState(10, 42, getCards(0, 0), CheaterMessage.WAS_CHEATING);
  }

  @Test
  public void testDeclaredCheaterAndIndeedCheatedStateForB() {
    UpdateUI updateUI = createUpdateUI(bId, bId, declaredCheaterAndIndeedCheated);
    CheatState cheatState =
        cheatLogic.gameApiStateToCheatState(updateUI.getState(), Color.B, playerIds);
    cheatPresenter.updateUI(updateUI);
    verify(mockView).setPlayerState(0, 42, getCards(0, 10), CheaterMessage.WAS_CHEATING);
    verify(mockContainer).sendMakeMove(cheatLogic.getMoveCheckIfCheated(cheatState));
  }

  @Test
  public void testDeclaredCheaterAndIndeedCheatedStateForViewer() {
    cheatPresenter.updateUI(createUpdateUI(viewerId, bId, declaredCheaterAndIndeedCheated));
    verify(mockView).setViewerState(0, 10, 42, CheaterMessage.WAS_CHEATING);
  }

  @Test
  public void testDeclaredCheaterAndDidNotCheatStateForW() {
    cheatPresenter.updateUI(createUpdateUI(wId, bId, declaredCheaterAndDidNotCheat));
    verify(mockView).setPlayerState(10, 42, getCards(0, 0), CheaterMessage.WAS_NOT_CHEATING);
  }

  @Test
  public void testDeclaredCheaterAndDidNotCheatStateForB() {
    UpdateUI updateUI = createUpdateUI(bId, bId, declaredCheaterAndDidNotCheat);
    CheatState cheatState =
        cheatLogic.gameApiStateToCheatState(updateUI.getState(), Color.B, playerIds);
    cheatPresenter.updateUI(updateUI);
    verify(mockView).setPlayerState(0, 42, getCards(0, 10), CheaterMessage.WAS_NOT_CHEATING);
    verify(mockContainer).sendMakeMove(cheatLogic.getMoveCheckIfCheated(cheatState));
  }

  @Test
  public void testDeclaredCheaterAndDidNotCheatStateForViewer() {
    cheatPresenter.updateUI(createUpdateUI(viewerId, bId, declaredCheaterAndDidNotCheat));
    verify(mockView).setViewerState(0, 10, 42, CheaterMessage.WAS_NOT_CHEATING);
  }

  @Test
  public void testGameOverStateForW() {
    cheatPresenter.updateUI(createUpdateUI(wId, bId, gameOver));
    verify(mockView).setPlayerState(52, 0, getCards(0, 0), CheaterMessage.INVISIBLE);
  }

  @Test
  public void testGameOverStateForB() {
    cheatPresenter.updateUI(createUpdateUI(bId, bId, gameOver));
    verify(mockView).setPlayerState(0, 0, getCards(0, 52), CheaterMessage.INVISIBLE);
  }

  @Test
  public void testGameOverStateForViewer() {
    cheatPresenter.updateUI(createUpdateUI(viewerId, bId, gameOver));
    verify(mockView).setViewerState(0, 52, 0, CheaterMessage.INVISIBLE);
  }

  /* Tests for preparing a claim. */
  @Test
  public void testEmptyMiddleStateForWTurnOfWPrepareClaimWithTwoCards() {
    UpdateUI updateUI = createUpdateUI(wId, wId, emptyMiddle);
    CheatState cheatState =
        cheatLogic.gameApiStateToCheatState(updateUI.getState(), Color.W, playerIds);
    cheatPresenter.updateUI(updateUI);
    List<Card> myCards = getCards(0, 10);
    cheatPresenter.cardSelected(myCards.get(0));
    cheatPresenter.cardSelected(myCards.get(1));
    cheatPresenter.finishedSelectingCards();
    cheatPresenter.rankSelected(Rank.ACE);
    verify(mockView).setPlayerState(42, 0, myCards, CheaterMessage.INVISIBLE);
    verify(mockView).chooseNextCard(ImmutableList.<Card>of(), myCards);
    verify(mockView).chooseNextCard(getCards(0, 1), getCards(1, 10));
    verify(mockView).chooseNextCard(getCards(0, 2), getCards(2, 10));
    verify(mockView).chooseRankForClaim(Arrays.asList(Rank.values()));
    verify(mockContainer).sendMakeMove(
        cheatLogic.getMoveClaim(cheatState, Rank.ACE, ImmutableList.of(0, 1)));
  }

  @Test
  public void testEmptyMiddleStateForWTurnOfWPrepareClaimAndUnselectOneCard() {
    UpdateUI updateUI = createUpdateUI(wId, wId, emptyMiddle);
    CheatState cheatState =
        cheatLogic.gameApiStateToCheatState(updateUI.getState(), Color.W, playerIds);
    cheatPresenter.updateUI(updateUI);
    List<Card> myCards = getCards(0, 10);
    cheatPresenter.cardSelected(myCards.get(0));
    cheatPresenter.cardSelected(myCards.get(1));
    cheatPresenter.cardSelected(myCards.get(1)); // remove card 1
    cheatPresenter.finishedSelectingCards();
    cheatPresenter.rankSelected(Rank.ACE);
    verify(mockView).setPlayerState(42, 0, myCards, CheaterMessage.INVISIBLE);
    verify(mockView).chooseNextCard(ImmutableList.<Card>of(), myCards);
    verify(mockView, times(2)).chooseNextCard(getCards(0, 1), getCards(1, 10));
    verify(mockView).chooseNextCard(getCards(0, 2), getCards(2, 10));
    verify(mockView).chooseRankForClaim(Arrays.asList(Rank.values()));
    verify(mockContainer).sendMakeMove(
        cheatLogic.getMoveClaim(cheatState, Rank.ACE, ImmutableList.of(0)));
  }

  @Test
  public void testNonEmptyMiddleStateForWTurnOfWDeclareCheater() {
    UpdateUI updateUI = createUpdateUI(wId, wId, nonEmptyMiddle);
    CheatState cheatState =
        cheatLogic.gameApiStateToCheatState(updateUI.getState(), Color.W, playerIds);
    cheatPresenter.updateUI(updateUI);
    cheatPresenter.declaredCheater();
    verify(mockView).setPlayerState(10, 32, getCards(0, 10), CheaterMessage.IS_OPPONENT_CHEATING);
    verify(mockView).chooseNextCard(ImmutableList.<Card>of(), getCards(0, 10));
    verify(mockContainer).sendMakeMove(
        cheatLogic.getMoveDeclareCheater(cheatState));
  }

  private List<Card> getCards(int fromInclusive, int toExclusive) {
    List<Card> cards = Lists.newArrayList();
    for (int i = fromInclusive; i < toExclusive; i++) {
      Rank rank = Rank.values()[i / 4];
      Suit suit = Suit.values()[i % 4];
      cards.add(new Card(suit, rank));
    }
    return cards;
  }

  private ImmutableMap<String, Object> createState(
      int numberOfWhiteCards, int numberOfBlackCards, boolean isCheater, Optional<Claim> claim) {
    Map<String, Object> state = Maps.newHashMap();
    state.put(W, cheatLogic.getIndicesInRange(0, numberOfWhiteCards - 1));
    state.put(B, cheatLogic.getIndicesInRange(numberOfWhiteCards,
        numberOfWhiteCards + numberOfBlackCards - 1));
    state.put(M, cheatLogic.getIndicesInRange(numberOfWhiteCards + numberOfBlackCards, 51));
    if (isCheater) {
      state.put(IS_CHEATER, YES);
    }
    if (claim.isPresent()) {
      state.put(CLAIM, Claim.toClaimEntryInGameState(claim.get()));
    }
    // We just reveal all the cards (hidden cards are not relevant for our testing).
    int i = 0;
    for (Card card : getCards(0, 52)) {
      state.put(C + (i++),
          card.getRank().getFirstLetter() + card.getSuit().getFirstLetterLowerCase());
    }
    return ImmutableMap.copyOf(state);
  }

  private UpdateUI createUpdateUI(
      int yourPlayerId, int turnOfPlayerId, Map<String, Object> state) {
    // Our UI only looks at the current state
    // (we ignore: lastState, lastMovePlayerId, playerIdToNumberOfTokensInPot)
    return new UpdateUI(yourPlayerId, playersInfo, state,
        emptyState, // we ignore lastState
        ImmutableList.<Operation>of(new SetTurn(turnOfPlayerId)),
        0,
        ImmutableMap.<Integer, Integer>of());
  }
}
