"use client";

import { useStore } from "@/shared/lib/store";
import { dismissToast, toastStore } from "@/shared/lib/toast";
import { Icon } from "./Icon";

const TONES = {
  success: "bg-slate-900 text-white",
  info: "bg-slate-900 text-white",
  error: "bg-rose-600 text-white",
} as const;

export function Toaster() {
  const toasts = useStore(toastStore);
  return (
    <div aria-live="polite" className="pointer-events-none fixed inset-x-0 bottom-4 z-[60] flex flex-col items-center gap-2 px-4">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          role={toast.tone === "error" ? "alert" : "status"}
          className={`pointer-events-auto flex max-w-md animate-slide-up items-center gap-3 rounded-full py-2.5 pr-3 pl-5 text-sm font-medium shadow-lg ${TONES[toast.tone]}`}
        >
          <span>{toast.message}</span>
          <button
            type="button"
            className="flex size-6 items-center justify-center rounded-full hover:bg-white/20"
            onClick={() => dismissToast(toast.id)}
            aria-label="Dismiss notification"
          >
            <Icon name="close" className="size-4" />
          </button>
        </div>
      ))}
    </div>
  );
}
