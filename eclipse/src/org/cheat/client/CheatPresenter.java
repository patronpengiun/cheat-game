package org.cheat.client;

import java.util.List;

import org.cheat.client.Card.Rank;
import org.cheat.client.GameApi.Container;
import org.cheat.client.GameApi.Operation;
import org.cheat.client.GameApi.SetTurn;
import org.cheat.client.GameApi.UpdateUI;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * The presenter that controls the cheat graphics.
 * We use the MVP pattern:
 * the model is {@link CheatState},
 * the view will have the cheat graphics and it will implement {@link CheatPresenter.View},
 * and the presenter is {@link CheatPresenter}.
 */
public class CheatPresenter {
  /**
   * The possible cheater messages.
   * The cheater related messages are:
   * INVISIBLE: if previous claim is null, then we show nothing (e.g., on the first move)
   * IS_OPPONENT_CHEATING: ask the player whether the opponent is cheating.
   * WAS_CHEATING: the player cheated.
   * WAS_NOT_CHEATING: the player did not cheat.
   */
  public enum CheaterMessage {
    INVISIBLE, IS_OPPONENT_CHEATING, WAS_CHEATING, WAS_NOT_CHEATING;
  }

  public interface View {
    /**
     * Sets the presenter. The viewer will call certain methods on the presenter, e.g.,
     * when a card is selected ({@link #cardSelected}),
     * when selection is done ({@link #finishedSelectingCards}), etc.
     *
     * The process of making a claim looks as follows to the viewer:
     * 1) The viewer calls {@link #cardSelected} a couple of times to select the cards to drop to
     * the middle pile
     * 2) The viewer calls {@link #finishedSelectingCards} to finalize his selection
     * 3) The viewer calls {@link #rankSelected} to pass the selected rank, which sends the claim.
     * The process of making a claim looks as follows to the presenter:
     * 1) The presenter calls {@link #chooseNextCard} and passes the current selection.
     * 2) The presenter calls {@link #chooseRankForClaim} and passes the possible ranks.
     */
    void setPresenter(CheatPresenter cheatPresenter);

    /** Sets the state for a viewer, i.e., not one of the players. */
    void setViewerState(int numberOfWhiteCards, int numberOfBlackCards,
        int numberOfCardsInMiddlePile,
        CheaterMessage cheaterMessage,
        Optional<Claim> lastClaim);

    /**
     * Sets the state for a player (whether the player has the turn or not).
     * The "declare cheater" button should be enabled only for CheaterMessage.IS_OPPONENT_CHEATING.
     */
    void setPlayerState(int numberOfOpponentCards,
        int numberOfCardsInMiddlePile,
        List<Card> myCards,
        CheaterMessage cheaterMessage,
        Optional<Claim> lastClaim);

    /**
     * Asks the player to choose the next card or finish his selection.
     * We pass what cards are selected (those cards will be dropped to the
     * middle pile), and what cards will remain in the player hands.
     * The user can either select a card (by calling {@link #cardSelected),
     * or finish selecting
     * (by calling {@link #finishedSelectingCards}; only allowed if selectedCards.size>1).
     * If the user selects a card from selectedCards, then it moves that card to remainingCards.
     * If the user selects a card from remainingCards, then it moves that card to selectedCards.
     */
    void chooseNextCard(List<Card> selectedCards, List<Card> remainingCards);

    /**
     * After the player finished selecting 1-4 cards, the player needs to choose the rank for his
     * claim.
     * The possible ranks depend on the rank Y in the previous claim (possibleRanks is Y-1,Y,Y+1),
     * or all ranks if there wasn't a previous claim.
     */
    void chooseRankForClaim(List<Rank> possibleRanks);
  }

  private final CheatLogic cheatLogic = new CheatLogic();
  private final View view;
  private final Container container;
  /** A viewer doesn't have a color. */
  private Optional<Color> myColor;
  private CheatState cheatState;
  private List<Card> selectedCards;

  public CheatPresenter(View view, Container container) {
    this.view = view;
    this.container = container;
    view.setPresenter(this);
  }

  /** Updates the presenter and the view with the state in updateUI. */
  public void updateUI(UpdateUI updateUI) {
    List<Integer> playerIds = updateUI.getPlayerIds();
    int yourPlayerId = updateUI.getYourPlayerId();
    int yourPlayerIndex = updateUI.getPlayerIndex(yourPlayerId);
    myColor = yourPlayerIndex == 0 ? Optional.of(Color.W)
        : yourPlayerIndex == 1 ? Optional.of(Color.B) : Optional.<Color>absent();
    selectedCards = Lists.newArrayList();
    if (updateUI.getState().isEmpty()) {
      // The W player sends the initial setup move.
      if (myColor.isPresent() && myColor.get().isWhite()) {
        sendInitialMove(playerIds);
      }
      return;
    }
    Color turnOfColor = null;
    for (Operation operation : updateUI.getLastMove()) {
      if (operation instanceof SetTurn) {
        turnOfColor = Color.values()[playerIds.indexOf(((SetTurn) operation).getPlayerId())];
      }
    }
    cheatState = cheatLogic.gameApiStateToCheatState(updateUI.getState(), turnOfColor, playerIds);

    CheaterMessage cheaterMessage = getCheaterMessage();
    if (updateUI.isViewer()) {
      view.setViewerState(cheatState.getWhite().size(), cheatState.getBlack().size(),
          cheatState.getMiddle().size(), cheaterMessage, cheatState.getClaim());
      return;
    }
    if (updateUI.isAiPlayer()) {
      // TODO: implement AI in a later HW!
      //container.sendMakeMove(..);
      return;
    }
    // Must be a player!
    Color myC = myColor.get();
    Color opponent = myC.getOppositeColor();
    int numberOfOpponentCards = cheatState.getWhiteOrBlack(opponent).size();
    boolean mustDeclareCheater =
        isMyTurn() && !cheatState.isCheater() && numberOfOpponentCards == 0
        && cheatState.getMiddle().size() > 0;
    view.setPlayerState(numberOfOpponentCards, cheatState.getMiddle().size(),
        getMyCards(), mustDeclareCheater ? CheaterMessage.INVISIBLE : cheaterMessage,
        cheatState.getClaim());
    if (mustDeclareCheater) {
      declaredCheater();
    }
    if (isMyTurn()) {
      if (cheatState.isCheater()) {
        checkIfCheated();
      } else {
        // Choose the next card only if the game is not over
        if (numberOfOpponentCards > 0) {
          chooseNextCard();
        }
      }
    }
  }

