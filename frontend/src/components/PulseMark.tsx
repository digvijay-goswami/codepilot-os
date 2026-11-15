export default function PulseMark({ size = 24 }: { size?: number }) {
  return (
    <svg
      className="pulse-mark"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M2 13h5l3-8 4 14 3-6h5" />
    </svg>
  )
}
