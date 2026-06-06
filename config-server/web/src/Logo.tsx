// Brand marks derived from Images/speculum-logo.svg. The diamond "looking-glass"
// mark carries the violet→teal glass gradient; the wordmark is adapted for the
// dark admin theme (ink → light, violet "u" kept). System fonts only — the
// mirror device may be offline, so no web-font fetches.

let gid = 0;

/** Diamond glass mark. Scales to `size` px. */
export function Mark({ size = 40, className }: { size?: number; className?: string }) {
  // Unique gradient/clip ids so multiple marks on one page don't collide.
  const id = `mk${gid++}`;
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 240 240"
      className={className}
      role="img"
      aria-label="Speculum"
      xmlns="http://www.w3.org/2000/svg"
    >
      <defs>
        <linearGradient id={`${id}-glass`} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#7B61FF" />
          <stop offset="1" stopColor="#3DD4C8" />
        </linearGradient>
        <clipPath id={`${id}-clip`}>
          <path d="M120 30 L210 120 L120 210 L30 120 Z" />
        </clipPath>
      </defs>
      <g clipPath={`url(#${id}-clip)`}>
        <rect x="30" y="30" width="180" height="180" fill={`url(#${id}-glass)`} />
        <rect x="30" y="30" width="90" height="180" fill="#000" opacity="0.14" />
        <path d="M48 75 L108 75 L168 165 L108 165 Z" fill="#fff" opacity="0.22" />
        <path d="M60 105 L87 105 L123 150 L96 150 Z" fill="#fff" opacity="0.18" />
      </g>
      <path
        d="M120 30 L210 120 L120 210 L30 120 Z"
        fill="none"
        stroke="#7B61FF"
        strokeWidth="3"
        opacity="0.5"
      />
      <line x1="120" y1="30" x2="120" y2="210" stroke="#fff" strokeWidth="2.5" opacity="0.45" />
    </svg>
  );
}

/** Full lockup: mark + serif wordmark + mono tagline. Used on the login screen. */
export function Wordmark() {
  return (
    <div className="lockup">
      <Mark size={68} />
      <div className="lockup-text">
        <div className="wordmark">
          Specul<span className="wordmark-u">u</span>m
        </div>
        <div className="tagline">SMART MIRROR · KOTLIN</div>
      </div>
    </div>
  );
}