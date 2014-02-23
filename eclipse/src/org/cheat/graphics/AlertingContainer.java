package org.cheat.graphics;

import java.util.List;

import org.cheat.client.GameApi.Container;
import org.cheat.client.GameApi.Operation;

import com.google.gwt.user.client.Window;

public class AlertingContainer implements Container {

  @Override
  public void sendGameReady() {
    Window.alert("sendGameReady");
  }

  @Override
  public void sendMakeMove(List<Operation> operations) {
    Window.alert("sendMakeMove: " + operations);
  }
}
