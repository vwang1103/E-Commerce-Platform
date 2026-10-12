"use client";

import { useId, type ReactNode } from "react";
import { useModalBehavior } from "@/shared/hooks/useModalBehavior";
import { Icon } from "./Icon";

export function Modal({
  open,
  onClose,
  title,
  children,
  wide = false,
}: Readonly<{ open: boolean; onClose: () => void; title: string; children: ReactNode; wide?: boolean }>) {
  const titleId = useId();
  useModalBehavior(open, onClose);
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center sm:p-4">
      <button
        type="button"
        tabIndex={-1}
        aria-label="Close dialog"
        className="absolute inset-0 animate-fade-in bg-slate-900/50"
        onClick={onClose}
      />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className={`relative max-h-[92vh] w-full animate-slide-up overflow-y-auto rounded-t-3xl bg-white p-6 shadow-2xl sm:rounded-3xl ${wide ? "sm:max-w-2xl" : "sm:max-w-md"}`}
      >
        <div className="mb-5 flex items-center justify-between gap-4">
          <h2 id={titleId} className="text-xl font-bold">
            {title}
          </h2>
          <button type="button" className="btn btn-ghost size-9 shrink-0 rounded-full p-0" onClick={onClose} aria-label="Close">
            <Icon name="close" />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}
