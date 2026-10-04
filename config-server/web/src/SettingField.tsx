import { ComponentType } from "react";
import { SettingSpec } from "./api";
import { ComplimentsEditor } from "./ComplimentsEditor";

/**
 * Editors a module can ask for by name with `type: "custom"`. The schema names
 * the editor, so adding one here is the only change the console needs — it
 * never branches on a module name. An unknown name falls back to a textarea so
 * the value stays editable.
 */
const EDITORS: Record<string, ComponentType<{ value: string; onChange: (v: string) => void }>> = {
  compliments: ComplimentsEditor,
};

/** The value in effect for a key: what is saved, else what the module defaults to. */
function effective(spec: SettingSpec | undefined, value: string | undefined): string {
  if (value !== undefined && value !== "") return value;
  return spec?.default ?? "";
}

/**
 * Resolves the first usable `preview` template. `{key}` interpolates the key's
 * effective value and `{key|fallback}` substitutes `fallback` when it is blank.
 * A template is skipped when any of its plain `{key}` references is blank, so a
 * module can list a specific template first and a derived one after it.
 */
export function resolvePreview(
  templates: string[],
  config: Record<string, string>,
  schema: SettingSpec[],
): string | null {
  const valueOf = (key: string) =>
    effective(schema.find((s) => s.key === key), config[key]);

  for (const tpl of templates) {
    let usable = true;
    const out = tpl.replace(/\{([^}|]+)(?:\|([^}]*))?\}/g, (_m, key: string, fallback?: string) => {
      const v = valueOf(key.trim());
      if (v) return v;
      if (fallback === undefined) {
        usable = false;
        return "";
      }
      return fallback;
    });
    if (usable && out.trim()) return out;
  }
  return null;
}

/** Renders one declared setting as a labelled, typed control. */
export function SettingField({
  spec, value, config, schema, ips, onChange,
}: {
  spec: SettingSpec;
  value: string | undefined;
  config: Record<string, string>;
  schema: SettingSpec[];
  ips: string[];
  onChange: (value: string) => void;
}) {
  const v = value ?? "";
  const preview = spec.preview.length ? resolvePreview(spec.preview, config, schema) : null;
  const hint = (
    <>
      {spec.help && <span className="field-help muted small">{spec.help}</span>}
      {preview && (
        <span className="field-help muted small">
          Resolves to: <code>{preview}</code>
        </span>
      )}
    </>
  );

  if (spec.type === "bool") {
    const on = (v || spec.default) === "true";
    return (
      <div className="field field-bool">
        <label className="bool-label">
          <input type="checkbox" checked={on} onChange={(e) => onChange(String(e.target.checked))} />
          <span className="bool-text">{spec.label}</span>
        </label>
        {hint}
      </div>
    );
  }

  if (spec.type === "custom") {
    const Editor = EDITORS[spec.editor];
    return (
      <div className="field field-wide">
        <span className="field-label">{spec.label}</span>
        {Editor ? (
          <Editor value={v} onChange={onChange} />
        ) : (
          <textarea rows={6} value={v} placeholder={spec.placeholder} onChange={(e) => onChange(e.target.value)} />
        )}
        {hint}
      </div>
    );
  }

  let control;
  if (spec.type === "enum") {
    control = (
      <select value={v || spec.default} onChange={(e) => onChange(e.target.value)}>
        {spec.options.map((o) => <option key={o} value={o}>{o}</option>)}
        {/* A saved value the module no longer offers stays selectable rather than
            being silently rewritten to the first option. */}
        {v && !spec.options.includes(v) && <option value={v}>{v} (unsupported)</option>}
      </select>
    );
  } else if (spec.type === "ip") {
    control = (
      <select value={v} onChange={(e) => onChange(e.target.value)}>
        <option value="">Auto-detect (LAN)</option>
        {ips.map((x) => <option key={x} value={x}>{x}</option>)}
        {v && !ips.includes(v) && <option value={v}>{v} (custom)</option>}
      </select>
    );
  } else if (spec.type === "int") {
    control = (
      <input
        type="number"
        value={v}
        placeholder={spec.placeholder || spec.default}
        min={spec.min ?? undefined}
        max={spec.max ?? undefined}
        step={spec.step ?? 1}
        onChange={(e) => onChange(e.target.value)}
      />
    );
  } else if (spec.type === "text") {
    control = (
      <textarea rows={4} value={v} placeholder={spec.placeholder} onChange={(e) => onChange(e.target.value)} />
    );
  } else {
    control = (
      <input
        type={spec.type === "url" ? "url" : "text"}
        value={v}
        placeholder={spec.placeholder || spec.default}
        onChange={(e) => onChange(e.target.value)}
      />
    );
  }

  const wide = spec.type === "url" || spec.type === "text";
  return (
    <div className={"field" + (wide ? " field-wide" : "")}>
      <label>
        {spec.label}
        {control}
      </label>
      {hint}
    </div>
  );
}