  private boolean canDeclareCheater() {
    return !cheatState.isCheater() && isMyTurn() && cheatState.getClaim().isPresent();
  }

  private CheaterMessage getCheaterMessage() {
    if (cheatState.isCheater()) {
      return cheatLogic.didCheat(cheatState)
          ? CheaterMessage.WAS_CHEATING : CheaterMessage.WAS_NOT_CHEATING;
    }
    // Only the player that has the turn can declare the opponent is a cheater
    if (canDeclareCheater()) {
      return CheaterMessage.IS_OPPONENT_CHEATING;
    }
    return CheaterMessage.INVISIBLE;
  }

  private List<Rank> getPossibleRanks() {
    Optional<Claim> lastClaim = cheatState.getClaim();
    List<Rank> possibleRanks = Lists.newArrayList();
    if (lastClaim.isPresent()) {
      Rank lastRank = lastClaim.get().getCardRank();
      possibleRanks.add(lastRank.getPrev());
      possibleRanks.add(lastRank);
      possibleRanks.add(lastRank.getNext());
    } else {
      for (Rank rank : Rank.values()) {
        possibleRanks.add(rank);
      }
    }
    return possibleRanks;
  }

  private boolean isMyTurn() {
    return myColor.isPresent() && myColor.get() == cheatState.getTurn();
  }

  private List<Card> getMyCards() {
    List<Card> myCards = Lists.newArrayList();
    ImmutableList<Optional<Card>> cards = cheatState.getCards();
    for (Integer cardIndex : cheatState.getWhiteOrBlack(myColor.get())) {
      myCards.add(cards.get(cardIndex).get());
    }
    return myCards;
  }

  private void chooseNextCard() {
    view.chooseNextCard(
        Lists.newArrayList(selectedCards), cheatLogic.subtract(getMyCards(), selectedCards));
  }

  private void check(boolean val) {
    if (!val) {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Adds/remove the card from the {@link #selectedCards}.
   * The view can only call this method if the presenter called {@link View#chooseNextCard}.
   */
  public void cardSelected(Card card) {
    check(isMyTurn() && !cheatState.isCheater());
    if (selectedCards.contains(card)) {
      selectedCards.remove(card);
    } else if (!selectedCards.contains(card) && selectedCards.size() < 4) {
      selectedCards.add(card);
    }
    chooseNextCard();
  }

  /**
   * Finishes the card selection process.
   * The view can only call this method if the presenter called {@link View#chooseNextCard}
   * and more than one card was selected by calling {@link #cardSelected}.
   */
  public void finishedSelectingCards() {
    check(isMyTurn() && !selectedCards.isEmpty());
    view.chooseRankForClaim(getPossibleRanks());
  }

  /**
   * Selects a rank and sends a claim.
   * The view can only call this method if the presenter called {@link View#chooseRankForClaim}.
   */
  public void rankSelected(Rank rank) {
    check(isMyTurn() && !selectedCards.isEmpty() && getPossibleRanks().contains(rank));
    List<Integer> myCardIndices = cheatState.getWhiteOrBlack(cheatState.getTurn());
    List<Card> myCards = getMyCards();
    List<Integer> cardsToMoveToMiddle = Lists.newArrayList();
    for (Card card : selectedCards) {
      int cardIndex = myCardIndices.get(myCards.indexOf(card));
      cardsToMoveToMiddle.add(cardIndex);
    }
    container.sendMakeMove(cheatLogic.getMoveClaim(cheatState, rank, cardsToMoveToMiddle));
  }

  /**
   * Sends a move that the opponent is a cheater.
   * The view can only call this method if the presenter passed
   * CheaterMessage.IS_OPPONENT_CHEATING in {@link View#setPlayerState}.
   */
  public void declaredCheater() {
    check(canDeclareCheater());
    container.sendMakeMove(cheatLogic.getMoveDeclareCheater(cheatState));
  }

  private void checkIfCheated() {
    container.sendMakeMove(cheatLogic.getMoveCheckIfCheated(cheatState));
  }

  private void sendInitialMove(List<Integer> playerIds) {
    container.sendMakeMove(cheatLogic.getMoveInitial(playerIds));
  }
}
