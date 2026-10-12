"use client";

import { CartDrawer } from "@/features/cart/components/CartDrawer";
import { useUser } from "@/shared/hooks/useUser";
import { useHydrated } from "@/shared/lib/store";
import { Footer } from "./Footer";
import { Header } from "./Header";
import { LoginScreen } from "./LoginScreen";

export function StorefrontShell({ children }: Readonly<{ children: React.ReactNode }>) {
  const hydrated = useHydrated();
  const { user } = useUser();

  if (!hydrated) return <div className="min-h-screen bg-[#f5f7f3]" />;
  if (!user) return <LoginScreen />;

  return (
    <>
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-[70] focus:rounded-full focus:bg-white focus:px-4 focus:py-2 focus:shadow-lg"
      >
        Skip to content
      </a>
      <Header />
      <main id="main" className="flex-1">
        {children}
      </main>
      <Footer />
      <CartDrawer />
    </>
  );
}
