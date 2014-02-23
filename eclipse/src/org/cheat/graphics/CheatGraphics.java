package org.cheat.graphics;

import java.util.List;

import org.cheat.client.Card;
import org.cheat.client.Card.Rank;
import org.cheat.client.CheatPresenter;
import org.cheat.client.CheatPresenter.CheaterMessage;
import org.cheat.client.Claim;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

/**
 * Graphics for the game of cheat.
 */
public class CheatGraphics extends Composite implements CheatPresenter.View {
  public interface CheatGraphicsUiBinder extends UiBinder<Widget, CheatGraphics> {
  }

  @UiField
  HorizontalPanel opponentArea;
  @UiField
  HorizontalPanel playerArea;
  @UiField
  HorizontalPanel selectedArea;
  @UiField
  HorizontalPanel middleArea;
  @UiField
  Button claimBtn;
  private boolean enableClicks = false;
  private final CardImageSupplier cardImageSupplier;
  private CheatPresenter presenter;

  public CheatGraphics() {
    CardImages cardImages = GWT.create(CardImages.class);
    this.cardImageSupplier = new CardImageSupplier(cardImages);
    CheatGraphicsUiBinder uiBinder = GWT.create(CheatGraphicsUiBinder.class);
    initWidget(uiBinder.createAndBindUi(this));
  }

  private List<Image> createBackCards(int numOfCards) {
    List<CardImage> images = Lists.newArrayList();
    for (int i = 0; i < numOfCards; i++) {
      images.add(CardImage.Factory.getBackOfCardImage());
    }
    return createImages(images, false);
  }

  private List<Image> createCardImages(List<Card> cards, boolean withClick) {
    List<CardImage> images = Lists.newArrayList();
    for (Card card : cards) {
      images.add(CardImage.Factory.getCardImage(card));
    }
    return createImages(images, withClick);
  }

  private List<Image> createImages(List<CardImage> images, boolean withClick) {
    List<Image> res = Lists.newArrayList();
    for (CardImage img : images) {
      final CardImage imgFinal = img;
      Image image = new Image(cardImageSupplier.getResource(img));
      if (withClick) {
        image.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            if (enableClicks) {
              presenter.cardSelected(imgFinal.card);
            }
          }
        });
      }
      res.add(image);
    }
    return res;
  }

  private void placeImages(HorizontalPanel panel, List<Image> images) {
    panel.clear();
    Image last = images.isEmpty() ? null : images.get(images.size() - 1);
    for (Image image : images) {
      FlowPanel imageContainer = new FlowPanel();
      imageContainer.setStyleName(image != last ? "imgShortContainer" : "imgContainer");
      imageContainer.add(image);
      panel.add(imageContainer);
    }
  }

  private void alertCheaterMessage(CheaterMessage cheaterMessage, Optional<Claim> lastClaim) {
    String message = "";
    List<String> options = Lists.newArrayList();
    final String callCheatOption = "Call cheater!";
    if (lastClaim.isPresent()) {
      Claim claim = lastClaim.get();
      message = "Player dropped " + claim.getNumberOfCards()
          + " cards, and claimed they are of rank " + claim.getCardRank() + ". ";
    }
    switch (cheaterMessage) {
      case WAS_CHEATING:
        message += "The player was cheating.";
        break;
      case WAS_NOT_CHEATING:
        message += "The player was NOT cheating.";
        break;
      case IS_OPPONENT_CHEATING:
        message += "Did the opponent cheat?";
        options.add("Probably told the truth");
        options.add(callCheatOption);
        break;
      case INVISIBLE:
        break;
      default:
        break;
    }
    if (message.isEmpty()) {
      return;
    }
    new PopupChoices(message, options,
        new PopupChoices.OptionChosen() {
      @Override
      public void optionChosen(String option) {
        if (option.equals(callCheatOption)) {
          presenter.declaredCheater();
        }
      }
    }).center();
  }

  private void disableClicks() {
    claimBtn.setEnabled(false);
    enableClicks = false;
  }

  @UiHandler("claimBtn")
  void onClickClaimBtn(ClickEvent e) {
    disableClicks();
    presenter.finishedSelectingCards();
  }

  @Override
  public void setPresenter(CheatPresenter cheatPresenter) {
    this.presenter = cheatPresenter;
  }

  @Override
  public void setViewerState(int numberOfWhiteCards, int numberOfBlackCards,
      int numberOfCardsInMiddlePile, CheaterMessage cheaterMessage,
      Optional<Claim> lastClaim) {
    placeImages(playerArea, createBackCards(numberOfWhiteCards));
    placeImages(selectedArea, ImmutableList.<Image>of());
    placeImages(opponentArea, createBackCards(numberOfBlackCards));
    placeImages(middleArea, createBackCards(numberOfCardsInMiddlePile));
    alertCheaterMessage(cheaterMessage, lastClaim);
    disableClicks();
  }

  @Override
  public void setPlayerState(int numberOfOpponentCards, int numberOfCardsInMiddlePile,
      List<Card> myCards, CheaterMessage cheaterMessage,
      Optional<Claim> lastClaim) {
    placeImages(playerArea, createCardImages(myCards, false));
    placeImages(selectedArea, ImmutableList.<Image>of());
    placeImages(opponentArea, createBackCards(numberOfOpponentCards));
    placeImages(middleArea, createBackCards(numberOfCardsInMiddlePile));
    alertCheaterMessage(cheaterMessage, lastClaim);
    disableClicks();
  }

  @Override
  public void chooseNextCard(List<Card> selectedCards, List<Card> remainingCards) {
    enableClicks = true;
    placeImages(playerArea, createCardImages(remainingCards, true));
    placeImages(selectedArea, createCardImages(selectedCards, true));
    claimBtn.setEnabled(true);
  }

  @Override
  public void chooseRankForClaim(List<Rank> possibleClaims) {
    List<String> options = Lists.newArrayList();
    for (Rank rank : possibleClaims) {
      options.add(rank.toString());
    }
    new PopupChoices("Choose rank", options, new PopupChoices.OptionChosen() {
          @Override
          public void optionChosen(String option) {
            presenter.rankSelected(Rank.valueOf(option));
          }
        }).center();
  }
}
