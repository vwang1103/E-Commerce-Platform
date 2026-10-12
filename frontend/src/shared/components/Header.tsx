"use client";

import Link from "next/link";
import { useCart } from "@/features/cart/hooks/useCart";
import { AccountMenu } from "./AccountMenu";
import { Icon } from "./Icon";
import { SearchBox } from "@/features/catalog/components/SearchBox";

export function Header() {
  const { count, setDrawerOpen } = useCart();

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur">
      <div className="bg-emerald-600 text-xs text-white">
        <div className="container-page flex min-h-8 items-center justify-center py-1 sm:justify-between">
          <p>Fresh finds, live stock, and a simple checkout.</p>
          <Link href="/seller/" className="hidden font-medium hover:underline sm:block">
            Sell with us
          </Link>
        </div>
      </div>

      <div className="container-page flex flex-wrap items-center gap-x-5 gap-y-3 py-4">
        <Link href="/" className="shrink-0 text-3xl font-bold tracking-tight text-slate-700" aria-label="Arcade home">
          <span className="text-emerald-600">arc</span>ade<span className="text-emerald-500">.</span>
        </Link>

        <nav aria-label="Primary navigation" className="hidden items-center gap-6 text-sm font-medium text-slate-600 lg:flex">
          <Link href="/" className="hover:text-emerald-700">Home</Link>
          <Link href="/shop/" className="hover:text-emerald-700">Shop</Link>
          <Link href="/orders/" className="hover:text-emerald-700">Orders</Link>
        </nav>

        <SearchBox className="order-last w-full md:order-none md:ml-auto md:w-auto md:max-w-sm md:flex-1" />

        <nav aria-label="Account and cart" className="ml-auto flex items-center gap-1 md:ml-0">
          <Link href="/orders/" className="btn btn-ghost hidden h-11 gap-2 px-2.5 sm:flex lg:hidden">
            <Icon name="package" />
            <span className="hidden lg:inline">Orders</span>
            <span className="sr-only lg:hidden">Your orders</span>
          </Link>
          <AccountMenu />
          <button type="button" className="btn relative h-11 gap-2 bg-slate-700 px-4 text-white hover:bg-slate-800" onClick={() => setDrawerOpen(true)}>
            <Icon name="cart" />
            <span className="hidden sm:inline">Cart</span>
            {count > 0 && (
              <span
                aria-label={`${count} items in cart`}
                className="absolute -top-1.5 -right-1.5 flex min-w-5 items-center justify-center rounded-full bg-rose-500 px-1.5 text-[11px] leading-5 font-bold text-white"
              >
                {count}
              </span>
            )}
            {count === 0 && <span className="sr-only">Open cart</span>}
          </button>
        </nav>
      </div>

    </header>
  );
}
