package org.cheat.client;

public enum Color {
  W, B;

  public boolean isWhite() {
    return this == W;
  }

  public boolean isBlack() {
    return this == B;
  }

  public Color getOppositeColor() {
    return this == W ? B : W;
  }
}
