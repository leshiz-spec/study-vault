export const TAG_PALETTE = [
  { name: "Red", value: "#8f4d4d" },
  { name: "Orange", value: "#86583d" },
  { name: "Amber", value: "#786128" },
  { name: "Yellow", value: "#67632f" },
  { name: "Lime", value: "#587044" },
  { name: "Green", value: "#3f6f58" },
  { name: "Teal", value: "#3d6f69" },
  { name: "Cyan", value: "#426b75" },
  { name: "Sky Blue", value: "#4b6d82" },
  { name: "Blue", value: "#4c6380" },
  { name: "Indigo", value: "#585a78" },
  { name: "Violet", value: "#695a78" },
  { name: "Purple", value: "#755772" },
  { name: "Pink", value: "#85596d" },
  { name: "Rose", value: "#8a5360" },
  { name: "Slate", value: "#596662" },
];

const TAG_COLORS: Record<string, string> = Object.fromEntries(
  TAG_PALETTE.map(({ name, value }) => [name, value]),
);

export function tagColor(name?: string | null) {
  return TAG_COLORS[name || "Blue"] || TAG_COLORS.Slate;
}
