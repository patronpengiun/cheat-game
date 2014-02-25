package org.cheat.graphics;

import org.cheat.client.CheatLogic;
import org.cheat.client.CheatPresenter;
import org.cheat.client.GameApi;
import org.cheat.client.GameApi.Game;
import org.cheat.client.GameApi.IteratingPlayerContainer;
import org.cheat.client.GameApi.UpdateUI;
import org.cheat.client.GameApi.VerifyMove;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class CheatEntryPoint implements EntryPoint {
  IteratingPlayerContainer container;
  CheatPresenter cheatPresenter;

  @Override
  public void onModuleLoad() {
    Game game = new Game() {
      @Override
      public void sendVerifyMove(VerifyMove verifyMove) {
        container.sendVerifyMoveDone(new CheatLogic().verify(verifyMove));
      }

      @Override
      public void sendUpdateUI(UpdateUI updateUI) {
        cheatPresenter.updateUI(updateUI);
      }
    };
    container = new IteratingPlayerContainer(game, 2);
    CheatGraphics cheatGraphics = new CheatGraphics();
    cheatPresenter =
        new CheatPresenter(cheatGraphics, container);
    final ListBox playerSelect = new ListBox();
    playerSelect.addItem("WhitePlayer");
    playerSelect.addItem("BlackPlayer");
    playerSelect.addItem("Viewer");
    playerSelect.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        int selectedIndex = playerSelect.getSelectedIndex();
        int playerId = selectedIndex == 2 ? GameApi.VIEWER_ID
            : container.getPlayerIds().get(selectedIndex);
        container.updateUi(playerId);
      }
    });
    FlowPanel flowPanel = new FlowPanel();
    flowPanel.add(cheatGraphics);
    flowPanel.add(playerSelect);
    RootPanel.get("mainDiv").add(flowPanel);
    container.sendGameReady();
    container.updateUi(container.getPlayerIds().get(0));
  }
}