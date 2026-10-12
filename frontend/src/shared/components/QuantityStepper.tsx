import { Icon } from "./Icon";

const lowerFirst = (text: string) => text.charAt(0).toLowerCase() + text.slice(1);

export function QuantityStepper({
  value,
  onChange,
  min = 1,
  max,
  label = "Quantity",
  size = "md",
}: Readonly<{
  value: number;
  onChange: (value: number) => void;
  min?: number;
  max: number;
  label?: string;
  size?: "sm" | "md";
}>) {
  const button = `flex items-center justify-center text-slate-700 transition-colors hover:bg-slate-100 disabled:cursor-not-allowed disabled:text-slate-300 disabled:hover:bg-transparent ${size === "sm" ? "size-8" : "size-11"}`;
  return (
    <div
      role="group"
      aria-label={label}
      className={`inline-flex items-center rounded-full border border-slate-300 bg-white ${size === "sm" ? "h-8" : "h-11"}`}
    >
      <button
        type="button"
        className={`${button} rounded-l-full`}
        disabled={value <= min}
        onClick={() => onChange(value - 1)}
        aria-label={`Decrease ${lowerFirst(label)}`}
      >
        <Icon name="minus" className="size-4" />
      </button>
      <output aria-live="polite" className={`text-center text-sm font-semibold tabular-nums ${size === "sm" ? "w-7" : "w-9"}`}>
        {value}
      </output>
      <button
        type="button"
        className={`${button} rounded-r-full`}
        disabled={value >= max}
        onClick={() => onChange(value + 1)}
        aria-label={`Increase ${lowerFirst(label)}`}
      >
        <Icon name="plus" className="size-4" />
      </button>
    </div>
  );
}
