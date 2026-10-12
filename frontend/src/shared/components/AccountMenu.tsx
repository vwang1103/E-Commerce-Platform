"use client";

import Link from "next/link";
import { useState } from "react";
import { useUser } from "@/shared/hooks/useUser";
import { Icon } from "./Icon";

function initials(username: string): string {
  return username
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");
}

export function AccountMenu() {
  const { username, logout } = useUser();
  const [open, setOpen] = useState(false);

  return (
    <div className="relative">
      <button
        type="button"
        className="btn btn-ghost h-11 gap-2 px-2.5 sm:px-3"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
      >
        <span className="flex size-8 items-center justify-center rounded-full bg-emerald-100 text-xs font-bold text-emerald-700">{initials(username)}</span>
        <span className="hidden text-left leading-tight lg:block">
          <span className="block text-xs font-normal text-slate-500">Hello,</span>
          <span className="block max-w-32 truncate">{username}</span>
        </span>
      </button>

      {open && (
        <>
          <button type="button" tabIndex={-1} aria-label="Close account menu" className="fixed inset-0 z-40 cursor-default" onClick={() => setOpen(false)} />
          <div role="menu" className="absolute right-0 z-50 mt-2 w-64 animate-fade-in rounded-2xl border border-slate-200 bg-white p-3 shadow-xl">
            <div className="px-3 py-2">
              <p className="text-xs text-slate-500">Signed in as</p>
              <p className="truncate font-semibold">{username}</p>
            </div>
            <nav className="mt-1 grid gap-1 border-t border-slate-100 pt-2 text-sm font-medium">
              <Link href="/orders/" onClick={() => setOpen(false)} className="flex items-center gap-2 rounded-lg px-3 py-2 hover:bg-slate-50">
                <Icon name="package" className="size-4 text-slate-500" /> Your orders
              </Link>
              <Link href="/seller/" onClick={() => setOpen(false)} className="flex items-center gap-2 rounded-lg px-3 py-2 hover:bg-slate-50">
                <Icon name="store" className="size-4 text-slate-500" /> Seller console
              </Link>
              <button
                type="button"
                role="menuitem"
                className="mt-1 rounded-lg bg-rose-600 px-3 py-2 text-left font-semibold text-white hover:bg-rose-700"
                onClick={() => {
                  setOpen(false);
                  logout();
                }}
              >
                Sign out
              </button>
            </nav>
          </div>
        </>
      )}
    </div>
  );
}
