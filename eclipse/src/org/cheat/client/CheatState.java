package org.cheat.client;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * Representation of the cheat game state.
 * The game state uses these keys: isCheater, W, B, M, claim, C1...C54
 * which are mapped to these fields: isCheater, white, black, middle, claim, cards
 */
public class CheatState {
  private final Color turn;
  private final ImmutableList<Integer> playerIds;

  /**
   * Note that some of the entries will have null, meaning the card is not visible to us.
   */
  private final ImmutableList<Optional<Card>> cards;

  /**
   * Index of the white cards, each integer is in the range [0-54).
   */
  private final ImmutableList<Integer> white;
  private final ImmutableList<Integer> black;
  private final ImmutableList<Integer> middle;
  private final boolean isCheater;
  private final Optional<Claim> claim;

  public CheatState(Color turn, ImmutableList<Integer> playerIds,
      ImmutableList<Optional<Card>> cards, ImmutableList<Integer> white,
      ImmutableList<Integer> black, ImmutableList<Integer> middle, boolean isCheater,
      Optional<Claim> claim) {
    super();
    this.turn = checkNotNull(turn);
    this.playerIds = checkNotNull(playerIds);
    this.cards = checkNotNull(cards);
    this.white = checkNotNull(white);
    this.black = checkNotNull(black);
    this.middle = checkNotNull(middle);
    this.isCheater = isCheater;
    this.claim = claim;
  }

  public Color getTurn() {
    return turn;
  }

  public ImmutableList<Integer> getPlayerIds() {
    return playerIds;
  }

  public int getPlayerId(Color color) {
    return playerIds.get(color.ordinal());
  }

  public ImmutableList<Optional<Card>> getCards() {
    return cards;
  }

  public ImmutableList<Integer> getWhite() {
    return white;
  }

  public ImmutableList<Integer> getBlack() {
    return black;
  }

  public ImmutableList<Integer> getWhiteOrBlack(Color color) {
    return color.isWhite() ? white : black;
  }

  public ImmutableList<Integer> getMiddle() {
    return middle;
  }

  public boolean isCheater() {
    return isCheater;
  }

  public Optional<Claim> getClaim() {
    return claim;
  }
}
