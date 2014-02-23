package org.cheat.graphics;

import java.util.Arrays;

import org.cheat.client.Card;
import org.cheat.client.Equality;
import org.cheat.client.Card.Rank;
import org.cheat.client.Card.Suit;

/**
 * A representation of a card image.
 */
public final class CardImage extends Equality {

  enum CardImageKind {
    JOKER,
    BACK,
    EMPTY,
    IS_RANK,
    NORMAL,
  }

  public static class Factory {
    public static CardImage getBackOfCardImage() {
      return new CardImage(CardImageKind.BACK, null, null);
    }

    public static CardImage getJoker() {
      return new CardImage(CardImageKind.JOKER, null, null);
    }

    public static CardImage getEmpty() {
      return new CardImage(CardImageKind.EMPTY, null, null);
    }

    public static CardImage getRankImage(Rank rank) {
      return new CardImage(CardImageKind.IS_RANK, rank, null);
    }

    public static CardImage getCardImage(Card card) {
      return new CardImage(CardImageKind.NORMAL, null, card);
    }
  }

  public final CardImageKind kind;
  public final Rank rank;
  public final Card card;

  private CardImage(CardImageKind kind, Rank rank, Card card) {
    this.kind = kind;
    this.rank = rank;
    this.card = card;
  }

  @Override
  public Object getId() {
    return Arrays.asList(kind, rank, card);
  }

  private String rank2str(Rank rank) {
    return rank.ordinal() <= 8 ? "" + (rank.ordinal() + 2)
        : rank == Rank.JACK ? "J"
        : rank == Rank.QUEEN ? "Q"
        : rank == Rank.KING ? "K"
        : rank == Rank.ACE ? "A" : "ERR";
  }
  private String card2str(Rank rank, Suit suit) {
    return (rank.ordinal() <= 7 ? "" + (rank.ordinal() + 2)
        : rank == Rank.TEN ? "t"
        : rank == Rank.JACK ? "j"
        : rank == Rank.QUEEN ? "q"
        : rank == Rank.KING ? "k"
        : rank == Rank.ACE ? "a" : "ERR")
        + (suit == Suit.CLUBS ? "c"
        : suit == Suit.DIAMONDS ? "d"
        : suit == Suit.HEARTS ? "h"
        : suit == Suit.SPADES ? "s" : "ERR"
            );
  }

  @Override
  public String toString() {
    switch (kind) {
      case JOKER:
        return "cards/joker.gif";
      case BACK:
        return "cards/b.gif";
      case EMPTY:
        return "cards/empty.gif";
      case IS_RANK:
        return "cards/Is" + rank2str(rank) + ".gif";
      case NORMAL:
        return "cards/" + card2str(card.getRank(), card.getSuit()) + ".gif";
      default:
        return "Forgot kind=" + kind;
    }
  }
}
