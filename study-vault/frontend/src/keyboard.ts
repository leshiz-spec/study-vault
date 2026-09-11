const MAC_PLATFORM = /Mac|iPhone|iPad|iPod/i.test(
  typeof navigator === "undefined" ? "" : navigator.platform,
);

export const shortcutModifier = MAC_PLATFORM ? "⌘" : "Ctrl";

export function hasShortcutModifier(event: KeyboardEvent) {
  return event.metaKey || event.ctrlKey;
}

export function isEditableTarget(target: EventTarget | null) {
  const element = target instanceof HTMLElement ? target : null;
  if (!element) return false;
  if (element.isContentEditable || element.tagName === "TEXTAREA") return true;
  if (element.tagName !== "INPUT") return element.tagName === "SELECT";
  const type = (element as HTMLInputElement).type.toLowerCase();
  return ![
    "button",
    "checkbox",
    "color",
    "file",
    "hidden",
    "image",
    "radio",
    "range",
    "reset",
    "submit",
  ].includes(type);
}
