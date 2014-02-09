package org.cheat.client;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import org.cheat.client.Card.Rank;

import com.google.common.collect.ImmutableList;

/**
 * A claim in a game of cheat is: "I have placed down 3 queens." So we record the cardRank and the
 * numberOfCards.
 */
public class Claim extends Equality {
  @Nullable public static Claim fromClaimEntryInGameState(@Nullable List<String> claimEntry) {
    if (claimEntry == null) {
      return null;
    }
    //"2cards", "rankA"
    int numberOfCards = Integer.parseInt(claimEntry.get(0).substring(0, 1));
    Rank cardRank = Rank.fromFirstLetter(claimEntry.get(1).substring(4));
    return new Claim(cardRank, numberOfCards);
  }

  @Nullable public static List<String> toClaimEntryInGameState(@Nullable Claim claim) {
    return claim == null ? null : ImmutableList.of(claim.getNumberOfCards() + "cards",
        "rank" + claim.getCardRank().getFirstLetter());
  }

  private final Rank cardRank;
  private final int numberOfCards;

  public Claim(Rank cardRank, int numberOfCards) {
    checkArgument(numberOfCards >= 0 && numberOfCards <= 4);
    this.cardRank = cardRank;
    this.numberOfCards = numberOfCards;
  }

  /**
   * @return if rank is -1,0,+1 from cardRank, i.e., rank is different by at most 1 from this claim.
   */
  public boolean isClose(Rank rank) {
    return rank == cardRank || rank == cardRank.getPrev() || rank == cardRank.getNext();
  }

  /**
   * @return the cardRank
   */
  public Rank getCardRank() {
    return cardRank;
  }

  /**
   * @return the numberOfCards
   */
  public int getNumberOfCards() {
    return numberOfCards;
  }

  @Override
  public Object getId() {
    return Arrays.asList(cardRank, numberOfCards);
  }
}
