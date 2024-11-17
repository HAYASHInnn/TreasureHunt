package plugin.treasurehunt.command;

public enum DropItem {
  GOLDEN_APPLE_DROP("golden_apple"),
  APPLE_DROP("apple"),
  NONE_DROP("none");

  private String dropItem;

  DropItem(String dropItem) {
    this.dropItem = dropItem;
  }
}