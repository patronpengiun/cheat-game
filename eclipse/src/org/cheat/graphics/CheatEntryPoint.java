package org.cheat.graphics;

import java.util.List;
import java.util.Map;

import org.cheat.client.Card;
import org.cheat.client.Card.Rank;
import org.cheat.client.Card.Suit;
import org.cheat.client.CheatLogic;
import org.cheat.client.CheatPresenter;
import org.cheat.client.Claim;
import org.cheat.client.GameApi.Operation;
import org.cheat.client.GameApi.SetTurn;
import org.cheat.client.GameApi.UpdateUI;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class CheatEntryPoint implements EntryPoint {
  @Override
  public void onModuleLoad() {
    CheatGraphics cheatGraphics = new CheatGraphics();
    CheatPresenter cheatPresenter = new CheatPresenter(cheatGraphics, new AlertingContainer());
    cheatPresenter.updateUI(createUpdateUI(wId, wId, nonEmptyMiddle));
    RootPanel.get("mainDiv").add(cheatGraphics);
  }


  private static final String PLAYER_ID = "playerId";
  private static final String C = "C"; // Card i
  private static final String W = "W"; // White hand
  private static final String B = "B"; // Black hand
  private static final String M = "M"; // Middle pile
  private static final String CLAIM = "claim"; // a claim has the form: [3cards, rankK]
  private static final String IS_CHEATER = "isCheater"; // we claim we have a cheater
  private static final String YES = "yes"; // we claim we have a cheater
  private final CheatLogic cheatLogic = new CheatLogic();
  private final int wId = 41;
  private final int bId = 42;
  private final ImmutableMap<String, Object> wInfo =
      ImmutableMap.<String, Object>of(PLAYER_ID, wId);
  private final ImmutableMap<String, Object> bInfo =
      ImmutableMap.<String, Object>of(PLAYER_ID, bId);
  private final ImmutableList<Map<String, Object>> playersInfo =
      ImmutableList.<Map<String, Object>>of(wInfo, bInfo);

  private final ImmutableMap<String, Object> emptyState = ImmutableMap.<String, Object>of();
  private final ImmutableMap<String, Object> nonEmptyMiddle =
      createState(10, 10, false, Optional.of(new Claim(Rank.ACE, 4)));

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

  private List<Card> getCards(int fromInclusive, int toExclusive) {
    List<Card> cards = Lists.newArrayList();
    for (int i = fromInclusive; i < toExclusive; i++) {
      Rank rank = Rank.values()[i / 4];
      Suit suit = Suit.values()[i % 4];
      cards.add(new Card(suit, rank));
    }
    return cards;
  }

  private UpdateUI createUpdateUI(
      int yourPlayerId, int turnOfPlayerId, Map<String, Object> state) {
    return new UpdateUI(yourPlayerId, playersInfo, state,
        emptyState, // we ignore lastState
        ImmutableList.<Operation>of(new SetTurn(turnOfPlayerId)),
        0,
        ImmutableMap.<Integer, Integer>of());
  }
}