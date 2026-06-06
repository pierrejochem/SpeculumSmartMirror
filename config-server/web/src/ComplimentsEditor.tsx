import { useEffect, useMemo, useState } from "react";

// Built-in defaults (mirror ComplimentsModule.DEFAULTS) shown when none are set.
const DEFAULTS: Record<string, string[]> = {
  morning: ["Good morning, handsome!", "Enjoy your day!", "How was your sleep?"],
  afternoon: ["Hello, beauty!", "You look sexy!", "Looking good today!"],
  evening: ["Wow, you look hot!", "You look nice!", "Hi, sexy!"],
};

const CATEGORIES = ["morning", "afternoon", "evening", "anytime"] as const;

type Pools = Record<string, string[]>;

function parse(value: string): Pools {
  if (!value.trim()) return { ...DEFAULTS };
  try {
    const obj = JSON.parse(value);
    const out: Pools = {};
    for (const [k, v] of Object.entries(obj)) out[k] = Array.isArray(v) ? (v as string[]) : [];
    return out;
  } catch {
    return { ...DEFAULTS };
  }
}

function serialize(pools: Pools): string {
  const clean: Pools = {};
  for (const [k, lines] of Object.entries(pools)) {
    const trimmed = lines.map((l) => l.trim()).filter(Boolean);
    if (k.trim() && trimmed.length) clean[k] = trimmed;
  }
  return JSON.stringify(clean);
}

const filtered = (lines: string[]) => lines.map((l) => l.trim()).filter(Boolean);

/**
 * Textarea backed by a local buffer (one line = one compliment). Empty lines
 * are kept while typing — so Enter adds a new line — and only filtered out when
 * serialized for storage. Resyncs only when the stored value really differs.
 */
function LineBox({
  lines, onLines, rows, placeholder,
}: { lines: string[]; onLines: (l: string[]) => void; rows: number; placeholder?: string }) {
  const [text, setText] = useState(lines.join("\n"));
  useEffect(() => {
    if (filtered(lines).join("\n") !== filtered(text.split("\n")).join("\n")) {
      setText(lines.join("\n"));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [lines]);
  return (
    <textarea
      rows={rows}
      placeholder={placeholder}
      value={text}
      onChange={(e) => {
        setText(e.target.value);
        onLines(e.target.value.split("\n"));
      }}
    />
  );
}

/**
 * Friendly editor for the compliments module's text pools. Edits the JSON
 * stored under config["compliments"] (one compliment per line per category,
 * plus optional date-based entries keyed by a date regex like "....-12-25").
 */
export function ComplimentsEditor({
  value, onChange,
}: { value: string; onChange: (v: string) => void }) {
  const pools = useMemo(() => parse(value), [value]);

  const setLines = (key: string, lines: string[]) =>
    onChange(serialize({ ...pools, [key]: lines }));

  const dateKeys = Object.keys(pools).filter((k) => !CATEGORIES.includes(k as any));

  const renameDateKey = (oldKey: string, newKey: string) => {
    const next: Pools = {};
    for (const [k, v] of Object.entries(pools)) next[k === oldKey ? newKey : k] = v;
    onChange(serialize(next));
  };
  const removeDateKey = (key: string) => {
    const next = { ...pools };
    delete next[key];
    onChange(serialize(next));
  };
  const addDateKey = () => onChange(serialize({ ...pools, "....-01-01": ["Happy New Year!"] }));

  return (
    <div className="compl">
      <div className="compl-hint">One compliment per line. Empty lines are ignored.</div>
      {CATEGORIES.map((cat) => (
        <label key={cat} className="compl-cat">
          {cat}
          <LineBox
            lines={pools[cat] ?? []}
            rows={3}
            placeholder={cat === "anytime" ? "(shown in every time slot)" : ""}
            onLines={(l) => setLines(cat, l)}
          />
        </label>
      ))}

      <div className="compl-dates">
        <div className="card-head">
          <span className="muted small">Date-based (regex on YYYY-MM-DD, e.g. <code>....-12-25</code>)</span>
          <button className="ghost small" onClick={addDateKey}>+ date</button>
        </div>
        {dateKeys.map((key, i) => (
          <div className="compl-date" key={i}>
            <input className="k" value={key} onChange={(e) => renameDateKey(key, e.target.value)} />
            <LineBox lines={pools[key] ?? []} rows={2} onLines={(l) => setLines(key, l)} />
            <button className="ghost danger" onClick={() => removeDateKey(key)}>×</button>
          </div>
        ))}
      </div>
    </div>
  );
}