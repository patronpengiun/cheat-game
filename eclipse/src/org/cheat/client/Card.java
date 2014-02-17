package org.cheat.client;

import java.util.Arrays;
import java.util.Comparator;

public class Card extends Equality {
  public enum Suit  {
    CLUBS, DIAMONDS, HEARTS, SPADES;

    private static final Suit[] VALUES = values();

    public static Suit fromFirstLetterLowerCase(String firstLetterLowerCase) {
      for (Suit suit : VALUES) {
        if (suit.getFirstLetterLowerCase().equals(firstLetterLowerCase)) {
          return suit;
        }
      }
      throw new IllegalArgumentException(
          "Did not find firstLetterLowerCase=" + firstLetterLowerCase);
    }

    public String getFirstLetterLowerCase() {
      return name().substring(0, 1).toLowerCase();
    }

    public Suit getNext() {
      if (this == VALUES[VALUES.length - 1]) {
        return VALUES[0];
      }
      return values()[ordinal() + 1];
    }

    public Suit getPrev() {
      if (this == VALUES[0]) {
        return VALUES[VALUES.length - 1];
      }
      return values()[ordinal() - 1];
    }
  }

  public enum Rank {
    TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING, ACE;

    private static final Rank[] VALUES = values();

    public static final Comparator<Rank> ACE_LOW_COMPARATOR = new Comparator<Rank>() {
      @Override
      public int compare(Rank o1, Rank o2) {
        int ord1 = o1 == ACE ? -1 : o1.ordinal();
        int ord2 = o2 == ACE ? -1 : o2.ordinal();
        return ord1 - ord2;
      }
    };

    public static final Comparator<Rank> TWO_HIGH_COMPARATOR = new Comparator<Rank>() {
      @Override
      public int compare(Rank o1, Rank o2) {
        int ord1 = o1 == TWO ? 13 : o1.ordinal();
        int ord2 = o2 == TWO ? 13 : o2.ordinal();
        return ord1 - ord2;
      }
    };

    public static Rank fromFirstLetter(String rankString) {
      int rankIndex = rankString.equals("J") ? 11
          : rankString.equals("Q") ? 12
          : rankString.equals("K") ? 13
          : rankString.equals("A") ? 14 : Integer.valueOf(rankString);
      return VALUES[rankIndex - 2];
    }

    public String getFirstLetter() {
      int rank = this.ordinal() + 2;
      return rank <= 10 ? String.valueOf(rank)
          : rank == 11 ? "J"
          : rank == 12 ? "Q"
          : rank == 13 ? "K" : "A";
    }

    public Rank getNext() {
      if (this == VALUES[VALUES.length - 1]) {
        return VALUES[0];
      }
      return values()[ordinal() + 1];
    }

    public Rank getPrev() {
      if (this == VALUES[0]) {
        return VALUES[VALUES.length - 1];
      }
      return values()[ordinal() - 1];
    }
  }

  public static final Comparator<Card> COMPARATOR = new Comparator<Card>() {
    @Override
    public int compare(Card o1, Card o2) {
      int rank = o1.rankValue.compareTo(o2.rankValue);
      int suit = o1.suitValue.compareTo(o2.suitValue);
      return rank == 0 ? suit : rank;
    }
  };

  private Suit suitValue;
  private Rank rankValue;


  /**
   * Creates a new playing card.
   *
   * @param suit the suit value of this card.
   * @param rank the rank value of this card.
   */
  public Card(Suit suit, Rank rank) {
    suitValue = suit;
    rankValue = rank;
  }

  /**
   * Returns the suit of the card.
   *
   * @return a Suit constant representing the suit value of the card.
   */
  public Suit getSuit() {
    return suitValue;
  }


  /**
   * Returns the rank of the card.
   *
   * @return a Rank constant representing the rank value of the card.
   */
  public Rank getRank() {
    return rankValue;
  }

  /**
   * Returns a description of this card.
   *
   * @return the name of the card.
   */
  @Override
  public String toString() {
    return rankValue.toString() + " of " + suitValue.toString();
  }

  @Override
  public Object getId() {
    return Arrays.asList(getSuit(), getRank());
  }
}
